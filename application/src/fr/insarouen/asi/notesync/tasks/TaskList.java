package fr.insarouen.asi.notesync.tasks;

import android.util.Log;

import java.util.UUID;
import java.util.ArrayList;
import java.util.Collections;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class TaskList extends ArrayList<Task> implements Serializable {
	private static final String TAG = "NoteSyncService";
	private ArrayList<UUID> deletedTasks = new ArrayList<UUID>();
	private ArrayList<String> projects = new ArrayList<String>();

	/**
	 * Inserts a new task at the adequate position based on its due date and priority.
	 */
	@Override
	public boolean add(Task task) {
		Task.CompareWithDueAndPriority comparator = new Task.CompareWithDueAndPriority();
		int i = Collections.binarySearch(this, task, comparator); 
		i = (i<0) ? (-i)-1 : ++i;
		super.add(i, task);
		if(!projects.contains(task.getProject()) && task.getProject() != null) {
			projects.add(task.getProject());
		}
		return true;
	}

	/**
	 * Returns whether or not a Task has been deleted from the TaskList
	 */
	public boolean deleted(Task task) {
		return deletedTasks.contains(task.getUUID());
	}

	/**
	 * Clears the deleted Tasks 
	 */
	public void clearDeleted() {
		deletedTasks.clear();
	}

	public ArrayList<String> getProjects() {
		return projects;
	}

	/**
	 * Removes a task from the TaskList and stores its UUID
	 */
	@Override
	public boolean remove(Object o) {
		boolean r = super.remove(o);
		deletedTasks.add(((Task)o).getUUID());
		cleanProjects(((Task)o).getProject());
		return r;
	}

	/**
	 * Removes a task from the TaskList and stores its UUID
	 */
	@Override
	public Task remove(int position) {
		Task task = super.remove(position);
		deletedTasks.add(task.getUUID());
		cleanProjects(task.getProject());
		return task;
	}

	private void cleanProjects(String project) {
		boolean finished = true;
		int i = 0;
		int len = this.size();
		while(i < len && finished) {
			Task t = get(i);
			if(t.getProject() != null && t.getProject().equals(project)) {
				finished = false;
			}
			i++;
		}
		if(finished) {
			projects.remove(project);
		}
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

	public String jsonify() {
		StringBuilder sb = new StringBuilder();
		JSONObject jsonTL = new JSONObject();
		JSONArray jsonDeletedTasks = new JSONArray();
		JSONArray jsonProjects = new JSONArray();
		try {
			for (int i = 0; i < deletedTasks.size(); i++)
				jsonDeletedTasks.put(deletedTasks.get(i).toString());
			jsonTL.put("deleted tasks", jsonDeletedTasks.toString());
			for (int i = 0; i < projects.size(); i++)
				jsonProjects.put(projects.get(i));
			jsonTL.put("projects", jsonProjects.toString());
			return jsonTL.toString();
		} catch (JSONException e) {
			Log.d(TAG, "Exception while jsonifying");
			return "";
		}
	}

	public void unJsonify(String json) {
	}
}
