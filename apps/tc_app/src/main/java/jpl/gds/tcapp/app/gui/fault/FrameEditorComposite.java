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
import java.util.Collections;

import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.tc.api.ITcTransferFrame;
import jpl.gds.tc.api.ITewUtility;
import jpl.gds.tc.api.TcApiBeans;
import jpl.gds.tc.api.exception.FrameWrapUnwrapException;
import jpl.gds.tc.api.frame.ITcTransferFrameFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
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
import org.eclipse.swt.widgets.Text;
import org.springframework.context.ApplicationContext;

import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.shared.util.BinOctHexUtility;
import jpl.gds.tc.api.config.CommandFrameProperties;
import jpl.gds.tc.api.config.ExecutionStringType;
import jpl.gds.tc.api.config.VirtualChannelType;
import jpl.gds.tc.api.frame.ITcTransferFrameSerializer;

/**
 * This class is a SWT composite that displays widgets for editing a command frame
 *
 * 07/17/19 change the reported FECF length to use the config value
 * 09/18/19 - updated to work with CTS
 */
public final class FrameEditorComposite extends Composite implements FaultInjectorGuiComponent
{
	private static final String FONT_NAME = "Helvetica";
	
	private static final String FAULT_PAGE_TITLE = "Frame Editor";
	private static final String FAULT_PAGE_DESC = "Add, remove, reorder, and edit telecommand frames.";

	private FaultInjectionState dataState = null;

	private List frameList = null;
	private Button upButton = null;
	private Button downButton = null;
	private Button newButton = null;
	private Button copyButton = null;
	private Button deleteButton = null;

	private Group rightPanel = null;
	private Composite frameComposite = null;

	private Composite frameHeaderComposite = null;
	private Label versionNumberLabel = null;
	private Combo versionNumberCombo = null;
	private Label bypassFlagLabel = null;
	private Combo bypassFlagCombo = null;
	private Label ccFlagLabel = null;
	private Combo ccFlagCombo = null;
	private Label spareLabel = null;
	private Combo spareCombo = null;
	private Label scidLabel = null;
	private Text scidText = null;
	private Label rceStringLabel = null;
	private Combo rceStringCombo = null;
	private Label vcNumberLabel = null;
	private Combo vcNumberCombo = null;
	private Label sequenceNumberLabel = null;
	private Text sequenceNumberText = null;
	private Label lengthLabel = null;
	private Text lengthText = null;
	private Button lengthButton = null;

	private Composite frameBodyComposite = null;
	private Label frameDataLabel = null;
	private Text frameDataText = null;

	private Composite frameTrailerComposite = null;
	private Label hasFecfLabel = null;
	private Combo hasFecfCombo = null;
	private Label fecfLabel = null;
	private Text fecfText = null;
	private Button fecfButton = null;

    private final EventHandler handler;

	private java.util.List<ITcTransferFrame> frames = null;
	private int currentFrame = -1;

	private final ApplicationContext appContext;
	private final ITcTransferFrameFactory frameFactory;
    private final ITewUtility tewUtility;
    private final CommandFrameProperties frameConfig;
    private final ITcTransferFrameSerializer frameSerializer;
    private final ITcTransferFrameSerializer legacyFrameSerializer;

    private final Tracer tracer;

	/**
	 * Constructor
	 * @param appContext the ApplicationContext of the calling class
	 * @param parent the parent composite
	 */
	public FrameEditorComposite(final ApplicationContext appContext, final Composite parent) {
		super(parent,SWT.NONE);
			
		this.appContext = appContext;

    	this.handler = new EventHandler();

    	createControls();

    	setTabList(new Control[] { this.frameList, this.rightPanel });
		layout(true);

        this.frameFactory = appContext.getBean(ITcTransferFrameFactory.class);
		this.tewUtility = appContext.getBean(ITewUtility.class);
        this.frameConfig = appContext.getBean(CommandFrameProperties.class);
        this.frameSerializer = appContext.getBean(ITcTransferFrameSerializer.class);
        this.legacyFrameSerializer = appContext.getBean(TcApiBeans.LEGACY_COMMAND_FRAME_SERIALIZER, ITcTransferFrameSerializer.class);

        tracer = TraceManager.getTracer(appContext, Loggers.UPLINK);
	}

	private void createControls() {
		setLayout(new FormLayout());

		createLeftPanel();
		createRightPanel();

		setDefaultFieldValues();
	}

