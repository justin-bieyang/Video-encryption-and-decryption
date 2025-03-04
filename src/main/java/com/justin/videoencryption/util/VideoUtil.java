package com.justin.videoencryption.util;

import nu.pattern.OpenCV;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.VideoWriter;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 视频操作工具类
 * @author 小杜
 * @version 1.0
 * @since 1.0
 */
public class VideoUtil {

    /**
     * 从视频文件提取帧
     * @param videoFile
     * @return List<Mat>，即一个 Mat 对象的列表，其中每个 Mat 对象代表视频中的一帧。
     * @throws IOException
     */
    public static List<Mat> getFrames(MultipartFile videoFile) throws IOException {

        // 设置新的临时目录路径
        String customTempDir = "F:\\my_custom_temp_dir";
        File tempDir = new File(customTempDir);
        if (!tempDir.exists()) {
            tempDir.mkdirs(); // 创建目录
        }
        // 将 MultipartFile 保存到临时文件
        // 因为 OpenCV 的 VideoCapture 类在读取视频文件时需要一个文件路径作为输入，
        // 而 MultipartFile 本身是一个内存中的文件对象，它没有直接的文件路径，因此需要先保存为一个实际存在的临时文件。
        Path tempFile = Files.createTempFile(tempDir.toPath(),"video", ".mp4");
        // 将videoFile保存到临时文件

        // System.out.println("临时文件路径 " + tempFile.toString());
        try {
            videoFile.transferTo(tempFile.toFile());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            throw new RuntimeException(e);
        }

        List<Mat> frames = new ArrayList<>();
        // VideoCapture 是 OpenCV 提供的一个类，用于从视频文件中读取帧
        VideoCapture capture = new VideoCapture(tempFile.toString());

        if (!capture.isOpened()) {
            System.out.println("视频无法打开！");
            return frames;
        }

        // Mat 是 OpenCV 用来表示图像或视频帧的类
        Mat frame = new Mat();
        while (capture.read(frame)) {
            // 使用 Mat.clone 来避免对原始帧的修改
            frames.add(frame.clone());
        }

        capture.release();

        return frames;
    }

    /**
     *将视频帧存储为视频文件
     * @param frames
     * @param outputVideoPath
     * @param rows
     * @param cols
     */
    public static void saveVideo(List<Mat> frames, String outputVideoPath, int rows, int cols) {
        /*
            VideoWriter 是 OpenCV 中用于将帧保存为视频文件的类
            outputVideoPath 是生成的输出视频的路径,该路径包括文件名和扩展名（例如 .mp4 或 .avi）
            'M', 'J', 'P', 'G' 是 fourcc 编码的字符，表示选择 MJPG 编码
            30：这是视频的帧率（Frame Per Second, FPS），即每秒播放多少帧，通常的视频帧率是 30 帧/秒
            new Size(cols, rows)：指定视频的分辨率
            这里 cols 是视频的宽度，rows 是视频的高度
            Size(cols, rows) 代表每一帧图像的大小
        */
        VideoWriter videoWriter = new VideoWriter(outputVideoPath,
                                                    VideoWriter.fourcc('H', '2', '6', '4'),
                                                30,
                                                    new Size(cols, rows));
        System.out.println("开始生成视频");
        for (Mat frame : frames) {
            videoWriter.write(frame);
        }
        videoWriter.release();
    }

    /**
     * 生成加密视频的文件名
     * @return
     */
    public static String generateUniqueFileName(String directory,
                                                String fileName, String fileType) {

        // 使用日期和时间来创建一个更加可读的文件名
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String timestampStr = sdf.format(new Date());

        // 检查文件夹是否存在，如果不存在则创建
        File folder = new File(directory);
        if (!folder.exists()) {
            folder.mkdirs(); // 创建文件夹
        }

        // 组合目录、文件名和扩展名
        return directory + fileName + timestampStr + fileType;
    }

}