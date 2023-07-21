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

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import jpl.gds.common.config.connection.ConnectionFactory;
import jpl.gds.common.config.connection.ConnectionKey;
import jpl.gds.common.config.connection.IDatabaseConnectionSupport;
import jpl.gds.common.config.connection.IDownlinkConnection;
import jpl.gds.common.config.connection.IFileConnectionSupport;
import jpl.gds.common.config.connection.INetworkConnection;
import jpl.gds.common.config.connection.IUplinkConnection;
import jpl.gds.common.config.types.DatabaseConnectionKey;
import jpl.gds.common.config.types.DownlinkStreamType;
import jpl.gds.common.config.types.TelemetryConnectionType;
import jpl.gds.common.config.types.TelemetryInputType;
import jpl.gds.common.config.types.UplinkConnectionType;
import jpl.gds.common.config.types.VenueType;
import jpl.gds.context.api.ContextTopicNameFactory;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;

/**
 * This class is responsible for parsing an XML session configuration.
 * 
 *
 */
public class SessionConfigurationParser extends DefaultHandler {

	private static Tracer log = TraceManager.getDefaultTracer();

	
	/**
	 * Session XML element name (tag) 
	 */
	public static final String SESSION_TAG = "Session";
	
	/**
	 * Buffer used to capture parsed text
	 */
	private StringBuilder text = new StringBuilder(512);
	
	private boolean inSessionId;
	private boolean inVenueInformation;
	private boolean inHostInformation;
	private final SessionConfiguration sessionConfig;
	private final DictionaryProperties dictConfig;
	private final SessionIdentificationParser sessionIdParser;
	private TelemetryInputType inputType;
	
