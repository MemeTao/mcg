package cn.pgyyd.mcg.ds;

import java.util.List;

//TODO: 完善选课结果的逻辑，如部分成功选课
@Deprecated
public class SelectCourseResult {
    public class Result {
        public Result() {}
        public Result(boolean success, long courseid) {
            Success = success;
            CourseID = courseid;
        }
        public boolean Success;
        public long CourseID;
    }
    public SelectCourseResult(int status, long jodbid) {
        Status = status;
        JobID = jodbid;
    }
    public Long JobID;
    public int Status;      //0处理完成 2排队中
    public List<Result> Results;
}
