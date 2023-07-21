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
package jpl.gds.session.config;

import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.context.api.IContextIdentification;
import jpl.gds.context.impl.ContextIdentification;
import jpl.gds.shared.metadata.IMetadataHeaderProvider;
import jpl.gds.shared.metadata.context.IContextKey;

/**
 * The SessionIdentification object is an implementation of the
 * IContextIdentification interface that works with the classic session
 * configuration. It incorporates the minimum set of session configuration
 * information to identify the session and locate it on the network.
 * 
 *
 */
public class SessionIdentification extends ContextIdentification implements
		IMetadataHeaderProvider, IContextIdentification {

	/** Default suffix used when constructing default session name **/
	public static final String DEFAULT_SESSION_NAME_SUFFIX = "_session";

	/**
	 * Creates an empty SessionIdentification. 
	 * @param mp the current mission properties object
	 */
	public SessionIdentification(final MissionProperties mp) {
		super(mp);
	}
     
	/**
	 * Creates a SessionIdentification with the specified spacecraft ID.
	 * @param mp the current mission properties object
	 * @param scid spacecraft ID.
	 */
    public SessionIdentification(final MissionProperties mp, final int scid) {
        super(mp, scid);
    }
	
	/**
     * Creates an empty SessionIdentification with the given context key object
     * and spacecraft ID. 
     * @param mp the current mission properties object
	 * @param inKey ContextKey object to use as key
	 * @param scid spacecraft ID.
     */
    public SessionIdentification(final MissionProperties mp, final IContextKey inKey, final int scid) {
        super(mp, inKey, scid);
    }

	@Override
	public void setName(final String name) {
		this.name = name;

		if (this.name == null) {
			this.name = this.user + DEFAULT_SESSION_NAME_SUFFIX;
		}
		this.dirty = true;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.context.impl.ContextIdentification#generateStaxXml(javax.xml.stream.XMLStreamWriter)
	 */
	@Override
	public void generateStaxXml(final XMLStreamWriter writer)
			throws XMLStreamException {

		generateStaxXml(null, writer);

	}

	/**
	 * A custom version of the STAX XML generator that accepts a spacecraft ID.
	 * This method is needed to avoid changing the current session XML schema,
	 * which co-locates the spacecraft ID with the session identification. The
	 * spacecraft ID has been moved to the ISpacecraftFilterInformation object,
	 * e.g., is no longer here, but is still needed for backward-compatible XML.
	 * 
	 * @param scid
	 *            spacecraft ID
	 * @param writer
	 *            XML stream writer to write to
	 * @throws XMLStreamException
	 *             if there is an issue with XML generation
	 */
	public void generateStaxXml(final Integer scid, final XMLStreamWriter writer)
			throws XMLStreamException {

		writer.writeStartElement(SessionIdentificationParser.SESSION_ID_TAG);

		generateInternalStaxElements("BasicSessionInfo", writer);

		writer.writeEndElement(); // </SessionId>

	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.shared.template.Templatable#setTemplateContext(java.util.Map)
	 */
	@Override
	public void setTemplateContext(final Map<String, Object> map) {
		super.setTemplateContext(map);
		if (key.getNumber() != null) {
		    map.put("sessionId", key.getNumber());
			map.put("sessionNumber", key.getNumber()); // Deprecated for R8
			map.put("testNumber", key.getNumber()); //  Deprecated for R8
			map.put("sessionKey", key.getNumber()); //  Deprecated for R8
		}

		map.put("sessionFragment", key.getFragment());

		if (name != null) {
			map.put("sessionName", name); 
			map.put("testName", name); // Deprecated for R8
		} else {
			map.put("sessionName", "");
			map.put("testName", ""); //  Deprecated for R8
		}

		if (type != null) {
			map.put("sessionType", type);
		} else {
			map.put("sessionType", "");
		}

		if (description != null) {
			map.put("sessionDescription", description);
		} else {
			map.put("sessionDescription", "");
		}

		if (key.getHost() != null) {
			map.put("host", key.getHost());
			map.put("sessionHost", key.getHost());
		} else {
			map.put("host", "");
			map.put("sessionHost", "");
		}

		if (key.getHostId() != null) {
			map.put("hostId", key.getHostId());
		}

		if (user != null) {
			map.put("user", user);
		} else {
			map.put("user", "");
		}

		if (startTime != null) {
			map.put("startTime", getStartTimeStr());
		} else {
			map.put("startTime", "");
		}

		if (getEndTimeStr() != null) {
			map.put("endTime", getEndTimeStr());
		}
		// No else. Do not include endTime if it has no value

	}

}
