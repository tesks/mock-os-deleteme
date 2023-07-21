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
package jpl.gds.shared.template;

import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.spring.context.flag.SseContextFlag;

/**
 * A factory that creates different kinds of template managers configured for the
 * current mission.
 * 
 *
 */
public class MissionConfiguredTemplateManagerFactory {

	/**
	 * Creates a message template manager.
	 * 
	 * @return  MessageTemplateManager
	 * 
	 * @throws TemplateException if there is an issue creating the manager
	 */
    @Deprecated
	public static MessageTemplateManager getNewMessageTemplateManager() throws TemplateException {
		return new MessageTemplateManager(getMission());
	}


    /**
     * Creates a message template manager.
     * 
     * @param sseFlag
     *            the SSE context flag
     * 
     * @return MessageTemplateManager
     * 
     * @throws TemplateException
     *             if there is an issue creating the manager
     */
    public static MessageTemplateManager getNewMessageTemplateManager(final SseContextFlag sseFlag)
            throws TemplateException {
        return new MessageTemplateManager(getMission(sseFlag));
    }
	
	/**
     * Creates a database template manager.
     * 
     * @param sseFlag
     *            the SSE context flag
     * 
     * @return DatabaseTemplateManager
     * 
     * @throws TemplateException
     *             if there is an issue creating the manager
     */
    public static DatabaseTemplateManager getNewDatabaseTemplateManager(final SseContextFlag sseFlag)
            throws TemplateException {
        return new DatabaseTemplateManager(getMission(sseFlag));
	}
	
    /**
     * Creates a dictionary template manager.
     * 
     * @param sseFlag
     *            the SSE context flag
     * 
     * @return DictionaryTemplateManager
     * 
     * @throws TemplateException
     *             if there is an issue creating the manager
     */
    public static DictionaryTemplateManager getNewDictionaryTemplateManager(final SseContextFlag sseFlag)
            throws TemplateException {
        return new DictionaryTemplateManager(getMission(sseFlag));
    }

	/**
     * Creates a basic template manager.
     * 
     * @param sseFlag
     *            the SSE context flag
     * 
     * @return TemplateManager
     * 
     * @throws TemplateException
     *             if there is an issue creating the manager
     */
    public static TemplateManager getNewTemplateManager(final SseContextFlag sseFlag) throws TemplateException {
        return new TemplateManager(getMission(sseFlag));
	}

	/**
     * Creates a product template manager.
     * 
     * @param sseFlag
     *            the SSE context flag
     * 
     * @return productT emplateManager
     * 
     * @throws TemplateException
     *             if there is an issue creating the manager
     */
    public static ProductTemplateManager getNewProductTemplateManager(final SseContextFlag sseFlag)
            throws TemplateException {
        return new ProductTemplateManager(getMission(sseFlag));
	}
	

	
	/**
	 * Returns a new template manager for applications.
	 * 
	 * @param app
	 *            The application object that is going to use this template
	 *            manager. In most cases, the application object should simply
	 *            pass itself ("this") to this method.
	 * @return New instance of ApplicationTemplateManager
	 * @throws TemplateException if there is a problem loading the template engine
	 */
    @Deprecated
	public static ApplicationTemplateManager getNewApplicationTemplateManager(final Object app) throws TemplateException {
		return new ApplicationTemplateManager(getMission(), app);
	}

    /**
     * Returns a new template manager for applications.
     * 
     * @param app
     *            The application object that is going to use this template
     *            manager. In most cases, the application object should simply
     *            pass itself ("this") to this method.
     * @param sseFlag
     *            the current SSE context flag
     * @return New instance of ApplicationTemplateManager
     * @throws TemplateException
     *             if there is a problem loading the template engine
     */
    public static ApplicationTemplateManager getNewApplicationTemplateManager(final Object app,
                                                                              final SseContextFlag sseFlag)
            throws TemplateException {
        return new ApplicationTemplateManager(getMission(sseFlag), app);
    }

    private static String getMission(final SseContextFlag sseFlag) {
        return GdsSystemProperties.getSystemMissionIncludingSse(sseFlag.isApplicationSse());
    }
	
    @Deprecated
	private static String getMission() {
		return GdsSystemProperties.getSystemMissionIncludingSse();
	}
}
