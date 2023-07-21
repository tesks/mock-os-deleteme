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
package jpl.gds.context.impl.message.parser;

import jpl.gds.common.config.TimeComparisonStrategy;
import jpl.gds.common.config.connection.ConnectionFactory;
import jpl.gds.common.config.connection.ConnectionKey;
import jpl.gds.common.config.connection.IConnectionMap;
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
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.context.api.ISimpleContextConfiguration;
import jpl.gds.context.api.TimeComparisonStrategyContextFlag;
import jpl.gds.context.impl.ContextConfiguration;
import jpl.gds.context.impl.ProcessServerContextConfiguration;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.types.UnsignedInteger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This class is responsible for parsing an XML Context Configuration
 *
 */
public class ContextConfigurationParser extends DefaultHandler {

    /**
     * Context XML element name (tag)
     */
    public static final String CONTEXT_TAG = "Context";

    private final ISimpleContextConfiguration contextConfig;

    private final Tracer log;

    private StringBuilder text = new StringBuilder(512);

    private boolean inContextId;
    private boolean inVenueInformation;
    private boolean inHostInformation;
    private boolean inConnectionInformation;
    private boolean inGeneralInformation;

    private final ContextIdentificationParser contextIdParser;
    private TelemetryInputType inputType;
    private int restPort;
    private CONNECT_STATE state;

    /**
     * Constructs a ContextonfigurationParser that will parse XML and store it in the specified target configuration
     * object.
     *
     * @param target the IContextConfiguration object to store parsed data in
     */
    public ContextConfigurationParser(final ISimpleContextConfiguration target) {
        if (target == null) {
            throw new IllegalArgumentException("Target context configuration may not be null");
        }
        this.contextConfig = target;
        this.log = TraceManager.getDefaultTracer();
        this.contextIdParser = new ContextIdentificationParser(target.getContextId());
        this.restPort = -1;
    }

    @Override
    public void startElement(final String uri, final String localName, final String qname, final Attributes attr)
            throws SAXException {

        text = new StringBuilder(512);

        if (inConnectionInformation) {
            parseConnectionElement(uri, localName, qname, attr);
        } else if (inContextId) {
            this.contextIdParser.startElement(uri, localName, qname, attr);
        } else {
            switch (qname) {
                case CONTEXT_TAG: {
                    final String versionStr = attr.getValue("version");
                    int version = 0;
                    if (versionStr != null) {
                        version = Integer.valueOf(versionStr);
                    }
                    if (version != ContextConfiguration.OBJECT_VERSION) {
                        throw new SAXException("Version in session configuration XML is " + version
                                + " but this release only supports version " + ContextConfiguration.OBJECT_VERSION);
                    }
                    contextIdParser.startElement(uri, localName, qname, attr);

                    if (attr.getValue("restPort") != null) {
                        restPort = Integer.valueOf(attr.getValue("restPort"));
                    }
                    break;
                }
                case "context": {
                    // TODO: Should we do anything with this?
                    // <context id="0/LMC-061398/0/1" />
                    break;
                }
                case "keyword_value": {
                    // TODO: parse metadata keyword value pairs
                    break;
                }
                case ContextIdentificationParser.CONTEXT_ID_TAG: {
                    this.inContextId = true;
                    this.contextIdParser.startElement(uri, localName, qname, attr);
                    break;
                }
                case "VenueInformation": {
                    inVenueInformation = true;
                    break;
                }
                case "HostInformation": {
                    inHostInformation = true;
                    break;
                }
                case "Connections": {
                    inConnectionInformation = true;
                    break;
                }
                case "GeneralInformation": {
                    inGeneralInformation = true;
                    break;
                }
                default: {
                    break;
                }
            }
        }
    }

    @Override
    public void endElement(final String uri, final String localName, final String qname) throws SAXException {

        endElement(uri, localName, qname, text);
    }

