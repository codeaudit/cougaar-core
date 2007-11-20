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
import org.cougaar.util.IsInstanceOf;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.util.annotations.Cougaar;
import org.cougaar.util.annotations.Subscribe;

/**
 * This class provides support for plugins that wish
 * to use annotations to create and manage blackboard
 * IncrementalSubscriptions.
 */
public abstract class AnnotatedSubscriptionsPlugin extends ParameterizedPlugin {
    private final Map<String, IncrementalSubscription> subscriptions = 
        new HashMap<String, IncrementalSubscription>();
    private final List<Invoker> invokers = new ArrayList<Invoker>();
    
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
                String when = annotation.when();
                if (!Cougaar.NO_VALUE.equals(when)){
                    id = when;
                } else {
                    // Use the type of the first arg as an implicit 'isa'
                    Class<?>[] parameterTypes = method.getParameterTypes();
                    if (parameterTypes.length < 1 || parameterTypes.length > 2) {
                        String message = method.getName() + " of class " +getClass().getName()+
                        " has the wrong number of arguments (should be 1 or 2)";
                        log.error(message);
                        continue;
                    }
                    if (parameterTypes.length == 2) {
                        // ensure 2nd arg is IncrementalSubscription
                        Class<?> cls = parameterTypes[1];
                        if (!IncrementalSubscription.class.isAssignableFrom(cls)) {
                            String message = method.getName() + " of class " +getClass().getName()+
                            " has an invalid second argument (should be IncrementalSubscription)";
                            log.error(message);
                            continue;
                        }
                    }
                    id = parameterTypes[0].getName();
                }
                IncrementalSubscription subscription = subscriptions.get(id);
                if (subscription != null) {
                    Invoker invoker = new Invoker(method, annotation, subscription);
                    invokers.add(invoker);
                } else {
                    Invoker invoker = new Invoker(method, annotation);
                    subscriptions.put(id, invoker.sub);
                    invokers.add(invoker);
                }
            }
        }
    }
    
    public IncrementalSubscription getSubscription(String id) {
        return subscriptions.get(id);
    }
    
    

    /**
     * Keeps the state of one IncrementalSubscription created via annotations.
     */
    private class Invoker {
        /**
         * The subscription itself.
         */
        private final IncrementalSubscription sub;
        
        /**
         * The annotated method which will be invoked on the plugin, passing in each element
         * of an IncrementalSubscription collection in turn as an argument.
         */
        private final Method method;
        
        /**
         * The set of operations, as given in the annotation, which determines
         * which ncrementalSubscription collection are relevant.
         */
        private final Subscribe.ModType[] ops;
        
        public Invoker(Method method, Cougaar.Execute annotation, IncrementalSubscription sub) {
            this.method = method;
            this.ops = annotation.on();
            this.sub = sub;
        }
        
        public Invoker(Method method, Cougaar.Execute annotation) {
            this.method = method;
            this.ops = annotation.on();
            this.sub = createIncrementalSubscription(annotation);
            
        }

        @SuppressWarnings("deprecation")
        private boolean isTesterMethod(Method method, String name, Class<?> argClass) {
            if (method.isAnnotationPresent(Cougaar.Predicate.class)) {
                Cougaar.Predicate pred = method.getAnnotation(Cougaar.Predicate.class);
                if (!name.equals(pred.when())) {
                    return false;
                }
            } else if (!name.equals(method.getName())) {
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
        
        private IncrementalSubscription createIncrementalSubscription(Cougaar.Execute annotation) {
            String when = annotation.when();
            UnaryPredicate predicate;
            if (Cougaar.NO_VALUE.equals(when)) {
                // Implicit instanceof if no 'when'
                Class<?> isa = method.getParameterTypes()[0];
                predicate = new IsInstanceOf(isa);
            } else {
                // Construct a UnaryPredicate from the 'when'
                Class<?> pluginClass = AnnotatedSubscriptionsPlugin.this.getClass();
                Method[] methods = pluginClass.getMethods();
                Class<?> argClass = method.getParameterTypes()[0];
                Method testerMethod = null;
                for (Method method : methods) {
                    if (isTesterMethod(method, when, argClass)) {
                        testerMethod = method;
                        break;
                    }
                }
                if (testerMethod == null) {
                    log.error("Couldn't find method " + when+ " (" + argClass + ")");
                    return null;
                }
                final Method tester = testerMethod;
                final Class<?> testerArgClass = tester.getParameterTypes()[0];
                predicate = new UnaryPredicate() {
                    public boolean execute(Object o) {
                        if (!testerArgClass.isAssignableFrom(o.getClass())) {
                            return false;
                        }
                        try {
                            return (Boolean) tester.invoke(AnnotatedSubscriptionsPlugin.this, o);
                        } catch (Exception e) {
                            log.error("Test failed", e);
                            return false;
                        }
                    }

                };
            }
            return (IncrementalSubscription) blackboard.subscribe(predicate);
        }

        private Collection<?> getCollection(Subscribe.ModType op) {
            switch (op) {
                case ADD:
                    return sub.getAddedCollection();
                    
                case CHANGE:
                    return sub.getChangedCollection();
                    
                case REMOVE:
                    return sub.getRemovedCollection();
                    
                default:
                    return null;
            }
        }
        
        public void execute() {
            if (sub == null || !sub.hasChanged()) {
                // failed to make a proper subscription, or no changes
                return;
            }
            boolean includeSubscription = method.getParameterTypes().length == 2;
            for (Subscribe.ModType op : ops) {
                Collection<?> objects = getCollection(op);
                if (objects != null) {
                    for (Object object : objects) {
                        try {
                            AnnotatedSubscriptionsPlugin plugin = AnnotatedSubscriptionsPlugin.this;
                            if (includeSubscription) {
                                method.invoke(plugin, object, sub);
                            } else {
                                method.invoke(plugin, object);
                            }
                        } catch (Exception e) {
                            log.error("Failed to invoke annotated method", e);
                        }
                    }
                }
            }
        }
    }
    
}
