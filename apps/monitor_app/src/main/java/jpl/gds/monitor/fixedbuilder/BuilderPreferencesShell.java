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

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.monitor.guiapp.common.gui.DictionarySelectionComposite;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.swt.ChillShell;
import jpl.gds.shared.swt.ColorComposite;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.shared.swt.types.ChillColor;
import jpl.gds.shared.swt.types.ChillColor.ColorName;
import jpl.gds.shared.swt.types.CoordinateSystemType;

/**
 * This class presents a GUI window for adjusting preferences in the fixed view
 * builder.
 * 
 * @see FixedBuilderShell
 * 
 */
public class BuilderPreferencesShell implements ChillShell {

    private static final String CHARACTER_LABEL = "  characters  ";
    private static final String PIXEL_LABEL = "  pixels       ";
    private static final String TITLE = "Builder Preferences";

    // Indicates if the shell was cancelled
    private boolean cancelled;
    // Indicates how the shell was cancelled
    private boolean buttonUsed;
    // Width of a character, in pixels, in the current layout's default font
    private final int charWidth;

    // Variables that hold the actual builder settings locally
    private CoordinateSystemType coordinateType;
    private String fswDictionaryDir;
    private String fswVersion;
    private ChillColor gridColor;
    private int gridSize;
    private String sseDictionaryDir;
    private String sseVersion;
    private boolean useGrid;
    private boolean useTicker;

    // GUI components
    private Shell mainShell;
    private final Shell parent;
    private DictionarySelectionComposite dictComp;
    private ColorComposite gridColorComp;
    private Spinner gridSizeSpinner;
    private Button useCharactersButton;
    private Button useTickerButton;
    private Button useGridButton;
    private Button usePixelsButton;
    private Label unitsLabel;
    
	private final MissionProperties missionProps;
    private final SseContextFlag         sseFlag;

    /**
     * Creates a new BuilderPreferencesShell with the given parent.
     * 
     * @param parent
     *            the parent Shell widget
     * @param currentCharWidth width of a character
     */
    public BuilderPreferencesShell(final MissionProperties missionProps,
            final SseContextFlag sseFlag,
    		final Shell parent,
            final int currentCharWidth) {
    	this.missionProps = missionProps;
        this.sseFlag = sseFlag;
        this.parent = parent;
        this.charWidth = currentCharWidth;
        createGui();
    }

    /**
     * Copies values entered in GUI fields to the given FixedBuilderSettings
     * object.
     * 
     * @param settings
     *            the FixedBuilderSettings object to update
     */
    public void getBuilderSettings(final FixedBuilderSettings settings) {

        settings.setUseMessageTicker(this.useTicker);
        settings.setUseGrid(this.useGrid);
        settings.setGridSize(this.gridSize);
        settings.setGridColor(this.gridColor);
        settings.setCoordinateSystem(this.coordinateType);
    }

    /**
     * Gets the FSW dictionary directory selected by the user.
     * 
     * @return the dictionary path
     */
    public String getFswDictionaryDir() {
        return this.fswDictionaryDir;
    }

