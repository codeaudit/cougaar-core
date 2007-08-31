/*
 *
 * Copyright 2007 by BBN Technologies Corporation
 *
 */

package org.cougaar.core.blackboard;

import java.util.ArrayList;
import java.util.List;

import org.cougaar.core.agent.service.alarm.AlarmBase;
import org.cougaar.core.plugin.AnnotatedPlugin;
import org.cougaar.util.annotations.Cougaar;
import org.cougaar.util.annotations.Subscribe;

/**
 * An AnnotatedPlugin which manages a single {@link TodoSubscription},
 * the contents of which are added to via an alarm expiration.
 */
public class TodoPlugin<T extends TodoItem> extends AnnotatedPlugin {
    private static final String TODO_ID = "todo";
    
    // In some cases we need to add items before the subscription exists.
    // Handle that with a list that holds the early adds until the
    // subscription is created.
    private final Object todoLock = new Object();
    private List<T> todoPending = new ArrayList<T>();
    
    protected TodoSubscription getTodoSubscription() {
        return (TodoSubscription) getSubscription(TODO_ID);
    }
   
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

    // No-op here, subclasses usually override but don't have to.
    protected void doTodoItem(T item) {
    }
    
    @Cougaar.Execute(on=Subscribe.ModType.ADD, todo=TODO_ID)
    public final void executeToDoItem(T item) {
        doTodoItem(item);
    }

    @Override
    protected void setupSubscriptions() {
       super.setupSubscriptions();
       synchronized (todoLock) {
            TodoSubscription todo = getTodoSubscription();
            todo.addAll(todoPending);
            todoPending = null;
        }
    }
    
    public class TodoAlarm extends AlarmBase {
        private final T item;
        
        public TodoAlarm(T item, long futureTime) {
            super(futureTime);
            this.item = item;
        }

        public void onExpire() {
            addTodoItem(item);
        }
    }

}
