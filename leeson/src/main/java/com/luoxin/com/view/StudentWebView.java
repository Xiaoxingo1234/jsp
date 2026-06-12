package com.luoxin.com.view;

import com.luoxin.com.model.PageResult;
import com.luoxin.com.model.Student;

import java.util.List;

/**
 * 网页视图：列表页与添加页分开显示
 */
public class StudentWebView {

    private static final String STYLE =
            "body{font-family:sans-serif;margin:24px;max-width:960px;}"
                    + ".nav{margin-bottom:20px;padding-bottom:12px;border-bottom:1px solid #ddd;}"
                    + ".nav a{margin-right:16px;text-decoration:none;color:#06c;}"
                    + ".nav a.active{font-weight:bold;color:#333;}"
                    + "table{border-collapse:collapse;width:100%;margin-top:16px;}"
                    + "th,td{border:1px solid #ccc;padding:8px;vertical-align:middle;text-align:center;}"
                    + "th{background:#f0f0f0;}"
                    + "form.panel{background:#fafafa;padding:16px;border:1px solid #ddd;border-radius:8px;max-width:520px;}"
                    + "form.inline{display:inline;margin:0 4px 0 0;}"
                    + "label{display:inline-block;min-width:4em;}"
                    + "input,select{margin:4px 0;padding:6px;}"
                    + "button,.btn-link{padding:6px 12px;margin:2px;cursor:pointer;text-decoration:none;display:inline-block;font-size:14px;}"
                    + ".btn-edit{background:#e8f4ff;color:#06c;border:1px solid #9cf;border-radius:4px;}"
                    + ".btn-del{background:#fff0f0;color:#c00;border:1px solid #f99;border-radius:4px;}"
                    + ".pager{margin:16px 0;}"
                    + ".pager a,.pager span{margin-right:8px;}"
                    + ".pager .current{font-weight:bold;color:#333;}"
                    + ".actions{white-space:nowrap;}"
                    + ".notice{color:#0a0;background:#e8ffe8;padding:8px;border-radius:4px;margin-bottom:12px;}"
                    + ".photo-cell{width:60px;}"
                    + ".photo-cell img{width:50px;height:50px;object-fit:cover;border-radius:4px;border:1px solid #ddd;}"
                    + ".photo-placeholder{width:50px;height:50px;display:inline-block;background:#f5f5f5;border:1px dashed #ccc;border-radius:4px;line-height:50px;color:#bbb;font-size:11px;}"
                    + ".photo-preview{text-align:center;margin:8px 0;}"
                    + ".photo-preview img{max-width:120px;max-height:120px;border-radius:4px;border:1px solid #ddd;}"
                    + "input[type=\"file\"]{font-size:14px;}";

    /** 学生列表页（表格 + 分页） */
    public String renderListPage(PageResult pageResult, int currentPage, String notice) {
        List<Student> students = pageResult.getList();
        StringBuilder rows = new StringBuilder();
        if (students == null || students.isEmpty()) {
            rows.append("<tr><td colspan=\"7\">（暂无数据）</td></tr>");
        } else {
            for (Student s : students) {
                String idCell = s.getId() != null ? String.valueOf(s.getId()) : "-";
                String editUrl = "/edit?id=" + idCell + "&page=" + currentPage;

                // 照片列（点击图片可放大查看）
                String photoHtml;
                if (s.getPhoto() != null && !s.getPhoto().isEmpty()) {
                    String photoUrl = "/files?name=" + escapeHtml(s.getPhoto());
                    photoHtml = "<a href=\"" + photoUrl + "\" target=\"_blank\" title=\"点击查看大图\">"
                            + "<img src=\"" + photoUrl + "\" alt=\"照片\">"
                            + "</a>";
                } else {
                    photoHtml = "<span class=\"photo-placeholder\">无</span>";
                }

                rows.append("<tr><td>")
                        .append(escapeHtml(idCell))
                        .append("</td><td>")
                        .append(escapeHtml(s.getName()))
                        .append("</td><td>")
                        .append(s.getAge())
                        .append("</td><td>")
                        .append(escapeHtml(s.getSex()))
                        .append("</td><td class=\"photo-cell\">")
                        .append(photoHtml)
                        .append("</td><td class=\"actions\">")
                        .append("<a class=\"btn-link btn-edit\" href=\"")
                        .append(editUrl)
                        .append("\">修改</a>")
                        .append("<form class=\"inline\" method=\"post\" action=\"/delete\" ")
                        .append("onsubmit=\"return confirm('确定删除该学生吗？');\">")
                        .append("<input type=\"hidden\" name=\"id\" value=\"")
                        .append(escapeHtml(idCell))
                        .append("\">")
                        .append("<input type=\"hidden\" name=\"page\" value=\"")
                        .append(currentPage)
                        .append("\">")
                        .append("<button type=\"submit\" class=\"btn-del\">删除</button>")
                        .append("</form>")
                        .append("</td></tr>");
            }
        }

        return pageShell("学生列表", "list", notice,
                "<h1>jsp 表 — 学生列表</h1>"
                        + renderPager(pageResult, currentPage)
                        + "<table><thead><tr><th>id</th><th>姓名</th><th>年龄</th><th>性别</th><th>照片</th><th>操作</th></tr></thead>"
                        + "<tbody>" + rows + "</tbody></table>"
                        + renderPager(pageResult, currentPage));
    }

