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
 * Represents the data leaving the Completion PSP --
 * <code>SimpleCompletionData</code> only contains counters of the
 * Tasks, not <code>AbstractTask</code>s.
 *
 * @see FullCompletionData
 * @see CompletionData
 **/
public class SimpleCompletionData extends CompletionData{

  //Variables:
  ////////////

  public static final String NAME_TAG = "SimpleCompletion";

  public static final String NUMBER_OF_UNPLANNED_TASKS_ATTR = 
    "NumUnplannedTasks";
  public static final String NUMBER_OF_UNESTIMATED_TASKS_ATTR = 
    "NumUnestimatedTasks";
  public static final String NUMBER_OF_UNCONFIDENT_TASKS_ATTR = 
    "NumUnconfidentTasks";
  public static final String NUMBER_OF_FAILED_TASKS_ATTR = 
    "NumFailedTasks";

  protected int numUnplannedTasks;
  protected int numUnestimatedTasks;
  protected int numUnconfidentTasks;
  protected int numFailedTasks;

  //Constructors:
  ///////////////

  public SimpleCompletionData() {
  }

  //Setters:
  //////////

  public void setNumberOfUnplannedTasks(int numUnplannedTasks) {
    this.numUnplannedTasks = numUnplannedTasks;
  }

  public void setNumberOfUnestimatedTasks(int numUnestimatedTasks) {
    this.numUnestimatedTasks = numUnestimatedTasks;
  }

  public void setNumberOfUnconfidentTasks(int numUnconfidentTasks) {
    this.numUnconfidentTasks = numUnconfidentTasks;
  }

  public void setNumberOfFailedTasks(int numFailedTasks) {
    this.numFailedTasks = numFailedTasks;
  }

  //Getters:
  //////////

  public int getNumberOfUnplannedTasks() {
    return numUnplannedTasks;
  }

  public int getNumberOfUnestimatedTasks() {
    return numUnestimatedTasks;
  }

  public int getNumberOfUnconfidentTasks() {
    return numUnconfidentTasks;
  }

  public int getNumberOfFailedTasks() {
    return numFailedTasks;
  }

  public UnplannedTask getUnplannedTaskAt(int i) {
    throw new UnsupportedOperationException("Only totals gathered!");
  }

  public UnestimatedTask getUnestimatedTaskAt(int i) {
    throw new UnsupportedOperationException("Only totals gathered!");
  }

  public UnconfidentTask getUnconfidentTaskAt(int i) {
    throw new UnsupportedOperationException("Only totals gathered!");
  }

  public FailedTask getFailedTaskAt(int i) {
    throw new UnsupportedOperationException("Only totals gathered!");
  }

  //XMLable members:
  //----------------

  /**
   * Write this class out to the Writer in XML format
   * @param w output Writer
   **/
  public void toXML(XMLWriter w) throws IOException {
    w.optagln(NAME_TAG);
    w.tagln(TIME_MILLIS_ATTR, getTimeMillis());
    w.tagln(NUMBER_OF_TASKS_ATTR, getNumberOfTasks());
    w.tagln(NUMBER_OF_UNPLANNED_TASKS_ATTR, getNumberOfUnplannedTasks());
    w.tagln(NUMBER_OF_UNESTIMATED_TASKS_ATTR, getNumberOfUnestimatedTasks());
    w.tagln(NUMBER_OF_UNCONFIDENT_TASKS_ATTR, getNumberOfUnconfidentTasks());
    w.tagln(NUMBER_OF_FAILED_TASKS_ATTR, getNumberOfFailedTasks());
    w.cltagln(NAME_TAG);
  }

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
  public void openTag(String name, Attributes attr, String data)
    throws UnexpectedXMLException {
   try {
      if (name.equals(NAME_TAG)) {
      } else if (name.equals(TIME_MILLIS_ATTR)) {
	timeMillis = Long.parseLong(data);
      } else if (name.equals(NUMBER_OF_TASKS_ATTR)) {
	numTasks = Integer.parseInt(data);
      } else if (name.equals(NUMBER_OF_UNPLANNED_TASKS_ATTR)) {
	numUnplannedTasks = Integer.parseInt(data);
      } else if (name.equals(NUMBER_OF_UNESTIMATED_TASKS_ATTR)) {
	numUnestimatedTasks = Integer.parseInt(data);
      } else if (name.equals(NUMBER_OF_UNCONFIDENT_TASKS_ATTR)) {
	numUnconfidentTasks = Integer.parseInt(data);
      } else if (name.equals(NUMBER_OF_FAILED_TASKS_ATTR)) {
	numFailedTasks = Integer.parseInt(data);
      } else {
        throw new UnexpectedXMLException("Unexpected tag: "+name);
      }
    } catch (NumberFormatException e) {
      throw new UnexpectedXMLException("Malformed Number: " + 
				       name + " : " + data);
    }
  }

  /**
   * Report an endElement.
   * @param name endElement tag
   * @return true iff the object is DONE being DeXMLized
   **/
  public boolean closeTag(String name)
    throws UnexpectedXMLException {
    return name.equals(NAME_TAG);
  }

  /**
   * This function will be called whenever a subobject has
   * completed de-XMLizing and needs to be encorporated into
   * this object.
   * @param name the startElement tag that caused this subobject
   * to be created
   * @param obj the object itself
   **/
  public void completeSubObject(String name, DeXMLable obj)
    throws UnexpectedXMLException {
      throw new UnexpectedXMLException("Unexpected object: " + obj);
  }

  //Inner Classes:

  /** 
   * Set the serialVersionUID to keep the object serializer from seeing
   * xerces (org.xml.sax.Attributes).
   */
  private static final long serialVersionUID = 2789998238928387475L;
}
