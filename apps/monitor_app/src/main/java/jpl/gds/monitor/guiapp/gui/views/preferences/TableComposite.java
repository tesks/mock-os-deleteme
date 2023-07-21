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
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

import jpl.gds.perspective.ChillTable;
import jpl.gds.perspective.ChillTableColumn;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.swt.ChillFontCreator;
import jpl.gds.shared.swt.types.ChillFont;

/**
 * This class presents a GUI composite specifically for configuring a 
 * ChillTable object in the MPCS perspective.
 * 
 */
public class TableComposite {

    private ChillTable currentTable;
    private final Composite mainComposite;
    private final Composite parent;
    private Button[] columnButtons;
    private boolean dirty;
    private Combo sortOrderCombo;
    private boolean useGroup = true;
    private ChillTableColumn[] tableColumns;

    /**
     * Creates a TableComposite with the given parent.
     * 
     * @param parent
     *            the parent Composite widget
     */
    public TableComposite(final Composite parent) {
        this(parent, true);
    }

    /**
     * Creates a TableComposite with the given parent, optionally using an SWT
     * Group rather than an SWT Composite as the main GUI widget.
     * 
     * @param parent
     *            the parent Composite widget
     * @param group
     *            true to use a Group rather than a Composite
     */
    public TableComposite(final Composite parent, final boolean group) {
        this.parent = parent;
        useGroup = group;
        mainComposite = new Composite(this.parent, SWT.NONE);
        mainComposite.setLayout(new FillLayout());
    }

