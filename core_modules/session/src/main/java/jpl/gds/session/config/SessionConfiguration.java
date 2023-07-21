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

import java.io.File;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.SAXParser;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.connection.ConnectionKey;
import jpl.gds.common.config.connection.ConnectionProperties;
import jpl.gds.common.config.connection.IDatabaseConnectionSupport;
import jpl.gds.common.config.connection.IDownlinkConnection;
import jpl.gds.common.config.connection.IFileConnectionSupport;
import jpl.gds.common.config.connection.INetworkConnection;
import jpl.gds.common.config.connection.IUplinkConnection;
import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.common.config.types.DatabaseConnectionKey;
import jpl.gds.common.config.types.DownlinkStreamType;
import jpl.gds.common.config.types.TelemetryConnectionType;
import jpl.gds.common.config.types.TelemetryInputType;
import jpl.gds.common.config.types.UplinkConnectionType;
import jpl.gds.common.config.types.VenueType;
import jpl.gds.context.api.EnableFswDownlinkContextFlag;
import jpl.gds.context.api.EnableSseDownlinkContextFlag;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.context.impl.ContextConfiguration;
import jpl.gds.shared.config.ReleaseProperties;
import jpl.gds.shared.metadata.context.ContextConfigurationType;
import jpl.gds.shared.metadata.context.IContextKey;
import jpl.gds.shared.util.HostPortUtility;
import jpl.gds.shared.xml.XmlUtility;
import jpl.gds.shared.xml.parse.SAXParserPool;

/**
 * SessionConfiguration holds all the fields required to establish a chill
 * session configuration, which defines all of the input parameters to the
 * session, including venue-related parameters, hosts, ports, unique session
 * identification and user-supplied session attributes. As of AMPCS R8, the
 * SessionConfiguration is one implementation of an IContextConfiguration.
 * This class exists primarily for use at the application level by applications
 * that require the old-style session configuration, and to generate Velocity
 * template maps and XML that is compatible with that configuration.
 * 
 */
public class SessionConfiguration extends ContextConfiguration implements IContextConfiguration {
    /**
     * Version number of the session configuration. This tells MPCS
     * which version of the code wrote a config file.
     */
    public static final int VERSION = 3;

    private final EnableSseDownlinkContextFlag runSse;
    private final EnableFswDownlinkContextFlag runFsw;

	/**
	 * Constructor for use without application context. THIS SHOULD NOT BE USED
	 * UNLESS LOADING A COMPLETE CONFIGURATION FROM A FILE OR MESSAGE CONTEXT.
	 * The resulting object WILL NOT WORK for other situations.
	 * 
	 * @param missionProps
	 *            the current mission properties object
	 * @param connectProps
	 *            the current connection properties object
	 * @param initFromFileSystem
	 *            true if the file system should be scanned for dictionaries
	 *            (performance intensive), false if not
	 * 
	 */
	public SessionConfiguration(final MissionProperties missionProps,
			final ConnectionProperties connectProps, final boolean initFromFileSystem) {

	    super(missionProps, connectProps, initFromFileSystem);
	    
	    this.contextId = new SessionIdentification(missionProps);
	    
	    this.runFsw = new EnableFswDownlinkContextFlag();
        this.runSse = new EnableSseDownlinkContextFlag();
	    
		initVenue();

        if (!getSseContextFlag()
				&& !missionProps.missionHasSse()) {
			runSse.setSseDownlinkEnabled(false);
		}

        if (getSseContextFlag()) {
			runFsw.setFswDownlinkEnabled(false);
		}

		this.contextId.getContextKey().setType(ContextConfigurationType.SESSION);
	}

	/**
	 * Constructor that initializes the object from an ApplicationContext. Note
	 * that this method can be called ONCE on each unique application context. A
	 * second attempt on the same context will result in an
	 * IllegalStateException.
	 * 
	 * @param springContext
	 *            the ApplicationContext to get configuration defaults from
	 */
	public SessionConfiguration(final ApplicationContext springContext) {
	   this(springContext, true);
	}

