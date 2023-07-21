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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import jpl.gds.sleproxy.server.sleinterface.profile.ESLEInterfaceProfilePropertyField;
import org.restlet.data.Status;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.ServerResource;

import jpl.gds.sleproxy.common.resources.ISLEInterfaceProfilesResource;
import jpl.gds.sleproxy.server.sleinterface.profile.SLEInterfaceProfileFactory;
import jpl.gds.sleproxy.server.sleinterface.profile.SLEInterfaceProfileManager;
import jpl.gds.sleproxy.server.sleinterface.profile.SLEInterfaceProfilePropertiesUtil;
import jpl.gds.sleproxy.server.websocket.MessageDistributor;

/**
 * Restlet resource for the SLE interface "profiles" API.
 * 
 */
public class SLEInterfaceProfilesServerResource extends ServerResource implements ISLEInterfaceProfilesResource {

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.nasa.jpl.ammos.sle_proxy_common.resources.
	 * ISLEInterfaceProfilesResource#toJson()
	 */
	@Override
	public final Representation toJson() {
		String providedProfileName = getAttribute("profile-name");

		if (providedProfileName == null) {
			return new JacksonRepresentation<List<Map<String, String>>>(
					SLEInterfaceProfileManager.INSTANCE.getProfilesPropertiesStructuredForJson());
		} else {

			if (SLEInterfaceProfileManager.INSTANCE.contains(providedProfileName)) {
				return new JacksonRepresentation<Map<String, String>>(
						SLEInterfaceProfileManager.INSTANCE.getProfilePropertiesStructuredForJson(providedProfileName));
			} else {
				return new JacksonRepresentation<Map<String, String>>(new HashMap<String, String>());
			}

		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.nasa.jpl.ammos.sle_proxy_common.resources.
	 * ISLEInterfaceProfilesResource#store()
	 */
	@Override
	public final void store(final Representation rep) {
		String providedProfileName = getAttribute("profile-name");

		if (providedProfileName == null) {
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "Profile name missing in request");
		} else {

			if (SLEInterfaceProfileManager.INSTANCE.contains(providedProfileName)) {
				getResponse().setStatus(Status.CLIENT_ERROR_CONFLICT, providedProfileName + " already exists");
			} else {
				JacksonRepresentation<Properties> profileRep = new JacksonRepresentation<Properties>(rep,
						Properties.class);

				try {
					Properties p = profileRep.getObject();
					Map<String, String> propertiesMap = new HashMap<>(p.size());
					p.stringPropertyNames().forEach(name -> {
						validateHosts(p, name);
						propertiesMap.put(name, p.getProperty(name));
					});

					SLEInterfaceProfileManager.INSTANCE.put(providedProfileName,
							SLEInterfaceProfileFactory.createProfile(providedProfileName,
									SLEInterfaceProfilePropertiesUtil.deriveProfilePropertiesFromJsonStructure(
											providedProfileName, propertiesMap)));
					SLEInterfaceProfileManager.INSTANCE.save();
					
					// Send SLE provider profile creation notice to clients
					MessageDistributor.INSTANCE.sleProfileCreate(providedProfileName, propertiesMap);
				
				} catch (IOException ie) {
					getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST,
							"Could not deserialize body content properly. Check format.");
				} catch (IllegalArgumentException iae) {
					getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST,
							iae.getMessage());
				}

			}

		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.nasa.jpl.ammos.sle_proxy_common.resources.
	 * ISLEInterfaceProfilesResource#accept()
	 */
	@Override
	public final void accept(final Representation rep) {
		String providedProfileName = getAttribute("profile-name");

		if (providedProfileName == null) {
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "Profile name missing in request");
		} else {

			if (!SLEInterfaceProfileManager.INSTANCE.contains(providedProfileName)) {
				getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, providedProfileName + " does not exist");
			} else {
				JacksonRepresentation<Properties> profileRep = new JacksonRepresentation<Properties>(rep,
						Properties.class);

				try {
					Properties p = profileRep.getObject();
					Map<String, String> propertiesMap = new HashMap<>(p.size());
					p.stringPropertyNames().forEach(name -> {
						validateHosts(p, name);
						propertiesMap.put(name, p.getProperty(name));
					});

					SLEInterfaceProfileManager.INSTANCE.update(providedProfileName, SLEInterfaceProfilePropertiesUtil
							.deriveProfilePropertiesFromJsonStructure(providedProfileName, propertiesMap));
					SLEInterfaceProfileManager.INSTANCE.save();
					
					// Send SLE provider profile update notice to clients
					MessageDistributor.INSTANCE.sleProfileUpdate(providedProfileName, propertiesMap);
					
				} catch (IOException ie) {
					getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST,
							"Could not deserialize body content properly. Check format.");
				} catch (IllegalArgumentException iae) {
					getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST,
							iae.getMessage());
				}

			}

		}

	}

	private static void validateHosts(final Properties p, final String name) {
		if (name.equalsIgnoreCase(ESLEInterfaceProfilePropertyField.PROVIDER_HOSTS.name())) {
			if (!SLEInterfaceProfilePropertiesUtil.validateHosts(p.getProperty(name))) {
				throw new IllegalArgumentException(
						"Hosts string is invalid. A valid list of hosts consists of 'host:port' pairs separated by the '|' pipe character.");
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.nasa.jpl.ammos.sle_proxy_common.resources.
	 * ISLEInterfaceProfilesResource#remove()
	 */
	@Override
	public final void remove() {
		String providedProfileName = getAttribute("profile-name");

		if (providedProfileName == null) {
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "Profile name missing in request");
		} else {

			if (!SLEInterfaceProfileManager.INSTANCE.contains(providedProfileName)) {
				getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, providedProfileName + " does not exist");
			} else {
				SLEInterfaceProfileManager.INSTANCE.remove(providedProfileName);
				SLEInterfaceProfileManager.INSTANCE.save();
				
				// Send SLE provider profile deletion notice to clients
				MessageDistributor.INSTANCE.sleProfileDelete(providedProfileName);
			}

		}

	}

}