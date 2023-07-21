/*
 * Copyright 2006-2019. California Institute of Technology.
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
package jpl.gds.tcapp.app.gui.fault;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.TypedEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.springframework.context.ApplicationContext;

import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.OpcodeUtil;
import jpl.gds.dictionary.api.command.CommandArgumentType;
import jpl.gds.dictionary.api.command.ICommandDefinition;
import jpl.gds.dictionary.api.command.ICommandDefinitionProvider;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.string.StringUtil;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.tc.api.ICommandObjectFactory;
import jpl.gds.tc.api.ISendCompositeState;
import jpl.gds.tc.api.command.ICommand;
import jpl.gds.tc.api.command.IFlightCommand;
import jpl.gds.tc.api.command.parser.UplinkInputParser;
import jpl.gds.tc.api.config.CommandProperties;
import jpl.gds.tc.api.config.ScmfProperties;
import jpl.gds.tc.api.config.UplinkParseException;
import jpl.gds.tc.api.exception.CommandParseException;
import jpl.gds.tc.api.exception.RawOutputException;
import jpl.gds.tc.api.exception.UplinkException;
import jpl.gds.tc.api.message.ICommandMessageFactory;
import jpl.gds.tc.api.output.OutputFileNameFactory;
import jpl.gds.tcapp.app.SendCommandApp;
import jpl.gds.tcapp.app.gui.AbstractUplinkComposite;
import jpl.gds.tcapp.app.gui.TransmitEvent;
import jpl.gds.tcapp.app.gui.UplinkExecutors;
import jpl.gds.tcapp.app.gui.fault.util.ArgumentGuiControlFactory;
import jpl.gds.tcapp.app.gui.fault.util.CommandArgumentGuiHelper;
import jpl.gds.tcapp.app.gui.fault.util.ICommandArgumentGuiControl;

/**
 * 
 * The GUI application to visualize the command dictionary and help in
 * constructing/corrupting uplink commands.
 * 
 * Can be used both as a standalone GUI launched from chill_up or as a component
 * in the fault injection wizard.
 * 
 * The basic layout is:
 * 
 * ================================================= search by [combo] [search
 * text ] command information | | | | | | ----------------------- | | | | | stem
 * | command arguments | list | | | | | | |-------------------------------- | |
 * button list | | ==================================================
 * 
 *
 * 6/23/14. Changes throughout to accomodate split or
 *          runtime command classes from command dictionary classes.
 * 03/22/19. Changes through the whole class to work
 *          with argument values and properties directly from commands instead
 *          of pulling the argument from the command and working with it
 *          individually.
 *          Added bitmask argument support
 */
