/*  
 * Copyright 2012 Sebastian Annies, Hamburg
 *
 * Licensed under the Apache License, Version 2.0 (the License); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0 
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an AS IS BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 */

package com.googlecode.mp4parser;

import com.coremedia.iso.BoxParser;
import com.coremedia.iso.Hex;
import com.coremedia.iso.IsoFile;
import com.coremedia.iso.IsoTypeWriter;
import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.Container;
import com.coremedia.iso.boxes.UserBox;
import com.googlecode.mp4parser.annotations.DoNotParseDetail;
import com.googlecode.mp4parser.util.Logger;
import com.googlecode.mp4parser.util.Path;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

import static com.googlecode.mp4parser.util.CastUtils.l2i;

/**
 * A basic on-demand parsing box. Requires the implementation of three methods to become a fully working box:
 * <ol>
 * <li>{@link #_parseDetails(java.nio.ByteBuffer)}</li>
 * <li>{@link #getContent(java.nio.ByteBuffer)}</li>
 * <li>{@link #getContentSize()}</li>
 * </ol>
 * additionally this new box has to be put into the <code>isoparser-default.properties</code> file so that
 * it is accessible by the <code>PropertyBoxParserImpl</code>
 */
public abstract class AbstractBox implements Box {
    private static Logger LOG = Logger.getLogger(AbstractBox.class);

    protected String type;
    private byte[] userType;
    private Container parent;
    boolean isParsed;
    boolean isRead;


    private ByteBuffer content;


    long contentStartPosition;
    long offset;
    long memMapSize = -1;
    DataSource dataSource;

