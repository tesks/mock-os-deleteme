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

import org.eclipse.swt.SWT;

import jpl.gds.shared.config.SharedGuiProperties;

/**
 * 
 * ChillFont represents a perspective font in a variety of sizes for selection
 * by the user. It is also capable of representing fonts of any size, in
 * addition to pre-established font sizes. Capable of generating an XML string 
 * representing the font. Loads font configuration using SharedGuiPorperties.
 * 
 *
 */
public class ChillFont {
    
    private static final SharedGuiProperties properties = SharedGuiProperties.getInstance();
    
    /**
     * SWT.UNDERLINE_SINGLE and SWT.NORMAL both equal 0, so this constant is
     * created to avoid duplicates
     */
    public static final int UNDERLINE = 3;

    /**
     * XML tag name for a font face
     */
    public static final String FONT_FACE_TAG = "dataFontFace";

    /**
     * XML tag name for a font size
     */
    public static final String FONT_SIZE_TAG = "dataFontSize";

    /**
     * XML tag name for a font style
     */
    public static final String FONT_STYLE_TAG = "dataFontStyle";

    /**
     * Default type face.
     */
    public static final String DEFAULT_FACE = properties.getDefaultVariableFace();

    /**
     * Monospaced font face
     */
    public static final String MONOSPACE_FACE = properties.getDefaultMonospaceFace();

    /**
     * Default type style.
     */
    public static final int DEFAULT_STYLE = SWT.NONE;

    private static final int MINISCULE_SIZE = properties.getMinisculeFontSize();
    private static final int MINI_SIZE = properties.getMiniFontSize();
    private static final int TINY_SIZE = properties.getTinyFontSize();
    private static final int SMALL_SIZE = properties.getSmallFontSize();
    private static final int MEDIUM_SIZE = properties.getMediumFontSize();
    private static final int LARGE_SIZE = properties.getLargeFontSize();
    private static final int SUPER_LARGE_SIZE = properties.getSuperLargeFontSize();
    

    /**
     * 
     * FontSize is an enumerated class for selecting type point sizes.
     * 
     *
     */
    public enum FontSize {
        /**
         * Smallest font size available
         */
        MINISCULE(MINISCULE_SIZE),

        /**
         * Mini font size
         */
        MINI(MINI_SIZE),

        /**
         * Tiny font size
         */
        TINY(TINY_SIZE),

        /**
         * Small font size
         */
        SMALL(SMALL_SIZE),

        /**
         * Medium font size
         */
        MEDIUM(MEDIUM_SIZE),

        /**
         * Large font size
         */
        LARGE(LARGE_SIZE),

        /**
         * Biggest font size available
         */
        SUPER_LARGE(SUPER_LARGE_SIZE);

        private int size;

        /**
         * 
         * Creates an instance of FontSize.
         * 
         * @param pointSize
         *            the type point size
         */
        FontSize(final int pointSize) {
            this.size = pointSize;
        }

        /**
         * Gets the point size for this font
         * 
         * @return the point size for this font size
         */
        public int getPointSize() {
            return (this.size);
        }
    };

    private String currentFontFace = DEFAULT_FACE;
    private int currentFontSize = FontSize.SMALL.getPointSize();
    private int currentFontStyle = DEFAULT_STYLE;

    /**
     * Flag that is set to true if it is desired to reverse the
     * foreground/background colors when drawing the element this font is
     * associated with.
     */
    private boolean reverseFlag;

    /**
     * Creates an instance of ChillFont with default characteristics.
     */
    public ChillFont() {
    }

    /**
     * Creates an instance of ChillFont with the given characteristics.
     * 
     * @param face
     *            the type face name
     * @param size
     *            the FontSize
     * @param style
     *            SWT.NONE, SWT.ITALIC, or SWT.BOLD
     */
    public ChillFont(final String face, final FontSize size, final int style) {
        setFont(face, size, style);
    }