    /** 添加学生页（表单 + 文件上传） */
    public String renderAddPage(String notice) {
        return pageShell("添加学生", "add", notice,
                "<h1>添加学生</h1>"
                        + "<form class=\"panel\" method=\"post\" action=\"/add\" "
                        + "enctype=\"multipart/form-data\" accept-charset=\"UTF-8\">"
                        + "<p><label>姓名</label><input type=\"text\" name=\"name\" required maxlength=\"64\" placeholder=\"姓名\"></p>"
                        + "<p><label>年龄</label><input type=\"number\" name=\"age\" required min=\"1\" max=\"150\" placeholder=\"年龄\"></p>"
                        + "<p><label>性别</label>"
                        + "<select name=\"sex\" required>"
                        + "<option value=\"\">请选择</option>"
                        + "<option value=\"男\">男</option>"
                        + "<option value=\"女\">女</option>"
                        + "</select></p>"
                        + "<p><label>照片</label>"
                        + "<input type=\"file\" name=\"photo\" accept=\"image/*\"></p>"
                        + "<p style=\"color:#999;font-size:13px;\">可选，支持 jpg/png/gif 格式</p>"
                        + "<p><button type=\"submit\">提交保存</button></p>"
                        + "</form>");
    }

    public String renderEditPage(Student student, int returnPage) {
        String idStr = student.getId() != null ? String.valueOf(student.getId()) : "";
        String sex = student.getSex() != null ? student.getSex() : "";
        String selectedMale = "男".equals(sex) ? " selected" : "";
        String selectedFemale = "女".equals(sex) ? " selected" : "";

        // 当前照片预览
        String photoPreview = "";
        if (student.getPhoto() != null && !student.getPhoto().isEmpty()) {
            photoPreview = "<div class=\"photo-preview\">"
                    + "<p>当前照片：</p>"
                    + "<img src=\"/files?name=" + escapeHtml(student.getPhoto()) + "\" alt=\"照片\">"
                    + "</div>";
        }

        return pageShell("修改学生", "", "",
                "<h1>修改学生</h1>"
                        + "<form class=\"panel\" method=\"post\" action=\"/update\" "
                        + "enctype=\"multipart/form-data\" accept-charset=\"UTF-8\">"
                        + "<input type=\"hidden\" name=\"id\" value=\"" + escapeHtml(idStr) + "\">"
                        + "<input type=\"hidden\" name=\"page\" value=\"" + returnPage + "\">"
                        + "<p><label>id</label><input type=\"text\" value=\"" + escapeHtml(idStr) + "\" disabled></p>"
                        + "<p><label>姓名</label><input type=\"text\" name=\"name\" required maxlength=\"64\" value=\""
                        + escapeHtml(student.getName()) + "\"></p>"
                        + "<p><label>年龄</label><input type=\"number\" name=\"age\" required min=\"1\" max=\"150\" value=\""
                        + student.getAge() + "\"></p>"
                        + "<p><label>性别</label>"
                        + "<select name=\"sex\" required>"
                        + "<option value=\"男\"" + selectedMale + ">男</option>"
                        + "<option value=\"女\"" + selectedFemale + ">女</option>"
                        + "</select></p>"
                        + photoPreview
                        + "<p><label>照片</label>"
                        + "<input type=\"file\" name=\"photo\" accept=\"image/*\"></p>"
                        + "<p style=\"color:#999;font-size:13px;\">选择新照片将替换当前照片，留空则不修改</p>"
                        + "<p><button type=\"submit\">保存修改</button> "
                        + "<a href=\"/?page=" + returnPage + "\">取消</a></p>"
                        + "</form>");
    }

