/*
 *
 * Copyright 2007 by BBN Technologies Corporation
 *
 */

package org.cougaar.core.plugin;

import org.cougaar.core.blackboard.TodoSubscription;

/**
 * Encapsulates a piece of work to be done through {@link TodoSubscription}
 * managed by a {@link TodoPlugin}.
 */
public interface TodoItem {
    public void execute();
}
