/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
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

package org.cougaar.core.util;

import java.io.OutputStream;
import java.util.ArrayList;
import org.w3c.dom.Document;

public interface XMLObjectProvider
{
     //
     // Add object to internal DOM Document.
     // Most legacy XMLObjectProviders assume obj is XMLizable 
     //
     void addObject(Object obj);

     //
     // Print DOM Document to System.out
     // Shorthand -- for diagnostics
     //
     void printDocument();


     // @deprecated
     void writeDocumentToFile(String pathname);

     //
     // write DOM Document to named output stream
     //
     void writeDocument(OutputStream stream);

     //
     // @return Document:  reference to internal document instance
     //
     Document getDocumentRef();
     //
     // Reset this ObjectProvider state to pristine.
     // So this ObjectProvider can be reused
     //
     void reset();

     //
     // Return number of objects in internal collection
     //
     int size();

     //
     // Obtain handle to internal Object collection.  Can be empty
     // if unused.
     //
     ArrayList getCollection();
}
