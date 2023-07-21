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
package jpl.gds.eha.api.message.aggregation;

import jpl.gds.dictionary.api.channel.ChannelDefinitionType;
import jpl.gds.eha.api.channel.ChannelCategoryEnum;
import jpl.gds.shared.time.CoarseFineEncoding;

public interface IEhaChannelGroupDiscriminator {
	
    /**
     * @return the discrinimator's DSSID
     */
    public Integer getDssId();

    /**
     * @return the discrinimator's VCID
     */
    public int getVcid();
    
    /**
     * @return the discriminator's APID
     */
    public int getApid();

    /**
     * @return
     */
    public boolean isRealtime();

    /**
     * @return
     */
    public boolean isSSE();

    /**
     * @return
     */
    public ChannelDefinitionType getChanType();
    
    /**
     * @return
     */
    public ChannelCategoryEnum getChanCatEnum();

    /**
     * @return
     */
    public CoarseFineEncoding getSclkEncoding();

    /**
     * @return
     */
    public int hashCode();
    
    /**
     * @param o
     * @return
     */
    public boolean equals(Object o);
}