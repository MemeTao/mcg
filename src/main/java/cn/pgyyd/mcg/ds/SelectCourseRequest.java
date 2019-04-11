package cn.pgyyd.mcg.ds;

import java.util.List;

//我就不搞什么bean了，直接public field
public class SelectCourseRequest {
    public SelectCourseRequest(int uid, int jobid, List<Long> courses) {
        UserID = uid;
        JobID = jobid;
        CourseIDs = courses;
    }
    public int UserID;
    public int JobID;
    public List<Long> CourseIDs;
}
