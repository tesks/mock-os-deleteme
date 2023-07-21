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
package jpl.gds.monitor.guiapp.gui.views;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.springframework.context.ApplicationContext;

import jpl.gds.monitor.guiapp.gui.MonitorViewReferences;
import jpl.gds.monitor.guiapp.gui.views.preferences.CustomGridPreferencesShell;
import jpl.gds.monitor.perspective.view.CustomGridViewConfiguration;
import jpl.gds.monitor.perspective.view.GridOrientationType;
import jpl.gds.perspective.view.IViewConfiguration;
import jpl.gds.perspective.view.View;
import jpl.gds.perspective.view.ViewConfigurationListener;
import jpl.gds.perspective.view.ViewFactory;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.swt.ChillColorCreator;
import jpl.gds.shared.swt.ChillFontCreator;

/**
 * 
 * CustomGridComposite is a monitor view that displays a grid of other views
 * in a set of adjustable sash forms.
 *
 */
public class CustomGridComposite implements View {
    
    /**
     * Custom grid composite title
     */
    public static final String TITLE = "Custom Grid";
   
    /**
     * Parent composite for custom grid
     */
    protected Composite parent;
    
    /**
     * Custom grid main composite
     */
    protected Composite mainComposite;
    
    /**
     * Custom grid view configuration
     */
    protected CustomGridViewConfiguration viewConfig;
    private Button configureButton;
    private List<SashForm> sashes;
    private CustomGridPreferencesShell prefShell;
    private Map<String,View> views;
    private SashForm masterSash;

	private final ApplicationContext appContext;
    
    /**
     * Creates an instance of CustomGridComposite.
     * @param appContext the current application context
     * @param config the CustomGridViewConfiguration object containing display settings
     */
    public CustomGridComposite(final ApplicationContext appContext, final IViewConfiguration config) {
    	this.appContext = appContext;
        this.viewConfig = (CustomGridViewConfiguration)config;
    }
    
    
    /**
     * @{inheritDoc}
     * @see jpl.gds.perspective.view.View#getViewConfig()
     */
    @Override
    public IViewConfiguration getViewConfig() {
        return this.viewConfig;
    }
    
    /**
     * @{inheritDoc}
     * @see jpl.gds.perspective.view.View#init(org.eclipse.swt.widgets.Composite)
     */
    @Override
    public void init(final Composite parent) {
        this.parent = parent;
        createViews();
        createControls();
    }
       
