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
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.channel.IChannelDefinitionProvider;
import jpl.gds.monitor.canvas.CanvasElement;
import jpl.gds.monitor.canvas.ChannelElement;
import jpl.gds.monitor.canvas.ElementConfigurationChangeListener;
import jpl.gds.monitor.perspective.view.fixed.IFixedFieldConfiguration;
import jpl.gds.monitor.perspective.view.fixed.fields.ChannelFieldConfiguration;
import jpl.gds.monitor.perspective.view.fixed.fields.ChannelFieldConfiguration.ChannelFieldType;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.swt.StringSelectorComposite;

/**
 * A small channel composite that contains a mini channel id 
 * selector, a source field drop down box and an alarm highlighting
 * toggle.
 */
public class ChannelComposite extends AbstractComposite{
	private static final int NUM_OF_CHANNELS_IN_SELECTOR = 4; 
	
	private Composite mainComposite;
	private StringSelectorComposite channelSelector;
	private Button button;
	private Combo channelSourceCombo;
	private Button highlightOnButton;
	private Button highlightOffButton;
	
	private boolean channelIdChanged;
	private boolean sourceChanged;
	private boolean alarmHighlightChanged;

	private List<CanvasElement> elements;

	private final IChannelDefinitionProvider defProv;
	
	/**
	 * Constructor: fires up the channel composite
	 * @param defProv the channel definition provider
	 * @param parent Composite in which the channel composite will be placed
	 */
	public ChannelComposite(final IChannelDefinitionProvider defProv, final Composite parent) {
		super(parent);
		this.defProv = defProv;
		createControls();
	}
	
	/**
     * Constructor: fires up the channel composite
     * @param parent Composite in which the channel composite will be placed
     * @param elements the fixed page element that is currently selected 
     *                in the canvas. This is needed to modify/retrieve the 
     *                correct configuration.
     */
    public ChannelComposite(final IChannelDefinitionProvider defProv,
            final Composite parent, 
            final List<CanvasElement> elements) {
        super(parent);
        this.defProv = defProv;
        this.elements = elements;
        createControls();
    }

