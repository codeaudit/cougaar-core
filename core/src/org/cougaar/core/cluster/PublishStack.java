/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.cluster;
import org.cougaar.core.blackboard.*;

public class PublishStack extends Throwable {
    public BlackboardClient theClient = BlackboardClient.current.getClient();
    private static String getClientName() {
        BlackboardClient aClient = BlackboardClient.current.getClient();
        if (aClient != null) {
            return aClient.getBlackboardClientName();
        } else {
            return "Unknown Client";
        }
    }
    public PublishStack(String message) {
        super(message + getClientName());
    }
    public String toString() {
        return getMessage();
    }
}