    private String pageShell(String title, String activeNav, String notice, String body) {
        String noticeBlock = "";
        if (notice != null && !notice.isEmpty()) {
            noticeBlock = "<p class=\"notice\">" + escapeHtml(notice) + "</p>";
        }
        String nav = renderNav(activeNav);
        return "<!DOCTYPE html>\n"
                + "<html lang=\"zh-CN\"><head><meta charset=\"UTF-8\">"
                + "<title>" + escapeHtml(title) + "</title>"
                + "<style>" + STYLE + "</style>"
                + "</head><body>"
                + nav
                + noticeBlock
                + body
                + "</body></html>";
    }

    private String renderNav(String active) {
        String listClass = "list".equals(active) ? " class=\"active\"" : "";
        String addClass = "add".equals(active) ? " class=\"active\"" : "";
        String uploadClass = "upload".equals(active) ? " class=\"active\"" : "";
        return "<nav class=\"nav\">"
                + "<a href=\"/\"" + listClass + ">学生列表</a>"
                + "<a href=\"/add\"" + addClass + ">添加学生</a>"
                + "<a href=\"/upload\"" + uploadClass + ">文件上传</a>"
                + "</nav>";
    }

    /**
     * 文件上传页面
     */
    public String renderUploadPage(String notice, List<String> uploadedFiles) {
        StringBuilder fileListHtml = new StringBuilder();
        if (uploadedFiles == null || uploadedFiles.isEmpty()) {
            fileListHtml.append("<p>（暂无已上传的文件）</p>");
        } else {
            fileListHtml.append("<ul>");
            for (String f : uploadedFiles) {
                String displayName = f;
                int idx = f.indexOf('_');
                if (idx > 0 && idx < f.length() - 1) {
                    displayName = f.substring(idx + 1);
                }
                fileListHtml.append("<li>")
                        .append("<a href=\"/files?name=").append(escapeHtml(f)).append("\">")
                        .append(escapeHtml(displayName))
                        .append("</a>")
                        .append(" <small style=\"color:#999;\">(")
                        .append(escapeHtml(f))
                        .append(")</small></li>");
            }
            fileListHtml.append("</ul>");
        }
        return pageShell("文件上传", "upload", notice,
                "<h1>文件上传</h1>"
                        + "<form class=\"panel\" method=\"post\" action=\"/upload\" "
                        + "enctype=\"multipart/form-data\" accept-charset=\"UTF-8\">"
                        + "<p><label>选择文件</label>"
                        + "<input type=\"file\" name=\"file\" required></p>"
                        + "<p style=\"color:#999;font-size:13px;\">支持任意类型文件，单文件最大 10MB</p>"
                        + "<p><button type=\"submit\">上传文件</button></p>"
                        + "</form>"
                        + "<hr style=\"margin:24px 0;border:0;border-top:1px solid #ddd;\">"
                        + "<h2>已上传的文件</h2>"
                        + fileListHtml);
    }

    private String renderPager(PageResult pageResult, int currentPage) {
        int totalPages = pageResult.getTotalPages();
        int totalCount = pageResult.getTotalCount();
        if (totalCount == 0) {
            return "<p class=\"pager\">共 0 条记录</p>";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<p class=\"pager\">共 ").append(totalCount).append(" 条，每页 ")
                .append(pageResult.getPageSize()).append(" 条，第 ")
                .append(currentPage).append(" / ").append(totalPages).append(" 页 ");
        if (currentPage > 1) {
            sb.append("<a href=\"/?page=1\">首页</a> ");
            sb.append("<a href=\"/?page=").append(currentPage - 1).append("\">上一页</a> ");
        }
        for (int i = 1; i <= totalPages; i++) {
            if (i == currentPage) {
                sb.append("<span class=\"current\">").append(i).append("</span> ");
            } else {
                sb.append("<a href=\"/?page=").append(i).append("\">").append(i).append("</a> ");
            }
        }
        if (currentPage < totalPages) {
            sb.append("<a href=\"/?page=").append(currentPage + 1).append("\">下一页</a> ");
            sb.append("<a href=\"/?page=").append(totalPages).append("\">末页</a>");
        }
        sb.append("</p>");
        return sb.toString();
    }

    public String renderError(String message) {
        return "<!DOCTYPE html>\n"
                + "<html lang=\"zh-CN\"><head><meta charset=\"UTF-8\"><title>错误</title>"
                + "<style>" + STYLE + "</style></head><body>"
                + renderNav("")
                + "<h1>操作失败</h1><p>" + escapeHtml(message) + "</p>"
                + "<p><a href=\"/\">返回列表</a> | <a href=\"/add\">去添加</a></p>"
                + "</body></html>";
    }

    private static String escapeHtml(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
