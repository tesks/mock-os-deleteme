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
package jpl.gds.session.config.gui;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

import org.apache.velocity.Template;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import jpl.gds.common.config.GeneralProperties;
import jpl.gds.common.swt.AboutUtility;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.swt.ChillShell;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.shared.template.TemplateException;
import jpl.gds.shared.template.TemplateManager;

/**
 * This is the PVL Editor window, which provides the capability to load/save/modify PVL files.
 * 
 */
public class PvlEditorShell implements ChillShell {
    /**
     * Event handler for the PVL editor window.
     */
    protected class EventHandler extends SelectionAdapter implements
            ModifyListener {
        /**
         * Default constructor for the event handler.
         */
        public EventHandler() {
            super();
        }


        @Override
		public void modifyText(final ModifyEvent me) {
            if (me.getSource() == pvlText) {
                changed = true;
                final String title = getShell().getText();
                if (!title.endsWith("*")) {
                    getShell().setText(title + "*");
                }
            }
        }

        /**
         *
         * Allow the edited PVL
         * to overwrite the current file or save as a new one as specified 
         * by internal configuration.

         */
        @Override
        public void widgetSelected(final SelectionEvent e) {
            if (e.getSource() == loadButton) {
                if (changed) {
                    final boolean doSave = SWTUtilities.showConfirmDialog(
                        getShell(), "Save Changes",
                        "You have unsaved changes. Would you like to Save "
                        + "before you continue?");
                    if (!doSave) {
                        return;
                    }

                    if (pvlFile != null && overwritePvlFile) {
                        if (!saveToFile(pvlFile.getAbsolutePath())) {
                            return;
                        }
                    } else {
                        if (!save()) {
                            return;
                        }
                    }
                }

                canceled = false;
                mainShell.close();
            } else if (e.getSource() == exitButton
                    || e.getSource() == exitMenuItem) {
                if (changed) {
                    final boolean doSave = SWTUtilities .showConfirmDialog(
                        getShell(), "Save Changes",
                        "You have unsaved changes. Would you like to Save "
                        + "before you continue?");
                    if (doSave) {
                        if (pvlFile != null && overwritePvlFile) {
                            if (!saveToFile(pvlFile.getAbsolutePath())) {
                                return;
                            }
                        } else {
                            if (!save()) {
                                return;
                            }
                        }
                    }
                }

                canceled = true;
                mainShell.close();
            } else if (e.getSource() == saveMenuItem) {
                if (!changed) {
                    return;
                }

                if (pvlFile != null) {
                    if (!saveToFile(pvlFile.getAbsolutePath())) {
                        return;
                    }
                } else {
                    if (!save()) {
                        return;
                    }
                }
            } else if (e.getSource() == saveAsMenuItem) {
                if (!save()) {
                    return;
                }
            } else if (e.getSource() == aboutMenuItem) {
                AboutUtility.showStandardAboutDialog(getShell(), genProps);
            }
        }
    }


    /**
     * The shell of the gui.
     */
    protected Shell mainShell = null;
    /**
     * The parent of the shell.
     */
    protected Shell parent = null;
    /**
     * Whether the gui shell was canceled with the cancel button.
     */
    protected boolean canceled = false;
    /**
     * The pvl file being modified or created.
     */
    protected File pvlFile = null;
    /**
     * Pvl Label.
     */
    protected Label pvlLabel = null;
    /**
     * File being modified.
     */
    protected Text pvlText = null;
    /**
     * Pvl load button.
     */
    protected Button loadButton = null;
    /**
     * Pvl editor exit button.
     */
    protected Button exitButton = null;
    /**
     * Pvl editor menu.
     */
    protected Menu fileMenu = null;
    /**
     * File menu header.
     */
    protected MenuItem fileMenuHeader = null;
    /**
     * Save menu option.
     */
    protected MenuItem saveMenuItem = null;
    /**
     * Save as menu option.
     */
    protected MenuItem saveAsMenuItem = null;
    /**
     * Load menu option.
     */
    protected MenuItem loadMenuItem = null;
    /**
     * Query menu option.
     */
    protected MenuItem queryMenuItem = null;
    /**
     * Exit menu option.
     */
    protected MenuItem exitMenuItem = null;
    /**
     * Help menu.
     */
    protected Menu helpMenu = null;
    /**
     * Help menu header.
     */
    protected MenuItem helpMenuHeader = null;
    /**
     * About menu option.
     */
    protected MenuItem aboutMenuItem = null;

