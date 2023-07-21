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
package jpl.gds.monitor.fixedbuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.ShellListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.CoolBar;
import org.eclipse.swt.widgets.CoolItem;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.GeneralProperties;
import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.common.swt.AboutUtility;
import jpl.gds.dictionary.api.channel.IChannelDefinitionProvider;
import jpl.gds.dictionary.api.client.channel.IChannelUtilityDictionaryManager;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.monitor.canvas.CanvasElement;
import jpl.gds.monitor.canvas.CanvasElementFactory;
import jpl.gds.monitor.canvas.CanvasSelectionListener;
import jpl.gds.monitor.canvas.ChannelElement;
import jpl.gds.monitor.canvas.ElementConfigurationChangeListener;
import jpl.gds.monitor.canvas.FixedCanvas;
import jpl.gds.monitor.canvas.FixedCanvas.AlignmentType;
import jpl.gds.monitor.canvas.HeaderElement;
import jpl.gds.monitor.fixedbuilder.palette.Palette;
import jpl.gds.monitor.perspective.PerspectiveDictionaryChecker;
import jpl.gds.monitor.perspective.view.SingleWindowViewConfiguration;
import jpl.gds.monitor.perspective.view.fixed.FixedFieldConfigurationFactory;
import jpl.gds.monitor.perspective.view.fixed.FixedFieldType;
import jpl.gds.monitor.perspective.view.fixed.FixedLayoutViewConfiguration;
import jpl.gds.monitor.perspective.view.fixed.IFixedFieldConfiguration;
import jpl.gds.monitor.perspective.view.fixed.IFixedLayoutViewConfiguration;
import jpl.gds.monitor.perspective.view.fixed.fields.DualPointFixedFieldConfiguration;
import jpl.gds.monitor.perspective.view.fixed.fields.HeaderFieldConfiguration;
import jpl.gds.perspective.config.PerspectiveProperties;
import jpl.gds.perspective.config.ViewType;
import jpl.gds.perspective.view.IViewConfiguration;
import jpl.gds.perspective.view.IViewConfigurationContainer;
import jpl.gds.perspective.view.ViewReference;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.swt.ChillColorCreator;
import jpl.gds.shared.swt.ChillFontCreator;
import jpl.gds.shared.swt.ChillShell;
import jpl.gds.shared.swt.ConfirmationShell;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.shared.swt.TextViewShell;
import jpl.gds.shared.swt.types.ChillFont;
import jpl.gds.shared.swt.types.ChillPoint;
import jpl.gds.shared.swt.types.CoordinateSystemType;

/**
 * This class provides the main GUI window for the Fixed View Builder. It
 * consists of a menu bar, a tool bar, a drawing canvas, a status bar, and a
 * drawing palate.
 * 
 */
public class FixedBuilderShell implements ChillShell {

    /**
     * Fixed view builder window title
     */
    public static final String TITLE = "Fixed View Builder";

    // Create images used in the GUI display
    private static Image boxImage = SWTUtilities.createImage(Display
            .getDefault(), "jpl/gds/monitor/fixed/builder/gui/Square.png");
    private static Image buttonImage = SWTUtilities.createImage(Display
            .getDefault(), "jpl/gds/monitor/fixed/builder/gui/BlueButton.png");
    private static Image channelImage = SWTUtilities.createImage(Display
            .getDefault(), "jpl/gds/monitor/fixed/builder/gui/Chart xy.png");
    private static Image clearImage = SWTUtilities.createImage(Display
            .getDefault(), "jpl/gds/monitor/fixed/builder/gui/Clear.gif");
    private static Image deleteImage = SWTUtilities.createImage(Display
            .getDefault(), "jpl/gds/monitor/fixed/builder/gui/Erase.png");
    private static Image duplicateImage = SWTUtilities.createImage(Display
            .getDefault(), "jpl/gds/monitor/fixed/builder/gui/Copy.png");
    private static Image gridImage = SWTUtilities.createImage(Display
            .getDefault(), "jpl/gds/monitor/fixed/builder/gui/Grid.png");
    private static Image headerImage = SWTUtilities.createImage(Display
            .getDefault(), "jpl/gds/monitor/fixed/builder/gui/Header.png");
    private static Image imageImage = SWTUtilities.createImage(Display
            .getDefault(), "jpl/gds/monitor/fixed/builder/gui/Picture.png");
    private static Image lineImage = SWTUtilities.createImage(Display
            .getDefault(), "jpl/gds/monitor/fixed/builder/gui/Line.png");
    private static Image newImage = SWTUtilities.createImage(Display
            .getDefault(), "jpl/gds/monitor/fixed/builder/gui/New file.png");
    private static Image openImage = SWTUtilities.createImage(Display
            .getDefault(), "jpl/gds/monitor/fixed/builder/gui/Export.png");
    private static Image paletteImage = SWTUtilities.createImage(Display
            .getDefault(), "jpl/gds/monitor/fixed/builder/gui/Color palette.png");
    private static Image saveImage = SWTUtilities.createImage(Display
            .getDefault(), "jpl/gds/monitor/fixed/builder/gui/Import.png");
    private static Image textImage = SWTUtilities.createImage(Display
            .getDefault(), "jpl/gds/monitor/fixed/builder/gui/Text tool.png");
    private static Image timeImage = SWTUtilities.createImage(Display
            .getDefault(), "jpl/gds/monitor/fixed/builder/gui/Time.png");

    // These are used to compute the size of the main window. This many pixels
    // must be added to the builder canvas size to get the total size of the
    // window.
    // It differs between MacOS and RedHat Linux.
    private static final int H_SIZE_PAD = GdsSystemProperties.isMacOs() ? 110
            : 140;
    private static final int W_SIZE_PAD = GdsSystemProperties.isMacOs() ? 45 : 65;

    // This is the smallest the main window can be
    private static final int MINIMUM_PIXEL_HEIGHT = 400;
    private static final int MINIMUM_PIXEL_WIDTH = 550;

    // Palette width differs between MacOS and RedHat Linux
    private static final int              PALETTE_WIDTH        = GdsSystemProperties.getSystemProperty("os.name")
            .contains("Mac") ? 307 : 257;

    // The settings object containing user preferences for the builder
    private final FixedBuilderSettings builderSettings = new FixedBuilderSettings();

    // The current fixed layout view configuration filepath
    private String currentFile;

    // The current fixed layout view configuration
    private IFixedLayoutViewConfiguration currentConfig;

    // Actual pixel dimensions on the main window, after canvas size, padding,
    // and minimum
    // size have been accounted for
    private int defaultPixelHeight;
    private int defaultPixelWidth;

    // Indicates if the current mission has an SSE
    private final boolean hasSse;

    // Utilities object for SWT operations
    private final SWTUtilities util = new SWTUtilities();

    // GUI components
    private final Display parent;
    private Shell mainShell;
    private ScrolledComposite canvasComp;
    private FixedCanvas canvas;
    private CanvasListener canvasListener;
    private CoolBar coolBar;
    private FixedBuilderStatusComposite statusBar;

    // Floating objects
    private XmlSourceShell sourceViewShell;
    private TextViewShell checkerViewShell;
    private Palette palette;

    // The currently selected element(s)
    private List<CanvasElement> liveElements = new ArrayList<CanvasElement>();

    // Menu Items
    private MenuItem checkDictionaryMenuItem;
    private MenuItem closeMenuItem;
    private MenuItem loadMenuItem;
    private MenuItem newMenuItem;
    private MenuItem orderMenuItem;
    private MenuItem preferencesMenuItem;
    private MenuItem saveAsMenuItem;
    private MenuItem saveImageMenuItem;
    private MenuItem saveMenuItem;
    private MenuItem showSourceMenuItem;
    private MenuItem snapMenuItem;

    // Tool Items
    private ToolItem boxItem;
    private ToolItem buttonItem;
    private ToolItem channelItem;
    private ToolItem clearItem;
    private ToolItem deleteItem;
    private ToolItem duplicateItem;
    private ToolItem gridItem;
    private ToolItem headerItem;
    private ToolItem imageItem;
    private ToolItem lineItem;
    private ToolItem newItem;
    private ToolItem openItem;
    private ToolItem paletteItem;
    private ToolItem saveItem;
    private ToolItem textItem;
    private ToolItem timeItem;
    
    private final ApplicationContext appContext;
    private final DictionaryProperties dictConfig;

    private final Tracer                  trace;

    /**
     * Creates a new FixedBuilderShell.
     * @param appContext the current application context
     * 
     * @param display
     *            the SWT Display parent
     * @param filename
     *            an optional filename of a fixed view configuration to load;
     *            may be null
     */
    public FixedBuilderShell(final ApplicationContext appContext, final Display display, final String filename) {
    	this.appContext = appContext;
    	hasSse = this.appContext.getBean(MissionProperties.class).missionHasSse();
    	dictConfig = this.appContext.getBean(DictionaryProperties.class);
        this.parent = display;
        this.currentFile = filename;
        this.trace = TraceManager.getDefaultTracer(appContext);
        createControls();
        if (this.currentFile != null) {
            openFile(this.currentFile);
        }
    }

