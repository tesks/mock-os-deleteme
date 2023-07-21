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

import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.string.StringUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.widgets.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static jpl.gds.shared.exceptions.ExceptionTools.rollUpMessages;

/**
 * This class contains general user interface utility methods for use in SWT
 * GUIs.
 * 
 */
public class SWTUtilities {
    /**
     * Class to wrap the run method of a Runnable and make sure that exceptions
     * are trapped.
     */
    private static class SafeRunnable extends Object implements Runnable {
        public static Tracer _trace;
        private final String _where;
        private final Runnable _runner;

        /**
         * Sets the member variables
         * 
         * @param trace used to log various levels of information
         * @param where is description of where exception occurred
         * @param runner object that will execute code while it is active
         */
        public SafeRunnable(final Tracer trace, final String where,
                final Runnable runner) {
            super();

            _trace = trace;
            _where = where;
            _runner = runner;
        }

        /**
         * Run method that traps exceptions.
         */
        @Override
		public void run() {
            try {
                _runner.run();
            } catch (final Exception e) {
                _trace.error("SafeRunnable error: " + _where + ": "
                        + rollUpMessages(e));

                e.printStackTrace();
            }
        }

         /**
         * {@inheritDoc}
         */
        @Override
		public String toString() {
            return _runner.toString();
        }
    }

    private static final int LINUX_EXTRA_HEIGHT = 10;
    private static final int LINUX_EXTRA_WIDTH = 10;

    /**
     * Vertically resizes a org.eclipse.swt.widgets.Label or
     * org.eclipse.swt.widgets.Text so that text can naturally wrap without
     * exceeding the display width limit.
     * 
     * @param control
     *            Label or Text object to resize
     */
    public static void adjustRowsForLineWrap(final Control control) {

        if (!(control instanceof Label) && !(control instanceof Text)) {
            throw new IllegalArgumentException(
                    "Only Label or Text can be passed to this method.");
        }

        GC gc = new GC(control);
        final FontMetrics fm = gc.getFontMetrics();

        // Find columns
        final int cols = (control.getBounds().width - LINUX_EXTRA_WIDTH)
                / fm.getAverageCharWidth(); // reverse arithmetic of
                                            // getFormData's

        gc.dispose();
        gc = null;

        if (cols <= 0) {
            // control's current bounds indicate that it's not ready to be
            // resized
            return;
        }

        // Find needed number of rows
        final String text = (control instanceof Label) ? ((Label) control)
                .getText() : ((Text) control).getText();

        if (text == null) {
            // no text, so nothing to resize for
            return;
        }

        final int approxRowsNeededForWrap = (text.length() / cols) + 1;
        // "approximate", as we're calculating based on average character 
        // width, so there's some guessing here

        // Re-adjust the object
        final FormData newFD = SWTUtilities.getFormData(control,
                approxRowsNeededForWrap, cols);
        newFD.left = ((FormData) control.getLayoutData()).left;
        newFD.right = ((FormData) control.getLayoutData()).right;
        newFD.top = ((FormData) control.getLayoutData()).top;
        control.setLayoutData(newFD);
    }

    /**
     * Locates the image file and creates an SWT Image from it.
     * 
     * @param device the SWT graphics device object being used for display
     * @param imageName
     *            the name of the image file, relative to classpath, or a path
     *            on the file system
     * 
     * @return the Image object, or null if the image could not be loaded
     * @throws UnsupportedOperationException if the image is in an unsupported format
     */
    public static Image createImage(
            final Device device, final String imageName) {
        try {

            final ClassLoader loader = SWTUtilities.class.getClassLoader();
            InputStream stream = loader.getResourceAsStream(imageName);

            if (stream == null) {
                String filename = imageName;
                filename = filename.replace('\\', '/');
                stream = loader.getResourceAsStream(filename);
            }
            if (stream == null) {
                String filename = imageName;
                filename = filename.replace('\\', '/');
                try {
                    stream = new FileInputStream(filename);
                } catch (final IOException e) {
                    stream = null;
                }
                if (stream == null) {
                    System.err.println(imageName + " not found");
                    return null;
                }
            }
            try {
               final Image result = new Image(device, stream);

               return result;
            } catch (final SWTException e) {
            	if (e.getMessage().indexOf("Unsupported or unrecognized format") != -1) {
            		throw new UnsupportedOperationException("Image file " + imageName + " is not in a supported image format");
            	}
            	else {
            		throw e;
            	}
            } finally {
            	stream.close();
            }
        } catch (final IOException e) {
            System.err.println("Error reading image file " + imageName);
            return null;
        } 
    }

