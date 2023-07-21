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
package jpl.gds.monitor.canvas;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import jpl.gds.monitor.perspective.view.fixed.FixedFieldType;
import jpl.gds.shared.swt.types.CoordinateSystemType;

/**
 * This class is the mouse hover handler for the fixed layout canvas. It has two
 * modes: runtime and buildtime. The information displayed by hover is different
 * in these two modes.
 * 
 */
public class CanvasHoverHandler {
	private Shell hoverShell;
	private Label hoverText;
	private CanvasElement hoverWidget;
	private Point hoverPosition;
	private boolean buildtime;
	private final FixedCanvas canvasParent;

	/**
	 * Creates a new CanvasHoverHandler
	 * 
	 * @param parent the parent Shell
	 * @param canvasParent the parent Canvas
	 */
	public CanvasHoverHandler(
	        final Shell parent, final FixedCanvas canvasParent) {
		this.canvasParent = canvasParent;
		createGui(parent);
	}

	/**
	 * Indicates whether we are in runtime or buildtime mode. This affects the
	 * content of the hover shell.
	 * 
	 * @param enable true to enabled build mode, false to disable
	 */
	public void setBuildtime(final boolean enable) {
		buildtime = enable;

	}

	private void createGui(Shell parent) {

		final Display display = parent.getDisplay();

		hoverShell = new Shell(parent, SWT.ON_TOP | SWT.TOOL);
		hoverShell.setLayout(new FillLayout());

		hoverShell.setBackground(display
				.getSystemColor(SWT.COLOR_INFO_BACKGROUND));

		hoverText = new Label(hoverShell, SWT.NONE);
		hoverText.setForeground(display
				.getSystemColor(SWT.COLOR_INFO_FOREGROUND));
		hoverText.setBackground(display
				.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
	}

	/**
	 * Enables customized hover for the parent canvas
	 * 
	 */
	public void activateHover() {
		/*
		 * Get out of the way if we attempt to activate the control underneath
		 * the tooltip
		 */
		canvasParent.getCanvas().addMouseListener(new MouseAdapter() {
			/** 
			 * {@inheritDoc}
			 * @see
			 * org.eclipse.swt.events.MouseAdapter#mouseDown(org.eclipse.swt
			 * .events.MouseEvent)
			 */
			@Override
			public void mouseDown(final MouseEvent e) {
				if (hoverShell.isVisible()) {
					hoverShell.setVisible(false);
				}
			}
		});

		/*
		 * Trap hover events to pop-up
		 */
		canvasParent.getCanvas().addMouseTrackListener(
		        new MouseTrackAdapter() {
			/**
			 * {@inheritDoc}
			 * @see
			 * org.eclipse.swt.events.MouseTrackAdapter#mouseExit(org.eclipse
			 * .swt.events.MouseEvent)
			 */
			@Override
			public void mouseExit(final MouseEvent e) {
				if (hoverShell.isVisible()) {
					hoverShell.setVisible(false);
				}
				hoverWidget = null;
			}

			/**
			 * {@inheritDoc}
			 * @see
			 * org.eclipse.swt.events.MouseTrackAdapter#mouseEnter(org.eclipse
			 * .swt.events.MouseEvent)
			 */
			@Override
			public void mouseEnter(final MouseEvent e) {
				if (hoverShell.isVisible()) {
					hoverShell.setVisible(false);
				}
				hoverWidget = null;
			}

			/**
			 * {@inheritDoc}
			 * @see
			 * org.eclipse.swt.events.MouseTrackAdapter#mouseHover(org.eclipse
			 * .swt.events.MouseEvent)
			 */
			@Override
			public void mouseHover(final MouseEvent event) {
				Point pt = new Point(event.x, event.y);
				List<CanvasElement> matchedElems = canvasParent
				.getElementsForPoint(pt);

				CanvasElement bestMatch = null;

				// This is the case in which the mouse is hovering over an 
				// empty spot on the canvas. The hover text is just the 
				// current coordinate.
				if (matchedElems.size() == 0) {
					if (buildtime) {
						hoverPosition = canvasParent.getCanvas().toDisplay(pt);
						pt = mapToCoordinateSystem(pt);
						String text = null;
						CoordinateSystemType locType = 
						    canvasParent.getViewConfig().getCoordinateSystem();
						text = locType.toString() + 
						" {" + pt.x + "," + pt.y + "}";
						hoverText.setText(text != null ? text : "");
						hoverShell.pack();
						setHoverLocation(hoverShell, hoverPosition);
						hoverShell.setVisible(true);
						hoverWidget = null;
					}
					return;
				} else {
					bestMatch = canvasParent.selectBestMatch(matchedElems);
				}

				// Otherwise, there is a canvas element under the mouse. The 
				// hover text is the object type and the current coordinate
				hoverWidget = bestMatch;
				hoverPosition = canvasParent.getCanvas().toDisplay(pt);
				String text = "";
				if (buildtime) {
					pt = mapToCoordinateSystem(pt);
					if (hoverWidget.getFieldType().equals(
					        FixedFieldType.CHANNEL)) {
						text = hoverWidget.getFieldType().toString() + "(" +
						((ChannelElement)hoverWidget).getChannelId() + ") : {"
						+ pt.x + "," + pt.y + "}";
					} else {
						text = hoverWidget.getFieldType().toString() + ": {"
						+ pt.x + "," + pt.y + "}";
					}
				} else {
					return;
				}
				hoverText.setText(text);
				hoverShell.pack();
				setHoverLocation(hoverShell, hoverPosition);
				hoverShell.setVisible(true);
			}
		});
	}

	private Point mapToCoordinateSystem(Point pIn) {
		Point pOut = new Point(pIn.x, pIn.y);
		if (canvasParent.isCharacterLayout()) {
			pOut.x = pIn.x / canvasParent.getCharacterWidth();
			pOut.y = pIn.y / canvasParent.getCharacterHeight();
		}
		return pOut;
	}

	private void setHoverLocation(final Shell shell, final Point position) {
		Rectangle displayBounds = shell.getDisplay().getBounds();
		Rectangle shellBounds = shell.getBounds();
		shellBounds.x = Math.max(Math.min(position.x, displayBounds.width
				- shellBounds.width), 0);
		shellBounds.y = Math.max(Math.min(position.y + 16, displayBounds.height
				- shellBounds.height), 0);
		shell.setBounds(shellBounds);
	}
}
