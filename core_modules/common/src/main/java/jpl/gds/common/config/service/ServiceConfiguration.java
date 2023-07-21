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
package jpl.gds.common.config.service;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.sys.SystemUtilities;
import jpl.gds.shared.template.Templatable;
import jpl.gds.shared.xml.XmlUtility;
import jpl.gds.shared.xml.stax.StaxSerializable;
import jpl.gds.shared.xml.stax.StaxStreamWriterFactory;

/**
 * This class holds a set of service configuration information for broadcast
 * in session messages.
 * 
 */
public class ServiceConfiguration implements Templatable, StaxSerializable {
    
    /**
     * An enumeration of allowed service types.
     * 
     * @since R8
     */
    public enum ServiceType {
    	/** The global LAD service */
        GLAD("lad"),
        /** The LOMS database service */
        LOMS("lomsDb"),
        /** The messaging service - would like to change from JMS to MESSAGE, but breaks message schemas */
        JMS("jms");
        
        private String templatePrefix;
        
        private ServiceType(String prefix) {
            this.templatePrefix = prefix;
        }
        
        /**
         * Gets the prefix string used by variables in the velocity map for
         * this service type.
         * 
         * @return prefix string for template variables
         */
        public String getTemplatePrefix() {
            return this.templatePrefix;
        }
        
        /**
         * Returns the ServiceType associated with the given velocity variable name.
         * @param varName variable name
         * @return ServiceType the variable applies to
         */
        public static ServiceType createFromPrefix(String varName) {
            for (final ServiceType s: values()) {
                if (varName.startsWith(s.getTemplatePrefix())) {
                    return s;
                }
            }
            throw new IllegalArgumentException("Cannot map XML or template keyword " + varName + " to a service type");
        }
    }
    
    private final Map<ServiceType, ServiceParams> services = new TreeMap<ServiceType, ServiceParams>();

    /**
     * Constructor for an empty service configuration. It is empty
     * until populated using other populateFromConfig().
     */
    public ServiceConfiguration() {
        SystemUtilities.doNothing();
    }
    
    /**
     * Registers service parameters for a service. Will NOT overwrite
     * if the parameters for the service have already been registered.
     * @param s the ServiceParams object to register
     */
    public void addService(ServiceParams s) {
        if (!this.services.containsKey(s.getType())) {
            this.services.put(s.getType(), s);
        }
    }
    
    /**
     * Gets a map of all the service parameters.
     * 
     * @return Map of ServiceType to ServiceParams
     */
    public Map<ServiceType, ServiceParams> getAllServices() {
        return Collections.unmodifiableMap(this.services);
    }
    
