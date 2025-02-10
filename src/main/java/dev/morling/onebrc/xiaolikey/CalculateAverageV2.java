package dev.morling.onebrc.xiaolikey;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Calculate Average version1 implementation
 *
 * @author xiaolikey
 * @date 2024/12/16
 * @since 0.0.1
 */
public class CalculateAverageV2 {
    public static final String FILE_PATH = "./measurements.txt";
    public static final int MAX_TEMP = 999;
    public static final int MIN_TEMP = -999;
    public static final int STATION_CAPACITY = 10000;
    //和cpu的level3 cache大小一致，24MB
    public static final long SEGMENT_SIZE = 24 * 1024 * 1024;

    public static final String[] TEMPERATURE_MAP = new String[999 + 1 + 999];

    static {
        for (int i = 0; i < 1999; i++) {
            int seq = i - 999;
            StringBuilder sb = seq < 0 ? new StringBuilder("-") : new StringBuilder();
            int abs = Math.abs(seq);
            if (abs < 10) {
                sb.append('0').append('.').append((char)(abs + '0'));
            } else if (abs < 100) {
                sb.append((char)((abs / 10) + '0')).append('.').append((char)((abs % 10) + '0'));
            } else {
                sb.append((char)(abs / 100 + '0')).append((char)((abs / 10) % 10 + '0')).append('.').append((char)((abs % 10) + '0'));
            }
            TEMPERATURE_MAP[i] = sb.toString();
        }
    }


    /**
     * 站点统计信息
     */
    public static class StationStatistics {
        int min = MIN_TEMP;
        int max = MAX_TEMP;
        int count;
        long sum;

        public StationStatistics(int temperature) {
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
    }

    // 定义文件分块信息
    public static class FileSegment {
        final Path filePath;
        final long start;  // 起始位置（字节偏移）
        final long length;// 块长度（字节数）
        int offset;
        int limit;
        byte[] buffer;

        public FileSegment loadBytes(Path filePath) {
            try (FileChannel channel = FileChannel.open(filePath, StandardOpenOption.READ)) {
                MappedByteBuffer buffer = channel.map(
                        FileChannel.MapMode.READ_ONLY,
                        start,
                        length
                );
                this.buffer = new byte[buffer.limit()];
                this.limit = buffer.limit();
                buffer.get(this.buffer);
            } catch (IOException e) {
                //ignore
            }
            return this;
        }

        public FileSegment collect(Map<String, StationStatistics> statMap) {
            while (hasNext()) {
                String station = nextStation();
                int temperature = nextTemperature();
                statMap.compute(station, (k, v) -> {
                    if (v == null) {
                        return new StationStatistics(temperature);
                    } else {
                        v.add(temperature);
                        return v;
                    }
                });
            }
            return this;
        }

        public void clear() {
            //gc
            buffer = null;
        }

        public FileSegment(Path filePath, long start, long length) {
            this.filePath = filePath;
            this.start = start;
            this.length = length;
        }

        public boolean hasNext() {
            return offset < limit;
        }

        public String nextStation() {
            int start = offset;
            while (buffer[offset++] != ';') {
            }
            return new String(buffer, start, offset - start - 1);
        }

        public int nextTemperature() {
            int ans = 0;
            boolean positive = true;
            if (buffer[offset] == '-') {
                positive = false;
                offset++;
            }
            while (true) {
                byte b = buffer[offset++];
                if (b == '\n') {
                    break;
                }
                if (b == '.'){
                    continue;
                }
                ans = ans * 10 + b - '0';
            }
            return positive ? ans : -ans;
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
            long position = 0;

            while (position < fileSize) {
                // 计算当前块的实际长度
                long remaining = fileSize - position;
                long length = Math.min(chunkSize, remaining);

                // 确保块边界对齐到行尾
                if (position + length < fileSize) {
                    MappedByteBuffer buffer = channel.map(
                            FileChannel.MapMode.READ_ONLY,
                            position,
                            length
                    );

                    // 查找最后一个换行符
                    for (int i = buffer.limit() - 1; i >= 0; i--) {
                        if (buffer.get(i) == '\n') {
                            //包含换行符
                            length = i + 1L;
                            break;
                        }
                    }
                }
                regions.add(new FileSegment(filePath, position, length));
                position += length;
            }
        }

        return regions;
    }

    public static void main(String[] args) throws Exception {
        Map<String, StationStatistics> stations = new ConcurrentHashMap<>();
        Path filePath = Paths.get(FILE_PATH);
        List<FileSegment> segments = splitFile(filePath, SEGMENT_SIZE);
        segments.parallelStream()
                .forEach(segment -> segment.loadBytes(segment.filePath).collect(stations).clear());
        System.out.println(new TreeMap<>(stations));
        Path outPath = Paths.get("./result_me.txt");
        try(PrintStream out = new PrintStream(Files.newOutputStream(outPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))){
            out.println(new TreeMap<>(stations));
        }
    }

}
