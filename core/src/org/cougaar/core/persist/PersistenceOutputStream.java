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
package org.cougaar.core.persist;

import java.io.ObjectOutputStream;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.ArrayList;

import org.cougaar.core.agent.ClusterContext;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.core.mts.MessageAddress;

public class PersistenceOutputStream extends ObjectOutputStream {
  private PrintWriter history = null;

  public MessageAddress getOriginator() { return null; }
  public MessageAddress getTarget() { return null; }

  public void setHistoryWriter(PrintWriter s) {
    history = s;
  }

  /**
   * Keeps track of the (identity of) objects written to the stream.
   */
  private List writeIndex = null;

  /**
   * The stream to which the objects are written
   */
  private ByteArrayOutputStream byteStream;

  /**
   * Public constructor
   */
  public PersistenceOutputStream() throws IOException {
    this(new ByteArrayOutputStream(10000));
  }

  /**
   * Private constructor needed to capture the ByteArrayOutputStream
   * we are using.
   * @param stream the ByteArrayOutputStream into which we store everything.
   */
  private PersistenceOutputStream(ByteArrayOutputStream stream) throws IOException {
    super(stream);
    byteStream = stream;
    enableReplaceObject(true);
  }

//    /**
//     * Get the array of bytes encoding everything we stored.
//     * @return the array of bytes encoding everything we stored.
//     */
//    public byte[] getBytes() throws IOException {
//      flush();
//      if (history != null) {
//        history.close();
//        history = null;
//      }
//      return byteStream.toByteArray();
//    }

  /**
   * Write the bytes encoding everything we stored preceded by the
   * byte count.
   **/
  public void writeBytes(ObjectOutputStream oos) throws IOException {
    oos.writeInt(byteStream.size());
    byteStream.writeTo(oos);
  }

  public byte[] getBytes() {
    return byteStream.toByteArray();
  }

  public int size() {
    return byteStream.size();
  }

  /**
   * Write a plan object from a PersistenceAssociation.
   * @param pAssoc the PersistenceAssocation having the object to save.
   * @return an array of the PersistenceReferences of the objects that
   * were actually saved. This typically includes the objects that are
   * referenced by the object being saved (and the objects they
   * reference, etc.).
   */
  public PersistenceReference[] writeAssociation(PersistenceAssociation pAssoc)
    throws IOException {
    writeIndex = new ArrayList();
    writeInt(pAssoc.getActive());
    try {
      writeObject(pAssoc.getObject());
    }
    catch (java.io.NotSerializableException e) {
      System.err.println(e + " for: " + pAssoc.getObject().getClass().getName()
                         + ": " + pAssoc.getObject());
    }
    catch (IllegalArgumentException e) {
      System.err.println(e + " for: " + pAssoc.getObject().getClass().getName());
    }
    PersistenceReference[] result = new PersistenceReference[writeIndex.size()];
    result = (PersistenceReference[]) writeIndex.toArray(result);
    writeIndex = null;
    return result;
  }

  /**
   * Replace objects that are in the identityTable with reference
   * objects. This operation is suppressed for objects that have
   * changed and actually need to be written.
   * @param o the object to consider for replacement.
   * @return the replacement object.
   */
  protected Object replaceObject(Object o) {
    if (o instanceof PersistenceReference) {
//      print("Writing Ref ", o);
      return o;
    }
    if (o.getClass().isArray()) {
//      print("Writing array " + java.lang.reflect.Array.getLength(o));
      return o;
    }
    if (o instanceof String) {
//      print("Writing String ", o);
      return o;
    }
    PersistenceAssociation pAssoc = identityTable.find(o);
    if (pAssoc == null) {
      if (writeIndex != null) {
	writeIndex.add(null);
      }
      if (o instanceof PlanElement) {
	return null;            // Not persisted yet
      }
//      print("Writing " + o);
      return o;
    }
    if (pAssoc.isMarked()) {
      if (writeIndex != null) {
	writeIndex.add(pAssoc.getReferenceId());
      }
//      print("Writing ", pAssoc, " as ", o);
      return o;
    }
//    print("Subst ", pAssoc, " for ", o);
    return pAssoc.getReferenceId();
  }

  private void print(String intro, PersistenceAssociation pAssoc,
                     String prep, Object o) {
    if (history != null) {
//      print(intro + pAssoc.getReferenceId() + (pAssoc.isActive() ? " active" : " inactive") + prep + o);
    }
  }

  private void print(String intro, Object o) {
    if (history != null) {
//      print(intro + o);
    }
  }

  private void print(String message) {
    if (history != null) {
      history.println(message);
    }
//  String clusterName = clusterContext.getClusterIdentifier().getAddress();
//  System.out.println(clusterName + " -- " + message);
  }

  /**
   * Object identity table. Normally, this is supplied by the creator of this stream.
   */
  private IdentityTable identityTable = new IdentityTable();

  /**
   * Thc ClusterContext
   */
  private ClusterContext clusterContext;

  /**
   * Get the IdentityTable being used by this stream. This is not
   * normally used since the IdentityTable is usually maintained by
   * the creator of this stream.
   * @return the IdentityTable being used by this stream.
   */
  public IdentityTable getIdentityTable() {
    return identityTable;
  }

  /**
   * Set the IdentityTable to be used by this stream. The
   * IdentityTable contains assocations of objects to earlier
   * persistence deltas. References to these earlier objects are
   * replaced with reference objects to save space.
   * @param identityTable the new IdentityTable to use.
   */
  public void setIdentityTable(IdentityTable identityTable) {
    this.identityTable = identityTable;
  }

  /**
   * Get the ClusterContext object that we are supporting. The
   * ClusterContext object is supplied by our creator.
   * @return the ClusterContext object of this stream.
   */
  public ClusterContext getClusterContext() {
    return clusterContext;
  }

  /**
   * Set the ClusterContext object for this stream. We remember this
   * to return from the getClusterContext method.
   * @param newClusterContext the ClusterContext object to remember.
   */
  public void setClusterContext(ClusterContext newClusterContext) {
    clusterContext = newClusterContext;
  }
}

