package kallos.repository

import kallos.model.Task

class TaskRepository {
    private val _tasks = mutableListOf<Task>()
    val tasks: List<Task> get() = _tasks.toList()

    fun addTask(task: Task) {
        _tasks.add(task)
    }

    fun updateTask(task: Task) {
        val index = _tasks.indexOfFirst { it.id == task.id }
        if (index != -1) _tasks[index] = task
    }

    fun removeTask(taskId: String) {
        _tasks.removeAll { it.id == taskId }
    }

    fun findById(taskId: String): Task? =
        _tasks.find { it.id == taskId }

    fun replaceAll(tasks: List<Task>) {
        _tasks.clear()
        _tasks.addAll(tasks)
    }
}
