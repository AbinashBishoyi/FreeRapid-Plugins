package cz.vity.freerapid.plugins.services.rtmp;

import java.util.HashMap;
import java.util.Map;

/**
 * a little bit of code reuse, would have been cleaner if enum types
 * could extend some other class - we implement an interface instead
 * and have to construct a static instance in each enum type we use
 *
 * @author Peter Thomas
 */
class ByteToEnum<T extends Enum<T> & ByteToEnum.Convert> {

    public interface Convert {
        byte byteValue();
    }

    private Map<Byte, T> map;

    public ByteToEnum(T[] values) {
        map = new HashMap<Byte, T>(values.length);
        for (T t : values) {
            map.put(t.byteValue(), t);
        }
    }

    public T parseByte(byte b) {
        T t = map.get(b);
        if (t == null) {
            throw new RuntimeException("bad byte: " + Utils.toHex(b));
        }
        return t;
    }

    public String toString(T t) {
        return t.name() + "(0x" + Utils.toHex(t.byteValue()) + ")";
    }

}