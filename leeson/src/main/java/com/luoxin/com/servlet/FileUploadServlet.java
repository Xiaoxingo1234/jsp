package com.luoxin.com.servlet;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * 文件上传 Servlet
 * 使用 Servlet 3.1 原生 Part API，无需任何第三方依赖
 */
@WebServlet("/upload")
@MultipartConfig(
    maxFileSize = 10 * 1024 * 1024,      // 单文件最大 10MB
    maxRequestSize = 50 * 1024 * 1024    // 请求最大 50MB
)
public class FileUploadServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");

        String contentType = req.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("multipart/")) {
            resp.sendRedirect("upload.jsp?error=1&msg=" + URLEncoder.encode("请求格式不正确", "UTF-8"));
            return;
        }

        try {
            // 上传目录
            String uploadPath = getServletContext().getRealPath("/") + "uploads";
            File uploadDir = new File(uploadPath);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            // 遍历所有上传的文件部分
            for (Part part : req.getParts()) {
                String fileName = getSubmittedFileName(part);
                if (fileName != null && !fileName.isEmpty()) {
                    // 去掉路径前缀，只保留文件名
                    fileName = new File(fileName).getName();
                    // 时间戳避免重名
                    String savedName = System.currentTimeMillis() + "_" + fileName;
                    Path destPath = Paths.get(uploadPath, savedName);

                    try (InputStream in = part.getInputStream()) {
                        Files.copy(in, destPath, StandardCopyOption.REPLACE_EXISTING);
                    }

                    System.out.println("文件已保存: " + destPath.toAbsolutePath());
                }
            }

            resp.sendRedirect("upload.jsp?ok=1");
        } catch (Exception e) {
            e.printStackTrace();
            String msg = e.getMessage() != null ? e.getMessage() : "未知错误";
            resp.sendRedirect("upload.jsp?error=1&msg=" + URLEncoder.encode("上传失败：" + msg, "UTF-8"));
        }
    }

    /**
     * 从 Part 的 Content-Disposition 头中提取原始文件名
     */
    private String getSubmittedFileName(Part part) {
        String header = part.getHeader("Content-Disposition");
        if (header == null) return null;

        for (String token : header.split(";")) {
            token = token.trim();
            if (token.startsWith("filename")) {
                String fileName = token.substring(token.indexOf('=') + 1).trim();
                // 去掉引号
                if (fileName.startsWith("\"") && fileName.endsWith("\"")) {
                    fileName = fileName.substring(1, fileName.length() - 1);
                }
                return fileName;
            }
        }
        return null;
    }
}
