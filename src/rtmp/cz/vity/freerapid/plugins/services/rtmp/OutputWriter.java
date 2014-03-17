package cz.vity.freerapid.plugins.services.rtmp;

import org.apache.mina.core.buffer.IoBuffer;

import java.io.InputStream;

/**
 * @author Peter Thomas
 * @author ntoskrnl
 */
public interface OutputWriter {

    public void close();

    public void write(Packet packet);

    public void writeFlvData(IoBuffer data);

    public InputStream getStream();

    public WriterStatus getStatus();

}