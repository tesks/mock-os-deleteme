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
import java.util.HashSet;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ExpandBar;
import org.eclipse.swt.widgets.ExpandItem;
import org.eclipse.swt.widgets.Shell;
import org.springframework.context.ApplicationContext;

import jpl.gds.dictionary.api.channel.IChannelDefinitionProvider;
import jpl.gds.monitor.canvas.CanvasElement;
import jpl.gds.monitor.canvas.ElementConfigurationChangeListener;
import jpl.gds.monitor.canvas.support.ButtonSupport;
import jpl.gds.monitor.canvas.support.ChannelSupport;
import jpl.gds.monitor.canvas.support.DualCoordinateSupport;
import jpl.gds.monitor.canvas.support.FontSupport;
import jpl.gds.monitor.canvas.support.FormatSupport;
import jpl.gds.monitor.canvas.support.ImageSupport;
import jpl.gds.monitor.canvas.support.LineSupport;
import jpl.gds.monitor.canvas.support.OneColorSupport;
import jpl.gds.monitor.canvas.support.SingleCoordinateSupport;
import jpl.gds.monitor.canvas.support.TextSupport;
import jpl.gds.monitor.canvas.support.TimeSupport;
import jpl.gds.monitor.canvas.support.TransparencySupport;
import jpl.gds.monitor.canvas.support.TwoColorSupport;
import jpl.gds.monitor.perspective.view.fixed.FixedFieldType;
import jpl.gds.monitor.perspective.view.fixed.IFixedLayoutViewConfiguration;
import jpl.gds.perspective.config.PerspectiveProperties;
import jpl.gds.shared.swt.SWTUtilities;

/**
 * A formatting palette for editing fixed page elements on the canvas that 
 * updates dynamically to tailor to the currently selected elements on the 
 * canvas. It is a floating window composed of several small composites 
 * (i.e. font, color, line) in an SWT ExpandBar widget.
 */
public class Palette
{
	private static final int EXPAND_ITEM_HEADER_HEIGHT = 22;
	private static final String EXPAND_ITEM_IMAGE_PATH = 
	    "jpl/gds/monitor/fixed/builder/gui/bluebox.gif";
	
	private final Shell parent;

	private final ExpandBar bar;

	private FontDialogComposite fontComp;
	private AbstractColorComposite colorComp;
	private LineStyleComposite lineComp;
	private TextComposite textComp;
	private CoordinateComposite coordinateComp;
	private TransparencyComposite transparentComp;
	private ImageComposite imageComp;
	private TimeComposite timeComp;
	private ChannelComposite channelComp;
	private AbstractFormattingComposite formatComp;
	private ButtonComposite buttonComp;
	
	private Composite fontCompParent;
	private Composite colorCompParent;
	private Composite lineCompParent;
	private Composite textCompParent;
	private Composite coordinateCompParent;
	private Composite imageCompParent;
	private Composite timeCompParent;
	private Composite channelCompParent;
	private Composite formatCompParent;
	private Composite buttonCompParent;

	private ExpandItem item0;	//Font
	private ExpandItem item1;	//Color
	private ExpandItem item2;	//Line
	private ExpandItem item3;	//Text
	private ExpandItem item4;	//Position
	private ExpandItem item5;	//Time
	private ExpandItem item6;	//Channel
	private ExpandItem item7;	//Format
	private ExpandItem item8;	//Image
	private ExpandItem item9;	//Button

	private int paletteHeight;

	private IFixedLayoutViewConfiguration viewConfig;
	private final ApplicationContext appContext;

	/**
	 * Constructor: Sets the parent shell and title of the palette and creates 
	 * an expand bar
	 * 
	 * @param parent is the shell that the palette is created in
	 */
	public Palette(final ApplicationContext appContext, final Shell parent)
	{
	    this.appContext = appContext;
		this.parent = parent;
		this.parent.setText("Palette");
		bar = new ExpandBar (parent, SWT.V_SCROLL);
		
	}

