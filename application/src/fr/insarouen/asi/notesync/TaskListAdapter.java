package fr.insarouen.asi.notesync;

import fr.insarouen.asi.notesync.tasks.*;

import android.content.Context;

import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;

import android.widget.TextView;
import android.widget.BaseAdapter;

public class TaskListAdapter extends BaseAdapter {
	private TaskList tasks;
	private LayoutInflater inflater;

	public TaskListAdapter(Context context, TaskList tasks) {
		inflater = LayoutInflater.from(context);
		this.tasks = tasks;
	}

	public void setTasks(TaskList tasks) {
		this.tasks = tasks;
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
}
