package dev.morling.onebrc.xiaolikey;

import sun.misc.Unsafe;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * Calculate Average version1 implementation
 *
 * @author xiaolikey
 * @date 2024/12/16
 * @since 0.0.1
 */
public class CalculateAverageV3 {
    private static final String FILE_PATH = "./measurements.txt";
    private static final int MAX_TEMP = 999;
    private static final int MIN_TEMP = -999;
    private static final int MAX_NAME_LENGTH = 100;
    //和cpu的level3 cache大小一致，24MB
    private static final long SEGMENT_SIZE = 24 * 1024 * 1024;

    private static final int MAX_NAME_LONG_LENGTH = 12;


    private static final String[] TEMPERATURE_MAP = new String[999 + 1 + 999];

    private static AtomicInteger segmentLock = new AtomicInteger(0);

    private static final long[] MASK = new long[]{0xFFL, 0xFFFFL, 0xFFFFFFL, 0xFFFFFFFFL, 0xFFFFFFFFFFL, 0xFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFFL,
            0xFFFFFFFFFFFFFFFFL};

    static {
        for (int i = 0; i < 1999; i++) {
            int seq = i - 999;
            StringBuilder sb = seq < 0 ? new StringBuilder("-") : new StringBuilder();
            int abs = Math.abs(seq);
            if (abs < 10) {
                sb.append('0').append('.').append((char) (abs + '0'));
            } else if (abs < 100) {
                sb.append((char) ((abs / 10) + '0')).append('.').append((char) ((abs % 10) + '0'));
            } else {
                sb.append((char) (abs / 100 + '0')).append((char) ((abs / 10) % 10 + '0')).append('.').append((char) ((abs % 10) + '0'));
            }
            TEMPERATURE_MAP[i] = sb.toString();
        }
    }


    /**
     * 站点统计信息
     */
    public static class StationStatistics {
        long[] nameWords;
        int hashCode;
        int min = MIN_TEMP;
        int max = MAX_TEMP;
        int count;
        long sum;
        int nameLen;

        public StationStatistics(int temperature) {
            this.min = temperature;
            this.max = temperature;
            this.count = 1;
            this.sum = temperature;
        }

        public StationStatistics(long[] nameWords, int nameLen, int hashCode,  int temperature) {
            this.nameWords = nameWords;
            this.nameLen = nameLen;
            this.hashCode = (int) hashCode;
            this.min = temperature;
            this.max = temperature;
            this.count = 1;
            this.sum = temperature;
        }

        public void add(int temperature) {
            min = Math.min(min, temperature);
            max = Math.max(max, temperature);
            count++;
            sum += temperature;
        }

        public void merge(StationStatistics other) {
            min = Math.min(min, other.min);
            max = Math.max(max, other.max);
            count += other.count;
            sum += other.sum;
        }

