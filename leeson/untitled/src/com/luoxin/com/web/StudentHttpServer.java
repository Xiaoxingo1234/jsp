package com.luoxin.com.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import com.luoxin.com.controller.StudentController;
import com.luoxin.com.model.PageResult;
import com.luoxin.com.model.Student;
import com.luoxin.com.model.StudentDao;
import com.luoxin.com.view.StudentWebView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * 学生管理：分页列表、添加、修改、删除。
 * 运行 main 后浏览器打开：http://localhost:8080/
 */
public class StudentHttpServer {

    private static final int PORT = 8080;

    public static void main(String[] args) throws Exception {
        StudentDao dao = new StudentDao();
        StudentController controller = new StudentController(dao, null);
        StudentWebView webView = new StudentWebView();

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/", new ListHandler(controller, webView));
        server.createContext("/add", new AddHandler(controller, webView));
        server.createContext("/edit", new EditHandler(controller, webView));
        server.createContext("/update", new UpdateHandler(controller, webView));
        server.createContext("/delete", new DeleteHandler(controller, webView));
        server.setExecutor(null);
        server.start();
        System.out.println("学生管理网页已启动：");
        System.out.println("  列表页 http://localhost:" + PORT + "/");
        System.out.println("  添加页 http://localhost:" + PORT + "/add");
        System.out.println("按 Ctrl+C 可结束（若在 IDE 中运行，请点停止按钮）。");
    }

    /** 列表页：仅分页表格 */
    private static class ListHandler implements HttpHandler {
        private final StudentController controller;
        private final StudentWebView webView;

