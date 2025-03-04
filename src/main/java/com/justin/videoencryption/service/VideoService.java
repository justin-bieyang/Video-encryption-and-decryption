package com.justin.videoencryption.service;

import com.justin.videoencryption.util.*;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;


@Service
public class VideoService {

    static {
        // 加载OpenCV库
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    // 视频加密逻辑
    public byte[] encryptVideos(MultipartFile originalVideo, MultipartFile secretVideo)
            throws IOException {

        // 自定义保存音频的文件夹
        String audioDirectory = "F:\\audios\\";

        // 将原视频文件转换为帧
        List<Mat> originalFrames = VideoUtil.getFrames(originalVideo);

        // 提取原视频的音频
        String originalAudioFileName = "original_audio_";
        String originalAudioType = ".mp3";
        String originalAudioPath = VideoUtil.generateUniqueFileName(audioDirectory,
                originalAudioFileName,
                originalAudioType);

        AudioUtil.extractAudioFromVideo(originalAudioPath);

        // 删除临时目录里的文件
        String customTempDir = "F:\\my_custom_temp_dir";
        FileUtil.deleteFilesInDirectory(customTempDir);

        // 将涉密视频转换为帧
        List<Mat> secretFrames = VideoUtil.getFrames(secretVideo);

        // 提取涉密视频的音频
        String secretAudioFileName = "secret_audio_";
        String secretAudioType = ".mp3";
        String secretAudioPath = VideoUtil.generateUniqueFileName(audioDirectory,
                                                                    secretAudioFileName,
                                                                    secretAudioType);
        AudioUtil.extractAudioFromVideo(secretAudioPath);

        // 删除临时目录里的文件
        FileUtil.deleteFilesInDirectory(customTempDir);

        // 获取开始加密的时间
        LocalDateTime startTime = LocalDateTime.now();

        // 使用并行加密每一帧
        List<Mat> encryptedFrames = EncryptUtil.encryptFramesWithLSB(originalFrames, secretFrames);

        // 判断是否加密成功
        if (encryptedFrames.isEmpty()) {
            System.out.println("没有视频帧数据！");
        } else {
            System.out.println("帧数: " + encryptedFrames.size());
        }

        // 获取加密完成的时间
        LocalDateTime endTime = LocalDateTime.now();

        //计算加密所用时间
        Duration duration = Duration.between(startTime, endTime);
        System.out.println("此次加密所用时间为：" + duration.getSeconds() + "秒");

        // 生成加密后无声视频文件路径
        String encryptedDirectory = "F:\\encrypted_videos\\";
        String encryptedVideoFileName = "encrypted_audio_";
        String encryptedVideoType = ".mp4";
        String outputVideoPath = VideoUtil.generateUniqueFileName(encryptedDirectory,
                                                                    encryptedVideoFileName,
                                                                    encryptedVideoType);

        // 保存加密后的无声视频
        VideoUtil.saveVideo(encryptedFrames, outputVideoPath,
                            encryptedFrames.get(0).rows(), encryptedFrames.get(0).cols());


        // 生成加密后有声视频文件路径
        String finalEncryptedDirectory = "F:\\final_encrypted_videos\\";
        String finalEncryptedVideoFileName = "final_encrypted_audio_";
        String finalEncryptedVideoType = ".mp4";
        String finalOutputVideoPath = VideoUtil.generateUniqueFileName(finalEncryptedDirectory,
                finalEncryptedVideoFileName,
                finalEncryptedVideoType);

        // 保存加密后的有声视频
        AudioUtil.mergeVideoAndAudio(outputVideoPath, originalAudioPath, secretAudioPath, finalOutputVideoPath);

        System.out.println("视频生成成功！");

        // 返回加密后视频的字节数组
        return Files.readAllBytes(Path.of(finalOutputVideoPath));

    }

    // 视频解密逻辑
    public byte[] decryptVideos(MultipartFile encryptedVideo)
            throws IOException {

        // 将加密视频转换为帧
        List<Mat> encryptedVideoFrames = VideoUtil.getFrames(encryptedVideo);

        // 删除临时目录里的文件
        String customTempDir = "F:\\my_custom_temp_dir";
        FileUtil.deleteFilesInDirectory(customTempDir);

        // 开始解密
        // 获取开始解密的时间
        LocalDateTime startTime = LocalDateTime.now();

        List<Mat> decryptFramesWithLSB = DecryptUtil.decryptFramesWithLSB(encryptedVideoFrames);

        //判断是否解密成功
        if (decryptFramesWithLSB.isEmpty()) {
            System.out.println("没有视频帧数据！");
        } else {
            System.out.println("帧数: " + decryptFramesWithLSB.size());
        }

        // 获取解密完成的时间
        LocalDateTime endTime = LocalDateTime.now();

        // 计算解密时间
        Duration duration = Duration.between(startTime, endTime);
        System.out.println("此次解密所用时间为：" + duration.getSeconds() + "秒");

        // 保存解密视频
        // 生成解密后无声视频文件路径
        String encryptedDirectory = "F:\\decrypted_videos\\";
        String encryptedVideoFileName = "decrypted_audio_";
        String encryptedVideoType = ".mp4";
        String outputVideoPath = VideoUtil.generateUniqueFileName(encryptedDirectory,
                encryptedVideoFileName,
                encryptedVideoType);

        // 保存加密后的无声视频
        VideoUtil.saveVideo(decryptFramesWithLSB, outputVideoPath,
                decryptFramesWithLSB.get(0).rows(), decryptFramesWithLSB.get(0).cols());

        System.out.println("视频生成成功！");

        return Files.readAllBytes(Path.of(outputVideoPath));

    }
}
