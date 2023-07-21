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

import java.util.List;

import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.config.AbstractUiBindingConfiguration;
import org.eclipse.nebula.widgets.nattable.grid.GridRegion;
import org.eclipse.nebula.widgets.nattable.layer.LabelStack;
import org.eclipse.nebula.widgets.nattable.ui.NatEventData;
import org.eclipse.nebula.widgets.nattable.ui.binding.UiBindingRegistry;
import org.eclipse.nebula.widgets.nattable.ui.matcher.MouseEventMatcher;
import org.eclipse.nebula.widgets.nattable.ui.menu.IMenuItemProvider;
import org.eclipse.nebula.widgets.nattable.ui.menu.IMenuItemState;
import org.eclipse.nebula.widgets.nattable.ui.menu.PopupMenuAction;
import org.eclipse.nebula.widgets.nattable.ui.menu.PopupMenuBuilder;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import jpl.gds.monitor.guiapp.common.gui.INatListItem;
import jpl.gds.monitor.guiapp.gui.views.support.CountShell;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.swt.TextViewShell;

/**
 * Defines a common menu for all NAT Tables displayed by AbstractNatTableViewComposite.
 * Custom items can be added by subclasses of that composite class.
 *
 * @param <T> any type that implements INatListItem
 */
public class CommonNatTableMenuConfiguration<T extends INatListItem>
extends AbstractUiBindingConfiguration {
    
    private static final String                    DETAILS_MENU_KEY                = "ViewDetails";
    private static final String                    MARK_MENU_KEY                   = "MarkRows";
    private static final String                    UNMARK_MENU_KEY                 = "UnmarkRows";
    private static final String                    COPY_MENU_KEY                   = "Copy";


    /** Default trace logger */
    protected static final Tracer                  tracer                          = TraceManager.getDefaultTracer();

    /** Title for preferences menu item */
    protected static final String PREFERENCES_MENU_TITLE = "Preferences...";

    /** Partial title for toggle row header menu item. */
    protected static final String HIDE_SHOW_ROW_HEADER_MENU_TITLE = "Row Header";

    /** Partial title for toggle column header menu item. */
    protected static final String HIDE_SHOW_COL_HEADER_MENU_TITLE = "Column Header";

    /** Parent composite for this menu */
    private final AbstractNatTableViewComposite<T> tableViewComposite;

    /** NAT table menu this class creates */
    private final Menu localMenu;
    
    private PopupMenuBuilder localMenuBuilder;
    
    private MenuItem pauseMenuItem;


    /**
     * @param abstractNatTableViewComposite
     *            abstractNatTableViewComposite view
     * @param natTable
     *            natTable table
     */
    public CommonNatTableMenuConfiguration(final AbstractNatTableViewComposite<T> abstractNatTableViewComposite, final NatTable natTable) {
        tableViewComposite = abstractNatTableViewComposite;

        /* Empty menu builder */
        localMenuBuilder = new PopupMenuBuilder(natTable);

        /* PopupMenuBuilder is a little weird. The only method for adding menu items to it
         * recreates the PopupBuilder every time.  Therefore, it must be re-assigned
         * every time a menu is added.  Go figure.
         */

        /* Add preferences menu item, always enabled */
        localMenuBuilder = localMenuBuilder.withMenuItemProvider(getPreferencesMenuItemProvider());  

        /* Add pause/resume menu item, always enabled  */
        localMenuBuilder = localMenuBuilder.withMenuItemProvider(getPauseMenuItemProvider());

        /* Add clear and count menu items, always enabled. */
        localMenuBuilder = localMenuBuilder.withMenuItemProvider(getClearMenuItemProvider());
        localMenuBuilder = localMenuBuilder.withMenuItemProvider(getCountMenuItemProvider());

        /* Add row and column header toggle menu items. The titles of these change based
         * upon current header state in the table.
         */
        localMenuBuilder = localMenuBuilder.withMenuItemProvider(getToggleRowHeaderItemProvider());
        localMenuBuilder = localMenuBuilder.withMenuItemProvider(getToggleColumnHeaderItemProvider());

        /* Here ends the menu items that do not require a row selection. */
        localMenuBuilder = localMenuBuilder.withSeparator();

        /* Add the view details menu item, enabled if one and only one row is selected */
        localMenuBuilder = localMenuBuilder.withMenuItemProvider(DETAILS_MENU_KEY, getViewDetailsMenuItemProvider())
                .withEnabledState(
                        DETAILS_MENU_KEY,
                        new IMenuItemState() {

                            @Override
                            public boolean isActive(final NatEventData natEventData) {
                                return CommonNatTableMenuConfiguration.this.tableViewComposite.getSelectedItems().size() == 1;
                            }
                        });

        /* Add the mark menu item, enabled if any rows are selected. */
        localMenuBuilder = localMenuBuilder.withMenuItemProvider(MARK_MENU_KEY, getMarkMenuItemProvider())
                .withEnabledState(
                        MARK_MENU_KEY,
                        new IMenuItemState() {

                            @Override
                            public boolean isActive(final NatEventData natEventData) {
                                return CommonNatTableMenuConfiguration.this.tableViewComposite.getSelectedItems().size() > 0;
                            }
                        });

        /* Add the unmark menu item, enabled if any rows are selected. */
        localMenuBuilder = localMenuBuilder.withMenuItemProvider(UNMARK_MENU_KEY, getUnmarkMenuItemProvider())
                .withEnabledState(
                        UNMARK_MENU_KEY,
                        new IMenuItemState() {

                            @Override
                            public boolean isActive(final NatEventData natEventData) {
                                return CommonNatTableMenuConfiguration.this.tableViewComposite.getSelectedItems().size() > 0;
                            }
                        });

        /* Add the copy menu item, enabled if any rows are selected. */
        localMenuBuilder = localMenuBuilder.withMenuItemProvider(COPY_MENU_KEY, getCopyMenuItemProvider())
                                           .withEnabledState(COPY_MENU_KEY, new IMenuItemState() {

                                               @Override
                                               public boolean isActive(final NatEventData natEventData) {
                                                   return CommonNatTableMenuConfiguration.this.tableViewComposite.getSelectedItems()
                                                                                                                 .size() > 0;
                                               }
                                           });

        /* Now request the subclass to add its specific menu items */      
        localMenuBuilder = tableViewComposite.addCustomBodyMenuItems(localMenuBuilder);

        /* Create the menu from the builder */
        this.localMenu = localMenuBuilder.build();


    }
    
    /**
     * Gets the menu builder used by this menu configuration.
     * 
     * @return PopupMenuBuilder
     */
    public PopupMenuBuilder getBuilder() {
        return this.localMenuBuilder;
    }

    /**
     * Set menu item names on the table view
     */
    public void setMenuItemNames() {
        if (pauseMenuItem != null) {
            pauseMenuItem.setText(tableViewComposite.isPaused() ? "Resume" : "Temporary Pause");
        }
    }
    
    
    /**
     * {@inheritDoc}
     * @see org.eclipse.nebula.widgets.nattable.config.IConfiguration#configureUiBindings(org.eclipse.nebula.widgets.nattable.ui.binding.UiBindingRegistry)
     */
    @Override
    public void configureUiBindings(
            final UiBindingRegistry uiBindingRegistry) {

        final MouseEventMatcher matcher = new MouseEventMatcher(SWT.NONE, GridRegion.BODY, MouseEventMatcher.RIGHT_BUTTON) {

            /**
             * Matches the conditions under which a mouse action takes place.
             * @param natTable the NAT table
             * @param event mouse event
             * @param regionLabels LabelStack for the cell clicked upon
             * @return always true
             */
            @Override
            public boolean matches(final NatTable natTable, final MouseEvent event, final LabelStack regionLabels) {
                /* We want this menu to display always, regardless of whether there are 
                 * rows in the table, as long as we are NOT in the column header region.
                 */
                if (regionLabels != null) {
                    return !regionLabels.getLabels().contains("COLUMN_HEADER");
                }
                return true;
            }

        };

        /* Enables the menu when the right button is clicked over the data rows */
        uiBindingRegistry.registerMouseDownBinding(matcher,
                new PopupMenuAction(this.localMenu));
    }

    /**
     * Creates the menu item provider for the mark menu item.
     * 
     * @return IMenuItemProvider
     */
    private IMenuItemProvider getMarkMenuItemProvider() {
        return new IMenuItemProvider() {

            /**
             * {@inheritDoc}
             * @see org.eclipse.nebula.widgets.nattable.ui.menu.IMenuItemProvider#addMenuItem(org.eclipse.nebula.widgets.nattable.NatTable, org.eclipse.swt.widgets.Menu)
             */
            @Override
            public void addMenuItem(final NatTable natTable,
                    final Menu popupMenu) {
                final MenuItem markMenuItem = new MenuItem(popupMenu, SWT.PUSH);
                markMenuItem.setText("Mark");

                markMenuItem.addSelectionListener(new SelectionAdapter() {
                    /**
                     * {@inheritDoc}
                     * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
                     */
                    @Override
                    public void widgetSelected(final SelectionEvent e) {

                        /* Does nothing if no rows selected */
                        if (tableViewComposite.getSelectedItems().isEmpty()) {
                            return;
                        }
                        /* Mark selected data items in the event list. */
                        for (final T selectedItem: tableViewComposite.getSelectedItems()) {
                            selectedItem.setMark(true);
                            tableViewComposite.updateListItem(selectedItem);
                            
                        }

                    }
                });
            }
        };
    }

    /**
     * Creates the menu item provider for the copy menu item.
     *
     * @return IMenuItemProvider
     */
    private IMenuItemProvider getCopyMenuItemProvider() {
        return new IMenuItemProvider() {

            /**
             * {@inheritDoc}
             * 
             * @see org.eclipse.nebula.widgets.nattable.ui.menu.IMenuItemProvider#addMenuItem(org.eclipse.nebula.widgets.nattable.NatTable,
             *      org.eclipse.swt.widgets.Menu)
             */
            @Override
            public void addMenuItem(final NatTable natTable, final Menu popupMenu) {
                final MenuItem copyMenuItem = new MenuItem(popupMenu, SWT.PUSH);
                copyMenuItem.setText("Copy");

                copyMenuItem.addSelectionListener(new SelectionAdapter() {
                    /**
                     * {@inheritDoc}
                     * 
                     * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
                     */
                    @Override
                    public void widgetSelected(final SelectionEvent e) {

                        /* Does nothing unless one or more row is selected */
                        if (tableViewComposite.getSelectedItems().size() < 1) {
                            return;
                        }

                        /*
                         * Get the detailed text for the selected data item and display it in
                         * a text window.
                         */
                        final StringBuilder textToCopy = new StringBuilder();
                        final List<? extends INatListItem> selectedItems = tableViewComposite.getSelectedItems();
                        for (int i = 0; i < selectedItems.size(); i++) {
                            if (i != 0) {
                                textToCopy.append("\n");
                            }
                            textToCopy.append(tableViewComposite.getText(selectedItems.get(i)));
                        }

                        final Object[] data = { textToCopy.toString() };
                        final Transfer[] transfers = { TextTransfer.getInstance() };
                        final Clipboard clip = new Clipboard(Display.getCurrent());
                        clip.setContents(data, transfers);
                        clip.dispose();

                    }

                });
            }
        };
    }

    /**
     * Creates the menu item provider for the unmark menu item.
     * 
     * @return IMenuItemProvider
     */
    private IMenuItemProvider getUnmarkMenuItemProvider() {
        return new IMenuItemProvider() {

            /**
             * {@inheritDoc}
             * @see org.eclipse.nebula.widgets.nattable.ui.menu.IMenuItemProvider#addMenuItem(org.eclipse.nebula.widgets.nattable.NatTable, org.eclipse.swt.widgets.Menu)
             */
            @Override
            public void addMenuItem(final NatTable natTable,
                    final Menu popupMenu) {
                final MenuItem unmarkMenuItem = new MenuItem(popupMenu, SWT.PUSH);
                unmarkMenuItem.setText("Unmark");

                unmarkMenuItem.addSelectionListener(new SelectionAdapter() {
                    /**
                     * {@inheritDoc}
                     * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
                     */
                    @Override
                    public void widgetSelected(final SelectionEvent e) {

                        /* Does nothing if no rows selected */
                        if (tableViewComposite.getSelectedItems().isEmpty()) {
                            return;
                        }

                        /* Unmark selected data items in the event list */ 
                        for (final T selectedItem: tableViewComposite.getSelectedItems()) {
                            selectedItem.setMark(false);
                            tableViewComposite.updateListItem(selectedItem);
                        }

                    }
                });
            }
        };
    }

    /**
     * Creates the menu item provider for the view details menu item.
     * 
     * @return IMenuItemProvider
     */
    private IMenuItemProvider getViewDetailsMenuItemProvider() {
        return new IMenuItemProvider() {

            /**
             * {@inheritDoc}
             * @see org.eclipse.nebula.widgets.nattable.ui.menu.IMenuItemProvider#addMenuItem(org.eclipse.nebula.widgets.nattable.NatTable, org.eclipse.swt.widgets.Menu)
             */
            @Override
            public void addMenuItem(final NatTable natTable,
                    final Menu popupMenu) {
                final MenuItem viewDetailsMenuItem = new MenuItem(popupMenu, SWT.PUSH);
                viewDetailsMenuItem.setText("View Details...");

                viewDetailsMenuItem.addSelectionListener(new SelectionAdapter() {
                    /**
                     * {@inheritDoc}
                     * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
                     */
                    @Override
                    public void widgetSelected(final SelectionEvent e) {

                        /* Does nothing unless 1 row is selected */
                        if (tableViewComposite.getSelectedItems().size() != 1) {
                            return;
                        }

                        /* Get the detailed text for the selected data item and display it in 
                         * a text window.
                         */
                        final String toDisplay = tableViewComposite.getSelectedItems().get(0).getRecordDetailString();
                        final String idForTitle = tableViewComposite.getSelectedItems().get(0).getRecordIdString();
                        final TextViewShell textShell = new TextViewShell(tableViewComposite.getMainControl().getShell(),
                                idForTitle, SWT.BORDER | SWT.V_SCROLL | SWT.WRAP, tracer);
                        textShell.setText(toDisplay);
                        textShell.open();

                    }

                });
            }
        };
    }

    /**
     * Creates the menu item provider for the preferences menu item.
     * 
     * @return IMenuItemProvider
     */
    protected IMenuItemProvider getPreferencesMenuItemProvider() {
        return new IMenuItemProvider() {

            /**
             * {@inheritDoc}
             * @see org.eclipse.nebula.widgets.nattable.ui.menu.IMenuItemProvider#addMenuItem(org.eclipse.nebula.widgets.nattable.NatTable, org.eclipse.swt.widgets.Menu)
             */
            @Override
            public void addMenuItem(final NatTable natTable,
                    final Menu popupMenu) {
                final MenuItem preferencesMenuItem = new MenuItem(popupMenu, SWT.PUSH);
                preferencesMenuItem.setText(PREFERENCES_MENU_TITLE);

                preferencesMenuItem.addSelectionListener(new SelectionAdapter() {
                    /**
                     * {@inheritDoc}
                     * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
                     */
                    @Override
                    public void widgetSelected(final SelectionEvent e) {
                        tableViewComposite.displayPreferences();
                    }

                });
            }
        };
    }

    /**
     * Creates the menu item provider for the pause/resume menu item.
     * 
     * @return IMenuItemProvider
     */
    private IMenuItemProvider getPauseMenuItemProvider() {
        return new IMenuItemProvider() {
          
            /**
             * {@inheritDoc}
             * @see org.eclipse.nebula.widgets.nattable.ui.menu.IMenuItemProvider#addMenuItem(org.eclipse.nebula.widgets.nattable.NatTable, org.eclipse.swt.widgets.Menu)
             */
            @Override
            public void addMenuItem(final NatTable natTable,
                    final Menu popupMenu) {
                pauseMenuItem = new MenuItem(popupMenu, SWT.PUSH);
                setMenuItemNames();

                pauseMenuItem.addSelectionListener(new SelectionAdapter() {
                    /**
                     * {@inheritDoc}
                     * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
                     */
                    @Override
                    public void widgetSelected(final SelectionEvent e) {
                        if (tableViewComposite.isPaused()) {
                            tableViewComposite.resume();
                        } else {
                            tableViewComposite.pause();
                        }

                    }
                });
            }
        };
    }


    /**
     * Creates the menu item provider for the count menu item.
     * 
     * @return IMenuItemProvider
     */
    private IMenuItemProvider getCountMenuItemProvider() {
        return new IMenuItemProvider() {

            private CountShell countShell;

            /**
             * {@inheritDoc}
             * @see org.eclipse.nebula.widgets.nattable.ui.menu.IMenuItemProvider#addMenuItem(org.eclipse.nebula.widgets.nattable.NatTable, org.eclipse.swt.widgets.Menu)
             */
            @Override
            public void addMenuItem(final NatTable natTable,
                    final Menu popupMenu) {
                final MenuItem countMenuItem = new MenuItem(popupMenu, SWT.PUSH);
                countMenuItem .setText("View Row Counts...");

                countMenuItem.addSelectionListener(new SelectionAdapter() {
                    /**
                     * {@inheritDoc}
                     * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
                     */
                    @Override
                    public void widgetSelected(final SelectionEvent e) {

                        /* If count shell already active, bring it forward. */
                        if (countShell != null) {
                            countShell.getShell().setActive();
                            return;
                        }

                        /* Create count shell and open it. It will populate itself by calling
                         * back to the table view.
                         */
                        countShell = new CountShell(tableViewComposite.getMainControl().getShell(), 
                                tableViewComposite, tableViewComposite.getViewConfig().getViewName());
                        countShell.getShell().addDisposeListener(new DisposeListener() {
                            /**
                             * {@inheritDoc}
                             * @see org.eclipse.swt.events.DisposeListener#widgetDisposed(org.eclipse.swt.events.DisposeEvent)
                             */
                            @Override
                            public void widgetDisposed(final DisposeEvent event) {
                                try {
                                    countShell = null;                              
                                } catch (final Exception e) {

                                } finally {
                                    countShell = null;
                                }
                            }
                        });
                        countShell.open();

                    }
                });
            }
        };
    }

    /**
     * Creates the menu item provider for the clear menu item.
     * 
     * @return IMenuItemProvider
     */
    private IMenuItemProvider getClearMenuItemProvider() {
        return new IMenuItemProvider() {

            /**
             * {@inheritDoc}
             * @see org.eclipse.nebula.widgets.nattable.ui.menu.IMenuItemProvider#addMenuItem(org.eclipse.nebula.widgets.nattable.NatTable, org.eclipse.swt.widgets.Menu)
             */
            @Override
            public void addMenuItem(final NatTable natTable,
                    final Menu popupMenu) {
                final MenuItem clearMenuItem = new MenuItem(popupMenu, SWT.PUSH);
                clearMenuItem.setText("Clear Data");

                clearMenuItem.addSelectionListener(new SelectionAdapter() {
                    /**
                     * {@inheritDoc}
                     * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
                     */
                    @Override
                    public void widgetSelected(final SelectionEvent e) {
                        tableViewComposite.clearView();

                    }
                });
            }
        };
    }


    /**
     * Creates the menu item provider for the toggle row header menu item.
     * 
     * @return IMenuItemProvider
     */
    private IMenuItemProvider getToggleRowHeaderItemProvider() {
        return new IMenuItemProvider() {

            /**
             * {@inheritDoc}
             * @see org.eclipse.nebula.widgets.nattable.ui.menu.IMenuItemProvider#addMenuItem(org.eclipse.nebula.widgets.nattable.NatTable, org.eclipse.swt.widgets.Menu)
             */
            @Override
            public void addMenuItem(final NatTable natTable,
                    final Menu popupMenu) {
                final MenuItem toggleHeaderMenuItem = new MenuItem(popupMenu, SWT.PUSH);
                toggleHeaderMenuItem.setText(tableViewComposite.getToggleRowHeaderText());

                toggleHeaderMenuItem.addSelectionListener(new SelectionAdapter() {
                    /**
                     * {@inheritDoc}
                     * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
                     */
                    @Override
                    public void widgetSelected(final SelectionEvent e) {
                        tableViewComposite.toggleRowHeader();

                        /* Reset menu item name based upon current state of the toggle. */
                        toggleHeaderMenuItem.setText(tableViewComposite.getToggleRowHeaderText());

                    }
                });
            }

        };
    }

    /**
     * Creates the menu item provider for the toggle column header menu item.
     * 
     * @return IMenuItemProvider
     */
    private IMenuItemProvider getToggleColumnHeaderItemProvider() {
        return new IMenuItemProvider() {

            /**
             * {@inheritDoc}
             * @see org.eclipse.nebula.widgets.nattable.ui.menu.IMenuItemProvider#addMenuItem(org.eclipse.nebula.widgets.nattable.NatTable, org.eclipse.swt.widgets.Menu)
             */
            @Override
            public void addMenuItem(final NatTable natTable,
                    final Menu popupMenu) {
                final MenuItem toggleHeaderMenuItem = new MenuItem(popupMenu, SWT.PUSH);
                toggleHeaderMenuItem.setText(tableViewComposite.getToggleColumnHeaderText());

                toggleHeaderMenuItem.addSelectionListener(new SelectionAdapter() {
                    /**
                     * {@inheritDoc}
                     * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
                     */
                    @Override
                    public void widgetSelected(final SelectionEvent e) {
                        tableViewComposite.toggleColumnHeader();
                        toggleHeaderMenuItem.setText(tableViewComposite.getToggleColumnHeaderText());

                    }
                });
            }
        };
    }

}