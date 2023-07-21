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
import java.util.LinkedList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
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
import jpl.gds.monitor.perspective.view.NatEvrViewConfiguration;
import jpl.gds.monitor.perspective.view.RealtimeRecordedFilterType;
import jpl.gds.perspective.view.IViewConfiguration;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.swt.ColorComposite;
import jpl.gds.shared.swt.FontComposite;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.shared.swt.TitleComposite;
import jpl.gds.shared.swt.types.ChillColor;

/**
 * NatEvrPreferencesShell is the SWT GUI preferences window class for the
 * message monitor's NAT EVR view.
 * 
 */
public class NatEvrPreferencesShell extends AbstractViewPreferences {

    /**
     * EVR list preferences window title
     */
    public static final String TITLE = "EVR List Preferences";

    private static final String ANY = "[Any]";

    private final Shell parent;
    private ColorComposite markColorGetter;
    private final java.util.List<String> levels;
    private final java.util.List<String> modules;
    private final java.util.List<String> sources = Arrays.asList("FSW", "SSE");
    private java.util.List<String> selectedLevels = new LinkedList<String>();
    private java.util.List<String> selectedSources = new LinkedList<String>();
    private java.util.List<String> selectedModules = new LinkedList<String>();
    private Button[] sourceButtons;
    private RealtimeRecordedComposite rtRecComposite;
    private Button useColorCodingButton;
    private ChillColor markColor;
    private TabFolder folder;
    private ListChooserComposite moduleChooser;
    private ListChooserComposite levelChooser;
    private RealtimeRecordedFilterType rtRecFilterType;
    private boolean useColorCoding;
    private Text maxRowsText;
    private int maxRows;
    private final Tracer                 trace;
    
