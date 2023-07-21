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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import jpl.gds.monitor.canvas.CanvasElement;
import jpl.gds.monitor.canvas.ElementConfigurationChangeListener;
import jpl.gds.monitor.canvas.TimeElement;
import jpl.gds.monitor.perspective.view.fixed.IFixedFieldConfiguration;
import jpl.gds.monitor.perspective.view.fixed.fields.TimeFieldConfiguration;
import jpl.gds.monitor.perspective.view.fixed.fields.TimeFieldConfiguration.SourceTimeType;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.swt.SWTUtilities;

/**
 * A small time composite that contains a combo box in which the 
 * time source may be selected.
 */
public class TimeComposite extends AbstractComposite {
    
    private Composite mainComposite;
    private Combo timeSourceCombo;
    
    private List<CanvasElement> elements;
    
    /**
	  * Constructor: fires up the time composite
	  * @param parent Composite in which the time composite will be 
	  *               placed
	  */
    public TimeComposite(final Composite parent) {
        super(parent);
    	createControls();
    }
    
    /**
	  * Constructor: fires up the time composite
	  * @param parent Composite in which the time composite will be placed
	  * @param elements the fixed page elements that are currently 
	  *                selected in the canvas. This is needed to 
	  *                modify/retrieve the correct configuration.
	  */
    public TimeComposite(final Composite parent, final List<CanvasElement> elements) {
        super(parent);
    	this.elements = elements;
        createControls();
    }
    
