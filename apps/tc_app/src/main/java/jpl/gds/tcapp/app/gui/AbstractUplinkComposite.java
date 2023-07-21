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
package jpl.gds.tcapp.app.gui;

import java.util.ArrayList;
import java.util.List;

import jpl.gds.tc.api.message.ITransmittableCommandMessage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.springframework.context.ApplicationContext;

import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.swt.ProgressIndicatorDialog;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.tc.api.config.UplinkParseException;
import jpl.gds.tcapp.app.gui.AbstractSendComposite.TransmitListener;

/**
 * Abstract composite with common methods used for chill_up tabs.
 * 
 */
public abstract class AbstractUplinkComposite extends Composite {

	protected static Tracer logger;
	protected java.util.List<TransmitListener> listeners;
	protected UplinkProgressIndicatorDialog indicatorDialog;
	protected ApplicationContext appContext;

	/**
     * Constructor
     * 
     * @param appContext
     *            The current application context
     * 
     * @param parent
     *            the parent composite
     * @param style
     *            the SWT style
     */
	public AbstractUplinkComposite(final ApplicationContext appContext, final Composite parent, final int style) {
		super(parent, style);

		this.appContext = appContext;
		this.listeners = new ArrayList<TransmitListener>();
        logger = TraceManager.getTracer(appContext, Loggers.DEFAULT);
	}

	/**
	 * Initiate uplink
	 * 
	 * @throws UplinkParseException
	 *             if parsing of uplink data fails
	 */
	public abstract void initiateSend() throws UplinkParseException;

	/**
	 * Indicates whether or not this composite needs the common Send button to
	 * be displayed
	 * 
	 * @return true if it needs the common Send button to be displayed, false
	 *         otherwise.
	 */
	public abstract boolean needSendButton();

	/**
	 * Add a listener to be notified when this composite transmits
	 * 
	 * @param listener
	 *            the listener to be notified when this composite transmits
	 */
	public void addTransmitListener(final TransmitListener listener) {
		this.listeners.add(listener);
	}

	/**
	 * Add a listener to be notified when this composite transmits
	 * 
	 * @param listener
	 *            the listener to be notified when this composite transmits
	 */
	public void removeTransmitListener(final TransmitListener listener) {
		this.listeners.remove(listener);
	}

	public void setTransmitListeners(final List<TransmitListener> listeners) {
		this.listeners = listeners;
	}

	/**
	 * Notify listeners of a transmit event
	 * 
	 * @param event
	 *            the TransmitEvent describing the event that occurred
	 */
	protected void notifyListeners(final TransmitEvent event) {
		for (final TransmitListener l : listeners) {
			l.onTransmit(event);
		}
	}
	/**
	 * Notify listeners of a transmit message
	 *
	 * @param event
	 *            the TransmittableCommandMessage describing the event that occurred
	 */
	protected void notifyListeners(final ITransmittableCommandMessage event) {
		for (final TransmitListener l : listeners) {
			l.onTransmit(event);
		}
	}

	/**
	 * Get the display name of this composite
	 * 
	 * @return the display name of this composite
	 */
	public abstract String getDisplayName();

	/**
	 * Repopulate the fields of this composite from a <code>TransmitEvent</code>
	 * 
	 * @param historyItem
	 *            the <code>TransmitEvent</code> that contains the necessary
	 *            information to repopulate the fields of this composite
	 */
	public abstract void setFieldsFromTransmitHistory(TransmitEvent historyItem);

	protected void showProgressIndicatorDialog() {
		SWTUtilities.safeAsyncExec(getDisplay(), "ProgressIndicator",
				new Runnable() {
					@Override
					public void run() {
						if (indicatorDialog == null) {
							indicatorDialog = new UplinkProgressIndicatorDialog(
									AbstractUplinkComposite.this.getShell(),
									"Uplink in Progress",
									"Uplink in progress, please stand by...");
						}

						indicatorDialog.open();
					}
				});
	}

	protected void closeProgressIndicatorDialog() {
		if (!this.isDisposed()) {
			SWTUtilities.safeSyncExec(getDisplay(), "ProgressIndicator",
					new Runnable() {
						@Override
						public void run() {
							if (indicatorDialog != null) {
								indicatorDialog.done();
								indicatorDialog.close();
							}
						}
					});
		}
	}

	protected class UplinkProgressIndicatorDialog extends
			ProgressIndicatorDialog {
		public UplinkProgressIndicatorDialog(final Shell parentShell, final String title,
				final String message) {
			super(parentShell, title, message);
		}

		@Override
		protected void handleShellCloseEvent() {
			SWTUtilities.showWarningDialog(this.getShell(),
					"Please be patient",
					"Please be patient while we uplink your data.");
		}
	}
}
