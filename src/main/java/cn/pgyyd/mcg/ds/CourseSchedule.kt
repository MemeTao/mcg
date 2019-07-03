package cn.pgyyd.mcg.ds

data class CourseSchedule(val course: String, val week: Int, val day: Int, val section: Int) : Comparable<CourseSchedule> {
    override fun compareTo(other: CourseSchedule): Int {
        return when {
            this.week != other.week -> this.week  - other.week
            this.day != other.day -> this.day - other.day
            this.section != other.section -> this.section - other.section
            else -> 0
        }
    }
}