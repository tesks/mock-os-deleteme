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
package jpl.gds.monitor.guiapp.gui;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.springframework.context.ApplicationContext;

import jpl.gds.monitor.guiapp.common.ViewReferenceListener;
import jpl.gds.perspective.config.PerspectiveProperties;
import jpl.gds.perspective.config.ViewType;
import jpl.gds.perspective.view.IViewConfiguration;
import jpl.gds.perspective.view.ViewReference;
import jpl.gds.perspective.view.ViewScanner;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.swt.ChillShell;
import jpl.gds.shared.swt.SWTUtilities;

/**
 * ViewBrowserShell presents a tree of known pre-defined views for selection by
 * the user. It can be used in three modes: by a window manager, in which case
 * it will add selected views as new windows, but a tab manager, in which case
 * it will add selected views as new tabs to an existing window, and as neither,
 * in which case it simply exits after a view configuration is selected, and
 * allows the caller to get the selected configuration via an accessor method.
 */
public class ViewBrowserShell implements ChillShell, ViewReferenceListener {
	/**
	 * Window padding height in pixels
	 */
	public static final int WINDOW_PAD_HEIGHT = 23;
	
	/**
	 * Window padding height in pixels
	 */
	public static final int WINDOW_PAD_WIDTH = 5;

	private final static String TITLE1 = "View Launcher";
	private final static String TITLE2 = "View Selector";

	private static final String WINDOW_IMAGE = "jpl/gds/monitor/gui/application_xp.gif";
	private static final String VIEW_IMAGE = "jpl/gds/monitor/gui/page.gif";
	private static final String CHECK_IMAGE = "jpl/gds/monitor/gui/flag_green.gif";

	private Shell mainShell;
	private boolean wasCancelled;
	private final Display parentDisplay;
	private final Shell parent;
	private Tree viewTree;
	private final ViewScanner viewScanner;
	private final boolean singleViewOnly;
	private final boolean forTabbedView;
	private final boolean forNewWindow;
	private TabularViewShell tabShell;
	private Button applyButton;
	private ViewReference currentReference;
	private IViewConfiguration selectedViewConfig;
	private Text searchText;

	private final ApplicationContext appContext;

	/**
	 * Creates a new ViewBrowser for use by a WindowManager.
	 * 
	 * @param appContext the current application context
	 * @param parent the parent shell
	 */
	public ViewBrowserShell(final ApplicationContext appContext, final Display parent) {
		this.appContext = appContext;
        viewScanner = new ViewScanner(appContext.getBean(PerspectiveProperties.class),
                                      appContext.getBean(SseContextFlag.class));
		parentDisplay = parent;
		this.parent = null;
		singleViewOnly = false;
		forTabbedView = false;
		forNewWindow = true;
		createControls();
	}

	/**
	 * Creates a new ViewBrowser for use by a TabularViewShell.
	 * 
	 * @param appContext the current application context
	 * @param parent the parent shell
	 * @param tabShell the TabularViewShell to add new views to
	 */
	public ViewBrowserShell(final ApplicationContext appContext, final Shell parent, final TabularViewShell tabShell) {
		this.appContext = appContext;
        viewScanner = new ViewScanner(appContext.getBean(PerspectiveProperties.class),
                                      appContext.getBean(SseContextFlag.class));
		this.parent = parent;
		parentDisplay = parent.getDisplay();
		this.tabShell = tabShell;
		singleViewOnly = true;
		forTabbedView = true;
		forNewWindow = false;
		createControls();
	}

	/**
	 * Creates a new ViewBrowser shell for the purpose of selecting a single
	 * view
	 * @param appContext the current application context
	 * @param parent the parent shell
	 */
	public ViewBrowserShell(final ApplicationContext appContext, final Shell parent) {
		this.appContext = appContext;
        viewScanner = new ViewScanner(appContext.getBean(PerspectiveProperties.class),
                                      appContext.getBean(SseContextFlag.class));
		this.parent = parent;
		parentDisplay = parent.getDisplay();
		singleViewOnly = true;
		forNewWindow = false;
		forTabbedView = false;
		createControls();
	}

