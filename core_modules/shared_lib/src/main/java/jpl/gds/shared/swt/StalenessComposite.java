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

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * This is a common GUI panel for allowing the user to enter a data staleness
 * interval im seconds.
 * 
 *
 */
public class StalenessComposite {

    private final Composite parent;
    private Text stalenessText;
    private Composite stalenessComposite;

    /**
     * Constructor.
     * 
     * @param parent
     *            the parent Composite on which to create this composite panel
     */
    public StalenessComposite(final Composite parent) {

        this.parent = parent;
        createControls();
    }

    /**
     * Gets the underlying SWT composite object, so that it can be used to
     * establish layout.
     * 
     * @return SWT Composite object
     */
    public Composite getComposite() {

        return this.stalenessComposite;
    }

    /**
     * Creates and draws the GUI components.
     */
    private void createControls() {

        this.stalenessComposite = new Group(this.parent, SWT.NULL);
        final FormLayout fl1 = new FormLayout();
        fl1.spacing = 7;
        fl1.marginTop = 5;
        fl1.marginLeft = 5;
        fl1.marginBottom = 5;
        this.stalenessComposite.setLayout(fl1);

        final Label staleLabel = new Label(this.stalenessComposite, SWT.NONE);
        staleLabel.setText("Staleness Interval:");
        final FormData slfd = new FormData();
        slfd.top = new FormAttachment(0, 3);
        slfd.left = new FormAttachment(0);
        staleLabel.setLayoutData(slfd);

        this.stalenessText = new Text(this.stalenessComposite, SWT.BORDER);
        final FormData ssfd = SWTUtilities.getFormData(this.stalenessText, 1,
                10);
        ssfd.top = new FormAttachment(staleLabel, 0, SWT.CENTER);
        ssfd.left = new FormAttachment(staleLabel, 0, 3);
        this.stalenessText.setLayoutData(ssfd);

        final Label secondsLabel = new Label(this.stalenessComposite, SWT.NONE);
        secondsLabel.setText(" seconds");
        final FormData slfd2 = new FormData();
        slfd2.top = new FormAttachment(0, 3);
        slfd2.left = new FormAttachment(this.stalenessText, 0, 3);
        secondsLabel.setLayoutData(slfd2);

    }

    /**
     * Gets the staleness interval entered by the user.
     * 
     * @return staleness interval in seconds
     * @throws NumberFormatException if the entered value is not a number >= 0
     */
    public int getStalenessInterval() throws NumberFormatException {

        if (this.stalenessText.isDisposed()) {
            return 0;
        }

        final String comboVal = this.stalenessText.getText();

        final int intVal = Integer.valueOf(comboVal);
        if (intVal < 0) {
            throw new NumberFormatException("Staleness < 0");
        }
        return intVal;
    }

    /**
     * Sets the staleness interval to display in this panel.
     * 
     * @param interval staleness interval in seconds
     */
    public void setStalenessInterval(final int interval) {

        if (this.stalenessText.isDisposed()) {
            return;
        }

        this.stalenessText.setText(String.valueOf(interval));

    }
}