    /**
	  * Create the time composite GUI. Add a combo box for time 
	  * source and its respective listener.
	  */
    private void createControls() {

        this.mainComposite = new Composite(this.parent, SWT.NONE);
        this.mainComposite.setLayout(new FormLayout());
        this.mainComposite.setBackground(backgroundColor);
        
        //Time source label
		final Label label = new Label (this.mainComposite, SWT.NONE);
		label.setBackground(backgroundColor);
		label.setText("Source:");
		FormData data = new FormData();
		data.left = new FormAttachment(0, 100, 5);
		data.top = new FormAttachment(0, 10);
		data.bottom = new FormAttachment(100, -10);
		label.setLayoutData(data);
		
		//Time source combo
		timeSourceCombo = new Combo (this.mainComposite, SWT.READ_ONLY);
		initializeCombo();
		data = SWTUtilities.getFormData(timeSourceCombo, 1, 28);
		data.top = new FormAttachment(0, 5);
		data.left = new FormAttachment(label, 5, 5);
		data.right = new FormAttachment(4, -5);
		timeSourceCombo.setLayoutData(data);
        
		if(elements != null)
        {
		    for(final CanvasElement element : elements) {
                fieldConfigs.add(element.getFieldConfiguration());
            }
            
            //load the default values into the time GUI before the listeners 
            //are added
            if(elements.size() == 1) {
                setConfiguration(elements.get(0).getFieldConfiguration());
            }
        }
		
		timeSourceCombo.addSelectionListener(new SelectionAdapter() {
	        @Override
	        public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
	            try {
            		notifyChangeListeners();
	            } catch(final Exception eE){
	                TraceManager.getDefaultTracer ().error(

	                        "SOURCE combo caught unhandled and " +
	                        "unexpected exception in TimeComposite.java" );
	            }
	        }
			
		});
    }
    
    /**
	  * Sets the values in the time source combo box to the values
	  * in the SourceTimeType enum
	  */
    private void initializeCombo()
    {
    	final int size = SourceTimeType.values().length;
    	final String[] sourceTypes = new String[size];
    	for(int i=0; i<size; i++)
    	{
    		sourceTypes[i] = SourceTimeType.values()[i].toString();
    	}
    	timeSourceCombo.setItems(sourceTypes);
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
     * {@inheritDoc}
     * @see jpl.gds.monitor.fixedbuilder.palette.AbstractComposite#notifyChangeListeners()
     */
    @Override
    public synchronized void notifyChangeListeners() {
		for (final ElementConfigurationChangeListener l : listeners) {
			if(fieldConfigs != null) {
			    final List<IFixedFieldConfiguration> fcs = 
			        new ArrayList<IFixedFieldConfiguration>();
                for(final IFixedFieldConfiguration fc : fieldConfigs)
                { 
                    fieldConfig = fc;
                    fcs.add(this.getConfiguration());
                }
			    
			    l.elementsChanged(fcs);
    			
    			//time needs a special action to be taken when source is 
    			//changed
    			l.timeSourceChanged(elements);
			}
		}
	}
    
    /**
     * @{inheritDoc}
     * @see jpl.gds.monitor.fixedbuilder.palette.AbstractComposite#addChangeListener(jpl.gds.monitor.canvas.ElementConfigurationChangeListener)
     */
    @Override  
    public void addChangeListener(final ElementConfigurationChangeListener l) {
        if (!this.listeners.contains(l)) {  
                this.listeners.add(l);  
        }   
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.monitor.canvas.ElementConfigurationComposite#getConfiguration()
     */
    @Override
	public IFixedFieldConfiguration getConfiguration() {
		final TimeFieldConfiguration time = ((TimeFieldConfiguration)fieldConfig);

		//store old type
		final SourceTimeType oldType = time.getTimeType();
		
		//Time Field type
		final String selection = timeSourceCombo.getItem(
		        timeSourceCombo.getSelectionIndex());
		for(final SourceTimeType type : SourceTimeType.values())
		{
			if(selection.equals(type.toString()))
			{
				time.setTimeType(type);
				break;
			}
		}
		
		//compare old selection with new selection. If went from useSprintf to 
		//not useSprintf or viceversa we need to update format now!
		//if the formatting type changed, we need to update format here
		if(oldType.isTimeFormattedField() != time.getTimeType().isTimeFormattedField() ||
		        oldType.isSolFormattedField() != time.getTimeType().isSolFormattedField()) {
		    time.setDefaultFormat();
		}
		
		return time;	
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setConfiguration(final IFixedFieldConfiguration config) {
		fieldConfig = config;
		TimeFieldConfiguration time = null;

		if (elements.get(0).getFieldConfiguration() != null) {
			time = (TimeFieldConfiguration)elements.get(0).
			getFieldConfiguration();
		} else {
			time = new TimeFieldConfiguration(config.getApplicationContext());
		}
		timeSourceCombo.select(time.getTimeType().ordinal());
	}
	
	/**
	 * The palette uses this class to determine which format 
	 * composite should be used (date or sprintf)
	 * @return Returns true if ERT/SCET/RCT/UTC/MST are selected.  
	 */
	public boolean useDateFormat()
	{
	    for(final CanvasElement elem : elements) {
	        if(!((TimeElement)elem).getTimeType().isTimeFormattedField()) {
	            return false;
	        }
	    }
	    return true;
	}
	
	/**
	 * Determines which time format should be used based on the type of 
	 * currently selected elements in the canvas.
	 * 
	 * LST time elements use special SOL formats
	 * ERT,SCET,RCT,UTC,MST use regular date formats
	 * SCLK time elements use sprintf string format
	 * 
	 * If a mix across these groups of time elements is selected no time 
	 * format should be used
	 * 
	 * @return timeFormatType is the kind of time format that will be used 
	 * either Sol, Date, String or None
	 */
	public TimeFormatType determineFormatType() {
	    final SourceTimeType timeType = ((TimeElement)elements.get(0)).getTimeType();
	    
	    //LST
	    if(timeType.isSolFormattedField()) {
	        for(final CanvasElement elem : elements) {
                if(!((TimeElement)elem).getTimeType().isSolFormattedField()) {
                    return TimeFormatType.NONE;
                }
            }
	        return TimeFormatType.SOL_DATE;
        }
	    
	    //ERT, SCET, RCT, UTC, MST
	    else if(timeType.isTimeFormattedField()) {
	        for(final CanvasElement elem : elements) {
                if(!(((TimeElement)elem).getTimeType().isTimeFormattedField()) || 
                        ((TimeElement)elem).getTimeType().isSolFormattedField()) {
                    return TimeFormatType.NONE;
                }
            }
	        return TimeFormatType.REGULAR_DATE;
	    }
	    
	    //SCLK
	    else {
	        for(final CanvasElement elem : elements) {
                if(((TimeElement)elem).getTimeType().isTimeFormattedField()) {
                    return TimeFormatType.NONE;
                }
            }
	        return TimeFormatType.STRING;
	    }
	}
    
}