    /**
     * Wrapper function for the standard SAX endElement event handler that allows a buffer containing the text parsed
     * from the most recent element to be passed in.
     *
     * @param uri       the Namespace URI, or the empty string if the element has no Namespace URI or if Namespace
     *                  processing is not being performed
     * @param localName the local name (without prefix), or the empty string if Namespace processing is not being
     *                  performed
     * @param qname     the qualified XML name (with prefix), or the empty string if qualified names are not available
     * @param buffer    StringBuilder that contains the text content of the element
     * @throws SAXException if there is a parsing error
     */
    public void endElement(final String uri, final String localName, final String qname, final StringBuilder buffer)
            throws SAXException {

        final String textStr = buffer.toString();

        final boolean isIContextConfiguration = contextConfig instanceof IContextConfiguration;

        if (inVenueInformation && !localName.equals("VenueInformation")) {
            parseElementFromVenueSection(qname, textStr);
        } else if (inHostInformation && !localName.equals("HostInformation")) {
            parseNetworkElement(qname, textStr);
        } else if (inContextId && !localName.equals(ContextIdentificationParser.CONTEXT_ID_TAG)) {
            this.contextIdParser.endElement(uri, localName, qname, buffer);
        } else if (inGeneralInformation && !localName.equals("GeneralInformation")) {
            parseGeneralInformation(uri, localName, qname, textStr);
        } else {
            switch (qname) {
                case ContextIdentificationParser.CONTEXT_ID_TAG: {
                    inContextId = false;
                    this.contextIdParser.endElement(uri, localName, qname, buffer);

                    this.contextConfig.getContextId().copyValuesFrom(this.contextIdParser.getTargetContextId());

                    if (restPort > 0) {
                        this.contextConfig.setRestPort(UnsignedInteger.valueOf(restPort));
                    }
                    break;
                }
                case "SpacecraftId": {
                    try {
                        contextConfig.getContextId().setSpacecraftId(Integer.parseInt(textStr));
                    } catch (final NumberFormatException nfe) {
                        throw new SAXException("Spacecraft Id " + text + " is not a valid Integer value", nfe);
                    }
                    break;
                }
                case "context": {
                    // TODO: no-op?
                    // <context id="0/LMC-061398/0/1" />
                    // also, this currently differs from the parsed user/host etc,
                    // which seems off since that is what a conext id consists of
                    break;
                }
                case "keyword_value": {
                    // TODO: parse metadata into the contextConfig?
                    break;
                }
                case "VenueInformation": {
                    inVenueInformation = false;
                    break;
                }
                case "HostInformation": {
                    inHostInformation = false;
                    break;
                }
                case "Connections": {
                    inConnectionInformation = false;
                    this.state = CONNECT_STATE.NONE;
                    break;
                }
                case "Vcid": {
                    try {
                        contextConfig.getFilterInformation().setVcid(Integer.parseInt(textStr));
                    } catch (final NumberFormatException nfe) {
                        throw new SAXException("Session VCID " + text + " is not a valid Integer value", nfe);
                    }
                    break;
                }
                case "DssId": {
                    try {
                        contextConfig.getFilterInformation().setDssId(Integer.parseInt(textStr));
                    } catch (final NumberFormatException nfe) {
                        throw new SAXException("Session DSS ID " + text + " is not a valid Integer value", nfe);
                    }
                    break;
                }
                case "FswVersion": {
                    if (isIContextConfiguration) {
                        ((IContextConfiguration) contextConfig).getDictionaryConfig().setFswVersion(textStr);
                    }
                    break;
                }
                case "SseVersion": {
                    if (isIContextConfiguration) {
                        ((IContextConfiguration) contextConfig).getDictionaryConfig().setSseVersion(textStr);
                    }
                    break;
                }
                case "FswDictionaryDirectory": {
                    if (isIContextConfiguration) {
                        ((IContextConfiguration) contextConfig).getDictionaryConfig().setFswDictionaryDir(textStr);
                    }
                    break;
                }
                case "SseDictionaryDirectory": {
                    if (isIContextConfiguration) {
                        ((IContextConfiguration) contextConfig).getDictionaryConfig().setSseDictionaryDir(textStr);
                    }
                    break;
                }
                case "Topic": {
                    contextConfig.getGeneralInfo().setRootPublicationTopic(textStr);
                    break;
                }
                case "Subtopic": {
                    contextConfig.getGeneralInfo().setSubtopic(textStr);
                    break;
                }
                case "OutputDirectory": {
                    contextConfig.getGeneralInfo().setOutputDir(textStr);
                    break;
                }
                case "OutputDirOverridden": {
                    try {
                        contextConfig.getGeneralInfo().setOutputDirOverridden(GDR.parse_boolean(textStr));
                    } catch (final NumberFormatException nfe) {
                        throw new SAXException("OutputDirOverridden " + textStr
                                + " is not a valid boolean value", nfe);
                    }
                    break;
                }
                case CONTEXT_TAG: {
                    if (isIContextConfiguration) {
                        IContextConfiguration contextConfiguration = (IContextConfiguration) contextConfig;
                        final String defTopic = ContextTopicNameFactory
                                .getVenueTopic(contextConfiguration.getVenueConfiguration().getVenueType(),
                                        ContextTopicNameFactory
                                                .getVenueName(contextConfiguration.getVenueConfiguration().getVenueType(),
                                                        contextConfiguration.getVenueConfiguration().getTestbedName(),
                                                        contextConfiguration.getContextId().getHost()
                                                                + "." + contextConfiguration.getContextId().getUser()),
                                        contextConfiguration.getVenueConfiguration().getDownlinkStreamId(),
                                        contextConfiguration.getGeneralInfo().getSubtopic(),
                                        contextConfiguration.getGeneralInfo().getSseContextFlag());
                        if (!defTopic.equals(contextConfiguration.getGeneralInfo().getRootPublicationTopic())) {
                            contextConfiguration.getGeneralInfo().setTopicIsOverridden(true);
                        }
                    }
                    break;
                }
                case "GeneralInformation": {
                    inGeneralInformation = false;
                    break;
                }
                case "TimeComparisonStrategy": {
                    if (contextConfig instanceof ProcessServerContextConfiguration) {
                        ((ProcessServerContextConfiguration) contextConfig)
                                .setTimeComparisonStrategyContextFlag(new TimeComparisonStrategyContextFlag(
                                        TimeComparisonStrategy.valueOf(textStr)));
                    }
                    break;
                }
                default:
                    break;
            }
        }

    }

