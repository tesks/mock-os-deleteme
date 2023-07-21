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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.springframework.context.ApplicationContext;

import jpl.gds.monitor.config.MonitorGuiProperties;
import jpl.gds.monitor.guiapp.gui.ViewBrowserShell;
import jpl.gds.monitor.perspective.view.CustomGridViewConfiguration;
import jpl.gds.monitor.perspective.view.GridOrientationType;
import jpl.gds.monitor.perspective.view.fixed.IFixedLayoutViewConfiguration;
import jpl.gds.perspective.config.PerspectiveProperties;
import jpl.gds.perspective.config.ViewProperties;
import jpl.gds.perspective.config.ViewType;
import jpl.gds.perspective.view.IViewConfiguration;
import jpl.gds.perspective.view.ViewFactory;
import jpl.gds.perspective.view.ViewTriple;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.swt.ChillColorCreator;
import jpl.gds.shared.swt.ChillFontCreator;
import jpl.gds.shared.swt.ColorComposite;
import jpl.gds.shared.swt.FontComposite;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.shared.swt.TitleComposite;
import jpl.gds.shared.swt.types.ChillColor;
import jpl.gds.shared.swt.types.ChillFont;

/**
 * 
 * CustomGridPreferencesShell is the preferences control for configuring a
 * Custom Grid view.
 *
 */
public class CustomGridPreferencesShell extends AbstractViewPreferences {
	
	/**
	 * Custom grid preferences window title
	 */
	public static final String TITLE = "Custom View Configuration";

	private static final String IMAGE_RESOURCE_DIR = "jpl/gds/monitor/gui/";
	
	private static final String FILL_IMAGE = IMAGE_RESOURCE_DIR + "blankbox.gif";
	private static final String TRASH_IMAGE = IMAGE_RESOURCE_DIR + "trash.gif";

	private Image fillImage = null;

	private static final int MAX_COLUMNS = 10;
	private static final int MAX_ROWS = 10;
	private static final int VIEW_IMAGE_COLUMNS = 10;

	private static String[] viewImageNames; 
	private static String[] viewNames;
	private static String[] viewTypes;
	private static Image[] images;
	
	private final Color black;
	private final Color white;

	private final Shell parent;
	private LabeledViewImage[] viewImages;
	private int numColumns;
	private int numRows;
	private GridOrientationType orientation;
	private boolean isNew = false;
	private Label viewButtons[][];
	private Composite viewButtonComp;

	private Combo rowCombo;
	private Combo columnCombo;
	private Group viewTableGroup;
	private Composite viewImageComposite;
	private Button orientButton;
	private Label removeButton;

    private final Tracer                         trace;

	private final Map<String,IViewConfiguration> viewConfigs = new HashMap<String,IViewConfiguration>();

	/**
	 * Creates an instance of CustomGridPreferencesShell.
	 * @param parent the Shell parent of this GUI widget
	 */
	public CustomGridPreferencesShell(final ApplicationContext appContext, final Shell parent) {
		super(appContext, TITLE);
		this.parent = parent;
        this.trace = TraceManager.getTracer(appContext, Loggers.DEFAULT);
		fillImage = SWTUtilities.createImage(this.parent.getDisplay(), FILL_IMAGE);

		setupViewConfiguration();
		createControls();
		prefShell.setLocation(parent.getLocation().x + 100, parent.getLocation().y + 100);
		black = ChillColorCreator.getColor(new ChillColor(ChillColor.ColorName.BLACK));
		white = ChillColorCreator.getColor(new ChillColor(ChillColor.ColorName.WHITE));
	}

	/**
	 * Sets the flag indicating this configuration is for a new grid.
	 * @param newFlag true if a new grid
	 */
	public void setNew(final boolean newFlag) {
		isNew = newFlag;  
		if (!orientButton.isDisposed()) {
			orientButton.setEnabled(isNew);
		}
	}

	private void setupViewConfiguration() {
		if (viewNames != null) {
			return;
		}
		final MonitorGuiProperties config = appContext.getBean(MonitorGuiProperties.class);

		viewTypes = config.getGridAllowedViewTypes().toArray(new String[] {});
		viewNames = getDefaultViewNames(viewTypes);
		viewImageNames = config.getGridImageNames().toArray(new String[] {});   
		images = new Image[viewNames.length];

		for (int i = 0; i < viewImageNames.length; i++) {
			images[i] = SWTUtilities.createImage(parent.getDisplay(), IMAGE_RESOURCE_DIR + viewImageNames[i]);
		}
	}

