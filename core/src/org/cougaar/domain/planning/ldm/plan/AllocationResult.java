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

import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;
import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.Set;

/**
 * The "result" of allocating a task.
 **/

public class AllocationResult implements AspectType, AuxiliaryQueryType, Serializable {
                                    
  private boolean isSuccess, isPhased;
  private float confrating;
  private int[] aspects = null;
  private Vector results = new Vector(); // vector of double[]
  private double[] rollupresults = null;
  private AspectValue[] aspectvalueresults = null;
  private List phasedavrs;
  private String[] auxqueries = null;
  
  /** Simple Constructor for a NON-PHASED result
   * @param rating The confidence rating of this result.
   * @param success  Whether the allocationresult violated any preferences.
   * @param aspecttypes  The AspectTypes (and order) of the results.
   * @param result  The value for each aspect.
   * @return AllocationResult
   */
  public AllocationResult(double rating, boolean success, int[] aspecttypes, double[] result) {
    isSuccess = success;
    aspects = (int[])aspecttypes.clone();
    rollupresults = (double[])result.clone();
    confrating = (float) rating;
    isPhased = false;
  }
  
  
  /** Simple Constructor for a PHASED result
   * @param rating The confidence rating of this result.
   * @param success  Whether the allocationresult violated any preferences.
   * @param aspecttypes  The AspectTypes (and order) of the results.
   * @param rollup  The summary values for each aspect.
   * @param allresults  An Enumeration of Vectors representing
   * each phased Collection of results.
   * For Example a phased answer may look like
   * [ [10, 100.00, c0], [5, 50.00, c3], [5, 50.00, c6] ]
   * @return AllocationResult
   */
  public AllocationResult(double rating, boolean success, int[] aspecttypes, double[] rollup, Enumeration allresults) {
    isSuccess = success;
    aspects = (int[])aspecttypes.clone();
    rollupresults = (double[])rollup.clone();
    this.setPhasedResults(allresults);
    confrating = (float) rating;
    isPhased = true;
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
    this.setAspectValueResults(rollupavs);
    this.setPhasedAspectValueResults(phasedresults);
    confrating=(float)rating;
    isPhased = true;
  }
  
  /** Constructor that takes a result in the form of AspectValues (NON-PHASED).
   * Subclasses of AspectValue, such as TypedQuantityAspectValue are allowed.
   * @param rating The confidence rating of this result.
   * @param success  Whether the allocationresult violated any preferences.
   * @param aspectvalues  The AspectValues(can be aspectvalue subclasses) that represent the results.  
   * @return AllocationResult
   */
  public AllocationResult(double rating, boolean success, AspectValue[] aspectvalues) {
    isSuccess=success;
    this.setAspectValueResults(aspectvalues);
    confrating=(float) rating;
    isPhased = false;
  }

  /**
   * Construct a merged AllocationResult containing AspectValues from
   * two AllocationResults. If both arguments have the same aspects,
   * the values from the first (dominant) result are used. The result
   * is never phased.
   **/
  public AllocationResult(AllocationResult ar1, AllocationResult ar2) {
    isSuccess = ar1.isSuccess() || ar2.isSuccess();
    int[] mergedAspectTypes = new int[ar1.aspects.length + ar2.aspects.length];
    double[] mergedAspectValues = new double[ar1.aspects.length + ar2.aspects.length];
    int nAspects = ar1.aspects.length;
    float sumConfRating = ar1.confrating * nAspects;
    System.arraycopy(ar1.aspects, 0, mergedAspectTypes, 0, nAspects);
    System.arraycopy(ar1.rollupresults, 0, mergedAspectValues, 0, nAspects);
  outer:
    for (int i = 0; i < ar2.aspects.length; i++) {
      int aspectType = ar2.aspects[i];
      for (int j = 0; j < nAspects; j++) {
        if (aspectType == mergedAspectTypes[j]) continue outer;
      }
      // New aspectType. Append to arrays
      mergedAspectTypes[nAspects] = aspectType;
      mergedAspectValues[nAspects] = ar2.rollupresults[i];
      sumConfRating += ar2.confrating;
      nAspects++;
    }
    aspects = new int[nAspects];
    System.arraycopy(mergedAspectTypes, 0, aspects, 0, nAspects);
    rollupresults = new double[nAspects];
    System.arraycopy(mergedAspectValues, 0, rollupresults, 0, nAspects);
    isPhased = false;
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
  }
  
  // memoize lookup of values
  private int _lasttype=-1;
  private int _lastindex=-1;

  //AllocationResult interface implementation.

