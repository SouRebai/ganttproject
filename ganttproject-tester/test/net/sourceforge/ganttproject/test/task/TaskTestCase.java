package net.sourceforge.ganttproject.test.task;

import java.awt.Color;
import java.net.URL;

import junit.framework.TestCase;
import net.sourceforge.ganttproject.GanttCalendar;
import net.sourceforge.ganttproject.TestSetupHelper;
import net.sourceforge.ganttproject.calendar.AlwaysWorkingTimeCalendarImpl;
import net.sourceforge.ganttproject.calendar.GPCalendar;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.roles.RoleManager;
import net.sourceforge.ganttproject.roles.RoleManagerImpl;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.TaskManagerConfig;
import net.sourceforge.ganttproject.task.dependency.TaskDependency;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyException;
import net.sourceforge.ganttproject.time.TimeUnitStack;
import net.sourceforge.ganttproject.time.gregorian.GregorianTimeUnitStack;

/**
 * Created by IntelliJ IDEA. User: bard
 */
public abstract class TaskTestCase extends TestCase {
    private TaskManager myTaskManager;

    protected TaskManager getTaskManager() {
        return myTaskManager;
    }

    protected void setUp() throws Exception {
        super.setUp();
        myTaskManager = newTaskManager();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        myTaskManager = null;
    }
    protected GanttCalendar newFriday() {
        return new GanttCalendar(2004, 9, 15);
    }

    protected GanttCalendar newSaturday() {
        return new GanttCalendar(2004, 9, 16);
    }

    protected GanttCalendar newSunday() {
        return new GanttCalendar(2004, 9, 17);
    }

    protected GanttCalendar newTuesday() {
        return new GanttCalendar(2004, 9, 19);
    }

    protected GanttCalendar newMonday() {
        return new GanttCalendar(2004, 9, 18);
    }

    protected GanttCalendar newWendesday() {
        return new GanttCalendar(2004, 9, 20);
    }

    protected GanttCalendar newThursday() {
        return new GanttCalendar(2004, 9, 21);
    }

    protected TaskManager newTaskManager() {
        return TestSetupHelper.newTaskManagerBuilder().build();
    }

    protected Task createTask() {
        Task result = getTaskManager().createTask();
        result.move(getTaskManager().getRootTask());
        result.setName(String.valueOf(result.getTaskID()));
        return result;
    }

    protected TaskDependency createDependency(Task dependant, Task dependee) throws TaskDependencyException {
        return getTaskManager().getDependencyCollection().createDependency(dependant, dependee);
    }
}