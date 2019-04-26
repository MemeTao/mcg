package cn.pgyyd.mcg.ds;

public class SelectCourseResponse {
    private  long job_id;
    
    public SelectCourseResponse(long id) {
        job_id = id;
    }
    public long job_id() {
        return job_id;
    }
}
