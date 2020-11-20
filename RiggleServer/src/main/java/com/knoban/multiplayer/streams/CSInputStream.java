package com.knoban.multiplayer.streams;

import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * This class was pulled from:
 * https://github.com/Chukobyte/Java_GML_Server/blob/master/GML_Java_Server/src/com/chukobyte/gmljavaserver/main/GMLInputStream.java
 * <br><br>
 * It has been modified slightly to improve readString performance by Alden Bansemer (kNoAPP).
 */
public class CSInputStream {

    private ByteBuffer byteBuffer;
    private DataInput byteReader;

    public CSInputStream(@NotNull InputStream in) {
        byteBuffer = ByteBuffer.allocate(8);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        byteReader = new DataInputStream(in);
    }

    private void prepare(int bytes) throws IOException {
        byteBuffer.clear();
        for(int i=0; i<bytes; i++)
            byteBuffer.put(byteReader.readByte());
        byteBuffer.rewind();
    }

    public byte readS8() throws IOException {
        prepare(1);
        return byteBuffer.get();
    }

    public short readS16() throws IOException {
        prepare(2);
        return byteBuffer.getShort();
    }

    public int readS32() throws IOException {
        prepare(4);
        return byteBuffer.getInt();
    }

    public long readS64() throws IOException {
        prepare(8);
        return byteBuffer.getLong();
    }

    public float readF32() throws IOException {
        prepare(4);
        return byteBuffer.getFloat();
    }

    public double readF64() throws IOException {
        prepare(8);
        return byteBuffer.getDouble();
    }

    @NotNull
    public String readString() throws IOException {
        byte b = byteReader.readByte();
        if(b == 0)
            return "";

        StringBuilder result = new StringBuilder();
        do {
            result.append((char)b);
            b = byteReader.readByte();
        } while(b != 0);
        return result.toString();
    }
}
