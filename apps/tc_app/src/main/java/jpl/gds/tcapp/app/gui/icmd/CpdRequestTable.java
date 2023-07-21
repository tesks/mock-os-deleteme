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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ContentViewer;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.springframework.context.ApplicationContext;

import gov.nasa.jpl.icmd.schema.AggregationMethod;
import gov.nasa.jpl.icmd.schema.ExecutionMethod;
import gov.nasa.jpl.icmd.schema.ExecutionState;
import gov.nasa.jpl.icmd.schema.ExecutionStateRequest;
import gov.nasa.jpl.icmd.schema.ListPreparationStateEnum;
import jpl.gds.common.config.security.SecurityProperties;
import jpl.gds.common.config.types.CommandUserRole;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.tc.api.CommandStatusType;
import jpl.gds.tc.api.ICpdUplinkStatus;
import jpl.gds.tc.api.config.CommandProperties;
import jpl.gds.tcapp.app.gui.CommandStatusColorMapper;
import jpl.gds.tcapp.app.gui.icmd.model.CpdParametersModel;
import jpl.gds.tcapp.app.gui.icmd.model.CpdRadiationListModel;
import jpl.gds.tcapp.app.gui.icmd.model.CpdRequestPoolModel;

/**
 * This class defines a view that contains the CPD request pool and radiation
 * list along with widgets that abstract certain CPD parameters
 * 
 * @since AMPCS R3
 */
public class CpdRequestTable extends ContentViewer {
	/** Logger */
	private final Tracer logger; 


	/** The path to the image for the opened icon */
	private final String openedIconPath = "jpl/gds/tcapp/icmd/gui/opened.png";

	/** The path to the image for the closed icon */
	private final String closedIconPath = "jpl/gds/tcapp/icmd/gui/closed.png";

	/** The path to the image for the question icon */
	private final String questionIconPath = "jpl/gds/tcapp/icmd/gui/unknown_state.png";

	/** The top level composite */
	private Composite masterComposite;

	/** The header composite */
	private Composite headerComposite;

	/** The body composite */
	private Composite bodyComposite;

	/** The comparator class that is used to sort the request pool */
	private CpdRequestTableViewComparator comparator;

	/** The request pool view. This is bound to the request pool data model */
	private TableViewer requestPool;

	/** The request pool data model */
	private CpdRequestPoolModel reqPoolModel;

	/** The radiation list view. This is bound to the radiation list data model */
	private TableViewer radiationList;

	/** The radiation list data model */
	private CpdRadiationListModel radListModel;

	/** Listeners that are interested in changes to request pool/radiation list */
	private List<ICpdRequestChangeListener> listeners;

	/** Listeners that are interested in changes to CPD parameters */
	private List<ICpdParametersChangeListener> paramListeners;

	/** The button bar composite */
	private Composite buttonBar;

	/** The button to lock/unlock the request pool */
	private ToggleImageButton requestPoolEnableButton;

	/** The button to lock/unlock the radiation list */
	private ToggleImageButton radiationListEnableButton;

	/** The button to enable/disable execution state */
	private Button execStateButton;

	/** The current Execution State */
	private ExecutionState currExecState;

	/** The context menu shown when right clicking on a request */
	private Menu contextMenu;

	/**
	 * The widget that displays Execution Method and allows changing the
	 * Execution Method
	 */
	private EditableItem execMethod;

	/**
	 * The widget that displays Aggregation Method and allows changing the
	 * Aggregation Method
	 */
	private EditableItem aggregationMethod;

	/**
	 * The selection manager that handles selections on the request
	 * pool/radiation list
	 */
	private ISelectionChangedListener tableSelectionManager;

	/** The label widget that displays the current preparation state */
	private Label preparationState;

	/** The label widget that displays the current execution state */
	private Label executionState;

	/** The label widget that displays the configured radiation list order */
	private Label radListOrder;

	/**
	 * The lock on needed to synchronize on in order to modify the current
	 * execution state
	 */
	private final Object execStateLock = new Object();

	/**
	 * Map of opened detailed view windows, by request ID
	 */
	private final Map<String, CpdDetailedRequestView> openedDetailedViews;

	/** Close listener for detailed view windows */
	private final ICpdDetailedViewCloseListener detailedViewListener;
	
	private Shell parentShell;
	
	private CpdParametersModel paramModel;
	
	private ApplicationContext appContext;

