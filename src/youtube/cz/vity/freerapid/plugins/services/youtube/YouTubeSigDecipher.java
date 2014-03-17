package cz.vity.freerapid.plugins.services.youtube;

import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.webclient.DownloadClient;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpDownloadClient;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.InflaterInputStream;

/**
 * @author tong2shot
 * @author ntoskrnl
 * @author JPEXS (http://www.free-decompiler.com)
 */
class YouTubeSigDecipher {

    private static final Logger logger = Logger.getLogger(YouTubeSigDecipher.class.getName());

    private static final int MINORwithDECIMAL = 17;
    private static final int KIND_NAMESPACE = 8;
    private static final int KIND_PRIVATE = 5;
    private static final int KIND_PACKAGE = 22;
    private static final int KIND_PACKAGE_INTERNAL = 23;
    private static final int KIND_PROTECTED = 24;
    private static final int KIND_EXPLICIT = 25;
    private static final int KIND_STATIC_PROTECTED = 26;
    private static final int nameSpaceKinds[] = new int[]{KIND_NAMESPACE, KIND_PRIVATE, KIND_PACKAGE, KIND_PACKAGE_INTERNAL, KIND_PROTECTED, KIND_EXPLICIT, KIND_STATIC_PROTECTED};
    private static final Pattern REVERSE_PATTERN = Pattern.compile("^\\Q\u005d\\E..\\Q\u00d2\u0046\\E..\\Q\u0001\u0080\u0014\\E$", Pattern.DOTALL);
    private static final Pattern CLONE_SWAP_PATTERN = Pattern.compile("^\\Q\u005d\\E(..)\\Q\u00d2\u0024\\E(.)\\Q\u0046\\E..\\Q\u0002\u0080\u0014\\E$", Pattern.DOTALL);

    private final HttpDownloadClient client;
    private Map<Integer, String> multiname_map = new HashMap<Integer, String>();

    public YouTubeSigDecipher(final HttpDownloadClient client) {
        this.client = new DownloadClient();
        this.client.initClient(client.getSettings());
    }

    private List<String> clone(List<String> lst, int from) {
        return lst.subList(from, lst.size());
    }

    private List<String> reverse(List<String> lst) {
        Collections.reverse(lst);
        return lst;
    }

    private List<String> swap(List<String> lstSig, int pos) {
        String head = lstSig.get(0);
        String headSwapTo = lstSig.get(pos % lstSig.size());
        lstSig.set(0, headSwapTo);
        lstSig.set(pos, head);
        return lstSig;
    }

    public String decipher(final HttpMethod method, final String sig) throws Exception {
        final String bytecode = getBytecode(method);
        List<String> lstSig = new ArrayList<String>(Arrays.asList(sig.split("")));
        lstSig.remove(0); //remove empty char at head
        for (final String callBytecode : bytecode.split("\\u00d6")) {
            Matcher matcher = REVERSE_PATTERN.matcher(callBytecode);
            if (matcher.find()) {
                logger.info("reverse");
                lstSig = reverse(lstSig);
                continue;
            }
            matcher = CLONE_SWAP_PATTERN.matcher(callBytecode);
            if (matcher.find()) {
                String a = matcher.group(1);
                byte[] c = new byte[2];
                for (int i = 0; i < a.length(); i++)
                    c[i] = (byte) Integer.parseInt(Integer.toHexString(a.charAt(i) | 0x10000).substring(1), 16);
                InputStream isa = new ByteArrayInputStream(c);
                int multiname_index = readU32(isa);
                if (multiname_map.containsKey(multiname_index)) {
                    if (multiname_map.get(multiname_index).contains("clone_")) { //clone
                        final int arg = matcher.group(2).charAt(0);
                        logger.info("clone " + arg);
                        lstSig = clone(lstSig, arg);
                    } else { //swap
                        final int arg = matcher.group(2).charAt(0);
                        logger.info("swap " + arg);
                        lstSig = swap(lstSig, arg);
                    }
                }
                continue;
            }
            throw new PluginImplementationException("Error parsing SWF (2)");
        }
        StringBuilder sb = new StringBuilder();
        for (String s : lstSig) {
            sb.append(s);
        }
        return sb.toString();
    }

    private String getBytecode(final HttpMethod method) throws Exception {
        final InputStream is = client.makeRequestForFile(method);
        if (is == null) {
            throw new ServiceConnectionProblemException("Error downloading SWF");
        }
        final String swf = readSwfStreamToString(is);

        final String regex = "(?s)\\Q\u00d0\u0030\u00d1\u002c\u0001\u0046\\E.\\Q\u002e\u0001\u0080\u0014\u00d6\\E"
                + "(.+?)\\Q\u00d6\u00d2\u002c\u0001\u0046\\E.\\Q\u0030\u0001\u0048\\E";
        final Matcher matcher = PlugUtils.matcher(regex, swf);
        if (!matcher.find()) {
            throw new PluginImplementationException("Error parsing SWF (1)");
        }
        return matcher.group(1);
    }

