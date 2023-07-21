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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.velocity.Template;

import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.formatting.SprintfFormat;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.IMessageConfiguration;
import jpl.gds.shared.message.IMessageType;
import jpl.gds.shared.message.MessageRegistry;
import jpl.gds.shared.spring.BeanUtil;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.template.MessageTemplateManager;
import jpl.gds.shared.template.MissionConfiguredTemplateManagerFactory;
import jpl.gds.shared.template.TemplateException;
import jpl.gds.shared.template.TemplateManager;
import jpl.gds.shared.xml.validation.XmlValidator;
import jpl.gds.shared.xml.validation.XmlValidatorFactory;
import jpl.gds.shared.xml.validation.XmlValidatorFactory.SchemaType;

/**
 * Static utility class for performing message-related functions, such as schema 
 * checks and template processing.
 */
public class MessageUtility {
    
    private static MessageTemplateManager templateMgr;
    
    private static Tracer log = TraceManager.getDefaultTracer();
    
    private static HashMap<String, Template> cachedTemplates = new HashMap<>();

    private MessageUtility() {
        // do nothing
    }

    /**
     * Returns the configured list of format styles for the given message types.
     * 
     * @param msgTypes
     *            the internal message types
     * @return a String array of the unique available format styles, or an empty
     *         array if none found
     */
    public static List<String> getMessageStyles(
        final IMessageType[] msgTypes) {
        final SortedSet<String> temp = new TreeSet<>();
        for (int i = 0; i < msgTypes.length; i++) {
            temp.addAll(getMessageStyles(msgTypes[i]));

        }
        return new ArrayList<>(temp);
    }
    
