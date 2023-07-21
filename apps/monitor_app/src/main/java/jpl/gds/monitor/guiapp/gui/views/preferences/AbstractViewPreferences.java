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
package jpl.gds.monitor.guiapp.gui.views.preferences;

import org.eclipse.swt.widgets.Shell;
import org.springframework.context.ApplicationContext;

import jpl.gds.monitor.guiapp.gui.ViewPreferencesShell;
import jpl.gds.perspective.view.IViewConfiguration;
import jpl.gds.shared.swt.ColorComposite;
import jpl.gds.shared.swt.FontComposite;
import jpl.gds.shared.swt.TitleComposite;
import jpl.gds.shared.swt.types.ChillColor;
import jpl.gds.shared.swt.types.ChillFont;

/**
 * 
 * AbstractViewPreferences is a base class implementing common functionality for
 * all View Preferences Shells. It includes support for the GUI controls to set
 * the view data font and color, should the child class need them.
 *
 */
public class AbstractViewPreferences implements ViewPreferencesShell {
    /**
     * Font used for all of the preferences windows
     */
    protected ChillFont dataFont;
    
    /**
     * Background color used for preferences windows
     */
    protected ChillColor backColor;
    
    /**
     * Foreground color used for preferences windows
     */
    protected ChillColor foreColor;
    
    /**
     * Preferences composite title
     */
    protected TitleComposite titleText;
    
    /**
     * Title user designates for the view
     */
    protected String viewTitle;
    
    /**
     * Window title
     */
    protected String windowTitle;
    
    /**
     * Composite for choosing font
     */
    protected FontComposite fontGetter;
    
    /**
     * Composite for choosing the background color
     */
    protected ColorComposite backColorGetter;
    
    /**
     * Composite for choosing the foreground color
     */
    protected ColorComposite foreColorGetter;
    
    /**
     * Indicates if title feature is turned on for this view
     */
    protected boolean titleEnabled;
    
    /**
     * Indicates if user cancelled out of the preferences window
     */
    protected  boolean canceled = true;
    
    /**
     * Main preferences window shell
     */
    protected Shell prefShell;

    /** The current application context */
	protected ApplicationContext appContext; 
    
    /**
     * Creates an instance of AbstractViewPreferences.
     * @param appContext the current application context
     * @param windowTitle the title of the preferences shell.
     */
    protected AbstractViewPreferences(final ApplicationContext appContext, final String windowTitle) {
    	this.appContext = appContext;
        this.windowTitle = windowTitle;
    }
    
    /**
     * Sets the title the user has assigned to the view ( not the preferences window)
     * @param title the title text
     */
    protected void setViewTitle(final String title) {
        this.viewTitle = title;
        if (this.titleText != null && !this.titleText.getComposite().isDisposed()) {
            this.titleText.setCurrentTitle(this.viewTitle);
        }
    }

    /**
     * Gets the title text the user has elected to assign to the view.
     * @return the title text
     */
    protected String getViewTitle() {
        return this.viewTitle;
    }

    /**
     * Gets the font object the user has selected for data display.
     * @return ChillFont object
     */
    protected ChillFont getDataFont() {
        return this.dataFont;
    }

    /**
     * Sets the font object the user has chosen for data display
     * @param dataFont the ChillFont to set
     */
    protected void setDataFont(final ChillFont dataFont) {
        this.dataFont = dataFont;
        if (this.fontGetter != null && !this.fontGetter.getComposite().isDisposed()) {
            this.fontGetter.setCurrentFont(this.dataFont);
        }
    }
    
    /**
     * Gets the color object the user has selected for the background.
     * @return ChillColor object
     */
    protected ChillColor getBackgroundColor() {
        return this.backColor;
    }

