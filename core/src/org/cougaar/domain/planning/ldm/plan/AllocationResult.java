/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.domain.planning.ldm.plan;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

/**
 * The "result" of allocating a task.
 **/

public class AllocationResult
  implements AspectType, AuxiliaryQueryType, Serializable, Cloneable
{
                                    
  private boolean isSuccess;
  private float confrating;
  private AspectValue[] avResults = null;
  private ArrayList phasedavrs = null;      // A List of AspectValue[], null if not phased
  private String[] auxqueries = null;
  
  /** Simple Constructor for a NON-PHASED result of ordinary AspectValues
   * @param rating The confidence rating of this result.
   * @param success  Whether the allocationresult violated any preferences.
   * @param aspecttypes  The AspectTypes (and order) of the results.
   * @param result  The value for each aspect.
   * @return AllocationResult
   */
  public AllocationResult(double rating, boolean success, int[] aspecttypes, double[] result) {
    isSuccess = success;
    setRollup(aspecttypes, result);
    confrating = (float) rating;
  }
  
  /** Simple Constructor for a PHASED result
   * @param rating The confidence rating of this result.
   * @param success  Whether the allocationresult violated any preferences.
   * @param aspecttypes  The AspectTypes (and order) of the results.
   * @param rollup  The summary values for each aspect.
   * @param allresults  An Enumeration representing
   * each phased List of results.
   * For Example a phased answer may look like
   * [ [10, 100.00, c0], [5, 50.00, c3], [5, 50.00, c6] ]
   * @return AllocationResult
   */
  public AllocationResult(double rating, boolean success, int[] aspecttypes,
                          double[] rollup, Enumeration allresults)
  {
    isSuccess = success;
    setRollup(aspecttypes, rollup);
    setPhasedResults(allresults);
    confrating = (float) rating;
  }
  
  /** Constructor that takes a PHASED result in the form of AspectValues.
   * Subclasses of AspectValue, such as TypedQuantityAspectValue are allowed.
   * @param rating The confidence rating of this result.
   * @param success  Whether the allocationresult violated any preferences.
   * @param rollupavs  The Summary (or rolled up) AspectValues that represent the results.
   * @param phasedresults  A List of the phased results. The List should contain
   * one List of AspectValues for each phase of the results.  
   * @return AllocationResult
   */
  public AllocationResult(double rating, boolean success, AspectValue[] rollupavs, List phasedresults) {
    isSuccess = success;
    setAspectValueResults((AspectValue[]) rollupavs.clone());
    setPhasedAspectValueResults(phasedresults);
    confrating = (float) rating;
  }
  
  /** Constructor that takes a result in the form of AspectValues (NON-PHASED).
   * Subclasses of AspectValue, such as TypedQuantityAspectValue are allowed.
   * @param rating The confidence rating of this result.
   * @param success  Whether the allocationresult violated any preferences.
   * @param aspectvalues  The AspectValues(can be aspectvalue subclasses) that represent the results.  
   * @return AllocationResult
   */
  public AllocationResult(double rating, boolean success, AspectValue[] aspectvalues) {
    isSuccess = success;
    setAspectValueResults((AspectValue[]) aspectvalues.clone());
    confrating = (float) rating;
  }

  /**
   * Construct a merged AllocationResult containing AspectValues from
   * two AllocationResults. If both arguments have the same aspects,
   * the values from the first (dominant) result are used. The result
   * is never phased.
   **/
  public AllocationResult(AllocationResult ar1, AllocationResult ar2) {
    int len1 = ar1.avResults.length;
    int len2 = ar2.avResults.length;
    int nAspects = len1;
    float sumConfRating = ar1.confrating * nAspects;
    System.arraycopy(ar1.avResults, 0, avResults, 0, nAspects);
    avResults = new AspectValue[len1 + len2];
  outer:
    for (int i = 0; i < len2; i++) {
      int aspectType = ar2.avResults[i].getAspectType();
      for (int j = 0; j < nAspects; j++) {
        if (aspectType == avResults[j].getAspectType()) {
          continue outer;       // Already have this AspectType
        }
      }
      // New aspectType. Append to arrays
      avResults[nAspects++] = ar2.avResults[i];
      sumConfRating += ar2.confrating;
    }
    confrating = sumConfRating / nAspects;

    if (ar1.auxqueries != null) {
      auxqueries = (String[]) ar1.auxqueries.clone();
    }
    if (ar2.auxqueries != null) {
      String[] mergedQueries = assureAuxqueries();
      for (int i = 0; i < AQTYPE_COUNT; i++) {
        if (mergedQueries[i] == null) mergedQueries[i] = ar2.auxqueries[i];
      }
    }
    isSuccess = ar1.isSuccess() || ar2.isSuccess();
  }

  public Object clone() {
    return new AllocationResult(this);
  }

  private AllocationResult(AllocationResult ar) {
    confrating = ar.confrating;
    isSuccess = ar.isSuccess;
    avResults = (AspectValue[]) ar.avResults.clone();
    if (ar.phasedavrs != null) {
      phasedavrs = new ArrayList(ar.phasedavrs.size());
      for (Iterator i = ar.phasedavrs.iterator(); i.hasNext(); ) {
        AspectValue[] av = (AspectValue[]) i.next();
        phasedavrs.add(av.clone());
      }
    }
    if (ar.auxqueries != null) auxqueries = (String[]) ar.auxqueries.clone();
  }

  private void setRollup(int[] aspects, double[] result) {
    AspectValue[] avs = new AspectValue[aspects.length];
    for (int i = 0; i < avs.length; i++) {
      if (aspects[i] == 14) throw new IllegalArgumentException("Creating AspectValue for DEMANDRATE");
      avs[i] = new AspectValue(aspects[i], result[i]);
    }
    setAspectValueResults(avs);
  }

  private int getIndexOfType(int aspectType) {
    if (aspectType == _lasttype) return _lastindex; // Use memoized value
    for (int i = 0 ; i < avResults.length; i++) {
      if (avResults[i].getAspectType() == aspectType) return i;
    }
    return -1;
  }

  //AllocationResult interface implementation.

  /** Get the result with respect to a given AspectType. 
   * If the AllocationResult is phased, this method will return
   * the summary value of the given AspectType.
   * <P> Warning!!! Not all AspectValues can be simply represented as
   * a double. Use of this method with such AspectValues is undefined.
   * @param aspectType
   * @return double The result of a given dimension. For example, 
   * getValue(AspectType.START_TIME) returns the Task start time.
   * Note : results are not required to contain data in each dimension - 
   * check the array of defined aspecttypes or ask if a specific
   * dimension is defined.  If there is a request for a value of an
   * undefined aspect, an IllegalArgumentException will be thrown.
   * @see org.cougaar.domain.planning.ldm.plan.AspectType
   */
  public double getValue(int aspectType) {
    synchronized (avResults) {
      if (_lasttype == aspectType) 
        return avResults[_lastindex].getValue(); // return memoized value
      int i = getIndexOfType(aspectType);
      if (i >= 0)
        return avResults[i].getValue();
    }
    // didn't find it.
    throw new IllegalArgumentException("AllocationResult.getValue(int "
                                       + aspectType
                                       + ") - The AspectType is not defined by this Result.");
  }

  /** Quick check to see if one aspect is defined as opposed to
    * looking through the AspectType array.
    * @param aspectType  The aspect you are checking
    * @return boolean Represents whether this aspect is defined
    * @see org.cougaar.domain.planning.ldm.plan.AspectType
    */
  public boolean isDefined(int aspectType) {
    int i = getIndexOfType(aspectType);
    if (i >= 0) {
      _lasttype = aspectType;
      _lastindex = i; // memoize lookup
      return true;
    }
    return false;
  }
    
          
  /** @return boolean Represents whether or not the allocation 
   * was a success. If any Constraints were violated by the 
   * allocation, then the isSuccess() method returns false 
   * and the PlugIn that created the subtask should
   * recognize this event. The Expander may re-expand, change the 
   * Constraints or Preferences, or indicate failure to its superior. 
   */
  public boolean isSuccess() {
    return isSuccess;
  }

  /** @return boolean Represents whether or not the allocation
   * result is phased.
   */
  public boolean isPhased() {
    return phasedavrs != null;
  }

  // Memoized variables
  private transient int[] _ats = null;// Array of aspect types
  private transient int _lasttype=-1; // Type of last type to index conversion
  private transient int _lastindex=-1; // Index of last type to index conversion
  private transient double[] _rs = null;

  private synchronized void clearMemos() {
    _ats = null;
    _lasttype=-1;
    _lastindex=-1;
    _rs = null;
  }

  /** A Collection of AspectTypes representative of the type and
   * order of the aspects in each the result.
   * @return int[]  The array of AspectTypes
   * @see org.cougaar.domain.planning.ldm.plan.AspectType   
   */
  public synchronized int[] getAspectTypes() {
    synchronized (avResults) {
      if (_ats != null) return _ats;
      _ats = new int[avResults.length];
      for (int i = 0; i < avResults.length; i++) {
        _ats[i] = avResults[i].getAspectType();
      }
      return _ats;
    }
  }
  
  /** A collection of doubles that represent the result for each
   * AspectType.  If the result is phased, the results are 
   * summarized.
   * <P> Warning!!! Not all AspectValues can be simply represented as
   * a double. Use of this method with such AspectValues is undefined.
   * @return double[]
   */
  public double[] getResult() {
    return convertToDouble(avResults);
  }

  private double[] convertToDouble(AspectValue[] avs) {
    double[] result = new double[avs.length];
    for (int i = 0; i < avs.length; i++) {
      result[i] = avs[i].getValue();
    }
    return result;
  }
  
  /** A collection of AspectValues that represent the result for each
   * preference.  Note that subclasses of AspectValue such as
   * TypedQuantityAspectValue may be used.  If this was not
   * defined through a constructor, one will be built from the result
   * and aspecttype arrays.  In this case only true AspectValue
   * objects will be build (no subclasses of AspectValues).
   * @return AspectValue[] 
   **/
  public AspectValue[] getAspectValueResults() {
    // go ahead and return it if its defined, otherwise create it
    return (AspectValue[]) avResults.clone();
  }
        
  /** A collection of arrays that represents each phased result.
   * If the result is not phased, use AllocationResult.getResult()
   * <P> Warning!!! Not all AspectValues can be simply represented as
   * a double. Use of this method with such AspectValues is undefined.
   * @return Enumeration{double[]}  The collection of double[]'s.
   */  
  public Enumeration getPhasedResults() {
    if (!isPhased()) throw new IllegalArgumentException("Not phased");
    return new Enumeration() {
      Iterator iter = phasedavrs.iterator();
      public boolean hasMoreElements() {
        return iter.hasNext();
      }
      public Object nextElement() {
        AspectValue[] avs = (AspectValue[]) iter.next();
        return convertToDouble(avs);
      }
    };
  }
  
  /** A List of Lists that represent each phased result in the form
   * of AspectValues.
   * If the result is not phased, use getAspectValueResults()
   * @return List  A List of Lists.  Each internal List represents
   * a phased result.
   */
  public List getPhasedAspectValueResults() {
    return new ArrayList(phasedavrs);
  }
        
  /** @return double The confidence rating of this result. */
  public double getConfidenceRating() {
    return confrating;
  }
  
  /** Return the String representing the auxilliary piece of data that 
   *  matches the query type given in the argument.  
   *  @param aqtype  The AuxiliaryQueryType you want the data for.
   *  @return String  The string representing the data matching the type requested 
   *  Note: may return null if nothing was defined
   *  @see org.cougaar.domain.planning.ldm.plan.AuxiliaryQueryType
   *  @throws IllegalArgumentException if the int passed in as an argument is not a defined
   *  AuxiliaryQueryType
   **/
  public String auxiliaryQuery(int aqtype) {
    if ( (aqtype < 0) || (aqtype > LAST_AQTYPE) ) {
      throw new IllegalArgumentException("AllocationResult.auxiliaryQuery(int) expects an int "
        + "that is represented in org.cougaar.domain.planning.ldm.plan.AuxiliaryQueryType");
    }
    if (auxqueries == null)
      return null;
    else
      return auxqueries[aqtype];
  }
  
  
  //NewAllocationResult interface implementations
  
  /** Set a single AuxiliaryQueryType and its data (String).
   *  @param aqtype The AuxiliaryQueryType
   *  @param data The string associated with the AuxiliaryQueryType
   *  @see org.cougaar.domain.planning.ldm.plan.AuxiliaryQueryType
   **/
  public void addAuxiliaryQueryInfo(int aqtype, String data) {
    if ( (aqtype < 0) || (aqtype > LAST_AQTYPE) ) {
      throw new IllegalArgumentException("AllocationResult.addAuxiliaryQueryInfo(int, String) expects an int "
        + "that is represented in org.cougaar.domain.planning.ldm.plan.AuxiliaryQueryType");
    }
    assureAuxqueries();
    auxqueries[aqtype] = data;
  }
  
  
  /** @param success Represents whether or not the allocation 
   * was a success. If any Constraints were violated by the 
   * allocation, then the isSuccess() method returns false 
   * and the PlugIn that created the subtask should
   * recognize this event. The Expander may re-expand, change the 
   * Constraints or Preferences, or indicate failure to its superior. 
   */
  private  void setSuccess(boolean success) {
    isSuccess = success;
  }
  
  /** Set the aspectvalues results and also split apart the array to set
   * the aspecttype and result array.
   * @param aspectvalues  The AscpectValues representing the result of each aspect.
   */
  private void setAspectValueResults(AspectValue[] avresult) {
    avResults = avresult;
    for (int i = 0; i < avResults.length; i++) {
      if (avResults[i].getAspectType() == 14) {
        if (!(avResults[i] instanceof AspectRate)) {
          throw new IllegalArgumentException("Aspect 14 is not AspectRate");
        }
      }
    }
    clearMemos();
  }
        
  /** A collection of arrays (double[]) that represents each phased result.
   * @param theresults  
   */
  private void setPhasedResults(Enumeration theResults) {
    phasedavrs = new ArrayList();
    while (theResults.hasMoreElements()) {
      AspectValue[] phase = (AspectValue[]) theResults.nextElement();
      phasedavrs.add(phase);
    }
    phasedavrs.trimToSize();
  }
  
  /** A List of Lists that represent phased results in the form of
    * AspectValues.
    * @param phasedaspectvalues
    */
  private void setPhasedAspectValueResults(List phasedaspectvalues) {
    phasedavrs = new ArrayList(phasedaspectvalues);
  }
    
                
  /** @param rating The confidence rating of this result. */
  private void setConfidenceRating(double rating) {
    confrating = (float) rating;
  }
  
  
  /** checks to see if the AllocationResult is equal to this one.
     * @param anAllocationResult
     * @return boolean
     */
  public boolean isEqual(AllocationResult that) {
    if (this == that) return true; // quick success
    if (that == null) return false; // quick fail
    if (!(this.isSuccess() == that.isSuccess() &&
          this.isPhased() == that.isPhased() &&
          this.getConfidenceRating() == that.getConfidenceRating())) {
      return false;
    }
       
    //check the real stuff now!
    //check the aspect types
    //check the summary results
    synchronized (avResults) {
      if (!Arrays.equals(this.avResults, that.avResults)) return false;
      // check the phased results
      if (isPhased()) {
        Iterator i1 = that.phasedavrs.iterator();
        Iterator i2 = this.phasedavrs.iterator();
        while (i1.hasNext()) {
          if (!i2.hasNext()) return false;
          if (!Arrays.equals((AspectValue[]) i1.next(), (AspectValue[]) i2.next())) return false;
        }
        if (i2.hasNext()) return false;
      }
    }

    // check the aux queries
    
    String[] taux = that.auxqueries;
    if (auxqueries != taux) {
      if (!Arrays.equals(taux, auxqueries)) return false;
    }

    // must be equals...
    return true;
  }

  // added to support AllocationResultBeanInfo

  public String[] getAspectTypesAsArray() {
    String[] aspectStrings = new String[avResults.length];
    for (int i = 0; i < aspectStrings.length; i++)
      aspectStrings[i] =  AspectValue.aspectTypeToString(avResults[i].getAspectType());
    return aspectStrings;
  }

  public String getAspectTypeFromArray(int i) {
    synchronized (avResults) {
      if (i < 0 || i >= avResults.length)
        throw new IllegalArgumentException("AllocationResult.getAspectType(int " + i + " not defined.");
      return AspectValue.aspectTypeToString(avResults[i].getAspectType());
    }
  }

  public String[] getResultsAsArray() {
    synchronized (avResults) {
      String[] resultStrings = new String[avResults.length];
      for (int i = 0; i < resultStrings.length; i++) {
        resultStrings[i] = getResultFromArray(i);
      }
      return resultStrings;
    }
  }

  public String getResultFromArray(int i) {
    synchronized (avResults) {
      if (i < 0 || i >= avResults.length)
        throw new IllegalArgumentException("AllocationResult.getAspectType(int " + i + " not defined.");
      int type = avResults[i].getAspectType();
      double value = avResults[i].getValue();
      if (type == AspectType.START_TIME || 
	  type == AspectType.END_TIME) {
	Date d = new Date((long) value);
	return d.toString();
      } else {
        return String.valueOf(value);
      }
    }
  }

  /**
   * Return phased results.
   * <P> Warning!!! Not all AspectValues can be simply represented as
   * a double. Use of this method with such AspectValues is undefined.
   * @return an array of an array of doubles
   **/
  public double[][] getPhasedResultsAsArray() {
    int len = phasedavrs.size();
    double[][] d = new double[len][];
    for (int i = 0; i < len; i++) {
      AspectValue[] avs = (AspectValue[]) phasedavrs.get(i);
      d[i] = convertToDouble(avs);
    }
    return d;
  }

  /**
   * Return a particular phase of a phased result as an array of doubles.
   * <P> Warning!!! Not all AspectValues can be simply represented as
   * a double. Use of this method with such AspectValues is undefined.
   * @return the i-th phase as double[]
   **/
  public double[] getPhasedResultsFromArray(int i) {
    if (!isPhased()) return null;
    if (i < 0 || i >= phasedavrs.size()) return null;
    return convertToDouble((AspectValue[]) phasedavrs.get(i));
  }
    
  private String[] assureAuxqueries() {
    if (auxqueries == null) {
      auxqueries = new String[AQTYPE_COUNT];
    }
    return auxqueries;
  }

  private void appendAVS(StringBuffer buf, AspectValue[] avs) {
    buf.append('[');
    for (int i = 0; i < avs.length; i++) {
      if (i > 0) buf.append(",");
      buf.append(avs[i]);
    }
    buf.append(']');
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("AllocationResult[isSuccess=");
    buf.append(isSuccess);
    buf.append(", confrating=");
    buf.append(confrating);
    appendAVS(buf, avResults);
    if (isPhased()) {
      for (int i = 0, n = phasedavrs.size(); i < n; i++) {
        buf.append("Phase ");
        buf.append(i);
        buf.append("=");
        appendAVS(buf, (AspectValue[]) phasedavrs.get(i));
      }
    }
    buf.append("]");
    return buf.toString();
  }

    // for unit testing only
    /*public static void main(String args[]) {
    int[] ats = {AspectType.START_TIME, AspectType.END_TIME, AspectType.COST};
    double[] rupres = {10.0, 20.0, 3000.00};
    double[] result1 = {10.0, 13.0, 500.00};
    double[] result2 = {15.0, 20.0, 2500.00};
    Vector total = new Vector();
    total.addElement(result1);
    total.addElement(result2);
    
    AllocationResult ar1 = new AllocationResult(99.0, true, ats, rupres, total.elements());
    System.out.println("instantiated");
    double answer1 = ar1.getValue(AspectType.START_TIME);
    double answer2 = ar1.getValue(AspectType.END_TIME);
    double answer3 = ar1.getValue(AspectType.COST);
    System.out.println("isDefined(AspectType.COST)???: " + ar1.isDefined(AspectType.COST));
    System.out.println("isDefined(AspectType.START_TIME)???: " + ar1.isDefined(AspectType.START_TIME));
    System.out.println("isDefined(AspectType.END_TIME)???: " + ar1.isDefined(AspectType.END_TIME));
    System.out.println("isDefined(AspectType.DANGER)???: " + ar1.isDefined(AspectType.DANGER));
    System.out.println("answers " + answer1 + " " + answer2 + " " +answer3);
    }
    */
      
}
