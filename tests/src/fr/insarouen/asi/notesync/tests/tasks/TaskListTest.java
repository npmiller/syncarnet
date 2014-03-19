package fr.insarouen.asi.notesync.tests.tasks;


import fr.insarouen.asi.notesync.tasks.*;
import static org.junit.Assert.*;
import org.junit.Before;
import java.util.Calendar;

import org.junit.Test;

public class TaskListTest {
	private TaskList tl1;
	private TaskList tl2;

	@Before
	public void setUp() {
	}

	@Test
	public void test_order_priority() {
		TaskList tl1 = new TaskList();
		tl1.add(new Task("t1", null, null, Priority.MEDIUM)); 
		tl1.add(new Task("t2", null, null, Priority.LOW)); 
		tl1.add(new Task("t3", null, null, Priority.HIGH)); 
		assertEquals(tl1.get(0).getDescription(), "t3");
		assertEquals(tl1.get(1).getDescription(), "t1");
		assertEquals(tl1.get(2).getDescription(), "t2");
	}

	@Test
	public void test_merge_deleted() {
		Task t = new Task("t11", null, null, Priority.MEDIUM);
		TaskList tl1 = new TaskList();
		tl1.add(t); 
		tl1.add(new Task("t12", null, null, Priority.LOW)); 
		tl1.add(new Task("t13", null, null, Priority.HIGH)); 
		TaskList tl2 = new TaskList();
		tl2.add(t); 
		tl2.add(new Task("t22", null, null, Priority.LOW)); 
		tl2.add(new Task("t23", null, null, Priority.HIGH)); 
		tl2.remove(t);
		assertEquals(true, tl2.deleted(t));
		TaskList tf = TaskList.merge(tl1, tl2);
		assertEquals(true, tf.deleted(t));
		assertEquals(4, tf.size());
	}
}
