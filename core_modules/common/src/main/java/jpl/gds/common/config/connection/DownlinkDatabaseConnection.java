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

import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import jpl.gds.common.config.types.DatabaseConnectionKey;
import jpl.gds.common.config.types.TelemetryConnectionType;
import jpl.gds.common.config.types.TelemetryInputType;
import jpl.gds.shared.xml.XmlUtility;

/**
 * A class that holds connection parameters for a telemetry (downlink)
 * connection to a database source.
 * 
 * @since R8
 */
public class DownlinkDatabaseConnection implements
		IDatabaseConnectionSupport, IDownlinkConnection {

	private TelemetryConnectionType connType = TelemetryConnectionType.DATABASE;
	private TelemetryInputType inputType = TelemetryInputType.RAW_TF;
	private DatabaseConnectionKey dbKey = new DatabaseConnectionKey();


	@Override
	public void setDatabaseConnectionKey(final DatabaseConnectionKey key) {
		if (key == null) {
			throw new IllegalArgumentException("DB key cannot be null");
		}
		this.dbKey = key;

	}

	@Override
	public DatabaseConnectionKey getDatabaseConnectionKey() {
		return this.dbKey;
	}
	
	@Override
	public TelemetryConnectionType getDownlinkConnectionType() {
		return this.connType;
	}
	
	@Override
	public void copyValuesFrom(final IConnection toCopy) {
		if (!(toCopy instanceof IDownlinkConnection)) {
			throw new IllegalArgumentException("Copy value is not an instance of IDownlinkConnectionConfiguration");
		}
		if (!(toCopy instanceof IDatabaseConnectionSupport)) {
			throw new IllegalArgumentException("Copy value is not an instance of IDatabaseConnectionSupport");
		}
		
		this.connType = ((IDownlinkConnection)toCopy).getDownlinkConnectionType();
		setInputType(((IDownlinkConnection)toCopy).getInputType());
		setDatabaseConnectionKey(((IDatabaseConnectionSupport)toCopy).getDatabaseConnectionKey());		
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
		if (!(o instanceof DownlinkDatabaseConnection)) {
			return false;
		}
		
		final DownlinkDatabaseConnection dbo = (DownlinkDatabaseConnection)o;
		
		final List<String> hostList = dbo.getDatabaseConnectionKey().getHostPatternList();
		final List<Long> keyList = dbo.getDatabaseConnectionKey().getSessionKeyList();
		if (hostList.size() != getDatabaseConnectionKey().getHostPatternList().size()) {
			return false;
		}
		
		if (keyList.size() != getDatabaseConnectionKey().getSessionKeyList().size()) {
			return false;
		}
		
		/* Though the key object supports multiple hosts and keys, as a telemetry input 
		 * connection, we support only one element. So only one is checked here.
		 */
		if (!hostList.isEmpty() && !hostList.get(0).equals(getDatabaseConnectionKey().getHostPatternList().get(0))) {
			return false;
		}
		
		if (!keyList.isEmpty() && !keyList.get(0).equals(getDatabaseConnectionKey().getSessionKeyList().get(0))) {
			return false;
		}
		
		return ((IDownlinkConnection)o).getDownlinkConnectionType() ==
				getDownlinkConnectionType() && ((IDownlinkConnection)o).getInputType().equals(getInputType());
	}

    @Override
    public void setTemplateContext(final Map<String, Object> map, final String prefix) {

        map.put(prefix + "ConnectionType", this.connType);
    
        final List<Long> keys = dbKey.getSessionKeyList();
        if (!keys.isEmpty()) {
            map.put(prefix + "DatabaseKey", keys.get(0));
        }

        final List<String> hosts = dbKey.getHostPatternList();
        if (!hosts.isEmpty()) {
            map.put(prefix + "DatabaseHost", hosts.get(0));
        }
        
        map.put(prefix + "InputFormat", inputType);       
    }

    @Override
    public void generateStaxXml(final XMLStreamWriter writer, final String elementName) throws XMLStreamException {
        writer.writeStartElement(elementName);
        XmlUtility.writeSimpleElement(writer, "InputFormat", this.inputType);
        writer.writeStartElement("DbConnection");
        final List<Long> keys = dbKey.getSessionKeyList();
        if (!keys.isEmpty()) {
            XmlUtility.writeSimpleAttribute(writer, "SourceKey", String.valueOf(keys.get(0)));
        }
        final List<String> hosts = dbKey.getHostPatternList();
        if (!hosts.isEmpty()) {
            XmlUtility.writeSimpleAttribute(writer, "SourceHost", hosts.get(0));
        }
        writer.writeEndElement();
        writer.writeEndElement();
    }

}
