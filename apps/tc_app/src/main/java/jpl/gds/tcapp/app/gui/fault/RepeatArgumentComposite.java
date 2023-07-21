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
package jpl.gds.tcapp.app.gui.fault;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TypedEvent;
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

import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.tc.api.command.IFlightCommand;
import jpl.gds.tc.api.command.parser.UplinkInputParser;
import jpl.gds.tc.api.config.CommandProperties;
import jpl.gds.tc.api.exception.CommandParseException;
import jpl.gds.tcapp.app.gui.fault.util.ArgumentGuiControlFactory;
import jpl.gds.tcapp.app.gui.fault.util.CommandArgumentGuiHelper;
import jpl.gds.tcapp.app.gui.fault.util.ICommandArgumentGuiControl;

/**
 * This class is a SWT composite that displays widgets that allow viewing and
 * manipulating a repeat command argument.
 * 
 *
 * 04/16/18 Changes through the whole class to work
 *          with the repeat argument and contained argument values and properties
 *          directly from commands instead of pulling the argument from the
 *          command and working with it individually.
 */
public final class RepeatArgumentComposite extends Composite {
    private final Shell parent;
    private final IFlightCommand cmd;
    private final int repeatArgIndex;
    private final int argBlockSize;
    private final CommandBuilderComposite cbComposite;
    private boolean canceled;
    private final String originalCmd;

    private List argBlockList = null;
    private Composite listButtonComposite = null;
    private Button upButton = null;
    private Button downButton = null;
    private Button newButton = null;
    private Button copyButton = null;
    private Button deleteButton = null;

    private Group rightPanel = null;
    private Composite repeatArgComposite = null;
    private Label nameLabel = null;
    private Label nameText = null;
    private Label rangeLabel = null;
    private Label rangeText = null;
    private Label numRepeatsLabel = null;
    private Text numRepeatsText = null;
    private Button numRepeatsButton = null;
    private Label cmdArgLabel = null;

    private ScrolledComposite argScrollComposite = null;
    private Composite argComposite = null;
    private Label[] argPrefixFields = null;
    private Control[] argValueFields = null;
    private Control[] argSuffixFields = null;

    private Button okButton = null;
    private Button cancelButton = null;

    private final EventHandler handler;

    private java.util.List<RepeatArgumentBlock> argBlocks = null;
    private int currentBlock = -1;
    
    private final ApplicationContext appContext;

    /**
     * Constructor
     * 
     * @param appContext the ApplicationContext in which this object is being used
     * @param parent the parent shell
     * @param cbc the command builder composite that this repeat argument will
     *        appear in
     * @param cmd the IFlightCommand containing the IRepeatCommandArgument
     * @param index the index to the IRepeatCommandArgument in the IFlightCommand
     * @param currentValue the current value
     * @throws CommandParseException if the argument cannot be parsed
     */
    public RepeatArgumentComposite(final ApplicationContext appContext, final Shell parent,
            final CommandBuilderComposite cbc, final IFlightCommand cmd, final int index,
            final String currentValue) throws CommandParseException {
        super(parent, SWT.NONE);
        
        this.appContext = appContext;
        
        final CommandProperties cmdConfig = appContext.getBean(CommandProperties.class);

        this.parent = parent;
        this.cmd = cmd;
        this.repeatArgIndex = index;
        this.argBlockSize = this.cmd.getRepeatArgumentDefinition(repeatArgIndex).getDictionaryArgumentCount(false);

        // wipe out existing values since we're just going to parse them in
        // below
        // (these lines are necessary to avoid weird arg list population errors
        // after a command has been sent once) (brn)
        this.cmd.clearArgumentValue(repeatArgIndex);
        this.cmd.setArgumentValue(index, "1");

        this.cbComposite = cbc;
        this.canceled = false;
        this.originalCmd = currentValue;
        this.argBlocks = new ArrayList<>(1);
        this.handler = new EventHandler();

        final int blockSize = this.cmd.getRepeatArgumentDefinition(repeatArgIndex).getDictionaryArgumentCount(false);
        if (currentValue.trim().length() > 0) {
            final boolean oldValue = cmdConfig.getValidateCommands();
            cmdConfig.setValidateCommands(false);
            UplinkInputParser.parseAndSetGuiArgument(appContext, cmd, repeatArgIndex, currentValue);
            cmdConfig.setValidateCommands(oldValue);


            final int repeatArgCount = this.cmd.getArgumentCount(repeatArgIndex);
            for (int i = 0 ; (i * blockSize) < repeatArgCount ; i++) {
            	final RepeatArgumentBlock temp = new RepeatArgumentBlock(i+1, i*blockSize);
            	argBlocks.add(temp);
            }
        }

        createControls();

        setTabList(new Control[] { this.argBlockList, this.rightPanel });
        layout(true);
    }

    private void createControls() {
        setLayout(new FormLayout());

        createLeftPanel();
        createRightPanel();
        createBottomPanel();

        populateBlockList();
        if (this.cmd.getArgumentCount(repeatArgIndex) > 0) {
            this.argBlockList.setSelection(0);
        }

        argBlockList.showSelection();

        displaySelectedBlock();
    }