    /**
     * Gets the FSW dictionary version selected by the user.
     * 
     * @return the dictionary version string
     */
    public String getFswVersion() {
        return this.fswVersion;
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
     * Gets the SSE dictionary directory selected by the user.
     * 
     * @return the dictionary path
     */
    public String getSseDictionaryDir() {
        return this.sseDictionaryDir;
    }

    /**
     * Gets the SSE dictionary version selected by the user.
     * 
     * @return the dictionary version string
     */
    public String getSseVersion() {
        return this.sseVersion;
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
        this.mainShell.open();
    }

    /**
     * Initializes GUI fields from data in the given FixedBuilderSettings
     * objects.
     * 
     * @param settings
     *            the FixedBuilderSettings object to get data from
     */
    public void setBuilderSettings(final FixedBuilderSettings settings) {
        this.useTicker = settings.useMessageTicker();
        this.useGrid = settings.useGrid();
        this.gridSize = settings.getGridSize();
        this.gridColor = settings.getGridColor();

        if (!this.useTickerButton.isDisposed()) {
            this.useTickerButton.setSelection(this.useTicker);
            this.useGridButton.setSelection(this.useGrid);
            this.gridSizeSpinner.setSelection(this.gridSize);
            this.gridColorComp.setCurrentColor(this.gridColor);
        }

        this.coordinateType = settings.getCoordinateSystem();
        if (!settings.isCharacterCoordinateSystem()) {
            this.usePixelsButton.setSelection(true);
            this.unitsLabel.setText(PIXEL_LABEL);
        } else {
            this.useCharactersButton.setSelection(true);
            this.unitsLabel.setText(CHARACTER_LABEL);
        }
    }

    /**
     * Initializes dictionary GUI fields from the given DictionaryConfiguration.
     * 
     * @param config
     *            the DictionaryConfiguration object to get dictionary information
     *            from
     */
    public void setDictionaryConfiguration(final DictionaryProperties config) {
        this.dictComp.setDictionaryConfiguration(config);
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
            fl.spacing = 5;
            fl.marginWidth = 5;
            fl.marginTop = 10;
            this.mainShell.setLayout(fl);

            final FontData groupFontData = new FontData("Helvetica", 14,
                    SWT.BOLD);
            final Font groupFont = new Font(this.mainShell.getDisplay(),
                    groupFontData);

            final Group settingsComp = new Group(this.mainShell, SWT.BORDER);
            settingsComp.setText("General");
            settingsComp.setFont(groupFont);
            final RowLayout rl = new RowLayout(SWT.VERTICAL);
            rl.spacing = 10;
            rl.marginBottom = 10;
            settingsComp.setLayout(rl);
            final FormData scfd = new FormData();
            scfd.top = new FormAttachment(0, 3);
            scfd.left = new FormAttachment(0, 3);
            scfd.right = new FormAttachment(100, -3);
            settingsComp.setLayoutData(scfd);

            createCoordinateGroup(settingsComp);
            createTickerControls(settingsComp);
            createGridControls(settingsComp);
            createDictionaryComposite(settingsComp);
            createButtonComposite();

            this.mainShell.pack();

        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    private void createTickerControls(final Group settingsComp) {
        this.useTickerButton = new Button(settingsComp, SWT.CHECK);
        this.useTickerButton.setText("Scrolling Status Message");
    }

    private void createDictionaryComposite(final Group settingsComp) {
        this.dictComp = new DictionarySelectionComposite(missionProps, sseFlag, this.mainShell);
        final FormData fd = new FormData();
        fd.top = new FormAttachment(settingsComp);
        fd.left = new FormAttachment(0);
        this.dictComp.getComposite().setLayoutData(fd);
    }

    private void createCoordinateGroup(final Group settingsComp) {
        final Group coordinateGroup = new Group(settingsComp, SWT.NONE);
        coordinateGroup.setLayout(new GridLayout(2, false));

        createPixelControls(coordinateGroup);
        createCharacterControls(coordinateGroup);
    }

    private void createButtonComposite() {
        final Label line = new Label(this.mainShell, SWT.SEPARATOR
                | SWT.HORIZONTAL | SWT.SHADOW_ETCHED_IN);
        final FormData formData6 = new FormData();
        formData6.top = new FormAttachment(this.dictComp.getComposite());
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

                    if (!BuilderPreferencesShell.this.dictComp.checkFields()) {
                        SWTUtilities.showErrorDialog(
                                BuilderPreferencesShell.this.mainShell,
                                "Empty Fields",
                                "You must fill in all dictionary fields.");
                        return;
                    }
                    BuilderPreferencesShell.this.fswDictionaryDir = BuilderPreferencesShell.this.dictComp
                            .getFswDictionaryDir();
                    BuilderPreferencesShell.this.sseDictionaryDir = BuilderPreferencesShell.this.dictComp
                            .getSseDictionaryDir();
                    BuilderPreferencesShell.this.fswVersion = BuilderPreferencesShell.this.dictComp
                            .getFswVersion();
                    BuilderPreferencesShell.this.sseVersion = BuilderPreferencesShell.this.dictComp
                            .getSseVersion();
                    BuilderPreferencesShell.this.useTicker = BuilderPreferencesShell.this.useTickerButton
                            .getSelection();
                    BuilderPreferencesShell.this.useGrid = BuilderPreferencesShell.this.useGridButton
                            .getSelection();
                    BuilderPreferencesShell.this.gridSize = BuilderPreferencesShell.this.gridSizeSpinner
                            .getSelection();
                    BuilderPreferencesShell.this.gridColor = BuilderPreferencesShell.this.gridColorComp
                            .getCurrentColor();
                    if (BuilderPreferencesShell.this.usePixelsButton
                            .getSelection()) {
                        BuilderPreferencesShell.this.coordinateType = CoordinateSystemType.PIXEL;
                    } else {
                        BuilderPreferencesShell.this.coordinateType = CoordinateSystemType.CHARACTER;
                    }
                    BuilderPreferencesShell.this.cancelled = false;
                    BuilderPreferencesShell.this.buttonUsed = true;
                    BuilderPreferencesShell.this.mainShell.close();
                } catch (final Exception e1) {
                    e1.printStackTrace();
                }
            }
        });
        cancelButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(
                    final org.eclipse.swt.events.SelectionEvent e) {
                try {
                    BuilderPreferencesShell.this.cancelled = true;
                    BuilderPreferencesShell.this.buttonUsed = true;
                    BuilderPreferencesShell.this.mainShell.close();
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
                    if (BuilderPreferencesShell.this.buttonUsed) {
                        BuilderPreferencesShell.this.buttonUsed = false;
                        return;
                    }

                    BuilderPreferencesShell.this.cancelled = true;
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

    private void createGridControls(final Group settingsComp) {
        final Composite gridSizeComp = new Composite(settingsComp, SWT.NONE);
        gridSizeComp.setLayout(new FormLayout());

        this.useGridButton = new Button(gridSizeComp, SWT.CHECK);
        this.useGridButton.setText("Show Drawing Grid");
        final FormData gbfd = new FormData();
        gbfd.top = new FormAttachment(0, 11);
        gbfd.left = new FormAttachment(0);
        this.useGridButton.setLayoutData(gbfd);

        final Label spacingLabel = new Label(gridSizeComp, SWT.NONE);
        spacingLabel.setText("Grid Spacing: ");
        final FormData wlfd = new FormData();
        wlfd.top = new FormAttachment(0, 13);
        wlfd.left = new FormAttachment(this.useGridButton, 20);
        spacingLabel.setLayoutData(wlfd);

        this.gridSizeSpinner = new Spinner(gridSizeComp, SWT.NONE);
        this.gridSizeSpinner.setDigits(0);
        this.gridSizeSpinner.setIncrement(1);
        this.gridSizeSpinner.setMaximum(100);
        this.gridSizeSpinner.setMinimum(1);
        final FormData wsfd = new FormData();
        wsfd.top = new FormAttachment(spacingLabel, 0, SWT.CENTER);
        wsfd.left = new FormAttachment(spacingLabel, 0);
        this.gridSizeSpinner.setLayoutData(wsfd);

        this.unitsLabel = new Label(gridSizeComp, SWT.NONE);
        this.unitsLabel.setText(PIXEL_LABEL);
        final FormData wpfd = new FormData();
        wpfd.top = new FormAttachment(0, 13);
        wpfd.left = new FormAttachment(this.gridSizeSpinner, 0, 3);
        this.unitsLabel.setLayoutData(wpfd);

        this.gridColorComp = new ColorComposite(gridSizeComp, "Grid Color", TraceManager.getDefaultTracer());
        final FormData bfd = new FormData();
        bfd.left = new FormAttachment(this.unitsLabel, 10);
        bfd.top = new FormAttachment(0);
        this.gridColorComp.getComposite().setLayoutData(bfd);
        this.gridColorComp
                .setCurrentColor(new ChillColor(ColorName.LIGHT_GREY));
    }

    private void createCharacterControls(final Group coordinateGroup) {
        this.useCharactersButton = new Button(coordinateGroup, SWT.RADIO);
        this.useCharactersButton.setText("Use Character Coordinates");

        this.useCharactersButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(
                    final org.eclipse.swt.events.SelectionEvent e) {
                try {
                    if (BuilderPreferencesShell.this.useCharactersButton
                            .getSelection()) {
                        BuilderPreferencesShell.this.unitsLabel
                                .setText(CHARACTER_LABEL);
                        if (BuilderPreferencesShell.this.coordinateType
                                .equals(CoordinateSystemType.PIXEL)) {
                            BuilderPreferencesShell.this.coordinateType = CoordinateSystemType.CHARACTER;
                            final int newSize = BuilderPreferencesShell.this.gridSizeSpinner
                                    .getSelection()
                                    / BuilderPreferencesShell.this.charWidth;
                            BuilderPreferencesShell.this.gridSizeSpinner
                                    .setSelection(newSize);
                        }
                    }
                } catch (final Exception e1) {
                    e1.printStackTrace();
                }
            }
        });
    }

    private void createPixelControls(final Group coordinateGroup) {
        this.usePixelsButton = new Button(coordinateGroup, SWT.RADIO);
        this.usePixelsButton.setText("Use Pixel Coordinates");

        this.usePixelsButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(
                    final org.eclipse.swt.events.SelectionEvent e) {
                try {
                    if (BuilderPreferencesShell.this.usePixelsButton
                            .getSelection()) {
                        BuilderPreferencesShell.this.unitsLabel
                                .setText(PIXEL_LABEL);
                        if (BuilderPreferencesShell.this.coordinateType
                                .equals(CoordinateSystemType.CHARACTER)) {
                            BuilderPreferencesShell.this.coordinateType = CoordinateSystemType.PIXEL;
                            final int newSize = BuilderPreferencesShell.this.gridSizeSpinner
                                    .getSelection()
                                    * BuilderPreferencesShell.this.charWidth;
                            BuilderPreferencesShell.this.gridSizeSpinner
                                    .setSelection(newSize);
                        }
                    }
                } catch (final Exception e1) {
                    e1.printStackTrace();
                }
            }
        });
    }
}
