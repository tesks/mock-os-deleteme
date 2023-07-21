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
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.springframework.context.ApplicationContext;

import jpl.gds.monitor.perspective.view.ProductStatusViewConfiguration;
import jpl.gds.perspective.ChillTable;
import jpl.gds.perspective.view.IViewConfiguration;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.swt.ColorComposite;
import jpl.gds.shared.swt.FontComposite;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.shared.swt.TitleComposite;

/**
 * ProductStatusPreferencesShell is the SWT GUI preferences window class for the 
 * message monitor's product status view.

 *
 */
public class ProductStatusPreferencesShell extends AbstractViewPreferences {
    
	/**
     * Product status preferences window title
     */
    public static final String TITLE = "Product Status Preferences";
    
    /**
     * Parent shell
     */
    protected Shell parent;
    
    /**
     * Text field widget for entering the time interval for flushing products 
     * in minutes
     */
    protected Text minutesText;
    
    /**
     * Time interval for flushing products in minutes
     */
    protected int flushMinutes;
    
    private TableComposite tableConfigurer;
    private ChillTable productTable;
    private boolean dirty;
    private boolean changeColumns;
    
    private final Tracer       trace;

    /**
     * Creates an instance of ProductStatusPreferencesShell.
     * @param parent the parent display of this widget
     */
    public ProductStatusPreferencesShell(final ApplicationContext appContext, final Shell parent) {
        super(appContext, TITLE);
        this.parent = parent;
        this.trace = TraceManager.getDefaultTracer(appContext);
        createControls();
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
        this.prefShell.setLayout(new FillLayout());

        final Composite mainComposite = new Composite(this.prefShell, SWT.NONE);
        mainComposite.setLayout(new FormLayout());

        this.titleText = new TitleComposite(mainComposite);
        final Composite titleComp = this.titleText.getComposite();
        final FormData fdLabel2 = new FormData();
        fdLabel2.top = new FormAttachment(0, 10);
        fdLabel2.left = new FormAttachment(0, 3);
        titleComp.setLayoutData(fdLabel2);
        
        final Label minutesLabel = new Label(mainComposite, SWT.NONE);
        minutesLabel.setText("Flush Products Every ");
        final FormData fd1 = new FormData();
        fd1.top = new FormAttachment(titleComp, 12);
        fd1.left = new FormAttachment(0, 3);
        minutesLabel.setLayoutData(fd1);
        final Label minutesLabel2 = new Label(mainComposite, SWT.NONE);
        minutesLabel2.setText("Minutes");
        final FormData fd11 = new FormData();
        fd11.top = new FormAttachment(titleComp, 12);
        minutesLabel2.setLayoutData(fd11);
        this.minutesText = new Text(mainComposite, SWT.SINGLE | SWT.BORDER);
        this.minutesText.setText(String.valueOf(this.flushMinutes));
        final FormData fd2 = SWTUtilities.getFormData(this.minutesText, 1, 8);
        fd2.left = new FormAttachment(minutesLabel);
        fd2.top = new FormAttachment(minutesLabel, 0, SWT.CENTER);
        this.minutesText.setLayoutData(fd2);
        fd11.left = new FormAttachment(this.minutesText);
       
        this.fontGetter = new FontComposite(mainComposite, "Data Font", trace);
        final Composite fontComp = this.fontGetter.getComposite();
        final FormData fontFd = new FormData();
        fontFd.top = new FormAttachment(this.minutesText, 0, 5);
        fontFd.left = new FormAttachment(0);
        fontFd.right = new FormAttachment(100);
        fontComp.setLayoutData(fontFd);
        
        this.foreColorGetter = new ColorComposite(mainComposite,
                "Foreground Color", trace);
        final Composite foreColorComp = this.foreColorGetter.getComposite();
        final FormData foreColorFd = new FormData();
        foreColorFd.top = new FormAttachment(fontComp, 0, 7);
        foreColorFd.left = new FormAttachment(0, 2);
        foreColorFd.right = new FormAttachment(100);
        foreColorComp.setLayoutData(foreColorFd);
        
        this.backColorGetter = new ColorComposite(mainComposite, "Background Color", trace);
        final Composite colorComp = this.backColorGetter.getComposite();
        final FormData colorFd = new FormData();
        colorFd.top = new FormAttachment(foreColorComp, 0, 3);
        colorFd.left = new FormAttachment(0);
        colorFd.right = new FormAttachment(100);
        colorComp.setLayoutData(colorFd);
        
        final Composite m = new Composite(mainComposite, SWT.NONE);
        final FormData fd = new FormData();
        fd.top = new FormAttachment(colorComp);
        fd.right = new FormAttachment(100);
        fd.left = new FormAttachment(0);
        m.setLayoutData(fd);
        final FillLayout layout = new FillLayout();
        layout.type = SWT.VERTICAL;
        m.setLayout(layout);
        
        this.tableConfigurer = new TableComposite(m);
        
        final Label line = new Label(mainComposite, SWT.SEPARATOR | SWT.HORIZONTAL
                | SWT.SHADOW_ETCHED_IN);
        final FormData formData6 = new FormData();
        formData6.left = new FormAttachment(0, 3);
        formData6.right = new FormAttachment(100, 3);
        formData6.top = new FormAttachment(m, 5);
        line.setLayoutData(formData6);
        
        final Composite composite = new Composite(mainComposite, SWT.NONE);
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
                    try {
                        final int mins = Integer.parseInt(ProductStatusPreferencesShell.this.minutesText.getText().trim());
                        if (mins < 0) {
                            SWTUtilities.showMessageDialog(ProductStatusPreferencesShell.this.prefShell, "Invalid Number",
                            "The flush interval must be a non-negative number.");
                            return;                        
                        }
                        ProductStatusPreferencesShell.this.flushMinutes = mins;
                    } catch (final NumberFormatException ex) {
                        SWTUtilities.showMessageDialog(ProductStatusPreferencesShell.this.prefShell, "Invalid Number",
                        "You must enter the flush interval as an integer.");
                        return;
                    }
                    if (tableConfigurer.getTable().getActualColumnCount() == 0) {
                        SWTUtilities.showMessageDialog(ProductStatusPreferencesShell.this.prefShell, "No Columns",
                                "You cannot disable all the table columns, or the table will be blank.");
                        return;
                    }
                    applyChanges();
                    ProductStatusPreferencesShell.this.dirty = ProductStatusPreferencesShell.this.tableConfigurer.getDirty();
                    if (ProductStatusPreferencesShell.this.dirty) {
                        ProductStatusPreferencesShell.this.changeColumns = true;
                    }
                    ProductStatusPreferencesShell.this.prefShell.close();
                } catch (final Exception e1) {
                    e1.printStackTrace();
                    ProductStatusPreferencesShell.this.prefShell.close();
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
                    ProductStatusPreferencesShell.this.canceled = true;
                    ProductStatusPreferencesShell.this.prefShell.close();
                } catch (final Exception e1) {
                     e1.printStackTrace();
                }
            }
        });
    }
    
    /**
     * Retrieves the product flush interval set by the user.
     * @return the flush interval in minutes
     */
    private int getFlushMinutes() {
        return this.flushMinutes;
    }
    
    private void setFlushMinutes(final int mins) {
        this.flushMinutes = mins;
        if (this.minutesText != null && !this.minutesText.isDisposed()) {
            this.minutesText.setText(String.valueOf(mins));
        }
    }
    
    /**
     * {@inheritDoc}
     * @see jpl.gds.monitor.guiapp.gui.views.preferences.AbstractViewPreferences#setValuesFromViewConfiguration(jpl.gds.perspective.view.ViewConfiguration)
     */
    @Override
    public void setValuesFromViewConfiguration(final IViewConfiguration config) {
        super.setValuesFromViewConfiguration(config);
        final ProductStatusViewConfiguration prodConfig = (ProductStatusViewConfiguration)config;
        setFlushMinutes(prodConfig.getFlushInterval()); 
        this.dirty = false;
        this.changeColumns = false;
        this.productTable = prodConfig.getTable(ProductStatusViewConfiguration.PRODUCT_TABLE_NAME);
        if (this.tableConfigurer != null && !this.tableConfigurer.getComposite().isDisposed()) {
            this.tableConfigurer.init(this.productTable);
            this.prefShell.layout(true);
            this.prefShell.pack();
        }
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.monitor.guiapp.gui.views.preferences.AbstractViewPreferences#getValuesIntoViewConfiguration(jpl.gds.perspective.view.ViewConfiguration)
     */
    @Override
    public void getValuesIntoViewConfiguration(final IViewConfiguration config) {
        super.getValuesIntoViewConfiguration(config);
        final ProductStatusViewConfiguration prodConfig = (ProductStatusViewConfiguration)config;
        prodConfig.setFlushInterval(getFlushMinutes());
        if (this.tableConfigurer != null) {
            prodConfig.setTable(this.tableConfigurer.getTable());  
        }
    }
    
    /**
     * Indicates whether the user has elected to change the columns in the product table.
     * @return true if column change required
     */
    public boolean needColumnChange() {
        return this.changeColumns;
    }
}