  /** Give the result with respect to a given AspectType. 
   * If the AllocationResult is phased, this method will return
   * the summary value of the given AspectType.
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
    synchronized (rollupresults) {
      if (_lasttype==aspectType) 
        return rollupresults[_lastindex]; // memoize
      for (int i = 0 ; i < aspects.length; i++) {
        if (aspects[i] == aspectType)
          return rollupresults[i];
      }
      // didn't find it.
      throw new IllegalArgumentException("AllocationResult.getValue(int "+aspectType+
                                         ") - The AspectType is not defined by this Result.");
    }
  }
  
  /** Quick check to see if one aspect is defined as opposed to
    * looking through the AspectType array.
    * @param aspectType  The aspect you are checking
    * @return boolean Represents whether this aspect is defined
    * @see org.cougaar.domain.planning.ldm.plan.AspectType
    */
  public boolean isDefined(int aspectType) {
    int[] definedaspects = aspects; // was cloning it! e.g. this.getAspectTypes();
    for (int i = 0; i < definedaspects.length; i++) {
      if (definedaspects[i] == aspectType) {
        _lasttype=aspectType; _lastindex=i; // memoize lookup
        return true;
      }
    }
    // if we made it this far its not defined
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
    return isPhased;
  }

  private transient int[] _ats = null;
  private synchronized void clear_ats() {
    _ats=null;
  }

  /** A Collection of AspectTypes representative of the type and
   * order of the aspects in each the result.
   * @return int[]  The array of AspectTypes
   * @see org.cougaar.domain.planning.ldm.plan.AspectType   
   */
  public synchronized int[] getAspectTypes() {
    if (_ats != null) return _ats;
    _ats = (int[])aspects.clone();
    return _ats;
  }
  
  private transient double[] _rs = null;
  private void clear_rs() {
    synchronized (rollupresults) {
      _rs = null;
    }
  }

  /** A collection of doubles that represent the result for each
   * AspectType.  If the result is phased, the results are 
   * summarized.
   * @return double[]
   */
  public double[] getResult() {
    synchronized (rollupresults) {
      if (_rs != null) return _rs;
      _rs = (double[])rollupresults.clone();
      return _rs;
    }
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
    int avrlength = 0;
    if (aspectvalueresults != null) {
      synchronized (aspectvalueresults) {
        avrlength = aspectvalueresults.length;
        if (avrlength > 0) {
          return (AspectValue[]) aspectvalueresults.clone();
        }
      }
    }

    AspectValue[] newavrs;
    synchronized (rollupresults) {
      double[] res = rollupresults; //getResult();
      int[] ats = aspects;  // getAspectTypes();
      newavrs = new AspectValue[ats.length];
      for (int x = 0; x < ats.length; x++) {
        AspectValue newav = new AspectValue(ats[x], res[x]);
        newavrs[x] = newav;
      }
    }
    // set the newly created aspectvalue array for the next time
    aspectvalueresults = newavrs;

    return (AspectValue[])newavrs.clone();
  }
        
  /** A collection of arrays that represents each phased result.
   * If the result is not phased, use AllocationResult.getResult()
   * @return Enumeration{double[]}  The collection of double[]'s.
   */  
  public Enumeration getPhasedResults() {
    return results.elements();
  }
  
  /** A List of Lists that represent each phased result in the form
   * of AspectValues.
   * If the result is not phased, use getAspectValueResults()
   * @return List  A List of Lists.  Each internal List represents
   * a phased restult.
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

  /** A Collection of AspectTypes representative of the type and
   * order of the aspects in each the result.
   * @param aspectTypes
   */
  private void setAspectTypes(int[] aspectTypes) {
    _lasttype=-1;
    aspects = (int[])aspectTypes.clone();
    clear_ats();
  }
  
  /** Set a single result for each aspect.  If the results are phased,
   * provide the rollup results for each aspect.
   * The order of the aspect results should appear in the
   * same order as specified by setAspectTypes(Enum e).
   * @param result  The doubles representing the result of each aspect.
   */
  private void setResult(double[] result) {
    rollupresults = (double[])result.clone();
    clear_rs();
  }
  
  /** Set the aspectvalues results and also split apart the array to set
   * the aspecttype and result array.
   * @param aspectvalues  The AscpectValues representing the result of each aspect.
   */
  private void setAspectValueResults(AspectValue[] avresult) {
    aspectvalueresults = (AspectValue[])avresult.clone();
    // set up the AspectType array
    int[] astypes = new int[avresult.length];
    for (int avat = 0; avat < avresult.length; avat++ ) {
      astypes[avat] = avresult[avat].getAspectType();
    }
    setAspectTypes(astypes);

    // set up the result array
    double[] dresults = new double[avresult.length];
    for ( int avr = 0; avr < avresult.length; avr++ ) {
      dresults[avr] = avresult[avr].getValue();
    }
    setResult(dresults);
  }
        