	private void createLeftPanel() {
		final Label frameListLabel = new Label(this,SWT.LEFT);
		frameListLabel.setText("Uplink Session Frames: ");
		final FormData fllFormData = new FormData();
		fllFormData.top = new FormAttachment(0,15);
		fllFormData.left = new FormAttachment(0,10);
		fllFormData.right = new FormAttachment(35);
		frameListLabel.setLayoutData(fllFormData);

		this.frameList = new List(this,SWT.SINGLE | SWT.BORDER | SWT.V_SCROLL);
		final FormData flFormData = new FormData();
		flFormData.top = new FormAttachment(frameListLabel,15);
		flFormData.left = new FormAttachment(0,10);
		flFormData.right = new FormAttachment(35);
		flFormData.bottom = new FormAttachment(80);
		this.frameList.setLayoutData(flFormData);
		this.frameList.addSelectionListener(this.handler);

		final Composite listButtonComposite = new Composite(this,SWT.NONE);
		final FormData lbcFormData = new FormData();
		lbcFormData.top = new FormAttachment(this.frameList,10);
		lbcFormData.bottom = new FormAttachment(100);
		lbcFormData.left = new FormAttachment(0,10);
		lbcFormData.right = new FormAttachment(35);
		listButtonComposite.setLayout(new FormLayout());
		listButtonComposite.setLayoutData(lbcFormData);

		final int buttonSpacing = 5;
		this.upButton = new Button(listButtonComposite,SWT.PUSH);
		this.upButton.setText("Up");
		final FormData ubFormData = new FormData();
		ubFormData.top = new FormAttachment(0,buttonSpacing);
		ubFormData.bottom = new FormAttachment(50,-1*buttonSpacing);
		ubFormData.left = new FormAttachment(6);
		ubFormData.right = new FormAttachment(47);
		this.upButton.setLayoutData(ubFormData);
		this.upButton.addSelectionListener(this.handler);

		this.downButton = new Button(listButtonComposite,SWT.PUSH);
		this.downButton.setText("Down");
		final FormData dbFormData = new FormData();
		dbFormData.top = new FormAttachment(0,buttonSpacing);
		dbFormData.bottom = new FormAttachment(50,-1*buttonSpacing);
		dbFormData.left = new FormAttachment(53);
		dbFormData.right = new FormAttachment(94);
		this.downButton.setLayoutData(dbFormData);
		this.downButton.addSelectionListener(this.handler);

		this.newButton = new Button(listButtonComposite,SWT.PUSH);
		this.newButton.setText("New");
		final FormData nbFormData = new FormData();
		nbFormData.top = new FormAttachment(50,5);
		nbFormData.bottom = new FormAttachment(100,-1*buttonSpacing);
		nbFormData.left = new FormAttachment(5);
		nbFormData.right = new FormAttachment(30);
		this.newButton.setLayoutData(nbFormData);
		this.newButton.addSelectionListener(this.handler);

		this.copyButton = new Button(listButtonComposite,SWT.PUSH);
		this.copyButton.setText("Copy");
		final FormData cbFormData = new FormData();
		cbFormData.top = new FormAttachment(50,5);
		cbFormData.bottom = new FormAttachment(100,-1*buttonSpacing);
		cbFormData.left = new FormAttachment(34);
		cbFormData.right = new FormAttachment(60);
		this.copyButton.setLayoutData(cbFormData);
		this.copyButton.addSelectionListener(this.handler);

		this.deleteButton = new Button(listButtonComposite,SWT.PUSH);
		this.deleteButton.setText("Delete");
		final FormData delFormData = new FormData();
		delFormData.top = new FormAttachment(50,5);
		delFormData.bottom = new FormAttachment(100,-1*buttonSpacing);
		delFormData.left = new FormAttachment(64);
		delFormData.right = new FormAttachment(95);
		this.deleteButton.setLayoutData(delFormData);
		this.deleteButton.addSelectionListener(this.handler);
	}

	private void createRightPanel() {
		this.rightPanel = new Group(this,SWT.NONE);
		final FontData groupFontData = new FontData("Helvetica", 14, SWT.BOLD);
        final Font groupFont = new Font(getDisplay(), groupFontData);
        this.rightPanel.setFont(groupFont);
        final FormLayout rpLayout = new FormLayout();
        rpLayout.marginTop = 10;
        rpLayout.marginBottom = 10;
        rpLayout.marginLeft = 0;
        rpLayout.marginRight = 0;
        this.rightPanel.setLayout(rpLayout);

		//position the right panel in the parent shell
		final FormData rpFormData = new FormData();
 		rpFormData.left = new FormAttachment(this.frameList,20);
 		rpFormData.top = new FormAttachment(0,15);
 		rpFormData.right = new FormAttachment(100,-10);
 		rpFormData.bottom = new FormAttachment(100,-10);
		this.rightPanel.setLayoutData(rpFormData);
	}

	private void setDefaultFieldValues()
	{
		setButtonsEnable(false);
	}

	private void setButtonsEnable(final boolean enabled) {
		this.upButton.setEnabled(enabled);
		this.downButton.setEnabled(enabled);
		this.newButton.setEnabled(enabled);
		this.copyButton.setEnabled(enabled);
		this.deleteButton.setEnabled(enabled);
	}

	private void displaySelectedFrame() {
		destroyFrameComposite();

		this.currentFrame = this.frameList.getSelectionIndex();
		final ITcTransferFrame frame = getSelectedFrame();
		if(frame == null) {
			return;
		}

		this.frameComposite = new Composite(this.rightPanel,SWT.NONE);
		this.rightPanel.setTabList(new Control[] { this.frameComposite });
		final FormData ccFormData = new FormData();
		ccFormData.left = new FormAttachment(0,10);
		ccFormData.right = new FormAttachment(100,-10);
		ccFormData.top = new FormAttachment(0);
		ccFormData.bottom = new FormAttachment(100);
		this.frameComposite.setLayoutData(ccFormData);
		this.frameComposite.setLayout(new FormLayout());
		this.rightPanel.setTabList(new Control[] { this.frameComposite });

		displayFrameHeader(frame);

		final Label separator1 = new Label(this.frameComposite,SWT.CENTER | SWT.SEPARATOR);
		final FormData s1FormData = new FormData();
		s1FormData.top = new FormAttachment(49);
		s1FormData.bottom = new FormAttachment(51);
		s1FormData.left = new FormAttachment(0,5);
		s1FormData.right = new FormAttachment(100,-5);
		separator1.setLayoutData(s1FormData);

		displayFrameBody(frame);

		final Label separator2 = new Label(this.frameComposite,SWT.CENTER | SWT.SEPARATOR);
		final FormData s2FormData = new FormData();
		s2FormData.top = new FormAttachment(74);
		s2FormData.bottom = new FormAttachment(76);
		s2FormData.left = new FormAttachment(0,5);
		s2FormData.right = new FormAttachment(100,-5);
		separator2.setLayoutData(s2FormData);

		displayFrameTrailer(frame);

		this.frameComposite.setTabList(new Control[]{
				this.frameHeaderComposite,
				this.frameBodyComposite,
				this.frameTrailerComposite
		});

		setButtonsEnable(true);

		this.rightPanel.layout(true);
	}