	/**
	 * Constructor that initializes the object from an ApplicationContext.
	 * If the register parameter is false, it can be called any number of times
	 *
	 * @param springContext
	 *            the ApplicationContext to get configuration defaults from
	 * @param register Whether to register the bean as singleton
	 */
	public SessionConfiguration(final ApplicationContext springContext, boolean register) {
		super(springContext, new SessionIdentification(springContext.getBean(MissionProperties.class),
		                                               springContext.getBean(IContextKey.class),
		                                               springContext.getBean(MissionProperties.class).getDefaultScid()),register);

		this.runFsw = springContext.getBean(EnableFswDownlinkContextFlag.class);
		this.runSse = springContext.getBean(EnableSseDownlinkContextFlag.class);

		initVenue();

		if (!getSseContextFlag()
				&& !missionProps.missionHasSse()) {
			runSse.setSseDownlinkEnabled(false);
		}

		if (getSseContextFlag()) {
			runFsw.setFswDownlinkEnabled(false);
		}

		this.contextId.getContextKey().setType(ContextConfigurationType.SESSION);
	}

	private void initVenue() {
    	
    	this.venueConfig.setVenueType(missionProps.getDefaultVenueType());
    	
    	final VenueType vt = venueConfig.getVenueType();
        
    	if (vt != null && vt.hasTestbedName()) {
        	missionProps.getDefaultTestbedName(vt, this.contextId.getHost());
        }
        
    	if (connectConfig.getConnectionProperties().getDefaultDownlinkConnectionType(vt, false) != null)  {
    		connectConfig.createFswDownlinkConnection(connectConfig.getConnectionProperties().getDefaultDownlinkConnectionType(vt, false));
        }

    	if (missionProps.isUplinkEnabled() && connectConfig.getConnectionProperties().getDefaultUplinkConnectionType(vt, false) != null)  {
    		connectConfig.createFswUplinkConnection(connectConfig.getConnectionProperties().getDefaultUplinkConnectionType(vt, false));
        }
    }
    
    @Override
	public void copyValuesFrom(final IContextConfiguration toCopy) {

    	if (!(toCopy instanceof SessionConfiguration)) {
    		throw new IllegalArgumentException("argument to copy must be a SessionConfiguration");
    	}
    	
    	super.copyValuesFrom(toCopy);
    	
    	final SessionConfiguration tc = (SessionConfiguration)toCopy;
    	
        this.runFsw.setFswDownlinkEnabled(tc.getRunFsw().isFswDownlinkEnabled());
        this.runSse.setSseDownlinkEnabled(tc.getRunSse().isSseDownlinkEnabled());
    }

    /**
     * Returns the "run flight software downlink" flag, which indicates
     * whether a flight software downlink is run as part of the session.
     * 
     * @return true if FSW Downlink is included in the session, false if not
     */
    public EnableFswDownlinkContextFlag getRunFsw() {
        return runFsw;
    }


