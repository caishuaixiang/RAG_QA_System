package org.example.rag_qa_system.utils;

import org.apache.tika.Tika;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.metadata.Metadata;
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
    public String parseDocument(String filePath) throws Exception {
        File file = new File(filePath);

        if (!file.exists()) {
            throw new IOException("File not found: " + filePath);
        }

        String content;

        // 针对 PDF 文件使用专门的解析器
        if (filePath.toLowerCase().endsWith(".pdf")) {
            try (FileInputStream fis = new FileInputStream(file)) {
                PDFParser pdfParser = new PDFParser();
                BodyContentHandler handler = new BodyContentHandler(-1); // -1 表示无限制
                ParseContext context = new ParseContext();
                context.set(org.apache.tika.parser.Parser.class, pdfParser);

                Metadata metadata = new Metadata();
                pdfParser.parse(fis, handler, metadata, context);
                content = handler.toString();
            }
        } else {
            // 其他文件类型使用 Tika 默认解析器
            try (FileInputStream fis = new FileInputStream(file)) {
                content = tika.parseToString(fis);
            }
        }

        if (content == null || content.trim().isEmpty()) {
            System.err.println("WARNING: No content extracted from document: " + filePath);
        }

        return content != null ? content : "";
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