    private String readSwfStreamToString(InputStream is) throws IOException {
        try {

            final byte[] bytes = new byte[2048];
            if (readBytes(is, bytes, 8) != 8) {
                throw new IOException("Error reading from stream");
            }
            if (bytes[0] == 'C' && bytes[1] == 'W' && bytes[2] == 'S') {
                bytes[0] = 'F';
                is = new InflaterInputStream(is);
            } else if (bytes[0] != 'F' || bytes[1] != 'W' || bytes[2] != 'S') {
                throw new IOException("Invalid SWF stream");
            }
            InputStream bis = new BufferedInputStream(is);
            bis.mark(Integer.MAX_VALUE);
            multiname_map = getMultinameMap(bis);

            final StringBuilder sb = new StringBuilder(8192);
            sb.append(new String(bytes, 0, 8, "ISO-8859-1"));
            int len;
            bis.reset();
            while ((len = bis.read(bytes)) != -1) {
                sb.append(new String(bytes, 0, len, "ISO-8859-1"));
            }
            return sb.toString();
        } finally {
            try {
                is.close();
            } catch (final Exception e) {
                LogUtils.processException(logger, e);
            }
        }
    }

    private Map<Integer, String> getMultinameMap(InputStream bis) throws IOException {
        readBytes(bis, 9); //RECT
        bis.read();
        bis.read(); //frame rate
        readUI16(bis); //frame count

        //read tag
        Map<Integer, String> constant_string_map = new HashMap<Integer, String>();  //k=string index, v=string name
        Map<Integer, String> multiname_map = new HashMap<Integer, String>();  //k=multiname index, v=string name
        while (true) {
            int tagCodeAndLength = readUI16(bis);
            int tagCode = (tagCodeAndLength) >> 6;
            if (tagCode == 0) {
                break;
            }


            long tagLength = (tagCodeAndLength & 0x003F);
            if (tagLength == 0x3f) {
                tagLength = readSI32(bis);
            }
            byte data[] = readBytes(bis, (int) tagLength);
            if (tagCode != 82) {
                continue;
            }

            InputStream abcIs = new ByteArrayInputStream(data);
            //DoABC tag
            //long flags = readUI32(abcIs);
            readUI32(abcIs); //flags
            String name = readString(abcIs);

            //ABC
            int minor_version = readUI16(abcIs); //minor ver
            readUI16(abcIs); //major ver

            //constant pool

            //constant int
            int constant_int_pool_count = readU30(abcIs);
            for (int i = 1; i < constant_int_pool_count; i++) { //index 0 not used. Values 1..n-1
                readS32(abcIs);
            }

            //constant uint
            int constant_uint_pool_count = readU30(abcIs);
            for (int i = 1; i < constant_uint_pool_count; i++) { //index 0 not used. Values 1..n-1
                readU32(abcIs);
            }

            //constant double
            int constant_double_pool_count = readU30(abcIs);
            for (int i = 1; i < constant_double_pool_count; i++) { //index 0 not used. Values 1..n-1
                readDouble(abcIs);
            }

            //constant decimal
            if (minor_version >= MINORwithDECIMAL) {
                int constant_decimal_pool_count = readU30(abcIs);
                for (int i = 1; i < constant_decimal_pool_count; i++) { //index 0 not used. Values 1..n-1
                    readDecimal(abcIs);
                }
            }

            //constant string
            int constant_string_pool_count = readU30(abcIs);
            for (int i = 1; i < constant_string_pool_count; i++) { //index 0 not used. Values 1..n-1
                String constant_string = readABCString(abcIs);
                if (constant_string.contains("clone_")
                        || constant_string.contains("reverse_")
                        || constant_string.contains("swap_")) {
                    constant_string_map.put(i, constant_string);
                }
            }

            //constant namespace
            int constant_namespace_pool_count = readU30(abcIs);
            for (int i = 1; i < constant_namespace_pool_count; i++) { //index 0 not used. Values 1..n-1
                int kind = abcIs.read();
                for (int nameSpaceKind : nameSpaceKinds) {
                    if (nameSpaceKind == kind) {
                        readU30(abcIs);
                        break;
                    }
                }
            }

            //constant namespace set
            int constant_namespace_set_pool_count = readU30(abcIs);
            for (int i = 1; i < constant_namespace_set_pool_count; i++) { //index 0 not used. Values 1..n-1
                int namespace_count = readU30(abcIs);
                for (int j = 0; j < namespace_count; j++) {
                    readU30(abcIs);
                }
            }

            //constant multiname
            int constant_multiname_pool_count = readU30(abcIs);
            for (int i = 1; i < constant_multiname_pool_count; i++) { //index 0 not used. Values 1..n-1
                int kind = abcIs.read();

                if ((kind == 7) || (kind == 0xd)) { // CONSTANT_QName and CONSTANT_QNameA.
                    readU30(abcIs);
                    int string_name_index = readU30(abcIs);
                    if (constant_string_map.containsKey(string_name_index)) {
                        multiname_map.put(i, constant_string_map.get(string_name_index));
                    }
                } else if ((kind == 0xf) || (kind == 0x10)) { //CONSTANT_RTQName and CONSTANT_RTQNameA
                    readU30(abcIs);
                } else if ((kind == 0x11) || (kind == 0x12))//kind==0x11,0x12 nothing CONSTANT_RTQNameL and CONSTANT_RTQNameLA.
                {
                    //
                } else if ((kind == 9) || (kind == 0xe)) { // CONSTANT_Multiname and CONSTANT_MultinameA.
                    readU30(abcIs);
                    readU30(abcIs);
                } else if ((kind == 0x1B) || (kind == 0x1C)) { //CONSTANT_MultinameL and CONSTANT_MultinameLA
                    readU30(abcIs);
                } else if (kind == 0x1D) {
                    //Constant_TypeName
                    readU30(abcIs);  //Multiname index!!!
                    int paramsLength = readU30(abcIs);
                    for (int j = 0; j < paramsLength; j++) {
                        readU30(abcIs); //multiname indices!
                    }
                } else {
                    throw new IOException("Unknown kind of Multiname:0x" + Integer.toHexString(kind));
                }
            }
        }

        return multiname_map;
    }

