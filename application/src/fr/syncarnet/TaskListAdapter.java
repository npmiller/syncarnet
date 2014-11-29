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

package fr.syncarnet;

import fr.syncarnet.tasks.*;

import android.content.Context;

import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;

import android.widget.Filter;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Filterable;
import android.widget.BaseAdapter;

public class TaskListAdapter extends BaseAdapter implements Filterable {
	private TaskList tasks;
	private TaskList origTasks;
	private LayoutInflater inflater;
	private ProjectsAdapter projects;

	public TaskListAdapter(Context context, TaskList tasks) {
		inflater = LayoutInflater.from(context);
		this.tasks = tasks;
		this.origTasks = tasks;
		projects = new ProjectsAdapter(context, android.R.layout.simple_spinner_item, tasks.getProjects());
	}
	
	public ProjectsAdapter getProjectsAdapter() {
		return projects;
	}
	
	public void setTasks(TaskList tasks) {
		this.tasks = tasks;
	}

	public void resetData() {
		tasks = origTasks;
		notifyDataSetChanged();
	}

	public void removeTask(int position) {
		origTasks.remove(tasks.remove(position));
		notifyDataSetChanged();
		if(getCount() == 0) {
			resetData();
		}
	}

	@Override
	public int getCount() {
		return tasks.size();
	}

	@Override
	public Object getItem(int position) {
		return tasks.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	private class ViewHolder {
		TextView description;
		TextView dueDate;
		TextView project;
		ImageView priority;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		
		if(convertView == null) {
			holder = new ViewHolder();
			convertView = inflater.inflate(R.layout.itemtask, null);
			holder.description = (TextView)convertView.findViewById(R.id.description);
			holder.dueDate = (TextView)convertView.findViewById(R.id.dueDate);
			holder.project = (TextView)convertView.findViewById(R.id.project);
			holder.priority = (ImageView)convertView.findViewById(R.id.priority);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		
		holder.description.setText(tasks.get(position).getDescription());
		holder.dueDate.setText(tasks.get(position).getFormattedDue());
		holder.project.setText(tasks.get(position).getProject());
		switch(tasks.get(position).getPriority()) {
			case HIGH :
				holder.priority.setImageResource(R.drawable.high_priority);
				break;
			case MEDIUM :
				holder.priority.setImageResource(R.drawable.medium_priority);
				break;
			case LOW :
				holder.priority.setImageResource(R.drawable.low_priority);
				break;
		}
		
		return convertView;
	}

	@Override
	public Filter getFilter() {
		return new ProjectFilter();
	}

	@Override
	public void notifyDataSetChanged() {
		projects.notifyDataSetChanged();
		super.notifyDataSetChanged();
	}

	private class ProjectFilter extends Filter {
		@Override
		protected FilterResults performFiltering(CharSequence constraint) {
			FilterResults r = new FilterResults();
			tasks = origTasks;
			if(constraint == null || constraint.length() == 0) {
				r.values = tasks;
				r.count = tasks.size();
			} else {
				TaskList filtered = new TaskList();
				for(Task t : tasks) {
					if(t.getProject() != null && t.getProject().equals(constraint.toString())) {
						filtered.add(t);
					}
				}
				r.values = filtered;
				r.count = filtered.size();
			}
			return r;
		}

		@Override
		protected void publishResults(CharSequence constraint, FilterResults results) {
			if(results.count == 0) {
				notifyDataSetInvalidated();
			} else {
				tasks = (TaskList) results.values;
				notifyDataSetChanged();
			}
		}
	}
}
