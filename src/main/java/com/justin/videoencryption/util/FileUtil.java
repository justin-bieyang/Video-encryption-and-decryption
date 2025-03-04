package com.justin.videoencryption.util;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * 文件工具类
 * @author 小杜
 * @version 1.0
 * @since 1.0
 */
public class FileUtil {

    /**
     * 删除指定文件夹下的文件
     * @param directoryPath
     * @throws IOException
     */
    public static void deleteFilesInDirectory(String directoryPath) throws IOException {
        Path dirPath = Paths.get(directoryPath);

        // 检查目录是否存在
        if (Files.exists(dirPath) && Files.isDirectory(dirPath)) {
            // 使用 walkFileTree 来遍历目录下的所有文件和子目录
            Files.walkFileTree(dirPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    // 删除文件
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    // 如果遇到无法访问的文件，则跳过
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    /**
     * 通过目录拿到里面文件的绝对路径
     * @param directoryPath
     * @return
     * @throws IOException
     */
    public static String getFilePathFromDirectory(String directoryPath) throws IOException {
        Path dirPath = Paths.get(directoryPath);

        // 检查目录是否存在
        if (Files.exists(dirPath) && Files.isDirectory(dirPath)) {
            List<Path> files = new ArrayList<>();

            // 获取目录中的所有文件路径
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath)) {
                for (Path entry : stream) {
                    files.add(entry);
                }

                // 如果目录下只有一个文件
                if (files.size() == 1) {
                    return files.get(0).toAbsolutePath().toString(); // 返回文件的绝对路径
                } else {
                    throw new IOException("Directory does not contain exactly one file.");
                }
            }
        } else {
            throw new IOException("Directory does not exist or is not a directory.");
        }
    }

}