	/**
	 * Constructor
	 * 
	 * @param appContext the ApplicationContext in which this object is being used
	 * @param model The CPD configurations model that is being used
	 * @param parent the parent composite
	 * @param style the SWT style
	 */
	public CpdRequestTable(final ApplicationContext appContext, final CpdParametersModel model, final Composite parent, final int style) {
		this.appContext = appContext;
        this.logger = TraceManager.getTracer(appContext, Loggers.CPD_UPLINK);
	    this.paramModel = model;
	    this.parentShell = parent.getShell();
		this.currExecState = ExecutionState.DISABLED;
		this.listeners = new LinkedList<ICpdRequestChangeListener>();
		this.paramListeners = new LinkedList<ICpdParametersChangeListener>();
		this.masterComposite = new Composite(parent, style);
		this.reqPoolModel = new CpdRequestPoolModel(appContext);
		this.tableSelectionManager = new RequestTableSelectionManager();
		this.radListModel = new CpdRadiationListModel(appContext);
		this.openedDetailedViews = new HashMap<String, CpdDetailedRequestView>();
		this.detailedViewListener = new ICpdDetailedViewCloseListener() {
			@Override
			public void onDetailedViewClose(final String requestId) {
				if (requestId != null) {
					CpdRequestTable.this.openedDetailedViews.remove(requestId);
				}
			}
		};

		final GridLayout l = new GridLayout();
		l.numColumns = 1;
		this.masterComposite.setLayout(l);

		createContextMenu();

		createHeaderBar();

		this.bodyComposite = new Composite(this.masterComposite, SWT.NONE);
		final FillLayout layout = new FillLayout();
		layout.type = SWT.VERTICAL;
		this.bodyComposite.setLayout(layout);
		final GridData gd = new GridData(GridData.FILL_BOTH);
		this.bodyComposite.setLayoutData(gd);

		createRequestPool();
		createRadiationList();
		createButtonBar();

		this.radListModel.inputChanged(new ContentViewer() {

			@Override
			public Control getControl() {
				return parentShell;
			}

			@Override
			public ISelection getSelection() {
				return null;
			}

			@Override
			public void refresh() {
				final Table radList = CpdRequestTable.this.radiationList.getTable();
				if (CpdRequestTable.this.radListModel.isStale()) {
					radList.setForeground(radList.getDisplay().getSystemColor(
							SWT.COLOR_GRAY));
				} else {
					radList.setForeground(null);
				}
			}

			@Override
			public void setSelection(final ISelection selection, final boolean reveal) {
				// this method is intentionally left blank
			}

		}, null, null);
	}