        ListHandler(StudentController controller, StudentWebView webView) {
            this.controller = controller;
            this.webView = webView;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                exchange.close();
                return;
            }
            Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
            int page = parsePage(query.get("page"), 1);
            String notice = resolveNotice(query);
            try {
                PageResult pageResult = controller.getStudentPage(page);
                String html = webView.renderListPage(pageResult, pageResult.getPage(), notice);
                sendHtml(exchange, 200, html);
            } catch (SQLException e) {
                sendHtml(exchange, 500, webView.renderError(e.getMessage()));
            }
        }
    }

    /** 添加页：仅表单 */
    private static class AddHandler implements HttpHandler {
        private final StudentController controller;
        private final StudentWebView webView;

        AddHandler(StudentController controller, StudentWebView webView) {
            this.controller = controller;
            this.webView = webView;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                handleGet(exchange);
            } else if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                handlePost(exchange);
            } else {
                exchange.sendResponseHeaders(405, -1);
                exchange.close();
            }
        }

        private void handleGet(HttpExchange exchange) throws IOException {
            Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
            String notice = "";
            if (query.containsKey("added")) {
                notice = "添加成功！可继续添加，或返回列表查看。";
            }
            sendHtml(exchange, 200, webView.renderAddPage(notice));
        }

        private void handlePost(HttpExchange exchange) throws IOException {
            Map<String, String> form = readForm(exchange);
            String name = trimToNull(form.get("name"));
            String ageStr = trimToNull(form.get("age"));
            String sex = trimToNull(form.get("sex"));
            if (name == null || ageStr == null || sex == null) {
                sendHtml(exchange, 400, webView.renderError("请填写姓名、年龄和性别"));
                return;
            }
            int age;
            try {
                age = Integer.parseInt(ageStr);
            } catch (NumberFormatException e) {
                sendHtml(exchange, 400, webView.renderError("年龄必须是数字"));
                return;
            }
            if (age < 1 || age > 150) {
                sendHtml(exchange, 400, webView.renderError("年龄应在 1～150 之间"));
                return;
            }
            try {
                controller.addStudent(new Student(name, age, sex));
            } catch (SQLException e) {
                sendHtml(exchange, 500, webView.renderError("保存失败：" + e.getMessage()));
                return;
            }
            redirect(exchange, "/add?added=1");
        }
    }

    private static class EditHandler implements HttpHandler {
        private final StudentController controller;
        private final StudentWebView webView;

        EditHandler(StudentController controller, StudentWebView webView) {
            this.controller = controller;
            this.webView = webView;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                exchange.close();
                return;
            }
            Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
            long id = parseId(query.get("id"));
            int page = parsePage(query.get("page"), 1);
            if (id <= 0) {
                sendHtml(exchange, 400, webView.renderError("无效的学生 id"));
                return;
            }
            try {
                Student student = controller.getStudentById(id);
                if (student == null) {
                    sendHtml(exchange, 404, webView.renderError("未找到该学生"));
                    return;
                }
                sendHtml(exchange, 200, webView.renderEditPage(student, page));
            } catch (SQLException e) {
                sendHtml(exchange, 500, webView.renderError(e.getMessage()));
            }
        }
    }

    private static class UpdateHandler implements HttpHandler {
        private final StudentController controller;
        private final StudentWebView webView;

        UpdateHandler(StudentController controller, StudentWebView webView) {
            this.controller = controller;
            this.webView = webView;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                exchange.close();
                return;
            }
            Map<String, String> form = readForm(exchange);
            long id = parseId(form.get("id"));
            int returnPage = parsePage(form.get("page"), 1);
            String name = trimToNull(form.get("name"));
            String ageStr = trimToNull(form.get("age"));
            String sex = trimToNull(form.get("sex"));
            if (id <= 0 || name == null || ageStr == null || sex == null) {
                sendHtml(exchange, 400, webView.renderError("请完整填写修改信息"));
                return;
            }
            int age;
            try {
                age = Integer.parseInt(ageStr);
            } catch (NumberFormatException e) {
                sendHtml(exchange, 400, webView.renderError("年龄必须是数字"));
                return;
            }
            if (age < 1 || age > 150) {
                sendHtml(exchange, 400, webView.renderError("年龄应在 1～150 之间"));
                return;
            }
            Student student = new Student(name, age, sex);
            student.setId(id);
            try {
                int rows = controller.updateStudent(student);
                if (rows == 0) {
                    sendHtml(exchange, 404, webView.renderError("未找到该学生，无法修改"));
                    return;
                }
            } catch (SQLException e) {
                sendHtml(exchange, 500, webView.renderError("修改失败：" + e.getMessage()));
                return;
            }
            redirect(exchange, "/?page=" + returnPage + "&updated=1");
        }
    }

    private static class DeleteHandler implements HttpHandler {
        private final StudentController controller;
        private final StudentWebView webView;

        DeleteHandler(StudentController controller, StudentWebView webView) {
            this.controller = controller;
            this.webView = webView;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                exchange.close();
                return;
            }
            Map<String, String> form = readForm(exchange);
            long id = parseId(form.get("id"));
            int returnPage = parsePage(form.get("page"), 1);
            if (id <= 0) {
                sendHtml(exchange, 400, webView.renderError("无效的学生 id"));
                return;
            }
            try {
                int rows = controller.deleteStudent(id);
                if (rows == 0) {
                    sendHtml(exchange, 404, webView.renderError("未找到该学生，无法删除"));
                    return;
                }
                PageResult after = controller.getStudentPage(returnPage);
                int page = after.getTotalCount() == 0 ? 1 : after.getPage();
                redirect(exchange, "/?page=" + page + "&deleted=1");
            } catch (SQLException e) {
                sendHtml(exchange, 500, webView.renderError("删除失败：" + e.getMessage()));
            }
        }
    }

    private static String resolveNotice(Map<String, String> query) {
        if (query.containsKey("updated")) {
            return "修改成功！";
        }
        if (query.containsKey("deleted")) {
            return "删除成功！";
        }
        return "";
    }

    private static int parsePage(String s, int defaultPage) {
        if (s == null || s.trim().isEmpty()) {
            return defaultPage;
        }
        try {
            int p = Integer.parseInt(s.trim());
            return p < 1 ? 1 : p;
        } catch (NumberFormatException e) {
            return defaultPage;
        }
    }

    private static long parseId(String s) {
        if (s == null || s.trim().isEmpty()) {
            return -1;
        }
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> map = new HashMap<>();
        if (rawQuery == null || rawQuery.isEmpty()) {
            return map;
        }
        for (String pair : rawQuery.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int eq = pair.indexOf('=');
            String rawKey = eq >= 0 ? pair.substring(0, eq) : pair;
            String rawVal = eq >= 0 ? pair.substring(eq + 1) : "";
            try {
                String key = URLDecoder.decode(rawKey, StandardCharsets.UTF_8.name());
                String val = URLDecoder.decode(rawVal, StandardCharsets.UTF_8.name());
                map.put(key, val);
            } catch (Exception ignored) {
            }
        }
        return map;
    }

    private static Map<String, String> readForm(HttpExchange exchange) throws IOException {
        String body;
        try (InputStream in = exchange.getRequestBody()) {
            body = readUtf8Stream(in);
        }
        return parseFormUrlEncoded(body);
    }

    private static void redirect(HttpExchange exchange, String location) throws IOException {
        exchange.getResponseHeaders().set("Location", location);
        exchange.sendResponseHeaders(303, -1);
        exchange.close();
    }

    private static void sendHtml(HttpExchange exchange, int status, String html) throws IOException {
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static Map<String, String> parseFormUrlEncoded(String body) throws IOException {
        Map<String, String> map = new HashMap<>();
        if (body == null || body.isEmpty()) {
            return map;
        }
        for (String pair : body.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int eq = pair.indexOf('=');
            String rawKey = eq >= 0 ? pair.substring(0, eq) : pair;
            String rawVal = eq >= 0 ? pair.substring(eq + 1) : "";
            String key = URLDecoder.decode(rawKey.replace('+', ' '), StandardCharsets.UTF_8.name());
            String val = URLDecoder.decode(rawVal.replace('+', ' '), StandardCharsets.UTF_8.name());
            map.put(key, val);
        }
        return map;
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String readUtf8Stream(InputStream in) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) != -1) {
            bos.write(buf, 0, n);
        }
        return new String(bos.toByteArray(), StandardCharsets.UTF_8);
    }
}