    /**
     * Returns the configured list of format styles for the given message type
     * as a List.
     * 
     * @param type
     *            the internal message type
     * @return a List of the available format styles as Strings, or an empty
     *         List if none found
     */
    public static List<String> getMessageStyles(final IMessageType type) {
        
        try {
            if (templateMgr == null) {
                templateMgr = new MessageTemplateManager(GdsSystemProperties.getSystemMission());
            }
            final IMessageConfiguration mc = MessageRegistry.getMessageConfig(type);
            if (mc == null) {
                throw new IllegalStateException("Attempting to fetch message styles for unregistered message type: " +
                        type.getSubscriptionTag());
            }
            return templateMgr.getStyleNames(mc);
        } catch (final TemplateException e) {
            log.error("Unexpected exception getting message template styles: " + ExceptionTools.getMessage(e), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Determines whether the given formatting style is valid for the given
     * message type by examining the configuration information.
     * 
     * @param type
     *            The IMessageConfiguration object for the message type being
     *            parsed
     * @param style
     *            the formatting style as a String
     * @return true if style is valid; false otherwise
     */
    public static boolean isValidStyle(final IMessageType type,
            final String style) {
        List<String> styles;
        try {
            if (templateMgr == null) {
                templateMgr = new MessageTemplateManager(GdsSystemProperties.getSystemMission());
            }

            final IMessageConfiguration mc = MessageRegistry.getMessageConfig(type);
            if (mc == null) {
                throw new IllegalStateException("Attempting to fetch message styles for unregistered message type " + type.getSubscriptionTag());
            }
            styles = templateMgr.getStyleNames(mc);
        } catch (final TemplateException e) {
            log.error("Unexpected exception getting message template styles: " + ExceptionTools.getMessage(e), e);
            return false;
        }
        return styles.contains(style.toLowerCase());
    }


    /**
     * Gets the velocity-formatted text for a single IMessage.
     * 
     * @param m
     *            the message to format
     * @param style
     *            the message style (template) name
     * @return the formatted String
     * @throws TemplateException
     *             if unable to retrieve template
     */
    public static String getMessageText(final IMessage m, final String style)
            throws TemplateException {
        if (templateMgr == null) {
            try {
                templateMgr = MissionConfiguredTemplateManagerFactory.getNewMessageTemplateManager(BeanUtil.getBean(SseContextFlag.class));
            }
            catch (final Exception e) {
                templateMgr = MissionConfiguredTemplateManagerFactory.getNewMessageTemplateManager();
            }
        }
        final IMessageConfiguration config = MessageRegistry.getMessageConfig(m
                .getType());
        
        if (config == null) {
            return "";
        }

        if (style == null) {
            return m.getOneLineSummary();
        }
        
        Template t = cachedTemplates.get(config.getMessageType().getSubscriptionTag() + style);
        if (t == null) {
            t = templateMgr.getTemplateForStyle(config, style);
            cachedTemplates.put(config.getMessageType().getSubscriptionTag() + style, t);
        }
        final HashMap<String, Object> map = new HashMap<>();
        m.setTemplateContext(map);
        map.put("formatter", new SprintfFormat());
        map.put("body", true);
        return TemplateManager.createText(t, map);
    }

    /**
     * Returns the text body content of all the given IMessages, formatted
     * according to the given style name.
     * 
     * @param messageStyle
     *            the message formatting style; styles are defined by the config
     *            file
     * @param messages
     *            the messages to format
     * @return the message content String
     */
    public static String getMessageText(final String messageStyle,
            final IMessage[] messages) {

        if (messages == null || messages.length == 0) {
            return "";
        }

        final StringBuilder messageText = new StringBuilder(4096);
        try {

            for (int i = 0; i < messages.length; i++) {
                messageText.append(getMessageText(messages[i], messageStyle)
                        + "\n");
            }

        } catch (final Exception e) {
            log.error("Unexpected exception formatting message output: " + ExceptionTools.getMessage(e), e);
            messageText.append("Unable to format message to requested style");
        }

        return messageText.toString();
    }
    
    /**
     * Validates the aggregated XML for a series of messages against the
     * external XML schema.
     * 
     * @param xml
     *            the XML to validate
     * @param messageType
     *            the IMessageConfiguration object for the message type in
     *            question
     * @return true if validation succeeded, false if not
     */
    public static boolean validateAgainstSchema(final String xml,
        final IMessageType messageType) {
        final IMessageConfiguration mc = MessageRegistry.getMessageConfig(messageType);
        return validateAgainstSchemaFile(xml, mc.getSchemaName());
    }

    
    private static boolean validateAgainstSchemaFile(final String xml,
            final String schemaName) {
        final XmlValidator validator = XmlValidatorFactory
                .createValidator(SchemaType.RNC);
        try {
            String schemaLoc = "schema" + File.separator
                    + GdsSystemProperties.getSystemMission() + File.separator
                    + schemaName;
            if (!new File(schemaLoc).exists()) {
                schemaLoc = "schema" + File.separator + schemaName;
            }
            return validator.validateXmlString(schemaLoc, xml);
        } catch (final Exception e) {
            log.error("Unexpected exception validating message against schema: " + ExceptionTools.getMessage(e), e);
            return false;
        }
    }
    
    /**
     * Gets text describing the message template directories, for output in help text.
     * 
     * @return template directory help text
     */
    public static String getTemplateDirectories() {
        TemplateManager templateManager = null;
        final StringBuilder result = new StringBuilder();
        try
        {
            try {
                templateManager = MissionConfiguredTemplateManagerFactory.getNewMessageTemplateManager(BeanUtil.getBean(SseContextFlag.class));
            }
            catch (final Exception e) {
                templateManager = MissionConfiguredTemplateManagerFactory.getNewMessageTemplateManager();
            }

            final List<String> directories = templateManager.getTemplateDirectories();

            result.append("\nTemplate directories searched are:\n");
            for (final String d: directories) {
                result.append("   " + d + "\n");
            }
        }
        catch (final TemplateException e)
        {
            TraceManager.getDefaultTracer().warn("Unable to determine template directories\n");
        }
        return result.toString();
    }

}
