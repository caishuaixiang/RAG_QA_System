package org.example.rag_qa_system.utils;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文本切片工具类
 * 采用"基于正则表达式的段落感知切分 + 重叠窗口"策略
 */
public class TextChunker {

    /**
     * 默认最大切片长度（字符数）
     */
    private static final int DEFAULT_MAX_LENGTH = 500;

    /**
     * 默认重叠长度（字符数）
     */
    private static final int DEFAULT_OVERLAP = 50;

    /**
     * 段落分隔正则（匹配一个或多个连续换行符）
     */
    private static final Pattern PARAGRAPH_PATTERN = Pattern.compile("\\n\\n+|\\r\\n\\r\\n+");

    /**
     * 切片结果类，包含切片内容和位置信息
     */
    public static class ChunkResult {
        private String content;
        private int startIndex;      // 在原文中的起始位置
        private int endIndex;        // 在原文中的结束位置
        private int paragraphIndex;  // 段落序号
        private String lineRange;    // 行号范围
        private String sectionTitle; // 章节标题（如果有）

        public ChunkResult(String content, int startIndex, int endIndex, int paragraphIndex, String lineRange) {
            this.content = content;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.paragraphIndex = paragraphIndex;
            this.lineRange = lineRange;
        }

        public String getContent() { return content; }
        public int getStartIndex() { return startIndex; }
        public int getEndIndex() { return endIndex; }
        public int getParagraphIndex() { return paragraphIndex; }
        public String getLineRange() { return lineRange; }
        public String getSectionTitle() { return sectionTitle; }
        public void setSectionTitle(String sectionTitle) { this.sectionTitle = sectionTitle; }
    }

    /**
     * 按固定长度切片（简单策略，不推荐使用）
     * @param text 文本内容
     * @param chunkSize 切片大小
     * @return 切片列表
     */
    @Deprecated
    public static List<String> splitByLength(String text, int chunkSize) {
        List<String> chunks = new ArrayList<>();
        for (int i = 0; i < text.length(); i += chunkSize) {
            int end = Math.min(i + chunkSize, text.length());
            chunks.add(text.substring(i, end));
        }
        return chunks;
    }

    /**
     * 按段落切片（基于换行符）
     * @param text 文本内容
     * @return 切片列表
     */
    public static List<String> splitByParagraph(String text) {
        List<String> chunks = new ArrayList<>();
        String[] paragraphs = text.split("\\n\\n+");
        for (String paragraph : paragraphs) {
            if (!paragraph.trim().isEmpty()) {
                chunks.add(paragraph.trim());
            }
        }
        return chunks;
    }

    /**
     * 按句子切片
     * @param text 文本内容
     * @return 切片列表
     */
    public static List<String> splitBySentence(String text) {
        List<String> chunks = new ArrayList<>();
        String[] sentences = text.split("[。！？]\\s*");
        for (String sentence : sentences) {
            if (!sentence.trim().isEmpty()) {
                chunks.add(sentence.trim() + "。");
            }
        }
        return chunks;
    }

