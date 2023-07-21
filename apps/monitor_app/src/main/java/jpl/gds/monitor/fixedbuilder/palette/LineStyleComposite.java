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
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import jpl.gds.monitor.canvas.CanvasElement;
import jpl.gds.monitor.canvas.ElementConfigurationChangeListener;
import jpl.gds.monitor.perspective.view.fixed.IFixedFieldConfiguration;
import jpl.gds.monitor.perspective.view.fixed.LineConfigSupport;
import jpl.gds.monitor.perspective.view.fixed.LineStyle;
import jpl.gds.shared.log.TraceManager;

/**
 * A small line style composite that contains a combo box and 
 * solid/dashed toggle buttons for selecting thickness and style, respectively.
 */
public class LineStyleComposite extends AbstractComposite{

	private Composite mainComposite;

	private Combo lineThicknessCombo;
	private Button solidLineButton;
	private Button dashedLineButton;

	private List<CanvasElement> elements;

	/**
	 * List of line thickness options (note: a user can enter a different font 
	 * in the combo box because it's not read-only)
	 */
	private final String[] thicknesses = new String [] {
	        "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", 
	        "12", "13", "14", "15", "16", "17", "18", "19", "20", "21"};

	/**
	 * Constructor: fires up the line style composite
	 * @param parent Composite in which the line style composite 
	 *               will be placed
	 */
	public LineStyleComposite(final Composite parent) {
		super(parent);
		createControls();
	}
	
	/**
     * Constructor: fires up the line composite
     * @param parent Composite in which the line composite will be 
     *               placed
     * @param elements the fixed page element that is currently 
     *                selected in the canvas. This is needed to 
     *                modify/retrieve the correct configuration.
     */
    public LineStyleComposite(final Composite parent, final List<CanvasElement> elements) {
        super(parent);
        this.elements = elements;
        createControls();
    }
	

	/**
	 * Create the line style composite GUI. Add a line thickness 
	 * combo box, radio buttons for solid/dashed option and their 
	 * respective listeners.
	 */
	private void createControls() {

		mainComposite = new Composite(parent, SWT.NONE);
		final FormLayout fl = new FormLayout();
		fl.spacing = 5;
		mainComposite.setLayout(fl);
		mainComposite.setBackground(backgroundColor);

		//Style label
		Label label = new Label (mainComposite, SWT.NONE);
		label.setBackground(backgroundColor);
		label.setText("Style:");
		FormData data = new FormData();
		data.left = new FormAttachment(0, 100, 5);
		data.top = new FormAttachment(0, 7);
		label.setLayoutData(data);

		//Solid style button
		solidLineButton = new Button (mainComposite, SWT.RADIO);
		solidLineButton.setBackground(backgroundColor);
		solidLineButton.setText("Solid");
		data = new FormData();
		data.top = new FormAttachment(0, 100, 5);
		data.left = new FormAttachment(label, 40);
		solidLineButton.setLayoutData(data);

		//Dashed style button
		dashedLineButton = new Button (mainComposite, SWT.RADIO);
		dashedLineButton.setBackground(backgroundColor);
		dashedLineButton.setText("Dashed");
		data = new FormData();
		data.top = new FormAttachment(0, 100, 5);
		data.left = new FormAttachment(solidLineButton, 5);
		data.right = new FormAttachment(4, -5);
		dashedLineButton.setLayoutData(data);

		//Thickness label
		label = new Label (mainComposite, SWT.NONE);
		label.setBackground(backgroundColor);
		label.setText("Thickness:");
		data = new FormData();
		data.left = new FormAttachment(0, 100, 5);
		data.top = new FormAttachment(solidLineButton, 5);
		label.setLayoutData(data);

		//thickness combo box
		lineThicknessCombo = new Combo (mainComposite, SWT.READ_ONLY);
		lineThicknessCombo.setItems (thicknesses);
		lineThicknessCombo.setSize (100, 100);
		data = new FormData();
		data.left = new FormAttachment(solidLineButton, 0, SWT.LEFT);
		data.top = new FormAttachment(solidLineButton, 0);
		data.bottom = new FormAttachment(100, -5);
		data.right = new FormAttachment(4, -5);
		lineThicknessCombo.setLayoutData(data);
		
		if(elements != null) {
		    for(final CanvasElement element : elements) {
		        fieldConfigs.add(element.getFieldConfiguration());
		    }
		    
		    //load the default values into the line GUI before the listeners 
            //are added
            if(elements.size() == 1) {
                setConfiguration(elements.get(0).getFieldConfiguration());
            }
		}

		dashedLineButton.addSelectionListener(new SelectionListener()
		{
			@Override
			public void widgetDefaultSelected(final SelectionEvent arg0) {
			    //intentionally empty
			}

			@Override
			public void widgetSelected(final SelectionEvent arg0) {
				try {
					notifyChangeListeners();
				} catch(final Exception e) {
				    TraceManager.getDefaultTracer ().error

                    ("Dashed BUTTON caught unhandled and unexpected " +
                            "exception in LineStyleComposite.java");
				}
			}
		});

		solidLineButton.addSelectionListener(new SelectionListener()
		{
			@Override
			public void widgetDefaultSelected(final SelectionEvent arg0) {
				//intentionally empty
			}

			@Override
			public void widgetSelected(final SelectionEvent arg0) {
				try {
					notifyChangeListeners();
				} catch(final Exception e) {
				    TraceManager.getDefaultTracer ().error

                    ("Solid BUTTON caught unhandled and unexpected " +
                            "exception in LineStyleComposite.java");
				}
			}
		});

		lineThicknessCombo.addSelectionListener(new SelectionListener()
		{
			@Override
			public void widgetDefaultSelected(final SelectionEvent arg0) {
				//intentionally empty
			}

			@Override
			public void widgetSelected(final SelectionEvent arg0) {
				try {
					notifyChangeListeners();
				} catch(final Exception e) {
				    TraceManager.getDefaultTracer ().error

                    ("Thickness COMBO caught unhandled and unexpected " +
                            "exception in LineStyleComposite.java");
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
		final LineConfigSupport lcs = (LineConfigSupport)fieldConfig;

		if(dashedLineButton.getSelection()){
			lcs.setLineStyle(LineStyle.DASHED);
		}
		else {
			lcs.setLineStyle(LineStyle.SOLID);
		}

		lcs.setLineThickness(Integer.valueOf(
		        lineThicknessCombo.getSelectionIndex())+1);

		return fieldConfig;	
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.ElementConfigurationComposite#setConfiguration(jpl.gds.monitor.perspective.view.fixed.fields.FixedFieldConfiguration)
	 */
	@Override
	public void setConfiguration(final IFixedFieldConfiguration config) {
		fieldConfig = config;

		LineConfigSupport lcs = null;
		try {
			lcs = (LineConfigSupport)fieldConfig;
		} catch (final ClassCastException e) {
			throw new IllegalArgumentException("config must be an instance of LineConfigSupport");
		}
		if(lcs.getLineStyle() == LineStyle.DASHED)
		{
			dashedLineButton.setSelection(true);
			solidLineButton.setSelection(false);
		}
		else
		{
			dashedLineButton.setSelection(false);
			solidLineButton.setSelection(true);
		}
		
		lineThicknessCombo.select(lcs.getLineThickness()-1);
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

}
