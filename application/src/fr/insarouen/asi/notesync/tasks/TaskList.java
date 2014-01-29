package fr.insarouen.asi.notesync.tasks;

import java.util.ArrayList;
import java.util.Collections;

import java.io.Serializable;

public class TaskList extends ArrayList<Task> implements Serializable {
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
	
	public static TaskList filterByPriority(TaskList l, Priority p) {
		TaskList filteredList = new TaskList();
		for(Task t : l) {
			if(t.getPriority().equals(p)) {
				filteredList.add(t);
			}
		}
		return filteredList;
	}
	
	public static TaskList filterByProject(TaskList l, String p) {
		TaskList filteredList = new TaskList();
		for(Task t : l) {
			if(t.getProject().equals(p)) {
				filteredList.add(t);
			}
		}
		return filteredList;
	}

	/**
	 * Merges two TaskLists into one
	 */
	public static TaskList merge(TaskList tl1, TaskList tl2) {
		TaskList tf = tl2;
		Task t2;
		for (Task t : tl1) {
			int pos = tf.indexOf(t);
			if(pos == -1) {
				tf.add(t);
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
