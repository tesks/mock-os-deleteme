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
import jpl.gds.monitor.perspective.view.fixed.fields.DualPointFixedFieldConfiguration;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.shared.swt.types.ChillPoint;

/**
 * A small coordinate composite that contains 4 textfields in which 
 * xStart, yStart, xEnd and yEnd may be selected.
 */
public class DualCoordinateComposite 
extends CoordinateComposite implements PositionChangeListener {

	private Composite mainComposite;

	private Text xStartTextField;
	private Text yStartTextField;
	private Text xEndTextField;
	private Text yEndTextField;

	private List<CanvasElement> elements;

	/**
	 * Constructor: fires up the coordinate composite
	 * @param parent Composite in which the coordinate composite will be placed
	 */
	public DualCoordinateComposite(final Composite parent) {
		super(parent);
		createControls();
	}

	/**
	 * Constructor: fires up the coordinate composite
	 * @param parent Composite in which the coordinate composite will be placed
	 * @param elements the fixed page element that is currently selected in the 
	 *                canvas. This is needed to modify/retrieve the correct 
	 *                configuration.
	 */
	public DualCoordinateComposite(final Composite parent, 
	        final List<CanvasElement> elements) {
		super(parent);
		this.elements = elements;
		createControls();
	}

	/**
	 * Create the dual coordinate composite GUI. Add textfields for entering 
	 * coordinates and their respective listeners.
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
		yStartTextField.setLayoutData(data);

		//x end label
		final Label label3 = new Label (mainComposite, SWT.NONE);
		label3.setBackground(backgroundColor);
		label3.setText("x End: ");
		data = new FormData();
		data.left = new FormAttachment(label1, 0, SWT.LEFT);
		data.top = new FormAttachment(label1, 15);
		label3.setLayoutData(data);

		//x end textfield
		xEndTextField = new Text (mainComposite, SWT.BORDER);
		data = SWTUtilities.getFormData(xEndTextField, 1, 6);
		data.top = new FormAttachment(xStartTextField, 5);
		data.left = new FormAttachment(xStartTextField, 0, SWT.LEFT);
		xEndTextField.setLayoutData(data);

		//y end label
		final Label label4 = new Label (mainComposite, SWT.NONE);
		label4.setBackground(backgroundColor);
		label4.setText(" y End: ");
		data = new FormData();
		data.left = new FormAttachment(label2, 0, SWT.LEFT);
		data.top = new FormAttachment(label2, 15);
		label4.setLayoutData(data);

		//y end textfield
		yEndTextField = new Text (mainComposite, SWT.BORDER);
		data = SWTUtilities.getFormData(yEndTextField, 1, 6);
		data.top = new FormAttachment(yStartTextField, 5);
		data.left = new FormAttachment(yStartTextField, 0, SWT.LEFT);
		data.right = new FormAttachment(4, -5);
		data.bottom = new FormAttachment(100, -5);
		yEndTextField.setLayoutData(data);

		
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

					("x start textfield caught unhandled and unexpected " +
					"exception in DualCoordinateComposite.java");
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

					("y start textfield caught unhandled and unexpected " +
					"exception in DualCoordinateComposite.java");
				}
			}
		});

		xEndTextField.addModifyListener(new ModifyListener()
		{
			@Override
            public void modifyText(final ModifyEvent arg0)
			{
				try {
					notifyChangeListeners();
				} catch (final Exception e) {
					TraceManager.getDefaultTracer ().error

					("x end textfield caught unhandled and unexpected " +
					"exception in DualCoordinateComposite.java");
				}
			}
		});

		yEndTextField.addModifyListener(new ModifyListener()
		{
			@Override
            public void modifyText(final ModifyEvent arg0)
			{
				try {
					notifyChangeListeners();
				} catch (final Exception e) {
					TraceManager.getDefaultTracer ().error

					("y end textfield caught unhandled and unexpected " +
					"exception in DualCoordinateComposite.java");
				}
			}
		});

		mainComposite.addDisposeListener(new DisposeListener() {
			@Override
            public void widgetDisposed(final DisposeEvent e) {
				if (fieldConfig != null) {
					fieldConfig.removePositionChangeListener
					(DualCoordinateComposite.this);
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

	    final DualPointFixedFieldConfiguration dpfc = 
	        (DualPointFixedFieldConfiguration)fieldConfig;

		try {
			final ChillPoint start = dpfc.getStartCoordinate();
			start.setX(Integer.valueOf(xStartTextField.getText()));
			start.setY(Integer.valueOf(yStartTextField.getText()));
			dpfc.setStartCoordinate(start);

			final ChillPoint end = dpfc.getEndCoordinate();
			end.setX(Integer.valueOf(xEndTextField.getText()));
			end.setY(Integer.valueOf(yEndTextField.getText()));
			dpfc.setEndCoordinate(end);

		}
		//the textfield is empty, but it's ok
		catch (final NumberFormatException e) {
			//we don't want to print an exception every time a user clears a textfield
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

		final DualPointFixedFieldConfiguration dpfc = (DualPointFixedFieldConfiguration)fieldConfig;

		//Set the correct values inside the x & y start text fields
		final ChillPoint start = dpfc.getStartCoordinate();
		xStartTextField.setText(Integer.toString(start.getX()));
		yStartTextField.setText(Integer.toString(start.getY()));

		//Set the correct values inside the x & y end text fields
		final ChillPoint end = dpfc.getEndCoordinate();
		xEndTextField.setText(Integer.toString(end.getX()));
		yEndTextField.setText(Integer.toString(end.getY()));
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
    public void positionChanged(final IFixedFieldConfiguration onElem, 
	        final ChillPoint startPoint) {
		// Not used by this composite
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.perspective.view.fixed.PositionChangeListener#positionChanged(jpl.gds.monitor.perspective.view.fixed.fields.FixedFieldConfiguration, jpl.gds.shared.swt.types.ChillPoint, jpl.gds.shared.swt.types.ChillPoint)
	 */
	@Override
    public synchronized void positionChanged(final IFixedFieldConfiguration onElem, 
	        final ChillPoint start, final ChillPoint end) {
	    try{
    	    xStartTextField.setText(Integer.toString(start.getX()));
    		yStartTextField.setText(Integer.toString(start.getY()));
    		xEndTextField.setText(Integer.toString(end.getX()));
    		yEndTextField.setText(Integer.toString(end.getY()));
	    }
	    catch(final SWTException e) {
	        //widget is disposed
	    }
	}

}