	/**
	 * Sets the fixed layout view configuration for the fixed view being configured.
	 * 
	 * @param config the FixedLayoutViewConfiguration to set
	 */
	public void setFixedLayoutViewConfiguration(final IFixedLayoutViewConfiguration config) {
		if (fontComp != null) {
			fontComp.setFixedLayoutViewConfiguration(config);
		}
		if (colorComp != null) {
			colorComp.setFixedLayoutViewConfiguration(config);
		}
		viewConfig = config;
	}

	/**
	 * Adds a listener to the composites within the palette
	 * 
	 * @param l is the listener that watches for changes in an element's configuration
	 */
	public synchronized void addChangeListener(final ElementConfigurationChangeListener l) {
		if(fontComp != null && !fontComp.getListeners().contains(l)) {
			fontComp.addChangeListener(l);
		}
		if (coordinateComp != null && !coordinateComp.getListeners().contains(l)) {
			coordinateComp.addChangeListener(l);
		}
		if(colorComp != null && !colorComp.getListeners().contains(l)) {
			colorComp.addChangeListener(l);
		}
		if(lineComp != null && !lineComp.getListeners().contains(l)) {
			lineComp.addChangeListener(l);
		}
		if(textComp != null && !textComp.getListeners().contains(l)) {
			textComp.addChangeListener(l);
		}
		if(transparentComp != null && !transparentComp.getListeners().contains(l)) {
			transparentComp.addChangeListener(l);
		}
		if(imageComp != null && !imageComp.getListeners().contains(l)) {
			imageComp.addChangeListener(l);
		}
		if(timeComp != null && !timeComp.getListeners().contains(l)) {
			timeComp.addChangeListener(l);
		}
		if(channelComp != null && !channelComp.getListeners().contains(l)) {
			channelComp.addChangeListener(l);
		}
		if(formatComp != null && !formatComp.getListeners().contains(l)) {
			formatComp.addChangeListener(l);
		}
		if(buttonComp != null && !buttonComp.getListeners().contains(l)) {
			buttonComp.addChangeListener(l);
		}
	}

	private void removeExistingComposites() {
	    cleanUp(item0, fontCompParent);
        cleanUp(item1, colorCompParent);
        cleanUp(item2, lineCompParent);
        cleanUp(item3, textCompParent);
        cleanUp(item4, coordinateCompParent);
        cleanUp(item5, timeCompParent);
        cleanUp(item6, channelCompParent);
        cleanUp(item7, formatCompParent);
        cleanUp(item8, imageCompParent);
        cleanUp(item9, buttonCompParent);
	}

	/**
	 * Helper function: Disposes the ExpandItem and Composite if they are not 
	 * null
	 * 
	 * @param item an ExpandItem in the ExpandBar palette
	 * @param parent the parent of the actual composite in the palette
	 */
	private void cleanUp(final ExpandItem item, final Composite parent)
	{
		if(item != null) {
			item.dispose();
		}

		if(parent != null) {
			parent.dispose();
		}
	}

