/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.domain.planning.ldm;

import org.cougaar.domain.planning.ldm.asset.Asset;
import org.cougaar.domain.planning.ldm.asset.NewTypeIdentificationPG;
import org.cougaar.domain.planning.ldm.asset.TypeIdentificationPG;
import java.util.Enumeration;
import org.cougaar.domain.planning.ldm.LDMServesPlugIn;
//import org.cougaar.core.plugin.SimplePlugIn;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.plugin.PrototypeProvider;

import org.cougaar.domain.planning.ldm.RootFactory;
import org.cougaar.domain.planning.ldm.Factory;
//import org.cougaar.core.plugin.PlugInAdapter;

import org.cougaar.core.plugin.LDMService;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.component.ServiceRevokedEvent;

import org.cougaar.domain.planning.ldm.DomainServiceProvider;
import org.cougaar.domain.planning.ldm.DomainServiceImpl;
import org.cougaar.domain.planning.ldm.DomainService;
import org.cougaar.domain.planning.ldm.DomainManager;

import org.cougaar.domain.planning.ldm.PrototypeRegistryService;
import org.cougaar.domain.planning.ldm.PrototypeRegistryServiceProvider;
import org.cougaar.domain.planning.ldm.PrototypeRegistry;

import org.cougaar.core.cluster.UIDServiceProvider;
import org.cougaar.core.cluster.UIDServiceImpl;
import org.cougaar.core.cluster.UIDService;
import org.cougaar.core.cluster.UIDServer;
import org.cougaar.core.society.UID;

/** Serve some default prototypes to the system.
 * At this point, this only serves stupid prototypes for
 * (temporary) backward compatability.
 *
 * At start, loads some low-level basics into the registry.
 * On demand, serve a few more.
 **/

public class DefaultPrototypeProviderPlugIn 
    extends ComponentPlugin
    implements PrototypeProvider 
{
    
    RootFactory theFactory = null;  
		//	LDMServesPlugIn theLDM = null;
    DomainService domainService = null;
    PrototypeRegistryService protregService = null;
    UIDService uidService = null;
    UID anID;
    
    public DefaultPrototypeProviderPlugIn() {}
    
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
																	 						public void serviceRevoked(ServiceRevokedEvent re) {
																 							theFactory = null;
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
			long num = anID.getId();
			System.out.println("NEXT UID MADE IS: " + name + " " + num);
			System.out.println("cluster id = " + uidService.getClusterIdentifier());
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
