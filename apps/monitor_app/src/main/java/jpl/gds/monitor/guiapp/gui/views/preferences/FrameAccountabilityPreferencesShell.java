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

import jpl.gds.monitor.guiapp.gui.ViewPreferencesShell;
import jpl.gds.monitor.perspective.view.FrameAccountabilityViewConfiguration;
import jpl.gds.perspective.view.IViewConfiguration;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.swt.ColorComposite;
import jpl.gds.shared.swt.FontComposite;
import jpl.gds.shared.swt.TitleComposite;

/**
 * PacketWatchPreferencesShell is the SWT GUI preferences window class for the 
 * packet watch view.d
 *
 */
public class FrameAccountabilityPreferencesShell extends AbstractViewPreferences implements ViewPreferencesShell {
    /**
     * Frame accountability preferences window
     */
    public static final String TITLE = "Frame Accountability Preferences";
    private final Shell parent;
    private boolean overlayCommands;
    private Button overlayCommandsButton;
    private final Tracer             trace;
    
    /**
     * Creates an instance of ChannelListPreferencesShell.
     * @param parent the parent display of this widget
     */
    public FrameAccountabilityPreferencesShell(final ApplicationContext appContext, final Shell parent) {
        super(appContext, TITLE);
        this.parent = parent;
        this.trace = TraceManager.getTracer(appContext, Loggers.DEFAULT);
        createControls();
       // this.prefShell.setSize(410, 330);
        this.prefShell.setLocation(parent.getLocation().x + 100, parent.getLocation().y + 100);
    }
    
    /**
     * Creates all the preferences controls and composites.
     */
    protected void createControls() {
        this.prefShell = new Shell(this.parent, SWT.SHELL_TRIM | 
                SWT.APPLICATION_MODAL);
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

        this.fontGetter = new FontComposite(this.prefShell, "Data Font", trace);
        final Composite fontComp = this.fontGetter.getComposite();
        final FormData fontFd = new FormData();
        fontFd.top = new FormAttachment(titleComp);
        fontFd.left = new FormAttachment(0);
        fontFd.right = new FormAttachment(100);
        fontComp.setLayoutData(fontFd);
        
        this.foreColorGetter = new ColorComposite(this.prefShell,
                "Foreground Color", trace);
        final Composite foreColorComp = this.foreColorGetter.getComposite();
        final FormData foreColorFd = new FormData();
        foreColorFd.top = new FormAttachment(fontComp, 0, 7);
        foreColorFd.left = new FormAttachment(0, 2);
        foreColorFd.right = new FormAttachment(100);
        foreColorComp.setLayoutData(foreColorFd);
        
        this.backColorGetter = new ColorComposite(this.prefShell, "Background Color", trace);
        final Composite colorComp = this.backColorGetter.getComposite();
        final FormData colorFd = new FormData();
        colorFd.top = new FormAttachment(foreColorComp, 0, 3);
        colorFd.left = new FormAttachment(0);
        colorFd.right = new FormAttachment(100);
        colorComp.setLayoutData(colorFd);
        
        this.overlayCommandsButton = new Button(this.prefShell, SWT.CHECK);
        this.overlayCommandsButton.setText("Include Commands in Event Tree");
        final FormData overlayFd = new FormData();
        overlayFd.top = new FormAttachment(colorComp);
        overlayFd.left = new FormAttachment(0,3);
        this.overlayCommandsButton.setLayoutData(overlayFd);
        
        final Label line = new Label(this.prefShell, SWT.SEPARATOR | SWT.HORIZONTAL
                | SWT.SHADOW_ETCHED_IN);
        final FormData formData6 = new FormData();
        formData6.top = new FormAttachment(this.overlayCommandsButton);
        formData6.left = new FormAttachment(0, 3);
        formData6.right = new FormAttachment(100, 3);
        line.setLayoutData(formData6);
 
        final Composite composite = new Composite(this.prefShell, SWT.NONE);
        final GridLayout rl = new GridLayout(2, true);
        composite.setLayout(rl);
        final FormData formData8 = new FormData();
        formData8.top = new FormAttachment(line);
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
        
        applyButton.addSelectionListener(new SelectionAdapter() {
        	/**
             * {@inheritDoc}
             * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
             */
            @Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
                try {
                    applyChanges();
                    overlayCommands = overlayCommandsButton.getSelection();
                } catch (final Exception e1) {
                    e1.printStackTrace();
                } finally {
                    FrameAccountabilityPreferencesShell.this.prefShell.close();
                }
            }
        });
        cancelButton.addSelectionListener(new SelectionAdapter() {
        	/**
             * {@inheritDoc}
             * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
             */
            @Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
                try {
                    FrameAccountabilityPreferencesShell.this.canceled = true;
                    FrameAccountabilityPreferencesShell.this.prefShell.close();
                } catch (final Exception e1) {
                    e1.printStackTrace();
                }
            }
        });
        this.prefShell.pack();
    }
    
    /**
     * {@inheritDoc}
     * @see jpl.gds.monitor.guiapp.gui.views.preferences.AbstractViewPreferences#getValuesIntoViewConfiguration(jpl.gds.perspective.view.ViewConfiguration)
     */
    @Override
	public void getValuesIntoViewConfiguration(final IViewConfiguration config) {
        super.getValuesIntoViewConfiguration(config);
        ((FrameAccountabilityViewConfiguration)config).setOverlayCommands(this.overlayCommands);
        
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.monitor.guiapp.gui.views.preferences.AbstractViewPreferences#setValuesFromViewConfiguration(jpl.gds.perspective.view.ViewConfiguration)
     */
    @Override
	public void setValuesFromViewConfiguration(final IViewConfiguration config) {
        super.setValuesFromViewConfiguration(config);
        this.overlayCommands = ((FrameAccountabilityViewConfiguration)config).isOverlayCommands();
        if (this.overlayCommandsButton != null && !this.overlayCommandsButton.isDisposed()) {
        	this.overlayCommandsButton.setSelection(this.overlayCommands);
        }
    }
}
