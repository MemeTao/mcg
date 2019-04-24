package cn.pgyyd.mcg.ds;

import java.util.ArrayList;

public class SelectCourseRequest {
    public SelectCourseRequest(int student_id, ArrayList<Integer> courses) {
        this.student_id = student_id;
        this.courses = courses;
    }
    public int student_id;
    public ArrayList<Integer> courses;
}
