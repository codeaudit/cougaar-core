package org.cougaar.core.persist;

import org.cougaar.core.plugin.ServiceUserPlugin;
import org.cougaar.core.service.BlackboardService;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Random;

public class Exercise extends ServiceUserPlugin {
  private Item[] objects = new Item[1000];
  private Random random = new Random();

  private static class Item implements Serializable {
    int serializationTime;
    int anotherInt;

    public Item(int st) {
      serializationTime = st;
    }

    public long wasteTime() {
      long t = 0;
      for (int i = 0; i < 10 * serializationTime; i++) {
        t += System.currentTimeMillis();
      }
      return t;
    }

    private void writeObject(ObjectOutputStream oos)
      throws IOException
    {
      oos.writeInt(serializationTime);
      long t = wasteTime();
      oos.writeLong(t);
    }

    private void readObject(ObjectInputStream ois)
      throws IOException
    {
      serializationTime = ois.readInt();
      ois.readInt();
    }
  }

  public Exercise() {
    super(new Class[0]);
  }

  public void setupSubscriptions() {
    logger.warn("Running " + blackboardClientName);
    startTimer(5000 + random.nextInt(45000));
  }

  private void wasteTime(Item item, int factor) {
    for (int i = 0; i < factor; i++) {
      item.wasteTime();
    }
  }

  public void execute() {
    if (timerExpired()) {
      long st = System.currentTimeMillis();
      cancelTimer();
      int n = random.nextInt(400);
      for (int i = 0; i < n; i++) {
        int x = random.nextInt(objects.length);
        if (objects[i] == null) {
          objects[i] = new Item(random.nextInt(1000));
          //          wasteTime(objects[i], 1);
          blackboard.publishAdd(objects[i]);
        } else {
//           wasteTime(objects[i], 1);
          blackboard.publishChange(objects[i]);
        }
      }
      long et = System.currentTimeMillis();
      logger.warn("execute for " + (et - st) + " millis");
      startTimer(5000 + random.nextInt(45000));
    }
  }
}
