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
package jpl.gds.common.config.connection;

import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import jpl.gds.common.config.types.DownlinkStreamType;
import jpl.gds.common.config.types.TelemetryConnectionType;
import jpl.gds.common.config.types.UplinkConnectionType;
import jpl.gds.common.config.types.VenueType;
import jpl.gds.shared.spring.context.flag.SseContextFlag;

/**
 * An interface to be implemented by a connection map object in context
 * configurations. The connection map contains connection objects for multiple
 * types of uplink and downlink connections.
 * 
 * @since R8
 *
 */
public interface IConnectionMap extends Map<ConnectionKey, IConnection> {
	
	/**
	 * Sets the connection object with the specified key into the map, replacing
	 * any previous entry for the key.
	 * 
	 * @param key
	 *            ConnectionKey
	 * @param config
	 *            the connection configuration to set; if null, the map entry is
	 *            deleted
	 */
	public void setConnection(ConnectionKey key, IConnection config);
	
	/**
	 * Creates a FSW downlink connection object in the map, using the
	 * supplied connection type to determine the type of the connection
	 * object created.
	 * 
	 * @param type TelemetryConnectionType for the new connection
	 */
	public void createFswDownlinkConnection(TelemetryConnectionType type);
	
	/**
	 * Creates an SSE downlink connection object in the map, using the
	 * supplied connection type to determine the type of the connection
	 * object created.
	 * 
	 * @param type TelemetryConnectionType for the new connection
	 */
	public void createSseDownlinkConnection(TelemetryConnectionType type);
	
	/**
	 * Creates a FSW or SSE downlink connection object in the map, using the
	 * supplied connection type to determine the type of the connection
	 * object created. Which connection is created is determined by examining
	 * the context. If currently operating in an SSE context, the SSE connection
	 * is created. Otherwise, the FSW connection is created.
	 * 
	 * @param type TelemetryConnectionType for the new connection
	 */
	public void createDownlinkConnection(TelemetryConnectionType type);
	
	/**
	 * Creates a FSW uplink connection object in the map, using the
	 * supplied connection type to determine the type of the connection
	 * object created.
	 * 
	 * @param type UplinkConnectionType for the new connection
	 */
	public void createFswUplinkConnection(UplinkConnectionType type);
	
	/**
	 * Creates an SSE uplink connection object in the map, using the
	 * supplied connection type to determine the type of the connection
	 * object created.
	 * 
	 * @param type UplinkConnectionType for the new connection
	 */
	public void createSseUplinkConnection(UplinkConnectionType type);
	
	/**
	 * Creates a FSW or SSE uplink connection object in the map, using the
	 * supplied connection type to determine the type of the connection
	 * object created. Which connection is created is determined by examining
	 * the context. If currently operating in an SSE context, the SSE connection
	 * is created. Otherwise, the FSW connection is created.
	 * 
	 * @param type UplinkConnectionType for the new connection
	 */
	public void createUplinkConnection(UplinkConnectionType type);
	
	/**
     * Deep copies connection members from the given connection map object to this one.
     * Does not copy properties objects.
     * 
     * @param map the IConnectionMap to copy from
     */
	public void copyValuesFrom(IConnectionMap map);
	
	/**
	 * Given a set of parameters, set any necessary corresponding host and port
	 * values in network connection objects from defaults in the
	 * ConnectionProperties. Currently this only has an effect if the source is
	 * TESTBED or SSE. NOTE: This method should be used to establish defaults
	 * BEFORE user arguments that can override the host/port are used. This
	 * method can change the current values of the downlink host and downlink
	 * port. It will have no affect on non-network connections.
	 * 
	 * @param venueType
	 *            the current venue
	 * @param tbName
	 *            the current testbed name; may be null for non-testbed venues
	 * @param dst
	 *            the current downlink stream type for testbed/ATLO venues
	 */
	public void setDefaultNetworkValuesForVenue(final VenueType venueType,
			String tbName, DownlinkStreamType dst);
	
