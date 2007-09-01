/*
 *
 * Copyright 2007 by BBN Technologies Corporation
 *
 */

package org.cougaar.core.plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.core.agent.service.alarm.AlarmBase;
import org.cougaar.core.blackboard.TodoSubscription;

/**
 * An AnnotatedPlugin which manages {@link TodoSubscription}s,
 * the contents of which can be added to via alarm expirations.
 */
public class TodoPlugin<T extends TodoItem> extends AnnotatedSubscriptionsPlugin {
    // In some cases we need to add items before the subscription exists.
    // Handle that with a list that holds the early adds until the
    // subscription is created.
    private final Object todoLock = new Object();
    private Map<String, List<T>> todoPending = new HashMap<String, List<T>>();
    
    protected TodoSubscription getTodoSubscription(String todoId) {
        return (TodoSubscription) getSubscription(todoId);
    }
   
    /**
     * Add an item to the todo list
     * @param item the datum to add
     */
    protected void addTodoItem(T item, String todoId) {
        synchronized (todoLock) {
            if (todoPending != null) {
                List<T> todoList = todoPending.get(todoId);
                if (todoList == null) {
                    todoList = new ArrayList<T>();
                    todoPending.put(todoId, todoList);
                }
                todoList.add(item);
                return;
            }
        }
        TodoSubscription todo = getTodoSubscription(todoId);
        if (todo == null) {
            log.error("Couldn't find TodoSubscription " + todoId);
            return;
        }
        todo.add(item);
    }
    
    /**
     * Add a item to the todo list in the future
     * @param futureTime when to add, as an offset from now in millis
     * @param item the datum to add
     */
    protected void addTodoItem(long delay, T item, String todoId) {
        Alarm alarm = new TodoAlarm(System.currentTimeMillis()+delay, item, todoId);
        getAlarmService().addRealTimeAlarm(alarm);
    }
    

    protected void setupSubscriptions() {
        super.setupSubscriptions();
        // Add any pending items that were added before
        // the subscription was made.
        synchronized (todoLock) {
            for (Map.Entry<String, List<T>> entry : todoPending.entrySet()) {
                String id = entry.getKey();
                List<T> todoList = entry.getValue();
                TodoSubscription todo = getTodoSubscription(id);
                todo.addAll(todoList);
            }
            todoPending = null;
        }
    }
    
    private final class TodoAlarm extends AlarmBase {
        private final T item;
        private final String todoId;
        
        public TodoAlarm(long futureTime, T item, String todoId) {
            super(futureTime);
            this.item = item;
            this.todoId = todoId;
        }

        public void onExpire() {
            addTodoItem(item, todoId);
        }
    }

}
