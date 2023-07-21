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
/**
 * File: ExportViewShell.java
 */
package jpl.gds.monitor.guiapp.gui;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.springframework.context.ApplicationContext;

import jpl.gds.perspective.ApplicationConfiguration;
import jpl.gds.perspective.DisplayConfiguration;
import jpl.gds.perspective.ExportableConfiguration;
import jpl.gds.perspective.config.ViewType;
import jpl.gds.perspective.view.IViewConfiguration;
import jpl.gds.perspective.view.IViewConfigurationContainer;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.reflect.ReflectionToolkit;
import jpl.gds.shared.swt.ChillShell;
import jpl.gds.shared.swt.SWTUtilities;


/**
 * PerspectiveEditorShell is used to edit the current perspective (actually, only
 * one application configuration in a perspective). It presents a tree view
 * of all the windows and views in the perspective. Windows and views can be editing, removed,
 * exported, or imported.
 *
 */
public class PerspectiveEditorShell implements ChillShell {
	/**
	 * Perspective editor shell title
	 */
	public static final String TITLE = "Perspective Editor";

	private static final String DISPLAY_IMAGE = "jpl/gds/monitor/gui/images.gif";
	private static final String WINDOW_IMAGE = "jpl/gds/monitor/gui/application_xp.gif";
	private static final String TABBED_WINDOW_IMAGE = "jpl/gds/monitor/gui/tabs_16.png";
	private static final String GRID_IMAGE = "jpl/gds/monitor/gui/application_view_tile.gif";
	private static final String REFERENCE_IMAGE = "jpl/gds/monitor/gui/redo.png";
	private static final String VIEW_IMAGE = "jpl/gds/monitor/gui/page.gif";

	private boolean reload = false;
	private Shell mainShell;
	private boolean wasCancelled = true;
	private final Shell parent;
	private Tree viewTree;
	private ApplicationConfiguration origConfig;
	private ApplicationConfiguration config;
	private TreeItemSelectionListener treeListener;
    private boolean firstSaveInstance = true;
	private boolean changesMade = false;
	private MenuItem removeButton;
	private MenuItem importButton;
	private MenuItem exportButton;
	private MenuItem editButton;
	private final SWTUtilities util = new SWTUtilities();

	private final ApplicationContext appContext;

	/**
	 * Creates a new PerspectiveEditorShell.
	 * @param parent the parent shell for this widget
	 * @param appConfig the ApplicationConfiguration to edit
	 * @throws IOException if a temporary perspective cannot be created on the file system
	 */
	public PerspectiveEditorShell(final ApplicationContext appContext, final Shell parent, final ApplicationConfiguration appConfig) throws IOException {
		this.appContext = appContext;
		this.parent = parent;
		loadTempConfig(appConfig);
		createControls();
		mainShell.setSize(600, 500);
	}

	/**
	 * Gets the flag indicating whether the current perspective should be restarted.
	 * @return true if the perspective should be reloaded after this shell is dismissed
	 */
	public boolean isReload() {
		return reload;
	}

	private void loadTempConfig(final ApplicationConfiguration appConfig) throws IOException {
		final String tempPath = GdsSystemProperties.getUserConfigDir() + "/TempPerspective";
		origConfig = appConfig;
		final String origPath = appConfig.getConfigPath();
		try {
			appConfig.setConfigPath(tempPath);
			appConfig.save();
			config = new ApplicationConfiguration(appContext);
			config.setConfigPath(tempPath);
			config.load(appConfig.getConfigFilename());           
		} finally {
			origConfig.setConfigPath(origPath);    
		}
	}

