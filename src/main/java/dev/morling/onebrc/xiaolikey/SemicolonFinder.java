package dev.morling.onebrc.xiaolikey;

/**
 * 通过将8个字节打包为long类型，使用位运算批量检测分号位置，减少循环次数和分支预测失败。
 * @author xiaolikey
 * @date 2025/2/13
 * @since 0.0.1
 */
public class SemicolonFinder {
    private static final long SEMICOLON_PATTERN = 0x3B3B3B3B3B3B3B3BL; // ';'的ASCII重复模式
    private static final long HIGH_BIT_MASK = 0x8080808080808080L;

    /**
     * 在字节数组中查找分号位置
     * @param buffer 字节数组
     * @param offset 起始偏移量
     * @return 分号位置（未找到返回-1）
     */
    public static int findSemicolonSWAR(byte[] buffer, int offset) {
        int i = offset;
        final int length = buffer.length;

        // 处理非对齐字节
        while (i < length && (i & 7) != 0) {
            if (buffer[i] == ';') return i;
            i++;
        }

        // 处理64位对齐块
        while (i + 8 <= length) {
            long word = pack64(buffer, i);
            long diff = word ^ SEMICOLON_PATTERN;
            long match = (diff - 0x0101010101010101L) & (~diff & HIGH_BIT_MASK);

            if (match != 0) {
                return i + (Long.numberOfTrailingZeros(match)) >>> 3;
            }
            i += 8;
        }

        // 处理剩余字节
        while (i < length) {
            if (buffer[i] == ';') return i;
            i++;
        }

        return -1;
    }

    // 将8个字节打包为long
    private static long pack64(byte[] bytes, int offset) {
        return ((long) bytes[offset]   << 56) |
                ((long) bytes[offset+1] << 48) |
                ((long) bytes[offset+2] << 40) |
                ((long) bytes[offset+3] << 32) |
                ((long) bytes[offset+4] << 24) |
                ((long) bytes[offset+5] << 16) |
                ((long) bytes[offset+6] << 8)  |
                ((long) bytes[offset+7]);
    }
}
