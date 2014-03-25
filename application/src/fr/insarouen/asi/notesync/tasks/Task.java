package fr.insarouen.asi.notesync.tasks;

import java.text.DateFormat; // Use theses two classes for any other manipulations with dates

import java.util.Date; // rightNow = new Date().getTime(); (In Unix Time)
import java.util.UUID;
import java.util.Calendar;
import java.util.Comparator;

import java.io.Serializable;

/**
 * Each task in our todo list is an instance of this class
 */
public class Task implements Serializable {
	private String description;
	private String project=null;
	private Priority priority=Priority.MEDIUM;
	private UUID uuid;

	// Dates as Unix Time integers
	private Calendar due=null;
	private long entry;
	private long modified=-1;

	private DateFormat dateFormat = DateFormat.getDateInstance();

	/* Constructors */
	/** 
	 * Builds a task object giving it a unique UUID and storing the current time as entry date.
	 * @param description
	 * 	The description of the task
	 * @param due
	 * 	Set it to <b>null</b> if not used
	 * @param project
	 * 	Set it to <b>null</b> if not used
	 * @param priority
	 * 	Set it to <b>MEDIUM</b> if not used
	 */
	public Task(String description, Calendar due, String project, Priority priority) {
		this.entry = new Date().getTime();
		this.uuid = UUID.randomUUID();
		this.description = description;
		if(due != null) {
			due.set(Calendar.HOUR_OF_DAY, 0);
			due.set(Calendar.MINUTE, 0);
			due.set(Calendar.SECOND, 0);
			due.set(Calendar.MILLISECOND, 0);
		}
		this.due = due;
		if(project != null && !project.equals("")) {
			this.project = project;
		}
		this.priority = priority;
	}

	/* Setters */ 

	public void setDescription(String description) {
		this.modified = new Date().getTime();
		this.description = description;
	}

	/**
	 * @param date
	 * 	The new date in UNIX time
	 */
	public void setDue(Calendar date) {
		this.modified = new Date().getTime();
		if(date != null) {
			date.set(Calendar.HOUR_OF_DAY, 0);
			date.set(Calendar.MINUTE, 0);
			date.set(Calendar.SECOND, 0);
			date.set(Calendar.MILLISECOND, 0);
		}
		this.due = date;
	}

	public void setProject(String project) {
		this.modified = new Date().getTime();
		if(project != null && project.equals("")) {
			this.project = null;
		} else {
			this.project = project;
		}
	}

	/**
	 * @param priority
	 * 	LOW - MEDIUM - HIGH
	 */
	public void setPriority(Priority priority) {
		this.modified = new Date().getTime();
		this.priority = priority;
	}

	/* Getters */
	public String getDescription() {
		return this.description;
	}

	public String getFormattedDue() {
		if(due != null) {
			return dateFormat.format(due.getTime()); 
		} else {
			return "";
		}
	}

	public Calendar getDue() {
		return due;
	}

	public String getProject() {
		return project;
	}

	public Priority getPriority() {
		return priority;
	}
	public long getModified() {
		return modified;
	}
	public UUID getUUID() {
		return uuid;
	}

	/* Comparators */
	public static class CompareWithDueAndPriority implements Comparator<Task> {
		/**
		 * Note : not consistent with task's .equals
		 */
		public int compare(Task t1, Task t2) {
			Calendar d1 = t1.getDue();
			Calendar d2 = t2.getDue();
			if(d1!=null && d2!=null) {
				int c = d1.compareTo(d2);
				if(c==0) {
					return t1.getPriority().compareTo(t2.getPriority());
				} else {
					return c;
				}
			} else if(d1==null && d2!=null) {
				return 1;
			} else if(d1!= null && d2==null) {
				return -1;
			} else {
				return t1.getPriority().compareTo(t2.getPriority());
			}
		}
	}

	/**
	 * Compares two tasks whith their UUID.
	 */
	public boolean equals(Object o) {
		if(!(o instanceof Task))
			return false;
		return uuid.equals(((Task)o).getUUID());
	}

	public int hashCode() {
		return uuid.hashCode();
	}
}
