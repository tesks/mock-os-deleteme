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
package jpl.gds.tcapp.app.gui.icmd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.connection.IConnectionMap;
import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.common.config.types.UplinkConnectionType;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.metadata.InvalidMetadataException;
import jpl.gds.tc.api.config.CommandProperties;
import jpl.gds.tcapp.app.gui.icmd.EditTable.EditDialogElement;

/**
 * This class is a widget that allows users to select uplink rates for each
 * uplink
 * 
 * @since AMPCS R3
 */
public class CpdUplinkRateView extends EditableView {
	/** Logger */
	private final Tracer logger;

	/** Label showing the selected bit rates */
	private Label bitRates;

	/** The available uplink rates */
	private String[] uplinkRates;

	/** A map showing which available uplink rate is selected */
	private Map<Double, Boolean> uplinkRateSelections;


	/**
	 * Constructor
	 * 
	 * @param appContext the ApplicationContext in which this object is being used
	 * @param parent the parent composite
	 * @param style the SWT style
	 * @throws InvalidMetadataException 
	 * @throws BeansException 
	 */
	public CpdUplinkRateView(final ApplicationContext appContext, final Composite parent, final int style) throws BeansException, InvalidMetadataException {
		super(appContext, parent, style, 1);
        this.logger = TraceManager.getTracer(appContext, Loggers.CPD_UPLINK);

        final UplinkConnectionType upConnectType = appContext.getBean(IConnectionMap.class).getFswUplinkConnection().
                getUplinkConnectionType();
		
        if (uplinkRates == null || upConnectType != UplinkConnectionType.COMMAND_SERVICE) {
			this.editButton.setEnabled(false);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jpl.gds.tcapp.icmd.gui.EditableView#getEditDialog()
	 */
	@Override
	protected Dialog getEditDialog() {
		return new EditDialog(parentShell);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jpl.gds.tcapp.icmd.gui.EditableView#createControls()
	 */
	@Override
	protected void createControls() {
		uplinkRates = appContext.getBean(MissionProperties.class).getAllowedUplinkBitrates().toArray(new String[] {});

		final Label bitRateLabel = new Label(this.controlsGroup, SWT.NONE);
		bitRateLabel.setText("Bit Rates (bps):");

		bitRates = new Label(this.controlsGroup, SWT.NONE);
		final FontData titleFontData = this.bitRates.getFont().getFontData()[0];
		  
		final Font titleFont = new Font(parentShell.getDisplay(),
				new FontData(titleFontData.getName(),
						titleFontData.getHeight(), SWT.BOLD));
		bitRates.setFont(titleFont);

        if (appContext.getBean(IConnectionMap.class).getFswUplinkConnection().getUplinkConnectionType()
                      .equals(UplinkConnectionType.COMMAND_SERVICE)) {
			bitRates.setForeground(parentShell.getDisplay()
					.getSystemColor(SWT.COLOR_BLUE));
			bitRates.setText("[ANY]");

			uplinkRateSelections = new HashMap<Double, Boolean>();
			setSelectedBitRates( appContext.getBean(CommandProperties.class)
					.getSelectedBitRates());
		} else {
			bitRates.setForeground(parentShell.getDisplay()
					.getSystemColor(SWT.COLOR_DARK_GRAY));
			bitRates.setText("N/A");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.Viewer#getSelection()
	 */
	@Override
	public ISelection getSelection() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.Viewer#refresh()
	 */
	@Override
	public void refresh() {
		// intentionally left blank
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.viewers.Viewer#setSelection(org.eclipse.jface.viewers
	 * .ISelection, boolean)
	 */
	@Override
	public void setSelection(final ISelection selection, final boolean reveal) {
		// intentionally left blank
	}

	private void setSelectedBitRates(final List<Double> selectedBitRates) {
		if (selectedBitRates == null) {
			return;
		}

		this.uplinkRateSelections.clear();

		final StringBuilder sb = new StringBuilder();

		int numSelected = 0;

		sb.append("[");

		for (final Double br : selectedBitRates) {
			try {
				uplinkRateSelections.put(br, true);
			} catch (final Exception e) {
			}

			sb.append(br);
			sb.append(",");
			numSelected++;
		}

		if (numSelected > 0) {
			sb.deleteCharAt(sb.length() - 1);
		} else {
			sb.append("ANY");
		}

		sb.append("]");

		final String bitRates = sb.toString();
		CpdUplinkRateView.this.bitRates.setText(bitRates);
		CpdUplinkRateView.this.controlsGroup.layout();
	}

	/**
	 * This class displays the dialog that allows users to select uplink rates
	 * 
	 */
	protected class EditDialog extends Dialog {
		/** List of bit rates */
		private final List<EditDialogElement> bitRateList;

		/**
		 * The aggregate selection box. Selecting this box will select/deselect
		 * all
		 */
		private EditDialogElement aggregateElement;

		/** Selection adapter to act on selection event */
		private final SelectionAdapter bitRateSelectionAdapter;

		/**
		 * Constructor
		 * 
		 * @param parentShell the parent shell
		 */
		protected EditDialog(final Shell parentShell) {
			super(parentShell);

			bitRateList = new ArrayList<EditDialogElement>();
			bitRateSelectionAdapter = new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					boolean state = bitRateList.get(0).isSelected();

					for (final EditDialogElement elem : bitRateList) {
						state &= elem.isSelected();
					}

					// if all checkboxes are checked, check the
					// aggregate
					aggregateElement.getCheckboxControl().setSelection(state);
				}
			};
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt
		 * .widgets.Composite)
		 */
		@Override
		protected Control createDialogArea(final Composite parent) {
			final EditTable editTable = new EditTable(parent, SWT.NONE, 2, true) {
				@Override
				protected void createHeader() {
					final Label selectBitratesLabel = new Label(this, SWT.CENTER);
					selectBitratesLabel.setText("Select Bit Rate(s)\n");
					final GridData gd = new GridData(SWT.CENTER, SWT.CENTER, true,
							true);
					gd.horizontalSpan = 2;
					selectBitratesLabel.setLayoutData(gd);
				}
			};
			editTable.setLayoutData(new GridData(GridData.FILL_BOTH));

			aggregateElement = editTable.new EditDialogElement(editTable) {

				@Override
				public String getValue() {
					return "";
				}

				@Override
				public void createValueElement(final EditTable parent) {
				}
			};

			aggregateElement.setLabel("Bit Rate (bps)");
			aggregateElement.setBackgroundColor(parentShell
					.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
			aggregateElement.setLabelFontStyle(SWT.BOLD);
			aggregateElement.getCheckboxControl().addSelectionListener(
					new SelectionAdapter() {
						@Override
						public void widgetSelected(final SelectionEvent e) {
							for (final EditDialogElement elem : bitRateList) {
								elem.getCheckboxControl().setSelection(
										aggregateElement.isSelected());
							}
						}
					});

			for (final String rate : uplinkRates) {
				try {
					/**
					 * This needs to be able to handle floats, so make sure it can
					 * be parsed as a float, not just an integer.
					 */
					Double.parseDouble(rate);

					addBitRate(editTable, rate);
				} catch (final NumberFormatException e) {
				}
			}

			return editTable;
		}

		private void addBitRate(final EditTable editTable, final String bitRate) {
			final EditDialogElement item = editTable.new EditDialogElement(editTable) {

				@Override
				public String getValue() {
					return label.getLabelWidget().getText();
				}

				@Override
				public void createValueElement(final EditTable parent) {
				}
			};

			item.setLabel(bitRate);
			item.getCheckboxControl().addSelectionListener(
					bitRateSelectionAdapter);
			bitRateList.add(item);

			try {
				boolean selected = false;
				/* Also here so we show the values that were previously selected. */
				final Double br = Double.parseDouble(bitRate);

				if (uplinkRateSelections.containsKey(br)) {
					selected = uplinkRateSelections.get(br);
				}

				item.getCheckboxControl().setSelection(selected);
			} catch (final Exception e) {
			}

			bitRateSelectionAdapter.widgetSelected(null);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
		 */
		@Override
		protected void okPressed() {
			final List<Double> selectedBitRates = new ArrayList<Double>();

			for (final EditDialogElement item : bitRateList) {
				if (item.isSelected()) {
					try {
						selectedBitRates.add(Double.parseDouble(item.getValue()));
					} catch (final NumberFormatException e) {
						logger.warn("Invalid bit rate selected: "
								+ item.getValue());
					}
				}
			}

			appContext.getBean(CommandProperties.class).setSelectedBitRates(selectedBitRates);
			CpdUplinkRateView.this.setSelectedBitRates(selectedBitRates);
			super.okPressed();
		}
	}
}
