package org.cougaar.core.adaptivity;

import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.SensorMeasurementService;
import org.cougaar.core.component.Service;
import org.cougaar.core.persist.NotPersistable;
import org.cougaar.util.GenericStateModelAdapter;

public class CPUTestPlugin extends ServiceUserPluginBase {
  public static final String CPU_SENSOR_MEASUREMENT_NAME = "SensorMeasurement.CPU";

  private static final OMSMRange[] CPU_RANGES = {
    new OMSMRange(0.0, 1.0)
  };

  private static final OMSMValueList CPU_VALUES = new OMSMValueList(CPU_RANGES);

  private SensorMeasurementService sensorMeasurementService;

  private LoggingService logger;

  private static final Double[] cpuValues = {
    new Double(1.0),
    new Double(0.9),
    new Double(0.8),
    new Double(0.7),
    new Double(0.6),
    new Double(0.5),
    new Double(0.4),
    new Double(0.3),
    new Double(0.2),
    new Double(0.1),
    new Double(0.09),
    new Double(0.05),
    new Double(0.09),
    new Double(0.1),
    new Double(0.2),
    new Double(0.3),
    new Double(0.4),
    new Double(0.5),
    new Double(0.6),
    new Double(0.7),
    new Double(0.8),
    new Double(0.9)
  };

  private int cpuStep = 0;

  /**
   * Private inner class precludes use by others to set our
   * measurement. Others can only reference the base SensorMeasurement
   * class which has no setter method.
   **/
  private static class CPUTestSensorMeasurement extends SensorMeasurement implements NotPersistable {
    public CPUTestSensorMeasurement(String name, OMSMValueList allowedValues, Comparable value) {
      super(name, allowedValues, value);
    }

    public void setValue(Comparable newValue) {
      super.setValue(newValue);
    }
  }

  private static final Class[] requiredServices = {
    LoggingService.class,
    SensorMeasurementService.class
  };

  public CPUTestPlugin() {
    super(requiredServices);
  }

  public void setupSubscriptions() {
    CPUTestSensorMeasurement cpu =
      new CPUTestSensorMeasurement(CPU_SENSOR_MEASUREMENT_NAME, CPU_VALUES, cpuValues[0]);
    getBlackboardService().publishAdd(cpu);
    if (haveServices()) setCPUSensor();
  }

  private boolean haveServices() {
    if (logger != null) return true;
    if (acquireServices()) {
      ServiceBroker sb = getServiceBroker();
      logger = (LoggingService)
        sb.getService(this, LoggingService.class, null);
      sensorMeasurementService = (SensorMeasurementService)
        sb.getService(this, SensorMeasurementService.class, null);
      return true;
    }
    return false;
  }

  public void execute() {
    if (timerExpired()) {
      if (haveServices()) {
        cancelTimer();
        setCPUSensor();
      }
    }
  }

  private void setCPUSensor() {
    CPUTestSensorMeasurement cpu = (CPUTestSensorMeasurement)
      sensorMeasurementService.getSensorMeasurementByName(CPU_SENSOR_MEASUREMENT_NAME);
    if (cpu != null) {
      if (logger.isInfoEnabled()) logger.info("Setting cpu = " + cpuValues[cpuStep]);
      cpu.setValue(cpuValues[cpuStep]);
      getBlackboardService().publishChange(cpu);
      cpuStep++;
      if (cpuStep == cpuValues.length) cpuStep = 0;
    }
    startTimer(10000);
  }
}
