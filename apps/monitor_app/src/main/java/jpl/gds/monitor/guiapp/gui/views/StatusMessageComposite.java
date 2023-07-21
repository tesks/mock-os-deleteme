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
package jpl.gds.monitor.guiapp.gui.views;

import org.springframework.context.ApplicationContext;

import jpl.gds.perspective.view.IViewConfiguration;
import jpl.gds.shared.message.CommonMessageType;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.message.MessageSubscriber;

/**
 * 
 * StatusMessageComposite is a widget that displays different types of
 * "status-related" messages, such as log messages and test control
 * messages.  It is considered a view by the monitor.
 *
 */
public class StatusMessageComposite extends MessageListComposite implements MessageSubscriber {
    /**
     * Status message composite title
     */
    public static final String TITLE = "Status Messages";
    
    private final IMessagePublicationBus bus;
    
    /**
     * 
     * Creates an instance of StatusMessageComposite.
     * @param appContext the current application context
     * @param viewConfig the StatusMessageViewConfiguration object containing display settings
     */
    public StatusMessageComposite(final ApplicationContext appContext, final IViewConfiguration viewConfig) {
        super(appContext, viewConfig);
        this.bus = appContext.getBean(IMessagePublicationBus.class);
    }
    
    /**
     * @{inheritDoc}
     * @see jpl.gds.monitor.guiapp.gui.views.MessageListComposite#getDefaultName()
     */
    @Override
    public String getDefaultName() {
        return TITLE;
    }
    
    /**
     * Creates the controls and composites for this tab display.
     */
    @Override
	protected void createGui() {
    	super.createGui();
    	bus.subscribe(CommonMessageType.Log, this);
    }

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.shared.message.MessageSubscriber#handleMessage(jpl.gds.shared.message.IMessage)
	 */
	@Override
	public void handleMessage(final IMessage message) {
		final IMessage[] msgs = new IMessage[1];
		msgs[0] = message;
		messageReceived(msgs);
	}
}
