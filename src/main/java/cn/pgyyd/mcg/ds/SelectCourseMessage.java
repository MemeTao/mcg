package cn.pgyyd.mcg.ds;

import java.io.Serializable;
import java.util.List;

public class SelectCourseMessage implements Serializable {
    public class SelectCourseRequest {
        public SelectCourseRequest(String uid, List<String> courses) {
            userId = uid;
            courseIds = courses;
        }
        public String userId;
        public List<String> courseIds;
    }
    public class SelectCourseResult {

        public SelectCourseResult(int status, long jodbid) {
            this.status = status;
            this.jobId = jodbid;
        }
        public Long jobId;
        public int status;      //0处理完成 2排队中
        public List<Result> results;
    }
    public class Result {
        public Result() {}
        public Result(boolean success, String courseid) {
            this.success = success;
            courseId = courseid;
        }
        public boolean success;
        public String courseId;
    }

    public SelectCourseRequest request;
    public SelectCourseResult result;
}