    private boolean changed = false;
    private final EventHandler handler;
    private final SWTUtilities util;

    private static TemplateManager templateManager;

    private static Template template = null;
	
    private GeneralProperties genProps;
    private final SseContextFlag   sseFlag;
    

    /**
     * overwrite the PVL being edited
     */
    private boolean overwritePvlFile = true;

    /**
     * Constructor which creates the Pvl Editor.
     * 
     * @param paramParent
     *            Parent shell.
     * @param gp
     *            the current GeneralProperties object
     * @param sseFlag
     *            the SSE context flag
     */
    public PvlEditorShell(final Shell paramParent, final GeneralProperties gp, final SseContextFlag sseFlag) {
        this(paramParent, null, gp, sseFlag);
        genProps = gp;
        changed = true;
    }

    /**
     * Constructor which creates the Pvl Editor and specifies the pvl file.
     * 
     * @param paramParent
     *            Parent shell.
     * @param paramPvlFile
     *            Pvl file to load/edit.
     * @param gp
     *            the current GeneralProperties object
     * @param sseFlag
     *            the SSE context flag
     */
    public PvlEditorShell(final Shell paramParent, final File paramPvlFile, final GeneralProperties gp,
            final SseContextFlag sseFlag) {
        this.parent = paramParent;
        this.pvlFile = paramPvlFile;
        this.sseFlag = sseFlag;
        this.templateManager = new TemplateManager(GdsSystemProperties.getSystemMissionIncludingSse(sseFlag.isApplicationSse()));
        genProps = gp;
        handler = new EventHandler();
        changed = false;
        util = new SWTUtilities();

        createControls();
    }
    
	/**
     * Constructor which creates the Pvl Editor and specifies the pvl file.
     * Allows changes to the PVL to overwrite the supplied PVL or to be saved as
     * a new file.
     * 
     * @param paramParent
     *            Parent shell.
     * @param paramPvlFile
     *            Pvl file to load/edit.
     * @param overwritePvlFile
     *            TRUE if the supplied PVL is to be overwritten with changes,
     *            FALSE otherwise
     * @param gp
     *            the current GeneralProperties object
     * 
     * @param sseFlag
     *            the SSE context flag
     */
    public PvlEditorShell(final Shell paramParent, final File paramPvlFile, final boolean overwritePvlFile,
            final GeneralProperties gp, final SseContextFlag sseFlag) {
        this(paramParent, paramPvlFile, gp, sseFlag);
        
        this.overwritePvlFile = overwritePvlFile;
    }

