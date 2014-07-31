package cz.vity.freerapid.plugins.services.youtube;

import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.zip.InflaterInputStream;

/**
 * Reading materials :
 * SWF File Format Specification            : http://wwwimages.adobe.com/www.adobe.com/content/dam/Adobe/en/devnet/swf/pdf/swf-file-format-spec.pdf
 * ActionScript Virtual Machine 2 Overview  : http://www.adobe.com/content/dam/Adobe/en/devnet/actionscript/articles/avm2overview.pdf
 *
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
    private static final int ATTR_METADATA = 4;
    private static final int CLASS_PROTECTED_NS = 8;
    private static final String SWAP_PATTERN = "(?s)(..).{5}\\Q\u00d0\u0030\u00d1\u0024\u0000\u0066\\E..\\Q\u0085\u00d7\\E";
    private static final String REVERSE_PATTERN = "(?s)(..).{5}\\Q\u00d0\u0030\u00d1\u004f\\E..\\Q\u0000\u00d1\u0048\\E";
    private static final String CLONE_PATTERN = "(?s)(..).{5}\\Q\u00d0\u0030\u00d1\u00d2\u0046\\E..\\Q\u0001\u0048\\E";
    private static final String DECIPHER_PATTERN = "(?s)(..).{5}\\Q\u00d0\u0030\u00d0\u00d1\u0024\\E(.+?)\\Q\u00d5\u00d1\u0048\\E";
    private static final String REVERSE_CLONE_SWAP_CALL_PATTERN = "(?s)(.)\\Q\u0046\\E(..)\\Q\u0002\u0080\\E.";
    private static final String CHARSET_NAME = "ISO-8859-1";

    private final InputStream is;
    private final Map<Integer, String> multinameMap = new HashMap<Integer, String>(); //k=name index, v=name
    private final Map<Integer, String> traitnameMethodMap = new HashMap<Integer, String>(); //k=method info, v=trait name
    private String bytecode = null;
    private String reverseTraitname = null;
    private String cloneTraitname = null;
    private String swapTraitname = null;

    public YouTubeSigDecipher(final InputStream is) {
        this.is = is;
    }

    private List<String> clone(List<String> lst, int from) {
        return lst.subList(from, lst.size());
    }

    private List<String> reverse(List<String> lst) {
        Collections.reverse(lst);
        return lst;
    }

    private List<String> swap(List<String> lst, int pos) {
        String head = lst.get(0);
        String headSwapTo = lst.get(pos % lst.size());
        lst.set(0, headSwapTo);
        lst.set(pos, head);
        return lst;
    }

    public String decipher(final String sig) throws Exception {
        init();
        List<String> lstSig = new ArrayList<String>(Arrays.asList(sig.split("")));
        lstSig.remove(0); //remove empty char at head
        Matcher matcher = PlugUtils.matcher(REVERSE_CLONE_SWAP_CALL_PATTERN, bytecode);
        boolean matched = false;
        while (matcher.find()) {
            int arg = matcher.group(1).charAt(0);
            int callPropertyIndex = readU30(new ByteArrayInputStream(matcher.group(2).getBytes(CHARSET_NAME)));
            if (multinameMap.containsKey(callPropertyIndex)) {
                String callPropertyMethodName = multinameMap.get(callPropertyIndex);
                if (callPropertyMethodName.equals(reverseTraitname)) {
                    logger.info("reverse " + arg);
                    lstSig = reverse(lstSig);
                } else if (callPropertyMethodName.equals(cloneTraitname)) {
                    logger.info("clone " + arg);
                    lstSig = clone(lstSig, arg);
                } else if (callPropertyMethodName.equals(swapTraitname)) {
                    logger.info("swap " + arg);
                    lstSig = swap(lstSig, arg);
                } else {
                    throw new PluginImplementationException("Unknown callproperty method name: " + callPropertyMethodName);
                }
            } else {
                throw new PluginImplementationException("Unknown multiname index: " + callPropertyIndex);
            }
            matched = true;
        }
        if (!matched) {
            throw new PluginImplementationException("Error parsing SWF (2)");
        }
        StringBuilder sb = new StringBuilder();
        for (String s : lstSig) {
            sb.append(s);
        }
        return sb.toString();
    }

    private void init() throws Exception {
        if (bytecode == null) {
            final String swf = readSwfStreamToString(is);
            int swapMethodInfo = -1, reverseMethodInfo = -1, cloneMethodInfo = -1;

            /*
            private function LiIsM0G5E1vk(param1:Array, param2:Number) : Array {
                var _loc3_:String = param1[0];
                var _loc4_:String = param1[param2 % param1.length];
                param1[0] = _loc4_;
                param1[param2] = _loc3_;
                return param1;
            }
            */
            Matcher matcher = PlugUtils.matcher(SWAP_PATTERN, swf);
            if (matcher.find()) {
                swapMethodInfo = readU30(new ByteArrayInputStream(matcher.group(1).getBytes(CHARSET_NAME))); //3205; traitname=LiIsM0G5E1vk
            }

            /*
            private function YyOGkH0orx4s(param1:Array, param2:Number) : Array {
                param1.reverse();
                return param1;
            }
            */
            matcher = PlugUtils.matcher(REVERSE_PATTERN, swf);
            if (matcher.find()) {
                reverseMethodInfo = readU30(new ByteArrayInputStream(matcher.group(1).getBytes(CHARSET_NAME))); //3203; traitname=YyOGkH0orx4s
            }

            /*
             private function kvS7BCL1VhHo(param1:Array, param2:Number) : Array {
                return param1.slice(param2);
             }
            */
            matcher = PlugUtils.matcher(CLONE_PATTERN, swf);
            if (matcher.find()) {
                cloneMethodInfo = readU30(new ByteArrayInputStream(matcher.group(1).getBytes(CHARSET_NAME))); //3204; traitname=kvS7BCL1VhHo
            }

            if ((reverseMethodInfo == -1) && (cloneMethodInfo == -1) && (swapMethodInfo == -1)) {
                throw new PluginImplementationException("Error parsing SWF (1)");
            }

            /*
            public function VRlnjHuFLJN6(param1:Array) : Array {
                var param1:Array = this.LiIsM0G5E1vk(param1,15);
                param1 = this.LiIsM0G5E1vk(param1,44);
                param1 = this.YyOGkH0orx4s(param1,71);
                param1 = this.LiIsM0G5E1vk(param1,24);
                param1 = this.kvS7BCL1VhHo(param1,3);
                param1 = this.YyOGkH0orx4s(param1,5);
                param1 = this.LiIsM0G5E1vk(param1,2);
                param1 = this.LiIsM0G5E1vk(param1,50);
                return param1;
            }
            */
            matcher = PlugUtils.matcher(DECIPHER_PATTERN, swf);
            if (!matcher.find()) {
                throw new PluginImplementationException("Decipher method not found");
            }
            //int decipherMethodInfo = readU30(new ByteArrayInputStream(matcher.group(1).getBytes(CHARSET_NAME)));
            bytecode = matcher.group(2);

            parseSwf(new ByteArrayInputStream(swf.getBytes(CHARSET_NAME)));
            reverseTraitname = traitnameMethodMap.get(reverseMethodInfo);
            cloneTraitname = traitnameMethodMap.get(cloneMethodInfo);
            swapTraitname = traitnameMethodMap.get(swapMethodInfo);
        }
    }

    private String readSwfStreamToString(InputStream is) throws IOException {
        try {
            final byte[] bytes = new byte[2048];
            //read the first 8 bytes :
            //3 bytes - signature
            //1 byte  - version
            //4 bytes - file length
            if (readBytes(is, bytes, 8) != 8) {
                throw new IOException("Error reading from stream");
            }
            if (bytes[0] == 'C' && bytes[1] == 'W' && bytes[2] == 'S') {
                bytes[0] = 'F';
                is = new InflaterInputStream(is);
            } else if (bytes[0] != 'F' || bytes[1] != 'W' || bytes[2] != 'S') {
                throw new IOException("Invalid SWF stream");
            }

            final StringBuilder sb = new StringBuilder(8192);
            int len;
            while ((len = is.read(bytes)) != -1) {
                sb.append(new String(bytes, 0, len, CHARSET_NAME));
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

    private void parseSwf(InputStream is) throws IOException {
        readBytes(is, 9); //RECT
        read(is); //ignore
        read(is); //frame rate
        readUI16(is); //frame count

        //read tag
        while (true) {
            int tagCodeAndLength = readUI16(is);
            int tagCode = (tagCodeAndLength) >> 6;
            if (tagCode == 0) {
                break;
            }

            long tagLength = (tagCodeAndLength & 0x003F);
            if (tagLength == 0x3f) {
                tagLength = readSI32(is);
            }

            //we only interested in DoABC tag (tag code = 82)
            byte data[] = readBytes(is, (int) tagLength);
            if (tagCode != 82) {
                continue;
            }
            doAbcTag(data);
        }
    }

    private void doAbcTag(byte[] data) throws IOException {
        //DoABC tag
        Map<Integer, String> constant_string_map = new HashMap<Integer, String>();  //k=string index, v=string name
        InputStream abcIs = new ByteArrayInputStream(data);
        readUI32(abcIs); //flags
        readString(abcIs); //name

        //ABC
        int minor_version = readUI16(abcIs);
        readUI16(abcIs); //major ver

        //constant pool

        //constant int
        int constant_int_pool_count = readU30(abcIs);
        for (int i = 1; i < constant_int_pool_count; i++) { //index 0 not used. Values 1..n-1
            readS32(abcIs); //int value
        }

        //constant uint
        int constant_uint_pool_count = readU30(abcIs);
        for (int i = 1; i < constant_uint_pool_count; i++) { //index 0 not used. Values 1..n-1
            readU32(abcIs); //uint value
        }

        //constant double
        int constant_double_pool_count = readU30(abcIs);
        for (int i = 1; i < constant_double_pool_count; i++) { //index 0 not used. Values 1..n-1
            readDouble(abcIs); //double value
        }

        //constant decimal
        if (minor_version >= MINORwithDECIMAL) {
            int constant_decimal_pool_count = readU30(abcIs);
            for (int i = 1; i < constant_decimal_pool_count; i++) { //index 0 not used. Values 1..n-1
                readDecimal(abcIs); //decimal value
            }
        }

        //constant string
        int constant_string_pool_count = readU30(abcIs);
        for (int i = 1; i < constant_string_pool_count; i++) { //index 0 not used. Values 1..n-1
            String constant_string = readAbcString(abcIs);
            constant_string_map.put(i, constant_string);
        }

        //constant namespace
        int constant_namespace_pool_count = readU30(abcIs);
        for (int i = 1; i < constant_namespace_pool_count; i++) { //index 0 not used. Values 1..n-1
            int kind = abcIs.read();
            for (int nameSpaceKind : nameSpaceKinds) {
                if (nameSpaceKind == kind) {
                    readU30(abcIs); //string name index
                    break;
                }
            }
        }

        //constant namespace set
        int constant_namespace_set_pool_count = readU30(abcIs);
        for (int i = 1; i < constant_namespace_set_pool_count; i++) { //index 0 not used. Values 1..n-1
            int namespace_count = readU30(abcIs);
            for (int j = 0; j < namespace_count; j++) {
                readU30(abcIs); //namespace index
            }
        }

        //constant multiname
        int constant_multiname_pool_count = readU30(abcIs);
        for (int i = 1; i < constant_multiname_pool_count; i++) { //index 0 not used. Values 1..n-1
            int kind = abcIs.read();

            if ((kind == 7) || (kind == 0xd)) { // CONSTANT_QName and CONSTANT_QNameA.
                readU30(abcIs); //namespace index
                int string_name_index = readU30(abcIs);
                //save multiname index (k) and string name (v) if the string name index exists in constant_string_map
                if (constant_string_map.containsKey(string_name_index)) {
                    multinameMap.put(i, constant_string_map.get(string_name_index));
                }
            } else if ((kind == 0xf) || (kind == 0x10)) { //CONSTANT_RTQName and CONSTANT_RTQNameA
                readU30(abcIs); //string name index
            } else if ((kind == 0x11) || (kind == 0x12)) { //kind==0x11,0x12 nothing CONSTANT_RTQNameL and CONSTANT_RTQNameLA.
                //
            } else if ((kind == 9) || (kind == 0xe)) { // CONSTANT_Multiname and CONSTANT_MultinameA.
                readU30(abcIs); //string name index
                readU30(abcIs); //namespace set index
            } else if ((kind == 0x1B) || (kind == 0x1C)) { //CONSTANT_MultinameL and CONSTANT_MultinameLA
                readU30(abcIs); //namespace set index
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

        //method info
        int methods_count = readU30(abcIs);
        for (int i = 0; i < methods_count; i++) {
            int param_count = readU30(abcIs);
            readU30(abcIs); //ret_type
            for (int j = 0; j < param_count; j++) {
                readU30(abcIs); //param type
            }
            readU30(abcIs); //name_index
            int flags = read(abcIs);
            //// 1=need_arguments, 2=need_activation, 4=need_rest 8=has_optional (16=ignore_rest, 32=explicit,) 64=setsdxns, 128=has_paramnames

            if ((flags & 8) == 8) { //if has_optional
                int optional_count = readU30(abcIs);
                for (int j = 0; j < optional_count; j++) {
                    readU30(abcIs);  //value index
                    read(abcIs); //value kind
                }
            }

            if ((flags & 128) == 128) { //if has_paramnames
                for (int j = 0; j < param_count; j++) {
                    readU30(abcIs); //param name
                }
            }
        }

        //metadata info
        int metadata_count = readU30(abcIs);
        for (int i = 0; i < metadata_count; i++) {
            readU30(abcIs); //name_index
            int values_count = readU30(abcIs);
            for (int v = 0; v < values_count; v++) {
                readU30(abcIs); //key
            }
            for (int v = 0; v < values_count; v++) {
                readU30(abcIs); //value
            }
        }

        //instance
        int class_count = readU30(abcIs);
        for (int i = 0; i < class_count; i++) {
            readInstanceInfo(abcIs);
        }

        //class info
        for (int i = 0; i < class_count; i++) {
            readU30(abcIs); //cinit index
            readTraits(abcIs); //static traits
        }

        //script info
        int script_count = readU30(abcIs);
        for (int i = 0; i < script_count; i++) {
            readU30(abcIs); //init index
            readTraits(abcIs);
        }

        //method body
        int bodies_count = readU30(abcIs);
        for (int i = 0; i < bodies_count; i++) {
            readU30(abcIs); //method_info
            readU30(abcIs); //max_stack
            readU30(abcIs); //max_regs
            readU30(abcIs); //init_scope_depth
            readU30(abcIs); //max_scope_depth
            int code_length = readU30(abcIs);
            readBytes(abcIs, code_length); //codeBytes

            //exceptions
            int ex_count = readU30(abcIs);
            for (int j = 0; j < ex_count; j++) {
                readU30(abcIs); //start
                readU30(abcIs); //end
                readU30(abcIs); //target
                readU30(abcIs); //type index
                readU30(abcIs); //name index
            }
            readTraits(abcIs);
        }
    }

    private void readInstanceInfo(InputStream is) throws IOException {
        readU30(is); //name index
        readU30(is); //super index
        int flags = read(is);
        if ((flags & CLASS_PROTECTED_NS) != 0) {
            readU30(is); //protected NS
        }
        int interfaces_count = readU30(is);
        for (int i = 0; i < interfaces_count; i++) {
            readU30(is); //interface
        }
        readU30(is); //iinit index
        readTraits(is);
    }

    private void readTrait(InputStream is) throws IOException {
        int name_index = readU30(is);
        String name = multinameMap.get(name_index);
        int kind = read(is);
        int kindType = 0xf & kind;
        int kindFlags = kind >> 4;

        switch (kindType) {
            case 0: //slot
            case 6: //const
                readU30(is); //slot id
                readU30(is); //type index
                int value_index = readU30(is);
                if (value_index != 0) {
                    read(is); //value kind
                }
                break;
            case 1: //method
            case 2: //getter
            case 3: //setter
                readU30(is); //disp id
                int method_info = readU30(is); //method info
                traitnameMethodMap.put(method_info, name);
                break;
            case 4: //class
                readU30(is); //slot id
                readU30(is); //class info
                break;
            case 5: //function
                readU30(is); //slot index
                readU30(is); //method info
                break;
            default:
                throw new IOException("Unknown trait kind:" + kind);
        }
        if ((kindFlags & ATTR_METADATA) != 0) {
            int metadata_count = readU30(is);
            for (int i = 0; i < metadata_count; i++) {
                readU30(is); //trait metadata
            }
        }
    }

    private void readTraits(InputStream is) throws IOException {
        int count = readU30(is);
        for (int i = 0; i < count; i++) {
            readTrait(is);
        }
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

    private static int read(InputStream is) throws IOException {
        return is.read();
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
                return new String(baos.toByteArray(), CHARSET_NAME);
            }
            baos.write(r);
        }
    }

    //encoded 32-bit unsigned integer
    private static int readU32(InputStream is) throws IOException {
        int i;
        int ret = 0;
        int bytePos = 0;
        //int byteCount = 0;
        boolean nextByte;
        do {
            i = is.read();
            nextByte = (i >> 7) == 1;
            i = i & 0x7f;
            ret = ret + (i << bytePos);
            //byteCount++;
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
        //int byteCount = 0;
        boolean nextByte;
        do {
            i = is.read();
            nextByte = (i >> 7) == 1;
            i = i & 0x7f;
            ret = ret + (i << bytePos);
            //byteCount++;
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

    private static String readAbcString(InputStream is) throws IOException {
        int length = readU30(is);
        byte b[] = safeRead(is, length);
        return new String(b, CHARSET_NAME);
    }

    /*
    public static void main(String[] args) throws Exception {
        //watch_as3-vfldOoVEA~.swf
        //watch_as3-vflJvFBCS.swf
        //watch_as3-vflNr3l6D.swf
        //watch_as3-vflU4Jt6h.swf
        //watch_as3-vflW5qQCZ.swf
        //watch_as3-vflmeWTlC.swf
        System.out.println(new YouTubeSigDecipher(new FileInputStream(new File("/media/DATA/kerja/javaProj/FRD/frd/youtube/watch_as3-vflU4Jt6h.swf")))
                .decipher("BB5A7F095FE6874253FF152F0145151A6791478EE4.0691ADBB55103ABA1088D2B98BF6B4A3A1444EDCDC"));
    }
    */

}
