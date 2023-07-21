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
package jpl.gds.message.api;

import java.io.StringWriter;
import java.text.DateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.codehaus.stax2.XMLStreamWriter2;

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.config.ReleaseProperties;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessageConfiguration;
import jpl.gds.shared.message.IMessageType;
import jpl.gds.shared.message.MessageConstants;
import jpl.gds.shared.message.MessageRegistry;
import jpl.gds.shared.metadata.ISerializableMetadata;
import jpl.gds.shared.metadata.MetadataKey;
import jpl.gds.shared.metadata.MetadataMap;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.TimeUtility;
import jpl.gds.shared.xml.stax.StaxStreamWriterFactory;
/**
 * This class is used to represent a standard external message header, for
 * publication of messages to the message service. It knows the details for
 * constructing the correct XML to represent the header. It allows known header
 * properties to be set in the header or fetched from it. The known property
 * names are a subset of those defined by the MetadataKey enumeration.
 */

public class BaseMessageHeader 
{	  

    private static String projectNamespace = MessageConstants.PROJECT_NAMESPACE;

    private static Tracer logger = TraceManager.getDefaultTracer();


    private static DateFormat dateFormatter =
            TimeUtility.getDayOfYearFormatter();
    
    //Need to keep a count per message type
    private static Map<IMessageType, MessageCounter> typesReceived = new HashMap<>();

    private final Map<MetadataKey, Object> properties =
            new TreeMap<>();

    private ISerializableMetadata contextHeader;

    /**
     * Creates a BaseMessageHeader given the message type.
     * 
     * @param missionProps
     *            the current MissionProperties object
     * @param type
     *            the internal message type
     * @param header
     *            the metadata context header containing metadata that goes into
     *            the message header
     * @param time
     *            the creation time for the message header
     */
    public BaseMessageHeader(final MissionProperties missionProps, final IMessageType type,
            final ISerializableMetadata header, final IAccurateDateTime time) {

        if (type == null) {
            throw new IllegalArgumentException(
                    "Message type property is a "
                            + "required argument");
        }
        setHeaderContext(header);

        this.contextHeader.setValue(MetadataKey.AMPCS_VERSION, ReleaseProperties.getShortCoreVersion());
        this.contextHeader.setValue(MetadataKey.CREATE_TIME, getDSMSTimeString(time == null ? new AccurateDateTime() : time));

        this.contextHeader.setValue(MetadataKey.MESSAGE_TYPE, type);
        this.contextHeader.setValue(MetadataKey.SOURCE_PID, GdsSystemProperties.getIntegerPid());

        //if already seen this type, increment
        if(typesReceived.containsKey(type)) {
            this.contextHeader.setValue(MetadataKey.MESSAGE_COUNTER, typesReceived.get(type).increment());
        }
        //if never seen this type, start counter at 0
        else {
            final MessageCounter msgCounter = new MessageCounter();
            typesReceived.put(type, msgCounter);
            this.contextHeader.setValue(MetadataKey.MESSAGE_COUNTER, msgCounter.getCount());
        }

        final Integer scid =  this.contextHeader.getValueAsInteger(MetadataKey.SPACECRAFT_ID, missionProps.getDefaultScid());
      
        this.contextHeader.setValue(MetadataKey.SPACECRAFT_NAME, missionProps.mapScidToMnemonic(scid.intValue()));
        this.contextHeader.setValue(MetadataKey.MISSION_ID, missionProps.getMissionId());
        this.contextHeader.setValue(MetadataKey.MISSION_NAME, GdsSystemProperties.getSystemMission());

        addContextProperties();

    }
    
    /**
     * Creates a BaseMessageHeader given the message type, mission properties, and event
     * timestamp.
     * 
     * @param props the current MissionProperties object
     * @param type the internal message type
     * @param time the Timestamp of the event
     */
    public BaseMessageHeader(final MissionProperties props, final IMessageType type, final IAccurateDateTime time) {
       this(props, type, null, time);
    }
    
    private String getDSMSTimeString(final IAccurateDateTime t) {
        synchronized (dateFormatter) {
            return (dateFormatter.format(t));
        }
    }

    /**
     * Gets the value of the header property with the given key.
     *
     * @param name the metadata key
     *
     * @return the property value, or null if not set
     */
    public String getHeaderProperty(final MetadataKey name) {
        return (String) this.properties.get(name);
    }
    
    /**
     * Gets the message metadata context header in use by this instance.
     * 
     * @return ISerializableMetadata object
     */
    public ISerializableMetadata getContextHeader() {
        return this.contextHeader;
    }
    
