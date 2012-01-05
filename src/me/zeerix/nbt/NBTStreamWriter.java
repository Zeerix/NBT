package me.zeerix.nbt;

import java.io.BufferedOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

/**
 * @author Zeerix
 */
public class NBTStreamWriter {
    /**
     * Reads a NBT tag compound from a GZIP compressed InputStream
     * @param inputstream
     * @return Map<String, Object> describing the NBT tag compound
     * @throws IOException
     */
    public static void write(OutputStream out, Map<String, Object> map) throws IOException {
        write(out, map, true);
    }

    /**
     * Reads a NBT tag compound from an InputStream, either GZIP compressed or uncompressed
     * @param inputstream
     * @param compressed 'true' if the data in InputStream is compressed
     * @return Map<String, Object> describing the NBT tag compound
     * @throws IOException
     */
    public static void write(OutputStream out, Map<String, Object> map, boolean compressed) throws IOException {
        DataOutputStream data = compressed
            ? new DataOutputStream(new GZIPOutputStream(out))
            : new DataOutputStream(new BufferedOutputStream(out));
        try {
            write( (DataOutput)data, map );
        } finally {
            data.close();
        }
    }

    private static void write(DataOutput out, Map<String, Object> map) throws IOException {
        out.writeByte(10);  // TypeID of NBT::TAG_Compound

        Object name = map.get("$this.name");

        writeString(out, name != null ? (String)name : "");
        writeCompound(out, map);
    }

    private static byte whichType(Object tag) {
        if (tag instanceof Byte) return 1;
        if (tag instanceof Short) return 2;
        if (tag instanceof Integer) return 3;
        if (tag instanceof Long) return 4;
        if (tag instanceof Float) return 5;
        if (tag instanceof Double) return 6;

        if (tag instanceof byte[]) return 7;
        if (tag instanceof String) return 8;
        if (tag instanceof List<?>) return 9;
        if (tag instanceof Map<?, ?>) return 10;

        throw new RuntimeException("Cannot serialize unknown type " + tag.getClass());
    }

    @SuppressWarnings("unchecked")
    private static void writeTag(DataOutput out, byte type, Object tag) throws IOException {
        switch (type) {
        case 1: out.writeByte( (Byte)tag ); break;
        case 2: out.writeShort( (Short)tag ); break;
        case 3: out.writeInt( (Integer)tag ); break;
        case 4: out.writeLong( (Long)tag ); break;
        case 5: out.writeFloat( (Float)tag ); break;
        case 6: out.writeDouble( (Double)tag ); break;

        case 7: writeByteArray(out, (byte[])tag); break;
        case 8: writeString(out, (String)tag); break;
        case 9: writeList(out, (List<Object>)tag); break;
        case 10: writeCompound(out, (Map<String, Object>)tag); break;
        default: throw new IOException("Invalid NBT tag type (1-10): " + type);
        }
    }

    private static void writeByteArray(DataOutput out, byte[] array) throws IOException {
        out.writeInt(array.length);
        out.write(array);
    }

    private static void writeString(DataOutput out, String str) throws IOException {
        out.writeUTF(str);
    }

    private static void writeList(DataOutput out, List<Object> list) throws IOException {
        byte type = list.isEmpty() ? 1 : whichType(list.get(0));
        out.writeByte(type);
        out.writeInt( list.size() );
        for (Object tag : list) {
            writeTag(out, type, tag);
        }
    }

    private static void writeCompound(DataOutput out, Map<String, Object> map) throws IOException {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getKey().equals("$this.name")) {
                continue;   // skip internal name
            }
            byte type = whichType(entry.getValue());
            out.writeByte(type);
            writeString(out, entry.getKey());
            writeTag(out, type, entry.getValue());
        }
        out.writeByte(0);
    }
}