	private void displayFrameHeader(final ITcTransferFrame frame) {
		final int verticalSpacing = 15;

		this.frameHeaderComposite = new Composite(this.frameComposite,SWT.NONE);
		final FormData fhcFormData = new FormData();
		fhcFormData.top = new FormAttachment(0);
		fhcFormData.bottom = new FormAttachment(49);
		fhcFormData.left = new FormAttachment(0);
		fhcFormData.right = new FormAttachment(100);
		this.frameHeaderComposite.setLayoutData(fhcFormData);
		this.frameHeaderComposite.setLayout(new FormLayout());

		this.versionNumberLabel = new Label(this.frameHeaderComposite,SWT.LEFT);
		this.versionNumberLabel.setText("Version #: ");
		final FormData vnlFormData = new FormData();
		vnlFormData.top = new FormAttachment(0,5);
		vnlFormData.left = new FormAttachment(0);
		vnlFormData.right = new FormAttachment(20);
		this.versionNumberLabel.setLayoutData(vnlFormData);

		this.versionNumberCombo = new Combo(this.frameHeaderComposite,SWT.LEFT | SWT.READ_ONLY | SWT.BORDER);
		for(long i = ITcTransferFrameSerializer.VERSION_NUMBER_MIN_VALUE; i <= ITcTransferFrameSerializer.VERSION_NUMBER_MAX_VALUE; i++) {
			this.versionNumberCombo.add(Long.toString(i));
		}
		this.versionNumberCombo.setText(Long.toString(frame.getVersionNumber()));
		final FormData vncFormData = new FormData();
		vncFormData.top = new FormAttachment(this.versionNumberLabel,0,SWT.CENTER);
		vncFormData.left = new FormAttachment(20,5);
		vncFormData.right = new FormAttachment(55,-10);
		this.versionNumberCombo.setLayoutData(vncFormData);

		this.bypassFlagLabel = new Label(this.frameHeaderComposite,SWT.LEFT);
		this.bypassFlagLabel.setText("Bypass: ");
		final FormData bflFormData = new FormData();
		bflFormData.top = new FormAttachment(0,5);
		bflFormData.left = new FormAttachment(55,10);
		bflFormData.right = new FormAttachment(70);
		this.bypassFlagLabel.setLayoutData(bflFormData);

		this.bypassFlagCombo = new Combo(this.frameHeaderComposite,SWT.LEFT | SWT.READ_ONLY | SWT.BORDER);
		for(long i = ITcTransferFrameSerializer.BYPASS_FLAG_MIN_VALUE; i <= ITcTransferFrameSerializer.BYPASS_FLAG_MAX_VALUE; i++) {
			this.bypassFlagCombo.add(Long.toString(i));
		}
		this.bypassFlagCombo.setText(Long.toString(frame.getBypassFlag()));
		final FormData bfcFormData = new FormData();
		bfcFormData.top = new FormAttachment(this.bypassFlagLabel,0,SWT.CENTER);
		bfcFormData.left = new FormAttachment(70,5);
		bfcFormData.right = new FormAttachment(100);
		this.bypassFlagCombo.setLayoutData(bfcFormData);

		this.ccFlagLabel = new Label(this.frameHeaderComposite,SWT.LEFT);
		this.ccFlagLabel.setText("C/C Flag: ");
		final FormData cflFormData = new FormData();
		cflFormData.top = new FormAttachment(this.versionNumberLabel,verticalSpacing);
		cflFormData.left = new FormAttachment(0);
		cflFormData.right = new FormAttachment(20);
		this.ccFlagLabel.setLayoutData(cflFormData);

		this.ccFlagCombo = new Combo(this.frameHeaderComposite,SWT.LEFT | SWT.READ_ONLY | SWT.BORDER);
		for(long i = ITcTransferFrameSerializer.CONTROL_COMMAND_FLAG_MIN_VALUE; i <= ITcTransferFrameSerializer.CONTROL_COMMAND_FLAG_MAX_VALUE; i++) {
			this.ccFlagCombo.add(Long.toString(i));
		}
		this.ccFlagCombo.setText(Long.toString(frame.getControlCommandFlag()));
		final FormData cfcFormData = new FormData();
		cfcFormData.top = new FormAttachment(this.ccFlagLabel,0,SWT.CENTER);
		cfcFormData.left = new FormAttachment(20,5);
		cfcFormData.right = new FormAttachment(55,-10);
		this.ccFlagCombo.setLayoutData(cfcFormData);

		this.spareLabel = new Label(this.frameHeaderComposite,SWT.LEFT);
		this.spareLabel.setText("Spare: ");
		final FormData slFormData = new FormData();
		slFormData.top = new FormAttachment(this.bypassFlagLabel,verticalSpacing);
		slFormData.left = new FormAttachment(55,10);
		slFormData.right = new FormAttachment(70);
		this.spareLabel.setLayoutData(slFormData);

		this.spareCombo = new Combo(this.frameHeaderComposite,SWT.LEFT | SWT.READ_ONLY | SWT.BORDER);
		for(long i = ITcTransferFrameSerializer.SPARE_MIN_VALUE; i <= ITcTransferFrameSerializer.SPARE_MAX_VALUE; i++) {
			this.spareCombo.add(Long.toString(i));
		}
		this.spareCombo.setText(Long.toString(frame.getSpare()));
		final FormData scFormData = new FormData();
		scFormData.top = new FormAttachment(this.spareLabel,0,SWT.CENTER);
		scFormData.left = new FormAttachment(70,5);
		scFormData.right = new FormAttachment(100);
		this.spareCombo.setLayoutData(scFormData);

		this.rceStringLabel = new Label(this.frameHeaderComposite,SWT.LEFT);
		this.rceStringLabel.setText("Exec String: ");
		final FormData rslFormData = new FormData();
		rslFormData.top = new FormAttachment(this.ccFlagLabel,verticalSpacing);
		rslFormData.left = new FormAttachment(0);
		rslFormData.right = new FormAttachment(20);
		this.rceStringLabel.setLayoutData(rslFormData);

		this.rceStringCombo = new Combo(this.frameHeaderComposite,SWT.LEFT | SWT.READ_ONLY | SWT.BORDER);
		for(long i = ITcTransferFrameSerializer.EXECUTION_STRING_MIN_VALUE; i <= ITcTransferFrameSerializer.EXECUTION_STRING_MAX_VALUE; i++) {
			final ExecutionStringType est = ExecutionStringType.getTypeFromVcidValue(appContext, i);
			if(est == null) {
				this.rceStringCombo.add(i + " (N/A)");
			} else {
				this.rceStringCombo.add(i + " (" + est.toString() + ")");
			}
		}
		this.rceStringCombo.setText(this.rceStringCombo.getItem(frame.getExecutionString()));
		final FormData rscFormData = new FormData();
		rscFormData.top = new FormAttachment(this.rceStringLabel,0,SWT.CENTER);
		rscFormData.left = new FormAttachment(20,5);
		rscFormData.right = new FormAttachment(55,-10);
		this.rceStringCombo.setLayoutData(rscFormData);

		this.vcNumberLabel = new Label(this.frameHeaderComposite,SWT.LEFT);
		this.vcNumberLabel.setText("VC #: ");
		final FormData vcnlFormData = new FormData();
		vcnlFormData.top = new FormAttachment(this.spareLabel,verticalSpacing);
		vcnlFormData.left = new FormAttachment(55,10);
		vcnlFormData.right = new FormAttachment(70);
		this.vcNumberLabel.setLayoutData(vcnlFormData);

		this.vcNumberCombo = new Combo(this.frameHeaderComposite,SWT.LEFT | SWT.READ_ONLY | SWT.BORDER);
		for(long i = ITcTransferFrameSerializer.VIRTUAL_CHANNEL_NUMBER_MIN_VALUE; i <= ITcTransferFrameSerializer.VIRTUAL_CHANNEL_NUMBER_MAX_VALUE; i++) {
			final VirtualChannelType vct = VirtualChannelType.getTypeFromNumber(appContext.getBean(CommandFrameProperties.class), i);
			if(vct.equals(VirtualChannelType.UNKNOWN)) {
				this.vcNumberCombo.add(i + " (N/A)");
			} else {
				this.vcNumberCombo.add(i + " (" + vct.toShortString() + ")");
			}
		}
		this.vcNumberCombo.setText(this.vcNumberCombo.getItem(frame.getVirtualChannelNumber()));
		final FormData vcncFormData = new FormData();
		vcncFormData.top = new FormAttachment(this.vcNumberLabel,0,SWT.CENTER);
		vcncFormData.left = new FormAttachment(70,5);
		vcncFormData.right = new FormAttachment(100);
		this.vcNumberCombo.setLayoutData(vcncFormData);
		this.vcNumberCombo.addSelectionListener(this.handler);

		this.scidLabel = new Label(this.frameHeaderComposite,SWT.LEFT);
		this.scidLabel.setText("Spacecraft ID:\n(" + ITcTransferFrameSerializer.SPACECRAFT_ID_MIN_VALUE + "-" +
				               ITcTransferFrameSerializer.SPACECRAFT_ID_MAX_VALUE + ")");
		final FormData scidlFormData = new FormData();
		scidlFormData.top = new FormAttachment(this.rceStringLabel,verticalSpacing);
		scidlFormData.left = new FormAttachment(0);
		scidlFormData.right = new FormAttachment(20);
		this.scidLabel.setLayoutData(scidlFormData);

		this.scidText = new Text(this.frameHeaderComposite,SWT.LEFT | SWT.BORDER);
		this.scidText.setFont(getTextFieldFont());
		this.scidText.setText(Integer.toString(frame.getSpacecraftId()));
		final FormData scidtFormData = new FormData();
		scidtFormData.top = new FormAttachment(this.scidLabel,0,SWT.CENTER);
		scidtFormData.left = new FormAttachment(20,5);
		scidtFormData.right = new FormAttachment(55,-10);
		this.scidText.setLayoutData(scidtFormData);

		this.sequenceNumberLabel = new Label(this.frameHeaderComposite,SWT.LEFT);
		this.sequenceNumberLabel.setText("Seq #:\n(" + ITcTransferFrameSerializer.SEQUENCE_NUMBER_MIN_VALUE + "-" +
										ITcTransferFrameSerializer.SEQUENCE_NUMBER_MAX_VALUE + ")");
		final FormData snlFormData = new FormData();
		snlFormData.top = new FormAttachment(this.vcNumberLabel,verticalSpacing);
		snlFormData.left = new FormAttachment(55,10);
		snlFormData.right = new FormAttachment(70);
		this.sequenceNumberLabel.setLayoutData(snlFormData);

		this.sequenceNumberText = new Text(this.frameHeaderComposite,SWT.LEFT | SWT.BORDER);
		this.sequenceNumberText.setFont(getTextFieldFont());
		this.sequenceNumberText.setText(Integer.toString(frame.getSequenceNumber()));
		final FormData sntFormData = new FormData();
		sntFormData.top = new FormAttachment(this.sequenceNumberLabel,0,SWT.CENTER);
		sntFormData.left = new FormAttachment(70,5);
		sntFormData.right = new FormAttachment(100);
		this.sequenceNumberText.setLayoutData(sntFormData);

		this.lengthLabel = new Label(this.frameHeaderComposite,SWT.LEFT);
		this.lengthLabel.setText("Length:\n(" + ITcTransferFrameSerializer.LENGTH_MIN_VALUE + "-" +
				                 ITcTransferFrameSerializer.LENGTH_MAX_VALUE + ")");
		final FormData llFormData = new FormData();
		llFormData.top = new FormAttachment(this.sequenceNumberLabel,verticalSpacing);
		llFormData.left = new FormAttachment(0);
		llFormData.right = new FormAttachment(20);
        this.lengthLabel.setLayoutData(llFormData);

		this.lengthText = new Text(this.frameHeaderComposite,SWT.LEFT | SWT.BORDER);
		this.lengthText.setFont(getTextFieldFont());
		this.lengthText.setText(frame.getLength() != 0 ? Integer.toString(frame.getLength()) : Integer.toString(frameSerializer.calculateLength(frame)));
		final FormData ltFormData = new FormData();
		ltFormData.top = new FormAttachment(this.lengthLabel,0,SWT.CENTER);
		ltFormData.left = new FormAttachment(20,5);
		ltFormData.right = new FormAttachment(70);
		this.lengthText.setLayoutData(ltFormData);

		this.lengthButton = new Button(this.frameHeaderComposite,SWT.PUSH | SWT.BORDER);
		this.lengthButton.setText("Recalculate");
        final FormData lbFormData = new FormData();
        lbFormData.top = new FormAttachment(this.lengthLabel,0,SWT.TOP);
        lbFormData.left = new FormAttachment(70,5);
        // lbFormData.right = new FormAttachment(100);
        this.lengthButton.setLayoutData(lbFormData);
        this.lengthButton.addSelectionListener(this.handler);

        this.frameHeaderComposite.setTabList(new Control[]{
        		this.versionNumberCombo,
				this.bypassFlagCombo,
				this.ccFlagCombo,
				this.spareCombo,
				this.rceStringCombo,
				this.vcNumberCombo,
				this.scidText,
				this.sequenceNumberText,
				this.lengthText
		});
	}

