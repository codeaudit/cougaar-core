/*
 * <copyright>
 *  Copyright 2001 BBNT Solutions, LLC
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
package org.cougaar.planning.servlet.data.completion;

import org.cougaar.planning.servlet.data.xml.*;

import java.io.Writer;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;

import org.xml.sax.Attributes;

/**
 * Abstract representation of the data leaving the Completion PSP.
 *
 * @see FullCompletionData
 * @see SimpleCompletionData
 **/
public abstract class CompletionData implements XMLable, DeXMLable, Serializable{

  //Variables:
  ////////////

  public static final String NAME_TAG = "Completion";

  public static final String TIME_MILLIS_ATTR = 
    "TimeMillis";
  public static final String RATIO_ATTR = 
    "Ratio";
  public static final String NUMBER_OF_TASKS_ATTR = 
    "NumTasks";

  protected long timeMillis;
  protected double ratio;
  protected int numTasks;

  //Constructors:
  ///////////////

  public CompletionData() {
  }

  //Setters:
  //////////

  public void setTimeMillis(long timeMillis) {
    this.timeMillis = timeMillis;
  }

  public void setRatio(double ratio) {
    // assert (0.0 <= ratio && ratio <= 1.0);
    this.ratio = ratio;
  }

  public void setNumberOfTasks(int numTasks) {
    this.numTasks = numTasks;
  }

  //Getters:
  //////////

  public long getTimeMillis() {
    return timeMillis;
  }

  /** number between 0.0 and 1.0, inclusive. */
  public double getRatio() {
    return ratio;
  }

  public int getNumberOfTasks() {
    return numTasks;
  }

  public abstract int getNumberOfUnplannedTasks();

  public abstract int getNumberOfUnestimatedTasks();

  public abstract int getNumberOfUnconfidentTasks();

  public abstract int getNumberOfFailedTasks();

  public int getNumberOfFullySuccessfulTasks() {
    return 
      (getNumberOfTasks() -
       (getNumberOfUnplannedTasks() +
        getNumberOfUnestimatedTasks() +
        getNumberOfUnconfidentTasks() +
        getNumberOfFailedTasks()));
  }

  public abstract UnplannedTask getUnplannedTaskAt(int i);

  public abstract UnestimatedTask getUnestimatedTaskAt(int i);

  public abstract UnconfidentTask getUnconfidentTaskAt(int i);

  public abstract FailedTask getFailedTaskAt(int i);

  //XMLable members:
  //----------------

  /**
   * Write this class out to the Writer in XML format
   * @param w output Writer
   **/
  public abstract void toXML(XMLWriter w) throws IOException;

  //DeXMLable members:
  //------------------

  /**
   * Report a startElement that pertains to THIS object, not any
   * sub objects.  Call also provides the elements Attributes and data.  
   * Note, that  unlike in a SAX parser, data is guaranteed to contain 
   * ALL of this tag's data, not just a 'chunk' of it.
   * @param name startElement tag
   * @param attr attributes for this tag
   * @param data data for this tag
   **/
  public abstract void openTag(String name, Attributes attr, String data)
    throws UnexpectedXMLException;

  /**
   * Report an endElement.
   * @param name endElement tag
   * @return true iff the object is DONE being DeXMLized
   **/
  public abstract boolean closeTag(String name)
    throws UnexpectedXMLException;

  /**
   * This function will be called whenever a subobject has
   * completed de-XMLizing and needs to be encorporated into
   * this object.
   * @param name the startElement tag that caused this subobject
   * to be created
   * @param obj the object itself
   **/
  public abstract void completeSubObject(String name, DeXMLable obj)
    throws UnexpectedXMLException;

  //Inner Classes:

  /** 
   * Set the serialVersionUID to keep the object serializer from seeing
   * xerces (org.xml.sax.Attributes).
   */
  private static final long serialVersionUID = 1234679540398212345L;
}
