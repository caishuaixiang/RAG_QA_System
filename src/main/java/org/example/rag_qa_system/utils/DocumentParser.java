package org.example.rag_qa_system.utils;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.tika.Tika;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.metadata.Metadata;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 文档解析工具类
 */
@Component
public class DocumentParser {

    private final Tika tika = new Tika();

    /**
     * 解析结果类，包含内容和页码映射
     */
    public static class ParseResult {
        private String content;
        private List<PagePosition> pagePositions;

        public ParseResult(String content, List<PagePosition> pagePositions) {
            this.content = content;
            this.pagePositions = pagePositions;
        }

        public String getContent() {
            return content;
        }

        public List<PagePosition> getPagePositions() {
            return pagePositions;
        }
    }

    /**
     * 页码位置信息
     */
    public static class PagePosition {
        private int pageNumber;
        private int startChar;
        private int endChar;

        public PagePosition(int pageNumber, int startChar, int endChar) {
            this.pageNumber = pageNumber;
            this.startChar = startChar;
            this.endChar = endChar;
        }

        public int getPageNumber() {
            return pageNumber;
        }

        public int getStartChar() {
            return startChar;
        }

        public int getEndChar() {
            return endChar;
        }
    }

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
     * 解析PDF文档并提取页码信息
     * @param filePath PDF文件路径
     * @return 解析结果（包含内容和页码映射）
     */
    public ParseResult parsePdfWithPageInfo(String filePath) throws Exception {
        File file = new File(filePath);

        if (!file.exists()) {
            throw new IOException("File not found: " + filePath);
        }

        if (!filePath.toLowerCase().endsWith(".pdf")) {
            // 非PDF文件，返回普通解析结果
            String content = parseDocument(filePath);
            return new ParseResult(content, null);
        }

        List<PagePosition> pagePositions = new ArrayList<>();
        StringBuilder allText = new StringBuilder();

        try (PDDocument document = PDDocument.load(file)) {
            int totalPages = document.getNumberOfPages();

            for (int pageNum = 1; pageNum <= totalPages; pageNum++) {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setStartPage(pageNum);
                stripper.setEndPage(pageNum);

                String pageText = stripper.getText(document);
                int startChar = allText.length();
                allText.append(pageText);
                int endChar = allText.length();

                if (pageText.trim().length() > 0) {
                    pagePositions.add(new PagePosition(pageNum, startChar, endChar));
                }
            }
        }

        return new ParseResult(allText.toString(), pagePositions);
    }

    /**
     * 根据字符位置获取页码
     * @param charIndex 字符位置
     * @param pagePositions 页码位置列表
     * @return 页码（从1开始），如果找不到返回null
     */
    public static Integer getPageNumber(int charIndex, List<PagePosition> pagePositions) {
        if (pagePositions == null || pagePositions.isEmpty()) {
            return null;
        }

        for (PagePosition pos : pagePositions) {
            if (charIndex >= pos.getStartChar() && charIndex < pos.getEndChar()) {
                return pos.getPageNumber();
            }
        }

        // 如果超出最后一页的范围，返回最后一页
        PagePosition lastPage = pagePositions.get(pagePositions.size() - 1);
        if (charIndex >= lastPage.getEndChar()) {
            return lastPage.getPageNumber();
        }

        return null;
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