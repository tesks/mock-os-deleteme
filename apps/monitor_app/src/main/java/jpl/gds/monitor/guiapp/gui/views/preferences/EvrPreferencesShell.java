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

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.springframework.context.ApplicationContext;

import jpl.gds.monitor.config.MonitorDictionaryUtility;
import jpl.gds.monitor.guiapp.common.gui.DisplayConstants;
import jpl.gds.monitor.guiapp.common.gui.RealtimeRecordedComposite;
import jpl.gds.monitor.perspective.view.EvrViewConfiguration;
import jpl.gds.monitor.perspective.view.RealtimeRecordedFilterType;
import jpl.gds.perspective.ChillTable;
import jpl.gds.perspective.PromptSettings;
import jpl.gds.perspective.view.IViewConfiguration;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.swt.ColorComposite;
import jpl.gds.shared.swt.ConfirmationShell;
import jpl.gds.shared.swt.FontComposite;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.shared.swt.TitleComposite;
import jpl.gds.shared.swt.types.ChillColor;

/**
 * EvrPreferencesShell is the SWT GUI preferences window class for the message
 * monitor's EVR view.
 * 
 */
public class EvrPreferencesShell extends AbstractViewPreferences {

    /**
     * EVR list preferences window title
     */
    public static final String TITLE = "EVR List Preferences";
    
    private static final String ANY = "[Any]";
    
    private final Shell parent;
    private Text linesText;
    private ColorComposite markColorGetter;
    private final java.util.List<String> levels;
    private final java.util.List<String> modules;
    private String[] selectedLevels;
    private String[] selectedSources;
    private String[] selectedModules;
    private Button[] levelButtons;
    private Button[] sourceButtons;
    private Button sclkSubsecsButton;
    private RealtimeRecordedComposite rtRecComposite;
    private Button useColorCodingButton;

    private final String[] sources = new String[] {
            "FSW", "SSE"
    };
    private int maxRows;
    private ChillColor markColor;
    private boolean useSclkSubseconds;
    private TableComposite tableConfigurer;
    private boolean dirty;
    private boolean changeColumns;
    private TabFolder folder;
    private ModuleComposite moduleTabView;
    /*
     * Recorded/RT filter is now
     * an enum rather than a boolean.
     */
    private RealtimeRecordedFilterType rtRecFilterType;
    private boolean useColorCoding;

    private final Tracer                       trace;

    /**
     * Creates an instance of EvrPreferencesShell.
     * 
     * @param appContext
     *            The current application context
     * 
     * @param parent
     *            the parent display of this widget
     */
    public EvrPreferencesShell(final ApplicationContext appContext, final Shell parent) {
        super(appContext, TITLE);
        modules = appContext.getBean(MonitorDictionaryUtility.class).getEvrModules();
        levels = appContext.getBean(MonitorDictionaryUtility.class).getEvrLevels();
        levels.add(DisplayConstants.UNKNOWN_EVR_LEVEL);
        this.parent = parent;
        this.trace = TraceManager.getDefaultTracer(appContext);
        createControls();
        this.prefShell.setLocation(parent.getLocation().x + 100, parent.getLocation().y + 100);
    }
    