    /**
     * Gets the registered ServiceParams object for a specific ServiceType.
     * 
     * @param type type of the service to get parameters for
     * @return ServiceParams object, or null if none registered
     */
    public ServiceParams getService(ServiceType type) {
        return this.services.get(type);
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.xml.stax.StaxSerializable#generateStaxXml(javax.xml.stream.XMLStreamWriter)
     */
    @Override
    public void generateStaxXml(XMLStreamWriter writer)
            throws XMLStreamException {
        
        writer.writeStartElement("ServiceConfiguration");
        for (final ServiceParams sp : this.services.values()) {
            XmlUtility.writeSimpleElement(writer, sp.getType().getTemplatePrefix() + "Host", sp.getHost());
            XmlUtility.writeSimpleElement(writer, sp.getType().getTemplatePrefix() + "Port", sp.getPort());
            XmlUtility.writeSimpleElement(writer, sp.getType().getTemplatePrefix() + "Enabled", String.valueOf(sp.isEnabled()));
            if (sp.getConnectData() != null) {
                XmlUtility.writeSimpleElement(writer, sp.getType().getTemplatePrefix() + "ConnectData", sp.getConnectData());
            }
            
        }
        writer.writeEndElement(); // ServiceConfiguration
        
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.xml.stax.StaxSerializable#toXml()
     */
    @Override
    public String toXml() {
        String output = "";
        try {
            output = StaxStreamWriterFactory.toXml(this);
        } catch (final XMLStreamException e) {
            e.printStackTrace();
            TraceManager.getDefaultTracer().error("Could not transform ServerConfiguration object to XML: "

                    + e.getMessage());
        }

        return (output);
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.template.Templatable#setTemplateContext(java.util.Map)
     */
    @Override
    public void setTemplateContext(Map<String, Object> map) {

        for (final ServiceParams sp : this.services.values()) {
            map.put(sp.getType().getTemplatePrefix() + "Host", sp.getHost());
            map.put(sp.getType().getTemplatePrefix() + "Port", sp.getPort());
            map.put(sp.getType().getTemplatePrefix() + "Enabled", String.valueOf(sp.isEnabled()));
            if (sp.getConnectData() != null) {
                map.put(sp.getType().getTemplatePrefix() + "ConnectData", sp.getConnectData());
            }
        }
    }

    /**
     * Parses an XML element into the appropriate field in this object.
     * Does nothing if the qname does not correspond to an XML element
     * written by this class.
     * 
     * @param qname XML element name
     * @param text the text content of the XML element
     */
    public void endElement(String qname, String text) {

        ServiceParams sp = null;
        try {
            final ServiceType type = ServiceType.createFromPrefix(qname);
            sp = this.services.get(type);
            if (sp == null) {
                sp = new ServiceParams(type);
                this.services.put(type, sp);
            }
        } catch (final IllegalArgumentException e) {
            return;
        }
        if (qname.endsWith("Host")) {
            sp.setHost(text.trim());
        } else if (qname.endsWith("Port")) {
            sp.setPort(XmlUtility.getIntFromText(text));
        } else if (qname.endsWith("Enabled")) {
            sp.setEnabled(Boolean.valueOf(text));
        }  else if (qname.endsWith("ConnectData")) {
            sp.setConnectData(text.trim());
        } 
    }
    
    /**
     * A class to track parameters for an individual service.
     * 
     *
     * @since R8
     */
    public static class ServiceParams {

        private ServiceType type;
        private boolean enabled;
        private String host;
        private int port;
        private String connectData;
        
        /**
         * Constructor.
         * 
         * @param type the type of the service these parameters are for
         */
        public ServiceParams(ServiceType type) {
            this.type = type;
        };
        
        /**
         * Constructor.
         * 
         * @param theType  the type of the service these parameters are for
         * @param isEnabled whether the service is enabled
         * @param theHost the host where the service resides
         * @param thePort the port on which the service is listening
         * @param connectData an additional string needed when creating the connection to the service, e.g,
         *        database name, etc.
         */
        public ServiceParams(ServiceType theType, boolean isEnabled, String theHost, int thePort, String connectData) {
            this.type = theType;
            this.enabled = isEnabled;
            this.host = theHost;
            this.port = thePort;
            this.connectData = connectData;
        }
        
        /**
         * Gets the additional connection data.
         * 
         * @return connection data string, may be null
         */
        public String getConnectData() {
            return connectData;
        }
        
        /**
         * Sets the additional connection data.
         * 
         * @param connectData the connection string to set
         */
        public void setConnectData(String connectData) {
            this.connectData = connectData;
        }
        
        /**
         * Sets the type of the service.
         * 
         * @param type ServiceType to set
         */
        public void setType(ServiceType type) {
            this.type = type;
        }

        /**
         * Sets the flag indicating whether the service is currently enabled.
         * 
         * @param enabled true if enabled, false if not
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * Sets the host on which the service can be contacted.
         * 
         * @param host host name to set
         */
        public void setHost(String host) {
            this.host = host;
        }

        /**
         * Sets the port on which the service can be contacted.
         * 
         * @param port port number to set
         */
        public void setPort(int port) {
            this.port = port;
        }

        /**
         * Gets the type of the service.
         * 
         * @return ServiceType
         */
        public ServiceType getType() {
            return type;
        }
        
        /**
         * Gets the flag indicating whether the service is currently enabled.
         * 
         * @return true if enabled, false if not
         */
        public boolean isEnabled() {
            return enabled;
        }
        
        /**
         * Gets the host on which the service can be contacted.
         * 
         * @return host name
         */
        public String getHost() {
            return host;
        }
        
        /**
         * Gets the port on which the service can be contacted.
         * 
         * @return port number
         */
        public int getPort() {
            return port;
        }
    }
}
