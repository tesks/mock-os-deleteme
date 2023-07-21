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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.ShellListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.monitor.canvas.FixedCanvas;
import jpl.gds.monitor.fixedbuilder.palette.FontDialogComposite;
import jpl.gds.monitor.guiapp.common.gui.RealtimeRecordedComposite;
import jpl.gds.monitor.guiapp.common.gui.StationComposite;
import jpl.gds.monitor.perspective.view.RealtimeRecordedFilterType;
import jpl.gds.monitor.perspective.view.fixed.IFixedLayoutViewConfiguration;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.swt.ChillShell;
import jpl.gds.shared.swt.ColorComposite;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.shared.swt.StalenessComposite;
import jpl.gds.shared.swt.types.ChillColor;
import jpl.gds.shared.swt.types.ChillColor.ColorName;
import jpl.gds.shared.swt.types.ChillFont;
import jpl.gds.shared.swt.types.CoordinateSystemType;

/**
 * This class is a GUI window used to configure layout-specific settings for the
 * fixed view builder.
 * 
 * @see FixedBuilderShell
 */
public class LayoutPreferencesShell implements ChillShell {

    private static final String TITLE = "Layout Preferences";
    private static final String FSW_VERSION_TAG = "FSW Version: ";
    private static final String SSE_VERSION_TAG = "SSE Version: ";
    private static final String PIXEL_LABEL = "  pixels       ";
    private static final String CHARACTER_LABEL = "  characters  ";

    private static final int DEFAULT_WIDTH = FixedCanvas.DEFAULT_PIXEL_WIDTH;
    private static final int DEFAULT_HEIGHT = FixedCanvas.DEFAULT_PIXEL_HEIGHT;

    // Indicates if this shell was cancelled
    private boolean cancelled;
    // Indicates how the shell was closed
    private boolean buttonUsed;

    // Variables that hold the actual layout settings locally

    private int canvasWidth;
    private int canvasHeight;
    private ChillFont font;
    private String layoutName;
    private ChillColor backgroundColor;
    private ChillColor foregroundColor;
    private boolean resetDictionary;
    private RealtimeRecordedFilterType rtRecFilter;
    private int stalenessInterval;

    private int stationId;

    // GUI components
    private Shell mainShell;
    private final Shell parent;
    private Text nameText;
    private ColorComposite backColorComp;
    private ColorComposite foreColorComp;
    private FontDialogComposite fontComp;
    private Spinner widthSpinner;
    private Spinner heightSpinner;
    private Label fswVersionLabel;
    private Label sseVersionLabel;
    private RealtimeRecordedComposite rtRecComposite;
    private StalenessComposite stalenessComposite;
    private StationComposite stationComposite;
    private Label widthPixelLabel;
    private Label heightPixelLabel;
	private final ApplicationContext appContext;

    private final Tracer               logger;

