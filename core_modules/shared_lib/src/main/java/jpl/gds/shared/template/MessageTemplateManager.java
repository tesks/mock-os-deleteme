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
/*
 * File: MessageTemplateManager.java
 * Created on Dec 1, 2005
 * 
 * Author: Marti DeMore
 *
 */
package jpl.gds.shared.template;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import org.apache.velocity.Template;

import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.message.IMessageConfiguration;
import jpl.gds.shared.message.IMessageType;
import jpl.gds.shared.message.MessageRegistry;
import jpl.gds.shared.spring.BeanUtil;
import jpl.gds.shared.spring.context.flag.SseContextFlag;

/**
 * 
 * MessageTemplateManager is a velocity template manager meant specifically for
 * formatting internal message objects in various ways.
 * 
 */
public class MessageTemplateManager extends TemplateManager {

    /**
     * The subdirectory under the base template directory where message
     * templates can be found.
     */
    public static final String DIRECTORY         = "message";

    /**
     * Message style for formatting external messages. This style must be
     * defined for all messages that are published externally.
     */
    public static final String XML_MESSAGE_STYLE = "Xml";

    /**
     * Creates an instance of MessageTemplateManager.
     *
     * @param mission
     *            Mission name
     *
     * @throws TemplateException
     *             Could not load template
     */
    public MessageTemplateManager(final String mission) throws TemplateException {
        super(mission);
    }

    /**
     * Returns the Velocity template object for the given message subdirectory
     * and formatting style.
     * 
     * @param templateSubdir
     *            the message subdirectory
     * @param style
     *            the string indicating the format style to use
     * @return the Velocity Template object to format the message in the given
     *         style, or null if no such template is found
     * @throws TemplateException
     *             Could not load template
     */
    private Template getTemplateForStyle(final String templateSubdir, final String style) throws TemplateException {

        String name = DIRECTORY + File.separator + templateSubdir + File.separator + style.toLowerCase();
        if (!name.endsWith(EXTENSION)) {
            name += EXTENSION;
        }

        return getTemplate(name);
    }

    /**
     * Returns the Velocity template object for the given message configuration
     * and formatting style.
     * 
     * @param config
     *            the message configuration
     * @param style
     *            the string indicating the format style to use
     * @return the Velocity Template object to format the message in the given
     *         style, or null if no such template is found
     * @throws TemplateException
     *             Could not load template
     */
    public Template getTemplateForStyle(final IMessageConfiguration config, final String style)
            throws TemplateException {
        for (final String subDir : config.getTemplateDirs()) {
            try {
                return getTemplateForStyle(subDir, style);
            } catch (final TemplateException e) {
                continue;
            }
        }
        throw new TemplateException("Unable to locate style template " + style + " for message type "
                + config.getSubscriptionTag());
    }

    /**
     * Returns a list of available style names found in the supplied message
     * subdirectories.
     * 
     * @param templateSubdirs
     *            The message subdirectories to search
     * @return a list of valid style names, or an empty list if none found
     */
    private List<String> getStyleNames(final List<String> templateSubdirs) {
        final FilenameFilter filter = new VelocityFilter(EXTENSION);
        final ArrayList<String> styles = new ArrayList<>();

        final List<String> directories = getTemplateDirectories();
        for (int i = 0; i < directories.size(); i++) {
            for (final String subDir : templateSubdirs) {
                final File dir = new File(directories.get(i) + File.separator + DIRECTORY + File.separator + subDir);
                if (!dir.exists()) {
                    continue;
                }

                final File[] files = dir.listFiles(filter);
                for (int j = 0; j < files.length; j++) {
                    String style = files[j].getName().substring(0, files[j].getName().length() - EXTENSION.length());
                    style = style.toLowerCase();
                    if (!styles.contains(style)) {
                        styles.add(style);
                    }
                }
            }
        }

        return (styles);
    }

    /**
     * Returns a list of available style names for the specified message
     * configuration.
     * 
     * @param msgConfig
     *            the message configuration object
     * @return a list of valid style names, or an empty list if none found
     */
    public List<String> getStyleNames(final IMessageConfiguration msgConfig) {
        return getStyleNames(msgConfig.getTemplateDirs());
    }

    /**
     * Gets the list of template directories, given a list of subdirectories
     * associated with a specific message type
     * 
     * @param templateSubdirs
     *            the list of message subdirectories
     * @return list of directory names
     */
    private List<String> getTemplateDirectories(final List<String> templateSubdirs) {
        final List<String> baseDirs = super.getTemplateDirectories();
        final List<String> localDirs = new ArrayList<>(baseDirs.size());
        for (final String baseDir : baseDirs) {
            for (final String subDir : templateSubdirs)
                localDirs.add(baseDir + File.separator + DIRECTORY + File.separator + subDir);
        }
        return localDirs;
    }

    /**
     * Gets the list of template directories for a specific message
     * configuration object.
     * 
     * @param msgConfig
     *            the message configuration
     * @return list of directory names
     */
    public List<String> getTemplateDirectories(final IMessageConfiguration msgConfig) {
        return getTemplateDirectories(msgConfig.getTemplateDirs());
    }
    

    /**
     * Static method to get list of template directories specifically for output in
     * application help text.
     * 
     * @param type message type to get directories for
     * @return text describing the template directories
     */
    public static String getTemplateDirectoriesForHelp(final IMessageType type) {
        MessageTemplateManager templateManager = null;
        final StringBuilder result = new StringBuilder();
        try {
            try {
                templateManager = MissionConfiguredTemplateManagerFactory.getNewMessageTemplateManager(BeanUtil.getBean(SseContextFlag.class));
            }
            catch (final Exception e) {
                templateManager = MissionConfiguredTemplateManagerFactory.getNewMessageTemplateManager();
            }

            final List<String> directories =
                    templateManager.getTemplateDirectories(MessageRegistry.getMessageConfig(type));

            result.append("\nTemplate directories searched are:\n");
            for (final String d : directories) {
                result.append("   " + d + "\n");
            }
        } catch (final TemplateException e) {
            TraceManager.getDefaultTracer().warn(

                    "Unable to determine template directories\n");
        }
        return result.toString();
    }
    
    /**
     * Returns an array of available template/style names (or empty if there are none)
     * specifically for display in application help text.
     * @param type the message type to get styles for
     * @return array of styles
     */
    public static String[] getTemplateStylesForHelp(final IMessageType type)
    {
        MessageTemplateManager templateManager;
        try
        {
            try {
                templateManager = MissionConfiguredTemplateManagerFactory.getNewMessageTemplateManager(BeanUtil.getBean(SseContextFlag.class));
            }
            catch (final Exception e) {
                templateManager = MissionConfiguredTemplateManagerFactory.getNewMessageTemplateManager();
            }

            return(templateManager.getStyleNames(MessageRegistry.getMessageConfig(type))).toArray(new String[] {});
        }
        catch (final TemplateException e)
        {
            TraceManager.getDefaultTracer().warn("Unable to determine available output formats\n");

        }

        return(new String[0]);
    }
}