    /**
     * Gets the average height of the text characters in the default font used
     * by the specified GC.
     * 
     * @param gc 
     *            the graphics context to get text width for
     * @return the width of one character in the Control's current font
     */
    public static int getFontCharacterHeight(final GC gc) {
        final FontMetrics fm = gc.getFontMetrics();
        final int height = fm.getHeight();
        return height;
    }

    /**
     * Gets the average width of the text characters in the default font used 
     * by the GC.
     * 
     * @param gc
     *            the graphics context to get text width for
     * @return the width of one character in the Control's current font
     */
    public static int getFontCharacterWidth(final GC gc) {
        final FontMetrics fm = gc.getFontMetrics();
        final int width = fm.getAverageCharWidth();
        return width;
    }

    /**
     * Gets a FormData layout object containing sizing information for the
     * Drawable control with the given number of rows and columns.
     * 
     * @param control
     *            the Drawable control to size
     * @param rows
     *            the desired number of Label rows
     * @param cols
     *            the desired number of Label columns
     * @return a FormData object containing proper size hints
     */
    public static FormData getFormData(final Drawable control, final int rows,
            final int cols) {
        if (control instanceof List) {
            return getFormData(control, rows, cols, ((List) control)
                    .getItemHeight());
        } else if (control instanceof Combo) {
            return getFormData(control, rows, cols, ((Combo) control)
                    .getItemHeight());
        } else if (control instanceof Text) {
            return getFormData(control, rows, cols, ((Text) control)
                    .getLineHeight());
        } else if (control instanceof CCombo) {
            return getFormData(control, rows, cols, ((CCombo) control)
                    .getItemHeight());
        } else {
            GC gc = new GC(control);
            final FontMetrics fm = gc.getFontMetrics();
            final int height = rows * fm.getHeight();
            final int width = cols * fm.getAverageCharWidth()
                    + LINUX_EXTRA_WIDTH;
            final FormData formData = new FormData(width, height);
            gc.dispose();
            gc = null;
            return formData;
        }
    }

    private static FormData getFormData(final Drawable control, final int rows,
            final int cols, final int lineHeight) {
        GC gc = new GC(control);
        final FontMetrics fm = gc.getFontMetrics();
        final int height = rows * lineHeight;
        final int width = cols * fm.getAverageCharWidth() + LINUX_EXTRA_WIDTH;
        final FormData formData = new FormData(width, height);
        gc.dispose();
        gc = null;
        return formData;
    }

    /**
     * Gets a GridData layout object containing sizing information for the
     * Drawable control with the given number of rows and columns.
     * 
     * @param control
     *            the Drawable control to size
     * @param rows
     *            the desired number of Label rows
     * @param cols
     *            the desired number of Label columns
     * @return a GridData object containing proper size hints
     */
    public static GridData getGridData(final Drawable control, final int rows,
            final int cols) {
        if (control instanceof List) {
            return getGridData(control, rows, cols, ((List) control)
                    .getItemHeight());
        } else if (control instanceof Combo) {
            return getGridData(control, rows, cols, ((Combo) control)
                    .getItemHeight());
        } else if (control instanceof Text) {
            return getGridData(control, rows, cols, ((Text) control)
                    .getLineHeight());
        } else if (control instanceof CCombo) {
            return getGridData(control, rows, cols, ((CCombo) control)
                    .getItemHeight());
        } else {
            GC gc = new GC(control);
            final FontMetrics fm = gc.getFontMetrics();
            gc.dispose();
            gc = null;
            final int height = rows * fm.getHeight();
            final int width = cols * fm.getAverageCharWidth()
                    + LINUX_EXTRA_WIDTH;
            final GridData gridData = new GridData(width, height);
            return gridData;
        }
    }

