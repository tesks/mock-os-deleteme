/*
 * Copyright 2006-2023. California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government sponsorship acknowledged.
 *
 *  This software is subject to U. S. export control laws and
 *  regulations (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the
 *  extent that the software is subject to U.S. export control laws
 *  and regulations, the recipient has the responsibility to obtain
 *  export licenses or other export authority as may be required
 *  before exporting such information to foreign countries or
 *  providing access to foreign nationals.
 */
package jpl.gds.common.config.connection;

import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import jpl.gds.common.config.types.UplinkConnectionType;

/**
 * A class that holds connection parameters for an uplink network
 * connection.
 * 
 * @since R8
 */
public class UplinkNetworkConnection extends
		NetworkConnection implements
		IUplinkConnection {
		
	private UplinkConnectionType connType = UplinkConnectionType.SOCKET;

	/**
	 * Constructor.
	 * 
	 * @param type UplinkConnectionType
	 */
	public UplinkNetworkConnection(final UplinkConnectionType type) {
		if (type == null) {
			throw new IllegalArgumentException("Connection type cannot be null");
		}
		this.connType = type;
	}

	@Override
	public UplinkConnectionType getUplinkConnectionType() {
		return this.connType;
	}
	
	@Override
	public void copyValuesFrom(final IConnection toCopy) {
		if (!(toCopy instanceof IUplinkConnection)) {
			throw new IllegalArgumentException("Copy value is not an instance of IUplinkConnectionConfiguration");
		}
		
		super.copyValuesFrom(toCopy);
		this.connType = ((IUplinkConnection)toCopy).getUplinkConnectionType();
	}
	
	@Override
	public boolean equals(final Object o) {
		if (!(o instanceof UplinkNetworkConnection)) {
			return false;
		}
		
		return ((UplinkNetworkConnection)o).getUplinkConnectionType() ==
				getUplinkConnectionType() && super.equals(o);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((connType == null) ? 0 : connType.hashCode());
		return result;
	}

	@Override
    public void setTemplateContext(final Map<String, Object> map, final String prefix) {
        super.setTemplateContext(map, prefix);
        map.put(prefix + "ConnectionType", this.connType);
    }
    
    @Override
    public void generateStaxXml(final XMLStreamWriter writer, final String elementName) throws XMLStreamException {
        writer.writeStartElement(elementName);
        switch (this.connType) {
            case COMMAND_SERVICE:
                super.generateStaxXml(writer, "DsnCpdConnection");
                break;
            case SOCKET:
                super.generateStaxXml(writer, "SocketConnection");
                break;
            case UNKNOWN:
            default:
                throw new IllegalStateException("Unrecognized connection type: " + this.connType);
            
        }
        writer.writeEndElement();
    }

}
