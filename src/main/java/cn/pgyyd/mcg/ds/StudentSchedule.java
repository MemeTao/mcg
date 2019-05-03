package cn.pgyyd.mcg.ds;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

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
        System.out.println(dump());
    }
    
    public int stduentId() {
        return student_id;
    }
    /**
     * 增加xx课程
     * @param course
     */
    public void add_course(CourseSchedule course) {
        System.out.println("[info] add lesson...");
        System.out.println(course.dump());
        for(Entry<Integer,ArrayList<Integer>> lesson : course.lessons().entrySet()) {
            add_lesson(course.courseId(),lesson.getKey(),lesson.getValue());
        }
        System.out.println(dump());
    }
    
    public void add_lesson(final int course_id,final int day,ArrayList<Integer> lessons) {
        for(int lesson : lessons) {
            add_lesson(course_id,day,lesson);
        }
    }
    
    public void add_lesson(int course_id, final int day, int lesson) {
        if(day <= DAYS_PER_WEEK && day > 0 &&
                lesson <= LESSONS_PER_Day && lesson > 0 &&
                course_id != INVALID_COURSE_ID) {
            courses[day-1][lesson-1] = course_id;
        }
    }
    
    public boolean exsit(final int day,int lesson) {
        boolean ret = false;
        if(day <= DAYS_PER_WEEK && day > 0 &&
                lesson <= LESSONS_PER_Day && lesson > 0) {
            ret = (courses[day-1][lesson-1] != INVALID_COURSE_ID);
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
    
    public boolean confict(CourseSchedule course) {
        boolean ret = false;
        for(Entry<Integer, ArrayList<Integer>> entry : course.lessons().entrySet()) {
            if(exsit(entry.getKey(),entry.getValue())) {
                ret = true;
                break;
            }
        }
        return ret;
    }
    public String dump() {
        String str = new String();
        str += "student(" + student_id +") private course schedule:\n";
        for(int day = 1 ; day <= DAYS_PER_WEEK ; day++) {
            str += "day(" + day + ") :";
            for(int lesson = 1 ; lesson <= LESSONS_PER_Day ; lesson ++) {
                if(exsit(day,lesson)) {
                    str += lesson + " ";
                }
            }
            str += "\n";
        }
        return str;
    }
}
