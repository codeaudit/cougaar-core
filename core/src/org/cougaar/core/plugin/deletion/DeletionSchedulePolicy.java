package org.cougaar.core.plugin.deletion;

import java.io.Serializable;
import java.util.SortedSet;
import java.util.TreeSet;
/**
 * Inner class specifies the policy for when deletions should
 * occur. This consists of a periodic element plus ad hoc
 * elements. The policy may be modified to add or remove ad hoc
 * times as well as altering the periodic schedule. Don't forget
 * to publishChange the policy after making modifications.
 **/
public class DeletionSchedulePolicy implements Serializable {
    private SortedSet deletionTimes = new TreeSet();
    private long period;
    private long phase;
    
    public DeletionSchedulePolicy(long period, long phase) {
      setPeriodicSchedule(period, phase);
    }
  
    public synchronized void setPeriodicSchedule(long period, long phase)
    {
      this.period = period;
      this.phase = phase;
    }
  
    public long getDeletionPhase() {
        return phase;
    }
  
    public long getDeletionPeriod() {
      return period;
    }
  
    public synchronized void addDeletionTime(long time) {
        deletionTimes.add(new Long(time));
    }
  
    public synchronized void removeDeletionTime(long time) {
        deletionTimes.remove(new Long(time));
    }
  
    synchronized long getNextDeletionTime(long now) {
        long deletionPhase = getDeletionPhase();
        long deletionPeriod = getDeletionPeriod();
        long ivn = (now - deletionPhase) / deletionPeriod;
        long nextAlarm = (ivn + 1) * deletionPeriod + deletionPhase;
        SortedSet oldTimes = deletionTimes.headSet(new Long(now));
        deletionTimes.removeAll(oldTimes);
        if (!deletionTimes.isEmpty()) {
            Long first = (Long) deletionTimes.first();
            long adHoc = first.longValue();
            if (adHoc < nextAlarm) {
                nextAlarm = adHoc;
                deletionTimes.remove(first);
            }
        }
        return nextAlarm;
    }
}

