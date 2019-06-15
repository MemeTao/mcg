drop database if exists mcg_m0;
create database mcg_m0;
drop database if exists mcg_s1;
create database mcg_s1;

use mcg_m0;
-- 课程表
-- 课程代码的一部分字段也可以用来区分二级组织
CREATE TABLE `mcg_courses` (
  `id` int(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code` char(50) NOT NULL COMMENT '课程代码',
  `students` int(20) NOT NULL COMMENT '开课人数',
  `type` int(4) NOT NULL COMMENT '选课类型：0院选 1校选 2体育',
  `organization` int(20) DEFAULT NULL COMMENT '开课的二级学院',
  PRIMARY KEY (`id`),
  KEY `idx_code` (`code`),
  KEY `idx_students` (`students`),
  KEY `idx_type` (`type`),
  KEY `idx_organization` (`organization`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT '待选课课程表';

use mcg_s1;
CREATE TABLE `mcg_courses` (
  `id` int(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code` char(50) NOT NULL COMMENT '课程代码',
  `students` int(20) NOT NULL COMMENT '开课人数',
  `type` int(4) NOT NULL COMMENT '选课类型：0院选 1校选 2体育',
  `organization` int(20) DEFAULT NULL COMMENT '开课的二级学院',
  PRIMARY KEY (`id`),
  KEY `idx_code` (`code`),
  KEY `idx_students` (`students`),
  KEY `idx_type` (`type`),
  KEY `idx_organization` (`organization`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT '待选课课程表';

use mcg_m0;
-- 剩余人数表
CREATE TABLE `mcg_course_remain` (
  `id` int(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code` char(64) NOT NULL COMMENT '课程代码',
  `number` int(16) NOT NULL COMMENT '剩余数目',
  PRIMARY KEY (`id`),
  KEY `code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT '剩余人数表';

load data local infile "database/mcg_course_remain.txt.org1" into table mcg_course_remain fields terminated by '|';
use mcg_s1;
CREATE TABLE `mcg_course_remain` (
  `id` int(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code` char(64) NOT NULL COMMENT '课程代码',
  `number` int(16) NOT NULL COMMENT '剩余数目',
  PRIMARY KEY (`id`),
  KEY `code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT '剩余人数表';

 load data local infile "database/mcg_course_remain.txt.org2" into table mcg_course_remain fields terminated by '|';
 
 use mcg_m0;
-- 学生-课程 关系表，每条记录代表x学生选择了（拥有了）这个课
CREATE TABLE `mcg_student_course` (
    `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `student` char(64) NOT NULL COMMENT '学号',
    `code` char(64) NOT NULL COMMENT '课程代码',
    PRIMARY KEY (`id`),
    KEY `idx_student` (`student`),
    KEY `idex_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '学生课表';

load data local infile "database/mcg_student_course.txt.org1" into table mcg_student_course fields terminated by '|';
 use mcg_s1;
CREATE TABLE `mcg_student_course` (
    `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `student` char(64) NOT NULL COMMENT '学号',
    `code` char(64) NOT NULL COMMENT '课程代码',
    PRIMARY KEY (`id`),
    KEY `idx_student` (`student`),
    KEY `idex_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '学生课表';

load data local infile "database/mcg_student_course.txt.org2" into table mcg_student_course fields terminated by '|';

 use mcg_m0;
-- 课程时间表
-- 一个课程可能是这样分布的： 第1-3周  周1 34节 周3 56节
--                       第4-15周 周2 78节
-- 遇到性能瓶颈的时候再考虑压缩信息
CREATE TABLE `mcg_course_schedule` (
  `id` int(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code` char(64) NOT NULL COMMENT '课程代码',
  `week` int(8) NOT NULL COMMENT '第几周',
  `day` int(8) NOT NULL COMMENT '星期几',
  `lessons` char(64) NOT NULL COMMENT '上课时间，以\‘,\'分割 .eg. 3,4    9,10,11',
   PRIMARY KEY (`id`),
   KEY `idx_code` (`code`),
   KEY `idx_week` (`week`),
   KEY `idx_day` (`day`),
   KEY `idx_lessons` (`lessons`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT '课程时间表';

load data local infile "database/mcg_course_schedule.txt.org1" into table mcg_course_schedule fields terminated by '|';

 use mcg_s1;
CREATE TABLE `mcg_course_schedule` (
  `id` int(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code` char(64) NOT NULL COMMENT '课程代码',
  `week` int(8) NOT NULL COMMENT '第几周',
  `day` int(8) NOT NULL COMMENT '星期几',
  `lessons` char(64) NOT NULL COMMENT '上课时间，以\‘,\'分割 .eg. 3,4    9,10,11',
   PRIMARY KEY (`id`),
   KEY `idx_code` (`code`),
   KEY `idx_week` (`week`),
   KEY `idx_day` (`day`),
   KEY `idx_lessons` (`lessons`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT '课程时间表';

load data local infile "database/mcg_course_schedule.txt.org2" into table mcg_course_schedule fields terminated by '|';
