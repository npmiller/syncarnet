package fr.insarouen.asi.notesync.tasks;

import java.util.UUID;
import java.util.ArrayList;
import java.util.Collections;

import java.io.Serializable;

public class TaskList extends ArrayList<Task> implements Serializable {
	private ArrayList<UUID> deletedTasks = new ArrayList<UUID>();

	/**
	 * Inserts a new task at the adequate position based on its due date and priority.
	 */
	@Override
	public boolean add(Task task) {
		Task.CompareWithDueAndPriority comparator = new Task.CompareWithDueAndPriority();
		int i = Collections.binarySearch(this, task, comparator); 
		i = (i<0) ? (-i)-1 : ++i;
		super.add(i, task);
		return true;
	}

	/**
	 * Returns whether or not a Task has been deleted from the TaskList
	 */
	public boolean deleted(Task task) {
		return deletedTasks.contains(task.getUUID());
	}

	/**
	 * Removes a task from the TaskList and stores its UUID
	 */
	@Override
	public boolean remove(Object o) {
		deletedTasks.add(((Task)o).getUUID());
		return super.remove(o);
	}

	/**
	 * Removes a task from the TaskList and stores its UUID
	 */
	@Override
	public Task remove(int position) {
		Task task = super.remove(position);
		deletedTasks.add(task.getUUID());
		return task;
	}

	/**
	 * Merges two TaskLists into one
	 */
	public static TaskList merge(TaskList tl1, TaskList tl2) {
		TaskList tf = tl2;
		Task t2;
		for(Task t : tf) {
			if(tl1.deleted(t)) {
				tf.remove(t);
			}
		}
		for(Task t : tl1) {
			int pos = tf.indexOf(t);
			if(pos == -1) {
				if(!tf.deleted(t)) {
					tf.add(t);
				}
			} else {
				t2 = tf.get(pos);
				if(t.getModified() != t2.getModified()) {
					if(t.getDue() != null && t.getDue().equals(t2.getDue()) &&
							t.getPriority().equals(t2.getPriority())) {
						tf.set(pos, (t.getModified()>t2.getModified()) ? t : t2);
					} else {
						tf.remove(pos);
						tf.add((t.getModified()>t2.getModified()) ? t : t2);
					}
				}
			}
		}
		return tf;
	}
}
