package org.example.rag_qa_system.controller;

import org.apache.tika.Tika;
import org.example.rag_qa_system.entity.Document;
import org.example.rag_qa_system.service.DocumentService;
import org.example.rag_qa_system.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 文档预览控制器
 */
@RestController
@RequestMapping("/api/document")
public class DocumentPreviewController {

    @Autowired
    private DocumentService documentService;

    @Value("${file.upload.path:./upload}")
    private String uploadPath;

    private final Tika tika = new Tika();

    /**
     * 预览文档（返回文件流）
     */
    @GetMapping("/{id}/preview")
    public ResponseEntity<Resource> previewDocument(@PathVariable Long id) {
        try {
            Document document = documentService.getDocumentById(id);
            if (document == null || document.getFilePath() == null) {
                return ResponseEntity.notFound().build();
            }

            File file = new File(document.getFilePath());
            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(file);
            String contentType = getContentType(document.getType());
            String fileName = URLEncoder.encode(document.getOriginalName(), StandardCharsets.UTF_8);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 下载文档
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long id) {
        try {
            Document document = documentService.getDocumentById(id);
            if (document == null || document.getFilePath() == null) {
                return ResponseEntity.notFound().build();
            }

            File file = new File(document.getFilePath());
            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(file);
            String contentType = getContentType(document.getType());
            String fileName = URLEncoder.encode(document.getOriginalName(), StandardCharsets.UTF_8);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取文档预览信息（用于前端预览组件）
     */
    @GetMapping("/{id}/preview-info")
    public Result getPreviewInfo(@PathVariable Long id) {
        try {
            Document document = documentService.getDocumentById(id);
            if (document == null) {
                return Result.error("文档不存在");
            }

            Map<String, Object> previewInfo = new HashMap<>();
            previewInfo.put("id", document.getId());
            previewInfo.put("name", document.getName());
            previewInfo.put("originalName", document.getOriginalName());
            previewInfo.put("type", document.getType());
            previewInfo.put("size", document.getSize());
            previewInfo.put("previewUrl", "/api/document/" + id + "/preview");
            previewInfo.put("downloadUrl", "/api/document/" + id + "/download");

            // 根据文件类型判断预览方式
            String previewType = getPreviewType(document.getType());
            previewInfo.put("previewType", previewType);

            // 如果是文本类型，直接返回内容
            if ("text".equals(previewType) && document.getContent() != null) {
                previewInfo.put("content", document.getContent());
            }

            return Result.success(previewInfo);
        } catch (Exception e) {
            return Result.error("获取预览信息失败: " + e.getMessage());
        }
    }
    /**
     * 获取文档Base64编码（用于前端直接预览）
     */
    @GetMapping("/{id}/base64")
    public Result getDocumentBase64(@PathVariable Long id) {
        try {
            Document document = documentService.getDocumentById(id);
            if (document == null || document.getFilePath() == null) {
                return Result.error("文档不存在");
            }

            File file = new File(document.getFilePath());
            if (!file.exists()) {
                return Result.error("文件不存在");
            }

            // 限制文件大小（10MB以内才返回Base64）
            if (file.length() > 10 * 1024 * 1024) {
                return Result.error("文件过大，请使用下载方式查看");
            }

            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] bytes = new byte[(int) file.length()];
                fis.read(bytes);
                String base64 = Base64.getEncoder().encodeToString(bytes);

                Map<String, Object> result = new HashMap<>();
                result.put("base64", base64);
                result.put("mimeType", getContentType(document.getType()));
                result.put("fileName", document.getOriginalName());

                return Result.success(result);
            }
        } catch (IOException e) {
            return Result.error("读取文件失败: " + e.getMessage());
        }
    }

    /**
     * 获取文件内容类型
     */
    private String getContentType(String fileType) {
        if (fileType == null) {
            return "application/octet-stream";
        }

        switch (fileType.toLowerCase()) {
            case "pdf":
                return "application/pdf";
            case "doc":
                return "application/msword";
            case "docx":
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls":
                return "application/vnd.ms-excel";
            case "xlsx":
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "ppt":
                return "application/vnd.ms-powerpoint";
            case "pptx":
                return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "txt":
                return "text/plain";
            case "html":
            case "htm":
                return "text/html";
            case "md":
                return "text/markdown";
            case "json":
                return "application/json";
            case "xml":
                return "application/xml";
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            default:
                return "application/octet-stream";
        }
    }

    /**
     * 获取预览类型
     */
    private String getPreviewType(String fileType) {
        if (fileType == null) {
            return "download";
        }

        switch (fileType.toLowerCase()) {
            case "pdf":
                return "pdf";
            case "txt":
            case "md":
            case "json":
            case "xml":
            case "html":
            case "htm":
            case "log":
                return "text";
            case "jpg":
            case "jpeg":
            case "png":
            case "gif":
            case "bmp":
            case "webp":
                return "image";
            case "doc":
            case "docx":
            case "xls":
            case "xlsx":
            case "ppt":
            case "pptx":
                return "office";
            default:
                return "download";
        }
    }
}