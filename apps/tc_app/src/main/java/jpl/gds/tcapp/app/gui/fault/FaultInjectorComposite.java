/*
 * Copyright 2006-2019. California Institute of Technology.
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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.springframework.context.ApplicationContext;

import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.shared.swt.types.ChillColor;
import jpl.gds.tc.api.config.UplinkParseException;
import jpl.gds.tcapp.app.gui.AbstractUplinkComposite;
import jpl.gds.tcapp.app.gui.TransmitEvent;

/**
 * This is the wizard portion of the uplink fault injector. This shell supports
 * the operations for laying out a fault injector page and navigating between
 * pages.
 * 
 * The general layout of the shell is like this:
 * 
 * ----------------------------------------------------------------------
 * 
 * Page Title Page Counter Page Description
 * ===========================Separator===================================
 * 
 * Specific editor section (GUI of the editor page that we're on)
 * 
 * ==========================Separator===================================
 * 
 * Exit Back Next Button Button Button
 * 
 * -----------------------------------------------------------------------
 * 
 *
 * Minor update to convert all exceptions
 *          encountered by display update to FaultInjectorException
 */
public class FaultInjectorComposite extends AbstractUplinkComposite {
	/** The title of the fault injector shell that will be displayed */
	protected static final String TITLE = "MPCS Uplink Fault Injector";

	/** Logger object for writing log messages */
	protected final Tracer trace; 


	/**
	 * The composite that will display and contain all the views we generate.
	 * This the top level GUI object in the fault injector.
	 */
	protected Composite parent = null;

	/** The main shell of the fault injector */
	protected final Shell mainShell;

	/**
	 * The font used for displaying individual fault injection page titles
	 */
	protected final Font titleFont;

	/**
	 * The font used for displaying individual fault injection page
	 * descriptions.
	 */
	protected final Font descFont;

	/**
	 * The upper 10% or so of the GUI located above the top separator that
	 * contains the title, description, and page counter for the current fault
	 * injection page.
	 */
	protected Composite titleComposite = null;

	/**
	 * The label containing the title of the current fault injection page. Uses
	 * the "titleFont".
	 */
	protected Label titleLabel = null;
	/**
	 * The label containing the counter info for the current fault injection
	 * page (e.g. page "2 of 5")
	 */
	protected Label pageCounterLabel = null;
	/**
	 * The label containing the description of the current fault injection page.
	 * Uses the "descriptionFont".
	 */
	protected Label descriptionLabel = null;

	/**
	 * The central 90% or so of the GUI between the top and bottom separators
	 * where individual editor composites are displayed. This is the section
	 * that contains the views like the CltuEditorComposite,
	 * CommandBuilderComposite, etc.
	 */
	protected Composite contentComposite = null;
	/**
	 * A pointer to the current GUI component that is contained in the content
	 * composite. This is the same object as the "contentComposite", but storing
	 * it as a FaultInjectorGuiComponent also means that we don't have to cast
	 * the Composite every time we want to do an operation on it.
	 */
	protected FaultInjectorGuiComponent currentContent = null;

	/**
	 * The bottom 10% or so of the GUI located beneath the bottom separator.
	 * Contains the buttons for closing the GUI and navigating between fault
	 * injector pages.
	 */
	protected Composite buttonComposite = null;
	/** Button to go to the previous fault injection page. */
	protected Button backButton = null;
	/** Button to go to the next fault injection page. */
	protected Button nextButton = null;

	/** The label that separates the titleComposite and contentComposite */
	protected Label topSeparator = null;
	/** The label that separates the contentComposite and buttonComposite */
	protected Label bottomSeparator = null;

	/**
	 * Handle to inner class instance that handles all GUI interactive events
	 * (e.g. button clicks)
	 */
	protected EventHandler handler = null;

	/**
	 * The data that is passed between editors in the fault injector GUI. This
	 * contains data such as the command being sent, frames, cltus, etc.
	 */
	protected FaultInjectionState dataState = null;
	/**
	 * The state of our fault injection wizard (if you picture it as a state
	 * machine with each page as a state) in terms of what page we're on and
	 * what the next and previous pages are.
	 */
	protected UplinkFaultInjectorPageState pageState = null;

