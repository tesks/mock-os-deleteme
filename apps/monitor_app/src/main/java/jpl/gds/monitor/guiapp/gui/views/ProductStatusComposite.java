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
package jpl.gds.monitor.guiapp.gui.views;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.springframework.context.ApplicationContext;

import jpl.gds.message.api.MessageUtility;
import jpl.gds.monitor.config.MonitorGuiProperties;
import jpl.gds.monitor.guiapp.MonitorTimers;
import jpl.gds.monitor.guiapp.common.GeneralFlushListener;
import jpl.gds.monitor.guiapp.common.GeneralMessageDistributor;
import jpl.gds.monitor.guiapp.common.GeneralMessageListener;
import jpl.gds.monitor.guiapp.gui.views.preferences.ProductStatusPreferencesShell;
import jpl.gds.monitor.guiapp.gui.views.support.CountShell;
import jpl.gds.monitor.guiapp.gui.views.support.ProductDecomThread;
import jpl.gds.monitor.perspective.view.ProductStatusViewConfiguration;
import jpl.gds.perspective.config.PerspectiveProperties;
import jpl.gds.perspective.config.ViewProperties;
import jpl.gds.perspective.config.ViewType;
import jpl.gds.perspective.view.IViewConfiguration;
import jpl.gds.perspective.view.View;
import jpl.gds.product.api.IProductMetadataProvider;
import jpl.gds.product.api.config.IProductPropertiesProvider;
import jpl.gds.product.api.message.IPartReceivedMessage;
import jpl.gds.product.api.message.IPartialProductMessage;
import jpl.gds.product.api.message.IProductAssembledMessage;
import jpl.gds.product.api.message.IProductStartedMessage;
import jpl.gds.product.api.message.ProductMessageType;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.IMessageType;
import jpl.gds.shared.swt.ChillColorCreator;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.shared.swt.TextViewShell;
import jpl.gds.shared.swt.types.ChillColor;
import jpl.gds.shared.template.TemplateException;
import jpl.gds.shared.thread.SleepUtilities;
import jpl.gds.shared.time.AccurateDateTime;

/**
 * 
 * ProductStatusComposite is a widget that displays a table of product status
 * information constructed from the information in product-related messages. It
 * is considered a monitor view.
 * 
 */