public class CommandBuilderComposite extends AbstractUplinkComposite implements
FaultInjectorGuiComponent {
    /** Logging interface */
    protected final Tracer trace ;


    /** The font to use in this GUI */
    protected static final String FONT_NAME = "Helvetica";
    /**
     * Error message for a known weird SWT condition. On Linux, if the user
     * starts typing inside a List, it will deselect the list item, but not send
     * the corresponding event and so the GUI will get into an inconsistent
     * state. Not cool.
     */
    protected static final String NULL_SELECTED_COMMAND = "No command selected. Please re-select your command in"
            + " the left hand list and try again.";
    /** The prefix for the title of this page */
    protected static final String TITLE_PREFIX = "Command Dictionary Version ";
    /** The title of this page in the fault injector */
    protected static final String FAULT_PAGE_TITLE = "Command Fault Injection";
    /** The description of this page in the fault injector */
    protected static final String FAULT_PAGE_DESC = "Choose a command and then corrupt its argment values and opcode.";

    /** The data state passed from component to component in the fault injector */
    protected FaultInjectionState dataState = null;

    /** The label for the search by combo box */
    protected Label searchByLabel = null;
    /**
     * The combo box letting the user select what to use to search for commands
     * in the command list
     */
    protected Combo searchByCombo = null;
    /** The Text box containing the user entered text for searching for commands */
    protected Text searchText = null;
    /** A list of all the various command stems */
    protected List stemList = null;

    /** The recessed right half of the GUI */
    protected Group rightPanel = null;
    /** The composite containing details for a particular command */
    protected Composite cmdComposite = null;
    /** The label for the stem text field */
    protected Label stemLabel = null;
    /** A text field holding the stem for the selected command */
    protected Label stemText = null;
    /** The label for the opcode text field */
    protected Label opcodeLabel = null;
    /** A text field holding the opcode for the selected command */
    protected Text opcodeText = null;
    /** The label for the module text field */
    protected Label moduleLabel = null;
    /** A text field holding the module for the selected command */
    protected Label moduleText = null;
    /** A scrolling field holding the description for the selected command */
    protected ScrolledComposite descriptionScrollComposite = null;
    /** The label for the description field */
    protected Label descriptionLabel = null;
    /** A text field holding the description for the selected command */
    protected Label descriptionText = null;

    /** The composite holding the arguments for the command */
    protected Composite argComposite = null;
    /** A scrolling pane that holds all the command arguments */
    protected ScrolledComposite argScrollComposite = null;
    /** The panel holding the buttons at the bottom of the GUI */
    protected Composite bottomPanel = null;
    /** The labels for all the arguments */
    protected Label[] argPrefixFields = null;
    /** The user input for all the arguments (combo boxes, text fields, etc.) */
    protected Control[] argValueFields = null;
    /** The trailing labels for all the arguments */
    protected Control[] argSuffixFields = null;

    /**
     * The button used to clear all the user input values and/or reset them to
     * their default values.
     */
    protected Button clearButton = null;
    /** The button to launch a preview window for the command */
    protected Button previewButton = null;
    /** The button to attempt the user input values for the command arguments */
    protected Button validateButton = null;
    /** The button to send the command to the flight system */
    protected Button sendButton = null;
    /** The button to close the GUI */
    protected Button exitButton = null;

    /** A handler for all user input events. */
    protected EventHandler handler;

    /** The color for fields in error (that fail validation) */
    protected Color errorColor = null;
    /** The color for fields that are ok (that pass validation) */
    protected Color okColor = null;
    /** The color for text for a successful transmission */
    protected Color successColor = null;
    /** The color for text for a failed transmission */
    protected Color failureColor = null;

    /** True if the user closed the GUI, false otherwise */
    protected boolean canceled = false;
    /** A reference to the command dictionary */
    protected ICommandDefinitionProvider commandDict = null;
    /** A sorted list of all the command stems in the command dictionary */
    protected String[] stems = null;
    /**
     * True if the GUI is running as a standalone Command Builder, false if it's
     * running as part of the fault injector.
     */
    protected final boolean isStandalone;

    private final boolean hasExit;
    
    private final IMessagePublicationBus bus;
    private final ICommandObjectFactory commandFactory;
    private final ICommandMessageFactory messageFactory;

    private final ScmfProperties scmfProperties;
    private final CommandProperties commandProperties;
    private final DictionaryProperties dictProps;

    /** A stem to FlightCommand mapping for commands that have been selected/modified in the GUI
     * This enables support for the following case: 
     * 		Select and Modify command A, then select and modify command B
     * 		Selecting command A again will display it with changes from the step above
     */
    private final Map<String,IFlightCommand> commandMap = new HashMap<>();


    /**
     * Create a new command builder composite
     * 
     * @param appContext the current application context
     * @param parent The parent widget of this widget
     * 
     * @throws DictionaryException If there's an error parsing the command
     *         dictionary
     */
    public CommandBuilderComposite(final ApplicationContext appContext, final Composite parent)
            throws DictionaryException {
        this(appContext, parent, true, true);
    }

    /**
     * Create a new command builder composite
     * 
     * @param appContext the current application context
     * @param parent The parent widget of this widget
     * @param isStandalone True if the command builder is running standalone,
     *        false if it's part of something else (like the fault injection)
     * 
     * @throws DictionaryException If there's an error parsing the command
     *         dictionary
     */
    public CommandBuilderComposite(final ApplicationContext appContext, final Composite parent,
            final boolean isStandalone) throws DictionaryException {
        this(appContext, parent, isStandalone, true);
    }

    /**
     * Create a new command builder composite
     * 
     * @param appContext the current application context
     * @param parent The parent widget of this widget
     * @param isStandalone True if the command builder is running standalone,
     *        false if it's part of something else (like the fault injection)
     * @param hasExit True if EXIT button is desired
     * 
     * @throws DictionaryException If there's an error parsing the command
     *         dictionary
     */
    public CommandBuilderComposite(final ApplicationContext appContext, final Composite parent,
            final boolean isStandalone, final boolean hasExit)
                    throws DictionaryException {
        super(appContext, parent, SWT.NONE);
        this.scmfProperties = appContext.getBean(ScmfProperties.class);
        this.commandProperties = appContext.getBean(CommandProperties.class);
        this.dictProps = this.appContext.getBean(DictionaryProperties.class);
        this.trace = TraceManager.getDefaultTracer(appContext);
        this.bus = appContext.getBean(IMessagePublicationBus.class);
        this.messageFactory = appContext.getBean(ICommandMessageFactory.class);
        this.commandFactory = appContext.getBean(ICommandObjectFactory.class);
        this.isStandalone = isStandalone;
        this.hasExit = hasExit;
        this.canceled = false;
        this.commandDict = appContext.getBean(ICommandDefinitionProvider.class);
        this.stems = this.commandDict.getStems().toArray(new String[] {});
        Arrays.sort(this.stems);

        this.handler = new EventHandler(this);

        createControls();

        setTabList(new Control[] { this.searchByCombo, this.searchText,
                this.stemList, this.rightPanel });
        this.searchText.forceFocus();
        layout(true);

        this.okColor = getDisplay().getSystemColor(SWT.COLOR_BLACK);
        this.errorColor = getDisplay().getSystemColor(SWT.COLOR_RED);
    }

    /**
     * Create the various widgets of the GUI
     */
    protected void createControls() {
        setLayout(new FormLayout());

        createLeftPanel();
        createRightPanel();
        createBottomPanel();

        setDefaultFieldValues();
    }

    /**
     * Create the left hand side of the GUI that contains a list of command
     * stems and fields for allowing the user to search the list.
     */
    protected void createLeftPanel() {
        this.searchByLabel = new Label(this, SWT.NONE);
        this.searchByLabel.setText("Search By: ");
        final FormData sblFormData = new FormData();
        sblFormData.top = new FormAttachment(0, 15);
        sblFormData.left = new FormAttachment(0, 10);
        this.searchByLabel.setLayoutData(sblFormData);

        this.searchByCombo = new Combo(this, SWT.READ_ONLY | SWT.BORDER);
        this.searchByCombo.addSelectionListener(this.handler);
        final FormData sbcFormData = new FormData();
        sbcFormData.left = new FormAttachment(this.searchByLabel, 15);
        sbcFormData.top = new FormAttachment(this.searchByLabel, 0, SWT.CENTER);
        sbcFormData.right = new FormAttachment(30);
        this.searchByCombo.setLayoutData(sbcFormData);

        this.searchText = new Text(this, SWT.SINGLE | SWT.BORDER | SWT.FILL);
        this.searchText.setFont(getTextFieldFont());
        final FormData stFormData = SWTUtilities.getFormData(this.searchText, 1, 20);
        stFormData.top = new FormAttachment(this.searchByLabel, 15);
        stFormData.left = new FormAttachment(0, 10);
        stFormData.right = new FormAttachment(30);
        this.searchText.setLayoutData(stFormData);
        this.searchText.addModifyListener(this.handler);

        this.stemList = new List(this, SWT.SINGLE | SWT.BORDER | SWT.V_SCROLL);
        final FormData slFormData = new FormData();
        slFormData.top = new FormAttachment(this.searchText, 15);
        slFormData.left = new FormAttachment(0, 10);
        slFormData.right = new FormAttachment(30);
        slFormData.bottom = new FormAttachment(100, -15);
        this.stemList.setLayoutData(slFormData);
        this.stemList.addSelectionListener(this.handler);
    }

    /**
     * Create the right hand panel that contains all the various details of the
     * selected commands.
     */
    protected void createRightPanel() {
        this.rightPanel = new Group(this, SWT.NONE);
        final FontData groupFontData = new FontData("Helvetica", 14, SWT.BOLD);
        final Font groupFont = new Font(getDisplay(), groupFontData);
        this.rightPanel.setFont(groupFont);
        this.rightPanel.setText("Command Information");
        final FormLayout rpLayout = new FormLayout();
        rpLayout.marginTop = 10;
        rpLayout.marginBottom = 10;
        rpLayout.marginLeft = 5;
        rpLayout.marginRight = 5;
        this.rightPanel.setLayout(rpLayout);

        // position the right panel in the parent shell
        final FormData rpFormData = new FormData();
        rpFormData.left = new FormAttachment(this.stemList, 20);
        rpFormData.top = new FormAttachment(0, 10);
        rpFormData.right = new FormAttachment(100, -10);
        rpFormData.bottom = new FormAttachment(this.isStandalone ? 80 : 90);
        this.rightPanel.setLayoutData(rpFormData);
    }

    /**
     * Create the bottom panel that contains all the various buttons the user
     * uses to interact
     */
    protected void createBottomPanel() {
        this.bottomPanel = new Composite(this, SWT.NONE);
        this.bottomPanel.setLayout(new FormLayout());

        // position the bottom panel in the parent shell
        final FormData bpFormData = new FormData();
        bpFormData.left = new FormAttachment(this.stemList, 20);
        // bpFormData.right = new FormAttachment(100,-10);
        // bpFormData.bottom = new FormAttachment(100,-5);
        bpFormData.top = new FormAttachment(this.rightPanel, 10);
        this.bottomPanel.setLayoutData(bpFormData);

        this.previewButton = new Button(this.bottomPanel, SWT.PUSH);
        this.previewButton.setText("Preview");
        final FormData pbFormData = new FormData();
        pbFormData.top = new FormAttachment(0);
        pbFormData.left = new FormAttachment(10);
        pbFormData.right = new FormAttachment(30);
        if (!this.isStandalone) {
            pbFormData.bottom = new FormAttachment(100);
        }
        this.previewButton.setLayoutData(pbFormData);
        this.previewButton.addSelectionListener(this.handler);

        this.validateButton = new Button(this.bottomPanel, SWT.PUSH);
        this.validateButton.setText("Validate");
        final FormData vbFormData = new FormData();
        vbFormData.top = new FormAttachment(0);
        vbFormData.left = new FormAttachment(40);
        vbFormData.right = new FormAttachment(60);
        if (!this.isStandalone) {
            vbFormData.bottom = new FormAttachment(100);
        }
        this.validateButton.setLayoutData(vbFormData);
        this.validateButton.addSelectionListener(this.handler);

        this.clearButton = new Button(this.bottomPanel, SWT.PUSH);
        this.clearButton.setText("Clear");
        final FormData cbFormData = new FormData();
        cbFormData.top = new FormAttachment(0);
        cbFormData.left = new FormAttachment(70);
        cbFormData.right = new FormAttachment(90);
        if (!this.isStandalone) {
            cbFormData.bottom = new FormAttachment(100);
        }
        this.clearButton.setLayoutData(cbFormData);
        this.clearButton.addSelectionListener(this.handler);

        // the send button and transmission status label are only applicable
        // in standalone mode
        if (this.isStandalone) {
            this.sendButton = new Button(this.bottomPanel, SWT.PUSH);
            this.sendButton.setText("Send");
            final FormData sbFormData = new FormData();
            sbFormData.top = new FormAttachment(this.previewButton, 5);
            sbFormData.left = new FormAttachment(25);
            sbFormData.right = new FormAttachment(45);
            // sbFormData.bottom = new FormAttachment(100);
            this.sendButton.setLayoutData(sbFormData);
            this.sendButton.addSelectionListener(this.handler);

            if (hasExit) {
                this.exitButton = new Button(this.bottomPanel, SWT.PUSH);
                this.exitButton.setText("Exit");
                final FormData ebFormData = new FormData();
                ebFormData.top = new FormAttachment(this.previewButton, 5);
                ebFormData.left = new FormAttachment(55);
                ebFormData.right = new FormAttachment(75);
                // ebFormData.bottom = new FormAttachment(100);
                this.exitButton.setLayoutData(ebFormData);
                this.exitButton.addSelectionListener(this.handler);
            }
        }
    }

    /**
     * Set all the default values for the various fields in the GUI (this does
     * not include populating fields for a selected command).
     */
    protected void setDefaultFieldValues() {
        this.searchByCombo.add("Stem");

        final boolean supportsCommandModule = commandProperties.supportsCommandModule();

        if (supportsCommandModule) {
            this.searchByCombo.add("Module");
        }

        this.searchByCombo.setText("Stem");

        for (final String stem : this.stems) {
            this.stemList.add(stem);
        }

        this.searchByCombo.select(0);

        this.previewButton.setEnabled(false);
        this.validateButton.setEnabled(false);
        this.clearButton.setEnabled(false);

        if (this.isStandalone) {
            this.sendButton.setEnabled(false);
        }
    }

    /**
     * Take the command selected in the stem list on the left side of the GUI
     * and display the details of that command on the right side of the GUI
     */
    protected void displaySelectedCommand() {
        final IFlightCommand currentCommand = getSelectedCommand();
        if (currentCommand == null) {
            return;
        }

        displayCommand(currentCommand);
    }

    /**
     * Display the details of a command on the right side of the GUI
     * 
     * @param command The command whose details should be displayed
     */
    protected void displayCommand(final IFlightCommand command) {
        destroyCommandComposite();

        final int labelLeftAttach = 0;
        final int labelLeftOffset = 10;
        final int labelRightAttach = 30;
        final int textLeftAttach = 40;
        final int textRightAttach = 100;
        final int textRightOffset = 0;
        final int vOffset = 11;

        this.cmdComposite = new Composite(this.rightPanel, SWT.NONE);
        this.rightPanel.setTabList(new Control[] { this.cmdComposite });
        final FormData ccFormData = new FormData();
        ccFormData.left = new FormAttachment(0);
        ccFormData.right = new FormAttachment(100);
        ccFormData.top = new FormAttachment(0);
        ccFormData.bottom = new FormAttachment(100);
        this.cmdComposite.setLayoutData(ccFormData);
        this.cmdComposite.setLayout(new FormLayout());

        this.stemLabel = new Label(this.cmdComposite, SWT.LEFT);
        this.stemLabel.setText("Stem: ");
        final FormData slFormData = new FormData();
        slFormData.left = new FormAttachment(labelLeftAttach, labelLeftOffset);
        slFormData.top = new FormAttachment(0, 5);
        slFormData.right = new FormAttachment(labelRightAttach);
        this.stemLabel.setLayoutData(slFormData);

        this.stemText = new Label(this.cmdComposite, SWT.LEFT | SWT.BORDER
                | SWT.SINGLE | SWT.READ_ONLY);
        this.stemText.setText(command.getDefinition().getStem());
        final FormData stFormData = new FormData();
        stFormData.left = new FormAttachment(textLeftAttach);
        stFormData.top = new FormAttachment(this.stemLabel, 0, SWT.CENTER);
        stFormData.right = new FormAttachment(textRightAttach, textRightOffset);
        this.stemText.setLayoutData(stFormData);

        this.opcodeLabel = new Label(this.cmdComposite, SWT.LEFT);
        this.opcodeLabel.setText("Opcode ("
                + dictProps.getOpcodeBitLength()
                + " bits): ");
        final FormData olFormData = new FormData();
        olFormData.left = new FormAttachment(labelLeftAttach, labelLeftOffset);
        olFormData.top = new FormAttachment(this.stemLabel, vOffset);
        olFormData.right = new FormAttachment(labelRightAttach);
        this.opcodeLabel.setLayoutData(olFormData);

        this.opcodeText = new Text(this.cmdComposite, SWT.LEFT | SWT.BORDER
                | SWT.SINGLE);
        this.opcodeText.setFont(getTextFieldFont());

        String opcode = command.getEnteredOpcode().trim();

        if (! opcode.equalsIgnoreCase("null"))
        {
            opcode = OpcodeUtil.addHexPrefix1(opcode);
        }

        this.opcodeText.setText(opcode);
        this.opcodeText.setEditable(!this.isStandalone); // opcode is
        // only
        // editable
        // in
        // fault
        // injector
        final FormData otFormData = new FormData();
        otFormData.left = new FormAttachment(textLeftAttach);
        otFormData.top = new FormAttachment(this.opcodeLabel, 0, SWT.CENTER);
        otFormData.right = new FormAttachment(textRightAttach, textRightOffset);
        this.opcodeText.setLayoutData(otFormData);

        this.moduleLabel = new Label(this.cmdComposite, SWT.LEFT);
        this.moduleLabel.setText("Module: ");
        final FormData mlFormData = new FormData();
        mlFormData.left = new FormAttachment(labelLeftAttach, labelLeftOffset);
        mlFormData.top = new FormAttachment(this.opcodeLabel, vOffset);
        mlFormData.right = new FormAttachment(labelRightAttach);
        this.moduleLabel.setLayoutData(mlFormData);
        this.moduleLabel.setVisible(commandProperties
                .supportsCommandModule());

        this.moduleText = new Label(this.cmdComposite, SWT.LEFT | SWT.BORDER
                | SWT.SINGLE | SWT.READ_ONLY);
        final String module = command.getDefinition().getCategory(ICommandDefinition.MODULE);
        this.moduleText.setText(module != null ? module.trim() : "");
        final FormData mtFormData = new FormData();
        mtFormData.left = new FormAttachment(textLeftAttach);
        mtFormData.top = new FormAttachment(this.moduleLabel, 0, SWT.CENTER);
        mtFormData.right = new FormAttachment(textRightAttach, textRightOffset);
        this.moduleText.setLayoutData(mtFormData);
        this.moduleText.setVisible(commandProperties
                .supportsCommandModule());

        this.descriptionLabel = new Label(this.cmdComposite, SWT.LEFT);
        this.descriptionLabel.setText("Description: ");
        final FormData dlFormData = new FormData();
        dlFormData.left = new FormAttachment(labelLeftAttach, labelLeftOffset);
        dlFormData.top = new FormAttachment(this.moduleLabel, vOffset);
        dlFormData.right = new FormAttachment(labelRightAttach);
        this.descriptionLabel.setLayoutData(dlFormData);

        this.descriptionScrollComposite = new ScrolledComposite(
                this.cmdComposite, SWT.V_SCROLL | SWT.BORDER);
        this.descriptionScrollComposite.setLayout(new FormLayout());
        this.descriptionScrollComposite.setExpandHorizontal(true);
        this.descriptionScrollComposite.setExpandVertical(true);
        final FormData dscFormData = SWTUtilities.getFormData(
                this.descriptionScrollComposite, 4, 10);
        dscFormData.left = new FormAttachment(textLeftAttach);
        dscFormData.top = new FormAttachment(this.descriptionLabel, 0, SWT.TOP);
        dscFormData.right = new FormAttachment(textRightAttach, textRightOffset);
        this.descriptionScrollComposite.setLayoutData(dscFormData);

        final Label l = new Label(this.descriptionScrollComposite, SWT.LEFT
                | SWT.WRAP | SWT.HORIZONTAL);
        final String desc = command.getDefinition().getDescription();
        l.setText(desc != null ? desc.trim() : "");
        final FormData lFormData = new FormData();
        lFormData.width = dscFormData.width;
        lFormData.top = new FormAttachment(0);
        lFormData.bottom = new FormAttachment(100);
        lFormData.left = new FormAttachment(0);
        lFormData.right = new FormAttachment(100);
        l.setLayoutData(lFormData);
        this.descriptionScrollComposite.setContent(l);
        this.descriptionScrollComposite.setMinSize(l.computeSize(
                dscFormData.width, SWT.DEFAULT, true));

        if (command.getArgumentCount() > 0) {
            displayArguments(command);
        }

        this.clearButton.setEnabled(true);
        this.previewButton.setEnabled(true);
        this.validateButton.setEnabled(true);

        if (this.isStandalone) {
            this.sendButton.setEnabled(true);
        }

        this.rightPanel.layout(true);
    }

    /**
     * Display the argument entry fields for a particular command
     * 
     * @param command the command being displayed that in needs its arguments to be displayed
     *
     * Changed data type of "args" to
     * use ICommandArgumentDefinition interface.
     */
    protected void displayArguments(final IFlightCommand command) {
        final FontData calFontData = new FontData("Helvetica", 12, SWT.BOLD);
        final Font calFont = new Font(getDisplay(), calFontData);
        final Label cmdArgLabel = new Label(this.cmdComposite, SWT.LEFT);
        cmdArgLabel.setFont(calFont);
        final FormData calFormData = new FormData();
        calFormData.left = new FormAttachment(0, 5);
        calFormData.top = new FormAttachment(this.descriptionScrollComposite,
                10);
        calFormData.right = new FormAttachment(50);
        cmdArgLabel.setLayoutData(calFormData);
        cmdArgLabel.setText("Command Arguments");

        this.argScrollComposite = new ScrolledComposite(this.cmdComposite,
                SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
        this.cmdComposite.setTabList(new Control[] { this.opcodeText,
                this.argScrollComposite });
        final FormData ascFormData = new FormData();
        ascFormData.left = new FormAttachment(0);
        ascFormData.top = new FormAttachment(cmdArgLabel, 10);
        ascFormData.right = new FormAttachment(100);
        ascFormData.bottom = new FormAttachment(100);
        this.argScrollComposite.setLayoutData(ascFormData);
        this.argScrollComposite.setLayout(new FillLayout());
        this.argScrollComposite.setExpandHorizontal(true);
        this.argScrollComposite.setExpandVertical(true);
        this.argComposite = new Composite(this.argScrollComposite, SWT.NONE);
        this.argScrollComposite.setTabList(new Control[] { this.argComposite });
        this.argScrollComposite.setContent(this.argComposite);

        final FormLayout acLayout = new FormLayout();
        acLayout.marginTop = 0;
        acLayout.marginBottom = 0;
        acLayout.marginLeft = 5;
        acLayout.marginRight = 5;
        this.argComposite.setLayout(acLayout);

        /* Moved code to construct command argument gui elements to a function */
        updateCommandBuilderArgumentComposite(command);
    }
    
    /**
     * Updates the command builder's argument composite with a new FlightCommand 
     * 
     * @param command FlightCommand to display arguments for
     */
    private void updateCommandBuilderArgumentComposite(final IFlightCommand command) {
        this.argPrefixFields = new Label[command.getArgumentCount()];
        this.argValueFields = new Control[command.getArgumentCount()];
        this.argSuffixFields = new Control[command.getArgumentCount()];

        for (int i = 0; i < command.getArgumentCount(); i++) {


            /*
             * Use new GUI control object factory
             * for the following rather than the argument class directly.
             */
            final ICommandArgumentGuiControl guiControl = ArgumentGuiControlFactory.getGuiControl(command, i);

            this.argPrefixFields[i] = guiControl
                    .createPrefixControl(this.argComposite);
            this.argSuffixFields[i] = guiControl
                    .createSuffixControl(this.argComposite);
            this.argValueFields[i] = guiControl.createArgumentValueControl(
                    this.argComposite, getTextFieldFont());

            // set backround color on paint
            Control control = argValueFields[i];
            if(control instanceof Combo) {
                control.addListener(SWT.Paint, e -> {
                    control.setBackground(getDisplay().getSystemColor(SWT.COLOR_WHITE));
                });
            }

            if (this.argSuffixFields[i] instanceof Button) {
                ((Button) this.argSuffixFields[i])
				.addSelectionListener(this.handler);
            }
            if (this.argValueFields[i] instanceof Combo) {
            		// Adds modify listener to an EnumeratedArgumentment combo box (drop-down)
                ((Combo)this.argValueFields[i]).addModifyListener(handler);
                ((Combo)this.argValueFields[i]).addSelectionListener(handler);
            } else if (this.argValueFields[i] instanceof Text && !command.getArgumentType(i).isFill()) { 
            		// Adds modify listener to non-enum command arguments
            		((Text)this.argValueFields[i]).addModifyListener(handler);
            }

            final FormData prefixFormData = new FormData();
            prefixFormData.left = new FormAttachment(0);
            prefixFormData.right = new FormAttachment(25);
            prefixFormData.top = new FormAttachment(this.argValueFields[i], 0,
                    SWT.CENTER);

            final FormData suffixFormData = new FormData();
            suffixFormData.left = new FormAttachment(this.argValueFields[i], 10);
            suffixFormData.right = new FormAttachment(100);
            suffixFormData.top = new FormAttachment(this.argValueFields[i], 0,
                    SWT.CENTER);

            final FormData valueFormData = (FormData) this.argValueFields[i]
                    .getLayoutData();
            valueFormData.left = new FormAttachment(this.argPrefixFields[i], 5);

            if (i == 0) {
                valueFormData.top = new FormAttachment(0, 15);
            } else {
                valueFormData.top = new FormAttachment(
                        this.argValueFields[i - 1], 15);
            }

            // make it so we don't run off the bottom of the arg composite
            if (i == (command.getArgumentCount() - 1)) {
                prefixFormData.bottom = new FormAttachment(100, -15);
            }

            this.argPrefixFields[i].setLayoutData(prefixFormData);
            this.argValueFields[i].setLayoutData(valueFormData);
            this.argSuffixFields[i].setLayoutData(suffixFormData);

            this.argValueFields[i].addFocusListener(this.handler);
            this.argValueFields[i].addMouseListener(this.handler);
        }

        this.argComposite.setTabList(this.argValueFields);

        // make the scrollbars appear on the scrolled composite when we need
        // them to...believe me, you
        // really don't want to remove this line
        this.argScrollComposite.setMinSize(this.argComposite.computeSize(
                SWT.DEFAULT, SWT.DEFAULT, true));
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.shared.swt.ChillShell#wasCanceled()
     */
    public boolean wasCanceled() {
        return (this.canceled);
    }

    /**
     * Look at all the user-entered values for the various command arguments and
     * validate them against their allowable values in the dictionary.
     * 
     * @return True if all the user input passes validation, false otherwise
     *
     * Set text color  for validation since seting background does not work on RedHat
     */
    protected boolean doValidation() {
        final IFlightCommand currentCommand = getSelectedCommand();
        if (currentCommand == null) {
            throw new IllegalStateException(NULL_SELECTED_COMMAND);
        }

        boolean cmdValid;

        // validate the opcode (GUI must still be hex)
        try {
            final String guiOpcodeStr = GDR.removeWhitespaceFromString(
                                      this.opcodeText.getText());
            final String cmdOpcodeStr =
                GDR.removeWhitespaceFromString(
                    currentCommand.getEnteredOpcode());

            if (!guiOpcodeStr.equalsIgnoreCase("null")
                    && !cmdOpcodeStr.equalsIgnoreCase("null")) {
                final long guiOpcode = GDR.parse_int(guiOpcodeStr);
                final long cmdOpcode = GDR.parse_int(
                                     OpcodeUtil.addHexPrefix1(cmdOpcodeStr));

                /** Insist that GUI opcode be in hex */
                if (! OpcodeUtil.hasHexPrefix(guiOpcodeStr) ||
                    (guiOpcode != cmdOpcode)) {
                    cmdValid = false;

                    opcodeText.setForeground(errorColor);
                    this.opcodeText.setFocus();
                } else {
                    cmdValid = true;

                    opcodeText.setForeground(okColor);
                }
            } else {
                cmdValid = false;

                opcodeText.setForeground(errorColor);
                this.opcodeText.setFocus();
            }
        }
        catch (final IllegalArgumentException iae) {
            /**
             *  parse_int can throw more general exception
             * than NumberFormatException
             */

            cmdValid = false;

            opcodeText.setForeground(errorColor);
            this.opcodeText.setFocus();
        }

        // validate each of the arguments
        final int argCount = currentCommand.getArgumentCount();
        for (int i = 0; i < argCount; i++) {
            /*
             *  Cast interface type back to internal
             * command argument type.
             */
            final Control valueControl = this.argValueFields[i];

            String value = null;
            if (valueControl instanceof Text) {
                value = ((Text) valueControl).getText().trim();
            } else if (valueControl instanceof Combo) {
                value = ((Combo) valueControl).getText().trim();
            }

            boolean argValid = true;
            final boolean oldValidate = commandProperties
                    .getValidateCommands();
            commandProperties.setValidateCommands(true);
            try {
                UplinkInputParser.parseAndSetGuiArgument(appContext, currentCommand, i, value);
            } catch (final Exception e) {
                argValid = false;
            } finally {
                commandProperties.setValidateCommands(
                        oldValidate);
            }
            this.argValueFields[i].setBackground(argValid ? this.okColor
                    : this.errorColor);

            /**
                // Set text color  for validation since
                // setting background does not work on RedHat
             * black seems to always change to white when you hover over
             * the field.
             */
                argValueFields[i].setForeground(argValid ? this.okColor : this.errorColor);

            // this is the first error we've found
            if (cmdValid && !argValid) {
                this.argValueFields[i].setFocus();
            }

            cmdValid = cmdValid && argValid;
        }

        return (cmdValid);
    }

    /**
     * Retrieve a reference to the selected command
     * 
     * @return The command object corresponding to the user's selection in the
     *         stem list
     */
    protected IFlightCommand getSelectedCommand() {
        int selection = stemList.getSelectionIndex();
        if (selection == -1) {
            /*
             * Users have discovered a strange bug where sometimes the
             * right-hand pane of this GUI is displaying a command's details,
             * but the left hand side of the pane does not have a command
             * selected in the list of stems. When that happens, we have to take
             * the Stem from the right hand pane and try to find and select the
             * necessary stem in the left hand pane. (brn)
             */
            if (this.stemText != null) {
                final String stem = this.stemText.getText();
                if (stem.trim().length() > 0) {
                    final String[] listStems = this.stemList.getItems();
                    for (int i = 0; i < listStems.length; i++) {
                        if (stem.equals(listStems[i])) {
                            selection = i;
                            this.stemList.setSelection(i);
                            break;
                        }
                    }
                }
            }

            if (selection == -1) {
                return (null);
            }
        }

        final String stem = this.stemList.getItem(selection);
        IFlightCommand currentCommand = commandMap.get(stem);

        if (currentCommand == null) {
        	final ICommandDefinition currentCommandDef = this.commandDict
                .getCommandDefinitionForStem(stem);

        	try {
        		currentCommand = commandFactory.createFlightCommand(currentCommandDef);
        		currentCommand.clearArgumentValues();

        		commandMap.put(stem, currentCommand);
        	} catch (final IllegalArgumentException e) {
        		// command is blacklisted
        		trace.warn(e.getMessage());
        		destroyCommandComposite();
        	}
        }
    
		this.rightPanel.setText(currentCommand == null ? "Command Information (BLACKLISTED)" : "Command Information");

        return (currentCommand);
    }

    /**
     * Clears the stem search entry widget and 
     * attempts to set the selection to the given stem.
     * 
     * @param stem command stem to select
     */
    public void clearStemSearchCriteria(final String stem) {
        searchText.setText("");

        if (stem != null) {
            if (stem.trim().length() > 0) {
                final String[] listStems = this.stemList.getItems();
                for (int i = 0; i < listStems.length; i++) {
                    if (stem.equals(listStems[i])) {
                        this.stemList.setSelection(i);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Take all of the user input in the various fields on the GUI and save them
     * into a command object.
     * 
     * @return A command object with the displayed command populated with all of
     *         the user-entered values.
     * 
     * @throws CommandParseException If there was an error interpreting the
     *         values input by the user.
     */
    protected IFlightCommand saveCurrentCommand()
            throws CommandParseException {
        final IFlightCommand selected = getSelectedCommand();
        if (selected == null) {
            throw new IllegalStateException(NULL_SELECTED_COMMAND);
        }

        final IFlightCommand command = selected.copy();

        boolean errorsFound = false;
        final StringBuilder errors = new StringBuilder(1024);

        String guiOpcode;
        try {
            guiOpcode = GDR.removeWhitespaceFromString(this.opcodeText
                    .getText());

            if (guiOpcode.length() == 0) {
                throw new IllegalArgumentException("You must supply an opcode.");
            } else if ("NULL".equalsIgnoreCase(guiOpcode) == true) {
                throw new IllegalArgumentException(
                        "The keyword \"NULL\" is reserved for unimplemented commands.");
            }

            command.setEnteredOpcode(guiOpcode);
        } catch (final Exception e) {
            errorsFound = true;
            errors.append("Opcode\n");
        }

        if (this.argValueFields != null) {
            for (int i = 0; i < this.argValueFields.length; i++) {
                final Control control = this.argValueFields[i];

                String value = null;
                try {
                    if (control instanceof Text) {
                        value = ((Text) control).getText();
                    } else if (control instanceof Combo) {
                        value = ((Combo) control).getText();
                    }

                    if (value == null) {
                        throw new IllegalArgumentException(
                                "You must supply a value.");
                    }

                    value = value.trim();
                    UplinkInputParser.parseArgument(appContext, command, i, value);
                } catch (final Exception e) {
                    errorsFound = true;
                    errors.append(this.argPrefixFields[i].getText());
                    errors.append("\n");
                }
            }
        }

        if (errorsFound) {
            throw new CommandParseException(
                    "The following command fields contain unusable values"
                            + "\n(Ensure that you entered valid characters and that the value fits within "
                            + "the bit size allotted to the argument):\n\n"
                            + errors.toString());
        }

        return (command);
    }

    /**
     * Generate a string representation of the command with all the argument
     * values entered by the user.
     * 
     * @return A String containing a command stem and all the associated
     *         arguments
     */
    protected String getCommandString() {
        if (this.opcodeText == null) {
            throw new IllegalStateException("No command is currently selected.");
        }

        final StringBuilder commandString = new StringBuilder(1024);

        final IFlightCommand currentCommand = getSelectedCommand();
        if (currentCommand == null) {
            throw new IllegalStateException(NULL_SELECTED_COMMAND);
        }


        commandString.append("'");
        commandString.append(currentCommand.getDefinition().getStem());

        // there won't be any arg value fields if there are no arguments for the
        // command
        if (this.argValueFields != null) {
            for (int i = 0; i < this.argValueFields.length; i++) {
                /*
                 * Cast the interface type to the
                 * internal type.
                 */
                if (!currentCommand.isUserEntered(i)) {
                    continue;
                }

                final Control control = this.argValueFields[i];

                String value = null;
                if (control instanceof Text) {
                    value = ((Text) control).getText().trim();
                } else if (control instanceof Combo) {
                    value = ((Combo) control).getText().trim();
                }

                // we don't want to unconditionally quote string args...what if
                // bin/hex was specified
                // or the value was quoted already?
                final boolean addQuotes = currentCommand.getArgumentType(i) == CommandArgumentType.VAR_STRING
                        && ! OpcodeUtil.hasBinaryPrefix(value)
                        && ! OpcodeUtil.hasHexPrefix(value)
                        && ! StringUtil.isQuoted(value);

                commandString.append(",");

                if (addQuotes) {
                    commandString.append("\"");
                }

                commandString.append(value);

                if (addQuotes) {
                    commandString.append("\"");
                }
            }
        }

        commandString.append("'");

        return (commandString.toString());
    }

    /**
     * Search the command list
     * 
     * @param searchText the search query
     */
    protected void doSearch(String searchText) {
        searchText = searchText.toUpperCase();
        this.stemList.removeAll();

        final int selection = this.searchByCombo.getSelectionIndex();
        final String searchByValue = this.searchByCombo.getItem(selection);

        final ArrayList<String> newStemList = new ArrayList<String>(1024);
        if ("stem".equalsIgnoreCase(searchByValue)) {
            for (final String stem : this.stems) {
                final String testStem = stem.toUpperCase();
                if (!testStem.contains(searchText)) {
                    continue;
                }

                newStemList.add(stem);
            }
        } else if ("module".equalsIgnoreCase(searchByValue)) {
            for (final String stem : this.stems) {
                final ICommandDefinition cmd = this.commandDict
                        .getCommandDefinitionForStem(stem);
                /* New call to categories. */
                final String module = cmd.getCategory(ICommandDefinition.MODULE).toUpperCase();
                if (!module.contains(searchText)) {
                    continue;
                }

                newStemList.add(stem);
            }
        }

        this.stemList.setItems(newStemList.toArray(new String[] {}));
        if (!newStemList.isEmpty()) {
            this.stemList.setSelection(0);
            displaySelectedCommand();
        } else {
            destroyCommandComposite();
        }
    }

    /**
     * Destroy the current command composite to prepare to build a new one
     */
    protected void destroyCommandComposite() {
        if (this.cmdComposite != null) {
            this.cmdComposite.dispose();
        }

        this.cmdComposite = null;
        this.argScrollComposite = null;
        this.argComposite = null;
        this.stemLabel = null;
        this.stemText = null;
        this.opcodeLabel = null;
        this.opcodeText = null;
        this.moduleLabel = null;
        this.moduleText = null;
        this.descriptionLabel = null;
        this.descriptionText = null;
        this.argPrefixFields = null;
        this.argValueFields = null;
        this.argSuffixFields = null;

        //  DO NOT CLEAR COMMAND MAP HERE
        
        this.clearButton.setEnabled(false);
        this.previewButton.setEnabled(false);
        this.validateButton.setEnabled(false);

        if (this.isStandalone) {
            this.sendButton.setEnabled(false);
        }
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.tcapp.app.gui.fault.FaultInjectorGuiComponent#getDescription()
     */
    @Override
    public String getDescription() {
        return (FAULT_PAGE_DESC);
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.tcapp.app.gui.fault.FaultInjectorGuiComponent#getTitle()
     */
    @Override
    public String getTitle() {
        return (FAULT_PAGE_TITLE);
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.tcapp.app.gui.fault.FaultInjectorGuiComponent#getCurrentState()
     */
    @Override
    public FaultInjectionState getCurrentState() {
        return (this.dataState);
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.tcapp.app.gui.fault.FaultInjectorGuiComponent#setFromState(jpl.gds.tcapp.app.gui.fault.FaultInjectionState)
     */
    @Override
    public void setFromState(final FaultInjectionState state) {
        this.dataState = state;

        /*
         *  Added same logic that's present in setFieldsFromTransmitHistory.
         * This initializes the displayed command with all of the proper values if the Back button is
         * used in Fault Injection Wizard to go back to command builder page (page #1).
         */
        if (dataState.command != null) {
            final String stem = dataState.command.getDefinition().getStem();
            this.stemList.setSelection(new String[]{stem});
            this.stemList.showSelection();
            this.commandMap.put(stem, dataState.command);
            this.displayCommand(dataState.command);
        }
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.tcapp.app.gui.fault.FaultInjectorGuiComponent#updateState()
     */
    @Override
    public void updateState() throws FaultInjectorException {
    	
        final boolean oldValue = this.commandProperties.getValidateCommands();
        commandProperties.setValidateCommands(false);
        try {
            this.dataState.command = saveCurrentCommand();
        } catch (final CommandParseException e) {
            throw new FaultInjectorException(e.getMessage(), e);
        } finally {
            commandProperties.setValidateCommands(oldValue);
        }

        this.dataState.frames = null;
        this.dataState.selectedFrameIndex = -1;
        this.dataState.cltus = null;
        this.dataState.selectedCltuIndex = -1;
        this.dataState.rawOutputHex = null;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.tcapp.app.gui.fault.FaultInjectorGuiComponent#destroy()
     */
    @Override
    public void destroy() {
        dispose();
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.tcapp.app.gui.fault.FaultInjectorGuiComponent#updateDisplay()
     */
    @Override
    public void updateDisplay() {
        if (this.dataState.command != null) {
            this.stemList.setSelection(this.stemList
                    .indexOf(this.dataState.command.getDefinition().getStem()));
            this.stemList.getFocusIndex();
            displayCommand(this.dataState.command);
        } else {
            this.stemList.setSelection(0);
            displaySelectedCommand();
        }
        this.stemList.showSelection();
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.tcapp.app.gui.fault.FaultInjectorGuiComponent#getTextFieldFont()
     */
    @Override
    public Font getTextFieldFont() {
        return (new Font(getDisplay(), new FontData(FONT_NAME, 14, SWT.NONE)));
    }

    /**
     * Indicates if this command builder composite is standalone (true) or part
     * of fault injector (false)
     * 
     * @return the isStandalone
     */
    public boolean isStandalone() {
        return isStandalone;
    }

    /**
     * When we actually click the "send" button, the argument values for the
     * command get stored in that command so that the user doesn't have to
     * re-type them in every time, but if they close the command builder and
     * reopen it, the values should not be stored any longer. This function
     * wipes out all stored argument values.
     * 
     */
    //    protected void clearDictionaryHistory() {
    //        for (ICommandDefinition command : this.commandDict.getCommands()) {
    //            command.clearArgumentValues();
    //        }
    //    }

    /**
     * Command Builder event handler
     * 
     */
    public class EventHandler implements ModifyListener, SelectionListener,FocusListener, MouseListener {
        private final Composite parent;
        
        private final AtomicBoolean handlingArgRequest = new AtomicBoolean(false);

        /**
         * Constructor
         * 
         * @param parent the parent composite
         */
        protected EventHandler(final Composite parent) {
            this.parent = parent;
        }
		
        // broke out into a separate function.
		private void handleArgUpdate(final TypedEvent event) {
			trace.debug("START ARG UPDATE");
		 // Moved the logic below from modify event to keyRelease
            // SWT is holding modify events for user input while the drop-down list is visible
            
            // Added typing/search update support for ENUM command args drop-down list
            if (event.getSource() instanceof Combo && CommandBuilderComposite.this.argValueFields != null) {
                // Find the selected combo box and determine the argument index
                final int argPosition = CommandArgumentGuiHelper.getSelectedCmdArgument(event, argValueFields);
                final Combo selectedCombo = argPosition == -1 ? null : (Combo) argValueFields[argPosition];
                final String userInput = selectedCombo != null ? selectedCombo.getText() : null;
                final boolean noInput = (userInput == null || userInput.isEmpty());
                final Point originalPos = CommandArgumentGuiHelper.getUserCaret(argValueFields[argPosition]);
                
                if (noInput) {
                    // User deleted text and/or the search box is empty. Rebuild command composite
                    // Update this command argument to be nothing as well
                    // case: select valid enum, then try to delete text
                    // make sure it doesnt set the previous selected enum
                    clearArgAndRefreshGui(argPosition, true);
                }

                if (argPosition != -1 && !noInput
                        && !selectedCombo.isDisposed() && selectedCombo.getSelectionIndex() != -1) {
                    // Best case; we have a valid selection.
                    // Do nothing and a SelectionEvent will be triggered
                    // Command and arguments will update in the selection event handling
                } 
                else if (argPosition != -1 && !noInput) {
                    final IFlightCommand cmd = getSelectedCommand();
                    // User has not yet entered a valid enum yet, update the valid arguments drop-down
                    refreshCmdArgItems(argPosition);
                    
                    // Translate user input text into the enum definition
                    final String argText = CommandArgumentGuiHelper.getArgumentText(selectedCombo);

					// Trims drop-down to only enums that contain user input while searching
					final boolean matched = CommandArgumentGuiHelper.updateAndSearchCmdArg(appContext, selectedCombo, cmd, argPosition);

					// Save list of filtered drop-down items
					final String[] comboItems = selectedCombo.getItems();
					
					// If user input text matched a drop-down item, parse this command arg
					if (matched) {
						CommandArgumentGuiHelper.parseCmdArgument(appContext, argPosition, argText, cmd);
					}
 					
					// Get current argument value from definition and compare against user input
					final String currentArg = cmd.getArgumentValue(argPosition);
					final boolean definitionMismatch = currentArg != null 
									&& !"".equals(currentArg) 
									&& !currentArg.equalsIgnoreCase(userInput);
					
					if (definitionMismatch) { 
						// Arg value def doesnt match user input
						// Clear this argument an any additional args that relied on this
						// Then refresh available enum drop-down items
						cmd.clearArgumentValue(argPosition);
						refreshCmdArgItems(argPosition);
					}
					displaySelectedCommand(); // Refresh GUI
					
					// Set filtered drop-down list and text after refresh 
					((Combo) argValueFields[argPosition]).setItems(comboItems);
					
					// Need to set the text box back to what user input was
					// If there was a match, we set the definition representation of user input (argText)
					// E.g.) If user types "acm", argText will reflect the actual enum definition "ACM"
					if (matched) { 
						((Combo) argValueFields[argPosition]).setText(argText);
					} else { 
						((Combo) argValueFields[argPosition]).setText(userInput);
					}

				}
				

                // Sets mouse caret position and forces focus back to control that triggered event
                if (argPosition != -1) { 
                    CommandArgumentGuiHelper.setOriginalCaret(argValueFields[argPosition],originalPos);
                }
            } else if (event.getSource() instanceof Text && argValueFields != null) {
                // This allows saving the state of a non-enum command args (Text)
                // Parse and set current user input for this field when the user types something
                final int argPosition = CommandArgumentGuiHelper.getSelectedCmdArgument(event, argValueFields);
                final Text selectedText = argPosition == -1 ? null : (Text) argValueFields[argPosition];
                final String userInput = selectedText != null ? selectedText.getText() : ""; 
                final Point originalPos = CommandArgumentGuiHelper.getUserCaret(argValueFields[argPosition]); 

                // Basically, everytime the user types something in a non-enum field
                // we are trying to parse that text, regardless of what it is, as the command argument value
                // The exceptions below occur when that argument is invalid, we hide them to prevent bombarding 
                // with error messages. When user tries to send/validate, let that display any errors for text fields
                if (argPosition != -1) {
                    // Added extra handling for binary/hex values
                    // Also removed the GUI refresh after parsing b/c other args are not updated from non-Combos fields
                    final boolean goodInput = CommandArgumentGuiHelper.isValidGuiInput(userInput);
                    
                    if (goodInput) { 
                        try {
                            UplinkInputParser.parseAndSetGuiArgument(appContext, getSelectedCommand(), argPosition, userInput);
                        } catch (NullPointerException | CommandParseException | IllegalStateException | IllegalArgumentException e) {
                            // Do nothing. Hide this exception from the user
                        }
                    } 
                    CommandArgumentGuiHelper.setOriginalCaret(argValueFields[argPosition],originalPos);
                }
            } 
            trace.debug("END ARG UPDATE");
		}

        /**
         * {@inheritDoc}
         * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
         */
        @Override
        public void widgetSelected(final SelectionEvent se) {
        	trace.debug("START WIDGET SELECTED ");
            try {
                if (se.getSource() == CommandBuilderComposite.this.exitButton) {
                    CommandBuilderComposite.this.canceled = true;
                    // clearDictionaryHistory();
                    this.parent.getShell().close();
                } else if (se.getSource() == CommandBuilderComposite.this.previewButton) {
                    displayPreview();
                    opcodeText.forceFocus();
                } else if (se.getSource() == CommandBuilderComposite.this.clearButton) {
                		commandMap.clear();
                    destroyCommandComposite();
                    displaySelectedCommand();
                    opcodeText.forceFocus();
                } else if (se.getSource() == CommandBuilderComposite.this.validateButton) {
                    final boolean valid = doValidation();
                    displayValidationResult(valid);
                    opcodeText.forceFocus();
                } else if (se.getSource() == CommandBuilderComposite.this.stemList) {
                    displaySelectedCommand();
                } else if (se.getSource() == CommandBuilderComposite.this.searchByCombo) {
                    CommandBuilderComposite.this.searchText.setText("");
                    doSearch("");
                } else if (se.getSource() instanceof Combo && CommandBuilderComposite.this.argValueFields != null) {
                	trace.debug("Combo was selected");
                		// Get command argument position and text 
                    final int argPosition = CommandArgumentGuiHelper.getSelectedCmdArgument(se, argValueFields);
                    final String argTxt = (argValueFields[argPosition] instanceof Combo ?
                    		((Combo)argValueFields[argPosition]).getText() 
                    		: ((Text)argValueFields[argPosition]).getText());
                    
                    // Parse and update the command argument
                    CommandArgumentGuiHelper.parseCmdArgument(appContext, argPosition, argTxt, getSelectedCommand());

                    /*
                     * commented out the displaySelectedCommand call
                     *   As of SWT 4.6.1, selecting an item from a Combo triggers a mouse down event
                     *   BEFORE this widget selected event and, more importantly, a mouse up event
                     *   AFTER. The displaySelectedCommand rebuilds the command arguments in the view,
                     *   which causes a NPE in the subsequent mouse up event.
                     *   
                     *   TODO: (Update/replace the commented out function call in order to only update the
                     *   arguments that are changed by the selection of a value from the combo and NOT replace everything!
                     */
					//displaySelectedCommand(); // Refresh GUI
					
					// Set focus back on control after refresh
					argValueFields[argPosition].forceFocus();
					trace.debug("Done with combo selected");
                }

                if (argSuffixFields != null) {
                    final int index = Arrays.asList(argSuffixFields).indexOf(se.getSource());
                    if (index != -1) {
                        final IFlightCommand cmd = getSelectedCommand();
                        if (cmd == null) {
                            throw new IllegalStateException(
                                    NULL_SELECTED_COMMAND);
                        }
                        displayArgumentEditor(cmd, index, argValueFields[index]);
                    }
                }
            } catch (final Exception e) {
            		trace.error("Encountered unexpected exception: " 
            				+ (e.getCause() != null ? e.getCause() : e.getMessage()), e);
                return;
            }
            trace.debug("END WIDGET SELECTED");
        }

        /**
         * Clears the command argument value and refreshed the GUI
         * 
         * @param position of the command argument to clear 
         * @param refreshCombo whether or not to refresh/update drop-down list (ENUM only)
         */
        private void clearArgAndRefreshGui(final int position, final boolean refreshCombo) { 
        	trace.debug("START CLEAR ARG & REFRESH GUI");
        	// User deleted text and/or the search box is empty. Rebuild command composite
        	// Update this command argument to be nothing as well
        	// case: select valid enum, then try to delete text
        	// make sure it doesnt set the previous selected enum

        	// set argument value to "" to prevent defalult value from populating in gui
        	getSelectedCommand().clearArgumentValue(position);
        	getSelectedCommand().setArgumentValue(position, "");
        	if (refreshCombo) { 
        		refreshCmdArgItems(position);
        	}
        	displaySelectedCommand();
        	trace.debug("END CLEAR ARG & REFRESH GUI");
        }
        
		/**
		 * {@inheritDoc}
		 * 
		 * @see org.eclipse.swt.events.ModifyListener#modifyText(org.eclipse.swt.events.ModifyEvent)
		 */
		@Override
		public void modifyText(final ModifyEvent me) {
			trace.debug("START MODIFY EVENT");
			try {
				if (me.getSource() == CommandBuilderComposite.this.searchText) {
					doSearch(CommandBuilderComposite.this.searchText.getText().trim());
				}
				else if (CommandBuilderComposite.this.argValueFields != null && Arrays.asList(CommandBuilderComposite.this.argValueFields).contains(me.getSource())) {
				    /*
				     * Unless we direct otherwise, the main thread handles our extension of the ModifyEvent. When the first ones is almost done calls a set text on the
				     *  new version of the field, which unfortunately with our current design calls another modify event and starts an infinite loop/stack overflow.
				     *  We don't want to do this call even once inside of itself so if this function is already in use, just ignore the second call.
				     */
				    if(handlingArgRequest.compareAndSet(false, true)) {
				        handleArgUpdate(me);
				        handlingArgRequest.set(false);
				    }
				}
			} catch (final Exception e) {
				trace.error("Encountered unexpected exception: " 
						+ (e.getCause() != null ? e.getCause() : e.getMessage()), e);
				return;
			}
			trace.debug("END MODIFY EVENT");
		}
		
		
        
		/**
		 * Re-creates the selected command's argument ENUM drop-down
		 * 
		 * @param position command argument position 
		 */
		private void refreshCmdArgItems(final int position) {
			trace.debug("REFRESH CMD ARG ITEMS START");
			if (!(argValueFields[position] instanceof Combo)) {
				return;
			}
			// First remove old entries. We have to iterate over the items for this.
			// This is done to correct any out of order elements in the drop-down list
			// removeAll() doesnt work because it triggers another event.

			// setting items to empty String[] will also not work.
			// Also, Trying to sort the current items array causes stack overflow (why?)
			
			trace.debug("Removing items from display...");
			for (final String s : ((Combo) argValueFields[position]).getItems()) {
				((Combo) argValueFields[position]).remove(s);
			}
			trace.debug("Removed");

			final IFlightCommand cmd = getSelectedCommand();
			// Now reconstruct command argument enum drop-down
			trace.debug("Inserting new items for display...");
			for (int i = 0; i < cmd.getArgumentDefinition(position).getEnumeration().getEnumerationValues()
					.size(); ++i) {
				final String val = cmd.getArgumentDefinition(position).getEnumeration().getEnumerationValues().get(i)
						.getDictionaryValue();
				((Combo) argValueFields[position]).add(val);
			}
			trace.debug("Inserted");
			trace.debug("REFRESH CMD ARG ITEMS END");
		}
        
        /**
         * Show validation results
         * 
         * @param result the validation results to show
         */
        protected void displayValidationResult(final boolean result) {
            if (result) {
                SWTUtilities.showMessageDialog(this.parent.getShell(),
                        "Validation Succeeded",
                        "All of the entered values are valid.");
            } else {
                SWTUtilities.showWarningDialog(this.parent.getShell(),
                        "Validation Failed",
                        "Fields with red font color contain invalid values.");
            }
        }

        /**
         * Show command preview
         */
        protected void displayPreview() {
            final Shell previewShell = new Shell(this.parent.getShell(),
                    SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.RESIZE);
            previewShell.setLocation(getLocation().x + getSize().x / 3,
                    getLocation().y + getSize().y / 3);
            previewShell.setSize(400, 100);
            previewShell.setText("Command Preview");
            final FillLayout fl = new FillLayout();
            fl.type = SWT.VERTICAL;
            fl.spacing = 10;
            fl.marginHeight = 10;
            fl.marginWidth = 10;
            previewShell.setLayout(fl);
            final Text cmdText = new Text(previewShell, SWT.BORDER | SWT.MULTI
                    | SWT.WRAP | SWT.V_SCROLL);
            cmdText.setFont(getTextFieldFont());
            cmdText.setText(getCommandString());
            cmdText.setEditable(true);
            cmdText.selectAll();
            previewShell.layout(true);
            previewShell.open();
        }

        /**
         * Show argument editor
         * 
         * @param cmd the command to have an argument displayed
         * @param index the index of the argument to be displayed
         * @param argControl the argument's widget
         */
        protected void displayArgumentEditor(final IFlightCommand cmd, final int index,
                final Control argControl) {
            
            if(cmd.getArgumentType(index).equals(CommandArgumentType.REPEAT)) {
                displayRepeatArgumentEditor(cmd, index, argControl);
            }
            else if(cmd.getArgumentType(index).equals(CommandArgumentType.BITMASK)) {
                displayBitmaskArgumentEditor(cmd, index, argControl);
            }
            
        }
        
        protected void displayRepeatArgumentEditor(final IFlightCommand cmd, final int index,
                final Control argControl) {
            if (cmd.getArgumentType(index) != CommandArgumentType.REPEAT) {
                return;
            }

            final Text textField = (Text) argControl;

            Shell editorShell = new Shell(this.parent.getShell(),
                    SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.RESIZE);
            RepeatArgumentComposite aac;
            try {
                aac = new RepeatArgumentComposite(appContext, editorShell,
                        (CommandBuilderComposite) this.parent, cmd, index,
                        textField.getText());
            } catch (final Exception e) {
                editorShell = new Shell(this.parent.getShell(), SWT.DIALOG_TRIM
                        | SWT.APPLICATION_MODAL | SWT.RESIZE);
                SWTUtilities
                .showErrorDialog(
                        getShell(),
                        "Editor Launch Error",
                        "Could not display the current value of the selected argument.  Displaying a blank editor instead.");
                try {
                    aac = new RepeatArgumentComposite(appContext, editorShell,
                            (CommandBuilderComposite) this.parent, cmd, index,
                            "");
                } catch (final Exception e1) {
                    SWTUtilities
                    .showErrorDialog(getShell(), "Editor Launch Error",
                            "Could not display the editor for the selected argument.");
                    return;
                }
            }

            editorShell.setLocation(getLocation().x + 50, getLocation().y + 50);
            editorShell.setSize(700, 525);
            editorShell.setText("Argument Editor");

            final FillLayout fl = new FillLayout();
            fl.type = SWT.VERTICAL;
            fl.spacing = 10;
            fl.marginHeight = 10;
            fl.marginWidth = 10;
            editorShell.setLayout(fl);

            editorShell.layout(true);
            editorShell.open();
            while (!editorShell.isDisposed()) {
                if (!editorShell.getDisplay().readAndDispatch()) {
                    editorShell.getDisplay().sleep();
                }
            }

            if (!aac.isCanceled()) {
                textField.setText(aac.getFlightCommand().getRepeatArgumentString(aac.getRepeatArgIndex()));
            }

            editorShell.dispose();
            editorShell = null;
        }
        
        protected void displayBitmaskArgumentEditor(final IFlightCommand cmd, final int index,
                final Control argControl) {
            if (cmd.getArgumentType(index) != CommandArgumentType.BITMASK) {
                return;
            }

            final Text textField = (Text) argControl;

            Shell editorShell = new Shell(this.parent.getShell(),
                    SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.RESIZE);
            BitmaskArgumentComposite bac = null;
            try {
                bac = new BitmaskArgumentComposite(appContext, editorShell,
                        (CommandBuilderComposite) this.parent, cmd, index,
                        textField.getText());
            } catch (final Exception e) {
                editorShell = new Shell(this.parent.getShell(), SWT.DIALOG_TRIM
                        | SWT.APPLICATION_MODAL | SWT.RESIZE);
                SWTUtilities
                .showErrorDialog(
                        getShell(),
                        "Editor Launch Error",
                        "Could not display the current value of the selected argument.  Displaying a blank editor instead.");
                try {
                    bac = new BitmaskArgumentComposite(appContext, editorShell,
                            (CommandBuilderComposite) this.parent, cmd, index,
                            "");
                } catch (final Exception e1) {
                    SWTUtilities
                    .showErrorDialog(getShell(), "Editor Launch Error",
                            "Could not display the editor for the selected argument.");
                    return;
                }
            }

            editorShell.setLocation(getLocation().x + 50, getLocation().y + 50);
            editorShell.setSize(700, 525);
            editorShell.setText("Argument Editor");

            final FillLayout fl = new FillLayout();
            fl.type = SWT.VERTICAL;
            fl.spacing = 10;
            fl.marginHeight = 10;
            fl.marginWidth = 10;
            editorShell.setLayout(fl);

            editorShell.layout(true);
            editorShell.open();
            while (!editorShell.isDisposed()) {
                if (!editorShell.getDisplay().readAndDispatch()) {
                    editorShell.getDisplay().sleep();
                }
            }

            if (!bac.isCanceled()) {
                textField.setText(bac.getFlightCommand().getRepeatArgumentString(bac.getArgIndex()));
            }

            editorShell.dispose();
            editorShell = null;
        }
        
        

        /**
         * {@inheritDoc}
         * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
         */
        @Override
        public void widgetDefaultSelected(final SelectionEvent se) {
            // don't care
        	trace.debug("WIDGET DEFAULT SELECTED EVENT");
        }

        /**
         * {@inheritDoc}
         * @see org.eclipse.swt.events.FocusListener#focusGained(org.eclipse.swt.events.FocusEvent)
         */
        @Override
        public void focusGained(final FocusEvent arg0) {
        	trace.debug("FOCUS GAINED START");
            final Control argValField = (Control) arg0.getSource();
            highlightFocusedField(argValField);

            // when the user is tabbing through arguments and we're
            // automatically scrolling, we actually want to keep the scrolling
            // part of
            // the arguments section with the PREVIOUS argument at the top, not
            // the CURRENT one. We were doing it with the current one before
            // (see commented line below), but users were getting lost. (brn)
            //
            // CommandBuilderComposite.this.argScrollComposite.setOrigin(0,argValField.getLocation().y);
            int fieldIndex = -1;
            for (int i = 0; i < argValueFields.length; i++) {
                if (argValueFields[i] == argValField) {
                    fieldIndex = i - 1;
                    break;
                }
            }
            
            // Use current x position instead of always setting to 0
            // prevents unnecessary horizontal scrolling when selecting drop-down with long enums
            CommandBuilderComposite.this.argScrollComposite
            .setOrigin(CommandBuilderComposite.this.argScrollComposite.getOrigin().x,
            		fieldIndex >= 0 ? 
            				argValueFields[fieldIndex].getLocation().y 
            				: argValField.getLocation().y);
            
            if (fieldIndex +1 >= 0) { 
            		argValueFields[fieldIndex + 1].setFocus();
            }
            
            trace.debug("FOCUS GAINED END");
        }

		/**
		 * {@inheritDoc}
		 * 
		 * @see org.eclipse.swt.events.FocusListener#focusLost(org.eclipse.swt.events.FocusEvent)
		 */
		@Override
		public void focusLost(final FocusEvent arg0) {
			// Do nothing
			trace.debug("FOCUS LOST EVENT");
		}

        private void highlightFocusedField(final Control argValField) {
        	trace.debug("START HILIGHT FOCUSED FIELD");
            // Highlight text in the field for easy deletion if it's a text
            // field.
            // It's the little things. (brn)
            if (argValField instanceof Text) {
                final Text textField = (Text) argValField;
                trace.debug("USING TEXT FIELD " + textField.toString());
                final String value = textField.getText();
                ((Text) argValField).setSelection(0, value.length());
                trace.debug("HILIGHTED THE TEXT " + value);
            }
            trace.debug("END HILIGHT FOCUSED FIELD ");
        }

        /**
         * {@inheritDoc}
         * @see org.eclipse.swt.events.MouseListener#mouseDoubleClick(org.eclipse.swt.events.MouseEvent)
         */
        @Override
        public void mouseDoubleClick(final MouseEvent arg0) {
            // ignore
        	trace.debug("DOUBLE CLICK EVENT");
        }

        /**
         * {@inheritDoc}
         * @see org.eclipse.swt.events.MouseListener#mouseDown(org.eclipse.swt.events.MouseEvent)
         */
        @Override
        public void mouseDown(final MouseEvent arg0) {
        	trace.debug("START MOUSE DOWN");
            final Control argValField = (Control) arg0.getSource();
            highlightFocusedField(argValField);
            trace.debug("END MOUSE DOWN");
        }

        /**
         * {@inheritDoc}
         * @see org.eclipse.swt.events.MouseListener#mouseUp(org.eclipse.swt.events.MouseEvent)
         */
        @Override
        public void mouseUp(final MouseEvent arg0) {
        	trace.debug("START MOUSE UP");
            final Control argValField = (Control) arg0.getSource();
            highlightFocusedField(argValField);
            trace.debug("END MOUSE UP");
        }
    }

    private void sendCommand() {
        final CommandBuilderState state = new CommandBuilderState();
        bus.publish(messageFactory.createClearUplinkGuiLogMessage());

        String commandString = null;
        try {
            // commandString = command.getDatabaseString(); //this is
            // causing problems with empty string arguments
            commandString = getCommandString();
            commandString = commandString.substring(1,
                    commandString.length() - 1);
            state.setCommand(commandString);

            final IFlightCommand command = saveCurrentCommand();
            
            /**
             * create copy of command for setting
             * later on TransmitEvent to preserve arguments (command that is 
             * sent gets arguments cleared)
             */
            final IFlightCommand commandCopy = saveCurrentCommand();
            
            state.setCommandDef(command);
            state.setCommandIndex(stemList.getSelectionIndex());

            final java.util.List<ICommand> commands = new ArrayList<ICommand>(
                    1);
            commands.add(command);
            if (scmfProperties.getScmfName() == null) {
            	scmfProperties.setScmfName(
                        OutputFileNameFactory.createNameForCommand(appContext, command));
            }

            UplinkExecutors.uplinkExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    final TransmitEvent event = new TransmitEvent();
                    final int transmitEventId = event.hashCode();

                    boolean success = true;
                    String eventMessage = "Command Successfully Transmitted";
                    String message = "";
                    
                    try {
                        SendCommandApp.sendCommands(appContext, commands, transmitEventId);
                    } catch (final RawOutputException e) {
                        message = "Missing output adapter for venue: "
                                + e.getMessage();
                        bus.publish(messageFactory.createUplinkGuiLogMessage(message));
                        success = false;
                        eventMessage = "Command transmission failed: "
                                + message;
                    } catch (final UplinkException e) {
                        message = e.getUplinkResponse().getDiagnosticMessage();
                        bus.publish(messageFactory.createUplinkGuiLogMessage(message));
                        success = false;

                        /** message can be null */
                        eventMessage = "Command transmission failed: " +
                                ((message != null) ? message : e.toString());
                    } finally {
                        if (!success) {
                            trace.error(eventMessage);

                            final String finalEventMessage = message;
                            SWTUtilities.safeAsyncExec(getDisplay(),
                                    "Command Builder", new Runnable() {

                                @Override
                                public void run() {
                                    SWTUtilities.showErrorDialog(
                                            getShell(),
                                            "Command Builder Error",
                                            finalEventMessage);
                                }

                            });
                        }

                        SWTUtilities.safeAsyncExec(getDisplay(),
                                "Command Builder", new Runnable() {
                            @Override
                            public void run() {
                                destroyCommandComposite();
                                CommandBuilderComposite.this.stemList
                                .deselectAll();
                                displaySelectedCommand();
                            }
                        });
                    }

                    /**
                     *  set copied command to state
                     * (original command had arguments cleared)
                     */
                    state.setCommandDef(commandCopy);
                    event.setTransmitInfo(CommandBuilderComposite.this, state, 
                    		success, eventMessage);
                    
                    notifyListeners(event);
                }
            });

        } catch (final CommandParseException e) {
            final String message = "Could not send command due to illegal formats of input values.  Please check the correctness of your inputs and try again.";
            bus.publish(messageFactory.createUplinkGuiLogMessage(message));
            final boolean success = false;
            final String eventMessage = "Command transmission failed: " + message;

            trace.error(eventMessage);

            final String finalEventMessage = eventMessage;
            SWTUtilities.safeAsyncExec(getDisplay(), "Command Builder",
                    new Runnable() {
                @Override
                public void run() {
                    SWTUtilities.showErrorDialog(getShell(),
                            "Command Builder Error", finalEventMessage);
                }

            });

            final TransmitEvent event = new TransmitEvent(
                    CommandBuilderComposite.this, state, success, eventMessage);
            notifyListeners(event);
        }
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.tcapp.app.gui.AbstractUplinkComposite#getDisplayName()
     */
    @Override
    public String getDisplayName() {
        return "Command Builder";
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.tcapp.app.gui.AbstractUplinkComposite#setFieldsFromTransmitHistory(jpl.gds.tcapp.app.gui.TransmitEvent)
     */
    @Override
    public void setFieldsFromTransmitHistory(final TransmitEvent historyItem) {
        final ISendCompositeState state = historyItem.getTransmitState();

        if (state instanceof CommandBuilderState) {
            final CommandBuilderState myState = (CommandBuilderState) state;
            if (myState.getCommand() != null) {
            		final String stem = myState.getCommandDef().getDefinition().getStem();
                this.stemList.setSelection(new String[]{stem});
                this.stemList.showSelection();
                //  Put command into map so args are correctly set
                final IFlightCommand commandToDisplay = myState.getCommandDef().copy();
                this.commandMap.put(stem, commandToDisplay);
                this.displayCommand(commandToDisplay);
            }
        }
    }

    /**
     * This class provides a representation of a command builder state
     * 
     */
    private class CommandBuilderState implements ISendCompositeState {
        private IFlightCommand commandDef;
        private int commandIndex;
        private String command;

        /**
         * Constructor
         * 
         * @param commandDef the command selected in the command builder
         */
        public void setCommandDef(final IFlightCommand commandDef) {
            this.commandDef = commandDef;
        }

        /**
         * Get the command object for the command selected in the command
         * builder
         * 
         * @return flight command object
         */
        public IFlightCommand getCommandDef() {
            return this.commandDef;
        }

        /**
         * Set the command index
         * 
         * @param index the command index
         */
        public void setCommandIndex(final int index) {
            this.commandIndex = index;
        }

        /**
         * Get the command index
         * 
         * @return the command index
         */
        public int getCommandIndex() {
            return this.commandIndex;
        }

        /**
         * Set the command
         * 
         * @param command the command to set
         */
        public void setCommand(final String command) {
            this.command = command;
        }

        /**
         * Get the command
         * 
         * @return the command
         */
        public String getCommand() {
            return command;
        }

        @Override
        public String getTransmitSummary() {
            return command;
        }
    }


    /**
     * {@inheritDoc}
     * @see jpl.gds.tcapp.app.gui.AbstractUplinkComposite#initiateSend()
     */
    @Override
    public void initiateSend() throws UplinkParseException
    {
        /**  Remove checkedResponse. Change color on validation failure. */

        if (commandProperties.getValidateCommands())
        {
            if (! doValidation())
            {
                final boolean result =
                    SWTUtilities.showConfirmDialog(
                        getShell(),
                        "Disable Command Validation",
                        "It looks like you're trying to send an invalid command. Would you like to turn off "                    +
                            "command validity checking for this MPCS uplink session?\n\n(NOTE: You can toggle command validity " +
                            "checking under the File->Configure... menu on the uplink GUI.)");

                commandProperties.setValidateCommands(!result);

                if (result) {
                	/**
                	 * User chose to ignore validation, so send
                	 */
                    sendCommand();
                }
                
            } else {
            	/**
            	 * Send command if input fields validate
            	 */
                sendCommand();            	
            }
            
        }
        /*
         * User decided not to validate the command.
         */
        else {
        	 sendCommand();
        }

        commandMap.clear();
        opcodeText.forceFocus();
    }


    /**
     * {@inheritDoc}
     * @see jpl.gds.tcapp.app.gui.AbstractUplinkComposite#needSendButton()
     */
    @Override
    public boolean needSendButton() {
        return true;
    }
}