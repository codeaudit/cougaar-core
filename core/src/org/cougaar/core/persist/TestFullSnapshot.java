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
