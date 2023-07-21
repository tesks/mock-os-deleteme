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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;

import jpl.gds.shared.message.MessageRegistry;

/**
 * MessageFilterComposite is a composite that contains the 
 * preferences related to message filtering.
 */
public class MessageFilterComposite {

        /**
         * Array of allowed message types
         */
        protected String[] messageTypes;
        
        /**
         * Selected message types
         */
        protected String[] selectedTypes;
        
        /**
         * Array of checkbox buttons for selecting types of messages to filter on
         */
        protected Button[] typeButtons;
        
        /**
         * Button for unchecking all of the message type buttons
         */
        protected Button uncheckAll;
        
        /**
         * Button for checking all of the message type buttons
         */
        protected Button checkAll;
        
        /**
         * Group widget that contains the message type buttons
         */
        protected Group filterGroup;
        
        /**
         * Main message filter composite
         */
        protected Composite filterComp;
        
        /**
         * Parent shell
         */
        protected Shell parent;
        
        /**
         * Creates an instance of MessageFilterComposite.
         * @param parent the parent Shell for this composite
          */
        public MessageFilterComposite(final Shell parent) {
            this.parent = parent;
            this.selectedTypes = new String[0];
            this.filterComp = new Composite(parent, SWT.NONE);
            this.filterComp.setLayout(new RowLayout(SWT.VERTICAL));
        }
        
        /**
         * Gets the message types and creates the check box buttons for them
         * 
         * @param types array of message types
         */
        public void setAllowedMessageTypes(final String[] types) {
            if (types == null) {
                this.messageTypes = MessageRegistry.getAllSubscriptionTags(true).toArray(new String[] {});
            } else {
                this.messageTypes = types;
            }
            createMessageTypeButtons();
        }
        
        /**
         * Retrieves the top-level Composite (container) object.
         * @return the Composite object
         */
        public Composite getContainer() {
            return this.filterComp;
        }
        
        /**
         * Creates the message type/filter controls.
         */
        protected void createMessageTypeButtons() {

            this.filterGroup = new Group(this.filterComp, SWT.BORDER | SWT.SHADOW_ETCHED_IN);
            this.filterGroup.setText("Message Filtering");
            if (this.messageTypes == null) {
                return;
            }
            
            final int numTypes = this.messageTypes.length  + 1;
            final int rows = 7;
            int cols = numTypes / 7;
            if (rows * cols < numTypes) {
                cols++;
            }
            
            final GridLayout gl = new GridLayout(cols, false);
            this.filterGroup.setLayout(gl);
            final boolean all = this.selectedTypes == null;
            List<String> filters = null;
            if (!all) {
              filters = new ArrayList<>();
              for (int i = 0; i < this.selectedTypes.length; i++) {
                  filters.add(this.selectedTypes[i]);
              }
            }
            
            this.typeButtons = new Button[this.messageTypes.length];
            
            for (int i = 0; i < this.messageTypes.length; i++) {
                final String name = this.messageTypes[i];
                this.typeButtons[i] = new Button(this.filterGroup, SWT.CHECK);
                this.typeButtons[i].setText(name.trim());
                this.typeButtons[i].setSelection(all || filters.contains(this.messageTypes[i]));
            }
            
            final Composite buttonComp = new Composite(this.filterComp, SWT.NONE);
            buttonComp.setLayout(new RowLayout(SWT.HORIZONTAL));
            
            this.checkAll = new Button(buttonComp, SWT.PUSH);
            this.checkAll.setText("Check All");
            
            this.uncheckAll = new Button(buttonComp, SWT.PUSH);
            this.uncheckAll.setText("Uncheck All");
            
            this.uncheckAll.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
                    for (int i = 0; i < MessageFilterComposite.this.typeButtons.length; i++) {
                        MessageFilterComposite.this.typeButtons[i].setSelection(false);
                        MessageFilterComposite.this.typeButtons[i].setEnabled(true);
                    }
                }
            });
            
            this.checkAll.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
                    for (int i = 0; i < MessageFilterComposite.this.typeButtons.length; i++) {
                        MessageFilterComposite.this.typeButtons[i].setSelection(true);
                        MessageFilterComposite.this.typeButtons[i].setEnabled(true);
                    }
                }
            });
        }
             
        /**
         * Applies control contents and settings to the current selected levels object.
         * @return true always because this overrides a method that must return a boolean
         */
        public boolean applyChanges() {
            final List<String> filter = new LinkedList<>();
            for (int i = 0; i < this.typeButtons.length; i++) {
                if (this.typeButtons[i].getSelection()) {
                    filter.add(this.messageTypes[i]);
                }
            }
            this.selectedTypes = new String[filter.size()];
            filter.toArray(this.selectedTypes);
            return true;
        }
        
        /**
         * Returns the array of selected message types. Will return null
         * if all types were selected.
         * @return the array of message types
         */
        public String[] getSelectedTypes() {
            return this.selectedTypes;
        }
        
        /**
         * Sets the currently selected message types.
         * @param types the array of selected message types; a null value
         * means that all message types are selected; an empty array means none are
         */
        public void setSelectedTypes(final String[] types) {
            this.selectedTypes = types;
            final boolean all = this.selectedTypes == null;
            List<String> filters = null;
            if (!all) {
              filters = new ArrayList<>();
              for (int i = 0; i < this.selectedTypes.length; i++) {
                  filters.add(this.selectedTypes[i]);
              }
            }
            for (int i = 0; i < this.messageTypes.length; i++) {
                this.typeButtons[i].setEnabled(!all);
                this.typeButtons[i].setSelection(all || filters.contains(this.messageTypes[i]));
            }
        }
}