    /**
     * Returns the "run SSE downlink" flag, which indicates
     * whether an SSE downlink is run as part of the session.
     * 
     * @return true if SSE Downlink is included in the session, false if not
     */
    public EnableSseDownlinkContextFlag getRunSse() {
        return runSse;
    }

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.shared.xml.stax.StaxSerializable#generateStaxXml(javax.xml.stream.XMLStreamWriter)
	 */
	@Override
	public void generateStaxXml(final XMLStreamWriter writer)
			throws XMLStreamException {

		writer.writeStartElement("Sessions");
		writer.writeStartElement("Session");
        writer.writeAttribute("version", String.valueOf(VERSION));
		this.contextId.generateStaxXml(writer);

		XmlUtility.writeSimpleElement(writer, "AmpcsVersion",
				ReleaseProperties.getShortVersion());
		try {
			XmlUtility.writeSimpleElement(writer, "FullName",
					contextId.getFullName());
		} catch (final IllegalStateException e) {
			// Insufficient state to get full name at this time, Skip element.
		}

		/*
		 * Dictionary members replaced with
		 * DictionaryConfiguration object.
		 */
		XmlUtility.writeSimpleElement(writer, "FswVersion",
				dictConfig.getFswVersion());

		if (missionProps.missionHasSse()
				&& !venueConfig.getVenueType().isOpsVenue()) {
			XmlUtility.writeSimpleElement(writer, "SseVersion",
					dictConfig.getSseVersion());
		}
		XmlUtility.writeSimpleElement(writer, "RunFswDownlink", runFsw.isFswDownlinkEnabled());
		XmlUtility.writeSimpleElement(writer, "RunSseDownlink", runSse.isSseDownlinkEnabled()
				&& !venueConfig.getVenueType().isOpsVenue());

		final boolean missionHasUplink = missionProps.isUplinkEnabled();
		XmlUtility.writeSimpleElement(writer, "RunUplink", missionHasUplink
				&& connectConfig.get(ConnectionKey.FSW_UPLINK) != null);

		/*
		 * Begin: Write dictionary and directory elements as CDATAs.
		 */

		if (dictConfig.getFswDictionaryDir() != null) {
			XmlUtility.writeSimpleCDataElement(writer,
					"FswDictionaryDirectory",
					new File(dictConfig.getFswDictionaryDir())
							.getAbsolutePath());
		}

		if (dictConfig.getSseDictionaryDir() != null
				&& missionProps.missionHasSse()
				&& !venueConfig.getVenueType().isOpsVenue()) {
			XmlUtility.writeSimpleCDataElement(writer,
					"SseDictionaryDirectory",
					new File(dictConfig.getSseDictionaryDir())
							.getAbsolutePath());
		}

		try {
			final String outputDir = generalInfo.getOutputDir();
			XmlUtility.writeSimpleCDataElement(writer, "OutputDirectory",
					new File(outputDir).getAbsolutePath());
		} catch (final IllegalStateException e) {
			// Insufficient state to determine output dir at this time. Skip the
			// element.
		}
		/*
		 * End: Write dictionary and directory
		 * elements as CDATAs.
		 */

		XmlUtility.writeSimpleElement(writer, "OutputDirOverridden",
				this.generalInfo.isOutputDirOverridden());

		writer.writeStartElement("VenueInformation");

		XmlUtility.writeSimpleElement(writer, "VenueType",
				venueConfig.getVenueType());

		/*  Need to choose SSE or FSW connection here. */
        final IDownlinkConnection downConnect = getSseContextFlag()
                ? connectConfig.getSseDownlinkConnection()
                : connectConfig.getFswDownlinkConnection();
		if (downConnect != null) {

			XmlUtility.writeSimpleElement(writer, "InputFormat", downConnect
					.getInputType() != null ? downConnect.getInputType()
					: TelemetryInputType.UNKNOWN);

			XmlUtility.writeSimpleElement(writer, "DownlinkConnectionType",
					downConnect.getDownlinkConnectionType());
		} else {
			/* InputFormat is required by Schema BEFORE connection types.
			 * Set to unknown since downlink connection is null
			 */
		    XmlUtility.writeSimpleElement(writer, "InputFormat", TelemetryInputType.UNKNOWN);
		    XmlUtility.writeSimpleElement(writer, "DownlinkConnectionType",
                    TelemetryConnectionType.UNKNOWN); 
		}

		final IUplinkConnection upConnect = connectConfig
				.getFswUplinkConnection();

		if (upConnect != null) {
			XmlUtility.writeSimpleElement(writer, "UplinkConnectionType",
					upConnect.getUplinkConnectionType());
		} else {
		    XmlUtility.writeSimpleElement(writer, "UplinkConnectionType",
                    UplinkConnectionType.UNKNOWN);
		}

		if (venueConfig.getVenueType() != null
				&& venueConfig.getVenueType().hasTestbedName()) {
			XmlUtility.writeSimpleElement(writer, "TestbedName",
					venueConfig.getTestbedName());
		}

		if (venueConfig.getVenueType() != null
				&& venueConfig.getVenueType().hasStreams()
				&& downConnect != null) {

			DownlinkStreamType sdsie = venueConfig.getDownlinkStreamId();

			if (sdsie == null) {
				sdsie = DownlinkStreamType.NOT_APPLICABLE;
			}

			XmlUtility.writeSimpleElement(writer, "DownlinkStreamId",
					DownlinkStreamType.convert(sdsie));
		}

		XmlUtility.writeSimpleElement(writer, "SessionDssId",
				scFilterInfo.getDssId());

		XmlUtility.writeSimpleElement(writer, "SessionVcid",
				scFilterInfo.getVcid());

		if (downConnect instanceof IFileConnectionSupport) {
			XmlUtility.writeSimpleElement(writer, "InputFile",
					((IFileConnectionSupport) downConnect).getFile());
		}

		if (downConnect instanceof IDatabaseConnectionSupport) {
			final DatabaseConnectionKey databaseSessionInfo = ((IDatabaseConnectionSupport) downConnect)
					.getDatabaseConnectionKey();

			final List<Long> keys = databaseSessionInfo.getSessionKeyList();
			if (!keys.isEmpty()) {
				XmlUtility.writeSimpleElement(writer, "DatabaseSessionKey",
						keys.get(0));
			}

			final List<String> hosts = databaseSessionInfo.getHostPatternList();
			if (!hosts.isEmpty()) {
				XmlUtility.writeSimpleElement(writer, "DatabaseSessionHost",
						hosts.get(0));
			}
		}

		XmlUtility.writeSimpleElement(writer, "JmsSubtopic",
				this.generalInfo.getSubtopic());

		XmlUtility.writeSimpleElement(writer, "Topic",
				this.generalInfo.getRootPublicationTopic());

		writer.writeEndElement(); // </VenueInformation>

		writer.writeStartElement("HostInformation");

	    /*  Need to check the flag to see if there is a FSW downlink instance. */
		if (downConnect instanceof INetworkConnection && runFsw.isFswDownlinkEnabled()) {
			XmlUtility.writeSimpleElement(writer, "FswDownlinkHost",
					((INetworkConnection) downConnect).getHost());
		}

		if (upConnect != null) {
			XmlUtility.writeSimpleElement(writer, "FswUplinkHost",
					upConnect.getHost());
		}

		final IDownlinkConnection sseDownConnect = connectConfig
				.getSseDownlinkConnection();

	    /*  Need to check the flag to see if there is an SSE downlink instance. */
		if (sseDownConnect != null && !venueConfig.getVenueType().isOpsVenue() && runSse.isSseDownlinkEnabled()) {

			if (sseDownConnect instanceof INetworkConnection) {
				XmlUtility.writeSimpleElement(writer, "SseHost",
						((INetworkConnection) sseDownConnect).getHost());
			}
		}

		if (upConnect != null
				&& upConnect.getPort() != HostPortUtility.UNDEFINED_PORT) {
			XmlUtility.writeSimpleElement(writer, "FswUplinkPort",
					upConnect.getPort());

		}

		final IUplinkConnection sseUpConnect = connectConfig
				.getSseUplinkConnection();

		if (sseUpConnect != null && !venueConfig.getVenueType().isOpsVenue()
				&& sseUpConnect.getPort() != HostPortUtility.UNDEFINED_PORT) {
			XmlUtility.writeSimpleElement(writer, "SseUplinkPort",
					sseUpConnect.getPort());
		}

	    /*  Need to check the flag to see if there is a FSW downlink instance. */
		if (downConnect instanceof INetworkConnection && runFsw.isFswDownlinkEnabled()
				&& ((INetworkConnection) downConnect).getPort() != HostPortUtility.UNDEFINED_PORT) {
			XmlUtility.writeSimpleElement(writer, "FswDownlinkPort",
					((INetworkConnection) downConnect).getPort());
		}

		if (sseDownConnect != null && !venueConfig.getVenueType().isOpsVenue()) {

		    /* Need to check the flag to see if there is a SSE downlink instance. */
			if (sseDownConnect instanceof INetworkConnection && runSse.isSseDownlinkEnabled()
					&& ((INetworkConnection) sseDownConnect).getPort() != HostPortUtility.UNDEFINED_PORT) {
				XmlUtility.writeSimpleElement(writer, "SseDownlinkPort",
						((INetworkConnection) sseDownConnect).getPort());
			}
		}

		writer.writeEndElement(); // </HostInformation>
		writer.writeEndElement(); // </Session>
		writer.writeEndElement(); // <Sessions>
	}


