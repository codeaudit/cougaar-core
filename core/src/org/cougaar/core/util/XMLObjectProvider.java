/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
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
     public void addObject(Object obj);

     //
     // Print DOM Document to System.out
     // Shorthand -- for diagnostics
     //
     public void printDocument();


     // @deprecated
     public void writeDocumentToFile(String pathname);

     //
     // write DOM Document to named output stream
     //
     public void writeDocument(OutputStream stream);

     //
     // @return Document:  reference to internal document instance
     //
     public Document getDocumentRef();
     //
     // Reset this ObjectProvider state to pristine.
     // So this ObjectProvider can be reused
     //
     public void reset();

     //
     // Return number of objects in internal collection
     //
     public int size();

     //
     // Obtain handle to internal Object collection.  Can be empty
     // if unused.
     //
     public ArrayList getCollection();
}
