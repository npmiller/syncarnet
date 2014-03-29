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

public class TaskAddFragment extends Fragment implements DatePickerDialog.OnDateSetListener {
	private Calendar cal = null;

	private Spinner priority = null;
	private EditText project = null;
	private EditText description = null;

	public interface Callbacks {
		public void addTask(Task t);
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if(savedInstanceState != null) {
			cal = (Calendar)savedInstanceState.getSerializable("cal");
		}
		ActionBar ab = getActivity().getActionBar();
		ab.setDisplayShowTitleEnabled(true);
		ab.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable("cal", cal);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup v, Bundle b) {
		View view = inflater.inflate(R.layout.taskadd_fragment, v, false);

		Button due = (Button)view.findViewById(R.id.due);
		due.setOnClickListener(new OnClickDueListener());

		Button add = (Button)view.findViewById(R.id.add);
		add.setOnClickListener(new OnClickAddListener()); 

		description = (EditText)view.findViewById(R.id.description);
		project = (EditText)view.findViewById(R.id.project);

		priority = (Spinner)view.findViewById(R.id.priority);
		priority.setSelection(1);

		return view;
	}

	private class OnClickAddListener implements OnClickListener {
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
				((Callbacks)getActivity()).addTask(new Task(d, cal, p, PrioritySpinnerHelper.getPriority(pos)));
			}
		}
	}

	private class OnClickDueListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			Calendar c = Calendar.getInstance();
			DatePickerDialog dpd = new DatePickerDialog(getActivity(), TaskAddFragment.this,
					      c.get(Calendar.YEAR), 
					      c.get(Calendar.MONTH), 
					      c.get(Calendar.DAY_OF_MONTH));
			dpd.getDatePicker().setCalendarViewShown(true);
			dpd.getDatePicker().setSpinnersShown(false);
			dpd.show();
		}
	}

	public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
		cal = Calendar.getInstance();
		cal.set(year, monthOfYear, dayOfMonth);
	}
}