public class ProductStatusComposite extends AbstractTableViewComposite implements GeneralMessageListener,
GeneralFlushListener, View {

	/**
	 * This enumeration tracks the status of a product entry in the status table.
	 *
	 */
	private enum StatusType {
		NO_STATUS("Unknown"),
		IN_PROGRESS_STATUS("In Progress"),
		COMPLETE_STATUS("Complete"),
		PARTIAL_STATUS("Partial");

		private String display;
  
		private StatusType(final String displayName) {
			display = displayName;
		}

		public String getDisplayString() {
			return display;
		}

	}
	
    private static final String DEFAULT_ROWS_PROPERTY = "defaultRows";

	/**
	 * Product status composite title
	 */
	public static final String TITLE = "Product Status";
	private static Tracer log;
	private static volatile Color blue;
	private static volatile Color white;
	private static volatile Color green;
	private static volatile Color black;
	private static final int FLUSH_PARTIAL = 1;
	private static final int FLUSH_COMPLETE = 2;
	private static final int FLUSH_ALL = 3;

	private TextViewShell textShell;
	private boolean paused;
	private int productFlushInterval;
	private Timer productFlushTimer;
	private ProductStatusPreferencesShell prefShell;
	private MenuItem viewMenuItem;
	private MenuItem decomMenuItem;
	private MenuItem decomToFileMenuItem;
	private MenuItem launchProdViewerMenuItem;
	private MenuItem launchDpoViewerMenuItem;
	private final Semaphore queueSemaphore;
	private final ConcurrentLinkedQueue<QueuedMessage> messageQueue = new ConcurrentLinkedQueue<QueuedMessage>();
	private final int batchSize;
	private final ProductStatusViewConfiguration productViewConfig;
	private final HashMap<String, TableItem> transactionMap = new HashMap<String, TableItem>();
	private final Semaphore syncFlag = new Semaphore(1);
	private final SWTUtilities util = new SWTUtilities();
	private String storageSubdir;
	private MenuItem copyMenuItem;
	private final int maxRows;
    private final AtomicBoolean changed = new AtomicBoolean(false);
    private final long sortInterval;
    private final IProductPropertiesProvider productConfig;
	private final ApplicationContext appContext;
    
	/**
	 * Creates an instance of ProductStatusComposite.
	 * @param appContext the current application context
	 * 
	 * @param config the ProductStatusViewConfiguration object containing
	 *            display settings
	 */
	public ProductStatusComposite(final ApplicationContext appContext, final IViewConfiguration config) {
		super(config);
		this.appContext = appContext;
		initColors();
		productViewConfig = (ProductStatusViewConfiguration) config;
		productFlushInterval = productViewConfig.getFlushInterval() * 60000;
		final ViewProperties vp = appContext.getBean(PerspectiveProperties.class).getViewProperties(ViewType.PRODUCT);
		maxRows = vp.getIntegerDefault(DEFAULT_ROWS_PROPERTY, 3000);
		sortInterval = vp.getIntegerDefault("sortInterval", 10000);
		batchSize = appContext.getBean(MonitorGuiProperties.class).getListFlushBatchSize();
		queueSemaphore = new Semaphore(appContext.getBean(MonitorGuiProperties.class).getListQueueScaleFactor() * batchSize);
		tableDef = productViewConfig
		.getTable(ProductStatusViewConfiguration.PRODUCT_TABLE_NAME);
		appContext.getBean(MonitorTimers.class).addGeneralFlushListener(this);
		appContext.getBean(GeneralMessageDistributor.class).replaceDataListeners(
                this,
                new IMessageType[] { ProductMessageType.ProductAssembled, ProductMessageType.ProductStarted,
                        ProductMessageType.PartialProduct, ProductMessageType.ProductPart });
		productConfig = appContext.getBean( IProductPropertiesProvider.class);
        log = TraceManager.getDefaultTracer(appContext);
	}

	private static void initColors() {
		if (blue == null) {
			blue = ChillColorCreator.getColor(new ChillColor(
					ChillColor.ColorName.BLUE));
			white = ChillColorCreator.getColor(new ChillColor(
					ChillColor.ColorName.WHITE));
			green = ChillColorCreator.getColor(new ChillColor(
					ChillColor.ColorName.GREEN));
			black = ChillColorCreator.getColor(new ChillColor(
					ChillColor.ColorName.BLACK));
		}
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.guiapp.gui.views.AbstractTableViewComposite#init(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void init(final Composite parent) {
		super.init(parent);
		startProductFlushTimer();
		setSortColumn();
	}

	private void startProductFlushTimer() {
		if (productFlushTimer != null) {
			productFlushTimer.cancel();
			productFlushTimer = null;
		}
		long interval = productFlushInterval;
		if (productFlushInterval == 0 || sortInterval < productFlushInterval) {
			interval = sortInterval;
		}
		
		if (interval == 0) {
			return;
		}
		
		productFlushTimer = new Timer();
		productFlushTimer.scheduleAtFixedRate(new TimerTask() {

			@Override
			public void run()
			{
				SWTUtilities.safeAsyncExec(ProductStatusComposite.this.parent.getDisplay(),
						"startProductFlushTimer",
						new Runnable()
				{
					
					@Override
					public String toString() {
						return "ProductStatusComposite.startProductFlushTimer.Runnable";
					}
					
					@Override
					public void run()
					{
						try
						{
							if (ProductStatusComposite.this.mainComposite.isDisposed()) {
								return;
							}

							if (productFlushInterval != 0) {
								clearMessages(FLUSH_PARTIAL, true);
								clearMessages(FLUSH_COMPLETE, true);
							}
							if (changed.compareAndSet(true, false)) {
								sortIfNeeded(true, false, true);
								changed.set(false);
							}
						}
						catch (final Exception e)
						{
							e.printStackTrace();
							return;
						}
					}
				});
			}
		}, interval, interval);
	}

	private void stopFlushTimer() {
		if (productFlushTimer != null) {
			productFlushTimer.cancel();
		}
	}

	/**
	 * Creates the controls and composites for this tab display.
	 */
	@Override
	protected void createGui() {
		mainComposite = new Composite(parent, SWT.NONE);
		final FormLayout shellLayout = new FormLayout();
		shellLayout.spacing = 5;
		mainComposite.setLayout(shellLayout);

		table = new Table(mainComposite, SWT.FULL_SELECTION
				| SWT.MULTI);
		table.setHeaderVisible(tableDef.isShowColumnHeader());

		final FormData formData2 = new FormData();
		formData2.left = new FormAttachment(0);
		formData2.top = new FormAttachment(0);
		formData2.right = new FormAttachment(100);
		formData2.bottom = new FormAttachment(100);
		table.setLayoutData(formData2);

		final Listener sortListener = new SortListener();

		final int numColumns = tableDef.getColumnCount();
		tableColumns = new TableColumn[numColumns];
		for (int i = 0; i < numColumns; i++) {
			if (tableDef.isColumnEnabled(i)) {
				tableColumns[i] = new TableColumn(table,
						SWT.NONE);
				tableColumns[i].setText(tableDef.getOfficialColumnName(i));
				tableColumns[i].setWidth(tableDef.getColumnWidth(i));
				tableColumns[i].addListener(SWT.Selection, sortListener);
				tableColumns[i].setMoveable(true);
				if (tableDef.isSortColumn(i) && tableDef.isSortAllowed()) {
					table.setSortColumn(tableColumns[i]);
					tableColumns[i].setImage(tableDef.isSortAscending() ? upImage : downImage);
				}
			} else {
				tableColumns[i] = null;
			}
		}

		table.setColumnOrder(tableDef.getColumnOrder());

		updateTableFontAndColors();

		createMenuItems();

		table.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(
					final org.eclipse.swt.events.SelectionEvent e) {
				try {
					final int[] i = ProductStatusComposite.this.table
					.getSelectionIndices();
					if (i != null && i.length != 0) {
						copyMenuItem.setEnabled(true);
					} else {
						copyMenuItem.setEnabled(false);
					}
					if (i != null && i.length == 1) {
						viewMenuItem.setEnabled(true);
						final TableItem item = ProductStatusComposite.this.table.getItem(i[0]);
						final StatusType status = (StatusType)item.getData("status");
						if (status != null && (status == StatusType.COMPLETE_STATUS || status == StatusType.PARTIAL_STATUS)) {
							decomMenuItem.setEnabled(true);
							decomToFileMenuItem.setEnabled(true);
							launchProdViewerMenuItem.setEnabled(true);
							launchDpoViewerMenuItem.setEnabled(productConfig.isProcessDpos());
						} else {
							decomMenuItem.setEnabled(false);
							decomToFileMenuItem.setEnabled(false);
							launchProdViewerMenuItem.setEnabled(false);
							launchDpoViewerMenuItem.setEnabled(false);
						}
					} else {
						viewMenuItem.setEnabled(false);
						decomMenuItem.setEnabled(false);
						decomToFileMenuItem.setEnabled(false);
						launchProdViewerMenuItem.setEnabled(false);
						launchDpoViewerMenuItem.setEnabled(false);
					}
				} catch (final Exception e1) {
					log.error("Error handling product table selection event " + e1.toString());
					e1.printStackTrace();
				}
			}
		});


		mainComposite.addDisposeListener(new DisposeListener() {

			@Override
			public void widgetDisposed(final DisposeEvent event) {
				try {
					appContext.getBean(GeneralMessageDistributor.class).removeDataListener(
							ProductStatusComposite.this);
					appContext.getBean(MonitorTimers.class).removeGeneralFlushListener(
							ProductStatusComposite.this);
					stopFlushTimer();
					if (prefShell != null) {
						prefShell.getShell().dispose();
						prefShell = null;
					}
				} catch (final Exception e) {
					log.error("Error handling product main shell disposal " + e.toString());
					e.printStackTrace();
				}
			}
		});
	}

	private void createMenuItems() {
		final Menu viewMenu = new Menu(mainComposite);
		viewMenuItem = new MenuItem(viewMenu, SWT.PUSH);
		viewMenuItem.setText("View Metadata...");
		table.setMenu(viewMenu);
		viewMenuItem.setEnabled(false);
		decomMenuItem = new MenuItem(viewMenu, SWT.PUSH);
		decomMenuItem.setText("View Product as Text...");
		decomMenuItem.setEnabled(false);
		decomToFileMenuItem = new MenuItem(viewMenu, SWT.PUSH);
		decomToFileMenuItem.setText("Save Product as Text...");
		decomToFileMenuItem.setEnabled(false);
		launchProdViewerMenuItem = new MenuItem(viewMenu, SWT.PUSH);
		launchProdViewerMenuItem.setText("Launch Product Viewer...");
		launchProdViewerMenuItem.setEnabled(false);
		launchDpoViewerMenuItem = new MenuItem(viewMenu, SWT.PUSH);
		launchDpoViewerMenuItem.setText("Launch DPO Viewers...");
		launchDpoViewerMenuItem.setEnabled(false);
		new MenuItem(viewMenu, SWT.SEPARATOR);
		final MenuItem prefMenuItem = new MenuItem(viewMenu, SWT.PUSH);
		prefMenuItem.setText("Preferences...");
		new MenuItem(viewMenu, SWT.SEPARATOR);
		final MenuItem pauseMenuItem = new MenuItem(viewMenu, SWT.PUSH);
		pauseMenuItem.setText("Pause");
		final MenuItem resumeMenuItem = new MenuItem(viewMenu, SWT.PUSH);
		resumeMenuItem.setText("Resume");
		resumeMenuItem.setEnabled(false);
		new MenuItem(viewMenu, SWT.SEPARATOR);
		final MenuItem partialMenuItem = new MenuItem(viewMenu, SWT.PUSH);
		partialMenuItem.setText("Flush Partials");
		final MenuItem completeMenuItem = new MenuItem(viewMenu, SWT.PUSH);
		completeMenuItem.setText("Flush Complete");
		final MenuItem allMenuItem = new MenuItem(viewMenu, SWT.PUSH);
		allMenuItem.setText("Flush All");
		new MenuItem(viewMenu, SWT.SEPARATOR);
		copyMenuItem = new MenuItem(viewMenu, SWT.PUSH);
		copyMenuItem.setText("Copy");
		copyMenuItem.setEnabled(false);
		new MenuItem(viewMenu, SWT.SEPARATOR);
		final MenuItem viewCountMenuItem = new MenuItem(viewMenu, SWT.PUSH);
		viewCountMenuItem.setText("View Count");

		copyMenuItem.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(
					final org.eclipse.swt.events.SelectionEvent e) {
				try {
					final int[] indices = ProductStatusComposite.this.table
					.getSelectionIndices();
					if (indices == null || indices.length == 0) {
						return;
					}
					Arrays.sort(indices);
					Clipboard clipboard = new Clipboard(
							ProductStatusComposite.this.mainComposite.getDisplay());
					final StringBuffer plainText = new StringBuffer();
					for (int i = 0; i < indices.length; i++) {
						final TableItem item = ProductStatusComposite.this.table
						.getItem(indices[i]);
						for (int j = 0; j < ProductStatusComposite.this.table
						.getColumnCount(); j++) {
							plainText.append("\"" + item.getText(j) + "\"");
							if (j < ProductStatusComposite.this.table
									.getColumnCount() - 1) {
								plainText.append(",");
							}
						}
						plainText.append("\n");
					}
					final TextTransfer textTransfer = TextTransfer.getInstance();
					clipboard.setContents(new String[] {
							plainText.toString()
					}, new Transfer[] {
							textTransfer
					});
					clipboard.dispose();
					clipboard = null;
				} catch (final Exception e1) {
					log.error("Error handling copy menu item " + e1.toString());
					e1.printStackTrace();
				}
			}
		});

		pauseMenuItem.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(
					final org.eclipse.swt.events.SelectionEvent e) {
				try {
					pause();
					resumeMenuItem.setEnabled(true);
					pauseMenuItem.setEnabled(false);
				} catch (final RuntimeException e1) {
					log.error("Error handling pause menu item " + e1.toString());
					e1.printStackTrace();
				}
			}
		});

		resumeMenuItem.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(
					final org.eclipse.swt.events.SelectionEvent e) {
				try {
					resume();
					resumeMenuItem.setEnabled(false);
					pauseMenuItem.setEnabled(true);
				} catch (final Exception e1) {
					log.error("Error handling resume menu item " + e1.toString());
					e1.printStackTrace();
				}
			}
		});
		partialMenuItem.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(
					final org.eclipse.swt.events.SelectionEvent e) {
				try {
					clearMessages(FLUSH_PARTIAL, false);
				} catch (final RuntimeException e1) {
					log.error("Error handling flush partial menu item " + e1.toString());
					e1.printStackTrace();
				}
			}
		});

		completeMenuItem.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(
					final org.eclipse.swt.events.SelectionEvent e) {
				try {
					clearMessages(FLUSH_COMPLETE, false);
				} catch (final RuntimeException e1) {
					log.error("Error handling flush complete menu item " + e1.toString());
					e1.printStackTrace();
				}
			}
		});

		allMenuItem.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(
					final org.eclipse.swt.events.SelectionEvent e) {
				try {
					clearMessages(FLUSH_ALL, false);
				} catch (final RuntimeException e1) {
					log.error("Error handling flush all menu item " + e1.toString());
					e1.printStackTrace();
				}
			}
		});

		prefMenuItem.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(
					final org.eclipse.swt.events.SelectionEvent e) {
				try {
					if (prefShell == null) {
						// This kludge works around an SWT bug on Linux
						// in which column sizes are not remembered
						final TableColumn[] cols = ProductStatusComposite.this.table.getColumns();
						for (int i = 0; i < cols.length; i++) {
							cols[i].setWidth(cols[i].getWidth());
						}
						prefShell = new ProductStatusPreferencesShell(
								appContext, ProductStatusComposite.this.mainComposite
								.getShell());
						prefShell
						.setValuesFromViewConfiguration(productViewConfig);
						prefShell.getShell()
						.addDisposeListener(new DisposeListener() {

							@Override
							public void widgetDisposed(
									final DisposeEvent event) {
								try {
									if (!prefShell
											.wasCanceled()) {
										
										cancelOldSortColumn();

										prefShell
										.getValuesIntoViewConfiguration(productViewConfig);
										if (productViewConfig
												.getFlushInterval() * 60000 != productFlushInterval) {
											productFlushTimer
											.cancel();
											productFlushInterval = productViewConfig
											.getFlushInterval() * 60000;
											startProductFlushTimer();
										}
										
										updateTableFromConfig(ProductStatusViewConfiguration.PRODUCT_TABLE_NAME, prefShell.needColumnChange());									
									}
								} catch (final Exception e) {
									log.error("Error handling exit from preferences window " + e.toString());
									e.printStackTrace();
								} finally {
									prefShell = null;
									prefMenuItem.setEnabled(true);
								}
							}
						});
						prefMenuItem.setEnabled(false);
						prefShell.open();
					}
				} catch (final Exception e1) {
					log.error("Error handling Preferences window " + e.toString());
					e1.printStackTrace();
				}
			}
		});

		viewCountMenuItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				try {       				      			
					final CountShell tvs = new CountShell(table.getShell(), ProductStatusComposite.this, viewConfig.getViewName());
					tvs.open();
				} catch (final Exception ex) {
					ex.printStackTrace();
					log.error("Error in showing Product Status count" + ex.toString());
				}
			}
		});

		viewMenuItem.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(final SelectionEvent arg0) {
				try {
					final int i = ProductStatusComposite.this.table
					.getSelectionIndex();
					if (i != -1) {
						if (textShell == null
								|| textShell.getShell()
								.isDisposed()) {
							textShell = new TextViewShell(
							        ProductStatusComposite.this.mainComposite
                                  .getShell(), TraceManager.getDefaultTracer());
						}
						final TableItem it = ProductStatusComposite.this.table
						.getItem(i);
						if (it.isDisposed()) {
							return;
						}
						textShell
						.getShell()
						.setText(
								it
								.getText(ProductStatusComposite.this.tableDef
										.getActualIndex(ProductStatusComposite.this.tableDef
												.getColumnIndex(ProductStatusViewConfiguration.PRODUCT_FILENAME_COLUMN))));
						final jpl.gds.shared.message.IMessage m = (jpl.gds.shared.message.IMessage) it
						.getData("message");
						try {
							final String msgText = MessageUtility.getMessageText(m,
									"MetadataSum");
							textShell.setText(msgText);
						} catch (final TemplateException e) {
							textShell
							.setText("Unable to format product metadata.");
						}
						textShell.open();
					}
				} catch (final Exception e) {
					log.error("Error handling view menu item " + e.toString());
					e.printStackTrace();
				}
			}

			@Override
			public void widgetDefaultSelected(final SelectionEvent arg0) {
			}

		});

		decomToFileMenuItem.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(final SelectionEvent arg0) {
				try {
					final int i = ProductStatusComposite.this.table.getSelectionIndex();
					if (i != -1) {
						
						final TableItem it = ProductStatusComposite.this.table.getItem(i);
						if (it.isDisposed()) {
							return;
						}
						final String filename = (String)it.getData("filename");
						if (filename == null) {
							SWTUtilities.showWarningDialog(ProductStatusComposite.this.mainComposite.getShell(), "No Filename Available", 
									"The location of the product file cannot be determined. Please retry this operation.");
							return;
						}
						final String decomFilename = new File(filename).getName();
						String saveFilename = decomFilename.indexOf(".dat") == -1 ? decomFilename.replaceFirst(".pdat", ".txt") : decomFilename.replaceFirst(".dat", ".txt");
						saveFilename = util.displayStickyFileSaver(ProductStatusComposite.this.mainComposite.getShell(), 
								"ProductStatusComposite", null, saveFilename);
						if (saveFilename == null) {
							return;
						}
						final ProductDecomThread thread = new ProductDecomThread(appContext, ProductDecomThread.LaunchType.TEXT_VIEW_TO_FILE,
								filename, saveFilename, mainComposite.getShell());
						thread.startProgress();
						new Thread(thread).start();
					}
				} catch (final Exception e) {
					log.error("Error handling decom to file menu item " + e.toString());
					e.printStackTrace();
				}
			}

			@Override
			public void widgetDefaultSelected(final SelectionEvent arg0) {
			}
		});

		decomMenuItem.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(final SelectionEvent arg0) {
				try {
					final int i = ProductStatusComposite.this.table.getSelectionIndex();
					if (i != -1) {
						final TableItem it = ProductStatusComposite.this.table.getItem(i);
						if (it.isDisposed()) {
							return;
						}
						final String filename = (String)it.getData("filename");
						if (filename == null) {
							SWTUtilities.showWarningDialog(ProductStatusComposite.this.mainComposite.getShell(), "No Filename Available", 
									"The location of the product file cannot be determined. Please retry this operation.");
							return;
						}
						
						final ProductDecomThread thread = new ProductDecomThread(appContext, ProductDecomThread.LaunchType.TEXT_VIEW_TO_WINDOW,
								filename, null, mainComposite.getShell());
						thread.startProgress();
						new Thread(thread).start();
					}
				} catch (final Exception e) {
					log.error("Error handling decom menu item " + e.toString());
					e.printStackTrace();
				}
			}

			@Override
			public void widgetDefaultSelected(final SelectionEvent arg0) {
			}
		});

		launchProdViewerMenuItem.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(final SelectionEvent arg0) {
				try {
					final int i = ProductStatusComposite.this.table.getSelectionIndex();
					if (i != -1) {
						final TableItem it = ProductStatusComposite.this.table.getItem(i);
						if (it.isDisposed()) {
							return;
						}
						final String filename = (String)it.getData("filename");
						if (filename == null) {
							SWTUtilities.showWarningDialog(ProductStatusComposite.this.mainComposite.getShell(), "No Filename Available", 
							"The location of the product file cannot be determined. Please retry this operation.");
							return;
						}
						final ProductDecomThread thread = new ProductDecomThread(appContext, ProductDecomThread.LaunchType.LAUNCH_PRODUCT_VIEWER,
								filename, null, mainComposite.getShell());
						thread.startProgress();
						new Thread(thread).start();
					}
				} catch (final Exception e) {
					log.error("Error handling decom menu item " + e.toString());
					e.printStackTrace();
				}
			}
			@Override
			public void widgetDefaultSelected(final SelectionEvent arg0) {
			}
		});

		launchDpoViewerMenuItem.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(final SelectionEvent arg0) {
				try {
					final int i = ProductStatusComposite.this.table.getSelectionIndex();
					if (i != -1) {
						
						final TableItem it = ProductStatusComposite.this.table.getItem(i);
						if (it.isDisposed()) {
							return;
						}
						final String filename = (String)it.getData("filename");
						if (filename == null) {
							SWTUtilities.showWarningDialog(ProductStatusComposite.this.mainComposite.getShell(), "No Filename Available", 
							"The location of the product file cannot be determined. Please retry this operation.");
							return;
						}
						final ProductDecomThread thread = new ProductDecomThread(appContext, ProductDecomThread.LaunchType.LAUNCH_DPO_VIEWERS,
								filename, null, mainComposite.getShell());
						thread.startProgress();
						new Thread(thread).start();
					}
				} catch (final Exception e) {
					log.error("Error handling decom menu item " + e.toString());
					e.printStackTrace();
				}
			}


			@Override
			public void widgetDefaultSelected(final SelectionEvent arg0) {
			}
		});
	}


	/**
     * {@inheritDoc}
	 * @see jpl.gds.perspective.view.View#clearView()
	 */
	@Override
	public void clearView() {
		clearMessages(FLUSH_ALL, false);
	}

	/**
	 * Flushes old product entries from the table.
	 * @param flushType FLUSH_ALL, FLUSH_PARTIAL, or FLUSH_COMPLETE
	 * @param agedOnly true to flush only products that have aged past the flush interval
	 */
	private void clearMessages(final int flushType, final boolean agedOnly) {
		if (mainComposite.isDisposed()) {
			return;
		}
		if (flushType == FLUSH_ALL) {
			SleepUtilities.fullAcquire(syncFlag,
					log,
					"ProductStatusComposite.clearMessages Error acquiring");
			transactionMap.clear();
			super.clearView();
			syncFlag.release();
		} else {
			SleepUtilities.fullAcquire(syncFlag, log, "ProductStatusComposite.clearMessages Error acquiring");
			final TableItem[] items = table.getItems();
			for (int i = items.length - 1; i >= 0; i--) {
				if (items[i].isDisposed()) {
					continue;
				}
				final StatusType status = (StatusType)items[i].getData("status");

				if (status != null) {
					if ((flushType == FLUSH_PARTIAL && status == StatusType.PARTIAL_STATUS)
							|| (flushType == FLUSH_COMPLETE && status == StatusType.COMPLETE_STATUS)) {
						if (!agedOnly || isOverAge(items[i])) {
							transactionMap.remove(items[i].getData());
							removeEntry(items[i]);
						}
					}
				}
			}
			table.setTopIndex(table.getItemCount() - computeVisibleRowCount());
			syncFlag.release();
		}
	}

	/**
	 * Determines is a table entry is old enough to be removed.
	 * 
	 * @param item the table entry to check
	 * @return true if the item is overage
	 */
	private boolean isOverAge(final TableItem item) {
		final long entryTime = (Long) item.getData("time");
		final long currentTime = new AccurateDateTime().getTime();
		if (currentTime - entryTime > productFlushInterval) {
			return true;
		}
		return false;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.guiapp.common.GeneralMessageListener#messageReceived(jpl.gds.shared.message.IMessage[])
	 */
	@Override
	public void messageReceived(final IMessage[] messages) {

		try {
			for (int i = 0; i < messages.length; i++) {
				final QueuedMessage qm = new QueuedMessage(messages[i], messages[i].getType());

				SleepUtilities.fullAcquire(queueSemaphore, log, "ProductStatusComposite.messageReceived " +
						"Error acquiring: ");
				
				messageQueue.add(qm);
			}
			displayMessages(false);

		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Displays queued messages.
	 * 
	 * @param timedFlush true if this display was triggered by the display flush
	 *            timer
	 */
	private synchronized void displayMessages(final boolean timedFlush) {

		if (parent.getDisplay().isDisposed() || parent.isDisposed()) {
			return;
		}
		if (messageQueue.size() > batchSize
				|| (timedFlush && messageQueue.size() > 0)) {

			SWTUtilities.safeAsyncExec(
					parent.getDisplay(),
					"displayMessages",
					new Runnable()
					{
						
						@Override
						public String toString() {
							return "ProductStatusComposite.displayMessages.Runnable";
						}
						
						@Override
						public void run() {
							try {
								if (ProductStatusComposite.this.mainComposite.isDisposed()) {
									queueSemaphore.release(messageQueue.size());
									messageQueue.clear();
									return;
								}
								if (messageQueue.size() == 0) {
									return;
								}
								if (paused) {
									return;
								}
								int count = 0;
								while (count < batchSize
										&& messageQueue
										.size() > 0) {
									count++;
									final QueuedMessage qm = messageQueue
									.poll();
									int messagesProcessed = 1;
									try
									{
										SleepUtilities.fullAcquire(
												syncFlag,
												log,
												"ProductStatusComposite.displayMessages " +
												"Error acquiring");

										if (IMessageType.matches(qm.type,
										        ProductMessageType.ProductAssembled)) {
										    processProductAssembled(qm.message);
										} else if (IMessageType.matches(qm.type,
										        ProductMessageType.PartialProduct)) {
										    processPartialProduct(qm.message);
										} else if (IMessageType.matches(qm.type,
										        ProductMessageType.ProductStarted)) {
										    processProductStarted(qm.message);
										} else if (IMessageType.matches(qm.type,
										        ProductMessageType.ProductPart)) {
										    messagesProcessed = processPartReceived(qm.message);
										}

										queueSemaphore.release(messagesProcessed);

									} catch (final Exception e) {
										e.printStackTrace();
									} finally {
										syncFlag.release();
									}
								}

							} catch (final Exception e) {
								e.printStackTrace();
								return;
							}
						}
					});
		}
	}

	private StatusType getNewStatus(final StatusType newStatus, final StatusType oldStatus) {
		StatusType currentStatus = oldStatus;
		if (currentStatus == null) {
			currentStatus = StatusType.NO_STATUS;
		}

		// Once complete, a product cannot revert to any other status
		if (currentStatus == StatusType.COMPLETE_STATUS) {
			return null;
		}
		// If the status is the same, do nothing except reset the current
		// message attached to the record in the case where a new part may have arrived.
		if (oldStatus == newStatus) {
			return null;
		}

		switch (newStatus) {
		case NO_STATUS:
		case PARTIAL_STATUS:
		case COMPLETE_STATUS: {
			currentStatus = newStatus;
			break;
		}
		case IN_PROGRESS_STATUS: {
			if (currentStatus != StatusType.PARTIAL_STATUS && currentStatus != StatusType.COMPLETE_STATUS) {
				currentStatus = newStatus;
			} else {
				return null;
			}
			break;
		}
		default: {
			return null;
		}
		}
		return currentStatus;
	}

	private void updateEntryColors(final TableItem entry) {
		if (entry.isDisposed()) {
			return;
		}
		StatusType newStatus = (StatusType)entry.getData("status");
		if (newStatus == null) {
			newStatus = StatusType.NO_STATUS;
		}

		switch (newStatus) {
			case PARTIAL_STATUS:
				entry.setBackground(blue);
				entry.setForeground(white);
				break;
			case COMPLETE_STATUS:
				entry.setBackground(green);
				entry.setForeground(black);
				break;
			case IN_PROGRESS_STATUS:
				break;
			case NO_STATUS:
				break;
			default:
				break;
		}
		setColumn(entry, tableDef.getColumnIndex(ProductStatusViewConfiguration.PRODUCT_STATUS_COLUMN),
				newStatus.getDisplayString());
	}

	private void processProductStarted(
			final IMessage message) {
		final IProductStartedMessage startMessage = (IProductStartedMessage) message;
		final String transaction = startMessage.getTransactionId();
		TableItem newEntry = findTableEntry(transaction);
		boolean sortNeeded = false;

		if (newEntry == null) {

			final String apid = String.valueOf(startMessage.getApid());
			final String type = startMessage.getProductType();
			final String filename =  transaction;
			final int numParts = startMessage.getTotalParts();
			final String parts = numParts == 0 ? "Unk" : String.valueOf(numParts);
			final String partsRecv = "0";
			final String lastPart = "None";
			final String sclk = "Unknown";
			final String scet = "Unknown";
			final String sol = "";
			final String create = "";

			newEntry = createTableItem(new String[] {
					StatusType.IN_PROGRESS_STATUS.toString(),
					apid,
					type,
					sclk,
					scet,
					create,
					filename,
					parts,
					partsRecv,
					lastPart,
					sol});

			newEntry.setData(transaction);
			transactionMap.put(transaction.trim(), newEntry);
			newEntry.setData("message", startMessage);
			newEntry.setData("time", System.currentTimeMillis());
		} else {
			sortNeeded = true;
		}
		final StatusType newStatus = getNewStatus(StatusType.IN_PROGRESS_STATUS,
				(StatusType)newEntry.getData("status"));
		if (newStatus != null) {
			newEntry.setData("status", newStatus);
		}
		updateEntryColors(newEntry);
		if (sortNeeded) {
			changed.set(true);
		}
	}

	/**
     * {@inheritDoc}
	 * @see jpl.gds.monitor.guiapp.gui.views.AbstractTableViewComposite#createTableItem(java.lang.String[])
	 */
	@Override
	protected TableItem createTableItem(final String[] args) {
		int len = table.getItemCount();
	    while (len > maxRows) {
			long oldest = Long.MAX_VALUE;
	        TableItem oldestItem = null;
   			for (int i = 0; i < len; i++) {
				final TableItem item = table.getItem(i);
				if (item.isDisposed()) {
					continue;
				}
		        final long time = (Long)item.getData("time");
		        if (time < oldest) {
		    	   oldest = time;
		    	   oldestItem = item; 
		       }
			}
   			if (oldestItem != null) {
   				transactionMap.remove(oldestItem.getData());
   				removeEntry(oldestItem);
   			} else {
   				break;
   			}
   			len = table.getItemCount();
		}
		return super.createTableItem(args);
	}
	
	private void processProductAssembled(
			final IMessage message) {

		final IProductAssembledMessage assemMessage = (IProductAssembledMessage) message;
		final String transaction = assemMessage.getTransactionId();
		final IProductMetadataProvider md = assemMessage.getMetadata();
		TableItem newEntry = findTableEntry(transaction);

		boolean sortNeeded = false;
		if (newEntry == null) {

			final String apid = String.valueOf(md.getApid());
			final String type = md.getProductType();
			final String filename =  getUniquePathComponent(md.getFullPath());
			final String parts = String.valueOf(md.getTotalParts());
			final String partsRecv = String.valueOf(md.getTotalParts());
			final String lastPart = "None";
			final String sclk = md.getDvtString();
			final String scet = md.getScetStr();
			final String sol = md.getSolStr();
			final String create =  md.getProductCreationTimeStr();

			newEntry = createTableItem(new String[] { StatusType.COMPLETE_STATUS.toString(),
					apid, 
					type, 
					sclk, 
					scet, 
					create, 
					filename, 
					parts, 
					partsRecv, 
					lastPart, 
					sol});

			transactionMap.put(transaction.trim(), newEntry);
		} else {
			setColumn(
					newEntry,
					tableDef
					.getColumnIndex(ProductStatusViewConfiguration.PRODUCT_PARTS_COLUMN),
					String.valueOf(md.getTotalParts()));
			setColumn(
					newEntry,
					tableDef
					.getColumnIndex(ProductStatusViewConfiguration.PRODUCT_SCLK_COLUMN),
					md.getDvtString());
			setColumn(
					newEntry,
					tableDef
					.getColumnIndex(ProductStatusViewConfiguration.PRODUCT_SCET_COLUMN),
					md.getScetStr());
			setColumn(
					newEntry,
					tableDef
					.getColumnIndex(ProductStatusViewConfiguration.PRODUCT_LST_COLUMN),
					md.getSolStr());
			setColumn(
					newEntry,
					tableDef
					.getColumnIndex(ProductStatusViewConfiguration.PRODUCT_CREATE_TIME_COLUMN),
					md.getProductCreationTimeStr());
			setColumn(
					newEntry,
					tableDef
					.getColumnIndex(ProductStatusViewConfiguration.PRODUCT_FILENAME_COLUMN),
					getUniquePathComponent(md.getFullPath()));
			sortNeeded = true;
		}

		newEntry.setData("message", assemMessage);
		newEntry.setData("time", System.currentTimeMillis());

		if (storageSubdir == null) {
			storageSubdir = productConfig.getStorageSubdir();
		}
		newEntry.setData("filename", md.getFullPath());

		final StatusType newStatus = getNewStatus(StatusType.COMPLETE_STATUS,
				(StatusType)newEntry.getData("status"));
		if (newStatus != null) {
			newEntry.setData("status", newStatus);
		}
		updateEntryColors(newEntry);

		if (sortNeeded) {
			changed.set(true);
		}
	}

	private void processPartialProduct(
			final IMessage message) {

		final IPartialProductMessage partialMessage = (IPartialProductMessage) message;
		final String transaction = partialMessage.getTransactionId();
		TableItem newEntry = findTableEntry(transaction);
		final IProductMetadataProvider md = partialMessage.getMetadata();
		boolean sortNeeded = false;
		StatusType newStatus = null;

		if (newEntry == null) {
			final String apid = String.valueOf(md.getApid());
			final String type = md.getProductType();
			final String filename =  getUniquePathComponent(md.getFullPath());
			final String parts = String.valueOf(md.getTotalParts());
			final String partsRecv = "0";
			final String lastPart = "None";
			final String sclk = md.getDvtString();
			final String scet = md.getScetStr();
			final String sol = md.getSolStr();
			final String create = md.getProductCreationTimeStr();

			newEntry = createTableItem(new String[] {
					StatusType.PARTIAL_STATUS.toString(),
					apid,
					type,
					sclk,
					scet,
					create,
					filename,
					parts,
					partsRecv,
					lastPart,
					sol});

			newEntry.setData(transaction);
			transactionMap.put(transaction.trim(), newEntry);
			newEntry.setData("message", partialMessage);
			newEntry.setData("status", StatusType.PARTIAL_STATUS);
		} else {
			setColumn(
					newEntry,
					tableDef
					.getColumnIndex(ProductStatusViewConfiguration.PRODUCT_PARTS_COLUMN),
					String.valueOf(md.getTotalParts()));
			setColumn(
					newEntry,
					tableDef
					.getColumnIndex(ProductStatusViewConfiguration.PRODUCT_SCLK_COLUMN),
					md.getDvtString());
			setColumn(
					newEntry,
					tableDef
					.getColumnIndex(ProductStatusViewConfiguration.PRODUCT_SCET_COLUMN),
					md.getScetStr());
			setColumn(
					newEntry,
					tableDef
					.getColumnIndex(ProductStatusViewConfiguration.PRODUCT_LST_COLUMN),
					md.getSolStr());
			setColumn(
					newEntry,
					tableDef
					.getColumnIndex(ProductStatusViewConfiguration.PRODUCT_CREATE_TIME_COLUMN),
					md.getProductCreationTimeStr());
			final String currentFilename = (String)newEntry.getData("filename");
			if (currentFilename == null || currentFilename.endsWith(".pdat")) {
				setColumn(
						newEntry,
						tableDef
						.getColumnIndex(ProductStatusViewConfiguration.PRODUCT_FILENAME_COLUMN),
						getUniquePathComponent(md.getFullPath()));
			}

			final StatusType oldStatus = (StatusType)newEntry.getData("status");
			newStatus = getNewStatus(StatusType.PARTIAL_STATUS, (StatusType)newEntry.getData("status"));

			if ((newStatus == null || (newStatus == StatusType.PARTIAL_STATUS)) &&
					(oldStatus != StatusType.COMPLETE_STATUS)) 		// If parts are still coming in after status was set to complete, do not change metadata
			{
				newEntry.setData("message", partialMessage);
			}

			if (newStatus != null) {
				newEntry.setData("status", newStatus);
			}
			sortNeeded = true;
		}

		newEntry.setData("time", System.currentTimeMillis());

		if (storageSubdir == null) {
			storageSubdir = productConfig.getStorageSubdir();
		}
		final String currentFilename = (String)newEntry.getData("filename");
		if (currentFilename == null || currentFilename.endsWith(".pdat")) {
			newEntry.setData("filename", md.getFullPath());
		}
		updateEntryColors(newEntry);

        if (sortNeeded) {
        	changed.set(true);
        }
	}

	private int processPartReceived(final IMessage message) {

		IPartReceivedMessage partMessage = (IPartReceivedMessage) message;
		final String transaction = partMessage.getPart().getTransactionId();
		int messagesProcessed = 1;
		int partsAdded = partMessage.getPart().getPartLength() == 0 ? 0 : 1;

		QueuedMessage qm = messageQueue.peek();
		StatusType newStatus = null;

		while (qm != null) {
            if (qm.message.isType(ProductMessageType.ProductPart)) {
				final IPartReceivedMessage newMessage = (IPartReceivedMessage) qm.message;
				if (newMessage.getPart().getTransactionId().equals(transaction)) {
					partsAdded += newMessage.getPart().getPartLength() == 0 ? 0 : 1;
					messagesProcessed++;
					partMessage = (IPartReceivedMessage) messageQueue.poll().message;
					qm = messageQueue.peek();
				} else {
					break;
				}
			} else {
				break;
			}
		}

		if (messagesProcessed == 0) {
			return 0;
		}

		TableItem newEntry = findTableEntry(transaction);
		boolean sortNeeded = false;
		if (newEntry == null) {

			final String apid = String.valueOf(String.valueOf(partMessage.getPart().getApid()));
			final String type = partMessage.getPart().getMetadata().getProductType();
			final String filename = partMessage.getPart().getTransactionId();
			final String parts = String.valueOf(partMessage.getPart().getMetadata()
					.getTotalParts());
			final String partsRecv = String.valueOf(partsAdded);
			final String lastPart = String.valueOf(partMessage.getPart().getPartNumber());
			final String sclk =  partMessage.getPart().getMetadata().getDvtString();
			final String scet = partMessage.getPart().getMetadata().getScetStr();
			final String sol = partMessage.getPart().getMetadata().getSolStr();
			final String create = "";

			newEntry = createTableItem(new String[] {
					StatusType.IN_PROGRESS_STATUS.toString(),
					apid,
					type,
					sclk,
					scet,
					create,
					filename,
					parts,
					partsRecv,
					lastPart,
					sol});

			newEntry.setData(transaction);
			newEntry.setData("message", partMessage);
			newEntry.setData("status", StatusType.IN_PROGRESS_STATUS);
			transactionMap.put(transaction.trim(), newEntry);

		} else {
			final int recvIndex = tableDef
			.getActualIndex(tableDef
					.getColumnIndex(ProductStatusViewConfiguration.PRODUCT_PARTS_RECV_COLUMN));
			String partText = newEntry.getText(recvIndex);
			if (partText.equalsIgnoreCase("None") || partText.equals("")) {
				partText = "0";
			}
			final int curParts = Integer.parseInt(partText);
			if (newEntry.getData("status") == StatusType.COMPLETE_STATUS) {
				log.debug("Adding parts to ", transaction, " after completion: ", curParts, " + ", partsAdded, " = ", curParts + partsAdded);
			}
			setColumn(
					newEntry,
					tableDef
					.getColumnIndex(ProductStatusViewConfiguration.PRODUCT_PARTS_RECV_COLUMN),
					String.valueOf(curParts + partsAdded));
			setColumn(
					newEntry,
					tableDef
					.getColumnIndex(ProductStatusViewConfiguration.PRODUCT_SCLK_COLUMN),
					partMessage.getPart().getMetadata().getDvtString());
			setColumn(
					newEntry,
					tableDef
					.getColumnIndex(ProductStatusViewConfiguration.PRODUCT_SCET_COLUMN),
					partMessage.getPart().getMetadata().getScetStr());
			setColumn(
					newEntry,
					tableDef
					.getColumnIndex(ProductStatusViewConfiguration.PRODUCT_LST_COLUMN),
					partMessage.getPart().getMetadata().getSolStr());
			setColumn(
					newEntry,
					tableDef
					.getColumnIndex(ProductStatusViewConfiguration.PRODUCT_LAST_PART_COLUMN),
					String.valueOf(partMessage.getPart().getPartNumber()));

			final StatusType oldStatus = (StatusType)newEntry.getData("status");
			newStatus = getNewStatus(StatusType.IN_PROGRESS_STATUS, oldStatus);

			if ((newStatus == null || (newStatus == StatusType.IN_PROGRESS_STATUS)) &&
					(oldStatus != StatusType.COMPLETE_STATUS) &&	// If parts are still coming in after status was set to complete
					(oldStatus != StatusType.PARTIAL_STATUS))		// or partial, do not change the metadata
			{
				newEntry.setData("message", partMessage);
			}

			if (newStatus != null) {
				newEntry.setData("status", newStatus);
			}
			sortNeeded = true;
		}

		newEntry.setData("time", System.currentTimeMillis());
		updateEntryColors(newEntry);

		if (sortNeeded) {
			changed.set(true);
		}
		return messagesProcessed;
	}

	private TableItem findTableEntry(final String transaction) {
		TableItem result = transactionMap.get(transaction.trim());
		if (result != null && result.isDisposed()) {
			transactionMap.remove(transaction.trim());
			result = null;
		}
		return result;
	}

	private void pause() {
		paused = true;
	}

	private void resume() {
		paused = false;
	}

	/**
	 * Computes the visible row count in the table.
	 * 
	 * @return the row count
	 */
	private int computeVisibleRowCount() {
		final Rectangle r = table.getBounds();
		final int rowheight = table.getItemHeight();
		final int rows = (r.height - table.getHeaderHeight()) / rowheight;
		return rows;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.guiapp.common.GeneralFlushListener#flushTimerFired()
	 */
	@Override
	public void flushTimerFired() {
		displayMessages(true);
	}

	/**
	 * 
	 * QueuedMessage represents an internal message that has been queued for
	 * display.
	 *
	 */
	public static class QueuedMessage {

		jpl.gds.shared.message.IMessage message;
		IMessageType type;

		/**
		 * Queued message object constructor
		 * 
		 * @param messages message
		 * @param iMessageType product status type
		 */
		public QueuedMessage(final IMessage messages,
				final IMessageType iMessageType) {
			message = messages;
			type = iMessageType;
		}
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.perspective.view.View#getDefaultName()
	 */
	@Override
	public String getDefaultName() {
		return TITLE;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.guiapp.gui.views.AbstractTableViewComposite#updateViewConfig()
	 */
	@Override
	public void updateViewConfig() {
		productViewConfig.setFlushInterval(productFlushInterval / 60000);
		super.updateViewConfig();
	}
	
	/**
     * {@inheritDoc}
	 * @see jpl.gds.monitor.guiapp.gui.views.AbstractTableViewComposite#sortTableItems(int)
	 */
	@Override
	protected void sortTableItems(final int index) {
		if (!tableDef.isSortAllowed()) {
			return;
		}

		try
		{
			SleepUtilities.fullAcquire(
					syncFlag,
					log,
					"ProductStatusComposite.sortTableItems Error acquiring");
			super.sortTableItems(index);
		}
		finally
		{
			syncFlag.release();
		}
	}

	/**
     * {@inheritDoc}
	 * @see jpl.gds.monitor.guiapp.gui.views.AbstractTableViewComposite#replaceTableItems(org.eclipse.swt.widgets.TableItem[])
	 */
	@Override
	protected void replaceTableItems(final TableItem[] items) {

        final TableItemStuff[] entryData = new TableItemStuff[items.length];
        final int colCount = tableDef.getActualColumnCount();
        
		ArrayList<TableItem> newSelected = null;

		for (int i = 0; i < items.length; i++) {
			entryData[i] = new TableItemStuff(items[i], colCount);
		}
		final TableItem[] existingItems = table.getItems();
		
		for (int i = 0; i < items.length; i++) {

			final int oldIndex = table.indexOf(items[i]);
			transactionMap.remove(entryData[i].transaction);
			
			final boolean selected = table.isSelected(oldIndex);
			
			entryData[i].assignStuffToNewItem(existingItems[i]);
			
			transactionMap.put(entryData[i].transaction, existingItems[i]);
			
			if (selected) {
				if (newSelected == null) {
					newSelected = new ArrayList<TableItem>(1);
				}
				newSelected.add(existingItems[i]);
			}

		}

		if (newSelected != null) {
			final TableItem[] selectedItems = new TableItem[newSelected.size()];	
			newSelected.toArray(selectedItems);
			table.setSelection(selectedItems);
		}
	}

	private String getUniquePathComponent(final String filename) {
		if (filename == null) {
			return null;
		}
		final File f = new File(filename);
		final String name = f.getName();
		if (f.getParent() == null) {
			return name;
		} else {
			return f.getParentFile().getName() + File.separator + name;
		}
	}

	// Note that we go to great lengths to avoid sorting if it is not needed. This is because sorting is CPU hungry.
	private void sortIfNeeded(final boolean needed, final boolean isPart, final boolean isNewStatus) {
		if (!needed) {
			return;
		}
		final String col = tableDef.getSortColumn();

		if (col == null || !tableDef.isSortAllowed()) {
			return;
		}
		final int index = tableDef.getColumnIndex(tableDef.getSortColumn());
		sortTableItems(index);
	}
	
	/**
	 * This class is used to temporarily store table item data during sorts.
	 *
	 */
	private static class TableItemStuff {
		private final String transaction;
		private final String[] values;
		private final Object time;
		private final Object message;
		private final Object status;
		private final Object filename;
		private final Color background;
		private final Color foreground;
		
		/**
		 * Constructor.
		 * 
		 * @param item TableItem containing data to store
		 * @param colCount actual table column count
		 */
		public TableItemStuff(final TableItem item, final int colCount) {
			values = new String[colCount];
			for (int valIndex = 0; valIndex < values.length; valIndex++) {
				values[valIndex] = item.getText(valIndex);
			}

			transaction = (String) item.getData();
			time = item.getData("time");
			message = item.getData("message");
			status = item.getData("status");
			filename = item.getData("filename");
			background = item.getBackground();
			foreground = item.getForeground();
		}

		/**
		 * Attaches values of fields in this object to a new TableItem.
		 * @param item TableItem to update
		 */
		public void assignStuffToNewItem(final TableItem item) {
			item.setData(transaction);
			item.setData("message", message);
			item.setData("time", time);
			item.setData("status", status);
			item.setData("filename", filename);
			item.setText(values);
			item.setBackground(background);
			item.setForeground(foreground);
		}
	}
}
