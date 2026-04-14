package org.example.rag_qa_system.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 文件工具类
 */
@Component
public class FileUtils {

    @Value("${file.upload.path}")
    private String uploadPath;

    /**
     * 保存文件到本地
     * @param file 上传的文件
     * @return 文件保存路径
     * @throws IOException
     */
    public String saveFile(MultipartFile file) throws IOException {
        // 创建上传目录
        File uploadDir = new File(uploadPath);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        // 生成唯一文件名
        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String uniqueFilename = UUID.randomUUID().toString() + fileExtension;

        // 保存文件
        Path filePath = Paths.get(uploadPath, uniqueFilename);
        Files.write(filePath, file.getBytes());

        return filePath.toString();
    }

    /**
     * 获取文件类型
     * @param fileName 文件名
     * @return 文件类型
     */
    public String getFileType(String fileName) {
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * 删除文件
     * @param filePath 文件路径
     * @return 是否删除成功
     */
    public boolean deleteFile(String filePath) {
        File file = new File(filePath);
        return file.delete();
    }
}