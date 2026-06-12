<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>文件上传</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: 'Microsoft YaHei', Arial, sans-serif; background: #f0f2f5; min-height: 100vh; }
        .nav { background: #1890ff; padding: 0 40px; display: flex; gap: 30px; }
        .nav a { color: #fff; text-decoration: none; padding: 14px 16px; font-size: 15px; transition: background .3s; }
        .nav a:hover, .nav a.active { background: rgba(255,255,255,.2); }
        .nav a.active { border-bottom: 3px solid #fff; }
        .container { max-width: 700px; margin: 40px auto; padding: 0 20px; }
        .notice { background: #52c41a; color: #fff; padding: 12px 20px; border-radius: 6px; margin-bottom: 20px; }
        .notice.error { background: #ff4d4f; }
        .panel { background: #fff; border-radius: 8px; padding: 30px; box-shadow: 0 2px 8px rgba(0,0,0,.1); margin-bottom: 24px; }
        .panel h1 { font-size: 22px; margin-bottom: 24px; color: #333; }
        .panel label { display: block; margin-bottom: 8px; font-weight: 600; color: #555; }
        .panel input[type="file"] { margin-bottom: 16px; font-size: 14px; }
        .panel .hint { color: #999; font-size: 13px; margin-bottom: 16px; }
        .panel button { background: #1890ff; color: #fff; border: none; padding: 10px 28px; border-radius: 4px; font-size: 15px; cursor: pointer; transition: background .3s; }
        .panel button:hover { background: #40a9ff; }
        .file-list { background: #fff; border-radius: 8px; padding: 24px 30px; box-shadow: 0 2px 8px rgba(0,0,0,.1); }
        .file-list h2 { font-size: 18px; margin-bottom: 16px; color: #333; }
        .file-list ul { list-style: none; }
        .file-list li { padding: 10px 0; border-bottom: 1px solid #f0f0f0; display: flex; align-items: center; gap: 12px; }
        .file-list li:last-child { border-bottom: none; }
        .file-list a { color: #1890ff; text-decoration: none; font-size: 14px; }
        .file-list a:hover { text-decoration: underline; }
        .file-list small { color: #999; font-size: 12px; }
    </style>
</head>
<body>
    <nav class="nav">
        <a href="student">学生列表</a>
        <a href="add">添加学生</a>
        <a href="upload.jsp" class="active">文件上传</a>
    </nav>

    <div class="container">
        <%
            String ok = request.getParameter("ok");
            String error = request.getParameter("error");
            if ("1".equals(ok)) {
        %>
            <div class="notice">文件上传成功！</div>
        <%
            } else if ("1".equals(error)) {
        %>
            <div class="notice error">上传失败：<%= request.getParameter("msg") != null ? request.getParameter("msg") : "未知错误" %></div>
        <%
            }
        %>

        <div class="panel">
            <h1>文件上传</h1>
            <form method="post" action="upload" enctype="multipart/form-data">
                <label>选择文件</label>
                <input type="file" name="file" required>
                <p class="hint">支持任意类型文件，单文件最大 10MB</p>
                <button type="submit">上传文件</button>
            </form>
        </div>

        <div class="file-list">
            <h2>已上传的文件</h2>
            <%
                java.io.File uploadDir = new java.io.File(application.getRealPath("/") + "uploads");
                java.io.File[] files = uploadDir.listFiles();
                if (files == null || files.length == 0) {
            %>
                <p style="color:#999;">（暂无已上传的文件）</p>
            <%
                } else {
                    java.util.Arrays.sort(files, (a, b) -> b.getName().compareTo(a.getName()));
                    for (java.io.File f : files) {
                        if (f.isFile()) {
                            String fname = f.getName();
                            // 去掉时间戳前缀显示原始文件名
                            int idx = fname.indexOf('_');
                            String display = (idx > 0 && idx < fname.length() - 1) ? fname.substring(idx + 1) : fname;
            %>
                <li>
                    <a href="download?name=<%= java.net.URLEncoder.encode(fname, "UTF-8") %>"><%= display %></a>
                    <small>(<%= fname %>)</small>
                </li>
            <%
                        }
                    }
                }
            %>
        </div>
    </div>
</body>
</html>