	/**
	 * Create the channel composite GUI. Add a mini channel selector, a channel 
	 * source combo, alarm highlight buttons and their respective listeners.
	 */
	private void createControls() {

		mainComposite = new Composite(parent, SWT.NONE);
		mainComposite.setLayout(new FormLayout());
		mainComposite.setBackground(backgroundColor);

		//get channel ids
		final Set<String> chans = defProv.getChanIds();

		//channel list with search box
		channelSelector = new StringSelectorComposite(mainComposite, 
				chans.toArray(new String[] {}), NUM_OF_CHANNELS_IN_SELECTOR, false);
		final FormData fd = new FormData();
		fd.top = new FormAttachment(0, 5);
		fd.left = new FormAttachment(0, 100, 5);
		channelSelector.getComposite().setLayoutData(fd);
		channelSelector.setBackground(backgroundColor);

		//select channel button
		button = new Button(mainComposite, SWT.PUSH);
		button.setText("Select");
		FormData data = new FormData();
		data.right = new FormAttachment(channelSelector.getComposite(), 0, SWT.RIGHT);
		data.top = new FormAttachment(channelSelector.getComposite(), 0);
		button.setLayoutData(data);

		//channel source label
		Label label = new Label(mainComposite, SWT.NONE);
		label.setBackground(backgroundColor);
		label.setText("Source: ");
		data = new FormData();
		data.left = new FormAttachment(0, 100, 5);
		data.top  = new FormAttachment(button, 10);
		label.setLayoutData(data);

		//channel source combo box
		channelSourceCombo = new Combo (mainComposite, SWT.READ_ONLY);
		data = new FormData();
		data.left = new FormAttachment(label,0);
		data.right = new FormAttachment(4, -5);
		data.top = new FormAttachment(button, 5);
		channelSourceCombo.setLayoutData(data);

		//alarm highlight label
		label = new Label(mainComposite, SWT.NONE);
		label.setBackground(backgroundColor);
		label.setText("Alarm highlight: ");
		data = new FormData();
		data.left = new FormAttachment(0, 100, 5);
		data.top  = new FormAttachment(channelSourceCombo, 5);
		label.setLayoutData(data);

		//alarm highlight ON button
		highlightOnButton = new Button(mainComposite, SWT.RADIO);
		highlightOnButton.setBackground(backgroundColor);
		highlightOnButton.setText("On");
		data = new FormData();
		data.left = new FormAttachment(label);
		data.top  = new FormAttachment(channelSourceCombo, 5);
		highlightOnButton.setLayoutData(data);

		//alarm highlight OFF button
		highlightOffButton = new Button(mainComposite, SWT.RADIO);
		highlightOffButton.setBackground(backgroundColor);
		highlightOffButton.setText("Off");
		data = new FormData();
		data.left = new FormAttachment(highlightOnButton, 5);
		data.top  = new FormAttachment(channelSourceCombo, 5);
		data.right = new FormAttachment(4, -5);
		data.bottom = new FormAttachment(100, -10);
		highlightOffButton.setLayoutData(data);
		
		if(elements != null) {
		    for(final CanvasElement element : elements) {
                fieldConfigs.add(element.getFieldConfiguration());
            }
		    
            //load the default values into the channel GUI before the listeners
			//are added
            if(elements.size() == 1) {
                setConfiguration(elements.get(0).getFieldConfiguration());
            }       
            else {
                initializeCombo(elements);
            }
        }

		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(
			        final org.eclipse.swt.events.SelectionEvent e) {
				try {
					if (channelSelector.getSingleSelectedString() == null) {
						return;
					}
					channelIdChanged = true;
					notifyChangeListeners();
					channelSelector.setStringInSearchField();
				} catch(final Exception eE){
					eE.printStackTrace();
					TraceManager.getDefaultTracer ().error

					("SELECT button caught unhandled and unexpected " +
					"exception in ChannelComposite.java");
				}
			}
		});

		channelSourceCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(
			        final org.eclipse.swt.events.SelectionEvent e) {
				try {
					sourceChanged = true;
					notifyChangeListeners();
				} catch(final Exception eE){
					TraceManager.getDefaultTracer ().error

					("SOURCE combo caught unhandled and unexpected " +
					"exception in ChannelComposite.java");
				}
			}
		});

		highlightOnButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(
			        final org.eclipse.swt.events.SelectionEvent e) {
				try {
					alarmHighlightChanged = true;
					notifyChangeListeners();
				} catch(final Exception eE){
					TraceManager.getDefaultTracer ().error

					("ON button caught unhandled and unexpected" +
					" exception in ChannelComposite.java");
				}
			}
		});

		highlightOffButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(
			        final org.eclipse.swt.events.SelectionEvent e) {
				try {
					alarmHighlightChanged = true;
					notifyChangeListeners();
				} catch(final Exception eE){
					TraceManager.getDefaultTracer ().error

					("OFF button caught unhandled and unexpected " +
					"exception in ChannelComposite.java" );
				}
			}
		});
	}

	/**
	 * Initializes the combo box to contain only the source fields 
	 * that are shared by all of the given elements
	 * 
	 * @param elements is the list of elements that are currently selected on 
	 * the canvas
	 */
	private void initializeCombo(final List<CanvasElement> elements) {
	    //check eu/status
	    boolean hasStatus = true;
	    boolean hasEu = true;
	    
	    for(final CanvasElement elem : elements) {
	        if(!hasStatus(((ChannelFieldConfiguration)elem.getFieldConfiguration()).
	                getChannelId())) {
	            hasStatus = false;
	        }
	        
	        if(!hasEu(((ChannelFieldConfiguration)elem.getFieldConfiguration()).
	                getChannelId())) {
                hasEu = false;
            }
	        
	        if(!hasStatus && !hasEu) {
	            break;
	        }
	    }
	    
	    //******************************************************************
	    //fill in combo accordingly
	    //******************************************************************
	    int size = ChannelFieldType.values().length;
        final ArrayList<String> sourceTypes = new ArrayList<String>(size);

        sourceTypes.clear();

        //add channel source types to ArrayList
        for(int i=0; i<size; i++)
        {
            sourceTypes.add(ChannelFieldType.values()[i].toString());
        }

        String[] comboItems;

        //remove EU if channel does not have it
        if(!hasEu) {
            sourceTypes.remove(ChannelFieldType.EU.toString());
            size = sourceTypes.size();
        }
        
        //remove Status if channel does not have it
        if(!hasStatus) {
            sourceTypes.remove(ChannelFieldType.STATUS.toString());
            comboItems = new String[size-1];
        }
        else {
            comboItems = new String[size];
        }

        //convert ArrayList to array
        sourceTypes.toArray(comboItems);

        //add list to combo box
        channelSourceCombo.setItems(comboItems);
	}
	
	
	
	/**
	 * Populates the source combo box with the channel field types
	 * 
	 * @param chanId the ID that is currently selected in the 
	 *               channel composite
	 */
	private void initializeCombo(final String chanId)
	{
		int size = ChannelFieldType.values().length;
		final ArrayList<String> sourceTypes = new ArrayList<String>(size);

		sourceTypes.clear();

		//add channel source types to ArrayList
		for(int i=0; i<size; i++)
		{
			sourceTypes.add(ChannelFieldType.values()[i].toString());
		}

		String[] comboItems;

		//remove EU if channel does not have it
		if(!hasEu(chanId)) {
			sourceTypes.remove(ChannelFieldType.EU.toString());
			size = sourceTypes.size();
		}

		//remove Status if channel does not have it
		if(!hasStatus(chanId)) {
			sourceTypes.remove(ChannelFieldType.STATUS.toString());
			comboItems = new String[size-1];
		}
		else {
			comboItems = new String[size];
		}

		//convert ArrayList to array
		sourceTypes.toArray(comboItems);

		//add list to combo box
		channelSourceCombo.setItems(comboItems);
	}

	/**
	 * Checks if the given channel has EU
	 * 
	 * @param chanId the ID that is going to be looked up in the table
	 * @return true if the channel has EU, false otherwise
	 */
	private boolean hasEu(final String chanId){
		if (chanId == null) {
			return false;
		}
		final IChannelDefinition def = defProv.getDefinitionFromChannelId(chanId);
		if (def == null) {
			return false;
		} else {
			return def.hasEu();
		}
	}

	private boolean hasStatus(final String chanId) {
		if (chanId == null) {
			return false;
		}
		final IChannelDefinition def =  defProv.getDefinitionFromChannelId(chanId);
		if (def == null) {
			return false;
		} else {
			//determine if channel has status
			if(def.getChannelType().hasEnumeration()) {
				return true;
			}
			return false;
		}
	}

	/**
	 * Selects the item in the combo box with the source in the 
	 * channel config
	 * 
	 * @param channel is the current channel field configuration
	 */
	public void setChannelSource(final ChannelFieldConfiguration channel) {

		final int channelFieldOrdinal = channel.getFieldType().ordinal();
		final boolean hasEu = hasEu(channel.getChannelId());
		final boolean hasStatus = hasStatus(channel.getChannelId());
		
		//if no EU and Status, list in combo is 2 items shorter than list in 
		//enum
		if(!hasEu && !hasStatus) {
		    
		    //if source if after Status, subtract 2 from the index
		    if(channelFieldOrdinal > ChannelFieldType.STATUS.ordinal()) {
		        channelSourceCombo.select(channelFieldOrdinal - 2);
		    }
		    //if source if after EU, subtract 1 from the index
		    else if(channelFieldOrdinal > ChannelFieldType.EU.ordinal()) {
		        channelSourceCombo.select(channelFieldOrdinal - 1);
		    }
		    //if the source is before EU, index is the same
		    else {
		       channelSourceCombo.select(channelFieldOrdinal);
		    }  
		}
		//if no EU, list in combo is 1 item shorter than list in enum
        else if(!hasEu && hasStatus){
            //if the source is before EU, index is the same
            if(channelFieldOrdinal < ChannelFieldType.EU.ordinal()) {
                channelSourceCombo.select(channelFieldOrdinal);
            }
            //if the source is after EU we subtract 1 from the index 
            //since EU isn't in the list anymore
            else {
                channelSourceCombo.select(channelFieldOrdinal - 1);
            }
        }
        else if(hasEu && !hasStatus) {
          //if the source is before Status, index is the same
            if(channelFieldOrdinal < ChannelFieldType.STATUS.ordinal()) {
                channelSourceCombo.select(channelFieldOrdinal);
            }
            //if the source is after Status we subtract 1 from the index 
            //since Status isn't in the list anymore
            else {
                channelSourceCombo.select(channelFieldOrdinal - 1);
            }
        }
		//if it has EU and Status, list in combo is the same as list in enum
        if(hasEu && hasStatus) {
            channelSourceCombo.select(channelFieldOrdinal);
        } 
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.monitor.fixedbuilder.palette.AbstractComposite#getComposite()
	 */
	@Override
	public Composite getComposite() {
		return mainComposite;
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
                
                //time needs a special action to be taken when source 
                //is changed
                if(sourceChanged) {
                    l.timeSourceChanged(elements);
                }
			}
			//reset fieldConfig and booleans
            fieldConfig = null;
            channelIdChanged = false;
			sourceChanged = false;
			alarmHighlightChanged = false;
		}
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.ElementConfigurationComposite#getConfiguration()
	 */
	@Override
	public synchronized IFixedFieldConfiguration getConfiguration() {
	    //get config
	    final ChannelFieldConfiguration channel = 
	        ((ChannelFieldConfiguration)fieldConfig);
	    
	    //Channel Id
		if(channelIdChanged) {
		    //the ID changed, set channel ID and reset format to the default
		    channel.setChannelId(channelSelector.getSingleSelectedString());
            
    		if (channelSelector.getSingleSelectedString() != null) {	
    			channel.setChannelId(channelSelector.getSingleSelectedString());
    		}
			//repopulate combo box once we have the id
			initializeCombo(channel.getChannelId());
			
			if(elements != null && elements.size() == 1) {
			    setChannelSource(channel);
		    }
		}

		//store old type
        final ChannelFieldType oldType = channel.getFieldType();
        
        //Channel Source
		if(sourceChanged) {

	        //Channel Field type
	        final String selection = channelSourceCombo.getItem(
	                channelSourceCombo.getSelectionIndex());

	        for(final ChannelFieldType type : ChannelFieldType.values())
	        {
	            if(selection.equals(type.toString()))
	            {
	                channel.setFieldType(type);
	                break;
	            }
	        }
		    
	        //Alarm highlighting
		    final boolean defaultAlarm = 
		        channel.getFieldType().isHighlightByDefault();
			highlightOnButton.setSelection(defaultAlarm);
			highlightOffButton.setSelection(!defaultAlarm);
			
			channel.setUseAlarmHighlight(highlightOnButton.getSelection());
		}
		
		//Alarm Highlighting
		if(alarmHighlightChanged) {
		    channel.setUseAlarmHighlight(highlightOnButton.getSelection());
		}

		//Do not reset to default format if:
		//1. alarmHighlighting status was changed
		//2. user re-selected the same source that was already 
		//   selected in the combo box
		//3. the old source and the new source use the same kind of formatting
		if(alarmHighlightChanged || 
				(sourceChanged && oldType == channel.getFieldType()) || 
				isSameFormat(oldType, channel.getFieldType())) {
			return channel;
		}

		//else, we reset the format to the default
		channel.setDefaultFormat();

		return channel;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.ElementConfigurationComposite#setConfiguration(jpl.gds.monitor.perspective.view.fixed.fields.FixedFieldConfiguration)
	 */
	@Override
	public void setConfiguration(final IFixedFieldConfiguration config) {
		fieldConfig = config;

		ChannelFieldConfiguration channel = null;

		if (elements.get(0).getFieldConfiguration() != null) {
			channel = (ChannelFieldConfiguration)elements.get(0).getFieldConfiguration();
		} else {
			channel = new ChannelFieldConfiguration(config.getApplicationContext());
		}
		if (channel.getChannelId() != null) {
			channelSelector.setSingleString(channel.getChannelId());
			channelSelector.setStringInSearchField();
		}

		//after the chan id has been set, we need to set the values 
		//in the source combo
		initializeCombo(channel.getChannelId());
		setChannelSource(channel);

		highlightOnButton.setSelection(channel.isUseAlarmHighlight());
		highlightOffButton.setSelection(!channel.isUseAlarmHighlight());
	}

	/**
	 * These times are formatted differently so the format composite 
	 * needs to be updated accordingly
	 * @return true if ERT/SCET/RCT/MST are selected
	 */
	public boolean useDateFormat()
	{
		final String selection = channelSourceCombo.getItem(
				channelSourceCombo.getSelectionIndex());

		return ((ChannelFieldType.valueOf(selection)).isTimeFormattedField());
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
        final ChannelFieldType channelType = ((ChannelElement)elements.get(0)).getChannelFieldType();
        
        //LST
        if(channelType.isSolFormattedField()) {
            for(final CanvasElement elem : elements) {
                if(!((ChannelElement)elem).getChannelFieldType().isSolFormattedField()) {
                    return TimeFormatType.NONE;
                }
            }
            return TimeFormatType.SOL_DATE;
        }
        
        //ERT, SCET, RCT, UTC, MST
        else if(channelType.isTimeFormattedField()) {
            for(final CanvasElement elem : elements) {
                if(!((ChannelElement)elem).getChannelFieldType().
                        isTimeFormattedField()  || 
                        ((ChannelElement)elem).getChannelFieldType().
                        isSolFormattedField()) {
                    return TimeFormatType.NONE;
                }
            }
            return TimeFormatType.REGULAR_DATE;
        }
        
        //SCLK
        else {
            for(final CanvasElement elem : elements) {
                if(((ChannelElement)elem).getChannelFieldType().
                        isTimeFormattedField()) {
                    return TimeFormatType.NONE;
                }
            }
            return TimeFormatType.STRING;
        }
    }

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.monitor.fixedbuilder.palette.AbstractComposite#getListeners()
	 */
	@Override
	public List<ElementConfigurationChangeListener> getListeners()
	{
		return listeners;
	}
	
	/**
	 * helper function: determines if the 2 channel type parameters require 
	 * the same formatting
	 * @param oldType previously selected source in the combo box
	 * @param newType currently selected source in the combo box
	 * @return true if format type changed, false otherwise
	 */
	private boolean isSameFormat(final ChannelFieldType oldType, 
	        final ChannelFieldType newType) {
	    //moved this to a separate method since the original if condition was 
	    //getting rather hairy. There must be a way to simplify this check but 
	    //this works fine for now...
	    return (oldType.isSolFormattedField() && 
	            newType.isSolFormattedField()) ||
        (newType.isTimeFormattedField() && 
                !newType.isSolFormattedField() && 
                !oldType.isSolFormattedField() && 
                (oldType.isTimeFormattedField() == 
                    newType.isTimeFormattedField()));
	}
}