    /**
     * Creates the GUI controls.
     */
    protected void createControls() {
        mainShell =
                new Shell(parent, SWT.PRIMARY_MODAL | SWT.DIALOG_TRIM
                        | SWT.RESIZE);
        mainShell.setText(changed || pvlFile == null ? "New PVL File" + "*"
                : pvlFile.getName());
        mainShell.setSize(getSize());

        final FormLayout shellLayout = new FormLayout();
        shellLayout.spacing = 5;
        shellLayout.marginHeight = 5;
        shellLayout.marginWidth = 5;
        mainShell.setLayout(shellLayout);

        createMenus();

        pvlLabel = new Label(mainShell, SWT.LEFT);
        pvlLabel.setFont(getHeaderFont());
        pvlLabel.setText("PVL Editor");
        final FormData plFormData = new FormData();
        plFormData.top = new FormAttachment(0, 10);
        plFormData.left = new FormAttachment(0, 10);
        plFormData.right = new FormAttachment(100, -10);
        pvlLabel.setLayoutData(plFormData);

        // create Text
        pvlText =
                new Text(mainShell, SWT.LEFT | SWT.BORDER | SWT.WRAP
                        | SWT.MULTI | SWT.V_SCROLL | SWT.SHADOW_ETCHED_OUT);
        pvlText.setFont(getTextFieldFont());
        pvlText.setText("");
        final FormData ptFormData = new FormData();
        ptFormData.top = new FormAttachment(pvlLabel, 10);
        ptFormData.left = plFormData.left;
        ptFormData.right = plFormData.right;
        ptFormData.bottom = new FormAttachment(90, -10);
        pvlText.setLayoutData(ptFormData);

        final Label separator =
                new Label(mainShell, SWT.CENTER | SWT.SEPARATOR
                        | SWT.HORIZONTAL);
        final FormData sepFormData = new FormData();
        sepFormData.top = new FormAttachment(pvlText, 10);
        sepFormData.left = new FormAttachment(0);
        sepFormData.right = new FormAttachment(100);
        separator.setLayoutData(sepFormData);

        loadButton = new Button(mainShell, SWT.PUSH);
        final FormData cpbFormData = new FormData();
        cpbFormData.top = new FormAttachment(separator, 10);
        cpbFormData.left = new FormAttachment(20);
        cpbFormData.right = new FormAttachment(40);
        loadButton.setLayoutData(cpbFormData);
        loadButton.setText("Load");

        exitButton = new Button(mainShell, SWT.PUSH);
        final FormData cbFormData = new FormData();
        cbFormData.top = new FormAttachment(separator, 10);
        cbFormData.left = new FormAttachment(60);
        cbFormData.right = new FormAttachment(80);
        exitButton.setLayoutData(cbFormData);
        exitButton.setText("Exit");

        setDefaultFieldValues();

        pvlText.addModifyListener(handler);
        loadButton.addSelectionListener(handler);
        exitButton.addSelectionListener(handler);
    }

    private void createMenus() {
        final Menu menuBar = new Menu(mainShell, SWT.BAR);

        fileMenu = new Menu(mainShell, SWT.DROP_DOWN);
        helpMenu = new Menu(mainShell, SWT.DROP_DOWN);

        fileMenuHeader = new MenuItem(menuBar, SWT.CASCADE);
        fileMenuHeader.setText("File");
        fileMenuHeader.setMenu(fileMenu);

        saveMenuItem = new MenuItem(fileMenu, SWT.PUSH);
        saveMenuItem.setText("Save");
        saveMenuItem.addSelectionListener(handler);

        saveAsMenuItem = new MenuItem(fileMenu, SWT.PUSH);
        saveAsMenuItem.setText("Save As...");
        saveAsMenuItem.addSelectionListener(handler);

        new MenuItem(fileMenu, SWT.SEPARATOR);

        exitMenuItem = new MenuItem(fileMenu, SWT.PUSH);
        exitMenuItem.setText("Exit");
        exitMenuItem.addSelectionListener(handler);

        helpMenuHeader = new MenuItem(menuBar, SWT.CASCADE);
        helpMenuHeader.setText("Help");
        helpMenuHeader.setMenu(helpMenu);

        aboutMenuItem = new MenuItem(helpMenu, SWT.PUSH);
        aboutMenuItem.setText("About...");
        aboutMenuItem.addSelectionListener(handler);

        mainShell.setMenuBar(menuBar);
    }

    /**
     * Gets the header font.
     * 
     * @return Helvetica 18 sized font.
     */
    private Font getHeaderFont() {
        return (new Font(mainShell.getDisplay(), new FontData("Helvetica", 18,
            SWT.BOLD)));
    }

    /**
     * Returns the File object for the Pvl File.
     * @return File object.
     */
    public File getPvlFile() {
        return (pvlFile);
    }

