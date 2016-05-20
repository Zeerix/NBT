package me.zeerix.nbt;

import java.io.BufferedInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * @author Zeerix
 */
public class NBTStreamReader {
    /**
     * Reads a NBT tag compound from a GZIP compressed InputStream
     * @param inputstream
     * @return Map<String, Object> describing the NBT tag compound
     * @throws IOException
     */
    public static Map<String, Object> read(InputStream in) throws IOException {
        return read(in, true);
    }

    /**
     * Reads a NBT tag compound from an InputStream, either GZIP compressed or uncompressed
     * @param inputstream
     * @param compressed 'true' if the data in InputStream is compressed
     * @return Map<String, Object> describing the NBT tag compound
     * @throws IOException
     */
    public static Map<String, Object> read(InputStream in, boolean compressed) throws IOException {
        DataInputStream data = compressed
            ? new DataInputStream(new GZIPInputStream(in))
            : new DataInputStream(new BufferedInputStream(in));
        try {
            return read( (DataInput)data );
        } finally {
            data.close();
        }
    }

    private static Map<String, Object> read(DataInput in) throws IOException {
        if (in.readByte() != 10)    // assume TypeID of NBT::TAG_Compound
            throw new IOException("Root tag must be a named compound tag");

        String name = readString(in);

        Map<String, Object> map = readCompound(in);
        if (!name.isEmpty()) {
            map.put("$this.name", name);
        }

        return map;
    }

    private static Object readTag(DataInput in, byte type) throws IOException {
        switch (type) {
        case 1: return in.readByte();
        case 2: return in.readShort();
        case 3: return in.readInt();
        case 4: return in.readLong();
        case 5: return in.readFloat();
        case 6: return in.readDouble();

        case 7: return readByteArray(in);
        case 8: return readString(in);
        case 9: return readList(in);
        case 10: return readCompound(in);
        case 11: return readIntArray(in);
        default: throw new IOException("Invalid NBT tag type (1-10): " + type);
        }
    }

    private static int[] readIntArray(DataInput in) throws IOException {
        int[] data = new int[ in.readInt() ];
        for (int i = 0; i < data.length; i++)
            data[i] = in.readInt();
        return data;
    }

    private static byte[] readByteArray(DataInput in) throws IOException {
        byte[] data = new byte[ in.readInt() ];
        in.readFully(data);
        return data;
    }

    private static String readString(DataInput in) throws IOException {
        return in.readUTF();
    }

    private static List<Object> readList(DataInput in) throws IOException {
        byte type = in.readByte();
        int length = in.readInt();
        List<Object> list = new ArrayList<Object>(length);
        for (int i = 0; i < length; ++i) {
            Object tag = readTag(in, type);
            list.add(tag);
        }
        return list;
    }

    private static Map<String, Object> readCompound(DataInput in) throws IOException {
        Map<String, Object> map = new HashMap<String, Object>();
        for (byte type; (type = in.readByte()) != 0; ) {
            String name = readString(in);
            Object tag = readTag(in, type);
            map.put(name, tag);
        }
        return map;
    }
}