    /**
     * Creates all the preferences controls and composites.
     */
    protected void createControls() {
        this.prefShell = new Shell(this.parent, SWT.SHELL_TRIM
                | SWT.APPLICATION_MODAL);
        this.prefShell.setText(TITLE);
        FormLayout fl = new FormLayout();
        fl.marginHeight = 5;
        fl.marginWidth = 5;
        fl.spacing = 5;
        this.prefShell.setLayout(fl);

        this.folder = new TabFolder(this.prefShell, SWT.NONE);
        final TabItem generalTab = new TabItem(this.folder, SWT.NONE);
        final FormData folderData = new FormData();
        folderData.left = new FormAttachment(0);
        folderData.right = new FormAttachment(100);
        this.folder.setLayoutData(folderData);

        generalTab.setText("General");

        final Composite generalComposite = new Composite(this.folder, SWT.NONE);
        fl = new FormLayout();
        fl.marginHeight = 5;
        fl.marginWidth = 5;
        fl.spacing = 5;
        generalComposite.setLayout(fl);

        generalTab.setControl(generalComposite);

        this.titleText = new TitleComposite(generalComposite);
        final Composite titleComp = this.titleText.getComposite();
        final FormData fdLabel2 = new FormData();
        fdLabel2.top = new FormAttachment(0, 5);
        fdLabel2.left = new FormAttachment(0, 3);
        titleComp.setLayoutData(fdLabel2);

        final Composite fontComposite = new Composite(generalComposite, SWT.NONE);
        fontComposite.setLayout(new GridLayout(3, false));
        final FormData fontFd = new FormData();
        fontFd.top = new FormAttachment(titleComp);
        fontFd.left = new FormAttachment(0);
        fontFd.right = new FormAttachment(100);
        fontComposite.setLayoutData(fontFd);

        this.fontGetter = new FontComposite(fontComposite, "Data Font", trace);

        final Label linesLabel = new Label(fontComposite, SWT.NONE);
        linesLabel.setText("Maximum # List Entries:");
        this.linesText = new Text(fontComposite, SWT.SINGLE | SWT.BORDER);
        this.linesText.setText(String.valueOf(this.maxRows));
        final GridData fd7 = SWTUtilities.getGridData(this.linesText, 1, 10);
        this.linesText.setLayoutData(fd7);

        this.foreColorGetter = new ColorComposite(generalComposite,
                "Foreground Color", trace);
        final Composite foreColorComp = this.foreColorGetter.getComposite();
        final FormData foreColorFd = new FormData();
        foreColorFd.top = new FormAttachment(fontComposite, 0, 7);
        foreColorFd.left = new FormAttachment(0, 2);
        foreColorFd.right = new FormAttachment(100);
        foreColorComp.setLayoutData(foreColorFd);

        this.backColorGetter = new ColorComposite(generalComposite,
                "Background Color", trace);
        final Composite colorComp = this.backColorGetter.getComposite();
        final FormData colorFd = new FormData();
        colorFd.top = new FormAttachment(foreColorComp, 0, 7);
        colorFd.left = new FormAttachment(0, 2);
        colorFd.right = new FormAttachment(100);
        colorComp.setLayoutData(colorFd);
        
        this.markColorGetter = new ColorComposite(generalComposite, "Mark Color", trace);
        final Composite markColorComp = this.markColorGetter.getComposite();
        final FormData markColorFd = new FormData();
        markColorFd.top = new FormAttachment(colorComp, 0, 7);
        markColorFd.left = new FormAttachment(0, 2);
        markColorFd.right = new FormAttachment(100);
        markColorComp.setLayoutData(markColorFd);

        final TabItem levelTab = new TabItem(this.folder, SWT.NONE);
        levelTab.setText("Levels");

        final Composite levelComposite = new Composite(this.folder, SWT.NONE);
        final GridLayout gl = new GridLayout(3, false);
        gl.marginHeight = 5;
        gl.marginWidth = 5;
        levelComposite.setLayout(gl);

        levelTab.setControl(levelComposite);

        this.levelButtons = new Button[levels.size()];

        for (int i = 0; i < levels.size(); i++) {
            this.levelButtons[i] = new Button(levelComposite, SWT.CHECK);
            this.levelButtons[i].setText(levels.get(i));
            this.levelButtons[i].setSelection(isInitLevel(levels.get(i)));
        }

        new Label(levelComposite, SWT.NONE);
        new Label(levelComposite, SWT.NONE);
        new Label(levelComposite, SWT.NONE);
        
        this.useColorCodingButton = new Button(levelComposite, SWT.CHECK);
        this.useColorCodingButton.setText("Color Code EVRs by Level");
        
        if (!modules.isEmpty()) {
            final TabItem moduleTab = new TabItem(this.folder, SWT.NONE);
            moduleTab.setText("Modules");

            this.moduleTabView = new ModuleComposite();
            moduleTab.setControl(this.moduleTabView.moduleComp);
        }

        final TabItem tableTab = new TabItem(this.folder, SWT.NONE);
        tableTab.setText("Table");

        final Composite tableComposite = new Composite(this.folder, SWT.NONE);
        fl = new FormLayout();
        fl.marginHeight = 5;
        fl.marginWidth = 5;
        fl.spacing = 5;
        tableComposite.setLayout(fl);

        tableTab.setControl(tableComposite);

        this.sclkSubsecsButton = new Button(tableComposite, SWT.CHECK);
        this.sclkSubsecsButton.setText("Display Fine SCLK in Subseconds");
        final FormData fdSclk = new FormData();
        fdSclk.top = new FormAttachment(0, 5);
        fdSclk.left = new FormAttachment(0, 9);
        this.sclkSubsecsButton.setLayoutData(fdSclk);
        
        final Composite m = new Composite(tableComposite, SWT.NONE);
        final FormData fd = new FormData();
        fd.top = new FormAttachment(this.sclkSubsecsButton);
        fd.right = new FormAttachment(100);
        fd.left = new FormAttachment(0);
        m.setLayoutData(fd);
        final FillLayout layout = new FillLayout();
        layout.type = SWT.VERTICAL;
        m.setLayout(layout);
        
        this.tableConfigurer = new TableComposite(m, false);

        final TabItem sourceTab = new TabItem(this.folder, SWT.NONE);
        sourceTab.setText("Sources");

        final Composite sourceComposite = new Composite(this.folder, SWT.NONE);
        final RowLayout rl = new RowLayout(SWT.VERTICAL);
        rl.spacing = 10;
        sourceComposite.setLayout(rl);

        sourceTab.setControl(sourceComposite);


        this.rtRecComposite = new RealtimeRecordedComposite(sourceComposite);  

        final Composite sourceButtonComposite = new Composite(sourceComposite, SWT.NONE);
        sourceButtonComposite.setLayout(new RowLayout());

        this.sourceButtons = new Button[this.sources.length];

        for (int i = 0; i < this.sources.length; i++) {
            this.sourceButtons[i] = new Button(sourceButtonComposite, SWT.CHECK);
            this.sourceButtons[i].setText(this.sources[i]);
            this.sourceButtons[i].setSelection(isInitSource(this.sources[i]));
        }
        
        final Composite composite = new Composite(this.prefShell, SWT.NONE);
        final GridLayout grl = new GridLayout(2, true);
        composite.setLayout(grl);
        final FormData formData8 = new FormData();
        formData8.right = new FormAttachment(100);
        formData8.bottom = new FormAttachment(100);
        formData8.top = new FormAttachment(this.folder);
        composite.setLayoutData(formData8);

        final Button applyButton = new Button(composite, SWT.PUSH);
        applyButton.setText("Ok");
        this.prefShell.setDefaultButton(applyButton);
        final GridData gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        applyButton.setLayoutData(gd);
        final Button cancelButton = new Button(composite, SWT.PUSH);
        cancelButton.setText("Cancel");

        applyButton.addSelectionListener(new ApplySelectionListener());

        cancelButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
                try {
                    EvrPreferencesShell.this.canceled = true;
                    EvrPreferencesShell.this.prefShell.close();
                } catch (final Exception e1) {
                    e1.printStackTrace();
                }
            }
        });
    }

    private String[] getSelectedLevels() {
        return this.selectedLevels;
    }

    private void setSelectedLevels(final String[] newLevels) {
        this.selectedLevels = newLevels;
        if (this.levelButtons != null && !this.prefShell.isDisposed()) {
            for (int i = 0; i < levels.size(); i++) {
                this.levelButtons[i].setSelection(isInitLevel(levels.get(i)));
            }
        }
    }

    /**
     * Gets the EVR sources the user selected
     * 
     * @return the list of selected EVR sources as Strings
     */
    private String[] getSelectedSources() {
        return this.selectedSources;
    }
    
    private void setSelectedSources(final String[] newSources) {
        this.selectedSources = newSources;
        if (this.sourceButtons != null && !this.prefShell.isDisposed()) {
            for (int i = 0; i < this.sources.length; i++) {
                this.sourceButtons[i].setSelection(isInitSource(this.sources[i]));
            }
        }
    }

    private String[] getSelectedModules() {
        return this.selectedModules;
    }

    private void setSelectedModules(final String[] newModules) {
        this.selectedModules = newModules;
        if (this.moduleTabView != null && !this.prefShell.isDisposed()) {
            this.moduleTabView.setSelectedModules(newModules);
        }
    }

    private void setIsSclkSubseconds(final boolean enable) {
        this.useSclkSubseconds = enable;
        if (this.sclkSubsecsButton != null
                && !this.sclkSubsecsButton.isDisposed()) {
            this.sclkSubsecsButton.setSelection(enable);
        }
    }
    
    private void setUseColorCoding(final boolean enable) {
        this.useColorCoding = enable;
        if (this.useColorCodingButton != null
                && !this.useColorCodingButton.isDisposed()) {
            this.useColorCodingButton.setSelection(enable);
        }
    }


    private boolean isSclkSubseconds() {
        return this.useSclkSubseconds;
    }


    private boolean isUseColorCoding() {
        return this.useColorCoding;
    }
    
    /**
     * Determines if an EVR level string is on the list of levels the users has
     * already configured to be of interest
     * 
     * @param level the EVR level String
     * @return true if the user elected to view evrs of the given level in his
     *         configuration
     */
    private boolean isInitLevel(final String level) {
        if (this.selectedLevels == null) {
            return true;
        }
        for (int i = 0; i < this.selectedLevels.length; i++) {
            if (this.selectedLevels[i].equals(level)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines if an EVR source string is on the list of sources the users
     * has already configured to be of interest
     * 
     * @param level the EVR source String
     * @return true if the user elected to view evrs of the given source in his
     *         configuration
     */
    private boolean isInitSource(final String source) {
        if (this.selectedSources == null) {
            return true;
        }
        for (int i = 0; i < this.selectedSources.length; i++) {
            if (this.selectedSources[i].equals(source)) {
                return true;
            }
        }
        return false;
    }

    private int getMaxTableRows() {
        return this.maxRows;
    }

    private void setMaxTableRows(final int size) {
        this.maxRows = size;
        if (this.linesText != null && !this.linesText.isDisposed()) {
            this.linesText.setText(String.valueOf(size));
        }
    }
    
    /**
     * Sets the color the user has chosen for the EVR entry marks.
     * @param color the RGB to set for new ChillColor
     */    
    private void setMarkColor(final String color) {
        this.markColor = new ChillColor(color);
        if (this.markColorGetter != null && !this.markColorGetter.getComposite().isDisposed()) {
            this.markColorGetter.setCurrentColor(this.markColor);
        }
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.monitor.guiapp.gui.views.preferences.AbstractViewPreferences#setValuesFromViewConfiguration(jpl.gds.perspective.view.ViewConfiguration)
     */
    @Override
    public void setValuesFromViewConfiguration(final IViewConfiguration config) {
        super.setValuesFromViewConfiguration(config);
        this.dirty = false;
        this.changeColumns = false;
        final EvrViewConfiguration evrConfig = (EvrViewConfiguration) config;
        setMaxTableRows(evrConfig.getMaxRows());
        setMarkColor(evrConfig.getMarkColor());
        setSelectedSources(evrConfig.getSources());
        setSelectedLevels(evrConfig.getLevels());
        setSelectedModules(evrConfig.getModules());
        setIsSclkSubseconds(evrConfig.isSclkSubseconds());
        /*
         * RT/Recorded flag is now an enum
         * rather than a boolean. 
         * 
         * Completed implementation of rt/recorded enum.
         */
        this.rtRecComposite.setRealtimeRecordedFilterType(evrConfig.getRealtimeRecordedFilterType());

        setUseColorCoding(evrConfig.isUseColorCoding());

        final ChillTable evrTable = evrConfig.getTable(EvrViewConfiguration.EVR_TABLE_NAME);
        if (this.tableConfigurer != null
                && !this.tableConfigurer.getComposite().isDisposed()) {
            this.tableConfigurer.init(evrTable);
            this.prefShell.layout();
            this.prefShell.pack();
        }
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.monitor.guiapp.gui.views.preferences.AbstractViewPreferences#getValuesIntoViewConfiguration(jpl.gds.perspective.view.ViewConfiguration)
     */
    @Override
    public void getValuesIntoViewConfiguration(final IViewConfiguration config) {
        super.getValuesIntoViewConfiguration(config);
        final EvrViewConfiguration evrConfig = (EvrViewConfiguration) config;
        evrConfig.setMaxRows(getMaxTableRows());
        evrConfig.setMarkColor(getMarkColor());
        evrConfig.setSources(getSelectedSources());
        evrConfig.setLevels(getSelectedLevels());
        evrConfig.setModules(getSelectedModules());
        evrConfig.setSclkIsSubseconds(isSclkSubseconds());
        /*
         * RT/Recorded flag  now an enum
         * rather than a boolean.
         */
        evrConfig.setRealtimeRecordedFilterType(this.rtRecFilterType);
        evrConfig.setUseColorCoding(isUseColorCoding());

        if (this.tableConfigurer != null) {
            evrConfig.setTable(this.tableConfigurer.getTable());
        }
    }

    private String getMarkColor() {
		return this.markColor.getRed() + "," + this.markColor.getGreen() + "," + this.markColor.getBlue();
	}

	/**
     * Indicates whether the user has elected to change the columns in the EVR
     * table.
     * 
     * @return true if column change required
     */
    public boolean needColumnChange() {
        return this.changeColumns;
    }

    /**
     * Module composite used for filtering which EVR modules to display
     *
     */
    private class ModuleComposite {

        protected Composite moduleComp;
        private final List availModules;
        private final List selectedModules;

        public ModuleComposite() {
            this.moduleComp = new Composite(EvrPreferencesShell.this.folder, SWT.NONE);
            final FormLayout gl = new FormLayout();
            gl.marginHeight = 5;
            gl.marginWidth = 5;
            this.moduleComp.setLayout(gl);

            final Label availableLabel = new Label(this.moduleComp, SWT.NONE);
            availableLabel.setText("Defined Modules:");
            final FormData aLabelFd = new FormData();
            aLabelFd.top = new FormAttachment(0);
            aLabelFd.left = new FormAttachment(0);
            availableLabel.setLayoutData(aLabelFd);

            this.availModules = new List(this.moduleComp, SWT.MULTI
                    | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
            final int height = 7 * this.availModules.getItemHeight();
            GC gc = new GC(this.availModules);
            final FontMetrics fm = gc.getFontMetrics();
            final int width = 25 * fm.getAverageCharWidth();
            gc.dispose();
            gc = null;
            FormData resultFd = new FormData(width, height);
            resultFd.top = new FormAttachment(availableLabel);
            resultFd.left = new FormAttachment(0);
            resultFd.bottom = new FormAttachment(98);
            resultFd.right = new FormAttachment(35);
            this.availModules.setLayoutData(resultFd);

            final Label selectedLabel = new Label(this.moduleComp, SWT.NONE);
            selectedLabel.setText("Selected Modules:");
            final FormData sLabelFd = new FormData();
            sLabelFd.top = new FormAttachment(0);
            sLabelFd.left = new FormAttachment(65);
            selectedLabel.setLayoutData(sLabelFd);

            this.selectedModules = new List(this.moduleComp, SWT.MULTI
                    | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);

            resultFd = new FormData(width, height);
            resultFd.top = new FormAttachment(selectedLabel);
            resultFd.left = new FormAttachment(65);
            resultFd.bottom = new FormAttachment(98);
            resultFd.right = new FormAttachment(100);
            this.selectedModules.setLayoutData(resultFd);

            for (int i = 0; i < modules.size(); i++) {
                this.availModules.add(modules.get(i));
            }

            final Button addButton = new Button(this.moduleComp, SWT.PUSH);
            addButton.setText("Add ->");
            final FormData leftFd = new FormData();
            leftFd.top = new FormAttachment(30);
            leftFd.left = new FormAttachment(37);
            leftFd.right = new FormAttachment(63);
            addButton.setLayoutData(leftFd);
            addButton.setEnabled(false);

            final Button removeButton = new Button(this.moduleComp, SWT.PUSH);
            removeButton.setText("Remove");
            final FormData removeFd = new FormData();
            removeFd.top = new FormAttachment(addButton, 15, 15);
            removeFd.left = new FormAttachment(37);
            removeFd.right = new FormAttachment(63);
            removeButton.setLayoutData(removeFd);
            removeButton.setEnabled(false);

            this.availModules.addSelectionListener(new SelectionAdapter() {

            	/**
                 * {@inheritDoc}
                 * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
                 */
                @Override
                public void widgetSelected(
                        final org.eclipse.swt.events.SelectionEvent e) {
                    try {
                        addButton
                                .setEnabled(ModuleComposite.this.availModules.getSelectionIndex() != -1);
                    } catch (final Exception e1) {
                        e1.printStackTrace();
                    }
                }
            });

            this.selectedModules.addSelectionListener(new SelectionAdapter() {

                
            	/**
                 * {@inheritDoc}
                 * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
                 */
                @Override
                public void widgetSelected(
                        final org.eclipse.swt.events.SelectionEvent e) {
                    try {
                        final int index = ModuleComposite.this.selectedModules.getSelectionIndex();
                        if (index == -1) {
                            removeButton.setEnabled(false);
                        } else {
                            if (ModuleComposite.this.selectedModules.getSelection()[0].equals(ANY)) {
                                removeButton.setEnabled(false);
                            } else {
                                removeButton.setEnabled(true);
                            }
                        }
                    } catch (final Exception e1) {
                        e1.printStackTrace();
                    }
                }
            });

            addButton.addSelectionListener(new SelectionAdapter() {

            	/**
                 * {@inheritDoc}
                 * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
                 */
                @Override
                @SuppressWarnings("PMD.UseArraysAsList")
                public void widgetSelected(
                        final org.eclipse.swt.events.SelectionEvent e) {
                    try {
                        if (ModuleComposite.this.selectedModules.getItemCount() == 1
                                && ModuleComposite.this.selectedModules.getItem(0).equals(ANY)) {
                            ModuleComposite.this.selectedModules.remove(0);
                        }
                        final String[] newItems = ModuleComposite.this.availModules.getSelection();
                        String[] selectedItems = ModuleComposite.this.selectedModules.getItems();
                        /*
                         * PMD SUPRESSION.
                         * PMD wants me to use Arrays.asList() here but that
                         * creates a list I cannot modify below.
                         */
                        final ArrayList<String> newList = new ArrayList<String>();
                        for (int i = 0; i < selectedItems.length; i++) {
                            newList.add(selectedItems[i]);
                        }
                        for (int i = 0; i < newItems.length; i++) {
                            if (!newList.contains(newItems[i])) {
                                newList.add(newItems[i]);
                            }
                        }
                        selectedItems = newList.toArray(selectedItems);
                        Arrays.sort(selectedItems);

                        ModuleComposite.this.selectedModules.removeAll();
                        for (int i = 0; i < selectedItems.length; i++) {
                            ModuleComposite.this.selectedModules.add(selectedItems[i]);
                        }
                    } catch (final Exception e1) {
                        e1.printStackTrace();
                    }

                }
            });

            removeButton.addSelectionListener(new SelectionAdapter() {

            	/**
                 * {@inheritDoc}
                 * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
                 */
                @Override
                public void widgetSelected(
                        final org.eclipse.swt.events.SelectionEvent e) {
                    try {
                        final String[] removeItems = ModuleComposite.this.selectedModules.getSelection();
                        final String[] selectedItems = ModuleComposite.this.selectedModules.getItems();
                        final String[] result = new String[selectedItems.length
                                - removeItems.length];
                        ModuleComposite.this.selectedModules.removeAll();
                        if (result.length == 0) {
                            ModuleComposite.this.selectedModules.add(ANY);
                            return;
                        }
                        final java.util.List<String> removeList = Arrays
                                .asList(removeItems);
                        int index = 0;
                        for (int i = 0; i < selectedItems.length; i++) {
                            if (!removeList.contains(selectedItems[i])) {
                                result[index++] = selectedItems[i];
                            }
                        }
                        for (int i = 0; i < result.length; i++) {
                            ModuleComposite.this.selectedModules.add(result[i]);
                        }
                    } catch (final Exception e1) {
                        e1.printStackTrace();
                    }
                }
            });
        }

        public void setSelectedModules(final String[] selected) {
            this.selectedModules.removeAll();
            if (selected == null || selected.length == 0) {
                this.selectedModules.add(ANY);
            } else {
                for (int i = 0; i < selected.length; i++) {
                    this.selectedModules.add(selected[i]);
                }
            }
        }

        public String[] getSelectedModules() {
            final String[] result = this.selectedModules.getItems();
            if (result.length == 1 && result[0].equals(ANY)) {
                return null;
            } else {
                return result;
            }
        }
    }

    /**
     * Selection Listener class for the APPLY button.
     */
    private class ApplySelectionListener extends SelectionAdapter {
        /**
         * {@inheritDoc}
         * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
         */
        @Override
        @SuppressWarnings("PMD.IdempotentOperations")
        public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
            try {

                /*
                 * Validate user entries first.
                 */
                int lines = 0;
                try {
                    lines = Integer.parseInt(EvrPreferencesShell.this.linesText.getText().trim());
                    if (lines < 1) {
                        SWTUtilities
                        .showMessageDialog(EvrPreferencesShell.this.prefShell, "Invalid Number",
                                "The maximum number of list entries must be greater than 0.");
                        return;
                    }

                } catch (final NumberFormatException ex) {
                    SWTUtilities
                    .showMessageDialog(EvrPreferencesShell.this.prefShell, "Invalid Number",
                            "You must enter the maximum number of list entries as an integer.");
                    return;
                }

                if (tableConfigurer.getTable().getActualColumnCount() == 0) {
                    SWTUtilities.showMessageDialog(EvrPreferencesShell.this.prefShell, "No Columns",
                            "You cannot disable all the table columns, or the table will be blank.");
                    return;
                }

                /*
                 * Prompt for confirmation if it's not turned off.
                 */
                final PromptSettings settings = new PromptSettings();
                if (settings.showMonitorEvrPreferencesConfirmation()) {
                    final ConfirmationShell confirmShell = new ConfirmationShell(prefShell,
                            "Changing EVR preferences will clear all EVRs currently displayed or already queued\n" +
                                    "for display. Are you sure you want to update the preferences?",
                                                                                 true);
                    confirmShell.open();
                    while (!confirmShell.getShell().isDisposed()) {
                        if (!confirmShell.getShell().getDisplay().readAndDispatch())
                        {
                            confirmShell.getShell().getDisplay().sleep();
                        }
                    }

                    /*
                     * Save the state of the "prompt again" flag in the confirmation
                     * window for next time.
                     */
                    final boolean promptAgain = confirmShell.getPromptAgain();
                    if (!promptAgain) {
                        settings.setMonitorEvrPreferencesConfirmation(false);
                        settings.save();
                    }

                    /*
                     * User did not confirm changes, so go no further.
                     */
                    if (confirmShell.wasCanceled()) {
                        return;
                    }
                }

                /*
                 * User confirmed changes.
                 */
                applyChanges();
                /*
                 * PMD SUPRESSION.
                 * PMD falsely flags assignments to members in the enclosing class as "idempotent".
                 */
                EvrPreferencesShell.this.dirty = EvrPreferencesShell.this.tableConfigurer.getDirty();
                if (EvrPreferencesShell.this.dirty) {
                    EvrPreferencesShell.this.changeColumns = true;
                }
                EvrPreferencesShell.this.maxRows = lines;
                EvrPreferencesShell.this.markColor = EvrPreferencesShell.this.markColorGetter.getCurrentColor();
                int selectedCount = 0;
                for (int i = 0; i < EvrPreferencesShell.this.levelButtons.length; i++) {
                    if (EvrPreferencesShell.this.levelButtons[i].getSelection()) {
                        selectedCount++;
                    }
                }
                EvrPreferencesShell.this.selectedLevels = new String[selectedCount];

                if (selectedCount == 0) {
                    EvrPreferencesShell.this.selectedLevels = null;
                } else {

                    int k = 0;
                    for (int i = 0; i < EvrPreferencesShell.this.levelButtons.length; i++) {
                        if (EvrPreferencesShell.this.levelButtons[i].getSelection()) {
                            EvrPreferencesShell.this.selectedLevels[k++] = levels.get(i);
                        }
                    }
                }

                selectedCount = 0;

                for (int i = 0; i < EvrPreferencesShell.this.sourceButtons.length; i++) {
                    if (EvrPreferencesShell.this.sourceButtons[i].getSelection()) {
                        selectedCount++;
                    }
                }
                if (selectedCount == 0) {
                    EvrPreferencesShell.this.selectedSources = null;
                } else {
                    EvrPreferencesShell.this.selectedSources = new String[selectedCount];

                    int k = 0;
                    for (int i = 0; i < EvrPreferencesShell.this.sourceButtons.length; i++) {
                        if (EvrPreferencesShell.this.sourceButtons[i].getSelection()) {
                            EvrPreferencesShell.this.selectedSources[k++] = EvrPreferencesShell.this.sources[i];
                        }
                    }
                }

                if (EvrPreferencesShell.this.moduleTabView != null) {
                    EvrPreferencesShell.this.selectedModules = EvrPreferencesShell.this.moduleTabView.getSelectedModules();
                }

                EvrPreferencesShell.this.useSclkSubseconds = EvrPreferencesShell.this.sclkSubsecsButton.getSelection();

                /*
                 * RT/recorded filter is now an enum rather than a boolean
                 * and we use a common composite.
                 */
                EvrPreferencesShell.this.rtRecFilterType = EvrPreferencesShell.this.rtRecComposite.getRealtimeRecordedFilterType();
                EvrPreferencesShell.this.useColorCoding = EvrPreferencesShell.this.useColorCodingButton.getSelection();

                EvrPreferencesShell.this.prefShell.close();

            } catch (final Exception ex) {
                ex.printStackTrace();
            } 
        }
    }
}
