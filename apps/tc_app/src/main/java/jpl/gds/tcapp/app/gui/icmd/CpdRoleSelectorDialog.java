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

import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import jpl.gds.common.config.security.SecurityProperties;
import jpl.gds.common.config.types.CommandUserRole;

public class CpdRoleSelectorDialog extends Dialog {
	/** ALL roles label */
	private static final String ALL_ROLES = "ALL";
	
	/** The title of the dialog */
	private final String title;
	
	/** The message of the dialog */
	private final String message;
	
	/** The combo selector with all the roles */
	private Combo roleSelector;
	
	/** The selected role */
	private CommandUserRole selectedRole;

	private final SecurityProperties securityProperties;

	/**
	 * Constructor
	 * @param parentShell the parent shell
	 * @param title the title of the dialog
	 * @param message the message of the dialog
	 */
	protected CpdRoleSelectorDialog(SecurityProperties secProps, Shell parentShell, String title, String message) {
		super(parentShell);
		
		this.securityProperties = secProps;
		this.title = title;
		this.message = message;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets.Shell)
	 */
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(this.title);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		final Composite c = (Composite) super.createDialogArea(parent);
		
		final Label messageLabel = new Label(c, SWT.NONE);
		messageLabel.setText(this.message);
		
		GridData gd = new GridData(SWT.CENTER, SWT.CENTER, true, false);
		messageLabel.setLayoutData(gd);
		
		roleSelector = new Combo(c, SWT.READ_ONLY);
		roleSelector.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				CpdRoleSelectorDialog.this.setSelectedRole();
			}
		});
		
		gd = new GridData(SWT.CENTER, SWT.CENTER, true, false);
		roleSelector.setLayoutData(gd);

        // It makes no sense to flush VIEWER, since there is no such queue

		final Set<CommandUserRole> roleSet =
            new TreeSet<CommandUserRole>(securityProperties.getRoles());

        roleSet.remove(CommandUserRole.VIEWER);
		
		roleSelector.add(ALL_ROLES);
		
		for(final CommandUserRole r : roleSet) {
			roleSelector.add(r.toString());
		}
		
		roleSelector.select(0);
		
		return c;
	}
	
	private void setSelectedRole() {
		CommandUserRole selectedRole = null;
		
		if(this.roleSelector != null) {
			final int selectedIndex = this.roleSelector.getSelectionIndex();
			
			final String selectedRoleStr = this.roleSelector.getItem(selectedIndex);
			
			if(!selectedRoleStr.equals(ALL_ROLES)) {
				selectedRole = CommandUserRole.valueOf(selectedRoleStr);
			}
		}
		
		this.selectedRole = selectedRole;
	}
	
	/**
	 * Get the selected role
	 * @return the selected role
	 */
	public CommandUserRole getSelectedRole() {
		return this.selectedRole;
	}
}