    private void createLeftPanel() {
        final Label argBlockListLabel = new Label(this, SWT.LEFT);
        argBlockListLabel.setText("Argument Blocks: ");
        final FormData cllFormData = new FormData();
        cllFormData.top = new FormAttachment(0, 15);
        cllFormData.left = new FormAttachment(0, 10);
        cllFormData.right = new FormAttachment(35);
        argBlockListLabel.setLayoutData(cllFormData);

        this.argBlockList = new List(this, SWT.SINGLE | SWT.BORDER
                | SWT.V_SCROLL);
        final FormData clFormData = new FormData();
        clFormData.top = new FormAttachment(argBlockListLabel, 15);
        clFormData.left = new FormAttachment(0, 10);
        clFormData.right = new FormAttachment(35);
        clFormData.bottom = new FormAttachment(80);
        this.argBlockList.setLayoutData(clFormData);
        this.argBlockList.addSelectionListener(this.handler);

        this.listButtonComposite = new Composite(this, SWT.NONE);
        final FormData lbcFormData = new FormData();
        lbcFormData.top = new FormAttachment(this.argBlockList, 10);
        lbcFormData.bottom = new FormAttachment(100);
        lbcFormData.left = new FormAttachment(0, 10);
        lbcFormData.right = new FormAttachment(35);
        this.listButtonComposite.setLayout(new FormLayout());
        this.listButtonComposite.setLayoutData(lbcFormData);

        final int buttonSpacing = 5;

        this.upButton = new Button(this.listButtonComposite, SWT.PUSH);
        this.upButton.setText("Up");
        final FormData ubFormData = new FormData();
        ubFormData.top = new FormAttachment(0, buttonSpacing);
        ubFormData.bottom = new FormAttachment(50, -1 * buttonSpacing);
        ubFormData.left = new FormAttachment(6);
        ubFormData.right = new FormAttachment(47);
        this.upButton.setLayoutData(ubFormData);
        this.upButton.addSelectionListener(this.handler);

        this.downButton = new Button(this.listButtonComposite, SWT.PUSH);
        this.downButton.setText("Down");
        final FormData dbFormData = new FormData();
        dbFormData.top = new FormAttachment(0, buttonSpacing);
        dbFormData.bottom = new FormAttachment(50, -1 * buttonSpacing);
        dbFormData.left = new FormAttachment(53);
        dbFormData.right = new FormAttachment(94);
        this.downButton.setLayoutData(dbFormData);
        this.downButton.addSelectionListener(this.handler);

        this.newButton = new Button(this.listButtonComposite, SWT.PUSH);
        this.newButton.setText("New");
        final FormData nbFormData = new FormData();
        nbFormData.top = new FormAttachment(50, buttonSpacing);
        nbFormData.bottom = new FormAttachment(100, -1 * buttonSpacing);
        nbFormData.left = new FormAttachment(2);
        nbFormData.right = new FormAttachment(30);
        this.newButton.setLayoutData(nbFormData);
        this.newButton.addSelectionListener(this.handler);

        this.copyButton = new Button(this.listButtonComposite, SWT.PUSH);
        this.copyButton.setText("Copy");
        final FormData cbFormData = new FormData();
        cbFormData.top = new FormAttachment(50, buttonSpacing);
        cbFormData.bottom = new FormAttachment(100, -1 * buttonSpacing);
        cbFormData.left = new FormAttachment(30, 2);
        cbFormData.right = new FormAttachment(60);
        this.copyButton.setLayoutData(cbFormData);
        this.copyButton.addSelectionListener(this.handler);

        this.deleteButton = new Button(this.listButtonComposite, SWT.PUSH);
        this.deleteButton.setText("Delete");
        final FormData delFormData = new FormData();
        delFormData.top = new FormAttachment(50, buttonSpacing);
        delFormData.bottom = new FormAttachment(100, -1 * buttonSpacing);
        delFormData.left = new FormAttachment(60, 2);
        delFormData.right = new FormAttachment(98);
        this.deleteButton.setLayoutData(delFormData);
        this.deleteButton.addSelectionListener(this.handler);
    }

    private void createRightPanel() {
        this.rightPanel = new Group(this, SWT.NONE);
        final FontData rightFontData = new FontData("Helvetica", 14, SWT.BOLD);
        final Font rightFont = new Font(getDisplay(), rightFontData);
        this.rightPanel.setFont(rightFont);
        this.rightPanel.setText("Argument Information");
        final FormLayout rpLayout = new FormLayout();
        rpLayout.marginTop = 5;
        rpLayout.marginBottom = 5;
        rpLayout.marginLeft = 0;
        rpLayout.marginRight = 0;
        this.rightPanel.setLayout(rpLayout);

        // position the right panel in the parent shell
        final FormData rpFormData = new FormData();
        rpFormData.left = new FormAttachment(this.argBlockList, 20);
        rpFormData.top = new FormAttachment(0, 15);
        rpFormData.right = new FormAttachment(100, -10);
        rpFormData.bottom = new FormAttachment(90);
        this.rightPanel.setLayoutData(rpFormData);

        displayRepeatArgument();
    }

    private void setButtonsEnable(final boolean enabled) {
        this.upButton.setEnabled(enabled);
        this.downButton.setEnabled(enabled);
        this.copyButton.setEnabled(enabled);
        this.deleteButton.setEnabled(enabled);
    }

    private void destroyRepeatArgComposite() {
        if (this.repeatArgComposite != null) {
            this.repeatArgComposite.dispose();
        }

        this.repeatArgComposite = null;
        this.nameLabel = null;
        this.nameText = null;
        this.rangeLabel = null;
        this.rangeText = null;
        this.numRepeatsLabel = null;
        this.numRepeatsText = null;
        this.numRepeatsButton = null;
        this.cmdArgLabel = null;
        this.argScrollComposite = null;
        this.argComposite = null;
        this.argPrefixFields = null;
        this.argValueFields = null;
        this.argSuffixFields = null;
    }