    private static GridData getGridData(final Drawable control, final int rows,
            final int cols, final int lineHeight) {
        GC gc = new GC(control);
        final FontMetrics fm = gc.getFontMetrics();
        gc.dispose();
        gc = null;
        final int height = rows * lineHeight;
        final int width = cols * fm.getAverageCharWidth() + LINUX_EXTRA_WIDTH;
        ;
        final GridData gridData = new GridData();
        gridData.heightHint = height;
        gridData.widthHint = width;
        return gridData;
    }

    /**
     * Get the approximate number of characters that will fit within the given
     * table column without changing the column width.
     * 
     * @param table
     *            the Table object to extract graphics context from
     * @param colIndex
     *            index of the column to check
     * 
     * @return number of characters that will fit within the column
     */
    public static int getNumCharacters(final Table table, final int colIndex) {
        GC gc = new GC(table);
        final FontMetrics fm = gc.getFontMetrics();
        final TableColumn col = table.getColumn(colIndex);
        final int pixelWidth = col.getWidth();
        gc.dispose();
        gc = null;
        return pixelWidth / fm.getAverageCharWidth();
    }

    /**
     * Gets a form layout with extra margin space
     * 
     * @return FormLayout with extra height
     */
    public static FormLayout getPaddedFormLayout() {
        final FormLayout fl = new FormLayout();

        fl.marginHeight = LINUX_EXTRA_HEIGHT;

        return fl;
    }

    /**
     * Get the pixel width for a string within a table.
     * 
     * @param table
     *            the table object to extract graphics context from
     * @param numChars
     *            number of characters
     * 
     * @return pixel width for string of numChars length
     */
    public static int getPixelWidth(
            final Composite table, final int numChars) {
        int width = 0;
        GC gc = new GC(table);
        final FontMetrics fm = gc.getFontMetrics();
        width = ((numChars + 2) * fm.getAverageCharWidth()) + 18;
        gc.dispose();
        gc = null;
        return width;
    }

    /**
     * Gets a RowData layout object containing sizing information for the Text
     * control with the given number of rows and columns.
     * 
     * @param textControl
     *            the Text control to size
     * @param rows
     *            the desired number of text rows
     * @param cols
     *            the desired number of text columns
     * @return a RowData object containing proper size hints
     */
    public static RowData getRowData(final Text textControl, final int rows,
            final int cols) {
        GC gc = new GC(textControl);
        final FontMetrics fm = gc.getFontMetrics();
        final int height = rows * textControl.getLineHeight();
        final int width = cols * fm.getAverageCharWidth() + LINUX_EXTRA_WIDTH;
        final RowData rowData = new RowData();
        rowData.height = height;
        rowData.width = width;
        gc.dispose();
        gc = null;
        return rowData;
    }

    /**
     * This is the way that the SWT book says you test whether you are in the 
     * UI thread
     * 
     * @return true if this method is called from the UI thread
     */
    public static boolean inDisplayThread() {
        return Display.getDefault().getThread() == Thread.currentThread();
    }

    /**
     * Resize all cells in a table such that each string fits completely within
     * its own cell. Note that this method is very time-consuming. Do not make 
     * large numbers of invocations.
     * 
     * @param table
     *            the SWT Table to justify
     */
    public static void justifyTable(final Table table) {
        // For each column, find the max char width and set table
        // width to that
        final TableColumn[] columns = table.getColumns();

        for (int column = 0; column < columns.length; column++) {
            columns[column].pack();
        }
        table.redraw();
    }