        @Override
        public String toString() {
            int avg = (int) Math.round((double) sum / count);
            return TEMPERATURE_MAP[min + 999] + "/" + TEMPERATURE_MAP[avg + 999] + "/" + TEMPERATURE_MAP[max + 999];
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            StationStatistics that = (StationStatistics) o;
            if (nameWords.length != that.nameWords.length) {
                return false;
            }
            for (int i = 0; i < nameWords.length; i++) {
                if (nameWords[i] != that.nameWords[i]) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        public String parseName() {
            byte[] bytes = new byte[nameLen];
            for (int i = 0; i < nameLen; i++) {
                long name = nameWords[i / 8];
                int offset = i % 8;
                bytes[i] = (byte) ((name & MASK[offset]) >> (offset * 8));
                System.out.println((char) (bytes[i]));
            }
            return new String(bytes);
        }

    }

    // 定义文件分块信息
    public static class FileSegment {
        final long start;  // 起始位置（字节偏移）
        final long length;// 块长度（字节数）
        int offset;

        static Unsafe unsafe = getUnsafe();

        private static Unsafe getUnsafe() {
            //Init Unsafe
            try {
                Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
                theUnsafe.setAccessible(true);
                return (Unsafe) theUnsafe.get(Unsafe.class);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                //Init failed
                System.out.println("初始化Unsafe失败");
                return null;
            }
        }

        public FileSegment collect(Map<String, StationStatistics> statMap) {
            while (hasNext()) {
                String stationName = nextStation();
                int temperature = nextTemperatureByBytes();
                StationStatistics cur = statMap.get(stationName);
                if (cur == null) {
                    statMap.put(stationName, new StationStatistics(temperature));
                } else {
                    cur.add(temperature);
                }
            }
            return this;
        }

        public FileSegment(long start, long length) {
            this.start = start;
            this.length = length;
        }

        public boolean hasNext() {
            return offset < length;
        }

        public String nextStation() {
            byte b;
            byte[] buffer = new byte[MAX_NAME_LENGTH];
            int i = 0;
            while ((b = unsafe.getByte(offset + start)) != ';') {
                buffer[i++] = b;
                offset++;
            }
            //skip ";"
            offset++;
            return new String(buffer, 0, i);
        }

        public int nextTemperature() {
            return offset + 8 < length ? nextTemperatureByLong() : nextTemperatureByBytes();
        }


        public int nextTemperatureByBytes() {
            int ans = 0;
            boolean positive = true;

            if (unsafe.getByte(offset + start) == '-') {
                positive = false;
                offset++;
            }
            while (true) {
                byte b = unsafe.getByte(offset + start);
                offset++;
                if (b == '\n') {
                    break;
                }
                if (b == '.') {
                    continue;
                }
                ans = ans * 10 + b - '0';
            }
            return positive ? ans : -ans;
        }

        /**
         * 如果当前long中存在分号，则返回分号位置，否则返回0
         * @param word long
         * @return 带有分号的问题
         */
        private static long findSemicolon(long word) {
            long input = word ^ 0x3B3B3B3B3B3B3B3BL;
            return (input - 0x0101010101010101L) & ~input & 0x8080808080808080L;
        }



        public StationStatistics nextRecord() {
            long[] names = new long[MAX_NAME_LONG_LENGTH];
            int nameLen = 0;
            // 自定义优化后的哈希算法（见下文）
            int hash = 0;
            while (true) {
                long chunk = unsafe.getLong(offset + start);
                long matches = findSemicolon(chunk);
                if (matches != 0) {
                    int bytePos = ((Long.numberOfTrailingZeros(matches)) >>> 3);
                    offset += bytePos;
                    names[nameLen] = chunk & MASK[bytePos];
                    hash = 31 * hash + (int)(names[nameLen] ^ (names[nameLen] >>> 32));
                    break;
                }
                names[nameLen] = chunk;
                hash = 31 * hash + (int)(chunk ^ (chunk >>> 32));
                nameLen++;
                offset += 8;
            }
            int temperature = nextTemperature();
            return new StationStatistics(names, nameLen, hash, temperature);
        }


        private  int nextTemperatureByLong() {
            long word = unsafe.getLong(offset + start);
            //0 ~ 63
            int decimalSepPos = Long.numberOfTrailingZeros(~word & 0x10101000L);
            int num = (int) convertIntoNumber(decimalSepPos, word);
            offset += ((decimalSepPos >>> 3) + 3);
            return num;
        }

        // Special method to convert a number in the ascii number into an int without branches created by Quan Anh Mai.
        private static long convertIntoNumber(int decimalSepPos, long numberWord) {
            int shift = 28 - decimalSepPos;
            // signed is -1 if negative, 0 otherwise
            long signed = (~numberWord << 59) >> 63;
            long designMask = ~(signed & 0xFF);
            // Align the number to a specific position and transform the ascii to digit value
            long digits = ((numberWord & designMask) << shift) & 0x0F000F0F00L;
            // Now digits is in the form 0xUU00TTHH00 (UU: units digit, TT: tens digit, HH: hundreds digit)
            // 0xUU00TTHH00 * (100 * 0x1000000 + 10 * 0x10000 + 1) =
            // 0x000000UU00TTHH00 + 0x00UU00TTHH000000 * 10 + 0xUU00TTHH00000000 * 100
            long absValue = ((digits * 0x640a0001) >>> 32) & 0x3FF;
            return (absValue ^ signed) - signed;
        }

    }

    /**
     * 将文件划分为多个内存映射区域
     * @param filePath 输入文件路径
     * @param chunkSize 每块大小（字节数）
     * @return 分块信息列表
     */
    public static List<FileSegment> splitFile(Path filePath, long chunkSize) throws IOException {
        List<FileSegment> regions = new ArrayList<>();

        try (FileChannel channel = FileChannel.open(filePath, StandardOpenOption.READ)) {
            long fileSize = channel.size();
            final long fileStart = channel.map(FileChannel.MapMode.READ_ONLY, 0, fileSize, java.lang.foreign.Arena.global()).address();
            long position = 0;
            FileSegment globalFileSegment = new FileSegment(fileStart, fileSize);
            while (position < fileSize) {
                // 计算当前块的实际长度
                long remaining = fileSize - position;
                long length = Math.min(chunkSize, remaining);

                // 确保块边界对齐到行尾
                if (position + length < fileSize) {
                    // 查找最后一个换行符
                    long startAddress = fileStart + position;
                    for (long i = length - 1; i >= 0; i--) {
                        if (FileSegment.unsafe.getByte(startAddress + i) == '\n') {
                            //包含换行符
                            length = i + 1L;
                            break;
                        }
                    }
                }
                regions.add(new FileSegment(fileStart + position, length));
                position += length;
            }
        }

        return regions;
    }

    public static void main(String[] args) throws Exception {
        Path filePath = Paths.get(FILE_PATH);
        // 将文件划分为多个内存映射区域
        List<FileSegment> segments = splitFile(filePath, SEGMENT_SIZE);
        segmentLock.set(segments.size());

        // 使用并行流处理每个文件块
        int coreNum = Runtime.getRuntime().availableProcessors();
        Map<String, StationStatistics>[] stationMaps = new HashMap[coreNum];
        Thread[] threads = new Thread[coreNum];
        for (int i = 0; i < coreNum; i++) {
            Map<String, StationStatistics> stationMap = new HashMap<>();
            stationMaps[i] = stationMap;
            Thread thread = new Thread(() -> {
                for (int segmentIndex = segmentLock.decrementAndGet(); segmentIndex >= 0; segmentIndex = segmentLock.decrementAndGet()) {
                    segments.get(segmentIndex).collect(stationMap);
                }
            });
            threads[i] = thread;
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        //Merge result
        Map<String, StationStatistics> stations = Stream.of(stationMaps).collect(HashMap::new, (m, v) -> {
            v.forEach((k, vv) -> m.merge(k, vv, (v1, v2) -> {
                v1.merge(v2);
                return v1;
            }));
        }, HashMap::putAll);
        System.out.println(new TreeMap<>(stations));
        Path outPath = Paths.get("./result_me.txt");
        try (PrintStream out = new PrintStream(Files.newOutputStream(outPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
            out.println(new TreeMap<>(stations));
        }
    }


}
