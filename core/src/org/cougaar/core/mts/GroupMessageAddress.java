/*
 *
 * Copyright 2008 by BBN Technologies Corporation
 *
 */

package org.cougaar.core.mts;

/**
 * An address that refers indirectly to a group of destination.
 * The canonical example is a UDP multicast address.
 */
public abstract class GroupMessageAddress extends SimpleMessageAddress {
    private transient Object reference;
    
    // for deserialization only
    public GroupMessageAddress() {
    }
    
    public GroupMessageAddress(Object reference) {
        super(reference.toString());
        this.reference = reference;
    }
    
    abstract protected Object makeReference(String id);
    
    public Object getReference() {
        if (reference == null) {
            reference = makeReference(getAddress());
        }
        return reference;
    }
    
    public boolean isGroupAddress() {
        return true;
    }
}
