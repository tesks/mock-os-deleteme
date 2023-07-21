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
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import jpl.gds.dictionary.api.channel.ChannelType;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.channel.IChannelDefinitionProvider;
import jpl.gds.monitor.canvas.CanvasElement;
import jpl.gds.monitor.canvas.ChannelElement;
import jpl.gds.monitor.canvas.TimeElement;
import jpl.gds.monitor.guiapp.gui.views.support.ChannelFormatEntryShell;
import jpl.gds.monitor.perspective.view.fixed.FixedFieldType;
import jpl.gds.monitor.perspective.view.fixed.FormatConfigSupport;
import jpl.gds.monitor.perspective.view.fixed.IFixedFieldConfiguration;
import jpl.gds.monitor.perspective.view.fixed.fields.ChannelFieldConfiguration;
import jpl.gds.monitor.perspective.view.fixed.fields.ChannelFieldConfiguration.ChannelFieldType;
import jpl.gds.shared.formatting.SprintfFormat;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.swt.SWTUtilities;
/**
 * A small composite that contains a textfield for sprintf format 
 * strings and a button that launches a sprintf helper.
 *
 */

public class SprintfFormatComposite extends AbstractFormattingComposite{
	private Composite mainComposite;
	private Text textfield;
	private Button button;

	private List<CanvasElement> elements;

	private ChannelFormatEntryShell formatShell;
	private final IChannelDefinitionProvider dictUtil;
    private SprintfFormat formatUtil;

	/**
	 * Constructor.
	 * 
	 * @param dictUtil channel definition provider
	 * @param formatter spacecraft-aware print formatter
	 * @param parent parent composite
	 */
	public SprintfFormatComposite(final IChannelDefinitionProvider dictUtil, final SprintfFormat formatter, final Composite parent) {
		super(parent);
		this.dictUtil = dictUtil;
		this.formatUtil = formatter;
		createControls();
	}

	/**
     * Constructor.
     * 
     * @param dictUtil channel definition provider
     * @param parent parent composite
     * @param elements the fixed page elements that are currently 
     *                selected in the canvas. This is needed to 
     *                modify/retrieve the correct configuration.
     */
	public SprintfFormatComposite(final IChannelDefinitionProvider dictUtil, final Composite parent, 
	        final List<CanvasElement> elements) {
		super(parent);
		this.dictUtil = dictUtil;
		this.elements = elements;
		createControls();
	}

