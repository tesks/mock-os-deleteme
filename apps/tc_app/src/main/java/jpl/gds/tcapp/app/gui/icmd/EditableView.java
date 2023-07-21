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
package jpl.gds.tcapp.app.gui.icmd;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ContentViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.springframework.context.ApplicationContext;

import jpl.gds.shared.swt.SWTUtilities;

/**
 * This is an abstract class that defines the basic features of an SWT control
 * that has the ability to pop up a dialog to edit its contents.
 * 
 * @since AMPCS R3
 */
public abstract class EditableView extends ContentViewer {

	protected Group controlsGroup;
	protected Button editButton;
	protected Shell parentShell;
	
	protected final ApplicationContext appContext;

	/**
	 * Constructor
	 * 
	 * @param parent the parent composite
	 * @param style the SWT style
	 */
	public EditableView(ApplicationContext appContext, Composite parent, int style, int editButtonHorizontalColSpan) {
		this.appContext = appContext;
		this.controlsGroup = new Group(parent, SWT.NONE);

		GridLayout gl = new GridLayout();
		gl.horizontalSpacing = 0;
		gl.verticalSpacing = 0;
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		gl.numColumns = 3;

		this.controlsGroup.setLayout(gl);
		
		this.parentShell = parent.getShell();

		this.createControls();
		this.createMasterControls(editButtonHorizontalColSpan);
	}

	protected void createMasterControls(int editButtonHorizontalColSpan) {
		Image editImage = SWTUtilities.createImage(this.getControl()
				.getDisplay(), "jpl/gds/tcapp/icmd/gui/pencil-icon.png");

		Composite buttonComp = new Composite(this.controlsGroup, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		
		buttonComp.setLayout(layout);
		
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalSpan = editButtonHorizontalColSpan;
		buttonComp.setLayoutData(gd);
		
		this.editButton = new Button(buttonComp, SWT.PUSH);
		this.editButton.setImage(editImage);
		this.editButton.setToolTipText("Edit parameters...");
		this.editButton.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				getEditDialog().open();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub

			}
		});
		
		gd = new GridData(SWT.RIGHT, SWT.BOTTOM, true, true);
		this.editButton.setLayoutData(gd);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.Viewer#getControl()
	 */
	@Override
	public Control getControl() {
		return this.controlsGroup;
	}

	protected abstract Dialog getEditDialog();

	protected abstract void createControls();
}
