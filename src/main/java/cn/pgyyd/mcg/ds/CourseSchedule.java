package cn.pgyyd.mcg.ds;

import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.TreeMap;

import cn.pgyyd.mcg.constant.McgConst;

/**
 * 课程时间信息
 * @author memetao
 * FIXME: 现在的实现很低效
 */
public class CourseSchedule {
    private static final int LESSONS_PER_Day = McgConst.LESSONS_PER_Day;
    
    private static final int DAYS_PER_WEEK = McgConst.DAYS_PER_WEEK;
    
    private String course_id ;
    
    private TreeMap<Integer,boolean[]> schedules = new TreeMap<Integer,boolean[]>();
    
    public CourseSchedule(String course_id){
        this.course_id = course_id;
    }
    
    public String courseId() {
        return course_id;
    }
    /**这个函数只要保证正确就行了，可读性无所谓
     * @param day     周几(1-7)
     * @param lessons 第几节(1-12)
     */
    public void add_info(final int week, final int day, final int[] lessons) {
        //TODO:未做检查
        if(!schedules.containsKey(week)) {
            schedules.put(week, new boolean[LESSONS_PER_Day * DAYS_PER_WEEK]);
        }
        boolean bitmap[] = schedules.get(week);
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
    public void add_info(final int week, final int day, int lesson_start,int lesson_end) {
        int [] lessons = new int[lesson_end - lesson_start + 1];
        for(int i = lesson_start, j = 0 ;i <= lesson_end; i++,j++) {
            lessons[j] = i;
        }
        add_info(week, day, lessons);
    }
    
    public boolean exsit(final int week) {
        return schedules.containsKey(week) ;
    }
    /**
     * 周x有没有这门课?
     * @param day   周x (1-7)
     * @return      
     */
    public boolean exsit(final int week, final int day) {
        boolean ret = false;
        /*FIXME: 换种更高效的实现*/
        for(int i = 1 ;i <= LESSONS_PER_Day ;i ++) {
            if(exsit(week,day,i)) {
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
    public boolean exsit(final int week, final int day, final int lesson) {
        boolean bitmap[] = schedules.get(week);
        return bitmap != null && bitmap[(day-1) * LESSONS_PER_Day + lesson -1] == true;
    }
    /**
     * 
     * @return KEY：周几   value：哪几节
     */
    public TreeMap<Integer,ArrayList<Integer>> lessons(int week) {
        TreeMap<Integer,ArrayList<Integer>> courses = new TreeMap<Integer,ArrayList<Integer>>();
        for(int day = 1 ;day <= DAYS_PER_WEEK ; day ++) {
            ArrayList<Integer> lessons = new ArrayList<Integer>();
            for(int lesson = 1 ; lesson <= LESSONS_PER_Day ; lesson ++) {
                if(exsit(week,day,lesson)) {
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
        for(Entry<Integer,boolean[]> schedule : schedules.entrySet()) {
            int week = schedule.getKey();
            str += "week " + week + ":\n";
            for(int day = 1 ;day <= DAYS_PER_WEEK ; day++) {
                if(exsit(week,day)) {
                    str += "day(" + day + "):";
                    for(int lesson = 1 ;lesson <= LESSONS_PER_Day ; lesson ++) {
                        if(exsit(week,day,lesson)) {
                            str += lesson + " ";
                        }
                    }
                    str += "\n";
                }
            }
        }
        return str;
    }
}