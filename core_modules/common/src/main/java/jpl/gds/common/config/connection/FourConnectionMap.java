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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.common.config.types.DownlinkStreamType;
import jpl.gds.common.config.types.TelemetryConnectionType;
import jpl.gds.common.config.types.TelemetryInputType;
import jpl.gds.common.config.types.UplinkConnectionType;
import jpl.gds.common.config.types.VenueType;
import jpl.gds.shared.spring.context.flag.SseContextFlag;


/**
 * A map that holds up to 4 connection objects: one for FSW downlink, one for FSW uplink,
 * one for SSE downlink, and one for SSE uplink. The latter are present only if the current
 * mission supports SSE.
 * 
 * @since R8
 */
@SuppressWarnings("serial")
public final class FourConnectionMap extends HashMap<ConnectionKey, IConnection> implements IConnectionMap  {
    
    private static final String FSW_UPLINK_ELEMENT = "fsw_uplink_connection";
    private static final String FSW_DOWNLINK_ELEMENT = "fsw_downlink_connection";
    private static final String SSE_UPLINK_ELEMENT = "sse_uplink_connection";
    private static final String SSE_DOWNLINK_ELEMENT = "sse_downlink_connection";
    
    private final ConnectionProperties connectProps;
    private final MissionProperties missionProps;
    private final SseContextFlag       sseFlag;

    /**
     * Constructor.
     * 
     * @param connectionProps
     *            the ConnectionProperties object to get defaults from
     * @param missionProps
     *            the current MissionProperties object
     * @param sseFlag
     *            the sse context flag
     */
    public FourConnectionMap(final ConnectionProperties connectionProps, 
                             final MissionProperties missionProps,
                             final SseContextFlag sseFlag) {
		this.connectProps = connectionProps;
		this.missionProps =  missionProps;
        this.sseFlag = sseFlag;
		init();
	}
    
    private void init() {
    	
         IDownlinkConnection cc = ConnectionFactory.createDownlinkConfiguration(TelemetryConnectionType.CLIENT_SOCKET);
         this.put(ConnectionKey.FSW_DOWNLINK, cc);
         if (missionProps.missionHasSse()) {
        	 cc = ConnectionFactory.createDownlinkConfiguration(TelemetryConnectionType.CLIENT_SOCKET);
             this.put(ConnectionKey.SSE_DOWNLINK, cc);
             cc.setInputType(TelemetryInputType.RAW_PKT);
             
         }
         if (missionProps.isUplinkEnabled()) {
        	 IUplinkConnection ucc = ConnectionFactory.createUplinkConfiguration(UplinkConnectionType.SOCKET);
        	 this.put(ConnectionKey.FSW_UPLINK, ucc);
        	 if (missionProps.missionHasSse()) {
            	 ucc = ConnectionFactory.createUplinkConfiguration(UplinkConnectionType.SOCKET);
                 this.put(ConnectionKey.SSE_UPLINK, ucc);
             }
         }
    }

    @Override
	public ConnectionProperties getConnectionProperties() {
    	return this.connectProps;
    }

    @Override
    public synchronized void setDefaultNetworkValuesForVenue(
                                 final VenueType venueType,
                                 final String tbName,
                                 final DownlinkStreamType dst,
                                 final boolean uplinkOnly, 
                                 final boolean downlinkOnly)
    {
        if (venueType == null)
        {
            throw new IllegalStateException("Cannot set default network " +
                                            "values if the venue type is null");
        }
        if (uplinkOnly && downlinkOnly) {
            throw new IllegalStateException("Cannot set uplinkOnly=" + uplinkOnly + " && downlinkOnly=" + downlinkOnly
                    + ": " + connectProps);
        }

        if (! uplinkOnly)
        {
            setFswDownlinkHost(connectProps.getDefaultDownlinkHost(venueType, tbName, dst));
            setFswDownlinkPort(connectProps.getDefaultDownlinkPort(venueType, tbName, dst));

            setFswDownlinkInputType(connectProps.getDefaultSourceFormat(venueType, false));

            if (missionProps.missionHasSse()) {
                setSseDownlinkPort(connectProps.getDefaultDownlinkPort(venueType, tbName, true));
                setSseDownlinkHost(connectProps.getDefaultDownlinkHost(venueType, tbName, true));

                setSseDownlinkInputType(connectProps.getDefaultSourceFormat(venueType, true));
            }
        }
        
        if (missionProps.isUplinkEnabled() && !downlinkOnly) {
            setFswUplinkHost(connectProps.getDefaultUplinkHost(venueType, tbName, false));
            setFswUplinkPort(connectProps.getDefaultUplinkPort(venueType, tbName, false));

            if (missionProps.missionHasSse()) {
                setSseUplinkHost(connectProps.getDefaultUplinkHost(venueType, tbName, true));
                setSseUplinkPort(connectProps.getDefaultUplinkPort(venueType, tbName, true));
            }
        }
    }