    /**
     * Creates an instance of ChillFont with the given characteristics.
     * 
     * @param face
     *            the type face name
     * @param size
     *            the point size
     * @param style
     *            SWT.NONE, SWT.ITALIC, or SWT.BOLD
     */
    public ChillFont(final String face, final int size, final int style) {
        setFont(face, size, style);
    }

    /**
     * Creates an instance of ChillFont with the given settings in the supplied
     * string.
     * 
     * @param fStr
     *            "face type, point size, style, reverse" string ie
     *            "Helvetica,11,NORMAL,REVERSE" The reverse string should only
     *            be provided if it is desired to reverse the
     *            foreground/background colors when drawing the element. size is
     *            a single or double digit number style can be NORMAL, BOLD,
     *            ITALIC, UNDERLINE, or BOLD
     * 
     */
    public ChillFont(final String fStr) {
        setFont(fStr);
    }

    /**
     * Copy constructor.
     * 
     * @param font
     *            ChillFont object containing attributes to copy to the new
     *            instance.
     */
    public ChillFont(final ChillFont font) {
        this(font.currentFontFace, font.currentFontSize, font.currentFontStyle);
    }

    /**
     * Sets the font characteristics.
     * 
     * @param face
     *            the type face name
     * @param size
     *            the FontSize
     * @param style
     *            SWT.NONE, SWT.ITALIC, or SWT.BOLD
     */
    public void setFont(final String face, final FontSize size, final int style) {
        this.currentFontFace = face;
        this.currentFontSize = size.getPointSize();
        this.currentFontStyle = style;
    }

    /**
     * Sets the font characteristics.
     * 
     * @param face
     *            the type face name
     * @param size
     *            the point size
     * @param style
     *            SWT.NONE, SWT.ITALIC, or SWT.BOLD
     */
    public void setFont(final String face, final int size, final int style) {
        this.currentFontFace = face;
        this.currentFontSize = size;
        this.currentFontStyle = style;
    }

    /**
     * Sets the font characteristics with the given settings in the supplied
     * string.
     * 
     * @param fStr
     *            "face type, point size, style, reverse" string ie
     *            "Helvetica,11,NORMAL,REVERSE" The reverse string should only
     *            be provided if it is desired to reverse the
     *            foreground/background colors when drawing the element. size is
     *            a single or double digit number style can be NORMAL, BOLD,
     *            ITALIC, UNDERLINE, or BOLD
     * @throws IllegalArgumentException
     *             thrown if parameter is null, does not have 4 comma-separated
     *             fields or the style is not one of the accepted values
     */
    public void setFont(final String fStr) throws IllegalArgumentException {

        if (fStr != null) {
            final String[] fStrings = fStr.split(",");

            if (fStrings.length > 4) {
                throw new IllegalArgumentException(
                        "Argument has invalid format");
            }

            int style;
            if (fStrings[2].equals("NORMAL")) {
                style = SWT.NORMAL;
            } else if (fStrings[2].equals("BOLD")) {
                style = SWT.BOLD;
            } else if (fStrings[2].equals("ITALIC")) {
                style = SWT.ITALIC;
            } else if (fStrings[2].equals("UNDERLINE")) {
                style = UNDERLINE;
            } else {
                throw new IllegalArgumentException(
                        "style should be NORMAL, BOLD, ITALIC, UNDERLINE, BOLD, or REVERSE");
            }

            setFont(fStrings[0], Integer.parseInt(fStrings[1]), style);

            // We must have the reverse flag if length = 4:
            if (fStrings.length == 4) {
                assert (fStrings[3].equals("REVERSE"));
                this.reverseFlag = true;
            }
        } else {
            throw new IllegalArgumentException("null argument is illegal");
        }
    }

    /**
     * Gets the font face name (i.e. Arial)
     * 
     * @return the current type face name
     */
    public String getFace() {
        return this.currentFontFace;
    }

    /**
     * Sets the current type face name.
     * 
     * @param currentFontFace
     *            the face to set
     */
    public void setFace(final String currentFontFace) {
        this.currentFontFace = currentFontFace;
    }