    private void createControls() {

        Composite columnComposite = null;

        if (useGroup) {
            columnComposite = new Group(mainComposite, SWT.BORDER);
            ((Group) columnComposite)
                    .setText(currentTable.getName() + " Table");
        } else {
            columnComposite = new Composite(mainComposite, SWT.NONE);
        }

        columnComposite.setLayout(new GridLayout(1, false));
        columnComposite.setFont(ChillFontCreator
                .getDefaultFont(ChillFont.FontSize.MEDIUM));

        final Button headerButton = new Button(columnComposite, SWT.CHECK);
        headerButton.setText("Show Column Headers");
        headerButton.setSelection(currentTable.isShowColumnHeader());
        headerButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(
                    final org.eclipse.swt.events.SelectionEvent e) {
                try {
                    currentTable.setShowColumnHeader(headerButton.getSelection());
                } catch (final Exception eE) {
                    TraceManager

                            .getDefaultTracer()
                            .error(
                                    "Show-Column-Headers button caught " +
                                    "unhandled and unexpected exception in " +
                                    "TableComposite.java");
                    eE.printStackTrace();
                }
            }
        });

        final Button sortButton = new Button(columnComposite, SWT.CHECK);
        sortButton.setText("Allow Sorting");
        sortButton.setSelection(currentTable.isSortAllowed());
        sortButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(
                    final org.eclipse.swt.events.SelectionEvent e) {
                try {
                    currentTable.setSortAllowed(sortButton.getSelection());
                    sortOrderCombo.setEnabled(sortButton.getSelection());
                } catch (final Exception eE) {
                    TraceManager

                            .getDefaultTracer()
                            .error(
                                    "ALLOW-SORTING button caught unhandled " +
                                    "and unexpected exception in " +
                                    "TableComposite.java");
                    eE.printStackTrace();
                }
            }
        });

        final Composite sortComposite = new Composite(
                columnComposite, SWT.NONE);
        sortComposite.setLayout(new GridLayout(2, false));
        sortComposite.setLayoutData(new GridData());

        final Label sortLabel = new Label(sortComposite, SWT.NONE);
        sortLabel.setText("Sort Order: ");
        sortOrderCombo = new Combo(sortComposite, SWT.DROP_DOWN);
        sortOrderCombo.add("ASCENDING");
        sortOrderCombo.add("DESCENDING");
        if (currentTable.isSortAscending()) {
            sortOrderCombo.setText("ASCENDING");
        } else {
            sortOrderCombo.setText("DESCENDING");
        }
        sortOrderCombo.setEnabled(currentTable.isSortAllowed());
        sortOrderCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(
                    final org.eclipse.swt.events.SelectionEvent e) {
                try {
                    currentTable.setSortAscending(sortOrderCombo.getText()
                            .equalsIgnoreCase("ASCENDING"));
                } catch (final Exception eE) {
                    TraceManager

                            .getDefaultTracer()
                            .error(
                                    "SORT-ORDER button caught unhandled and " +
                                    "unexpected exception in TableComposite.java");
                    eE.printStackTrace();
                }
            }
        });

        if (useGroup) {
            // easy way to create a blank space
            final Label x = new Label(columnComposite, SWT.NONE);
            x.setLayoutData(new GridData());
        }

        columnButtons = new Button[currentTable.getColumnCount()];
        tableColumns = currentTable.getAvailableColumns();

        for (int i = 0; i < tableColumns.length; i++) {
            if (currentTable.isColumnDeprecated(i)) {
                columnButtons[i] = null;
                continue;
            }
            columnButtons[i] = new Button(columnComposite, SWT.CHECK);
            columnButtons[i].setText("Show "
                    + tableColumns[i].getOfficialName());
            columnButtons[i].setSelection(tableColumns[i].isEnabled());
            final int index = i;
            columnButtons[i].addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(
                        final org.eclipse.swt.events.SelectionEvent e) {
                    try {
						currentTable.enableColumn(index, columnButtons[index].getSelection());
						
						/**
                         * Manually set column
						 * order here because if selected columns change from 
						 * "Edit Perspective" menu item, column positions never 
						 * get set.
						 */
						int[] currentOrder = currentTable.getColumnOrder();
						
						if(columnButtons[index].getSelection()) {
							// ADDING column
							int newColumnPosition = -1;
							
							// First, find new column that has position -1
							for(int j=0; j<currentOrder.length; j++) {
								if(currentOrder[j] == -1) {
									newColumnPosition = j;
									break;
								}
							}
	
							// Then, scoot columns AFTER newly inserted column over by 1
							for(int j=0; j<currentOrder.length; j++) {
								if(currentOrder[j] >= newColumnPosition) {
									currentOrder[j]+=1;
								}
							}
							
							// Last, update new column value
							currentOrder[newColumnPosition] = newColumnPosition;
						}
						else {
							//REMOVING column
							int removedColumnPosition = -1;
							
							// Put array in ArrayList
							List<Integer> orderList = new ArrayList<Integer>();
							for(int j=0; j<currentOrder.length; j++) {
								orderList.add(currentOrder[j]);
							}
							
							// Search for removed column
							boolean found = false;
							for(int j=0; j<orderList.size(); j++) {
								if(!orderList.contains(j)) {
									removedColumnPosition = j;
									found = true;
									break;
								}
							}
							
							// Adjust column position for columns after removed column
							// Nothing to do if removed column was last column
							if(found) {
								for(int j=0; j<currentOrder.length; j++) {
									if(currentOrder[j] > removedColumnPosition) {
										currentOrder[j]-=1;
									}
								}
							}
						}
						
						currentTable.setColumnOrder(currentOrder);
						
                        dirty = true;
                    } catch (final Exception eE) {
                        TraceManager

                                .getDefaultTracer()
                                .error(
                                        "SHOW-COLUMNS button caught " +
                                        "unhandled and unexpected exception " +
                                        "in TableComposite.java");
                        eE.printStackTrace();
                    }
                }
            });
        }

        mainComposite.layout(true);
        mainComposite.pack();
    }

    /**
     * Disables the column selection buttons for indicated table columns.
     * 
     * @param nums
     *            list of column numbers to disable, starting at 0
     */
    public void disableColumnButtons(final int... nums) {

        if (columnButtons == null) {
            return;
        }

        for (int i = 0; i < nums.length; i++) {
            columnButtons[nums[i]].setEnabled(false);
        }
    }

    /**
     * Gets the main Composite object.
     * 
     * @return Composite or Group object
     */
    public Composite getComposite() {
        return mainComposite;
    }

    /**
     * Indicates whether the user has made changes to the table configuration.
     * 
     * @return true if user changed anything, false if not
     */
    public boolean getDirty() {
        return dirty;
    }

    /**
     * Gets the current table object
     * 
     * @return Returns the configured table object
     */
    public ChillTable getTable() {
        return currentTable;
    }

    /**
     * Initializes the fields in this composite from the given ChillTable
     * object.
     * 
     * @param table
     *            ChillTable object containing configuration of the table
     */
    public void init(final ChillTable table) {
        currentTable = table.copy();
        dirty = false;
        createControls();
        mainComposite.pack();
    }
}
