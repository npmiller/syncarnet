package fr.insarouen.asi.notesync;

import fr.insarouen.asi.notesync.tasks.*;

import android.content.Context;

import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;

import android.widget.Filter;
//import android.widget.Filter.FilterResults;
import android.widget.TextView;
import android.widget.Filterable;
import android.widget.BaseAdapter;

public class TaskListAdapter extends BaseAdapter implements Filterable {
	private TaskList tasks;
	private TaskList origTasks;
	private LayoutInflater inflater;

	public TaskListAdapter(Context context, TaskList tasks) {
		inflater = LayoutInflater.from(context);
		this.tasks = tasks;
		this.origTasks = tasks;
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
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		
		holder.description.setText(tasks.get(position).getDescription());
		holder.dueDate.setText(tasks.get(position).getFormattedDue());
		holder.project.setText(tasks.get(position).getProject());
		
		return convertView;
	}

	@Override
	public Filter getFilter() {
		return new ProjectFilter();
	}

	private class ProjectFilter extends Filter {
		@Override
		protected FilterResults performFiltering(CharSequence constraint) {
			FilterResults r = new FilterResults();
			if(constraint == null || constraint.length() == 0) {
				r.values = tasks;
				r.count = tasks.size();
			} else {
				TaskList filtered = new TaskList();
				for(Task t : tasks) {
					if(t.getProject().equals(constraint.toString())) {
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
