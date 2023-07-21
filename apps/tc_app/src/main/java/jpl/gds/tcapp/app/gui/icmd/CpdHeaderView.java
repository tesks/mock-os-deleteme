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


import org.eclipse.jface.viewers.ContentViewer;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import jpl.gds.common.config.security.AccessControlParameters;
import jpl.gds.tcapp.app.gui.icmd.model.CpdParametersModel;

public class CpdHeaderView extends ContentViewer implements HeaderManager {
	
	private final Composite headerComposite;
	private final Label cpdHostLabel;
	private final Label stationLabel;
	private final Label roleLabel;
	private final Label cpdHostText;
	private final Label stationText;
	private final Label roleText;
	
	public CpdHeaderView(final AccessControlParameters accessParams, final Composite parent, final int style) {
		headerComposite = new Composite(parent, SWT.NONE);
		final GridLayout gl = new GridLayout();
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		headerComposite.setLayout(gl);
		
		final Composite innerComp = new Composite(headerComposite, SWT.NONE);
		final GridData gd = new GridData(SWT.CENTER, SWT.CENTER, true, true);
		innerComp.setLayoutData(gd);
		
		final FormLayout fl = new FormLayout();
		fl.spacing = 5;
		fl.marginHeight = 5;
		fl.marginWidth = 5;

		innerComp.setLayout(fl);
		
		// make labels
		this.cpdHostLabel = new Label(innerComp, SWT.NONE);
		this.stationLabel = new Label(innerComp, SWT.NONE);
		this.roleLabel = new Label(innerComp, SWT.NONE);
	
		this.cpdHostLabel.setText("CPD Host:");
		this.stationLabel.setText("Connected To:");
		this.roleLabel.setText("Role:");
	
		final String role = accessParams.getUserRole().toString();
	
		this.cpdHostText = new Label(innerComp, SWT.NONE);
		final FontData titleFontData = this.cpdHostText.getFont().getFontData()[0];
		final Font titleFont = new Font(innerComp.getShell()
				.getDisplay(), new FontData(titleFontData.getName(), titleFontData
				.getHeight(), SWT.BOLD));

		this.cpdHostText.setFont(titleFont);
	
		this.stationText = new Label(innerComp, SWT.NONE);
		//this.stationText.setText(station);
		this.stationText.setFont(titleFont);
		//TODO add listener this.stationText.add
		this.roleText = new Label(innerComp, SWT.NONE);
		this.roleText.setText(role);
		this.roleText.setFont(titleFont);
	
		// lay everything out
		final FormData hostLabelFd = new FormData();
		hostLabelFd.top = new FormAttachment(0);
		hostLabelFd.left = new FormAttachment(0);
		this.cpdHostLabel.setLayoutData(hostLabelFd);
	
		final FormData hostTextFd = new FormData();
		hostTextFd.top = new FormAttachment(0);
		hostTextFd.left = new FormAttachment(this.cpdHostLabel, 0);
		this.cpdHostText.setLayoutData(hostTextFd);
	
		final FormData stationLabelFd = new FormData();
		stationLabelFd.top = new FormAttachment(0);
		stationLabelFd.left = new FormAttachment(this.cpdHostText, 0);
		this.stationLabel.setLayoutData(stationLabelFd);
	
		final FormData stationTextFd = new FormData();
		stationTextFd.top = new FormAttachment(0);
		stationTextFd.left = new FormAttachment(this.stationLabel, 0);
		this.stationText.setLayoutData(stationTextFd);
	
		final FormData roleLabelFd = new FormData();
		roleLabelFd.top = new FormAttachment(0);
		roleLabelFd.left = new FormAttachment(this.stationText, 0);
		this.roleLabel.setLayoutData(roleLabelFd);
	
		final FormData roleTextFd = new FormData();
		roleTextFd.top = new FormAttachment(0);
		roleTextFd.left = new FormAttachment(this.roleLabel, 0);
		this.roleText.setLayoutData(roleTextFd);
	}
	
	public void setHostText(final String host) {
		this.cpdHostText.setText(host);
		this.headerComposite.layout();
	}

	@Override
	public Control getControl() {
		return headerComposite;
	}

	@Override
	public Object getInput() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ISelection getSelection() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void refresh() {
		final IContentProvider provider = this.getContentProvider();

		if (provider instanceof CpdParametersModel) {
			final CpdParametersModel model = (CpdParametersModel) provider;

			final String connectedStation = model.getConnectedStation();
			String stationStr = connectedStation == null || connectedStation.equalsIgnoreCase("") ? "UNKNOWN" : connectedStation;
			
			if(stationStr.equals("DISCONNECTED")) {
				stationStr = "NONE";
			}
			
			this.stationText.setText(stationStr);
			
//			CommandUserRole role = model.getRole();
//			String roleString = role == null ? "UNKNOWN" : role.toString();
//			this.roleText.setText(roleString);

//			AggregationMethod aggMethod = model.get;
//			String aggMethodStr = aggMethod == null ? "UNKNOWN" : aggMethod
//					.toString();
//
//			this.aggregationMethodValue.getLabelWidget().setText(aggMethodStr);
//			this.controlsGroup.layout();
		} else {
			return;
		}

		this.headerComposite.layout();
	}

	@Override
	public void setSelection(final ISelection arg0, final boolean arg1) {
		// TODO Auto-generated method stub
		
	}

	
	@Override
	public void sendRoleConfigurationChange(final String role) {
		this.roleText.setText(role);
		this.headerComposite.layout();
	}


}