    /**
     * Gets the font size
     * 
     * @return the current point size
     */
    public int getSize() {
        return this.currentFontSize;
    }

    /**
     * Returns the SWT name for the font style if it exists, otherwise returns
     * null
     * 
     * @return SWT style name (normal, bold, italic, underline) or null if none
     */
    public String getStyleName() {

        if (this.currentFontStyle == SWT.NORMAL) {
            return "NORMAL";
        } else if (this.currentFontStyle == SWT.BOLD) {
            return "BOLD";
        } else if (this.currentFontStyle == SWT.ITALIC) {
            return "ITALIC";
        } else if (this.currentFontStyle == UNDERLINE) {
            return "UNDERLINE";
        } else {
            return null;
        }
    }

    /**
     * Sets the current font size.
     * 
     * @param currentFontSize
     *            the FontSize to set
     */
    public void setSize(final FontSize currentFontSize) {
        this.currentFontSize = currentFontSize.getPointSize();
    }

    /**
     * Sets the current font size.
     * 
     * @param currentFontSize
     *            the point size to set
     */
    public void setSize(final int currentFontSize) {
        this.currentFontSize = currentFontSize;
    }

    /**
     * Gets the font style
     * 
     * @return the current type style: SWT.NONE, SWT.ITALIC, SWT.BOLD, UNDERLINE
     */
    public int getStyle() {
        return this.currentFontStyle;
    }

    /**
     * Sets the current type style
     * 
     * @param currentFontStyle
     *            SWT.NONE, SWT.ITALIC, SWT.BOLD
     */
    public void setStyle(final int currentFontStyle) {
        this.currentFontStyle = currentFontStyle;
    }

    /**
     * Gets the name of a font size, if it matches a Chill font size
     * 
     * @param size
     *            the point size
     * @return the name of the font (SMALL,MEDIUM,LARGE, etc) if the input size
     *         matches a Chill font size; otherwise null
     */
    public static String getSizeName(final int size) {
        if (size == SUPER_LARGE_SIZE) {
            return FontSize.SUPER_LARGE.name();
        } else if (size == LARGE_SIZE) {
            return FontSize.LARGE.name();
        } else if (size == MEDIUM_SIZE) {
            return FontSize.MEDIUM.name();
        } else if (size == SMALL_SIZE) {
            return FontSize.SMALL.name();
        } else if (size == TINY_SIZE) {
            return FontSize.TINY.name();
        } else if (size == MINI_SIZE) {
            return FontSize.MINI.name();
        } else if (size == MINISCULE_SIZE) {
            return FontSize.MINISCULE.name();
        }
        return null;
    }

    /**
     * Returns the reverseFlag
     * 
     * @return reverseFlag which is set to true if it is desired to reverse the
     *         foreground/background colors when drawing the element this font
     *         is associated with.
     */
    public boolean getReverseFlag() {
        return this.reverseFlag;
    }

    /**
     * {@inheritDoc}
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object compare) {
        if (compare == null || !(compare instanceof ChillFont)) {
            return false;
        }
        final ChillFont fontCompare = (ChillFont) compare;
        return fontCompare.currentFontFace.equals(this.currentFontFace)
                && fontCompare.currentFontSize == this.currentFontSize
                && fontCompare.currentFontStyle == this.currentFontStyle;
    }

    /**
     * {@inheritDoc}
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return this.currentFontFace.hashCode() + this.currentFontSize
                + this.currentFontStyle;
    }

    /**
     * Gets the XML representation for this font
     * @return an XML string representing the current font settings
     */
    public String toXml() {

        return this.getFontString();
    }

    /**
     * Returns a string of the form "face,size,style"
     * 
     * @return String representation of this font
     */
    public String getFontString() {

        final String fontString = this.getFace() + "," + this.getSize() + ","
                + this.getStyleName();

        if (!this.reverseFlag) {
            return fontString;
        } else {
            return fontString + ",REVERSE";
        }
    }
}