    private static int readBytes(InputStream is, byte[] buffer, int count) throws IOException {
        int read = 0, i;
        while (count > 0 && (i = is.read(buffer, 0, count)) != -1) {
            count -= i;
            read += i;
        }
        return read;
    }

    private static byte[] readBytes(InputStream is, long count) throws IOException {
        if (count <= 0) {
            return new byte[0];
        }
        byte ret[] = new byte[(int) count];
        for (int i = 0; i < count; i++) {
            ret[i] = (byte) is.read();
        }
        return ret;
    }

    private static int readUI16(InputStream is) throws IOException {
        return is.read() + (is.read() << 8);
    }

    private static long readUI32(InputStream is) throws IOException {
        return is.read() + (is.read() << 8) + (is.read() << 16) + (is.read() << 24);
    }


    private static long readSI32(InputStream is) throws IOException {
        long uval = is.read() + (is.read() << 8) + (is.read() << 16) + (is.read() << 24);
        if (uval >= 0x80000000) {
            return -(((~uval)) + 1);
        } else {
            return uval;
        }
    }

    private static String readString(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int r;
        while (true) {
            r = is.read();
            if (r <= 0) {
                return new String(baos.toByteArray(), "utf8");
            }
            baos.write(r);
        }
    }

    //encoded 32-bit unsigned integer
    private static int readU32(InputStream is) throws IOException {
        int i;
        int ret = 0;
        int bytePos = 0;
        int byteCount = 0;
        boolean nextByte;
        do {
            i = is.read();
            nextByte = (i >> 7) == 1;
            i = i & 0x7f;
            ret = ret + (i << bytePos);
            byteCount++;
            bytePos += 7;
        } while (nextByte);
        return ret;
    }

    private static int readU30(InputStream is) throws IOException {
        return readU32(is);
    }

    private static long readS32(InputStream is) throws IOException {
        int i;
        long ret = 0;
        int bytePos = 0;
        int byteCount = 0;
        boolean nextByte;
        do {
            i = is.read();
            nextByte = (i >> 7) == 1;
            i = i & 0x7f;
            ret = ret + (i << bytePos);
            byteCount++;
            bytePos += 7;
            if (bytePos == 35) {
                if ((ret >> 31) == 1) {
                    ret = -(ret & 0x7fffffff);
                }
                break;
            }
        } while (nextByte);
        return ret;
    }

    private static long readLong(InputStream is) throws IOException {
        byte readBuffer[] = safeRead(is, 8);
        return (((long) readBuffer[7] << 56)
                + ((long) (readBuffer[6] & 255) << 48)
                + ((long) (readBuffer[5] & 255) << 40)
                + ((long) (readBuffer[4] & 255) << 32)
                + ((long) (readBuffer[3] & 255) << 24)
                + ((readBuffer[2] & 255) << 16)
                + ((readBuffer[1] & 255) << 8)
                + ((readBuffer[0] & 255)));
    }

    private static double readDouble(InputStream is) throws IOException {
        long el = readLong(is);
        return Double.longBitsToDouble(el);
    }

    private static byte[] readDecimal(InputStream is) throws IOException {
        return readBytes(is, 16);
    }

    private static byte[] safeRead(InputStream is, int count) throws IOException {
        byte ret[] = new byte[count];
        for (int i = 0; i < count; i++) {
            ret[i] = (byte) is.read();
        }
        return ret;
    }

    private static String readABCString(InputStream is) throws IOException {
        int length = readU30(is);
        byte b[] = safeRead(is, length);
        return new String(b, "UTF-8");
    }

}