    /**
     * Will run the runnable in the UI thread. If there is a widget that is
     * involved that may get disposed, use
     * {@link #runInDisplayThread(Widget, Runnable)} instead.
     * 
     * If this is called from within the UI thread, it will be run immediately
     * otherwise it will be asyncExeced.
     * 
     * @param runnable
     *            the Object to run
     */
    public static void runInDisplayThread(final Runnable runnable) {
        final Display controlDisplay = Display.getDefault();
        if (Display.findDisplay(Thread.currentThread()) == controlDisplay) {
            runnable.run();
        } else {
            safeAsyncExec(controlDisplay, "runInDisplayThread", 
                    new Runnable() {
                @Override
				public void run() {
                    runnable.run();
                }

                @Override
                public String toString() {
                    return runnable.toString();
                }
            });
        }
    }

    /**
     * Will run the runnable in the UI thread if the given widget is not
     * disposed.
     * 
     * If this is called from within the UI thread, it will be run immediately
     * otherwise it will be asyncExeced.
     * 
     * @param widget
     *            the widget to check for disposal
     * @param runnable
     *            the Object to run
     */
    public static void runInDisplayThread(final Widget widget,
            final Runnable runnable) {
        if (widget == null || widget.isDisposed()) {
            return;
        }
        final Display controlDisplay = widget.getDisplay();
        if (Display.findDisplay(Thread.currentThread()) == controlDisplay) {
            runnable.run();
        } else {
            safeAsyncExec(
                    controlDisplay, "runInDisplayThread", new Runnable() {
                @Override
				public void run() {
                    try {
                        // check the widget for validity before running the
                        // posted runnable
                        if (!widget.isDisposed()) {
                            runnable.run();
                        }
                    } catch (final Exception eE) {
                    	SafeRunnable._trace.error(
                                        "controlDisplay widget caught " +
                                        "unhandled and unexpected " +
                                        "exception in SWTUtilities.java");
                        eE.printStackTrace();
                    }
                }

                @Override
                public String toString() {
                    return runnable.toString();
                }
            });
        }
    }

    /**
     * Run asyncExec, trapping and logging the exceptions.
     * 
     * @param display
     *            The Display in question
     * @param where
     *            A description of the functor
     * @param runner
     *            The functor to be run
     */
    public static void safeAsyncExec(final Display display, final String where,
            final Runnable runner) {
        if (display.isDisposed()) {
            return;
        }
        safeAsyncExec(display, SafeRunnable._trace, where, runner);
    }

    /**
     * Run asyncExec, trapping and logging the exceptions. This is done at two
     * levels, the asyncExec call itself, and the ultimate run method.
     * 
     * @param display
     *            The Display in question
     * @param trace
     *            The tracer for exception logging
     * @param where
     *            A description of the functor
     * @param runner
     *            The functor to be run
     */
    public static void safeAsyncExec(final Display display, final Tracer trace,
            final String where, final Runnable runner) {
        if (display.isDisposed()) {
            return;
        }
        final SafeRunnable proxy = new SafeRunnable(trace, where, runner);

        try {
            display.asyncExec(proxy);
        } catch (final Exception e) {
            trace.error("AsyncExec error: " + where + ": " + 
                    rollUpMessages(e));

            e.printStackTrace();
        }
    }
    
    /**
     * Run syncExec, trapping and logging the exceptions.
     * 
     * @param display
     *            The Display in question
     * @param where
     *            A description of the functor
     * @param runner
     *            The functor to be run
     */
    public static void safeSyncExec(final Display display, final String where,
            final Runnable runner) {
        if (display.isDisposed()) {
            return;
        }
        safeSyncExec(display, SafeRunnable._trace, where, runner);
    }

