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
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import jpl.gds.monitor.canvas.CanvasElement;
import jpl.gds.monitor.perspective.view.fixed.IFixedFieldConfiguration;
import jpl.gds.monitor.perspective.view.fixed.TextConfigSupport;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.swt.SWTUtilities;

/**
 * A small text composite that contains a textfield in which the 
 * desired text may be entered.
 */
public class TextComposite extends AbstractComposite{

	private Composite mainComposite;
	private Text textfield;

	private List<CanvasElement> elements;

	/**
	 * Constructor: fires up the text composite
	 * @param parent Composite in which the text composite will be 
	 *               placed
	 */
	public TextComposite(final Composite parent) {
		super(parent);
		createControls();
	}

	/**
	 * Constructor: fires up the text composite
	 * @param parent Composite in which the text composite will be placed
	 * @param elements the fixed page elements that are currently selected in the 
	 *                canvas. This is needed to modify/retrieve the correct 
	 *                configuration.
	 */
	public TextComposite(final Composite parent, final List<CanvasElement> elements) {
		super(parent);
		this.elements = elements;
		createControls();
	}

	/**
	 * Create the text composite GUI. Add a textfield for entering 
	 * text and its respective listener.
	 */
	private void createControls() {

		mainComposite = new Composite(parent, SWT.NONE);
		mainComposite.setLayout(new FormLayout());
		mainComposite.setBackground(backgroundColor);

		//Text label
		final Label label = new Label (mainComposite, SWT.NONE);
		label.setBackground(backgroundColor);
		label.setText("Text: ");
		FormData data = new FormData();
		data.left = new FormAttachment(0, 100, 5);
		data.top = new FormAttachment(0, 10);
		label.setLayoutData(data);

		//Textfield
		textfield = new Text (mainComposite, SWT.BORDER);
		data = SWTUtilities.getFormData(textfield, 1, 29);
		data.top = new FormAttachment(0, 100, 5);
		data.left = new FormAttachment(label, 5);
		data.right = new FormAttachment(4, -5);
		data.bottom = new FormAttachment(100, -5);
		textfield.setLayoutData(data);

		
		if(elements != null)
		{
		    for(final CanvasElement element : elements) {
                fieldConfigs.add(element.getFieldConfiguration());
            }
            
            //load the default values into the channel GUI before the listeners
            //are added
            if(elements.size() == 1) {
                setConfiguration(elements.get(0).getFieldConfiguration());
            }  
		}

		textfield.addKeyListener(new KeyListener() {
			@Override
            public void keyPressed(final KeyEvent arg0) {
				//intentionally empty (using keyReleased instead)
			}

			@Override
            public void keyReleased(final KeyEvent arg0) {
				try {
					notifyChangeListeners();
				} catch (final Exception e) {
					TraceManager.getDefaultTracer ().error

					("TEXT textfield caught unhandled and unexpected " +
					"exception in TextComposite.java");
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

		final TextConfigSupport tcs = (TextConfigSupport)fieldConfig;
		tcs.setText(textfield.getText());
		return fieldConfig;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.ElementConfigurationComposite#setConfiguration(jpl.gds.monitor.perspective.view.fixed.fields.FixedFieldConfiguration)
	 */
	@Override
    public void setConfiguration(final IFixedFieldConfiguration config) {
		fieldConfig = config;
		final TextConfigSupport tcs = (TextConfigSupport)config;
		textfield.setText(tcs.getText() == null ? "" : tcs.getText());
	}

}
