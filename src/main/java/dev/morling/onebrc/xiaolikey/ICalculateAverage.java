package dev.morling.onebrc.xiaolikey;

/**
 * Calculate Average Interface
 *
 * @author xiaolikey
 * @date 2024/12/16
 * @since 0.0.1
 */
public interface ICalculateAverage {

    default void dose100m() throws Exception {
        dose("./measurements_100m.txt");
    }

    default void dose10m() throws Exception {
        dose("./measurements_10m.txt");
    }

    default void dose1m() throws Exception {
        dose("./measurements_1m.txt");
    }

    default void dose1b() throws Exception {
        dose("./measurements_1b.txt");
    }

    void dose(String filePath) throws Exception;
}
