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
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import jpl.gds.monitor.canvas.CanvasElement;
import jpl.gds.monitor.canvas.ElementConfigurationChangeListener;
import jpl.gds.monitor.perspective.view.fixed.IFixedFieldConfiguration;
import jpl.gds.monitor.perspective.view.fixed.fields.ButtonFieldConfiguration;
import jpl.gds.monitor.perspective.view.fixed.fields.ButtonFieldConfiguration.ActionType;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.swt.SWTUtilities;
/**
 * A small button composite that contains a combo box and textfield 
 * in which the action type and action string may be selected.
 */
public class ButtonComposite extends AbstractComposite{
     
    private Composite mainComposite;
   
    private Combo actionCombo;
    private Text actionText;
    private Label actionName;
    private Button browseButton;
	
	private List<CanvasElement> elements;
	
	private boolean actionTypeChanged; //(i.e. not text)

	/**
	  * Constructor: fires up the button composite
	  * @param parent Composite in which the button composite will be placed
	  */
	public ButtonComposite(final Composite parent) {
        super(parent);
        createControls();
    }
    
    /**
     * Constructor: fires up the button composite
     * @param parent Composite in which the button composite will be placed
     * @param element the fixed page element that is currently selected 
     *         in the canvas. This is needed to modify/retrieve the correct 
     *         configuration.
     */
    public ButtonComposite(final Composite parent, final List<CanvasElement> elements) {
        super(parent);
        this.elements = elements;
        createControls();
    }
    
