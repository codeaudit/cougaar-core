package org.cougaar.core.society;

public interface Debug
{
    public static final boolean DEBUG_TRANSPORT = 
	Boolean.getBoolean("org.cougaar.core.society.transport.debug");
}
