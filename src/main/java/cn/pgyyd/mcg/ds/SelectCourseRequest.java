package cn.pgyyd.mcg.ds;

import java.util.ArrayList;

public class SelectCourseRequest {
    public SelectCourseRequest(String student_id, ArrayList<String> courses) {
        this.student_id = student_id;
        this.courses = courses;
    }
    public String student_id;
    public ArrayList<String> courses;
}
