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

public class ThreatconTestPlugin extends ServiceUserPluginBase {
  public static final String THREATCON_SENSOR_MEASUREMENT_NAME = "SensorMeasurement.THREATCON";

  private static final OMSMRange[] THREATCON_RANGES = {
    new OMSMPoint("low"),
    new OMSMPoint("high")
  };

  private static final OMSMValueList THREATCON_VALUES = new OMSMValueList(THREATCON_RANGES);

  private SensorMeasurementService sensorMeasurementService;

  private LoggingService logger;

  private static final String[] threatconValues = {"low", "high"};

  private int threatconStep = 0;

  /**
   * Private inner class precludes use by others to set our
   * measurement. Others can only reference the base SensorMeasurement
   * class which has no setter method.
   **/
  private static class ThreatconTestSensorMeasurement extends SensorMeasurement implements NotPersistable {
    public ThreatconTestSensorMeasurement(String name, OMSMValueList allowedValues, Comparable value) {
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

  public ThreatconTestPlugin() {
    super(requiredServices);
  }

  public void setupSubscriptions() {
    ThreatconTestSensorMeasurement threatcon =
      new ThreatconTestSensorMeasurement(THREATCON_SENSOR_MEASUREMENT_NAME, THREATCON_VALUES, threatconValues[0]);
    getBlackboardService().publishAdd(threatcon);
    if (haveServices()) setThreatconSensor();
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
        setThreatconSensor();
      }
    }
  }

  private void setThreatconSensor() {
    ThreatconTestSensorMeasurement threatcon = (ThreatconTestSensorMeasurement)
      sensorMeasurementService.getSensorMeasurementByName(THREATCON_SENSOR_MEASUREMENT_NAME);
    if (threatcon != null) {
      if (logger.isInfoEnabled()) logger.info("Setting threatcon = " + threatconValues[threatconStep]);
      threatcon.setValue(threatconValues[threatconStep]);
      getBlackboardService().publishChange(threatcon);
      threatconStep++;
      if (threatconStep == threatconValues.length) threatconStep = 0;
    }
    startTimer(115000);
  }
}
