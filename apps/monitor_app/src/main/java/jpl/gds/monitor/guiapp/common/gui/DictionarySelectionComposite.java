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
package jpl.gds.monitor.guiapp.common.gui;

import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.common.config.types.VenueType;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.swt.SWTUtilities;

/**
 * This class is a standalone GUI composite that prompts for dictionary
 * directories and versions.
 * 
 */
public class DictionarySelectionComposite {
    private static final int LONG_FIELD_SIZE = 55;

    private Composite mainComposite;
    private Text fswDictionaryDirText;
    private Combo fswVersionCombo;
    private Text sseDictionaryDirText;
    private Combo sseVersionCombo;
    private Button sseBrowseButton;
    private Button fswBrowseButton;
    private DictionaryProperties dictConfig;
    private final SseContextFlag    sseFlag;
    private final SWTUtilities util = new SWTUtilities();
    private final MissionProperties missionProps;

    private Group sseComposite;

    /**
     * Creates a DictionarySelectionComposite with the given shell as parent.
     * 
     * @param missionProps the current mission properties object
     * 
     * @param parent
     *            the parent Shell widget
     */
    public DictionarySelectionComposite(final MissionProperties missionProps, final SseContextFlag sseFlag,
            final Shell parent) {
    	this.missionProps = missionProps;
        this.sseFlag = sseFlag;
        createGui(parent);
    }

    private void checkAndSetFswVersionList(final String dir) {
        final List<String> fswNames = dictConfig
                .getAvailableFswVersions(dir);

        if (fswNames.isEmpty() && fswVersionCombo.getEnabled()) {
            if (!sseFlag.isApplicationSse()) {
                SWTUtilities.showErrorDialog(mainComposite.getShell(),
                        "Missing FSW Versions",
                        "There are no FSW versions available in the "
                                + "specified dictionary directory.");
            }
            fswVersionCombo.removeAll();
        } else {
            String[] items = fswNames.toArray(new String[fswNames.size()]);
            items = reverseSort(items);
            fswVersionCombo.removeAll();
            fswVersionCombo.setItems(items);
            fswVersionCombo.select(0);
        }
    }

    private void checkAndSetSseVersionList(final String dir) {

        if (!missionProps.missionHasSse()) {
            return;
        }

        final List<String> sseNames = dictConfig
                .getAvailableSseVersions(dir);

        if (sseNames.isEmpty() && sseVersionCombo.getEnabled()) {
            SWTUtilities.showErrorDialog(mainComposite.getShell(),
                    "Missing SSE Versions",
                    "There are no SSE versions available in the "
                            + "specified dictionary directory.");
            sseVersionCombo.removeAll();
        } else {
            String[] items = sseNames.toArray(new String[sseNames.size()]);
            items = reverseSort(items);
            sseVersionCombo.setItems(items);
            sseVersionCombo.select(0);
        }
    }

    /**
     * Validates the fields in this composite.
     * 
     * @return true if all fields have valid values, false if not
     */
    public boolean checkFields() {
        return (!((sseComposite != null && getSseVersion().equals(""))
                || getFswVersion().equals("")
                || getFswDictionaryDir().equals("")
                || (sseComposite != null && 
                        getSseDictionaryDir().equals(""))));
    }

    private void createGui(final Shell parent) {
        mainComposite = new Composite(parent, SWT.NONE);
        final RowLayout rl = new RowLayout(SWT.VERTICAL);
        rl.marginTop = 5;
        rl.marginBottom = 5;
        rl.spacing = 10;
        rl.pack = false;
        mainComposite.setLayout(rl);

        createFswComposite();
        
        mainComposite.pack();
    }
    
    /**
     * Adjusts the GUI fields for a venue change.
     * @param vt the new venue type
     */
    public void setVenue(final VenueType vt) {
        if (missionProps.missionHasSse() && !vt.isOpsVenue()) {
            /* 
             * Check for null composite here instead of above.
             * If the previous venue setting created the composite, it will still exist, and
             * checking for null above made the wrong thing happen.
             */
            if (sseComposite == null) {
                createSseComposite();
            }
            sseDictionaryDirText.setText(dictConfig
                    .getSseDictionaryDir());
            sseDictionaryDirText.setToolTipText(sseDictionaryDirText
                    .getText());
            checkAndSetSseVersionList(dictConfig.getSseDictionaryDir());
            if (dictConfig.getSseVersion() != null) {
                sseVersionCombo.setText(dictConfig.getSseVersion());
            }
            enable(fswDictionaryDirText.isEnabled());
        } else {
            if (sseComposite != null) {
                sseComposite.dispose();
                sseComposite = null;
            }
        }
        
        mainComposite.layout();
        mainComposite.pack();
    }
    
