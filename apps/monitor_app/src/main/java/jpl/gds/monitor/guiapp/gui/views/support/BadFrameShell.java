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
package jpl.gds.monitor.guiapp.gui.views.support;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apache.velocity.Template;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import jpl.gds.shared.formatting.SprintfFormat;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.message.MessageRegistry;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.swt.ChillShell;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.shared.template.MessageTemplateManager;
import jpl.gds.shared.template.MissionConfiguredTemplateManagerFactory;
import jpl.gds.shared.template.TemplateException;
import jpl.gds.shared.template.TemplateManager;
import jpl.gds.tm.service.api.TmServiceMessageType;
import jpl.gds.tm.service.api.frame.IFrameEventMessage;

/**
 * Shows list of bad frames for selected
 *
 */
public class BadFrameShell implements ChillShell {
	
	/**
	 * Window title for bad frames shell
	 */
	public static final String TITLE = "Bad Frames";

	private static String[] columns = {
		"DSSID",
		"VCID",
		"VCFC",
		"Type",
		"ERT",
		"Bitrate",
		"Bad Reason"
	};
	private static int[] widths = {
		60,
		60,
		60,
		100,
		120,
		60,
		150
	};

	private Shell tableShell;
	private final Shell parent;
	private boolean canceled;
	private Table table;
	private final SWTUtilities utils = new SWTUtilities();
	private List<IFrameEventMessage> messages;
	private Template saveTemplate;

	/**
     * Creates an instance of BadFrameShell.
     * 
     * @param parent
     *            the parent Shell
     * @param sseFlag
     *            the SSE context flag
     */
    public BadFrameShell(final Shell parent, final SseContextFlag sseFlag) {
		this.parent = parent;
		createControls();
		try {
            final MessageTemplateManager mgr = MissionConfiguredTemplateManagerFactory.getNewMessageTemplateManager(sseFlag);
			saveTemplate = mgr.getTemplateForStyle(MessageRegistry.getMessageConfig(TmServiceMessageType.BadTelemetryFrame),"badframecsv");
		} catch (final TemplateException e) {
		    TraceManager.getDefaultTracer().error("Unexpected error getting mesage template: " + e.toString(), e);
		}
	}

