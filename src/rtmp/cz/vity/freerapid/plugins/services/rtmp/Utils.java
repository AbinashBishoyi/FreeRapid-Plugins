package cz.vity.freerapid.plugins.services.rtmp;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.mina.core.buffer.IoBuffer;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

/**
 * @author Peter Thomas
 * @author ntoskrnl
 */
class Utils {

    public static int readInt24(IoBuffer in) {
        int val = 0;
        val += (in.get() & 0xFF) * 256 * 256;
        val += (in.get() & 0xFF) * 256;
        val += (in.get() & 0xFF);
        return val;
    }

    @SuppressWarnings("PointlessBitwiseExpression")
    public static void writeInt24(IoBuffer out, int value) {
        out.put((byte) (0xFF & (value >> 16)));
        out.put((byte) (0xFF & (value >> 8)));
        out.put((byte) (0xFF & (value >> 0)));
    }

    @SuppressWarnings("PointlessBitwiseExpression")
    public static int readInt32Reverse(IoBuffer in) {
        int val = 0;
        val += (in.get() & 0xFF) << 0;
        val += (in.get() & 0xFF) << 8;
        val += (in.get() & 0xFF) << 16;
        val += (in.get() & 0xFF) << 24;
        return val;
    }

    public static void writeInt32Reverse(IoBuffer out, int value) {
        out.put((byte) (0xFF & value));
        out.put((byte) (0xFF & (value >> 8)));
        out.put((byte) (0xFF & (value >> 16)));
        out.put((byte) (0xFF & (value >> 24)));
    }

    private static final char[] HEX_DIGITS = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'A', 'B', 'C', 'D', 'E', 'F'};

    private static final char BYTE_SEPARATOR = ' ';

    public static String toHex(byte[] ba) {
        return toHex(ba, true);
    }

    public static String toHex(byte[] ba, boolean withSeparator) {
        return toHex(ba, 0, ba.length, withSeparator);
    }

    public static String toHex(byte[] ba, int offset, int length, boolean withSeparator) {
        char[] buf;
        if (withSeparator) {
            buf = new char[length * 3];
        } else {
            buf = new char[length * 2];
        }
        char[] chars;
        for (int i = offset, j = 0; i < offset + length; ) {
            chars = toHexChars(ba[i++]);
            buf[j++] = chars[0];
            buf[j++] = chars[1];
            if (withSeparator) {
                buf[j++] = BYTE_SEPARATOR;
            }
        }
        return new String(buf);
    }

    private static char[] toHexChars(int b) {
        char left = HEX_DIGITS[(b >>> 4) & 0x0F];
        char right = HEX_DIGITS[b & 0x0F];
        return new char[]{left, right};
    }

    public static String toHex(byte b) {
        char[] chars = toHexChars(b);
        return chars[0] + "" + chars[1];
    }

    public static byte[] fromHex(char[] hex) {
        int length = hex.length / 2;
        byte[] raw = new byte[length];
        for (int i = 0; i < length; i++) {
            int high = Character.digit(hex[i * 2], 16);
            int low = Character.digit(hex[i * 2 + 1], 16);
            int value = (high << 4) | low;
            if (value > 127) {
                value -= 256;
            }
            raw[i] = (byte) value;
        }
        return raw;
    }

    public static byte[] fromHex(String s) {
        String temp = s.replace(" ", "");
        return fromHex(temp.toCharArray());
    }

    public static CharSequence readAsString(String fileName) {
        return readAsString(new File(fileName));
    }

    public static CharSequence readAsString(File file) {
        StringBuilder sb = new StringBuilder();
        try {
            FileInputStream fis = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            String s;
            while ((s = reader.readLine()) != null) {
                sb.append(s);
            }
            return sb;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] readAsByteArray(String fileName) {
        File file = new File(fileName);
        return readAsByteArray(file, file.length());
    }

    public static byte[] readAsByteArray(String fileName, int length) {
        return readAsByteArray(new File(fileName), length);
    }

    public static byte[] readAsByteArray(File file) {
        return readAsByteArray(file, file.length());
    }

    public static byte[] readAsByteArray(File file, long length) {
        try {
            byte[] bytes = new byte[(int) length];
            int offset = 0;
            int numRead = 0;
            FileInputStream is = new FileInputStream(file);
            while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
                offset += numRead;
            }
            is.close();
            return bytes;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /*
    public static void convert(String inFileName, String outFileName) {
        FlvWriter flvWriter = new FlvWriter(0, outFileName);
        DecoderOutput decoderOutput = new DecoderOutput() {
            public void write(Object packet) {
            }

            public void disconnect() {
            }
        };
        RtmpSession session = new RtmpSession();
        session.setOutputWriter(flvWriter);
        session.setDecoderOutput(decoderOutput);
        session.setInvokeResultHandler(new DefaultInvokeResultHandler());
        byte[] bytes = readAsByteArray(inFileName);
        IoBuffer buf = IoBuffer.wrap(bytes);
        int prevPosition = -1;
        while (prevPosition < buf.position()) {
            prevPosition = buf.position();
            RtmpDecoder.decode(buf, session);
        }
        flvWriter.close();
    }
    */

    public static String getOverHttp(String url) {
        HttpClient client = new HttpClient();
        String response = null;
        HttpMethod get = new GetMethod(url);
        try {
            client.executeMethod(get);
            response = get.getResponseBodyAsString();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            get.releaseConnection();
        }
        return response;
    }

    public static byte[] sha256(byte[] message, byte[] key) {
        Mac mac;
        try {
            mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return mac.doFinal(message);
    }

    public static Packet hexToPacket(String hex) {
        byte[] bytes = fromHex(hex);
        IoBuffer buf = IoBuffer.wrap(bytes);
        RtmpSession session = new RtmpSession();
        session.setInChunkSize(buf.limit());
        Packet packet = new Packet();
        packet.decode(buf, session);
        return packet;
    }

}