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
package jpl.gds.monitor.guiapp.gui.views.preferences;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.springframework.context.ApplicationContext;

import jpl.gds.perspective.view.IViewConfiguration;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.swt.ColorComposite;
import jpl.gds.shared.swt.FontComposite;
import jpl.gds.shared.swt.TitleComposite;

/**
 * WindowPreferencesShell is the SWT GUI preferences window class for the
 * message monitor's window-type views.
 * 
 */
public class WindowPreferencesShell extends AbstractViewPreferences {

    /**
     * Window preferences title
     */
    public static final String TITLE = "Window Preferences";
    
    /**
     * Parent shell
     */
    protected Shell parent;

    /**
     * Creates an instance of TabularViewPreferencesShell.
     * 
     * @param parent the parent display of this widget
     */
    public WindowPreferencesShell(ApplicationContext appContext, Shell parent) {
        super(appContext, TITLE);
        this.parent = parent;
        createControls();
      //  this.prefShell.setSize(400, 245);
        this.prefShell.setLocation(parent.getLocation().x + 100, parent.getLocation().y + 100);
    }

    /**
     * Creates all the preferences controls and composites.
     */
    protected void createControls() {
        this.prefShell = new Shell(this.parent, SWT.SHELL_TRIM
                | SWT.APPLICATION_MODAL);
        this.prefShell.setText(TITLE);
        final FormLayout fl = new FormLayout();
        fl.marginHeight = 5;
        fl.marginWidth = 5;
        fl.spacing = 5;
        this.prefShell.setLayout(fl);

        this.titleText = new TitleComposite(this.prefShell);
        final Composite titleComp = this.titleText.getComposite();
        final FormData fdLabel2 = new FormData();
        fdLabel2.top = new FormAttachment(0, 10);
        fdLabel2.left = new FormAttachment(0, 3);
        titleComp.setLayoutData(fdLabel2);

        final Label infoLabel = new Label(this.prefShell, SWT.NONE);
        infoLabel
                .setText("(Include '[test-name]' if you want the session name included\nand [session-key] if you want the session number included.)");
        final FormData fdInfoLabel = new FormData();
        fdInfoLabel.top = new FormAttachment(titleComp);
        fdInfoLabel.left = new FormAttachment(0, 3);
        infoLabel.setLayoutData(fdInfoLabel);

        this.fontGetter = new FontComposite(this.prefShell, "Data Font", TraceManager.getDefaultTracer());
        final Composite fontComp = this.fontGetter.getComposite();
        final FormData fontFd = new FormData();
        fontFd.top = new FormAttachment(infoLabel, 0, 5);
        fontFd.left = new FormAttachment(0);
        fontFd.right = new FormAttachment(100);
        fontComp.setLayoutData(fontFd);

        this.foreColorGetter = new ColorComposite(this.prefShell,
                "Foreground Color", TraceManager.getDefaultTracer());
        final Composite foreColorComp = this.foreColorGetter.getComposite();
        final FormData foreColorFd = new FormData();
        foreColorFd.top = new FormAttachment(fontComp, 0, 7);
        foreColorFd.left = new FormAttachment(0, 2);
        foreColorFd.right = new FormAttachment(100);
        foreColorComp.setLayoutData(foreColorFd);
        
        this.backColorGetter = new ColorComposite(this.prefShell, "Background Color", TraceManager.getDefaultTracer());
        final Composite colorComp = this.backColorGetter.getComposite();
        final FormData colorFd = new FormData();
        colorFd.top = new FormAttachment(foreColorComp, 0, 3);
        colorFd.left = new FormAttachment(0);
        colorFd.right = new FormAttachment(100);
        colorComp.setLayoutData(colorFd);

        final Composite composite = new Composite(this.prefShell, SWT.NONE);
        final GridLayout rl = new GridLayout(2, true);
        composite.setLayout(rl);
        final FormData formData8 = new FormData();
        formData8.top = new FormAttachment(this.backColorGetter.getComposite());
        formData8.right = new FormAttachment(100);
        formData8.bottom = new FormAttachment(100);
        composite.setLayoutData(formData8);

        final Button applyButton = new Button(composite, SWT.PUSH);
        applyButton.setText("Ok");
        this.prefShell.setDefaultButton(applyButton);
        final GridData gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        applyButton.setLayoutData(gd);
        final Button cancelButton = new Button(composite, SWT.PUSH);
        cancelButton.setText("Cancel");
        
        
        final Label line = new Label(this.prefShell, SWT.SEPARATOR | SWT.HORIZONTAL
                | SWT.SHADOW_ETCHED_IN);
        final FormData formData6 = new FormData();
        formData6.left = new FormAttachment(0, 3);
        formData6.right = new FormAttachment(100, 3);
        formData6.bottom = new FormAttachment(composite, 5);
        line.setLayoutData(formData6);

        applyButton.addSelectionListener(new SelectionAdapter() {

        	/**
             * {@inheritDoc}
             * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
             */
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                try {
                    applyChanges();
                    WindowPreferencesShell.this.prefShell.close();
                } catch (final Exception ex) {
                	ex.printStackTrace();
                	TraceManager.getDefaultTracer().error("Problem handling apply button in WindowPreferencesShell");

                }
            }
        });
        cancelButton.addSelectionListener(new SelectionAdapter() {

        	/**
             * {@inheritDoc}
             * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
             */
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                try {
                    WindowPreferencesShell.this.canceled = true;
                    WindowPreferencesShell.this.prefShell.close();
                } catch (final Exception e1) {
                    e1.printStackTrace();
                }
            }
        });
        this.prefShell.pack();
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.monitor.guiapp.gui.views.preferences.AbstractViewPreferences#setValuesFromViewConfiguration(jpl.gds.perspective.view.ViewConfiguration)
     */
    @Override
    public void setValuesFromViewConfiguration(IViewConfiguration config) {
        super.setValuesFromViewConfiguration(config);
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.monitor.guiapp.gui.views.preferences.AbstractViewPreferences#getValuesIntoViewConfiguration(jpl.gds.perspective.view.ViewConfiguration)
     */
    @Override
    public void getValuesIntoViewConfiguration(IViewConfiguration config) {
        super.getValuesIntoViewConfiguration(config);
    }
}
