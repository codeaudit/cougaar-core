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

package org.cougaar.core.wp.resolver;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.core.service.wp.WhitePagesService;
import org.cougaar.util.ConfigFinder;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * This component reads the bootstrap config and fills
 * the TableService.
 */
public class ConfigReader
extends GenericStateModelAdapter
implements Component
{
  private static final String FILENAME =
    "alpreg.ini";
  private static final String SERVER_PROP =
    "org.cougaar.name.server";
  private static final String PORT_PROP =
    "org.cougaar.name.server.port";

  private ServiceBroker sb;
  private LoggingService logger;
  private AgentIdentificationService agentIdService;
  private WhitePagesService wps;

  private String agentName;

  private String host;
  private int port;

  public void setBindingSite(BindingSite bs) {
    this.sb = bs.getServiceBroker();
  }

  public void load() {
    super.load();

    logger = (LoggingService) sb.getService(
        this, LoggingService.class, null);
    agentIdService = (AgentIdentificationService) sb.getService(
        this, AgentIdentificationService.class, null);
    wps = (WhitePagesService) sb.getService(
        this, WhitePagesService.class, null);

    agentName = agentIdService.getMessageAddress().getAddress();

    // do all our work
    readConfig();

    if (wps != null) {
      sb.releaseService(this, WhitePagesService.class, wps);
      wps = null;
    }
    if (agentIdService != null) {
      sb.releaseService(
          this, AgentIdentificationService.class, agentIdService);
      agentIdService = null;
    }
    if (logger != LoggingService.NULL) {
      sb.releaseService(this, LoggingService.class, logger);
      logger = null;
    }
    sb = null;
  }

  private void readConfig() {
    read_file(FILENAME);
    read_props();
    if (host != null && port > 0) {
      // backwards compatibility!
      parse("WP=-RMI_REG,rmi://"+host+":"+port+"/*");
    }
  }
  
  private void add(AddressEntry ae) {
    if (logger.isInfoEnabled()) {
      logger.info("Add bootstrap entry: "+ae);
    }
    try {
      wps.hint(ae);
    } catch (Exception e) {
      if (logger.isErrorEnabled()) {
        logger.error(
            "Unable to add bootstrap hint: "+ae, e);
      }
    }
  }

  private void parse(String line) {
    if (line.startsWith("#") ||
        line.startsWith("[") ||
        line.startsWith("//")) {
      return;
    }
    int index = line.indexOf('=');
    if (index <= 0) {
      return;
    }
    String name = line.substring(0, index).trim();
    String value = line.substring(index + 1).trim();
    if ("address".equals(name) ||
        "hostname".equals(name)) {
      host = value;
    } else if ("port".equals(name)) {
      port = Integer.parseInt(value);
    } else if (
        "alias".equals(name) ||
        "lpsport".equals(name)) {
      // ignore
    } else {
      int sep = value.indexOf(',');
      if (sep >= 0) {
        String type = value.substring(0, sep).trim();
        String suri = value.substring(sep+1).trim();
        URI uri;
        try {
          uri = URI.create(suri);
        } catch (Exception e) {
          throw new RuntimeException("Invalid line: "+line, e);
        }
        AddressEntry ae = AddressEntry.getAddressEntry(
            name, type, uri);
        add(ae);
      }
    }
  }

  private void read_file(String filename) {
    try {
      InputStream fs = ConfigFinder.getInstance().open(filename);
      if (fs != null) {
        BufferedReader in = 
          new BufferedReader(new InputStreamReader(fs));
        String line = null;
        while ((line = in.readLine()) != null) {
          parse(line);
        }
        in.close();
      }
    } catch (FileNotFoundException fnfe) {
    } catch(Exception e) {
      if (logger.isErrorEnabled()) {
        logger.error("Failed read of "+filename, e);
      }
    }
  }

  private void read_props() {
    String server = System.getProperty(SERVER_PROP);
    if (server != null) {
      int sep1 = server.indexOf(':');
      String rawHost = 
        (sep1 >= 0 ?
         server.substring(0, sep1) :
         server);
      try {
        InetAddress addr = InetAddress.getByName(rawHost);
        host = addr.getHostAddress();
      } catch (UnknownHostException uhe) {
        logger.error(SERVER_PROP+" specifies unknown host: " + server);
      }
      if (sep1 >= 0) {
        int sep2 = server.indexOf(':', sep1+1);
        if (sep2 < 0) {
          sep2 = server.length();
        }
        String sport = server.substring(sep1+1, sep2);
        try {
          port = Integer.parseInt(sport);
        } catch (NumberFormatException nfe) {
          logger.error(
              SERVER_PROP+" port is not an integer: "+server);
        }
      }
    }
    String serverPort = System.getProperty(PORT_PROP);
    if (serverPort != null) {
      try {
        port = Integer.parseInt(serverPort);
      } catch (NumberFormatException nfe) {
        logger.error(PORT_PROP+" is not an integer: " + serverPort);
      }
    }
  }
}
