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

import java.io.*;
import java.util.*;
import java.lang.reflect.*;
import org.cougaar.core.agent.ClusterContext;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.LoggingService;

/**
 * Read persisted objects from a stream. Detects objects that have
 * been wrapped in a PersistenceAssociation and resolves those to the
 * current definition of the object. The first occurence of any object
 * must be inside a PersistenceAssociation. This defining instance is
 * stored in the identityTable. Thereafter, its values are updated
 * from later versions of the same object.
 **/
public class PersistenceInputStream extends ObjectInputStream {
  private LoggingService logger;

  public MessageAddress getOriginator() { return null; }
  public MessageAddress getTarget() { return null; }

  public void close() throws IOException {
    super.close();
  }

  /**
   * The array of object references that are expected during the
   * decoding of an object.
   */
  private PersistenceReference[] references;

  /**
   * Steps through the references during decoding.
   */
  private int nextReadIndex;

  /**
   * InputStream implementation that extracts a segment of bytes from
   * an ObjectInputStream. This is the input counterpart of the
   * PersistenceOutputStream.writeBytes
   **/
  private static class Substream extends FilterInputStream {
    private int bytes;
    public Substream(ObjectInputStream ois) throws IOException {
      super(ois);
      bytes = ois.readInt();
    }
    public int read(byte[] buf) throws IOException {
      return read(buf, 0, buf.length);
    }
    public int read() throws IOException {
      if (bytes == 0) return -1;
      bytes--;
      return super.read();
    }
    public int read(byte[] buf, int offset, int nb) throws IOException {
      if (nb > bytes) nb = bytes;
      nb = super.read(buf, offset, nb);
      if (nb >= 0) bytes -= nb;
      return nb;
    }
  }

  /**
   * Construct from the array of bytes containing the encoded objects.
   * @param bytes the bytes containing the encoded objects.
   */
  public PersistenceInputStream(ObjectInputStream ois, LoggingService logger) throws IOException {
    super(new Substream(ois));
    enableResolveObject(true);
    this.logger = logger;
  }

  static void checkSuperclass() {
    try {
      PersistenceInputStream.class.getSuperclass().getDeclaredMethod("newInstanceFromDesc", new Class[] {ObjectStreamClass.class});
    }
    catch (Exception e) {
      System.err.println("Fatal error " + e.toString());
      System.err.println("Incorrect boot class path does not contain modified java/io/ObjectInputStream.class");
      System.err.println("class loader is " +
                         PersistenceInputStream.class.getClassLoader().getClass().getName());
      System.exit(13);
    }
  }

  /**
   * Read the association for one object. This is the inverse of
   * PersistenceOutputStream.writeAssociation. The active state of the
   * PersistenceAssociation is set according to whether it was active
   * when the persistence delta was generated.
   * @param references the array of references for objects that were
   * written when this association was written. This allows us to know
   * in advance the identity of each object as it is read.
   **/
  public PersistenceAssociation readAssociation(PersistenceReference[] references)
    throws IOException, ClassNotFoundException
  {
    this.references = references;
    nextReadIndex = 0;
    int active = readInt();
    Object object = readObject();
    checkObject(object);
    this.references = null;
    PersistenceAssociation pAssoc = identityTable.find(object);
    if (pAssoc == null) {
      System.err.println("Null PersistenceAssociation found for " + object.getClass().getName() + ": " + object);
    } else {
      pAssoc.setActive(active);
    }
    if (logger.isDebugEnabled()) logger.debug("read association " + pAssoc);
    return pAssoc;
  }

  // Use reflection to avoid calling super.newInstanceFromDesc. Don't want to
  // force installation of javaiopatch.jar for compilation if persistence not 
  // involved.
  private static Method _ano = null;
  private static Object _anoLock = new Object();
  private static Object callNewInstanceFromDesc(ObjectInputStream stream, ObjectStreamClass desc) 
    throws InstantiationException, IllegalAccessException
  {
    Method m;
    synchronized (_anoLock) {
      if ((m = _ano) == null) {
        try {
          Class c = ObjectInputStream.class;
          Class[] argp = new Class[] {ObjectStreamClass.class};
          m = c.getDeclaredMethod("real_newInstanceFromDesc", argp);
          _ano = m;
        } catch (Exception e) {
          e.printStackTrace();
          System.err.println("javaiopatch is not installed properly!");
          throw new RuntimeException("javaiopatch not installed");
        }
      }
    }
    try {
      Object[] args= new Object[]{desc};
      return m.invoke(stream, args);
    } catch (Exception e) {
      e.printStackTrace();
      if (e instanceof InvocationTargetException) {
        Throwable t = ((InvocationTargetException)e).getTargetException();
        if (t instanceof RuntimeException) {
          throw (RuntimeException)t;
        } else if (t instanceof InstantiationException) {
          throw (InstantiationException) t;
        } else if (t instanceof IllegalAccessException) {
          throw (IllegalAccessException) t;
        }
      }
      throw new RuntimeException("javaiopatch not installed");
    }
  }