    /**
	  * Create the button composite GUI. 
	  * Add a combo box, textfield and their respective listeners.
	  */
    private void createControls() {

        this.mainComposite = new Composite(this.parent, SWT.NONE);
        this.mainComposite.setLayout(new FormLayout());
        this.mainComposite.setBackground(backgroundColor);
        
        //Action type label
		final Label label = new Label (this.mainComposite, SWT.NONE);
		label.setBackground(backgroundColor);
		label.setText("Launch:");
		FormData data = new FormData();
		data.left = new FormAttachment(0, 100, 5);
		data.top = new FormAttachment(0, 10);
		label.setLayoutData(data);
		
		//Action type button
		actionCombo = new Combo (this.mainComposite, SWT.READ_ONLY);
		actionCombo.setItems(new String[] {"Fixed View", "Script"});
		data = new FormData();
		data.top = new FormAttachment(0, 100, 5);
		data.left = new FormAttachment(label, 5);
		data.right = new FormAttachment(4, -5);
		actionCombo.setLayoutData(data);
		
		//Action string label
		actionName = new Label (this.mainComposite, SWT.NONE);
		actionName.setBackground(backgroundColor);
		actionName.setText("Name:");
		data = new FormData();
		data.left = new FormAttachment(0, 100, 5);
		data.top = new FormAttachment(label, 15);
		actionName.setLayoutData(data);
		
		//Action string textfield
		actionText = new Text (this.mainComposite, SWT.BORDER);
		data = SWTUtilities.getFormData(actionText, 1, 26);
		data.left = new FormAttachment(actionCombo, 0, SWT.LEFT);
		data.right = new FormAttachment(4,-5);
		data.top = new FormAttachment(actionCombo, 5);
		actionText.setLayoutData(data);
		
		browseButton = new Button(this.mainComposite, SWT.PUSH);
		browseButton.setText("Browse");
        data = new FormData();
        data.top = new FormAttachment(actionText, 0, -5);
        data.right = new FormAttachment(4, -5);
        data.bottom = new FormAttachment(100, -5);
        browseButton.setLayoutData(data);
        browseButton.setVisible(false);
		
        if(elements != null) {
            for(final CanvasElement element : elements) {
                fieldConfigs.add(element.getFieldConfiguration());
            } 
            
            //load the default values into the button GUI before the listeners 
            //are added
            if(elements.size() == 1) {
                setConfiguration(elements.get(0).getFieldConfiguration());
            }
        }

		
		this.actionCombo.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
	            try {
	            	actionTypeChanged = true;
	                notifyChangeListeners();
	            } catch(final Exception eE){
	                TraceManager.getDefaultTracer ().error

	                ("Action combo caught unhandled and unexpected " +
	                		"exception in ButtonComposite.java");
	            }
	        }
		});
        
        this.actionText.addModifyListener(new ModifyListener()
		{
			@Override
            public void modifyText(final ModifyEvent arg0)
			{
				try {
				    actionTypeChanged = false;
				    notifyChangeListeners();
				} catch (final Exception e) {
				    TraceManager.getDefaultTracer().error

                    ("Action text caught unhandled and unexpected " +
                    		"exception in ButtonComposite.java");
				}
			}
		});
        
        browseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
                try {
                    final SWTUtilities swt = new SWTUtilities();
                    
                    final String path = swt.displayStickyFileChooser(false, 
                            mainComposite.getShell(), 
                            "ButtonComposite");
                    
                    if (path != null) {
                        //set the text field to the user specified path
                        actionText.setText(path);
                        actionTypeChanged = false;
                        notifyChangeListeners();
                    }
                } catch(final Exception eE){
                    TraceManager.getDefaultTracer ().error

                    ("BROWSE button caught unhandled and unexpected" +
                            " exception in ButtonComposite.java");
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
        return this.mainComposite;
    }
    
    
    /**
	  * Get the button configuration settings by retrieving the selected 
	  * button action type and action string
	  * 
	  * @param config current configuration for the fixed page element
	  */
	@Override
    public IFixedFieldConfiguration getConfiguration() {		
		//Update the configuration with the settings selected in the composite
		if(actionTypeChanged) {
    	    if(actionCombo.getItem(actionCombo.getSelectionIndex()).equals(
    	            "Fixed View")){
    		    ((ButtonFieldConfiguration)fieldConfig).setActionType(
    		            ActionType.LAUNCH_PAGE);
    			browseButton.setVisible(true);
    		}
    		else {
    		    ((ButtonFieldConfiguration)fieldConfig).setActionType(
    		            ActionType.LAUNCH_SCRIPT);
    			browseButton.setVisible(false);
    		}
		}
		else {
		    ((ButtonFieldConfiguration)fieldConfig).setActionString(actionText.
		            getText());
		}
		
		return fieldConfig;
	}

	/**
	  * Set the button configuration settings in the composite gui
	  * @param config current configuration for the fixed page element
	  */
	@Override
    public void setConfiguration(final IFixedFieldConfiguration config) {
		fieldConfig = config;
		
		//Get the configuration
		ButtonFieldConfiguration button = null;

		if (elements.get(0).getFieldConfiguration() != null) {
			button = (ButtonFieldConfiguration)elements.get(0).
			getFieldConfiguration();
		} else {
			button = new ButtonFieldConfiguration();
		}
		
		//set default action type and string if null
		if(button.getActionType() == null) {
			button.setActionType(ActionType.LAUNCH_PAGE);
		}
		if(button.getActionString() == null) {
			button.setActionString("");
		}
		
		//Select action type in the combo box
		if(button.getActionType().equals(ActionType.LAUNCH_PAGE)) {
			actionCombo.select(actionCombo.indexOf("Fixed View"));
			browseButton.setVisible(true);
		}
		else {
			actionCombo.select(actionCombo.indexOf("Script"));
			browseButton.setVisible(false);
		}
		
		//Set action string from config in textfield
		actionText.setText(button.getActionString());
	}

	/**
	  * Get all the listeners that will be notified if a different action
	  * type or string is selected in the button composite
	  * @return listeners
	  */
	@Override
    public List<ElementConfigurationChangeListener> getListeners()
	{
		return listeners;
	}
    
	/**
	  * launches a standalone button composite
	  */
	public static void main(final String[] args) {
        final Display display = new Display();
        final Shell shell = new Shell(display);
        shell.setText("Button Chooser");
        shell.setLayout(new FormLayout());
        final Composite m = new Composite(shell, SWT.NONE);
        final FormData fd = new FormData();
        fd.top = new FormAttachment(0);
        fd.right = new FormAttachment(100);
        fd.left = new FormAttachment(0);
        fd.bottom = new FormAttachment(100);
        m.setLayoutData(fd);
        final FillLayout layout = new FillLayout();
        layout.type = SWT.VERTICAL;
        m.setLayout(layout);
        new ButtonComposite(m);
        shell.pack();
        shell.open();
        while (!shell.isDisposed()) {
          if (!display.readAndDispatch()) {
            display.sleep();
          }
        }
        display.dispose();
    }

}
