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
package org.cougaar.core.agent.service.registry;

import org.cougaar.core.domain.*;

import org.cougaar.core.service.*;

import org.cougaar.core.agent.*;

import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.NewTypeIdentificationPG;
import org.cougaar.planning.ldm.asset.TypeIdentificationPG;
import java.util.Enumeration;
import org.cougaar.core.domain.LDMServesPlugin;
//import org.cougaar.core.plugin.SimplePlugin;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.plugin.PrototypeProvider;

import org.cougaar.core.domain.RootFactory;
import org.cougaar.core.domain.Factory;
//import org.cougaar.core.plugin.PluginAdapter;

import org.cougaar.core.plugin.LDMService;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.component.ServiceRevokedEvent;

import org.cougaar.core.agent.service.domain.DomainServiceProvider;
import org.cougaar.core.agent.service.domain.DomainServiceImpl;
import org.cougaar.core.service.DomainService;
import org.cougaar.core.domain.DomainManager;

import org.cougaar.core.service.PrototypeRegistryService;
import org.cougaar.core.agent.service.registry.PrototypeRegistryServiceProvider;
import org.cougaar.core.agent.service.registry.PrototypeRegistry;

import org.cougaar.core.agent.service.uid.UIDServiceProvider;
import org.cougaar.core.agent.service.uid.UIDServiceImpl;
import org.cougaar.core.service.UIDService;
import org.cougaar.core.service.UIDServer;
import org.cougaar.core.util.UID;

/** Serve some default prototypes to the system.
 * At this point, this only serves stupid prototypes for
 * (temporary) backward compatability.
 *
 * At start, loads some low-level basics into the registry.
 * On demand, serve a few more.
 **/

public class DefaultPrototypeProviderPlugin 
  extends ComponentPlugin
  implements PrototypeProvider 
{
  
  RootFactory theFactory = null;  
  DomainService domainService = null;
  PrototypeRegistryService protregService = null;
  UIDService uidService = null;
  UID anID;
  
  public DefaultPrototypeProviderPlugin() {}
  
  protected void setupSubscriptions() {
  
    // get the domain service  	
    if (theFactory == null) {
      domainService = (DomainService) getBindingSite().getServiceBroker().getService(
					       this, DomainService.class,
				          new ServiceRevokedListener() {
				   public void serviceRevoked(ServiceRevokedEvent re) {
				  	   theFactory = null;
			  		 }
			            });
      
      // get the registry service  	
      protregService = (PrototypeRegistryService) getBindingSite().getServiceBroker().getService(
    				       this, PrototypeRegistryService.class,
        				 new ServiceRevokedListener() {
			  	     public void serviceRevoked(ServiceRevokedEvent re) {
				      theFactory = null;
       				     }
				   });
      
      // get the UIDService   	
      uidService = (UIDService) getBindingSite().getServiceBroker().getService(
						   this, UIDService.class,
					          new ServiceRevokedListener() {
	   			   public void serviceRevoked(ServiceRevokedEvent re) {								     theFactory = null;
					   }
       				 });
      
      
      
    }
    //use the services
    theFactory = domainService.getFactory();
    //anID = uidService.nextUID();
    
    preloadPrototypes();
  }
  
  // no subscriptions, so we'll never actually be run.
  protected void execute() {}
  
  
  public Asset getPrototype(String typename, Class hint) {
    // I was going to handle OPlan here, but OPlan isn't an Asset!
    try {
      // try some dynamic prototypes (for backward compatibility)
      if ("Solenoid".equals(typename)) {
        return makeProto(typename, "Consumable");
      } 
      else if ("M1A1 Tank".equals(typename)) {
        return makeProto(typename, "SelfPropelledGroundWeapon");
      }
      else if ("OTHER/Passenger".equals(typename)) {
        return makeProto(typename, "Person");
      }
      else if ("NSN/1520011069519".equals(typename)) {
        return makeProto(typename, "RotaryWingAircraftWeapon");
      }
      else if ("NSN/2350010871095".equals(typename)) {
        return makeProto(typename, "SelfPropelledGroundWeapon");
      }
      else if ("NSN/2350001226826".equals(typename)) {
        return makeProto(typename, "SelfPropelledGroundWeapon");
      }
      else if ("NSN/2350010318851".equals(typename)) {
        return makeProto(typename, "SelfPropelledGroundWeapon");
      }
      else if ("UTC/SupportOrg".equals(typename)) {
        return makeProto(typename, "SupportOrganization");
      }
      else if ("UTC/SupplyOrg".equals(typename)) {
        return makeProto(typename, "SupplyOrganization");
      }
      else if ("UTC/CombatOrg".equals(typename)) {
        return makeProto(typename, "CombatOrganization");
      }
      else if ("UTC/CommandOrg".equals(typename)) {
        return makeProto(typename, "CommandOrganization");
      }
      else if ("Organization".equals(typename)) {
        return makeProto(typename, "Organization");
      }
      
      
    } catch (Exception e) {
      // cannot really throw any of these exceptions.
    }
    return null;
  }
		
  
  private void submitAbstract(String name) {
    try {
      Asset proto = makeProto(name, "AbstractAsset");
      NewTypeIdentificationPG tip = (NewTypeIdentificationPG)proto.getTypeIdentificationPG();
      tip.setTypeIdentification(name);
      tip.setNomenclature(name);
      protregService.cachePrototype(name, proto);
      
      // here test out UIDService 
      anID = uidService.nextUID();
      //System.out.println(anID);
      //     System.out.println("cluster id = " + uidService.getClusterIdentifier());
    } catch (Exception e) {
      // cannot really throw any of these exceptions.
    }
  }
  
  
  /*
   * modified makeProto to make prototypes with under the 'root' domain  and
   * register prototypes via the PrototypeRegistry 
   * make a prototype and an instance asset,  then test property groups 
   */
  private Asset makeProto(String typeid, String cl) {
    
    // create a prototype - register with registry service
    Asset proto = theFactory.createPrototype(cl, typeid);
  
    protregService.cachePrototype(typeid, proto);
    //* getLDM().cachePrototype(typeid, proto);
    System.out.println("making prototype: " + cl);
    return proto;
  }
  
  private void preloadPrototypes() {
    submitAbstract("Subordinates");
    submitAbstract("Ammunition");
    submitAbstract("SpareParts");
    submitAbstract("Consumable");

    submitAbstract("Repairable");
    submitAbstract("StrategicTransportation");
    submitAbstract("GSMaintenance");
    submitAbstract("DSMaintenance");
    submitAbstract("TheaterTransportation");
  }

}
