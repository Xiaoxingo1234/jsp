package com.luoxin.com.web;

import com.sun.net.httpserver.*;
import com.luoxin.com.controller.StudentController;
import com.luoxin.com.model.*;
import com.luoxin.com.view.StudentWebView;

import org.apache.commons.fileupload.*;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.io.*;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.SQLException;
import java.util.*;

public class StudentHttpServer {

    private static final int PORT = 8080;
    private static final String UPLOAD_DIR = "uploads";
    private static final String UTF8 = StandardCharsets.UTF_8.name();

    public static void main(String[] args) throws Exception {
        Files.createDirectories(Paths.get(UPLOAD_DIR));
        StudentDao dao = new StudentDao();
        StudentController ctrl = new StudentController(dao, null);
        StudentWebView view = new StudentWebView();

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/",       new ListHandler(ctrl, view));
        server.createContext("/add",    new AddHandler(ctrl, view));
        server.createContext("/edit",   new EditHandler(ctrl, view));
        server.createContext("/update", new UpdateHandler(ctrl, view));
        server.createContext("/delete", new DeleteHandler(ctrl, view));
        server.createContext("/upload", new UploadHandler(view));
        server.createContext("/files",  new FileDownloadHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("学生管理网页已启动：");
        System.out.println("  列表页    http://localhost:" + PORT + "/");
        System.out.println("  添加页    http://localhost:" + PORT + "/add");
        System.out.println("  文件上传  http://localhost:" + PORT + "/upload");
        System.out.println("按 Ctrl+C 可结束（若在 IDE 中运行，请点停止按钮）。");
    }

    // ===================== 工具方法 =====================

    /** 发送 HTML 响应 */
    private static void sendHtml(HttpExchange ex, int code, String html) throws IOException {
        byte[] b = html.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        ex.sendResponseHeaders(code, b.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(b); }
    }

    /** 303 重定向 */
    private static void redirect(HttpExchange ex, String to) throws IOException {
        ex.getResponseHeaders().set("Location", to);
        ex.sendResponseHeaders(303, -1);
        ex.close();
    }

    /** 只允许指定方法，否则返回 405 */
    private static boolean requireMethod(HttpExchange ex, String method) throws IOException {
        if (method.equalsIgnoreCase(ex.getRequestMethod())) return true;
        ex.sendResponseHeaders(405, -1);
        ex.close();
        return false;
    }

    /** 解析 URL 查询字符串（GET 参数或 POST body，统一处理） */
    private static Map<String, String> parseParams(String raw, boolean isFormBody) {
        Map<String, String> m = new HashMap<>();
        if (raw == null || raw.isEmpty()) return m;
        for (String p : raw.split("&")) {
            if (p.isEmpty()) continue;
            int i = p.indexOf('=');
            try {
                String k = URLDecoder.decode(i >= 0 ? p.substring(0, i) : p, UTF8);
                String v = URLDecoder.decode(i >= 0 ? p.substring(i + 1) : "", UTF8);
                if (isFormBody) { k = k.replace('+', ' '); v = v.replace('+', ' '); }
                m.put(k, v);
            } catch (Exception ignored) {}
        }
        return m;
    }

    /** 解析 URL 查询参数 */
    private static Map<String, String> parseQuery(String q) { return parseParams(q, false); }

    /** 读取 POST 表单体并解析 */
    private static Map<String, String> readForm(HttpExchange ex) throws IOException {
        return parseParams(IOUtils.toString(ex.getRequestBody(), StandardCharsets.UTF_8), true);
    }

    private static int parseInt(String s, int def) {
        if (s == null || s.trim().isEmpty()) return def;
        try { int v = Integer.parseInt(s.trim()); return v < 1 ? 1 : v; }
        catch (NumberFormatException e) { return def; }
    }

    private static long parseId(String s) {
        if (s == null || s.trim().isEmpty()) return -1;
        try { return Long.parseLong(s.trim()); } catch (NumberFormatException e) { return -1; }
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    /** 解析并校验年龄 */
    private static int parseAge(String s, HttpExchange ex, StudentWebView view) throws IOException {
        int age = parseInt(s, -1);
        if (age < 1) { sendHtml(ex, 400, view.renderError("年龄必须是 1～150 之间的数字")); return -1; }
        if (age > 150) { sendHtml(ex, 400, view.renderError("年龄应在 1～150 之间")); return -1; }
        return age;
    }

    // ===================== multipart 解析 =====================

    /**
     * 解析 multipart/form-data 请求，返回 [字段 Map, 文件 Map(filename -> 原始文件名)]
     */
    private static Object[] parseMultipart(HttpExchange ex) throws IOException {
        Map<String, String> fields = new HashMap<>();
        Map<String, String> files = new HashMap<>(); // fieldName -> originalFileName
        try {
            String ct = ex.getRequestHeaders().getFirst("Content-Type");
            byte[] body = IOUtils.toByteArray(ex.getRequestBody());

            DiskFileItemFactory factory = new DiskFileItemFactory();
            factory.setSizeThreshold(1024 * 1024);
            factory.setRepository(new File(System.getProperty("java.io.tmpdir")));

            FileUpload upload = new FileUpload(factory);
            upload.setSizeMax(50 * 1024 * 1024);
            upload.setFileSizeMax(10 * 1024 * 1024);

            for (FileItem item : upload.parseRequest(new JdkReqCtx(ct, body))) {
                if (item.isFormField()) {
                    fields.put(item.getFieldName(), item.getString(UTF8));
                } else {
                    String originalName = item.getName();
                    if (originalName != null && !originalName.isEmpty()) {
                        // 去掉路径前缀
                        originalName = new File(originalName).getName();
                        String savedName = System.currentTimeMillis() + "_" + originalName;
                        FileUtils.copyInputStreamToFile(item.getInputStream(),
                                Paths.get(UPLOAD_DIR, savedName).toFile());
                        files.put(item.getFieldName(), savedName);
                    }
                }
            }
        } catch (FileUploadException e) {
            throw new IOException("文件上传解析失败", e);
        }
        return new Object[]{fields, files};
    }

    // ===================== 处理器 =====================

    /** 通用处理器基类 */
    private static abstract class BaseHandler implements HttpHandler {
        protected final StudentController ctrl;
        protected final StudentWebView view;
        BaseHandler(StudentController ctrl, StudentWebView view) { this.ctrl = ctrl; this.view = view; }
    }

    /** GET / — 学生列表（分页） */
    private static class ListHandler extends BaseHandler {
        ListHandler(StudentController c, StudentWebView v) { super(c, v); }
        public void handle(HttpExchange ex) throws IOException {
            if (!requireMethod(ex, "GET")) return;
            Map<String, String> q = parseQuery(ex.getRequestURI().getRawQuery());
            String notice = q.containsKey("updated") ? "修改成功！" : q.containsKey("deleted") ? "删除成功！" : "";
            try {
                PageResult pr = ctrl.getStudentPage(parseInt(q.get("page"), 1));
                sendHtml(ex, 200, view.renderListPage(pr, pr.getPage(), notice));
            } catch (SQLException e) { sendHtml(ex, 500, view.renderError(e.getMessage())); }
        }
    }

    /** GET/POST /add — 添加学生（multipart 表单，支持照片上传） */
    private static class AddHandler extends BaseHandler {
        AddHandler(StudentController c, StudentWebView v) { super(c, v); }
        public void handle(HttpExchange ex) throws IOException {
            if ("GET".equalsIgnoreCase(ex.getRequestMethod())) {
                Map<String, String> q = parseQuery(ex.getRequestURI().getRawQuery());
                sendHtml(ex, 200, view.renderAddPage(q.containsKey("added") ? "添加成功！" : ""));
            } else if ("POST".equalsIgnoreCase(ex.getRequestMethod())) {
                handlePost(ex);
            } else { ex.sendResponseHeaders(405, -1); ex.close(); }
        }
        private void handlePost(HttpExchange ex) throws IOException {
            String ct = ex.getRequestHeaders().getFirst("Content-Type");
            if (ct == null || !ct.startsWith("multipart/form-data")) {
                sendHtml(ex, 400, view.renderError("请求格式不正确")); return;
            }
            Object[] parsed = parseMultipart(ex);
            @SuppressWarnings("unchecked")
            Map<String, String> f = (Map<String, String>) parsed[0];
            @SuppressWarnings("unchecked")
            Map<String, String> files = (Map<String, String>) parsed[1];

            String name = trimToNull(f.get("name")), sex = trimToNull(f.get("sex"));
            int age = parseAge(f.get("age"), ex, view);
            if (name == null || sex == null || age < 0) return;
            Student student = new Student(name, age, sex);
            // 如果有上传照片，设置文件名
            if (files.containsKey("photo")) {
                student.setPhoto(files.get("photo"));
            }
            try {
                ctrl.addStudent(student);
                redirect(ex, "/add?added=1");
            } catch (SQLException e) { sendHtml(ex, 500, view.renderError("保存失败：" + e.getMessage())); }
        }
    }

    /** GET /edit — 编辑页面 */
    private static class EditHandler extends BaseHandler {
        EditHandler(StudentController c, StudentWebView v) { super(c, v); }
        public void handle(HttpExchange ex) throws IOException {
            if (!requireMethod(ex, "GET")) return;
            Map<String, String> q = parseQuery(ex.getRequestURI().getRawQuery());
            long id = parseId(q.get("id"));
            if (id <= 0) { sendHtml(ex, 400, view.renderError("无效的学生 id")); return; }
            try {
                Student s = ctrl.getStudentById(id);
                sendHtml(ex, s == null ? 404 : 200, s == null ? view.renderError("未找到该学生") : view.renderEditPage(s, parseInt(q.get("page"), 1)));
            } catch (SQLException e) { sendHtml(ex, 500, view.renderError(e.getMessage())); }
        }
    }

    /** POST /update — 更新学生（multipart 表单，支持照片上传） */
    private static class UpdateHandler extends BaseHandler {
        UpdateHandler(StudentController c, StudentWebView v) { super(c, v); }
        public void handle(HttpExchange ex) throws IOException {
            if (!requireMethod(ex, "POST")) return;
            String ct = ex.getRequestHeaders().getFirst("Content-Type");
            if (ct == null || !ct.startsWith("multipart/form-data")) {
                sendHtml(ex, 400, view.renderError("请求格式不正确")); return;
            }
            Object[] parsed = parseMultipart(ex);
            @SuppressWarnings("unchecked")
            Map<String, String> f = (Map<String, String>) parsed[0];
            @SuppressWarnings("unchecked")
            Map<String, String> files = (Map<String, String>) parsed[1];

            long id = parseId(f.get("id"));
            int page = parseInt(f.get("page"), 1);
            String name = trimToNull(f.get("name")), sex = trimToNull(f.get("sex"));
            int age = parseAge(f.get("age"), ex, view);
            if (id <= 0 || name == null || sex == null || age < 0) return;

            Student s = new Student(name, age, sex); s.setId(id);
            // 如果有上传新照片，设置文件名
            if (files.containsKey("photo")) {
                s.setPhoto(files.get("photo"));
            } else {
                // 保留旧照片（如果数据库有的话）
                try {
                    Student old = ctrl.getStudentById(id);
                    if (old != null && old.getPhoto() != null && !old.getPhoto().isEmpty()) {
                        s.setPhoto(old.getPhoto());
                    }
                } catch (SQLException ignored) {}
            }
            try {
                if (ctrl.updateStudent(s) == 0) { sendHtml(ex, 404, view.renderError("未找到该学生")); return; }
                redirect(ex, "/?page=" + page + "&updated=1");
            } catch (SQLException e) { sendHtml(ex, 500, view.renderError("修改失败：" + e.getMessage())); }
        }
    }

    /** POST /delete — 删除学生 */
    private static class DeleteHandler extends BaseHandler {
        DeleteHandler(StudentController c, StudentWebView v) { super(c, v); }
        public void handle(HttpExchange ex) throws IOException {
            if (!requireMethod(ex, "POST")) return;
            Map<String, String> f = readForm(ex);
            long id = parseId(f.get("id"));
            int page = parseInt(f.get("page"), 1);
            if (id <= 0) { sendHtml(ex, 400, view.renderError("无效的学生 id")); return; }
            try {
                if (ctrl.deleteStudent(id) == 0) { sendHtml(ex, 404, view.renderError("未找到该学生")); return; }
                PageResult pr = ctrl.getStudentPage(page);
                redirect(ex, "/?page=" + (pr.getTotalCount() == 0 ? 1 : pr.getPage()) + "&deleted=1");
            } catch (SQLException e) { sendHtml(ex, 500, view.renderError("删除失败：" + e.getMessage())); }
        }
    }

    // ===================== 文件上传/下载 =====================

    /** GET/POST /upload — 文件上传 */
    private static class UploadHandler implements HttpHandler {
        private final StudentWebView view;
        UploadHandler(StudentWebView v) { this.view = v; }
        public void handle(HttpExchange ex) throws IOException {
            if ("GET".equalsIgnoreCase(ex.getRequestMethod())) {
                Map<String, String> q = parseQuery(ex.getRequestURI().getRawQuery());
                String notice = q.containsKey("ok") ? "文件上传成功！" : q.containsKey("error") ? "上传失败：" + q.getOrDefault("msg", "未知错误") : "";
                sendHtml(ex, 200, view.renderUploadPage(notice, listFiles()));
            } else if ("POST".equalsIgnoreCase(ex.getRequestMethod())) {
                handleUpload(ex);
            } else { ex.sendResponseHeaders(405, -1); ex.close(); }
        }
        private void handleUpload(HttpExchange ex) throws IOException {
            String ct = ex.getRequestHeaders().getFirst("Content-Type");
            if (ct == null || !ct.startsWith("multipart/form-data")) {
                redirect(ex, "/upload?error=1&msg=" + URLEncoder.encode("请求格式不正确", UTF8)); return;
            }
            try {
                byte[] body = IOUtils.toByteArray(ex.getRequestBody());
                DiskFileItemFactory factory = new DiskFileItemFactory();
                factory.setSizeThreshold(1024 * 1024);
                factory.setRepository(new File(System.getProperty("java.io.tmpdir")));
                FileUpload upload = new FileUpload(factory);
                upload.setSizeMax(50 * 1024 * 1024);
                upload.setFileSizeMax(10 * 1024 * 1024);
                for (FileItem item : upload.parseRequest(new JdkReqCtx(ct, body))) {
                    if (!item.isFormField() && item.getName() != null && !item.getName().isEmpty()) {
                        String name = System.currentTimeMillis() + "_" + new File(item.getName()).getName();
                        FileUtils.copyInputStreamToFile(item.getInputStream(), Paths.get(UPLOAD_DIR, name).toFile());
                    }
                }
                redirect(ex, "/upload?ok=1");
            } catch (FileUploadException e) {
                redirect(ex, "/upload?error=1&msg=" + URLEncoder.encode("上传失败：" + e.getMessage(), UTF8));
            }
        }
    }

    /** GET /files?name=xxx — 文件下载（支持直接显示图片） */
    private static class FileDownloadHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            if (!requireMethod(ex, "GET")) return;
            String name = parseQuery(ex.getRequestURI().getRawQuery()).get("name");
            if (name == null || name.isEmpty()) { ex.sendResponseHeaders(400, -1); ex.close(); return; }
            Path fp = Paths.get(UPLOAD_DIR, name).normalize();
            if (!fp.startsWith(Paths.get(UPLOAD_DIR).normalize())) { ex.sendResponseHeaders(403, -1); ex.close(); return; }
            File file = fp.toFile();
            if (!file.isFile()) { sendHtml(ex, 404, "<h1>404</h1><p>文件不存在</p><a href='/upload'>返回</a>"); return; }
            byte[] data = FileUtils.readFileToByteArray(file);
            String mime = Files.probeContentType(fp);
            if (mime == null) mime = "application/octet-stream";

            // 对于图片类型，直接内联显示而非下载
            if (mime.startsWith("image/")) {
                ex.getResponseHeaders().set("Content-Type", mime);
                // 不设置 Content-Disposition: attachment，让浏览器直接显示
                ex.sendResponseHeaders(200, data.length);
            } else {
                ex.getResponseHeaders().set("Content-Type", mime);
                ex.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"" + name + "\"");
                ex.sendResponseHeaders(200, data.length);
            }
            try (OutputStream os = ex.getResponseBody()) { os.write(data); }
        }
    }

    /** commons-fileupload RequestContext 适配器 */
    private static class JdkReqCtx implements RequestContext {
        private final String ct; private final byte[] body;
        JdkReqCtx(String ct, byte[] body) { this.ct = ct; this.body = body; }
        public String getCharacterEncoding() {
            if (ct != null) for (String p : ct.split(";")) if (p.trim().toLowerCase().startsWith("charset=")) return p.trim().substring(8);
            return UTF8;
        }
        public int getContentLength() { return body.length; }
        public String getContentType() { return ct; }
        public InputStream getInputStream() { return new ByteArrayInputStream(body); }
    }

    /** 列出已上传文件（按时间倒序） */
    private static List<String> listFiles() {
        File[] ff = new File(UPLOAD_DIR).listFiles();
        if (ff == null) return Collections.emptyList();
        return Arrays.stream(ff).filter(File::isFile).map(File::getName).sorted(Comparator.reverseOrder()).collect(java.util.stream.Collectors.toList());
    }
}
