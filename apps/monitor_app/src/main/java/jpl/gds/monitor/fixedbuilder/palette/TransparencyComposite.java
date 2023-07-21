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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import jpl.gds.monitor.canvas.CanvasElement;
import jpl.gds.monitor.perspective.view.fixed.IFixedFieldConfiguration;
import jpl.gds.monitor.perspective.view.fixed.TransparencyConfigSupport;
import jpl.gds.shared.log.TraceManager;

/**
 * A small transparency composite that contains a transparency toggle which 
 * can be set to on or off.

 */
public class TransparencyComposite extends AbstractComposite{

	private Composite mainComposite;
	private Button transparentOnButton;
	private Button transparentOffButton;

	private List<CanvasElement> elements;

	/**
	 * Constructor: fires up the channel composite
	 * @param parent Composite in which the channel composite will be placed
	 */
	public TransparencyComposite(final Composite parent) {
		super(parent);
		createControls();
	}
	
	/**
     * Constructor: fires up the transparency composite
     * @param parent Composite in which the transparency composite 
     *               will be placed
     * @param elements the fixed page elements that are currently 
     *                selected in the canvas. This is needed to 
     *                modify/retrieve the correct configuration.
     */
    public TransparencyComposite(final Composite parent, 
            final List<CanvasElement> elements) {
        super(parent);
        this.elements = elements;
        createControls();
    }

	/**
	 * Create the transparency composite GUI. Add 2 radio buttons 
	 * for transparency on and off and their respective listeners.
	 */
	private void createControls() {

		mainComposite = new Composite(parent, SWT.NONE);
		final FormLayout fl = new FormLayout();
		fl.spacing = 5;
		mainComposite.setLayout(fl);
		mainComposite.setBackground(backgroundColor);

		//Text label
		final Label label = new Label (mainComposite, SWT.NONE);
		label.setBackground(backgroundColor);
		label.setText("Transparency: ");
		FormData data = new FormData();
		data.left = new FormAttachment(0, 100, 5);
		label.setLayoutData(data);

		//transparent ON button
		transparentOnButton = new Button(mainComposite, SWT.RADIO);
		transparentOnButton.setBackground(backgroundColor);
		transparentOnButton.setText("On");
		data = new FormData();
		data.left = new FormAttachment(label);
		transparentOnButton.setLayoutData(data);

		//transparent OFF button
		transparentOffButton = new Button(mainComposite, SWT.RADIO);
		transparentOffButton.setBackground(backgroundColor);
		transparentOffButton.setText("Off");
		data = new FormData();
		data.left = new FormAttachment(transparentOnButton, 5);
		data.bottom = new FormAttachment(100, -5);
		data.right = new FormAttachment(3, -5);
		transparentOffButton.setLayoutData(data);
		
		if(elements != null) {
            for(final CanvasElement element : elements) {
                fieldConfigs.add(element.getFieldConfiguration());
            } 
            
            //load the default values into the transparency GUI before the 
            //listeners are added
            if(elements.size() == 1) {
                setConfiguration(elements.get(0).getFieldConfiguration());
            }
        }

		transparentOnButton.addSelectionListener(new SelectionListener(){

			@Override
			public void widgetDefaultSelected(final SelectionEvent arg0) {
                  // Empty method required for interface
			}

			@Override
			public void widgetSelected(final SelectionEvent arg0) {
				try {
					notifyChangeListeners();
				} catch (final Exception e) {
					TraceManager.getDefaultTracer ().error

					("TRANSPARENCY on button caught unhandled and unexpected " +
					"exception in TransparencyComposite.java");
				}
			}

		});

		transparentOffButton.addSelectionListener(new SelectionListener(){

			@Override
			public void widgetDefaultSelected(final SelectionEvent arg0) {
				 // Empty method required for interface
			}

			@Override
			public void widgetSelected(final SelectionEvent arg0) {
				try {
					notifyChangeListeners();
				} catch (final Exception e) {
					TraceManager.getDefaultTracer ().error

					("TRANSPARENCY off button caught unhandled and unexpected " +
					"exception in TransparencyComposite.java");
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

		final TransparencyConfigSupport fcs = (TransparencyConfigSupport)fieldConfig;
		fcs.setTransparent(transparentOnButton.getSelection());

		return fieldConfig;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.ElementConfigurationComposite#setConfiguration(jpl.gds.monitor.perspective.view.fixed.fields.FixedFieldConfiguration)
	 */
	@Override
	public void setConfiguration(final IFixedFieldConfiguration config) {
		fieldConfig = config;
		final TransparencyConfigSupport fcs = (TransparencyConfigSupport)fieldConfig;
		transparentOnButton.setSelection(fcs.isTransparent());
		transparentOffButton.setSelection(!fcs.isTransparent());		
	}

}