  /** A collection of arrays (double[]) that represents each phased result.
   * @param theresults  
   */
  private void setPhasedResults(Enumeration theresults) {
    results.removeAllElements();
    while(theresults.hasMoreElements()) {
      double[] phase = (double[]) theresults.nextElement();
      results.addElement(phase);
    }
    isPhased = true;
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
  public boolean isEqual(AllocationResult testresult) {
    if (this==testresult) return true; // quick success

    if (! (this.isSuccess() == testresult.isSuccess() &&
           this.isPhased() == testresult.isPhased() &&
           this.getConfidenceRating() == testresult.getConfidenceRating() )) {
      return false;
    }
       
    //check the real stuff now!
    //check the aspect types
    int[] traspects = testresult.aspects;
    if (aspects.length != traspects.length) return false;
    for ( int i = 0; i < aspects.length; i++ ) 
      if ( aspects[i] != traspects[i] ) return false;
         
    //check the summary results
    synchronized (rollupresults) {
      double[] testresults = testresult.rollupresults;
      if (rollupresults.length == testresults.length) {
        for (int j = 0; j < rollupresults.length; j++) {
          if ( rollupresults[j] != testresults[j] ) return false;
        }
            
        // check the phased results
        if (isPhased()) {
          Enumeration e = testresult.getPhasedResults();
          Vector testpr = new Vector();
          while (e.hasMoreElements()) {
            double[] testarr = (double[]) e.nextElement();
            testpr.addElement(testarr);
          }
          if (results.size() == testpr.size()) {
            for (int k = 0; k < results.size(); k++) {
              double[] thisarray = (double[]) results.elementAt(k);
              double[] testarray = (double[]) testpr.elementAt(k);
              if ( thisarray.length == testarray.length ) {
                for (int m = 0; m < thisarray.length; m++) {
                  if( thisarray[m] != testarray[m] ) return false;
                }
              }
            }
          }
        }
      }
    }

    // check the aux queries
    
    String[] taux = testresult.auxqueries;
    if (auxqueries != taux) {
      if (taux == null || auxqueries == null) return false;
      int l = auxqueries.length;
      int tl = taux.length;
      if (l != tl) return false;
      for (int i = 0; i<l; i++) {
        String s = auxqueries[i];
        String ts = taux[i];
        if (s==null) {
          if (ts != null) return false;
        } else {
          if (! s.equals(ts)) return false;
        }
      }
    }

    // must be equals...
    return true;
  }

  // added to support AllocationResultBeanInfo

  public String[] getAspectTypesAsArray() {
    int[] aspectTypes = aspects;
    String[] aspectStrings = new String[aspectTypes.length];
    for (int i = 0; i < aspectTypes.length; i++)
      aspectStrings[i] =  AspectValue.aspectTypeToString(aspectTypes[i]);
    return aspectStrings;
  }

  public String getAspectTypeFromArray(int i) {
    int[] aspectTypes = aspects;
    if (i < aspectTypes.length)
      return AspectValue.aspectTypeToString(aspectTypes[i]);
    else
      throw new IllegalArgumentException("AllocationResult.getAspectType(int " + i + " not defined.");
  }

  public String[] getResultsAsArray() {
    synchronized (rollupresults) {
      double[] rresults = rollupresults; //getResult();
      int[] aspectTypes = aspects;
      String[] resultStrings = new String[rresults.length];
      for (int i = 0; i < aspectTypes.length; i++)
      if (aspectTypes[i] == AspectType.START_TIME || 
	  aspectTypes[i] == AspectType.END_TIME) {
	Date d = new Date((long)rresults[i]);
	resultStrings[i] = d.toString();
      } else {
        resultStrings[i] = String.valueOf(rresults[i]);
      }
      return resultStrings;
    }
  }

  public String getResultFromArray(int i) {
    synchronized (rollupresults) {
      double[] rresults = rollupresults; //getResult();
      int[] aspectTypes = aspects;
      if (i < rresults.length) {
        if (aspectTypes[i] == AspectType.START_TIME || 
            aspectTypes[i] == AspectType.END_TIME) {
          Date d = new Date((long)rresults[i]);
          return d.toString();
        } else {
          return String.valueOf(rresults[i]);
        }
      } else {
        throw new IllegalArgumentException("AllocationResult.getResultFromArray(int " + i + " not defined.");
      }
    }
  }

  // returns an array of an array of doubles
  public double[][] getPhasedResultsAsArray() {
    double[][] d = new double[results.size()][];
    for (int i = 0; i < results.size(); i++)
      d[i] = (double[])results.elementAt(i);
    return d;
  }

  public double[] getPhasedResultsFromArray(int i) {
    if (i < results.size()) {
      return (double[])results.elementAt(i);
    } else {
      return null;
    }
  }
    
  private String[] assureAuxqueries() {
    if (auxqueries == null) {
      auxqueries = new String[AQTYPE_COUNT];
    }
    return auxqueries;
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