    private synchronized void readContent() {
        if (!isRead) {
            try {
                LOG.logDebug("mem mapping " + this.getType());
                content = dataSource.map(contentStartPosition, memMapSize);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            isRead = true;
        }
    }

    private ByteBuffer deadBytes = null;

    public long getOffset() {
        return offset;
    }

    protected AbstractBox(String type) {
        this.type = type;
        isRead = true;
        isParsed = true;
    }

    protected AbstractBox(String type, byte[] userType) {
        this.type = type;
        this.userType = userType;
        isRead = true;
        isParsed = true;
    }

    /**
     * Get the box's content size without its header. This must be the exact number of bytes
     * that <code>getContent(ByteBuffer)</code> writes.
     *
     * @return Gets the box's content size in bytes
     * @see #getContent(java.nio.ByteBuffer)
     */
    protected abstract long getContentSize();

    /**
     * Write the box's content into the given <code>ByteBuffer</code>. This must include flags
     * and version in case of a full box. <code>byteBuffer</code> has been initialized with
     * <code>getSize()</code> bytes.
     *
     * @param byteBuffer the sink for the box's content
     */
    protected abstract void getContent(ByteBuffer byteBuffer);

    /**
     * Parse the box's fields and child boxes if any.
     *
     * @param content the box's raw content beginning after the 4-cc field.
     */
    protected abstract void _parseDetails(ByteBuffer content);

    /**
     * {@inheritDoc}
     */
    @DoNotParseDetail
    public void parse(DataSource dataSource, ByteBuffer header, long contentSize, BoxParser boxParser) throws IOException {

        this.contentStartPosition = dataSource.position();
        this.offset = contentStartPosition - header.remaining();
        this.memMapSize = contentSize;
        this.dataSource = dataSource;

        dataSource.position(dataSource.position() + contentSize);
        isRead = false;
        isParsed = false;

    }

    public void getBox(WritableByteChannel os) throws IOException {
        if (isRead) {
            if (isParsed) {
                ByteBuffer bb = ByteBuffer.allocate(l2i(getSize()));
                getHeader(bb);
                getContent(bb);
                if (deadBytes != null) {
                    deadBytes.rewind();
                    while (deadBytes.remaining() > 0) {
                        bb.put(deadBytes);
                    }
                }
                os.write((ByteBuffer) bb.rewind());
            } else {
                ByteBuffer header = ByteBuffer.allocate((isSmallBox() ? 8 : 16) + (UserBox.TYPE.equals(getType()) ? 16 : 0));
                getHeader(header);
                os.write((ByteBuffer) header.rewind());
                os.write((ByteBuffer) content.position(0));
            }

        } else {
            ByteBuffer header = ByteBuffer.allocate((isSmallBox() ? 8 : 16) + (UserBox.TYPE.equals(getType()) ? 16 : 0));
            getHeader(header);
            os.write((ByteBuffer) header.rewind());
            dataSource.transferTo(contentStartPosition, memMapSize, os);
        }
    }


    /**
     * Parses the raw content of the box. It surrounds the actual parsing
     * which is done
     */
    public synchronized final void parseDetails() {
        readContent();
        LOG.logDebug("parsing details of " + this.getType());
        if (content != null) {
            ByteBuffer content = this.content;
            isParsed = true;
            content.rewind();
            _parseDetails(content);
            if (content.remaining() > 0) {
                deadBytes = content.slice();
            }
            this.content = null;
            assert verify(content);
        }
    }

    /**
     * Sets the 'dead' bytes. These bytes are left if the content of the box
     * has been parsed but not all bytes have been used up.
     *
     * @param newDeadBytes the unused bytes with no meaning but required for bytewise reconstruction
     */
    protected void setDeadBytes(ByteBuffer newDeadBytes) {
        deadBytes = newDeadBytes;
    }


    /**
     * Gets the full size of the box including header and content.
     *
     * @return the box's size
     */
    public long getSize() {
        long size = (isRead ? (isParsed ? getContentSize() : (content != null ? content.limit() : 0)) : memMapSize);
        size += (8 + // size|type
                (size >= ((1L << 32) - 8) ? 8 : 0) + // 32bit - 8 byte size and type
                (UserBox.TYPE.equals(getType()) ? 16 : 0));
        size += (deadBytes == null ? 0 : deadBytes.limit());
        return size;
    }

    @DoNotParseDetail
    public String getType() {
        return type;
    }

    @DoNotParseDetail
    public byte[] getUserType() {
        return userType;
    }

    @DoNotParseDetail
    public Container getParent() {
        return parent;
    }

    @DoNotParseDetail
    public void setParent(Container parent) {
        this.parent = parent;
    }

    /**
     * Check if details are parsed.
     *
     * @return <code>true</code> whenever the content <code>ByteBuffer</code> is not <code>null</code>
     */
    public boolean isParsed() {
        return isParsed;
    }


    /**
     * Verifies that a box can be reconstructed byte-exact after parsing.
     *
     * @param content the raw content of the box
     * @return <code>true</code> if raw content exactly matches the reconstructed content
     */
    private boolean verify(ByteBuffer content) {
        ByteBuffer bb = ByteBuffer.allocate(l2i(getContentSize() + (deadBytes != null ? deadBytes.limit() : 0)));
        getContent(bb);
        if (deadBytes != null) {
            deadBytes.rewind();
            while (deadBytes.remaining() > 0) {
                bb.put(deadBytes);
            }
        }
        content.rewind();
        bb.rewind();


        if (content.remaining() != bb.remaining()) {
            System.err.print(this.getType() + ": remaining differs " + content.remaining() + " vs. " + bb.remaining());
            LOG.logError(this.getType() + ": remaining differs " + content.remaining() + " vs. " + bb.remaining());
            return false;
        }
        int p = content.position();
        for (int i = content.limit() - 1, j = bb.limit() - 1; i >= p; i--, j--) {
            byte v1 = content.get(i);
            byte v2 = bb.get(j);
            if (v1 != v2) {
                LOG.logError(String.format("%s: buffers differ at %d: %2X/%2X", this.getType(), i, v1, v2));
                byte[] b1 = new byte[content.remaining()];
                byte[] b2 = new byte[bb.remaining()];
                content.get(b1);
                bb.get(b2);
                System.err.println("original      : " + Hex.encodeHex(b1, 4));
                System.err.println("reconstructed : " + Hex.encodeHex(b2, 4));
                return false;
            }
        }
        return true;

    }

    private boolean isSmallBox() {
        int baseSize = 8;
        if (UserBox.TYPE.equals(getType())) {
            baseSize += 16;
        }
        if (isRead) {
            if (isParsed) {
                return (getContentSize() + (deadBytes != null ? deadBytes.limit() : 0) + baseSize) < (1L << 32);
            } else {
                return content.limit() + baseSize < (1L << 32);
            }
        } else {
            return memMapSize + baseSize < (1L << 32);
        }
    }

    private void getHeader(ByteBuffer byteBuffer) {
        if (isSmallBox()) {
            IsoTypeWriter.writeUInt32(byteBuffer, this.getSize());
            byteBuffer.put(IsoFile.fourCCtoBytes(getType()));
        } else {
            IsoTypeWriter.writeUInt32(byteBuffer, 1);
            byteBuffer.put(IsoFile.fourCCtoBytes(getType()));
            IsoTypeWriter.writeUInt64(byteBuffer, getSize());
        }
        if (UserBox.TYPE.equals(getType())) {
            byteBuffer.put(getUserType());
        }
    }

    @DoNotParseDetail
    public String getPath() {
        return Path.createPath(this);
    }
}