	private void displayFrameBody(final ITcTransferFrame frame) {
		this.frameBodyComposite = new Composite(this.frameComposite,SWT.NONE);
		final FormData fbcFormData = new FormData();
		fbcFormData.top = new FormAttachment(51,10);
		fbcFormData.bottom = new FormAttachment(74,-10);
		fbcFormData.left = new FormAttachment(0);
		fbcFormData.right = new FormAttachment(100);
		this.frameBodyComposite.setLayoutData(fbcFormData);
		this.frameBodyComposite.setLayout(new FormLayout());

		this.frameDataLabel = new Label(this.frameBodyComposite,SWT.LEFT);
		this.frameDataLabel.setText("Data (Hex): ");
		final FormData fdlFormData = new FormData();
		fdlFormData.top = new FormAttachment(0);
		fdlFormData.left = new FormAttachment(0);
		fdlFormData.right = new FormAttachment(20);
		this.frameDataLabel.setLayoutData(fdlFormData);

		this.frameDataText = new Text(this.frameBodyComposite,SWT.LEFT | SWT.V_SCROLL | SWT.WRAP | SWT.MULTI);
		this.frameDataText.setFont(getTextFieldFont());
		final byte[] data = frame.getData();
		this.frameDataText.setText(BinOctHexUtility.toHexFromBytes(data));
		final FormData fdtFormData = new FormData();
		fdtFormData.top = new FormAttachment(this.frameDataLabel,0,SWT.TOP);
		fdtFormData.left = new FormAttachment(20,5);
		fdtFormData.right = new FormAttachment(100);
		fdtFormData.bottom = new FormAttachment(100);
		this.frameDataText.setLayoutData(fdtFormData);

		this.frameBodyComposite.setTabList(new Control[] { this.frameDataText });
	}

