/*
 * Copyright 2006-2019. California Institute of Technology.
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
package jpl.gds.tcapp.app.gui;

import jpl.gds.tc.api.message.ITransmittableCommandMessage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.springframework.context.ApplicationContext;

import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.tc.api.config.UplinkParseException;
import jpl.gds.tc.api.message.ICommandMessageFactory;

/**
 * The AbstractSendComposite is an abstract GUI class for the various chill_up
 * tabs that are used to take a file from the file system and transmit it to
 * another entity in some way
 * 
 */
public abstract class AbstractSendComposite extends AbstractUplinkComposite {
	protected final Composite parent;
	protected Shell sendShell;
	protected final SWTUtilities util;
	protected boolean canceled;

	protected Button exitButton;

	protected IMessagePublicationBus bus;
	protected ICommandMessageFactory messageFactory;
	private final boolean doExit;

	/**
	 * Primary constructor. Allows use of the exit button
	 * @param appContext the current application context
	 * @param parent the parent GUI Composite object
	 */
	public AbstractSendComposite(final ApplicationContext appContext, final Composite parent) {
		this(appContext, parent, true);
	}

	/**
	 * Constructor that allows an exit button to be included or not
	 * @param appContext the current application context
	 * @param parent the parent GUI Composite object
	 * @param doExit boolean indicating if the exit button should be included.
	 */
	public AbstractSendComposite(final ApplicationContext appContext, final Composite parent, final boolean doExit) {
		super(appContext, parent, SWT.NONE);
		
		this.bus = appContext.getBean(IMessagePublicationBus.class);
		this.messageFactory = appContext.getBean(ICommandMessageFactory.class);

		this.parent = parent;
		this.util = new SWTUtilities();
		this.canceled = false;

		this.sendShell = parent.getShell();
		this.exitButton = null;

		this.doExit = doExit;

		createControls();
	}

	/**
	 * Create all of the GUI elements
	 */
	protected void createControls() {
		final Control bottomControl = createBodyControls();
		final EventHandler handler = getEventHandler();

		if (doExit) {
			this.exitButton = new Button(this, SWT.PUSH);
			this.exitButton.setText("Exit");
			final FormData cbFormData = new FormData();
			cbFormData.top = new FormAttachment(bottomControl, 5);
			cbFormData.left = new FormAttachment(60);
			cbFormData.right = new FormAttachment(80, -5);
			this.exitButton.setLayoutData(cbFormData);
			this.exitButton.addSelectionListener(handler);
		}
	}

	/**
	 * Create the GUI elements that allow the user to configure what will be sent
	 * @return the Control of the last element
	 */
	protected abstract Control createBodyControls();

	/*
	 * (non-Javadoc)
	 * 
	 * @see jpl.gds.swt.common.ChillShell#getShell()
	 */
	@Override
	public Shell getShell() {
		return this.sendShell;
	}

	/**
	 * The action to be performed when the object is to be sent
	 */
	protected abstract void send();

	/**
	 * Get the event handler
	 * @return the tab's event handler
	 */
	protected abstract EventHandler getEventHandler();

	/**
	 * The EventHandler class for AbstractSendComposite handles all GUI driven
	 * events caused by the user interacting with the GUI 
	 * 05/08/19 - changed class to public and
	 *   constructor to protected to allow it to be visible to outside classes.
	 */
	public abstract class EventHandler extends SelectionAdapter implements
			KeyListener {
		/**
		 * default constructor
		 */
		protected EventHandler() {
			super();
		}

		@Override
		public void widgetSelected(final SelectionEvent se) {
			try {
				if (se.getSource() == exitButton) {
					canceled = true;
				}
			} catch (final Exception e) {
				e.printStackTrace();
				SWTUtilities.showErrorDialog(sendShell, "Execution Error",
						e.getMessage());
			}
		}

		@Override
		public void keyReleased(final KeyEvent arg0) {

		}
	}

	public interface TransmitListener {
		public void onTransmit(TransmitEvent event);
		public void onTransmit(ITransmittableCommandMessage event);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jpl.gds.tcapp.app.gui.AbstractUplinkComposite#initiateSend()
	 */
	@Override
	public void initiateSend() throws UplinkParseException {
		send();
	}
}