	private void createContextMenu() {
		this.contextMenu = new Menu(this.getControl().getParent().getShell(),
				SWT.POP_UP);

		final MenuItem contextMenuItem = new MenuItem(this.contextMenu, SWT.PUSH);
		contextMenuItem.setText("Request Details...");
		contextMenuItem.addSelectionListener(new SelectionAdapter() {

			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse
			 * .swt.events.SelectionEvent)
			 */
			@Override
			public void widgetSelected(final SelectionEvent arg0) {
				final ICpdUplinkStatus request = CpdRequestTable.this
						.getSelectedRequest();
				if (request != null) {
					if (CpdRequestTable.this.openedDetailedViews
							.containsKey(request.getId())) {
						final CpdDetailedRequestView openedView = CpdRequestTable.this.openedDetailedViews
								.get(request.getId());
						openedView.getControl().forceFocus();
					} else {
						final CpdDetailedRequestView detailedView = new CpdDetailedRequestView(
								CpdRequestTable.this.getControl().getShell(),
								request);
						detailedView
								.addCloseListener(CpdRequestTable.this.detailedViewListener);

						detailedView.setContentProvider(reqPoolModel);

						// must open first so the shell is there to be the
						// widget
						// that is "the viewer"
						// otherwise we cannot subscribe to the model
						detailedView.open();

						CpdRequestTable.this.openedDetailedViews.put(
								request.getId(), detailedView);

						detailedView.setInput("");
					}
				}
			}
		});
	}

	private void createHeaderBar() {
		headerComposite = new Composite(this.masterComposite, SWT.BORDER);

		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		headerComposite.setLayoutData(gd);

		final GridLayout gl = new GridLayout();
		headerComposite.setLayout(gl);

		final Composite innerComp = new Composite(headerComposite, SWT.NONE);
		gd = new GridData(SWT.CENTER, SWT.CENTER, true, true);
		innerComp.setLayoutData(gd);

		final FormLayout layout = new FormLayout();
		innerComp.setLayout(layout);

		// preparation state
		final Label prepStateLabel = new Label(innerComp, SWT.NONE);
		prepStateLabel.setText("Preparation State:");

		FormData fd = new FormData();
		fd.left = new FormAttachment(0);
		fd.top = new FormAttachment(0);

		prepStateLabel.setLayoutData(fd);

		preparationState = new Label(innerComp, SWT.NONE);
		FontData fontData = preparationState.getFont().getFontData()[0];
		preparationState.setFont(new Font(parentShell
				.getDisplay(), new FontData(fontData.getName(), fontData
				.getHeight(), SWT.BOLD)));
		preparationState.setForeground(parentShell
				.getDisplay().getSystemColor(SWT.COLOR_BLUE));

		fd = new FormData();
		fd.left = new FormAttachment(prepStateLabel, 0);
		fd.top = new FormAttachment(prepStateLabel, 0, SWT.CENTER);

		preparationState.setLayoutData(fd);

		// execution state
		final Label execStateLabel = new Label(innerComp, SWT.NONE);
		execStateLabel.setText("Execution State:");

		fd = new FormData();
		fd.left = new FormAttachment(preparationState, 5);
		fd.top = new FormAttachment(preparationState, 0, SWT.CENTER);

		execStateLabel.setLayoutData(fd);

		executionState = new Label(innerComp, SWT.NONE);
		fontData = executionState.getFont().getFontData()[0];
		executionState.setFont(new Font(parentShell
				.getDisplay(), new FontData(fontData.getName(), fontData
				.getHeight(), SWT.BOLD)));
		executionState.setForeground(parentShell
				.getDisplay().getSystemColor(SWT.COLOR_BLUE));

		fd = new FormData();
		fd.left = new FormAttachment(execStateLabel, 0);
		fd.top = new FormAttachment(execStateLabel, 0, SWT.CENTER);

		executionState.setLayoutData(fd);

		// Radiation list order
		final Label radListOrderLabel = new Label(innerComp, SWT.NONE);
		radListOrderLabel.setText("Radiation List Order:");

		fd = new FormData();
		fd.left = new FormAttachment(executionState, 5);
		fd.top = new FormAttachment(executionState, 0, SWT.CENTER);

		radListOrderLabel.setLayoutData(fd);

		radListOrder = new Label(innerComp, SWT.NONE);
		fontData = radListOrder.getFont().getFontData()[0];
		radListOrder.setFont(new Font(parentShell
				.getDisplay(), new FontData(fontData.getName(), fontData
				.getHeight(), SWT.BOLD)));
		radListOrder.setForeground(parentShell
				.getDisplay().getSystemColor(SWT.COLOR_BLUE));

		fd = new FormData();
		fd.left = new FormAttachment(radListOrderLabel, 0);
		fd.top = new FormAttachment(radListOrderLabel, 0, SWT.CENTER);

		radListOrder.setLayoutData(fd);

		radListOrder.setText(appContext.getBean(CommandProperties.class).getRadiationListOrder().toString());
	}

	private void createRequestPool() {
		final Composite reqPoolComp = new Composite(this.bodyComposite, SWT.BORDER);
		final GridLayout gl = new GridLayout();
		gl.numColumns = 3;
		reqPoolComp.setLayout(gl);

		final Label reqPoolLabel = new Label(reqPoolComp, SWT.NONE);
		reqPoolLabel.setText("Request Pool");
		final FontData fd = reqPoolLabel.getFont().getFontData()[0];
		reqPoolLabel.setFont(new Font(parentShell
				.getDisplay(), new FontData(fd.getName(), fd.getHeight(),
				SWT.BOLD | SWT.UNDERLINE_SINGLE)));
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		reqPoolLabel.setLayoutData(gd);

		this.requestPoolEnableButton = new ToggleImageButton(reqPoolComp,
				SWT.NONE, openedIconPath, closedIconPath, questionIconPath);
		this.requestPoolEnableButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(final Event event) {
				CpdRequestTable.this.notifyPreparationStateChange();
			}
		});
		
		this.requestPoolEnableButton
				.setSelectedTooltip("Click to close request pool");
		this.requestPoolEnableButton
				.setDeselectedTooltip("Click to open request pool");

		reqPoolLabel.addListener(SWT.MouseHover, new Listener() {
			@Override
			public void handleEvent(final Event event) {
				final int itemCount = requestPool.getTable().getItemCount();
				reqPoolLabel.setToolTipText("Number of requests: " + itemCount);
			}
		});
		requestPool = new TableViewer(reqPoolComp, SWT.BORDER
				| SWT.FULL_SELECTION);

		// Keydown listener to disable text field popup
		requestPool.getTable().addListener(SWT.KeyDown, new Listener() {
			@Override
            public void handleEvent(final Event e) {
				if (e.character > 0) {
					e.doit = false;
				}
			}
		}); 
		requestPool.addSelectionChangedListener(this.tableSelectionManager);
		requestPool.getTable().addMouseListener(new MouseListener() {

			@Override
			public void mouseDoubleClick(final MouseEvent event) {
				// intentionally left blank
			}

			@Override
			public void mouseDown(final MouseEvent event) {
				if (event.button == 3) {
					CpdRequestTable.this.contextMenu.setVisible(true);
				}
			}

			@Override
			public void mouseUp(final MouseEvent event) {
				// intentionally left blank
			}
		});

		// configure the table for display
		final TableLayout layout = new TableLayout();
		layout.addColumnData(new ColumnWeightData(50, true));
		layout.addColumnData(new ColumnWeightData(20, true));
		layout.addColumnData(new ColumnWeightData(15, true));
		layout.addColumnData(new ColumnWeightData(15, true));

		requestPool.getTable().setLayout(layout);
		requestPool.getTable().setLinesVisible(true);
		requestPool.getTable().setHeaderVisible(true);

		requestPool.setLabelProvider(new CpdRequestPoolLabelProvider());

		final TableColumn filenameCol = new TableColumn(requestPool.getTable(),
				SWT.LEFT);
		filenameCol.setText("File Name");
		filenameCol.addSelectionListener(getSelectionAdapter(filenameCol, 0));

		final TableColumn statusCol = new TableColumn(requestPool.getTable(),
				SWT.CENTER);
		statusCol.setText("Status");
		statusCol.addSelectionListener(getSelectionAdapter(statusCol, 1));

		final TableColumn roleCol = new TableColumn(requestPool.getTable(),
				SWT.CENTER);
		roleCol.setText("Role");
		roleCol.addSelectionListener(getSelectionAdapter(roleCol, 2));

		final TableColumn checksumCol = new TableColumn(requestPool.getTable(),
				SWT.CENTER);
		checksumCol.setText("Checksum");

		gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 3;
		requestPool.getTable().setLayoutData(gd);

		comparator = new CpdRequestTableViewComparator();
		requestPool.setComparator(comparator);
		requestPool.setContentProvider(this.reqPoolModel);
		requestPool.setInput("");

		final Button rolePoolAggMethodButton = new Button(reqPoolComp, SWT.PUSH);
		rolePoolAggMethodButton.setText("Role Pool Settings...");
		gd = new GridData(SWT.CENTER, SWT.CENTER, true, false);
		gd.horizontalSpan = 3;
		rolePoolAggMethodButton.setLayoutData(gd);
		rolePoolAggMethodButton.setEnabled(false);
	}

	private void createRadiationList() {
		final Composite radListComp = new Composite(this.bodyComposite, SWT.BORDER);
		final GridLayout gl = new GridLayout();
		gl.numColumns = 3;
		radListComp.setLayout(gl);

		final Label radListLabel = new Label(radListComp, SWT.NONE);
		radListLabel.setText("Radiation List");
		final FontData fd = radListLabel.getFont().getFontData()[0];
		radListLabel.setFont(new Font(parentShell
				.getDisplay(), new FontData(fd.getName(), fd.getHeight(),
				SWT.BOLD | SWT.UNDERLINE_SINGLE)));

		// aggregation method
		aggregationMethod = new EditableItem(radListComp, SWT.NONE);
		aggregationMethod.setReadOnly(true);
		aggregationMethod.setLabel("Aggregation Method:");
		aggregationMethod.setEditDialogMessage("Set Aggregation Method");
		aggregationMethod.addEditListener(new IEditListener() {
			@Override
			public void onEdit(final EditEvent event) {
				AggregationMethod aggMethod = null;
				try {
					aggMethod = AggregationMethod.valueOf(event.getNewValue());
				} catch (final IllegalArgumentException e) {
					return;
				}

				for (final ICpdParametersChangeListener l : CpdRequestTable.this.paramListeners) {
					l.onAggregationMethodChange(aggMethod);
				}
			}
		});

		GridData gd = new GridData(SWT.CENTER, SWT.CENTER, true, false);
		aggregationMethod.setLayoutData(gd);

		this.radiationListEnableButton = new ToggleImageButton(radListComp,
				SWT.NONE, openedIconPath, closedIconPath, questionIconPath);
		this.radiationListEnableButton.addListener(SWT.Selection,
				new Listener() {
					@Override
					public void handleEvent(final Event event) {
						if (CpdRequestTable.this.requestPoolEnableButton
								.isSelected()) {
							CpdRequestTable.this.notifyPreparationStateChange();
						} else {
							CpdRequestTable.this.radiationListEnableButton
									.setSelection(false);
							SWTUtilities
									.showMessageDialog(CpdRequestTable.this
											.getControl().getShell(), "Info",
											"Cannot open radiation list while request pool is closed");
						}
					}
				});
		this.radiationListEnableButton
				.setSelectedTooltip("Click to close radiation list");
		this.radiationListEnableButton
				.setDeselectedTooltip("Click to open radiation list");

		radListLabel.addListener(SWT.MouseHover, new Listener() {
			@Override
			public void handleEvent(final Event event) {
				final int itemCount = radiationList.getTable().getItemCount();
				radListLabel.setToolTipText("Number of requests: " + itemCount);
			}
		});
		radiationList = new TableViewer(radListComp, SWT.BORDER
				| SWT.FULL_SELECTION);

		// Keydown listener to disable text field popup
		radiationList.getTable().addListener(SWT.KeyDown, new Listener() {
			@Override
            public void handleEvent(final Event e) {
				if (e.character > 0) {
					e.doit = false;
				}
			}
		}); 
		radiationList.addSelectionChangedListener(this.tableSelectionManager);
		radiationList.getTable().addMouseListener(new MouseListener() {

			@Override
			public void mouseDoubleClick(final MouseEvent event) {
				// intentionally left blank
			}

			@Override
			public void mouseDown(final MouseEvent event) {
				if (event.button == 3) {
					CpdRequestTable.this.contextMenu.setVisible(true);
				}
			}

			@Override
			public void mouseUp(final MouseEvent event) {
				// intentionally left blank
			}
		});

		// configure the table for display
		final TableLayout layout = new TableLayout();
		layout.addColumnData(new ColumnWeightData(30, true));
		layout.addColumnData(new ColumnWeightData(20, true));
		layout.addColumnData(new ColumnWeightData(16, true));
		layout.addColumnData(new ColumnWeightData(16, true));
		layout.addColumnData(new ColumnWeightData(18, true));

		radiationList.getTable().setLayout(layout);
		radiationList.getTable().setLinesVisible(true);
		radiationList.getTable().setHeaderVisible(true);

		radiationList.setLabelProvider(new CpdRadiationListLabelProvider());

		final TableColumn filenameCol = new TableColumn(radiationList.getTable(),
				SWT.LEFT);
		filenameCol.setText("File Name");

		final TableColumn statusCol = new TableColumn(radiationList.getTable(),
				SWT.CENTER);
		statusCol.setText("Status");

		final TableColumn durationCol = new TableColumn(radiationList.getTable(),
				SWT.CENTER);
		durationCol.setText("Est. Duration");

		final TableColumn totalCltuCol = new TableColumn(radiationList.getTable(),
				SWT.CENTER);
		totalCltuCol.setText("Total CLTUs");

		final TableColumn bit1RadTimeCol = new TableColumn(radiationList.getTable(),
				SWT.CENTER);
		bit1RadTimeCol.setText("Bit 1 Rad Time");

		gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 3;
		radiationList.getTable().setLayoutData(gd);
		radiationList.setContentProvider(this.radListModel);
		radiationList.setInput("");

		final Composite footer = new Composite(radListComp, SWT.NONE);

		gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gd.horizontalSpan = 3;
		footer.setLayoutData(gd);

		final GridLayout gl2 = new GridLayout();
		gl2.numColumns = 2;
		footer.setLayout(gl2);

		// execution method
		execMethod = new EditableItem(footer, SWT.NONE);
		execMethod.setLabel("Execution Method:");
		execMethod.setEditDialogMessage("Set Execution Method");
		execMethod.addEditListener(new IEditListener() {
			@Override
			public void onEdit(final EditEvent event) {
				ExecutionMethod execMethod = null;
				try {
					execMethod = ExecutionMethod.valueOf(event.getNewValue());
				} catch (final IllegalArgumentException e) {
					return;
				}

				for (final ICpdParametersChangeListener l : CpdRequestTable.this.paramListeners) {
					l.onExecutionMethodChange(execMethod);
				}
			}
		});

		final ExecutionMethod[] execMethods = ExecutionMethod.values();
		final String[] execMethodValues = new String[execMethods.length];

		for (int i = 0; i < execMethods.length; i++) {
			execMethodValues[i] = execMethods[i].toString();
		}

		execMethod.setEditValues(execMethodValues);

		gd = new GridData(SWT.LEFT, SWT.CENTER, true, false);
		execMethod.setLayoutData(gd);
		execMethod.setReadOnly(true);

		this.execStateButton = new Button(footer, SWT.PUSH);
		this.execStateButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(final Event event) {
				synchronized (CpdRequestTable.this.currExecState) {
					if (CpdRequestTable.this.currExecState
							.equals(ExecutionState.ENABLED)) {
						CpdRequestTable.this
								.notifyExecutionStateChange(ExecutionStateRequest.DISABLE);
					} else if (CpdRequestTable.this.currExecState
							.equals(ExecutionState.DISABLED)) {
						final MessageBox confirmDialog = new MessageBox(
								CpdRequestTable.this.getControl().getShell(),
								SWT.YES | SWT.NO);
						confirmDialog.setMessage("Enable Radiation?");

						final int returnCode = confirmDialog.open();

						if (returnCode == SWT.YES) {
							CpdRequestTable.this
									.notifyExecutionStateChange(ExecutionStateRequest.ENABLE);
						}
					}
				}
			}
		});
	}

	/**
	 * Add a listener to be notified when CPD request(s) are modified via the
	 * request pool/radiation list
	 * 
	 * @param listener the listener to be notified
	 */
	public void addCpdRequestChangeListener(final ICpdRequestChangeListener listener) {
		this.listeners.add(listener);
	}

	/**
	 * Remove a listener to stop notification of CPD request(s) modifications
	 * 
	 * @param listener the listener to stop notifications to
	 */
	public void removeCpdRequestChangeListener(
			final ICpdRequestChangeListener listener) {
		this.listeners.remove(listener);
	}

	/**
	 * Add a listener to be notified when CPD parameters are modified
	 * 
	 * @param listener the listener to be notified
	 */
	public void addCpdParameterChangeListener(
			final ICpdParametersChangeListener listener) {
		this.paramListeners.add(listener);
	}

	/**
	 * Remove a listener to stop notification of CPD parameters modifications
	 * 
	 * @param listener the listener to stop notifications to
	 */
	public void removeCpdParameterChangeListener(
			final ICpdParametersChangeListener listener) {
		this.paramListeners.remove(listener);
	}

	/**
	 * Get the selected request, either in the Request Pool or Radiation List.
	 * 
	 * @return the selected CPD request in the form of a CpdUplinkStatus
	 */
	protected final ICpdUplinkStatus getSelectedRequest() {
		ICpdUplinkStatus selectedRequest = null;
		final ISelection selection = this.getSelection();

		if (selection != null && selection instanceof IStructuredSelection) {
			final IStructuredSelection sel = (IStructuredSelection) selection;
			final Object firstElem = sel.getFirstElement();

			if (firstElem != null && firstElem instanceof ICpdUplinkStatus) {
				selectedRequest = (ICpdUplinkStatus) firstElem;
			}
		}

		return selectedRequest;
	}

	private SelectionAdapter getSelectionAdapter(final TableColumn column,
			final int index) {
		final SelectionAdapter selectionAdapter = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				comparator.setColumn(index);
				final int dir = comparator.getDirection();
				requestPool.getTable().setSortDirection(dir);
				requestPool.getTable().setSortColumn(column);
				requestPool.refresh();
			}
		};
		return selectionAdapter;
	}

	private void createButtonBar() {
		this.buttonBar = new Composite(this.masterComposite, SWT.NONE);

		final GridData gd = new GridData(SWT.FILL, SWT.FILL, false, false);
		this.buttonBar.setLayoutData(gd);

		final RowLayout layout = new RowLayout();
		layout.pack = true;
		layout.justify = true;
		this.buttonBar.setLayout(layout);

		final Button deleteButton = new Button(this.buttonBar, SWT.PUSH);
		deleteButton.setText("Delete");

		deleteButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent arg0) {
				final ICpdUplinkStatus request = CpdRequestTable.this
						.getSelectedRequest();

				if (request != null
						&& (request.getId() != null && request.getRoleId() != null)) {
					final String requestId = request.getId();
					final String requestRole = request.getRoleId();

					final MessageBox confirmDialog = new MessageBox(parentShell, SWT.YES | SWT.NO);
					confirmDialog
							.setMessage("Are you sure you want to delete the selected request from the CPD server?");
					confirmDialog.setText("Delete Request Confirmation");

					final int returnCode = confirmDialog.open();

					if (returnCode == SWT.YES) {
						logger.debug("Deleting request with ID: " + requestId);
						CpdRequestTable.this.notifyDelete(requestId,
								CommandUserRole.valueOf(requestRole));
					}
				} else {
					SWTUtilities.showErrorDialog(
							parentShell, "Delete Error",
							"No Request Selected");
				}

			}
		});

		final Button flushButton = new Button(this.buttonBar, SWT.PUSH);
		flushButton.setText("Flush");

		flushButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent arg0) {
				if (CpdRequestTable.this.reqPoolModel.getElements(null).length > 0) {
					final CpdRoleSelectorDialog roleDialog = new CpdRoleSelectorDialog(
							appContext.getBean(SecurityProperties.class), parentShell, "Flush Requests",
							"Select role to flush");
					int returnCode = roleDialog.open();

					if (returnCode == CpdRoleSelectorDialog.OK) {
						final CommandUserRole selectedRole = roleDialog.getSelectedRole();

						final MessageBox confirmDialog = new MessageBox(
								parentShell, SWT.YES
										| SWT.NO);

						String message = "";

						if (selectedRole == null) {
							message = "Are you sure you want to delete ALL active requests?";
						} else {
							message = "Are you sure you want to delete ALL active requests from the "
									+ selectedRole.toString() + " role pool?";
						}

						confirmDialog.setMessage(message);

						confirmDialog.setText("Flush Request Confirmation");

						returnCode = confirmDialog.open();

						if (returnCode == SWT.YES) {
							String debugMessage = "";

							if (selectedRole == null) {
								debugMessage = "Flushing ALL requests from CPD server";
							} else {
								debugMessage = "Flushing requests from "
										+ selectedRole.toString()
										+ " role pool on CPD server";
							}
							logger.debug(debugMessage);
							CpdRequestTable.this.notifyFlush(selectedRole);
						}
					}
				} else {
					SWTUtilities.showMessageDialog(
							parentShell, "Information",
							"Request pool is empty");
				}
			}
		});
	}

	/**
	 * This class determines what to display on each column of the Request Pool
	 * 
	 */
	private static class CpdRequestPoolLabelProvider implements
			ITableLabelProvider, ITableColorProvider {

		@Override
		public void addListener(final ILabelProviderListener listener) {
			// not implemented
		}

		@Override
		public void dispose() {
			// not implemented

		}

		@Override
		public boolean isLabelProperty(final Object element, final String property) {
			// not implemented
			return false;
		}

		@Override
		public void removeListener(final ILabelProviderListener listener) {
			// not implemented

		}

		@Override
		public Image getColumnImage(final Object element, final int index) {
			// not implemented
			return null;
		}

		@Override
		public String getColumnText(final Object element, final int index) {
			ICpdUplinkStatus status = null;
			if (element instanceof ICpdUplinkStatus) {
				status = (ICpdUplinkStatus) element;
			} else {
				return null;
			}

			switch (index) {
				case 0: {
					return status.getFilename();
				}
				case 1: {
					return status.getStatus().toString();
				}
				case 2: {
					return status.getRoleId();
				}
				case 3: {
					return status.getChecksum();
				}
			}

			return "";
		}

		@Override
		public Color getForeground(final Object element, final int columnIndex) {
			return null;
		}

		@Override
		public Color getBackground(final Object element, final int columnIndex) {
			ICpdUplinkStatus message = null;
			if (element instanceof ICpdUplinkStatus) {
				message = (ICpdUplinkStatus) element;
			} else {
				return null;
			}

			if (columnIndex == 1) {
				final CommandStatusType statusType = message.getStatus();

				final CommandStatusColorMapper colorMap = new CommandStatusColorMapper();
				return colorMap.getColorForStatus(statusType);
			}

			return null;
		}

	}

	/**
	 * This class determines what to display on each column of the Radiation
	 * List
	 * 
	 */
	private class CpdRadiationListLabelProvider implements
			ITableLabelProvider, ITableColorProvider {
		private boolean lastParamPollFailed = false;

		@Override
		public void addListener(final ILabelProviderListener listener) {
			// not implemented
		}

		@Override
		public void dispose() {
			// not implemented

		}

		@Override
		public boolean isLabelProperty(final Object element, final String property) {
			// not implemented
			return false;
		}

		@Override
		public void removeListener(final ILabelProviderListener listener) {
			// not implemented

		}

		@Override
		public Image getColumnImage(final Object element, final int index) {
			// not implemented
			return null;
		}

		@Override
		public String getColumnText(final Object element, final int index) {
			ICpdUplinkStatus status = null;
			if (element instanceof ICpdUplinkStatus) {
				status = (ICpdUplinkStatus) element;
			} else {
				return null;
			}

			switch (index) {
				case 0: {
					return status.getFilename();
				}
				case 1: {
					return status.getStatus().toString();
				}
				case 2: {
					if (lastParamPollFailed) {
						paramModel.refresh();
					}

					if (paramModel.getBitRate() > -1) {
						lastParamPollFailed = false;
						return status
								.getRadiationDurationString((float) paramModel
										.getBitRate());
					} else {
						if (paramModel.getConnectionStatus() != null
								&& paramModel.getConnectionStatus()
										.isConnected()) {
							lastParamPollFailed = true;
						}

						return "TBD";
					}
				}
				case 3: {
					if (status.getTotalCltus() < 0) {
						return "TBD";
					} else {
						return Integer.toString(status.getTotalCltus());
					}
				}
				case 4: {
					if (status.getBit1RadTimeString() != null) {
						return status.getBit1RadTimeString().split("T")[1];
					} else {
						return "TBD";
					}
				}
			}

			return "";
		}

		@Override
		public Color getForeground(final Object element, final int columnIndex) {
			return null;
		}

		@Override
		public Color getBackground(final Object element, final int columnIndex) {
			ICpdUplinkStatus message = null;
			if (element instanceof ICpdUplinkStatus) {
				message = (ICpdUplinkStatus) element;
			} else {
				return null;
			}

			if (columnIndex == 1) {
				final CommandStatusType statusType = message.getStatus();

				final CommandStatusColorMapper colorMap = new CommandStatusColorMapper();
				return colorMap.getColorForStatus(statusType);
			}

			return null;
		}

	}

	/**
	 * This class is a ViewerComparator that is used to sort requests in the CPD
	 * request pool
	 * 
	 */
	private static class CpdRequestTableViewComparator extends ViewerComparator {
		/** The index of the column to sort on */
		private int propertyIndex;

		/** DESCENDING...-1 is ASCENDING */
		private static final int DESCENDING = 1;

		/** The current sort direction */
		private int direction = DESCENDING;

		/**
		 * Constructor
		 */
		public CpdRequestTableViewComparator() {
			this.propertyIndex = 0;
			direction = DESCENDING;
		}

		/**
		 * Get the sort direction
		 * 
		 * @return the sort direction. SWT.UP or SWT.DOWN
		 */
		public int getDirection() {
			return direction == 1 ? SWT.DOWN : SWT.UP;
		}

		/**
		 * Set the column to sort on
		 * 
		 * @param column the column (by index) to sort on
		 */
		public void setColumn(final int column) {
			if (column == this.propertyIndex) {
				// Same column as last sort; toggle the direction
				direction = 1 - direction;
			} else {
				// New column; do an ascending sort
				this.propertyIndex = column;
				direction = DESCENDING;
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.jface.viewers.ViewerComparator#compare(org.eclipse.jface
		 * .viewers.Viewer, java.lang.Object, java.lang.Object)
		 */
		@Override
		public int compare(final Viewer viewer, final Object e1, final Object e2) {
			final ICpdUplinkStatus status1 = (ICpdUplinkStatus) e1;
			final ICpdUplinkStatus status2 = (ICpdUplinkStatus) e2;

			int rc = 0;
			switch (propertyIndex) {
				case 0:
					rc = status1.getFilename().compareTo(status2.getFilename());
					break;
				case 1:
					rc = status1.getStatus().compareTo(status2.getStatus());
					break;
				case 2:
					rc = status1.getRoleId().compareTo(status2.getRoleId());
					break;
				case 3:
					rc = status1.getChecksum().compareTo(status2.getChecksum());
					break;
				default:
					rc = 0;
			}

			// If descending order, flip the direction
			if (direction == DESCENDING) {
				rc = -rc;
			}
			return rc;
		}
	}

	/**
	 * Notify listeners that a request has been selected for deletion
	 * 
	 * @param requestId the request ID of the request to be deleted
	 * @param requestRole the role of the original user that submitted the
	 *            request
	 */
	protected void notifyDelete(final String requestId, final CommandUserRole requestRole) {
		for (final ICpdRequestChangeListener l : listeners) {
			l.onRequestDelete(requestId, requestRole);
		}
	}

	/**
	 * Notify listeners that a flush has been initialized by the user
	 * 
	 * @param rolePool the role pool to flush
	 */
	protected void notifyFlush(final CommandUserRole rolePool) {
		for (final ICpdRequestChangeListener l : listeners) {
			l.onRequestFlush(rolePool);
		}
	}

	private void notifyPreparationStateChange() {
		ListPreparationStateEnum newState = null;

		// if request pool is locked, then list preparation is DISABLED
		if (!requestPoolEnableButton.isSelected()) {
			// radiation list must be locked if request pool is locked. There is
			// no corresponding preparation state that maps to a locked request
			// pool and an unlocked radiation list.
			this.radiationListEnableButton.setSelection(false);
			newState = ListPreparationStateEnum.DISABLED;
		} else {
			// if request pool is unlocked and radiation list is locked, then
			// list preparation is LOCK
			if (!radiationListEnableButton.isSelected()) {
				newState = ListPreparationStateEnum.LOCK;
			} else { // if both are unlocked, then list preparation is OPEN
				newState = ListPreparationStateEnum.OPEN;
			}
		}

		for (final ICpdParametersChangeListener l : paramListeners) {
			l.onPreparationStateChange(newState);
		}
	}

	private void notifyExecutionStateChange(final ExecutionStateRequest newState) {
		for (final ICpdParametersChangeListener l : paramListeners) {
			l.onExecutionStateChange(newState);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.Viewer#getControl()
	 */
	@Override
	public Control getControl() {
		return this.masterComposite;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.Viewer#getSelection()
	 */
	@Override
	public ISelection getSelection() {
		ISelection selection = this.requestPool.getSelection();

		if (selection == null || selection.isEmpty()) {
			selection = this.radiationList.getSelection();
		}

		return selection;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.Viewer#refresh()
	 */
	@Override
	public void refresh() {
		final IContentProvider provider = this.getContentProvider();

		if (provider instanceof CpdParametersModel) {
			final CpdParametersModel model = (CpdParametersModel) provider;

			final boolean stale = model.isStale();

			if (stale) {
				this.preparationState.setForeground(parentShell.getDisplay()
						.getSystemColor(SWT.COLOR_GRAY));
				this.executionState.setForeground(parentShell.getDisplay()
						.getSystemColor(SWT.COLOR_GRAY));
				this.aggregationMethod.setForeground(parentShell.getDisplay()
						.getSystemColor(SWT.COLOR_GRAY));
				this.execMethod.setForeground(parentShell
						.getDisplay().getSystemColor(SWT.COLOR_GRAY));
			} else {
				if (model.getPreparationState() != null) {
					if (model.getPreparationState().equals(
							ListPreparationStateEnum.OPEN)) {
						this.preparationState.setForeground(parentShell.getDisplay()
								.getSystemColor(SWT.COLOR_DARK_GREEN));
					} else if (model.getPreparationState().equals(
							ListPreparationStateEnum.LOCK)) {
						this.preparationState.setForeground(parentShell.getDisplay()
								.getSystemColor(SWT.COLOR_DARK_YELLOW));
					} else if (model.getPreparationState().equals(
							ListPreparationStateEnum.DISABLED)) {
						this.preparationState.setForeground(parentShell.getDisplay()
								.getSystemColor(SWT.COLOR_RED));
					} else {
						this.preparationState.setForeground(parentShell.getDisplay()
								.getSystemColor(SWT.COLOR_BLUE));
					}
				} else {
					this.preparationState.setForeground(parentShell.getDisplay()
							.getSystemColor(SWT.COLOR_BLUE));
				}

				if (model.getExecutionState() != null) {
					if (model.getExecutionState()
							.equals(ExecutionState.ENABLED)) {
						this.executionState.setForeground(parentShell.getDisplay()
								.getSystemColor(SWT.COLOR_DARK_GREEN));
					} else if (model.getExecutionState().equals(
							ExecutionState.DISABLED)) {
						this.executionState.setForeground(parentShell.getDisplay()
								.getSystemColor(SWT.COLOR_RED));
					} else {
						this.executionState.setForeground(parentShell.getDisplay()
								.getSystemColor(SWT.COLOR_BLUE));
					}
				} else {
					this.executionState.setForeground(parentShell.getDisplay()
							.getSystemColor(SWT.COLOR_BLUE));
				}

				this.aggregationMethod.setForeground(parentShell.getDisplay()
						.getSystemColor(SWT.COLOR_BLUE));
				this.execMethod.setForeground(parentShell
						.getDisplay().getSystemColor(SWT.COLOR_BLUE));
			}

			// preparation state
			final ListPreparationStateEnum prepState = model.getPreparationState();

			if (prepState == null) {
				this.requestPoolEnableButton.setEnabled(false);
				this.radiationListEnableButton.setEnabled(false);
				this.preparationState.setText("UNKNOWN");
			} else {
				this.preparationState.setText(prepState.toString());
				this.requestPoolEnableButton.setEnabled(true);
				this.radiationListEnableButton.setEnabled(true);

				switch (prepState) {
					case DISABLED:
						// lock the request pool
						this.requestPoolEnableButton.setSelection(false);
						break;
					case OPEN:
						// unlock both the request pool and radiation list
						this.requestPoolEnableButton.setSelection(true);
						this.radiationListEnableButton.setSelection(true);
						break;
					case LOCK:
						// unlock request pool but lock radiation list
						this.requestPoolEnableButton.setSelection(true);
						this.radiationListEnableButton.setSelection(false);
						break;
				}
			}

			// execution state
			final ExecutionState execState = model.getExecutionState();
			if (execState == null) {
				// disable red/green button
				this.execStateButton.setEnabled(false);
				this.executionState.setText("UNKNOWN");
			} else {
				this.executionState.setText(execState.toString());

				synchronized (this.execStateLock) {
					this.currExecState = execState;
					this.execStateButton.setEnabled(true);
					switch (execState) {
						case ENABLED:
							this.execStateButton.setText("Disable Execution");
							break;
						case DISABLED:
							this.execStateButton.setText("Enable Execution");
							break;
						default:
							this.execStateButton.setEnabled(false);
							break;
					}

					this.execStateButton.getParent().layout();
				}
			}

			// layout header bar
			this.headerComposite.layout();

			// execution method
			final ExecutionMethod execMethod = model.getExecutionMethod();
			final String execMethodStr = execMethod == null ? "UNKNOWN" : execMethod
					.toString();

			this.execMethod.setValue(execMethodStr);
			this.execMethod.getParent().layout();

			// aggregation method
			final AggregationMethod aggMethod = model.getAggregationMethod();
			final String aggMethodStr = aggMethod == null ? "UNKNOWN" : aggMethod
					.toString();

			this.aggregationMethod.setValue(aggMethodStr);
			this.aggregationMethod.pack(true);
			this.aggregationMethod.layout();
		} else {
			return;
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
	public void setSelection(final ISelection arg0, final boolean arg1) {
		// intentionally left blank
	}

	private class RequestTableSelectionManager implements
			ISelectionChangedListener {

		@Override
		public void selectionChanged(final SelectionChangedEvent event) {
			if (event.getSource() == CpdRequestTable.this.requestPool) {
				CpdRequestTable.this.radiationList.getTable().deselectAll();
			} else if (event.getSource() == CpdRequestTable.this.radiationList) {
				CpdRequestTable.this.requestPool.getTable().deselectAll();
			}
		}
	}
}