    @Override
    public synchronized void setDefaultNetworkValuesForVenue(final VenueType venueType, final String tbName,
                                                             final DownlinkStreamType dst, final boolean uplinkOnly) {
        setDefaultNetworkValuesForVenue(venueType, tbName, dst, uplinkOnly, false);
    }

    @Override
	public void setDefaultNetworkValuesForVenue(final VenueType venueType, final String tbName, final DownlinkStreamType dst)
    {
        setDefaultNetworkValuesForVenue(venueType, tbName, dst, false);
    }


    private void setFswDownlinkHost(final String paramDownlinkHost)
    {
        if (paramDownlinkHost == null) {
            throw new IllegalArgumentException("Null input host");
        }

        final IConnection cc = this.get(ConnectionKey.FSW_DOWNLINK);
        if (cc == null) {
        	throw new IllegalStateException("Attempting to set FSW Downlink host from configuration, but no such connection in the connection map");
        }
        
        if (!(cc instanceof INetworkConnection)) {
        	return;
        }
        ((INetworkConnection)cc).setHost(paramDownlinkHost);
    }

    private void setSseDownlinkInputType(final TelemetryInputType inputType) {
        final IConnection cc = this.get(ConnectionKey.SSE_DOWNLINK);
        if (cc == null) {
            throw new IllegalStateException("Attempting to set SSE Downlink Input Type from configuration, but no such connection in the connection map");
        }

        if (!(cc instanceof IDownlinkConnection)) {
            return;
        }

        ((IDownlinkConnection) cc).setInputType(inputType);
    }

    private void setFswDownlinkInputType(final TelemetryInputType inputType) {
        final IConnection cc = this.get(ConnectionKey.FSW_DOWNLINK);
        if (cc == null) {
            throw new IllegalStateException("Attempting to set FSW Downlink Input Type from configuration, but no such connection in the connection map");
        }

        if (!(cc instanceof IDownlinkConnection)) {
            return;
        }

        ((IDownlinkConnection) cc).setInputType(inputType);
    }

    private void setFswDownlinkPort(final int paramFswDownlinkPort) {
    	
        final IConnection cc = this.get(ConnectionKey.FSW_DOWNLINK);
        if (cc == null) {
        	throw new IllegalStateException("Attempting to set FSW Downlink port from configuration, but no such connection in the connection map");
        }
        
        if (!(cc instanceof INetworkConnection)) {
        	return;
        }
  
        ((INetworkConnection)cc).setPort(paramFswDownlinkPort);
    }

    private void setFswUplinkHost(final String paramFswHost) {
        if (paramFswHost == null) {
            throw new IllegalArgumentException("Null input host");
        }

        final IConnection cc = this.get(ConnectionKey.FSW_UPLINK);
        if (cc == null) {
        	throw new IllegalStateException("Attempting to set FSW Uplink host from configuration, but no such connection in the connection map");
        }
        
        if (!(cc instanceof INetworkConnection)) {
        	return;
        }
        ((INetworkConnection)cc).setHost(paramFswHost);
    }

    private void setFswUplinkPort(final int uplinkPort) {
    	final IConnection cc = this.get(ConnectionKey.FSW_UPLINK);
        if (cc == null) {
        	throw new IllegalStateException("Attempting to set FSW Uplink port from configuration, but no such connection in the connection map");
        }
        
        if (!(cc instanceof INetworkConnection)) {
        	return;
        }
  
        ((INetworkConnection)cc).setPort(uplinkPort);
    }

