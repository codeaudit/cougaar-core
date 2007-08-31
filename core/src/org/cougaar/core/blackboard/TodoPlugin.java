/*
 *
 * Copyright 2007 by BBN Technologies Corporation
 *
 */

package org.cougaar.core.blackboard;

import java.util.ArrayList;
import java.util.List;

import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.core.agent.service.alarm.AlarmBase;
import org.cougaar.core.plugin.AnnotatedSubscriptionsPlugin;
import org.cougaar.util.annotations.Cougaar;
import org.cougaar.util.annotations.Subscribe;

/**
 * An AnnotatedPlugin which manages a single {@link TodoSubscription},
 * the contents of which can be added to via an alarm expiration.
 */
public class TodoPlugin<T extends TodoItem> extends AnnotatedSubscriptionsPlugin {
    protected static final String TODO_ID = "todo";
    
    // In some cases we need to add items before the subscription exists.
    // Handle that with a list that holds the early adds until the
    // subscription is created.
    private final Object todoLock = new Object();
    private List<T> todoPending = new ArrayList<T>();
    
    protected TodoSubscription getTodoSubscription() {
        return (TodoSubscription) getSubscription(TODO_ID);
    }
   
    /**
     * Add an item to the todo list
     * @param item the datum to add
     */
    protected void addTodoItem(T item) {
        synchronized (todoLock) {
            if (todoPending != null) {
                todoPending.add(item);
                return;
            }
        }
        TodoSubscription todo = getTodoSubscription();
        if (todo == null) {
            return;
        }
        todo.add(item);
    }
    
    /**
     * Add a item to the todo list in the future
     * @param futureTime when to add, as an offset from now in millis
     * @param item the datum to add
     */
    protected void addTodoItem(long delay, T item) {
        Alarm alarm = new TodoAlarm(System.currentTimeMillis()+delay, item);
        getAlarmService().addRealTimeAlarm(alarm);
    }
    
    
    /**
     * Do the domain-specific work on one item.
     * No-op here, subclasses will almost always override.
     * Overriding methods must include the annotation!
     * 
     * @param item datum to work on.
     */
    @Cougaar.Execute(on=Subscribe.ModType.ADD, todo=TODO_ID)
    public void executeToDoItem(T item) {
    }

    @Override
    protected void setupSubscriptions() {
       super.setupSubscriptions();
       // Add any pending items that were added before
       // the subscription was made.
       synchronized (todoLock) {
            TodoSubscription todo = getTodoSubscription();
            todo.addAll(todoPending);
            todoPending = null;
        }
    }
    
    public class TodoAlarm extends AlarmBase {
        private final T item;
        
        public TodoAlarm(long futureTime, T item) {
            super(futureTime);
            this.item = item;
        }

        public void onExpire() {
            addTodoItem(item);
        }
    }

}
