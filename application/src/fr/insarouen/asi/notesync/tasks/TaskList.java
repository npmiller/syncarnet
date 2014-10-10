/*
 * Copyright (C) 2013-14 Nicolas Miller, Florian Paindorge
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

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
		JSONObject jsonTL = new JSONObject();
		JSONArray jsonTasks = new JSONArray();
		JSONArray jsonDeletedTasks = new JSONArray();
		JSONArray jsonProjects = new JSONArray();
		try {
			for (int i = 0; i < this.size(); i++)
				jsonTasks.put(this.get(i).jsonify());
			jsonTL.put("tasks", jsonTasks.toString());
			Log.d(TAG, "Added tasks to json");
			for (int i = 0; i < deletedTasks.size(); i++)
				jsonDeletedTasks.put(deletedTasks.get(i).toString());
			jsonTL.put("deletedTasks", jsonDeletedTasks.toString());
			Log.d(TAG, "Added deleted tasks to json");
			for (int i = 0; i < projects.size(); i++)
				jsonProjects.put(projects.get(i));
			jsonTL.put("projects", jsonProjects.toString());
			Log.d(TAG, "Added projects to json");
			return jsonTL.toString();
		} catch (JSONException e) {
			Log.d(TAG, "Exception while jsonifying");
			return "";
		}
	}

	public void unJsonify(String json) {
		try {
			JSONObject jsonTL = new JSONObject(json);
			JSONArray jsonTasks = new JSONArray(jsonTL.getString("tasks"));
			Task taskTemp;
			for (int i = 0; i< jsonTasks.length(); i++) {
				taskTemp = new Task();
				taskTemp.unJsonify(jsonTasks.getString(i));
				this.add(taskTemp);
			}
			Log.d(TAG, "Recreated tasks from json");
			JSONArray jsonDeletedTasks = new JSONArray(jsonTL.getString("deletedTasks"));
			for (int i = 0; i< jsonDeletedTasks.length(); i++)
				this.deletedTasks.add(UUID.fromString(jsonDeletedTasks.getString(i)));
			Log.d(TAG, "Recreated deleted tasks from json");
			JSONArray jsonProjects = new JSONArray(jsonTL.getString("projects"));
			for (int i = 0; i< jsonProjects.length(); i++)
				this.projects.add(jsonProjects.getString(i));
			Log.d(TAG, "Recreated projects from json");
		} catch (JSONException e) {
			Log.d(TAG, "Exception while unjsonifying");
		}
	}
}
