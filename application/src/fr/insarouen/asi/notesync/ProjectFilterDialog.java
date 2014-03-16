package fr.insarouen.asi.notesync;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.AlertDialog;
import android.app.Activity;

import android.os.Bundle;

import android.content.DialogInterface;

import android.view.View;
import android.view.LayoutInflater;

import android.widget.EditText;

public class ProjectFilterDialog extends DialogFragment {
	private ProjectFilterListener listener;
	private EditText project;
	
	public ProjectFilterDialog(ProjectFilterListener pfl) {
		this.listener = pfl;
	}

	public interface ProjectFilterListener {
		public void filterByProject(String project);
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.project);

		LayoutInflater inflater = getActivity().getLayoutInflater();
		View v = inflater.inflate(R.layout.dialog_projectfilter, null);
		project = (EditText)v.findViewById(R.id.project);
		builder.setView(v)
			.setPositiveButton(R.string.filter, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface d, int id) {
					String p = ProjectFilterDialog.this.project.getText().toString().trim();
					ProjectFilterDialog.this.listener.filterByProject(p);
					d.dismiss();
				}
			})
			.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
				@Override 
				public void onClick(DialogInterface d, int id) {
					d.cancel();
				}
			});

		return builder.create();
	}
}
