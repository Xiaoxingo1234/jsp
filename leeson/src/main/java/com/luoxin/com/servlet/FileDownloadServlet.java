package com.luoxin.com.servlet;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 文件下载 Servlet
 * GET /download?name=xxx 下载文件
 */
@WebServlet("/download")
public class FileDownloadServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");

        String fileName = req.getParameter("name");
        if (fileName == null || fileName.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少文件名参数");
            return;
        }

        // 上传目录路径
        String uploadPath = getServletContext().getRealPath("/") + "uploads";
        Path uploadDir = Paths.get(uploadPath).normalize();
        Path filePath = uploadDir.resolve(fileName).normalize();

        // 安全检查：防止路径穿越
        if (!filePath.startsWith(uploadDir)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "非法访问");
            return;
        }

        File file = filePath.toFile();
        if (!file.exists() || !file.isFile()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "文件不存在");
            return;
        }

        // 设置响应头
        String mimeType = Files.probeContentType(filePath);
        if (mimeType == null) mimeType = "application/octet-stream";

        resp.setContentType(mimeType);
        resp.setHeader("Content-Disposition",
            "attachment; filename=\"" + URLEncoder.encode(fileName, "UTF-8") + "\"");
        resp.setContentLength((int) file.length());

        // 读取文件并写入响应（纯 JDK，无第三方依赖）
        try (FileInputStream fis = new FileInputStream(file);
             OutputStream out = resp.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
        }
    }
}
