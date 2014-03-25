package fr.insarouen.asi.notesync;

import android.content.Context;

import android.widget.ArrayAdapter;

import java.util.List;

public class ProjectsAdapter extends ArrayAdapter<String> {

	public ProjectsAdapter(Context context, int resource, List<String> projects) {
		super(context, resource, projects);
	}

	@Override
	public String getItem(int position) {
		if(position==0) {
			return getContext().getString(R.string.AllProjects);
		} else {
			return super.getItem(position - 1);
		}
	}

	@Override
	public int getCount() {
		return super.getCount() + 1;
	}

	@Override
	public long getItemId(int position) {
		if(position == 0) {
			return 45122;
		} else {
			return super.getItemId(position - 1);
		}
	}
}

