package org.cougaar.core.util;

/**
 * Minimal base class for UniqueObjects that use the UID for equality
 */
public class BaseUniqueObject {
    private final UID uid;

    public BaseUniqueObject(UID uid) {
        this.uid = uid;
    }

    public UID getUID() {
        return uid;
    }

    public boolean equals(Object o) {
        return (o == this) || ((o instanceof BaseUniqueObject) && uid.equals(((BaseUniqueObject) o).uid));
    }

    public int hashCode() {
        return uid.hashCode();
    }
}