    @Override
	public boolean load(final String filename) {
        SAXParser sp = null;
        try {
            final SessionConfigurationParser tcSax = new SessionConfigurationParser(this);
            sp = SAXParserPool.getInstance().getNonPooledParser();
            sp.parse(filename, tcSax);
            setConfigFile(filename);
            return true;
        } catch (final Exception e) {
            log.error("Could not parse Session Configuration file " + filename);
            log.error("Message was " + e.getMessage() == null ? e.toString()
                    : e.getMessage());
            return false;
        }
    }


	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.shared.template.Templatable#setTemplateContext(java.util.Map)
	 */
	@Override
	public void setTemplateContext(final Map<String, Object> map) {

	    super.setTemplateContext(map);
	    
        map.put("configVersion", VERSION);
        
		map.put("runFswDownlink", runFsw.isFswDownlinkEnabled());
		map.put("fswDownlinkFlag", runFsw.isFswDownlinkEnabled());
		map.put("runSseDownlink", runSse.isSseDownlinkEnabled());
		map.put("sseDownlinkFlag", runSse.isSseDownlinkEnabled());
		final boolean missionHasUplink = missionProps.isUplinkEnabled();

		IUplinkConnection upConnect = connectConfig.getFswUplinkConnection();
		IDownlinkConnection downConnect = connectConfig
				.getFswDownlinkConnection();

		map.put("uplinkFlag", missionHasUplink && upConnect != null);

		/*
		 * Remove references to DownlinkSpacecraftSide
		 * because it is not multimission.
		 */

		map.put("hostConfig", Boolean.TRUE);

		/**
		 * The super class has a different approach to manage connections. 
		 * Leave this code here so that the old template variables still work.
		 */
		if (downConnect != null) {
			map.put("inputFormat", downConnect.getInputType().toString());
			map.put("downlinkConnectionType",
					downConnect.getDownlinkConnectionType());
			if (downConnect instanceof IFileConnectionSupport) {
				map.put("inputFile",
						((IFileConnectionSupport) downConnect).getFile());
			}
			if (downConnect instanceof IDatabaseConnectionSupport) {
				final DatabaseConnectionKey databaseSessionInfo = ((IDatabaseConnectionSupport) downConnect)
						.getDatabaseConnectionKey();

				final List<Long> keys = databaseSessionInfo.getSessionKeyList();
				if (!keys.isEmpty()) {
					map.put("databaseSessionKey", keys.get(0));
				}

				final List<String> hosts = databaseSessionInfo
						.getHostPatternList();
				if (!hosts.isEmpty()) {
					map.put("databaseSessionHost", hosts.get(0));
				}
			}
			if (downConnect instanceof INetworkConnection) {
				map.put("fswDownlinkHost",
						((INetworkConnection) downConnect).getHost());
				map.put("fswDownlinkPort",
						((INetworkConnection) downConnect).getPort());
			}
		}

		if (upConnect != null) {
			map.put("uplinkConnectionType", upConnect.getUplinkConnectionType());
			map.put("fswUplinkHost", ((INetworkConnection) upConnect).getHost());
			map.put("fswUplinkPort", ((INetworkConnection) upConnect).getPort());
		}

		downConnect = connectConfig.getSseDownlinkConnection();
		if (downConnect != null && !venueConfig.getVenueType().isOpsVenue()) {

			if (downConnect instanceof INetworkConnection) {
				map.put("sseHost", ((INetworkConnection) downConnect).getHost());
				map.put("sseDownlinkPort",
						((INetworkConnection) downConnect).getPort());
			}
		}

		upConnect = connectConfig.getSseUplinkConnection();

		if (upConnect != null && !venueConfig.getVenueType().isOpsVenue()) {
			map.put("sseHost", ((INetworkConnection) upConnect).getHost());
			map.put("sseUplinkPort", ((INetworkConnection) upConnect).getPort());
		}

		/*
		 * R8 Refactoring - Leave this in for backward compatibility, but
		 * set to unknown
		 */
		map.put("gdsHost", "unknown");

		if (scFilterInfo.getVcid() != null) {
			map.put("sessionVcid", scFilterInfo.getVcid());
		}


		if (scFilterInfo.getDssId() != null) {
			map.put("sessionDssId", scFilterInfo.getDssId());
		}

	}

