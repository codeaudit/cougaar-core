/*
 * <copyright>
 *  
 *  Copyright 1997-2006 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */
package org.cougaar.core.plugin;

import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.core.agent.service.alarm.AlarmBase;
import org.cougaar.core.blackboard.TodoSubscription;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.util.UniqueObject;

public final class DeferrableTodoSubscription extends TodoSubscription {
    private final TodoPlugin plugin;
    
    DeferrableTodoSubscription(String id, TodoPlugin plugin) {
        super(id);
        this.plugin = plugin;
    }
    
    /**
     * Add a item to the todo list in the future
     * @param delay when to add, as an offset from now in millis
     * @param item the datum to add
     */
    public void add(TodoItem item, long delay) {
        Alarm alarm = new TodoAlarm(System.currentTimeMillis()+delay, item, this);
        plugin.getAlarmService().addRealTimeAlarm(alarm);
    }
    
    /**
     * Schedule the publish-add of an object during the next execute block.
     * 
     * @param object The object to be added to the blackboard
     * @param blackboard
     */
    public void deferredPublishAdd(final UniqueObject object, final  BlackboardService blackboard) {
        add(new TodoItem() {
            public void execute() {
                blackboard.publishAdd(object);
            }
        });
    }
    
    private final class TodoAlarm extends AlarmBase {
        private final TodoItem item;
        private final TodoSubscription sub;
        
        public TodoAlarm(long futureTime, TodoItem item, TodoSubscription sub) {
            super(futureTime);
            this.item = item;
            this.sub = sub;
        }
        
        public void onExpire() {
            sub.add(item);
        }
    }
}