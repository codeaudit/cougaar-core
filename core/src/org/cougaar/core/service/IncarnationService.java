package org.cougaar.core.service;

import org.cougaar.core.component.Service;
import org.cougaar.core.mts.MessageAddress;

public interface IncarnationService extends Service {

    /** @return agent incarnation, or throw exception
     * if not known
     * @throws IncarnationNotKnownException
     */
    long getIncarnation(MessageAddress addr) 
	throws IncarnationNotKnownException;

    void subscribe(MessageAddress addr, Callback cb);
    void unsubscribe(MessageAddress addr, Callback cb);

    interface Callback {
	void incarnationChanged(MessageAddress addr, long inc);
    }

    class IncarnationNotKnownException extends Exception {
    }
}