    /**
     * Creates the controls and composites for this tab display.
     */
    private void createControls() {
        this.mainComposite = new Composite(this.parent, SWT.NONE);
        final FormLayout shellLayout = new FormLayout();
        this.mainComposite.setLayout(shellLayout);
          
        if (this.views.size() == 0) {
            this.configureButton = new Button(this.mainComposite, SWT.PUSH);
            this.configureButton.setText("Configure Grid");

            this.configureButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
                    try {
                        if (CustomGridComposite.this.prefShell == null) { 
                            CustomGridComposite.this.prefShell = new CustomGridPreferencesShell(appContext, CustomGridComposite.this.mainComposite.getShell());
                            CustomGridComposite.this.prefShell.setNew(true);
                            CustomGridComposite.this.prefShell.setValuesFromViewConfiguration(CustomGridComposite.this.viewConfig);
                            CustomGridComposite.this.prefShell.getShell().addDisposeListener(new DisposeListener() {
                                @Override
                                public void widgetDisposed(final DisposeEvent event) {
                                    try {
                                        if (!CustomGridComposite.this.prefShell.wasCanceled()) {
                                            CustomGridComposite.this.prefShell.getValuesIntoViewConfiguration(CustomGridComposite.this.viewConfig);
                                            CustomGridComposite.this.configureButton.dispose();
                                            configureButton = null;
                                            createViews();
                                            MonitorViewReferences.getInstance().addViewReferences(CustomGridComposite.this.viewConfig.getAllViewReferences());
                                            createSashes();
                                            CustomGridComposite.this.mainComposite.layout();
                                        } else {
                                            CustomGridComposite.this.configureButton.setEnabled(true);
                                        }
                                        CustomGridComposite.this.prefShell = null;
                                    } catch (final Exception e) {
                                        e.printStackTrace();
                                        TraceManager.getDefaultTracer().error("Error handling exit from preferences shell");

                                        CustomGridComposite.this.configureButton.setEnabled(true);
                                        CustomGridComposite.this.prefShell = null;
                                    }
                                }
                            });
                            CustomGridComposite.this.configureButton.setEnabled(false);
                            CustomGridComposite.this.prefShell.open();
                        }
                    } catch (final Exception ec) {
                        ec.printStackTrace();
                        TraceManager.getDefaultTracer().error("Error handling configure button");

                    }
                }
            });
        } else {
            createSashes();
        }
        this.mainComposite.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(final DisposeEvent event) {
                try {
                    if (CustomGridComposite.this.prefShell != null) {
                        CustomGridComposite.this.prefShell.getShell().dispose();
                        prefShell = null;
                    }
                } catch (final Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }  
    
    private void createSashes() {
        if (this.viewConfig.getGridOrientation() == GridOrientationType.ROW_DOMINANT) {
            createRowSashes();
        } else {
            createColumnSashes();
        }
    }
    private void createRowSashes() {
        this.masterSash = new SashForm(this.mainComposite, SWT.VERTICAL | SWT.NULL);
         
        final FormData fd = new FormData();
        fd.top = new FormAttachment(0);
        fd.bottom = new FormAttachment(100);
        fd.left = new FormAttachment(0);
        fd.right = new FormAttachment(100);
        this.masterSash.setLayoutData(fd);
        
        final int rows = this.viewConfig.getGridRows();
        this.sashes = new ArrayList<SashForm>();
        for (int row = 0; row < rows; row++) {
            final SashForm sashForm = new SashForm(this.masterSash, SWT.HORIZONTAL | SWT.NULL);
            this.sashes.add(sashForm);

            final int cols = this.viewConfig.getGridColumns();
            for (int col = 0; col < cols; col++) {
                final IViewConfiguration vc = this.viewConfig.getViewConfigAt(row, col);
                if (vc != null) {
                    final String key = row + "," + col;
                    final View v = this.views.get(key);
                    new ViewBox(vc, v, sashForm);
                }
            }
            final int[] colWeights = this.viewConfig.getVariableWeights(row);
            if (colWeights != null && colWeights.length != 0) {
                sashForm.setWeights(colWeights);
            }
        }
        this.masterSash.setWeights(this.viewConfig.getDominantDimensionWeights());
    }
    
    private void createColumnSashes() {
        this.masterSash = new SashForm(this.mainComposite, SWT.HORIZONTAL | SWT.NULL);
         
        final FormData fd = new FormData();
        fd.top = new FormAttachment(0);
        fd.bottom = new FormAttachment(100);
        fd.left = new FormAttachment(0);
        fd.right = new FormAttachment(100);
        this.masterSash.setLayoutData(fd);
        
        final int cols = this.viewConfig.getGridColumns();
        this.sashes = new ArrayList<SashForm>();
        for (int col = 0; col < cols; col++) {
            final SashForm sashForm = new SashForm(this.masterSash, SWT.VERTICAL | SWT.NULL);
            this.sashes.add(sashForm);

            final int rows = this.viewConfig.getGridRows();
            for (int row = 0; row < rows; row++) {
                final IViewConfiguration vc = this.viewConfig.getViewConfigAt(row, col);
                if (vc != null) {
                    final String key = row + "," + col;
                    final View v = this.views.get(key);
                    new ViewBox(vc, v, sashForm);
                }
            }
            final int[] rowWeights = this.viewConfig.getVariableWeights(col);
            if (rowWeights != null && rowWeights.length != 0) {
                sashForm.setWeights(rowWeights);
            }
        }
        this.masterSash.setWeights(this.viewConfig.getDominantDimensionWeights());
    }
    
    private void createViews() {
        final int rows = this.viewConfig.getGridRows();
        this.views = new HashMap<String,View>();
        for (int row = 0; row < rows; row++) {

            final int cols = this.viewConfig.getGridColumns();
            for (int col = 0; col < cols; col++) {
                final IViewConfiguration vc = this.viewConfig.getViewConfigAt(row, col);
                if (vc != null) {
                    final View v = ViewFactory.createView(appContext, vc, false);
                    final String key = String.valueOf(row) + "," + String.valueOf(col);
                    this.views.put(key,v);       
                }
            }
        }
    } 

    /**
     * @{inheritDoc}
     * @see jpl.gds.perspective.view.View#getDefaultName()
     */
    @Override
    public String getDefaultName() {
        return TITLE;
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.perspective.view.View#updateViewConfig()
     */
    @Override
    public void updateViewConfig() {
        final Collection<String> c = this.views.keySet();
        final Iterator<String> it = c.iterator();
        while (it.hasNext()) {
            final String key = it.next();
            final View v = this.views.get(key);
            v.updateViewConfig();
        }

        if (this.masterSash != null) {
            final int[] rowWeights = this.masterSash.getWeights();
            for (int i = 0; i < rowWeights.length; i++) {
                rowWeights[i] = rowWeights[i] / 10;
            }
            for (int i = 0; i < rowWeights.length; i++) {
                this.viewConfig.setDominantDimensionWeight(i, rowWeights[i]);
                final SashForm rowSash = this.sashes.get(i);
                final int[] colWeights = rowSash.getWeights();
                for (int j = 0; j < colWeights.length; j++) {
                    colWeights[j] = colWeights[j] / 10;
                }
                this.viewConfig.setVariableWeights(i, colWeights);                
            }
        }
    }  
    
    /**
     * {@inheritDoc}
     * @see jpl.gds.perspective.view.View#clearView()
     */
    @Override
    public void clearView() {
        final Collection<String> c = this.views.keySet();
        final Iterator<String> it = c.iterator();
        while (it.hasNext()) {
            final String key = it.next();
            final View v = this.views.get(key);
            v.clearView();
        }
    }
    
    /**
     * 
     * ViewBox is a container class for displaying a single view within the grid.
     */
    private static class ViewBox extends Composite implements ViewConfigurationListener, Runnable {
        private final IViewConfiguration config;
        private final View view;
        private Label viewLabel;
        private Font dataFont;
        private Color background;
        private Color foreground;
        
        public ViewBox(final IViewConfiguration config, final View view, final SashForm parent) {
             super(parent, SWT.BORDER);
             this.config = config;
             this.view = view;
             createControls();
             this.config.addConfigurationListener(this);
        }
        
        private void createControls() {
            setLayout(new FormLayout());
            dataFont = ChillFontCreator.getFont(this.config.getDataFont());            
            background = ChillColorCreator.getColor(this.config.getBackgroundColor());
            foreground = ChillColorCreator.getColor(this.config.getForegroundColor());
            
            if (this.config.isDisplayViewTitle()) {
                this.viewLabel = new Label(this, SWT.NONE);
                final FormData labelFd = new FormData();
                labelFd.top = new FormAttachment(0, 2);
                labelFd.left = new FormAttachment(0);
                labelFd.right = new FormAttachment(98);
                this.viewLabel.setLayoutData(labelFd);
                this.viewLabel.setFont(ChillFontCreator.getFont(this.config.getDataFont()));
                this.viewLabel.setText(this.config.getViewName());
                this.viewLabel.setBackground(background);
                this.viewLabel.setForeground(foreground);
            }
            this.setBackground(background);
            this.view.init(this);
            final Control c = this.view.getMainControl();
            final FormData controlFd = new FormData();
            if (this.viewLabel == null) {
                controlFd.top = new FormAttachment(0);
            } else {
                controlFd.top = new FormAttachment(this.viewLabel);
            }
            controlFd.left = new FormAttachment(0);
            controlFd.right = new FormAttachment(100);
            controlFd.bottom = new FormAttachment(100);
            c.setLayoutData(controlFd);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void configurationChanged(final IViewConfiguration config) {
        	final Display display = Display.getDefault();
        	if ((null != display) && (!display.isDisposed())) {
        		Display.getDefault().asyncExec(this);
        	}
        }

        @Override
        public void run() {
        	if (this.dataFont != null && !this.dataFont.isDisposed()) {
    			this.dataFont.dispose();
    			this.dataFont = null;
    		}
    		dataFont = ChillFontCreator.getFont(config.getDataFont());
    		
    		if (this.foreground != null && !this.foreground.isDisposed()) {
    			this.foreground.dispose();
    			this.foreground = null;
    		}
    		this.foreground = ChillColorCreator.getColor(config.getForegroundColor());
    		
    		if (this.background != null && !this.background.isDisposed()) {
    			this.background.dispose();
    			this.background = null;
    		}
    		this.background = ChillColorCreator.getColor(config.getBackgroundColor());

            if (this.viewLabel != null && !this.config.isDisplayViewTitle()) {
                this.viewLabel.dispose();
                this.viewLabel = null;
                final Control c = this.view.getMainControl();
                final FormData fd = (FormData)c.getLayoutData();
                fd.top = new FormAttachment(0);
                this.layout(true);
            } else if (this.viewLabel != null && this.config.isDisplayViewTitle()) {
                this.viewLabel.setText(this.config.getViewName());
                this.viewLabel.setFont(dataFont);
                this.viewLabel.setBackground(background);
                this.viewLabel.setForeground(foreground);
            } else if (this.viewLabel == null && this.config.isDisplayViewTitle()) {
                this.viewLabel = new Label(this, SWT.NONE);
                final FormData labelFd = new FormData();
                labelFd.top = new FormAttachment(0, 2);
                labelFd.left = new FormAttachment(0);
                labelFd.right = new FormAttachment(98);
                this.viewLabel.setLayoutData(labelFd);
                this.viewLabel.setFont(dataFont);
                this.viewLabel.setText(this.config.getViewName());
                this.viewLabel.setBackground(background);    
                this.viewLabel.setForeground(foreground);    
                final Control c = this.view.getMainControl();
                final FormData fd = (FormData)c.getLayoutData();
                fd.top = new FormAttachment(this.viewLabel);
                this.layout(true);
            }
            this.setBackground(background);
            this.setForeground(foreground);
        }
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.perspective.view.View#getMainControl()
     */
    @Override
    public Control getMainControl() {
        return this.mainComposite;
    }
}
