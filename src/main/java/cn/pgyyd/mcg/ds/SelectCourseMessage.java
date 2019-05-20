package cn.pgyyd.mcg.ds;

import java.io.Serializable;
import java.util.List;

public class SelectCourseMessage implements Serializable {
    public class SelectCourseRequest {
        public SelectCourseRequest(int uid, List<Integer> courses) {
            userID = uid;
            courseIDs = courses;
        }
        public int userID;
        public List<Integer> courseIDs;
    }
    public class SelectCourseResult {

        public SelectCourseResult(int status, long jodbid) {
            this.status = status;
            this.jobID = jodbid;
        }
        public Long jobID;
        public int status;      //0处理完成 2排队中
        public List<Result> results;
    }
    public class Result {
        public Result() {}
        public Result(boolean success, long courseid) {
            this.success = success;
            courseID = courseid;
        }
        public boolean success;
        public long courseID;
    }

    public SelectCourseRequest request;
    public SelectCourseResult result;
}