    /**
     * Sets the color the user has chosen for the background.
     * @param color the ChillColor to set
     */
    protected void setBackgroundColor(final ChillColor color) {
        this.backColor = color;
        if (this.backColorGetter != null && !this.backColorGetter.getComposite().isDisposed()) {
            this.backColorGetter.setCurrentColor(this.backColor);
        }
    }
    
    /**
     * Gets the color object the user has selected for the foreground.
     * @return ChillColor object
     */
    protected ChillColor getForegroundColor() {
        return this.foreColor;
    }

    /**
     * Sets the color the user has chosen for the foreground.
     * @param color the ChillColor to set
     */
    protected void setForegroundColor(final ChillColor color) {
        this.foreColor = color;
        if (this.foreColorGetter != null && !this.foreColorGetter.getComposite().isDisposed()) {
            this.foreColorGetter.setCurrentColor(this.foreColor);
        }
    }
    
    /**
     * Sets the display-title feature 
     * 
     * @param enable true if title should be displayed for this view, false
     * otherwise
     */
    protected void setDisplayTitle(final boolean enable) {
        this.titleEnabled = enable;
        if (this.titleText != null && !this.titleText.getComposite().isDisposed()) {
            this.titleText.setEnableTitle(enable);
        }
    }
    
    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.swt.ChillShell#open()
     */
    @Override
    public void open() {
        this.prefShell.open();
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.swt.ChillShell#getShell()
     */
    @Override
    public Shell getShell() {
        return this.prefShell;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.swt.ChillShell#wasCanceled()
     */
    @Override
    public boolean wasCanceled() {
        return this.canceled;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.swt.ChillShell#getTitle()
     */
    @Override
    public String getTitle() {
        return this.windowTitle;
    }
    
    /**
     * Sets the window title.
     * @param title the title text
     */
    protected void setTitle(final String title) {
       this.windowTitle = title;     
    }
    

    /**
     * @{inheritDoc}
     * @see jpl.gds.monitor.guiapp.gui.ViewPreferencesShell#setValuesFromViewConfiguration(jpl.gds.perspective.view.ViewConfiguration)
     */
    @Override
    public void setValuesFromViewConfiguration(final IViewConfiguration config) {
        if (config.getDataFont() != null) {
            setDataFont(config.getDataFont());
        } else {
            setDataFont(new ChillFont());
        }
        if (config.getBackgroundColor() != null) {
            setBackgroundColor(config.getBackgroundColor());
        } else {
            setBackgroundColor(new ChillColor(ChillColor.ColorName.WHITE));
        }
        if (config.getForegroundColor() != null) {
            setForegroundColor(config.getForegroundColor());
        } else {
            setForegroundColor(new ChillColor(ChillColor.ColorName.BLACK));
        }
        setViewTitle(config.getViewName());
        this.setDisplayTitle(config.isDisplayViewTitle());
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.monitor.guiapp.gui.ViewPreferencesShell#getValuesIntoViewConfiguration(jpl.gds.perspective.view.ViewConfiguration)
     */
    @Override
    public void getValuesIntoViewConfiguration(final IViewConfiguration config) {
        config.setViewName(getViewTitle());
        config.setDataFont(getDataFont()); 
        config.setBackgroundColor(getBackgroundColor());
        config.setForegroundColor(getForegroundColor());
        config.setDisplayViewTitle(this.titleEnabled);
    }
    
    /**
     * Applies changes to the title, data font, or background color controls by saving them to
     * local variables.
     *
     */
    protected void applyChanges() {

        this.canceled = false;

        if (this.fontGetter != null) {
            this.dataFont = this.fontGetter.getCurrentFont();
        }
        if (this.backColorGetter != null) {
            this.backColor = this.backColorGetter.getCurrentColor();
        }
        if (this.foreColorGetter != null) {
            this.foreColor = this.foreColorGetter.getCurrentColor();
        }
        if (this.titleText != null) {
            this.viewTitle = this.titleText.getCurrentTitle();  
            this.titleEnabled = this.titleText.isEnableTitle();
        }
    }
}