	private String[] getDefaultViewNames(final String[] viewTypes) {
		final String[] result = new String[viewTypes.length];
		int i = 0;
		for (final String type: viewTypes) {
		    final ViewProperties vp = appContext.getBean(PerspectiveProperties.class).getViewProperties(new ViewType(type));
			final String str = vp.getStringDefault(IViewConfiguration.DEFAULT_CONFIG_NAME_PROPERTY);
			if (str != null) {
				result[i++] = str;
			} else {
				result[i++] = vp.getStringDefault(IViewConfiguration.DEFAULT_NAME_PROPERTY);
			}
		}
		return result;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.monitor.guiapp.gui.views.preferences.AbstractViewPreferences#setValuesFromViewConfiguration(jpl.gds.perspective.view.ViewConfiguration)
	 */
	@Override
	public void setValuesFromViewConfiguration(final IViewConfiguration config) {
		super.setValuesFromViewConfiguration(config);
		final CustomGridViewConfiguration gridConfig = (CustomGridViewConfiguration)config;
		setOrientation(gridConfig.getGridOrientation());
		setRows(gridConfig.getGridRows());
		setColumns(gridConfig.getGridColumns());
		buildViewConfigs(gridConfig.getViewCoordinates(), gridConfig.getViews());
		fillSlotTable();
		assignViewsToSlots();
		prefShell.pack();
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.monitor.guiapp.gui.views.preferences.AbstractViewPreferences#getValuesIntoViewConfiguration(jpl.gds.perspective.view.ViewConfiguration)
	 */
	@Override
	public void getValuesIntoViewConfiguration(final IViewConfiguration config) {
		super.getValuesIntoViewConfiguration(config);
		final CustomGridViewConfiguration gridConfig = (CustomGridViewConfiguration)config;
		gridConfig.setGridOrientation(getOrientation());
		gridConfig.setGridRows(getRows());
		gridConfig.setGridColumns(getColumns());
		gridConfig.setViewCoordinates(getViewTriples());
		gridConfig.setViews(getViewConfigs());
		setWeights(gridConfig);
	}

	private GridOrientationType getOrientation() {
		return orientation;
	}

	private void setOrientation(final GridOrientationType orient) {
		orientation = orient;
		if (!orientButton.isDisposed()) {
			orientButton.setSelection(orient == GridOrientationType.COLUMN_DOMINANT);
		}
	}

	private void buildViewConfigs(final List<ViewTriple> coords, final List<IViewConfiguration> list) {

		viewConfigs.clear();
		final Iterator<ViewTriple> it = coords.iterator();
		while (it.hasNext()) {
			final ViewTriple triple = it.next();
			final int viewNumber = triple.getNumber();
			final IViewConfiguration currentView = list.get(viewNumber);
			final String key = String.valueOf(triple.getXLoc()) + "," + String.valueOf(triple.getYLoc());
			viewConfigs.put(key, currentView);

		}
	}

	private List<ViewTriple> getViewTriples() {
		final List<ViewTriple> result = new ArrayList<ViewTriple>();
		if (orientation == GridOrientationType.ROW_DOMINANT) {
			int num = 0;
			for (int row = 0; row < numRows; row++) {
				int actualCol = 0;
				for (int col = 0; col < numColumns; col++) {
					final String key = String.valueOf(col) + "," + String.valueOf(row);
					final IViewConfiguration config = viewConfigs.get(key);
					if (config != null) {
						final ViewTriple vt = new ViewTriple(config.getViewType(), actualCol++, row, num++);
						result.add(vt);
					}
				}
			}
		} else {
			int num = 0;
			for (int col = 0; col < numColumns; col++) {
				int actualRow = 0;
				for (int row = 0; row < numRows; row++) {
					final String key = String.valueOf(col) + "," + String.valueOf(row);
					final IViewConfiguration config = viewConfigs.get(key);
					if (config != null) {
						final ViewTriple vt = new ViewTriple(config.getViewType(), col, actualRow++, num++);
						result.add(vt);
					}
				}
			}
		}
		return result;
	}

	private void setWeights(final CustomGridViewConfiguration config) {
		if (orientation == GridOrientationType.ROW_DOMINANT) {
			final int rowWeight = 100 / numRows;

			for (int i = 0; i < numRows; i++) {
				config.setDominantDimensionWeight(i, rowWeight);
				int cols = getActualColumnCount(i);
				final int[] weights = new int[cols];
				if (cols == 0) {
					cols = 1;
				}
				final int colWeight = 100 / cols;
				Arrays.fill(weights, colWeight);
				config.setVariableWeights(i, weights);
			}
		} else {
			final int colWeight = 100 / numColumns;

			for (int i = 0; i < numColumns; i++) {
				config.setDominantDimensionWeight(i, colWeight);
				int rows = getActualRowCount(i);
				final int[] weights = new int[rows];
				if (rows == 0) {
					rows = 1;
				}
				final int rowWeight = 100 / rows;
				Arrays.fill(weights, rowWeight);
				config.setVariableWeights(i, weights);
			}
		}
	}

	private int getActualColumnCount(final int rowIndex) {
		int colCount = 0;
		for (int col = 0; col < numColumns; col++) {
			final String key = String.valueOf(col) + "," + String.valueOf(rowIndex);
			final IViewConfiguration config = viewConfigs.get(key);
			if (config != null) {
				colCount++;
			}
		}
		return colCount;
	}

	private int getActualRowCount(final int colIndex) {
		int rowCount = 0;
		for (int row = 0; row < numRows; row++) {
			final String key = String.valueOf(colIndex) + "," + String.valueOf(row);
			final IViewConfiguration config = viewConfigs.get(key);
			if (config != null) {
				rowCount++;
			}
		}
		return rowCount;
	}

	private ArrayList<IViewConfiguration> getViewConfigs() {
		final ArrayList<IViewConfiguration> result = new ArrayList<IViewConfiguration>();

		if (orientation == GridOrientationType.ROW_DOMINANT) {
			for (int row = 0; row < numRows; row++) {
				int actualCol = 0;
				for (int col = 0; col < numColumns; col++) {
					final String key = String.valueOf(col) + "," + String.valueOf(row);
					final IViewConfiguration config = viewConfigs.get(key);
					if (config != null) {
						if (isNew && !(config instanceof IFixedLayoutViewConfiguration)) {
							config.setDataFont(getDataFont());
							config.setBackgroundColor(getBackgroundColor());
						}
						result.add(config);
						actualCol++;
					}
				}
			}
		} else {
			for (int col = 0; col < numColumns; col++) {
				int actualRow = 0;
				for (int row = 0; row < numRows; row++) {
					final String key = String.valueOf(col) + "," + String.valueOf(row);
					final IViewConfiguration config = viewConfigs.get(key);
					if (config != null) {
						if (isNew && !(config instanceof IFixedLayoutViewConfiguration)) {
							config.setDataFont(getDataFont());
							config.setBackgroundColor(getBackgroundColor());
						}
						result.add(config);
						actualRow++;
					}
				}
			}
		}
		return result;
	}

	private int getRows() {
		return numRows;
	}

	private int getColumns() {
		return numColumns;
	}

	private void setRows(final int rows) {
		numRows = rows;
		if (rowCombo != null && !rowCombo.isDisposed()) {
			rowCombo.setText(String.valueOf(rows));
		}
	}

	private void setColumns(final int cols) {
		numColumns = cols;
		if (columnCombo != null && !columnCombo.isDisposed()) {
			columnCombo.setText(String.valueOf(cols));
		}
	}

	private void createControls() {

		prefShell = new Shell(parent);
		prefShell.setText(TITLE);
		final FormLayout fl = new FormLayout();
		fl.marginHeight = 5;
		fl.marginWidth = 5;
		fl.spacing = 5;
		prefShell.setLayout(fl);

		titleText = new TitleComposite(prefShell);
		final Composite titleComp = titleText.getComposite();
		final FormData fdLabel2 = new FormData();
		fdLabel2.top = new FormAttachment(0, 10);
		fdLabel2.left = new FormAttachment(0, 3);
		titleComp.setLayoutData(fdLabel2);

        fontGetter = new FontComposite(prefShell, "Default Data Font", trace);
		final Composite fontComp = fontGetter.getComposite();
		final FormData fontFd = new FormData();
		fontFd.top = new FormAttachment(titleComp, 0, 5);
		fontFd.left = new FormAttachment(0);
		fontFd.right = new FormAttachment(100);
		fontComp.setLayoutData(fontFd);

        backColorGetter = new ColorComposite(prefShell, "Default Background Color", trace);
		Composite colorComp = backColorGetter.getComposite();
		final FormData colorFd = new FormData();
		colorFd.top = new FormAttachment(fontComp, 0, 3);
		colorFd.left = new FormAttachment(0);
		colorFd.right = new FormAttachment(100);
		colorComp.setLayoutData(colorFd);

        foreColorGetter = new ColorComposite(prefShell, "Default Foreground Color", trace);
		final FormData foreColorFd = new FormData();
		foreColorFd.top = new FormAttachment(colorComp, 0, 3);
		foreColorFd.left = new FormAttachment(0);
		foreColorFd.right = new FormAttachment(100);
		colorComp = foreColorGetter.getComposite();
		colorComp.setLayoutData(foreColorFd);

		orientButton = new Button(prefShell, SWT.CHECK);
		orientButton.setText("Use Column Dominate Layout");
		final FormData orientFd = new FormData();
		orientFd.top = new FormAttachment(colorComp, 0, 3);
		orientFd.left = new FormAttachment(0);
		orientButton.setLayoutData(orientFd);
		orientButton.setEnabled(isNew);

		createRowColumnCombos(orientButton);
		createViewImages();
		createViewTable();
		createButtonPanel();

		prefShell.pack();
	}

	private void createRowColumnCombos(final Control widgetAbove) {
		columnCombo = new Combo(prefShell, SWT.NONE);
		final Label columnComboLabel = new Label(prefShell, SWT.NONE);
		columnComboLabel.setText("Number of Columns:");
		FormData fd = new FormData();
		fd.top = new FormAttachment(widgetAbove, 15, 10);
		fd.left = new FormAttachment(0, 4);
		columnComboLabel.setLayoutData(fd);
		fd = SWTUtilities.getFormData(columnCombo, 1, 8);
		fd.top = new FormAttachment(columnComboLabel, 0, SWT.CENTER);
		fd.left = new FormAttachment(columnComboLabel);
		columnCombo.setLayoutData(fd);
		for (int i = 1; i <= MAX_COLUMNS; i++) {
			columnCombo.add(String.valueOf(i));
		}
		columnCombo.setText(String.valueOf(numColumns));

		columnCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				try {
					final int oldColumns = numColumns;
					final int newColumns = Integer.parseInt(columnCombo.getText());
					if (oldColumns == newColumns) {
						return;
					}
					if (oldColumns > newColumns) {
						final int removeCount = oldColumns - newColumns;
						for (int i = 1; i <= removeCount; i++) {
							removeLastColumn();
						}
					} else if (oldColumns < newColumns) {
						addColumns(newColumns - oldColumns);
					}

					CustomGridPreferencesShell.this.prefShell.pack();
				} catch (final Exception e1) {
					e1.printStackTrace();
				}
			}
		});

