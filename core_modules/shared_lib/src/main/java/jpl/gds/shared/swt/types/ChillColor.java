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
package jpl.gds.shared.swt.types;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;


/**
 * Common utility to generate SWT Color objects as requested, using static methods,
 * or to act as a container for a particular instance of a color, using non-static
 * methods. Capable of generating an XML string representing the color.
 *
 *
 */
public class ChillColor
{
	/**
	 * XML element name for a red color
	 */
	public static final String RED_COLOR_TAG = "colorRed";
	
	/**
	 * XML element name for a green color
	 */
	public static final String GREEN_COLOR_TAG = "colorGreen";
	
	/**
	 * XML element name for a blue color
	 */
	public static final String BLUE_COLOR_TAG = "colorBlue";

	private final static int[] YELLOW_VALS = {255, 255, 0};
	private final static int[] GREEN_VALS = { 0, 255, 0};
	private final static int[] DARK_GREEN_VALS = { 0, 150, 0};
	private final static int[] RED_VALS = {255, 0, 0};
	private final static int[] GREY_VALS = {200, 200, 200};
	private final static int[] DARK_GREY_VALS = {109, 109, 109};
	private final static int[] BLACK_VALS = {0, 0, 0};
	private final static int[] WHITE_VALS = {255, 255, 255};
	private final static int[] BLUE_VALS = {0, 0, 255};
	private final static int[] DARK_RED_VALS = {220,0,0};
	private final static int[] LIGHT_GREY_VALS = {220,220,220};
	private final static int[] LIGHT_AQUA_BLUE_VALS = {175,238,238};
	private final static int[] ORANGE_VALS = {255,127,36};
	private final static int[] PURPLE_VALS = {191,62,255};
	private final static int[] PINK_VALS = {255,181,197};
	private final static int[] BLUE_GREY_VALS = {119,136,153};
	private final static int[] AQUA_VALS = {0,197,205};

	private final static int[][] colorMap = new int[][] {
		YELLOW_VALS,
		GREEN_VALS,
		DARK_GREEN_VALS,
		RED_VALS,
		GREY_VALS,
		DARK_GREY_VALS,
		BLACK_VALS,
		WHITE_VALS,
		BLUE_VALS,
		DARK_RED_VALS,
		LIGHT_GREY_VALS,
		LIGHT_AQUA_BLUE_VALS,
		ORANGE_VALS,
		PURPLE_VALS,
		PINK_VALS,
		BLUE_GREY_VALS,
		AQUA_VALS
	};

	/**
	 * ColorName is a convenience enumeration for selecting from a set of known colors.
	 *
	 *
	 */
	public enum ColorName {
		/**
		 * Yellow convenience name
		 */
		 YELLOW,
	    
		/**
		 * Convenience name for green
		 */
		 GREEN,
		 
		 /**
		  * Convenience name for dark green
		  */
		 DARK_GREEN,
		 
		 /**
		  * Convenience name for red
		  */
		 RED,
		 
		 /**
		  * Convenience name for grey
		  */
		 GREY,
		 
		 /**
		  * Convenience name for dark grey
		  */
		 DARK_GREY,
		 
		 /**
		  * Convenience name for black
		  */
		 BLACK,
		 
		 /**
		  * Convenience name for white
		  */
		 WHITE,
		 
		 /**
          * Convenience name for blue
          */
		 BLUE,
		 
         /**
          * Convenience name for dark red
          */
		 DARK_RED,

         /**
          * Convenience name for light grey
          */
		 LIGHT_GREY,
         
         /**
          * Convenience name for light aqua
          */
		 LIGHT_AQUA_BLUE,
         
         /**
          * Convenience name for orange
          */
		 ORANGE,
         
         /**
          * Convenience name for purple
          */
		 PURPLE,
         
         /**
          * Convenience name for pink
          */
		 PINK,
         
         /**
          * Convenience name for blue grey
          */
		 BLUE_GREY,
         
         /**
          * Convenience name for aqua
          */
		 AQUA
	 };

	 private int red;
	 private int green;
	 private int blue;

	 /**
	  * Creates an instance of ChillColor.
	  */
	 public ChillColor() {
	 }

	 /**
	  * Creates an instance of ChillColor from a ColorName value.
	  * @param colorName the ColorName enumerated value
	  */
	 public ChillColor(final ColorName colorName) {
		 setColor(colorName);
	 }

