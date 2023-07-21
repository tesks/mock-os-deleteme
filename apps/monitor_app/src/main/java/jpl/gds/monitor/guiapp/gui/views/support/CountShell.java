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
 * 
 */
package jpl.gds.monitor.guiapp.gui.views.support;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.swt.ChillShell;
import jpl.gds.shared.swt.TextViewShell;


/**
 * The CountShell class is a small pop-up window that is displayed when
 * "View Count" is selected from the right-click menu on chill monitor.
 * 
 * It shows the total number of rows, the number of selected rows and, for EVRs,
 * the number of marked rows.
 *
 */
public class CountShell implements ChillShell {
	/**
	 * Used for formatting latest date count was refreshed
	 */
	public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
	
    private Shell mainShell = null;
    private Shell parent = null;
    private final static int TEXT_SHELL_HEIGHT = 200;
    private final static int TEXT_SHELL_WIDTH  = 255;
    private Text mainText;
    private final ICountableView forView;
    
    /**
     * Count shell title
     */
    public String title;
    
    /**
     * Constructor: sets the parent shell, composite type that is launching 
     * this count shell and title. Creates the GUI.
     * 
     * @param parent parent shell
     * @param comp table view composite (EVR, Message List or Product)
     */
    public CountShell(Shell parent, ICountableView view, String viewName)
    {
        this.parent = parent;
        this.title = getTitle();
        this.forView = view;
        this.title = viewName + " Row Counts";
        createGui();
     }
    
    /**
     * Sets the text in this composite (i.e. number of selected rows, number 
     * of marked rows and total number of rows)
     * 
     * @param text text that will be set in the composite
     */
    private void setText(String text) {
        this.mainText.setText(text);
        mainShell.pack();
    }
    
    /**
     * {@inheritDoc}
	 * @see jpl.gds.shared.swt.ChillShell#getShell()
     */
    @Override
    public Shell getShell() {
        return this.mainShell;
    }
    
    private void createGui() {
        this.mainShell = new Shell(this.parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MODELESS);
        this.mainShell.setSize(TEXT_SHELL_WIDTH, TEXT_SHELL_HEIGHT);
        this.mainShell.setText(this.title);
        FormLayout shellLayout = new FormLayout();
        shellLayout.spacing = 5;
        shellLayout.marginHeight = 2;
        shellLayout.marginWidth = 2;
        this.mainShell.setLayout(shellLayout);
        
        this.mainText = new Text(this.mainShell, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL );
        FormData fd = new FormData();
        fd.top = new FormAttachment(0);
        fd.bottom = new FormAttachment(80);
        fd.left = new FormAttachment(0);
        fd.right = new FormAttachment(100);
        this.mainText.setLayoutData(fd);
        
        this.mainText.setText("Temp");
        this.mainShell.setLocation(this.parent.getLocation().x + 50, this.parent.getLocation().y + 50);

        Button refreshButton = new Button(this.mainShell, SWT.PUSH);
        refreshButton.setText("Refresh");
        this.mainShell.setDefaultButton(refreshButton);
        FormData fdb = new FormData();
        fdb.top = new FormAttachment(mainText);
        fdb.right = new FormAttachment(50);
        
        refreshButton.setLayoutData(fdb);
        
        Button closeButton = new Button(this.mainShell, SWT.PUSH);
        closeButton.setText("Close");
        fdb = new FormData();
        fdb.top = new FormAttachment(mainText);
        fdb.left = new FormAttachment(refreshButton, 5);
        closeButton.setLayoutData(fdb);
        
        closeButton.addSelectionListener(new SelectionAdapter() {
            @Override
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                try {
                    mainShell.close();
                } catch (Exception eE) {
                    TraceManager.getDefaultTracer()
                            .error("close button caught unhandled and unexpected exception in CountShell.java");
                    eE.printStackTrace();
                 }
            }
        });
        
        refreshButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                try
                {
                	refresh();
                } catch (RuntimeException e1) {
                    e1.printStackTrace();
                }
            }
        });
        
        refresh();
    }

	private void refresh()
	{
		String text = "Selected Rows: " + forView.getSelectedCount() + "\n";
	    text += "Marked Rows: " + forView.getMarkedCount() + "\n";
		text += "Total Rows: " + forView.getRowCount() + "\n\n";
		text += "Timestamp: " + now();
		
		this.setText(text);
	}
	
	private String now()
	{
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
		return sdf.format(cal.getTime());
	}
	
	/**
     * {@inheritDoc}
	 * @see jpl.gds.shared.swt.ChillShell#open()
     */
    @Override
    public void open() {
        this.mainShell.open();
    }
    
    /**
     * Closes this count viewer shell
     */
    public void close() {
        this.mainShell.close();
    }

    /**
     * Launches a standalone version of this count shell
     * 
     * @param args command line arguments (not used in this app)
     */
    public static void main(String[] args)
    {
        Display mainDisplay = new Display();
        Shell parent = new Shell(mainDisplay);
        TextViewShell textView = new TextViewShell(parent, TraceManager.getDefaultTracer());
        textView.open();
     
        textView.setText("Blah\nBlah");
        while (!parent.isDisposed()) {
            if (!mainDisplay.readAndDispatch()) {
                mainDisplay.sleep();
            }
        }
        mainDisplay.dispose();
        mainDisplay = null;
        System.exit(0);
    }

    /**
     * {@inheritDoc}
	 * @see jpl.gds.shared.swt.ChillShell#getTitle()
     */
    @Override
    public String getTitle()
    {
        return this.title;
    }

    /**
     * {@inheritDoc}
	 * @see jpl.gds.shared.swt.ChillShell#wasCanceled()
     */
    @Override
    public boolean wasCanceled() {
        return false;
    }

}