    /**
     * Run syncExec, trapping and logging the exceptions. This is done at two
     * levels, the asyncExec call itself, and the ultimate run method.
     * 
     * @param display
     *            The Display in question
     * @param trace
     *            The tracer for exception logging
     * @param where
     *            A description of the functor
     * @param runner
     *            The functor to be run
     */
    public static void safeSyncExec(final Display display, final Tracer trace,
            final String where, final Runnable runner) {
        if (display.isDisposed()) {
            return;
        }
        final SafeRunnable proxy = new SafeRunnable(trace, where, runner);

        try {
            display.syncExec(proxy);
        } catch (final Exception e) {
            trace.error("syncExec error: " + where + ": " + 
                    rollUpMessages(e));

            e.printStackTrace();
        }
    }

    /**
     * Convenience method to display a confirmation dialog. This dialog is
     * modal, so this method will block until the dialog is dismissed.
     * 
     * @param parent
     *            the parent Shell widget for the dialog
     * @param title
     *            a String to display in the dialog's title bar
     * @param message
     *            the error message to display in the dialog
     * @return true if the user confirmed, false otherwise
     */
    public static boolean showConfirmDialog(final Shell parent,
            final String title, final String message) {
        final MessageBox msgDialog = new MessageBox(parent, SWT.ICON_QUESTION
                | SWT.YES | SWT.NO);
        msgDialog.setText(title);
        msgDialog.setMessage(message);
        final int selection = msgDialog.open();
        return selection == SWT.YES;
    }

    /**
     * Convenience method to display an error dialog. This dialog is modal, so
     * this method will block until the dialog is dismissed.
     * 
     * @param parent
     *            the parent Shell widget for the dialog
     * @param title
     *            a String to display in the dialog's title bar
     * @param message
     *            the error message to display in the dialog
     */
    public static void showErrorDialog(final Shell parent, final String title,
            final String message) {
        if (parent.isDisposed()) {
            return;
        }
        final MessageBox errorDialog = new MessageBox(parent, SWT.ICON_ERROR
                | SWT.OK);
        errorDialog.setText(title);

        errorDialog.setMessage(StringUtil.safeTrim(message));

        errorDialog.open();
    }

    /**
     * Convenience method to display a message dialog. This dialog is modal, so
     * this method will block until the dialog is dismissed.
     * 
     * @param parent
     *            the parent Shell widget for the dialog
     * @param title
     *            a String to display in the dialog's title bar
     * @param message
     *            the error message to display in the dialog
     */
    public static void showMessageDialog(final Shell parent,
            final String title, final String message) {
        if (parent.isDisposed()) {
            return;
        }
        final MessageBox msgDialog = new MessageBox(parent,
                SWT.ICON_INFORMATION | SWT.OK);
        msgDialog.setText(title);
        msgDialog.setMessage(message);
        msgDialog.open();
    }
    
    

    /**
     * Convenience method to display a warning dialog. This dialog is modal, so
     * this method will block until the dialog is dismissed.
     * 
     * @param parent
     *            the parent Shell widget for the dialog
     * @param title
     *            a String to display in the dialog's title bar
     * @param message
     *            the error message to display in the dialog
     */
    public static void showWarningDialog(final Shell parent,
            final String title, final String message) {
        if (parent.isDisposed()) {
            return;
        }
        final MessageBox msgDialog = new MessageBox(parent, SWT.ICON_WARNING
                | SWT.OK);
        msgDialog.setText(title);
        msgDialog.setMessage(message);
        msgDialog.open();
    }

    private final Map<String, String> _lastSelection = 
        new HashMap<String, String>();

    private final String              _userDir       = GdsSystemProperties.getSystemProperty("user.dir");

    /**
     * See above. This overloading supplies no default and sets up for a file
     * save dialog.
     * 
     * @param parent
     *            the parent Shell
     * @param name
     *            the association name for the remembered directory
     * @param defaultDir
     *            the default directory path
     * @return the filename, or null if no file chosen
     */
    public String displayStickyDirSaver(final Shell parent, final String name,
            final String defaultDir) {
        return displayStickyFileChooser(true, parent, name, defaultDir, null,
                SWT.SAVE, null);
    }