    /**
     * Creates a LayoutPreferencesShell with the given shell as parent.
     * 
     * @param parent
     *            the parent Shell widget
     */
    public LayoutPreferencesShell(final ApplicationContext appContext, final Shell parent) {
    	this.appContext = appContext;
        this.parent = parent;
        this.logger = TraceManager.getDefaultTracer(appContext);
        createGui();
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
     * Gets the reset dictionary flag.
     * 
     * @return true if dictionary settings were modified by the user; false if
     *         not
     */
    public boolean isResetDictionary() {
        return this.resetDictionary;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.swt.ChillShell#open()
     */
    @Override
    public void open() {
        this.mainShell.open();
    }

    /**
     * Sets the fields in this GUI object from the given fixed layout view
     * configuration.
     * 
     * @param tempConfig
     *            the FixedLayoutViewConfiguration to get values from
     */
    public void setFieldsFromViewConfiguration(
            final IFixedLayoutViewConfiguration tempConfig) {
        if (tempConfig.getViewName() != null) {
            this.nameText.setText(tempConfig.getViewName());
        }
        if (tempConfig.getBackgroundColor() != null) {
            this.backColorComp.setCurrentColor(tempConfig.getBackgroundColor());
        }
        if (tempConfig.getForegroundColor() != null) {
            this.foreColorComp.setCurrentColor(tempConfig.getForegroundColor());
        }

        this.fontComp.setFont(tempConfig.getDataFont());

        this.heightSpinner.setSelection(tempConfig.getPreferredHeight());
        this.widthSpinner.setSelection(tempConfig.getPreferredWidth());
        this.fswVersionLabel.setText(FSW_VERSION_TAG + tempConfig.getFswVersion());
        if (this.sseVersionLabel != null) {
            this.sseVersionLabel.setText(SSE_VERSION_TAG + tempConfig.getSseVersion());
        }

        /*
         * Recorded/realtime flag is  an enum
         * rather than a boolean. Temporarily map back to a boolean.
         */
        this.rtRecComposite.setRealtimeRecordedFilterType(tempConfig.getRealtimeRecordedFilterType());

        this.stalenessComposite.setStalenessInterval(tempConfig.getStalenessInterval());
        if (tempConfig.getCoordinateSystem().equals(CoordinateSystemType.PIXEL)) {
            this.widthPixelLabel.setText(PIXEL_LABEL);
            this.heightPixelLabel.setText(PIXEL_LABEL);
        } else {
            this.widthPixelLabel.setText(CHARACTER_LABEL);
            this.heightPixelLabel.setText(CHARACTER_LABEL);
        }
        this.stationComposite.setStationId(tempConfig.getStationId());

        this.mainShell.layout();
        this.mainShell.pack();
    }

    /**
     * Updates the given FixedLayoutViewConfiguration with values entered in
     * this window.
     * 
     * @param config
     *            The FixedLayoutViewConfiguration to update
     */
    public void updateViewConfiguration(
            final IFixedLayoutViewConfiguration config) {
        config.setViewName(this.layoutName);
        config.setPreferredHeight(this.canvasHeight);
        config.setPreferredWidth(this.canvasWidth);

        if (this.font != null) {
            config.setDataFont(this.font);
        }

        if (this.backgroundColor != null) {
            config.setBackgroundColor(this.backgroundColor);
        }
        if (this.foregroundColor != null) {
            config.setForegroundColor(this.foregroundColor);
        }

        config.setStalenessInterval(this.stalenessInterval);
        /*
         * Recorded/realtime flag is  an enum
         * rather than a boolean. Temporarily map the boolean back to the enum.
         */
        config.setRealtimeRecordedFilterType(this.rtRecFilter);
        

        config.setStationId(this.stationId);
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.swt.ChillShell#wasCanceled()
     */
    @Override
    public boolean wasCanceled() {
        return this.cancelled;
    }

    // Entry point for GUI creation
    private void createGui() {
        try {
            this.mainShell = new Shell(this.parent, SWT.DIALOG_TRIM);
            final FormLayout fl = new FormLayout();
            fl.spacing = 10;
            fl.marginWidth = 10;
            fl.marginTop = 10;
            this.mainShell.setLayout(fl);

            final FontData groupFontData = new FontData("Helvetica", 14,
                    SWT.BOLD);
            final Font groupFont = new Font(this.mainShell.getDisplay(),
                    groupFontData);

            createNameControls();
            final Group sizeGroup = createSizeGroup(groupFont);
            final Group colorGroup = createColorGroup(groupFont, sizeGroup);
            final Group fontGroup = createFontGroup(groupFont, colorGroup);
            final Group dataGroup = createDataGroup(groupFont, fontGroup);
            final Group dictComp = createDictionaryGroup(groupFont, dataGroup);

            createButtonComposite(dictComp);
        } catch (final Exception e) {
            e.printStackTrace();
            TraceManager.getDefaultTracer().error(

                    "Error creating GUI controls in LayoutPreferencesShell");
        }
    }

    private void createButtonComposite(final Group dictComp) {
        final Label line = new Label(this.mainShell, SWT.SEPARATOR
                | SWT.HORIZONTAL | SWT.SHADOW_ETCHED_IN);
        final FormData formData6 = new FormData();
        formData6.top = new FormAttachment(dictComp);
        formData6.left = new FormAttachment(0);
        formData6.right = new FormAttachment(100);
        line.setLayoutData(formData6);

        final Composite composite = new Composite(this.mainShell, SWT.NONE);
        final GridLayout gl = new GridLayout(2, true);
        composite.setLayout(gl);
        final FormData formData8 = new FormData();
        formData8.top = new FormAttachment(line);
        formData8.right = new FormAttachment(100);
        formData8.bottom = new FormAttachment(100);
        composite.setLayoutData(formData8);

        final Button applyButton = new Button(composite, SWT.PUSH);
        applyButton.setText("Ok");
        this.mainShell.setDefaultButton(applyButton);
        final GridData gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        applyButton.setLayoutData(gd);
        final Button cancelButton = new Button(composite, SWT.PUSH);
        cancelButton.setText("Cancel");

        applyButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(
                    final org.eclipse.swt.events.SelectionEvent e) {
                try {
                    LayoutPreferencesShell.this.layoutName = LayoutPreferencesShell.this.nameText
                            .getText().trim();
                    if (LayoutPreferencesShell.this.layoutName.equals("")) {
                        SWTUtilities.showErrorDialog(
                                LayoutPreferencesShell.this.mainShell,
                                "No Name",
                                "You must enter a name for the fixed layout.");
                        return;
                    }
                    LayoutPreferencesShell.this.canvasHeight = LayoutPreferencesShell.this.heightSpinner
                            .getSelection();
                    LayoutPreferencesShell.this.canvasWidth = LayoutPreferencesShell.this.widthSpinner
                            .getSelection();
                    LayoutPreferencesShell.this.backgroundColor = LayoutPreferencesShell.this.backColorComp
                            .getCurrentColor();
                    LayoutPreferencesShell.this.foregroundColor = LayoutPreferencesShell.this.foreColorComp
                            .getCurrentColor();
                    LayoutPreferencesShell.this.font = LayoutPreferencesShell.this.fontComp
                            .getFont();
                    LayoutPreferencesShell.this.rtRecFilter = LayoutPreferencesShell.this.rtRecComposite
                            .getRealtimeRecordedFilterType();
                    try {
                        LayoutPreferencesShell.this.stalenessInterval = LayoutPreferencesShell.this.stalenessComposite
                                .getStalenessInterval();
                    } catch (final NumberFormatException ex) {
                        SWTUtilities
                        .showErrorDialog(
                                LayoutPreferencesShell.this.mainShell,
                                "Bad Staleness Interval",
                                "Staleness interval must be a number greater than or equal to 0");
                        return;
                    }

                    LayoutPreferencesShell.this.stationId = 
                    		LayoutPreferencesShell.this.stationComposite.
                    		getStationId();
                    
                    LayoutPreferencesShell.this.cancelled = false;
                    LayoutPreferencesShell.this.buttonUsed = true;
                    LayoutPreferencesShell.this.mainShell.close();
                } catch (final Exception e1) {
                    e1.printStackTrace();
                    TraceManager

                            .getDefaultTracer()
                            .error(
                                    "Error handling APPLY button event in LayoutPreferencesShell");
                }
            }
        });
        cancelButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(
                    final org.eclipse.swt.events.SelectionEvent e) {
                try {
                    LayoutPreferencesShell.this.cancelled = true;
                    LayoutPreferencesShell.this.buttonUsed = true;
                    LayoutPreferencesShell.this.mainShell.close();
                } catch (final Exception e1) {
                    e1.printStackTrace();
                }
            }
        });

