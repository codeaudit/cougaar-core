/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.core.cluster.persist;

import java.io.*;
import java.util.*;
import java.lang.reflect.*;

import org.cougaar.core.cluster.ClusterContext;
import org.cougaar.core.society.MessageAddress;

/**
 * Read persisted objects from a stream. Detects objects that have
 * been wrapped in a PersistenceAssociation and resolves those to the
 * current definition of the object. The first occurence of any object
 * must be inside a PersistenceAssociation. This defining instance is
 * stored in the identityTable. Thereafter, its values are updated
 * from later versions of the same object.
 **/
public class PersistenceInputStream extends ObjectInputStream {
  private PrintWriter history = null;


  public MessageAddress getOriginator() { return null; }
  public MessageAddress getTarget() { return null; }

  public void setHistoryWriter(PrintWriter s) {
    history = s;
  }

  public void close() throws IOException {
    super.close();
    if (history != null) {
      history.close();
      history = null;
    }
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
  public PersistenceInputStream(ObjectInputStream ois) throws IOException {
    super(new Substream(ois));
    enableResolveObject(true);
  }

  static void checkSuperclass() {
    try {
      PersistenceInputStream.class.getSuperclass().getDeclaredMethod("allocateNewObjectOverride", new Class[] {Class.class, Class.class});
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
    if (history != null) {
      print("read association " + pAssoc);
    }
    return pAssoc;
  }

  private static Method _ano = null;
  private static Object _anoLock = new Object();
  private static Object callAllocateNewObject(Class clazz, Class serializableClazz) 
    throws InstantiationException, IllegalAccessException
  {
    Method m;
    synchronized (_anoLock) {
      if ((m = _ano) == null) {
        try {
          Class c = ObjectInputStream.class;
          Class[] argp = new Class[] {Class.class, Class.class};
          m = c.getDeclaredMethod("allocateNewObject",argp);
          _ano=m;
        } catch (Exception e) {
          e.printStackTrace();
          System.err.println("javaiopatch is not installed properly!");
          throw new RuntimeException("javaiopatch not installed");
        }
      }
    }
    try {
      Object[] args= new Object[]{clazz,serializableClazz};
      return m.invoke(null, args);
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
  protected Object allocateNewObjectOverride(Class clazz, Class serializableClazz)
    throws InstantiationException, IllegalAccessException {
    if (references != null &&
	clazz != PersistenceReference.class &&
	!clazz.isArray() &&
	clazz != String.class) {
      PersistenceReference reference = references[nextReadIndex++];
      if (reference != null) {
	PersistenceAssociation pAssoc = identityTable.get(reference);
	if (pAssoc == null) {
	  Object object = callAllocateNewObject(clazz, serializableClazz);
	  pAssoc = identityTable.create(object, reference);
	  print("Allocating " + BasePersistence.getObjectName(object) + " @ " + reference);
	  return object;
	}
	Object result = pAssoc.getObject();
	if (result == null) throw new InstantiationException("no object @ " + reference);
	if (result.getClass() != clazz) throw new InstantiationException("wrong object @ " + reference);
	print("Overwriting " + BasePersistence.getObjectName(result) + " @ " + reference);
	return result;
      } else {
        Object result = callAllocateNewObject(clazz, serializableClazz);
        print("Allocating " + (nextReadIndex-1) + " " +
              BasePersistence.getObjectName(result));
        return result;
      }
    }
    Object result = callAllocateNewObject(clazz, serializableClazz);
    print("Allocating " + BasePersistence.getObjectName(result));
    return result;
  }

  private void checkObject(Object object) {
    if (object instanceof org.cougaar.domain.planning.ldm.plan.PlanElement) {
      if (object instanceof org.cougaar.domain.planning.ldm.plan.AssetTransfer) {
      } else {
        org.cougaar.domain.planning.ldm.plan.PlanElement pe = (org.cougaar.domain.planning.ldm.plan.PlanElement) object;
        org.cougaar.domain.planning.ldm.plan.Task task = pe.getTask();
        if (task != null) {
          if (task.getPlanElement() != pe) {
            print("Bad " + object.getClass().getName() + ": pe.getTask()=" + pe.getTask() + " task.getPlanElement()=" + task.getPlanElement());
          }
        } else {
          print("Bad " + object.getClass().getName() + ": pe.getTask()=null");
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
      print("Resolving " + BasePersistence.getObjectName(result) + " @ " + pRef);
      return result;
    } else {
      print("Passing " + BasePersistence.getObjectName(o));
      return o;
    }
  }

  private void print(String message) {
    if (history != null) {
      history.println(message);
      history.flush();
    }
//      String clusterName = clusterContext.getClusterIdentifier().getAddress();
//      System.out.println(clusterName + " -- " + message);
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
