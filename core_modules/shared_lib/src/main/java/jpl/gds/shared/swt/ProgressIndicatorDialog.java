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
package jpl.gds.shared.swt;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ProgressIndicator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * A JFace dialog for displaying progress.
 * 
 *
 */
public class ProgressIndicatorDialog extends Dialog {
	private ProgressIndicator indicator;
	private final String title;
	private final String message;

	/**
	 * Constructor.
	 * 
	 * @param parentShell the parent shell for the dialog
	 * @param title the title bar text for the dialog
	 * @param message the test with which to label the progress bar
	 */
	public ProgressIndicatorDialog(Shell parentShell, String title, String message) {
		super(parentShell);
		setShellStyle(SWT.TITLE | SWT.APPLICATION_MODAL);
		this.title = title;
		this.message = message;
	}

	/**
	 * {@inheritDoc}
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Control c = super.createDialogArea(parent);

		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.verticalSpacing = 10;

		parent.setLayout(layout);

		Label message = new Label(parent, SWT.NONE);
		message.setText(this.message);

		indicator = new ProgressIndicator(parent);
		indicator.beginAnimatedTask();
		indicator.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));

		return c;
	}

	/**
	 * {@inheritDoc}
	 * @see org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets.Shell)
	 */
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(title);
	}

	/**
	 * {@inheritDoc}
	 * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// we don't want buttons
	}

	/**
	 * Sets the progress indicator to the 100% done state.
	 */
	public void done() {
		if (indicator != null && !indicator.isDisposed()) {
			indicator.done();
		}
	}
}