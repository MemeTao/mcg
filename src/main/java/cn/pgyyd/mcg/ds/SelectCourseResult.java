package cn.pgyyd.mcg.ds;

//TODO: 完善选课结果的逻辑，如部分成功选课
public class SelectCourseResult {
    public SelectCourseResult(int status, long pendingID) {
        Status = status;
        PendingID = pendingID;
    }
    public long PendingID;
    public int Status;      //0成功 1失败 2排队
}