  /**
   * Allocate an object to be filled in from the serialized
   * stream. This is a hook provided by the ObjectInputStream class
   * for obtaining an object whose fields can be filled in. Normally,
   * this returns a brand new object, but during rehydration we need
   * to update the values of objects that already exist so we override
   * this method and return existing objects corresponding to the
   * reference ids we expect to encounter.
   * @param clazz the class of the object to find/create.
   * @param serializableClass the first serializable class in the
   * ancestry chain of the class. All earlier base classes will be
   * constructed using their default constructors.
   * @return the object to be filled in.
   */
  protected Object newInstanceFromDesc(ObjectStreamClass desc) 
    throws InstantiationException, IllegalAccessException, java.lang.reflect.InvocationTargetException  {
    Class clazz = desc.forClass();
    if (references != null &&
	clazz != PersistenceReference.class &&
	!clazz.isArray() &&
	clazz != String.class) {
      PersistenceReference reference = references[nextReadIndex++];
      if (reference != null) {
	PersistenceAssociation pAssoc = identityTable.get(reference);
	if (pAssoc == null) {
	  Object object = callNewInstanceFromDesc(this, desc);
	  pAssoc = identityTable.create(object, reference);
	  if (logger.isDebugEnabled()) logger.debug("Allocating " + BasePersistence.getObjectName(object) + " @ " + reference);
	  return object;
	}
	Object result = pAssoc.getObject();
	if (result == null) throw new InstantiationException("no object @ " + reference);
	if (result.getClass() != clazz) throw new InstantiationException("wrong object @ " + reference);
	if (logger.isDebugEnabled()) logger.debug("Overwriting " + BasePersistence.getObjectName(result) + " @ " + reference);
	return result;
      } else {
        Object result = callNewInstanceFromDesc(this, desc);
        if (logger.isDebugEnabled()) logger.debug("Allocating " + (nextReadIndex-1) + " " +
              BasePersistence.getObjectName(result));
        return result;
      }
    }
    Object result = callNewInstanceFromDesc(this, desc);
    if (logger.isDebugEnabled()) logger.debug("Allocating " + BasePersistence.getObjectName(result));
    return result;
  }

  private void checkObject(Object object) {
    if (object instanceof org.cougaar.planning.ldm.plan.PlanElement) {
      if (object instanceof org.cougaar.planning.ldm.plan.AssetTransfer) {
      } else {
        org.cougaar.planning.ldm.plan.PlanElement pe = (org.cougaar.planning.ldm.plan.PlanElement) object;
        org.cougaar.planning.ldm.plan.Task task = pe.getTask();
        if (task != null) {
          if (task.getPlanElement() != pe) {
            //            if (logger.isWarnEnabled()) logger.warn("Bad " + object.getClass().getName() + ": pe.getTask()=" + pe.getTask() + " task.getPlanElement()=" + task.getPlanElement());
          }
        } else {
          //          if (logger.isWarnEnabled()) logger.warn("Bad " + object.getClass().getName() + ": pe.getTask()=null");
        }
      }
    }
  }

  /**
   * Resolve an object just read from the stream into the actual
   * result object. We replace PersistenceReference objects with the
   * object to which they refer.
   * @param o the object to resolve.
   * @return the replacement.
   */
  protected Object resolveObject(Object o) throws IOException {
    if (o instanceof PersistenceReference) {
      PersistenceReference pRef = (PersistenceReference) o;
      PersistenceAssociation pAssoc = identityTable.get(pRef);
      if (pAssoc == null) {
	System.err.println("Reference to non-existent object id = " + pRef);
	for (int i = 0; i < identityTable.size(); i++) {
	  System.err.println(i + ": " + identityTable.get(i));
	}
	throw new IOException("Reference to non-existent object id = " + pRef);
//  	return null;
      }
      Object result = pAssoc.getObject();
      if (logger.isDebugEnabled()) logger.debug("Resolving " + BasePersistence.getObjectName(result) + " @ " + pRef);
      return result;
    } else {
      if (logger.isDebugEnabled()) logger.debug("Passing " + BasePersistence.getObjectName(o));
      return o;
    }
  }

  /**
   * Object identity table. Normally, this is supplied by the creator of this stream.
   */
  private IdentityTable identityTable = new IdentityTable();

  /**
   * The ClusterContext
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