	/** The background color to use to denote fields in error. */
	protected static final ChillColor.ColorName errorColorName = ChillColor.ColorName.YELLOW;
	/** The background color to use to denote fields with no error. */
	protected static final ChillColor.ColorName okColorName = ChillColor.ColorName.WHITE;
	/** An objectified version of the okColorName */
	public static final ChillColor ocColor = new ChillColor(
			FaultInjectorComposite.okColorName);
	/** An objectified version of the errorColorName */
	public static final ChillColor ecColor = new ChillColor(
			FaultInjectorComposite.errorColorName);

	/**
	 * The constructor used to create a fault injection GUI
	 * 
	 * @param parent The composite that this will be a child of
	 * 
	 * @throws DictionaryException If there's a problem reading/using the
	 *         command dictionary
	 * @throws FaultInjectorException If there's a problem constructing the
	 *         initial GUI layout
	 */
	public FaultInjectorComposite(final ApplicationContext appContext, final Composite parent)
			throws DictionaryException, FaultInjectorException {
		super(appContext, parent, SWT.NONE);
        this.trace = TraceManager.getDefaultTracer(appContext);
		this.parent = parent;

		this.handler = new EventHandler();

		this.mainShell = parent.getShell();

		// TODO: The initial state should eventually be chosen by the user so
		// that we can go down multiple fault injection
		// paths (e.g. command vs. file load vs. SCMF)
		this.pageState = CommandFaultInjectionPageState.COMMAND_BUILDER;
		this.dataState = new FaultInjectionState();

		this.titleFont = new Font(this.mainShell.getDisplay(), new FontData(
				"Helvetica", 14, SWT.BOLD));
		this.descFont = new Font(this.mainShell.getDisplay(), new FontData(
				"Helvetica", 12, SWT.NONE));

		createControls();

		updateContent(this.pageState
				.getComponentForState(appContext, this.contentComposite));
	}

	/**
	 * Create and locate all the controls for the initial layout of the GUI
	 * 
	 * @throws DictionaryException If there's an error interpreting the command
	 *         dictionary
	 */
	protected void createControls() throws DictionaryException {
		final int spacingOffset = 5;

		// build the title composite (it's empty to start)
		this.titleComposite = new Composite(this, SWT.NONE);
		final FormData tcFormData = new FormData();
		tcFormData.top = new FormAttachment(0, spacingOffset);
		tcFormData.bottom = new FormAttachment(12);
		tcFormData.left = new FormAttachment(0);
		tcFormData.right = new FormAttachment(100);
		this.titleComposite.setLayoutData(tcFormData);

		// separate the title and content composites
		this.topSeparator = new Label(this, SWT.SEPARATOR | SWT.CENTER
				| SWT.HORIZONTAL);
		final FormData tsFormData = new FormData();
		tsFormData.left = new FormAttachment(0);
		tsFormData.right = new FormAttachment(100);
		tsFormData.top = new FormAttachment(this.titleComposite, spacingOffset);
		tsFormData.bottom = new FormAttachment(tcFormData.bottom.numerator + 2);
		this.topSeparator.setLayoutData(tsFormData);

		// build the initial button composite (it's empty to start)
		this.buttonComposite = new Composite(this, SWT.NONE);
		final FormData bcFormData = new FormData();
		bcFormData.bottom = new FormAttachment(100);
		bcFormData.left = new FormAttachment(0);
		bcFormData.right = new FormAttachment(100);
		this.buttonComposite.setLayoutData(bcFormData);

		// separate the content and button composites
		this.bottomSeparator = new Label(this, SWT.SEPARATOR | SWT.CENTER
				| SWT.HORIZONTAL);
		final FormData bsFormData = new FormData();
		bsFormData.left = new FormAttachment(0);
		bsFormData.right = new FormAttachment(100);
		bsFormData.bottom = new FormAttachment(this.buttonComposite,
				spacingOffset);
		this.bottomSeparator.setLayoutData(bsFormData);

		// build the initial content composite (it's empty to start)
		this.contentComposite = new Composite(this, SWT.NONE);
		final FormData ccFormData = new FormData();
		ccFormData.top = new FormAttachment(this.topSeparator, spacingOffset);
		ccFormData.bottom = new FormAttachment(this.bottomSeparator,
				spacingOffset);
		ccFormData.left = new FormAttachment(0);
		ccFormData.right = new FormAttachment(100);
		this.contentComposite.setLayoutData(ccFormData);

		createTitleComposite();

		createContentComposite();

		createButtonComposite();

		// set the table order for the GUI to go top to bottom (title then
		// content then buttons)
		this.setTabList(new Control[] { this.titleComposite,
				this.contentComposite, this.buttonComposite });
	}