    /**
     * 智能切片（结合多种策略，无重叠）
     * @param text 文本内容
     * @param maxLength 最大切片大小
     * @return 切片列表
     */
    public static List<String> smartSplit(String text, int maxLength) {
        List<String> chunks = new ArrayList<>();

        // 首先按段落分割
        List<String> paragraphs = splitByParagraph(text);

        for (String paragraph : paragraphs) {
            if (paragraph.length() <= maxLength) {
                chunks.add(paragraph);
            } else {
                // 段落太长，按句子分割
                List<String> sentences = splitBySentence(paragraph);
                StringBuilder currentChunk = new StringBuilder();

                for (String sentence : sentences) {
                    if (currentChunk.length() + sentence.length() <= maxLength) {
                        currentChunk.append(sentence);
                    } else {
                        if (currentChunk.length() > 0) {
                            chunks.add(currentChunk.toString());
                            currentChunk = new StringBuilder();
                        }
                        currentChunk.append(sentence);
                    }
                }

                // 处理单个超长句子（强制分割）
                if (currentChunk.length() > maxLength) {
                    String remaining = currentChunk.toString();
                    while (remaining.length() > maxLength) {
                        chunks.add(remaining.substring(0, maxLength));
                        remaining = remaining.substring(maxLength);
                    }
                    if (remaining.length() > 0) {
                        chunks.add(remaining);
                    }
                } else if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString());
                }
            }
        }

        return chunks;
    }
    /**
     * 增强版智能切片 - 基于正则表达式的段落感知切分 + 重叠窗口
     * @param text 文本内容
     * @return 切片结果列表（包含位置信息）
     */
    public static List<ChunkResult> smartSplitWithOverlap(String text) {
        return smartSplitWithOverlap(text, DEFAULT_MAX_LENGTH, DEFAULT_OVERLAP);
    }

    /**
     * 增强版智能切片 - 基于正则表达式的段落感知切分 + 重叠窗口
     *
     * 设计方案：
     * 1. 以换行符、句号等作为切分边界
     * 2. 设定单个Chunk最大长度为maxLength字符
     * 3. 相邻Chunk设置overlap字符的重叠，保证检索时上下文的连贯性
     *
     * @param text 文本内容
     * @param maxLength 最大切片长度（字符数）
     * @param overlap 相邻切片重叠长度（字符数）
     * @return 切片结果列表（包含位置信息）
     */
    public static List<ChunkResult> smartSplitWithOverlap(String text, int maxLength, int overlap) {
        List<ChunkResult> chunks = new ArrayList<>();

        if (text == null || text.trim().isEmpty()) {
            return chunks;
        }

        // 计算行号映射表（字符位置 -> 行号）
        int[] charToLineMap = buildCharToLineMap(text);

        // 按段落分割文本
        List<ParagraphInfo> paragraphs = splitIntoParagraphs(text);

        StringBuilder currentChunk = new StringBuilder();
        int currentStartIndex = 0;
        int currentParagraphIndex = 0;
        int globalCharIndex = 0;

        for (int pIdx = 0; pIdx < paragraphs.size(); pIdx++) {
            ParagraphInfo paragraph = paragraphs.get(pIdx);
            String paragraphText = paragraph.text;
            int paragraphStart = paragraph.startIndex;

            // 如果当前段落本身就超过最大长度，需要进一步按句子分割
            if (paragraphText.length() > maxLength) {
                // 先保存当前累积的内容
                if (currentChunk.length() > 0) {
                    String chunkContent = currentChunk.toString().trim();
                    if (!chunkContent.isEmpty()) {
                        int startLine = charToLineMap[currentStartIndex] + 1;
                        int endLine = charToLineMap[Math.min(currentStartIndex + chunkContent.length() - 1, charToLineMap.length - 1)] + 1;
                        chunks.add(new ChunkResult(
                                chunkContent,
                                currentStartIndex,
                                currentStartIndex + chunkContent.length(),
                                currentParagraphIndex,
                                startLine + "-" + endLine
                        ));
                    }
                    currentChunk = new StringBuilder();
                }

                // 按句子分割长段落
                List<SentenceInfo> sentences = splitIntoSentences(paragraphText, paragraphStart);
                for (SentenceInfo sentence : sentences) {
                    if (sentence.text.length() > maxLength) {
                        // 单个句子超长，需要强制分割（带重叠）
                        List<ChunkResult> forcedChunks = forceSplitWithOverlap(
                                sentence.text, sentence.startIndex, maxLength, overlap, charToLineMap, pIdx
                        );
                        chunks.addAll(forcedChunks);
                    } else if (currentChunk.length() + sentence.text.length() <= maxLength) {
                        currentChunk.append(sentence.text);
                        if (currentChunk.length() == 0) {
                            currentStartIndex = sentence.startIndex;
                        }
                    } else {
                        // 保存当前块
                        String chunkContent = currentChunk.toString().trim();
                        if (!chunkContent.isEmpty()) {
                            int startLine = charToLineMap[currentStartIndex] + 1;
                            int endLine = charToLineMap[Math.min(currentStartIndex + chunkContent.length() - 1, charToLineMap.length - 1)] + 1;
                            chunks.add(new ChunkResult(
                                    chunkContent,
                                    currentStartIndex,
                                    currentStartIndex + chunkContent.length(),
                                    pIdx,
                                    startLine + "-" + endLine
                            ));
                        }
                        // 添加重叠内容
                        currentChunk = new StringBuilder();
                        if (overlap > 0 && chunkContent.length() > overlap) {
                            String overlapText = chunkContent.substring(chunkContent.length() - overlap);
                            currentChunk.append(overlapText);
                            currentStartIndex = currentStartIndex + chunkContent.length() - overlap;
                        } else {
                            currentStartIndex = sentence.startIndex;
                        }
                        currentChunk.append(sentence.text);
                        currentParagraphIndex = pIdx;
                    }
                }
            } else {
                // 段落长度合适
                if (currentChunk.length() + paragraphText.length() + 1 <= maxLength) {
                    if (currentChunk.length() > 0) {
                        currentChunk.append("\n\n");
                    } else {
                        currentStartIndex = paragraphStart;
                    }
                    currentChunk.append(paragraphText);
                    currentParagraphIndex = pIdx;
                } else {
                    // 保存当前块
                    String chunkContent = currentChunk.toString().trim();
                    if (!chunkContent.isEmpty()) {
                        int startLine = charToLineMap[currentStartIndex] + 1;
                        int endLine = charToLineMap[Math.min(currentStartIndex + chunkContent.length() - 1, charToLineMap.length - 1)] + 1;
                        chunks.add(new ChunkResult(
                                chunkContent,
                                currentStartIndex,
                                currentStartIndex + chunkContent.length(),
                                currentParagraphIndex,
                                startLine + "-" + endLine
                        ));
                    }

                    // 添加重叠内容
                    currentChunk = new StringBuilder();
                    if (overlap > 0 && chunkContent.length() > overlap) {
                        String overlapText = chunkContent.substring(chunkContent.length() - overlap);
                        currentChunk.append(overlapText);
                        currentStartIndex = currentStartIndex + chunkContent.length() - overlap;
                    } else {
                        currentStartIndex = paragraphStart;
                    }

                    if (currentChunk.length() > 0) {
                        currentChunk.append("\n\n");
                    }
                    currentChunk.append(paragraphText);
                    currentParagraphIndex = pIdx;
                }
            }
        }

        // 处理最后一个块
        String chunkContent = currentChunk.toString().trim();
        if (!chunkContent.isEmpty()) {
            int startLine = charToLineMap[currentStartIndex] + 1;
            int endLine = charToLineMap[Math.min(currentStartIndex + chunkContent.length() - 1, charToLineMap.length - 1)] + 1;
            chunks.add(new ChunkResult(
                    chunkContent,
                    currentStartIndex,
                    currentStartIndex + chunkContent.length(),
                    currentParagraphIndex,
                    startLine + "-" + endLine
            ));
        }

        return chunks;
    }
    /**
     * 构建字符位置到行号的映射表
     */
    private static int[] buildCharToLineMap(String text) {
        int[] map = new int[text.length()];
        int lineNum = 0;
        for (int i = 0; i < text.length(); i++) {
            map[i] = lineNum;
            if (i < text.length() - 1 && text.charAt(i) == '\n') {
                if (text.charAt(i + 1) != '\r') {
                    lineNum++;
                }
            } else if (text.charAt(i) == '\r') {
                lineNum++;
            }
        }
        return map;
    }

    /**
     * 段落信息
     */
    private static class ParagraphInfo {
        String text;
        int startIndex;

        ParagraphInfo(String text, int startIndex) {
            this.text = text;
            this.startIndex = startIndex;
        }
    }

    /**
     * 句子信息
     */
    private static class SentenceInfo {
        String text;
        int startIndex;

        SentenceInfo(String text, int startIndex) {
            this.text = text;
            this.startIndex = startIndex;
        }
    }

    /**
     * 按段落分割文本，返回段落信息列表
     */
    private static List<ParagraphInfo> splitIntoParagraphs(String text) {
        List<ParagraphInfo> paragraphs = new ArrayList<>();
        Matcher matcher = PARAGRAPH_PATTERN.matcher(text);

        int lastEnd = 0;
        while (matcher.find()) {
            String paragraph = text.substring(lastEnd, matcher.start()).trim();
            if (!paragraph.isEmpty()) {
                paragraphs.add(new ParagraphInfo(paragraph, lastEnd));
            }
            lastEnd = matcher.end();
        }

        // 处理最后一个段落
        if (lastEnd < text.length()) {
            String paragraph = text.substring(lastEnd).trim();
            if (!paragraph.isEmpty()) {
                paragraphs.add(new ParagraphInfo(paragraph, lastEnd));
            }
        }

        return paragraphs;
    }

    /**
     * 按句子分割文本，返回句子信息列表
     * 使用 Unicode 标准边界检测（支持中文、英文、日文等）
     */
    private static List<SentenceInfo> splitIntoSentences(String text, int offset) {
        List<SentenceInfo> sentences = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return sentences;
        }

        BreakIterator iterator = BreakIterator.getSentenceInstance(Locale.CHINESE);
        iterator.setText(text);

        int start = iterator.first();
        for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
            String sentence = text.substring(start, end).trim();
            if (!sentence.isEmpty()) {
                sentences.add(new SentenceInfo(sentence, offset + start));
            }
        }

        return sentences;
    }

    /**
     * 强制分割超长文本（带重叠）
     */
    private static List<ChunkResult> forceSplitWithOverlap(String text, int startIndex,
                                                           int maxLength, int overlap, int[] charToLineMap, int paragraphIndex) {
        List<ChunkResult> chunks = new ArrayList<>();

        int pos = 0;
        while (pos < text.length()) {
            int end = Math.min(pos + maxLength, text.length());

            // 尝试在边界处找到合适的分割点（优先在标点处分割）
            if (end < text.length()) {
                int lastPunctuation = findLastPunctuation(text, pos, end);
                if (lastPunctuation > pos + maxLength / 2) {
                    end = lastPunctuation + 1;
                }
            }

            String chunkContent = text.substring(pos, end).trim();
            if (!chunkContent.isEmpty()) {
                int absoluteStartIndex = startIndex + pos;
                int startLine = charToLineMap[Math.min(absoluteStartIndex, charToLineMap.length - 1)] + 1;
                int endLine = charToLineMap[Math.min(absoluteStartIndex + chunkContent.length() - 1, charToLineMap.length - 1)] + 1;

                chunks.add(new ChunkResult(
                        chunkContent,
                        absoluteStartIndex,
                        absoluteStartIndex + chunkContent.length(),
                        paragraphIndex,
                        startLine + "-" + endLine
                ));
            }

            // 移动到下一个位置，考虑重叠
            pos = end - overlap;
            if (pos <= chunks.get(chunks.size() - 1).getStartIndex() - startIndex) {
                pos = end; // 避免无限循环
            }
        }

        return chunks;
    }

    /**
     * 在指定范围内查找最后一个标点符号位置
     */
    private static int findLastPunctuation(String text, int start, int end) {
        for (int i = end - 1; i >= start; i--) {
            char c = text.charAt(i);
            if (c == '。' || c == '！' || c == '？' || c == '.' || c == '!' || c == '?' ||
                    c == '；' || c == ';' || c == '\n' || c == '，' || c == ',') {
                return i;
            }
        }
        return end;
    }

    /**
     * 提取章节标题（基于常见的章节模式）
     * @param text 文档文本
     * @return 章节信息列表（章节标题 -> 起始位置）
     */
    public static List<SectionInfo> extractSections(String text) {
        List<SectionInfo> sections = new ArrayList<>();

        // 匹配常见的章节标题模式
        // 支持格式：第X章、第X节、一、二、三、1.、2.、Chapter X、Section X等
        Pattern sectionPattern = Pattern.compile(
                "(第[一二三四五六七八九十百千万零\\d]+[章节篇部][^\\n]*)" +  // 中文章节
                        "|([一二三四五六七八九十]+、[^\\n]*)" +                        // 中文数字标题
                        "|((\\d+\\.)+\\s*[^\\n]*)" +                                  // 数字编号标题
                        "|(Chapter\\s+\\d+[^\\n]*)" +                                 // 英文Chapter
                        "|(Section\\s+\\d+[^\\n]*)" +                                 // 英文Section
                        "|(#[^\\n]+)" +                                               // Markdown标题
                        "|(={3,}|-{3,}|~{3,})" +                                       // Markdown分隔线
                        "|([A-Za-z\u4e00-\u9fa5][A-Za-z0-9\u4e00-\u9fa5\\s]{5,50})" +  // 独立标题行（中英文，5-50字符）
                        "|(研究生手册|学生手册|本科生手册|博士生手册)" +               // 常见手册名称
                        "|([\\(（][^\\n]{5,30}[\\)）])",                               // 括号内的副标题
                Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
        );

        Matcher matcher = sectionPattern.matcher(text);
        while (matcher.find()) {
            String title = matcher.group().trim();
            if (!title.isEmpty() && title.length() < 200) {  // 标题长度限制
                sections.add(new SectionInfo(title, matcher.start()));
            }
        }

        // 额外提取：识别独立成行且长度适中的标题（可能是文档标题或章节标题）
        extractStandaloneTitles(text, sections);

        return sections;
    }

    /**
     * 提取独立成行的标题
     * 识别规则：
     * 1. 单独一行
     * 2. 长度在3-50字符之间
     * 3. 不以标点符号结尾（除顿号、书名号）
     * 4. 包含关键词或格式特征
     */
    private static void extractStandaloneTitles(String text, List<SectionInfo> sections) {
        String[] lines = text.split("\\r?\\n");
        int currentPos = 0;

        // 关键词列表（用于识别标题）
        String[] titleKeywords = {
                "制度", "规定", "办法", "条例", "守则", "手册", "指南", "须知",
                "章程", "细则", "意见", "通知", "决定", "方案", "措施",
                "第一章", "第二章", "第三章", "第四章", "第五章", "第六章", "第七章", "第八章", "第九章", "第十章",
                "第一节", "第二节", "第三节", "第四节", "第五节",
                "附件", "附录", "附则"
        };

        for (String line : lines) {
            String trimmedLine = line.trim();
            int lineStart = text.indexOf(trimmedLine, currentPos);

            // 检查是否是独立标题行
            if (isStandaloneTitle(trimmedLine, titleKeywords)) {
                // 检查是否已经存在相近位置的章节
                boolean exists = false;
                for (SectionInfo section : sections) {
                    if (Math.abs(section.getStartIndex() - lineStart) < 50) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    sections.add(new SectionInfo(trimmedLine, lineStart));
                }
            }

            currentPos = lineStart + line.length();
        }
    }

    /**
     * 判断是否是独立标题行
     */
    private static boolean isStandaloneTitle(String line, String[] titleKeywords) {
        if (line == null || line.isEmpty()) {
            return false;
        }

        // 长度限制
        if (line.length() < 3 || line.length() > 50) {
            return false;
        }

        // 不以标点符号结尾（除顿号、书名号）
        char lastChar = line.charAt(line.length() - 1);
        if ("。！？，；：、.!?,:;".indexOf(lastChar) >= 0) {
            return false;
        }

        // 检查关键词
        for (String keyword : titleKeywords) {
            if (line.contains(keyword)) {
                return true;
            }
        }

        // 检查是否是数字编号开头（如"1."、"一、"）
        if (line.matches("^[一二三四五六七八九十]+、.*") ||
                line.matches("^\\d+[\\.、．].*") ||
                line.matches("^第[一二三四五六七八九十百千万零\\d]+[章节篇部].*")) {
            return true;
        }

        // 检查是否是手册名称格式（如"西安科技大学研究生请假制度"）
        if (line.matches(".*[大学学院学校].*[制度规定办法条例守则手册指南].*")) {
            return true;
        }

        return false;
    }

    /**
     * 章节信息类
     */
    public static class SectionInfo {
        private String title;
        private int startIndex;

        public SectionInfo(String title, int startIndex) {
            this.title = title;
            this.startIndex = startIndex;
        }

        public String getTitle() { return title; }
        public int getStartIndex() { return startIndex; }
    }
}