    private void parseGeneralInformation(final String uri, final String localName, final String qname, final String text) throws
                                                                                                                                   SAXException {
        switch (qname) {
            case "Topic": {
                contextConfig.getGeneralInfo().setRootPublicationTopic(text);
                break;
            }
            case "Subtopic": {
                contextConfig.getGeneralInfo().setSubtopic(text);
                break;
            }
            case "OutputDirectory": {
                contextConfig.getGeneralInfo().setOutputDir(text);
                break;
            }
            case "OutputDirOverridden": {
                if (contextConfig.getGeneralInfo().getOutputDir() == null) {
                    try {
                        contextConfig.getGeneralInfo().setOutputDirOverridden(GDR.parse_boolean(text));
                    } catch (final NumberFormatException nfe) {
                        throw new SAXException("OutputDirOverridden " + text
                                + " is not a valid boolean value", nfe);
                    }
                }
                break;
            }
        }
    }

    private void parseConnectionElement(final String elementName, final String text, final String qname,
                                        final Attributes attr)
            throws SAXException {

        switch (text) {
            case "fsw_downlink_connection": {
                state = CONNECT_STATE.FSW_DOWN;
                break;
            }
            case "sse_downlink_connection": {
                state = CONNECT_STATE.SSE_DOWN;
                break;
            }
            case "fsw_uplink_connection": {
                state = CONNECT_STATE.FSW_UP;
                break;
            }
            case "sse_uplink_connection": {
                state = CONNECT_STATE.SSE_UP;
                break;
            }
            default:
                break;
        }

        if (state == CONNECT_STATE.FSW_DOWN || state == CONNECT_STATE.SSE_DOWN) {
            if (attr.getValue("host") != null) {
                parseNetworkElement((state == CONNECT_STATE.SSE_DOWN ? "SseDownlinkHost" : "FswDownlinkHost"),
                        attr.getValue("host"));
            }
            if (attr.getValue("port") != null) {
                parseNetworkElement((state == CONNECT_STATE.SSE_DOWN ? "SseDownlinkPort" : "FswDownlinkPort"),
                        attr.getValue("port"));
            }
            if (attr.getValue("InputFormat") != null) {
                try {
                    this.inputType = TelemetryInputType.valueOf(attr.getValue("InputFormat"));
                    if (state == CONNECT_STATE.SSE_DOWN) {
                        ((IContextConfiguration) contextConfig).getConnectionConfiguration().getSseDownlinkConnection()
                                .setInputType(this.inputType);
                    } else if (state == CONNECT_STATE.FSW_DOWN) {
                        ((IContextConfiguration) contextConfig).getConnectionConfiguration().getFswDownlinkConnection()
                                .setInputType(this.inputType);
                    }
                } catch (final IllegalArgumentException e) {
                    throw new SAXException("Input Format " + attr.getValue("InputFormat")
                            + " is not a valid InputFormat value", e);
                }
            }

        } else if (state == CONNECT_STATE.FSW_UP || state == CONNECT_STATE.SSE_UP) {
            if (attr.getValue("host") != null) {
                parseNetworkElement((state == CONNECT_STATE.SSE_UP ? "SseUplinkHost" : "FswUplinkHost"),
                        attr.getValue("host"));
            }
            if (attr.getValue("port") != null) {
                parseNetworkElement((state == CONNECT_STATE.SSE_UP ? "SseUplinkPort" : "FswUplinkPort"),
                        attr.getValue("port"));
            }
        }

    }

