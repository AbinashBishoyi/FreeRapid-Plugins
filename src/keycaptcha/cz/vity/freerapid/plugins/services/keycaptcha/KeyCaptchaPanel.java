package cz.vity.freerapid.plugins.services.keycaptcha;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author ntoskrnl
 */
class KeyCaptchaPanel extends JPanel {

    private final KeyCaptchaComponent keyCaptchaComponent;

    public KeyCaptchaPanel(final KeyCaptchaImages images) {
        final JLabel labelMessage = new JLabel("Please assemble the image as you see in the upper right corner", JLabel.CENTER);
        keyCaptchaComponent = new KeyCaptchaComponent(images);

        final GroupLayout layout = new GroupLayout(this);
        setLayout(layout);

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                        .addComponent(labelMessage)
                        .addComponent(keyCaptchaComponent)
        );
        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(labelMessage)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(keyCaptchaComponent)
        );
    }

    public List<Point> getPieceLocations() {
        return keyCaptchaComponent.getPieceLocations();
    }

    public List<Point> getMouseLocations() {
        return keyCaptchaComponent.getMouseLocations();
    }

    private static class KeyCaptchaComponent extends JComponent implements MouseListener, MouseMotionListener {

        private final KeyCaptchaImages images;
        private final List<Point> pieceLocations;

        private final LinkedList<Point> mouseLocations = new LinkedList<Point>();
        private long lastMouseLogTime;

        private Point dragLocation = null;
        private int pieceBeingDragged = -1;

        public KeyCaptchaComponent(final KeyCaptchaImages images) {
            this.images = images;
            this.pieceLocations = new ArrayList<Point>(images.getPieces().size());
            for (int i = 0; i < images.getPieces().size(); i++) {
                final int coordinate = (i + 1) * 4;
                pieceLocations.add(new Point(coordinate, coordinate));
            }
            setPreferredSize(new Dimension(images.getBackground().getWidth(), images.getBackground().getHeight()));
            setMinimumSize(getPreferredSize());
            setMaximumSize(getPreferredSize());
            addMouseListener(this);
            addMouseMotionListener(this);
        }

        public List<Point> getPieceLocations() {
            return pieceLocations;
        }

        public List<Point> getMouseLocations() {
            return mouseLocations;
        }

        @Override
        protected void paintComponent(final Graphics graphics) {
            super.paintComponent(graphics);
            final Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Color.LIGHT_GRAY);
            drawBackground(g);
            drawSample(g);
            drawPieces(g);
            g.dispose();
        }

        private void drawBackground(final Graphics2D g) {
            g.drawImage(images.getBackground(), 0, 0, null);
            g.drawRect(0, 0, images.getBackground().getWidth() - 1, images.getBackground().getHeight() - 1);
        }

        private void drawSample(final Graphics2D g) {
            final int edge = 2;
            final int x = images.getBackground().getWidth() - images.getSample().getWidth() - edge;
            g.drawImage(images.getSample(), x, edge, null);
            g.drawRect(x - edge, 0, images.getSample().getWidth() - 1 + edge + edge, images.getSample().getHeight() - 1 + edge + edge);
        }

        private void drawPieces(final Graphics2D g) {
            for (int i = 0; i < pieceLocations.size(); i++) {
                final Point location = pieceLocations.get(i);
                g.drawImage(images.getPieces().get(i), location.x, location.y, null);
            }
            drawHighlight(g);
        }

        private void drawHighlight(final Graphics2D g) {
            if (pieceBeingDragged != -1) {
                final BufferedImage piece = images.getPieces().get(pieceBeingDragged);
                final Point location = pieceLocations.get(pieceBeingDragged);
                g.drawImage(piece, location.x, location.y, null);
                if (dragLocation != null) {
                    g.drawRect(location.x, location.y, piece.getWidth() - 1, piece.getHeight() - 1);
                }
            }
        }

        @Override
        public void mousePressed(final MouseEvent e) {
            final Point eventLocation = e.getPoint();
            for (int i = pieceLocations.size() - 1; i >= 0; i--) {
                final BufferedImage image = images.getPieces().get(i);
                final Point pieceLocation = pieceLocations.get(i);
                if (new Rectangle(pieceLocation.x, pieceLocation.y, image.getWidth(), image.getHeight())
                        .contains(eventLocation)) {
                    pieceBeingDragged = i;
                    dragLocation = new Point(eventLocation.x - pieceLocation.x, eventLocation.y - pieceLocation.y);
                    repaint();
                    break;
                }
            }
        }

        @Override
        public void mouseReleased(final MouseEvent e) {
            if (dragLocation != null) {
                dragLocation = null;
                repaint();
            }
        }

        @Override
        public void mouseDragged(final MouseEvent e) {
            logMouseLocation(e.getPoint());
            if (dragLocation != null) {
                final Point eventLocation = e.getPoint();
                final int x = eventLocation.x - dragLocation.x;
                final int y = eventLocation.y - dragLocation.y;
                pieceLocations.set(pieceBeingDragged, createPoint(x, y));
                repaint();
            }
        }

        private Point createPoint(int x, int y) {
            final BufferedImage piece = images.getPieces().get(pieceBeingDragged);
            x = Math.min(x, images.getBackground().getWidth() - piece.getWidth());
            y = Math.min(y, images.getBackground().getHeight() - piece.getHeight());
            x = Math.max(x, 0);
            y = Math.max(y, 0);
            return new Point(x, y);
        }

        @Override
        public void mouseMoved(final MouseEvent e) {
            logMouseLocation(e.getPoint());
        }

        private void logMouseLocation(final Point p) {
            if (System.currentTimeMillis() > lastMouseLogTime + 1000) {
                lastMouseLogTime = System.currentTimeMillis();
                p.translate(465, 264);
                if (mouseLocations.isEmpty() || !mouseLocations.getLast().equals(p)) {
                    mouseLocations.add(p);
                    if (mouseLocations.size() > 40) {
                        mouseLocations.removeFirst();
                    }
                }
            }
        }

        @Override
        public void mouseClicked(final MouseEvent e) {
        }

        @Override
        public void mouseEntered(final MouseEvent e) {
        }

        @Override
        public void mouseExited(final MouseEvent e) {
        }

    }

}