    /**
     * See above. This overloading supplies no default.
     * 
     * @param dirOnly
     *            true if only directories should be selectable; false for 
     *            files
     * @param parent
     *            the parent Shell
     * @param name
     *            the association name for the remembered directory
     * 
     * @return the filename, or null if no file chosen
     */
    public String displayStickyFileChooser(final boolean dirOnly,
            final Shell parent, final String name) {
        return displayStickyFileChooser(dirOnly, parent, name, (String) null);
    }

    /**
     * See above. This overloading supplies no default.
     * 
     * @param dirOnly
     *            true if only directories should be selectable; false for 
     *            files
     * @param parent
     *            the parent Shell
     * @param name
     *            the association name for the remembered directory
     * @param defalt
     *            default directory if none set
     * 
     * @return the filename, or null if no file chosen
     */
    public String displayStickyFileChooser(final boolean dirOnly,
            final Shell parent, final String name, final String defalt) {
        return displayStickyFileChooser(dirOnly, parent, name, defalt, null,
                SWT.OPEN, null);
    }
    
    /**
     * See above. This overloading supplies no default.
     * 
     * @param dirOnly
     *            true if only directories should be selectable; false for 
     *            files
     * @param parent
     *            the parent Shell
     * @param name
     *            the association name for the remembered directory
     * @param defalt
     *            default directory if none set
     * 
     * @return the filename, or null if no file chosen
     */
    public String displayFileChooser(final boolean dirOnly,
            final Shell parent, final String name, final String defalt) {
        return displayFileChooser(dirOnly, parent, name, defalt, null,
                SWT.OPEN, null);
    }
    

    /**
     * Displays the file or directory chooser for the given type of operation
     * and returns the chosen filename or directory name, or null if none
     * chosen. The last directory/file chosen is remembered by this class and
     * used as the starting directory the next time this method is called.
     * 
     * Note that we do NOT use getFilterPath, as that does not return the full
     * path selected. Note also that setFilterPath even works for setting the
     * last selected file, not just the directory.
     * 
     * @param dirOnly
     *            true if only directories should be selectable; false for 
     *            files
     * @param parent
     *            the parent Shell
     * @param name
     *            the association name for the remembered directory
     * @param defaultDir
     *            default path
     * @param defaultFile
     *            default filename
     * @param style
     *            SWT style
     * 
     * @return the filename, or null if no file chosen
     */
    private String displayStickyFileChooser(final boolean dirOnly,
            final Shell parent, final String name, final String defaultDir,
            final String defaultFile, final int style, 
            final String[] extensions) {
        String filename = null;
        String previous = _lastSelection.get(name);

        if (previous == null) {
            // First time, make an entry for the name

            final String use_default = (defaultDir != null) ? defaultDir
                    : _userDir;

            _lastSelection.put(name, use_default);

            previous = use_default;
        }

        filename = displayFileChooser(dirOnly, parent, name, previous, defaultFile, style, extensions);

        if (filename != null) {
            _lastSelection.put(name, new File(filename).getParent());
        }

        return filename;
    }
    
    /**
     * Displays the file or directory chooser for the given type of operation
     * and returns the chosen filename or directory name, or null if none
     * chosen.
     * 
     * Note that we do NOT use getFilterPath, as that does not return the full
     * path selected. Note also that setFilterPath even works for setting the
     * last selected file, not just the directory.
     * 
     * @param dirOnly
     *            true if only directories should be selectable; false for 
     *            files
     * @param parent
     *            the parent Shell
     * @param name
     *            the association name for the remembered directory
     * @param defaultDir
     *            default path
     * @param defaultFile
     *            default filename
     * @param style
     *            SWT style
     * 
     * @return the filename, or null if no file chosen
     */
    private String displayFileChooser(final boolean dirOnly,
            final Shell parent, final String name, final String defaultDir,
            final String defaultFile, final int style, 
            final String[] extensions) {
        String filename = null;

        final String startDir = (defaultDir != null) ? defaultDir
                    : _userDir;

        if (dirOnly) {
            final DirectoryDialog dirBrowser = new DirectoryDialog(parent);

            dirBrowser.setText("Select Directory");

            dirBrowser.setFilterPath(startDir);
            filename = dirBrowser.open();
        } else {
            final FileDialog fileBrowser = new FileDialog(parent, style);

            fileBrowser.setText("Select File");

            if (defaultFile != null) {
                fileBrowser.setFileName(defaultFile);
            }
            if (extensions != null) {
                fileBrowser.setFilterExtensions(extensions);
            }

            fileBrowser.setFilterPath(startDir);

            filename = fileBrowser.open();
        }

        return filename;
    }

