/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.cluster;

public class PublishStack extends Throwable {
    public SubscriptionClient theClient = SubscriptionClient.current.getClient();
    private static String getClientName() {
        SubscriptionClient aClient = SubscriptionClient.current.getClient();
        if (aClient != null) {
            return aClient.getSubscriptionClientName();
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
