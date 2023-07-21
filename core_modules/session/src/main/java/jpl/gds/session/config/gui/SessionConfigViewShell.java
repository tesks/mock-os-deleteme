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
package jpl.gds.session.config.gui;

import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.velocity.Template;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.springframework.context.ApplicationContext;

import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.session.config.SessionConfigurationUtilities;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.swt.ChillShell;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.shared.template.ApplicationTemplateManager;
import jpl.gds.shared.template.TemplateException;
import jpl.gds.shared.template.TemplateManager;

/**
 * This is a GUI shell for displaying a session configuration as text. It uses a
 * velocity template to format the displayed text. It allows the user to save
 * the session configuration to a file as text or XML.
 * 
 */
public class SessionConfigViewShell implements ChillShell {

    private final String               TITLE;
	private static final String DISPLAY_STYLE = "text";
	private static final String FONT_NAME = "Courier";
	private static final int FONT_SIZE = 10;

	private Shell mainShell = null;
	private final Shell parent;
	private static final int TEXT_SHELL_HEIGHT = 800;
	private static final int TEXT_SHELL_WIDTH = 750;
	private Text mainText;
	private IContextConfiguration sessionConfig;
	private ApplicationTemplateManager templateMgr;
	private final SWTUtilities util = new SWTUtilities();
	private final ApplicationContext appContext;
    private final SseContextFlag       sseFlag;

	/**
	 * Creates a SessionConfigViewShell with the given shell as its parent.
	 * 
	 * @param appContext  the current ApplicationContext object
	 * 
	 * @param parent
	 *            the parent Shell widget
	 */
	public SessionConfigViewShell(final ApplicationContext appContext, final Shell parent) {

		this.parent = parent;
		this.appContext = appContext;
        this.sseFlag = appContext.getBean(SseContextFlag.class);

        TITLE = "Current Session Configuration for "
                + GdsSystemProperties.getSystemMissionIncludingSse(sseFlag.isApplicationSse()).toUpperCase();
		
		try {
			// We need a velocity template manager
            this.templateMgr = new ApplicationTemplateManager(GdsSystemProperties.getSystemMissionIncludingSse(sseFlag.isApplicationSse()),
                                                              this);
		} catch (final TemplateException e) {
			// Should never happen, but if it does there is little we can do.
            TraceManager.getDefaultTracer(appContext).error(e);
		}
		createGui();
	}

	/**
	 * Closes this shell.
	 */
	public void close() {

		this.mainShell.close();
	}

	/**
	 * Sets a new context configuration object for this shell to display.
	 * 
	 * @param config
	 *            the context configuration to set
	 */
	public void setContextConfiguration(final IContextConfiguration config) {

		this.sessionConfig = config;
		if (this.mainText != null && !this.mainText.isDisposed()) {
			this.mainText.setText(formatConfigText());
		}
	}