	/**
	 * Given a set of parameters, set any necessary corresponding host and port
	 * values in network connection objects from defaults in the
	 * ConnectionProperties. Currently this only has an effect if the source is
	 * TESTBED or SSE. NOTE: This method should be used to establish defaults
	 * BEFORE user arguments that can override the host/port are used. This
	 * method can change the current values of the downlink host and downlink
	 * port. It will have no affect on non-network connections.
	 * 
	 * @param venueType
	 *            the current venue
	 * @param tbName
	 *            the current testbed name; may be null for non-testbed venues
	 * @param dst
	 *            the current downlink stream type for testbed/ATLO venues
	 * @param uplinkOnly
	 *            true if downlink stuff is not to be configured
	 */
	public void setDefaultNetworkValuesForVenue(
             final VenueType venueType,
             String tbName,
             DownlinkStreamType dst,
             final boolean uplinkOnly);
	
	   /**
     * Given a set of parameters, set any necessary corresponding host and port
     * values in network connection objects from defaults in the
     * ConnectionProperties. Currently this only has an effect if the source is
     * TESTBED or SSE. NOTE: This method should be used to establish defaults
     * BEFORE user arguments that can override the host/port are used. This
     * method can change the current values of the downlink host and downlink
     * port. It will have no affect on non-network connections.
     * 
     * @param venueType
     *            the current venue
     * @param tbName
     *            the current testbed name; may be null for non-testbed venues
     * @param dst
     *            the current downlink stream type for testbed/ATLO venues
     * @param uplinkOnly
     *            true if downlink stuff is not to be configured
     * @param downlinkOnly
     *            true if uplink stuff is not to be configured
     */
    public void setDefaultNetworkValuesForVenue(
             final VenueType venueType,
             String tbName,
             DownlinkStreamType dst,
             final boolean uplinkOnly, 
             final boolean downlinkOnly);
	

	/**
	 * Gets the FSW Downlink connection object from the map. 
	 * 
	 * @return downlink connection object
	 */
	public IDownlinkConnection getFswDownlinkConnection();
	
	/**
	 * Gets the SSE Downlink connection object from the map.
	 * 
	 * @return downlink connection object; will be null if SSE is not supported
	 *         by the current mission
	 */
	public IDownlinkConnection getSseDownlinkConnection();
	
	/**
	 * Gets the FSW or SSE Downlink connection object from the map. Which
	 * connection is returned is determined by examining the context. If
	 * currently operating in an SSE context, the SSE connection is returned.
	 * Otherwise, the FSW connection is returned.
	 * 
	 * @return downlink connection object
	 */
	public IDownlinkConnection getDownlinkConnection();
	
	/**
	 * Gets the FSW Uplink connection object from the map.
	 * 
	 * @return uplink connection object; will be null if the current mission
	 *         does not support uplink
	 */
	public IUplinkConnection getFswUplinkConnection();
	
	/**
	 * Gets the SSE Uplink connection object from the map.
	 * 
	 * @return uplink connection object; will be null if the current mission
	 *         does not support uplink or does not support SSE
	 */
	public IUplinkConnection getSseUplinkConnection();
	
	/**
	 * Gets the FSW or SSE Uplink connection object from the map. Which
	 * connection is returned is determined by examining the context. If
	 * currently operating in an SSE context, the SSE connection is returned.
	 * Otherwise, the FSW connection is returned.
	 * 
	 * @return uplink connection object
	 */
	public IUplinkConnection getUplinkConnection();
	
	 /**
     * Gets the ConnectionProperties object this one was created with.
     * 
     * @return ConnectionProperties;
     */
	public ConnectionProperties getConnectionProperties();

	
	/**
     * Generates the XML for this connection.
     * 
     * @param writer XML stream to write to
     * @param includeSse true to include SSE connection information in the output
     * @throws XMLStreamException if there is a problem generating the XML
     */
    public void generateStaxXml(XMLStreamWriter writer, boolean includeSse) throws XMLStreamException;

    /**
     * Generates the template context map variables for this connection.
     * 
     * @param map map to write variables and values to
     * @param includeSse true to include SSE connection information in the map
     */
    void setTemplateContext(Map<String, Object> map, boolean includeSse);

    public SseContextFlag getSseContextFlag();
}
