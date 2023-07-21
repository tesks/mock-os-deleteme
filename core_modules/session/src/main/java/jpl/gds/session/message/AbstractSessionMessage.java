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
package jpl.gds.session.message;

import org.springframework.context.ApplicationContext;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import jpl.gds.common.config.connection.ConnectionProperties;
import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.session.config.SessionConfiguration;
import jpl.gds.session.config.SessionConfigurationParser;
import jpl.gds.shared.message.BaseXmlMessageParseHandler;
import jpl.gds.shared.message.IMessageType;
import jpl.gds.shared.message.Message;

/**
 * This is an intermediate abstract message class to be extended by
 * messages that include full session configuration.
 * 
 *
 */
public abstract class AbstractSessionMessage extends Message {

	/**
	 * The SessionConfiguration object belonging to this message.
	 */
	protected IContextConfiguration sessionConfig;

	/**
	 * Constructor.
	 * @param type the internal message type 
	 */
	public AbstractSessionMessage(final IMessageType type) {
        super(type);
	}

//	/**
//	 * Sets the session configuration object for this message.
//	 * This is the full configuration, as opposed to the SessionIdentification
//	 * object kept in the parent class.
//	 * 
//	 * @param config SessionConfiguration to set
//	 */
//	public void setContextConfiguration(IContextConfiguration config) {
//		this.sessionConfig = config;
//	}

	/**
	 * Gets the session configuration object for this message.
	 * This is the full configuration, as opposed to the SessionIdentification
	 * object kept in the parent class.
	 * 
	 * @return SessionConfiguration 
	 */
	public IContextConfiguration getContextConfiguration() {
		return this.sessionConfig;
	}
	
	/**
     * FullSessionMessageParseHandler performs the parsing of an XML version of a 
     * AbstractFullSessionBasedMessage in order to specifically parse the session 
     * configuration. Classes which extend AbstractFullSessionBasedMessage should
     * be given ParseHandler classes that extend this class rather than
     * SessionXmlMessageParseHandler.
     */
    protected static class FullSessionMessageParseHandler extends BaseXmlMessageParseHandler {
    	
        private boolean inSessionConfiguration;
        private SessionConfiguration sessionConfig;
        private SessionConfigurationParser sessionConfigParser;
        private final ApplicationContext appContext;
        
        /**
         * Constructor.
         * 
         * @param appContext the current ApplicationContext
         */
        public FullSessionMessageParseHandler(final ApplicationContext appContext) {
        	this.appContext = appContext;
        }
        
        /**
         * {@inheritDoc}
         * @see org.xml.sax.ContentHandler#endElement(java.lang.String,
         *      java.lang.String, java.lang.String)
         */
        @Override
        public void endElement(final String uri, final String localName,
                final String qname) throws SAXException {
        	
        	super.endElement(uri, localName, qname);
        	
           	// It is important to understand this bit of magic. This message
           	// contains the Session XML element. We want only one set of code for
           	// parsing that. So we must create a SessionConfigurationParser here
           	// and invoke it. But the SAX event handling will not invoke it automatically
           	// since each SAX parser has only one event handler.  So we invoke
           	// the start/endElement methods in the SessionConfigurationParser
           	// directly and supply it the SessionConfiguration object we want
           	// it to populate. We start this process when we see the SessionId 
           	// XML tag and continue until we exit that tag. There is one other catch 
        	// on this endElement call. Note is is not the standard SAX endElement
        	// signature. The buffer into which SAX events have been assembling text 
        	// for the current element is in THIS class, not in the SessionConfiguration
        	// parser, so we must pass this string buffer along to endElement.
            if (qname.equalsIgnoreCase(SessionConfigurationParser.SESSION_TAG)) {
                inSessionConfiguration = false;
                sessionConfigParser.endElement(uri, localName, qname, buffer);
                ((AbstractSessionMessage) getCurrentMessage()).sessionConfig = sessionConfig;
            } else if (inSessionConfiguration) {
                sessionConfigParser.endElement(uri, localName, qname, buffer);
            } 
        }

        /**
         * {@inheritDoc}
         * @see org.xml.sax.ContentHandler#startElement(java.lang.String,
         *      java.lang.String, java.lang.String, org.xml.sax.Attributes)
         */
        @Override
        public void startElement(final String uri, final String localName,
                final String qname, final Attributes attr)
                throws SAXException {
        	
            super.startElement(uri, localName, qname, attr);

        	// It is important to understand this bit of magic. This message
        	// contains the Session XML element. We want only one set of code for
        	// parsing that. So we must create a SessionConfigurationParser here
        	// and invoke it. But the SAX event handling will not invoke it automatically
        	// since each SAX parser has only one event handler.  So we invoke
        	// the start/endElement methods in the SessionConfigurationParser
        	// directly and supply it the SessionConfiguration object we want
        	// it to populate. We start this process when we see the SessionId 
        	// XML tag and continue until we exit that tag.
            if (qname.equalsIgnoreCase(SessionConfigurationParser.SESSION_TAG)) {
             	this.sessionConfig = new SessionConfiguration(appContext.getBean(MissionProperties.class),
                		appContext.getBean(ConnectionProperties.class), false);
             	
            	sessionConfigParser = new SessionConfigurationParser(sessionConfig);
            	inSessionConfiguration = true;
            	sessionConfigParser.startElement(uri, localName, qname, attr);
            } else if (inSessionConfiguration) {
            	sessionConfigParser.startElement(uri, localName, qname, attr);
            }
        }
    }
}