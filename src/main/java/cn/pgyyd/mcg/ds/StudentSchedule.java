package cn.pgyyd.mcg.ds;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import cn.pgyyd.mcg.constant.McgConst;

public class StudentSchedule {
    private static final int LESSONS_PER_Day = McgConst.LESSONS_PER_Day;
    
    private static final int DAYS_PER_WEEK = McgConst.DAYS_PER_WEEK;
    
    private static final int INVALID_COURSE_ID = McgConst.INVALID_COURSE_ID;
    
    private int [][] courses = new int[DAYS_PER_WEEK][LESSONS_PER_Day];
    
    private int student_id;
    
    public StudentSchedule(final int id) {
        student_id = id;
        for(int [] a : courses) {
            for(int i = 0 ;i < a.length;i++) {
                a[i] = INVALID_COURSE_ID;
            }
        }
    }
    /**
     * 导入xx课程
     * @param course
     */
    public void import_course(CourseSchedule course) {
        ;
    }
    
    public void add_info(final int day,int lesson,int course_id) {
        if(day <= DAYS_PER_WEEK && day > 0 &&
                lesson <= LESSONS_PER_Day && lesson > 0 &&
                course_id != INVALID_COURSE_ID) {
            courses[day][lesson] = course_id;
        }
    }
    
    public boolean exsit(final int day,int lesson) {
        boolean ret = false;
        if(day <= DAYS_PER_WEEK && day > 0 &&
                lesson <= LESSONS_PER_Day && lesson > 0) {
            ret = (courses[day][lesson] != INVALID_COURSE_ID);
        }
        return ret;
    }
    public boolean exsit(final int day,List<Integer> lessons) {
        boolean ret = false;
        for(int lesson : lessons) {
            if(exsit(day,lesson)) {
                ret = true;
                break;
            }
        }
        return ret;
    }
    
    public HashMap<Integer,ArrayList<Integer>> lessons() {
        return null;
    }
}
