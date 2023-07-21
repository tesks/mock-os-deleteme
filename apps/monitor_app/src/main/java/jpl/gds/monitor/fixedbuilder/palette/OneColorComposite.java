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
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import jpl.gds.monitor.canvas.CanvasElement;
import jpl.gds.monitor.perspective.view.fixed.IFixedFieldConfiguration;
import jpl.gds.monitor.perspective.view.fixed.OneColorConfigSupport;
import jpl.gds.perspective.view.IViewConfiguration;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.swt.ChillColorCreator;
import jpl.gds.shared.swt.types.ChillColor;

/**
 * A small color composite that contains a single color (foreground).
 * When the label is clicked, a color dialog appears and the color 
 * may be changed.
 */
public class OneColorComposite extends AbstractColorComposite{

	private ChillColor defaultForeColor = 
	    new ChillColor(ChillColor.ColorName.BLACK);

	private Composite mainComposite;
	private Color foreground;
	private Text foregroundLabel;
	private Label noneLabel;

	private List<CanvasElement> elements;

	/**
	 * Constructor: fires up the color composite
	 * @param parent Composite in which the color composite will be 
	 *               placed
	 */
	public OneColorComposite(final Composite parent) {
		super(parent);
		createControls();
	}
	
	/**
     * Constructor: fires up the one color composite
     * @param parent Composite in which the one color composite 
     *               will be placed
     * @param elements the fixed page elements that are currently 
     *                selected in the canvas. This is needed to 
     *                modify/retrieve the correct configuration.
     */
    public OneColorComposite(final Composite parent,final List<CanvasElement> elements){
        super(parent);
        this.elements = elements;
        createControls();
    }


	/**
	 * Create the color composite GUI. Add 1 color label for 
	 * foreground and its respective listener.
	 */
	private void createControls() {

		mainComposite = new Composite(parent, SWT.NONE);
		mainComposite.setLayout(new FormLayout());
		mainComposite.setBackground(backgroundColor);

		foreground = new Color(parent.getDisplay(), new RGB(0, 0, 0));

		//foreground label
		final Label nameLabel = new Label(mainComposite, SWT.NONE);
		nameLabel.setBackground(backgroundColor);
		nameLabel.setText("Foreground: ");
		FormData data = new FormData();
		data.left = new FormAttachment(0,100,5);
		data.top = new FormAttachment(0,9);
		nameLabel.setLayoutData(data);

		// Use a label full of spaces to show the color
		foregroundLabel = new Text(mainComposite, SWT.BORDER | SWT.READ_ONLY);
		foregroundLabel.setText("       ");
		foregroundLabel.setBackground(foreground);
		data = new FormData();
		data.left = new FormAttachment(nameLabel,5);
		data.top = new FormAttachment(0,100,3);
		data.bottom = new FormAttachment(100, -5);
		foregroundLabel.setLayoutData(data);

		//NONE label
		noneLabel = new Label(mainComposite, SWT.READ_ONLY);
		noneLabel.setBackground(backgroundColor);
		noneLabel.setText(DEFAULT_COLOR);
		data = new FormData();
		data.left = new FormAttachment(foregroundLabel, 20);
		data.top = new FormAttachment(foregroundLabel, 5, SWT.TOP);
		data.right = new FormAttachment(4, -5);
		noneLabel.setLayoutData(data);
		noneLabel.setVisible(false);

		
        if(elements != null) {
            for(final CanvasElement element : elements) {
                fieldConfigs.add(element.getFieldConfiguration());
            }
            
            //load the default values into the color GUI before the listeners 
            //are added
            if(elements.size() == 1) {
                setConfiguration(elements.get(0).getFieldConfiguration());
            } 
        }

		foregroundLabel.addMouseListener(new MouseListener() {
			@Override
            public void mouseDoubleClick(final MouseEvent arg0) {
				//intentionally empty (only need mouseUp)
			}

			@Override
            public void mouseDown(final MouseEvent arg0) {
				//intentionally empty (only need mouseUp)
			}

			@Override
            public void mouseUp(final MouseEvent arg0) {
				try {
					// Create the color-change dialog
					final ColorDialog dlg = new ColorDialog(mainComposite.
					        getShell());

					// Set the selected color in the dialog from
					// user's selected color
					dlg.setRGB(foregroundLabel.getBackground().getRGB());

					// Change the title bar text
					dlg.setText("Choose a Color");

					// Open the dialog and retrieve the selected color
					final RGB rgb = dlg.open();
					if (rgb != null) {
						// Dispose the old color, create the
						// new one, and set into the label
						foreground.dispose();
						foreground = null;
						foreground = new Color(parent.getDisplay(), rgb);
						foregroundLabel.setBackground(foreground);
						notifyChangeListeners();
						hideNoneLabel();
					}
				} catch(final Exception eE) {
					TraceManager.getDefaultTracer ().error

					("Color button caught an unhandled and unexpected" +
					" Exception");
				}
			}
		});
	}

	/**
	 * helper method: a color value in the composite has changed 
	 * so the element is no longer using the default color.
	 * 
	 * This method tells the configuration that the default color is 
	 * no longer being used
	 */
	private void hideNoneLabel() {
	    if(elements == null) {
            return;
        }
        for(final CanvasElement elem : elements) {
            ((OneColorConfigSupport)elem.getFieldConfiguration()).
            usesDefaultColors(false);
        }
	    
		noneLabel.setVisible(false);
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

		final OneColorConfigSupport ocs = (OneColorConfigSupport)fieldConfig;
		final ChillColor fgColor = new ChillColor(foreground.getRed(), 
				foreground.getGreen(), foreground.getBlue());

		ocs.setForeground(fgColor);

		return fieldConfig;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.ElementConfigurationComposite#setConfiguration(jpl.gds.monitor.perspective.view.fixed.fields.FixedFieldConfiguration)
	 */
	@Override
    public void setConfiguration(final IFixedFieldConfiguration config) {
		fieldConfig = config;
		final OneColorConfigSupport ocs = (OneColorConfigSupport)fieldConfig;

		final Color old = foregroundLabel.getBackground();
		if (old != null && !old.isDisposed()) {
			old.dispose();			
		}
		
		if(ocs.getForeground() == null)
		{
			foregroundLabel.setBackground(ChillColorCreator.getColor(
			        defaultForeColor));
		} else {
			foregroundLabel.setBackground(ChillColorCreator.getColor(
			        ocs.getForeground()));
		}

		if(ocs.usesDefaultColors()) {
			noneLabel.setVisible(true);
		}
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.monitor.fixedbuilder.palette.AbstractComposite#configurationChanged(jpl.gds.perspective.view.ViewConfiguration)
	 */
	@Override
	public void configurationChanged(final IViewConfiguration config) {
		defaultForeColor = config.getForegroundColor();

		if ((elements != null) && (elements.size() == 1)) {
			final IFixedFieldConfiguration fc = elements.get(0).
			getFieldConfiguration();
			setConfiguration(fc);
		}
	}
}