    /**
     * Returns the filename of the saved pvl file. 
     * @return filename
     */
    protected String getSaveFile() {
        String filename = null;
        if (pvlFile != null) {
            filename =
                    util.displayStickyFileSaver(getShell(), "PvlEditorShell",
                        pvlFile.getParent(), pvlFile.getName());
        } else {
            filename =
                    util.displayStickyFileSaver(getShell(), "PvlEditorShell",
                        null, "new_mpcs.pvl");
        }

        return (filename);
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.swt.ChillShell#getShell()
     */
    @Override
	public Shell getShell() {
        return mainShell;
    }

    /**
     * Gets the dimensions of the window.
     * 
     * @return Size of the window.
     */
    public Point getSize() {
        return (new Point(700, 600));
    }

    /**
     * Gets the text field font.
     * 
     * @return Courier 14 sized font.
     */
    private Font getTextFieldFont() {
        return (new Font(mainShell.getDisplay(), new FontData("Courier", 14,
            SWT.NONE)));
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.swt.ChillShell#getTitle()
     */
    @Override
	public String getTitle() {
        return (getShell().getText());
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.swt.ChillShell#open()
     */
    @Override
	public void open() {
        canceled = true;
        mainShell.open();
    }

    /**
     * Returns true if the pvl file was saved, false otherwise.
     * @return if saved
     */
    protected boolean save() {
        final String filename = getSaveFile();
        if (filename == null) {
            return (false);
        }

        return (saveToFile(filename));
    }

    /**
     * Saves the pv information to the filename.
     * @param filename File to save to.
     * @return True if saved successfully, false otherwise.
     */
    protected boolean saveToFile(final String filename) {
        final File oldPvlFile =
                pvlFile != null ? pvlFile.getAbsoluteFile() : null;
        pvlFile = new File(filename);

        try {
            final BufferedWriter writer =
                    new BufferedWriter(new FileWriter(pvlFile, false));
            final String text = pvlText.getText();
            writer.write(text.replaceAll("\r", "\n"));
            writer.flush();
            writer.close();
        } catch (final Exception e) {
            SWTUtilities.showErrorDialog(getShell(), "PVL I/O Error",
                "Could not write PVL file to the location " + filename + ": "
                        + e.getMessage());
            pvlFile = oldPvlFile.getAbsoluteFile();
            return (false);
        }

        changed = false;
        getShell().setText(pvlFile.getName());
        return (true);
    }

    /**
     * Sets the default field values of the Pvl Editor.
     */
    public void setDefaultFieldValues() {
        if (pvlFile != null) {
            final StringBuilder sb = new StringBuilder(2048);
            try {
                final BufferedReader reader =
                        new BufferedReader(new FileReader(pvlFile));
                String line = reader.readLine();
                while (line != null) {
                    sb.append(line);
                    sb.append("\n"); // using buffered reader this way loses the
                    // newline characters
                    line = reader.readLine();
                }
                reader.close();
            } catch (final IOException e) {
                SWTUtilities.showErrorDialog(getShell(), "PVL I/O Error",
                    "Could not open the PVL file " + pvlFile.getName()
                            + " for editing. Defaulting to blank editor.");
                pvlText.setText("");
                return;
            }

            pvlText.setText(sb.toString());
        } else {
            try {
                if (template == null) {
                    template = templateManager.getTemplate("pvl.vm");
                }

                final HashMap<String, Object> map =
                        new HashMap<String, Object>();
                map.put("missionName",
                        GdsSystemProperties.getSystemMissionIncludingSse(sseFlag.isApplicationSse()).toUpperCase());
                map.put("body", true);

                pvlText.setText(TemplateManager.createText(template, map));
            } catch (final TemplateException e) {
                pvlText.setText("");
            }
        }
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.swt.ChillShell#wasCanceled()
     */
    @Override
	public boolean wasCanceled() {
        return canceled;
    }
}
