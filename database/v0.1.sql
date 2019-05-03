drop TABLE mcg_course;
drop TABLE mcg_course_remain;
drop TABLE mcg_course_timerange;
drop TABLE mcg_student_course;

-- 课程
CREATE TABLE `mcg_course` (
  `id` int(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `courseId` int(20) NOT NULL COMMENT '课程id',
  `basicId` int(20) NOT NULL COMMENT '课程基本信息id',
  `teacherId` int(20) NOT NULL COMMENT '老师id',
  `type` int(4) NOT NULL DEFAULT '0' COMMENT '选课类型：0院选 1校选',
  `majorId` int(20) DEFAULT NULL COMMENT '专业id',
  PRIMARY KEY (`id`),
  KEY `idx_basicId` (`basicId`),
  KEY `idx_teacherId` (`teacherId`),
  KEY `idx_type` (`type`),
  KEY `idx_majorId` (`majorId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT '课程表';

-- 课程时间
CREATE TABLE `mcg_course_timerange` (
  `id` int(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `courseId` int(20) NOT NULL COMMENT '课程id',
  `day` int(20) NOT NULL COMMENT '星期几',
  `start` int(20) NOT NULL COMMENT '开始第几节',
  `end` int(20) NOT NULL COMMENT '结束第几节',
  PRIMARY KEY (`id`),
  KEY `idx_courseId` (`courseId`),
  KEY `idx_day` (`day`),
  KEY `idx_start_end` (`start`,`end`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT '课程时间表';

-- 学生-课程 关系表，每条记录代表x学生选择了（拥有了）这个课
CREATE TABLE `mcg_student_course` (
    `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `studentId` int(11) NOT NULL COMMENT '学生id',
    `courseId` int(11) NOT NULL COMMENT '课程id',
    PRIMARY KEY (`id`),
    UNIQUE KEY (`studentId`, `courseId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '学生课表(已有)';


-- 选课中课程信息
CREATE TABLE `mcg_course_remain` (
  `id` int(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `courseId` int(20) NOT NULL COMMENT '课程id',
  `remain` int(20) NOT NULL COMMENT '剩余数目',
  PRIMARY KEY (`id`),
  KEY `idx_courseId` (`courseId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT '选课中课程信息表';



