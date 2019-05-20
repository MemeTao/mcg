package cn.pgyyd.mcg.ds;

import java.util.List;

//我就不搞什么bean了，直接public field
@Deprecated
public class SelectCourseRequest {
    public SelectCourseRequest(int uid, List<Integer> courses) {
        UserID = uid;
        CourseIDs = courses;
    }
    public int UserID;
    public List<Integer> CourseIDs;
    public int JobID = -1;
}
