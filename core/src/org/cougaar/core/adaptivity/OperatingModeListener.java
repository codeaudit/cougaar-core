package org.cougaar.core.adaptivity;

public interface OperatingModeListener {
    void operatingModeChanged(OperatingMode m, Comparable oldValue);
}
