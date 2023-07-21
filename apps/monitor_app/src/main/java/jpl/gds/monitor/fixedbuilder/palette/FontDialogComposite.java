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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import jpl.gds.monitor.canvas.CanvasElement;
import jpl.gds.monitor.canvas.ElementConfigurationChangeListener;
import jpl.gds.monitor.perspective.view.fixed.FontConfigSupport;
import jpl.gds.monitor.perspective.view.fixed.IFixedFieldConfiguration;
import jpl.gds.monitor.perspective.view.fixed.IFixedLayoutViewConfiguration;
import jpl.gds.perspective.view.IViewConfiguration;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.swt.types.ChillFont;

/**
 * A small font composite that contains 3 combo boxes in which the font name, 
 * size and style may be selected.
 */
public class FontDialogComposite extends AbstractComposite{
	private static final String DEFAULT_FONT = "[DEFAULT]";

	/**
	 * Font size options (note: a user can enter a different font in the combo 
	 * box because it's not read-only)
	 */
	private final String[] sizes = 
	{"8", "9", "10", "11", "12", "14", "16", "18", "20", 
			"22", "24", "26", "28", "36", "48", "72", "96"};
	
	private final String[] styles = {"Normal", "Italic", "Bold"};
	private Color defaultBackgroundColor;
	private ChillFont defaultFont = IFixedLayoutViewConfiguration.DEFAULT_FONT;

	private Composite mainComposite;
	private Label sizeLabel;
	private Label styleLabel;
	private Combo nameCombo;
	private Combo sizeCombo;
	private Combo styleCombo;
	private Button reverseButton;
	private Label noneLabel;
	private List<CanvasElement> elements;
	
	private boolean reverseFlagEnabled = true;
	private boolean nameChanged;
	private boolean sizeChanged;
	private boolean styleChanged;

	/**
	 * Constructor: create a mini font dialog composite in the parent 
	 * component
	 * @param parent the composite in which the font composite will reside
	 * @param reverseEnabled true if the font composite should have a reverse video check box
	 */
	public FontDialogComposite(final Composite parent, final boolean reverseEnabled) {
		super(parent);
		reverseFlagEnabled = reverseEnabled;
		createControls();
	}

	/**
	 * Constructor: create a mini font dialog composite for the 
	 * specified element in the given parent component
	 * @param parent composite in which the font composite will reside
	 * @param elements list of elements that are currently selected on the 
	 * canvas
	 */
	public FontDialogComposite(final Composite parent, final List<CanvasElement> elements) {
		super(parent);
		this.elements = elements;
		createControls();
	}

