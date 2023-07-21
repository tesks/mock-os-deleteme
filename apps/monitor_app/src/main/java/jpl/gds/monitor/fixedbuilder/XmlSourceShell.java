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
package jpl.gds.monitor.fixedbuilder;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.swt.ChillShell;

/**
 * This GUI window displays the XML source in the fixed view builder. It also
 * allows the user to copy content between the XML source and the canvas.
 */
public class XmlSourceShell implements ChillShell {
    /**
     * Title on the XML source window
     */
    public static final String TITLE = "XML Source";
    private final static int TEXT_SHELL_HEIGHT = 500;
    private final static int TEXT_SHELL_WIDTH = 800;

    // GUI components
    private Shell mainShell = null;
    private Shell parent = null;
    private Text mainText;

    // The parent fixed builder window
    private final FixedBuilderShell builder;

    /**
     * Creates an XmlSourceShell with the given FixedBuilderShell as its parent.
     * 
     * @param builder
     *            the parent FixedBuilderShell
     */
    public XmlSourceShell(final FixedBuilderShell builder) {
        this.parent = builder.getShell();
        this.builder = builder;
        createGui();
    }

    /**
     * Sets the XML text to be displayed in this window.
     * 
     * @param text
     *            the XML text to display
     */
    public void setXmlText(final String text) {
        this.mainText.setText(text);
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.swt.ChillShell#getShell()
     */
    @Override
    public Shell getShell() {
        return this.mainShell;
    }

    // Main entry point for GUI creation
    private void createGui() {
        try {
            this.mainShell = new Shell(this.parent, SWT.DIALOG_TRIM
                    | SWT.RESIZE);
            this.mainShell.setSize(TEXT_SHELL_WIDTH, TEXT_SHELL_HEIGHT);
            final FormLayout shellLayout = new FormLayout();
            shellLayout.spacing = 5;
            shellLayout.marginHeight = 2;
            shellLayout.marginWidth = 2;
            this.mainShell.setLayout(shellLayout);

            this.mainText = new Text(this.mainShell, SWT.BORDER | SWT.V_SCROLL
                    | SWT.H_SCROLL);
            final FormData fd = new FormData();
            fd.top = new FormAttachment(0);
            fd.bottom = new FormAttachment(90);
            fd.left = new FormAttachment(0);
            fd.right = new FormAttachment(100);
            this.mainText.setLayoutData(fd);

            this.mainText.setText("Temp");
            this.mainShell.setLocation(this.parent.getLocation().x + 50,
                    this.parent.getLocation().y + 50);

            final Composite buttonComp = new Composite(this.mainShell, SWT.NONE);
            buttonComp.setLayout(new GridLayout(3, false));
            final FormData fdb = new FormData();
            fdb.top = new FormAttachment(this.mainText);
            fdb.right = new FormAttachment(100);
            fdb.bottom = new FormAttachment(100);
            buttonComp.setLayoutData(fdb);

            final Button refreshButton = new Button(buttonComp, SWT.PUSH);
            refreshButton.setText("Update from Canvas");
            refreshButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(
                        final org.eclipse.swt.events.SelectionEvent e) {
                    try {
                        setXmlText(XmlSourceShell.this.builder.getCurrentXml());
                    } catch (final Exception eE) {
                        TraceManager

                                .getDefaultTracer()
                                .error(
                                        "refresh button caught unhandled an unexpected exception in XmlSourceShell.java");
                        eE.printStackTrace();
                    }
                }
            });

            final Button putButton = new Button(buttonComp, SWT.PUSH);
            putButton.setText("Write to Canvas");
            putButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(
                        final org.eclipse.swt.events.SelectionEvent e) {
                    try {
                        XmlSourceShell.this.builder
                                .setCurrentXml(XmlSourceShell.this.mainText
                                        .getText());
                    } catch (final Exception eE) {
                        TraceManager

                                .getDefaultTracer()
                                .error(
                                        "put button caught unhandled an unexpected exception in XmlSourceShell.java");
                        eE.printStackTrace();
                    }
                }
            });

            final Button closeButton = new Button(buttonComp, SWT.PUSH);
            closeButton.setText("Close");

            closeButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(
                        final org.eclipse.swt.events.SelectionEvent e) {
                    try {
                        XmlSourceShell.this.mainShell.close();
                    } catch (final Exception eE) {
                        TraceManager

                                .getDefaultTracer()
                                .error(
                                        "close button caught unhandled an unexpected exception in TextViewShell.java");
                        eE.printStackTrace();
                    }
                }
            });
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.swt.ChillShell#open()
     */
    @Override
    public void open() {
        this.mainShell.open();
    }

    /**
     * Closes this shell.
     */
    public void close() {
        this.mainShell.close();
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.swt.ChillShell#getTitle()
     */
    @Override
    public String getTitle() {
        return TITLE;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.swt.ChillShell#wasCanceled()
     */
    @Override
    public boolean wasCanceled() {
        return false;
    }
}