	private void displayFrameTrailer(final ITcTransferFrame frame) {
		final int verticalSpacing = 20;

		this.frameTrailerComposite = new Composite(this.frameComposite,SWT.NONE);
		final FormData ftcFormData = new FormData();
		ftcFormData.top = new FormAttachment(76);
		ftcFormData.bottom = new FormAttachment(100);
		ftcFormData.left = new FormAttachment(0);
		ftcFormData.right = new FormAttachment(100);
		this.frameTrailerComposite.setLayoutData(ftcFormData);
		this.frameTrailerComposite.setLayout(new FormLayout());

		this.hasFecfLabel = new Label(this.frameTrailerComposite,SWT.LEFT);
		this.hasFecfLabel.setText("Has FECF: ");
		final FormData vnlFormData = new FormData();
		vnlFormData.top = new FormAttachment(0,verticalSpacing);
		vnlFormData.left = new FormAttachment(0);
		vnlFormData.right = new FormAttachment(20);
		this.hasFecfLabel.setLayoutData(vnlFormData);

		this.hasFecfCombo = new Combo(this.frameTrailerComposite,SWT.LEFT | SWT.READ_ONLY | SWT.BORDER);
		this.hasFecfCombo.add(Boolean.FALSE.toString());
		this.hasFecfCombo.add(Boolean.TRUE.toString());
		this.hasFecfCombo.setText(frame.hasFecf() ? Boolean.TRUE.toString() : Boolean.FALSE.toString());
		final FormData vncFormData = new FormData();
		vncFormData.top = new FormAttachment(this.hasFecfLabel,0,SWT.CENTER);
		vncFormData.left = new FormAttachment(20,5);
		vncFormData.right = new FormAttachment(55,-10);
		this.hasFecfCombo.setLayoutData(vncFormData);
		this.hasFecfCombo.addSelectionListener(this.handler);

		this.fecfLabel = new Label(this.frameTrailerComposite,SWT.LEFT);
		this.fecfLabel.setText("FECF (Hex):\n(" + (frameConfig.getFecfLength()*8) + " bits)");
		final FormData flFormData = new FormData();
		flFormData.top = new FormAttachment(this.hasFecfLabel,verticalSpacing);
		flFormData.left = new FormAttachment(0,5);
		flFormData.right = new FormAttachment(20);
		this.fecfLabel.setLayoutData(flFormData);

		this.fecfText = new Text(this.frameTrailerComposite,SWT.LEFT | SWT.BORDER);
		this.fecfText.setFont(getTextFieldFont());
		this.fecfText.setEnabled(frame.hasFecf());
		if(frame.hasFecf()) {
			final byte[] fecf = frame.getFecf().length > 0 ? frame.getFecf() : frameSerializer.calculateFecf(frame);
			this.fecfText.setText(BinOctHexUtility.toHexFromBytes(fecf));
		}
		final FormData ftFormData = new FormData();
		ftFormData.top = new FormAttachment(this.fecfLabel,0,SWT.CENTER);
		ftFormData.left = new FormAttachment(20,5);
		ftFormData.right = new FormAttachment(70);
		this.fecfText.setLayoutData(ftFormData);

		this.fecfButton = new Button(this.frameTrailerComposite,SWT.PUSH | SWT.BORDER);
		this.fecfButton.setText("Recalculate");
		this.fecfButton.setEnabled(frame.hasFecf());
        final FormData fbFormData = new FormData();
        fbFormData.top = new FormAttachment(this.fecfLabel,0,SWT.TOP);
        fbFormData.left = new FormAttachment(70,5);
        // fbFormData.right = new FormAttachment(100);
        this.fecfButton.setLayoutData(fbFormData);
        this.fecfButton.addSelectionListener(this.handler);

        this.frameTrailerComposite.setTabList(new Control[] { this.hasFecfCombo, this.fecfText });
	}

