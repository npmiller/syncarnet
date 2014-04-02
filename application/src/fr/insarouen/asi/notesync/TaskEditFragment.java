package fr.insarouen.asi.notesync;

import fr.insarouen.asi.notesync.tasks.*;
import fr.insarouen.asi.notesync.helpers.*;

import android.app.Fragment;
import android.app.ActionBar;
import android.app.DatePickerDialog;

import android.os.Bundle;

import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.view.View.OnClickListener;

import android.widget.Toast;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.EditText;
import android.widget.DatePicker;

import java.util.Calendar;

public class TaskEditFragment extends Fragment implements DatePickerDialog.OnDateSetListener {
	private Task task;
	private Calendar cal;

	private Spinner priority = null;
	private EditText project = null;
	private EditText description = null;

	public interface Callbacks {
		public void replaceTask(Task t, boolean orderChanged);
	}

	public TaskEditFragment() {
		super();
	}

	public TaskEditFragment(Task task) {
		super();
		this.task = task;
		this.cal = task.getDue();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if(savedInstanceState != null) {
			task = (Task)savedInstanceState.getSerializable("task");
			cal = (Calendar)savedInstanceState.getSerializable("cal");
		}
		ActionBar ab = getActivity().getActionBar();
		ab.setDisplayShowTitleEnabled(true);
		ab.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable("task", task);
		outState.putSerializable("cal", cal);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup v, Bundle b) {
		View view = inflater.inflate(R.layout.taskedit_fragment, v, false);

		description = (EditText)view.findViewById(R.id.description);
		description.setText(task.getDescription());

		project = (EditText)view.findViewById(R.id.project);
		project.setText(task.getProject());

		Button due = (Button)view.findViewById(R.id.due);
		due.setOnClickListener(new OnClickDueListener());

		Button clearDate = (Button)view.findViewById(R.id.clearDate);
		clearDate.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View V) {
				TaskEditFragment.this.cal = null;
			}
		});

		Button apply = (Button)view.findViewById(R.id.apply);
		apply.setOnClickListener(new OnClickApplyListener());

		priority = (Spinner)view.findViewById(R.id.priority);
		PrioritySpinnerHelper.setPriority(priority, task.getPriority());
		return view;
	}

	private class OnClickApplyListener implements OnClickListener {
		@Override
		public void onClick(View view) {
			String d = description.getText().toString().trim();
			if(d.equals("")) {
				Toast toast = Toast.makeText(getActivity(),
						getActivity().getString(R.string.nodescription),
						Toast.LENGTH_LONG);
				toast.show();
			} else {
				String p = project.getText().toString().trim();
				int pos = priority.getSelectedItemPosition();
				task.setDescription(d);
				if(task.getDue() != null && task.getDue().equals(cal) && task.getPriority().equals(PrioritySpinnerHelper.getPriority(pos))) {
					task.setDue(cal);
					task.setProject(p);
					task.setPriority(PrioritySpinnerHelper.getPriority(pos));
					((Callbacks)getActivity()).replaceTask(task, false);
				} else {
					task.setDue(cal);
					task.setProject(p);
					task.setPriority(PrioritySpinnerHelper.getPriority(pos));
					((Callbacks)getActivity()).replaceTask(task, true);
				}
			}
		}
	}

	private class OnClickDueListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			Calendar c = TaskEditFragment.this.cal;
			if(c == null) 
				c = Calendar.getInstance();
			DatePickerDialog dpd = new DatePickerDialog(getActivity(), TaskEditFragment.this,
					c.get(Calendar.YEAR), 
					c.get(Calendar.MONTH), 
					c.get(Calendar.DAY_OF_MONTH));
			dpd.getDatePicker().setCalendarViewShown(true);
			dpd.getDatePicker().setSpinnersShown(false);
			dpd.show();
		}
	}

	public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
		if(cal==null)
			cal = Calendar.getInstance();
		cal.set(year, monthOfYear, dayOfMonth);
	}
}