	/**
	 * Create all the contents of the title composite
	 */
	protected void createTitleComposite() {
		final FormLayout tcLayout = new FormLayout();
		this.titleComposite.setLayout(tcLayout);

		// create a label for the title of the current fault injection page
		// (empty for now)
		this.titleLabel = new Label(this.titleComposite, SWT.LEFT);
		final FormData tlFormData = new FormData();
		tlFormData.left = new FormAttachment(0);
		tlFormData.right = new FormAttachment(70);
		tlFormData.top = new FormAttachment(0);
		this.titleLabel.setLayoutData(tlFormData);
		this.titleLabel.setFont(this.titleFont);

		// create a label for the page X of Y display (empty for now)
		this.pageCounterLabel = new Label(this.titleComposite, SWT.RIGHT);
		final FormData pclFormData = new FormData();
		pclFormData.left = new FormAttachment(75);
		pclFormData.right = new FormAttachment(100, -5);
		pclFormData.top = new FormAttachment(this.titleLabel, 0, SWT.CENTER);
		this.pageCounterLabel.setLayoutData(pclFormData);
		this.pageCounterLabel.setFont(this.descFont);

		// create a label for the description of the current fault injection
		// page (empty for now)
		this.descriptionLabel = new Label(this.titleComposite, SWT.LEFT);
		final FormData dlFormData = new FormData();
		dlFormData.left = new FormAttachment(0);
		dlFormData.right = new FormAttachment(100);
		dlFormData.top = new FormAttachment(this.titleLabel, 5);
		dlFormData.bottom = new FormAttachment(100);
		this.descriptionLabel.setLayoutData(dlFormData);
		this.descriptionLabel.setFont(this.descFont);
	}

	/**
	 * Create the contents of the content composite. This does nothing except
	 * set a layout for the content composite. The actual contents will be added
	 * dynamically later.
	 */
	protected void createContentComposite() {
		final FillLayout layout = new FillLayout();
		this.contentComposite.setLayout(layout);
	}

	/**
	 * Create all the contents of the button composite
	 */
	protected void createButtonComposite() {
		final FormLayout bcLayout = new FormLayout();
		this.buttonComposite.setLayout(bcLayout);

		// Create the back button to go to the previous page (place it in the
		// lower right). Disabled to start.
		this.backButton = new Button(this.buttonComposite, SWT.PUSH);
		this.backButton.setText("< Back");
		final FormData bbFormData = new FormData();
		bbFormData.top = new FormAttachment(0);
		bbFormData.left = new FormAttachment(70);
		this.backButton.setLayoutData(bbFormData);
		this.backButton.addSelectionListener(this.handler);
		this.backButton.setEnabled(false);

		// Create the next button to go to the next page (place it in the lower
		// right). Enabled to start.
		this.nextButton = new Button(this.buttonComposite, SWT.PUSH);
		this.nextButton.setText("Next >");
		final FormData nbFormData = new FormData();
		nbFormData.top = new FormAttachment(0);
		nbFormData.left = new FormAttachment(85);
		this.nextButton.setLayoutData(nbFormData);
		this.nextButton.addSelectionListener(this.handler);
		this.nextButton.setEnabled(true);
	}