	/**
	 * Creates all the GUI controls
	 */
	private void createGui() {

		// Create the main shell
		this.mainShell = new Shell(this.parent, SWT.DIALOG_TRIM | SWT.RESIZE);
		this.mainShell.setSize(TEXT_SHELL_WIDTH, TEXT_SHELL_HEIGHT);
		final FormLayout shellLayout = new FormLayout();
		shellLayout.spacing = 5;
		shellLayout.marginHeight = 2;
		shellLayout.marginWidth = 2;
		this.mainShell.setLayout(shellLayout);
		this.mainShell.setText(TITLE);

		// Create the Text display widget
		this.mainText = new Text(this.mainShell, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		final FormData fd = new FormData();
		fd.top = new FormAttachment(0);
		fd.bottom = new FormAttachment(90);
		fd.left = new FormAttachment(0);
		fd.right = new FormAttachment(100);
		this.mainText.setLayoutData(fd);
		final Font textFont = new Font(this.mainShell.getDisplay(), new FontData(
				FONT_NAME, FONT_SIZE, SWT.NONE));
		this.mainText.setFont(textFont);
		this.mainText.setText("Temp");
		this.mainShell.setLocation(this.parent.getLocation().x + 50,
				this.parent.getLocation().y + 50);

		// Create the button holder composite
		final Composite composite = new Composite(this.mainShell, SWT.NONE);
		final GridLayout rl = new GridLayout(3, true);
		composite.setLayout(rl);
		final FormData formData8 = new FormData();
		formData8.right = new FormAttachment(100);
		formData8.bottom = new FormAttachment(100);
		composite.setLayoutData(formData8);

		// Create the save to XML button and hang a selection handler on it
		final Button saveXmlButton = new Button(composite, SWT.PUSH);
		saveXmlButton.setText("Save as XML...");
		GridData gd = new GridData();
		gd.horizontalAlignment = SWT.FILL;
		saveXmlButton.setLayoutData(gd);

		saveXmlButton.addSelectionListener(new SelectionAdapter() {
			/**
			 * {@inheritDoc}
			 * 
			 * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
			 */
			@Override
			public void widgetSelected(
					final org.eclipse.swt.events.SelectionEvent e) {

				try {

					final String text = SessionConfigViewShell.this.sessionConfig.toPrettyXml();
					saveToFile("<?xml version=\"1.0\"?>\n" + text);

				} catch (final Exception eE) {
					TraceManager.getDefaultTracer().error(
							"save XML button caught unhandled an unexpected exception", eE);
				}
			}
		});

		// CReate the save as text button and hang a selection handler on it
		final Button saveTextButton = new Button(composite, SWT.PUSH);
		saveTextButton.setText("Save as Text...");
		gd = new GridData();
		gd.horizontalAlignment = SWT.FILL;
		saveTextButton.setLayoutData(gd);

		saveTextButton.addSelectionListener(new SelectionAdapter() {
			/**
			 * {@inheritDoc}
			 * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
			 */
			@Override
			public void widgetSelected(
					final org.eclipse.swt.events.SelectionEvent e) {

				try {
					final String text = formatConfigText();
					saveToFile(text);
				} catch (final Exception eE) {
					TraceManager.getDefaultTracer().error(
							"save text button caught unhandled an unexpected exception", eE);
				}
			}
		});

		// Create the close button and hang a selection handler on it
		final Button closeButton = new Button(composite, SWT.PUSH);
		closeButton.setText("Close");
		gd = new GridData();
		gd.horizontalAlignment = SWT.FILL;
		closeButton.setLayoutData(gd);

		closeButton.addSelectionListener(new SelectionAdapter() {
			/**
			 * {@inheritDoc}
			 * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
			 */
			@Override
			public void widgetSelected(
					final org.eclipse.swt.events.SelectionEvent e) {

				try {

					SessionConfigViewShell.this.mainShell.close();

				} catch (final Exception eE) {
					TraceManager.getDefaultTracer().error(
							"close button caught unhandled an unexpected exception", eE);
				}
			}
		});
	}

	/**
	 * Formats the current session configuration into text using velocity.
	 * 
	 * @return formatted text
	 */
	private String formatConfigText() {
        final SessionConfigurationUtilities sessionUtil = new SessionConfigurationUtilities(appContext);
		try {
			// The template comes from
			// templates/common/app/ShowConfigViewShell/ShowConfig
			final Template t = this.templateMgr.getTemplateForStyle("ShowConfig",
					DISPLAY_STYLE);
			if (t != null) {
                return TemplateManager.createText(t, sessionUtil.assembleSessionConfigData(sessionConfig));
			} else {
				return "Unable to format session text because the template could not be found.";
			}
		} catch (final TemplateException e) {
		    TraceManager.getDefaultTracer().error(e);
		}
		return "Unable to format session configuration because of a template error";
	}

	/**
	 * Saves the given session text to a user-selected file.
	 * 
	 * @param text
	 *            the text to save
	 */
	private void saveToFile(final String text) {

		final String filename = this.util.displayStickyFileSaver(this.mainShell,
				"SessionConfigViewShell", null, null);

		if (filename != null) {
			try (
			        FileOutputStream fos = new FileOutputStream(filename);
			    ) {

				fos.write(text.getBytes());
				fos.close();

			} catch (final IOException e) {
				SWTUtilities.showErrorDialog(
						this.mainShell,
						"File Save Error",
						"Could not save to file " + filename + ": "
								+ e.toString());
			}
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.shared.swt.ChillShell#getShell()
	 */
	@Override
	public Shell getShell() {

		return this.mainShell;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.shared.swt.ChillShell#getTitle()
	 */
	@Override
	public String getTitle() {

		return TITLE;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.shared.swt.ChillShell#open()
	 */
	@Override
	public void open() {

		this.mainShell.open();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.shared.swt.ChillShell#wasCanceled()
	 */
	@Override
	public boolean wasCanceled() {

		return false;
	}
}
