/* 
 * <copyright>
 *  Copyright 2002-2003 BBNT Solutions, LLC
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

package org.cougaar.core.adaptivity;

import org.cougaar.core.qos.metrics.Metric;
import org.cougaar.core.qos.metrics.MetricsService;
import org.cougaar.core.qos.metrics.VariableEvaluator;

public abstract class MetricsCondition implements Condition {
  public static final java.lang.String METRICS_PREFIX = "metrics:";
  public static final java.lang.String METRICS_DOUBLE_PREFIX = METRICS_PREFIX + "double:";
  public static final java.lang.String METRICS_STRING_PREFIX = METRICS_PREFIX + "string:";
  public static final java.lang.String METRICS_INTEGER_PREFIX = METRICS_PREFIX + "int:";
  public static final java.lang.String METRICS_LONG_PREFIX = METRICS_PREFIX + "long:";
  public static final java.lang.String METRICS_BOOLEAN_PREFIX = METRICS_PREFIX + "boolean:";

  private static final OMCRangeList ALL_BOOLEAN_RANGE_LIST =
    new OMCRangeList(new int[] {0, 1});

  private static final java.lang.Integer ONE = new java.lang.Integer(1);
  private static final java.lang.Integer ZERO = new java.lang.Integer(0);

  public static MetricsCondition create(java.lang.String name,
                                        MetricsService metricsService,
                                        VariableEvaluator variableEvaluator)
  {
    if (name.startsWith(METRICS_DOUBLE_PREFIX)) {
      return new Double(name, metricsService, variableEvaluator);
    }
    if (name.startsWith(METRICS_STRING_PREFIX)) {
      return new String(name, metricsService, variableEvaluator);
    }
    if (name.startsWith(METRICS_INTEGER_PREFIX)) {
      return new Integer(name, metricsService, variableEvaluator);
    }
    if (name.startsWith(METRICS_BOOLEAN_PREFIX)) {
      return new Boolean(name, metricsService, variableEvaluator);
    }
    throw new IllegalArgumentException("Unknown MetricsCondition type: " + name);
  }

  private java.lang.String name;
  private OMCRangeList allowedValues;
  private java.lang.String metricsPath;
  private MetricsService metricsService;
  private VariableEvaluator variableEvaluator;

  protected MetricsCondition(java.lang.String name,
                             MetricsService metricsService,
                             VariableEvaluator variableEvaluator,
                             OMCRangeList allowedValues,
                             java.lang.String prefix)
  {
    this.metricsService = metricsService;
    this.variableEvaluator= variableEvaluator;
    this.name = name;
    this.allowedValues = allowedValues;
    metricsPath = name.substring(prefix.length());
    Metric testMetric = getMetric();
    if (testMetric == null) {
      throw new IllegalArgumentException("Metric has no value:" + metricsPath);
    }
  }

  public java.lang.String toString() {
    return "MetricsCondition(" + getName() + ")";
  }

  protected Metric getMetric() {
    return metricsService.getValue(metricsPath, variableEvaluator);
  }

  public java.lang.String getName() {
    return name;
  }

  public OMCRangeList getAllowedValues() {
    return allowedValues;
  }

  public abstract Comparable getValue();

  public static class Double extends MetricsCondition {
    public Double(java.lang.String name,
                  MetricsService metricsService,
                  VariableEvaluator variableEvaluator)
    {
      super(name, metricsService, variableEvaluator,
            OMCRangeList.ALL_DOUBLE_RANGE_LIST, METRICS_DOUBLE_PREFIX);
    }

    protected Double(java.lang.String name,
                     MetricsService metricsService,
                     VariableEvaluator variableEvaluator,
                     java.lang.String prefix)
    {
      super(name, metricsService, variableEvaluator,
            OMCRangeList.ALL_DOUBLE_RANGE_LIST, prefix);
    }

    public Comparable getValue() {
      Metric metric = getMetric();
      Object rawValue = metric.getRawValue();
      if (rawValue instanceof java.lang.Double) return (java.lang.Double) rawValue;
      return new java.lang.Double(metric.doubleValue());
    }
  }

  public static class Default extends Double {
    public Default(java.lang.String name,
                   MetricsService metricsService,
                   VariableEvaluator variableEvaluator)
    {
      super(name, metricsService, variableEvaluator, METRICS_DOUBLE_PREFIX);
    }
  }

  public static class Integer extends MetricsCondition {
    public Integer(java.lang.String name,
                   MetricsService metricsService,
                   VariableEvaluator variableEvaluator)
    {
      super(name, metricsService, variableEvaluator,
            OMCRangeList.ALL_INTEGER_RANGE_LIST, METRICS_INTEGER_PREFIX);
    }

    public Comparable getValue() {
      Metric metric = getMetric();
      Object rawValue = metric.getRawValue();
      if (rawValue instanceof java.lang.Integer) return (java.lang.Integer) rawValue;
      return new java.lang.Integer(metric.intValue());
    }
  }

  public static class Long extends MetricsCondition {
    public Long(java.lang.String name,
                MetricsService metricsService,
                VariableEvaluator variableEvaluator)
    {
      super(name, metricsService, variableEvaluator,
            OMCRangeList.ALL_LONG_RANGE_LIST, METRICS_LONG_PREFIX);
    }

    public Comparable getValue() {
      Metric metric = getMetric();
      Object rawValue = metric.getRawValue();
      if (rawValue instanceof java.lang.Long) return (java.lang.Long) rawValue;
      return new java.lang.Long(metric.longValue());
    }
  }

  public static class String extends MetricsCondition {
    public String(java.lang.String name,
                  MetricsService metricsService,
                  VariableEvaluator variableEvaluator)
    {
      super(name, metricsService, variableEvaluator,
            OMCRangeList.ALL_STRING_RANGE_LIST, METRICS_STRING_PREFIX);
    }

    public Comparable getValue() {
      Metric metric = getMetric();
      Object rawValue = metric.getRawValue();
      if (rawValue instanceof java.lang.String) return (java.lang.String) rawValue;
      return metric.stringValue();
    }
  }

  public static class Boolean extends MetricsCondition {
    public Boolean(java.lang.String name,
                   MetricsService metricsService,
                   VariableEvaluator variableEvaluator)
    {
      super(name, metricsService, variableEvaluator,
            ALL_BOOLEAN_RANGE_LIST, METRICS_BOOLEAN_PREFIX);
    }
    
    public Comparable getValue() {
      Metric metric = getMetric();
      if (metric.booleanValue()) {
        return ONE;
      } else {
        return ZERO;
      }
    }
  }
}
