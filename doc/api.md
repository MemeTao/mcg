# MCG API接口
HEAD|SUMMARY
-----|--------
Author:| Numbaa, 
Status:| Draft
Date:| 15-June-2019

### 1.登录接口
#### 接口说明
> 学生从登录页面输入学号密码，点击登录会将学号密码发送至学校统一身份认证中心，取回token，再将学号与密码通过此接口发送到选课系统，系统认证通过后，页面将重定向至选课系统主页。
#### URL
> www.example.com/login
#### 请求方式
> POST
#### 请求参数
|参数|必选|类型|说明|
|:----- |:-------|:-----|----- |
|uid |true |int|学生学号 |
|token |true |string |学生从学校取到的token|
#### 返回结果
|返回字段|字段类型|说明 |
|:----- |:------|:----------------------------- |
|status_code | int |返回结果状态。0：登录成功；1：登录失败 |
|msg| int | 登录失败原因 |
 
---
### 2.课程信息接口
#### 接口说明
> 获取选课列表中的静态信息，如任课老师、课程描述、上课时间等
#### URL
> www.example.com/courseinfo
#### 请求方式
> POST
#### 请求参数
|参数|必选|类型|说明|
|:----- |:-------|:-----|----- |
|uid |true |int|学生学号 |
|type |true |string |课程类别，pe-体育，university-校选，college-院选|
#### 返回结果
|返回字段|字段类型|说明 |
|:----- |:------|:----------------------------- |
|status_code | int |返回结果状态。0：正常；1：错误。 |
|data | array | 课程信息数组 |
|data.id | int |课程id|
|data.name|string|课程名称|
|data.summary|string|课程简介|
|data.teacher|string|任课老师|
|data.credi|int|学分|
|data.capacity|int|课程容量|
|data.period|string|上课时间(用string格式是临时的，只是为了展示在前端页面)|


---
### 3.课程剩余人数接口
#### 接口说明
> 获取课程剩余可选人数
#### URL
> www.example.com/remain
#### 请求方式
> POST
#### 请求参数
|参数|必选|类型|说明|
|:----- |:-------|:-----|----- |
|courseids|true |string|课程id，如10001,10002,10003 |
#### 返回结果
|返回字段|字段类型|说明 |
|:----- |:------|:----------------------------- |
|status_code | int |返回结果状态。0：成功；1：失败|
|msg| string | 请求失败原因 |
|data|array|课程剩余人数|
|data.courseid|int|课程id|
|data.remain|int|剩余人数|


---
### 4.选课请求接口
#### URL
> www.example.com/select
#### 接口说明
> 提交选中课程
#### 请求方式
> POST
#### 请求参数
|参数|必选|类型|说明|
|:----- |:-------|:-----|----- |
|uid |true |int|学生学号 |
|courseids |true |string |课程id，适用半角逗号连接，如"23434,234345,34534"|
#### 返回结果
|返回字段|字段类型|说明 |
|:----- |:------|:----------------------------- |
|status_code | int |返回结果状态。0：选课成功；1：选课失败；3：选课排队中 |
|jobid| string | 排队id |

---
### 5.查询选课结果接口
#### URL
> www.example.com/check
#### 请求方式
> POST
#### 请求参数
|参数|必选|类型|说明|
|:----- |:-------|:-----|----- |
|jobid |true |string|选课排队id |
#### 返回结果
|返回字段|字段类型|说明 |
|:----- |:------|:----------------------------- |
|status_code | int |返回结果状态。0：成功；1：失败 |
|msg| string | 失败原因 |
|results|array|选课结果|
|results.course|int|课程id|
|results.success|bool|选课成功与否|
