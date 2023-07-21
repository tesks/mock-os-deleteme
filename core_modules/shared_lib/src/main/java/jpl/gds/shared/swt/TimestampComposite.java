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

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import jpl.gds.shared.swt.types.ChillColor;
import jpl.gds.shared.time.AccurateDateTime;

/**
 * TimestampComposite implements a GUI container that allows the entry and/or
 * display of a timestamp in YYYY-MM-DD hh:mm:ss format.
 * 
 *
 */
public class TimestampComposite {
    private static final String FORMAT = "YYYY-MM-DD hh:mm:ss";

    private Composite timeComp;
    private Text year;
    private Text month;
    private Text day;
    private Text hours;
    private Text minutes;
    private Text seconds;

    private final Composite parent;

    /**
     * Creates an instance of TimestampComposite.
     * 
     * @param parent parent Composite for this one
     */
    public TimestampComposite(final Composite parent) {
        this.parent = parent;
        createControls();
    }

    /**
     * Clears all fields.
     */
    public void clear() {
        this.year.setText("");
        this.month.setText("");
        this.day.setText("");
        this.hours.setText("");
        this.minutes.setText("");
        this.seconds.setText("");
    }

    /**
     * Creates the GUI widgets.
     * 
     */
    protected void createControls() {
        this.timeComp = new Composite(this.parent, SWT.NONE);
        final FormLayout compLayout = new FormLayout();
        compLayout.spacing = 0;
        compLayout.marginHeight = 5;
        compLayout.marginWidth = 0;
        this.timeComp.setLayout(compLayout);

        final Color grey = ChillColorCreator.getColor(new ChillColor(
                ChillColor.ColorName.DARK_GREY));

        final Label yearLabel = new Label(this.timeComp, SWT.LEFT);
        yearLabel.setText("");
        final FormData yearLabelFd = new FormData();
        yearLabelFd.top = new FormAttachment(0);
        yearLabelFd.left = new FormAttachment(0);
        yearLabel.setLayoutData(yearLabelFd);

        this.year = new Text(this.timeComp, SWT.SINGLE | SWT.BORDER);
        this.year.setTextLimit(4);
        final FormData yearFd = SWTUtilities.getFormData(this.year, 1, 4);
        yearFd.left = new FormAttachment(yearLabel);
        yearFd.top = new FormAttachment(yearLabel, 0, SWT.CENTER);
        this.year.setLayoutData(yearFd);

        final Label monthLabel = new Label(this.timeComp, SWT.SINGLE);
        monthLabel.setText("-");
        final FormData monthLabelFd = new FormData();
        monthLabelFd.left = new FormAttachment(this.year);
        monthLabelFd.top = new FormAttachment(yearLabel, 0, SWT.CENTER);
        monthLabel.setLayoutData(monthLabelFd);

        this.month = new Text(this.timeComp, SWT.SINGLE | SWT.BORDER);
        this.month.setTextLimit(2);
        final FormData monthFd = SWTUtilities.getFormData(this.month, 1, 2);
        monthFd.left = new FormAttachment(monthLabel);
        monthFd.top = new FormAttachment(yearLabel, 0, SWT.CENTER);
        this.month.setLayoutData(monthFd);

        final Label dayLabel = new Label(this.timeComp, SWT.SINGLE);
        dayLabel.setText("-");
        final FormData dayLabelFd = new FormData();
        dayLabelFd.left = new FormAttachment(this.month);
        dayLabelFd.top = new FormAttachment(yearLabel, 0, SWT.CENTER);
        dayLabel.setLayoutData(dayLabelFd);

        this.day = new Text(this.timeComp, SWT.SINGLE | SWT.BORDER);
        this.day.setTextLimit(2);
        final FormData dayFd = SWTUtilities.getFormData(this.day, 1, 2);
        dayFd.left = new FormAttachment(dayLabel);
        dayFd.top = new FormAttachment(yearLabel, 0, SWT.CENTER);
        this.day.setLayoutData(dayFd);

        final Label hourLabel = new Label(this.timeComp, SWT.SINGLE);
        hourLabel.setText(" ");
        final FormData hourLabelFd = new FormData();
        hourLabelFd.left = new FormAttachment(this.day);
        hourLabelFd.top = new FormAttachment(yearLabel, 0, SWT.CENTER);
        hourLabel.setLayoutData(hourLabelFd);

        this.hours = new Text(this.timeComp, SWT.SINGLE | SWT.BORDER);
        this.hours.setTextLimit(2);
        final FormData hoursFd = SWTUtilities.getFormData(this.hours, 1, 2);
        hoursFd.left = new FormAttachment(hourLabel);
        hoursFd.top = new FormAttachment(yearLabel, 0, SWT.CENTER);
        this.hours.setLayoutData(hoursFd);

        final Label minLabel = new Label(this.timeComp, SWT.SINGLE);
        minLabel.setText(":");
        final FormData minLabelFd = new FormData();
        minLabelFd.left = new FormAttachment(this.hours);
        minLabelFd.top = new FormAttachment(yearLabel, 0, SWT.CENTER);
        minLabel.setLayoutData(minLabelFd);

        this.minutes = new Text(this.timeComp, SWT.SINGLE | SWT.BORDER);
        this.minutes.setTextLimit(2);
        final FormData minutesFd = SWTUtilities.getFormData(
                this.minutes, 1, 2);
        minutesFd.left = new FormAttachment(minLabel);
        minutesFd.top = new FormAttachment(yearLabel, 0, SWT.CENTER);
        this.minutes.setLayoutData(minutesFd);

        final Label secLabel = new Label(this.timeComp, SWT.SINGLE);
        secLabel.setText(":");
        final FormData secLabelFd = new FormData();
        secLabelFd.left = new FormAttachment(this.minutes);
        secLabelFd.top = new FormAttachment(yearLabel, 0, SWT.CENTER);
        secLabel.setLayoutData(secLabelFd);

        this.seconds = new Text(this.timeComp, SWT.SINGLE | SWT.BORDER);
        this.seconds.setTextLimit(2);
        final FormData secondsFd = SWTUtilities.getFormData(
                this.seconds, 1, 2);
        secondsFd.left = new FormAttachment(secLabel);
        secondsFd.top = new FormAttachment(yearLabel, 0, SWT.CENTER);
        this.seconds.setLayoutData(secondsFd);

        final Label formatLabel = new Label(this.timeComp, SWT.SINGLE);
        formatLabel.setText("(" + FORMAT + ")");
        formatLabel.setForeground(grey);
        final FormData formatLabelFd = new FormData();
        formatLabelFd.left = new FormAttachment(this.seconds);
        formatLabelFd.top = new FormAttachment(yearLabel, 0, SWT.CENTER);
        formatLabel.setLayoutData(formatLabelFd);
    }

