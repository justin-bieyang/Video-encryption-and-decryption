package com.justin.videoencryption.controller;

import com.justin.videoencryption.service.VideoService;

import jakarta.annotation.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api")
public class VideoController {

    @Resource
    private VideoService videoService;

    @PostMapping("/encrypt")
    public ResponseEntity<byte[]> encryptVideo(
            @RequestParam("originalVideo") MultipartFile originalVideo,
            @RequestParam("secretVideo") MultipartFile secretVideo) {

        try {
            byte[] encryptedVideos = videoService.encryptVideos(originalVideo, secretVideo);

            // 设置响应头，指定返回的视频文件类型
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.valueOf("video/mp4")); // 设置视频类型
            headers.setContentDispositionFormData("attachment", "encrypted_video.mp4"); // 文件名

            // 返回加密视频的字节数据
            return new ResponseEntity<>(encryptedVideos, headers, HttpStatus.OK);
        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/decrypt")
    public ResponseEntity<byte[]> decryptVideo(
            @RequestParam("encryptedVideo") MultipartFile encryptedVideo) {

        try {
            byte[] decryptedVideos = videoService.decryptVideos(encryptedVideo);

            // 设置响应头，指定返回的视频文件类型
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.valueOf("video/mp4")); // 设置视频类型
            headers.setContentDispositionFormData("attachment", "encrypted_video.mp4"); // 文件名

            // 返回加密视频的字节数据
            return new ResponseEntity<>(decryptedVideos, headers, HttpStatus.OK);
        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