    private void displayRepeatArgument() {
        destroyRepeatArgComposite();

        this.repeatArgComposite = new Composite(this.rightPanel, SWT.NONE);
        final FormData racFormData = new FormData();
        racFormData.left = new FormAttachment(0, 10);
        racFormData.right = new FormAttachment(100, -10);
        racFormData.top = new FormAttachment(0);
        racFormData.bottom = new FormAttachment(100);
        this.repeatArgComposite.setLayoutData(racFormData);
        this.repeatArgComposite.setLayout(new FormLayout());
        this.rightPanel.setTabList(new Control[] { this.repeatArgComposite });

        this.nameLabel = new Label(this.repeatArgComposite, SWT.LEFT);
        this.nameLabel.setText("Name:");
        final FormData nlFormData = new FormData();
        nlFormData.top = new FormAttachment(0, 5);
        nlFormData.left = new FormAttachment(0);
        nlFormData.right = new FormAttachment(40);
        this.nameLabel.setLayoutData(nlFormData);

        this.nameText = new Label(this.repeatArgComposite, SWT.LEFT);
        this.nameText.setFont(this.cbComposite.getTextFieldFont());
        this.nameText.setText(this.cmd.getArgumentDisplayName(repeatArgIndex));
        final FormData ntFormData = new FormData();
        ntFormData.top = new FormAttachment(this.nameLabel, 0, SWT.TOP);
        ntFormData.left = new FormAttachment(40, 5);
        ntFormData.right = new FormAttachment(100);
        this.nameText.setLayoutData(ntFormData);

        this.rangeLabel = new Label(this.repeatArgComposite, SWT.LEFT);
        this.rangeLabel.setText("Ranges:");
        final FormData rlFormData = new FormData();
        rlFormData.top = new FormAttachment(this.nameText, 10);
        rlFormData.left = new FormAttachment(0);
        rlFormData.right = new FormAttachment(40);
        this.rangeLabel.setLayoutData(rlFormData);

        this.rangeText = new Label(this.repeatArgComposite, SWT.LEFT);
        this.rangeText.setFont(this.cbComposite.getTextFieldFont());
        this.rangeText.setText(this.cmd.getArgumentDefinition(repeatArgIndex).getRangeString(false));
        final FormData rtFormData = new FormData();
        rtFormData.top = new FormAttachment(this.rangeLabel, 0, SWT.TOP);
        rtFormData.left = new FormAttachment(40, 5);
        rtFormData.right = new FormAttachment(100);
        this.rangeText.setLayoutData(rtFormData);

        this.numRepeatsLabel = new Label(this.repeatArgComposite, SWT.LEFT);
        this.numRepeatsText = new Text(this.repeatArgComposite, SWT.LEFT
                | SWT.BORDER);
        this.numRepeatsButton = new Button(this.repeatArgComposite, SWT.PUSH);

        this.numRepeatsLabel.setText("# of Blocks (U"
                + this.cmd.getArgumentDefinition(repeatArgIndex).getBitLength() + "):");
        final FormData nrlFormData = new FormData();
        nrlFormData.top = new FormAttachment(this.numRepeatsText, 0, SWT.CENTER);
        nrlFormData.left = new FormAttachment(0);
        nrlFormData.right = new FormAttachment(40);
        this.numRepeatsLabel.setLayoutData(nrlFormData);

        this.numRepeatsText.setFont(this.cbComposite.getTextFieldFont());
        /**
         * Command Builder incorrectly builds
         * MOT_EXER_TIMED commands with no mot_spec array If there is nothing in
         * the IRepeatCommandArgument, return zero indicating that fact.
         */
        this.numRepeatsText
        .setText(this.cmd.getArgumentValue(repeatArgIndex) == null ? "0"
                : this.cmd.getArgumentValue(repeatArgIndex));
        this.numRepeatsText
        .setEditable(!this.cbComposite.isStandalone());
        final FormData nrtFormData = new FormData();
        nrtFormData.top = new FormAttachment(this.rangeText, 15);
        nrtFormData.left = new FormAttachment(40, 5);
        nrtFormData.right = new FormAttachment(60);
        this.numRepeatsText.setLayoutData(nrtFormData);

        this.numRepeatsButton.setText("Recalculate");
        final FormData nrbFormData = new FormData();
        nrbFormData.top = new FormAttachment(this.numRepeatsText, 0, SWT.CENTER);
        nrbFormData.left = new FormAttachment(70);
        nrbFormData.right = new FormAttachment(100);
        this.numRepeatsButton.setLayoutData(nrbFormData);
        this.numRepeatsButton.addSelectionListener(this.handler);

        final FontData calFontData = new FontData("Helvetica", 12, SWT.BOLD);
        final Font calFont = new Font(getDisplay(), calFontData);
        cmdArgLabel = new Label(this.repeatArgComposite, SWT.LEFT);
        cmdArgLabel.setFont(calFont);
        final FormData calFormData = new FormData();
        calFormData.left = new FormAttachment(0, 5);
        calFormData.top = new FormAttachment(this.numRepeatsText, 10);
        calFormData.right = new FormAttachment(30);
        cmdArgLabel.setLayoutData(calFormData);
        cmdArgLabel.setText("Arguments");

        this.repeatArgComposite
        .setTabList(new Control[] { this.numRepeatsText });

        displaySelectedBlock();

        this.rightPanel.layout(true);
    }

