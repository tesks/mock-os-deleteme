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
package jpl.gds.shared.message;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * A class that represents a message configuration.  Message configurations
 * must be registered in the MessageRegistry to support either use of message
 * templates, or external publication of the message for receipt by clients.
 * 
 *
 * @since R8
 *
 */
public class RegisteredMessageConfiguration implements IMessageConfiguration {

    private final IMessageType regType;
	private final String subscriptionTag;
	private List<String> tagAliases = null;
	private final List<String> templateDirs = new ArrayList<String>();
	private final String externalRootElement;
	private final String xmlParserClass;
	private final String binaryParserClass;
	private final String schemaName;

    /**
     * Constructor for internal messages. The resulting configuration will
     * define no message parsers, so the message cannot be received by message
     * service clients.
     * 
     * @param type
     *            the type of the message
     */
	public RegisteredMessageConfiguration(final IMessageType type) {
	    this (type, null, null);
	}
	
	/**
	 * Constructor.
	 * 
	 * @param type
	 *            the message type
	 * @param xmlParserClass
	 *            the XML parser class name for the message type
	 * @param binaryParserClass
	 *            the binary parser class name for the message type
	 */
	public RegisteredMessageConfiguration(final IMessageType type, final String xmlParserClass,
			final String binaryParserClass) {
	    this(type, 
                MessageRegistry.getDefaultTemplateDir(type), 
                MessageRegistry.getDefaultExternalXmlRoot(type), 
                MessageRegistry.getDefaultSchemaName(type), xmlParserClass,
                binaryParserClass, null);
	}

	/**
     * Constructor.
     * 
     * @param type
     *            the message type
     * @param xmlParserClass
     *            the XML parser class name for the message
     * @param binaryParserClass
     *            the binary parser class name for the message
	 * @param aliases optional array of subscription tag aliases; these will also
	 *        be used as template subdirectory aliases
     */
    public RegisteredMessageConfiguration(final IMessageType type,
            final String xmlParserClass,
            final String binaryParserClass, final String[] aliases) {
        this(type, 
                MessageRegistry.getDefaultTemplateDir(type), 
                MessageRegistry.getDefaultExternalXmlRoot(type), 
                MessageRegistry.getDefaultSchemaName(type), xmlParserClass,
                binaryParserClass, aliases);
    }
    
	/**
	 * Constructor.
	 * 
	 * @param type
	 *            the message type
	 * @param templateDir
	 *            the name of the velocity template sub-directory for the
	 *            message type
	 * @param externalRoot
	 *            the external XML root name for the message type
	 * @param schemaName
	 *            the name of the XML schema for the message
	 * @param xmlParserClass
	 *            the XML parser class name for the message
	 * @param binaryParserClass
	 *            the binary parser class name for the message
	 * @param aliases optional array of subscription tag aliases; these will also
     *        be used as template subdirectory aliases
	 */
	public RegisteredMessageConfiguration(final IMessageType type, final String templateDir,
			final String externalRoot, final String schemaName, final String xmlParserClass,
			final String binaryParserClass, final String[] aliases) {
	    this.regType = type;
		this.subscriptionTag = type.getSubscriptionTag();
		if (templateDir != null) {
		    this.templateDirs.add(templateDir);
		}
		if (aliases != null) {
		    this.tagAliases = Arrays.asList(aliases);
		    for (final String str: tagAliases) {
		        this.templateDirs.add(str);
		    }
		}
		this.externalRootElement = externalRoot;
		this.xmlParserClass = xmlParserClass;
		this.binaryParserClass = binaryParserClass;
		this.schemaName = schemaName;
	}

	@Override
	public String getXmlParserClassName() {
		return xmlParserClass;
	}

	@Override
	public String getBinaryParserClassName() {
		return binaryParserClass;
	}

	@Override
	public String getSubscriptionTag() {
		return subscriptionTag;
	}

	@Override
	public List<String> getTemplateDirs() {
		return templateDirs;
	}

	@Override
	public String getExternalRootElement() {
		return externalRootElement;
	}

	@Override
	public String getSchemaName() {
		return schemaName;
	}
	
	@Override
    public int hashCode() {
	    return regType.hashCode();
	}  
	
	@Override
    public boolean equals(final Object o) {
	    if (o instanceof IMessageType) {
	        return IMessageType.matches(this, (IMessageType)o);
	    } 
	    return false;
	}
	
	@Override
    public String toString() {
	    return getSubscriptionTag();
	}

    @Override
    public IMessageType getMessageType() {
        return this.regType;
    }

    @Override
    public List<String> getSubscriptionTagAliases() {
        return this.tagAliases;
    }

    @Override
    public boolean isTagAliasFor(final String tag) {
        if (tag.equalsIgnoreCase(this.subscriptionTag)) {
            return true;
        }
        if (this.tagAliases != null) {
            for (final String alias: tagAliases) {
                if (tag.equalsIgnoreCase(alias)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean isExternal() {
        return this.xmlParserClass != null || this.binaryParserClass != null;
    }

}