	/**
	 * Cause the fault injector to move back to the previous fault injection
	 * page. This is the method invoked by the back button on the GUI.
	 * 
	 * @param isReverting Should be set to false unless you know what you're
	 *        doing. When set to true, this will save the doPageBack/doPageNext
	 *        methods from going into an infinite co-recursion loop when trying
	 *        to recover from an error. In this case, "true" tells doPageBack
	 *        that it has been called by "doPageNext" and should not call
	 *        "doPageNext" if it sees an error.
	 */
	protected void doPageBack(final boolean isReverting) {
		// If somehow we get a back event on the first page, ignore it...we
		// can't go back from there
		if (!FaultInjectorComposite.this.pageState.isFirstPage()) {
			// Change our state to point to the previous page in the wizard
			FaultInjectorComposite.this.pageState = FaultInjectorComposite.this.pageState
					.backState();

			// This is the new component to go into the content composite.
			// Create it and put it in where it goes.
			FaultInjectorGuiComponent component = null;
			try {
				component = this.pageState
						.getComponentForState(appContext, this.contentComposite);
				updateContent(component);
				return;
			}
			// If we hit this catch, it means there was an error transitioning
			// to the previous page
			catch (final FaultInjectorException fie) {
				// Shows the error to the user and log it
				SWTUtilities.showErrorDialog(
						FaultInjectorComposite.this.mainShell,
						"Data Processing Error", fie.getMessage());
				trace.error(fie.getMessage(), fie);

				// destroy the new component we created...if you don't do this,
				// you'll end up with multiple editor composites
				// in the contentComposite
				component.destroy();

				// if we haven't tried yet, try to move back to the page we were
				// on
				if (isReverting == false) {
					doPageNext(true);
				}
				// We're hosed.
				else {
					SWTUtilities
							.showErrorDialog(getShell(), "Unrecoverable Error",
									"The Fault Injector has fallen into an unusable state");
					trace.fatal("The Fault Injector has fallen into an unusable state");
				}
			}
		}
	}

	/**
	 * Cause the fault injector to move forward to the next fault injection
	 * page. This is the method invoked by the next button on the GUI.
	 * 
	 * @param isReverting Should be set to false unless you know what you're
	 *        doing. When set to true, this will save the doPageBack/doPageNext
	 *        methods from going into an infinite co-recursion loop when trying
	 *        to recover from an error. In this case, "true" tells doPageNext
	 *        that it has been called by "doPageBack" and should not call
	 *        "doPageBack" if it sees an error.
	 */
	protected void doPageNext(final boolean isReverting) {
		// If somehow we get a next event on the last page, ignore it...we can't
		// go forward from there
		if (FaultInjectorComposite.this.pageState.isLastPage() == false) {
			// This is the new component to go into the content composite.
			// Create it and put it in where it goes.
			FaultInjectorGuiComponent component = null;
			try {
				// save the page state in the current editor before tossing it
				// and moving on to the next editor
				if (this.currentContent != null) {
					this.currentContent.updateState();
				}
			} catch (final FaultInjectorException fie) {
				SWTUtilities.showErrorDialog(
						FaultInjectorComposite.this.mainShell,
						"Data Processing Error", fie.getMessage());
				return;
			}

			try {
				// Change our state to point to the next page in the wizard
				FaultInjectorComposite.this.pageState = FaultInjectorComposite.this.pageState
						.nextState();

				component = this.pageState
						.getComponentForState(appContext, this.contentComposite);
				updateContent(component);
				return;
			}
			// If we hit this catch, it means there was an error transitioning
			// to the next page
			catch (final FaultInjectorException fie) {

				// Shows the error to the user and log it
				SWTUtilities.showErrorDialog(
						FaultInjectorComposite.this.mainShell,
						"Data Processing Error", fie.getMessage());

				// destroy the new component we created...if you don't do this,
				// you'll end up with multiple editor composites
				// in the contentComposite
				if (component != null) {
					component.destroy();
				}

				// if we haven't tried yet, try to move back to the page we were
				// on
				if (isReverting == false) {
					doPageBack(true);
				}
				// We're hosed. Close the whole shell.
				else {
					SWTUtilities
							.showErrorDialog(getShell(), "Unrecoverable Error",
									"The Fault Injector has fallen into an unusable state");
					trace.fatal("The Fault Injector has fallen into an unusable state");
				}
			}
		}
	}