    private void parseElementFromVenueSection(final String elementName, final String text) throws SAXException {
        IContextConfiguration contextConfiguration = (IContextConfiguration) contextConfig;
        if (elementName.equals("VenueType")) {
            contextConfiguration.getVenueConfiguration().setVenueType(VenueType.valueOf(text));
        } else if (elementName.equals("InputFormat")) {
            try {
                this.inputType = TelemetryInputType.valueOf(text);
                contextConfiguration.getConnectionConfiguration().getDownlinkConnection().setInputType(this.inputType);
            } catch (final IllegalArgumentException e) {
                throw new SAXException("Input Format " + text + " is not a valid InputFormat value", e);
            }
        } else if (elementName.equals("DownlinkConnectionType")) {
            try {
                if (!text.equals(TelemetryConnectionType.UNKNOWN.name())) {
                    final IDownlinkConnection connect = ConnectionFactory
                            .createDownlinkConfiguration(TelemetryConnectionType.valueOf(text));
                    /* Need to check the flag to see if this is an sse_chill_down instance */
                    contextConfiguration.getConnectionConfiguration()
                            .setConnection(contextConfiguration.getSseContextFlag()
                                    ? ConnectionKey.SSE_DOWNLINK
                                    : ConnectionKey.FSW_DOWNLINK, connect);
                    connect.setInputType(this.inputType);
                }
            } catch (final IllegalArgumentException e) {
                throw new SAXException("Downlink Connection Type " + text
                        + " is not a valid DownlinkConnectionType value", e);
            }
        } else if (elementName.equals("UplinkConnectionType")) {
            try {
                if (!text.equals(UplinkConnectionType.UNKNOWN.name())) {
                    contextConfiguration.getConnectionConfiguration()
                            .createFswUplinkConnection(UplinkConnectionType.valueOf(text));
                }
            } catch (final IllegalArgumentException e) {
                throw new SAXException("Uplink Connection Type " + text + " is not a valid UplinkConnectionType value",
                        e);
            }
        } else if (elementName.equals("TestbedName")) {
            contextConfiguration.getVenueConfiguration().setTestbedName(text);
        } else if (elementName.equals("DownlinkStreamId")) {
            contextConfiguration.getVenueConfiguration().setDownlinkStreamId(DownlinkStreamType.convert(text));
        } else if (elementName.equals("InputFile")) {
            /* Get SSE or FSW downlink connection */
            final IDownlinkConnection connect = contextConfiguration.getConnectionConfiguration().getDownlinkConnection();
            if (connect instanceof IFileConnectionSupport) {
                ((IFileConnectionSupport) connect).setFile(text);
            }
        } else if (elementName.equals("SessionVcid")) {
            try {
                contextConfiguration.getFilterInformation().setVcid(Integer.parseInt(text));
            } catch (final NumberFormatException nfe) {
                throw new SAXException("Session VCID " + text + " is not a valid Integer value", nfe);
            }
        } else if (elementName.equals("SessionDssId")) {
            try {
                contextConfiguration.getFilterInformation().setDssId(Integer.parseInt(text));
            } catch (final NumberFormatException nfe) {
                throw new SAXException("Session DSS ID " + text + " is not a valid Integer value", nfe);
            }
        } else if (elementName.equals("DatabaseSessionKey")) {
            /*  Get SSE or FSW downlink connection */
            final IDownlinkConnection connect = contextConfiguration.getConnectionConfiguration().getDownlinkConnection();
            if (connect instanceof IDatabaseConnectionSupport) {
                final DatabaseConnectionKey databaseSessionInfo = ((IDatabaseConnectionSupport) connect)
                        .getDatabaseConnectionKey();
                databaseSessionInfo.addSessionKey(Long.parseLong(text));
            }
        } else if (elementName.equals("DatabaseSessionHost")) {
            /* Get SSE or FSW downlink connection */
            final IDownlinkConnection connect = contextConfiguration.getConnectionConfiguration().getDownlinkConnection();
            if (connect instanceof IDatabaseConnectionSupport) {
                final DatabaseConnectionKey databaseSessionInfo = ((IDatabaseConnectionSupport) connect)
                        .getDatabaseConnectionKey();
                databaseSessionInfo.addHostPattern(text);
            }
        } else if (elementName.equals("JmsSubtopic")) {
            contextConfiguration.getGeneralInfo().setSubtopic(text);
        } else if (elementName.equalsIgnoreCase("Topic")) {
            contextConfiguration.getGeneralInfo().setRootPublicationTopic(text);
        }
    }

