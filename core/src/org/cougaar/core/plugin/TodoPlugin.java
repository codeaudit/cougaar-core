/*
 *
 * Copyright 2007 by BBN Technologies Corporation
 *
 */

package org.cougaar.core.plugin;

import java.util.Collection;

import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.core.agent.service.alarm.AlarmBase;
import org.cougaar.core.blackboard.TodoSubscription;
import org.cougaar.core.util.UniqueObject;

/**
 * An AnnotatedPlugin which manages {@link TodoSubscription}s,
 * the contents of which can be added to via alarm expirations.
 */
public class TodoPlugin extends AnnotatedSubscriptionsPlugin {
    private TodoSubscription todo;
    
    public void load() {
        super.load();
        todo = new TodoSubscription("me");
    }
    
    public void executeLater(Runnable runnable) {
        todo.add(runnable);
    }
    
    public Alarm executeLater(long delayMillis, Runnable runnable) {
        if (delayMillis <= 0) {
            executeLater(runnable);
            return null;
        } else {
            Alarm alarm = new TodoAlarm(System.currentTimeMillis()+delayMillis, runnable);
            getAlarmService().addRealTimeAlarm(alarm);
            return alarm;
        }
    }
        
    protected void execute() {
        super.execute();
        @SuppressWarnings("unchecked")
        Collection<Runnable> items = todo.getAddedCollection();
        for (Runnable item : items) {
            item.run();
        }
    }
    
    protected void setupSubscriptions() {
        super.setupSubscriptions();
        blackboard.subscribe(todo);
    }
    
    protected void publishAddLater(UniqueObject object) {
        publishAddLater(0, object);
    }
    
    protected Alarm publishAddLater(long delay, final UniqueObject object) {
        return executeLater(delay, new Runnable() {
            public void run() {
                blackboard.publishAdd(object);
            }
        });
    }
    
    protected void publishRemoveLater(UniqueObject object) {
        publishRemoveLater(0, object);
    }
    
    protected Alarm publishRemoveLater(long delay, final UniqueObject object) {
        return executeLater(delay, new Runnable() {
            public void run() {
                blackboard.publishRemove(object);
            }
        });
    }
   
    protected void publishChangeLater(UniqueObject object) {
        publishChangeLater(0, object);
    }
    
    protected Alarm publishChangeLater(long delay, final UniqueObject object) {
        return executeLater(delay, new Runnable() {
            public void run() {
                blackboard.publishChange(object);
            }
        });
    }
    
    protected void publishChangeLater(UniqueObject object, Collection<?> changeReports) {
        publishChangeLater(0, object, changeReports);
    }
    
    protected Alarm publishChangeLater(long delay, 
                                       final UniqueObject object,
                                       final Collection<?> changeReports) {
        return executeLater(delay, new Runnable() {
            public void run() {
                blackboard.publishChange(object, changeReports);
            }
        });
    }

    
    private final class TodoAlarm extends AlarmBase {
        private final Runnable item;
        
        public TodoAlarm(long futureTime, Runnable item) {
            super(futureTime);
            this.item = item;
        }
        
        public void onExpire() {
            todo.add(item);
        }
    }
}
