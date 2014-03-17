package cz.vity.freerapid.plugins.services.rtmp;

import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpDownloadClient;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.HttpMethod;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.util.logging.Logger;
import java.util.zip.InflaterInputStream;

/**
 * Helper for managing SWF verification.
 * The design is to have a static instance of this class in the runner,
 * which makes it possible to reuse the SWF hash without having to
 * download the SWF and calculate the hash every time
 * a new download is started.
 *
 * @author ntoskrnl
 * @see #setSwfVerification(RtmpSession, HttpDownloadClient)
 */
public class SwfVerificationHelper {
    private final static Logger logger = Logger.getLogger(SwfVerificationHelper.class.getName());

    private final static int REFRESH_INTERVAL = 2 * 60 * 60 * 1000; //2 hours ought to be enough for anyone
    private long lastRefreshTime = 0;

    private byte[] hash;
    private int size;

    private final String swfURL;

    /**
     * Creates a new instance.
     *
     * @param swfURL URL to the SWF file which should be used for verification
     */
    public SwfVerificationHelper(final String swfURL) {
        this.swfURL = swfURL;
    }

    /**
     * @return SWF URL, which was set in the constructor
     */
    public String getSwfURL() {
        return swfURL;
    }

    /**
     * Sets the SWF verification parameters in the specified RtmpSession.
     * <p/>
     * If {@link #REFRESH_INTERVAL} time has passed since the last download of the SWF,
     * it is redownloaded and hashed. Otherwise the cached parameters are set.
     * <p/>
     * This method is synchronized to make sure the SWF is only downloaded once.
     *
     * @param session RtmpSession where the SWF verification parameters are to be set
     * @param client  DownloadClient to use for fetching the SWF
     * @throws Exception if something goes wrong
     */
    public synchronized void setSwfVerification(final RtmpSession session, final HttpDownloadClient client) throws Exception {
        if (System.currentTimeMillis() >= lastRefreshTime + REFRESH_INTERVAL) {
            logger.info("Refreshing SWF hash");
            HttpMethod method = client.getGetMethod(swfURL);
            method.setFollowRedirects(true);
            InputStream is = client.makeRequestForFile(method);
            if (is == null) {
                throw new ServiceConnectionProblemException("Error downloading SWF");
            }
            is = new BufferedInputStream(is, 2048);
            hash = new byte[32];
            size = getSwfHash(is, hash);
            lastRefreshTime = System.currentTimeMillis();
        }
        session.setSwfHash(hash);
        session.setSwfSize(size);
        logger.info("SWF hash: " + Utils.toHex(hash, false));
        logger.info("SWF size: " + String.valueOf(size));
    }

    /**
     * Calculates the SWF verification hash of the specified file.
     * The file can be compressed or uncompressed.
     *
     * @param file   swf file to calculate the hash of
     * @param output swf hash goes here, length should be 32
     * @return swf size
     * @throws Exception if something goes wrong
     */
    public static int getSwfHash(File file, byte[] output) throws Exception {
        final InputStream is = new BufferedInputStream(new FileInputStream(file), 2048);
        return getSwfHash(is, output);
    }

    /**
     * Calculates the SWF verification hash of the SWF in the specified stream.
     * The stream can be compressed or uncompressed.
     *
     * @param is     stream to the swf file to calculate the hash of. stream is closed when returning.
     * @param output swf hash goes here, length should be 32
     * @return swf size
     * @throws Exception if something goes wrong
     */
    public static int getSwfHash(InputStream is, byte[] output) throws Exception {
        if (is == null) throw new NullPointerException();
        try {
            if (output.length < 32) {
                throw new IllegalArgumentException("Output length must be 32");
            }
            final Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(Handshake.CLIENT_CONST, "HmacSHA256"));
            final byte[] b = new byte[2048];
            int length = 0;
            if (readBytes(is, b, 8) != 8) {
                throw new IOException("Error reading from stream");
            }
            if (b[0] == 'C' && b[1] == 'W' && b[2] == 'S') {
                //compressed
                b[0] = (byte) 'F';
                is = new InflaterInputStream(is);
            } else if (b[0] != 'F' || b[1] != 'W' || b[2] != 'S') {
                throw new IOException("Invalid SWF stream");
            }
            length += 8;
            mac.update(b, 0, 8);
            for (int i; (i = is.read(b)) > -1; ) {
                length += i;
                mac.update(b, 0, i);
            }
            System.arraycopy(mac.doFinal(), 0, output, 0, 32);
            return length;
        } finally {
            try {
                is.close();
            } catch (Exception e) {
                LogUtils.processException(logger, e);
            }
        }
    }

    /**
     * Reads the specified number of bytes from the stream.
     * Should always return count, except when EOF is reached.
     *
     * @param is     stream to read from
     * @param buffer buffer to put the read bytes into
     * @param count  number of bytes to read
     * @return number of bytes actually read
     * @throws IOException               if something goes wrong IO-wise
     * @throws IndexOutOfBoundsException if count is something illegal
     */
    private static int readBytes(InputStream is, byte[] buffer, int count) throws IOException {
        int read = 0, i;
        while (count > 0 && (i = is.read(buffer, 0, count)) > -1) {
            count -= i;
            read += i;
        }
        return read;
    }

}