    /**
     * See above. This overloading allows specification of file extensions.
     * 
     * @param dirOnly
     *            true if only directories should be selectable; false for 
     *            files
     * @param parent
     *            the parent Shell
     * @param name
     *            the association name for the remembered directory
     * @param extensions
     *            an array of accepted file extensions
     * 
     * @return the filename, or null if no file chosen
     */
    public String displayStickyFileChooser(final boolean dirOnly,
            final Shell parent, final String name, final String[] extensions) {
        return displayStickyFileChooser(dirOnly, parent, name, null, null,
                SWT.OPEN, extensions);
    }

    /**
     * See above. This overloading supplies no default and sets up for a file
     * save dialog.
     * 
     * @param parent
     *            the parent Shell
     * @param name
     *            the association name for the remembered directory
     * @param defaultDir
     *            the default directory to save to
     * @param defaultFile
     *            the default file name to save to
     * @return the filename, or null if no file chosen
     */
    public String displayStickyFileSaver(final Shell parent, final String name,
            final String defaultDir, final String defaultFile) {
        return displayStickyFileChooser(false, parent, name, defaultDir,
                defaultFile, SWT.SAVE, null);
    }

    /**
     * Fake a mouse click on the given button object and thereby trigger any
     * associated listeners.
     * 
     * (NOTE: The button WILL NOT animate like it has been clicked)
     * 
     * @param display
     *            The high level display (probably the highest level parent in
     *            your app)
     * @param button
     *            The button that should be clicked
     */
    public void fakeButtonClick(final Display display, final Button button) {
        final Event clickEvent = new Event();
        clickEvent.data = null;
        clickEvent.detail = 0;
        clickEvent.display = display;
        clickEvent.doit = true;
        clickEvent.height = 1;
        clickEvent.item = button;
        clickEvent.stateMask = SWT.BUTTON1;
        clickEvent.text = null;
        clickEvent.time = 0;
        clickEvent.widget = button;
        clickEvent.width = 1;
        clickEvent.x = button.getLocation().x;
        clickEvent.y = button.getLocation().y;
        button.notifyListeners(SWT.Selection, clickEvent);
    }
    
    /**
     * Starts a thread that updates the given progress bar, using the given timer.
     * 
     * @param progressShell The ProgressBarShell to update
     * @param progressTimer the timer to control update
     * @param interval the interval for update, milliseconds
     * @param updateStep the incremental update value for the progress bar 
     */
    public static void startProgressBarUpdate(final ProgressBarShell progressShell, 
    		final Timer progressTimer, final long interval, final int updateStep) {
    	progressTimer.scheduleAtFixedRate(new TimerTask() {

    		@Override
    		public void run() {
    			SWTUtilities.runInDisplayThread(new Runnable() {
    				@Override
    				public void run() {
    					if (progressShell == null) {
    						return;
    					}
    					if (progressShell.getShell() == null || progressShell.getShell().isDisposed()) {
    						return;
    					}
    					if (progressShell.getProgressBar().getSelection() == 100) {
    						progressShell.getProgressBar().setSelection(0);
    					} else {
    						progressShell.getProgressBar().
    						setSelection(progressShell.getProgressBar().getSelection() + updateStep);
    					}
    				}
    			});
    		}
    	}, interval, interval);
    }

}
