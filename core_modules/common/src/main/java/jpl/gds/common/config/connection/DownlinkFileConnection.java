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
 * connection to a file source.
 * 
 * @since R8
 */
public class DownlinkFileConnection implements
		IDownlinkConnection, IFileConnectionSupport {
	
	private String file;
	private TelemetryConnectionType connType = TelemetryConnectionType.FILE;
	private TelemetryInputType inputType = TelemetryInputType.RAW_TF;

	@Override
	public void setFile(final String filePath) {
		this.file = filePath;

	}

	@Override
	public String getFile() {
		return this.file;
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
		if (!(toCopy instanceof IFileConnectionSupport)) {
			throw new IllegalArgumentException("Copy value is not an instance of IDatabaseConnectionSupport");
		}
		
		this.connType = ((IDownlinkConnection)toCopy).getDownlinkConnectionType();
		setInputType(((IDownlinkConnection)toCopy).getInputType());
		setFile(((IFileConnectionSupport)toCopy).getFile());

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
		if (!(o instanceof DownlinkFileConnection)) {
			return false;
		}
		
		final DownlinkFileConnection fo = (DownlinkFileConnection)o;
		
		if ((fo.getFile() == null && getFile() != null) || (fo.getFile() != null && getFile() == null)) {
			return false;
		}
		
		return ((DownlinkFileConnection)o).getDownlinkConnectionType() ==
				getDownlinkConnectionType() && ((DownlinkFileConnection)o).getInputType().equals(getInputType())
				&& ((DownlinkFileConnection)o).getFile().equals(getFile());
	}

	@Override
	public void setTemplateContext(final Map<String, Object> map, final String prefix) {

	    map.put(prefix + "ConnectionType", this.connType);

	    if (getFile() != null) {
	        map.put(prefix + "InputFile", getFile());
	    }

	    map.put(prefix + "InputFormat", inputType);       

	}
	

    @Override
    public void generateStaxXml(final XMLStreamWriter writer, final String elementName) throws XMLStreamException {
        writer.writeStartElement(elementName);
        XmlUtility.writeSimpleAttribute(writer, "InputFormat", this.inputType.toString());
        writer.writeStartElement("FileConnection");
        if (this.file != null) {
            XmlUtility.writeSimpleAttribute(writer, "FilePath", this.file);
        }
        writer.writeEndElement();
        writer.writeEndElement();
    }

}
