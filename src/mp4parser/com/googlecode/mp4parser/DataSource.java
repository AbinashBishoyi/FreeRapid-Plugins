package com.googlecode.mp4parser;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

public interface DataSource extends Closeable {
    /**
     * Reads a sequence of bytes from this channel into the given buffer.
     * <p/>
     * <p> Bytes are read starting at this channel's current position, and
     * then the file position is updated with the number of bytes actually
     * read.  </p>
     */
    int read(ByteBuffer byteBuffer) throws IOException;


    /**
     * Returns the current size of this DataSource.  </p>
     *
     * @return The current size of this DataSource,
     * measured in bytes
     * @throws java.io.IOException If some I/O error occurs
     */
    long size() throws IOException;

    /**
     * Returns the DataSource's current position.
     *
     * @return This DataSource's file position,
     * a non-negative integer counting the number of bytes
     * from the beginning of the data to the current position
     * @throws java.io.IOException
     */
    long position() throws IOException;

    /**
     * Sets the DataSource's position.
     *
     * @param nuPos The new position, a non-negative integer counting
     *              the number of bytes from the beginning of the data
     * @throws java.io.IOException If some I/O error occurs
     */
    void position(long nuPos) throws IOException;

    /**
     * Transfers bytes from this DataSource to the given writable byte
     * channel.
     * <p/>
     * <p> An attempt should be made to read up to <tt>count</tt> bytes starting at
     * the given <tt>position</tt> in this DataSource and write them to the
     * target channel.  An invocation of this method may or may not transfer
     * all of the requested bytes;
     *
     * @param position The position within the DataSource at which the transfer is to begin;
     *                 must be non-negative
     * @param count    The maximum number of bytes to be transferred; must be
     *                 non-negative
     * @param target   The target channel
     * @return the actual number of bytes written
     * @throws java.io.IOException
     */
    long transferTo(long position, long count, WritableByteChannel target) throws IOException;

    /**
     * @param startPosition
     * @param size
     * @return
     * @throws java.io.IOException
     */
    ByteBuffer map(long startPosition, long size) throws IOException;

    void close() throws IOException;
}
