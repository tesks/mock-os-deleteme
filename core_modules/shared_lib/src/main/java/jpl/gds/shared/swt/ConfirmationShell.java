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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;

/**
 * ConfirmationShell is a GUI window that allows a user to answer a
 * yes or no question and also indicate whether to ask the question again next
 * time. If the answer is no, then the "prompt again" setting is always true.
 * 
 *
 */
public class ConfirmationShell implements ChillShell {
    private static final String TITLE = "Confirmation";
    private Shell promptShell;
    private final Display parent;
    private final String question;
    // set default to cancelled
    private boolean cancelled = true;
    private boolean promptAgain = true;
    private Button dontAsk;
    private final boolean useRememberPrompt;

    private final Tracer        trace;

    /**
     * Creates an instance of ConfirmationShell.
     *
     * @param parent
     *            the parent Shell
     * @param prompt
     *            the prompt text to display as the message in the window
     * @param useRemember
     *            sets the flag indicating whether the " do not ask again"
     *            prompt just says "do not ask again" or also includes "remember
     *            my answer"
     */
    public ConfirmationShell(final Shell parent, final String prompt,
            final boolean useRemember) {
        this.parent = parent.getDisplay();
        this.trace = TraceManager.getDefaultTracer();
        question = prompt;
        useRememberPrompt = useRemember;
        createControls();
    }

    /**
     * Creates an instance of ConfirmationShell.
     *
     * @param parent
     *            the parent Display
     * @param prompt
     *            the prompt text to display as the message in the window
     * @param useRemember
     *            sets the flag indicating whether the " do not ask again"
     *            prompt just says "do not ask again" or also includes "remember
     *            my answer"
     */
    public ConfirmationShell(final Display parent, final String prompt,
                             final boolean useRemember) {
        this.parent = parent;
        this.trace = TraceManager.getDefaultTracer();
        question = prompt;
        useRememberPrompt = useRemember;
        createControls();
    }

    /**
     * Creates the GUI controls.
     */
    protected void createControls() {
        promptShell = new Shell(
                parent, SWT.APPLICATION_MODAL | SWT.DIALOG_TRIM);
        promptShell.setText(TITLE);
        final FormLayout shellLayout = new FormLayout();
        shellLayout.spacing = 10;
        shellLayout.marginHeight = 5;
        shellLayout.marginWidth = 5;
        promptShell.setLayout(shellLayout);

        final Label userLabel = new Label(promptShell, SWT.WRAP);
        userLabel.setText(question);
        final FormData fd18 = new FormData();
        fd18.top = new FormAttachment(0, 10);
        fd18.left = new FormAttachment(0, 10);
        userLabel.setLayoutData(fd18);

        dontAsk = new Button(promptShell, SWT.CHECK);
        if (useRememberPrompt) {
            dontAsk.setText("Remember my answer and do not ask this "
                    + "question again");
        } else {
            dontAsk.setText("Do not ask this question again");
        }
        dontAsk.setSelection(false);
        final FormData fd1 = new FormData();
        fd1.top = new FormAttachment(userLabel);
        fd1.left = new FormAttachment(0, 10);
        dontAsk.setLayoutData(fd1);

        final Label line = new Label(promptShell, SWT.SEPARATOR
                | SWT.HORIZONTAL | SWT.SHADOW_ETCHED_IN);
        final FormData formData6 = new FormData();
        formData6.left = new FormAttachment(0, 3);
        formData6.right = new FormAttachment(100, 3);
        formData6.top = new FormAttachment(dontAsk);

        final Composite composite = new Composite(promptShell, SWT.NONE);
        final GridLayout rl = new GridLayout(2, true);
        composite.setLayout(rl);
        final FormData formData8 = new FormData();
        formData8.right = new FormAttachment(100);
        formData8.bottom = new FormAttachment(100);
        composite.setLayoutData(formData8);

        formData6.bottom = new FormAttachment(composite, 5);

        final Button yesButton = new Button(composite, SWT.PUSH);
        yesButton.setText("Yes");
        final GridData gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        yesButton.setLayoutData(gd);
        final Button noButton = new Button(composite, SWT.PUSH);
        noButton.setText("No");
        promptShell.setDefaultButton(noButton);

        line.setLayoutData(formData6);

        yesButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(
                    final org.eclipse.swt.events.SelectionEvent e) {
                try {
                    promptAgain = !dontAsk.getSelection();
                    cancelled = false;
                    promptShell.close();
                } catch (final Exception eE) {
                    trace.error(
                            "YES button caught unhandled and "
                                    + "unexpected exception.");
                    eE.printStackTrace();
                }
            }
        });

        noButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(
                    final org.eclipse.swt.events.SelectionEvent e) {
                try {
                    promptAgain = !dontAsk.getSelection();
                    cancelled = true;
                    promptShell.close();
                } catch (final Exception eE) {
                    trace.error(
                            "NO button caught unhandled and "
                                    + "unexpected exception.");
                    eE.printStackTrace();
                }
            }
        });
        final Rectangle r = parent.getClientArea();

        promptShell.setLocation(r.x + 50, r.y + 50);
        promptShell.pack();
    }

    /**
     * Gets the value of the prompt again flag, indicating whether the user
     * wants to see this question again.
     * 
     * @return true if the question should be asked again next time, or false 
     *         if not
     */
    public boolean getPromptAgain() {
        return promptAgain;
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public Shell getShell() {
        return promptShell;
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public String getTitle() {
        return TITLE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public void open() {
        promptShell.open();
    }

    /**
     * {@inheritDoc}
     *
     * Indicates if a "no" answer was selected by the user.
     * 
     * @see jpl.gds.shared.swt.ChillShell#wasCanceled()
     */
    @Override
	public boolean wasCanceled() {
        return cancelled;
    }
}