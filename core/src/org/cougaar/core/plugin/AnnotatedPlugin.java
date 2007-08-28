/*
 *
 * Copyright 2007 by BBN Technologies Corporation
 *
 */

package org.cougaar.core.plugin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.service.LoggingService;
import org.cougaar.util.IsInstanceOf;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.util.annotations.Cougaar;
import org.cougaar.util.annotations.Cougaar.BlackboardEvt;
import org.cougaar.util.annotations.Cougaar.BlackboardOp;

/**
 * This class provides support for plugins that wish
 * to use annotations to create and manage blackboard
 * subscriptions.
 */
public abstract class AnnotatedPlugin extends ParameterizedPlugin {
    private final List<Invoker> invokers = new ArrayList<Invoker>();
    
    private LoggingService logger;
    
    public void setLoggingService(LoggingService logger) {
        this.logger = logger;
    }
    
    protected void execute() {
        for (Invoker invoker : invokers) {
            invoker.execute();
        }
     }
    
    protected void setupSubscriptions() {
        for (Method method : getClass().getMethods()) {
            if (method.isAnnotationPresent(Cougaar.Execute.class)) {
                Invoker invoker = new Invoker(method);
                invokers.add(invoker);
            }
        }
    }
    
    

    /**
     * Keeps the state of one blackboard subscription created via annotations.
     */
    private class Invoker {
        /**
         * The subscription itself, created via {@link #makeSubscription}.
         */
        private final IncrementalSubscription sub;
        
        /**
         * The annotated method which will be invoked on the plugin, passing in each element
         * of a blackboard collection in turn as an argument.
         */
        private final Method method;
        
        /**
         * The set of operations, as given in the annotation.
         */
        private final Cougaar.BlackboardOp[] ops;
        
      
        public Invoker(Method method) {
            Cougaar.Execute annotation = method.getAnnotation(Cougaar.Execute.class);
            this.method = method;
            this.ops = annotation.on();
            this.sub = makeSubscription(annotation);
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
            // Prefer the 'isa' value if there is one
            Class<?> isa = annotation.isa();
            if (isa != null && isa != Object.class) {
                IsInstanceOf predicate = new IsInstanceOf(isa);
                return (IncrementalSubscription) blackboard.subscribe(predicate);
            }

            // No isa, use the 'when' method
            String testerName = annotation.when().trim();
            if (testerName == null || testerName.length() == 0) {
                logger.error("Neither a 'when' nor a 'isa' clause was provided");
                return null;
            }
            Class<?>[] parameterTypes = {Object.class};
            Class<?> pluginClass = AnnotatedPlugin.this.getClass();
            try {
                final Method tester = pluginClass.getMethod(testerName, parameterTypes);
                Class<?> returnType = tester.getReturnType();
                if (returnType != Boolean.class && returnType != boolean.class) {
                    throw new IllegalArgumentException("Wrong return type");
                }
                UnaryPredicate predicate = new UnaryPredicate() {
                    public boolean execute(Object o) {
                        Object[] args = { o };
                        try {
                            return (Boolean) tester.invoke(AnnotatedPlugin.this, args);
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

        private void execute(BlackboardOp op) {
            Collection<?> objects;
            switch (op) {
                case ADD:
                    objects = sub.getAddedCollection();
                    break;

                case MODIFY:
                    objects = sub.getChangedCollection();
                    break;

                case REMOVE:
                    objects = sub.getRemovedCollection();
                    break;

                default:
                    return;
            }
            Object[] args = new Object[1];
            for (Object object : objects) {
                args[0] = object;
                try {
                    method.invoke(AnnotatedPlugin.this, args);
                } catch (Exception e) {
                    logger.error("Failed to invoke annotated method", e);
                }
            }
        }

        public void execute() {
            if (sub == null) {
                // failed to make a proper subscription
                return;
            }
            for (Cougaar.BlackboardOp op : ops) {
                execute(op);
            }
        }
    }
    
}
