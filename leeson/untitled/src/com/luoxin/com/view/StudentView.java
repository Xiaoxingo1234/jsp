package com.luoxin.com.view;

import com.luoxin.com.model.Student;

import java.util.List;

/**
 * 视图层：仅负责如何展示数据（此处为控制台输出）。
 */
public class StudentView {

    public void showTitle() {
        System.out.println("jsp 表中的学生列表：");
    }

    public void showStudentList(List<Student> students) {
        if (students == null || students.isEmpty()) {
            System.out.println("（暂无数据）");
            return;
        }
        for (Student s : students) {
            String idStr = s.getId() != null ? String.valueOf(s.getId()) : "-";
            System.out.println("id: " + idStr
                    + ", name: " + s.getName()
                    + ", age: " + s.getAge()
                    + ", sex: " + s.getSex());
        }
    }

    public void showMessage(String msg) {
        System.out.println(msg);
    }

    public void showError(String msg) {
        System.err.println(msg);
    }
}