    /**
     * Creates an instance of NatEvrPreferencesShell.
     * 
     * @param appContext
     *            The current ApplicationContext
     * 
     * @param parent
     *            the parent display of this widget
     */
    public NatEvrPreferencesShell(final ApplicationContext appContext, final Shell parent) {
        super(appContext, TITLE);
        this.parent = parent;
        modules = appContext.getBean(MonitorDictionaryUtility.class).getEvrModules();
        levels = appContext.getBean(MonitorDictionaryUtility.class).getEvrLevels();
        levels.add(DisplayConstants.UNKNOWN_EVR_LEVEL);
        trace = TraceManager.getDefaultTracer(appContext);
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

        this.foreColorGetter = new ColorComposite(generalComposite,
                "Foreground Color", TraceManager.getDefaultTracer());
        final Composite foreColorComp = this.foreColorGetter.getComposite();
        final FormData foreColorFd = new FormData();
        foreColorFd.top = new FormAttachment(fontComposite, 0, 7);
        foreColorFd.left = new FormAttachment(0, 2);
        foreColorFd.right = new FormAttachment(100);
        foreColorComp.setLayoutData(foreColorFd);

        this.backColorGetter = new ColorComposite(generalComposite,
                "Background Color", TraceManager.getDefaultTracer());
        final Composite colorComp = this.backColorGetter.getComposite();
        final FormData colorFd = new FormData();
        colorFd.top = new FormAttachment(foreColorComp, 0, 7);
        colorFd.left = new FormAttachment(0, 2);
        colorFd.right = new FormAttachment(100);
        colorComp.setLayoutData(colorFd);

        this.markColorGetter = new ColorComposite(generalComposite,
                "Mark Color", TraceManager.getDefaultTracer());
        final Composite markColorComp = this.markColorGetter.getComposite();
        final FormData markColorFd = new FormData();
        markColorFd.top = new FormAttachment(colorComp, 0, 7);
        markColorFd.left = new FormAttachment(0, 2);
        markColorFd.right = new FormAttachment(100);
        markColorComp.setLayoutData(markColorFd);

        final Label maxRowsLabel = new Label(generalComposite, SWT.NONE);
        maxRowsLabel.setText("Maximum # List Entries:");
        final FormData maxRowsFd2 = new FormData();
        maxRowsFd2.top = new FormAttachment(markColorComp, 15);
        maxRowsFd2.left = new FormAttachment(0, 4);
        maxRowsLabel.setLayoutData(maxRowsFd2);
        this.maxRowsText = new Text(generalComposite, SWT.SINGLE);
        this.maxRowsText.setText(String.valueOf(this.maxRows));
        final FormData fd2 = SWTUtilities.getFormData(maxRowsText, 1, 10);
        fd2.left = new FormAttachment(maxRowsLabel);
        fd2.top = new FormAttachment(maxRowsLabel, 0, SWT.CENTER);
        maxRowsText.setLayoutData(fd2);

        final TabItem levelTab = new TabItem(this.folder, SWT.NONE);
        levelTab.setText("Levels");

        final Composite levelComposite = new Composite(this.folder, SWT.NONE);
        final GridLayout gl = new GridLayout(1, false);
        gl.marginHeight = 5;
        gl.marginWidth = 5;
        levelComposite.setLayout(gl);

        levelTab.setControl(levelComposite);
        this.levelChooser = new ListChooserComposite("Levels", this.levels, levelComposite);

        this.useColorCodingButton = new Button(levelComposite, SWT.CHECK);
        this.useColorCodingButton.setText("Color Code EVRs by Level");

        if (modules.size() != 0) {
            final TabItem moduleTab = new TabItem(this.folder, SWT.NONE);
            moduleTab.setText("Modules");

            this.moduleChooser = new ListChooserComposite("Modules", this.modules, this.folder);
            moduleTab.setControl(this.moduleChooser.getComposite());
        }

        final TabItem sourceTab = new TabItem(this.folder, SWT.NONE);
        sourceTab.setText("Sources");

        final Composite sourceComposite = new Composite(this.folder, SWT.NONE);
        final RowLayout rl = new RowLayout(SWT.VERTICAL);
        rl.spacing = 10;
        sourceComposite.setLayout(rl);

        sourceTab.setControl(sourceComposite);

        this.rtRecComposite = new RealtimeRecordedComposite(sourceComposite, "Both");  

        final Composite sourceButtonComposite = new Composite(sourceComposite, SWT.NONE);
        sourceButtonComposite.setLayout(new RowLayout());

        this.sourceButtons = new Button[this.sources.size()];

        for (int i = 0; i < this.sources.size(); i++) {
            this.sourceButtons[i] = new Button(sourceButtonComposite, SWT.CHECK);
            this.sourceButtons[i].setText(this.sources.get(i));
            this.sourceButtons[i].setSelection(false);
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
                    NatEvrPreferencesShell.this.canceled = true;
                    NatEvrPreferencesShell.this.prefShell.close();
                } catch (final Exception e1) {
                    e1.printStackTrace();
                }
            }
        });
    }

    /**
     * Sets the currently selected EVR levels.
     * 
     * @param newLevels levels to set
     */
    private void setSelectedLevels(final java.util.List<String> newLevels) {
        this.selectedLevels = newLevels;
        if (this.levelChooser != null && !this.prefShell.isDisposed()) {
            this.levelChooser.setSelectedItems(newLevels);
        }
    }

    /**
     * Sets the currently selected EVR sources.
     * 
     * @param newSources sources to set
     */
    private void setSelectedSources(final java.util.List<String> newSources) {
        this.selectedSources = newSources;
        if (this.sourceButtons != null && !this.prefShell.isDisposed()) {
            for (int i = 0; i < this.sources.size(); i++) {
                this.sourceButtons[i].setSelection(selectedSources.isEmpty() || selectedSources.contains(this.sources.get(i)));
            }
        }
    }

    /**
     * Sets the currently selected EVR modules.
     * 
     * @param newModules modules to set
     */
    private void setSelectedModules(final java.util.List<String> newModules) {
        this.selectedModules = newModules;
        if (this.moduleChooser != null && !this.prefShell.isDisposed()) {
            this.moduleChooser.setSelectedItems(newModules);
        }
    }

    /**
     * Sets the current use EVR color coding flag.
     * @param enable true to enable color coding, false to disable
     */
    private void setUseColorCoding(final boolean enable) {
        this.useColorCoding = enable;
        if (this.useColorCodingButton != null
                && !this.useColorCodingButton.isDisposed()) {
            this.useColorCodingButton.setSelection(enable);
        }
    }
    
    /**
     * Sets the color the user has chosen for the EVR entry marks.
     * @param color the RGB string to set for new ChillColor
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
        final NatEvrViewConfiguration evrConfig = (NatEvrViewConfiguration) config;
        setMarkColor(evrConfig.getMarkColor());
        setSelectedSources(evrConfig.getSources());
        setSelectedLevels(evrConfig.getLevels());
        setSelectedModules(evrConfig.getModules());
        this.rtRecComposite.setRealtimeRecordedFilterType(evrConfig.getRealtimeRecordedFilterType());
        setUseColorCoding(evrConfig.isUseColorCoding());
        maxRows = evrConfig.getMaxRows();
        maxRowsText.setText(String.valueOf(maxRows));

        this.prefShell.layout();
        this.prefShell.pack();

    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.monitor.guiapp.gui.views.preferences.AbstractViewPreferences#getValuesIntoViewConfiguration(jpl.gds.perspective.view.ViewConfiguration)
     */
    @Override
    public void getValuesIntoViewConfiguration(final IViewConfiguration config) {
        super.getValuesIntoViewConfiguration(config);
        final NatEvrViewConfiguration evrConfig = (NatEvrViewConfiguration) config;
        evrConfig.setMarkColor(getMarkColor());
        evrConfig.setSources(this.selectedSources);
        evrConfig.setLevels(this.selectedLevels);
        evrConfig.setModules(this.selectedModules);
        evrConfig.setRealtimeRecordedFilterType(this.rtRecFilterType);
        evrConfig.setUseColorCoding(this.useColorCoding);
        evrConfig.setMaxRows(this.maxRows);
    }

    /**
     * Convenience method to create an RGB string from the current mark color.
     * 
     * @return RGB string
     */
    private String getMarkColor() {
        return this.markColor.getRed() + "," + this.markColor.getGreen() + "," + this.markColor.getBlue();
    }

    /**
     * A general composite for selecting/de-selecting string values from a list of available string
     * values, in the form of two lists (available and selected), between which items can be moved.
     *
     */
    private class ListChooserComposite {

        private Composite itemComp;
        private final List availItems;
        private final List selectedItems;

        /**
         * Constructor.
         * 
         * @param itemType the type of items being selected, used for label text
         * @param items the list of available items
         * @param parent the parent composite
         */
        public ListChooserComposite(final String itemType, final java.util.List<String> items, final Composite parent) {
            this.itemComp = new Composite(parent, SWT.NONE);
            final FormLayout gl = new FormLayout();
            gl.marginHeight = 5;
            gl.marginWidth = 5;
            this.itemComp.setLayout(gl);

            final Label availableLabel = new Label(this.itemComp, SWT.NONE);
            availableLabel.setText("Defined " + itemType + ":");
            final FormData aLabelFd = new FormData();
            aLabelFd.top = new FormAttachment(0);
            aLabelFd.left = new FormAttachment(0);
            availableLabel.setLayoutData(aLabelFd);

            this.availItems = new List(this.itemComp, SWT.MULTI
                    | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
            final int height = 7 * this.availItems.getItemHeight();
            GC gc = new GC(this.availItems);
            final FontMetrics fm = gc.getFontMetrics();
            final int width = 25 * fm.getAverageCharWidth();
            gc.dispose();
            gc = null;
            FormData resultFd = new FormData(width, height);
            resultFd.top = new FormAttachment(availableLabel);
            resultFd.left = new FormAttachment(0);
            resultFd.bottom = new FormAttachment(98);
            resultFd.right = new FormAttachment(35);
            this.availItems.setLayoutData(resultFd);

            final Label selectedLabel = new Label(this.itemComp, SWT.NONE);
            selectedLabel.setText("Selected " + itemType + ":");
            final FormData sLabelFd = new FormData();
            sLabelFd.top = new FormAttachment(0);
            sLabelFd.left = new FormAttachment(65);
            selectedLabel.setLayoutData(sLabelFd);

            this.selectedItems = new List(this.itemComp, SWT.MULTI
                    | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);

            resultFd = new FormData(width, height);
            resultFd.top = new FormAttachment(selectedLabel);
            resultFd.left = new FormAttachment(65);
            resultFd.bottom = new FormAttachment(98);
            resultFd.right = new FormAttachment(100);
            this.selectedItems.setLayoutData(resultFd);

            for (final String item: items) {
                this.availItems.add(item);
            }

            final Button addButton = new Button(this.itemComp, SWT.PUSH);
            addButton.setText("Add ->");
            final FormData leftFd = new FormData();
            leftFd.top = new FormAttachment(30);
            leftFd.left = new FormAttachment(37);
            leftFd.right = new FormAttachment(63);
            addButton.setLayoutData(leftFd);
            addButton.setEnabled(false);

            final Button removeButton = new Button(this.itemComp, SWT.PUSH);
            removeButton.setText("Remove");
            final FormData removeFd = new FormData();
            removeFd.top = new FormAttachment(addButton, 15, 15);
            removeFd.left = new FormAttachment(37);
            removeFd.right = new FormAttachment(63);
            removeButton.setLayoutData(removeFd);
            removeButton.setEnabled(false);

            this.availItems.addSelectionListener(new SelectionAdapter() {

                /**
                 * {@inheritDoc}
                 * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
                 */
                @Override
                public void widgetSelected(
                        final org.eclipse.swt.events.SelectionEvent e) {
                    try {
                        addButton
                        .setEnabled(ListChooserComposite.this.availItems.getSelectionIndex() != -1);
                    } catch (final Exception e1) {
                        e1.printStackTrace();
                    }
                }
            });

            this.selectedItems.addSelectionListener(new SelectionAdapter() {


                /**
                 * {@inheritDoc}
                 * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
                 */
                @Override
                public void widgetSelected(
                        final org.eclipse.swt.events.SelectionEvent e) {
                    try {
                        final int index = ListChooserComposite.this.selectedItems.getSelectionIndex();
                        if (index == -1) {
                            removeButton.setEnabled(false);
                        } else {
                            if (ListChooserComposite.this.selectedItems.getSelection()[0].equals(ANY)) {
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
                public void widgetSelected(
                        final org.eclipse.swt.events.SelectionEvent e) {
                    try {
                        if (ListChooserComposite.this.selectedItems.getItemCount() == 1
                                && ListChooserComposite.this.selectedItems.getItem(0).equals(ANY)) {
                            ListChooserComposite.this.selectedItems.remove(0);
                        }
                        final String[] newItems = ListChooserComposite.this.availItems.getSelection();
                        String[] selectedItems = ListChooserComposite.this.selectedItems.getItems();

                        final java.util.List<String> newList = new ArrayList<String>();
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

                        ListChooserComposite.this.selectedItems.removeAll();
                        for (int i = 0; i < selectedItems.length; i++) {
                            ListChooserComposite.this.selectedItems.add(selectedItems[i]);
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
                        final String[] removeItems = ListChooserComposite.this.selectedItems.getSelection();
                        final String[] selectedItems = ListChooserComposite.this.selectedItems.getItems();
                        final String[] result = new String[selectedItems.length
                                                     - removeItems.length];
                        ListChooserComposite.this.selectedItems.removeAll();
                        if (result.length == 0) {
                            ListChooserComposite.this.selectedItems.add(ANY);
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
                            ListChooserComposite.this.selectedItems.add(result[i]);
                        }
                    } catch (final Exception e1) {
                        e1.printStackTrace();
                    }
                }
            });
        }

        /**
         * Sets the array of currently selected items.
         * 
         * @param selected selected item strings
         */
        public void setSelectedItems(final java.util.List<String> selected) {
            this.selectedItems.removeAll();
            if (selected == null || selected.size() == 0) {
                this.selectedItems.add(ANY);
            } else {
                for (final String isSelected: selected) {
                    this.selectedItems.add(isSelected);
                }
            }
        }

        /**
         * Gets an array of currently selected items.
         * 
         * @return selected item strings
         */
        public java.util.List<String> getSelectedItems() {
           
            final String[] result = this.selectedItems.getItems();
            if (result.length == 1 && result[0].equals(ANY) || result.length == availItems.getItemCount()) {
                return null;
            } else {
                return Arrays.asList(result);
            }
        }
        
        /**
         * Gets the underlying composite object.
         * 
         * @return Composite
         */
        public Composite getComposite() {
            return this.itemComp;
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
        public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
            try {

                try {
                    final int lines = Integer.parseInt(maxRowsText.getText().trim());
                    if (lines < 1) {
                        SWTUtilities.showMessageDialog(NatEvrPreferencesShell.this.prefShell, "Invalid Number",
                                                       "The maximum number of list entries must be greater than 0.");
                        return;
                    }
                    maxRows = lines;
                }
                catch (final NumberFormatException ex) {
                    SWTUtilities.showMessageDialog(NatEvrPreferencesShell.this.prefShell, "Invalid Number",
                                                   "You must enter the maximum number of list entries as an integer.");
                    return;
                }

                /* Applies changes from the super class to the view configuration */
                applyChanges();

                /* Update EVR-specific view configuration from current settings */

                NatEvrPreferencesShell.this.markColor = NatEvrPreferencesShell.this.markColorGetter.getCurrentColor();
                
                if (NatEvrPreferencesShell.this.levelChooser != null) {
                    NatEvrPreferencesShell.this.selectedLevels = NatEvrPreferencesShell.this.levelChooser.getSelectedItems();
                }
                
                selectedSources = new LinkedList<String>();

                for (int i = 0; i < NatEvrPreferencesShell.this.sourceButtons.length; i++) {
                    if (NatEvrPreferencesShell.this.sourceButtons[i].getSelection()) {
                        selectedSources.add(sources.get(i));
                    } 
                }
                
                /* If all sources are selected, we actually want to set the sources list to
                 * be empty.
                 */
                if (selectedSources.size() == sources.size()) {
                    selectedSources.clear();
                }
                
                if (NatEvrPreferencesShell.this.moduleChooser != null) {
                    NatEvrPreferencesShell.this.selectedModules = NatEvrPreferencesShell.this.moduleChooser.getSelectedItems();
                }

                NatEvrPreferencesShell.this.rtRecFilterType = NatEvrPreferencesShell.this.rtRecComposite.getRealtimeRecordedFilterType();
                NatEvrPreferencesShell.this.useColorCoding = NatEvrPreferencesShell.this.useColorCodingButton.getSelection();

                NatEvrPreferencesShell.this.prefShell.close();

            } catch (final Exception ex) {
                ex.printStackTrace();
            } 
        }
    }
}
