package com.instructure.canvasapi2.managers

import com.instructure.canvasapi2.StatusCallback
import com.instructure.canvasapi2.apis.ToDoAPI
import com.instructure.canvasapi2.builders.RestBuilder
import com.instructure.canvasapi2.builders.RestParams
import com.instructure.canvasapi2.models.CanvasContext
import com.instructure.canvasapi2.models.ToDo
import java.util.*
import kotlin.collections.HashSet


object ToDoManager : BaseManager() {

    private val mTesting = false

    @JvmStatic
    fun getUserTodos(callback: StatusCallback<List<ToDo>>, forceNetwork: Boolean) {
        if (isTesting() || mTesting) {
            // TODO
        } else {
            val adapter = RestBuilder(callback)
            val params = RestParams.Builder()
                    .withForceReadFromNetwork(forceNetwork)
                    .withPerPageQueryParam(true)
                    .build()
            ToDoAPI.getUserTodos(adapter, params, callback)
        }
    }

    @JvmStatic
    fun getCourseTodos(canvasContext: CanvasContext, callback: StatusCallback<List<ToDo>>, forceNetwork: Boolean) {
        if (isTesting() || mTesting) {
            // TODO
        } else {
            val adapter = RestBuilder(callback)
            val params = RestParams.Builder()
                    .withCanvasContext(canvasContext)
                    .withForceReadFromNetwork(forceNetwork)
                    .build()
            ToDoAPI.getCourseTodos(canvasContext, adapter, params, callback)
        }
    }

    @JvmStatic
    fun getTodos(canvasContext: CanvasContext, callback: StatusCallback<List<ToDo>>, forceNetwork: Boolean) {
        if (canvasContext.type == CanvasContext.Type.USER) {
            getUserTodos(callback, forceNetwork)
        } else {
            getCourseTodos(canvasContext, callback, forceNetwork)
        }
    }

    @JvmStatic
    fun dismissTodo(toDo: ToDo, callback: StatusCallback<Void>) {
        if (isTesting() || mTesting) {
            // TODO
        } else {
            val adapter = RestBuilder(callback)
            val params = RestParams.Builder().build()
            ToDoAPI.dismissTodo(toDo, adapter, params, callback)
        }
    }

    @JvmStatic
    fun getTodosSynchronous(canvasContext: CanvasContext, forceNetwork: Boolean): List<ToDo>? {
        return if (canvasContext.type == CanvasContext.Type.USER) {
            getUserTodosSynchronous(forceNetwork)
        } else {
            getCourseTodosSynchronous(canvasContext, forceNetwork)
        }
    }

    @JvmStatic
    fun getUserTodosSynchronous(forceNetwork: Boolean): List<ToDo>? {
        if (isTesting() || mTesting) {
            // TODO
        } else {
            val adapter = RestBuilder()
            val params = RestParams.Builder()
                    .withForceReadFromNetwork(forceNetwork)
                    .build()
            return ToDoAPI.getUserTodosSynchronous(adapter, params)
        }
        return null
    }

    @JvmStatic
    fun getCourseTodosSynchronous(canvasContext: CanvasContext, forceNetwork: Boolean): List<ToDo>? {
        if (isTesting() || mTesting) {
            // TODO
        } else {
            val adapter = RestBuilder()
            val params = RestParams.Builder()
                    .withForceReadFromNetwork(forceNetwork)
                    .build()
            return ToDoAPI.getCourseTodosSynchronous(canvasContext, adapter, params)
        }
        return null
    }

    @JvmStatic
    fun mergeToDoUpcoming(todoList: List<ToDo>?, eventList: List<ToDo>?): List<ToDo> {
        val todos = todoList ?: emptyList()
        var events = eventList ?: emptyList()

        // Add all Assignment ids from todos
        val assignmentIds = HashSet(todos.filter { it.assignment != null }.map { it.assignment.id })

        // If the set contains any assignment ids from Upcoming, it's a duplicate
        events = events.filter { it.assignment?.id ?: -1 !in assignmentIds }

        // Return combined list, sorted by date
        val defaultDate = Date(0)
        return (todos + events).sortedBy { it.assignment?.dueAt ?: defaultDate }
    }
}
