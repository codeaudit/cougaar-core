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
 
package org.cougaar.domain.planning.ldm.plan;
 
import org.cougaar.domain.planning.ldm.measure.Longitude;
import org.cougaar.domain.planning.ldm.measure.Latitude;
 
public class LatLonPointImpl 
  implements LatLonPoint, NewLatLonPoint, java.io.Serializable
{
	
  protected Latitude lat = null;
  protected Longitude lon = null;
	
  public LatLonPointImpl() {
    super();
  }
	
  public LatLonPointImpl(Latitude la, Longitude lo) {
    lat = la;
    lon = lo;
  } 
	
  /** @return Latitude - the Latitude representing this location */
  public Latitude getLatitude() {
    return lat;
  }	
	
  /** @return Longitude - the Longitude representing this location */
  public Longitude getLongitude() {
    return lon;
  }
	
  /** @param latitude - set the Latitude representing this location */
  public void setLatitude(Latitude latitude) {
    lat = latitude;
  }
	
  /** @param longitude - set the Longitude representing this location */
  public void setLongitude(Longitude longitude) {
    lon = longitude;
  }
	
  public Object clone() {
    return new LatLonPointImpl(lat, lon);
  }

  public String toString() {
    return "("+lat+", "+lon+")";
  }

}