		rowCombo = new Combo(prefShell, SWT.NONE);
		final Label rowComboLabel = new Label(prefShell, SWT.NONE);
		rowComboLabel.setText("Number of Rows:");
		fd = new FormData();
		fd.top = new FormAttachment(widgetAbove, 15, 10);
		fd.left = new FormAttachment(columnCombo, 8);
		rowComboLabel.setLayoutData(fd);
		fd = SWTUtilities.getFormData(columnCombo, 1, 8);
		fd.top = new FormAttachment(rowComboLabel, 0, SWT.CENTER);
		fd.left = new FormAttachment(rowComboLabel);
		rowCombo.setLayoutData(fd);
		for (int i = 1; i <= MAX_ROWS; i++) {
			rowCombo.add(String.valueOf(i));
		}
		rowCombo.setText(String.valueOf(numRows));

		rowCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				try {
					final int oldRows = numRows;
					final int newRows = Integer.parseInt(rowCombo.getText());
					if (oldRows == newRows) {
						return;
					}
					if (oldRows > newRows) {
						final int removeRows = oldRows - newRows;
						for (int i = 1; i <= removeRows ; i++) {
							removeLastRow();
						}
					} else if (oldRows < newRows) {
						addRows(newRows - oldRows);
					}
					CustomGridPreferencesShell.this.prefShell.pack();
				} catch (final Exception e1) {
					e1.printStackTrace();
				}
			}
		});
	}

	private void createViewImages() {
		viewTableGroup = new Group(prefShell, SWT.BORDER | SWT.NONE);
		viewTableGroup.setText("Window Layout");
		final FontData groupFontData = new FontData("Helvetica", 14, SWT.NONE);
		final Font groupFont = new Font(parent.getDisplay(), groupFontData);
		viewTableGroup.setFont(groupFont);
		final FormData viewtablefd = new FormData();
		viewtablefd.top = new FormAttachment(rowCombo, 15);
		viewtablefd.left = new FormAttachment(0);
		viewtablefd.right = new FormAttachment(100);
		viewTableGroup.setLayoutData(viewtablefd);
		viewTableGroup.setLayout(new FormLayout());

		viewImageComposite = new Composite(viewTableGroup, SWT.NONE);
		final FormData fd = new FormData();
		fd.top = new FormAttachment(0);
		fd.left = new FormAttachment(0, 10);
		viewImageComposite.setLayoutData(fd);
		viewImageComposite.setLayout(new GridLayout(VIEW_IMAGE_COLUMNS, false));

		viewImages = new LabeledViewImage[viewNames.length];

		for (int i = 0; i < viewNames.length; i++) {

			viewImages[i] = new LabeledViewImage(images[i], viewNames[i], viewImageComposite, i);  

			// Allow drag of the image label
			// Provide data to the drag destination in text format
			final int operations = DND.DROP_MOVE | DND.DROP_COPY;
			final DragSource source = new DragSource(viewImages[i].imageLabel, operations);
			final Transfer[] types = new Transfer[] { TextTransfer.getInstance() };
			source.setTransfer(types);

			// enable processing of drag events
			source.addDragListener(new DragSourceListener() {
				@Override
				public void dragStart(final DragSourceEvent event) {
					try {
						// Only start the drag if there is actually text in the
						// label - this text will be what is dropped on the target.
						final DragSource source = (DragSource) event.getSource();
						if (((Label) source.getControl()).getText().length() == 0) {
							event.doit = false;
						}
					} catch (final Exception e) {
						e.printStackTrace();
					}
				}

				@Override
				public void dragSetData(final DragSourceEvent event) {
					try {
						// Provide the data of the requested type.
						final DragSource source = (DragSource) event.getSource();
						if (TextTransfer.getInstance().isSupportedType(
								event.dataType)) {
							event.data = ((Label) source.getControl()).getText();
						}
					} catch (final Exception e) {
						e.printStackTrace();
					}
				}

				@Override
				public void dragFinished(final DragSourceEvent event) {
				}
			});

		}

		removeButton = new Label(viewTableGroup, SWT.NONE);
		removeButton.setImage(SWTUtilities.createImage(parent.getDisplay(), TRASH_IMAGE));
		final FormData removeFd = new FormData();
		removeFd.right = new FormAttachment(100, -5);
		removeFd.bottom = new FormAttachment(100, -5);
		removeButton.setLayoutData(removeFd);

		final int operations = DND.DROP_MOVE;
		final DropTarget target = new DropTarget(removeButton, operations);
		final TextTransfer textTransfer = TextTransfer.getInstance();
		final Transfer[] types = new Transfer[] { textTransfer };
		target.setTransfer(types);

		// Add the handling for drop events
		target.addDropListener(new DropTargetListener() {
			@Override
			public void dragEnter(final DropTargetEvent event) {
				try {
					if (event.detail == DND.DROP_DEFAULT) {
						if ((event.operations & DND.DROP_MOVE) != 0) {
							event.detail = DND.DROP_MOVE;
						} else {
							event.detail = DND.DROP_NONE;
						}
					}
				} catch (final Exception e) {
					e.printStackTrace();
				}
			}

			/**
		     * {@inheritDoc}
			 * @see org.eclipse.swt.dnd.DropTargetListener#dragOver(org.eclipse.swt.dnd.DropTargetEvent)
			 */
			@Override
			public void dragOver(final DropTargetEvent event) {
			}

			/**
		     * {@inheritDoc}
			 * @see org.eclipse.swt.dnd.DropTargetListener#dragOperationChanged(org.eclipse.swt.dnd.DropTargetEvent)
			 */
			@Override
			public void dragOperationChanged(final DropTargetEvent event) {
				try {
					if (event.detail == DND.DROP_DEFAULT) {
						if ((event.operations & DND.DROP_MOVE) != 0) {
							event.detail = DND.DROP_MOVE;
						} else {
							event.detail = DND.DROP_NONE;
						}
					}
				} catch (final Exception e) {
					e.printStackTrace();
				}
			}

			/**
		     * {@inheritDoc}
			 * @see org.eclipse.swt.dnd.DropTargetListener#dragLeave(org.eclipse.swt.dnd.DropTargetEvent)
			 */
			@Override
			public void dragLeave(final DropTargetEvent event) {
			}

			/**
		     * {@inheritDoc}
			 * @see org.eclipse.swt.dnd.DropTargetListener#dropAccept(org.eclipse.swt.dnd.DropTargetEvent)
			 */
			@Override
			public void dropAccept(final DropTargetEvent event) {
			}

			/**
		     * {@inheritDoc}
			 * @see org.eclipse.swt.dnd.DropTargetListener#drop(org.eclipse.swt.dnd.DropTargetEvent)
			 */
			@Override
			public void drop(final DropTargetEvent event) {
				try {
					if (textTransfer.isSupportedType(event.currentDataType)) {
						final String text = (String) event.data;
						viewConfigs.remove(text);
						final String[] rowcol = text.split(",");
						if (rowcol.length < 2) {
							return;
						}
						final int col = Integer.parseInt(rowcol[0]);
						final int row = Integer.parseInt(rowcol[1]);
						final Label item = viewButtons[row][col];
						item.setImage(fillImage);
						removeButton.setFocus();
					}
				} catch (final Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	private void createViewTable() {
		viewButtonComp = new Composite(viewTableGroup, SWT.NONE);
		final FormData tablefd = new FormData();
		tablefd.top = new FormAttachment(viewImageComposite, 10);
		tablefd.left = new FormAttachment(0, 10);
		tablefd.bottom= new FormAttachment(100, -5);
		viewButtonComp.setLayoutData(tablefd);
		viewButtonComp.setToolTipText("Drag desired views to layout");

		fillSlotTable();
	}

	private void createButtonPanel() {

		// Add the button bar and the buttons       
		final Label line = new Label(prefShell, SWT.SEPARATOR | SWT.HORIZONTAL
				| SWT.SHADOW_ETCHED_IN);
		final FormData formData6 = new FormData();
		formData6.left = new FormAttachment(0);
		formData6.right = new FormAttachment(100);
		formData6.top = new FormAttachment(viewTableGroup, 10);
		line.setLayoutData(formData6);


		final Composite buttonComposite = new Composite(prefShell, SWT.NONE);
		final GridLayout rl = new GridLayout(2, true);
		buttonComposite.setLayout(rl);
		final FormData formData8 = new FormData();
		formData8.top = new FormAttachment(line, 0, 8);
		formData8.right = new FormAttachment(100);
		formData8.bottom = new FormAttachment(100);
		buttonComposite.setLayoutData(formData8);

		final Button okButton = new Button(buttonComposite, SWT.PUSH);
		okButton.setText("Ok");
		GridData gd = new GridData();
		gd.horizontalAlignment = SWT.FILL;
		okButton.setLayoutData(gd);

		okButton.addSelectionListener(new SelectionAdapter() {
			/**
		     * {@inheritDoc}
			 * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
			 */
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				try {
					CustomGridPreferencesShell.this.canceled = false;
					applyChanges();
					numRows = Integer.parseInt(rowCombo.getText());
					numColumns = Integer.parseInt(columnCombo.getText());
					orientation = orientButton.getSelection() ? GridOrientationType.COLUMN_DOMINANT : GridOrientationType.ROW_DOMINANT;
				} catch (final Exception e1) {
					e1.printStackTrace();
				} finally {
					CustomGridPreferencesShell.this.prefShell.close();
				}
			}
		});

		final Button cancelButton = new Button(buttonComposite, SWT.PUSH);
		cancelButton.setText("Cancel");
		gd = new GridData();
		gd.horizontalAlignment = SWT.FILL;
		cancelButton.setLayoutData(gd);

		cancelButton.addSelectionListener(new SelectionAdapter() {
			/**
		     * {@inheritDoc}
			 * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
			 */
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				try {
					CustomGridPreferencesShell.this.canceled = true;
					CustomGridPreferencesShell.this.prefShell.close();
				} catch (final Exception e1) {
					e1.printStackTrace();
				}
			}
		});
	}

	private void removeLastColumn() {
		final Label[][] newButtons = new Label[numRows][numColumns - 1];
		for (int i = 0; i < numRows; i++) { 
			System.arraycopy(viewButtons[i], 0, newButtons[i], 0, numColumns - 1);
			viewButtons[i][numColumns - 1].dispose();
			viewButtons[i][numColumns - 1] = null;
			final String key = String.valueOf(numColumns - 1) + "," + String.valueOf(i);
			viewConfigs.remove(key);
		}
		viewButtons = newButtons;
		numColumns--;
		viewButtonComp.setLayout(new GridLayout(numColumns, false));
		viewTableGroup.layout();
	}

	private void removeLastRow() {
		final Label[][] newButtons = new Label[numRows - 1][numColumns];
		for (int i = 0; i < numRows - 1; i++) {
			System.arraycopy(viewButtons[i], 0, newButtons[i], 0, numColumns);
		}

		for (int i = 0; i < numColumns; i++) {
			final String key = i + "," + String.valueOf(numRows - 1);
			viewConfigs.remove(key);
			viewButtons[numRows - 1][i].dispose();
			viewButtons[numRows - 1][i] = null;
		}
		viewButtons = newButtons;
		numRows--;
		viewTableGroup.layout();
	}

	private void addRows(final int rows) {
		final int oldRows = viewButtons.length;
		final Label[][] newButtons = new Label[numRows + rows][numColumns];   

		for (int j = 0; j < oldRows; j++) {
			for (int i = 0; i < numColumns; i++) {
				newButtons [j][i] = viewButtons[j][i];
			}
		}
		for (int j = oldRows; j < oldRows + rows; j++) {
			for (int i = 0; i < numColumns; i++) {
				final Label item = new Label(viewButtonComp, SWT.NONE);
				item.setData("row", j);
				item.setData("col", i);
				new SlotButtonDropListener(item);
				new SlotButtonDragListener(item);
				newButtons[j][i] = item;
				item.setImage(fillImage);

				/*
				 * Colorization of new rows matches colorization of new columns
				 */
				item.setBackground(white);
				item.setForeground(black);
			}
		}
		viewButtons = newButtons;
		numRows = numRows + rows;
		viewTableGroup.layout();
	}

	private void addColumns(final int colCount) {
		viewButtonComp.setLayout(new GridLayout(numColumns + colCount, false));

		final int oldColumns = numColumns;
		final Label[][] newButtons = new Label[numRows][oldColumns + colCount];      

		for (int j = 0; j < numRows; j++) {

			for (int i = 0; i < oldColumns + colCount; i++) {
				newButtons[j][i] = new Label(viewButtonComp, SWT.NONE);
				newButtons[j][i].setData("row", j);
				newButtons[j][i].setData("col", i);
				new SlotButtonDropListener(newButtons[j][i]);
				new SlotButtonDragListener(newButtons[j][i]);
				if (i < oldColumns) {
					newButtons[j][i].setImage(viewButtons[j][i].getImage());
					viewButtons[j][i].dispose();
					viewButtons[j][i] = null;
				} else {
					newButtons[j][i].setImage(fillImage);
				}
				newButtons[j][i].setBackground(white);
				newButtons[j][i].setForeground(black);
			}
		}
		viewButtons = newButtons;

		viewTableGroup.layout();
		numColumns = numColumns + colCount;
		assignViewsToSlots();
	}

	private void fillSlotTable() {

		viewButtonComp.setLayout(new GridLayout(numColumns, false));
		viewButtons = new Label[numRows][numColumns];

		for (int j = 0; j < numRows; j++) {
			for (int i = 0; i < numColumns; i++) {
				final Label item = new Label(viewButtonComp, SWT.NONE);
				item.setData("row", j);
				item.setData("col", i);
				new SlotButtonDropListener(item);
				new SlotButtonDragListener(item);
				item.setImage(fillImage);

				item.setBackground(white);
				item.setForeground(black);
				viewButtons[j][i] = item;  
			}
		}
		viewTableGroup.layout();
	}

	private void assignViewsToSlots() {
		if (viewButtonComp != null && !viewButtonComp.isDisposed()) {
			for (int i = 0; i < numRows; i++) {
				for (int j = 0; j < numColumns; j++) {
					final String key = String.valueOf(j) + "," + String.valueOf(i);
					final IViewConfiguration vc = viewConfigs.get(key);
					if (vc != null) {
						final Label item = viewButtons[i][j];                  						
						item.setImage(getImageForType(vc.getViewType()));
					}
				}
			}
		}
	}
	private Image getImageForType(final ViewType type) {
		final String typeStr = type.getValueAsString();
		for (int i = 0; i < viewTypes.length; i++) {
			if (typeStr.equalsIgnoreCase(viewTypes[i])) {
				return images[i];
			}
		}
		return null;
	}


	/**
	 * LabeledViewImage is a GUI widget that contains an image with a text label
	 * below it.
	 *
	 */
	private static class LabeledViewImage extends Composite {
		Label imageLabel;

		/**
		 * Creates a LabeledView image with the given icon, name, parent, and number.
		 * @param image the image icon for the view
		 * @param viewName the name of the view
		 * @param parent the parent Composite for the LabeledViewImage
		 * @param imageNum the number of the labelled view image within the grid view
		 * selector
		 */
		public LabeledViewImage(final Image image, String viewName, final Composite parent, final int imageNum) {
			super(parent, SWT.NONE);
			setLayout(new RowLayout(SWT.VERTICAL));
			imageLabel = new Label(this, SWT.NONE);
			imageLabel.setText(String.valueOf(imageNum));
			imageLabel.setImage(image);
			imageLabel.setToolTipText(viewName);
			final Label viewLabel = new Label(this, SWT.WRAP);
			if (viewName.indexOf(' ') == -1) {
				viewName = viewName + "\n"; 
			} else {
				viewName = viewName.replace(' ', '\n');
			}
			viewLabel.setText(viewName);
			final ChillFont font = new ChillFont();
			font.setSize(ChillFont.FontSize.SMALL);
			viewLabel.setFont(ChillFontCreator.getFont(font));
			viewLabel.setBounds(viewLabel.getBounds().x, viewLabel.getBounds().y, 25, 50);
			viewLabel.addDisposeListener(new DisposeListener() {

				@Override
				public void widgetDisposed(final DisposeEvent arg0) {
					viewLabel.getFont().dispose();
					viewLabel.setFont(null);
				}
				
			});
		}
	}

	/**
	 * This class listens for drop events into the grid layout table.
	 */
	private class SlotButtonDropListener implements DropTargetListener {

		private final TextTransfer textTransfer;
		private final Label target;

		/**
		 * Creates a drop listener for the given label.
		 * @param theLabel the label to create the drop listener for
		 */
		public SlotButtonDropListener(final Label theLabel) {
			final int operations = DND.DROP_MOVE;
			final DropTarget dropTarget = new DropTarget(theLabel, operations);
			textTransfer = TextTransfer.getInstance();
			final Transfer[] types = new Transfer[] { textTransfer };
			dropTarget.setTransfer(types);
			target = theLabel;
			dropTarget.addDropListener(this);
		}

		/**
	     * {@inheritDoc}
		 * @see org.eclipse.swt.dnd.DropTargetListener#dragEnter(org.eclipse.swt.dnd.DropTargetEvent)
		 */
		@Override
		public void dragEnter(final DropTargetEvent event) {
			try {
				if (event.detail == DND.DROP_DEFAULT) {
					if ((event.operations & DND.DROP_MOVE) != 0) {
						event.detail = DND.DROP_MOVE;
					} else {
						event.detail = DND.DROP_NONE;
					}
				}
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}

		/**
	     * {@inheritDoc}
		 * @see org.eclipse.swt.dnd.DropTargetListener#dragOver(org.eclipse.swt.dnd.DropTargetEvent)
		 */
		@Override
		public void dragOver(final DropTargetEvent event) {
		}

		/**
	     * {@inheritDoc}
		 * @see org.eclipse.swt.dnd.DropTargetListener#dragOperationChanged(org.eclipse.swt.dnd.DropTargetEvent)
		 */
		@Override
		public void dragOperationChanged(final DropTargetEvent event) {
			try {
				if (event.detail == DND.DROP_DEFAULT) {
					if ((event.operations & DND.DROP_MOVE) != 0) {
						event.detail = DND.DROP_MOVE;
					} else {
						event.detail = DND.DROP_NONE;
					}
				}
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}

		/**
	     * {@inheritDoc}
		 * @see org.eclipse.swt.dnd.DropTargetListener#dragLeave(org.eclipse.swt.dnd.DropTargetEvent)
		 */
		@Override
		public void dragLeave(final DropTargetEvent event) {
		}

		/**
	     * {@inheritDoc}
		 * @see org.eclipse.swt.dnd.DropTargetListener#dropAccept(org.eclipse.swt.dnd.DropTargetEvent)
		 */
		@Override
		public void dropAccept(final DropTargetEvent event) {
		}

		/**
	     * {@inheritDoc}
		 * @see org.eclipse.swt.dnd.DropTargetListener#drop(org.eclipse.swt.dnd.DropTargetEvent)
		 */
		@Override
		public void drop(final DropTargetEvent event) {
			try {
				if (textTransfer
						.isSupportedType(event.currentDataType)) {
					final String text = (String) event.data;
					if (text.equals("")) {
						return;
					}
					if (text.indexOf(",") == -1) { 
						final int row = (Integer)target.getData("row");
						final int col = (Integer)target.getData("col");
						final int viewNum = Integer.parseInt(text);
						final ViewType type = new ViewType(viewTypes[viewNum]);
						IViewConfiguration vc = null;
						if (type.equals(ViewType.FIXED_LAYOUT)) {
							final ViewBrowserShell browser = new ViewBrowserShell(appContext, prefShell);
							browser.open();

							while (!browser.getShell().isDisposed()) {
								if (!browser.getShell().getDisplay().readAndDispatch())
								{
									browser.getShell().getDisplay().sleep();
								}
							}
							final boolean cancel = browser.wasCanceled();
							if (cancel) {
								return;
							}
							vc = browser.getSelectedViewConfig();
							if(vc != null) {
								target.setImage(findImage(vc.getViewType()));
							}
						} else {
							vc = ViewFactory.createViewConfig(appContext, type);
							target.setImage(images[viewNum]);
						}
						if(vc != null) {
							vc.setDisplayViewTitle(false);
							final String key = String.valueOf(col) + "," + String.valueOf(row);
							viewConfigs.put(key, vc);
						}
					} else {
						final IViewConfiguration vc = viewConfigs.remove(text);
						final String[] rowcol = text.split(",");
						final int col = Integer.parseInt(rowcol[0]);
						final int row = Integer.parseInt(rowcol[1]);
						final Label oldItem = viewButtons[row][col];
						target.setImage(oldItem.getImage());
						final String key = target.getData("col") + "," + target.getData("row");
						viewConfigs.put(key, vc);
						oldItem.setImage(fillImage);
						removeButton.setFocus();
					}
				}
			} catch (final Exception e) {
				e.printStackTrace();
			} 
		}
	}

	private Image findImage(final ViewType type) {
		int index = 0;
		for (final String typeStr : viewTypes) {
			if (typeStr.equals(type.getValueAsString())) {
				return images[index];
			}
			index++;
		}
		return null;
	}

	/**
	 * This class listens for drags into the grid layout table.
	 *
	 */
	private static class SlotButtonDragListener implements DragSourceListener {

		private final Label target;

		/**
		 * Creates a drag listener.
		 * @param theLabel the view label to create this listener for
		 */
		public SlotButtonDragListener(final Label theLabel) {
			final int operations = DND.DROP_MOVE | DND.DROP_COPY;
			final DragSource source = new DragSource(theLabel, operations);
			final Transfer[] types = new Transfer[] { TextTransfer.getInstance() };
			source.setTransfer(types);
			target = theLabel;
			source.addDragListener(this);
		}    

		/**
	     * {@inheritDoc}
		 * @see org.eclipse.swt.dnd.DragSourceListener#dragStart(org.eclipse.swt.dnd.DragSourceEvent)
		 */
		@Override
		public void dragStart(final DragSourceEvent event) {
		}

		/**
	     * {@inheritDoc}
		 * @see org.eclipse.swt.dnd.DragSourceListener#dragSetData(org.eclipse.swt.dnd.DragSourceEvent)
		 */
		@Override
		public void dragSetData(final DragSourceEvent event) {
			try {
				if (TextTransfer.getInstance().isSupportedType(
						event.dataType)) {
					event.data = target.getData("col") + "," + target.getData("row");
				}
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}

		/**
	     * {@inheritDoc}
		 * @see org.eclipse.swt.dnd.DragSourceListener#dragFinished(org.eclipse.swt.dnd.DragSourceEvent)
		 */
		@Override
		public void dragFinished(final DragSourceEvent event) {
		}
	}

}
