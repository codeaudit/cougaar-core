/*
 * <copyright>
 *  
 *  Copyright 2004 BBNT Solutions, LLC
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
package org.cougaar.core.persist;

import java.io.Serializable;

import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.util.UnaryPredicate;

public class TestFullSnapshot extends ComponentPlugin {
  private static class Item implements Serializable {
    byte[] bytes = new byte[10000];
  }

  public void setupSubscriptions() {
  }

  public void execute() {
    System.out.println("Running TestFullSnapShot");
    Item[] items = new Item[1000];
    for (int i = 0; i < items.length; i++) {
      items[i] = new Item();
      blackboard.publishAdd(items[i]);
    }
    try {
      blackboard.persistNow();
    } catch (PersistenceNotEnabledException pnee) {
    }
    printBBSize();
    for (int i = 0; i < items.length; i++) {
      blackboard.publishRemove(items[i]);
      items[i] = null;
    }
    blackboard.closeTransaction();
    try {
      Thread.sleep(5000);
    } catch (InterruptedException ie) {
    }
    blackboard.openTransaction();
    try {
      blackboard.persistNow();
    } catch (PersistenceNotEnabledException pnee) {
    }
    printBBSize();
  }

  private void printBBSize() {
    Runtime rt = Runtime.getRuntime();
    long heap = rt.totalMemory() - rt.freeMemory();
    System.out.println(blackboard.query(new UnaryPredicate() {
        public boolean execute(Object o) {
          return true;
        }
      }).size() + " objects on blackboard, heap = " + heap);
  }
}