	private ITcTransferFrame getSelectedFrame() {
		final int selection = this.frameList.getSelectionIndex();
		if(selection == -1) {
			return(null);
		}

		final ITcTransferFrame frame = this.frames.get(selection);

		return(frame);
	}

	/* (non-Javadoc)
	 * @see jpl.gds.tcapp.app.gui.fault.FaultInjectorGuiComponent#getDescription()
	 */
	@Override
	public String getDescription() {
		return(FAULT_PAGE_DESC);
	}

	/* (non-Javadoc)
	 * @see jpl.gds.tcapp.app.gui.fault.FaultInjectorGuiComponent#getTitle()
	 */
	@Override
	public String getTitle() {
		return(FAULT_PAGE_TITLE);
	}

	/* (non-Javadoc)
	 * @see jpl.gds.tcapp.app.gui.fault.FaultInjectorGuiComponent#getCurrentState()
	 */
	@Override
	public FaultInjectionState getCurrentState() {
		return(this.dataState);
	}

	/* (non-Javadoc)
	 * @see jpl.gds.tcapp.app.gui.fault.FaultInjectorGuiComponent#setFromState(jpl.gds.tcapp.app.gui.fault.FaultInjectionState)
	 */
	@Override
	public void setFromState(final FaultInjectionState state) {
		this.dataState = state;
	}

	/* (non-Javadoc)
	 * @see jpl.gds.tcapp.app.gui.fault.FaultInjectorGuiComponent#updateState(jpl.gds.tcapp.app.gui.fault.FaultInjectionState)
	 */
	@Override
	public void updateState() throws FaultInjectorException {
		try {
			saveCurrentFrame();
		} catch(final FrameWrapUnwrapException e) {
			throw new FaultInjectorException(e.getMessage(),e);
		}

		this.dataState.frames = this.frames;
		this.dataState.selectedFrameIndex = this.currentFrame;

		this.dataState.cltus = null;
		this.dataState.selectedCltuIndex = -1;
		this.dataState.rawOutputHex = null;
	}

	/*
	 * (non-Javadoc)
	 * @see jpl.gds.tcapp.app.gui.fault.FaultInjectorGuiComponent#updateDisplay()
	 */
	@Override
	public void updateDisplay() throws FaultInjectorException {
		if(this.dataState.frames != null) {
			this.frames = this.dataState.frames;
			populateFrameList();
			if(this.frames.size() > 0 && this.dataState.selectedFrameIndex == -1) {
				this.frameList.setSelection(0);
			}
			this.frameList.setSelection(this.dataState.selectedFrameIndex);
			this.frameList.showSelection();
			displaySelectedFrame();
			return;
		} else if(this.dataState.frames == null && this.dataState.command != null) {

			try {
				this.frames = tewUtility.wrapCommandToFrames(this.dataState.command);
			} catch (FrameWrapUnwrapException e) {
				throw new FaultInjectorException(e.getMessage(), e);
			}

			this.dataState.frames = this.frames;

			populateFrameList();

			if(this.frames.size() > 0) {
				this.frameList.setSelection(0);
				this.frameList.showSelection();
				displaySelectedFrame();
			}

			return;
		}

		throw new IllegalStateException(getTitle() + " display does not have enough information to construct the editor GUI.");
	}

	/*
	 * (non-Javadoc)
	 * @see jpl.gds.tcapp.app.gui.fault.FaultInjectorGuiComponent#destroy()
	 */
	@Override
	public void destroy() {
		this.dispose();
	}

	/*
	 * (non-Javadoc)
	 * @see jpl.gds.tcapp.app.gui.fault.FaultInjectorGuiComponent#getTextFieldFont()
	 */
	@Override
	public Font getTextFieldFont() {
		return(new Font(getDisplay(), new FontData(FONT_NAME, 14, SWT.NONE)));
	}