    /**
     * Gets the message header property map, with strings as keys.
     * 
     * @return map of header property name to property value, never null
     */
    public Map<String, Object> getPropertiesWithStringKeys() {
        final Map<String, Object> result = new TreeMap<>();
        for (final Entry<MetadataKey, Object> entry : properties.entrySet()) {
            result.put(entry.getKey().toString(), entry.getValue());
        }
        return result;
    }

    /**
     * Sets a header property value. If the property already exists, its value
     * is overwritten. The supplied value may not be null.
     * @param name the metadata key for the property to set
     * @param val the value of the property
     */
    public void setHeaderProperty(final MetadataKey name, final String val) {
        properties.put(name, val);
    }

    /**
     * Sets the context metadata for the message header.
     * @param header the ISerializableMetadata object to set
     */
    public void setHeaderContext(final ISerializableMetadata header) {
        this.contextHeader =  new MetadataMap(header == null ? null : header.getContextId());
        if (header != null) {
            this.contextHeader.addContextValues(header);
        }
    }
    
    private void addContextProperties() {
        if (contextHeader != null) {
            
            final Set<MetadataKey> contextProps = contextHeader.getKeys();
            for (final MetadataKey key: contextProps) {
                properties.put(key, contextHeader.getValue(key, null));
            }
        }
    }

    /**
     * Wraps an XML message body in a root element with the supplied name,
     * and adds the XML representation of the metadata header and the required
     * namespace declarations. The end result is a complete message suitable for
     * sending on the DSMS message bus, as follows:
     * 
     * <pre>
     * <rootElementName {namespace specifiers)>
     * <DSMSHeader> 
     * {XML Header content}
     * </DSMSHeader>
     * <DSMSBody>
     * {XML Body content}
     * </DSMSBody>
     * </rootElementName>
     * </pre>
     * @param type the internal message type
     * @param content the XML content to use as the body of the DSMS element
     * @return wrapped content
     */
    public String wrapContent(final IMessageType type, final String content)
    {
    	String output = "";
    	try
    	{
    		final StringWriter sw = new StringWriter(2048);
       	 	final XMLStreamWriter writer = StaxStreamWriterFactory.getInstance().get(sw);
       	 	
       	 	final IMessageConfiguration mc = MessageRegistry.getMessageConfig(type);
       	 	if (mc == null) {
       	 	    throw new IllegalStateException("Unable to find message registry entry for message type " + type);
       	 	}
	        writer.writeStartElement(mc.getExternalRootElement()); // <messageName>
	        writer.writeAttribute("xmlns",projectNamespace);
	        writer.writeAttribute("xmlns:xsi",XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI);
	        
	        if(contextHeader != null)
	        {
	        	contextHeader.toContextXml(writer);
	        }

	        ((XMLStreamWriter2)writer).writeRaw(content);
	                
	        writer.writeEndElement(); // </messageName>
			
			writer.flush();
			writer.close();
			
			output = sw.toString();
		}
    	catch(final XMLStreamException e)
    	{
            logger.error("Could not convert Message Header to XML", e);
		}
    	
    	return(output);
    }
    
    /**
     * Populates a message wrap template and returns the resulting text, which contains
     * a %s place holder for the message body.
     * @param type the internal message type
     * @return the wrap text
     */

    public String getWrapTemplate(final IMessageType type)
    {
    	return(wrapContent(type,"%s"));
    }
       
    /**
     * Gets a protobuf binary version of required header fields for binary messages.
     * 
     * @return a byte array containing the header and context configuration information
     */
    public byte[] getBinaryHeader() {
    	byte[] contextBytes = null;

    	int len = 0;
    	if (this.contextHeader != null) {
    	    /* Messages use brief session ID binary */
    	    contextBytes = this.contextHeader.toContextBinary();
    	    len += contextBytes.length;
    	} else {
    		len += 2;
    	}
    	final byte[] result = new byte[len];
    	final int off = 0;
    	if (contextBytes != null) {
    		System.arraycopy(contextBytes, 0, result, off, contextBytes.length);
    	}
    	return result;
    }

	
    
	/**
     * Message counter class.
     *
     * Need to synchronize maybe? since msgs come in so fast...
     */
	private static class MessageCounter {
		private int counter = 0;
		
		public MessageCounter()
        {
            super();
		}
		
		public int increment() {
			return ++counter;
		}
		
		public int getCount() {
			return counter;
		}
	}

}
