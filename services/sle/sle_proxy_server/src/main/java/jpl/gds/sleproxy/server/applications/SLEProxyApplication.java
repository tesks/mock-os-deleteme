/*
 * Copyright 2006-2018. California Institute of Technology.
 * ALL RIGHTS RESERVED.
 * U.S. Government sponsorship acknowledged.
 *
 * This software is subject to U. S. export control laws and
 * regulations (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the
 * extent that the software is subject to U.S. export control laws
 * and regulations, the recipient has the responsibility to obtain
 * export licenses or other export authority as may be required
 * before exporting such information to foreign countries or
 * providing access to foreign nationals.
 */
package jpl.gds.sleproxy.server.applications;

import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.routing.Router;

import jpl.gds.sleproxy.server.chillinterface.config.ChillInterfaceConfigManager;
import jpl.gds.sleproxy.server.chillinterface.internal.config.ChillInterfaceInternalConfigManager;
import jpl.gds.sleproxy.server.chillinterface.uplink.ChillInterfaceUplinkManager;
import jpl.gds.sleproxy.server.messages.config.MessagesConfigManager;
import jpl.gds.sleproxy.server.resources.ChillInterfaceConfigServerResource;
import jpl.gds.sleproxy.server.resources.ChillInterfaceDownlinkActionServerResource;
import jpl.gds.sleproxy.server.resources.ChillInterfaceUplinkActionServerResource;
import jpl.gds.sleproxy.server.resources.MessagesServerResource;
import jpl.gds.sleproxy.server.resources.RootServerResource;
import jpl.gds.sleproxy.server.resources.SLEInterfaceForwardActionServerResource;
import jpl.gds.sleproxy.server.resources.SLEInterfaceProfilesServerResource;
import jpl.gds.sleproxy.server.resources.SLEInterfaceReturnActionServerResource;
import jpl.gds.sleproxy.server.resources.StateServerResource;
import jpl.gds.sleproxy.server.sleinterface.internal.config.SLEInterfaceInternalConfigManager;
import jpl.gds.sleproxy.server.sleinterface.profile.SLEInterfaceProfileManager;
import jpl.gds.sleproxy.server.state.ProxyStateManager;

/**
 * Entry-point for the chill_sle_proxy Restlet application.
 * 
 */
public class SLEProxyApplication extends Application {

	/**
	 * Default constructor for the chill_sle_proxy Restlet application.
	 */
	public SLEProxyApplication() {
		setName("chill_sle_proxy Application");
		setDescription("Restlet application to bridge existing AMPCS TC and TM "
				+ "applications with SLE services, i.e. a proxy.");
		setOwner("Jet Propulsion Laboratory (JPL), California Institute of " + "Technology (Caltech)");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.restlet.Application#start()
	 */
	@Override
	public final synchronized void start() throws Exception {
		super.start();
		initFirst();
		initSecond();
		initLast();
	}

	/**
	 * Private method to do information gathering that needs to be done before
	 * all other information gathering and initializations.
	 * 
	 * This method was created because MessagesConfigManager needs to be
	 * initialized before everything else.
	 * 
	 * @throws Exception
	 *             Thrown when information gathering fails
	 */
	private void initFirst() throws Exception {
		MessagesConfigManager.INSTANCE.init(getContext().getParameters().getFirstValue("messages-config-file"));
	}

	/**
	 * Private method to do information gathering that needs to be done after
	 * other information gathering.
	 * 
	 * @throws Exception
	 *             Thrown when information gathering fails
	 */
	private void initSecond() throws Exception {
		ProxyStateManager.INSTANCE.init(getContext().getParameters().getFirstValue("proxy-state-file"));
		SLEInterfaceInternalConfigManager.INSTANCE
				.init(getContext().getParameters().getFirstValue("sle-interface-internal-config-file"));
		SLEInterfaceProfileManager.INSTANCE.init(
				getContext().getParameters().getFirstValue("sle-interface-profiles-file"),
				getContext().getParameters().getFirstValue("sle-interface-passwords-file"));
		ChillInterfaceConfigManager.INSTANCE
				.init(getContext().getParameters().getFirstValue("chill-interface-config-file"));
		ChillInterfaceInternalConfigManager.INSTANCE
				.init(getContext().getParameters().getFirstValue("chill-interface-internal-config-file"));
	}

	/**
	 * Private method to initialize things only after all information gathering
	 * has been completed.
	 * 
	 * @throws Exception
	 *             Thrown when initialization(s) fail(s)
	 */
	private void initLast() throws Exception {
		// Automatic services based on the gathered information block
		ChillInterfaceUplinkManager.INSTANCE.init();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.restlet.Application#createInboundRoot()
	 */
	@Override
	public final Restlet createInboundRoot() {
		Router router = new Router(getContext());
		router.attach("", RootServerResource.class);
		router.attach("/", RootServerResource.class);
		router.attach("/sle-interface/profiles", SLEInterfaceProfilesServerResource.class);
		router.attach("/sle-interface/profiles/{profile-name}", SLEInterfaceProfilesServerResource.class);
		router.attach("/chill-interface/config", ChillInterfaceConfigServerResource.class);
		router.attach("/sle-interface/forward/action/{action}", SLEInterfaceForwardActionServerResource.class);
		router.attach("/sle-interface/return/action/{action}", SLEInterfaceReturnActionServerResource.class);
		router.attach("/chill-interface/uplink/action/{action}", ChillInterfaceUplinkActionServerResource.class);
		router.attach("/chill-interface/downlink/action/{action}", ChillInterfaceDownlinkActionServerResource.class);
		router.attach("/messages", MessagesServerResource.class);
		router.attach("/state", StateServerResource.class);
		return router;
	}

}