    private void createFswComposite() {
        
        final Group fswComposite = new Group(mainComposite, SWT.BORDER);

        final FormLayout fl = new FormLayout();
        fl.spacing = 10;
        fl.marginHeight = 3;
        fl.marginBottom = 6;
        fl.marginWidth = 5;
        fswComposite.setLayout(fl);

        final Label fswDictionaryDirLabel = new Label(fswComposite, SWT.NONE);
        fswDictionaryDirLabel.setText("FSW Dict Dir:");
        final FormData fswlfd = new FormData();
        fswlfd.top = new FormAttachment(0, 10);
        fswlfd.right = new FormAttachment(10);
        fswDictionaryDirLabel.setLayoutData(fswlfd);

        fswDictionaryDirText = new Text(fswComposite, SWT.BORDER);
        fswDictionaryDirText.setEditable(false);
        final FormData fswFd = SWTUtilities.getFormData(fswDictionaryDirText,
                1, LONG_FIELD_SIZE);
        fswFd.top = new FormAttachment(fswDictionaryDirLabel, 0, SWT.CENTER);
        fswFd.left = new FormAttachment(fswDictionaryDirLabel);
        fswDictionaryDirText.setLayoutData(fswFd);

        fswBrowseButton = new Button(fswComposite, SWT.PUSH);
        fswBrowseButton.setText("Browse...");
        final FormData fswbfd = new FormData();
        fswbfd.top = new FormAttachment(fswDictionaryDirText, 0, SWT.CENTER);
        fswbfd.left = new FormAttachment(fswDictionaryDirText);
        fswBrowseButton.setLayoutData(fswbfd);

        final Label fswVersionLabel = new Label(fswComposite, SWT.NONE);
        fswVersionLabel.setText("FSW Version:");
        final FormData fsvlfd = new FormData();
        fsvlfd.top = new FormAttachment(fswDictionaryDirText);
        fsvlfd.right = new FormAttachment(10);
        fswVersionLabel.setLayoutData(fsvlfd);

        fswVersionCombo = new Combo(fswComposite, SWT.DROP_DOWN);
        final FormData fsvcfd = SWTUtilities
                .getFormData(fswVersionCombo, 1, 50);
        fsvcfd.top = new FormAttachment(fswVersionLabel, 0, SWT.CENTER);
        fsvcfd.left = new FormAttachment(fswVersionLabel);
        fswVersionCombo.setLayoutData(fsvcfd);

        fswBrowseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(
                    final org.eclipse.swt.events.SelectionEvent e) {
                try {
                    final String dirName = util.displayStickyFileChooser(true,
                            mainComposite.getShell(),
                            "fswDictionaryDirBrowseButton",
                            fswDictionaryDirText.getText());
                    if (dirName != null) {
                        fswDictionaryDirText.setText(dirName);
                        fswDictionaryDirText.setToolTipText(dirName);
                        checkAndSetFswVersionList(dirName);
                    }
                } catch (final Exception e1) {
                    TraceManager.getDefaultTracer().error("Unexpected exception handling button event: " + ExceptionTools.getMessage(e1), e1);
                }
            }
        });

    }
    
    private void createSseComposite() {
        if (missionProps.missionHasSse()) {
            sseComposite = new Group(mainComposite, SWT.BORDER);
            final FormLayout fl1 = new FormLayout();
            fl1.marginWidth = 5;
            fl1.marginHeight = 5;
            fl1.marginBottom = 10;
            fl1.spacing = 10;
            sseComposite.setLayout(fl1);

            final Label sseDictionaryDirLabel = new Label(sseComposite,
                    SWT.NONE);
            sseDictionaryDirLabel.setText("SSE Dict Dir:");
            final FormData sselfd = new FormData();
            sselfd.top = new FormAttachment(0, 10);
            sselfd.right = new FormAttachment(10);
            sseDictionaryDirLabel.setLayoutData(sselfd);

            sseDictionaryDirText = new Text(sseComposite, SWT.BORDER);
            sseDictionaryDirText.setEditable(false);
            final FormData sseFd = SWTUtilities.getFormData(
                    sseDictionaryDirText, 1, LONG_FIELD_SIZE);
            sseFd.top = new FormAttachment(
                    sseDictionaryDirLabel, 0, SWT.CENTER);
            sseFd.left = new FormAttachment(sseDictionaryDirLabel);
            sseDictionaryDirText.setLayoutData(sseFd);

            sseBrowseButton = new Button(sseComposite, SWT.PUSH);
            sseBrowseButton.setText("Browse...");
            final FormData ssebfd = new FormData();
            ssebfd.top = new FormAttachment(
                    sseDictionaryDirText, 0, SWT.CENTER);
            ssebfd.left = new FormAttachment(sseDictionaryDirText);
            sseBrowseButton.setLayoutData(ssebfd);

            final Label sseVersionLabel = new Label(sseComposite, SWT.NONE);
            sseVersionLabel.setText("SSE Version:");
            final FormData ssevlfd = new FormData();
            ssevlfd.top = new FormAttachment(sseDictionaryDirText);
            ssevlfd.right = new FormAttachment(10);
            sseVersionLabel.setLayoutData(ssevlfd);

            sseVersionCombo = new Combo(sseComposite, SWT.DROP_DOWN);
            final FormData ssevcfd = SWTUtilities.getFormData(sseVersionCombo,
                    1, 50);
            ssevcfd.top = new FormAttachment(sseVersionLabel, 0, SWT.CENTER);
            ssevcfd.left = new FormAttachment(sseVersionLabel);
            sseVersionCombo.setLayoutData(ssevcfd);

            sseBrowseButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(
                        final org.eclipse.swt.events.SelectionEvent e) {
                    try {
                        final String dirName = util.displayStickyFileChooser(
                                true, mainComposite.getShell(),
                                "sseDictionaryDirBrowseButton",
                                sseDictionaryDirText.getText());
                        if (dirName != null) {
                            sseDictionaryDirText.setText(dirName);
                            sseDictionaryDirText.setToolTipText(dirName);
                            checkAndSetSseVersionList(dirName);
                        }
                    } catch (final Exception e1) {
                        TraceManager.getDefaultTracer().error("Unexpected exception handling button event: " + ExceptionTools.getMessage(e1), e1);
                    }
                }
            });
        }
    }

    /**
     * Enables/disables fields in this composite
     * 
     * @param selection
     *            true to enable all fields, false to disable
     */
    public void enable(final boolean selection) {
        if (fswDictionaryDirText != null) {
            fswDictionaryDirText.setEnabled(selection);
        }
        if (fswVersionCombo != null) {
            fswVersionCombo.setEnabled(selection);
        }
        if (fswBrowseButton != null) {
            fswBrowseButton.setEnabled(selection);
        }
        if (sseComposite != null) {
            if (sseDictionaryDirText != null) {
                sseDictionaryDirText.setEnabled(selection);
            }
            if (sseVersionCombo != null) {
                sseVersionCombo.setEnabled(selection);
            }
            if (sseBrowseButton != null) {
                sseBrowseButton.setEnabled(selection);
            }
        }
    }

    /**
     * Returns the main GUI composite used by this class.
     * 
     * @return Composite object
     */
    public Composite getComposite() {
        return mainComposite;
    }

    /**
     * Gets the FSW Dictionary Directory set by the user in this widget.
     * 
     * @return directory name
     */
    public String getFswDictionaryDir() {
        return fswDictionaryDirText.getText().trim();
    }

    /**
     * Gets the FSW version set by the user in this widget.
     * 
     * @return version string
     */
    public String getFswVersion() {
        return fswVersionCombo.getText();
    }

    /**
     * Gets the SSE Dictionary Directory set by the user in this widget.
     * 
     * @return directory name
     */
    public String getSseDictionaryDir() {
        if (sseComposite != null) {
            return sseDictionaryDirText.getText().trim();
        } 
        return null;
    }

    /**
     * Gets the SSE version set by the user in this widget.
     * 
     * @return version string
     */
    public String getSseVersion() {
        if (sseComposite != null) {
            return sseVersionCombo.getText();
        } 
        return null;
    }

    private String[] reverseSort(final String[] items) {
        Arrays.sort(items);
        final String[] newItems = new String[items.length];
        int index = items.length - 1;
        for (final String item : items) {
            newItems[index--] = item;
        }
        return newItems;
    }

    /**
     * Sets the fields in this composite from the given DictionaryConfiguration
     * object.
     * 
     * @param config
     *            the DictionaryConfiguration object to get dictionary information
     *            from
     */
    public void setDictionaryConfiguration(final DictionaryProperties config) {
        dictConfig = config;
        if (dictConfig != null) {
            fswDictionaryDirText.setText(dictConfig.getFswDictionaryDir());
            fswDictionaryDirText.setToolTipText(dictConfig
                    .getFswDictionaryDir());
            checkAndSetFswVersionList(dictConfig.getFswDictionaryDir());
            if (dictConfig.getFswVersion() != null) {
                fswVersionCombo.setText(dictConfig.getFswVersion());
            }
            if (sseComposite != null) {
                sseDictionaryDirText.setText(dictConfig
                        .getSseDictionaryDir());
                sseDictionaryDirText.setToolTipText(sseDictionaryDirText
                        .getText());
                checkAndSetSseVersionList(dictConfig.getSseDictionaryDir());
                if (dictConfig.getSseVersion() != null) {
                    sseVersionCombo.setText(dictConfig.getSseVersion());
                }
            }
        }
    }
}
