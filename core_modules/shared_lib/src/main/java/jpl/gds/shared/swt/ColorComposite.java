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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.swt.types.ChillColor;

/**
 * This class represents a GUI composite that can be used to configure a color.
 * It uses ChillColor to represent the configured color.
 * 
 *
 */
public class ColorComposite {


    private ChillColor currentColor;
    private Composite mainComposite;
    private final Composite parent;
    private Color color;
    private Text colorLabel;
    private final String label;
    private final Tracer              trace;

    private PanelChangeListener panelListener;

    /**
     * Creates a ColorComposite with the given parent and display label.
     * 
     * @param parent
     *            the parent Composite widget
     * @param label
     *            the text to display as the color label
     * @param trace
     *            Tracer logger
     */
    public ColorComposite(final Composite parent, final String label, final Tracer trace) {
        this.parent = parent;
        this.label = label;
        this.trace = trace;
        createControls();
    }

    private void createControls() {

        mainComposite = new Composite(parent, SWT.NONE);
        mainComposite.setLayout(new GridLayout(3, false));

        color = new Color(parent.getDisplay(), new RGB(0, 0, 0));

        final Label nameLabel = new Label(mainComposite, SWT.NONE);
        nameLabel.setText(label + ": ");

        // Use a label full of spaces to show the color
        colorLabel = new Text(mainComposite, SWT.BORDER | SWT.READ_ONLY);
        colorLabel.setText("       ");
        
        /* setting background color no longer works under RedHat.
         * Now have to set the color swatches using this paint listener.
         */
        colorLabel.addListener(SWT.Paint, new Listener()
        {
            @Override
            public void handleEvent(final Event e)
            {
                    final GC gc = e.gc;

                    final String text = colorLabel.getText();
                    final Rectangle bounds = colorLabel.getBounds();

                    gc.setBackground(color);
                    gc.setForeground(color);
                    gc.fillRectangle(0, 0, bounds.width, bounds.height);
                    gc.drawText(text, 3, 2);
                
            }
        });
        

        final Button button = new Button(mainComposite, SWT.PUSH);
        button.setText("Color...");
        button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent event) {
                try {
                    // Create the color-change dialog
                    final ColorDialog dlg = new ColorDialog(mainComposite
                            .getShell());

                    // Set the selected color in the dialog from
                    // user's selected color
                    dlg.setRGB(colorLabel.getBackground().getRGB());

                    // Change the title bar text
                    dlg.setText("Choose a Color");

                    // Open the dialog and retrieve the selected color
                    final RGB rgb = dlg.open();
                    if (rgb != null) {
                        // Dispose the old color, create the
                        // new one, and set into the label
                        color.dispose();
                        color = null;
                        color = new Color(parent.getDisplay(), rgb);
                        /* setting background color no longer works under RedHat.
                         * Now have to set the color swatches using the paint listener, so all we do here
                         * is redraw.
                         */
                        colorLabel.redraw();
                        currentColor = new ChillColor(rgb.red, rgb.green,
                                rgb.blue);
                        if (panelListener != null) {
                            panelListener.panelDataChanged();
                        }
                    }
                } catch (final Exception eE) {
                    trace.error(
                                    "Color... button caught an unhandled " +
                                    "and unexpected Exception");
                    eE.printStackTrace();
                }
            }
        });
    }

    /**
     * Gets the main Composite object.
     * 
     * @return Composite object
     */
    public Composite getComposite() {
        return mainComposite;
    }

    /**
     * Gets the color that is currently selected in the composite
     * 
     * @return Returns the current color selected by the user.
     */
    public ChillColor getCurrentColor() {
        return currentColor;
    }

    /**
     * Sets the current color field in the composite.
     * 
     * @param currentColor
     *            The ChillColor to set.
     */
    public void setCurrentColor(final ChillColor currentColor) {
        this.currentColor = currentColor;
        color = new Color(parent.getDisplay(), new RGB(currentColor.getRed(),
                currentColor.getGreen(), currentColor.getBlue()));
        if (colorLabel != null && !colorLabel.isDisposed()) {
            colorLabel.setBackground(color);
        }
    }

    /**
     * Sets the PanelChangeListener for this composite.
     * 
     * @param l
     *            the listener to set
     */
    public void setPanelChangeListener(final PanelChangeListener l) {
        panelListener = l;
    }

}
