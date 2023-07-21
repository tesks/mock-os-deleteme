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
/*
 * Title: TextViewShell.java
 * 
 * Author: dan
 * Created: May 2, 2006
 * 
 */
package jpl.gds.shared.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import jpl.gds.shared.log.Tracer;


/**
 * This class is a general purpose GUI shell for displaying a block of text.
 * 
 */
public class TextViewShell implements ChillShell {
    private static final String TITLE = "Text";
    private Shell mainShell = null;
    private Shell parent = null;
    private final static int TEXT_SHELL_HEIGHT = 500;
    private final static int TEXT_SHELL_WIDTH = 800;
    private Text mainText;
    private String title = TITLE;
    private final int textFlags;
    private Tracer              trace;

    /**
     * Creates a TextViewShell with the given shell as its parent. By default,
     * has title "Text" and both horizontal and vertical scroll bars, but does
     * not wrap.
     * 
     * @param parent
     *            the parent Shell widget
     * @param trace
     *            Tracer logger
     */
    public TextViewShell(final Shell parent, Tracer trace) {
        this(parent, TITLE, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL, trace);
    }
    
    /**
     * Creates a TextViewShell with the given shell as its parent.
     * 
     * @param parent
     *            the parent Shell widget
     * @param title
     *            title text for the window
     * @param textFlags
     *            SWT flags for the text widget
     * 
     * @param trace
     *            Tracer logger
     */
    public TextViewShell(final Shell parent, String title, int textFlags, Tracer trace) {
        this.parent = parent;
        this.title = title;
        this.textFlags = textFlags;
        createGui();
    }

    /**
     * Closes this shell.
     */
    public void close() {
        mainShell.close();
    }

    private void createGui() {
        mainShell = new Shell(parent, SWT.DIALOG_TRIM | SWT.RESIZE);
        mainShell.setSize(TEXT_SHELL_WIDTH, TEXT_SHELL_HEIGHT);
        mainShell.setText(this.title);
        final FormLayout shellLayout = new FormLayout();
        shellLayout.spacing = 5;
        shellLayout.marginHeight = 2;
        shellLayout.marginWidth = 2;
        mainShell.setLayout(shellLayout);

        mainText = new Text(
                mainShell, textFlags);
        final FormData fd = new FormData();
        fd.top = new FormAttachment(0);
        fd.bottom = new FormAttachment(90);
        fd.left = new FormAttachment(0);
        fd.right = new FormAttachment(100);
        mainText.setLayoutData(fd);

        mainText.setText("Temp");
        mainShell.setLocation(parent.getLocation().x + 50,
                parent.getLocation().y + 50);

        final Button closeButton = new Button(mainShell, SWT.PUSH);
        closeButton.setText("Close");
        final FormData fdb = new FormData();
        fdb.top = new FormAttachment(mainText);
        fdb.right = new FormAttachment(100);
        fdb.bottom = new FormAttachment(100);
        closeButton.setLayoutData(fdb);

        closeButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(
                    final org.eclipse.swt.events.SelectionEvent e) {
                try {
                    mainShell.close();
                } catch (final Exception eE) {
                    trace.error(
                                    "close button caught unhandled an " +
                                    "unexpected exception in " +
                                    "TextViewShell.java");
                    eE.printStackTrace();
                }
            }
        });
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.swt.ChillShell#getShell()
     */
    @Override
	public Shell getShell() {
        return mainShell;
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
     * @see jpl.gds.shared.swt.ChillShell#open()
     */
    @Override
	public void open() {
        mainShell.open();
    }

    /**
     * Sets the text to be displayed in this window.
     * 
     * @param text
     *            the text to display
     */
    public void setText(final String text) {
        mainText.setText(text);
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