	/**
	 * Constructs a SessionConfigurationParser that will parse XML and store it in
	 * the specified target configuration object.
	 * 
	 * @param target the SessionConfiguration object to store parsed data in
	 */
	public SessionConfigurationParser(final SessionConfiguration target) {
		if (target == null) {
			throw new IllegalArgumentException("Target session configuration may not be null");
		}
		this.sessionConfig = target;
		this.dictConfig = this.sessionConfig.getDictionaryConfig();
		this.sessionIdParser = new SessionIdentificationParser(target.getContextId());
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
	 */
	@Override
	public void startElement(final String uri, final String localName,
			final String qname, final Attributes attr) throws SAXException {
		text = new StringBuilder(512);
  
    	// It is important to understand this bit of magic. Every Session
    	// contains the SessionId XML element. We want only one set of code for
    	// parsing that. So we must create a SessionIdentificationParser here
    	// and invoke it. But the SAX event handling will not invoke it automatically
    	// since each SAX parser has only one event handler.  So we invoke
    	// the start/endElement methods in the SessionIdentificationParser
    	// directly and supply it the SessionIdentificaiton object we want
    	// it to populate. We start this process when we see the SessionId 
    	// XML tag and continue until we exit that tag.	
		if (qname.equals(SESSION_TAG)) {
			final String versionStr = attr.getValue("version");
			int version = 0;
			if (versionStr != null) {
				version = Integer.valueOf(versionStr);
			}
			if (version != SessionConfiguration.VERSION) {
				throw new SAXException("Version in session configuration XML is " + version + 
						" but this release only supports version " + SessionConfiguration.VERSION);
			}
		} else if (qname.equals(SessionIdentificationParser.SESSION_ID_TAG)) {
			this.inSessionId = true;
			this.sessionIdParser.startElement(uri, localName, qname, attr);
		} else if (qname.equals("VenueInformation")) {
			inVenueInformation = true;
		} else if (qname.equals("HostInformation")) {
			inHostInformation = true;
		} else if (inSessionId) {
			this.sessionIdParser.startElement(uri, localName, qname, attr);
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
	 */
	@Override
	public void characters(final char[] chars, final int start,
			final int length) {
		final String newText = new String(chars, start, length);
		if (!newText.equals("\n")) {
			text.append(newText);
		}
	}

	/**
	 * {@inheritDoc}
	 * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public void endElement(final String uri, final String localName,
			final String qname) throws SAXException {

		endElement(uri, localName, qname, text);
	}
	
	/**
	 * Wrapper function for the standard SAX endElement event handler that
	 * allows a buffer containing the text parsed from the most recent element
	 * to be passed in.
	 * 
	 * @param uri
	 *            the Namespace URI, or the empty string if the element has no
	 *            Namespace URI or if Namespace processing is not being
	 *            performed
	 * @param localName
	 *            the local name (without prefix), or the empty string if
	 *            Namespace processing is not being performed
	 * @param qname
	 *            the qualified XML name (with prefix), or the empty string if
	 *            qualified names are not available
	 * @param buffer
	 *            StringBuilder that contains the text content of the element
	 * @throws SAXException
	 *             if there is a parsing error
	 */
	public void endElement(final String uri, final String localName,
			final String qname, final StringBuilder buffer) throws SAXException {

		final String textStr = buffer.toString();
		
       	// It is important to understand this bit of magic. Every Session
       	// contains the SessionId XML element. We want only one set of code for
       	// parsing that. So we must create a SessionIdentificationParser here
       	// and invoke it. But the SAX event handling will not invoke it automatically
       	// since each SAX parser has only one event handler.  So we invoke
       	// the start/endElement methods in the SessionIdentificationParser
       	// directly and supply it the SessionIdentificaiton object we want
       	// it to populate. We start this process when we see the SessionId 
       	// XML tag and continue until we exit that tag. There is one other catch 
    	// on this endElement call. Note is is not the standard SAX endElement
    	// signature. The buffer into which SAX events have been assembling text 
    	// for the current element is in THIS class, not in the SessionIdentification
    	// parser, so we must pass this string buffer along to endElement.
		if (qname.equals(SessionIdentificationParser.SESSION_ID_TAG)) {
			inSessionId = false;
			this.sessionIdParser.endElement(uri, localName, qname, buffer);
			this.sessionConfig.getContextId().copyValuesFrom(this.sessionIdParser.getTargetSessionId());
		} else if (qname.equals("SpacecraftId")) {
			try {
				sessionConfig.getContextId().setSpacecraftId(Integer.parseInt(textStr));
			} catch (final NumberFormatException nfe) {
				throw new SAXException("Spacecraft Id " + text
						+ " is not a valid Integer value", nfe);
			}
		} else if (inSessionId) {
			this.sessionIdParser.endElement(uri, localName, qname, buffer);
		} else if (qname.equals("VenueInformation")) {
			inVenueInformation = false;
		} else if (qname.equals("HostInformation")) {
			inHostInformation = false;
		} else if (inVenueInformation) {
			parseElementFromVenueSection(qname, textStr);			
		} else if (inHostInformation) {
			parseNetworkElement(qname, textStr);			
		} else if (qname.equals("FswVersion")) {
			dictConfig.setFswVersion(textStr);
		} else if (qname.equals("SseVersion")) {
			dictConfig.setSseVersion(textStr);
		} else if (qname.equals("RunFswDownlink")) {
			try {
				sessionConfig.getRunFsw().setFswDownlinkEnabled(GDR.parse_boolean(textStr));
			} catch (final NumberFormatException nfe) {
				throw new SAXException("RunFswDownlink value " + textStr
						+ " is not a valid boolean value", nfe);
			}
		} else if (qname.equals("RunSseDownlink")) {
			try {
				sessionConfig.getRunSse().setSseDownlinkEnabled(GDR.parse_boolean(textStr));
			} catch (final NumberFormatException nfe) {
				throw new SAXException("RunSseDownlink value " + textStr
						+ " is not a valid boolean value", nfe);
			}
		} else if (qname.equals("FswDictionaryDirectory")) {
			dictConfig.setFswDictionaryDir(textStr);
		} else if (qname.equals("SseDictionaryDirectory")) {
			dictConfig.setSseDictionaryDir(textStr);
		} else if (qname.equals("OutputDirectory")) {
			sessionConfig.getGeneralInfo().setOutputDir(textStr);
		} else if (qname.equals("OutputDirOverridden")) {
			try {
				sessionConfig.getGeneralInfo().setOutputDirOverridden(GDR.parse_boolean(textStr));
			} catch (final NumberFormatException nfe) {
				throw new SAXException("OutputDirOverridden " + textStr
						+ " is not a valid boolean value", nfe);
			}
		} else if (qname.equals("Session")) {
		    final String defTopic = ContextTopicNameFactory.getVenueTopic(sessionConfig.getVenueConfiguration().getVenueType(),
		            ContextTopicNameFactory.getVenueName(sessionConfig.getVenueConfiguration().getVenueType(), 
		                    sessionConfig.getVenueConfiguration().getTestbedName(), 
		                    sessionConfig.getContextId().getHost() + "." + 
		                            sessionConfig.getContextId().getUser()),
	                sessionConfig.getVenueConfiguration().getDownlinkStreamId(), 
                                                                          sessionConfig.getGeneralInfo().getSubtopic(),
                                                                          sessionConfig.getGeneralInfo().getSseContextFlag());
		    if (!defTopic.equals(sessionConfig.getGeneralInfo().getRootPublicationTopic())) {
		        sessionConfig.getGeneralInfo().setTopicIsOverridden(true);
		    }
		}

	}
	
	 /**
     * {@inheritDoc}
     * @see org.xml.sax.helpers.DefaultHandler#error(
     *      org.xml.sax.SAXParseException)
     */
    @Override
    public void error(final SAXParseException e) throws SAXException {
        log.error("Line: " + e.getLineNumber() + " Col: "
                + e.getColumnNumber() + ": " + e.toString());
        throw new SAXException(e);
    }

    /**
     * {@inheritDoc}
     * @see org.xml.sax.helpers.DefaultHandler#fatalError(
     *      org.xml.sax.SAXParseException)
     */
    @Override
    public void fatalError(final SAXParseException e) throws SAXException {
        log.error("Line: " + e.getLineNumber() + " Col: "
                + e.getColumnNumber() + ": " + e.toString());
        throw new SAXException(e);
    }

    /**
     * {@inheritDoc}
     * @see org.xml.sax.helpers.DefaultHandler#warning(
     *      org.xml.sax.SAXParseException)
     */
    @Override
    public void warning(final SAXParseException e) throws SAXException {
        log.warn("Line: " + e.getLineNumber() + " Col: "
                + e.getColumnNumber() + ": " + e.toString());
    }
	
	private void parseElementFromVenueSection(final String elementName, final String text)
			throws SAXException {
		if (elementName.equals("VenueType")) {
			sessionConfig.getVenueConfiguration().setVenueType(VenueType.valueOf(text));
		} else if (elementName.equals("InputFormat")) {
			try {
				this.inputType = TelemetryInputType.valueOf(text);
			} catch (final IllegalArgumentException e) {
				throw new SAXException("Input Format " + text
						+ " is not a valid InputFormat value", e);
			}
		} else if (elementName.equals("DownlinkConnectionType")) {
			try {
			    if (!text.equals(TelemetryConnectionType.UNKNOWN.name())) {
			        final IDownlinkConnection connect = ConnectionFactory.createDownlinkConfiguration(TelemetryConnectionType.valueOf(text));
			        /*  Need to check the flag to see if this is an sse_chill_down instance */
                    sessionConfig.getConnectionConfiguration()
                                 .setConnection(sessionConfig.getSseContextFlag()
                                         ? 
			                ConnectionKey.SSE_DOWNLINK : ConnectionKey.FSW_DOWNLINK, connect);
			        connect.setInputType(this.inputType);
			    }
			} catch (final IllegalArgumentException e) {
				throw new SAXException("Downlink Connection Type " + text
						+ " is not a valid DownlinkConnectionType value", e);
			}
		} else if (elementName.equals("UplinkConnectionType")) {
			try {
			    if (!text.equals(UplinkConnectionType.UNKNOWN.name())) {
			        sessionConfig.getConnectionConfiguration().createFswUplinkConnection(UplinkConnectionType.valueOf(text));
			    }
			} catch (final IllegalArgumentException e) {
				throw new SAXException("Uplink Connection Type " + text
						+ " is not a valid UplinkConnectionType value", e);
			}
		} else if (elementName.equals("TestbedName")) {
			sessionConfig.getVenueConfiguration().setTestbedName(text);
		} else if (elementName.equals("DownlinkStreamId")) {
			sessionConfig.getVenueConfiguration().setDownlinkStreamId(DownlinkStreamType.convert(text));
		} else if (elementName.equals("InputFile")) {
		      /*  Get SSE or FSW downlink connection */
			final IDownlinkConnection connect = getDownlinkConnection();
			if (connect instanceof IFileConnectionSupport) {
				((IFileConnectionSupport)connect).setFile(text);
			}
		} else if (elementName.equals("SessionVcid")) {
			try {
				sessionConfig.getFilterInformation().setVcid(Integer.parseInt(text));
			} catch (final NumberFormatException nfe) {
				throw new SAXException("Session VCID " + text
						+ " is not a valid Integer value", nfe);
			}
		} else if (elementName.equals("SessionDssId")) {
			try {
				sessionConfig.getFilterInformation().setDssId(Integer.parseInt(text));
			} catch (final NumberFormatException nfe) {
				throw new SAXException("Session DSS ID " + text
						+ " is not a valid Integer value", nfe);
			}
		} else if (elementName.equals("DatabaseSessionKey")) {
		     /* Get SSE or FSW downlink connection */
			final IDownlinkConnection connect = getDownlinkConnection();
			if (connect instanceof IDatabaseConnectionSupport) {
				final DatabaseConnectionKey databaseSessionInfo = ((IDatabaseConnectionSupport)connect).getDatabaseConnectionKey();
				databaseSessionInfo.addSessionKey(Long.parseLong(text));
			}
		} else if (elementName.equals("DatabaseSessionHost")) {
		    /* Get SSE or FSW downlink connection */
			final IDownlinkConnection connect = getDownlinkConnection();
			if (connect instanceof IDatabaseConnectionSupport) {
				final DatabaseConnectionKey databaseSessionInfo = ((IDatabaseConnectionSupport)connect).getDatabaseConnectionKey();
				databaseSessionInfo.addHostPattern(text);
			}
		} else if (elementName.equals("JmsSubtopic")) {
			sessionConfig.getGeneralInfo().setSubtopic(text);
		} else if (elementName.equalsIgnoreCase("Topic")) {
			sessionConfig.getGeneralInfo().setRootPublicationTopic(text);
		} 
	}
	
	private void parseNetworkElement(final String name, final String text) {
		
		if (name.equalsIgnoreCase("FswDownlinkHost")) {
			final IDownlinkConnection connect = sessionConfig
					.getConnectionConfiguration().getFswDownlinkConnection();
			if (connect != null
					&& connect instanceof INetworkConnection) {
				((INetworkConnection) connect)
						.setHost(text.trim());
			}

		} else if (name.equalsIgnoreCase("FswUplinkHost")) {
			final IUplinkConnection uconnect = sessionConfig
					.getConnectionConfiguration().getFswUplinkConnection();
			if (uconnect != null) {
				uconnect.setHost(text.trim());
			}

		} else if (name.equalsIgnoreCase("SseHost")) {
			final IDownlinkConnection connect = sessionConfig
					.getConnectionConfiguration().getSseDownlinkConnection();
			if (connect != null
					&& connect instanceof INetworkConnection) {
				((INetworkConnection) connect)
						.setHost(text.trim());
			}
			final IUplinkConnection uconnect = sessionConfig
					.getConnectionConfiguration().getSseUplinkConnection();
			if (uconnect != null) {
				uconnect.setHost(text.trim());
			}

		} else if (name.equalsIgnoreCase("FswUplinkPort")) {
			final IUplinkConnection uconnect =sessionConfig
					.getConnectionConfiguration().getFswUplinkConnection();
			if (uconnect != null) {
				uconnect.setPort(Integer.parseInt(text));
			}

		} else if (name.equalsIgnoreCase("SseUplinkPort")) {
			final IUplinkConnection uconnect = sessionConfig
					.getConnectionConfiguration().getSseUplinkConnection();
			if (uconnect != null) {
				uconnect.setPort(Integer.parseInt(text));
			}

		} else if (name.equalsIgnoreCase("SseDownlinkPort")) {
			final IDownlinkConnection connect = sessionConfig
					.getConnectionConfiguration().getSseDownlinkConnection();
			if (connect != null
					&& connect instanceof INetworkConnection) {
				((INetworkConnection) connect).setPort(Integer
						.parseInt(text));
			}

		} else if (name.equalsIgnoreCase("FswDownlinkPort")) {
			final IDownlinkConnection connect = sessionConfig
					.getConnectionConfiguration().getFswDownlinkConnection();
			if (connect != null
					&& connect instanceof INetworkConnection) {
				((INetworkConnection) connect).setPort(Integer
						.parseInt(text));
			}
		}
	}
	
	/**
	 * Gets FSW or SSE downlink connection based upon SSE flag.
	 * 
	 */
	private IDownlinkConnection getDownlinkConnection () {
        return sessionConfig.getSseContextFlag()
                ? sessionConfig.getConnectionConfiguration().getSseDownlinkConnection()
                : sessionConfig.getConnectionConfiguration().getFswDownlinkConnection();
	}
}
