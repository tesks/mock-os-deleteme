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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import jpl.gds.monitor.canvas.CanvasElement;
import jpl.gds.monitor.canvas.ElementConfigurationChangeListener;
import jpl.gds.monitor.perspective.view.fixed.IFixedFieldConfiguration;
import jpl.gds.monitor.perspective.view.fixed.fields.ImageFieldConfiguration;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.swt.SWTUtilities;

/**
 * An image composite that contains a textfield and a browse button in
 * which the image path may be selected.
 */
public class ImageComposite extends AbstractComposite{

    /**
     * Permitted extensions in the file chooser. More can be added as needed.
     */
    private final String[] extensions = 
	{"*.png", "*.gif", "*.jpg", "*.tiff", "*.bmp"};

	private Composite mainComposite;
	private Text textfield;
	private Button button;

	private List<CanvasElement> elements;

	/**
	 * Constructor: fires up the image composite
	 * @param parent Composite in which the image composite will be 
	 * placed
	 */
	public ImageComposite(final Composite parent) {
		super(parent);
		createControls();
	}

	/**
	 * Constructor: fires up the image composite
	 * @param parent Composite in which the image composite will be 
	 *               placed
	 * @param elements the fixed page element that is currently 
	 *                selected in the canvas. This is needed to 
	 *                modify/retrieve the correct configuration.
	 */
	public ImageComposite(final Composite parent, final List<CanvasElement> elements) {
		super(parent);
		this.elements = elements;
		createControls();
	}

	/**
	 * Create the image composite GUI. Add a textfield to display 
	 * the current file path, a browse button and their respective 
	 * listeners.
	 */
	private void createControls() {

		mainComposite = new Composite(parent, SWT.NONE);
		mainComposite.setLayout(new FormLayout());
		mainComposite.setBackground(backgroundColor);

		//Textfield
		textfield = new Text (mainComposite, SWT.BORDER);
		textfield.setEditable(false);
		FormData data = SWTUtilities.getFormData(textfield, 1, 35);
		data.top = new FormAttachment(0, 5);
		data.left = new FormAttachment(0, 100,5);
		data.right = new FormAttachment(4, -5);
		textfield.setLayoutData(data);

		//Button
		button = new Button(mainComposite, SWT.PUSH);
		button.setText("Browse");
		data = new FormData();
		data.top = new FormAttachment(textfield, 0, -5);
		data.right = new FormAttachment(4, -5);
		data.bottom = new FormAttachment(100, -5);
		button.setLayoutData(data);

		
		if(elements != null)
		{
		    for(final CanvasElement element : elements) {
                fieldConfigs.add(element.getFieldConfiguration());
            }
            
            //load the default values into the image GUI before the listeners 
            //are added
            if(elements.size() == 1) {
                setConfiguration(elements.get(0).getFieldConfiguration());
            }
		}

		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(
			        final org.eclipse.swt.events.SelectionEvent e) {
				try {
					final SWTUtilities swt = new SWTUtilities();
					
					final String path = swt.displayStickyFileChooser(false, 
					        mainComposite.getShell(), 
					        "ImageComposite", extensions);
					
					if (path != null) {
						//set the text field to the user specified path
						textfield.setText(path);
						notifyChangeListeners();
					}
				} catch(final Exception eE){
					TraceManager.getDefaultTracer ().error

					("BROWSE button caught unhandled and unexpected" +
							" exception in ImageComposite.java");
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
	    final ImageFieldConfiguration image = ((ImageFieldConfiguration)fieldConfig);
	    
		final String imagePath = textfield.getText();

		if (imagePath != null) {
			image.setImagePath(imagePath);

			//Note: the draw() method in ImageElement.java takes 
			//care of sizing the image
		}
		return image;	
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
    public void setConfiguration(final IFixedFieldConfiguration config) {
		fieldConfig = config;
		ImageFieldConfiguration image = null;

		if (elements.get(0).getFieldConfiguration() != null) {
			image = (ImageFieldConfiguration)elements.get(0).
			getFieldConfiguration();
		} else {
			image = new ImageFieldConfiguration();
		}
		textfield.setText(image.getImagePath());
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
