
all:
	javac -classpath ../application/bin/classes:/usr/share/java/junit-4.11.jar:/usr/share/java/hamcrest-core.jar -d classes src/fr/insarouen/asi/notesync/tests/tasks/*.java

test:
	java -classpath ../application/bin/classes:/usr/share/java/junit-4.11.jar:/usr/share/java/hamcrest-core.jar:./classes org.junit.runner.JUnitCore fr.insarouen.asi.notesync.tests.tasks.TaskListTest 