    /**
     * Gets the XML representation of the current fixed view configuration.
     * 
     * @return XML text, or the empty string if there is no current view
     *         configuration
     */
    public String getCurrentXml() {
        if (this.currentConfig == null) {
            return "";
        } else {
            return this.currentConfig.toXML();
        }
    }


    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.swt.ChillShell#getShell()
     */
    @Override
	public Shell getShell() {
        return this.mainShell;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.swt.ChillShell#getTitle()
     */
    @Override
	public String getTitle() {
        return TITLE;
    }


    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.swt.ChillShell#open()
     */
    @Override
	public void open() {
        if (this.mainShell != null) {
            this.mainShell.open();
        }
    }

    /**
     * Sets the current fixed layout configuration from an XML string. The XML
     * will be parsed, creating a new current layout. If the XML does not parse,
     * an error message is displayed, and the canvas is left as it was before
     * the call.
     * 
     * @param xml
     *            the XML representing the new fixed layout configuration
     */
    public void setCurrentXml(final String xml) {

        if (xml == null || xml.equals("")) {
            return;
        }

        // Parse the view configuration out of the XML.
        List<IViewConfiguration> configs = null;
        try {
            configs = IViewConfiguration.loadFromString(appContext, xml);
        } catch (final Exception e) {
            SWTUtilities.showErrorDialog(this.mainShell, "Error Parsing XML",
                    "The text in the XML source window could not be properly parsed:\n"
                            + e.toString());
            return;
        }
        if (configs == null || configs.size() == 0) {
            SWTUtilities
                    .showErrorDialog(this.mainShell, "Error Parsing XML",
                            "The text in the XML source window could not be properly parsed");
            return;
        }

        // Clear off the canvas, set the current configuration to the parsed
        // configuration,
        // assign it to the canvas, and set the global coordinate system.
        this.canvas.clear();
        this.currentConfig = (IFixedLayoutViewConfiguration) configs.get(0);
        normalizeCoordinates(this.currentConfig);
        this.canvas.setViewConfiguration(this.currentConfig);
        setCoordinateSystem();

        // Create the canvas elements that are called for by the new
        // configuration
        final List<IFixedFieldConfiguration> allConfigs = this.currentConfig
                .getFieldConfigs();
        if (allConfigs != null) {
            for (final IFixedFieldConfiguration fieldConfig : allConfigs) {
                final CanvasElement elem = CanvasElementFactory.create(
                        fieldConfig, this.canvas.getCanvas());
                elem.setCoordinateSystem(this.currentConfig
                        .getCoordinateSystem());
                this.canvas.addCanvasElement(elem);
            }
        }

        // Perform GUI configuration changes based upon the new configuration
        updateGuiFromConfig();

        // Update the builder's default dictionary if called for
        if (promptForBuilderDictionaryUpdate()) {
            setBuilderDictionariesFromConfig(this.currentConfig);
        }

        if (this.palette != null) {
            this.palette.setFixedLayoutViewConfiguration(this.currentConfig);
        }

        // Update the status bar to reflect the XML has been loaded
        this.statusBar
                .setMessage("Updated layout "
                        + this.currentConfig.getViewName()
                        + " from XML Source window.");
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.swt.ChillShell#wasCanceled()
     */
    @Override
	public boolean wasCanceled() {
        return false;
    }

    /**
     * Main entry point for GUI creation.
     */
    private void createControls() {

        createMainShell();

        createMenuBar();
        createCoolBar();
        createCanvas();
        createStatusBar();

        // Compute shell size
        // Default size is default size of the canvas + pad for main window
        // elements.
        // If the builder is running in CHARACTER mode, its dimensions must be
        // converted
        // to pixels based upon the current font sizes in the canvas.
        if (this.builderSettings.getCoordinateSystem().equals(
                CoordinateSystemType.CHARACTER)) {
            this.defaultPixelWidth = FixedCanvas.DEFAULT_CHARACTER_WIDTH
                    * this.canvas.getCharacterWidth() + W_SIZE_PAD;
            this.defaultPixelHeight = FixedCanvas.DEFAULT_CHARACTER_HEIGHT
                    * this.canvas.getCharacterHeight() + H_SIZE_PAD;
        } else {
            this.defaultPixelWidth = FixedCanvas.DEFAULT_PIXEL_WIDTH
                    + W_SIZE_PAD;
            this.defaultPixelHeight = FixedCanvas.DEFAULT_PIXEL_HEIGHT
                    + H_SIZE_PAD;
        }
        this.mainShell.setSize(this.defaultPixelWidth, this.defaultPixelHeight);
    }

    /**
     * Creates the main shell and its event handlers.
     */
    private void createMainShell() {

        this.mainShell = new Shell(this.parent, SWT.SHELL_TRIM);
        this.mainShell.setText(TITLE);
        this.mainShell.setLayout(new FormLayout());

        this.mainShell.addShellListener(new ShellListener() {

            @Override
			public void shellActivated(final ShellEvent arg0) {
            }

            @Override
			public void shellClosed(final ShellEvent event) {
                try {
                    boolean yes = true;
                    boolean cancelled = false;
                    if (FixedBuilderShell.this.currentConfig != null
                            && FixedBuilderShell.this.currentConfig
                                    .getFieldConfigs().size() != 0) {
                        yes = SWTUtilities
                                .showConfirmDialog(
                                        FixedBuilderShell.this.mainShell,
                                        "Save Reminder",
                                        "Do you want to save the current layout before exiting?");
                        if (yes) {
                            cancelled = !saveToFile(false);
                        }
                    }

                    if (cancelled) {
                        event.doit = false;
                    }
                } catch (final Exception ex) {
                    ex.printStackTrace();
                }
            }

            @Override
			public void shellDeactivated(final ShellEvent arg0) {
            }

            @Override
			public void shellDeiconified(final ShellEvent arg0) {
            }

            @Override
			public void shellIconified(final ShellEvent arg0) {
            }
        });
    }

    /**
     * Creates the menu bar.
     */
    private void createMenuBar() {

        final Menu menuBar = new Menu(this.mainShell, SWT.BAR);
        this.mainShell.setMenuBar(menuBar);

        createFileMenuItems(menuBar);
        createUtilityMenu(menuBar);
        createObjectMenu(menuBar);
        createHelpMenu(menuBar);
    }

    /**
     * Creates the File menu.
     * 
     * @param menuBar
     *            the MenuBar parent for the new menu
     */
    private void createFileMenuItems(final Menu menuBar) {
        final MenuItem fileMenuItem = new MenuItem(menuBar, SWT.CASCADE);
        fileMenuItem.setText("File");

        final Menu fileMenu = new Menu(this.mainShell, SWT.DROP_DOWN);
        fileMenuItem.setMenu(fileMenu);

        createNewLayoutMenuItem(fileMenu);
        creatOpenMenuItem(fileMenu);
        createSaveMenuItems(fileMenu);
        createCloseMenuItem(fileMenu);

        new MenuItem(fileMenu, SWT.SEPARATOR);

        createSaveImageSubmenu(fileMenu);

        new MenuItem(fileMenu, SWT.SEPARATOR);

        createPreferencesMenuItems(fileMenu);

        new MenuItem(fileMenu, SWT.SEPARATOR);

        createExitMenuItem(fileMenu);

        // If a configuration is currently loaded, enable appropriate menu items
        if (this.currentFile != null) {
            this.saveMenuItem.setEnabled(true);
            this.saveAsMenuItem.setEnabled(true);
            this.preferencesMenuItem.setEnabled(true);
            this.saveImageMenuItem.setEnabled(true);
            this.closeMenuItem.setEnabled(true);
        }
    }

    /**
     * Creates the Exit menu item on the File menu.
     * 
     * @param fileMenu
     *            the File menu object
     */
    private void createExitMenuItem(final Menu fileMenu) {

        final MenuItem exitMenuItem = new MenuItem(fileMenu, SWT.PUSH);
        exitMenuItem.setText("Exit");

        exitMenuItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(
                    final org.eclipse.swt.events.SelectionEvent e) {
                try {
                    FixedBuilderShell.this.mainShell.close();
                } catch (final Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    /**
     * Creates the Preferences menu items on the File menu, and their event
     * handlers.
     * 
     * @param fileMenu
     *            the file menu object
     */
    private void createPreferencesMenuItems(final Menu fileMenu) {

        final MenuItem builderPreferencesMenuItem = new MenuItem(fileMenu,
                SWT.PUSH);
        builderPreferencesMenuItem.setText("Builder Preferences...");
        builderPreferencesMenuItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(
                    final org.eclipse.swt.events.SelectionEvent e) {
                try {
                    setBuilderPreferences();
                } catch (final Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        this.preferencesMenuItem = new MenuItem(fileMenu, SWT.PUSH);
        this.preferencesMenuItem.setText("Layout Preferences...");
        this.preferencesMenuItem.setEnabled(false);
        this.preferencesMenuItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(
                    final org.eclipse.swt.events.SelectionEvent e) {
                try {
                    setLayoutPreferences();
                } catch (final Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    /**
     * Creates the Save as Image sub-menu on the file menu, and all of the
     * sub-menu's child menu items.
     * 
     * @param fileMenu
     *            the file menu object
     */
    private void createSaveImageSubmenu(final Menu fileMenu) {

        this.saveImageMenuItem = new MenuItem(fileMenu, SWT.CASCADE);
        this.saveImageMenuItem.setText("Save Snapshot As Image");
        this.saveImageMenuItem.setEnabled(false);

        final Menu imageSubMenu = new Menu(this.mainShell, SWT.DROP_DOWN);
        this.saveImageMenuItem.setMenu(imageSubMenu);

        final MenuItem jpgMenuItem = new MenuItem(imageSubMenu, SWT.PUSH);
        jpgMenuItem.setText("As JPG...");
        addSaveImageListener(jpgMenuItem, SWT.IMAGE_JPEG);

        final MenuItem pngMenuItem = new MenuItem(imageSubMenu, SWT.PUSH);
        pngMenuItem.setText("As PNG...");
        addSaveImageListener(pngMenuItem, SWT.IMAGE_PNG);

        final MenuItem bmpMenuItem = new MenuItem(imageSubMenu, SWT.PUSH);
        bmpMenuItem.setText("As BMP...");
        addSaveImageListener(bmpMenuItem, SWT.IMAGE_BMP);
    }

    /**
     * Creates the Close menu item on the File menu, and its event handlers.
     * 
     * @param fileMenu
     *            the File menu object
     */
    private void createCloseMenuItem(final Menu fileMenu) {
        this.closeMenuItem = new MenuItem(fileMenu, SWT.PUSH);
        this.closeMenuItem.setText("Close");
        this.closeMenuItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(
                    final org.eclipse.swt.events.SelectionEvent e) {
                boolean yes = true;
                boolean cancelled = false;
                if (FixedBuilderShell.this.currentConfig != null
                        && FixedBuilderShell.this.currentConfig
                                .getFieldConfigs().size() != 0) {
                    yes = SWTUtilities
                            .showConfirmDialog(
                                    FixedBuilderShell.this.mainShell,
                                    "Save Reminder",
                                    "Do you want to save the layout before closing it?");
                    if (yes) {
                        cancelled = !saveToFile(false);
                    }
                }
                if (cancelled) {
                    return;
                }
                FixedBuilderShell.this.currentFile = null;
                FixedBuilderShell.this.currentConfig = null;
                enableMenuItems(false);
                enableToolItems(false);
                FixedBuilderShell.this.canvas.clear();
                FixedBuilderShell.this.statusBar.reset();
                FixedBuilderShell.this.statusBar
                        .setMessage("Select 'File->New' or the 'New' toolbar button to create a new fixed layout.");
            }
        });
    }

    /**
     * Creates the Save and Save As menu item on the file menu, and their event
     * handlers.
     * 
     * @param fileMenu
     *            the File menu object
     */
    private void createSaveMenuItems(final Menu fileMenu) {
        this.saveMenuItem = new MenuItem(fileMenu, SWT.PUSH);
        this.saveMenuItem.setText("Save");
        this.saveMenuItem.setEnabled(false);
        this.saveMenuItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(
                    final org.eclipse.swt.events.SelectionEvent e) {
                saveToFile(false);
            }
        });

        this.saveAsMenuItem = new MenuItem(fileMenu, SWT.PUSH);
        this.saveAsMenuItem.setText("Save As...");
        this.saveAsMenuItem.setEnabled(false);
        this.saveAsMenuItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(
                    final org.eclipse.swt.events.SelectionEvent e) {
                saveToFile(true);
            }
        });
    }

    /**
     * Creates the Open menu item on the file menu, and its event handlers.
     * 
     * @param fileMenu
     *            the File menu object
     */
    private void creatOpenMenuItem(final Menu fileMenu) {
        this.loadMenuItem = new MenuItem(fileMenu, SWT.PUSH);
        this.loadMenuItem.setText("Open...");
        this.loadMenuItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(
                    final org.eclipse.swt.events.SelectionEvent e) {
                boolean yes = true;
                boolean cancelled = false;
                if (FixedBuilderShell.this.currentConfig != null
                        && FixedBuilderShell.this.currentConfig
                                .getFieldConfigs().size() != 0) {
                    yes = SWTUtilities
                            .showConfirmDialog(
                                    FixedBuilderShell.this.mainShell,
                                    "Save Reminder",
                                    "Do you want to save the existing layout before opening a new one?");
                    if (yes) {
                        cancelled = !saveToFile(false);
                    }
                }
                if (!cancelled) {
                    openFile(null);
                }
            }
        });
    }

    /**
     * Creates the New menu item on the file menu, and its event handlers.
     * 
     * @param fileMenu
     *            the File menu object
     */
    private void createNewLayoutMenuItem(final Menu fileMenu) {
        this.newMenuItem = new MenuItem(fileMenu, SWT.PUSH);
        this.newMenuItem.setText("New...");
        this.newMenuItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(
                    final org.eclipse.swt.events.SelectionEvent e) {
                try {
                    boolean yes = true;
                    boolean cancelled = false;
                    if (FixedBuilderShell.this.currentConfig != null
                            && FixedBuilderShell.this.currentConfig
                                    .getFieldConfigs().size() != 0) {
                        yes = SWTUtilities
                                .showConfirmDialog(
                                        FixedBuilderShell.this.mainShell,
                                        "Save Reminder",
                                        "Do you want to save the current layout before creating a new one?");
                        if (yes) {
                            cancelled = !saveToFile(false);
                        }
                    }
                    if (!cancelled) {
                        createNewLayout();
                    }
                } catch (final Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    /**
     * Creates the selection listener for the Save as Image sub-menu items.
     * 
     * @param item
     *            the menu item to create the listener for
     * @param imageType
     *            The SWT type constant for the image type
     */
    private void addSaveImageListener(final MenuItem item, final int imageType) {

        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(
                    final org.eclipse.swt.events.SelectionEvent e) {
                String filename = FixedBuilderShell.this.currentConfig
                        .getViewName().replace(' ', '_');
                switch (imageType) {
                case SWT.IMAGE_JPEG:
                    filename += ".jpg";
                    break;
                case SWT.IMAGE_PNG:
                    filename += ".png";
                    break;
                case SWT.IMAGE_BMP:
                    filename += ".bmp";
                    break;
                case SWT.IMAGE_GIF:
                    filename += ".gif";
                    break;
                }
                filename = FixedBuilderShell.this.util.displayStickyFileSaver(
                        FixedBuilderShell.this.mainShell, "Save Image", null,
                        filename);
                final String newFilename = filename;
                SWTUtilities.safeAsyncExec(FixedBuilderShell.this.mainShell
                        .getDisplay(), "Fixed Builder Shell Save Image",
                        new Runnable() {
                            @Override
							public void run() {
                                FixedBuilderShell.this.canvas.saveImage(
                                        newFilename, imageType);
                            }
                        });
            }
        });
    }

    /**
     * Creates the Object menu.
     * 
     * @param menuVar
     *            the Menubar to use as parent
     */
    private void createObjectMenu(final Menu menuBar) {
        final MenuItem objectMenuItem = new MenuItem(menuBar, SWT.CASCADE);
        objectMenuItem.setText("Object");

        final Menu objectMenu = new Menu(this.mainShell, SWT.DROP_DOWN);
        objectMenuItem.setMenu(objectMenu);

        createSnapSubmenu(objectMenu);
        createOrderSubmenu(objectMenu);
    }

    /**
     * Creates the Ordering sub-menu on the Object menu, the sub-menu items, and
     * their event handlers.
     * 
     * @param objectMenu
     *            the Object menu object
     */
    private void createOrderSubmenu(final Menu objectMenu) {

        this.orderMenuItem = new MenuItem(objectMenu, SWT.CASCADE);
        this.orderMenuItem.setText("Ordering");
        this.orderMenuItem.setEnabled(false);

        final Menu orderSubMenu = new Menu(this.mainShell, SWT.DROP_DOWN);
        this.orderMenuItem.setMenu(orderSubMenu);

        final MenuItem moveForwardMenuItem = new MenuItem(orderSubMenu,
                SWT.PUSH);
        moveForwardMenuItem.setText("Move Forward One");

        final MenuItem moveBackwardMenuItem = new MenuItem(orderSubMenu,
                SWT.PUSH);
        moveBackwardMenuItem.setText("Move Backward One");

        moveForwardMenuItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(
                    final org.eclipse.swt.events.SelectionEvent e) {
                try {
                    FixedBuilderShell.this.canvas
                            .moveSelectedElementsTowardsRear(false);
                    final List<CanvasElement> selected = FixedBuilderShell.this.canvas
                            .getSelectedElements();
                    for (final CanvasElement elem : selected) {
                        FixedBuilderShell.this.currentConfig
                                .moveFieldTowardsRear(elem
                                        .getFieldConfiguration(), false);
                    }
                    FixedBuilderShell.this.statusBar
                            .setMessage("Object(s) moved forwards in the Z order.");
                } catch (final Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        moveBackwardMenuItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(
                    final org.eclipse.swt.events.SelectionEvent e) {
                try {
                    FixedBuilderShell.this.canvas
                            .moveSelectedElementsTowardsZero(false);
                    final List<CanvasElement> selected = FixedBuilderShell.this.canvas
                            .getSelectedElements();
                    for (final CanvasElement elem : selected) {
                        FixedBuilderShell.this.currentConfig
                                .moveFieldTowardsZero(elem
                                        .getFieldConfiguration(), false);
                    }
                    FixedBuilderShell.this.statusBar
                            .setMessage("Object(s) moved backwards in the Z order.");
                } catch (final Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        final MenuItem moveToFrontMenuItem = new MenuItem(orderSubMenu,
                SWT.PUSH);
        moveToFrontMenuItem.setText("Move to Front");

        final MenuItem moveToBackMenuItem = new MenuItem(orderSubMenu, SWT.PUSH);
        moveToBackMenuItem.setText("Move to Back");

        moveToFrontMenuItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(
                    final org.eclipse.swt.events.SelectionEvent e) {
                try {
                    FixedBuilderShell.this.canvas
                            .moveSelectedElementsTowardsRear(true);
                    final List<CanvasElement> selected = FixedBuilderShell.this.canvas
                            .getSelectedElements();
                    for (final CanvasElement elem : selected) {
                        FixedBuilderShell.this.currentConfig
                                .moveFieldTowardsRear(elem
                                        .getFieldConfiguration(), true);
                    }
                    FixedBuilderShell.this.statusBar
                            .setMessage("Object(s) moved all the way forwards in the Z order.");
                } catch (final Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        moveToBackMenuItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(
                    final org.eclipse.swt.events.SelectionEvent e) {
                try {
                    FixedBuilderShell.this.canvas
                            .moveSelectedElementsTowardsZero(true);
                    final List<CanvasElement> selected = FixedBuilderShell.this.canvas
                            .getSelectedElements();
                    for (final CanvasElement elem : selected) {
                        FixedBuilderShell.this.currentConfig
                                .moveFieldTowardsZero(elem
                                        .getFieldConfiguration(), true);
                    }
                    FixedBuilderShell.this.statusBar
                            .setMessage("Object(s) moved all the way backwards in the Z order.");
                } catch (final Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    /**
     * Creates the Snap sub-menu on the Object menu, the sub-menu items, and
     * their event handlers.
     * 
     * @param objectMenu
     *            the Object menu object
     */
    private void createSnapSubmenu(final Menu objectMenu) {
        this.snapMenuItem = new MenuItem(objectMenu, SWT.CASCADE);
        this.snapMenuItem.setText("Snap to Grid");
        this.snapMenuItem.setEnabled(false);

        final Menu snapSubMenu = new Menu(this.mainShell, SWT.DROP_DOWN);
        this.snapMenuItem.setMenu(snapSubMenu);

        final MenuItem snapVerticalMenuItem = new MenuItem(snapSubMenu,
                SWT.PUSH);
        snapVerticalMenuItem.setText("Top to Bottom");

        final MenuItem snapHorizontalMenuItem = new MenuItem(snapSubMenu,
                SWT.PUSH);
        snapHorizontalMenuItem.setText("Left to Right");

        snapVerticalMenuItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(
                    final org.eclipse.swt.events.SelectionEvent e) {
                try {
                    FixedBuilderShell.this.canvas
                            .snapSelectedElementsToGrid(AlignmentType.VERTICAL);
                    FixedBuilderShell.this.statusBar
                            .setMessage("Object(s) aligned to nearest horizontal grid line.");
                } catch (final Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        snapHorizontalMenuItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(
                    final org.eclipse.swt.events.SelectionEvent e) {
                try {
                    FixedBuilderShell.this.canvas
                            .snapSelectedElementsToGrid(AlignmentType.HORIZONTAL);
                    FixedBuilderShell.this.statusBar
                            .setMessage("Object(s) aligned to nearest vertical grid line.");
                } catch (final Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    /**
     * Creates the Utility menu.
     * 
     * @param menuBar
     *            the MenuBar to use as parent
     */
    private void createUtilityMenu(final Menu menuBar) {
        final MenuItem utilityMenuItem = new MenuItem(menuBar, SWT.CASCADE);
        utilityMenuItem.setText("Utility");

        final Menu utilityMenu = new Menu(this.mainShell, SWT.DROP_DOWN);
        utilityMenuItem.setMenu(utilityMenu);

        createShowSourceMenuItem(utilityMenu);
        createCheckDictionaryMenuItem(utilityMenu);
    }

    /**
     * Creates the Compare to Dictionary menu item on the Utility menu, and its
     * event handlers.
     * 
     * @param utilityMenu
     *            the Utility menu object
     */
    private void createCheckDictionaryMenuItem(final Menu utilityMenu) {
        this.checkDictionaryMenuItem = new MenuItem(utilityMenu, SWT.PUSH);
        this.checkDictionaryMenuItem.setText("Compare to Dictionary...");
        this.checkDictionaryMenuItem.setEnabled(false);

        this.checkDictionaryMenuItem
                .addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(
                            final org.eclipse.swt.events.SelectionEvent e) {
                        try {
                            checkConfigAgainstDictionary();
                        } catch (final Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                });
    }

    /**
     * Creates the Show XML Source menu item on the Utility menu, and its event
     * handlers.
     * 
     * @param utilityMenu
     *            the Utility menu object
     */
    private void createShowSourceMenuItem(final Menu utilityMenu) {
        this.showSourceMenuItem = new MenuItem(utilityMenu, SWT.PUSH);
        this.showSourceMenuItem.setText("Show XML Source...");
        this.showSourceMenuItem.setEnabled(false);

        this.showSourceMenuItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(
                    final org.eclipse.swt.events.SelectionEvent e) {
                try {
                    if (FixedBuilderShell.this.sourceViewShell == null) {
                        FixedBuilderShell.this.sourceViewShell = new XmlSourceShell(
                                FixedBuilderShell.this);

                        FixedBuilderShell.this.sourceViewShell.getShell()
                                .addDisposeListener(new DisposeListener() {
                                    @Override
									public void widgetDisposed(
                                            final DisposeEvent event) {
                                        FixedBuilderShell.this.sourceViewShell = null;
                                    }
                                });
                        FixedBuilderShell.this.sourceViewShell
                                .setXmlText(FixedBuilderShell.this.currentConfig
                                        .toXML());
                        FixedBuilderShell.this.sourceViewShell.open();
                    } else {
                        FixedBuilderShell.this.sourceViewShell
                                .setXmlText(FixedBuilderShell.this.currentConfig
                                        .toXML());
                    }
                } catch (final Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    /**
     * Creates the Help menu and its event handlers.
     * 
     * @param menuBar
     *            the MenuBar to use a parent.
     */
    private void createHelpMenu(final Menu menuBar) {
        final MenuItem helpMenuItem = new MenuItem(menuBar, SWT.CASCADE);
        helpMenuItem.setText("Help");

        final Menu helpMenu = new Menu(this.mainShell, SWT.DROP_DOWN);
        helpMenuItem.setMenu(helpMenu);

        final MenuItem aboutMenuItem = new MenuItem(helpMenu, SWT.PUSH);
        aboutMenuItem.setText("About");

        aboutMenuItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(
                    final org.eclipse.swt.events.SelectionEvent e) {
                displayAboutDialog();
            }

            /**
             * Displays the "about" window for the application.
             */
            private void displayAboutDialog() {
                AboutUtility
                        .showStandardAboutDialog(FixedBuilderShell.this.mainShell, appContext.getBean(GeneralProperties.class));
            }
        });
    }

    /**
     * Creates the coolbar and nested toolbars.
     */
    private void createCoolBar() {

        this.coolBar = new CoolBar(this.mainShell, SWT.NONE);
        final FormData fd = new FormData();
        fd.top = new FormAttachment(0);
        fd.left = new FormAttachment(0);
        fd.right = new FormAttachment(100);
        this.coolBar.setLayoutData(fd);
        this.coolBar.setLocked(true);

        final CoolItem fileCoolItem = new CoolItem(this.coolBar, SWT.NONE);
        final ToolBar fileTb = new ToolBar(this.coolBar, SWT.HORIZONTAL);

        createNewLayoutToolItem(fileTb);
        createOpenToolItem(fileTb);
        createSaveToolItem(fileTb);

        fileTb.pack();
        Point size = fileTb.getSize();
        fileCoolItem.setControl(fileTb);
        fileCoolItem.setSize(fileCoolItem.computeSize(size.x, size.y));

        final CoolItem addCoolItem = new CoolItem(this.coolBar, SWT.NONE);
        final ToolBar addTb = new ToolBar(this.coolBar, SWT.HORIZONTAL);

        createAddObjectToolItems(addTb);
        createHeaderToolItem(addTb);

        addTb.pack();
        size = addTb.getSize();
        addCoolItem.setControl(addTb);
        addCoolItem.setSize(addCoolItem.computeSize(size.x, size.y));

        final CoolItem utilCoolItem = new CoolItem(this.coolBar, SWT.NONE);
        final ToolBar utilTb = new ToolBar(this.coolBar, SWT.HORIZONTAL);

        createPaletteToolItem(utilTb);
        createGridToolItem(utilTb);

        utilTb.pack();
        size = utilTb.getSize();
        utilCoolItem.setControl(utilTb);
        utilCoolItem.setSize(utilCoolItem.computeSize(size.x, size.y));

        final CoolItem otherCoolItem = new CoolItem(this.coolBar, SWT.NONE);
        final ToolBar otherTb = new ToolBar(this.coolBar, SWT.HORIZONTAL);

        createDeleteToolItem(otherTb);
        createDuplicateToolItem(otherTb);
        createClearToolItem(otherTb);

        otherTb.pack();
        size = otherTb.getSize();
        otherCoolItem.setControl(otherTb);
        otherCoolItem.setSize(otherCoolItem.computeSize(size.x, size.y));

        // New and open items are always enabled
        this.newItem.setEnabled(true);
        this.openItem.setEnabled(true);

        // Other tool items are enabled if there is a current configuration
        // loaded
        if (this.currentFile != null) {
            this.saveItem.setEnabled(true);
            enableToolItems(true);
        }
    }

    /**
     * Creates the Clear toolbar item and its event handlers.
     * 
     * @param otherTb
     *            the ToolBar to use as parent
     */
    private void createClearToolItem(final ToolBar otherTb) {
        this.clearItem = createToolItem(otherTb,
                "Clear all objects from the canvas", clearImage);
        this.clearItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(
                    final org.eclipse.swt.events.SelectionEvent e) {
                final boolean yes = SWTUtilities
                        .showConfirmDialog(
                                FixedBuilderShell.this.mainShell,
                                "Clear Confirmation",
                                "Are you sure you want to clear all objects from the canvas?\nThis cannot be undone.");
                if (yes) {
                    FixedBuilderShell.this.statusBar
                            .setMessage("All objects cleared. Use the 'Add' buttons on the toolbar to add new objects.");
                    FixedBuilderShell.this.canvas.removeAllElements();
                    if (FixedBuilderShell.this.palette != null) {
                        FixedBuilderShell.this.palette
                                .updateGui(new ArrayList<CanvasElement>());
                    }
                }
            }
        });
    }

    /**
     * Creates the Duplicate toolbar item and its event handlers.
     * 
     * @param otherTb
     *            the ToolBar to use as parent
     */
    private void createDuplicateToolItem(final ToolBar otherTb) {
        this.duplicateItem = createToolItem(otherTb,
                "Duplicate Selected Object(s)", duplicateImage);
        this.duplicateItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(
                    final org.eclipse.swt.events.SelectionEvent e) {
                FixedBuilderShell.this.canvas.duplicateSelectedElements();
                final List<CanvasElement> selected = FixedBuilderShell.this.canvas
                        .getSelectedElements();
                for (final CanvasElement elem : selected) {
                    FixedBuilderShell.this.currentConfig.addField(elem
                            .getFieldConfiguration());
                }
                FixedBuilderShell.this.statusBar
                        .setMessage("Selected objects duplicated.");
            }
        });
    }

    /**
     * Creates the Delete toolbar item and its event handlers.
     * 
     * @param otherTb
     *            the ToolBar to use as parent
     */
    private void createDeleteToolItem(final ToolBar otherTb) {
        this.deleteItem = createToolItem(otherTb, "Delete Selected Object(s)",
                deleteImage);
        this.deleteItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(
                    final org.eclipse.swt.events.SelectionEvent e) {
                FixedBuilderShell.this.canvas.removeSelectedElements();
                FixedBuilderShell.this.statusBar
                        .setMessage("Selected objects removed.");
            }
        });
    }

    /**
     * Creates the Grid toolbar item and its event handlers.
     * 
     * @param utilTb
     *            the ToolBar to use as parent
     */
    private void createGridToolItem(final ToolBar utilTb) {
        this.gridItem = createToolItem(utilTb, "Toggle Drawing Grid", gridImage);
        this.gridItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(
                    final org.eclipse.swt.events.SelectionEvent e) {
                FixedBuilderShell.this.builderSettings
                        .setUseGrid(!FixedBuilderShell.this.builderSettings
                                .useGrid());
                FixedBuilderShell.this.canvas
                        .setUseGrid(FixedBuilderShell.this.builderSettings
                                .useGrid());
                FixedBuilderShell.this.snapMenuItem
                        .setEnabled(FixedBuilderShell.this.builderSettings
                                .useGrid());
            }
        });
    }

    /**
     * Creates the Palette toolbar item and its event handlers.
     * 
     * @param utilTb
     *            the ToolBar to use as parent
     */
    private void createPaletteToolItem(final ToolBar utilTb) {
        this.paletteItem = createToolItem(utilTb, "Show Formatting Palette",
                paletteImage);
        this.paletteItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(
                    final org.eclipse.swt.events.SelectionEvent e) {
                if (FixedBuilderShell.this.palette == null) {
                    launchPalette();
                } else {
                    FixedBuilderShell.this.palette.makeVisible();
                }
            }
        });
    }

    /**
     * Creates the Add Header toolbar item and its event handlers.
     * 
     * @param addTb
     *            the ToolBar to use as parent
     */
    private void createHeaderToolItem(final ToolBar addTb) {
        this.headerItem = createAddObjectToolItem(addTb, "Add Header",
                headerImage, FixedFieldType.HEADER, SWT.DROP_DOWN);
        final HeaderDropdownSelectionListener listener = new HeaderDropdownSelectionListener(
                this.headerItem);
        final List<String> headers = appContext.getBean(PerspectiveProperties.class).getHeaderNames();
        if (headers != null) {
            for (final String headerType : headers) {
                listener.add(headerType);
            }
        }
        this.headerItem.addSelectionListener(listener);
    }

    /**
     * Creates the Add Object toolbar item (besides the Add Header tool) and its
     * event handlers.
     * 
     * @param addTb
     *            the ToolBar to use as parent
     */
    private void createAddObjectToolItems(final ToolBar addTb) {
        this.boxItem = createAddObjectToolItem(addTb, "Add Box", boxImage,
                FixedFieldType.BOX, SWT.PUSH);
        this.lineItem = createAddObjectToolItem(addTb, "Add Line", lineImage,
                FixedFieldType.LINE, SWT.PUSH);
        this.channelItem = createAddObjectToolItem(addTb, "Add Channel",
                channelImage, FixedFieldType.CHANNEL, SWT.PUSH);
        this.textItem = createAddObjectToolItem(addTb, "Add Text", textImage,
                FixedFieldType.TEXT, SWT.PUSH);
        this.buttonItem = createAddObjectToolItem(addTb, "Add Button",
                buttonImage, FixedFieldType.BUTTON, SWT.PUSH);
        this.imageItem = createAddObjectToolItem(addTb, "Add Image",
                imageImage, FixedFieldType.IMAGE, SWT.PUSH);
        this.timeItem = createAddObjectToolItem(addTb, "Add Time", timeImage,
                FixedFieldType.TIME, SWT.PUSH);
    }

    /**
     * Creates the Save toolbar item and its event handlers.
     * 
     * @param fileTb
     *            the ToolBar to use as parent
     */
    private void createSaveToolItem(final ToolBar fileTb) {
        this.saveItem = createToolItem(fileTb, "Save Current Fixed Layout",
                saveImage);
        this.saveItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(
                    final org.eclipse.swt.events.SelectionEvent e) {
                saveToFile(false);
            }
        });
    }

    /**
     * Creates the Open toolbar item and its event handlers.
     * 
     * @param fileTb
     *            the ToolBar to use as parent
     */
    private void createOpenToolItem(final ToolBar fileTb) {
        this.openItem = createToolItem(fileTb, "Open Fixed Layout", openImage);
        this.openItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(
                    final org.eclipse.swt.events.SelectionEvent e) {
                boolean yes = true;
                boolean cancelled = false;
                if (FixedBuilderShell.this.currentConfig != null
                        && FixedBuilderShell.this.currentConfig
                                .getFieldConfigs().size() != 0) {
                    yes = SWTUtilities
                            .showConfirmDialog(
                                    FixedBuilderShell.this.mainShell,
                                    "Save Reminder",
                                    "Do you want to save the existing layout before opening a new one?");
                    if (yes) {
                        cancelled = !saveToFile(false);
                    }
                }
                if (!cancelled) {
                    openFile(null);
                }
            }
        });
    }

    /**
     * Creates the New Layout toolbar item and its event handlers.
     * 
     * @param fileTb
     *            the ToolBar to use as parent
     */
    private void createNewLayoutToolItem(final ToolBar fileTb) {
        this.newItem = createToolItem(fileTb, "Create New Fixed Layout",
                newImage);
        this.newItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(
                    final org.eclipse.swt.events.SelectionEvent e) {
                try {
                    boolean yes = true;
                    boolean cancelled = false;
                    if (FixedBuilderShell.this.currentConfig != null
                            && FixedBuilderShell.this.currentConfig
                                    .getFieldConfigs().size() != 0) {
                        yes = SWTUtilities
                                .showConfirmDialog(
                                        FixedBuilderShell.this.mainShell,
                                        "Save Reminder",
                                        "Do you want to save the current layout before creating a new one?");
                        if (yes) {
                            cancelled = !saveToFile(false);
                        }
                    }
                    if (!cancelled) {
                        createNewLayout();
                    }
                } catch (final Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    private ToolItem createToolItem(final ToolBar tb, final String tooltip,
            final Image image) {
        final ToolItem item = new ToolItem(tb, SWT.PUSH);
        item.setToolTipText(tooltip);
        item.setImage(image);
        item.setEnabled(false);
        return item;
    }

    /**
     * Creates a toolbar item (tool button) designed to add an object to the
     * canvas.
     * 
     * @param tb
     *            the ToolBar to use as parent
     * @param tooltip
     *            the tooltip text for this item
     * @param image
     *            the image to display on the tool button
     * @param type
     *            the type of fixed field added by this tool button
     * @param widgetType
     *            the SWT widget type of the button (SWT.PUSH, SWT.DROP_DOWN,
     *            SWT.CASCADE)
     * @return the new ToolItem
     */
    private ToolItem createAddObjectToolItem(final ToolBar tb,
            final String tooltip, final Image image, final FixedFieldType type,
            final int widgetType) {
        final ToolItem item = new ToolItem(tb, widgetType);
        item.setToolTipText(tooltip);
        item.setImage(image);
        item.setEnabled(false);
        if (widgetType == SWT.PUSH) {
            addObjectAddButtonListener(item, type);
        }
        return item;
    }

    /**
     * Adds the selection listener for any add object tool item (tool button) on
     * the toolbar.
     * 
     * @param item
     *            the ToolItem to add the listener to
     * @param type
     *            the type of fixed field added by this tool button
     */
    private void addObjectAddButtonListener(final ToolItem item,
            final FixedFieldType type) {
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(
                    final org.eclipse.swt.events.SelectionEvent e) {
                try {
                    addNewElement(type);
                    setStatusMessageForNewObject(type);
                } catch (final Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    /**
     * Creates the drawing canvas.
     */
    private void createCanvas() {
        this.canvasComp = new ScrolledComposite(this.mainShell, SWT.BORDER
                | SWT.H_SCROLL | SWT.V_SCROLL);
        final FormLayout fl = new FormLayout();
        fl.marginHeight = 10;
        fl.marginWidth = 10;
        this.canvasComp.setLayout(fl);
        FormData fd = new FormData();
        fd.top = new FormAttachment(this.coolBar, 5);
        fd.left = new FormAttachment(0, 5);
        fd.right = new FormAttachment(100, -5);
        fd.bottom = new FormAttachment(100, -25);

        this.canvasComp.setLayoutData(fd);

        this.canvas = new FixedCanvas(this.canvasComp);
        fd = new FormData();
        fd.top = new FormAttachment(0);
        fd.left = new FormAttachment(0);
        this.canvas.getCanvas().setLayoutData(fd);
        this.canvas.setDefaultBackgroundColor(this.mainShell.getDisplay()
                .getSystemColor(SWT.COLOR_WHITE));
        this.canvas.setDefaultForegroundColor(this.mainShell.getDisplay()
                .getSystemColor(SWT.COLOR_BLACK));

        updateCanvasFromSettings();

        this.canvas.setEditable(true);
        this.canvasListener = new CanvasListener();
        this.canvas.addSelectionListener(this.canvasListener);
        this.canvasComp.setContent(this.canvas.getCanvas());
    }

    /**
     * Creates the builder status bar.
     */
    private void createStatusBar() {
        this.statusBar = new FixedBuilderStatusComposite(this.mainShell);
        final FormData fd = new FormData();
        fd.top = new FormAttachment(this.canvasComp);
        fd.left = new FormAttachment(0);
        fd.right = new FormAttachment(100);
        fd.bottom = new FormAttachment(100);
        this.statusBar.getComposite().setLayoutData(fd);
        if (this.currentFile == null) {
            this.statusBar
                    .setMessage("Select 'File->New' or the 'New' toolbar button to create a new fixed layout.");
        }
        this.statusBar.setCharacterMode(this.builderSettings
                .isCharacterCoordinateSystem());
        this.statusBar.enableTicker(this.builderSettings.useMessageTicker());
    }

    /**
     * Adds a new element (other than a header) to the canvas.
     * 
     * @param type
     *            the type of element to add, as a FixedFieldType
     */
    private void addNewElement(final FixedFieldType type) {

        // Create a new field configuration based upon the selected field type
        final IFixedFieldConfiguration config = FixedFieldConfigurationFactory
                .create(appContext, type);
        config.setCoordinateSystem(this.currentConfig.getCoordinateSystem());
        config.setBuilderDefaults(this.currentConfig.getCoordinateSystem());

        // Position the new element on the canvas at the latest mouse position
        final ChillPoint mousePos = this.canvas
                .getLastMousePositionInLayoutCoordinates();
        final ChillPoint start = config.getStartCoordinate();

        if (config instanceof DualPointFixedFieldConfiguration) {
            final DualPointFixedFieldConfiguration dpConfig = (DualPointFixedFieldConfiguration) config;
            final ChillPoint end = dpConfig.getEndCoordinate();
            if (end != null && !end.isUndefined()) {
                final int xlen = end.getX() - start.getX();
                final int ylen = end.getY() - start.getY();
                end.setX(mousePos.getX() + xlen);
                end.setY(mousePos.getY() + ylen);
                dpConfig.setEndCoordinate(end);
            }
        }
        start.setX(mousePos.getX());
        start.setY(mousePos.getY());
        config.setStartCoordinate(start);

        // Now create the actual canvas element
        final CanvasElement liveElem = CanvasElementFactory.create(config,
                this.canvas.getCanvas());
        this.liveElements = new ArrayList<CanvasElement>();
        this.liveElements.add(liveElem);
        liveElem.enableEditMode(true);

        // Add it to the current fixed layout view configuration
        this.currentConfig.addField(config);
        this.canvas.addCanvasElement(liveElem);

        this.canvas.setSelectedElement(liveElem);

        // Set static text if it's a channel field
        if (liveElem instanceof ChannelElement) {
            ((ChannelElement) liveElem).setTextFromChannelDefinition();
        }

        this.canvas.redraw();
    }

    /**
     * Adds a new header element to the drawing canvas.
     * 
     * @param type
     *            the header type identifier
     */
    private void addNewHeaderElement(final String type) {

        // Create a new header field configuration object of the right type
        final HeaderFieldConfiguration config = (HeaderFieldConfiguration) FixedFieldConfigurationFactory
                .create(appContext, FixedFieldType.HEADER);
        config.setHeaderType(type);
        config.load();

        // Headers may have a different coordinate system that the current
        // default.
        // If so, convert the coordinates within the header to the current
        // default.
        if (config.getCoordinateSystem() != this.builderSettings
                .getCoordinateSystem()) {
            config.convertCoordinates(this.builderSettings
                    .getCoordinateSystem(), this.canvas.getCharacterWidth(),
                    this.canvas.getCharacterHeight());
        }

        // Set the position for the new header to the last mouse position on the
        // canvas
        final ChillPoint mousePos = this.canvas
                .getLastMousePositionInLayoutCoordinates();
        final ChillPoint start = config.getStartCoordinate();
        start.setX(mousePos.getX());
        start.setY(mousePos.getY());
        config.setStartCoordinate(start);

        // Now create the actual canvas element, add it to the current view
        // configuration,
        // and select it on the canvas
        final CanvasElement liveElem = CanvasElementFactory.create(config,
                this.canvas.getCanvas());
        this.liveElements = new ArrayList<CanvasElement>();
        this.liveElements.add(liveElem);
        liveElem.enableEditMode(true);
        this.currentConfig.addField(config);
        this.canvas.addCanvasElement(liveElem);
        this.canvas.setSelectedElement(liveElem);
        this.canvas.redraw();
    }

    /**
     * Checks the current fixed layout configuration against the current
     * dictionary and display the results.
     */
    private void checkConfigAgainstDictionary() {
        if (this.currentConfig == null) {
            return;
        }

        // Run the current layout through the dictionary checker
        final PerspectiveDictionaryChecker checker = new PerspectiveDictionaryChecker(appContext);
        final List<String> undefinedChannels = checker
                .findUndefinedChannels(this.currentConfig);
        String displayText = null;
        if (undefinedChannels.size() == 0) {
            displayText = "All channels in this fixed layout are defined in the current dictionary.";
        } else {
            final StringBuilder sb = new StringBuilder(
                    "The following channels are referenced in this fixed layout but are not found in the current dictionary:\n\n");
            for (final String chan : undefinedChannels) {
                sb.append("   " + chan + "\n");
            }
            displayText = sb.toString();
        }

        // Display the results in a text shell
        if (this.checkerViewShell == null) {
            this.checkerViewShell = new TextViewShell(this.mainShell, trace);
            this.checkerViewShell.setText(displayText);
            this.checkerViewShell.getShell().setSize(450, 400);
            this.checkerViewShell.getShell().addDisposeListener(
                    new DisposeListener() {
                        @Override
						public void widgetDisposed(final DisposeEvent event) {
                            FixedBuilderShell.this.checkerViewShell = null;
                        }
                    });
            this.checkerViewShell.open();
        } else {
            this.checkerViewShell.setText(displayText);
            this.checkerViewShell.getShell().setActive();
        }
    }

    /**
     * Enables/disables tool items that are used in "add" mode.
     * 
     * @param en
     *            true to enable buttons, false to disable
     */
    private void enableToolItems(final boolean en) {
        this.boxItem.setEnabled(en);
        this.lineItem.setEnabled(en);
        this.textItem.setEnabled(en);
        this.channelItem.setEnabled(en);
        this.buttonItem.setEnabled(en);
        this.imageItem.setEnabled(en);
        this.timeItem.setEnabled(en);
        this.headerItem.setEnabled(en);
        this.paletteItem.setEnabled(en);
        this.gridItem.setEnabled(en);
        this.clearItem.setEnabled(en);
    }

    /**
     * Enables/disables menu items when a layout is opened/closed.
     * 
     * @param en
     *            true to enable buttons, false to disable
     */
    private void enableMenuItems(final boolean en) {
        enableToolItems(en);
        this.saveItem.setEnabled(en);
        this.saveMenuItem.setEnabled(en);
        this.saveAsMenuItem.setEnabled(en);
        this.preferencesMenuItem.setEnabled(en);
        this.saveImageMenuItem.setEnabled(en);
        this.closeMenuItem.setEnabled(en);
        this.showSourceMenuItem.setEnabled(en);
        this.checkDictionaryMenuItem.setEnabled(en);
        if (this.sourceViewShell != null) {
            this.sourceViewShell.close();
            this.sourceViewShell = null;
        }
    }

    /**
     * Launches the drawing palette.
     */
    private void launchPalette() {
        final Shell paletteShell = new Shell(this.parent, SWT.SHELL_TRIM);
        paletteShell.setLayout(new FillLayout());
        paletteShell.setSize(PALETTE_WIDTH, 500);
        this.palette = new Palette(this.appContext, paletteShell);

        if (this.canvas.getSelectedElements().isEmpty()) {
            this.palette.updateGui(new ArrayList<CanvasElement>());
        } else {
            this.palette.updateGui(this.canvas.getSelectedElements());
            this.palette.addChangeListener(this.canvasListener);
        }

        if (this.currentConfig != null) {
            this.palette.setFixedLayoutViewConfiguration(this.currentConfig);
        }
        paletteShell.open();

        paletteShell.addDisposeListener(new DisposeListener() {
            @Override
			public void widgetDisposed(final DisposeEvent arg0) {
                FixedBuilderShell.this.palette = null;
            }

        });
    }

    /**
     * Loads an existing fixed layout view into the builder form an XML file.
     * 
     * @param filename
     *            the path to the file to load
     */
    private void openFile(String filename) {

        if (filename == null) {
            filename = this.util.displayStickyFileChooser(false,
                    this.mainShell, "FixedBuilderShell", new String[] {
                            "*.xml", "*" });
        }
        if (filename == null) {
            return;
        }
        try {

            final ViewReference ref = new ViewReference();
            ref.setPath(filename);
            IViewConfiguration config = ref.parse(appContext);

            if (config == null) {
                SWTUtilities.showErrorDialog(this.mainShell, "Load Error",
                        "There was a problem loading the fixed layout from file "
                                + filename);
                return;
            }
            if (config.getViewType().equals(ViewType.SINGLE_VIEW_WINDOW)) {
                final List<IViewConfiguration> children = ((SingleWindowViewConfiguration) config)
                        .getViews();
                if (children == null || children.size() < 1) {
                    SWTUtilities.showErrorDialog(this.mainShell, "Load Error",
                            "There was no fixed layout found in file "
                                    + filename);
                    return;
                }
                config = children.get(0);
            }
            if (!(config.getViewType().equals(ViewType.FIXED_LAYOUT))) {
                SWTUtilities.showErrorDialog(this.mainShell, "Load Error",
                        "There was no fixed layout found in file " + filename);
                return;
            }
            this.currentFile = filename;

            normalizeCoordinates(config);

            this.canvas.clear();
            this.currentConfig = (IFixedLayoutViewConfiguration) config;
            this.canvas.setViewConfiguration(this.currentConfig);

            setCoordinateSystem();

            this.currentConfig.setViewReference(null);
            setConfigDictionariesFromBuilder(this.currentConfig, false);

            final List<IFixedFieldConfiguration> allConfigs = this.currentConfig
                    .getFieldConfigs();
            if (allConfigs != null) {
                for (final IFixedFieldConfiguration fieldConfig : allConfigs) {
                    final CanvasElement elem = CanvasElementFactory.create(
                            fieldConfig, this.canvas.getCanvas());
                    this.canvas.addCanvasElement(elem);
                }
            }

            updateGuiFromConfig();

            enableMenuItems(true);

            if (promptForBuilderDictionaryUpdate()) {
                setBuilderDictionariesFromConfig(this.currentConfig);
            }

            if (this.palette != null) {
                this.palette
                        .setFixedLayoutViewConfiguration(this.currentConfig);
            }

            this.statusBar.setMessage("Fixed layout '"
                    + this.currentConfig.getViewName()
                    + "' was loaded from file " + filename);

        } catch (final Exception ex) {
            SWTUtilities.showErrorDialog(this.mainShell, "Load Error",
                    "There was an error loading the layout: " + ex.toString());
            ex.printStackTrace();
            SWTUtilities.showErrorDialog(this.mainShell, "Load Error",
                    "There was an error loading the layout: " + ex.toString());
        }
    }

    /**
     * Creates a new fixed layout configuration.
     * 
     * @return true if the layout was created, false if not
     */
    private boolean createNewLayout() {

        final LayoutPreferencesShell shell = new LayoutPreferencesShell(
                appContext, this.mainShell);

        final IFixedLayoutViewConfiguration tempConfig = new FixedLayoutViewConfiguration(appContext);
        tempConfig.setCoordinateSystem(this.builderSettings
                .getCoordinateSystem());
        if (tempConfig.getCoordinateSystem().equals(CoordinateSystemType.PIXEL)) {
            tempConfig.setPreferredHeight(FixedCanvas.DEFAULT_PIXEL_HEIGHT);
            tempConfig.setPreferredWidth(FixedCanvas.DEFAULT_PIXEL_WIDTH);
        } else {
            tempConfig.setPreferredHeight(FixedCanvas.DEFAULT_CHARACTER_HEIGHT);
            tempConfig.setPreferredWidth(FixedCanvas.DEFAULT_CHARACTER_WIDTH);
        }
        setConfigDictionariesFromBuilder(tempConfig, true);

        shell.setFieldsFromViewConfiguration(tempConfig);
        shell.open();

        final Display d = shell.getShell().getDisplay();
        while (!shell.getShell().isDisposed()) {
            if (!d.readAndDispatch()) {
                d.sleep();
            }
        }

        final boolean cancelled = shell.wasCanceled();
        if (!cancelled) {
            this.canvas.clear();
            this.currentConfig = tempConfig;
            shell.updateViewConfiguration(this.currentConfig);
            this.canvas.setViewConfiguration(this.currentConfig);

            updateGuiFromConfig();

            if (this.palette != null) {
                this.palette
                        .setFixedLayoutViewConfiguration(this.currentConfig);
            }

            enableMenuItems(true);

            this.statusBar
                    .setMessage("New layout "
                            + this.currentConfig.getViewName()
                            + " created. Use the 'Add' buttons on the toolbar to add objects.");

            this.currentFile = null;
        }
        return !cancelled;
    }

    /**
     * Prompt the user to ask whether to set a new default dictionary.
     * 
     * @return true if the user wants the builder default dictionary to be
     *         updated.
     */
    private boolean promptForBuilderDictionaryUpdate() {

        boolean cancelled = true;

        String prompt = null;

        if (!dictConfig.getFswVersion().equals(this.currentConfig.getFswVersion())
                || (this.hasSse && !dictConfig.getSseVersion().equals(
                        this.currentConfig.getSseVersion()))) {
            if (this.hasSse) {
                prompt = "This fixed layout was defined with FSW/SSE dictionary versions '"
                        + this.currentConfig.getFswVersion()
                        + "'/'"
                        + this.currentConfig.getSseVersion()
                        + "'.\n"
                        + "Do you want the builder to load these versions?";
            } else {
                prompt = "The current fixed layout was defined with FSW dictionary version '"
                        + this.currentConfig.getFswVersion()
                        + "'.\n"
                        + "Do you want the builder to load this version?";
            }
        } else if (!dictConfig.getFswDictionaryDir().equals(
                this.currentConfig.getFswDictionaryDir())
                || (this.hasSse && !dictConfig.getSseDictionaryDir().equals(
                        this.currentConfig.getSseDictionaryDir()))) {
            if (this.hasSse) {
                prompt = "This fixed layout was defined using FSW/SSE dictionary directories\n'"
                        + this.currentConfig.getFswDictionaryDir()
                        + "' and '"
                        + this.currentConfig.getSseDictionaryDir()
                        + "'.\n"
                        + "Do you want the builder to load these versions?";
            } else {
                prompt = "The current fixed layout was defined using FSW dictionary directory\n'"
                        + this.currentConfig.getFswDictionaryDir()
                        + "'.\n"
                        + "Do you want the builder to load this version?";
            }
        }
        if (prompt == null) {
            return false;
        }
        if (this.builderSettings.showBuilderDictReplaceConfirmation()) {
            final ConfirmationShell confirmShell = new ConfirmationShell(this.mainShell, prompt, true);
            confirmShell.open();
            final Display d = confirmShell.getShell().getDisplay();
            while (!confirmShell.getShell().isDisposed()) {
                if (!d.readAndDispatch()) {
                    d.sleep();
                }
            }
            cancelled = confirmShell.wasCanceled();
            this.builderSettings
                    .setShowBuilderDictReplaceConfirmation(confirmShell
                            .getPromptAgain());
            this.builderSettings.setBuilderDictReplaceAnswer(!cancelled);
            this.builderSettings.save();
        } else {
            cancelled = !this.builderSettings.getBuilderDictReplaceAnswer();
        }
        return !cancelled;
    }

    /**
     * Prompt the user to ask whether to set the dictionary in the current fixed
     * layout view configuration.
     * 
     * @return true if the user wants to replace the current view
     *         configuration's dictionary.
     */
    private boolean promptForViewDictionaryUpdate() {

        boolean cancelled = true;
        String prompt = null;
        final String newFswDir = dictConfig.getFswDictionaryDir();
        final String newFswVersion = dictConfig.getFswVersion();
        final String newSseDir = dictConfig.getSseDictionaryDir();
        final String newSseVersion = dictConfig.getSseVersion();

        if (!this.currentConfig.getFswVersion().equals(newFswVersion)
                || (this.hasSse && !this.currentConfig.getSseVersion().equals(
                        newSseVersion))) {
            if (this.hasSse) {
                prompt = "The current fixed layout was defined with FSW/SSE dictionary versions '"
                        + this.currentConfig.getFswVersion()
                        + "'/'"
                        + this.currentConfig.getSseVersion()
                        + "'.\n"
                        + "Do you want to update this layout to use the versions ('"
                        + newFswVersion
                        + "'/'"
                        + newSseVersion
                        + "') you have just selected?";
            } else {
                prompt = "The current fixed layout was defined with FSW dictionary version '"
                        + this.currentConfig.getFswVersion()
                        + "'.\n"
                        + "Do you want to update this layout to use the version ('"
                        + newFswVersion + "') you have just selected?";
            }
        } else if (!this.currentConfig.getFswDictionaryDir().equals(newFswDir)
                || (this.hasSse && !this.currentConfig.getSseDictionaryDir()
                        .equals(newSseDir))) {
            if (this.hasSse) {
                prompt = "The current fixed layout was defined using FSW/SSE dictionary directories\n'"
                        + newFswDir
                        + "' and '"
                        + newSseDir
                        + "'.\n"
                        + "Do you want to update this layout to use the new directories?";
            } else {
                prompt = "The current fixed layout was defined using FSW dictionary directory\n'"
                        + newFswDir
                        + "'.\n"
                        + "Do you want to update the layout to use the new directory?";
            }
        }

        if (prompt != null
                && this.builderSettings.showViewerDictReplaceConfirmation()) {
            final ConfirmationShell confirmShell = new ConfirmationShell(this.mainShell, prompt, true);
            confirmShell.open();
            final Display d = confirmShell.getShell().getDisplay();
            while (!confirmShell.getShell().isDisposed()) {
                if (!d.readAndDispatch()) {
                    d.sleep();
                }
            }
            cancelled = confirmShell.wasCanceled();
            this.builderSettings
                    .setShowViewerDictReplaceConfirmation(confirmShell
                            .getPromptAgain());
            this.builderSettings.setViewerDictReplaceAnswer(!cancelled);
            this.builderSettings.save();
        } else {
            cancelled = !this.builderSettings.getViewerDictReplaceAnswer();
        }
        return !cancelled;
    }


    /**
     * Saves the current fixed layout configuration to an XML file. If the file
     * has not been saved before, or the user selected "Save As" then the user
     * will be prompted for save location.
     * 
     * @param saveAs
     *            true to perform a "save as"; false for simple save
     * @return
     */
    private boolean saveToFile(final boolean saveAs) {
        String filename = null;
        if (saveAs || this.currentFile == null) {
            filename = this.util.displayStickyFileSaver(this.mainShell,
                    "FixedBuilderShell", null, this.currentConfig.getViewName()
                            + ".xml");
            if (filename == null) {
                return false;
            }
        } else {
            filename = this.currentFile;
        }
        try {
            this.currentConfig.save(filename);
            this.statusBar.setMessage("Fixed layout saved to file " + filename
                    + ".");
            this.currentFile = filename;
        } catch (final IOException ex) {
            SWTUtilities.showErrorDialog(this.mainShell, "Layout Not Saved",
                    "There was an error saving the fixed layout: "
                            + ex.toString());
            return false;
        }
        return true;
    }

    /**
     * Sets the builder default dictionaries to the values in the given fixed
     * layout view configuration.
     * 
     * @param config
     *            the IFixedLayoutViewConfiguration to get dictionary from
     */
    private void setBuilderDictionariesFromConfig(
            final IFixedLayoutViewConfiguration config) {
        try {
            dictConfig.setFswVersion(config.getFswVersion());
            dictConfig.setFswDictionaryDir(config.getFswDictionaryDir());
            if (this.hasSse) {
                dictConfig.setSseVersion(config.getSseVersion());
                dictConfig.setSseDictionaryDir(config.getSseDictionaryDir());
            }
            // Why are we reloading?  I guess a different version...?
            final IChannelUtilityDictionaryManager dictMgr =  appContext.getBean(IChannelUtilityDictionaryManager.class);
            dictMgr.clearAll();
            dictMgr.loadAll(false);

        } catch (final Exception e) {
            //e.printStackTrace();
            SWTUtilities.showErrorDialog(this.mainShell,
                    "Dictionary Load Error",
                    "The selected channel dictionary could not be loaded:"
                            + (e.getMessage() == null ? e.toString() : e
                                    .getMessage()));
        }
        resetChannelDefinitionsInElements();
        this.canvas.redraw();
    }

    /**
     * Sets the dictionaries in the given fixed layout configuration to the
     * builder defaults.
     * 
     * @param config
     *            the IFixedLayoutViewConfiguration to set dictionary in
     * @param unconditional
     *            true to set dictionary even if the view configuration already
     *            contains one; false to set it only if the dictionary in the
     *            view configuration is null
     */
    private void setConfigDictionariesFromBuilder(
            final IFixedLayoutViewConfiguration config,
            final boolean unconditional) {

        if (config == null) {
            return;
        }
        if (unconditional || config.getFswVersion() == null) {
            config.setFswVersion(dictConfig.getFswVersion());
        }
        if (unconditional || config.getFswDictionaryDir() == null) {
            config.setFswDictionaryDir(dictConfig.getFswDictionaryDir());
        }
        if (this.hasSse) {
            if (unconditional || config.getSseVersion() == null) {
                config.setSseVersion(dictConfig.getSseVersion());
            }
            if (unconditional || config.getSseDictionaryDir() == null) {
                config.setSseDictionaryDir(dictConfig.getSseDictionaryDir());
            }
        }
        resetChannelDefinitionsInElements();
        this.canvas.redraw();
    }

    private void resetChannelDefinitionsInElements() {

        for (final CanvasElement elem : this.canvas.getCanvasElements()) {
            if (elem instanceof ChannelElement) {
                final ChannelElement chanElem = (ChannelElement) elem;
                chanElem.setChannelDefinition(appContext.getBean(IChannelDefinitionProvider.class).getDefinitionFromChannelId(
                                chanElem.getChannelId()));
            } else if (elem instanceof HeaderElement) {
                final List<CanvasElement> childElems = ((HeaderElement) elem)
                        .getChildElements();
                if (childElems != null) {
                    for (final CanvasElement childElem : childElems) {
                        if (childElem instanceof ChannelElement) {
                            final ChannelElement chanElem = (ChannelElement) childElem;
                            chanElem
                                    .setChannelDefinition(appContext.getBean(IChannelDefinitionProvider.class).getDefinitionFromChannelId(
                                                    chanElem.getChannelId()));
                        }
                    }
                }
            }
        }
    }

    /**
     * Display the builder preferences window and updates builder preferences
     * from user entries.
     * 
     * @return true if preferences were changed; false if canceled
     */
    private boolean setBuilderPreferences() {

        final BuilderPreferencesShell shell = new BuilderPreferencesShell(
                                                                          appContext.getBean(MissionProperties.class),
                                                                          appContext.getBean(SseContextFlag.class),
                                                                          this.mainShell,
                                                                          this.canvas.getCharacterWidth());

        shell.setDictionaryConfiguration(dictConfig);

        shell.setBuilderSettings(this.builderSettings);
        shell.open();

        final Display d = shell.getShell().getDisplay();
        while (!shell.getShell().isDisposed()) {
            if (!d.readAndDispatch()) {
                d.sleep();
            }
        }
        final boolean cancelled = shell.wasCanceled();
        if (!cancelled) {
            final int oldGridSize = this.builderSettings.getGridSize();
            final String oldFswDir = dictConfig.getFswDictionaryDir();
            final String oldFswVersion = dictConfig.getFswVersion();
            final String oldSseDir = dictConfig.getSseDictionaryDir();
            final String oldSseVersion = dictConfig.getSseVersion();

            shell.getBuilderSettings(this.builderSettings);
            if (this.currentConfig != null) {
                if (!this.builderSettings.getCoordinateSystem().equals(
                        this.currentConfig.getCoordinateSystem())) {
                    SWTUtilities
                            .showWarningDialog(
                                    this.mainShell,
                                    "Coordinate System Mismatch",
                                    "The current layout uses the "
                                            + this.currentConfig
                                                    .getCoordinateSystem()
                                                    .toString()
                                            + " coordinate system,\nwhich cannot be changed. Close the current layout "
                                            + " and then\nchange the coordinate system.");
                    this.builderSettings.setCoordinateSystem(this.currentConfig
                            .getCoordinateSystem());
                    this.builderSettings.setGridSize(oldGridSize);
                }
            }

            this.builderSettings.save();
            this.statusBar
                    .enableTicker(this.builderSettings.useMessageTicker());
            this.statusBar.setCharacterMode(this.builderSettings
                    .isCharacterCoordinateSystem());
            updateCanvasFromSettings();

            setConfigDictionariesFromBuilder(this.currentConfig, false);

            dictConfig.setFswDictionaryDir(shell.getFswDictionaryDir());
            dictConfig.setFswVersion(shell.getFswVersion());


            if (this.hasSse) {
                dictConfig.setSseDictionaryDir(shell.getSseDictionaryDir());
                dictConfig.setSseVersion(shell.getSseVersion());
            }

            if (!oldFswDir.equals(dictConfig.getFswDictionaryDir())
                    || !oldFswVersion.equals(dictConfig.getFswVersion())
                    || !oldSseDir.equals(dictConfig.getSseDictionaryDir())
                    || !oldSseVersion.equals(dictConfig.getSseVersion())) {
                try {
                	// Don't know why we are doing this here since it was already loaded, but...?
                	final IChannelUtilityDictionaryManager dictMgr =  appContext.getBean(IChannelUtilityDictionaryManager.class);
                    dictMgr.clearAll();
                    dictMgr.loadAll(false);
                    if (this.currentConfig != null) {
                        if (promptForViewDictionaryUpdate()) {
                            setConfigDictionariesFromBuilder(
                                    this.currentConfig, true);
                            this.statusBar
                                    .setMessage("Dictionary changed: Suggest running 'Utilities->Compare to Dictionary'.");
                        }
                    }
                    resetChannelDefinitionsInElements();
                } catch (final Exception e) {
                    e.printStackTrace();
                    SWTUtilities.showErrorDialog(this.mainShell,
                            "Dictionary Load Error",
                            "The selected channel dictionary could not be loaded:"
                                    + (e.getMessage() == null ? e.toString()
                                            : e.getMessage()));
                }
            }
        }
        return !cancelled;
    }

    /**
     * Display the layout preferences window and updates the current fixed
     * layout configuration from user entries.
     * 
     * @return true if preferences were changed; false if canceled
     */
    private boolean setLayoutPreferences() {
        final LayoutPreferencesShell shell = new LayoutPreferencesShell(
                appContext, this.mainShell);
        shell.setFieldsFromViewConfiguration(this.currentConfig);
        shell.open();
        final Display d = shell.getShell().getDisplay();
        while (!shell.getShell().isDisposed()) {
            if (!d.readAndDispatch()) {
                d.sleep();
            }
        }
        final boolean cancelled = shell.wasCanceled();
        if (!cancelled) {
            final String oldName = this.currentConfig.getViewName();
            shell.updateViewConfiguration(this.currentConfig);

            updateGuiFromConfig();
            if (oldName != null
                    && !oldName.equals(this.currentConfig.getViewName())) {
                this.currentFile = null;
            }
            if (shell.isResetDictionary()) {
                setConfigDictionariesFromBuilder(this.currentConfig, true);
            }
            this.canvas.setViewConfiguration(this.currentConfig);
            this.statusBar.setMessage("Layout preferences updated");
        }
        return !cancelled;
    }

    /**
     * Changes the builder's coordinate system to match the current fixed layout
     * view configuration.
     */
    private void setCoordinateSystem() {
        if (!this.builderSettings.getCoordinateSystem().equals(
                this.currentConfig.getCoordinateSystem())) {
            SWTUtilities.showMessageDialog(this.mainShell,
                    "Coordinate System Change",
                    "The builder has been switched to the "
                            + this.currentConfig.getCoordinateSystem()
                                    .toString() + " coordinate system.");
            this.builderSettings.setCoordinateSystem(this.currentConfig
                    .getCoordinateSystem());
            if (this.currentConfig.getCoordinateSystem().equals(
                    CoordinateSystemType.PIXEL)) {
                this.builderSettings.setGridSize(this.builderSettings
                        .getGridSize()
                        * this.canvas.getCharacterWidth());
            } else {
                this.builderSettings.setGridSize(this.builderSettings
                        .getGridSize()
                        / this.canvas.getCharacterWidth());
            }
            this.builderSettings.save();
        }

        this.canvas.setGridSize(this.builderSettings.getGridSize());
        this.statusBar.setCharacterMode(this.builderSettings
                .isCharacterCoordinateSystem());
    }

    /**
     * Updates the status bar when a new object is created on the canvas.
     * 
     * @param type
     *            the FixedFieldType of the new object
     */
    private void setStatusMessageForNewObject(final FixedFieldType type) {
        this.statusBar.setMessage("Use the palette to customize your "
                + type.toString().toLowerCase() + ".");
    }

    /**
     * Updates the drawing canvas from current builder settings.
     */
    private void updateCanvasFromSettings() {
        this.canvas.setCharacterLayout(this.builderSettings
                .isCharacterCoordinateSystem());
        this.canvas.setUseGrid(this.builderSettings.useGrid());
        this.canvas.setGridSize(this.builderSettings.getGridSize());
        this.canvas.setGridColor(ChillColorCreator
                .getColor(this.builderSettings.getGridColor()));
    }

    /**
     * Adjusts attributes of the overall GUI builder from the current fixed
     * layout configuration.
     */
    private void updateGuiFromConfig() {

        final Point size = getViewSize(this.currentConfig,
                this.mainShell.getDisplay());
        this.canvas.resizeInPixels(size.x, size.y);
        if (this.currentConfig.getBackgroundColor() != null) {
            this.canvas.setDefaultBackgroundColor(ChillColorCreator
                    .getColor(this.currentConfig.getBackgroundColor()));
        }
        if (this.currentConfig.getForegroundColor() != null) {
            this.canvas.setDefaultForegroundColor(ChillColorCreator
                    .getColor(this.currentConfig.getForegroundColor()));
        }
        if (this.currentConfig.getDataFont() != null) {
            this.canvas.setDefaultFont(ChillFontCreator
                    .getFont(this.currentConfig.getDataFont()));
        }
        this.canvas.setGridSize(this.builderSettings.getGridSize());
        this.statusBar.setName(this.currentConfig.getViewName());
        this.statusBar.setSize(this.currentConfig.getPreferredWidth(),
                this.currentConfig.getPreferredHeight());
        this.mainShell.setText(TITLE + " (" + this.currentConfig.getViewName()
                + ")");
        final int widthForView = Math.min(size.x + W_SIZE_PAD,
                this.defaultPixelWidth);
        final int heightForView = Math.min(size.y + H_SIZE_PAD,
                this.defaultPixelHeight);
        this.mainShell.setSize(Math.max(widthForView, MINIMUM_PIXEL_WIDTH),
                Math.max(heightForView, MINIMUM_PIXEL_HEIGHT));
        this.canvas.redraw();
    }
    
    /**
     * Returns the preferred size, if any, of a view to be loaded, in pixels.
     * @param vc ViewConfiguration the configuration for the view to get size for.
     * @param display the SWT device to use for computing size
     * @return Point object, giving size as x,y pixel coordinate
     */
    public Point getViewSize(final IViewConfiguration vc, final Display display) {
        int width = vc.getPreferredWidth();
        int height = vc.getPreferredHeight();
        if (vc.getViewType().equals(ViewType.FIXED_LAYOUT)) {
            final IFixedLayoutViewConfiguration vfc = (IFixedLayoutViewConfiguration)vc;    
            if (vfc.getCoordinateSystem().equals(CoordinateSystemType.CHARACTER)) {
                final ChillFont defFont = vc.getDataFont();
                Font font = ChillFontCreator.getFont(defFont);
                GC gc = new GC(display);
                gc.setFont(font);
                final int charWidth = SWTUtilities.getFontCharacterWidth(gc);
                final int charHeight = SWTUtilities.getFontCharacterHeight(gc);
                width = charWidth * width;
                height = charHeight * height;
                gc.dispose();
                gc = null;
                font.dispose();
                font = null;
            }
        }
        return new Point(width, height);
    }
    
    /**
     * Normalizes the coordinates in a ViewConfiguration so that all Fixed View coordinates 
     * are either pixel or character based.
     * 
     * @param config the top-level ViewConfiguration to normalize. Children will be normalized 
     * recursively.
     */
    public void normalizeCoordinates(final IViewConfiguration config) {
        if (config instanceof IFixedLayoutViewConfiguration) {
            final IFixedLayoutViewConfiguration fixedConfig = (IFixedLayoutViewConfiguration)config;
            final CoordinateSystemType coordSystem = fixedConfig.getCoordinateSystem();
            final ChillFont chillFont = fixedConfig.getDataFont(); 
            Font font = ChillFontCreator.getFont(chillFont);
            GC gc = new GC(Display.getCurrent());
            gc.setFont(font);
            final int charWidth = SWTUtilities.getFontCharacterWidth(gc);
            final int charHeight = SWTUtilities.getFontCharacterHeight(gc);
            final List<IFixedFieldConfiguration> fieldConfigs = ((IFixedLayoutViewConfiguration)config).getFieldConfigs();
            for (final IFixedFieldConfiguration field: fieldConfigs) {
                if (field instanceof HeaderFieldConfiguration) {
                    field.convertCoordinates(coordSystem, charWidth, charHeight);
                }
            }
            gc.dispose();
            gc = null;
            font.dispose();
            font = null;
        } else if (config instanceof IViewConfigurationContainer) {
            final List<IViewConfiguration> children = ((IViewConfigurationContainer)config).getViews();
            if (children != null) {
                for (final IViewConfiguration vc: children) {
                    normalizeCoordinates(vc);
                }
            }
        }
    }

    /**
     * This class listens for changes and selections to elements on the canvas.
     * 
     */
    public class CanvasListener implements CanvasSelectionListener,
            ElementConfigurationChangeListener {

        /**
         * @{inheritDoc}
         * @see jpl.gds.monitor.canvas.ElementConfigurationChangeListener#elementsChanged(java.util.List)
         */
        @Override
		public void elementsChanged(final List<IFixedFieldConfiguration> configs) {
            if (!FixedBuilderShell.this.liveElements.isEmpty()) {
                // question: I wonder if the liveElements and configs array are
                // lined up or if there will be a bug here
                for (int i = 0; i < configs.size(); i++) {
                    FixedBuilderShell.this.liveElements.get(i)
                            .setFieldConfiguration(configs.get(i));
                }
                FixedBuilderShell.this.canvas.redraw();
            }
        }

        /**
         * @{inheritDoc}
         * @see jpl.gds.monitor.canvas.CanvasSelectionListener#elementsDeselected(java.util.List, java.util.List)
         */
        @Override
		public void elementsDeselected(final List<CanvasElement> oldElements,
                final List<CanvasElement> newElements) {
            if (newElements == null || newElements.size() == 1) {
                FixedBuilderShell.this.deleteItem.setEnabled(false);
                FixedBuilderShell.this.duplicateItem.setEnabled(false);
                FixedBuilderShell.this.snapMenuItem.setEnabled(false);
                FixedBuilderShell.this.orderMenuItem.setEnabled(false);
                FixedBuilderShell.this.liveElements = null;

                if (FixedBuilderShell.this.palette != null) {
                    FixedBuilderShell.this.palette
                            .updateGui(new ArrayList<CanvasElement>());
                }
            }
        }

        /**
         * @{inheritDoc}
         * @see jpl.gds.monitor.canvas.CanvasSelectionListener#elementsSelected(java.util.List, java.util.List)
         */
        @Override
		public void elementsSelected(final List<CanvasElement> oldElements,
                final List<CanvasElement> newElements) {
            FixedBuilderShell.this.liveElements = new ArrayList<CanvasElement>(
                    newElements);

            FixedBuilderShell.this.deleteItem.setEnabled(true);
            FixedBuilderShell.this.duplicateItem.setEnabled(true);
            FixedBuilderShell.this.snapMenuItem
                    .setEnabled(FixedBuilderShell.this.builderSettings
                            .useGrid());
            FixedBuilderShell.this.orderMenuItem.setEnabled(true);

            // Set message
            if (newElements.size() == 1) {
                setStatusMessageForNewObject(newElements.get(0).getFieldType());

            } else {
                FixedBuilderShell.this.statusBar
                        .setMessage("Multiple objects selected.");
            }

            if (FixedBuilderShell.this.palette != null) {
                FixedBuilderShell.this.palette.updateGui(newElements);
                FixedBuilderShell.this.palette.addChangeListener(this);
            }
        }

        /**
         * @{inheritDoc}
         * @see jpl.gds.monitor.canvas.ElementConfigurationChangeListener#timeSourceChanged(java.util.List)
         */
        @Override
		public void timeSourceChanged(final List<CanvasElement> elements) {
            FixedBuilderShell.this.palette
                    .changeFormatComposite(elements, this);
        }
    }

    /**
     * This class handles the header drop down on the coolbar.
     * 
     */
    class HeaderDropdownSelectionListener extends SelectionAdapter {

        private final Menu menu;

        /**
         * Creates a new drop down menu and selection listener.
         * 
         * @param dropdown
         *            the ToolItem to add the drop down menu to
         */
        public HeaderDropdownSelectionListener(final ToolItem dropdown) {
            this.menu = new Menu(dropdown.getParent().getShell());
        }

        /**
         * Adds a menu item to the drop down menu.
         * 
         * @param item
         *            the text of the menu item to add
         */
        public void add(final String item) {
            final MenuItem menuItem = new MenuItem(this.menu, SWT.NONE);
            menuItem.setText(item);

            menuItem.addSelectionListener(new SelectionAdapter() {

                @Override
                public void widgetSelected(final SelectionEvent event) {
                    final MenuItem selected = (MenuItem) event.widget;
                    addNewHeaderElement(selected.getText());
                    setStatusMessageForNewObject(FixedFieldType.HEADER);
                }
            });
        }

        /**
         * {@inheritDoc}
         * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
         */
        @Override
        public void widgetSelected(final SelectionEvent event) {
                final ToolItem item = (ToolItem) event.widget;
                final Rectangle rect = item.getBounds();
                final Point pt = item.getParent().toDisplay(
                        new Point(rect.x, rect.y));
                this.menu.setLocation(pt.x, pt.y + rect.height);
                this.menu.setVisible(true);
            
        }
    }
}
