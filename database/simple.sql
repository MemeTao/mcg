CREATE DATABASE mcg;
use mcg;

-- 学生 表，每条记录代表一个独立的学生
CREATE TABLE `tb_student` (
    `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `student_name` varchar(50) NOT NULL COMMENT '学生姓名',
    PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 COMMENT '学生表';

-- 课程 表，每条记录代表一个课
CREATE TABLE `tb_course` (
    `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `capacity` int(11) NOT NULL COMMENT '课程容量',
    `students` int(11) NOT NULL COMMENT '已选人数',
    `course_name` varchar(50) NOT NULL COMMENT '课程名称',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '课程表';

-- 学生-课程 关系表，每条记录代表x学生选择了（拥有了）这个课
CREATE TABLE `tb_student_course` (
    `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `student` int(11) NOT NULL COMMENT '学生id',
    `course` int(11) NOT NULL COMMENT '课程id',
    PRIMARY KEY (`id`),
    UNIQUE KEY (`student`, `course`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '学生选课表';

-- 课程 时间表，每条记录代表某个课程，在第x周、星期y、第z节有课
CREATE TABLE `tb_course_schedule` (
    `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `course_id` int(11) NOT NULL COMMENT '所属课程id',
    `week` int(11) NOT NULL COMMENT '第几周',
    `day_of_week` int(11) NOT NULL COMMENT '周几',
    `section_of_day` int(11) NOT NULL COMMENT '第几节',
    PRIMARY KEY  (`id`),
    KEY (`course_id`),
    UNIQUE KEY (`course_id`, `week`, `day_of_week`, `section_of_day`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 COMMENT '课程时间表';


