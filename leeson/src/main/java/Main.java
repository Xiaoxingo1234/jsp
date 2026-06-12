import com.luoxin.com.controller.StudentController;
import com.luoxin.com.model.Student;
import com.luoxin.com.model.StudentDao;
import com.luoxin.com.view.StudentView;

public class Main {
    public static void main(String[] args) {
        StudentDao dao = new StudentDao();
        StudentView view = new StudentView();
        StudentController controller = new StudentController(dao, view);

        // 若需要演示插入再列表，取消下面一行注释，并视情况注释掉仅查询的分支
        // controller.addStudentThenDisplay(new Student("王五", 19, "女"));

        controller.displayAllStudents();
    }
}