    /**
     * Gets the actual GUI composite displayed by this class.
     * 
     * @return the Composite object
     */
    public Composite getComposite() {
        return this.timeComp;
    }

    /**
     * Gets the integer value of a Text widget's contents.
     * 
     * @param field
     *            the Text field
     * @return the integer value; if the field is empty, the return value will
     *         be null.
     */
    protected int getIntFromField(final Text field) {
        if (field.getText().trim().equals("")) {
            return 0;
        }
        return Integer.parseInt(field.getText());
    }

    /**
     * Gets the maximum possible Date value, regardless of current field 
     * values.
     * 
     * @return the maximum Date
     */
    public Date getMaxTime() {
        return new AccurateDateTime(Long.MAX_VALUE);
    }

    /**
     * Gets the minimum possible Date value, regardless of current field 
     * values.
     * 
     * @return the minimum Date
     */
    public Date getMinTime() {
        return new AccurateDateTime(0);
    }

    /**
     * Gets the Timestamp reflecting the current values entered in the time 
     * text fields.
     * 
     * @return the Timestamp; if none of the fields have values, then the value
     *         of new Timestamp(0) is returned.
     */
    public Date getTime() {
        final int iyear = getIntFromField(this.year);
        final int imonth = getIntFromField(this.month) - 1;
        final int iday = getIntFromField(this.day);
        final int ihour = getIntFromField(this.hours);
        final int imin = getIntFromField(this.minutes);
        final int isec = getIntFromField(this.seconds);
        final Calendar c = new GregorianCalendar(iyear, imonth, iday, ihour,
                imin, isec);
        final Date t = new Date(c.getTimeInMillis());
        return t;
    }