	 /**
	  * Creates an instance of ChillColor with the given color settings.
	  * @param red red level
	  * @param green green level
	  * @param blue blue level
	  */
	 public ChillColor(final int red, final int green, final int blue) {
		 setColor(red, green, blue);
	 }

	 /**
	  * Creates an instance of ChillColor with the given color settings in RGB string format.
	  * @param rgbStr "<red level>,<green level>,<blue level>" string
	  */
	 public ChillColor(final String rgbStr) {

		 setColors(rgbStr);      
	 }

	 /**
	  * Copy constructor.
	  * 
	  * @param toCopy ChillColor object containing attributes to copy to the new instance
	  */
	 public ChillColor(ChillColor toCopy) {
		 this(toCopy.red, toCopy.green, toCopy.blue);
	 }

	 /**
	  * Sets all three color levels at once.
	  * @param red red level
	  * @param green green level
	  * @param blue blue level
	  */
	 public void setColor(final int red, final int green, final int blue) {
		 this.red = red;
		 this.green = green;
		 this.blue = blue;
	 }

	 /**
	  * Sets color levels from a ColorName enumerated value.
	  * @param color the ColorName
	  */
	 public void setColor(final ColorName color) {
		 red = colorMap[color.ordinal()][0];
		 green = colorMap[color.ordinal()][1];
		 blue = colorMap[color.ordinal()][2];
	 }

	 /**
	  * Sets color levels from given color settings in RGB string format.
	  * @param rgbStr "<red level>,<green level>,<blue level>" string
	  * @throws IllegalArgumentException thrown when the RGB value is null or 
	  *            does not have 3 comma-separated values
	  */
	 public void setColors(final String rgbStr) throws IllegalArgumentException{

		 int[] rgb = null;
		 if (rgbStr != null) {
			 String[] rgbStrings = rgbStr.split(",");
			 rgb = new int[rgbStrings.length];
			 for (int i = 0; i < rgbStrings.length; i++) {
				 rgb[i] = Integer.parseInt(rgbStrings[i]);
			 }

			 if (rgb.length != 3) {
				 throw new IllegalArgumentException("Argument has invalid format");
			 }

			 setColor(rgb[0],rgb[1],rgb[2]);
		 }
		 else {
			 throw new IllegalArgumentException("null argument is illegal");
		 }

	 }

	 /**
	  * Gets an XML representation of a chill color
	  * @return an XML string representing the current color settings
	  */
	 public String toXml() {
		 return getRgbString();
	 }

	 /**
	  * Gets a comma-separated RGB (R,G,B) string for this color.
	  * @return RGB text string
	  */
	 public String getRgbString() {
		 return red + "," + green + "," + blue;	
	 }

	 /**
	  * Gets the blue value in the (R,G,B)
	  * @return the blue color level
	  */
	 public int getBlue() {
		 return blue;
	 }

	 /**
	  * Sets the blue color level.
	  * @param blue the blue level
	  */
	 public void setBlue(final int blue) {
		 this.blue = blue;
	 }

	 /**
	  * Gets the green value in the (R,G,B)
	  * @return the green color level
	  */
	 public int getGreen() {
		 return green;
	 }

	 /**
	  * Sets the green color level.
	  * @param green the green level
	  */
	 public void setGreen(final int green) {
		 this.green = green;
	 }

	 /**
	  * Gets the red value in the (R,G,B)
	  * @return the red color level
	  */
	 public int getRed() {
		 return red;
	 }

	 /**
	  * Sets the red color level.
	  * @param red the red level
	  */
	 public void setRed(final int red) {
		 this.red = red;
	 }

	 /**
	  * Creates and returns a new SWT color for the given device
	  * @param device SWT device
	  * @return a new SWT color object created with the current R,G,B values 
	  *            in this class
	  */
	 public Color getSwtColor(final Device device)
	 {
		 return(new Color(device,red,green,blue));
	 }
	 
	/**
     * {@inheritDoc}
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	 @Override
	public boolean equals(final Object compare) {
		 if (compare == null || !(compare instanceof ChillColor)) {
			 return false;
		 }
		 ChillColor compareCol = (ChillColor)compare;
		 return compareCol.red == red && compareCol.green == green &&
		 compareCol.blue == blue;
	 }

	 /**
	  * {@inheritDoc}
	  * @see java.lang.Object#hashCode()
	  */
	 @Override
	 public int hashCode() {
		 return red + green + blue;
	 }
}