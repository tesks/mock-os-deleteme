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
package jpl.gds.monitor.guiapp.gui;

import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.widgets.Shell;

import jpl.gds.message.api.util.MessageCaptureHandler;

/**
 * CaptureFilterComposite is a composite that contains the 
 * preferences related to message filtering for capture files
 */
public class CaptureFilterComposite extends MessageFilterComposite {


	private final MessageCaptureHandler captureHandler;

	/**
	 * Creates an instance of CaptureFilterComposite.
	 * @param parent the parent Shell for this composite
	 * @param capture the message caoture handler object to configure
	 */
	public CaptureFilterComposite(final Shell parent, final MessageCaptureHandler capture) {
		super(parent);
		this.captureHandler = capture;
		super.setAllowedMessageTypes(null);
		super.setSelectedTypes(captureHandler.getCaptureMessageFiltersAsStrings());
	}


	/**
	 * Refreshes the panel from the global capture object.
	 */
	public void refreshFromData() {
		final List<String> filters = Arrays.asList(captureHandler.getCaptureMessageFiltersAsStrings());
		final boolean all = filters.isEmpty();
		for (int i = 0; i < this.messageTypes.length; i++) {
			this.typeButtons[i].setEnabled(!all);
			this.typeButtons[i].setSelection(all || filters.contains(this.messageTypes[i]));
		}
		this.selectedTypes = captureHandler.getCaptureMessageFiltersAsStrings();
	}

	/**
	 * {@inheritDoc}
	 * Applies control contents and settings to the global capture object.
	 */
	@Override
	public boolean applyChanges() {
		super.applyChanges();
		boolean first = true;
		final StringBuffer filter = new StringBuffer();
		for (int i = 0; i < this.typeButtons.length; i++) {
			if (this.typeButtons[i].getSelection()) {
				if (first) {
					filter.append(this.messageTypes[i]);
					first = false;
				} else {
					filter.append("," + this.messageTypes[i]);
				}
			}
		}
		if (filter.length() == 0) {
			captureHandler.setCaptureMessageFilter("");
		} else {
			captureHandler.setCaptureMessageFilter(filter.toString());
		}
		return true;
	}
}