    /**
     * Indicates if all time text fields are 0 or empty.
     * 
     * @return true if all fields are empty; false otherwise
     */
    public boolean isEmpty() {
        return validateAllZero();
    }

    /**
     * Enables or disables text entry in all the time entry fields.
     * 
     * @param enable
     *            true to enable; false to disable
     */
    public void setEnabled(final boolean enable) {
        this.year.setEnabled(enable);
        this.month.setEnabled(enable);
        this.day.setEnabled(enable);
        this.hours.setEnabled(enable);
        this.minutes.setEnabled(enable);
        this.seconds.setEnabled(enable);
    }

    /**
     * Sets a Text widget's value from an integer, left-padding to the given
     * number of places.
     * 
     * @param field
     *            the Text field to update
     * @param val
     *            the integer value to set
     * @param places
     *            the requested length of the field value; the integer value
     *            will be left-padded to this length with zeros.
     */
    protected void setFieldFromInt(final Text field, final int val,
            final int places) {
        String valStr = String.valueOf(val);
        while (valStr.length() < places) {
            valStr = "0" + valStr;
        }
        field.setText(valStr);
    }

    /**
     * Sets the time fields from the given Date
     * 
     * @param set
     *            the Date to set
     */
    public void setTime(final Date set) {
        final GregorianCalendar c = new GregorianCalendar();
        c.setTime(new Date(set.getTime()));
        setFieldFromInt(this.year, c.get(Calendar.YEAR), 4);
        setFieldFromInt(this.month, c.get(Calendar.MONTH) + 1, 2);
        setFieldFromInt(this.day, c.get(Calendar.DAY_OF_MONTH), 2);
        setFieldFromInt(this.hours, c.get(Calendar.HOUR_OF_DAY), 2);
        setFieldFromInt(this.minutes, c.get(Calendar.MINUTE), 2);
        setFieldFromInt(this.seconds, c.get(Calendar.SECOND), 2);
    }

    /**
     * Validates the currently entered time.
     * 
     * @return true if the time is valid; false otherwise
     */
    public boolean validate() {
        return validateAllZero() || validateInt(this.year, 0000, 9999)
                && validateInt(this.month, 1, 12)
                && validateInt(this.day, 1, 31)
                && validateInt(this.hours, 0, 23)
                && validateInt(this.minutes, 0, 59)
                && validateInt(this.seconds, 0, 59) && validateDayOfMonth();
    }

    /**
     * Determines if all of the time fields are empty or 0.
     * 
     * @return true if all fields are empty; false otherwise
     */
    protected boolean validateAllZero() {
        try {
            final int iyear = getIntFromField(this.year);
            final int imonth = getIntFromField(this.month);
            final int iday = getIntFromField(this.day);
            final int ihour = getIntFromField(this.hours);
            final int imin = getIntFromField(this.minutes);
            final int isec = getIntFromField(this.seconds);
            return iyear == 0 && imonth == 0 && iday == 0 && ihour == 0
                    && imin == 0 && isec == 0;
        } catch (final NumberFormatException e) {
            return false;
        }
    }

    /**
     * Validates that the current time has a day of month that is valid for the
     * selected month.
     * 
     * @return true if the day of month is valid; false otherwise
     */
    protected boolean validateDayOfMonth() {
        try {
            final int iyear = getIntFromField(this.year);
            final int imonth = getIntFromField(this.month) - 1;
            final int iday = getIntFromField(this.day);
            final Calendar c = new GregorianCalendar(iyear, imonth, iday);
            c.setLenient(false);
            c.set(Calendar.DAY_OF_MONTH, iday);
        } catch (final NumberFormatException e) {
            return false;
        } catch (final IllegalArgumentException e) {
            return false;
        }
        return true;
    }

    /**
     * Validates that the integer value of a Text widget's contents is within a
     * certain range.
     * 
     * @param field
     *            the Text widget
     * @param min
     *            the minimum allowed integer value for the field
     * @param max
     *            the maximum allowed integer value for the field
     * @return true if the field value is valid; false otherwise
     */
    protected boolean validateInt(
            final Text field, final int min, final int max) {
        try {
            final int val = getIntFromField(field);
            if (val < min || val > max) {
                return false;
            }
        } catch (final NumberFormatException e) {
            return false;
        }

        return true;
    }
}