    private void setSseDownlinkPort(final int paramSseDownlinkPort) {

    	final IConnection cc = this.get(ConnectionKey.SSE_DOWNLINK);
    	if (cc == null) {
    		throw new IllegalStateException("Attempting to set SSE Downlink port from configuration, but no such connection in the connection map");
    	}

    	if (!(cc instanceof INetworkConnection)) {
    		return;
    	}

    	((INetworkConnection)cc).setPort(paramSseDownlinkPort);
    }

    private void setSseDownlinkHost(final String paramSseHost) {
        if (paramSseHost == null) {
            throw new IllegalArgumentException("Null input host");
        }
        final IConnection cc = this.get(ConnectionKey.SSE_DOWNLINK);
        if (cc == null) {
        	throw new IllegalStateException("Attempting to set SSE Downlink host from configuration, but no such connection in the connection map");
        }
        
        if (!(cc instanceof INetworkConnection)) {
        	return;
        }
        ((INetworkConnection)cc).setHost(paramSseHost);
    }

    private void setSseUplinkHost(final String paramSseHost) {
        if (paramSseHost == null) {
            throw new IllegalArgumentException("Null input host");
        }
        final IConnection cc = this.get(ConnectionKey.SSE_UPLINK);
        if (cc == null) {
        	throw new IllegalStateException("Attempting to set SSE Uplink host from configuration, but no such connection in the connection map");
        }
        
        if (!(cc instanceof INetworkConnection)) {
        	return;
        }
        ((INetworkConnection)cc).setHost(paramSseHost);
    }
    
    private void setSseUplinkPort(final int ssePort) {
    	final IConnection cc = this.get(ConnectionKey.SSE_UPLINK);
    	if (cc == null) {
    		throw new IllegalStateException("Attempting to set SSE Uplink port from configuration, but no such connection in the connection map");
    	}

    	if (!(cc instanceof INetworkConnection)) {
    		return;
    	}

    	((INetworkConnection)cc).setPort(ssePort);
    }

    @Override
	public void setConnection(final ConnectionKey key, final IConnection config) {
    	this.put(key, config);
    }

	@Override
	public void copyValuesFrom(final IConnectionMap map) {
		if (this == map) {
			return;
		}
		clear();
		for (final Map.Entry<ConnectionKey, IConnection> entry: map.entrySet()) {
			IConnection newEntry = null;
			switch (entry.getKey()) {
			case FSW_DOWNLINK:
			case SSE_DOWNLINK:
				newEntry = ConnectionFactory.
				    createDownlinkConfiguration(((IDownlinkConnection)entry.getValue()).getDownlinkConnectionType());
				break;
			case SSE_UPLINK:
			case FSW_UPLINK:
				newEntry = ConnectionFactory.
			        createUplinkConfiguration(((IUplinkConnection)entry.getValue()).getUplinkConnectionType());
				break;
			default:
				break;
			
			}
			if (newEntry != null) {
			    newEntry.copyValuesFrom(entry.getValue());
			    put(entry.getKey(), newEntry);
			}
		}
		
	}

	@Override
	public IDownlinkConnection getFswDownlinkConnection() {
		return (IDownlinkConnection) get(ConnectionKey.FSW_DOWNLINK);
	}

	@Override
	public IDownlinkConnection getSseDownlinkConnection() {
		return (IDownlinkConnection) get(ConnectionKey.SSE_DOWNLINK);
	}

	@Override
	public IDownlinkConnection getDownlinkConnection() {
        if (sseFlag.isApplicationSse()) {
			return getSseDownlinkConnection();
		} else {
			return getFswDownlinkConnection();
		}
	}

	@Override
	public IUplinkConnection getFswUplinkConnection() {
		return (IUplinkConnection) get(ConnectionKey.FSW_UPLINK);
	}

	@Override
	public IUplinkConnection getSseUplinkConnection() {
		return (IUplinkConnection) get(ConnectionKey.SSE_UPLINK);
	}
	
	@Override
	public IUplinkConnection getUplinkConnection() {
        if (sseFlag.isApplicationSse()) {
			return getSseUplinkConnection();
		} else {
			return getFswUplinkConnection();
		}
	}

	@Override
	public void createFswDownlinkConnection(final TelemetryConnectionType type) {
        put(ConnectionKey.FSW_DOWNLINK, ConnectionFactory.createDownlinkConfiguration(type));
        setDownlinkConnectionInputType(false);
    }

