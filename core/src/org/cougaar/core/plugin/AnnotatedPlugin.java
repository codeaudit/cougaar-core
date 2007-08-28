/*
 *
 * Copyright 2007 by BBN Technologies Corporation
 *
 */

package org.cougaar.core.plugin;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.service.LoggingService;
import org.cougaar.util.IsInstanceOf;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.util.annotations.Cougaar;
import org.cougaar.util.annotations.Cougaar.BlackboardEvt;

/**
 * This class provides support for plugins that wish
 * to use annotations to create and manage blackboard
 * subscriptions.
 */
public abstract class AnnotatedPlugin extends ParameterizedPlugin {
    private final Map<IncrementalSubscription, Method> adds = 
        new HashMap<IncrementalSubscription, Method>();
    
    private final Map<IncrementalSubscription, Method> removes = 
        new HashMap<IncrementalSubscription, Method>();
    
    private final Map<IncrementalSubscription, Method> modifies = 
        new HashMap<IncrementalSubscription, Method>();
    
    private LoggingService logger;
    
    public void setLoggingService(LoggingService logger) {
        this.logger = logger;
    }
    
    private void executeAdds() {
        for (Map.Entry<IncrementalSubscription, Method> entry  : adds.entrySet()) {
            IncrementalSubscription sub = entry.getKey();
            Method method = entry.getValue();
            Object[] args = new Object[1];
            for (Object added : sub.getAddedCollection()) {
                args[0] = added;
                try {
                    method.invoke(this, args);
                } catch (Exception e) {
                    logger.error("Failed to invoke annotated method", e);
                }
            }
        }
    }
    
    private void executeModifies() {
        for (Map.Entry<IncrementalSubscription, Method> entry  : modifies.entrySet()) {
            IncrementalSubscription sub = entry.getKey();
            Method method = entry.getValue();
            Object[] args = new Object[1];
            for (Object changed : sub.getChangedCollection()) {
                args[0] = changed;
                try {
                    method.invoke(this, args);
                } catch (Exception e) {
                    logger.error("Failed to invoke annotated method", e);
                }   
            }
        }
    }
    
    private void executeRemoves() {
        for (Map.Entry<IncrementalSubscription, Method> entry  : removes.entrySet()) {
            IncrementalSubscription sub = entry.getKey();
            Method method = entry.getValue();
            Object[] args = new Object[1];
            for (Object removed : sub.getRemovedCollection()) {
                args[0] = removed;
                try {
                    method.invoke(this, args);
                } catch (Exception e) {
                    logger.error("Failed to invoke annotated method", e);
                }
            }
        }
    }
    
    private Map<IncrementalSubscription, Method>[] getSubscriptions(Cougaar.Execute annotation) {
        Cougaar.BlackboardOp[] ops = annotation.on();
        @SuppressWarnings("unchecked")
        Map<IncrementalSubscription, Method>[] maps = new Map[ops.length];
        for (int i=0; i<ops.length; i++) {
            Cougaar.BlackboardOp op = ops[i];
            switch (op) {
                case ADD:
                    maps[i] = adds;
                    break;
                    
                case MODIFY:
                    maps[i] = modifies;
                    break;
                    
                case REMOVE:
                    maps[i] = removes;
            }
        }
        return maps;
    }

    private IncrementalSubscription createAlarmSubscription(Cougaar.Execute annotation) {
        String message = "The ALARM type is not supported yet";
        logger.error(message);
        return null;
    }
    
    private IncrementalSubscription createTodoSubscription(Cougaar.Execute annotation) {
        String message = "The TODO type is not supported yet";
        logger.error(message);
        return null;
    }
    
    private IncrementalSubscription createBlackboardSubscription(Cougaar.Execute annotation) {
        Class<?>[] parameterTypes = { Object.class };
        Class<?> isa = annotation.isa();
        if (isa != null) {
            IsInstanceOf predicate = new IsInstanceOf(isa);
            return (IncrementalSubscription) blackboard.subscribe(predicate);
        }
        String testerName = annotation.when();
        if (testerName == null || testerName.length() == 0) {
            logger.error("Neither a 'when' nor a 'isa' clause was provided");
            return null;
        }
        try {
            final Method tester = getClass().getMethod(testerName, parameterTypes);
            if (tester.getReturnType() != Boolean.class) {
                throw new IllegalArgumentException("Wrong return type");
            }
            UnaryPredicate predicate = new UnaryPredicate() {
                public boolean execute(Object o) {
                    try {
                        return (Boolean) tester.invoke(this, args);
                    } catch (Exception e) {
                        logger.error("Test failed", e);
                        return false;
                    }
                }
                
            };
            return (IncrementalSubscription) blackboard.subscribe(predicate);
        } catch (Exception e) {
            logger.error("Couldn't find method " + testerName, e);
            return null;
        }
    }
    
    private IncrementalSubscription makeSubscription(Cougaar.Execute annotation) {
        BlackboardEvt from = annotation.from();
        switch (from) {
            case BLACKBOARD:
                return createBlackboardSubscription(annotation);
                
            case ALARM:
                return createAlarmSubscription(annotation);
                
            case TODO:
                return createTodoSubscription(annotation);
                
            default:
                logger.error("Weird 'from' spec " + from);
                return null;
        }
    }
    
    
    protected void execute() {
        executeAdds();
        executeModifies();
        executeRemoves();
     }
    
    protected void setupSubscriptions() {
        for (Method method : getClass().getMethods()) {
            if (method.isAnnotationPresent(Cougaar.Execute.class)) {
                Cougaar.Execute annotation = method.getAnnotation(Cougaar.Execute.class);
                IncrementalSubscription subscription = makeSubscription(annotation);
                if (subscription == null) {
                    continue;
                }
                Map<IncrementalSubscription, Method>[] maps = getSubscriptions(annotation);
                for (Map<IncrementalSubscription, Method> map : maps) {
                    map.put(subscription, method);
                }
            }
        }
    }
}
