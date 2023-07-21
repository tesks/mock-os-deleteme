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
package jpl.gds.monitor.fixedbuilder.palette;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;

import jpl.gds.monitor.canvas.CanvasElement;
import jpl.gds.monitor.perspective.view.fixed.IFixedFieldConfiguration;
import jpl.gds.monitor.perspective.view.fixed.fields.ChannelFieldConfiguration;
import jpl.gds.monitor.perspective.view.fixed.fields.TimeFieldConfiguration;
import jpl.gds.perspective.config.PerspectiveProperties;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.swt.SWTUtilities;

/**
 * A small date format composite that contains 1 combo box of the 
 * default GdsSystemConfig.xml date formats in which a date format 
 * may be selected.
 */
public class DateFormatComposite extends AbstractFormattingComposite{
 
	private Composite mainComposite;
    private Combo formatCombo;
    
    private List<CanvasElement> elements;
    private TimeFormatType formatType = TimeFormatType.NONE;
	private final PerspectiveProperties perspectiveProps;
    
    /**
	  * Constructor: fires up the date format composite
      * @param pp current PerspectiveProperties object
	  * @param parent Composite in which the date format composite 
	  *               will be placed
	  */
    public DateFormatComposite(final PerspectiveProperties pp, final Composite parent) {
        super(parent);
        this.perspectiveProps = pp;
    	createControls();
    }
    
    /**
	  * Constructor: fires up the date format composite
	  * @param parent Composite in which the date format composite 
	  *               will be placed
	  * @param elements the fixed page element that is currently 
	  *                selected in the canvas. This is needed to 
	  *                modify/retrieve the correct configuration.
     * @param formatType time format type
	  */
    public DateFormatComposite(final PerspectiveProperties pp, final Composite parent, final List<CanvasElement> elements, 
            final TimeFormatType formatType) {
        super(parent);
        this.perspectiveProps = pp;
    	this.elements = elements;
    	this.formatType = formatType;
        createControls();
    }
    
    /**
	  * Create the date format composite GUI. Add a combo box for 
	  * date formats and its respective listener.
	  */
    private void createControls() {

        this.mainComposite = new Composite(this.parent, SWT.NONE);
        this.mainComposite.setLayout(new FormLayout());
        this.mainComposite.setBackground(backgroundColor);
		
		//Format combo
		formatCombo = new Combo (this.mainComposite, SWT.READ_ONLY);
		initializeCombo();
		final FormData data = SWTUtilities.getFormData(formatCombo, 1, 37);
		data.top = new FormAttachment(0, 5);
		data.left = new FormAttachment(0, 100, 5);
		data.right = new FormAttachment(4, -5);
		data.bottom = new FormAttachment(100, -15);
		formatCombo.setLayoutData(data);
        
		if(elements != null)
        {
		    for(final CanvasElement element : elements) {
                fieldConfigs.add(element.getFieldConfiguration());
            }
            
            //load the default values into the date GUI before the listeners 
            //are added
            if(elements.size() == 1) {
                setConfiguration(elements.get(0).getFieldConfiguration());
            }
        }
		
		formatCombo.addSelectionListener(new SelectionAdapter() {
	        @Override
	        public void widgetSelected(
	                final org.eclipse.swt.events.SelectionEvent e) {
	            try {
            		notifyChangeListeners();
	            } catch(final Exception eE){
	            	eE.printStackTrace();
	                TraceManager.getDefaultTracer ().error

	                ("Format combo caught unhandled and unexpected " +
	                		"exception in DateFormatComposite.java");
	            }
	        }
			
		});
    }
    
    /**
     * Sets the options in the date format combo box. The values are 
     * retrieved from the GdsSystemConfig.xml file.
     */
    private void initializeCombo()
    {
    	if(formatType.equals(TimeFormatType.SOL_DATE)) {
    	    formatCombo.setItems(getConfiguredLstTimeFormats());
    	}
    	else {
    	    formatCombo.setItems(getConfiguredTimeFormats());
    	}
    }
    
    /**
     * Gets list of date formats stored in the GdsSystemConfig.xml 
     * file
     * @return String array of date formats
     */
    private String[] getConfiguredTimeFormats() {
    	
        return this.perspectiveProps.getUtcTimeFormats().toArray(new String[] {});
    }
    
    /**
     * Gets list of LST (Local Solar Time) date formats stored in the 
     * GdsSystemConfig.xml file
     * @return String array of LST (Local Solar Time) date formats
     */
    private String[] getConfiguredLstTimeFormats() {
        
        return (String[])this.perspectiveProps.getLstTimeFormats().toArray();
    }
    
	 /**
	 * {@inheritDoc}
	 * @see jpl.gds.monitor.fixedbuilder.palette.AbstractComposite#getComposite()
	 */
	@Override
    public Composite getComposite() {
        return this.mainComposite;
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.monitor.canvas.ElementConfigurationComposite#getConfiguration()
     */
    @Override
    public IFixedFieldConfiguration getConfiguration() {
    	
    	switch(fieldConfig.getType())
    	{
    	case TIME:
    		//get config
            final TimeFieldConfiguration time = 
                ((TimeFieldConfiguration)fieldConfig);
			
			time.setFormat(formatCombo.getItem(
			        formatCombo.getSelectionIndex()));
	    	
			return time;
		
	    case CHANNEL:
	        //get config
	        final ChannelFieldConfiguration channel = 
	            ((ChannelFieldConfiguration)fieldConfig);
			
			channel.setFormat(formatCombo.getItem(
			        formatCombo.getSelectionIndex()));
			return channel;
		}
    	return null;
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
    public void setConfiguration(final IFixedFieldConfiguration config) {
    	
	    switch(config.getType())
    	{
    	
    	case TIME:
    	    //get config
            final TimeFieldConfiguration time = ((TimeFieldConfiguration)config);
    		
            if(time.getFormat()==null) {
                time.setDefaultFormat();
            }
            
            //select the config value in the combo box
            formatCombo.select(formatCombo.indexOf(time.getFormat()));
            break;
    	
    	case CHANNEL:
    	    //get config
            final ChannelFieldConfiguration channel = 
                ((ChannelFieldConfiguration)config);
            
            if(channel.getFormat()==null) {
                channel.setDefaultFormat();
            }
            
            //select the config value in the combo box
            formatCombo.select(formatCombo.indexOf(channel.getFormat()));
            break;
    	}
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.monitor.fixedbuilder.palette.AbstractFormattingComposite#setDefaultFormat()
	 */
	@Override
	public void setDefaultFormat() {
		//only being used by SprintfFormatComposite so far
	}
}