    /**
     * Indicates if this session configuration is for uplink only.
     * 
     * @return true if this session is for uplink only; false if it's integrated
     * or for downlink
     */
    public boolean isUplinkOnly() {
        /* check both FSW and SSE connections */
        return connectConfig.get(ConnectionKey.FSW_DOWNLINK) == null && connectConfig.get(ConnectionKey.SSE_DOWNLINK) == null;
    }

    /**
     * Indicates if this session configuration is for downlink only.
     * 
     * @return true if this session is for downlink only; false if it's integrated
     * or for downlink
     */
    public boolean isDownlinkOnly() {
        /* check both FSW and SSE connections */
        return connectConfig.get(ConnectionKey.FSW_UPLINK) == null && connectConfig.get(ConnectionKey.SSE_UPLINK) == null;
    }

    /**
     * Indicates if this session configuration is for SSE downlink only.
     * 
     * @return true if this session is for SSE downlink only; false if it's integrated
     * or for FSW downlink
     */
    public boolean isSseDownlinkOnly() {
    	return connectConfig.get(ConnectionKey.FSW_DOWNLINK) == null &&
    		   connectConfig.get(ConnectionKey.FSW_UPLINK) == null;
    }

    /**
     * Indicates if this session configuration is for SSE downlink only.
     * 
     * @return true if this session is for FSW downlink only; false if it's integrated
     * or for SSE downlink
     */
    public boolean isFswDownlinkOnly() {
    	return connectConfig.get(ConnectionKey.SSE_DOWNLINK) == null &&
     		   connectConfig.get(ConnectionKey.FSW_UPLINK) == null;
    }    

}
