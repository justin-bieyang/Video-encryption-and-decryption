package com.justin.videoencryption.util;

import org.opencv.core.Mat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class DecryptUtil {

    /**
     * 使用 LSB 解密方式恢复嵌入的秘密信息
     * @param encryptedValue 加密后的像素值（0-255）
     * @return 解密后的秘密信息（0-255）
     */
    public static int decryptWithLSB(int encryptedValue) {
        return (encryptedValue & 0x07) << 5;
    }

    /**
     * 批量解密逻辑，使用 LSB 隐写技术恢复秘密信息
     * @param encryptedPixels 加密后的像素值
     * @return 恢复的涉密像素值
     */
    public static int[] batchDecryptWithLSB(int[] encryptedPixels) {
        int[] secretPixels = new int[encryptedPixels.length];

        for (int i = 0; i < encryptedPixels.length; i++) {
            int rEncrypted = (encryptedPixels[i] >> 16) & 0xFF;
            int gEncrypted = (encryptedPixels[i] >> 8) & 0xFF;
            int bEncrypted = encryptedPixels[i] & 0xFF;

            // 使用 LSB 提取每个颜色通道的秘密信息
            int rSecret = decryptWithLSB(rEncrypted);
            int gSecret = decryptWithLSB(gEncrypted);
            int bSecret = decryptWithLSB(bEncrypted);

            secretPixels[i] = (rSecret << 16) | (gSecret << 8) | bSecret;
        }

        return secretPixels;
    }

    /**
     * 解密视频帧的核心逻辑，使用 LSB 解密每一帧
     * @param encryptedFrame 加密的视频帧
     * @return 解密后的视频帧
     */
    public static Mat decryptFrameWithLSB(Mat encryptedFrame) throws Exception {
        // 获取行和列数
        int rows = encryptedFrame.rows();
        int cols = encryptedFrame.cols();
        int totalPixels = rows * cols;

        // 创建解密后的视频帧
        Mat secretFrame = new Mat(rows, cols, encryptedFrame.type());

        // 获取底层数据指针
        byte[] encryptedData = new byte[totalPixels * 3];
        byte[] secretData = new byte[totalPixels * 3];

        encryptedFrame.get(0, 0, encryptedData);

        // 将字节数据转换为 int 数组，便于批量解密
        int[] encryptedPixels = new int[totalPixels];

        for (int i = 0; i < totalPixels; i++) {
            int bEncrypted = encryptedData[i * 3] & 0xFF;
            int gEncrypted = encryptedData[i * 3 + 1] & 0xFF;
            int rEncrypted = encryptedData[i * 3 + 2] & 0xFF;
            encryptedPixels[i] = (rEncrypted << 16) | (gEncrypted << 8) | bEncrypted;
        }

        // 使用 LSB 方法解密像素，提取出涉密视频的像素值
        int[] secretPixels = batchDecryptWithLSB(encryptedPixels);

        // 将解密后的数据写回 secretData
        for (int i = 0; i < totalPixels; i++) {
            secretData[i * 3] = (byte) (secretPixels[i] & 0xFF); // B
            secretData[i * 3 + 1] = (byte) ((secretPixels[i] >> 8) & 0xFF); // G
            secretData[i * 3 + 2] = (byte) ((secretPixels[i] >> 16) & 0xFF); // R
        }

        secretFrame.put(0, 0, secretData);
        return secretFrame;
    }

    /**
     * 使用并行化处理解密每一帧
     * @param encryptedFrames 加密的视频帧列表
     * @return 解密后的帧列表
     */
    public static List<Mat> decryptFramesWithLSB(List<Mat> encryptedFrames) {
        List<Mat> secretFrames = new ArrayList<>();

        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        List<Future<Mat>> futures = new ArrayList<>();

        for (int i = 0; i < encryptedFrames.size(); i++) {
            final int frameIndex = i;

            futures.add(executorService.submit(() -> {
                Mat encryptedFrame = encryptedFrames.get(frameIndex);
                return decryptFrameWithLSB(encryptedFrame);
            }));
        }

        for (Future<Mat> future : futures) {
            try {
                secretFrames.add(future.get());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        executorService.shutdown();
        return secretFrames;
    }
}
