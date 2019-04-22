package cn.pgyyd.mcg.ds;

import java.util.List;

//我就不搞什么bean了，直接public field
public class SelectCourseRequest {
    public SelectCourseRequest(int uid, List<Long> courses) {
        UserID = uid;
        CourseIDs = courses;
    }
    public int UserID;
    public List<Long> CourseIDs;
    public int JobID = -1;
}
