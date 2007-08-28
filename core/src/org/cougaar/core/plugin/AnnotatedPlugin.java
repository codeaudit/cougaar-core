/*
 *
 * Copyright 2007 by BBN Technologies Corporation
 *
 */

package org.cougaar.core.plugin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.blackboard.Subscription;
import org.cougaar.core.blackboard.TodoSubscription;
import org.cougaar.core.service.LoggingService;
import org.cougaar.util.IsInstanceOf;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.util.annotations.Cougaar;
import org.cougaar.util.annotations.Cougaar.BlackboardOp;

/**
 * This class provides support for plugins that wish
 * to use annotations to create and manage blackboard
 * subscriptions.
 */
public abstract class AnnotatedPlugin extends ParameterizedPlugin {
    private final Map<String, Subscription> subscriptions = new HashMap<String, Subscription>();
    private final List<Invoker> invokers = new ArrayList<Invoker>();
    protected LoggingService log;
    
    public final void setLoggingService(LoggingService logger) {
        this.log = logger;
    }
    
    protected void execute() {
        for (Invoker invoker : invokers) {
            invoker.execute();
        }
     }
    
    protected void setupSubscriptions() {
        for (Method method : getClass().getMethods()) {
            if (method.isAnnotationPresent(Cougaar.Execute.class)) {
                Cougaar.Execute annotation = method.getAnnotation(Cougaar.Execute.class);
                String id;
                String todo = annotation.todo().trim();
                Class<?> isa =  annotation.isa();
                if (todo.length() > 0) {
                    id = todo;
                } else if (!Cougaar.isNoClass(isa)) {
                    id = isa.getName();
                } else {
                    id = annotation.when().trim();
                }
                Subscription subscription = subscriptions.get(id);
                if (subscription != null) {
                    // TODO Make a new invoker with the old invoker's subscription
                } else {
                    Invoker invoker = new Invoker(method, annotation);
                    subscriptions.put(id, invoker.sub);
                    invokers.add(invoker);
                }
            }
        }
    }
    
    public Subscription getSubscription(String id) {
        return subscriptions.get(id);
    }
    
    

    /**
     * Keeps the state of one blackboard subscription created via annotations.
     */
    private class Invoker {
        /**
         * The subscription itself, created via {@link #makeSubscription}.
         */
        private final Subscription sub;
        
        /**
         * The annotated method which will be invoked on the plugin, passing in each element
         * of a blackboard collection in turn as an argument.
         */
        private final Method method;
        
        /**
         * The set of operations, as given in the annotation.
         */
        private final Cougaar.BlackboardOp[] ops;
        
        public Invoker(Method method, Cougaar.Execute annotation, Subscription sub) {
            this.method = method;
            this.ops = annotation.on();
            this.sub = sub;
        }
        
        public Invoker(Method method, Cougaar.Execute annotation) {
            this.method = method;
            this.ops = annotation.on();
            this.sub = makeSubscription(annotation);
            
        }

        private Subscription createTodoSubscription(Cougaar.Execute annotation) {
            String key = annotation.todo().trim();
            if (key.length() == 0) {
                log.error("TODO key is empty");
                return null;
            }
            return blackboard.subscribe(new TodoSubscription(key));
        }

        private boolean isTesterMethod(Method method, String name, Class<?> argClass) {
            if (!name.equals(method.getName())) {
                return false;
            }
            Class<?>[] paramTypes = method.getParameterTypes();
            if (paramTypes.length != 1) {
                return false;
            }
            if (!argClass.isAssignableFrom(paramTypes[0])) {
                return false;
            }
            Class<?> returnType = method.getReturnType();
            return returnType == Boolean.class || returnType == boolean.class;
        }
        
        private Subscription createBlackboardSubscription(Cougaar.Execute annotation) {
            // Prefer the 'isa' value if there is one
            Class<?> isa = annotation.isa();
            if (!Cougaar.isNoClass(isa)) {
                IsInstanceOf predicate = new IsInstanceOf(isa);
                return blackboard.subscribe(predicate);
            }

            // No isa, use the 'when' method
            String testerName = annotation.when().trim();
            if (testerName == null || testerName.length() == 0) {
                log.error("Neither a 'when' nor a 'isa' clause was provided");
                return null;
            }
            Class<?> pluginClass = AnnotatedPlugin.this.getClass();
            Method[] methods = pluginClass.getMethods();
            Class<?> argClass = method.getParameterTypes()[0];
            Method testerMethod = null;
            for (Method method : methods) {
                if (isTesterMethod(method, testerName, argClass)) {
                    testerMethod = method;
                    break;
                }
            }
            if (testerMethod == null) {
                log.error("Couldn't find method " + testerName+ " (" + argClass + ")");
                return null;
            }
            final Method tester = testerMethod;
            final Class<?> testerArgClass = tester.getParameterTypes()[0];
            UnaryPredicate predicate = new UnaryPredicate() {
                public boolean execute(Object o) {
                    if (!testerArgClass.isAssignableFrom(o.getClass())) {
                        return false;
                    }
                    try {
                        return (Boolean) tester.invoke(AnnotatedPlugin.this, o);
                    } catch (Exception e) {
                        log.error("Test failed", e);
                        return false;
                    }
                }
                
            };
            return blackboard.subscribe(predicate);
        }

        private Subscription makeSubscription(Cougaar.Execute annotation) {
            String todo = annotation.todo().trim();
            if (todo.length() > 0) {
                return createTodoSubscription(annotation);
            } else {
                return createBlackboardSubscription(annotation);
            }
        }

        private Collection<?> getCollection(BlackboardOp op) {
            if (sub instanceof IncrementalSubscription) {
                IncrementalSubscription is = (IncrementalSubscription) sub;
                switch (op) {
                    case ADD:
                        return is.getAddedCollection();

                    case CHANGE:
                        return is.getChangedCollection();

                    case REMOVE:
                        return is.getRemovedCollection();

                    default:
                        return null;
                }
            } else if (sub instanceof TodoSubscription) {
                TodoSubscription ts = (TodoSubscription) sub;
                switch (op) {
                    case ADD:
                        return ts.getAddedCollection();

                    case CHANGE:
                        return null;

                    case REMOVE:
                        return null;

                    default:
                        return null;
                }
            } else {
                return null;
            }
        }
        
        public void execute() {
            if (sub == null || !sub.hasChanged()) {
                // failed to make a proper subscription
                return;
            }
            for (Cougaar.BlackboardOp op : ops) {
                Collection<?> objects = getCollection(op);
                if (objects != null) {
                    for (Object object : objects) {
                        try {
                            method.invoke(AnnotatedPlugin.this, object);
                        } catch (Exception e) {
                            log.error("Failed to invoke annotated method", e);
                        }
                    }
                }
            }
        }
    }
    
}
