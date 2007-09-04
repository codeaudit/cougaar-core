/*
 *
 * Copyright 2007 by BBN Technologies Corporation
 *
 */

package org.cougaar.core.plugin;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.cougaar.core.blackboard.TodoSubscription;
import org.cougaar.core.service.AlarmService;
import org.cougaar.util.annotations.Cougaar;

/**
 * An AnnotatedPlugin which manages {@link TodoSubscription}s,
 * the contents of which can be added to via alarm expirations.
 */
public class TodoPlugin extends AnnotatedSubscriptionsPlugin {
    private final Map<String, DeferrableTodoSubscription> todos = 
        new HashMap<String, DeferrableTodoSubscription>();
    
    public void load() {
        super.load();
        for (Field field : getClass().getFields()) {
            if (field.isAnnotationPresent(Cougaar.Todo.class)) {
                Cougaar.Todo todo = field.getAnnotation(Cougaar.Todo.class);
                String key = todo.id();
                DeferrableTodoSubscription todoSubscription = 
                    new DeferrableTodoSubscription(key, this);
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
    }
    
    /**
     * This is public so that {@link DeferrableTodoSubscription} can use it.
     */
    public AlarmService getAlarmService() {
        return alarmService;
    }
    
    protected void execute() {
        super.execute();
        for (TodoSubscription subscription : todos.values()) {
            if (subscription.hasChanged()) {
                @SuppressWarnings("unchecked")
                Collection<TodoItem> items = subscription.getAddedCollection();
                for (TodoItem item : items) {
                    item.execute();
                }
            }
        }
    }
    
    protected void setupSubscriptions() {
        super.setupSubscriptions();
        for (TodoSubscription subscription : todos.values()) {
            blackboard.subscribe(subscription);
        }
    }

}