	private void createControls() {
		mainComposite = new Composite(parent, SWT.NONE);
		mainComposite.setLayout(new FormLayout());
		mainComposite.setBackground(backgroundColor);

		//Format label
		final Label label = new Label (mainComposite, SWT.NONE);
		label.setBackground(backgroundColor);
		label.setText("Format: ");
		FormData data = new FormData();
		data.left = new FormAttachment(0, 100, 5);
		data.top = new FormAttachment(0, 10);
		label.setLayoutData(data);

		//Format help button
		button = new Button (mainComposite, SWT.PUSH);
		button.setText("?");
		data = new FormData();
		data.top = new FormAttachment(0, 5);
		data.right = new FormAttachment(4, -5);
		data.bottom = new FormAttachment(100, -5);
		button.setLayoutData(data);

		//Format combo
		textfield = new Text (mainComposite, SWT.BORDER);
		data = SWTUtilities.getFormData(textfield, 1, 23);
		data.top = new FormAttachment(0, 5);
		data.left = new FormAttachment(label, 0, 5);
		data.right = new FormAttachment(button, 0, -5);
		textfield.setLayoutData(data);

		if(elements != null)
		{
		    for(final CanvasElement element : elements) {
                fieldConfigs.add(element.getFieldConfiguration());
            }
            
            //load the default values into the sprintf GUI before 
		    //the listeners are added
            if(elements.size() == 1) {
                setConfiguration(elements.get(0).getFieldConfiguration());
            }
		}

		textfield.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(final ModifyEvent arg0) {
				try {
					notifyChangeListeners();
				} catch(final Exception eE){
					TraceManager.getDefaultTracer ().error(

							"FORMAT textfield caught unhandled and unexpected "+
					"exception in SprintfFormatComposite.java" );
				}
			}

		});

		//FloatFormatComposite/StringFormatComposite/UnsignedIntFormatComposite/SignedIntComposite
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
			    try {
                    boolean isAscii = true;
                    boolean isDnOrRaw = true;
                    boolean isEuOrValue = true;
			        
                    //check if ascii string (time, text and all string channels)
			        for(final CanvasElement elem : elements) {
			            if(elem.getFieldType().equals(FixedFieldType.CHANNEL)) {
			                final ChannelFieldType chanType = ((ChannelElement)elem).
			                getChannelFieldType();
			                
		                    if((chanType.equals(ChannelFieldType.DN)) ||
		                            chanType.equals(ChannelFieldType.RAW) ||
		                            chanType.equals(ChannelFieldType.EU) ||
		                            chanType.equals(ChannelFieldType.VALUE)){
		                        isAscii = false;
		                        break;
		                    }
			            }    
			        }
			        
			        ChannelType oldType = null;
			        ChannelType dataType1 = null;
			        
			        
			        //check if dn or raw (need to check if data type matches)
			        if(!isAscii) {
			            for(final CanvasElement elem : elements) {
	                        if(elem.getFieldType().equals(FixedFieldType.CHANNEL)) {
	                            final ChannelFieldType chanType = ((ChannelElement)elem).
	                            getChannelFieldType();
	                            
	                            if(!(chanType.equals(ChannelFieldType.DN)) &&
	                                    !(chanType.equals(ChannelFieldType.RAW))) {
	                                isDnOrRaw = false;
	                                break;
	                            }
	                            else {

                                    final ChannelElement chan = (ChannelElement)elem;
                                    
                                    final IChannelDefinition def = 
                                            dictUtil.getDefinitionFromChannelId(chan.getChannelId());
                                    if (def == null) {
                                        SWTUtilities.showErrorDialog(
                                                mainComposite.getShell(), 
                                                "Channel Not Defined", 
                                        "The selected channel is undefined in " +
                                        "the current dictionary, so its format " +
                                        "cannot be changed.");
                                        return;
                                    }
                                    oldType = dataType1;
                                    dataType1 = def.getChannelType();
                                    if(oldType != null && oldType != dataType1) {
                                        isEuOrValue = false;
                                        break;
                                    }
	                            }
	                        } else {
	                            isDnOrRaw = false;
	                            break;
	                        }
	                    }
			        }
			        else {
			            isDnOrRaw = false;
			        }
			        
			        ChannelType dataType2 = null;
			        
			        //check if eu or value (need to check if data type matches)
			        //check if dn or raw (need to check if data type matches)
                    if(!isAscii && !isDnOrRaw) {
                        for(final CanvasElement elem : elements) {
                            if(elem.getFieldType().equals(FixedFieldType.CHANNEL)) {
                                final ChannelFieldType chanType = ((ChannelElement)
                                        elem).getChannelFieldType();
                                
                                if(!(chanType.equals(ChannelFieldType.EU)) &&
                                        !(chanType.equals(ChannelFieldType.VALUE))) {
                                    isEuOrValue = false;
                                    break;
                                }
                                else {

                                    final ChannelElement chan = (ChannelElement)elem;
                                    
                                    final IChannelDefinition def = 
                                            dictUtil.getDefinitionFromChannelId(chan.getChannelId());
                                    if (def == null) {
                                        SWTUtilities.showErrorDialog(
                                                mainComposite.getShell(), 
                                                "Channel Not Defined", 
                                        "The selected channel is undefined in " +
                                        "the current dictionary, so its format " +
                                        "cannot be changed.");
                                        return;
                                    }
                                    oldType = dataType2;
                                    dataType2 = def.getChannelType();
                                    if(oldType != null && oldType != dataType2) {
                                        isEuOrValue = false;
                                        break;
                                    }
                                }
                            } else {
                                isEuOrValue = false;
                                break;
                            }
                        }
                    }
                    else {
                        isEuOrValue = false;
                    }
                    
			        if(!isAscii && !isDnOrRaw && !isEuOrValue) {
                        //cannot show formatting wizard, incompatible format 
			            //types are selected
			            return;
                    }
			        
			        //everything is fine; display format wizard
			        final Display mainDisplay = Display.getDefault();
                    final Shell parent = new Shell(mainDisplay, SWT.SHELL_TRIM);
                    parent.setSize(625, 600);
                    final FormLayout shellLayout = new FormLayout();
                    shellLayout.spacing = 5;
                    shellLayout.marginHeight = 5;
                    shellLayout.marginWidth = 5;
                    parent.setLayout(shellLayout);

                    if (formatShell == null) {

                        formatShell = new ChannelFormatEntryShell(parent, formatUtil);
		        
    			        if(isAscii) {
    			            formatShell.init(ChannelType.ASCII, false, 
                                    textfield.getText());
    			        }
    			        else if(isDnOrRaw) {
    			            
    			            formatShell.init(dataType1, true, 
    			                    textfield.getText());
    			        }
    			        else {
    			            formatShell.init(dataType2, false, 
    			                    textfield.getText());
    			        }
    			        
    			        formatShell.open();
                        final Display display = Display.getDefault();
                        while (!formatShell.getShell().isDisposed()) {
                            if (!display.readAndDispatch()) {
                                display.sleep();
                            }
                        }  
                        if (!formatShell.wasCanceled()) {
                            String val = formatShell.getFormatString();
                            if (val.equals("")) {
                                val = null;
                            }

                            textfield.setText(formatShell.getFormatString());

                            notifyChangeListeners();
                        }
                        formatShell = null;
                    }

				} catch(final Exception eE){
					TraceManager.getDefaultTracer ().error(

							"FORMAT button caught unhandled and unexpected " +
					"exception in SprintfFormatComposite.java" );
				}
			}
		});
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
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.ElementConfigurationComposite#getConfiguration()
	 */
	@Override
	public IFixedFieldConfiguration getConfiguration() {
		final FormatConfigSupport fcs = (FormatConfigSupport)fieldConfig;

		fcs.setFormat(textfield.getText());

		return fieldConfig;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.ElementConfigurationComposite#setConfiguration(jpl.gds.monitor.perspective.view.fixed.fields.FixedFieldConfiguration)
	 */
	@Override
	public void setConfiguration(final IFixedFieldConfiguration config) {
		fieldConfig = config;
		final FormatConfigSupport fcs = (FormatConfigSupport)fieldConfig;
		
		if(fcs.getFormat()==null) {
		    //channelConfig
		    if(config instanceof ChannelFieldConfiguration) {
		        fcs.setFormat(((ChannelFieldConfiguration)config).setDefaultFormat());
		    }
		    //timeConfig: SCLK
		    else {
		        fcs.setFormat("%s");
		    }
		}
		if(fcs.getFormat() != null) {
		    textfield.setText(fcs.getFormat());
		}
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.monitor.fixedbuilder.palette.AbstractFormattingComposite#setDefaultFormat()
	 */
	@Override
	public void setDefaultFormat()
	{
		if(elements.size() == 1) {
    	    //SCLK and LMST are strings
    		if(elements.get(0) instanceof TimeElement)
    		{
    			textfield.setText("%s");
    			return;
    		}
    
    		final ChannelElement channel = (ChannelElement) elements.get(0);
    		textfield.setText(((ChannelFieldConfiguration)channel.
    		        getFieldConfiguration()).getFormat());
		}
	}


//	/**
//	 * Launches a standalone color composite
//	 */
//	public static void main(String[] args) {
//		Display display = new Display();
//		Shell shell = new Shell(display);
//		shell.setText("Format Chooser");
//		shell.setLayout(new FormLayout());
//		Composite m = new Composite(shell, SWT.NONE);
//		FormData fd = new FormData();
//		fd.top = new FormAttachment(0);
//		fd.right = new FormAttachment(100);
//		fd.left = new FormAttachment(0);
//		fd.bottom = new FormAttachment(100);
//		m.setLayoutData(fd);
//		FillLayout layout = new FillLayout();
//		layout.type = SWT.VERTICAL;
//		m.setLayout(layout);
//		new SprintfFormatComposite(dictUtil, m);
//		shell.pack();
//		shell.open();
//		while (!shell.isDisposed()) {
//			if (!display.readAndDispatch()) {
//				display.sleep();
//			}
//		}
//		display.dispose();
//	}
}