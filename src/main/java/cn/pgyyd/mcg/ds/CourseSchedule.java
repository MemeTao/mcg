package cn.pgyyd.mcg.ds;

import java.util.ArrayList;
import java.util.TreeMap;

import cn.pgyyd.mcg.constant.McgConst;

/**
 * 课程时间信息
 * @author memetao
 *
 */
public class CourseSchedule {
    private static final int LESSONS_PER_Day = McgConst.LESSONS_PER_Day;
    
    private static final int DAYS_PER_WEEK = McgConst.DAYS_PER_WEEK;
    
    //FIXME: 以后使用位图的方式减小内存占用
    private boolean bitmap[] = new boolean[LESSONS_PER_Day * DAYS_PER_WEEK];  
    
    private int course_id ;
    
    public CourseSchedule(int course_id){
        this.course_id = course_id;
    }
    
    public int courseId() {
        return course_id;
    }
    /**这个函数只要保证正确就行了，可读性无所谓
     * @param day     周几(1-7)
     * @param lessons 第几节(1-12)
     */
    public void add_info(final int day,final int[] lessons) {
        if(day > DAYS_PER_WEEK  || day <= 0) {
            return ;
        }
        
        for(int lesson : lessons) {
            if(lesson > LESSONS_PER_Day || lesson <= 0) {
                continue;
            }else {
                bitmap[(day-1) * LESSONS_PER_Day + lesson -1] = true;
            }
        }
    }
    /**添加课程信息，周x的第n-m节
     * @param day
     * @param lesson_start  
     * @param lesson_end
     */
    public void add_info(final int day, int lesson_start,int lesson_end) {
        int [] lessons = new int[lesson_end - lesson_start + 1];
        for(int i = lesson_start, j = 0 ;i <= lesson_end; i++,j++) {
            lessons[j] = i;
        }
        add_info(day,lessons);
    }
    /**
     * 周x有没有这门课?
     * @param day   周x (1-7)
     * @return      
     */
    public boolean exsit(final int day) {
        boolean ret = false;
        /*FIXME: 换种更高效的实现*/
        for(int i = 1 ;i <= LESSONS_PER_Day ;i ++) {
            if(exsit(day,i)) {
                ret = true;
                break;
            }
        }
        return ret;
    }
    /**
     * 这门课在 周x第x节 有没有？
     * @param day
     * @param lesson
     * @return
     */
    public boolean exsit(final int day,final int lesson) {
        return bitmap[(day-1) * LESSONS_PER_Day + lesson -1] == true;
    }
    /**
     * 
     * @return KEY：周几   value：哪几节
     */
    public TreeMap<Integer,ArrayList<Integer>> lessons(){
        TreeMap<Integer,ArrayList<Integer>> courses = new TreeMap<Integer,ArrayList<Integer>>();
        for(int day = 1 ;day <= DAYS_PER_WEEK ; day ++) {
            ArrayList<Integer> lessons = new ArrayList<Integer>();
            for(int lesson = 1 ; lesson <= LESSONS_PER_Day ; lesson ++) {
                if(exsit(day,lesson)) {
                    lessons.add(lesson);
                }
            }
            if(lessons.size() > 0) {
                courses.put(day, lessons);
            }
        }
        return courses;
    }
    
    public String dump() {
        String str = new String();
        str = "course(" + course_id + ") schedule: \n";
        for(int i = 1 ;i <= DAYS_PER_WEEK ; i++) {
            if(exsit(i)) {
                str += "day(" + i + "):";
                for(int j = 1 ;j <= LESSONS_PER_Day ; j++) {
                    if(exsit(i,j)) {
                        str += j + " ";
                    }
                }
                str += "\n";
            }
        }
        return str;
    }
}
