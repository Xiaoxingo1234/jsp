package com.luoxin.com.model;

/**
 * 学生实体（对应 jsp 表：自增 id、name、age、sex）
 */
public class Student {

    /** 数据库自增主键，新建未保存时可为 null */
    private Long id;
    private String name;
    private int age;
    private String sex;

    public Student() {
    }

    /** 用于插入：无需指定 id */
    public Student(String name, int age, String sex) {
        this.name = name;
        this.age = age;
        this.sex = sex;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }
}
