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
package jpl.gds.jms.portal;

import jpl.gds.shared.message.IMessage;

/**
 * TranslatedMessage represents an internal Message that has been translated to
 * XML.
 */
public class TranslatedMessage {

    private final IMessage message;
    private byte[] translation;
    private boolean translated;

    /**
     * Creates an instance of TranslatedMessage.
     * 
     * @param m
     *            the internal Message object
     */
    public TranslatedMessage(IMessage m) {
        this.message = m;
    }

    /**
     * Returns the translated flag.
     * 
     * @return true if translated
     */
    public boolean isTranslated() {
        return translated;
    }

    /**
     * Sets the translated flag.
     * 
     * @param translated
     *            The translated to set.
     */
    public void setTranslated(boolean translated) {
        this.translated = translated;
    }

    /**
     * Returns the translation text.
     * 
     * @return translation text
     */
    public String getTranslationAsString() {
        return new String(translation);
    }

    /**
     * Returns the translation text.
     * 
     * @return translation as a byte array
     */
    public byte[] getTranslationAsBytes() {
        return translation;
    }

    /**
     * Sets the translated text.
     * 
     * @param translation
     *            The translation to set.
     */
    public void setTranslation(String translation) {
        this.translation = translation.getBytes();
    }

    /**
     * Sets the translated blob.
     * 
     * @param translation
     *            The translation to set.
     */
    public void setTranslation(byte[] translation) {
        this.translation = translation;
    }

    /**
     * Returns the internal Message object.
     * 
     * @return message
     */
    public IMessage getMessage() {
        return message;
    }

    /**
     * Returns the translation size, or size if there is no translation.
     * 
     * @return translation size
     */
    public int getByteSize() {
        if (translation == null) {
            return 0;
        }
        return translation.length;
    }
}