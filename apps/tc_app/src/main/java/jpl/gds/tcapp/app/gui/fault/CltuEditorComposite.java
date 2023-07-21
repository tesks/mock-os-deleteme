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
import java.util.Collections;

import jpl.gds.shared.checksum.BchAlgorithm;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.tc.api.ITewUtility;
import jpl.gds.tc.api.cltu.IBchCodeBlockBuilder;
import jpl.gds.tc.api.config.CommandFrameProperties;
import jpl.gds.tc.api.config.VirtualChannelType;
import jpl.gds.tc.api.exception.CltuEndecException;
import jpl.gds.tc.api.exception.CommandParseException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
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
import jpl.gds.tc.api.IBchCodeblock;
import jpl.gds.tc.api.cltu.ICltu;
import jpl.gds.tc.api.cltu.ICltuFactory;
import jpl.gds.tc.api.ICommandObjectFactory;
import jpl.gds.tc.api.config.PlopProperties;
import jpl.gds.tc.api.plop.ICommandLoadBuilder;

/**
 * This class is a SWT composite that displays widgets for editing a CLTUs
 * 
 */
public final class CltuEditorComposite extends Composite implements
		FaultInjectorGuiComponent {
	private static final String FONT_NAME = "Helvetica";

	private static final String FAULT_PAGE_TITLE = "CLTU Editor";
	private static final String FAULT_PAGE_DESC = "Add, remove, reorder, and edit CLTUs.";

	private FaultInjectionState dataState = null;

	private List cltuList = null;
	private Button upButton = null;
	private Button downButton = null;
	private Button newButton = null;
	private Button copyButton = null;
	private Button deleteButton = null;

	private Group rightPanel = null;
	private Composite cltuComposite = null;
	private Text acqSeqText = null;
	private Text startSeqText = null;
	private Text tailSeqText = null;
	private Text idleSeqText = null;

	private Label[] numberLabelFields = null;
	private Text[] dataTextFields = null;
	private Text[] edacTextFields = null;
	private Button[] edacButtonFields = null;

	private final EventHandler handler;

	private java.util.List<ICltu> cltus = null;
	private int currentCltu = -1;
	
	private final ApplicationContext appContext;
    private final ICltuFactory cltuBuilder;

	/**
	 * Constructor
	 * 
	 * @param parent parent composite
	 * @param appContext the application context in which this editor composite is running
	 */
	public CltuEditorComposite(final ApplicationContext appContext, final Composite parent) {
		super(parent, SWT.NONE);

		this.handler = new EventHandler();

		this.appContext = appContext;
        this.cltuBuilder = appContext.getBean(ICltuFactory.class);
		
		createControls();

		setTabList(new Control[] { this.cltuList, this.rightPanel });
		layout(true);
	}

	private void createControls() {
		setLayout(new FormLayout());

		createLeftPanel();
		createRightPanel();
	}

	private void createLeftPanel() {
		final Label cltuListLabel = new Label(this, SWT.LEFT);
		cltuListLabel.setText("Command Load CLTUs: ");
		final FormData cllFormData = new FormData();
		cllFormData.top = new FormAttachment(0, 15);
		cllFormData.left = new FormAttachment(0, 10);
		cllFormData.right = new FormAttachment(35);
		cltuListLabel.setLayoutData(cllFormData);

		this.cltuList = new List(this, SWT.SINGLE | SWT.BORDER | SWT.V_SCROLL);
		final FormData clFormData = new FormData();
		clFormData.top = new FormAttachment(cltuListLabel, 15);
		clFormData.left = new FormAttachment(0, 10);
		clFormData.right = new FormAttachment(35);
		clFormData.bottom = new FormAttachment(80);
		this.cltuList.setLayoutData(clFormData);
		this.cltuList.addSelectionListener(this.handler);

		final Composite listButtonComposite = new Composite(this, SWT.NONE);
		final FormData lbcFormData = new FormData();
		lbcFormData.top = new FormAttachment(this.cltuList, 10);
		lbcFormData.bottom = new FormAttachment(100);
		lbcFormData.left = new FormAttachment(0, 10);
		lbcFormData.right = new FormAttachment(35);

		listButtonComposite.setLayout(new FormLayout());
		listButtonComposite.setLayoutData(lbcFormData);

		final int buttonSpacing = 5;

		this.upButton = new Button(listButtonComposite, SWT.PUSH);
		this.upButton.setText("Up");
		final FormData ubFormData = new FormData();
		ubFormData.top = new FormAttachment(0, buttonSpacing);
		ubFormData.bottom = new FormAttachment(50, -1 * buttonSpacing);
		ubFormData.left = new FormAttachment(6);
		ubFormData.right = new FormAttachment(47);
		this.upButton.setLayoutData(ubFormData);
		this.upButton.addSelectionListener(this.handler);

		this.downButton = new Button(listButtonComposite, SWT.PUSH);
		this.downButton.setText("Down");
		final FormData dbFormData = new FormData();
		dbFormData.top = new FormAttachment(0, buttonSpacing);
		dbFormData.bottom = new FormAttachment(50, -1 * buttonSpacing);
		dbFormData.left = new FormAttachment(53);
		dbFormData.right = new FormAttachment(94);
		this.downButton.setLayoutData(dbFormData);
		this.downButton.addSelectionListener(this.handler);

		this.newButton = new Button(listButtonComposite, SWT.PUSH);
		this.newButton.setText("New");
		final FormData nbFormData = new FormData();
		nbFormData.top = new FormAttachment(50, buttonSpacing);
		nbFormData.bottom = new FormAttachment(100, -1 * buttonSpacing);
		nbFormData.left = new FormAttachment(5);
		nbFormData.right = new FormAttachment(30);
		this.newButton.setLayoutData(nbFormData);
		this.newButton.addSelectionListener(this.handler);

		this.copyButton = new Button(listButtonComposite, SWT.PUSH);
		this.copyButton.setText("Copy");
		final FormData cbFormData = new FormData();
		cbFormData.top = new FormAttachment(50, buttonSpacing);
		cbFormData.bottom = new FormAttachment(100, -1 * buttonSpacing);
		cbFormData.left = new FormAttachment(34);
		cbFormData.right = new FormAttachment(60);
		this.copyButton.setLayoutData(cbFormData);
		this.copyButton.addSelectionListener(this.handler);

		this.deleteButton = new Button(listButtonComposite, SWT.PUSH);
		this.deleteButton.setText("Delete");
		final FormData delFormData = new FormData();
		delFormData.top = new FormAttachment(50, buttonSpacing);
		delFormData.bottom = new FormAttachment(100, -1 * buttonSpacing);
		delFormData.left = new FormAttachment(64);
		delFormData.right = new FormAttachment(95);
		this.deleteButton.setLayoutData(delFormData);
		this.deleteButton.addSelectionListener(this.handler);
	}

	private void createRightPanel() {
		this.rightPanel = new Group(this, SWT.NONE);
		final FontData groupFontData = new FontData("Helvetica", 14, SWT.BOLD);
		final Font groupFont = new Font(getDisplay(), groupFontData);
		this.rightPanel.setFont(groupFont);
		final FormLayout rpLayout = new FormLayout();
		rpLayout.marginTop = 10;
		rpLayout.marginBottom = 10;
		rpLayout.marginLeft = 0;
		rpLayout.marginRight = 0;
		this.rightPanel.setLayout(rpLayout);

		// position the right panel in the parent shell
		final FormData rpFormData = new FormData();
		rpFormData.left = new FormAttachment(this.cltuList, 20);
		rpFormData.top = new FormAttachment(0, 15);
		rpFormData.right = new FormAttachment(100, -10);
		rpFormData.bottom = new FormAttachment(100);
		this.rightPanel.setLayoutData(rpFormData);
	}

	private void setButtonsEnable(final boolean enabled) {
		this.upButton.setEnabled(enabled);
		this.downButton.setEnabled(enabled);
		this.newButton.setEnabled(enabled);
		this.copyButton.setEnabled(enabled);
		this.deleteButton.setEnabled(enabled);
	}

	private void displaySelectedCltu() {
		destroyCltuComposite();

		this.currentCltu = this.cltuList.getSelectionIndex();
		final ICltu cltu = getSelectedCltu();
		if (cltu == null) {
			return;
		}

		this.cltuComposite = new Composite(this.rightPanel, SWT.NONE);
		this.rightPanel.setTabList(new Control[] { this.cltuComposite });
		final FormData ccFormData = new FormData();
		ccFormData.left = new FormAttachment(0, 10);
		ccFormData.right = new FormAttachment(100, -10);
		ccFormData.top = new FormAttachment(0);
		ccFormData.bottom = new FormAttachment(100);
		this.cltuComposite.setLayoutData(ccFormData);
		this.cltuComposite.setLayout(new FormLayout());
		this.rightPanel.setTabList(new Control[] { this.cltuComposite });

		this.cltuComposite.setTabList(new Control[] {});

		// leading acq sequence
		final Label acqSeqLabel = new Label(this.cltuComposite, SWT.LEFT);
		acqSeqLabel.setText("Acquisition Seq (Hex):");
		final FormData aslFormData = new FormData();
		aslFormData.top = new FormAttachment(0, 5);
		aslFormData.left = new FormAttachment(0);
		aslFormData.right = new FormAttachment(40);
		acqSeqLabel.setLayoutData(aslFormData);

		this.acqSeqText = new Text(this.cltuComposite, SWT.LEFT | SWT.BORDER
				| SWT.WRAP | SWT.V_SCROLL);
		this.acqSeqText.setFont(getTextFieldFont());
		this.acqSeqText.setText(BinOctHexUtility.toHexFromBytes(cltu
				.getAcquisitionSequence()));
		final FormData astFormData = SWTUtilities.getFormData(this.acqSeqText, 3, 20);
		astFormData.top = new FormAttachment(acqSeqLabel, 0, SWT.TOP);
		astFormData.left = new FormAttachment(40, 5);
		astFormData.right = new FormAttachment(100);
		this.acqSeqText.setLayoutData(astFormData);

		// start sequence
		final Label startSeqLabel = new Label(this.cltuComposite, SWT.LEFT);
		startSeqLabel.setText("Start Seq (Hex):");
		final FormData sslFormData = new FormData();
		sslFormData.top = new FormAttachment(this.acqSeqText, 5);
		sslFormData.left = new FormAttachment(0);
		sslFormData.right = new FormAttachment(40);
		startSeqLabel.setLayoutData(sslFormData);

		this.startSeqText = new Text(this.cltuComposite, SWT.LEFT | SWT.BORDER);
		this.startSeqText.setFont(getTextFieldFont());
		this.startSeqText.setText(BinOctHexUtility.toHexFromBytes(cltu
				.getStartSequence()));
		final FormData sstFormData = new FormData();
		sstFormData.top = new FormAttachment(startSeqLabel, 0, SWT.TOP);
		sstFormData.left = new FormAttachment(40, 5);
		sstFormData.right = new FormAttachment(100);
		this.startSeqText.setLayoutData(sstFormData);

		// tail sequence
		final Label tailSeqLabel = new Label(this.cltuComposite, SWT.LEFT);
		tailSeqLabel.setText("Tail Seq (Hex):");
		final FormData tslFormData = new FormData();
		tslFormData.top = new FormAttachment(startSeqLabel, 25);
		tslFormData.left = new FormAttachment(0);
		tslFormData.right = new FormAttachment(40);
		tailSeqLabel.setLayoutData(tslFormData);

		this.tailSeqText = new Text(this.cltuComposite, SWT.LEFT | SWT.BORDER);
		this.tailSeqText.setFont(getTextFieldFont());
		this.tailSeqText.setText(BinOctHexUtility.toHexFromBytes(cltu
				.getTailSequence()));
		final FormData tstFormData = new FormData();
		tstFormData.top = new FormAttachment(tailSeqLabel, 0, SWT.TOP);
		tstFormData.left = new FormAttachment(40, 5);
		tstFormData.right = new FormAttachment(100);
		this.tailSeqText.setLayoutData(tstFormData);

		// trailing idle sequence
		final Label idleSeqLabel = new Label(this.cltuComposite, SWT.LEFT);
		idleSeqLabel.setText("Idle Seq (Hex):");
		final FormData islFormData = new FormData();
		islFormData.top = new FormAttachment(tailSeqLabel, 25);
		islFormData.left = new FormAttachment(0);
		islFormData.right = new FormAttachment(40);
		idleSeqLabel.setLayoutData(islFormData);

		this.idleSeqText = new Text(this.cltuComposite, SWT.LEFT | SWT.BORDER
				| SWT.WRAP | SWT.V_SCROLL);
		this.idleSeqText.setFont(getTextFieldFont());
		this.idleSeqText.setText(BinOctHexUtility.toHexFromBytes(cltu
				.getIdleSequence()));
		final FormData istFormData = SWTUtilities
				.getFormData(this.idleSeqText, 3, 20);
		istFormData.top = new FormAttachment(idleSeqLabel, 0, SWT.TOP);
		istFormData.left = new FormAttachment(40, 5);
		istFormData.right = new FormAttachment(100);
		this.idleSeqText.setLayoutData(istFormData);

		displayCodeblocks(cltu);

		setButtonsEnable(true);

		this.rightPanel.layout(true);
	}

	private void displayCodeblocks(final ICltu cltu) {
		final FontData clFontData = new FontData("Helvetica", 12, SWT.BOLD);
		final Font clFont = new Font(getDisplay(), clFontData);
		final Label codeblockLabel = new Label(this.cltuComposite, SWT.LEFT);
		codeblockLabel.setFont(clFont);
		final FormData clFormData = new FormData();
		clFormData.left = new FormAttachment(0, 5);
		clFormData.top = new FormAttachment(this.idleSeqText, 10);
		clFormData.right = new FormAttachment(30);
		codeblockLabel.setLayoutData(clFormData);
		codeblockLabel.setText("Codeblocks");

		final ScrolledComposite codeblockScrollComposite = new ScrolledComposite(
				this.cltuComposite, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		this.cltuComposite.setTabList(new Control[] { this.acqSeqText,
				this.startSeqText, this.tailSeqText, this.idleSeqText,
				codeblockScrollComposite });
		final FormData cscFormData = new FormData();
		cscFormData.left = new FormAttachment(0);
		cscFormData.top = new FormAttachment(codeblockLabel, 10);
		cscFormData.right = new FormAttachment(100);
		cscFormData.bottom = new FormAttachment(100);
		codeblockScrollComposite.setLayoutData(cscFormData);
		codeblockScrollComposite.setLayout(new FillLayout());
		codeblockScrollComposite.setExpandHorizontal(true);
		codeblockScrollComposite.setExpandVertical(true);
		final Composite codeblockComposite = new Composite(codeblockScrollComposite,
				SWT.NONE);
		codeblockScrollComposite
				.setTabList(new Control[] { codeblockComposite });
		codeblockScrollComposite.setContent(codeblockComposite);

		final FormLayout acLayout = new FormLayout();
		acLayout.marginTop = 0;
		acLayout.marginBottom = 0;
		acLayout.marginLeft = 5;
		acLayout.marginRight = 5;
		codeblockComposite.setLayout(acLayout);

		java.util.List<IBchCodeblock> codeblocks = cltu.getCodeblocks();
		if (codeblocks == null) {
			codeblocks = new ArrayList<IBchCodeblock>(1);
			codeblocks.add(appContext.getBean(ICommandObjectFactory.class).createEmptyBchCodeblock());
			cltu.setCodeblocks(codeblocks);
		}
		this.numberLabelFields = new Label[codeblocks.size()];
		this.dataTextFields = new Text[codeblocks.size()];
		this.edacTextFields = new Text[codeblocks.size()];
		this.edacButtonFields = new Button[codeblocks.size()];

		final Label numberHeaderLabel = new Label(codeblockComposite, SWT.CENTER);
		numberHeaderLabel.setText(" # ");
		final FormData nhlFormData = new FormData();
		nhlFormData.left = new FormAttachment(0);
		nhlFormData.top = new FormAttachment(0, 5);
		nhlFormData.right = new FormAttachment(10, -5);
		numberHeaderLabel.setLayoutData(nhlFormData);

		final Label dataHeaderLabel = new Label(codeblockComposite, SWT.CENTER);
		dataHeaderLabel.setText("Data");
		final FormData dhlFormData = new FormData();
		dhlFormData.left = new FormAttachment(10, 5);
		dhlFormData.top = new FormAttachment(numberHeaderLabel, 0, SWT.CENTER);
		dhlFormData.right = new FormAttachment(55, -5);
		dataHeaderLabel.setLayoutData(dhlFormData);

		final Label edacHeaderLabel = new Label(codeblockComposite, SWT.CENTER);
		edacHeaderLabel.setText("EDAC");
		final FormData ehlFormData = new FormData();
		ehlFormData.left = new FormAttachment(55, 5);
		ehlFormData.top = new FormAttachment(numberHeaderLabel, 0, SWT.CENTER);
		ehlFormData.right = new FormAttachment(70, -5);
		edacHeaderLabel.setLayoutData(ehlFormData);

		for (int i = 0; i < codeblocks.size(); i++) {
			final IBchCodeblock codeblock = codeblocks.get(i);

			this.numberLabelFields[i] = new Label(codeblockComposite,
					SWT.CENTER);
			this.numberLabelFields[i].setText(Integer.toString(i + 1));
			this.dataTextFields[i] = new Text(codeblockComposite, SWT.LEFT
					| SWT.BORDER);
			this.dataTextFields[i].setFont(getTextFieldFont());
			this.dataTextFields[i].setText(BinOctHexUtility
					.toHexFromBytes(codeblock.getData()));
			this.edacTextFields[i] = new Text(codeblockComposite, SWT.LEFT
					| SWT.BORDER);
			this.edacTextFields[i].setFont(getTextFieldFont());
			this.edacTextFields[i].setText(BinOctHexUtility
					.toHexFromBytes(codeblock.getEdac()));
			this.edacButtonFields[i] = new Button(codeblockComposite,
					SWT.CENTER | SWT.PUSH);
			this.edacButtonFields[i].setText("Recalculate");
			this.edacButtonFields[i].addSelectionListener(this.handler);

			final FormData nlfFormData = new FormData();
			nlfFormData.left = nhlFormData.left;
			nlfFormData.right = nhlFormData.right;
			final FormData dtfFormData = new FormData();
			dtfFormData.left = dhlFormData.left;
			dtfFormData.right = dhlFormData.right;
			dtfFormData.top = new FormAttachment(this.numberLabelFields[i], 0,
					SWT.CENTER);
			final FormData etfFormData = new FormData();
			etfFormData.left = ehlFormData.left;
			etfFormData.right = ehlFormData.right;
			etfFormData.top = new FormAttachment(this.numberLabelFields[i], 0,
					SWT.CENTER);
			final FormData ebfFormData = new FormData();
			ebfFormData.left = new FormAttachment(70, 5);
			// ebfFormData.right = new FormAttachment(100);
			ebfFormData.top = new FormAttachment(this.numberLabelFields[i], 0,
					SWT.CENTER);

			if (i == 0) {
				nlfFormData.top = new FormAttachment(numberHeaderLabel, 10);
			} else {
				nlfFormData.top = new FormAttachment(
						this.numberLabelFields[i - 1], 20);
			}

			this.numberLabelFields[i].setLayoutData(nlfFormData);
			this.dataTextFields[i].setLayoutData(dtfFormData);
			this.edacTextFields[i].setLayoutData(etfFormData);
			this.edacButtonFields[i].setLayoutData(ebfFormData);
		}

		codeblockScrollComposite.setMinSize(codeblockComposite.computeSize(
				SWT.DEFAULT, SWT.DEFAULT, true));
	}

	private ICltu getSelectedCltu() {
		final int selection = this.cltuList.getSelectionIndex();
		if (selection == -1) {
			return (null);
		}

		final ICltu cltu = this.cltus.get(selection);

		return (cltu);
	}

    /**
     * {@inheritDoc}
     */
	@Override
	public String getDescription() {
		return (FAULT_PAGE_DESC);
	}

    /**
     * {@inheritDoc}
     */
	@Override
	public String getTitle() {
		return (FAULT_PAGE_TITLE);
	}

    /**
     * {@inheritDoc}
     */
	@Override
	public FaultInjectionState getCurrentState() {
		return (this.dataState);
	}

    /**
     * {@inheritDoc}
     */
	@Override
	public void setFromState(final FaultInjectionState state) {
		this.dataState = state;
	}

    /**
     * {@inheritDoc}
     */
	@Override
	public void updateState() throws FaultInjectorException {
		try {
			saveCurrentCltu();
		} catch (final CltuEndecException e) {
			throw new FaultInjectorException(e.getMessage(), e);
		}

		this.dataState.cltus = this.cltus;
		this.dataState.selectedCltuIndex = this.currentCltu;
		this.dataState.rawOutputHex = null;
	}

    /**
     * {@inheritDoc}
     */
	@Override
	public void destroy() {
		this.dispose();
	}

    /**
     * {@inheritDoc}
     */
	@Override
	public void updateDisplay() {
		if (this.dataState.cltus != null) {
			this.cltus = this.dataState.cltus;
			populateCltuList();
			if (this.cltus.size() > 0 && this.dataState.selectedCltuIndex == -1) {
				this.cltuList.setSelection(0);
			}
			this.cltuList.setSelection(this.dataState.selectedCltuIndex);
			cltuList.showSelection();
			displaySelectedCltu();
			return;
		} else if (this.dataState.cltus == null
				&& this.dataState.frames != null) {
			final ICommandLoadBuilder clb = appContext.getBean(ICommandLoadBuilder.class);

			try {
				clb.addCltus(cltuBuilder.createCltusFromFrames(this.dataState.frames));
			} catch (final CltuEndecException e) {
				throw new IllegalStateException( getTitle() + " could not encode the frames to CLTUs " + ExceptionTools.getMessage(e));
			}

			this.cltus = clb.getPlopCltus(appContext.getBean(PlopProperties.class));
			this.dataState.cltus = this.cltus;

			populateCltuList();

			if (this.cltus.size() > 0) {
				this.cltuList.setSelection(0);
				cltuList.showSelection();
				displaySelectedCltu();
			}

			return;
		}

		throw new IllegalStateException(
				getTitle()
						+ " display does not have enough information to construct the editor GUI.");
	}

    /**
     * {@inheritDoc}
     */
	@Override
	public Font getTextFieldFont() {
		return (new Font(getDisplay(), new FontData(FONT_NAME, 14, SWT.NONE)));
	}

	private void destroyCltuComposite() {
		if (this.cltuComposite != null) {
			this.cltuComposite.dispose();
		}

		this.cltuComposite = null;
		this.currentCltu = -1;

		setButtonsEnable(false);
	}

	private void populateCltuList() {
		int maxOrderId = 0;
		for (final ICltu cltu : this.cltus) {
			final Integer orderId = cltu.getOrderId();
			if (orderId != null) {
				final int oi = orderId.intValue();
				if (oi > maxOrderId) {
					maxOrderId = oi;
				}
			}
		}

		final ArrayList<String> newCltuList = new ArrayList<String>(256);
		for (int i = 0; i < this.cltus.size(); i++) {
			final ICltu cltu = this.cltus.get(i);

			if (cltu.getOrderId() == null) {
				cltu.setOrderId(++maxOrderId);
			}

			newCltuList.add("CLTU #" + cltu.getOrderId());
		}

		this.cltuList.setItems(newCltuList.toArray(new String[] {}));
	}

	private void saveCurrentCltu() throws CltuEndecException {
		boolean errorsFound = false;
		final StringBuilder errors = new StringBuilder(1024);

		if (CltuEditorComposite.this.currentCltu != -1) {
			final ICltu cltu = CltuEditorComposite.this.cltus
					.get(CltuEditorComposite.this.currentCltu);

			try {
				cltu.setAcquisitionSequence(BinOctHexUtility.toBytesFromHex(GDR
						.removeWhitespaceFromString(CltuEditorComposite.this.acqSeqText
								.getText())));
			} catch (final Exception e) {
				errors.append("Acquisition Seq (Hex)\n");
				errorsFound = true;
			}

			try {
				cltu.setStartSequence(BinOctHexUtility.toBytesFromHex(GDR
						.removeWhitespaceFromString(CltuEditorComposite.this.startSeqText
								.getText())));
			} catch (final Exception e) {
				errors.append("Start Seq (Hex)\n");
				errorsFound = true;
			}

			try {
				cltu.setTailSequence(BinOctHexUtility.toBytesFromHex(GDR
						.removeWhitespaceFromString(CltuEditorComposite.this.tailSeqText
								.getText())));
			} catch (final Exception e) {
				errors.append("Tail Seq (Hex)\n");
				errorsFound = true;
			}

			try {
				cltu.setIdleSequence(BinOctHexUtility.toBytesFromHex(GDR
						.removeWhitespaceFromString(CltuEditorComposite.this.idleSeqText
								.getText())));
			} catch (final Exception e) {
				errors.append("Idle Seq (Hex)\n");
				errorsFound = true;
			}

			final java.util.List<IBchCodeblock> newCodeblocks = new ArrayList<IBchCodeblock>(
					CltuEditorComposite.this.numberLabelFields.length);
			for (int i = 0; i < CltuEditorComposite.this.numberLabelFields.length; i++) {
				final IBchCodeBlockBuilder cb = appContext.getBean(IBchCodeBlockBuilder.class);

				try {
					byte[] data = BinOctHexUtility.toBytesFromHex(GDR
							.removeWhitespaceFromString(CltuEditorComposite.this.dataTextFields[i].getText()));
					cb.setDataLength(data.length);
					cb.setData(data);
				} catch (final Exception e) {
					errors.append("Codeblock #");
					errors.append(i);
					errors.append(" Data\n");
					errorsFound = true;
				}

				try {
					byte[] edac = BinOctHexUtility.toBytesFromHex(GDR
							.removeWhitespaceFromString(CltuEditorComposite.this.edacTextFields[i].getText()));
					cb.setEdacLength(edac.length);
					cb.setEdac(edac);
				} catch (final Exception e) {
					errors.append("Codeblock #");
					errors.append(i);
					errors.append(" EDAC\n");
					errorsFound = true;
				}

				try {
					newCodeblocks.add(cb.build());
				} catch (IllegalStateException e) {
					errors.append("Codeblock #");
					errors.append(i);
					errors.append(" " + e.getMessage() + "\n");
					errorsFound = true;
				}
			}

			try {
				cltu.setCodeblocks(newCodeblocks);
			} catch (Exception e) {
				throw new CltuEndecException(e);
			}
		}

		if (errorsFound) {
			throw new CltuEndecException(
					"The following CLTU fields contain unusable values: \n\n"
							+ errors.toString());
		}
	}

	/**
	 * Event handler for user actions
	 * 
	 */
	private class EventHandler extends SelectionAdapter {
		/**
		 * Constructor
		 */
		public EventHandler() {
			super();
		}

        /**
         * {@inheritDoc}
         */
		@Override
		public void widgetSelected(final SelectionEvent se) {
			if (se.getSource() == CltuEditorComposite.this.deleteButton) {
				final int position = CltuEditorComposite.this.cltuList
						.getSelectionIndex();
				if (position == -1) {
					return;
				}
				CltuEditorComposite.this.cltus.remove(position);
				populateCltuList();

				final int newPosition = (position - 1) > 0 ? (position - 1) : 0;
				if (CltuEditorComposite.this.cltus.size() > 0) {
					CltuEditorComposite.this.cltuList.setSelection(newPosition);
					cltuList.showSelection();
					displaySelectedCltu();
				} else {
					destroyCltuComposite();
				}
				return;
			}

			try {
				saveCurrentCltu();
			} catch (final CltuEndecException e) {
				se.doit = false;
				SWTUtilities.showErrorDialog(getShell(), "CLTU Format Error",
						e.getMessage());
				return;
			}

			if (se.getSource() == CltuEditorComposite.this.upButton) {
				final int oldPosition = CltuEditorComposite.this.cltuList
						.getSelectionIndex();
				final int newPosition = oldPosition - 1;
				if (oldPosition == -1 || oldPosition == 0) {
					return;
				}
				Collections.swap(CltuEditorComposite.this.cltus, oldPosition,
						newPosition);
				populateCltuList();
				CltuEditorComposite.this.cltuList.setSelection(newPosition);
				cltuList.showSelection();
				displaySelectedCltu();
				return;
			} else if (se.getSource() == CltuEditorComposite.this.downButton) {
				final int oldPosition = CltuEditorComposite.this.cltuList
						.getSelectionIndex();
				final int newPosition = oldPosition + 1;
				if (oldPosition == -1
						|| oldPosition == (CltuEditorComposite.this.cltus
								.size() - 1)) {
					return;
				}
				Collections.swap(CltuEditorComposite.this.cltus, oldPosition,
						newPosition);
				populateCltuList();
				CltuEditorComposite.this.cltuList.setSelection(newPosition);
				cltuList.showSelection();
				displaySelectedCltu();
				return;
			} else if (se.getSource() == CltuEditorComposite.this.newButton) {
				final int position = CltuEditorComposite.this.cltuList
						.getSelectionIndex();

				final ICltu cltu = appContext.getBean(ICommandObjectFactory.class).createCltu();
				CltuEditorComposite.this.cltus.add(position + 1, cltu);
				populateCltuList();
				CltuEditorComposite.this.cltuList.setSelection(position + 1);
				cltuList.showSelection();
				displaySelectedCltu();
				return;
			} else if (se.getSource() == CltuEditorComposite.this.copyButton) {
				final int position = CltuEditorComposite.this.cltuList
						.getSelectionIndex();
				if (position == -1) {
					return;
				}

				final ICltu cltu = getSelectedCltu().copy();
				CltuEditorComposite.this.cltus.add(position + 1, cltu);
				populateCltuList();
				CltuEditorComposite.this.cltuList.setSelection(position + 1);
				cltuList.showSelection();
				displaySelectedCltu();
				return;
			} else if (se.getSource() == CltuEditorComposite.this.cltuList) {
				displaySelectedCltu();
				return;
			}

			for (int i = 0; i < CltuEditorComposite.this.edacButtonFields.length; i++) {
				if (se.getSource() == CltuEditorComposite.this.edacButtonFields[i]) {

					try {

						final ICltu cltu = getSelectedCltu();
						final IBchCodeblock cb = cltu.getCodeblocks().get(i);
						byte[] data = cb.getData();
						byte[] edac;

						if (data == null) {
							throw new IllegalArgumentException("Cannot calculate EDAC because data is not set (data == null).");
						} else {
							edac = BchAlgorithm.doEncode(data);
						}

						CltuEditorComposite.this.edacTextFields[i].setText(BinOctHexUtility.toHexFromBytes(edac));
					} catch (IllegalArgumentException e) {
						String message = e.getMessage();
						int subMsgLoc = message.lastIndexOf(':') + 1;
						if(subMsgLoc > 0) {
							message = message.substring(subMsgLoc);
						}
						SWTUtilities.showErrorDialog(getShell(), "EDAC Error", message);
					}

					return;
				}
			}
		}
	}

	/*
	 * public static void main(final String[] args) throws Exception { Display
	 * display = null; try { display = Display.getDefault(); } catch(SWTError e)
	 * { if (e.getMessage().indexOf("No more handles") != -1) { throw new
	 * RuntimeException(
	 * "Unable to initialize user interface.  If you are using X-Windows, make sure your DISPLAY variable is set."
	 * ); } else { throw(e); } } TestConfiguration tc = new TestConfiguration();
	 * Shell ces = new
	 * Shell(display,SWT.SHELL_TRIM); ces.setSize(700,525); ces.setLayout(new
	 * FillLayout()); CltuEditorComposite cec = new CltuEditorComposite(ces);
	 * CommandInputParser parser = new CommandInputParser();
	 * parser.parseAndStoreCommandString("CMD_NO_OP");
	 * java.util.List<AbstractCommand> cmd = parser.getCommandsToExecute();
	 * ITcTransferFrame frame =
	 * ITcTransferFrameFactory.createCommandFrame((AbstractFlightCommand
	 * )cmd.get(0)); SessionBuilder builder = new SessionBuilder();
	 * builder.addFrame(frame); java.util.List<ITcTransferFrame> frames =
	 * builder.getSessionFrames(); java.util.List<Cltu> cltus =
	 * CltuFactory.createCltusFromFrames(frames); FaultInjectionState fis = new
	 * FaultInjectionState(); fis.frames = frames; fis.cltus = cltus;
	 * cec.setFromState(fis); cec.updateDisplay(); cec.displaySelectedCltu();
	 * cec.layout(true); ces.open(); while(!ces.isDisposed()) {
	 * if(!display.readAndDispatch()) { display.sleep(); } } display.dispose();
	 * }
	 */
}
