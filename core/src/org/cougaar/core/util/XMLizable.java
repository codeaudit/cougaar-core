/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.util;

import org.w3c.dom.Element;
import org.w3c.dom.Document;

/** An interface to advertise that an object is a plan object
 * that can be converted to XML.
 * @see org.cougaar.core.util.XMLize
 **/

public interface XMLizable
{
  /** Add the object to the document as an XML Element and return the Element
   *  Leave it up to the client which XML document implementation to use
   **/
  Element getXML(Document doc);

}
