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

import java.io.Serializable;

/** An opaque marker class used to persist state of an object which 
 * is not itself persistable.  
 **/
public class PersistenceObject implements Serializable {
    private byte[] bytes;
    private String name;
    public PersistenceObject(String name, byte[] bytes) {
        this.bytes = bytes;
        this.name = name;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public String getName() {
        return name;
    }

    public String toString() {
        return name + "(" + bytes.length + " bytes)";
    }
}
