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

import org.cougaar.planning.servlet.data.Failure;
import org.cougaar.planning.servlet.data.xml.*;

import java.io.Writer;
import java.io.IOException;
import java.io.Serializable;

import org.xml.sax.Attributes;

/**
 * Factory that produces sub-objects based on tags and attributes
 * for CompletionData
 **/
public class CompletionDataFactory implements DeXMLableFactory{

  //Variables:
  ////////////

  //Members:
  //////////
  
  /**
   * This is a look ahead to see if we should start a sub object.
   * The caller will first call this function on startElement.  If
   * this function returns null, the startElement will be reported
   * to the current object with a call to openTag(...).  Otherwise
   * this function should return a new DeXMLable subobject that
   * further output will be deligated to, until the subobject returns
   * true from a call to endElement().
   *
   * @param curObj the current object
   * @param name startElement tag
   * @param attr startElement attributes
   * @return a new DeXMLable subobject if a subobject should be created,
   * otherwise null.
   **/
  public DeXMLable beginSubObject(DeXMLable curObj, String name, 
				  Attributes attr)
    throws UnexpectedXMLException{
    if(curObj==null){
      if(name.equals(FullCompletionData.NAME_TAG)){
	return new FullCompletionData();
      } else if(name.equals(SimpleCompletionData.NAME_TAG)){
	return new SimpleCompletionData();
      }else if(name.equals(Failure.NAME_TAG)){
	return new Failure();
      }
    } else if (curObj instanceof CompletionData) {
      if (name.equals(UnplannedTask.NAME_TAG)) {
        return new UnplannedTask();
      } else if (name.equals(UnestimatedTask.NAME_TAG)) {
        return new UnestimatedTask();
      } else if (name.equals(UnconfidentTask.NAME_TAG)) {
        return new UnconfidentTask();
      } else if (name.equals(FailedTask.NAME_TAG)) {
        return new FailedTask();
      }
    }
    return null;
  }
}

