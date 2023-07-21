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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * Small composite that allows user to enter title and enable/disable its display.
 * 
 *
 */
public class TitleComposite {

    private Composite mainComposite;
    private final Composite parent;
    private Text titleText;

    private Button enableButton;

    /**
     * Creates a TitleComposite.
     * 
     * @param parent the parent composite for this one
     */
    public TitleComposite(final Composite parent) {
        this.parent = parent;
        createControls();
    }

    private void createControls() {

        this.mainComposite = new Composite(this.parent, SWT.NONE);
        final FormLayout fl = new FormLayout();
        fl.marginHeight = 5;
        fl.marginWidth = 5;
        fl.spacing = 5;
        this.mainComposite.setLayout(fl);

        final Label titleLabel = new Label(this.mainComposite, SWT.NONE);
        titleLabel.setText("View Title: ");
        titleLabel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_END));
        final FormData fdLabel2 = new FormData();
        fdLabel2.top = new FormAttachment(0, 10);
        fdLabel2.left = new FormAttachment(0);
        titleLabel.setLayoutData(fdLabel2);
        titleText = new Text(this.mainComposite, SWT.BORDER);
        final FormData fdTitle2 = SWTUtilities.getFormData(titleText, 1, 25);
        fdTitle2.top = new FormAttachment(titleLabel, 0, SWT.CENTER);
        fdTitle2.left = new FormAttachment(titleLabel);
        titleText.setLayoutData(fdTitle2);

        enableButton = new Button(this.mainComposite, SWT.CHECK);
        enableButton.setText("Enable Title");
        final FormData fdEnable = new FormData();
        fdEnable.top = new FormAttachment(titleLabel, 0, SWT.CENTER);
        fdEnable.left = new FormAttachment(titleText, 0, 10);
        fdEnable.right = new FormAttachment(100);
        enableButton.setLayoutData(fdEnable);

    }

    /**
     * Gets the main composite
     * 
     * @return main composite in this class
     */
    public Composite getComposite() {
        return this.mainComposite;
    }

    /**
     * Gets the title that is currently entered in the text field
     * 
     * @return Returns the currentTitle.
     */
    public String getCurrentTitle() {
        return this.titleText.getText();
    }

    /**
     * Checks if the title feature is enabled
     * 
     * @return true if title is enabled, false otherwise
     */
    public boolean isEnableTitle() {
        return this.enableButton.getSelection();
    }

    /**
     * Sets the currentTitle
     * 
     * @param text
     *            The title text to set.
     */
    public void setCurrentTitle(final String text) {
        titleText.setText(text);
    }

    /**
     * Sets the enable check box in the composite
     * 
     * @param enable is true if check box should be checked, false otherwise
     */
    public void setEnableTitle(final boolean enable) {
        this.enableButton.setSelection(enable);
    }
}
