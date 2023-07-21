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

import jpl.gds.common.config.types.TelemetryConnectionType;
import jpl.gds.common.config.types.TelemetryInputType;
import jpl.gds.shared.xml.XmlUtility;

/**
 * A class that holds connection parameters for a telemetry (downlink)
 * connection to a network source.
 * 
 * @since R8
 */
public class DownlinkNetworkConnection extends
		NetworkConnection implements
		IDownlinkConnection {
	
	private TelemetryConnectionType connType = TelemetryConnectionType.UNKNOWN;
	private TelemetryInputType inputType = TelemetryInputType.RAW_TF;

	/**
	 * Constructor.
	 * 
	 * @param type the Telemetry Connection Type, which must be a network
	 *        type
	 */
	public DownlinkNetworkConnection(final TelemetryConnectionType type) {
		if (type == null) {
			throw new IllegalArgumentException("Connection type cannot be null");
		}
		if (!type.isNetwork()) {
			throw new IllegalArgumentException("A network connection cannot be created with connection type " 
		      + type);
		}
		this.connType = type;
	}

	@Override
	public TelemetryConnectionType getDownlinkConnectionType() {
		return this.connType;
	}
	
	
	@Override
	public void copyValuesFrom(final IConnection toCopy) {
		if (!(toCopy instanceof IDownlinkConnection)) {
			throw new IllegalArgumentException("Copy value is not an instance of  INetworkConnectionConfiguration");
		}
		
		super.copyValuesFrom(toCopy);
		this.connType = ((IDownlinkConnection)toCopy).getDownlinkConnectionType();
		setInputType(((IDownlinkConnection)toCopy).getInputType());
	}
	

	@Override
	public TelemetryInputType getInputType() {
		//if defined by adaptation, the bean is loaded too late and the input type was already created
		//with default properties like hasFrames etc
		return TelemetryInputType.valueOf(this.inputType.name());
	}

	@Override
	public void setInputType(final TelemetryInputType type) {
		if (type == null) {
			throw new IllegalArgumentException("Input type cannot be null");
		}
		this.inputType = type;		
	}
	
	@Override
	public boolean equals(final Object o) {
		if (!(o instanceof DownlinkNetworkConnection)) {
			return false;
		}
		
		return ((DownlinkNetworkConnection)o).getDownlinkConnectionType() ==
				getDownlinkConnectionType() && ((DownlinkNetworkConnection)o).getInputType().equals(
				getInputType()) && super.equals(o);
	}
	
    @Override
    public void setTemplateContext(final Map<String, Object> map, final String prefix) {
        super.setTemplateContext(map, prefix);
        map.put(prefix + "ConnectionType", this.connType);
        map.put(prefix + "InputFormat", inputType);         
    }
    
    @Override
    public void generateStaxXml(final XMLStreamWriter writer, final String elementName) throws XMLStreamException {
        writer.writeStartElement(elementName);
        XmlUtility.writeSimpleAttribute(writer, "InputFormat", this.inputType.toString());
        String role = null;
        String nestedElement = null;
        switch(this.connType) {
            case CLIENT_SOCKET: 
                nestedElement = "SocketConnection";
                role = "CLIENT";
                break;
            case NEN_SN_CLIENT:
                nestedElement = "NenSnConnection";
                role = "CLIENT";
                break;
            case NEN_SN_SERVER:
                nestedElement = "NenSnConnection";
                role = "SERVER";
                break;
            case SERVER_SOCKET:
                nestedElement = "SocketConnection";
                role = "SERVER";
                break;
            case DATABASE:
            case FILE:
            case TDS:
            case UNKNOWN:
            default:
                throw new IllegalStateException("Unrecognized connection type: " + this.connType);
            
        }
        writer.writeStartElement(nestedElement);
        XmlUtility.writeSimpleAttribute(writer, "host", getHost());
        XmlUtility.writeSimpleAttribute(writer, "port",String.valueOf(getPort()));
        XmlUtility.writeSimpleAttribute(writer, "role", role);
        writer.writeEndElement();
        writer.writeEndElement();
    }
}
