package fr.insarouen.asi.notesync.tests.tasks;


import fr.insarouen.asi.notesync.tasks.*;
import static org.junit.Assert.*;
import org.junit.Before;
import java.util.Calendar;

import org.junit.Test;

public class TaskListTest {
	private TaskList tl1;
	private TaskList tl2;

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
	public void test_order_calendar() {
		TaskList tl1 = new TaskList();
		Calendar c1 = Calendar.getInstance();
		c1.set(2014,1,2);
		Calendar c2 = Calendar.getInstance();
		c2.set(2014,1,3);
		Calendar c3 = Calendar.getInstance();
		c3.set(2014,1,1);
		tl1.add(new Task("t1", c1, null, Priority.MEDIUM)); 
		tl1.add(new Task("t2", c2, null, Priority.LOW)); 
		tl1.add(new Task("t3", c3, null, Priority.HIGH)); 
		assertEquals(tl1.get(0).getDescription(), "t3");
		assertEquals(tl1.get(1).getDescription(), "t1");
		assertEquals(tl1.get(2).getDescription(), "t2");
	}

	@Test
	public void test_order_mixed() {
		TaskList tl1 = new TaskList();
		Calendar c1 = Calendar.getInstance();
		c1.set(2014,1,2);
		Calendar c2 = Calendar.getInstance();
		c2.set(2014,1,1);
		tl1.add(new Task("t1", c1, null, Priority.MEDIUM)); 
		tl1.add(new Task("t2", c2, null, Priority.LOW)); 
		tl1.add(new Task("t3", c2, null, Priority.HIGH)); 
		assertEquals(tl1.get(0).getDescription(), "t3");
		assertEquals(tl1.get(1).getDescription(), "t2");
		assertEquals(tl1.get(2).getDescription(), "t1");
	}

	@Test
	public void test_remove() {
		Task t = new Task("t1", null, null, Priority.MEDIUM);
		TaskList tl1 = new TaskList();
		tl1.add(t); 
		tl1.add(new Task("t2", null, null, Priority.LOW)); 
		tl1.add(new Task("t3", null, null, Priority.HIGH)); 
		tl1.remove(t);
		assertTrue(tl1.deleted(t));
		assertFalse(tl1.contains(t));
	}

	@Test
	public void test_remove_index() {
		Task t = new Task("t1", null, null, Priority.HIGH);
		TaskList tl1 = new TaskList();
		tl1.add(t); 
		tl1.add(new Task("t2", null, null, Priority.LOW)); 
		tl1.add(new Task("t3", null, null, Priority.MEDIUM)); 
		tl1.remove(0);
		assertTrue(tl1.deleted(t));
		assertFalse(tl1.contains(t));
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
		assertTrue(tl2.deleted(t));
		TaskList tf = TaskList.merge(tl1, tl2);
		assertTrue(tf.deleted(t));
		assertEquals(4, tf.size());
	}

	@Test
	public void test_merge() {
		Task t = new Task("t11", null, null, Priority.MEDIUM);
		TaskList tl1 = new TaskList();
		tl1.add(t); 
		tl1.add(new Task("t12", null, null, Priority.LOW)); 
		tl1.add(new Task("t13", null, null, Priority.HIGH)); 
		TaskList tl2 = new TaskList();
		tl2.add(t);
		t.setDescription("t11-test");
		tl2.add(new Task("t22", null, null, Priority.LOW)); 
		tl2.add(new Task("t23", null, null, Priority.HIGH)); 
		TaskList tf = TaskList.merge(tl1, tl2);
		assertEquals("t23", tf.get(0).getDescription());
		assertEquals("t13", tf.get(1).getDescription());
		assertEquals("t11-test", tf.get(2).getDescription());
		assertEquals("t22", tf.get(3).getDescription());
		assertEquals("t12", tf.get(4).getDescription());
		assertEquals(5, tf.size());
	}
}