	private void destroyFrameComposite() {
		if(this.frameComposite != null) {
			this.frameComposite.dispose();
		}

		this.frameComposite = null;
		this.versionNumberLabel = null;
		this.versionNumberCombo = null;
		this.bypassFlagLabel = null;
		this.bypassFlagCombo = null;
		this.ccFlagLabel = null;
		this.ccFlagCombo = null;
		this.spareLabel = null;
		this.spareCombo = null;
		this.rceStringLabel = null;
		this.rceStringCombo = null;
		this.vcNumberLabel = null;
		this.vcNumberCombo = null;
		this.scidLabel = null;
		this.scidText = null;
		this.sequenceNumberLabel = null;
		this.sequenceNumberText = null;
		this.lengthLabel = null;
		this.lengthText = null;
		this.lengthButton = null;
		this.frameDataLabel = null;
		this.frameDataText = null;
		this.hasFecfLabel = null;
		this.hasFecfCombo = null;
		this.fecfLabel = null;
		this.fecfText = null;
		this.fecfButton = null;
		this.currentFrame = -1;

		setButtonsEnable(false);
	}

	private void populateFrameList() {
		int maxOrderId = 0;
		for(final ITcTransferFrame frame : this.frames) {
			final Integer orderId = frame.getOrderId();
			if(orderId != null) {
				final int oi = orderId.intValue();
				if(oi > maxOrderId) {
					maxOrderId = oi;
				}
			}
		}

		final ArrayList<String> newFrameList = new ArrayList<String>(256);
		for(int i=0; i < this.frames.size(); i++) {
			final ITcTransferFrame frame = this.frames.get(i);

			final Integer orderId = frame.getOrderId();
			if(orderId == null) {
				frame.setOrderId(++maxOrderId);
			}

			final byte vcNumber = frame.getVirtualChannelNumber();
			final VirtualChannelType vct = VirtualChannelType.getTypeFromNumber(appContext.getBean(CommandFrameProperties.class), vcNumber);
			if(!vct.equals(VirtualChannelType.UNKNOWN)) {
				newFrameList.add("VC-" + vcNumber + " (" + vct.toShortString() + ") Frame #" + frame.getOrderId());
			} else {
				newFrameList.add("VC-" + vcNumber + " (N/A) Frame #" + frame.getOrderId());
			}
		}

		this.frameList.setItems(newFrameList.toArray(new String[] {}));
	}

	private void saveCurrentFrame() throws FrameWrapUnwrapException {
		boolean errorsFound = false;
		final StringBuilder errors = new StringBuilder(1024);

		if(FrameEditorComposite.this.currentFrame != -1) {
			final ITcTransferFrame frame = FrameEditorComposite.this.frames.get(FrameEditorComposite.this.currentFrame);

			//it's not possible to choose bad values for these fields (they're all uneditable combo boxes)
			frame.setVersionNumber(Byte.parseByte(FrameEditorComposite.this.versionNumberCombo.getText()));
			frame.setBypassFlag(Byte.parseByte(FrameEditorComposite.this.bypassFlagCombo.getText()));
			frame.setControlCommandFlag(Byte.parseByte(FrameEditorComposite.this.ccFlagCombo.getText()));
			frame.setSpare(Byte.parseByte(FrameEditorComposite.this.spareCombo.getText()));
			frame.setExecutionString((byte)FrameEditorComposite.this.rceStringCombo.getSelectionIndex());
			frame.setVirtualChannelNumber((byte)FrameEditorComposite.this.vcNumberCombo.getSelectionIndex());

			try {
				frame.setSpacecraftId(Integer.parseInt(GDR.removeWhitespaceFromString(FrameEditorComposite.this.scidText.getText())));
			} catch(final Exception nfe) {
				errors.append("Spacecraft ID\n");
				errorsFound = true;
			}

			try {
				frame.setSequenceNumber(Integer.parseInt(GDR.removeWhitespaceFromString(FrameEditorComposite.this.sequenceNumberText.getText())));
			} catch(final Exception nfe) {
				errors.append("Seq #\n");
				errorsFound = true;
			}

			try {
				frame.setLength(Integer.parseInt(GDR.removeWhitespaceFromString(FrameEditorComposite.this.lengthText.getText())));
			} catch(final Exception nfe) {
				errors.append("Length\n");
				errorsFound = true;
			}

			try {
				frame.setData(BinOctHexUtility.toBytesFromHex(GDR.removeWhitespaceFromString(FrameEditorComposite.this.frameDataText.getText())));
			} catch(final Exception iae) {
				errors.append("Data (Hex)\n");
				errorsFound = true;
			}

			//this field can't be bad (it's an uneditable combo)
			final boolean hasFecf = GDR.getBooleanFromInt(FrameEditorComposite.this.hasFecfCombo.getSelectionIndex());
			frame.setHasFecf(hasFecf);

			if(hasFecf) {
				try {
					frame.setFecf(BinOctHexUtility.toBytesFromHex(GDR.removeWhitespaceFromString(FrameEditorComposite.this.fecfText.getText())));
				} catch(final Exception iae) {
					errors.append("FECF (Hex)\n");
					errorsFound = true;
				}
			}
		}

		if(errorsFound) {
			throw new FrameWrapUnwrapException("The following telecommand frame fields contain unusable values: \n\n" + errors.toString());
		}
	}

	/**
	 * The Frame Editor Composite event handler
	 */
	private class EventHandler extends SelectionAdapter {
		public EventHandler()
		{
			super();
		}