	private void createControls() {
		if (parent == null) {
			mainShell = new Shell(parentDisplay, SWT.SHELL_TRIM);
		} else {
			mainShell = new Shell(parent, SWT.SHELL_TRIM);
		}
		final FormLayout fl = new FormLayout();
		mainShell.setLayout(fl);
		mainShell.setText(singleViewOnly ? TITLE2 : TITLE1);
		
		
		final Label searchLabel = new Label(mainShell, SWT.NONE);
		searchLabel.setText("Search:");
		final FormData labelFd = new FormData();
		labelFd.top = new FormAttachment(0, 5);
		labelFd.left = new FormAttachment(0, 5);
		searchLabel.setLayoutData(labelFd);

		searchText = new Text(mainShell, SWT.BORDER);
		final FormData textFd = SWTUtilities.getFormData(searchText, 1, 35);
		textFd.left = new FormAttachment(searchLabel);
		textFd.top = new FormAttachment(searchLabel, 0, SWT.CENTER);
		textFd.right = new FormAttachment(100, -5);
		searchText.setLayoutData(textFd);

		searchText.addTraverseListener(new TraverseListener() {
			@Override
            public void keyTraversed(final TraverseEvent e) {
				if (e.detail == SWT.TRAVERSE_RETURN) {
					e.doit = false;
					e.detail = SWT.TRAVERSE_NONE; 
				}
				final String text = searchText.getText();

				scrollToFirstMatchingTreeNode(text);

			}
		});

		searchText.addModifyListener(new ModifyListener() {
			@Override
            public void modifyText(final ModifyEvent arg0) {
				try {
					if (searchText.getText().length() < 1) {
						viewTree.deselectAll();
						applyButton.setEnabled(false);

					} else {
						scrollToFirstMatchingTreeNode(searchText.getText());
					}
				} catch (final Exception e) {
					e.printStackTrace();
				}
			}
		});

		viewTree = new Tree(mainShell, SWT.BORDER | SWT.SINGLE);
		final FormData treeFd = new FormData();
		treeFd.top = new FormAttachment(searchText);
		treeFd.left = new FormAttachment(0, 5);
		treeFd.right = new FormAttachment(100, -5);
		treeFd.bottom = new FormAttachment(85, 5);
		
		viewTree.setLayoutData(treeFd);

		final TreeItemSelectionListener treeListener = new TreeItemSelectionListener();
		viewTree.addListener(SWT.Selection, treeListener);
		viewTree.addListener(SWT.DefaultSelection, treeListener);

		populateTree();
		
		final Composite stuff = new Composite(mainShell, SWT.NONE);
		final RowLayout rl = new RowLayout(SWT.HORIZONTAL);
		stuff.setLayout(rl);
		
		final FormData stufffd = new FormData();
		stufffd.top = new FormAttachment(viewTree);
		stufffd.left = new FormAttachment(0, 5);
		stufffd.right = new FormAttachment(100);
		stuff.setLayoutData(stufffd);

		final Image viewImage = SWTUtilities.createImage(parentDisplay, VIEW_IMAGE);
		final Image checkImage = SWTUtilities.createImage(parentDisplay, CHECK_IMAGE);
		
		final Label legendLabel1 = new Label(stuff, SWT.NONE);
		legendLabel1.setImage(checkImage);
		
		final Label legendLabel1a = new Label(stuff, SWT.NONE);
		legendLabel1a.setText("= View is open");
		
		final Label legendLabel2 = new Label(stuff, SWT.NONE);
	    legendLabel2.setImage(viewImage);
	    
		final Label legendLabel2a = new Label(stuff, SWT.NONE);
		legendLabel2a.setText("= View is not open");
			
		final Composite composite = new Composite(mainShell, SWT.NONE);
		final GridLayout gl = new GridLayout(3, false);

		composite.setLayout(gl);
		final FormData formData8 = new FormData();
		formData8.top = new FormAttachment(stuff, 10);
		formData8.right = new FormAttachment(100);
		formData8.bottom = new FormAttachment(100, 0);
		composite.setLayoutData(formData8);

		applyButton = new Button(composite, SWT.PUSH);
		if (forTabbedView) {
			applyButton.setText("Add Tab");
		} else if (forNewWindow) {
			applyButton.setText("Open");
		} else {
			applyButton.setText("Select");
		}
		applyButton.setEnabled(false);

		mainShell.setDefaultButton(applyButton);
		GridData gd = new GridData();
		gd.horizontalAlignment = SWT.FILL;
		applyButton.setLayoutData(gd);

		final Button refreshButton = new Button(composite, SWT.PUSH);
		refreshButton.setText("Refresh");
		gd = new GridData();
		gd.horizontalAlignment = SWT.FILL;
		refreshButton.setLayoutData(gd);
		
		final Button cancelButton = new Button(composite, SWT.PUSH);
		if (forNewWindow || forTabbedView) {
			cancelButton.setText("Close");
		} else {
			cancelButton.setText("Cancel");
		}

		applyButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				try {
					loadView(currentReference);
					if (!forNewWindow && !forTabbedView) {
						wasCancelled = false;
						mainShell.close();
					}
				} catch (final Exception eE) {
					eE.printStackTrace();
				}
			}
		});

		refreshButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				try {
					populateTree();
				} catch (final Exception eE) {
					eE.printStackTrace();
				}
			}
		});

		cancelButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				try {
					selectedViewConfig = null;
					wasCancelled = true;
					mainShell.close();
				} catch (final Exception eE) {
					eE.printStackTrace();
				}
			}
		});

		mainShell.addDisposeListener(new DisposeListener() {

			@Override
            public void widgetDisposed(final DisposeEvent arg0) {
				MonitorViewReferences.getInstance().removeViewReferenceListener(
						ViewBrowserShell.this);
			}
		});

		MonitorViewReferences.getInstance().addViewReferenceListener(this);
		mainShell.pack();
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
     * {@inheritDoc}
	 * @see jpl.gds.shared.swt.ChillShell#getTitle()
	 */
	@Override
    public String getTitle() {
		return singleViewOnly ? TITLE2 : TITLE1;
	}

	/**
     * {@inheritDoc}
	 * @see jpl.gds.shared.swt.ChillShell#open()
	 */
	@Override
    public void open() {
		mainShell.open();
	}

	/**
     * {@inheritDoc}
	 * @see jpl.gds.shared.swt.ChillShell#wasCanceled()
	 */
	@Override
    public boolean wasCanceled() {
		return wasCancelled;
	}

	private void scrollToFirstMatchingTreeNode(final String text) {
		final TreeItem[] rootItems = viewTree.getItems();

		for (final TreeItem rootItem : rootItems) {

			final TreeItem[] items = rootItem.getItems();

			for (final TreeItem item : items) {
				if (item.getText().startsWith(text)) {
					viewTree.setSelection(item);
					viewTree.setTopItem(item);
					applyButton.setEnabled(true);
					return;
				}
			}
			applyButton.setEnabled(false);
		}
	}

	private void populateTree() {
		final TreeItem[] selectedItems = viewTree.getSelection();
		ViewReference selectedReference = null;
		if (selectedItems != null && selectedItems.length > 0) {
			selectedReference = (ViewReference) selectedItems[0].getData();
		}

		viewTree.removeAll();
		viewScanner.scanViews();

		final Image windowImage = SWTUtilities.createImage(parentDisplay,
				WINDOW_IMAGE);
		final Image viewImage = SWTUtilities.createImage(parentDisplay, VIEW_IMAGE);
		final Image checkImage = SWTUtilities.createImage(parentDisplay, CHECK_IMAGE);

		final TreeItem viewRootItem = new TreeItem(viewTree, SWT.NULL);
		viewRootItem.setText("Standalone Views");

		TreeItem windowRootItem = null;
		if (!singleViewOnly) {
			windowRootItem = new TreeItem(viewTree, SWT.NULL);
			windowRootItem.setText("Tabbed Windows");
		}

		final List<ViewReference> viewsOfType = viewScanner.getViewList();
		if (viewsOfType == null || viewsOfType.isEmpty()) {
			return;
		}

		TreeItem selectedItem = null;

		for (final ViewReference ref : viewsOfType) {
			TreeItem item = null;

			final ViewType type = ref.getType();

			if (singleViewOnly && type.equals(ViewType.MESSAGE_TAB)) {
				continue;
			}
			if (type.equals(ViewType.MESSAGE_TAB)) {
				item = new TreeItem(windowRootItem, SWT.NULL);
			} else {
				item = new TreeItem(viewRootItem, SWT.NULL);
			}
			item.setData(ref);
			item.setText(ref.getName());
			if (MonitorViewReferences.getInstance().isViewReferenceLoaded(ref)) {
				item.setImage(checkImage);
			} else if (ref.getType().isStandaloneWindow()) {
				item.setImage(windowImage);
			} else {
				item.setImage(viewImage);
			}

			if (selectedReference != null
					&& ref.compareTo(selectedReference) == 0) {
				selectedItem = item;
			}
		}

		viewRootItem.setExpanded(true);
		if (windowRootItem != null) {
			windowRootItem.setExpanded(true);
		}

		if (selectedItem != null) {
			viewTree.setSelection(selectedItem);
		}
	}

	private void loadView(final ViewReference ref) {
	    final ViewUtility vu = new ViewUtility(appContext);
		final IViewConfiguration vc = vu.loadViewConfiguration(ref, singleViewOnly, mainShell.getDisplay());
		if (vc == null) {
			SWTUtilities.showErrorDialog(mainShell, "Error Loading View",
			"The view could not be loaded due to an error");
			return;
		}

		selectedViewConfig = vc;

		if (forTabbedView) {
			vu.loadViewAsTab(ref, mainShell, tabShell);

		} else if (forNewWindow) {
			vu.loadView(ref, mainShell);
		}
	}

	/**
	 * Gets the selected view configuration after the browser window is
	 * dismissed. If wasCancelled() returns true, this method will return null
	 * 
	 * @return selected view configuration
	 */
	public IViewConfiguration getSelectedViewConfig() {
		return selectedViewConfig;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.guiapp.common.ViewReferenceListener#referencesChanged()
	 */
	@Override
    public void referencesChanged() {
		populateTree();
	}

	/**
	 * View Launches selection listener. Enables/disables buttons based on 
	 * which view is currently selected
	 *
	 */
	private class TreeItemSelectionListener implements Listener {
		@Override
        public void handleEvent(final Event e) {
			try {
				if (e.type == SWT.Selection) {
					if (viewTree.getSelectionCount() == 0) {
						applyButton.setEnabled(false);
						currentReference = null;
						return;
					} else {
						final TreeItem[] selection = viewTree.getSelection();
						if (selection.length == 0
								|| selection[0].getData() == null) {
							applyButton.setEnabled(false);
							currentReference = null;
							return;
						}
						currentReference = (ViewReference) selection[0]
						                                             .getData();
						applyButton.setEnabled(true);
					}
				} else if (e.type == SWT.DefaultSelection) {
					if (currentReference != null) {
						loadView(currentReference);
						if (!forNewWindow && !forTabbedView) {
							wasCancelled = false;
							mainShell.close();
						}
					}
				}
			} catch (final Exception eE) {
				TraceManager

				.getDefaultTracer()
				.error(
				"tree listener failed to catch exception in ViewEditorShell");
				eE.printStackTrace();
			}
		}
	}

}