	/**
	 * Sets attributes for an expand item
	 * 
	 * @param item the item whose attributes are being set
	 * @param title the name of the expand item
	 * @parent parent the composite that will be shown in the expand item
	 */
	private void createExpandBarItem(final ExpandItem item, final String title, final Composite parent)
	{
		item.setText(title);
		item.setHeight(parent.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
		item.setControl(parent);
		item.setExpanded(true);
		final Image clearImage = SWTUtilities.createImage(Display
				.getDefault(), EXPAND_ITEM_IMAGE_PATH);
		item.setImage(clearImage);
	}

	/**
	 * Sets the correct format composite in the palette, either date 
	 * or sprintf. This method is called if there is a change in the 
	 * channel or time composite
	 * 
	 * @param element the object that is currently selected
	 * @parent l the canvas listener that listens for changes and 
	 *         selections to objects on the canvas
	 */
	public void changeFormatComposite(final List<CanvasElement> elements, 
	        final ElementConfigurationChangeListener l)
	{
	    
	    TimeFormatType formatType = TimeFormatType.NONE;
        
	    boolean hasSupport = true;
        final List<String> supports = new ArrayList<String>();
        
        //channel
        for(final CanvasElement elem : elements) {
            if(!(elem instanceof ChannelSupport)) {
                hasSupport = false;
                break;
            }
        }
        if(hasSupport) {
            supports.add("ChannelSupport");
        }
        
        //time
        hasSupport = true;
        for(final CanvasElement elem : elements) {
            if(!(elem instanceof TimeSupport)) {
                hasSupport = false;
                break;
            }
        }
        if(hasSupport) {
            supports.add("TimeSupport");
        }
	    
        if(supports.contains("TimeSupport")) {
            formatType = timeComp.determineFormatType();
        }else if(supports.contains("ChannelSupport")) {
            formatType = channelComp.determineFormatType();
        }
        
        cleanUp(item7, formatCompParent);
        
        if(!formatType.equals(TimeFormatType.NONE)) {
    
    		formatCompParent = new Composite (bar, SWT.NONE);
    		final FormLayout layout = new FormLayout ();
    		formatCompParent.setLayout(layout);
        
            if(formatType.equals(TimeFormatType.SOL_DATE) || 
                    formatType.equals(TimeFormatType.REGULAR_DATE)) {
                formatComp = new DateFormatComposite(appContext.getBean(PerspectiveProperties.class), formatCompParent, elements, formatType);
            }
            else if(formatType.equals(TimeFormatType.STRING)) {
                formatComp = new SprintfFormatComposite(appContext.getBean(IChannelDefinitionProvider.class), formatCompParent, elements);
                formatComp.setDefaultFormat();
            }
        
            final FormData fd = new FormData();
            fd.top = new FormAttachment();
            fd.left = new FormAttachment(0);
            formatComp.getComposite().setLayoutData(fd);

            item7 = new ExpandItem (bar, SWT.NONE);
            createExpandBarItem(item7, "Format", formatCompParent);
            
            //add listeners to new formatcomposite
            if(!formatComp.getListeners().contains(l)) {
                formatComp.addChangeListener(l);
            }
        }
        setHeight();
	}
	
	/**
	 * Adjusts the height of the palette by adding up the height of each 
	 * individual expand item
	 * 
	 */
	private void setHeight() {
		paletteHeight = 0;
		if(item0 != null && !item0.isDisposed())
		{
			paletteHeight += item0.getHeight() + EXPAND_ITEM_HEADER_HEIGHT;
		}
		if(item1 != null && !item1.isDisposed())
		{
			paletteHeight += item1.getHeight() + EXPAND_ITEM_HEADER_HEIGHT;
		}
		if(item2 != null && !item2.isDisposed())
		{
			paletteHeight += item2.getHeight() + EXPAND_ITEM_HEADER_HEIGHT;
		}
		if(item3 != null && !item3.isDisposed())
		{
			paletteHeight += item3.getHeight() + EXPAND_ITEM_HEADER_HEIGHT;
		}
		if(item4 != null && !item4.isDisposed())
		{
			paletteHeight += item4.getHeight() + EXPAND_ITEM_HEADER_HEIGHT;
		}
		if(item5 != null && !item5.isDisposed())
		{
			paletteHeight += item5.getHeight() + EXPAND_ITEM_HEADER_HEIGHT;
		}
		if(item6 != null && !item6.isDisposed())
		{
			paletteHeight += item6.getHeight() + EXPAND_ITEM_HEADER_HEIGHT;
		}
		if(item7 != null && !item7.isDisposed())
		{
			paletteHeight += item7.getHeight() + EXPAND_ITEM_HEADER_HEIGHT;
		}
		if(item8 != null && !item8.isDisposed())
		{
			paletteHeight += item8.getHeight() + EXPAND_ITEM_HEADER_HEIGHT;
		}
		if(item9 != null && !item9.isDisposed())
		{
			paletteHeight += item9.getHeight() + EXPAND_ITEM_HEADER_HEIGHT;
		}
		parent.setSize(parent.getSize().x, paletteHeight + 40);
	}
	
	/**
     * Brings the palette to the front of all windows and forces it be 
     * the active window
     */
	public void makeVisible() {
		parent.setActive();
	}
	
	public List<FixedFieldType> removeDuplicates(final List<FixedFieldType> types)
    {
	    final HashSet<FixedFieldType> h = new HashSet<FixedFieldType>(types);
        types.clear();
        types.addAll(h);
        return types;
    }
	
	/**
     * Changes the formatting palette dynamically when a canvas element is selected
     * 
     * @param elements determine which composites are drawn in the palette
     */
	//I think it should work for any combination of elements selected, they 
	//don't necessarily have to be the same type. (So you can change the 
	//color of a box and line at the same time for example)
	public void updateGui(final List<CanvasElement> newElements) {
	    removeExistingComposites();
	    
	    //if no element is selected in the canvas
        //the palette will contain empty expand bar items
        if(newElements == null || newElements.isEmpty()) {
            item0 = new ExpandItem (bar, SWT.NONE);
            item0.setText("Font");
            item0.setExpanded(false);

            item1 = new ExpandItem (bar, SWT.NONE);
            item1.setText("Color");
            item1.setExpanded(false);

            item2 = new ExpandItem (bar, SWT.NONE);         
            item2.setText("Line");
            item2.setExpanded(false);

            item3 = new ExpandItem (bar, SWT.NONE);
            item3.setText("Text");
            item3.setExpanded(false);

            item4 = new ExpandItem (bar, SWT.NONE);
            item4.setText("Position");
            item4.setExpanded(false);

            item5 = new ExpandItem (bar, SWT.NONE);
            item5.setText("Time");
            item5.setExpanded(false);

            item6 = new ExpandItem (bar, SWT.NONE);
            item6.setText("Channel");
            item6.setExpanded(false);

            item7 = new ExpandItem (bar, SWT.NONE);
            item7.setText("Format");
            item7.setExpanded(false);

            item8 = new ExpandItem (bar, SWT.NONE);
            item8.setText("Image");
            item8.setExpanded(false);

            item9 = new ExpandItem (bar, SWT.NONE);
            item9.setText("Button");
            item9.setExpanded(false);

            bar.setEnabled(false);
            setHeight();
            return;
        }
	    
        boolean hasSupport;
        final List<String> supports = new ArrayList<String>();
        
        //font
        hasSupport = true;
        for(final CanvasElement elem : newElements) {
            if(!(elem instanceof FontSupport)) {
                hasSupport = false;
                break;
            }
        }
        if(hasSupport) {
            supports.add("FontSupport");
        }
        
        //button
        hasSupport = true;
        for(final CanvasElement elem : newElements) {
            if(!(elem instanceof ButtonSupport)) {
                hasSupport = false;
                break;
            }
        }
        if(hasSupport) {
            supports.add("ButtonSupport");
        }
        
        //channel
        hasSupport = true;
        for(final CanvasElement elem : newElements) {
            if(!(elem instanceof ChannelSupport)) {
                hasSupport = false;
                break;
            }
        }
        if(hasSupport) {
            supports.add("ChannelSupport");
        }
        
        //line
        hasSupport = true;
        for(final CanvasElement elem : newElements) {
            if(!(elem instanceof LineSupport)) {
                hasSupport = false;
                break;
            }
        }
        if(hasSupport) {
            supports.add("LineSupport");
        }
        
        //one color
        hasSupport = true;
        for(final CanvasElement elem : newElements) {
            if(!(elem instanceof OneColorSupport)) {
                hasSupport = false;
                break;
            }
        }
        if(hasSupport) {
            supports.add("OneColorSupport");
        }
        
        //two color
        hasSupport = true;
        for(final CanvasElement elem : newElements) {
            if(!(elem instanceof TwoColorSupport)) {
                hasSupport = false;
                break;
            }
        }
        if(hasSupport) {
            supports.add("TwoColorSupport");
        }
        
        //transparency
        hasSupport = true;
        for(final CanvasElement elem : newElements) {
            if(!(elem instanceof TransparencySupport)) {
                hasSupport = false;
                break;
            }
        }
        if(hasSupport) {
            supports.add("TransparencySupport");
        }
        
        //text
        hasSupport = true;
        for(final CanvasElement elem : newElements) {
            if(!(elem instanceof TextSupport)) {
                hasSupport = false;
                break;
            }
        }
        if(hasSupport) {
            supports.add("TextSupport");
        }
        
        //time
        hasSupport = true;
        for(final CanvasElement elem : newElements) {
            if(!(elem instanceof TimeSupport)) {
                hasSupport = false;
                break;
            }
        }
        if(hasSupport) {
            supports.add("TimeSupport");
        }
        
        //format
        hasSupport = true;
        final FixedFieldType baseType = newElements.get(0).getFieldType();
        for(final CanvasElement elem : newElements) {
            if(!(elem instanceof FormatSupport)) {
                hasSupport = false;
                break;
            }
            //TODO here is where i disallow the format composite to appear for mix of time and channel elements. why did i do this?
            //check to see they are all the same type of field
            if(baseType != elem.getFieldType()) {
                hasSupport = false;
                break;
            }
        }
        if(hasSupport) {
            supports.add("FormatSupport");
        }
        
        //image
        hasSupport = true;
        for(final CanvasElement elem : newElements) {
            if(!(elem instanceof ImageSupport)) {
                hasSupport = false;
                break;
            }
        }
        if(hasSupport) {
            supports.add("ImageSupport");
        }
        
        if(newElements.size()==1) {
        
            //coordinate
            hasSupport = true;
            for(final CanvasElement elem : newElements) {
                if(!(elem instanceof DualCoordinateSupport)) {
                    hasSupport = false;
                    break;
                }
            }
            if(hasSupport) {
                supports.add("DualCoordinateSupport");
            }
            
            //coordinate
            hasSupport = true;
            for(final CanvasElement elem : newElements) {
                if(!(elem instanceof SingleCoordinateSupport)) {
                    hasSupport = false;
                    break;
                }
            }
            if(hasSupport) {
                supports.add("SingleCoordinateSupport");
            }
        }

	    
        bar.setEnabled(true);
        
        /**********************************************************************
         * FONT
         *********************************************************************/
        if(supports.contains("FontSupport"))
        {
            fontCompParent = new Composite (bar, SWT.BORDER);
            final FormLayout layout = new FormLayout ();
            fontCompParent.setLayout(layout);

            fontComp = new FontDialogComposite(fontCompParent, newElements);
            
            fontComp.setFixedLayoutViewConfiguration(viewConfig); 

            final FormData fd = new FormData();
            fd.top = new FormAttachment();
            fd.left = new FormAttachment(0);
            fontComp.getComposite().setLayoutData(fd);

            item0 = new ExpandItem (bar, SWT.NONE);
            createExpandBarItem(item0, "Font", fontCompParent);
        }
        
        /**********************************************************************
         * LINE
         *********************************************************************/
        if(supports.contains("LineSupport")) {
            lineCompParent = new Composite (bar, SWT.BORDER);
            final FormLayout layout = new FormLayout ();
            lineCompParent.setLayout(layout);

            lineComp = new LineStyleComposite(lineCompParent, newElements);

            final FormData fd = new FormData();
            fd.top = new FormAttachment();
            fd.left = new FormAttachment(0);
            lineComp.getComposite().setLayoutData(fd);

            item2 = new ExpandItem (bar, SWT.NONE);
            createExpandBarItem(item2, "Line", lineCompParent);
        }
        
        /**********************************************************************
         * COLOR & TRANSPARENCY
         *********************************************************************/
        if(supports.contains("TwoColorSupport") || supports.contains("OneColorSupport"))
        {
            colorCompParent = new Composite(bar, SWT.BORDER);
            final FormLayout layout = new FormLayout ();
            colorCompParent.setLayout(layout);
            
            if(supports.contains("TwoColorSupport"))
            {
                colorComp = new TwoColorComposite(colorCompParent, newElements);
            }
            else if(supports.contains("OneColorSupport"))
            {
                colorComp = new OneColorComposite(colorCompParent, newElements);
            }
            
            colorComp.setFixedLayoutViewConfiguration(viewConfig);
            
            FormData fd = new FormData();
            fd = new FormData();
            fd.top = new FormAttachment();
            fd.left = new FormAttachment(0);
            colorComp.getComposite().setLayoutData(fd);

            //the transparency composite is appended to the color composite
            //since they're related
            if(supports.contains("TransparencySupport"))
            {
                transparentComp = new TransparencyComposite(colorCompParent, newElements);
                fd = new FormData();
                fd.top = new FormAttachment(colorComp.getComposite());
                fd.left = new FormAttachment(0);
                transparentComp.getComposite().setLayoutData(fd);
            }
            
            item1 = new ExpandItem (bar, SWT.NONE);
            createExpandBarItem(item1, "Color", colorCompParent);
        }
        
        /**********************************************************************
         * TEXT
         *********************************************************************/
        if(supports.contains("TextSupport"))
        {
            textCompParent = new Composite (bar, SWT.BORDER);
            final FormLayout layout = new FormLayout ();
            textCompParent.setLayout(layout);

            textComp = new TextComposite(textCompParent, newElements);

            final FormData fd = new FormData();
            fd.top = new FormAttachment();
            fd.left = new FormAttachment(0);
            textComp.getComposite().setLayoutData(fd);

            item3 = new ExpandItem (bar, SWT.NONE);
            createExpandBarItem(item3, "Text", textCompParent);
        }
        
        /************************************************************************
         * COORDINATES
         ***********************************************************************/
        if(supports.contains("DualCoordinateSupport") || 
                supports.contains("SingleCoordinateSupport"))
        {
            coordinateCompParent = new Composite (bar, SWT.BORDER);
            final FormLayout layout = new FormLayout ();
            coordinateCompParent.setLayout(layout);
    
            if(supports.contains("DualCoordinateSupport"))    
            {
                coordinateComp = new DualCoordinateComposite(coordinateCompParent, newElements);
            }
            else if(supports.contains("SingleCoordinateSupport"))
            {
                coordinateComp = new SingleCoordinateComposite(coordinateCompParent, newElements);
            }
            
            final FormData fd = new FormData();
            fd.top = new FormAttachment();
            fd.left = new FormAttachment(0);
            coordinateComp.getComposite().setLayoutData(fd);

            item4 = new ExpandItem (bar, SWT.NONE);
            createExpandBarItem(item4, "Position", coordinateCompParent);
        }
        
        /**********************************************************************
         * TIME
         *********************************************************************/
        if(supports.contains("TimeSupport"))  
        {
            timeCompParent = new Composite (bar, SWT.BORDER);
            final FormLayout layout = new FormLayout ();
            timeCompParent.setLayout(layout);
            
            timeComp = new TimeComposite(timeCompParent, newElements);

            final FormData fd = new FormData();
            fd.top = new FormAttachment();
            fd.left = new FormAttachment(0);
            timeComp.getComposite().setLayoutData(fd);

            item5 = new ExpandItem (bar, SWT.NONE);
            createExpandBarItem(item5, "Time", timeCompParent);
        }
        
        /**********************************************************************
         * CHANNEL
         *********************************************************************/
        if(supports.contains("ChannelSupport"))   
        {
            channelCompParent = new Composite (bar, SWT.BORDER);
            final FormLayout layout = new FormLayout ();
            channelCompParent.setLayout(layout);
            
            channelComp = new ChannelComposite(this.appContext.getBean(IChannelDefinitionProvider.class), channelCompParent, newElements);

            final FormData fd = new FormData();
            fd.top = new FormAttachment();
            fd.left = new FormAttachment(0);
            channelComp.getComposite().setLayoutData(fd);

            item6 = new ExpandItem (bar, SWT.NONE);
            createExpandBarItem(item6, "Channel", channelCompParent);
        }
        
        /**********************************************************************
         * FORMAT
         *********************************************************************/
        if(supports.contains("FormatSupport"))    
        {
            TimeFormatType formatType = TimeFormatType.NONE;
            
            if(supports.contains("TimeSupport")) {
                formatType = timeComp.determineFormatType();
            }else if(supports.contains("ChannelSupport")) {
                formatType = channelComp.determineFormatType();
            }
            
            if(!formatType.equals(TimeFormatType.NONE)) {
                
                formatCompParent = new Composite (bar, SWT.BORDER);
                final FormLayout layout = new FormLayout ();
                formatCompParent.setLayout(layout);
                
                if(formatType.equals(TimeFormatType.SOL_DATE) || 
                        formatType.equals(TimeFormatType.REGULAR_DATE)) {
                    formatComp = new DateFormatComposite(appContext.getBean(PerspectiveProperties.class), formatCompParent, newElements, formatType);
                }
                else if(formatType.equals(TimeFormatType.STRING)) {
                    formatComp = new SprintfFormatComposite(appContext.getBean(IChannelDefinitionProvider.class), formatCompParent, newElements);
                }
            
                final FormData fd = new FormData();
                fd.top = new FormAttachment();
                fd.left = new FormAttachment(0);
                formatComp.getComposite().setLayoutData(fd);
    
                item7 = new ExpandItem (bar, SWT.NONE);
                createExpandBarItem(item7, "Format", formatCompParent);
            }
        }

        /**********************************************************************
         * IMAGE
         *********************************************************************/
        if(supports.contains("ImageSupport")) 
        {
            imageCompParent = new Composite (bar, SWT.BORDER);
            final FormLayout layout = new FormLayout ();
            imageCompParent.setLayout(layout);
            
            imageComp = new ImageComposite(imageCompParent, newElements);
            
            FormData fd = new FormData();
            fd = new FormData();
            fd.top = new FormAttachment();
            fd.left = new FormAttachment(0);
            imageComp.getComposite().setLayoutData(fd);

            item8 = new ExpandItem (bar, SWT.NONE);
            createExpandBarItem(item8, "Image", imageCompParent);
        }
        
        /**********************************************************************
         * BUTTON
         *********************************************************************/
        if(supports.contains("ButtonSupport"))
        {
            buttonCompParent = new Composite (bar, SWT.BORDER);
            final FormLayout layout = new FormLayout ();
            buttonCompParent.setLayout(layout);

            buttonComp = new ButtonComposite(buttonCompParent, newElements);

            final FormData fd = new FormData();
            fd.top = new FormAttachment();
            fd.left = new FormAttachment(0);
            buttonComp.getComposite().setLayoutData(fd);

            item9 = new ExpandItem (bar, SWT.BORDER);
            createExpandBarItem(item9, "Button", buttonCompParent);
        }
        
        //adjust palette height
        setHeight();
    }

//	/**
//	 * Launches a stand-alone formatting palette
//	 * 
//	 * @param args
//	 */
//	public static void main (String [] args) {
//		SessionConfiguration tc = new SessionConfiguration(true);
//		tc.setVenueType(VenueType.TESTSET);
//		SessionConfiguration.setGlobalInstance(tc);
//
//		Display display = new Display ();
//		Shell shell = new Shell (display);
//		shell.setLayout(new FillLayout());
//		shell.setSize(230, 500);
//		Palette palette = new Palette(shell);
//
//		palette.updateGui(new ArrayList<CanvasElement>());
//
//		shell.open();
//		while (!shell.isDisposed()) {
//			if (!display.readAndDispatch()) {
//				display.sleep();
//			}
//		}  
//
//		display.dispose();
//	}
}