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
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import jpl.gds.monitor.canvas.CanvasElement;
import jpl.gds.monitor.canvas.ElementConfigurationChangeListener;
import jpl.gds.monitor.perspective.view.fixed.IFixedFieldConfiguration;
import jpl.gds.monitor.perspective.view.fixed.PositionChangeListener;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.shared.swt.types.ChillPoint;

/**
 * A small coordinate composite that contains 2 textfields in which 
 * xStart and yStart may be selected.
 */
public class SingleCoordinateComposite 
            extends CoordinateComposite implements PositionChangeListener {

	private Composite mainComposite;

	private Text xStartTextField;
	private Text yStartTextField;

	private List<CanvasElement> elements;

	/**
	 * Constructor: fires up the coordinate composite
	 * @param parent Composite in which the coordinate composite 
	 *               will be placed
	 */
	public SingleCoordinateComposite(final Composite parent) {
		super(parent);
		createControls();
	}

	/**
	 * Constructor: fires up the coordinate composite
	 * @param parent Composite in which the coordinate composite 
	 *               will be placed
	 * @param elements the fixed page elements that are currently 
	 *                selected in the canvas. This is needed to 
	 *                modify/retrieve the correct configuration.
	 */
	public SingleCoordinateComposite(final Composite parent, 
	        final List<CanvasElement> elements) {
		super(parent);
		this.elements = elements;
		createControls();
	}

	/**
	 * Create the single coordinate composite GUI. Add textfields 
	 * for entering x and y coordinates and their respective 
	 * listeners.
	 */
	private void createControls() {

		mainComposite = new Composite(parent, SWT.NONE);
		mainComposite.setLayout(new FormLayout());
		mainComposite.setBackground(backgroundColor);

		//x start label
		final Label label1 = new Label (mainComposite, SWT.NONE);
		label1.setBackground(backgroundColor);
		label1.setText("x Start: ");
		FormData data = new FormData();
		data.left = new FormAttachment(0, 100, 5);
		data.top = new FormAttachment(0, 10);
		label1.setLayoutData(data);

		//x start textfield
		xStartTextField = new Text (mainComposite, SWT.BORDER);
		data = SWTUtilities.getFormData(xStartTextField, 1, 6);
		data.top = new FormAttachment(0, 100, 5);
		data.left = new FormAttachment(label1, 5);
		xStartTextField.setLayoutData(data);

		//y start label
		final Label label2 = new Label (mainComposite, SWT.NONE);
		label2.setBackground(backgroundColor);
		label2.setText(" y Start: ");
		data = new FormData();
		data.left = new FormAttachment(xStartTextField, 5);
		data.top = new FormAttachment(0, 10);
		label2.setLayoutData(data);

		//y start textfield
		yStartTextField = new Text (mainComposite, SWT.BORDER);
		data = SWTUtilities.getFormData(yStartTextField, 1, 6);
		data.top = new FormAttachment(0, 100, 5);
		data.left = new FormAttachment(label2, 5);
		data.right = new FormAttachment(4, -5);
		data.bottom = new FormAttachment(100, -5);
		yStartTextField.setLayoutData(data);

		
		if(elements != null)
		{
		    for(final CanvasElement element : elements) {
                fieldConfigs.add(element.getFieldConfiguration());
            }
            
            //load the default values into the coordinate GUI before the 
		    //listeners are added
            if(elements.size() == 1) {
                setConfiguration(elements.get(0).getFieldConfiguration());
            }
		}

		xStartTextField.addModifyListener(new ModifyListener()
		{
			@Override
            public void modifyText(final ModifyEvent arg0)
			{
				try {
					notifyChangeListeners();
				} catch (final Exception e) {
				    TraceManager.getDefaultTracer ().error

                    ("x start text caught unhandled and unexpected " +
                            "exception in SingleCoordinateComposite.java");
				}
			}
		});

		yStartTextField.addModifyListener(new ModifyListener()
		{
			@Override
            public void modifyText(final ModifyEvent arg0)
			{
				try {
					notifyChangeListeners();
				} catch (final Exception e) {
				    TraceManager.getDefaultTracer ().error

                    ("x end text caught unhandled and unexpected " +
                            "exception in SingleCoordinateComposite.java");
				}
			}
		});

		mainComposite.addDisposeListener(new DisposeListener() {
			@Override
            public void widgetDisposed(final DisposeEvent e) {
				if (fieldConfig != null) {
					fieldConfig.removePositionChangeListener(
					        SingleCoordinateComposite.this);
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
    public synchronized IFixedFieldConfiguration getConfiguration() {


		try {
			final ChillPoint start = fieldConfig.getStartCoordinate();
			start.setX(Integer.valueOf(xStartTextField.getText()));
			start.setY(Integer.valueOf(yStartTextField.getText()));
			fieldConfig.setStartCoordinate(start);
		} 
		//textfield is empty, but it's ok
		catch (final NumberFormatException e) {
		    //we don't want to print an exception every time a user 
		    //clears a textfield
		}

		return fieldConfig;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.ElementConfigurationComposite#setConfiguration(jpl.gds.monitor.perspective.view.fixed.fields.FixedFieldConfiguration)
	 */
	@Override
    public synchronized void setConfiguration(final IFixedFieldConfiguration config) {
		if (fieldConfig != null) {
			fieldConfig.removePositionChangeListener(this);
		}
		fieldConfig = config;
		fieldConfig.addPositionChangeListener(this);
		final ChillPoint start = config.getStartCoordinate();
		xStartTextField.setText(Integer.toString(start.getX()));
		yStartTextField.setText(Integer.toString(start.getY()));
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.monitor.fixedbuilder.palette.CoordinateComposite#getListeners()
	 */
	@Override
	public List<ElementConfigurationChangeListener> getListeners()
	{
		return listeners;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.perspective.view.fixed.PositionChangeListener#positionChanged(jpl.gds.monitor.perspective.view.fixed.fields.FixedFieldConfiguration, jpl.gds.shared.swt.types.ChillPoint)
	 */
	@Override
    public synchronized void positionChanged(
	        final IFixedFieldConfiguration onElem, final ChillPoint startPoint) {
	    try {
    	    xStartTextField.setText(Integer.toString(startPoint.getX()));
    	    yStartTextField.setText(Integer.toString(startPoint.getY()));
	    }
	    catch(final SWTException e) {
	        //widget is disposed
	    }
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.perspective.view.fixed.PositionChangeListener#positionChanged(jpl.gds.monitor.perspective.view.fixed.fields.FixedFieldConfiguration, jpl.gds.shared.swt.types.ChillPoint, jpl.gds.shared.swt.types.ChillPoint)
	 */
	@Override
    public void positionChanged(final IFixedFieldConfiguration onElem, 
	        final ChillPoint start, final ChillPoint end) {
		// Not used by this composite
	}
}
