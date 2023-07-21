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
import jpl.gds.monitor.canvas.ElementConfigurationChangeListener;
import jpl.gds.monitor.perspective.view.fixed.IFixedFieldConfiguration;
import jpl.gds.monitor.perspective.view.fixed.TwoColorConfigSupport;
import jpl.gds.perspective.view.IViewConfiguration;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.swt.ChillColorCreator;
import jpl.gds.shared.swt.types.ChillColor;

/**
 * A small color composite that contains 2 color labels 
 * (background/foreground). When a label is clicked, a color dialog 
 * appears and the color may be changed.
 */
public class TwoColorComposite extends AbstractColorComposite {

	private ChillColor defaultForeColor = new ChillColor(
	        ChillColor.ColorName.BLACK);
	private ChillColor defaultBackColor = new ChillColor(
	        ChillColor.ColorName.WHITE);

	private Composite mainComposite;
	private Color background;
	private Color foreground;
	private Text backgroundLabel;
	private Text foregroundLabel;
	private Label noneLabel;
	private boolean foregroundChanged;
	private List<CanvasElement> elements;
	
	/**
	 * Constructor: fires up the color composite
	 * @param parent Composite in which the color composite will be 
	 *               placed
	 */
	public TwoColorComposite(final Composite parent) {
		super(parent);
		createControls();
	}
	
	/**
     * Constructor: fires up the 2 color composite
     * @param parent Composite in which the two color composite 
     *               will be placed
     * @param elements the fixed page elements that are currently 
     *                selected in the canvas. This is needed to 
     *                modify/retrieve the correct configuration.
     */
    public TwoColorComposite(final Composite parent, final List<CanvasElement> elements) {
        super(parent);
        this.elements = elements;
        createControls();
    }

