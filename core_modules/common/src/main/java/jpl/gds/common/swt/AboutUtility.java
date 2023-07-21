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
package jpl.gds.common.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import jpl.gds.common.config.GeneralProperties;
import jpl.gds.shared.config.ReleaseProperties;

/**
 * Static GUI methods for display of information about the current
 * AMPCS release.
 *
 */
public class AboutUtility {
 
    /**
     * Shows a standard AMPCS "About" dialog.
     * 
     * @param parent
     *            the parent Shell for the dialog
     * @param genProps the current GeneralProperties object
     */
    public static void showStandardAboutDialog(final Shell parent, GeneralProperties genProps) {

        final MessageBox msgDialog = new MessageBox(parent,
                SWT.ICON_INFORMATION | SWT.OK);
        final String product = ReleaseProperties.getProductLine();
        msgDialog.setText("About " + product);
        final String version = ReleaseProperties.getVersion();
        final String message = product + " version " + version
                + "\n\nTo report a bug, please " + "email " + genProps.getHelpAddress()
                + " and include " + product
                + " version with problem description,\nor file a Jira at "
                + genProps.getBugAddress();
        msgDialog.setMessage(message);
        msgDialog.open();
    }


}