	@Override
	public void createSseDownlinkConnection(final TelemetryConnectionType type) {
        put(ConnectionKey.SSE_DOWNLINK, ConnectionFactory.createDownlinkConfiguration(type));
        setDownlinkConnectionInputType(true);

	}

	@Override
	public void createFswUplinkConnection(final UplinkConnectionType type) {
        put(ConnectionKey.FSW_UPLINK, ConnectionFactory.createUplinkConfiguration(type));
	}

	@Override
	public void createSseUplinkConnection(final UplinkConnectionType type) {
        put(ConnectionKey.SSE_UPLINK, ConnectionFactory.createUplinkConfiguration(type));
	}

	@Override
	public void createDownlinkConnection(final TelemetryConnectionType type) {
        if (sseFlag.isApplicationSse()) {
			createSseDownlinkConnection(type);
		} else {
			createFswDownlinkConnection(type);
		}

	}
	
	@Override
	public void createUplinkConnection(final UplinkConnectionType type) {
        if (sseFlag.isApplicationSse()) {
			createSseUplinkConnection(type);
		} else {
			createFswUplinkConnection(type);
		}
		
	}

	@Override
	public void setTemplateContext(final Map<String, Object> map, final boolean includeSse) {
	    for (final ConnectionKey k: this.keySet()) {
	        final IConnection connect = get(k);
	        switch(k) {
	            case FSW_UPLINK:
	                if (this.missionProps.isUplinkEnabled()) {
	                    connect.setTemplateContext(map, "fswUplink");
	                }
	                break;
	            case SSE_UPLINK:
	                if (this.missionProps.isUplinkEnabled() && includeSse) {
	                    connect.setTemplateContext(map, "sseUplink");
	                }
	                break;
	            case FSW_DOWNLINK:
	                connect.setTemplateContext(map, "fswDownlink");
	                break;
	            case SSE_DOWNLINK:
	                if (includeSse) {
	                    connect.setTemplateContext(map, "sseDownlink");
	                }
	                break;
	            default:
	                break;
	        }
	    }

	}

    @Override
    public void generateStaxXml(final XMLStreamWriter writer, final boolean includeSse) throws XMLStreamException {
        writer.writeStartElement("Connections");
        for (final ConnectionKey k: this.keySet()) {
            final IConnection connect = get(k);
            switch(k) {
                case FSW_UPLINK:
                    if (this.missionProps.isUplinkEnabled()) {
                        connect.generateStaxXml(writer, FSW_UPLINK_ELEMENT);
                    }
                    break;
                case SSE_UPLINK:
                    if (this.missionProps.isUplinkEnabled() && includeSse) {
                        connect.generateStaxXml(writer, SSE_UPLINK_ELEMENT);
                    }
                    break;
                case FSW_DOWNLINK:
                    connect.generateStaxXml(writer, FSW_DOWNLINK_ELEMENT);
                    break;
                case SSE_DOWNLINK:
                    if (includeSse) {
                        connect.generateStaxXml(writer, SSE_DOWNLINK_ELEMENT);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private void setDownlinkConnectionInputType(final boolean isSse) {
        IDownlinkConnection connection;
        if (isSse) {
            connection = getSseDownlinkConnection();
        } else {
            connection = getFswDownlinkConnection();
        }

        if (connection == null) {
            throw new IllegalStateException("Attempting to set TelemetryInputFormat from configuration, but no such connection in the connection map");
        }
        if (!(connection instanceof IDownlinkConnection)) {
            return;
        }
        final TelemetryInputType defaultInputType = connectProps.getDefaultSourceFormat(isSse);
        final Set<TelemetryInputType> allowedInputTypes = connectProps.getAllowedDownlinkSourceFormats(connection.getDownlinkConnectionType(), isSse);

        if (allowedInputTypes.contains(defaultInputType)) {
            connection.setInputType(connectProps.getDefaultSourceFormat(isSse));
        }
        else if (!allowedInputTypes.isEmpty()) {
            connection.setInputType(allowedInputTypes.iterator().next());
        }
    }

    @Override
    public SseContextFlag getSseContextFlag() {
        return sseFlag;
    }
}
