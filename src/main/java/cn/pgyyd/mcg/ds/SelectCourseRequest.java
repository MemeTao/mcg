package cn.pgyyd.mcg.ds;

import java.util.ArrayList;

//我就不搞什么bean了，直接public field
public class SelectCourseRequest {
    public SelectCourseRequest(int uid, ArrayList<Integer> courses) {
        UserID = uid;
        CourseIDs = courses;
    }
    public int UserID;
    public ArrayList<Integer> CourseIDs;
    public int JobID = -1;
}