	private void createControls() {
		mainShell = new Shell(parent, SWT.SHELL_TRIM);
		mainShell.setLayout(new FormLayout());
		viewTree = new Tree(mainShell, SWT.BORDER | SWT.SINGLE);
		final FormData treeFd = new FormData();
		treeFd.top = new FormAttachment(0,5);
		treeFd.left = new FormAttachment(0,5);
		treeFd.right = new FormAttachment(100,-5);
		viewTree.setLayoutData(treeFd);  
		treeListener = new TreeItemSelectionListener();
		populateTree();
		viewTree.addListener(SWT.Selection, treeListener);

		final Composite composite = new Composite(mainShell, SWT.NONE);
		final GridLayout rl = new GridLayout(2, true);
		composite.setLayout(rl);
		final FormData formData8 = new FormData();
		formData8.top = new FormAttachment(viewTree);
		formData8.right = new FormAttachment(100);
		formData8.bottom = new FormAttachment(100);
		composite.setLayoutData(formData8);

		final Button applyButton = new Button(composite, SWT.PUSH);
		applyButton.setText("Ok");
		mainShell.setDefaultButton(applyButton);
		final GridData gd = new GridData();
		gd.horizontalAlignment = SWT.FILL;
		applyButton.setLayoutData(gd);

		final Button cancelButton = new Button(composite, SWT.PUSH);
		cancelButton.setText("Cancel");

		treeFd.bottom = new FormAttachment(100, -70);

		applyButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				try {
					wasCancelled = false;
					boolean close = true;
					if (changesMade) {
						boolean yes = SWTUtilities.showConfirmDialog(mainShell, "Save Inquiry", 
						"Changes will be saved to your current perspective. Continue and save?");
						if (yes) {
							try {
								config.setConfigPath(origConfig.getConfigPath());
								config.save();
								close = true;
								yes = SWTUtilities.showConfirmDialog(mainShell, "Save Inquiry", 
										"You must restart your perspective to see these changes.\n" +
										"Do you want to restart your current perspective at this time?\n" +
								"You will lose the contents of your current windows.");
								if (yes) {
									reload = true;
								} else {
									reload = false;
								}
							} catch (final IOException ex) {
								SWTUtilities.showErrorDialog(mainShell, "Save Failed", "Could not save configuration to " + config.getConfigPath());
								reload = false;
								close = false;
							}
						} else {
							close = false;
						}
					}

					if (close) {
						mainShell.close();
					}
				} catch ( final Exception eE ) {
					eE.printStackTrace();
				}
			}
		});
		cancelButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				try {
					wasCancelled = true;
					mainShell.close();
				} catch ( final Exception eE) {
					eE.printStackTrace();
				}
			}
		});

		final Menu actionMenu = new Menu(viewTree);
		viewTree.setMenu(actionMenu);
		importButton = new MenuItem(actionMenu, SWT.PUSH);
		importButton.setText("Import...");
		importButton.setEnabled(false);

		exportButton = new MenuItem(actionMenu, SWT.PUSH);
		exportButton.setText("Export...");
		exportButton.setEnabled(false);

		removeButton = new MenuItem(actionMenu, SWT.PUSH);
		removeButton.setText("Remove");
		removeButton.setEnabled(false);

		editButton = new MenuItem(actionMenu, SWT.PUSH);
		editButton.setText("Edit...");
		editButton.setEnabled(false);


		editButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				try{
					if (viewTree.getSelectionCount() == 0) {
						return;
					}
					final TreeItem[] selection = viewTree.getSelection();
					final IViewConfiguration itemConfig = (IViewConfiguration)selection[0].getData();
					final Class<?> c = itemConfig.getViewPreferencesClass();
					if (c == null) {
						SWTUtilities.showErrorDialog(mainShell, "Edit Failed", "Could not create view preferences window");
					}

					final ViewPreferencesShell shell = (ViewPreferencesShell)ReflectionToolkit.createObject(c, 
					        new Class[] {ApplicationContext.class,  Shell.class}, 
					        new Object[] {appContext, parent});
					shell.setValuesFromViewConfiguration(itemConfig);
					shell.getShell().addDisposeListener(new DisposeListener() {
						@Override
						public void widgetDisposed(final DisposeEvent event) {
							if (!shell.wasCanceled()) {
								shell.getValuesIntoViewConfiguration(itemConfig);
								populateTree();
								changesMade = true;
							}
						}
					});
					shell.open();
				} catch (final Exception ex) {
					ex.printStackTrace();
					SWTUtilities.showErrorDialog(mainShell, "Edit Failed", "Could not create view preferences window");
				}

			}
		});
		removeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				try {
					if (viewTree.getSelectionCount() == 0) {
						return;
					}
					final boolean yes = SWTUtilities.showConfirmDialog(mainShell, "Removal Confirmation", 
					"Are you sure you want to remove this view from your perspective?");
					if (yes) {
						final TreeItem[] selection = viewTree.getSelection();
						TreeItem parent = selection[0].getParentItem();
						removeFromParent(selection[0]);
						selection[0].dispose();
						selection[0] = null;
						final Object parentConfig = parent.getData();
						if (parentConfig instanceof IViewConfiguration) {
							if (((IViewConfiguration)parentConfig).getViewType().equals(ViewType.SINGLE_VIEW_WINDOW)) {
								removeFromParent(parent);
								parent.dispose();
								parent = null;
							}
						}
						changesMade = true;
					}
				} catch ( final Exception eE ){
					eE.printStackTrace();
				}
			}
		});
		exportButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				try {
					if (viewTree.getSelectionCount() == 0) {
						return;
					}
					final TreeItem[] selection = viewTree.getSelection();
					if (selection.length == 0) {
						return;
					}
					final Object itemConfig = selection[0].getData();
					String filename = null;
					String path = null;
					path = config.getConfigPath();
					if (itemConfig instanceof DisplayConfiguration) {
						filename = ((DisplayConfiguration)itemConfig).getConfigFile();
					} else {
						filename = makeFilename(((IViewConfiguration)itemConfig).getViewName()) + ".xml";
					}
					String outputPath = null;
					if (firstSaveInstance) {
						outputPath = util.displayStickyFileSaver(mainShell, "PerspectiveEditorShell", path, filename);
						firstSaveInstance = false;
					} else {
						outputPath = util.displayStickyFileSaver(mainShell, "PerspectiveEditorShell", null, filename);
					}
					if (outputPath != null) {
						if (itemConfig instanceof ExportableConfiguration) {
							final ExportableConfiguration exporter = (ExportableConfiguration)itemConfig;
							final boolean ok = exporter.exportToPath(outputPath);
							if (ok) {
								SWTUtilities.showMessageDialog(mainShell, "Configuration Saved", "Configuration was saved to " + outputPath);
							} else {
								SWTUtilities.showErrorDialog(mainShell, "Save Failed", "Could not save configuration to " + outputPath);
							}
						}
					}
				} catch ( final Exception eE ) {
					eE.printStackTrace();
				}
			}
		});

		importButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				try {
					if (viewTree.getSelectionCount() == 0) {
						return;
					}
					final TreeItem[] selection = viewTree.getSelection();
					if (selection.length == 0) {
						return;
					}
					final Object itemConfig = selection[0].getData();
					if (itemConfig instanceof IViewConfiguration) {
						if (((IViewConfiguration)itemConfig).isReference()) {
							((IViewConfiguration)itemConfig).setViewReference(null);
							SWTUtilities.showMessageDialog(mainShell, "View Reference Resolved", 
							"The referenced file has been imported and is now part of the perspective");
							populateTree();
							changesMade = true;
							return;
						}
					}
					final String inputPath = util.displayStickyFileChooser(false, mainShell,"PerspectiveEditorShell");
					if (inputPath != null) {
						final List<IViewConfiguration> viewList = IViewConfiguration.load(appContext, inputPath);
						if (viewList == null) {
							SWTUtilities.showErrorDialog(mainShell, "Load Failed", "Could not load configuration from " + inputPath);
							return;
						}
						if (itemConfig instanceof IViewConfigurationContainer) {
							Iterator<IViewConfiguration> it = viewList.iterator();
							final IViewConfigurationContainer containerConfig = (IViewConfigurationContainer)itemConfig;
							while (it.hasNext()) {
								final IViewConfiguration view = it.next();
								final boolean isWindow = view.getViewType().isStandaloneWindow();
								if (isWindow && !containerConfig.isWindowContainer()) {
									SWTUtilities.showErrorDialog(mainShell, "Load Failed", "Cannot load window configurations under a window configuration");
									return;
								} else if (!isWindow && containerConfig.isWindowContainer()) {
									SWTUtilities.showErrorDialog(mainShell, "Load Failed", "Can only load window configurations under a display");
									return;
								}
							}
							it = viewList.iterator();
							while (it.hasNext()) {
								final IViewConfiguration view = it.next();
								containerConfig.addViewConfiguration(view);
							}
						}
						populateTree();
						changesMade = true;
					}
				} catch ( final Exception eE ) {
					TraceManager.getDefaultTracer().error( "importButton Listener failed to catch exception in ViewEditorShell" );

					eE.printStackTrace();
				}
			}
		});
	}

	private void removeFromParent(final TreeItem item) {
		final TreeItem parentItem = item.getParentItem();
		final Object parentConfig = parentItem.getData();
		final IViewConfiguration vc = (IViewConfiguration)item.getData();
		//vc.clearViewReferences();
		if (parentConfig instanceof IViewConfigurationContainer) {
			((IViewConfigurationContainer)parentConfig).removeViewConfiguration(vc);
		} 
	}

	private String makeFilename(final String viewName) {
		final StringBuffer result = new StringBuffer(viewName);
		for (int i= 0; i < result.length(); i++) {
			final char c = result.charAt(i);
			if (!Character.isLetterOrDigit(c) && c != '_' && c != '-') {
				result.setCharAt(i, '_');
			}
		}
		return result.toString();
	}

	private void populateTree() {
		viewTree.removeAll();
		final Map<String, DisplayConfiguration> displayMap = config.getDisplayMap();
		final Set<String> set = displayMap.keySet();
		final Iterator<String> it = set.iterator();
		while (it.hasNext()) {
			final TreeItem item = new TreeItem(viewTree, SWT.NULL);
			item.addListener(SWT.Selection, treeListener);
			final DisplayConfiguration display = displayMap.get(it.next());
			item.setData(display);
			item.setText((display.getName().equals("") ? display.getType().getFancyDisplayName() : display.getName()) + " Display");
			final Image displayImage = SWTUtilities.createImage(parent.getDisplay(), DISPLAY_IMAGE);
			item.setImage(displayImage);
			final List<IViewConfiguration> views = display.getViewConfigs();
			addViewsToItem(item, views);
			item.setExpanded(true);
		}
		final TreeItem[] items = viewTree.getItems();
		for (int i = 0; i < items.length; i++) {
			items[i].setExpanded(true);
		}        
	}

	private void addViewsToItem(final TreeItem item, final List<IViewConfiguration> viewList) {
		if (viewList == null) {
			return;
		}
		final Image windowImage = SWTUtilities.createImage(parent.getDisplay(), WINDOW_IMAGE);
		final Image tabbedWindowImage = SWTUtilities.createImage(parent.getDisplay(), TABBED_WINDOW_IMAGE);
		final Image gridImage = SWTUtilities.createImage(parent.getDisplay(), GRID_IMAGE);
		final Image viewImage = SWTUtilities.createImage(parent.getDisplay(), VIEW_IMAGE);
		final Image refImage = SWTUtilities.createImage(parent.getDisplay(), REFERENCE_IMAGE);

		final Iterator<IViewConfiguration> viewIt = viewList.iterator();
		while (viewIt.hasNext()) {
			final IViewConfiguration view = viewIt.next();
			final TreeItem viewItem = new TreeItem(item, SWT.NULL);
			viewItem.setData(view);
			if (view.isReference()) {
				String text = "Reference to ";
				if (view.getViewType().equals(ViewType.MESSAGE_TAB) || view.getViewType().equals(ViewType.SINGLE_VIEW_WINDOW)) {
					text = text + "Window " + view.getViewName();
				} else if (view.getViewType().equals(ViewType.CUSTOM_GRID)) {
					text = text + "Grid " + view.getViewName();
				} else {
					text = text + "View " + view.getViewName();
				}
				viewItem.setText(text);
				viewItem.setImage(refImage);
			} else if (view.getViewType().equals(ViewType.MESSAGE_TAB)) {
				viewItem.setText("Tabbed Window " + view.getViewName());
				viewItem.setImage(tabbedWindowImage);
			} else if (view.getViewType().equals(ViewType.SINGLE_VIEW_WINDOW)) {
				viewItem.setText("Simple Window " + view.getViewName());
				viewItem.setImage(windowImage);
			} else if (view.getViewType().equals(ViewType.CUSTOM_GRID)) {
				viewItem.setText("Grid " + view.getViewName());
				viewItem.setImage(gridImage);
			} else {
				viewItem.setText("View " + view.getViewName()); 
				viewItem.setImage(viewImage);
			}
			if (view instanceof IViewConfigurationContainer) {
				addViewsToItem(viewItem, ((IViewConfigurationContainer)view).getViews());
			}
			viewItem.setExpanded(true);
		}
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
		return TITLE;
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

	/**
	 * Perspective editor listener. Enables/disables right click menu options 
	 * based on which item was right-clicked
	 *
	 */
	private class TreeItemSelectionListener implements Listener {
		@Override
		public void handleEvent(final Event e) {
			try {
				if (viewTree.getSelectionCount() == 0) {
					removeButton.setEnabled(false);
					importButton.setEnabled(false);
					exportButton.setEnabled(false);
				} else {
					final TreeItem[] selection = viewTree.getSelection();
					if (selection.length == 0) {
						return;
					}
					final Object itemConfig = selection[0].getData();
					if (itemConfig instanceof IViewConfiguration) {
						if (((IViewConfiguration)itemConfig).isReference()) {
							removeButton.setEnabled(true);
							editButton.setEnabled(false);
							exportButton.setEnabled(false);
							importButton.setEnabled(true);                           
						} else if (itemConfig instanceof IViewConfigurationContainer) {
							final IViewConfigurationContainer viewConfig = (IViewConfigurationContainer)itemConfig;
							removeButton.setEnabled(viewConfig.isRemovable());
							importButton.setEnabled(viewConfig.isImportViews());
							exportButton.setEnabled(itemConfig instanceof ExportableConfiguration);
							editButton.setEnabled(itemConfig instanceof IViewConfiguration);
						} else {
							removeButton.setEnabled(true);
							importButton.setEnabled(false);
							exportButton.setEnabled(true);
							editButton.setEnabled(true);
						}
					} else {
						importButton.setEnabled(itemConfig instanceof IViewConfigurationContainer);
						exportButton.setEnabled(itemConfig instanceof ExportableConfiguration);
						editButton.setEnabled(itemConfig instanceof IViewConfiguration);
					}
				}
			} catch ( final Exception eE ) {
				TraceManager.getDefaultTracer().error( "importButton Listener failed to catch exception in ViewEditorShell" );

				eE.printStackTrace();
			}
		}     
	}
}