	/**
	 * Changes the contentComposite of the fault injector GUI to display a new
	 * editor (the one that's passed in)
	 * 
	 * @param component The new component to display in the content portion of
	 *        the fault injector GUI
	 * 
	 * @throws FaultInjectorException If there's an issue creating and
	 *         displaying the new content
	 */
	protected void updateContent(final FaultInjectorGuiComponent component)
			throws FaultInjectorException {
		// pass along the state from the old component to the new one (e.g. pass
		// the frame list to the CLTU editor)
		if (this.currentContent != null) {
			component.setFromState(this.currentContent.getCurrentState());
		}
		// if there's no state, this must be the first component, so create new
		// state for it to use
		else {
			component.setFromState(new FaultInjectionState());
		}

		// now that the state has been set, tell the component to set up its
		// display with the state information
		try {
			component.updateDisplay();
		} catch (Exception e) {
			throw new FaultInjectorException(e);
		}

		// change the titleComposite to display the information specific to the
		// new component
		this.titleLabel.setText(component.getTitle());
		this.descriptionLabel.setText(component.getDescription());
		this.pageCounterLabel.setText("Page #"
				+ this.pageState.getCurrentPageNumber() + " of "
				+ this.pageState.getLastPageNumber());

		// get rid of the old content in the contentComposite
		if (this.currentContent != null) {
			this.currentContent.destroy();
		}

		// put the new content into the contentComposite
		this.currentContent = component;

		// we need to forward the listeners
		if (this.currentContent instanceof UplinkOutputComposite) {
			((UplinkOutputComposite) this.currentContent)
					.setTransmitListeners(listeners);
		}

		// set the state of the buttons based on what page we're on
		// (e.g. disable the Next button if we're on the last page)
		setButtonStateForPage();

		// Tell the GUI to redo the layout so the user sees the change
		this.contentComposite.pack(true);
		this.mainShell.layout(true);
	}

	/**
	 * Set the state of the buttons in the button composite based on what page
	 * we're on in the flow. If we're on the first page, disable the Back
	 * button. If we're on the last page, disable the Next button.
	 */
	protected void setButtonStateForPage() {
		this.nextButton.setEnabled(this.pageState.isLastPage() == false);
		this.backButton.setEnabled(this.pageState.isFirstPage() == false);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.shared.swt.ChillShell#getShell()
	 */
	@Override
	public Shell getShell() {
		return (this.mainShell);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.shared.swt.ChillShell#getTitle()
	 */
	public String getTitle() {
		return (TITLE);
	}

	/**
	 * Internal class to handle all the events from the buttons in the
	 * buttonComposite
	 * 
	 *
	 */
	public class EventHandler implements SelectionListener {
		
		protected EventHandler() {}
		
		/**
		 * {@inheritDoc}
		 * 
		 * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
		 */
		@Override
		public void widgetDefaultSelected(final SelectionEvent arg0) {

		}

		/**
		 * {@inheritDoc}
		 * 
		 * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
		 */
		@Override
		public void widgetSelected(final SelectionEvent arg0) {
			// back button press...move back a page in the flow
			if (arg0.getSource() == FaultInjectorComposite.this.backButton) {
				doPageBack(false);
				FaultInjectorComposite.this.layout();
			}
			// next button press...move forward a page in the flow
			else if (arg0.getSource() == FaultInjectorComposite.this.nextButton) {
				doPageNext(false);
				FaultInjectorComposite.this.layout();
			}
		}
	}

	@Override
	public String getDisplayName() {
		return "Fault Injector";
	}

	@Override
	public void setFieldsFromTransmitHistory(final TransmitEvent historyItem) {
		// TODO Auto-generated method stub

	}

	@Override
	public void initiateSend() throws UplinkParseException {
		// no-op...fault injector will use its own send button
	}

	@Override
	public boolean needSendButton() {
		// fault injector will use its own send button
		return false;
	}
}
