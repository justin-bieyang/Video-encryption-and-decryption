package com.justin.videoencryption.util;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 音频工具类
 * @author 小杜
 * @version 1.0
 * @since 1.0
 */
public class AudioUtil {

    private static String ffmpegPath = "E:\\AI工具\\ffmpeg-7.1-essentials_build\\bin\\ffmpeg.exe";

    /**
     * 从视频中提取音频
     * @param audioPath
     * @throws IOException
     */
    public static void extractAudioFromVideo(String audioPath)
            throws IOException {

        // 获取原视频文件的绝对路径
        String originalVideoPath = "F:\\my_custom_temp_dir";
        String originalVideoAbsolutePath = FileUtil.getFilePathFromDirectory(originalVideoPath);

        // FFmpeg 命令：将视频中的音频提取并转换为 MP3
        String command = String.format("%s -i %s -vn -acodec libmp3lame -ab 192k %s",
                ffmpegPath, originalVideoAbsolutePath, audioPath);

        // 使用 ProcessBuilder 启动 FFmpeg 进程
        ProcessBuilder processBuilder = new ProcessBuilder(command.split(" "));
        processBuilder.inheritIO();  // 显示 FFmpeg 输出到控制台

        // 启动进程
        Process process = processBuilder.start();

        try {
            // 等待 FFmpeg 进程执行完毕
            process.waitFor();
            System.out.println("音频提取并转换为 MP3 完成，保存至 " + audioPath);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    /**
     * 合并视频和两段音频（将其中一段静音）
     * @param videoFile
     * @param audioFile1
     * @param audioFile2
     * @param outputFile
     * @return
     */
    public static boolean mergeVideoAndAudio(String videoFile, String audioFile1,
                                             String audioFile2, String outputFile) {

        // 构建 FFmpeg 命令
        String ffmpegCommand = String.format(
                "%s -i %s -i %s -i %s -filter_complex \"[2:a]volume=0[a2];[1:a][a2]amix=inputs=2[a]\" " +
                        "-map 0:v -map \"[a]\" -c:v copy -c:a aac -strict experimental %s",
                ffmpegPath, videoFile, audioFile1, audioFile2, outputFile
        );

        try {
            // 执行 FFmpeg 命令
            Process process = new ProcessBuilder(ffmpegCommand.split(" ")).inheritIO().start();
            int exitCode = process.waitFor(); // 等待 FFmpeg 命令执行完成

            if (exitCode == 0) {
                System.out.println("视频和音频合并成功，输出文件：" + outputFile);
                return true;
            } else {
                System.out.println("FFmpeg 命令执行失败，退出代码：" + exitCode);
                return false;
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

}
