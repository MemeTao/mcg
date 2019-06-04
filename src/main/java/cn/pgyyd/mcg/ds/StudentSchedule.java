package cn.pgyyd.mcg.ds;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import cn.pgyyd.mcg.constant.McgConst;

public class StudentSchedule {
    private static final int WEEKS_PER_SEMESTER = McgConst.WEEKS_PER_SEMESTER;
    
    private static final int LESSONS_PER_Day = McgConst.LESSONS_PER_Day;
    
    private static final int DAYS_PER_WEEK = McgConst.DAYS_PER_WEEK;
    
    private static final String INVALID_COURSE_ID = McgConst.INVALID_COURSE_ID;
    
    //private int [][] courses = new int[DAYS_PER_WEEK][LESSONS_PER_Day];
    
    private String student_id;
    
    private TreeMap<Integer,String [][]> schedules = new TreeMap<Integer,String [][]>();
    
    public StudentSchedule(final String id) {
        student_id = id;
        for(int week = 0 ; week < WEEKS_PER_SEMESTER ; week++) {
            String [][] courses = new String[DAYS_PER_WEEK][LESSONS_PER_Day];
            for(String [] a : courses) {
                for(int i = 0 ;i < a.length;i++) {
                    a[i] = INVALID_COURSE_ID;
                }
            }
            schedules.put(week, courses);
        }
        //log.info(dump());
    }
    
    public String stduentId() {
        return student_id;
    }
    /**
     * 增加xx课程
     * @param course
     */
    public void add_course(CourseSchedule course) {
        for(int week = 0 ; week < WEEKS_PER_SEMESTER ; week ++) {
            for(Entry<Integer,ArrayList<Integer>> lesson : course.lessons(week).entrySet()) {
                add_lesson(course.courseId(),week, lesson.getKey(),lesson.getValue());
            }
        }
        //log.info("course schedule update.\n" + dump());
    }
    
    public void add_lesson(final String course_id,final int week, final int day,ArrayList<Integer> lessons) {
        for(int lesson : lessons) {
            add_lesson(course_id,week, day,lesson);
        }
    }
    
    public void add_lesson(String course_id, final int week, final int day, int lesson) {
        if(!schedules.containsKey(week)) {
            return;
        }
        String [][] courses = schedules.get(week);
        if(day <= DAYS_PER_WEEK && day > 0 &&
                lesson <= LESSONS_PER_Day && lesson > 0 &&
                course_id != INVALID_COURSE_ID) {
            courses[day-1][lesson-1] = course_id;
        }
    }
    
    public boolean exsit(final int week, final int day,int lesson) {
        if(!schedules.containsKey(week)) {
            return false;
        }
        String [][] courses = schedules.get(week);
        boolean ret = false;
        if(day <= DAYS_PER_WEEK && day > 0 &&
                lesson <= LESSONS_PER_Day && lesson > 0) {
            ret = (courses[day-1][lesson-1] != INVALID_COURSE_ID);
        }
        return ret;
    }
    
    public boolean exsit(final int week, final int day,List<Integer> lessons) {
        boolean ret = false;
        for(int lesson : lessons) {
            if(exsit(week, day, lesson)) {
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
        for(int week = 0 ; week < WEEKS_PER_SEMESTER ; week ++)
        for(Entry<Integer, ArrayList<Integer>> entry : course.lessons(week).entrySet()) {
            if(exsit(week, entry.getKey(), entry.getValue())) {
                ret = true;
                break;
            }
        }
        return ret;
    }
    public String dump() {
        String str = new String();
        str += "student(" + student_id +") private course schedule:\n";
        for(int week = 0 ;week < WEEKS_PER_SEMESTER ; week ++) {
            str += "week " + week + 1 + ": \n";
            for(int day = 1 ; day <= DAYS_PER_WEEK ; day++) {
                str += "day(" + day + ") :";
                for(int lesson = 1 ; lesson <= LESSONS_PER_Day ; lesson ++) {
                    if(exsit(week,day,lesson)) {
                        str += lesson + " ";
                    }
                }
                str += "\n";
            }
        }
        return str;
    }
}