	/**
	 * Create the font composite GUI. Add 3 combo boxes for font
	 * name, size and style and their respective listeners.
	 */
	private void createControls() {

		mainComposite = new Composite(parent, SWT.NONE);
		final FormLayout fl = new FormLayout();
		fl.spacing=5;
		mainComposite.setLayout(fl);
		defaultBackgroundColor = mainComposite.getBackground();
		mainComposite.setBackground(backgroundColor);

		//font name combo
		nameCombo = new Combo(mainComposite, SWT.READ_ONLY);
		populateFontNames();
		FormData data = new FormData();
		data.left = new FormAttachment(0, 100, 5);
		data.top = new FormAttachment(0,3);
		data.right = new FormAttachment(50, -5);
		nameCombo.setLayoutData(data);

		//font size label
		sizeLabel = new Label(mainComposite, SWT.READ_ONLY);
		sizeLabel.setBackground(backgroundColor);
		sizeLabel.setText("Size:");
		data = new FormData();
		data.left = new FormAttachment(nameCombo,4, SWT.LEFT);
		data.top = new FormAttachment(nameCombo, 5);
		sizeLabel.setLayoutData(data);

		//font size combo
		sizeCombo = new Combo(mainComposite, SWT.NONE);
		populateFontSizes();
		data = new FormData();
		data.left = new FormAttachment(sizeLabel,0);
		data.top = new FormAttachment(nameCombo, 0);
		data.right = new FormAttachment(0, 105);
		sizeCombo.setLayoutData(data);

		//font style label
		styleLabel = new Label(mainComposite, SWT.READ_ONLY);
		styleLabel.setBackground(backgroundColor);
		styleLabel.setText("Style:");
		data = new FormData();
		data.left = new FormAttachment(sizeCombo, 5);
		data.top = new FormAttachment(nameCombo, 5);
		styleLabel.setLayoutData(data);

		//font style combo
		styleCombo = new Combo(mainComposite, SWT.READ_ONLY);
		populateStyles();
		data = new FormData();
		data.left = new FormAttachment(styleLabel, 0);
		data.top = new FormAttachment(nameCombo,0);
		data.right = new FormAttachment(50, -5);
		styleCombo.setLayoutData(data);

		//reverse video flag check box
		if (reverseFlagEnabled) {
			reverseButton = new Button(mainComposite, SWT.CHECK);
			reverseButton.setBackground(backgroundColor);
			reverseButton.setText("Reverse Video");
			data = new FormData();
			data.left = new FormAttachment(nameCombo, 3, SWT.LEFT);
			data.top = new FormAttachment(sizeLabel,5);
			data.bottom = new FormAttachment(100, -3);
			reverseButton.setLayoutData(data);
		}

		//NONE label
		noneLabel = new Label(mainComposite, SWT.READ_ONLY);
		noneLabel.setBackground(backgroundColor);
		noneLabel.setText(DEFAULT_FONT);
		data = new FormData();
		data.left = new FormAttachment(reverseButton == null ? styleCombo : 
		    reverseButton, 40);
		data.top = new FormAttachment(sizeLabel, 8);
		data.right = new FormAttachment(50, -5);
		noneLabel.setLayoutData(data);
		noneLabel.setVisible(false);

		if(elements != null)
		{
		    for(final CanvasElement element : elements) {
                fieldConfigs.add(element.getFieldConfiguration());
            }
            
            //load the default values into the font GUI before the listeners 
            //are added
            if(elements.size() == 1) {
                setConfiguration(elements.get(0).getFieldConfiguration());
            } 
		}

		nameCombo.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(
			        final org.eclipse.swt.events.SelectionEvent e) {
				try {
					nameChanged = true;
				    notifyChangeListeners();
					hideNoneLabel();
					nameChanged = false;
				} catch(final Exception eE){
					TraceManager.getDefaultTracer ().error

					("Name combo caught unhandled and unexpected exception " +
					"in FontDialogComposite.java");
				}
			}
		});

		sizeCombo.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(
			        final org.eclipse.swt.events.SelectionEvent e) {
				try {
					sizeChanged = true;
				    notifyChangeListeners();
					hideNoneLabel();
					sizeChanged = false;
				} catch(final Exception eE){
					TraceManager.getDefaultTracer ().error

					("Size combo caught unhandled and unexpected" +
					" exception in FontDialogComposite.java");
				}
			}
		});

		styleCombo.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				try {
					styleChanged = true;
				    notifyChangeListeners();
					hideNoneLabel();
					styleChanged = false;
				} catch(final Exception eE){
					TraceManager.getDefaultTracer ().error

					("Style combo caught unhandled and unexpected " +
					"exception in FontDialogComposite.java");
				}
			}
		});

		if (reverseButton != null) {
			reverseButton.addSelectionListener(new SelectionAdapter(){
				@Override
				public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
					try {
					    notifyChangeListeners();
						hideNoneLabel();
					} catch(final Exception eE){
						TraceManager.getDefaultTracer ().error

						("Reverse button caught unhandled and unexpected " +
						"exception in FontDialogComposite.java");
					}
				}
			});
		}
	}

	/**
	 * Helper method: a font value in the composite has changed 
	 * so the element is no longer using the default font.
	 * 
	 * This method tells the configuration that the default font is 
	 * no longer being used
	 */
	private void hideNoneLabel() {
		if(elements==null)
		{
			return;
		}
		for(final CanvasElement elem : elements) {
            ((FontConfigSupport)elem.getFieldConfiguration()).usesDefaultFont(
                    false);
        }
        noneLabel.setVisible(false);
	}

	/**
	 * Add list of offered font names to the font name combo box
	 */
	public void populateFontNames()
	{
		//need to alphabetize and remove duplicates
		final ArrayList<String> allFonts = new ArrayList<String>();

		//display all scalable fonts in the system
		final FontData[] fd = mainComposite.getDisplay().getFontList(null, true);
		for( int i = 0; i < fd.length; i++ ) {
			allFonts.add(fd[i].getName());    
		}

		//Remove duplicates: List order not maintained
		removeDuplicates(allFonts);
		
		//Alphabetize list
		Collections.sort(allFonts);
		
		/*Remove problematic Tahoma font from list*/
		//there are 2 Tahoma fonts in the list the first of which has invisible
		//characters in the string so removeDuplicates() fails to remove it. 
		//It causes errors in the xml that is generated.
		//This only occurs on MAC not Linux
		if(allFonts.contains("Tahoma")) {
		    allFonts.remove(allFonts.indexOf("Tahoma")-1);
		}
		
		for( int i = 0; i < allFonts.size(); i++ ) {
			nameCombo.add(allFonts.get(i));
		}
	}

	/**
	 * Remove duplicates from unsorted list
	 * @param list an arraylist of strings
	 * @return an arraylist of strings with all duplicate values 
	 * removed
	 */
	public ArrayList<String> removeDuplicates(final ArrayList<String> list)
	{
		final Set<String> h = new HashSet<String>(list);
		list.clear();
		list.addAll(h);
		return list;
	}

	/**
	 * Add list of offered sizes to the size combo box
	 */
	public void populateFontSizes()
	{
		for( int i = 0; i < sizes.length; i++ ) {
			sizeCombo.add(sizes[i]);
		}
	}

	/**
	 * Add list of offered styles to the style combo box
	 */
	public void populateStyles()
	{
		for( int i = 0; i < styles.length; i++ ) {
			styleCombo.add(styles[i]);
		}
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
	    final FontConfigSupport fs = (FontConfigSupport)fieldConfig;
	    ChillFont newFont;
	    if(fs.getFont() != null) {
	        newFont = getFont(fs.getFont());
	    } else {
	        newFont = getFont(fieldConfig.getDefaultFont());
	    }
		fs.setFont(newFont);
		return fieldConfig;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.ElementConfigurationComposite#setConfiguration(jpl.gds.monitor.perspective.view.fixed.fields.FixedFieldConfiguration)
	 */
	@Override
	@SuppressWarnings({"BC_UNCONFIRMED_CAST","STYLE"})
	public void setConfiguration(final IFixedFieldConfiguration config) {
		fieldConfig = config;
		final FontConfigSupport fs = (FontConfigSupport)fieldConfig;
		setFont(fs.getFont());

		if(fs.usesDefaultFont()) {
			noneLabel.setVisible(true);
		}
	}

	/**
	 * Sets the current font fields from the given ChillFont object.
	 * 
	 * @param font ChillFont object, or null to set defaults
	 */
	public void setFont(ChillFont font) {
		if (font == null) {
			font = defaultFont;
		}
		if(font.getFace() == null) {
			font.setFace(defaultFont.getFace());
		}
		if(font.getSize() < 0 && font.getSize() >= 100) {
			font.setSize(defaultFont.getSize());
		}
		if(font.getStyle() != SWT.NONE
				&& font.getStyle() != SWT.BOLD 
				&& font.getStyle() != SWT.ITALIC 
				&& font.getStyle() != ChillFont.UNDERLINE) {
			font.setStyle(defaultFont.getStyle());
		}
		nameCombo.select(nameCombo.indexOf(font.getFace()));
		sizeCombo.select(sizeCombo.indexOf(String.valueOf(font.getSize())));
		switch(font.getStyle()) {
		case SWT.NONE:
			styleCombo.setText("Normal");
			break;
		case SWT.ITALIC:
			styleCombo.setText("Italic");
			break;
		case SWT.BOLD:
			styleCombo.setText("Bold");
			break;
		case ChillFont.UNDERLINE:
			styleCombo.setText("Underline");
			break;
		}

		if(font.getReverseFlag() && reverseButton != null){
			reverseButton.setSelection(true);
		} else if (reverseButton != null) {
			reverseButton.setSelection(false);
		}
	}

	/**
	 * Retrieves a ChillFont object created from the selections made 
	 * by the user in this composite.
	 * @return ChillFont object
	 */
	public ChillFont getFont() {
		if(reverseButton != null && reverseButton.getSelection()) {
			return new ChillFont(nameCombo.getText() + "," 
					+ sizeCombo.getText() + ","
					+ styleCombo.getText().toUpperCase() + ","
					+ "REVERSE");
		}
		else {
			return new ChillFont(nameCombo.getText() + "," 
					+ sizeCombo.getText() + ","
					+ styleCombo.getText().toUpperCase());
		}
	}
	
	/**
     * Retrieves a ChillFont object created from the selection made 
     * by the user in this composite.
     * @param font the previous ChillFont object
     * @return ChillFont object
     */
    public ChillFont getFont(final ChillFont font) {
        if(nameChanged) {
            return new ChillFont(nameCombo.getText() + "," 
                    + font.getSize() + ","
                    + font.getStyleName()
                    + (font.getReverseFlag() ? ",REVERSE" : ""));
        } else if(sizeChanged) {
            return new ChillFont(font.getFace() + "," 
                    + sizeCombo.getText() + ","
                    + font.getStyleName()
                    + (font.getReverseFlag() ? ",REVERSE" : ""));
        } else if(styleChanged) {
            return new ChillFont(font.getFace() + "," 
                    + font.getSize() + ","
                    + styleCombo.getText().toUpperCase()
                    + (font.getReverseFlag() ? ",REVERSE" : ""));
        } else {
            return new ChillFont(font.getFace() + "," 
                    + font.getSize() + ","
                    + font.getStyleName()
                    + (reverseButton.getSelection() ? ",REVERSE" : ""));
        }
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

	/**
	 * Sets the background colors of the composite and the widgets 
	 * within back to the original color
	 */
	public void useDefaultColors() {
		mainComposite.setBackground(defaultBackgroundColor);
		sizeLabel.setBackground(defaultBackgroundColor);
		styleLabel.setBackground(defaultBackgroundColor);
		if (reverseButton != null) {
			reverseButton.setBackground(defaultBackgroundColor);
		}
		noneLabel.setBackground(defaultBackgroundColor);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.monitor.fixedbuilder.palette.AbstractComposite#configurationChanged(jpl.gds.perspective.view.ViewConfiguration)
	 */
	@Override
	public void configurationChanged(final IViewConfiguration config) {
	    defaultFont = config.getDataFont();
        if ((elements != null) && (elements.size() == 1)) {
            final IFixedFieldConfiguration fc = 
                elements.get(0).getFieldConfiguration();
            final FontConfigSupport fs = (FontConfigSupport)fc;
            setFont(fs.getFont());

            if(fs.usesDefaultFont()) {
                noneLabel.setVisible(true);
            }
        }
	}

}
