package com.subgraph.orchid.circuits;

import com.subgraph.orchid.*;
import com.subgraph.orchid.circuits.cells.CellImpl;
import com.subgraph.orchid.circuits.cells.RelayCellImpl;
import com.subgraph.orchid.dashboard.DashboardRenderable;
import com.subgraph.orchid.dashboard.DashboardRenderer;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CircuitIO implements DashboardRenderable {
    private static final Logger logger = Logger.getLogger(CircuitIO.class.getName());
    private final static long CIRCUIT_BUILD_TIMEOUT_MS = 30 * 1000;
    private final static long CIRCUIT_RELAY_RESPONSE_TIMEOUT = 20 * 1000;

    private final CircuitImpl circuit;
    private final Connection connection;
    private final int circuitId;

    private final BlockingQueue<RelayCell> relayCellResponseQueue;
    private final BlockingQueue<Cell> controlCellResponseQueue;
    private final Map<Integer, StreamImpl> streamMap;
    private final Object relaySendLock = new Object();

    private boolean isMarkedForClose;
    private boolean isClosed;

    CircuitIO(CircuitImpl circuit, Connection connection, int circuitId) {
        this.circuit = circuit;
        this.connection = connection;
        this.circuitId = circuitId;

        this.relayCellResponseQueue = new LinkedBlockingQueue<RelayCell>();
        this.controlCellResponseQueue = new LinkedBlockingQueue<Cell>();
        this.streamMap = new HashMap<Integer, StreamImpl>();
    }

    Connection getConnection() {
        return connection;
    }

    int getCircuitId() {
        return circuitId;
    }

    RelayCell dequeueRelayResponseCell() {
        try {
            final long timeout = getReceiveTimeout();
            return relayCellResponseQueue.poll(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    private RelayCell decryptRelayCell(Cell cell) {
        for (CircuitNode node : circuit.getNodeList()) {
            if (node.decryptBackwardCell(cell)) {
                return RelayCellImpl.createFromCell(node, cell);
            }
        }
        destroyCircuit();
        throw new TorException("Could not decrypt relay cell");
    }

    // Return null on timeout
    Cell receiveControlCellResponse() {
        try {
            final long timeout = getReceiveTimeout();
            return controlCellResponseQueue.poll(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }


    private long getReceiveTimeout() {
        if (circuit.getStatus().isBuilding())
            return remainingBuildTime();
        else
            return CIRCUIT_RELAY_RESPONSE_TIMEOUT;
    }

    private long remainingBuildTime() {
        final long elapsed = circuit.getStatus().getMillisecondsElapsedSinceCreated();
        if (elapsed == 0 || elapsed >= CIRCUIT_BUILD_TIMEOUT_MS)
            return 0;
        return CIRCUIT_BUILD_TIMEOUT_MS - elapsed;
    }

    /*
     * This is called by the cell reading thread in ConnectionImpl to deliver control cells
     * associated with this circuit (CREATED, CREATED_FAST, or DESTROY).
     */
    void deliverControlCell(Cell cell) {
        if (cell.getCommand() == Cell.DESTROY) {
            processDestroyCell(cell.getByte());
        } else {
            controlCellResponseQueue.add(cell);
        }
    }

    private void processDestroyCell(int reason) {
        logger.fine("DESTROY cell received (" + CellImpl.errorToDescription(reason) + ") on " + circuit);
        destroyCircuit();
    }

    /* This is called by the cell reading thread in ConnectionImpl to deliver RELAY cells. */
    void deliverRelayCell(Cell cell) {
        circuit.getStatus().updateDirtyTimestamp();
        final RelayCell relayCell = decryptRelayCell(cell);
        logRelayCell("Dispatching: ", relayCell);
        switch (relayCell.getRelayCommand()) {
            case RelayCell.RELAY_EXTENDED:
            case RelayCell.RELAY_EXTENDED2:
            case RelayCell.RELAY_RESOLVED:
            case RelayCell.RELAY_TRUNCATED:
            case RelayCell.RELAY_COMMAND_RENDEZVOUS_ESTABLISHED:
            case RelayCell.RELAY_COMMAND_INTRODUCE_ACK:
            case RelayCell.RELAY_COMMAND_RENDEZVOUS2:
                relayCellResponseQueue.add(relayCell);
                break;
            case RelayCell.RELAY_DATA:
            case RelayCell.RELAY_END:
            case RelayCell.RELAY_CONNECTED:
                processRelayDataCell(relayCell);
                break;

            case RelayCell.RELAY_SENDME:
                if (relayCell.getStreamId() != 0)
                    processRelayDataCell(relayCell);
                else
                    processCircuitSendme(relayCell);
                break;
            case RelayCell.RELAY_BEGIN:
            case RelayCell.RELAY_BEGIN_DIR:
            case RelayCell.RELAY_EXTEND:
            case RelayCell.RELAY_RESOLVE:
            case RelayCell.RELAY_TRUNCATE:
                destroyCircuit();
                throw new TorException("Unexpected 'forward' direction relay cell type: " + relayCell.getRelayCommand());
        }
    }

    /* Runs in the context of the connection cell reading thread */
    private void processRelayDataCell(RelayCell cell) {
        if (cell.getRelayCommand() == RelayCell.RELAY_DATA) {
            cell.getCircuitNode().decrementDeliverWindow();
            if (cell.getCircuitNode().considerSendingSendme()) {
                final RelayCell sendme = createRelayCell(RelayCell.RELAY_SENDME, 0, cell.getCircuitNode());
                sendRelayCellTo(sendme, sendme.getCircuitNode());
            }
        }

        synchronized (streamMap) {
            final StreamImpl stream = streamMap.get(cell.getStreamId());
            // It's not unusual for the stream to not be found.  For example, if a RELAY_CONNECTED arrives after
            // the client has stopped waiting for it, the stream will never be tracked and eventually the edge node
            // will send a RELAY_END for this stream.
            if (stream != null) {
                stream.addInputCell(cell);
            }
        }
    }

    RelayCell createRelayCell(int relayCommand, int streamId, CircuitNode targetNode) {
        return new RelayCellImpl(targetNode, circuitId, streamId, relayCommand);
    }

    void sendRelayCellTo(RelayCell cell, CircuitNode targetNode) {
        synchronized (relaySendLock) {
            logRelayCell("Sending:     ", cell);
            cell.setLength();
            targetNode.updateForwardDigest(cell);
            cell.setDigest(targetNode.getForwardDigestBytes());

            for (CircuitNode node = targetNode; node != null; node = node.getPreviousNode())
                node.encryptForwardCell(cell);

            if (cell.getRelayCommand() == RelayCell.RELAY_DATA)
                targetNode.waitForSendWindowAndDecrement();

            sendCell(cell);
        }
    }


    private void logRelayCell(String message, RelayCell cell) {
        final Level level = getLogLevelForCell(cell);
        if (!logger.isLoggable(level)) {
            return;
        }
        logger.log(level, message + cell);
    }

    private Level getLogLevelForCell(RelayCell cell) {
        switch (cell.getRelayCommand()) {
            case RelayCell.RELAY_DATA:
            case RelayCell.RELAY_SENDME:
                return Level.FINEST;
            default:
                return Level.FINER;
        }
    }

    void sendCell(Cell cell) {
        final CircuitStatus status = circuit.getStatus();
        if (!(status.isConnected() || status.isBuilding()))
            return;
        try {
            status.updateDirtyTimestamp();
            connection.sendCell(cell);
        } catch (ConnectionIOException e) {
            destroyCircuit();
        }
    }

    void markForClose() {
        synchronized (streamMap) {
            if (isMarkedForClose) {
                return;
            }
            isMarkedForClose = true;
            if (streamMap.isEmpty()) {
                closeCircuit();
            }
        }
    }

    boolean isMarkedForClose() {
        return isMarkedForClose;
    }

    private void closeCircuit() {
        logger.fine("Closing circuit " + circuit);
        sendDestroyCell(Cell.ERROR_NONE);
        connection.removeCircuit(circuit);
        circuit.setStateDestroyed();
        isClosed = true;
    }

    void sendDestroyCell(int reason) {
        Cell destroy = CellImpl.createCell(circuitId, Cell.DESTROY);
        destroy.putByte(reason);
        try {
            connection.sendCell(destroy);
        } catch (ConnectionIOException e) {
            logger.warning("Connection IO error sending DESTROY cell: " + e.getMessage());
        }
    }

    private void processCircuitSendme(RelayCell cell) {
        cell.getCircuitNode().incrementSendWindow();
    }

    void destroyCircuit() {
        synchronized (streamMap) {
            if (isClosed) {
                return;
            }
            circuit.setStateDestroyed();
            connection.removeCircuit(circuit);
            final List<StreamImpl> tmpList = new ArrayList<StreamImpl>(streamMap.values());
            for (StreamImpl s : tmpList) {
                s.close();
            }
            isClosed = true;
        }
    }

    StreamImpl createNewStream(boolean autoclose) {
        synchronized (streamMap) {
            final int streamId = circuit.getStatus().nextStreamId();
            final StreamImpl stream = new StreamImpl(circuit, circuit.getFinalCircuitNode(), streamId, autoclose);
            streamMap.put(streamId, stream);
            return stream;
        }
    }

    void removeStream(StreamImpl stream) {
        synchronized (streamMap) {
            streamMap.remove(stream.getStreamId());
            if (streamMap.isEmpty() && isMarkedForClose) {
                closeCircuit();
            }
        }
    }

    List<Stream> getActiveStreams() {
        synchronized (streamMap) {
            return new ArrayList<Stream>(streamMap.values());
        }
    }

    public void dashboardRender(DashboardRenderer renderer, PrintWriter writer, int flags) throws IOException {
        if ((flags & DASHBOARD_STREAMS) == 0) {
            return;
        }
        for (Stream s : getActiveStreams()) {
            renderer.renderComponent(writer, flags, s);
        }
    }
}
