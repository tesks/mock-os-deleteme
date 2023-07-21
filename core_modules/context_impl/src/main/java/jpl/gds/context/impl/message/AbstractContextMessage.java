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
package jpl.gds.context.impl.message;

import jpl.gds.context.api.IContextConfigurationFactory;
import jpl.gds.context.api.ISimpleContextConfiguration;
import jpl.gds.shared.metadata.context.ContextConfigurationType;
import org.springframework.context.ApplicationContext;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import jpl.gds.common.config.connection.ConnectionProperties;
import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.context.impl.ContextConfiguration;
import jpl.gds.context.impl.message.parser.ContextConfigurationParser;
import jpl.gds.shared.message.BaseXmlMessageParseHandler;
import jpl.gds.shared.message.IMessageType;
import jpl.gds.shared.message.Message;

/**
 * This is an intermediate abstract message class to be extended by
 * messages that include full session configuration.
 * 
 *
 */
public abstract class AbstractContextMessage extends Message {

    /**
     * The ContextConfiguration object belonging to this message.
     */
    protected ISimpleContextConfiguration contextConfig;

    /**
     * Constructor.
     * 
     * @param type
     *            the internal message type
     */
    public AbstractContextMessage(final IMessageType type) {
        super(type);
    }


    /**
     * Gets the Context configuration object for this message.
     * This is the full configuration, as opposed to the ContextIdentification
     * object kept in the parent class.
     * 
     * @return IContextConfiguration
     */
    public ISimpleContextConfiguration getContextConfiguration() {
        return this.contextConfig;
    }

    /**
     * FullContextMessageParseHandler performs the parsing of an XML version of a
     * AbstractContextBasedMessage in order to specifically parse the Context
     * configuration. Classes which extend AbstractContextBasedMessage should
     * be given ParseHandler classes that extend this class rather than
     * ContextXmlMessageParseHandler.
     */
    protected static class ContextMessageParseHandler extends BaseXmlMessageParseHandler {
        private boolean                    inContextConfiguration;
        private ISimpleContextConfiguration      contextConfig;
        private ContextConfigurationParser contextConfigParser;
        private final ApplicationContext         appContext;

        /**
         * Constructor
         * 
         * @param appContext
         *            the current ApplicationContext
         */
        public ContextMessageParseHandler(final ApplicationContext appContext) {
            this.appContext = appContext;
        }

        @Override
        public void endElement(final String uri, final String localName, final String qname) throws SAXException {
            super.endElement(uri, localName, qname);

            if (qname.equals(ContextConfigurationParser.CONTEXT_TAG)) {
                inContextConfiguration = false;
                contextConfigParser.endElement(uri, localName, qname, buffer);
                ((AbstractContextMessage) getCurrentMessage()).contextConfig = contextConfig;
            }
            else if (inContextConfiguration) {
                contextConfigParser.endElement(uri, localName, qname, buffer);
            }
        }


        @Override
        public void startElement(final String uri, final String localName, final String qname, final Attributes attr)
                throws SAXException {
            super.startElement(uri, localName, qname, attr);

            if (qname.equals(ContextConfigurationParser.CONTEXT_TAG)) {
                ContextConfigurationType ccType = ContextConfigurationType.valueOf(attr.getValue("type"));
                IContextConfigurationFactory fact = appContext.getBean(IContextConfigurationFactory.class);
                this.contextConfig = fact.createContextConfiguration(ccType, true);
                contextConfigParser = new ContextConfigurationParser(contextConfig);
                inContextConfiguration = true;
                contextConfigParser.startElement(uri, localName, qname, attr);
            }
            else if (inContextConfiguration) {
                contextConfigParser.startElement(uri, localName, qname, attr);
            }
        }
    }
}