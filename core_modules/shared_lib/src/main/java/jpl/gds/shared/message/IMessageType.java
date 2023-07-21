package jpl.gds.shared.message;

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
/**
 * An interface to be implemented by message type enums and ojects.
 * 
 *
 * @since R8
 */
public interface IMessageType {
    /**
     * Returns the message subscription tag (also known as the internal type
     * name)
     * 
     * @return subscription tag
     */
    public abstract String getSubscriptionTag();
    
    /**
     * A method to compare message types. Uses a case insensitive comparison
     * of the subscription tags.
     * 
     * @param one the first message type
     * @param two the second message type
     * @return true if equal, false if not
     */
    public static boolean matches(final IMessageType one, final IMessageType two) {
        return one.getSubscriptionTag().equalsIgnoreCase(two.getSubscriptionTag());
    }
}
