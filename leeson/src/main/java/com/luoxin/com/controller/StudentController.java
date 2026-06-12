package com.luoxin.com.controller;

import com.luoxin.com.model.PageResult;
import com.luoxin.com.model.Student;
import com.luoxin.com.model.StudentDao;
import com.luoxin.com.view.StudentView;

import java.sql.SQLException;
import java.util.List;

/**
 * 控制器：协调 Model 与 View，不包含具体 SQL 与具体输出格式细节。
 */
public class StudentController {

    /** 每页条数 */
    public static final int PAGE_SIZE = 2;

    private final StudentDao studentDao;
    private final StudentView studentView;

    public StudentController(StudentDao studentDao, StudentView studentView) {
        this.studentDao = studentDao;
        this.studentView = studentView;
    }

    public PageResult getStudentPage(int page) throws SQLException {
        int totalCount = studentDao.count();
        int totalPages = totalCount == 0 ? 1 : (int) Math.ceil((double) totalCount / PAGE_SIZE);
        if (page < 1) {
            page = 1;
        }
        if (page > totalPages) {
            page = totalPages;
        }
        List<Student> list = studentDao.findByPage(page, PAGE_SIZE);
        PageResult result = new PageResult();
        result.setList(list);
        result.setPage(page);
        result.setPageSize(PAGE_SIZE);
        result.setTotalCount(totalCount);
        result.setTotalPages(totalPages);
        return result;
    }

    public List<Student> getStudentList() throws SQLException {
        return studentDao.findAll();
    }

    public Student getStudentById(long id) throws SQLException {
        return studentDao.findById(id);
    }

    public int addStudent(Student student) throws SQLException {
        return studentDao.insert(student);
    }

    public int updateStudent(Student student) throws SQLException {
        return studentDao.update(student);
    }

    public int deleteStudent(long id) throws SQLException {
        return studentDao.deleteById(id);
    }

    public void displayAllStudents() {
        if (studentView == null) {
            throw new IllegalStateException("未注入控制台 StudentView，无法使用 displayAllStudents");
        }
        try {
            List<Student> list = studentDao.findAll();
            studentView.showTitle();
            studentView.showStudentList(list);
        } catch (SQLException e) {
            studentView.showError("查询学生列表失败：" + e.getMessage());
        }
    }

    public void addStudentThenDisplay(Student student) {
        if (studentView == null) {
            throw new IllegalStateException("未注入控制台 StudentView，无法使用 addStudentThenDisplay");
        }
        try {
            int rows = studentDao.insert(student);
            studentView.showMessage("成功添加 " + rows + " 条数据！");
            displayAllStudents();
        } catch (SQLException e) {
            studentView.showError("添加或查询失败：" + e.getMessage());
        }
    }
}
