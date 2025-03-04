package com.justin.videoencryption.util;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class EncryptUtil {

    /**
     * 使用 LSB 加密方式修改像素的最低有效位
     * @param originalValue 原始像素的值（0-255）
     * @param secretValue 要嵌入的秘密信息（0-255）
     * @return 加密后的像素值
     */
    public static int encryptWithLSB(int originalValue, int secretValue) {
        // 清除 originalValue 的低三位
        originalValue = originalValue & 0xFFFFFFF8;
        // 提取 secretValue 的高三位
        secretValue = (secretValue >> 5) & 0x07;
        // 合并 originalValue 和 secretValue
        return originalValue | secretValue;
    }

    /**
     * 批量加密逻辑，使用 LSB 隐写技术
     * @param originalPixels 原始图像的像素值
     * @param secretPixels 涉密图像的像素值
     * @return 加密后的像素值
     */
    public static int[] batchEncryptWithLSB(int[] originalPixels, int[] secretPixels) {
        int[] encryptedPixels = new int[originalPixels.length];

        for (int i = 0; i < originalPixels.length; i++) {
            int rOriginal = (originalPixels[i] >> 16) & 0xFF;
            int gOriginal = (originalPixels[i] >> 8) & 0xFF;
            int bOriginal = originalPixels[i] & 0xFF;

            int rSecret = (secretPixels[i] >> 16) & 0xFF;
            int gSecret = (secretPixels[i] >> 8) & 0xFF;
            int bSecret = secretPixels[i] & 0xFF;

            // 使用 LSB 方式对每个颜色通道进行加密
            int rEncrypted = encryptWithLSB(rOriginal, rSecret);
            int gEncrypted = encryptWithLSB(gOriginal, gSecret);
            int bEncrypted = encryptWithLSB(bOriginal, bSecret);

            encryptedPixels[i] = (rEncrypted << 16) | (gEncrypted << 8) | bEncrypted;
        }

        return encryptedPixels;
    }

    /**
     * 加密视频帧的核心逻辑，使用 LSB 加密每一帧
     * @param originalFrame 原始视频帧
     * @param secretFrame 涉密视频帧
     * @return 加密后的视频帧
     */
    public static Mat encryptFrameWithLSB(Mat originalFrame, Mat secretFrame) throws Exception {
        // 获取行和列数
        int rows = originalFrame.rows();
        int cols = originalFrame.cols();
        int totalPixels = rows * cols;

        // 创建加密后的视频帧
        Mat encryptedFrame = new Mat(rows, cols, originalFrame.type());

        // 每个像素3个字节，BGR
        // totalPixels * 3 是图像中所有像素的数量
        byte[] originalData = new byte[totalPixels * 3];
        byte[] secretData = new byte[totalPixels * 3];
        byte[] encryptedData = new byte[totalPixels * 3];

        // 这行代码的目的是从 originalFrame（原始图像帧）中获取图像的像素数据
        // 并将其存储到 originalData 数组中
        // 0, 0 表示从什么位置开始提取
        originalFrame.get(0, 0, originalData);
        secretFrame.get(0, 0, secretData);

        int[] originalPixels = new int[totalPixels];
        int[] secretPixels = new int[totalPixels];

        for (int i = 0; i < totalPixels; i++) {

            // 将 BGR 值转换为 int 数字
            int bOriginal = originalData[i * 3] & 0xFF;
            int gOriginal = originalData[i * 3 + 1] & 0xFF;
            int rOriginal = originalData[i * 3 + 2] & 0xFF;
            originalPixels[i] = (rOriginal << 16) | (gOriginal << 8) | bOriginal;

            int bSecret = secretData[i * 3] & 0xFF;
            int gSecret = secretData[i * 3 + 1] & 0xFF;
            int rSecret = secretData[i * 3 + 2] & 0xFF;
            secretPixels[i] = (rSecret << 16) | (gSecret << 8) | bSecret;
        }

        // 使用 LSB 方法加密像素
        int[] encryptedPixels = batchEncryptWithLSB(originalPixels, secretPixels);

        // 将加密后的数据存到 encryptedData
        for (int i = 0; i < totalPixels; i++) {
            encryptedData[i * 3] = (byte) (encryptedPixels[i] & 0xFF); // B
            encryptedData[i * 3 + 1] = (byte) ((encryptedPixels[i] >> 8) & 0xFF); // G
            encryptedData[i * 3 + 2] = (byte) ((encryptedPixels[i] >> 16) & 0xFF); // R
        }

        encryptedFrame.put(0, 0, encryptedData);

        return encryptedFrame;
    }

    /**
     * 使用并行化处理加密每一帧
     * @param originalFrames 原始视频帧列表
     * @param secretFrames 涉密视频帧列表
     * @return 加密后的帧列表
     */
    public static List<Mat> encryptFramesWithLSB(List<Mat> originalFrames, List<Mat> secretFrames) {
        // 计算最大帧数
        int maxFrames = Math.max(originalFrames.size(), secretFrames.size());

        // 创建加密视频帧集合
        List<Mat> encryptedFrames = new ArrayList<>();

        // 使用线程池来加速加密过程，ExecutorService 用于管理线程池。
        // Executors.newFixedThreadPool() 会创建一个固定大小的线程池，
        // 大小为当前系统可用的处理器数量（Runtime.getRuntime().availableProcessors()）。
        // futures 是一个 List<Future<Mat>>，用于保存每个加密任务的 Future 对象，Future 可以用来等待任务的执行结果。
        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        List<Future<Mat>> futures = new ArrayList<>();

        for (int i = 0; i < maxFrames; i++) {
            final int frameIndex = i;

            futures.add(executorService.submit(() -> {
                Mat originalFrame = originalFrames.get(frameIndex % originalFrames.size());
                Mat secretFrame = secretFrames.get(frameIndex % secretFrames.size());

                // 调整尺寸
                if (originalFrame.rows() != secretFrame.rows() || originalFrame.cols() != secretFrame.cols()) {
                    Imgproc.resize(secretFrame, secretFrame, new Size(originalFrame.cols(), originalFrame.rows()));
                }

                return encryptFrameWithLSB(originalFrame, secretFrame);
            }));
        }

        for (Future<Mat> future : futures) {
            try {
                encryptedFrames.add(future.get());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        executorService.shutdown();
        return encryptedFrames;
    }
}
