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

import java.util.LinkedList;
import java.util.List;

import org.eclipse.jface.viewers.ContentViewer;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import jpl.gds.tc.api.CommandStatusType;
import jpl.gds.tc.api.ICpdUplinkStatus;
import jpl.gds.tcapp.app.gui.CommandStatusColorMapper;
import jpl.gds.tcapp.app.gui.icmd.model.CpdRequestPoolModel;

/**
 * This class provides a detailed view for a CPD request
 * 
 * @since AMPCS R3
 */
public class CpdDetailedRequestView extends ContentViewer {
	/** The window that contains detailed information */
	private final DetailedRequestWindow window;

	/**
	 * The most updated version of the request
	 */
	private ICpdUplinkStatus currentRequest;

	/** Close listeners */
	private List<ICpdDetailedViewCloseListener> closeListeners;

	/**
	 * Constructor
	 * 
	 * @param parentShell the parent shell
	 * @param request the CPD request whose detailed is to be displayed in this
	 *            view
	 */
	public CpdDetailedRequestView(Shell parentShell, ICpdUplinkStatus request) {
		super();
		this.window = new DetailedRequestWindow(parentShell);
		this.currentRequest = request;
		this.closeListeners = new LinkedList<ICpdDetailedViewCloseListener>();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.Viewer#getControl()
	 */
	@Override
	public Control getControl() {
		return this.window.getShell();
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

	/**
	 * Get the CPD request ID of the request being displayed by this view
	 * 
	 * @return the CPD request ID of the request being displayed by this view
	 */
	public String getRequestId() {
		if (this.currentRequest != null) {
			return this.currentRequest.getId();
		} else {
			return null;
		}
	}

	/**
	 * Open the window
	 * 
	 * @return boolean indicating whether or not the window is opened
	 * @see org.eclipse.jface.window.Window#open()
	 */
	public int open() {
		return window.open();
	}

	/**
	 * Close the window
	 * 
	 * @return true if the window is (or was already) closed, and false if it is
	 *         still open
	 * @see org.eclipse.jface.dialogs.Dialog#close()
	 */
	public boolean close() {
		CpdDetailedRequestView.this.notifyCloseListeners();
		return window.close();
	}

	/**
	 * Notify close listeners
	 */
	protected void notifyCloseListeners() {
		for (ICpdDetailedViewCloseListener l : this.closeListeners) {
			l.onDetailedViewClose(getRequestId());
		}
	}

	/**
	 * Add a close listener
	 * 
	 * @param listener a close listener
	 */
	public void addCloseListener(ICpdDetailedViewCloseListener listener) {
		if (!this.closeListeners.contains(listener)) {
			this.closeListeners.add(listener);
		}
	}

	/**
	 * Remove a close listener
	 * 
	 * @param listener close listener to remove
	 */
	public void removeCloseListener(ICpdDetailedViewCloseListener listener) {
		this.closeListeners.remove(listener);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.Viewer#refresh()
	 */
	@Override
	public void refresh() {
		IContentProvider provider = this.getContentProvider();

		if (provider instanceof CpdRequestPoolModel) {
			CpdRequestPoolModel model = (CpdRequestPoolModel) provider;
			this.currentRequest = model.getRequest(this.currentRequest.getId());

			if (this.currentRequest != null) {
				this.window.refresh();
			} else {
				this.close();
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.viewers.Viewer#setSelection(org.eclipse.jface.viewers
	 * .ISelection, boolean)
	 */
	@Override
	public void setSelection(ISelection selection, boolean reveal) {
		// intentionally left blank
	}

	/**
	 * Internal class that handles the layout and creation of the actual window
	 * 
	 * @since AMPCS R3
	 */
	private class DetailedRequestWindow extends Window {
		private final CommandStatusColorMapper colorMapper = new CommandStatusColorMapper();

		/** File Info Group Widgets */
		private Label fileName;
		private Label checksum;
		private Label creationTime;

		private Label totalCltus;

		/** CPD Request Status Group Widgets */
		private Label state;
		private Label updatedAt;
		// Radiation Times
		private Label bit1RadTime;
		private Label lastBitRadTime;
		private Table bitRatesEstRadDurationTable;

		/** Uplink Request Metadata Group Widgets */
		private Label requestId;
		private Label commandDictVer;
		private Label userId;
		private Label roleId;
		private Label submitTime;

		protected DetailedRequestWindow(Shell parentShell) {
			super(parentShell);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets
		 * .Shell)
		 */
		@Override
		protected void configureShell(Shell newShell) {
			super.configureShell(newShell);
			newShell.setText("CPD Uplink Request - Detailed View");
		}

		/**
		 * Populate with most updated data
		 */
		protected void refresh() {
			ICpdUplinkStatus request = CpdDetailedRequestView.this.currentRequest;
			if (request != null) {
				// File Info
				safeSetLabelText(this.fileName, request.getFilename());
				safeSetLabelText(this.checksum, request.getChecksum());
				safeSetLabelText(this.creationTime,
						request.getScmfCreationTime());
				safeSetLabelText(
						this.totalCltus,
						request.getTotalCltus() < 0 ? "N/A" : Integer
								.toString(request.getTotalCltus()));

				// CPD Request Status
				setRequestState(request.getStatus());
				safeSetLabelText(this.updatedAt, request.getTimestampString());

				String bit1RadTimeStr = request.getBit1RadTimeString();
				safeSetLabelText(this.bit1RadTime,
						bit1RadTimeStr == null ? "TBD" : bit1RadTimeStr);

				String lastBitRadTimeStr = request.getLastBitRadTimeString();
				safeSetLabelText(this.lastBitRadTime,
						lastBitRadTimeStr == null ? "TBD" : lastBitRadTimeStr);

				List<Float> bitRates = request.getBitrates();

				// clear old values
				this.bitRatesEstRadDurationTable.clearAll();
				this.bitRatesEstRadDurationTable.setItemCount(0);

				for (Float br : bitRates) {
					TableItem newRow = new TableItem(
							this.bitRatesEstRadDurationTable, SWT.NONE);

					int i = 0;

					newRow.setText(i++, br.toString());
					newRow.setText(i++, request.getRadiationDurationString(br));
				}

				// Uplink Request Metadata
				safeSetLabelText(this.requestId, request.getId());
				safeSetLabelText(this.userId, request.getUserId());
				safeSetLabelText(this.roleId, request.getRoleId());
				safeSetLabelText(this.commandDictVer,
						request.getCommandDictVer());
				safeSetLabelText(this.submitTime, request.getSubmitTime());
			}
		}

		private void safeSetLabelText(Label widget, String text) {
			if (widget != null && !widget.isDisposed()) {
				text = text == null ? "UNTKNOWN" : text;
				widget.setText(text);
				widget.pack(true);
			}
		}

		private void setRequestState(CommandStatusType state) {
			if (state != null) {
				safeSetLabelText(this.state, state.toString());

				this.state.setBackground(colorMapper.getColorForStatus(state));
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jface.window.Window#handleShellCloseEvent()
		 */
		@Override
		protected void handleShellCloseEvent() {
			CpdDetailedRequestView.this.notifyCloseListeners();
			super.handleShellCloseEvent();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.jface.window.Window#createContents(org.eclipse.swt.widgets
		 * .Composite)
		 */
		@Override
		protected Control createContents(Composite parent) {
			Composite contents = new Composite(parent, SWT.NONE);
			GridLayout layout = new GridLayout();
			contents.setLayout(layout);

			GridData gd = new GridData(GridData.FILL_BOTH);
			contents.setLayoutData(gd);

			this.createFileInfoGroup(contents);
			this.createCpdRequestStatusGroup(contents);
			this.createUplinkRequestMetadataGroup(contents);

			this.refresh();

			return contents;
		}

		private Label getBoldBlueLabel(Composite parent, int style) {
			Label label = new Label(parent, style);
			label.setForeground(label.getShell().getDisplay()
					.getSystemColor(SWT.COLOR_BLUE));
			FontData fontData = label.getFont().getFontData()[0];
			Font font = new Font(CpdDetailedRequestView.this.getControl()
					.getDisplay(), new FontData(fontData.getName(),
					fontData.getHeight(), SWT.BOLD));

			label.setFont(font);

			return label;
		}

		private Label getItalicLabel(Composite parent, int style) {
			Label label = new Label(parent, style);
			FontData fontData = label.getFont().getFontData()[0];
			Font font = new Font(CpdDetailedRequestView.this.getControl()
					.getDisplay(), new FontData(fontData.getName(),
					fontData.getHeight(), SWT.ITALIC));

			label.setFont(font);

			return label;
		}

		private Group getGroupWithFormLayout(Composite parent, int style) {
			Group group = new Group(parent, style);

			FormLayout fl = new FormLayout();
			fl.marginWidth = 5;
			fl.marginHeight = 5;
			group.setLayout(fl);

			return group;
		}

		private void createFileInfoGroup(Composite parent) {
			// widget creation
			Group fileInfoGroup = this.getGroupWithFormLayout(parent, SWT.NONE);
			fileInfoGroup.setText("File Info");

			GridData gd = new GridData(SWT.FILL, SWT.TOP, true, false);
			fileInfoGroup.setLayoutData(gd);

			Label fileNameLabel = new Label(fileInfoGroup, SWT.NONE);
			fileNameLabel.setText("File Name:");

			this.fileName = this.getBoldBlueLabel(fileInfoGroup, SWT.NONE);

			Label checksumLabel = new Label(fileInfoGroup, SWT.NONE);
			checksumLabel.setText("Checksum:");

			this.checksum = this.getBoldBlueLabel(fileInfoGroup, SWT.NONE);

			Label creationTimeLabel = new Label(fileInfoGroup, SWT.NONE);
			creationTimeLabel.setText("Creation Time:");

			this.creationTime = this.getBoldBlueLabel(fileInfoGroup, SWT.NONE);

			Label totalCltuLabel = new Label(fileInfoGroup, SWT.NONE);
			totalCltuLabel.setText("Total CLTUs:");

			this.totalCltus = this.getBoldBlueLabel(fileInfoGroup, SWT.NONE);

			// widget layout
			FormData fd = new FormData();
			fd.top = new FormAttachment(0);
			fd.left = new FormAttachment(0);
			fileNameLabel.setLayoutData(fd);

			fd = new FormData();
			fd.top = new FormAttachment(fileNameLabel, 0, SWT.CENTER);
			fd.left = new FormAttachment(fileNameLabel, 0);
			this.fileName.setLayoutData(fd);

			fd = new FormData();
			fd.top = new FormAttachment(fileNameLabel, 5);
			fd.left = new FormAttachment(0);
			checksumLabel.setLayoutData(fd);

			fd = new FormData();
			fd.top = new FormAttachment(checksumLabel, 0, SWT.CENTER);
			fd.left = new FormAttachment(checksumLabel, 0);
			this.checksum.setLayoutData(fd);

			fd = new FormData();
			fd.top = new FormAttachment(checksumLabel, 0, SWT.CENTER);
			fd.left = new FormAttachment(this.checksum, 5);
			creationTimeLabel.setLayoutData(fd);

			fd = new FormData();
			fd.top = new FormAttachment(checksumLabel, 0, SWT.CENTER);
			fd.left = new FormAttachment(creationTimeLabel, 0);
			this.creationTime.setLayoutData(fd);

			fd = new FormData();
			fd.top = new FormAttachment(checksumLabel, 0, SWT.CENTER);
			fd.left = new FormAttachment(this.creationTime, 5);
			totalCltuLabel.setLayoutData(fd);

			fd = new FormData();
			fd.top = new FormAttachment(checksumLabel, 0, SWT.CENTER);
			fd.left = new FormAttachment(totalCltuLabel, 0);
			this.totalCltus.setLayoutData(fd);
		}

		private void createCpdRequestStatusGroup(Composite parent) {
			// widget creation
			Group cpdRequestStatusGroup = this.getGroupWithFormLayout(parent,
					SWT.NONE);
			cpdRequestStatusGroup.setText("CPD Request Status");

			GridData gd = new GridData(SWT.FILL, SWT.TOP, true, false);
			cpdRequestStatusGroup.setLayoutData(gd);

			Label stateLabel = new Label(cpdRequestStatusGroup, SWT.NONE);
			stateLabel.setText("State:");

			this.state = new Label(cpdRequestStatusGroup, SWT.NONE);

			Label updatedAtLabel = new Label(cpdRequestStatusGroup, SWT.NONE);
			updatedAtLabel.setText("Updated at:");

			this.updatedAt = this.getBoldBlueLabel(cpdRequestStatusGroup,
					SWT.NONE);

			Group radiationTimesGroup = this.getGroupWithFormLayout(
					cpdRequestStatusGroup, SWT.NONE);
			radiationTimesGroup.setText("Radiation Times");

			Label bit1RadTimeLabel = new Label(radiationTimesGroup, SWT.NONE);
			bit1RadTimeLabel.setText("Bit 1 Radiation Time:");

			this.bit1RadTime = this.getBoldBlueLabel(radiationTimesGroup,
					SWT.NONE);

			Label lastBitRadTimeLabel = new Label(radiationTimesGroup, SWT.NONE);
			lastBitRadTimeLabel.setText("Last Bit Radiation Time:");

			this.lastBitRadTime = this.getBoldBlueLabel(radiationTimesGroup,
					SWT.NONE);

			Label bitRateEstRadDurationLabel = this.getBoldBlueLabel(
					cpdRequestStatusGroup, SWT.NONE);
			bitRateEstRadDurationLabel
					.setForeground(CpdDetailedRequestView.this.getControl()
							.getDisplay().getSystemColor(SWT.COLOR_BLACK));
			bitRateEstRadDurationLabel
					.setText("Estimated Radiation Durations, by Bit Rate");

			this.bitRatesEstRadDurationTable = new Table(cpdRequestStatusGroup,
					SWT.BORDER);

			this.bitRatesEstRadDurationTable.setHeaderVisible(true);
			this.bitRatesEstRadDurationTable.setLinesVisible(true);

			TableColumn bitRateCol = new TableColumn(
					this.bitRatesEstRadDurationTable, SWT.LEFT);
			bitRateCol.setText("Bit Rate (bps)");
			bitRateCol.setWidth(100);

			TableColumn estRadDurationCol = new TableColumn(
					this.bitRatesEstRadDurationTable, SWT.LEFT);
			estRadDurationCol.setText("Duration (Hours:Minutes:Seconds)");
			estRadDurationCol.setWidth(185);

			// widget layout
			FormData fd = new FormData();
			fd.top = new FormAttachment(0);
			fd.left = new FormAttachment(0);
			stateLabel.setLayoutData(fd);

			fd = new FormData();
			fd.top = new FormAttachment(stateLabel, 0, SWT.CENTER);
			fd.left = new FormAttachment(stateLabel, 0);
			this.state.setLayoutData(fd);

			fd = new FormData();
			fd.top = new FormAttachment(stateLabel, 0, SWT.CENTER);
			fd.right = new FormAttachment(this.updatedAt, 0);
			updatedAtLabel.setLayoutData(fd);

			fd = new FormData();
			fd.top = new FormAttachment(stateLabel, 0, SWT.CENTER);
			fd.right = new FormAttachment(100);
			this.updatedAt.setLayoutData(fd);

			fd = new FormData();
			fd.top = new FormAttachment(stateLabel, 5);
			fd.left = new FormAttachment(0);
			fd.right = new FormAttachment(100);
			radiationTimesGroup.setLayoutData(fd);

			fd = new FormData();
			fd.top = new FormAttachment(0);
			fd.left = new FormAttachment(0);
			bit1RadTimeLabel.setLayoutData(fd);

			fd = new FormData();
			fd.top = new FormAttachment(bit1RadTimeLabel, 0, SWT.CENTER);
			fd.left = new FormAttachment(bit1RadTimeLabel, 0);
			this.bit1RadTime.setLayoutData(fd);

			fd = new FormData();
			fd.top = new FormAttachment(bit1RadTimeLabel, 5);
			fd.left = new FormAttachment(0);
			lastBitRadTimeLabel.setLayoutData(fd);

			fd = new FormData();
			fd.top = new FormAttachment(lastBitRadTimeLabel, 0, SWT.CENTER);
			fd.left = new FormAttachment(lastBitRadTimeLabel, 0);
			this.lastBitRadTime.setLayoutData(fd);

			fd = new FormData();
			fd.top = new FormAttachment(radiationTimesGroup, 0);
			fd.left = new FormAttachment(radiationTimesGroup, 0, SWT.CENTER);
			bitRateEstRadDurationLabel.setLayoutData(fd);

			fd = new FormData();
			fd.top = new FormAttachment(bitRateEstRadDurationLabel, 5);
			fd.left = new FormAttachment(radiationTimesGroup, 0, SWT.CENTER);
			this.bitRatesEstRadDurationTable.setLayoutData(fd);
		}

		private void createUplinkRequestMetadataGroup(Composite parent) {
			// widget creation
			Group uplinkRequestMetadataGroup = this.getGroupWithFormLayout(
					parent, SWT.NONE);
			uplinkRequestMetadataGroup.setText("Uplink Request Metadata");

			GridData gd = new GridData(SWT.FILL, SWT.TOP, true, false);
			uplinkRequestMetadataGroup.setLayoutData(gd);

			Label requestIdLabel = new Label(uplinkRequestMetadataGroup,
					SWT.NONE);
			requestIdLabel.setText("Request ID:");

			this.requestId = this.getItalicLabel(uplinkRequestMetadataGroup,
					SWT.NONE);

			Label userIdLabel = new Label(uplinkRequestMetadataGroup, SWT.NONE);
			userIdLabel.setText("User ID:");

			this.userId = this.getItalicLabel(uplinkRequestMetadataGroup,
					SWT.NONE);

			Label roleIdLabel = new Label(uplinkRequestMetadataGroup, SWT.NONE);
			roleIdLabel.setText("Role ID:");

			this.roleId = this.getItalicLabel(uplinkRequestMetadataGroup,
					SWT.NONE);

			Label commandDictVerLabel = new Label(uplinkRequestMetadataGroup,
					SWT.NONE);
			commandDictVerLabel.setText("Command Dictionary Version:");

			this.commandDictVer = this.getItalicLabel(
					uplinkRequestMetadataGroup, SWT.NONE);

			Label submitTimeLabel = new Label(uplinkRequestMetadataGroup,
					SWT.NONE);
			submitTimeLabel.setText("Submit Time:");

			this.submitTime = this.getItalicLabel(uplinkRequestMetadataGroup,
					SWT.NONE);

			// widget layout
			FormData fd = new FormData();
			fd.top = new FormAttachment(0);
			fd.left = new FormAttachment(0);
			requestIdLabel.setLayoutData(fd);

			fd = new FormData();
			fd.top = new FormAttachment(requestIdLabel, 0, SWT.CENTER);
			fd.left = new FormAttachment(requestIdLabel, 0);
			this.requestId.setLayoutData(fd);

			fd = new FormData();
			fd.top = new FormAttachment(requestIdLabel, 0);
			fd.left = new FormAttachment(0);
			userIdLabel.setLayoutData(fd);

			fd = new FormData();
			fd.top = new FormAttachment(userIdLabel, 0, SWT.CENTER);
			fd.left = new FormAttachment(userIdLabel);
			this.userId.setLayoutData(fd);

			fd = new FormData();
			fd.top = new FormAttachment(userIdLabel, 0, SWT.CENTER);
			fd.left = new FormAttachment(this.userId, 5);
			roleIdLabel.setLayoutData(fd);

			fd = new FormData();
			fd.top = new FormAttachment(userIdLabel, 0, SWT.CENTER);
			fd.left = new FormAttachment(roleIdLabel, 0);
			this.roleId.setLayoutData(fd);

			fd = new FormData();
			fd.top = new FormAttachment(userIdLabel, 0);
			fd.left = new FormAttachment(0);
			commandDictVerLabel.setLayoutData(fd);

			fd = new FormData();
			fd.top = new FormAttachment(commandDictVerLabel, 0, SWT.CENTER);
			fd.left = new FormAttachment(commandDictVerLabel, 0);
			this.commandDictVer.setLayoutData(fd);

			fd = new FormData();
			fd.top = new FormAttachment(commandDictVerLabel, 0);
			fd.left = new FormAttachment(0);
			submitTimeLabel.setLayoutData(fd);

			fd = new FormData();
			fd.top = new FormAttachment(submitTimeLabel, 0, SWT.CENTER);
			fd.left = new FormAttachment(submitTimeLabel, 0);
			this.submitTime.setLayoutData(fd);
		}
	}
}
