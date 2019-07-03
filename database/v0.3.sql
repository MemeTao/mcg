DROP DATABASE mcg_common;
CREATE  DATABASE mcg_common;
USE mcg_common;

-- 学生
CREATE TABLE `mcg_student` (
    `uid` VARCHAR(20) NOT NULL COMMENT '学号',
    `name` VARCHAR(30) NOT NULL COMMENT '学生姓名',
    PRIMARY KEY (`uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '学生表';

-- 必修课程（此次选课之前的所有课都当作必修）
CREATE TABLE `req_course` (
  -- 课程代码示例：(2012-2013-2)-K0110570-00A1105013-2
  `cid` VARCHAR(50) NOT NULL COMMENT '课程代码',
  `size` INT(11) NOT NULL COMMENT '课程人数',   -- 由于这是必修课，在选课中用不上这个字段
  `ctype` INT(4) NOT NULL COMMENT '课程类型：体育必修、体育选修、高数那种必修、专业那种必修',  -- 用不上这个字段
  `org` INT(11) DEFAULT NULL COMMENT '开课的二级学院',  -- 用不上这个字段
  PRIMARY KEY (`cid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT '必修课程';

-- 必修课程时间表
-- 一个课程可能是这样分布的： 第1-3周  周1 34节 周3 56节
--                       第4-15周 周2 78节
CREATE TABLE `req_course_schedule` (
  `id` INT(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `cid` VARCHAR(50) NOT NULL COMMENT '课程代码',
  `week` INT(8) NOT NULL COMMENT '第几周',
  `day` INT(8) NOT NULL COMMENT '星期几',
  `lessons` VARCHAR(20) NOT NULL COMMENT '上课时间，以","分割 eg. 9,10,11',
   PRIMARY KEY (`id`),
   KEY (`cid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT '课程时间表';

-- 学生-必修课程 关系表
CREATE TABLE `req_student_course` (
    `id` INT(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `uid` VARCHAR(20) NOT NULL COMMENT '学号',
    `cid` VARCHAR(50) NOT NULL COMMENT '课程代码',
    PRIMARY KEY (`id`),
    KEY (`uid`,`cid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '学生-必修课关系表';


------------------------------------------------------------------
-- mcg_option数据库每个学院都有一份，其中的静态表可能不同学院的会有重复数据
DROP DATABASE mcg_option;
CREATE  DATABASE mcg_option;
USE mcg_option;

-- 选修课程（特指此此轮选课中的课）
CREATE TABLE `opt_course` (
  -- 课程代码示例：(2012-2013-2)-K0110570-00A1105013-2
  `cid` VARCHAR(50) NOT NULL COMMENT '课程代码',
  `capacity` INT(11) NOT NULL COMMENT '课程容量',
  `ctype` INT(4) NOT NULL COMMENT '课程类型：院选 校选 体育',
  `org` INT(11) DEFAULT NULL COMMENT '开课的二级学院',
  PRIMARY KEY (`cid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT '必修课程';

-- 剩余人数表
CREATE TABLE `opt_course_remain` (
  `cid` VARCHAR(50) NOT NULL COMMENT '课程代码',
  `remain` INT(11) NOT NULL COMMENT '剩余人数',
  PRIMARY KEY (`cid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT '剩余人数表';

-- 选修课时间表
CREATE TABLE `opt_course_schedule` (
  `id` INT(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `cid` VARCHAR(50) NOT NULL COMMENT '课程代码',
  `week` INT(8) NOT NULL COMMENT '第几周',
  `day` INT(8) NOT NULL COMMENT '星期几',
  `lessons` VARCHAR(20) NOT NULL COMMENT '上课时间，以","分割 eg. 9,10,11',
   PRIMARY KEY (`id`),
   KEY (`cid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT '选修课时间表';

-- 学生-选修课程 关系表
CREATE TABLE `opt_student_course` (
    `id` INT(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `uid` VARCHAR(20) NOT NULL COMMENT '学号',
    `cid` VARCHAR(50) NOT NULL COMMENT '课程代码',
    PRIMARY KEY (`id`),
    KEY (`uid`,`cid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '学生-必修课关系表';
