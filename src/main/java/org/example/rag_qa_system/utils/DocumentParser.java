package org.example.rag_qa_system.utils;

import org.apache.tika.Tika;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * 文档解析工具类
 */
@Component
public class DocumentParser {

    private final Tika tika = new Tika();

    /**
     * 解析文档内容
     * @param filePath 文档路径
     * @return 文档内容
     * @throws IOException
     * @throws org.apache.tika.exception.TikaException
     */
    public String parseDocument(String filePath) throws IOException, org.apache.tika.exception.TikaException {
        File file = new File(filePath);
        try (FileInputStream fis = new FileInputStream(file)) {
            return tika.parseToString(fis);
        }
    }

    /**
     * 获取文件类型
     * @param fileName 文件名
     * @return 文件类型
     */
    public String getFileType(String fileName) {
        int lastDotIndex = fileName.lastIndexOf(".");
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            return "unknown";
        }
        return fileName.substring(lastDotIndex + 1).toLowerCase();
    }
}