		/* (non-Javadoc)
		 * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
		 */
		@Override
		public void widgetSelected(final SelectionEvent se) {
			Object source = se.getSource();
			if(source == FrameEditorComposite.this.deleteButton) {
				deleteFrame();
			} else if(source == FrameEditorComposite.this.vcNumberCombo) {
				updateVcNumber();
			}

			boolean foundError = false;
			try {
				saveCurrentFrame();
			} catch(final FrameWrapUnwrapException e) {
				se.doit = false;
				SWTUtilities.showErrorDialog(getShell(),"Frame Format Error",e.getMessage());
				foundError = true;
			}

			if(source == FrameEditorComposite.this.upButton && !foundError) {
				moveFrameUp();
			} else if(source == FrameEditorComposite.this.downButton && !foundError) {
				moveFrameDown();
			} else if(source == FrameEditorComposite.this.newButton && !foundError) {
				addNewFrame();
			} else if(source == FrameEditorComposite.this.copyButton && !foundError) {
				copyCurrentFrame();
			} else if(source == FrameEditorComposite.this.frameList && !foundError) {
				displaySelectedFrame();
			} else if(source == FrameEditorComposite.this.lengthButton && !foundError) {
				calculateFrameLength();
			} else if(source == FrameEditorComposite.this.fecfButton && !foundError) {
				calculateFrameFecf();
			} else if(source == FrameEditorComposite.this.hasFecfCombo) {
				enableFrameFecf(foundError);
			}
		}

		private void displayFrameAtIndex(final int position) {
			populateFrameList();
			FrameEditorComposite.this.frameList.setSelection(position);
			frameList.showSelection();
			displaySelectedFrame();
		}

		private void deleteFrame() {
			final int position = FrameEditorComposite.this.frameList.getSelectionIndex();
			if(position == -1) {
				return;
			}

			FrameEditorComposite.this.frames.remove(position);
			populateFrameList();

			final int newPosition = (position-1) > 0 ? (position-1) : 0;
			if(FrameEditorComposite.this.frames.size() > 0) {
				FrameEditorComposite.this.frameList.setSelection(newPosition);
				frameList.showSelection();
				displaySelectedFrame();
			} else {
				destroyFrameComposite();
			}
		}

		private void updateVcNumber() {
			final int selectedFrameIndex = FrameEditorComposite.this.frameList.getSelectionIndex();
			final String newVcNumber = FrameEditorComposite.this.vcNumberCombo.getText();
			FrameEditorComposite.this.frameList.setItem(selectedFrameIndex,"VC-" + newVcNumber + " Frame");
		}

		private void moveFrameUp() {
			moveFrame(false);
		}

		private void moveFrameDown() {
			moveFrame(true);
		}

		private void moveFrame(boolean down) {
			int delta = down ? 1 : -1;

			final int oldPosition = FrameEditorComposite.this.frameList.getSelectionIndex();
			final int newPosition = oldPosition + delta;
			if(oldPosition == -1 ||
					oldPosition == (down ? (FrameEditorComposite.this.frames.size()-1) : 0 )) {
				return;
			}

			Collections.swap(FrameEditorComposite.this.frames,oldPosition,newPosition);
			displayFrameAtIndex(newPosition);

		}

		private void addNewFrame() {
			final int position = FrameEditorComposite.this.frameList.getSelectionIndex();

			/* updated to place begin delimiter
			 * frame before all data or after an end delimiter frame and
			 * place end delimiter frame after any other type of frame.
			 */
			ITcTransferFrame frame;
			final ITcTransferFrame current = FrameEditorComposite.this.frames.get(position);
			final ITcTransferFrame begin = frameFactory.createBeginDelimiterFrame();
			final ITcTransferFrame end = frameFactory.createEndDelimiterFrame();

			if(current.getVirtualChannelId() == end.getVirtualChannelId() && Arrays.equals(current.getData(), end.getData())){
				frame = begin;
			} else {
				frame = end;
			}

			FrameEditorComposite.this.frames.add(position+1,frame);
			displayFrameAtIndex(position + 1);
		}

		private void copyCurrentFrame() {
			final int position = FrameEditorComposite.this.frameList.getSelectionIndex();
			if(position == -1) {
				return;
			}

			final ITcTransferFrame frame = getSelectedFrame().copy();
			FrameEditorComposite.this.frames.add(position+1,frame);
			displayFrameAtIndex(position + 1);
		}

		private void calculateFrameLength() {
			final ITcTransferFrame frame = getSelectedFrame();
			final short newLength = frameSerializer.calculateLength(frame);
			FrameEditorComposite.this.lengthText.setText(Short.toString(newLength));
		}

		private void calculateFrameFecf() {
			final ITcTransferFrame frame = getSelectedFrame();
			byte[] newFecf = new byte[0];

			try {
				newFecf = frameSerializer.calculateFecf(frame,
						frameConfig.getChecksumCalcluator(frame.getVirtualChannelNumber()));
			} catch (Exception e) {
				tracer.info("Encountered an exception while attempting to calculate new FECF for frame using CTS: " + ExceptionTools.getMessage(e));
			}

			if(newFecf == null || newFecf.length == 0) {
				tracer.info("Computing frame FECF with legacy AMPCS frame serializer");
                newFecf = legacyFrameSerializer.calculateFecf(frame,
                        frameConfig.getChecksumCalcluator(frame.getVirtualChannelNumber()));
                tracer.info("FECF calculation with legacy code successful");
			}

			FrameEditorComposite.this.fecfText.setText(BinOctHexUtility.toHexFromBytes(newFecf));
		}

		private void enableFrameFecf(final boolean foundError) {
			final boolean hasFecf = GDR.getBooleanFromInt(FrameEditorComposite.this.hasFecfCombo.getSelectionIndex());
			FrameEditorComposite.this.fecfText.setEnabled(hasFecf);
			FrameEditorComposite.this.fecfButton.setEnabled(hasFecf);

			if(!foundError) {
				final ITcTransferFrame frame = getSelectedFrame();

				frame.setFecf(new byte[0]);
				if(hasFecf) {
					calculateFrameFecf();
				} else {
					FrameEditorComposite.this.fecfText.setText("");
				}
			}
		}
	}
}
