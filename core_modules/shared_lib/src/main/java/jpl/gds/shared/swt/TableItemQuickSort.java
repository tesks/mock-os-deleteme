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

import java.io.Serializable;
import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;

import org.eclipse.swt.widgets.TableItem;

/**
 * A convenience class for quick sorting SWT Table Items. Keeps track of whether
 * anything actually changed so we can know whether to recreate the table items
 * in the GUI class that invokes this sort. This makes the class state
 * sensitive, so be aware.
 *
 *
 */
public class TableItemQuickSort
{
    /** Types of collator */
    public enum CollatorType
    {
        /** Character collator */
        CHARACTER,

        /** Numeric collator */
        NUMERIC;
    }

    private boolean swapOccured = false;

    /**
     * Constructor.
     */
    public TableItemQuickSort() {
        swapOccured = false;
    }


    /**
     * Get collator.
     *
     * @param type Collator type
     *
     * @return Collator
     */
    public static Comparator<?> getCollator(CollatorType type) {
        if (type.equals(CollatorType.CHARACTER)) {
            return Collator.getInstance ( Locale.getDefault() );
        } 
        else {
            return new TableItemQuickSort.NumericCollator();
        }
    }


    /**
     * Resets swap state. Swap state is set by each call to quickSort().
     */
    public synchronized void reset() {
        swapOccured = false;
    }

    /** 
     * Performs a quick sort on an array of SWT TableItems. This method is recursive.
     * The initial invocation should pass 0 as left boundary, and array length-1 as
     * right boundary. Sets the swap flag to indicate if anything actually changed
     * in the array.
     * 
     * @param a                 array to be sorted
     * @param left              left boundary of array partition
     * @param right             right boundary of array partition
     * @param ascending         True if ascending sort
     * @param columnBeingSorted Index of sort column in the TableItem
     * @param colType           Collator to use tl determine item ordering
     *
     */
    @SuppressWarnings("unchecked")
    public synchronized void quickSort ( TableItem a[], int left, int right, boolean ascending, int columnBeingSorted, CollatorType colType ) {
        Comparator<String> col = (Comparator<String>)getCollator(colType);

        int i,j,k;
        if( left < right)
        {
            k =  (left + right) >>> 1;
            String midString = a[k].getText ( columnBeingSorted );
            i = left;
            j = right;
            while(i <= j)
            {
                if (ascending) {
                    while((i < right) && (col.compare ( a [ i ].getText ( columnBeingSorted ), midString ) < 0)) {
                        i++;
                    }
                } else {
                    while((i < right) && (col.compare ( a [ i ].getText ( columnBeingSorted ), midString ) > 0)) {
                        i++;
                    }
                }
                if (ascending) {
                    while((j > left) && ( col.compare ( a [ j ].getText ( columnBeingSorted ), midString ) > 0)) {
                        j--;
                    }
                } else {
                    while((j > left) && ( col.compare ( a [ j ].getText ( columnBeingSorted ), midString ) < 0)) {
                        j--;
                    }
                }
                if (i <= j) {
                    swap(a, i, j);
                    i++;
                    j--;
                } 
            }

            // recursively sort the lesser list
            if (left < j) {
                quickSort(a,left,j, ascending, columnBeingSorted, colType);
            }
            if (i < right) {
                quickSort(a,i,right, ascending, columnBeingSorted, colType);
            }
        }

    }

    /**
     * Swaps two table items in the given array.
     * 
     * @param a the array of items
     * @param i first index
     * @param j second index
     */
    private void swap ( TableItem[] a, int i, int j ) {
        TableItem t;
        swapOccured = true;
        t = a [ i ];
        a [ i ] = a [ j ];
        a [ j ] = t;
    }


    /**
     * Get swapped state, indicating something was changed in the array order by the
     * last call to quickSort().
     *
     * @return True if swap occurred
     */
    public synchronized boolean wasSwapped() {
        return swapOccured;
    }

    /** 
     * Numeric comparator class for sorting 
     */
    public static class NumericCollator implements Comparator<String>, Serializable
    {
        private static final long serialVersionUID = 0L;

        /**
         * {@inheritDoc}
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         */
        @Override
        public int compare(String arg0, String arg1) {
            String firstVal = arg0;
            String secondVal = arg1;
            double firstNumVal = 0.0;
            double secondNumVal = 0.0;
            try {
                firstNumVal = Double.valueOf(firstVal);
            } catch (NumberFormatException e) {
                return -1;
            }
            try {
                secondNumVal = Double.valueOf(secondVal);
            } catch (NumberFormatException e) {
                return 1;
            }
            if (firstNumVal == secondNumVal) {
                return 0;
            } else if (firstNumVal < secondNumVal) {
                return -1;
            } else {
                return 1;	
            }
        }
    }
}
