package com.luoxin.com.model;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据访问层：仅负责与数据库交互，不包含界面逻辑。
 */
public class StudentDao {

    private static final String URL = "jdbc:mysql://localhost:3306/stu_db?useSSL=false&characterEncoding=utf8";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "root";

    static {
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL 驱动加载失败", e);
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USERNAME, PASSWORD);
    }

    private Student mapRow(ResultSet rs) throws SQLException {
        Student s = new Student();
        long idVal = rs.getLong("id");
        if (!rs.wasNull()) {
            s.setId(idVal);
        }
        s.setName(rs.getString("name"));
        s.setAge(rs.getInt("age"));
        s.setSex(rs.getString("sex"));
        return s;
    }

    public int count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM jsp";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        }
    }

    /**
     * 分页查询，page 从 1 开始
     */
    public List<Student> findByPage(int page, int pageSize) throws SQLException {
        List<Student> list = new ArrayList<>();
        int offset = (page - 1) * pageSize;
        String sql = "SELECT id, name, age, sex FROM jsp ORDER BY id LIMIT ? OFFSET ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, pageSize);
            ps.setInt(2, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    public Student findById(long id) throws SQLException {
        String sql = "SELECT id, name, age, sex FROM jsp WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    /**
     * 查询 jsp 表中全部学生
     */
    public List<Student> findAll() throws SQLException {
        List<Student> list = new ArrayList<>();
        String sql = "SELECT id, name, age, sex FROM jsp ORDER BY id";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    public int insert(Student student) throws SQLException {
        String sql = "INSERT INTO jsp (name, age, sex) VALUES (?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, student.getName());
            ps.setInt(2, student.getAge());
            ps.setString(3, student.getSex());
            return ps.executeUpdate();
        }
    }

    public int update(Student student) throws SQLException {
        String sql = "UPDATE jsp SET name = ?, age = ?, sex = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, student.getName());
            ps.setInt(2, student.getAge());
            ps.setString(3, student.getSex());
            ps.setLong(4, student.getId());
            return ps.executeUpdate();
        }
    }

    public int deleteById(long id) throws SQLException {
        String sql = "DELETE FROM jsp WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate();
        }
    }
}
