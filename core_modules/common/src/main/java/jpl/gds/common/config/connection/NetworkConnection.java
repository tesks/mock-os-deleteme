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

import jpl.gds.shared.util.HostPortUtility;
import jpl.gds.shared.xml.XmlUtility;

/**
 * A class that implements a network connection, which can be extended by 
 * either downlink or uplink connection classes.
 * 
 * @since R8
 *
 */
public class NetworkConnection implements
		INetworkConnection {

	private int port = HostPortUtility.UNDEFINED_PORT;
	private String hostName = HostPortUtility.getLocalHostName();
	
	@Override
	public void setHost(final String host) {
		this.hostName = host;

	}

	@Override
	public String getHost() {
		return this.hostName;
	}

	@Override
	public void setPort(final int port) {
		this.port = port;

	}

	@Override
	public int getPort() {
		return this.port;
	}

	@Override
	public void copyValuesFrom(final IConnection toCopy) {
		if (!(toCopy instanceof INetworkConnection)) {
			throw new IllegalArgumentException("Copy value is not an instance of INetworkConnection");
		}
		
		final INetworkConnection ncToCopy = (INetworkConnection)toCopy;
		setHost(ncToCopy.getHost());
		setPort(ncToCopy.getPort());		
	}

	@Override
	public boolean equals(final Object o) {
		if (!(o instanceof NetworkConnection)) {
			return false;
		}

		return ((NetworkConnection)o).getHost().equals(getHost()) &&
				((NetworkConnection)o).getPort() == getPort();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((hostName == null) ? 0 : hostName.hashCode());
		result = prime * result + port;
		return result;
	}
	
	@Override
    public void setTemplateContext(final Map<String, Object> map, final String prefix) {
	    if (getHost() != null) {
	        map.put(prefix + "Host", getHost());
	    }
	    
	    if (port != HostPortUtility.UNDEFINED_PORT) {
	        map.put(prefix + "Port", port);
	    }
	}

	@Override
	public void generateStaxXml(final XMLStreamWriter writer, final String elementName) throws XMLStreamException {
	    writer.writeStartElement(elementName);
	    XmlUtility.writeSimpleAttribute(writer, "host", getHost());
	    XmlUtility.writeSimpleAttribute(writer, "port",String.valueOf(getPort()));
	    writer.writeEndElement();
	}
}
