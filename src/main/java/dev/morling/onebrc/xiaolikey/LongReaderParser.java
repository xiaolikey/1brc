package dev.morling.onebrc.xiaolikey;

/**
 * TODO
 * @author xiaolikey
 * @date 2025/3/12
 * @since 0.0.1
 */

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class LongReaderParser {
    private static final long SEMICOLON_MASK = 0x3B3B3B3B3B3B3B3BL;
    private static final long HIGH_BIT_MASK = 0x8080808080808080L;

    public static void parse(ByteBuffer buffer) {
        buffer.order(ByteOrder.nativeOrder());
        long[] longBuffer = new long[buffer.remaining() / 8 + 1];
        int longCount = 0;

        // 批量读取为long数组
        while (buffer.remaining() >= 8) {
            longBuffer[longCount++] = buffer.getLong();
        }

        // 处理剩余字节（不足8字节部分）
        int remaining = buffer.remaining();
        if (remaining > 0) {
            long last = 0;
            for (int i = 0; i < remaining; i++) {
                last |= (long) buffer.get() << (56 - 8 * i);
            }
            longBuffer[longCount++] = last;
        }

        processLongs(longBuffer, longCount);
    }

    private static void processLongs(long[] longs, int count) {
        for (int i = 0; i < count; i++) {
            long chunk = longs[i];
            long diff = chunk ^ SEMICOLON_MASK;
            long matches = (diff - 0x0101010101010101L) & (~diff & HIGH_BIT_MASK);

            while (matches != 0) {
                int pos = Long.numberOfTrailingZeros(matches);
                int bytePos = pos / 8;
                System.out.println("Found ';' at position: " + (i * 8 + bytePos));
                matches &= matches - 1; // 清除最低有效位
            }
        }
    }

    private static final long[] MASK = new long[]{ 0xFFL, 0xFFFFL, 0xFFFFFFL, 0xFFFFFFFFL, 0xFFFFFFFFFFL, 0xFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFFL,
            0xFFFFFFFFFFFFFFFFL };

    private static long findSemicolon(long word) {
        long input = word ^ 0x3B3B3B3B3B3B3B3BL;
        return (input - 0x0101010101010101L) & ~input & 0x8080808080808080L;
    }

    public static long[] extractStationLongs(long[] longs){
        long[] names = new long[2];

        return names;
    }

    public static String parseName(long[] names, int len){
        byte[] bytes = new byte[len];
        for (int i = 0; i < len; i++) {
            long name = names[i / 8];
            int shift = 56 - (i % 8) * 8;
            bytes[i] = (byte) ((name >> shift) & 0xFF);
        }
        return new String(bytes);
    }


    public static void main(String[] args) {
        ByteBuffer buffer = ByteBuffer.wrap("New York;12.3\nLondon;9.8\nParis;15.6".getBytes());
        parse(buffer);
    }
}