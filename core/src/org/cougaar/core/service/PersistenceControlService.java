/*
 * <copyright>
 *  Copyright 2001-2003 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */

package org.cougaar.core.service;

import org.cougaar.core.adaptivity.OMCRangeList;
import org.cougaar.core.component.Service;

public interface PersistenceControlService extends Service {
    /**
     * Gets the names of all controls (operating modes). These are
     * controls over persistence as a whole, not media-specific.
     * @return an array of all control names
     **/
    String[] getControlNames();

    /**
     * Gets the (allowed) values of a given control. Values used to
     * set a control must be in the ranges specified by the return.
     * @return a list of ranges of values that are allowed for the
     * named control.
     * @param controlName the name of a persistence-wide control
     **/
    OMCRangeList getControlValues(String controlName);

    /**
     * Sets the value for the named control. The value must be in the
     * list or ranges returned by {@link #getControlValues}.
     * @param controlName the name of the control to set.
     * @param newValue the new value for the control. Must be in the
     * allowed ranges.
     **/
    void setControlValue(String controlName, Comparable newValue);

    /**
     * Gets the names of the installed media plugins.
     * @return an array of the names of the installed media plugins.
     **/
    String[] getMediaNames();

    /**
     * Gets the names of the controls for the named media (plugin).
     * @return an array of all control names for the given media.
     * @param mediaName the name of the media.
     **/
    String[] getMediaControlNames(String mediaName);

    /**
     * Gets the allowed values of then named control for the named
     * media plugin. Values used to set a control must be in the
     * ranges specified by the return.
     * @return a list of ranges of values that are allowed for the
     * named control.
     * @param mediaName the name of the media having the control
     * @param controlName the name of a media-specific control
     * @return a list of the allowed value ranges.
     **/
    OMCRangeList getMediaControlValues(String mediaName, String controlName);

    /**
     * Sets the value for the named control for the named media
     * plugin. The value must be in the list or ranges returned by
     * {@link #getMediaControlValues}.
     * @param mediaName the name of the media having the control
     * @param controlName the name of the control to set.
     * @param newValue the new value for the control. Must be in the
     * allowed ranges.
     **/
    void setMediaControlValue(String mediaName, String controlName, Comparable newValue);
}
