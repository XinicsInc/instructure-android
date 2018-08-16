package com.instructure.soseedy.producers

object Pipelines {
    val usersPipeline = UserProducer.produceUsers()
    val coursesPipeline = CourseProducer.produceCourses()
}
