/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.domain.planning.ldm.asset;

/**
 * This is a simple class to support the perturbation
 * scenario of making a transportation link unavailable.
 * We need it because the perturbator plugin does not
 * support changing an object availability through the 
 * role schedule (in xml).  That plugin only supports 
 * changing an objects properties. 
 * 
 * Perturbator is then a mini-property of all transportation
 * links.
 */
public class Perturbator{
  String marker;

  public Perturbator() {
    marker = "original";
  }
  public String getMarker() {
    return marker;
  }
  public void setMarker(String new_marker) {
    marker = new_marker;
    //System.out.println("Perturbator set to "+bar);
  }
}