    private void parseNetworkElement(final String name, final String text) {
        IConnectionMap connectionMap = ((IContextConfiguration) contextConfig).getConnectionConfiguration();

        if (name.equalsIgnoreCase("FswDownlinkHost")) {
            final IDownlinkConnection connect = connectionMap.getFswDownlinkConnection();
            if (connect != null && connect instanceof INetworkConnection) {
                ((INetworkConnection) connect).setHost(text.trim());
            }

        } else if (name.equalsIgnoreCase("FswUplinkHost")) {
            final IUplinkConnection uconnect = connectionMap.getFswUplinkConnection();
            if (uconnect != null) {
                uconnect.setHost(text.trim());
            }

        } else if (name.equalsIgnoreCase("SseHost")) {
            final IDownlinkConnection connect = connectionMap.getSseDownlinkConnection();
            if (connect != null && connect instanceof INetworkConnection) {
                ((INetworkConnection) connect).setHost(text.trim());
            }
            final IUplinkConnection uconnect = connectionMap.getSseUplinkConnection();
            if (uconnect != null) {
                uconnect.setHost(text.trim());
            }

        } else if (name.equalsIgnoreCase("FswUplinkPort")) {
            final IUplinkConnection uconnect = connectionMap.getFswUplinkConnection();
            if (uconnect != null) {
                uconnect.setPort(Integer.parseInt(text));
            }

        } else if (name.equalsIgnoreCase("SseUplinkPort")) {
            final IUplinkConnection uconnect = connectionMap.getSseUplinkConnection();
            if (uconnect != null) {
                uconnect.setPort(Integer.parseInt(text));
            }

        } else if (name.equalsIgnoreCase("SseDownlinkPort")) {
            final IDownlinkConnection connect = connectionMap.getSseDownlinkConnection();
            if (connect != null && connect instanceof INetworkConnection) {
                ((INetworkConnection) connect).setPort(Integer.parseInt(text));
            }

        } else if (name.equalsIgnoreCase("FswDownlinkPort")) {
            final IDownlinkConnection connect = connectionMap.getFswDownlinkConnection();
            if (connect != null && connect instanceof INetworkConnection) {
                ((INetworkConnection) connect).setPort(Integer.parseInt(text));
            }
        }
    }

    @Override
    public void characters(final char[] chars, final int start, final int length) {
        final String newText = new String(chars, start, length);
        if (!newText.equals("\n")) {
            text.append(newText);
        }
    }

    @Override
    public void error(final SAXParseException e) throws SAXException {
        log.error("Line: " + e.getLineNumber() + " Col: " + e.getColumnNumber() + ": " + e.toString());
        throw new SAXException(e);
    }

    @Override
    public void fatalError(final SAXParseException e) throws SAXException {
        error(e);
    }

    @Override
    public void warning(final SAXParseException e) throws SAXException {
        log.warn("Line: " + e.getLineNumber() + " Col: " + e.getColumnNumber() + ": " + e.toString());
    }

    private enum CONNECT_STATE {
        FSW_UP, FSW_DOWN, SSE_UP, SSE_DOWN, NONE
    }
}
