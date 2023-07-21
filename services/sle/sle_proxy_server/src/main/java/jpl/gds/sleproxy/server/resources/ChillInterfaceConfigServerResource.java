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
package jpl.gds.sleproxy.server.resources;

import java.io.IOException;
import java.util.Properties;

import org.restlet.data.Status;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.ServerResource;

import jpl.gds.sleproxy.common.resources.IChillInterfaceConfigResource;
import jpl.gds.sleproxy.server.chillinterface.config.ChillInterfaceConfigManager;
import jpl.gds.sleproxy.server.chillinterface.config.EChillInterfaceConfigPropertyField;
import jpl.gds.sleproxy.server.websocket.MessageDistributor;

/**
 * Restlet resource for the chill interface "config" API.
 * 
 */
public class ChillInterfaceConfigServerResource extends ServerResource implements IChillInterfaceConfigResource {

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.nasa.jpl.ammos.sle_proxy_common.resources.
	 * IChillInterfaceConfigResource#toJson()
	 */
	@Override
	public final Representation toJson() {
		return new JacksonRepresentation<Properties>(ChillInterfaceConfigManager.INSTANCE.getConfigProperties());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.nasa.jpl.ammos.sle_proxy_common.resources.
	 * IChillInterfaceConfigResource#accept(org.restlet.representation.
	 * Representation)
	 */
	@Override
	public final void accept(final Representation rep) {
		JacksonRepresentation<Properties> configRep = new JacksonRepresentation<Properties>(rep, Properties.class);

		try {
			Properties configProperties = configRep.getObject();
			// Disallow changing the uplink listening port
			configProperties.remove(EChillInterfaceConfigPropertyField.UPLINK_LISTENING_PORT.name());			
			ChillInterfaceConfigManager.INSTANCE.setFromProperties(configProperties);
			ChillInterfaceConfigManager.INSTANCE.save();
			
			// Send chill_down config updates to clients
			MessageDistributor.INSTANCE.chillConfigUpdate(configProperties);
			
		} catch (IllegalArgumentException | IOException e) {
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST,
					"Could not deserialize body content properly. Check format.");
		}
		
	}

}