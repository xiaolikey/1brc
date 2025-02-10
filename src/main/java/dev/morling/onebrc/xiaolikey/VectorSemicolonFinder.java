package dev.morling.onebrc.xiaolikey;

/**
 * 利用JDK16引入的Vector API实现SIMD并行查找
 * @author xiaolikey
 * @date 2025/2/13
 * @since 0.0.1
 */
import jdk.incubator.vector.*;

public class VectorSemicolonFinder {
    private static final VectorSpecies<Byte> SPECIES = ByteVector.SPECIES_256;
    private static final ByteVector SEMICOLON_VEC = ByteVector.broadcast(SPECIES, (byte)';');

    public static int findSemicolonVector(byte[] buffer, int offset) {
        int i = offset;
        final int length = buffer.length;
        final int vectorLength = SPECIES.length();

        // 向量化处理
        for (; i + vectorLength <= length; i += vectorLength) {
            ByteVector chunk = ByteVector.fromArray(SPECIES, buffer, i);
            VectorMask<Byte> mask = chunk.eq(SEMICOLON_VEC);

            if (mask.anyTrue()) {
                return i + mask.firstTrue();
            }
        }

        // 处理尾部数据
        for (; i < length; i++) {
            if (buffer[i] == ';') return i;
        }

        return -1;
    }
}