    private void displaySelectedBlock() {
        this.currentBlock = this.argBlockList.getSelectionIndex();
        final RepeatArgumentBlock block = getSelectedBlock();
        if (block == null) {
            if (this.cmd.getArgumentCount(repeatArgIndex) == 0) {
                setButtonsEnable(false);
            }
            return;
        }

        this.argScrollComposite = new ScrolledComposite(
                this.repeatArgComposite, SWT.V_SCROLL | SWT.H_SCROLL
                | SWT.BORDER);
        this.repeatArgComposite.setTabList(new Control[] { this.numRepeatsText,
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

        displayArguments();

        setButtonsEnable(true);
    }

    private void displayArguments() {
    	final RepeatArgumentBlock block = getSelectedBlock();
    	
        this.argPrefixFields = new Label[argBlockSize];
        this.argValueFields = new Control[argBlockSize];
        this.argSuffixFields = new Control[argBlockSize];

        final int startIndex = block == null ? 0 : block.getStartIndex();
        
        for (int i = 0 ; i < argBlockSize ; i++) {

            /*
             * Use new GUI control object factory
             * for the following rather than the argument class directly.
             */
            final ICommandArgumentGuiControl guiControl = ArgumentGuiControlFactory.getGuiControl(this.cmd, this.repeatArgIndex, startIndex + i);
            this.argPrefixFields[i] = guiControl
                    .createPrefixControl(this.argComposite);
            this.argSuffixFields[i] = guiControl
                    .createSuffixControl(this.argComposite);
            this.argValueFields[i] = guiControl.createArgumentValueControl(
                    this.argComposite, this.cbComposite.getTextFieldFont());

            if (this.argSuffixFields[i] instanceof Button) {
                ((Button) this.argSuffixFields[i])
                .addSelectionListener(this.handler);
            }
            
            if (this.argValueFields[i] instanceof Combo) { 
                // Adds modify and selection listeners to an EnumeratedArgumentment combo box (drop-down)
                ((Combo)this.argValueFields[i]).addModifyListener(handler);
                ((Combo)this.argValueFields[i]).addSelectionListener(handler);
            } else if (this.argValueFields[i] instanceof Text) {
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
            if (i == (argBlockSize - 1)) {
                prefixFormData.bottom = new FormAttachment(100, -15);
            }

            this.argPrefixFields[i].setLayoutData(prefixFormData);
            this.argValueFields[i].setLayoutData(valueFormData);
            this.argSuffixFields[i].setLayoutData(suffixFormData);

            this.argValueFields[i].addFocusListener(this.handler);
        }

        this.argComposite.setTabList(this.argValueFields);

        // make the scrollbars appear on the scrolled composite when we need
        // them to...believe me, you
        // really don't want to remove this line
        this.argScrollComposite.setMinSize(this.argComposite.computeSize(
                SWT.DEFAULT, SWT.DEFAULT, true));
    }

    private void createBottomPanel() {
        final Composite buttonComposite = new Composite(this, SWT.NONE);
        final FormLayout rpLayout = new FormLayout();
        rpLayout.marginTop = 10;
        rpLayout.marginBottom = 10;
        rpLayout.marginLeft = 0;
        rpLayout.marginRight = 10;
        buttonComposite.setLayout(rpLayout);

        // position the right panel in the parent shell
        final FormData rpFormData = new FormData();
        rpFormData.left = new FormAttachment(this.listButtonComposite, 20);
        rpFormData.top = new FormAttachment(this.rightPanel, 10);
        rpFormData.right = new FormAttachment(100);
        rpFormData.bottom = new FormAttachment(100);
        buttonComposite.setLayoutData(rpFormData);

        this.okButton = new Button(buttonComposite, SWT.PUSH);
        this.okButton.setText("OK");
        final FormData obFormData = new FormData();
        obFormData.left = new FormAttachment(20);
        obFormData.right = new FormAttachment(40);
        this.okButton.setLayoutData(obFormData);
        this.okButton.addSelectionListener(this.handler);

        this.cancelButton = new Button(buttonComposite, SWT.PUSH);
        this.cancelButton.setText("Cancel");
        final FormData cbFormData = new FormData();
        cbFormData.left = new FormAttachment(60);
        cbFormData.right = new FormAttachment(80);
        this.cancelButton.setLayoutData(cbFormData);
        this.cancelButton.addSelectionListener(this.handler);
    }

    private RepeatArgumentBlock getSelectedBlock() {
        final int selection = this.argBlockList.getSelectionIndex();
        if (selection == -1) {
            return (null);
        }

        final RepeatArgumentBlock block = this.argBlocks.get(selection);

        return (block);
    }

    private void populateBlockList() {
    	
    	final int argCount = this.cmd.getRepeatArgumentDefinition(repeatArgIndex).getDictionaryArgumentCount(false);

        final ArrayList<String> newBlockList = new ArrayList<>(256);
        for (int i = 0; i < this.argBlocks.size(); i++) {
            final RepeatArgumentBlock arg = this.argBlocks.get(i);
            
            arg.setStartIndex(i*argCount);

            newBlockList.add("Argument Block #" + arg.getOrderId());
        }

        this.argBlockList.setItems(newBlockList.toArray(new String[] {}));
    }

    private void saveCurrentArgument() throws CommandParseException {
        boolean errorsFound = false;
        final StringBuilder errors = new StringBuilder(1024);

        try {
            final String value = GDR.removeWhitespaceFromString(this.numRepeatsText
                    .getText());
            if (value == null || value.length() == 0) {
                throw new CommandParseException("You must supply a value.");
            }

            // moved the isTransmittable check to the bottom of this function
            
            this.cmd.setArgumentValue(repeatArgIndex, value);
            
        } catch (final Exception e) {
            errorsFound = true;
            errors.append("# of Blocks\n");
        }

        if (this.currentBlock != -1) {
            final RepeatArgumentBlock block = this.argBlocks.get(this.currentBlock);
            if (this.argValueFields != null) {
                for (int i = 0; i < this.argValueFields.length; i++) {
                    final Control control = this.argValueFields[i];
                    final int subIndex = block.getStartIndex() + i;

                    String value = null;
                    try {
                        if (control instanceof Text) {
                            value = ((Text) control).getText();
                        } else if (control instanceof Combo) {
                            value = ((Combo) control).getText();
                        }

                        if (value == null) {
                            throw new CommandParseException(
                                    "You must supply a value.");
                        }

                        value = value.trim();
                        UplinkInputParser.parseAndSetCommandArgument(appContext, cmd, repeatArgIndex, subIndex, value);
                    } catch (final Exception e) {
                        errorsFound = true;
                        errors.append("\n");
                        errors.append(this.cmd.getArgumentDefinition(repeatArgIndex, i).getFswName());
                        errors.append(" (index " + i + ")");
                    }
                }
            }
        }
        
        if (!this.cmd.isArgumentValueTransmittable(repeatArgIndex)) {
        	final StringBuilder invalidArgs = new StringBuilder();
        	final long argCount = Long.parseLong(this.cmd.getArgumentValue(repeatArgIndex));
        	if(argCount > ((Number)cmd.getArgumentDefinition(repeatArgIndex).getMaximumValue()).longValue() ||
        			argCount < ((Number)cmd.getArgumentDefinition(repeatArgIndex).getMinimumValue()).longValue()) {
        		invalidArgs.append(cmd.getArgumentDefinition(repeatArgIndex).getFswName());
        	}
        	
        	for(int i = 0 ; i < this.cmd.getArgumentCount(repeatArgIndex) ; i++) {
        	    //this will return all of the errors, not just the non-transmittable ones
        		if(!this.cmd.isArgumentValueValid(repeatArgIndex, i)) {
        			if(invalidArgs.length() == 0) {
        			    invalidArgs.append("The following command fields contain unusable values:\n");
        			}
        			invalidArgs.append("\n");
        			invalidArgs.append(this.cmd.getArgumentDefinition(repeatArgIndex, i).getFswName());
        			invalidArgs.append(" (index " + i + ")");
        		}
        	}
        	
            throw new CommandParseException(
                    "The following command fields contain unusable values: " + invalidArgs.toString());
        }

        //no longer needed, set the value in the repeat arg directly

        if (errorsFound) {
            throw new CommandParseException(
                    "The following command fields contain unusable values"
                            + "\n(Ensure that you entered valid characters and that the value fits within "
                            + "the bit size allotted to the argument):\n"
                            + errors.toString());
        }
    }

    /**
     * A block of repeat argument
     * 
     */
    private static class RepeatArgumentBlock {
        private int orderId;
        private int startIndex;

        /*
         * Changed to use interface type for command arguments here.
         */
        public RepeatArgumentBlock(final Integer orderId, final int startIndex) {
        	this.orderId = orderId;
            this.startIndex = startIndex;
        }

        /**
         * Get the order ID
         * @return Returns the orderId.
         */
        public int getOrderId() {
            return (this.orderId);
        }

        /**
         * Sets the orderId
         * 
         * @param orderId The orderId to set.
         */
        public void setOrderId(final Integer orderId) {
            this.orderId = orderId;
        }
        
        public void setStartIndex(final int startIndex) {
        	this.startIndex = startIndex;
        }
        
        public int getStartIndex(){
        	return this.startIndex;
        }
    }

    /**
     * Handles user events
     * 
     *
     */
    private class EventHandler extends SelectionAdapter implements
    FocusListener, ModifyListener {

        AtomicBoolean handlingArgRequest = new AtomicBoolean(false);
        
        /**
         * Constructor
         */
        public EventHandler() {
            super();
        }
        
        /**
         *
         * @see org.eclipse.swt.events.ModifyListener#modifyText(org.eclipse.swt.events.ModifyEvent)
         *  09/21/18 - Uses same logic as CommandBuilderCompsite.EventHandler.modifyText
         *          for updating displayed arguments
         */
        @Override
        public void modifyText(final ModifyEvent me) {
            try {
                if (RepeatArgumentComposite.this.argValueFields != null && Arrays.asList(RepeatArgumentComposite.this.argValueFields).contains(me.getSource())) {
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
                TraceManager.getDefaultTracer().error("Encountered unexpected exception: " 
                        + (e.getCause() != null ? e.getCause() : e.getMessage()), e);
                return;
            }
        }

        /*
         * moved this functionality from the event handler to its own function.
         *      Additionally, renamed argPosition to displayArgPosition and realPosition to subArgIndex in order
         *      to eliminate some confusion between the two variables as this was causing the odd behavior.
         */
        private void handleArgUpdate(final TypedEvent event) {
            try {
                if (event.getSource() instanceof Combo && argValueFields != null) {
                    // Find the selected combo box and determine the argument index
                    final int displayArgPosition = CommandArgumentGuiHelper.getSelectedCmdArgument(event, argValueFields);
                    final Combo selectedCombo = displayArgPosition == -1 ? null : (Combo) argValueFields[displayArgPosition];
                    final String userInput = selectedCombo != null ? selectedCombo.getText() : null;
                    final boolean noInput = (userInput == null || userInput.isEmpty());
                    final Point originalPos = CommandArgumentGuiHelper.getUserCaret(argValueFields[displayArgPosition]);
                    // real position = block start index = current command argument position 
                    final int subArgIndex = displayArgPosition + argBlocks.get(argBlockList.getSelectionIndex()).getStartIndex();

                    if (noInput) {
                        // User deleted text and/or the search box is empty. Rebuild composite
                        // Update this command argument to be nothing as well
                        // case: select valid enum, then try to delete text
                        // make sure it doesnt set the previous selected enum
                        cmd.clearArgumentValue(repeatArgIndex, subArgIndex);
                        // set argument value to "" to prevent defalult value from populating in gui
                        cmd.setArgumentValue(repeatArgIndex, subArgIndex, "");
                        refreshCmdArgItems(repeatArgIndex, subArgIndex, displayArgPosition);
                        displayRepeatArgument(); // Refresh GUI
                    }

                    if (displayArgPosition != -1 && !noInput 
                            && !selectedCombo.isDisposed() && selectedCombo.getSelectionIndex() != -1) {
                        // Best case; we have a valid selection.
                        // Do nothing and a SelectionEvent will be triggered
                        // Command and arguments will update in the selection event handling
                    } else if (displayArgPosition != -1 && !noInput) {
                        // User has not yet entered a valid enum yet
                        // First update the valid arguments drop-down
                        refreshCmdArgItems(repeatArgIndex, subArgIndex, displayArgPosition);

                        // Try and translate user input into the enum definition
                        final String argText = CommandArgumentGuiHelper.getArgumentText(selectedCombo);

                        // Trim drop-down to only enums that start with user input text while searching
                        final boolean matched = CommandArgumentGuiHelper.updateAndSearchCmdArg(appContext, selectedCombo, cmd, repeatArgIndex, subArgIndex);
                        // Save list of filtered drop-down items
                        final String[] comboItems = selectedCombo.getItems();

                        // If user input text matched a drop-down item
                        if (matched) {
                            CommandArgumentGuiHelper.parseCmdArgument(appContext, repeatArgIndex, subArgIndex, argText, cmd);
                        } 

                        // Get current argument value from definition and compare against user input
                        final String currentArg = cmd.getArgumentValue(repeatArgIndex, subArgIndex);
                        final boolean definitionMismatch = currentArg != null 
                                && !"".equals(currentArg) 
                                && !currentArg.equalsIgnoreCase(userInput);
                        if (definitionMismatch) { 
                            // Arg value def doesnt match user input. Clear this argument
                            cmd.clearArgumentValue(repeatArgIndex, subArgIndex);
                            refreshCmdArgItems(repeatArgIndex, subArgIndex, displayArgPosition);
                        }
                        displayRepeatArgument(); // Refresh GUI

                        // Set filtered drop-down list and text after refresh 
                        ((Combo) argValueFields[displayArgPosition]).setItems(comboItems);

                        // Need to set the text box back to what user input was
                        // If there was a match, we set the definition representation of user input (argText)
                        // E.g.) If user types "acm", argText will reflect the actual enum definition "ACM"
                        if (matched) { 
                            ((Combo) argValueFields[displayArgPosition]).setText(argText);
                        } else { 
                            ((Combo) argValueFields[displayArgPosition]).setText(userInput);
                        }
                    }

                    if (displayArgPosition != -1) { // just to be safe.. 
                        CommandArgumentGuiHelper.setOriginalCaret(argValueFields[displayArgPosition],originalPos);
                    }
                }
                else if (event.getSource() instanceof Text && argValueFields != null) {
                    // This allows saving the state of a non-enum command args (Text)
                    // Parse and set current user input for this field when the user types something
                    final int displayArgPosition = CommandArgumentGuiHelper.getSelectedCmdArgument(event, argValueFields);
                    final Text selectedText = displayArgPosition == -1 ? null : (Text) argValueFields[displayArgPosition];
                    final String userInput = selectedText != null ? selectedText.getText() : ""; 
                    final Point originalPos = CommandArgumentGuiHelper.getUserCaret(argValueFields[displayArgPosition]);
                    // real position = block start index = current command argument position 
                    final int subArgIndex = displayArgPosition + argBlocks.get(argBlockList.getSelectionIndex()).getStartIndex();

                    // Basically, everytime the user types something in a non-enum field
                    // we are trying to parse that text, regardless of what it is, as the command argument value
                    // The exceptions below occur when that argument is invalid, we hide them to prevent bombarding 
                    // with error messages. When user tries to send/validate, let that display any errors for text fields
                    if (displayArgPosition != -1) {
                        // Added extra handling for binary/hex values
                        // Also removed the GUI refresh after parsing b/c other args are not updated from non-Combos fields
                        final boolean goodInput = CommandArgumentGuiHelper.isValidGuiInput(userInput);
                        
                        if (goodInput) { 
                        	final boolean oldCheck = appContext.getBean(CommandProperties.class).getValidateCommands();
                            try {
                                appContext.getBean(CommandProperties.class).setValidateCommands(false);
                                CommandArgumentGuiHelper.parseCmdArgument(appContext, repeatArgIndex, subArgIndex, userInput, cmd);
                            } catch (NullPointerException | IllegalStateException | IllegalArgumentException e) {
                                // Do nothing. Hide this exception from the user
                            } finally {
                            	appContext.getBean(CommandProperties.class).setValidateCommands(oldCheck);
                            }
                        } 
                        
                        CommandArgumentGuiHelper.setOriginalCaret(argValueFields[displayArgPosition],originalPos);
                    }
                } 
            } catch (final Exception e1) {
                TraceManager.getDefaultTracer().error("Encountered unexpected exception: "
                        + (e1.getCause() != null ? e1.getCause() : e1.getMessage()), e1);
                return;
            }
        }
        
		/**
		 * Re-creates the selected command's argument ENUM drop-down
		 * 
		 * @param index the command argument index 
		 * @param subindex the repeat argument subindex
		 * @param displayIndex the placement of the argument in the display
		 */
		private void refreshCmdArgItems(final int index, final int subindex, final int displayIndex) {
			if (!(argValueFields[displayIndex] instanceof Combo)) { 
				return;
			}
			// First remove old entries. We have to iterate over the items for this.
			// This is done to correct any out of order elements in the drop-down list
			// removeAll() doesnt work because it triggers another event.

			// setting items to empty String[] will also not work.
			// Also, Trying to sort the current items array causes stack overflow (why?)
			if (((Combo) argValueFields[displayIndex]).getItems() == null) { 
				return;
			}
			for (final String s : ((Combo) argValueFields[displayIndex]).getItems()) {
				((Combo) argValueFields[displayIndex]).remove(s);
			}
			// Now reconstruct command argument enum drop-down
			for (int i = 0; i < cmd.getArgumentDefinition(index, subindex).getEnumeration().getEnumerationValues()
					.size(); ++i) {
				final String val = cmd.getArgumentDefinition(index, subindex).getEnumeration().getEnumerationValues().get(i).getDictionaryValue();
				((Combo) argValueFields[displayIndex]).add(val);
			}
		}
		

        /*
         * (non-Javadoc)
         * @see
         * org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse
         * .swt.events.SelectionEvent)
         */
        @Override
        public void widgetSelected(final SelectionEvent se) {
            if (se.getSource() == deleteButton) {
                final int position = RepeatArgumentComposite.this.argBlockList
                        .getSelectionIndex();
                if (position == -1) {
                    return;
                }
                
                cmd.removeRepeatArguments(repeatArgIndex, argBlocks.get(position).getStartIndex());
                argBlocks.remove(position);
                populateBlockList();

                final int newPosition = (position - 1) > 0 ? (position - 1) : 0;
                if (cmd.getArgumentCount(repeatArgIndex) > 0) {
                    argBlockList.setSelection(newPosition);
                    argBlockList.showSelection();
                    displayRepeatArgument();
                } else {
                    destroyBlockComposite();
                    /**
                     * Command Builder incorrectly
                     * builds MOT_EXER_TIMED commands with no mot_spec array If
                     * the last argument is deleted, then clear the arg tables.
                     */
                    RepeatArgumentComposite.this.argPrefixFields = null;
                    RepeatArgumentComposite.this.argValueFields = null;
                    RepeatArgumentComposite.this.argSuffixFields = null;
                }
                numRepeatsText.setText(String.valueOf(argBlocks.size()));
                cmd.setArgumentValue(repeatArgIndex, String.valueOf(argBlocks.size()));

                focusOnArguments();

                return;
            } else if (se.getSource() == cancelButton) {
            	try {
            	final boolean oldValue = appContext.getBean(CommandProperties.class)
                        .getValidateCommands();
                appContext.getBean(CommandProperties.class).setValidateCommands(false);
                UplinkInputParser.parseAndSetGuiArgument(appContext, cmd, repeatArgIndex, originalCmd);
                appContext.getBean(CommandProperties.class).setValidateCommands(oldValue);
            	}
            	catch(final CommandParseException e) {
            		//Surprisingly this wasn't caught before! doesn't matter much anyhow.
            	}
                canceled = true;
                parent.getShell().close();
                return;
            } else if (se.getSource() == numRepeatsButton) {
                numRepeatsText.setText(String.valueOf(argBlocks.size()));
                cmd.setArgumentValue(repeatArgIndex, String.valueOf(argBlocks.size()));
                return;
            } 
            if (se.getSource() instanceof Combo && argValueFields != null) { 
				// Find the selected combo box and determine the argument index
				final int argPosition = CommandArgumentGuiHelper.getSelectedCmdArgument(se, argValueFields);
				final String argTxt = (argValueFields[argPosition] instanceof Combo ? 
                		((Combo)argValueFields[argPosition]).getText() 
                		: ((Text)argValueFields[argPosition]).getText());
				
				// Add block start index to current command argument position
				final int repeatIndex = argPosition + argBlocks.get(argBlockList.getSelectionIndex()).getStartIndex();
				
				// Update command definition argument with selected value
				CommandArgumentGuiHelper.parseCmdArgument(appContext, repeatArgIndex, repeatIndex, argTxt, cmd);
				
				/*
                 * TODO: Update/replace the commented out function call in order to only update the
                 *   arguments that are changed by the selection of a value from the combo and NOT replace everything!
                 */
				//displayRepeatArgument();  // Update GUI
				
				// Set focus back on this Combo item after refresh
				argValueFields[argPosition].forceFocus();

				// return so saveCurrentArgument() below does not get called
				// it tries to validate the current arguments and creates an annoying amount of pop-ups
				// instead, this validation takes place when selecting 'OK'
				return; 
			}
            
            boolean foundError = false;
            try {
                saveCurrentArgument();
            } catch (final CommandParseException e) {
                se.doit = false;
                SWTUtilities.showErrorDialog(getShell(),
                        "Argument Format Error", e.getMessage());
                foundError = true;
            }

            if (se.getSource() == upButton && !foundError) {
                final int oldPosition = argBlockList.getSelectionIndex();
                final int newPosition = oldPosition - 1;
                if (oldPosition == -1 || oldPosition == 0) {
                    return;
                }
                swapBlocks(oldPosition, newPosition);
                populateBlockList();
                argBlockList.setSelection(newPosition);
                argBlockList.showSelection();
                displayRepeatArgument();

                focusOnArguments();

                return;
            } else if (se.getSource() == downButton && !foundError) {
                final int oldPosition = argBlockList.getSelectionIndex();
                final int newPosition = oldPosition + 1;
                if (oldPosition == -1 || oldPosition == (argBlocks.size() - 1)) {
                    return;
                }
                swapBlocks(oldPosition, newPosition);
                populateBlockList();
                argBlockList.setSelection(newPosition);
                argBlockList.showSelection();
                displayRepeatArgument();

                focusOnArguments();

                return;
            } else if (se.getSource() == newButton && !foundError) {
                final int position = argBlockList.getSelectionIndex() + 1;
                final int argCount = RepeatArgumentComposite.this.cmd.getRepeatArgumentDefinition(repeatArgIndex).getDictionaryArgumentCount(false);
                final int startIndex = position * argCount;
                
                cmd.addRepeatArguments(repeatArgIndex, startIndex);
                final RepeatArgumentBlock arg = new RepeatArgumentBlock(nextBlockNumber(), startIndex);
                argBlocks.add(position, arg);
                populateBlockList();
                argBlockList.setSelection(position);
                argBlockList.showSelection();
                displayRepeatArgument();
                numRepeatsText.setText(String.valueOf(argBlocks.size()));
                cmd.setArgumentValue(repeatArgIndex, String.valueOf(argBlocks.size()));

                focusOnArguments();

                return;
            } else if (se.getSource() == copyButton && !foundError) {
                final int position = argBlockList.getSelectionIndex();
                if (position == -1) {
                    return;
                }

                final int argCount = RepeatArgumentComposite.this.cmd.getRepeatArgumentDefinition(repeatArgIndex).getDictionaryArgumentCount(false);
                final int repeatPosition = position + 1;
                final int repeatIndex = repeatPosition * argCount;
                
                cmd.addRepeatArguments(repeatArgIndex, repeatIndex);
                final RepeatArgumentBlock arg = new RepeatArgumentBlock(nextBlockNumber(), repeatIndex);
                argBlocks.add(repeatPosition,arg);
                copyBlock(position,repeatPosition);
                
                populateBlockList();
                argBlockList.setSelection(position + 1);
                argBlockList.showSelection();
                displayRepeatArgument();
                numRepeatsText.setText(String.valueOf(argBlocks.size()));
                cmd.setArgumentValue(repeatArgIndex, String.valueOf(argBlocks.size()));

                focusOnArguments();

                return;
            } else if (se.getSource() == argBlockList) {
                if (foundError) {
                    argBlockList.setSelection(currentBlock);
                    argBlockList.showSelection();
                } else {
                    displayRepeatArgument();
                }
                return;
            } else if (se.getSource() == okButton && !foundError) {
                canceled = false;
                parent.getShell().close();
                return;
            }
        }

        /**
         * Focus on arguments
         */
        public void focusOnArguments() {
            if (argValueFields != null && argValueFields.length > 0) {
                argValueFields[0].setFocus();
            }
        }

        /*
         * (non-Javadoc)
         * @see
         * org.eclipse.swt.events.FocusListener#focusGained(org.eclipse.swt.
         * events.FocusEvent)
         */
        @Override
        public void focusGained(final FocusEvent arg0) {
            final Control argValField = (Control) arg0.getSource();
            argScrollComposite.setOrigin(0, argValField.getLocation().y);
        }

        /*
         * (non-Javadoc)
         * @see
         * org.eclipse.swt.events.FocusListener#focusLost(org.eclipse.swt.events
         * .FocusEvent)
         */
        @Override
        public void focusLost(final FocusEvent arg0) {
            // Empty method required by interface
        }
        
        private void destroyBlockComposite() {
            if (argScrollComposite != null) {
                argScrollComposite.dispose();
            }

            argComposite = null;
            currentBlock = -1;

            setButtonsEnable(false);
        }
        
        private int nextBlockNumber() {
        	if(argBlocks == null || argBlocks.isEmpty()) {
        		return 1;
        	}
        	
        	int max = 1;
        	for(final RepeatArgumentBlock block : argBlocks) {
        		if(block.getOrderId() >= max) {
        			max = block.getOrderId() + 1;
        		}
        	}
        	return max;
        }
        
        private void swapBlocks(final int oldPosition, final int newPosition){
        	final ArrayList<String> oldVal = new ArrayList<>();
        	final ArrayList<String> newVal = new ArrayList<>();
        	final int oldStart = argBlocks.get(oldPosition).getStartIndex();
        	final int newStart = argBlocks.get(newPosition).getStartIndex();
        	
        	for(int i = 0 ; i < argBlockSize ; i++) {
        		oldVal.add(cmd.getArgumentValue(repeatArgIndex, oldStart + i));
        		newVal.add(cmd.getArgumentValue(repeatArgIndex, newStart + i));
        	}
        	for (int j=0; j< argBlockSize; j++) { 
        		
        		cmd.setArgumentValue(repeatArgIndex, newStart + j, oldVal.get(j));
        		cmd.setArgumentValue(repeatArgIndex, oldStart + j, newVal.get(j));
        	}
        	
        	final int oldId = argBlocks.get(oldPosition).getOrderId();
        	final int newId = argBlocks.get(newPosition).getOrderId();
        	argBlocks.get(oldPosition).setOrderId(newId);
        	argBlocks.get(newPosition).setOrderId(oldId);
        	
        	
        }
        
        private void copyBlock(final int originalPosition, final int newPosition){
        	final int oldStart = argBlocks.get(originalPosition).getStartIndex();
        	final int newStart = argBlocks.get(newPosition).getStartIndex();
        	final int numArgs = cmd.getRepeatArgumentDefinition(repeatArgIndex).getDictionaryArgumentCount(false);
        	
        	for(int i = 0 ; i < numArgs ; i++) {
        		cmd.setArgumentValue(repeatArgIndex, newStart + i, cmd.getArgumentValue(repeatArgIndex, oldStart + i));
        		
        	}
        }

    }

    /**
     * Indicates whether or not it was canceld
     * @return Returns the canceled.
     */
    public boolean isCanceled() {
        return (this.canceled);
    }

    /**
     * Sets the canceled
     * 
     * @param canceled The canceled to set.
     */
    public void setCanceled(final boolean canceled) {
        this.canceled = canceled;
    }

    /**
     * Get the IFlightCommand containing the repeat argument
     * @return the IFlightCommand containing this repeat argument
     */
    public IFlightCommand getFlightCommand() {
        return (this.cmd);
    }
    
    
    /**
     * Get the index to the repeat argument within the associated IFlightCommand
     * @return the index of the IRepeatCommandArgument
     */
    public int getRepeatArgIndex() {
    	return this.repeatArgIndex;
    }
}
