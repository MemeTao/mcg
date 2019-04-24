package cn.pgyyd.mcg.module;

import cn.pgyyd.mcg.ds.SelectCourseRequest;
import cn.pgyyd.mcg.ds.SelectCourseResponse;

public class BussinessMessage {
    /*选课消息*/
    static public class SelectCourseMessage{
        private SelectCourseRequest request;
        
        private SelectCourseResponse response;
        
        public SelectCourseMessage(SelectCourseRequest r) {
            request = r;
            response = null;
        }
        public SelectCourseMessage(SelectCourseResponse r) {
            request = null;
            response = r;
        }
        public SelectCourseMessage(SelectCourseRequest req,SelectCourseResponse res) {
            request = req;
            response = res;
        }
        public SelectCourseRequest request() {
            return request;
        }
        public SelectCourseResponse response() {
            return response;
        }
    }
}
