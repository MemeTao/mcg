-- 选课系统
-- 学院
CREATE TABLE `mcg_institute` (
  `id` int(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(50) NOT NULL COMMENT '名称',
  PRIMARY KEY (`id`),
  KEY `idx_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT '学院表';

-- 专业
CREATE TABLE `mcg_major` (
  `id` int(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `instituteId` int(20) NOT NULL COMMENT '学院id',
  `name` varchar(50) NOT NULL COMMENT '名称',
  PRIMARY KEY (`id`),
  KEY `idx_instituteId` (`instituteId`),
  KEY `idx_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT '专业表';

-- 班级
CREATE TABLE `mcg_class` (
  `id` int(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `majorId` int(20) NOT NULL COMMENT '专业id',
  `number` int(20) NOT NULL COMMENT '序号',
  PRIMARY KEY (`id`),
  KEY `idx_majorId` (`majorId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT '班级表';

-- 用户
CREATE TABLE `mcg_user` (
  `id` int(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `number` varchar(50) NOT NULL COMMENT '学号/工号',
  `password` varchar(50) NOT NULL COMMENT '密码',
  `type` int(4) NOT NULL DEFAULT '0' COMMENT '用户类型：0学生 1老师',
  `name` varchar(50) NOT NULL COMMENT '姓名',
  PRIMARY KEY (`id`),
  KEY `idx_number` (`number`),
  KEY `idx_type` (`type`),
  KEY `idx_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT '用户表';

-- 学生
CREATE TABLE `mcg_user_student` (
  `id` int(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `userId` int(20) NOT NULL COMMENT '用户id',
  `classId` int(20) NOT NULL COMMENT '班级id',
  PRIMARY KEY (`id`),
  KEY `idx_userId` (`userId`),
  KEY `idx_classId` (`classId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT '学生表';

-- 老师
CREATE TABLE `mcg_user_teacher` (
  `id` int(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `userId` int(20) NOT NULL COMMENT '用户id',
  `instituteId` int(20) NOT NULL COMMENT '学院id',
  PRIMARY KEY (`id`),
  KEY `idx_userId` (`userId`),
  KEY `idx_instituteId` (`instituteId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT '老师表';

-- 课程基本信息
CREATE TABLE `mcg_course_basic` (
  `id` int(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code` varchar(50) NOT NULL COMMENT '编码',
  `name` int(20) NOT NULL COMMENT '名称',
  `time` int(20) NOT NULL COMMENT '时长',
  PRIMARY KEY (`id`),
  KEY `idx_code` (`code`),
  KEY `idx_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT '课程基本信息表';

-- 课程
CREATE TABLE `mcg_course` (
  `id` int(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
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

-- 学生选课信息
CREATE TABLE `mcg_student_course_relation` (
  `id` int(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `studentId` int(20) NOT NULL COMMENT '学生id',
  `courseId` int(20) NOT NULL COMMENT '课程id',
  PRIMARY KEY (`id`),
  KEY `idx_studentId` (`studentId`),
  KEY `idx_courseId` (`courseId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT '学生选课信息表';

-- 选课中课程信息
CREATE TABLE `mcg_course_remain` (
  `id` int(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `courseId` int(20) NOT NULL COMMENT '课程id',
  `remain` int(20) NOT NULL COMMENT '剩余数目',
  PRIMARY KEY (`id`),
  KEY `idx_courseId` (`courseId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT '选课中课程信息表';



