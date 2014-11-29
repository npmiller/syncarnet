
all:
	javac -classpath .:../application/bin/classes:/usr/share/java/junit-4.11.jar:/usr/share/java/hamcrest-core.jar -d classes src/fr/syncarnet/tests/tasks/*.java

test:
	java -classpath .:../application/bin/classes:/usr/share/java/junit-4.11.jar:/usr/share/java/hamcrest-core.jar:./classes org.junit.runner.JUnitCore fr.syncarnet.tests.tasks.TaskListTest
