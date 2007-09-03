/*
 *
 * Copyright 2007 by BBN Technologies Corporation
 *
 */

package org.cougaar.core.plugin;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.core.agent.service.alarm.AlarmBase;
import org.cougaar.core.blackboard.TodoSubscription;
import org.cougaar.util.annotations.Cougaar;

/**
 * An AnnotatedPlugin which manages {@link TodoSubscription}s,
 * the contents of which can be added to via alarm expirations.
 */
public class TodoPlugin extends AnnotatedSubscriptionsPlugin {
    // In some cases we need to add items before the subscription exists.
    // Handle that with a list that holds the early adds until the
    // subscription is created.
    private final Object todoLock = new Object();
    private Map<String, List<TodoItem>> todoPending = new HashMap<String, List<TodoItem>>();
    private Map<String, TodoSubscription> todos = new HashMap<String, TodoSubscription>();
    
    /**
     * Add an item to the todo list
     * @param item the datum to add
     */
    protected void addTodoItem(TodoItem item, String todoId) {
        synchronized (todoLock) {
            if (todoPending != null) {
                List<TodoItem> todoList = todoPending.get(todoId);
                if (todoList == null) {
                    todoList = new ArrayList<TodoItem>();
                    todoPending.put(todoId, todoList);
                }
                todoList.add(item);
                return;
            }
        }
        TodoSubscription todo = todos.get(todoId);
        if (todo == null) {
            log.error("Couldn't find TodoSubscription " + todoId);
            return;
        }
        todo.add(item);
    }
    
    /**
     * Add a item to the todo list in the future
     * @param delay when to add, as an offset from now in millis
     * @param item the datum to add
     */
    protected void addTodoItem(long delay, TodoItem item, String todoId) {
        Alarm alarm = new TodoAlarm(System.currentTimeMillis()+delay, item, todoId);
        getAlarmService().addRealTimeAlarm(alarm);
    }
    
    protected void execute() {
        super.execute();
        for (TodoSubscription subscription : todos.values()) {
            if (subscription.hasChanged()) {
                @SuppressWarnings("unchecked")
                Collection<TodoItem> items = subscription.getAddedCollection();
                for (TodoItem item : items) {
                    item.doWork();
                }
            }
        }
    }
    
    protected void setupSubscriptions() {
        super.setupSubscriptions();
        for (Field field : getClass().getFields()) {
            if (field.isAnnotationPresent(Cougaar.Todo.class)) {
                Cougaar.Todo todo = field.getAnnotation(Cougaar.Todo.class);
                String key = todo.id();
                TodoSubscription todoSubscription = new TodoSubscription(key);
                blackboard.subscribe(todoSubscription);
                todos.put(key, todoSubscription);
                try {
                    field.set(this, todoSubscription);
                } catch (Exception e) {
                    String message = "Couldn't set field " +field.getName()+ " of " + this;
                    log.error(message, e);
                    throw new RuntimeException(message, e);
                }
            }
        }
        // Add any pending items that were added before
        // the subscription was made.
        synchronized (todoLock) {
            for (Map.Entry<String, List<TodoItem>> entry : todoPending.entrySet()) {
                String id = entry.getKey();
                List<TodoItem> todoList = entry.getValue();
                TodoSubscription todo = todos.get(id);
                todo.addAll(todoList);
            }
            todoPending = null;
        }
    }
    
    private final class TodoAlarm extends AlarmBase {
        private final TodoItem item;
        private final String todoId;
        
        public TodoAlarm(long futureTime, TodoItem item, String todoId) {
            super(futureTime);
            this.item = item;
            this.todoId = todoId;
        }

        public void onExpire() {
            addTodoItem(item, todoId);
        }
    }

}
