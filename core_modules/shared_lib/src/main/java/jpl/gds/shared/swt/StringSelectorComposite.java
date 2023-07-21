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
package jpl.gds.shared.swt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Text;

/**
 * This GUI class presents a searchable list of strings from which users can
 * select a specific string.
 * 
 */
public class StringSelectorComposite {

    private final Composite parent;
    private Composite mainComposite;
    private List stringList;
    private int numStrings = 15;
    private boolean selectMultiple = true;
    private Label searchLabel;
    private Text searchText;

    private SortedSet<String> idList;

    /**
     * Constructor.
     * 
     * @param parent
     *            the parent composite
     * @param chans
     *            the list of strings to present for selection
     */
    public StringSelectorComposite(final Composite parent,
            final SortedSet<String> chans) {
        this.parent = parent;
        idList = chans;
      
        createGUI();
    }

    /**
     * Constructor that allows multiple selection and
     * maximum display lines options. 
     * 
     * @param parent
     *            the parent composite
     * @param strList
     *            the list of strings to present for selection
     * @param numOnDisplay
     *            the number of strings to be displayed in the selector at a
     *            time
     * @param selectMultiple 
     *            true if user is permitted to select more than 1 
     *            item in the list
     */
    public StringSelectorComposite(final Composite parent,
            final String[] strList, final int numOnDisplay,
            final boolean selectMultiple) {
        this.parent = parent;
        idList = new TreeSet<String>(Arrays.asList(strList));
        this.numStrings = numOnDisplay;
        this.selectMultiple = selectMultiple;
        createGUI();
    }

    /**
     * Clears the currently selected strings.
     */
    public void clearSelection() {
        if (stringList != null) {
            stringList.setSelection(new int[0]);
        }
    }

    private void createGUI() {
        mainComposite = new Composite(parent, SWT.NONE);
        final FormLayout fl = new FormLayout();
        fl.spacing = 5;
        fl.marginHeight = 2;
        fl.marginWidth = 2;
        mainComposite.setLayout(fl);
        searchLabel = new Label(mainComposite, SWT.NONE);
        searchLabel.setText("Search: ");
        final FormData fd = new FormData();
        fd.top = new FormAttachment(0, 5);
        fd.left = new FormAttachment(0);
        searchLabel.setLayoutData(fd);

        searchText = new Text(mainComposite, SWT.BORDER);
        FormData rd = SWTUtilities.getFormData(searchText, 1, 12);
        rd.left = new FormAttachment(searchLabel);
        rd.top = new FormAttachment(searchLabel, 0, SWT.CENTER);
        searchText.setLayoutData(rd);

        stringList = new List(mainComposite, SWT.MULTI | SWT.V_SCROLL
                | SWT.BORDER);
        rd = SWTUtilities.getFormData(stringList, numStrings, 20);
        rd.left = new FormAttachment(0);
        rd.top = new FormAttachment(searchLabel, 0, 5);
        stringList.setLayoutData(rd);
        stringList.setItems(idList.toArray(new String[] {}));

        searchText.addModifyListener(new ModifyListener() {
            boolean emptyTextModify = false;

            @Override
            public void modifyText(final ModifyEvent arg0) {
                try {
                    if (searchText.getText().length() < 1) {
                        if (emptyTextModify == false) {
                            stringList.setItems(idList.toArray(new String[] {}));
                            emptyTextModify = true;
                        } else {
                            return;
                        }
                    } else {

                        final ArrayList<String> valList = 
                            new ArrayList<String>();
                        for (String id: idList) {
                            if (meetsFilters(id, 
                                    searchText.getText().trim())) {
                                valList.add(id);
                            }
                        }
                        String[] filteredVals = new String[valList.size()];
                        filteredVals = valList.toArray(filteredVals);
                        stringList.setItems(filteredVals);

                        emptyTextModify = false;
                    }
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Gets the main composite for layout purposes.
     * 
     * @return Composite object
     */
    public Composite getComposite() {
        return mainComposite;
    }

    /**
     * Retrieves the strings that were selected by the user.
     * 
     * @return list of strings currently selected in the selector (1 or more)
     */
    public String[] getSelectedStrings() {
        if (stringList.getSelectionCount() == 0) {
            return null;
        } else {
            return stringList.getSelection();
        }
    }

    /**
     * Retrieves the string that was selected by the user (only 1 can be
     * selected)
     * 
     * @return string currently selected in the selector (only 1)
     */
    public String getSingleSelectedString() {
        if (stringList.getSelectionCount() == 0) {
            return null;
        } else if ((!selectMultiple) && (stringList.getSelectionCount() != 1)) {
            return null;
        } else {
            return stringList.getItem(stringList.getSelectionIndex());
        }
    }

    private boolean meetsFilters(final String chanId, 
            final String filterText) {
        return chanId.startsWith(filterText);
    }

    /**
     * Sets the background color for widgets to match the background of the
     * composite
     * 
     * @param color
     *            is the background color that will be set for the items in the
     *            composite
     */
    public void setBackground(final Color color) {
        mainComposite.setBackground(color);
        searchLabel.setBackground(color);
    }

    /**
     * Fills the search text field with the string that is currently selected
     * in the search list.
     */
    public void setStringInSearchField() {
        final int index = stringList.getSelectionIndex();
        if (index != -1) {
            searchText.setText(stringList.getSelection()[0]);
            stringList.select(0);
        }
    }

    /**
     * Sets the strings in the selection list.
     * 
     * @param strs
     *            the strings to set
     */
    public void setStrings(final String[] strs) {
        //store a copy of object instead of reference to avoid unchecked 
        //changes to internal object
        idList = new TreeSet<String>(Arrays.asList(strs));
        if (stringList != null) {
            stringList.setItems(idList.toArray(new String[] {}));
        }
    }

    /**
     * Selects a single string in the selection list.
     * 
     * @param str
     *            the string to select
     */
    public void setSingleString(final String str) {
        stringList.select(stringList.indexOf(str));
    }

}