        // listener for exit button on the title bar
        this.mainShell.addShellListener(new ShellListener() {

            @Override
            public void shellActivated(final ShellEvent arg0) {
                // intentionally empty
            }

            @Override
            public void shellClosed(final ShellEvent event) {
                try {
                    if (LayoutPreferencesShell.this.buttonUsed) {
                        LayoutPreferencesShell.this.buttonUsed = false;
                        return;
                    }

                    LayoutPreferencesShell.this.cancelled = true;
                } catch (final Exception ex) {
                    ex.printStackTrace();
                }
            }

            @Override
            public void shellDeactivated(final ShellEvent arg0) {
                // intentionally empty
            }

            @Override
            public void shellDeiconified(final ShellEvent arg0) {
                // intentionally empty
            }

            @Override
            public void shellIconified(final ShellEvent arg0) {
                // intentionally empty
            }
        });
    }

    private Group createColorGroup(final Font groupFont, final Group sizeGroup) {
        final Group colorGroup = new Group(this.mainShell, SWT.BORDER);
        colorGroup.setFont(groupFont);
        colorGroup.setText("Default Colors");
        final FormData cgfd = new FormData();
        cgfd.top = new FormAttachment(sizeGroup);
        cgfd.left = new FormAttachment(0);
        cgfd.right = new FormAttachment(100);
        colorGroup.setLayoutData(cgfd);
        colorGroup.setLayout(new FormLayout());

        this.backColorComp = new ColorComposite(colorGroup, "Background Color", logger);
        final FormData bfd = new FormData();
        bfd.left = new FormAttachment(0);
        bfd.top = new FormAttachment(0);
        this.backColorComp.getComposite().setLayoutData(bfd);
        this.backColorComp.setCurrentColor(new ChillColor(ColorName.WHITE));

        this.foreColorComp = new ColorComposite(colorGroup, "Foreground Color", logger);
        final FormData ffd = new FormData();
        ffd.left = new FormAttachment(0);
        ffd.top = new FormAttachment(this.backColorComp.getComposite());
        this.foreColorComp.getComposite().setLayoutData(ffd);
        this.foreColorComp.setCurrentColor(new ChillColor(ColorName.BLACK));
        return colorGroup;
    }

    private Group createDataGroup(final Font groupFont, final Group fontGroup) {
        RowLayout rl;
        final Group dataGroup = new Group(this.mainShell, SWT.BORDER);
        dataGroup.setFont(groupFont);
        dataGroup.setText("Data Handling");
        final FormData dgfd = new FormData();
        dgfd.top = new FormAttachment(fontGroup);
        dgfd.left = new FormAttachment(0);
        dgfd.right = new FormAttachment(100);
        dataGroup.setLayoutData(dgfd);
        rl = new RowLayout(SWT.VERTICAL);
        rl.marginLeft = 5;
        rl.marginTop = 10;
        dataGroup.setLayout(rl);

        this.rtRecComposite = new RealtimeRecordedComposite(dataGroup);

        final Composite horizontalComposite = new Composite(dataGroup, SWT.NONE);
        final RowLayout horizontalRow = new RowLayout(SWT.HORIZONTAL);
        horizontalRow.marginLeft = 0;
        horizontalComposite.setLayout(horizontalRow);
        
        this.stalenessComposite = new StalenessComposite(horizontalComposite);

        this.stationComposite = new StationComposite(appContext.getBean(MissionProperties.class).getStationMapper(), horizontalComposite);
      
        return dataGroup;
    }

    private Group createDictionaryGroup(final Font groupFont,
            final Group dataGroup) {
        final Group dictComp = new Group(this.mainShell, SWT.BORDER);
        dictComp.setFont(groupFont);
        dictComp.setText("Dictionaries");
        final FormData dcfd = new FormData();
        dcfd.top = new FormAttachment(dataGroup);
        dcfd.left = new FormAttachment(0);
        dcfd.right = new FormAttachment(100);
        dictComp.setLayoutData(dcfd);
        final RowLayout rl = new RowLayout(SWT.HORIZONTAL);
        rl.spacing = 10;

        dictComp.setLayout(rl);

        this.fswVersionLabel = new Label(dictComp, SWT.NONE);
        this.fswVersionLabel.setText(FSW_VERSION_TAG);

        if (appContext.getBean(MissionProperties.class).missionHasSse()) {
            this.sseVersionLabel = new Label(dictComp, SWT.NONE);
            this.sseVersionLabel.setText(SSE_VERSION_TAG);
        }

        final Button setDictionaryButton = new Button(dictComp, SWT.PUSH);
        setDictionaryButton.setText("Set to Builder Defaults");
        setDictionaryButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(
                    final org.eclipse.swt.events.SelectionEvent e) {
                try {
                    final DictionaryProperties config = appContext.getBean(DictionaryProperties.class);
                    LayoutPreferencesShell.this.fswVersionLabel
                            .setText(FSW_VERSION_TAG + config.getFswVersion());
                    if (LayoutPreferencesShell.this.sseVersionLabel != null) {
                        LayoutPreferencesShell.this.sseVersionLabel
                                .setText(SSE_VERSION_TAG
                                        + config.getSseVersion());
                    }
                    LayoutPreferencesShell.this.resetDictionary = true;
                    LayoutPreferencesShell.this.mainShell.layout();
                    LayoutPreferencesShell.this.mainShell.pack();
                } catch (final Exception e1) {
                    e1.printStackTrace();
                }
            }
        });
        return dictComp;
    }

    private Group createFontGroup(final Font groupFont, final Group colorGroup) {
        final Group fontGroup = new Group(this.mainShell, SWT.BORDER);
        fontGroup.setFont(groupFont);
        fontGroup.setText("Default Font");
        final FormData fgfd = new FormData();
        fgfd.top = new FormAttachment(colorGroup);
        fgfd.left = new FormAttachment(0);
        fgfd.right = new FormAttachment(100);
        fontGroup.setLayoutData(fgfd);
        fontGroup.setLayout(new FormLayout());

        this.fontComp = new FontDialogComposite(fontGroup, false);
        final FormData fcfd = new FormData();
        fcfd.left = new FormAttachment(0);
        fcfd.top = new FormAttachment(0);
        this.fontComp.getComposite().setLayoutData(fcfd);
        this.fontComp.useDefaultColors();
        return fontGroup;
    }

    private void createNameControls() {
        final Label nameLabel = new Label(this.mainShell, SWT.NONE);
        nameLabel.setText("Fixed Layout Name:");
        final FormData nlfd = new FormData();
        nlfd.top = new FormAttachment(0);
        nlfd.left = new FormAttachment(0);
        nameLabel.setLayoutData(nlfd);

        this.nameText = new Text(this.mainShell, SWT.BORDER);
        final FormData ntfd = SWTUtilities.getFormData(this.nameText, 1, 30);
        ntfd.top = new FormAttachment(nameLabel, 0, SWT.CENTER);
        ntfd.left = new FormAttachment(nameLabel);
        this.nameText.setLayoutData(ntfd);
    }

    private Group createSizeGroup(final Font groupFont) {
        final Group sizeGroup = new Group(this.mainShell, SWT.BORDER);
        sizeGroup.setFont(groupFont);
        sizeGroup.setText("Canvas Size");
        final FormData sgfd = new FormData();
        sgfd.top = new FormAttachment(this.nameText);
        sgfd.left = new FormAttachment(0);
        sgfd.right = new FormAttachment(100);
        sizeGroup.setLayoutData(sgfd);
        sizeGroup.setLayout(new RowLayout(SWT.VERTICAL));

        final Composite heightComp = new Composite(sizeGroup, SWT.NONE);
        heightComp.setLayout(new FormLayout());

        final Label heightLabel = new Label(heightComp, SWT.NONE);
        heightLabel.setText("Height: ");
        final FormData hlfd = new FormData();
        hlfd.top = new FormAttachment(0, 3);
        hlfd.left = new FormAttachment(0);
        heightLabel.setLayoutData(hlfd);

        this.heightSpinner = new Spinner(heightComp, SWT.NONE);
        this.heightSpinner.setDigits(0);
        this.heightSpinner.setIncrement(10);
        this.heightSpinner.setMaximum(2096);
        this.heightSpinner.setMinimum(2);
        this.heightSpinner.setSelection(DEFAULT_HEIGHT);
        final FormData hsfd = new FormData();
        hsfd.top = new FormAttachment(heightLabel, 0, SWT.CENTER);
        hsfd.left = new FormAttachment(heightLabel);
        this.heightSpinner.setLayoutData(hsfd);

        this.heightPixelLabel = new Label(heightComp, SWT.NONE);
        this.heightPixelLabel.setText(PIXEL_LABEL);
        final FormData hpfd = new FormData();
        hpfd.top = new FormAttachment(heightLabel, 0, SWT.CENTER);
        hpfd.left = new FormAttachment(this.heightSpinner, 0, 3);
        this.heightPixelLabel.setLayoutData(hpfd);

        final Composite widthComp = new Composite(sizeGroup, SWT.NONE);
        widthComp.setLayout(new FormLayout());

        final Label widthLabel = new Label(widthComp, SWT.NONE);
        widthLabel.setText("Width: ");
        final FormData wlfd = new FormData();
        wlfd.top = new FormAttachment(0, 3);
        wlfd.left = new FormAttachment(0);
        widthLabel.setLayoutData(wlfd);

        this.widthSpinner = new Spinner(widthComp, SWT.NONE);
        this.widthSpinner.setDigits(0);
        this.widthSpinner.setIncrement(10);
        this.widthSpinner.setMaximum(2096);
        this.widthSpinner.setMinimum(2);
        this.widthSpinner.setSelection(DEFAULT_WIDTH);
        final FormData wsfd = new FormData();
        wsfd.top = new FormAttachment(widthLabel, 0, SWT.CENTER);
        wsfd.left = new FormAttachment(widthLabel, 0, 3);
        this.widthSpinner.setLayoutData(wsfd);

        this.widthPixelLabel = new Label(widthComp, SWT.NONE);
        this.widthPixelLabel.setText(PIXEL_LABEL);
        final FormData wpfd = new FormData();
        wpfd.top = new FormAttachment(0, 3);
        wpfd.left = new FormAttachment(this.widthSpinner, 0, 3);
        this.widthPixelLabel.setLayoutData(wpfd);
        return sizeGroup;
    }
}
