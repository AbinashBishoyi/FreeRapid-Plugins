package cz.vity.freerapid.plugins.container;

import org.apache.commons.codec.binary.Hex;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author ntoskrnl
 */
public final class ContainerUtils {

    /**
     * Do not instantiate.
     */
    private ContainerUtils() {
    }

    /**
     * Use only in initializers!
     * {@link org.apache.commons.codec.DecoderException DecoderExceptions}
     * are wrapped in {@link RuntimeException RuntimeExceptions}.
     *
     * @param hex hex string
     * @return decoded bytes
     */
    public static byte[] hexToBytes(final String hex) {
        try {
            return Hex.decodeHex(hex.toCharArray());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Reads the specified stream completely into memory and creates a String.
     * Use sparingly! Handling streams directly is preferred to using this method.
     *
     * @param is Stream to read
     * @return String constructed from the read bytes
     * @throws IOException If something goes wrong while reading
     */
    public static String readToString(final InputStream is) throws IOException {
        final StringBuilder sb = new StringBuilder();
        final byte[] b = new byte[1024];
        int i;
        while ((i = is.read(b)) > -1) {
            sb.append(new String(b, 0, i, "UTF-8"));
        }
        return sb.toString();
    }

}