	/**
	 * Creates the GUI controls.
	 */
	protected void createControls() {
		tableShell = new Shell(parent, SWT.APPLICATION_MODAL
				| SWT.SHELL_TRIM);
		tableShell.setText(TITLE);

		final FormLayout shellLayout = new FormLayout();
		shellLayout.spacing = 10;
		shellLayout.marginHeight = 5;
		shellLayout.marginWidth = 5;
		tableShell.setLayout(shellLayout);

		table = new Table(tableShell, SWT.READ_ONLY | SWT.MULTI | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		createTableColumns();
		final FormData tableFd = new FormData();
		tableFd.top = new FormAttachment(0);
		tableFd.left = new FormAttachment(0);
		tableFd.right = new FormAttachment(100);
		tableFd.bottom = new FormAttachment(100);
		table.setLayoutData(tableFd);
		final Menu viewMenu = new Menu(table);
		final MenuItem copyMenuItem = new MenuItem(viewMenu, SWT.PUSH);
		copyMenuItem.setText("Copy");
		table.setMenu(viewMenu);

		copyMenuItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				try {
					final int[] indices = table.getSelectionIndices();
					if (indices == null || indices.length == 0) {
						return;
					}
					Arrays.sort(indices);
					final Clipboard clipboard = new Clipboard(table.getDisplay());
					final StringBuilder plainText = new StringBuilder();
					for (int i = 0; i < indices.length; i++) {
						final TableItem item = table.getItem(indices[i]);
						for (int j = 0; j < table.getColumnCount(); j++) {
							plainText.append("\"" + item.getText(j) + "\"");
							if (j < table.getColumnCount() - 1) {
								plainText.append(",");
							}
						}
						plainText.append("\n");
					}
					final TextTransfer textTransfer = TextTransfer.getInstance();
					clipboard.setContents(new String[]{plainText.toString()}, new Transfer[]{textTransfer});
					clipboard.dispose();
				} catch (final Exception ex) {
					TraceManager.getDefaultTracer().error("Error handling copy menu item " + ex.toString(), e);

				}
			}
		});

		table.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				try {
					final int[] i = table.getSelectionIndices();
					if (i != null && i.length != 0) {
						copyMenuItem.setEnabled(true);
					} else {
						copyMenuItem.setEnabled(false);
					}
				} catch (final Exception e1) {
					TraceManager.getDefaultTracer().error("Unable to hand table selection event " + e.toString(), e1);

				}
			}
		});

		final Composite composite = new Composite(tableShell, SWT.NONE);
		final GridLayout rl = new GridLayout(2, true);
		composite.setLayout(rl);
		final FormData formData8 = new FormData();
		formData8.right = new FormAttachment(100);
		formData8.bottom = new FormAttachment(100);
		composite.setLayoutData(formData8);

		formData8.top = new FormAttachment(table);

		final Button saveButton = new Button(composite, SWT.PUSH);
		saveButton.setText("Save As...");
		final GridData gd = new GridData();
		gd.horizontalAlignment = SWT.FILL;
		saveButton.setLayoutData(gd);

		final Button closeButton = new Button(composite, SWT.PUSH);
		closeButton.setText("Close");
		tableShell.setDefaultButton(closeButton);

		saveButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				try {
					if (saveTemplate == null) {
						SWTUtilities.showErrorDialog(parent, "Missing Template", 
						"Bad frame information cannot be saved because the template is undefined.");
						return;
					}
					if (messages == null || messages.isEmpty()) {
						SWTUtilities.showMessageDialog(parent, "No Bad Frames", 
						"There are no bad frames to save.");
						return;
					}
					final String filename = utils.displayStickyFileSaver(tableShell, "BadFrameShell", null, "BadFrames.csv");
					if (filename == null) {
						return;
					}
					try {
						final FileOutputStream fos = new FileOutputStream(filename);
						final HashMap<String, Object> properties = new HashMap<>();
						properties.put("header", true);
						String result = TemplateManager.createText(saveTemplate, properties);
						fos.write(result.getBytes());
						for (final IFrameEventMessage m : messages) {
							properties.clear();
							properties.put("body", true);
							m.setTemplateContext(properties);
							result = TemplateManager.createText(saveTemplate, properties);
							fos.write(result.getBytes());
							fos.write("\n".getBytes());
						}
						fos.close();
					} catch (final IOException ex) {
						SWTUtilities.showErrorDialog(parent, "Save Failed", 
								"There was an error saving the frame information: " + e.toString());
						return;
					}

				} catch(final Exception eE){
					TraceManager.getDefaultTracer ().error ( "Save As button caught unhandled and unexpected exception.", eE );
				}
			}
		});

		closeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				try {
					canceled = true;
					tableShell.close();
				} catch(final Exception eE){
					TraceManager.getDefaultTracer ().error ( "Close button caught unhandled and unexpected exception.", eE );
				}
			}
		});
		tableShell.setLocation(parent.getLocation().x + 50, parent.getLocation().y + 50);
	}

	private void createTableColumns() {
		for (int i = 0; i < columns.length; i++) {
			final TableColumn col = new TableColumn(table, SWT.NONE);
			col.setMoveable(false);
			col.setText(columns[i]);
			col.setWidth(widths[i]);
		}
	}

	/**
	 * Adds a row to the table for each bad frame in the given list
	 * 
	 * @param vcidBadFrames list of bad frame messages
	 */
	public void setBadFrameList(final List<IFrameEventMessage> vcidBadFrames) {
		this.messages = vcidBadFrames;
		for (final IFrameEventMessage msg : vcidBadFrames) {
			final TableItem item = new TableItem(table, SWT.NONE);
			item.setText(0, String.valueOf(msg.getStationInfo().getDssId()));
			item.setText(1, String.valueOf(msg.getFrameInfo().getVcid()));
			item.setText(2, String.valueOf(msg.getFrameInfo().getSeqCount()));
			item.setText(3, msg.getFrameInfo().getType());
			item.setText(4, msg.getStationInfo().getErtString());
            item.setText(5, new SprintfFormat().anCsprintf("%10.1f",
                    msg.getStationInfo().getBitRate()));
			item.setText(6, msg.getFrameInfo().getBadReason().toString());
		}
		SWTUtilities.justifyTable(table);
	}

	/**
     * {@inheritDoc}
	 * @see jpl.gds.shared.swt.ChillShell#open()
	 */
	@Override
    public void open() {
		tableShell.setSize(600, 400);
		tableShell.open();
	}

	/**
     * {@inheritDoc}
	 * @see jpl.gds.shared.swt.ChillShell#getShell()
	 */
	@Override
    public Shell getShell() {
		return tableShell;
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
	 * Indicates if a "close" was selected by the user.
	 * @see jpl.gds.shared.swt.ChillShell#wasCanceled()
	 */
	@Override
    public boolean wasCanceled() {
		return canceled;
	}
}