	/**
	 * Create the color composite GUI. Add 2 color labels for 
	 * background and foreground and their respective listeners.
	 */
	private void createControls() {

		mainComposite = new Composite(parent, SWT.NONE);
		final FormLayout fl = new FormLayout();
		fl.spacing = 5;
		mainComposite.setLayout(fl);
		mainComposite.setBackground(backgroundColor);

		background = new Color(parent.getDisplay(), new RGB(0, 0, 0));
		foreground = new Color(parent.getDisplay(), new RGB(0, 0, 0));

		//background label
		Label nameLabel = new Label(mainComposite, SWT.NONE);
		nameLabel.setBackground(backgroundColor);
		nameLabel.setText("Background: ");
		FormData data = new FormData();
		data.left = new FormAttachment(0,100,5);
		data.top = new FormAttachment(0,9);
		nameLabel.setLayoutData(data);

		// Use a label full of spaces to show the color
		backgroundLabel = new Text(mainComposite, SWT.BORDER | SWT.READ_ONLY);
		backgroundLabel.setText("       ");
		backgroundLabel.setBackground(background);
		data = new FormData();
		data.left = new FormAttachment(nameLabel,5);
		data.top = new FormAttachment(0,100,5);
		backgroundLabel.setLayoutData(data);

		//foreground label
		nameLabel = new Label(mainComposite, SWT.NONE);
		nameLabel.setBackground(backgroundColor);
		nameLabel.setText("Foreground: ");
		data = new FormData();
		data.left = new FormAttachment(0,100,5);
		data.top = new FormAttachment(backgroundLabel,3);
		nameLabel.setLayoutData(data);

		// Use a label full of spaces to show the color
		foregroundLabel = new Text(mainComposite, SWT.BORDER | SWT.READ_ONLY);
		foregroundLabel.setText("       ");
		foregroundLabel.setBackground(foreground);
		data = new FormData();
		data.left = new FormAttachment(backgroundLabel,0,SWT.LEFT);
		data.top = new FormAttachment(backgroundLabel,0);
		data.bottom = new FormAttachment(100,-3);
		foregroundLabel.setLayoutData(data);
		
		//NONE label
		noneLabel = new Label(mainComposite, SWT.READ_ONLY);
		noneLabel.setBackground(backgroundColor);
		noneLabel.setText(DEFAULT_COLOR);
		data = new FormData();
		data.left = new FormAttachment(backgroundLabel, 26);
		data.top = new FormAttachment(backgroundLabel, 20, SWT.TOP);
		data.right = new FormAttachment(4, -5);
		noneLabel.setLayoutData(data);
		noneLabel.setVisible(false);

        if(elements != null) {
            for(final CanvasElement element : elements) {
                fieldConfigs.add(element.getFieldConfiguration());
            } 
            
            //load the default values into the 2 color GUI before the listeners
            //are added
            if(elements.size() == 1) {
                setConfiguration(elements.get(0).getFieldConfiguration());
            }  
        }

		backgroundLabel.addMouseListener(new MouseListener() {
			@Override
            public void mouseDoubleClick(final MouseEvent arg0) {
			    //intentionally empty (only mouseUp is used)
			}

			@Override
            public void mouseDown(final MouseEvent arg0) {
			    //intentionally empty (only mouseUp is used)
			}

			@Override
            public void mouseUp(final MouseEvent arg0) {
				try {
					// Create the color-change dialog
					final ColorDialog dlg = 
					    new ColorDialog(mainComposite.getShell());

					// Set the selected color in the dialog from
					// user's selected color
					dlg.setRGB(backgroundLabel.getBackground().getRGB());

					// Change the title bar text
					dlg.setText("Choose a Color");

					// Open the dialog and retrieve the selected color
					final RGB rgb = dlg.open();
					if (rgb != null) {
						// Dispose the old color, create the
						// new one, and set into the label
						background.dispose();
						background = null;
						background = new Color(parent.getDisplay(), rgb);
						backgroundLabel.setBackground(background);
						foregroundChanged=false;
						notifyChangeListeners();
						hideNoneLabel();
					}
				} catch(final Exception eE) {
					TraceManager.getDefaultTracer ().error(

					        "COLOR button caught an unhandled and " +
					        "unexpected Exception" );
					//eE.printStackTrace();
				}
			}
		});

		foregroundLabel.addMouseListener(new MouseListener() {
			@Override
            public void mouseDoubleClick(final MouseEvent arg0) {
			    //intentionally empty (only mouseUp is used)
			}

			@Override
            public void mouseDown(final MouseEvent arg0) {
			    //intentionally empty (only mouseUp is used)
			}

			@Override
            public void mouseUp(final MouseEvent arg0) {
				try {
					// Create the color-change dialog
					final ColorDialog dlg = new ColorDialog(
					        mainComposite.getShell());

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
						foreground = new Color(parent.getDisplay(), rgb);
						foregroundLabel.setBackground(foreground);
						foregroundChanged=true;
						notifyChangeListeners();
						hideNoneLabel();
					}
				} catch(final Exception eE) {
					TraceManager.getDefaultTracer ().error(

					        "COLOR button caught an unhandled and " +
					        "unexpected Exception");
					//eE.printStackTrace();
				}
			}
		});
	}
	
	/**
	 * Helper method: a color value in the composite has changed 
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
	        ((TwoColorConfigSupport)elem.getFieldConfiguration()).
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
	    
	    //foreground
        if(foregroundChanged) {
            final Color foreground = foregroundLabel.getBackground();
            final ChillColor chillForeground = new ChillColor(
                    foreground.getRed(), 
                    foreground.getGreen(), 
                    foreground.getBlue());
            ((TwoColorConfigSupport)fieldConfig).setForeground(
                    chillForeground);
        }
	    //background
        else {
	        final Color background = backgroundLabel.getBackground();
    		final ChillColor chillBackground = new ChillColor(
    		        background.getRed(), 
    		        background.getGreen(), 
    		        background.getBlue());
    		((TwoColorConfigSupport)fieldConfig).setBackground(
    		        chillBackground);
	    }

		return fieldConfig;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.ElementConfigurationComposite#setConfiguration(jpl.gds.monitor.perspective.view.fixed.fields.FixedFieldConfiguration)
	 */
	@Override
    public void setConfiguration(final IFixedFieldConfiguration config) {
		fieldConfig = config;

		final TwoColorConfigSupport twoConfig = (TwoColorConfigSupport)fieldConfig;
		Color old = foregroundLabel.getBackground();
		if (old != null && !old.isDisposed()) {
			old.dispose();			
		}
		 old = backgroundLabel.getBackground();
			if (old != null && !old.isDisposed()) {
				old.dispose();			
			}
		if (twoConfig.getBackground() == null) {
			backgroundLabel.setBackground(ChillColorCreator.getColor(
					defaultBackColor));
		} else {
			backgroundLabel.setBackground(ChillColorCreator.getColor(
					twoConfig.getBackground()));
		}
		if (twoConfig.getForeground() == null) {
			foregroundLabel.setBackground(ChillColorCreator.getColor(
					defaultForeColor));
		} else {
			foregroundLabel.setBackground(ChillColorCreator.getColor(
					twoConfig.getForeground()));
		}

		if(twoConfig.usesDefaultColors()) {
			noneLabel.setVisible(true);
		}
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.monitor.fixedbuilder.palette.AbstractColorComposite#getListeners()
	 */
	@Override
	public List<ElementConfigurationChangeListener> getListeners()
	{
		return listeners;
	}


	/**
	 * {@inheritDoc}
	 * @see jpl.gds.monitor.fixedbuilder.palette.AbstractComposite#configurationChanged(jpl.gds.perspective.view.ViewConfiguration)
	 */
	@Override
	public void configurationChanged(final IViewConfiguration config) {
		defaultForeColor = config.getForegroundColor();
		defaultBackColor = config.getBackgroundColor();

		if (elements != null) {
			for(final CanvasElement elem : elements) {
    		    final IFixedFieldConfiguration fc = elem.getFieldConfiguration();
    			setConfiguration(fc);
			}
		}
	}

}
