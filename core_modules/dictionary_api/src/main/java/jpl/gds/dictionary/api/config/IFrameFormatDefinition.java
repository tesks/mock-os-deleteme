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
package jpl.gds.dictionary.api.config;

/**
 * An interface to be implemented by frame format definition objects. Defines 
 * a frame format type enumeration and allows access to class names used to 
 * parse the frame header and compute the frame CRC.
 * <br>
 * <b>THIS IS A MULTI-MISSION CORE ADAPTATION INTERFACE</b><br>
 * <br>
 * <b>This is a controlled interface. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b><br>
 * <br>
 * 
 * 
 *
 */
public interface IFrameFormatDefinition {

    /**
     * An enumeration of supported transfer frame formats.
     * 
     *
     */
    public enum TypeName {
        
        /** Frame is a vanilla CCSDS TM (V1) frame */
        CCSDS_TM_1("jpl.gds.ccsds.impl.tm.frame.CcsdsFrameHeaderV1", null),
        
        /** Frame is a vanilla CCSDS AOS (V2) frame with an M_PDU header in the data area*/
        CCSDS_AOS_2_MPDU("jpl.gds.ccsds.impl.tm.frame.CcsdsFrameHeaderV2", null),
        
        /** Frame is a vanilla CCSDS AOS (V2) frame with a B_PDU header in the data area*/
        CCSDS_AOS_2_BPDU("jpl.gds.ccsds.impl.tm.frame.CcsdsFrameHeaderV2", null),
        
        /** Frame is a CCSDS-like frame, but requires custom classes for processing. */
        CUSTOM_CLASS(null, null),
        
        /** Frame type is unknown */
        UNKNOWN(null, null);
    
        String defaultFrameHeaderClass;
        String defaultFrameErrorControlClass;
    
        /**
         * Constructs a TypeName enum value.
         * 
         * @param headerClass
         *            the full class name of the frame header (IFrameHeader)
         *            class.
         * @param frameErrorClass
         *            the full class name of the frame error control computation
         *            (IFrameChecksumComputation) class
         */
        private TypeName(final String headerClass, final String frameErrorClass) {
            defaultFrameHeaderClass = headerClass;
            defaultFrameErrorControlClass = frameErrorClass;
            if (defaultFrameErrorControlClass == null) {
                defaultFrameErrorControlClass = FrameErrorControlType.CCSDS_CRC_16.getDefaultFrameErrorClass();
            }
            
        }
    
        /**
         * Gets the default frame header parsing class for this frame format type.
         * 
         * @return full class name
         */
        public String getDefaultFrameHeaderClass() {
            return defaultFrameHeaderClass;
        }
    
        /**
         * Gets the default frame error computation class for this frame format type.
         * 
         * @return full class name
         */
        public String getDefaultFrameErrorControlClass() {
            return defaultFrameErrorControlClass;
        }  
    
    }

    /**
     * Gets the full package name of the frame header processing class.
     * 
     * @return class name
     */
    public String getFrameHeaderClass();

    /**
     * Gets the full package name of the frame error computation
     * (IFrameChecksumComputation) class.
     * 
     * @return class name
     */
    public String getFrameErrorControlClass();

    /**
     * Sets the full package name of the frame header (IFrameHeader) class.
     * 
     * @param headerClass class name
     */
    public void setFrameHeaderClass(String headerClass);

    /**
     * Sets the full package name of the frame error computation
     * (IFrameChecksumComputation) class.
     * 
     * @param errorClass class name
     */
    public void setFrameErrorControlClass(String errorClass);

    /**
     * Gets the frame format TypeName.
     * 
     * @return TypeName
     */
    public IFrameFormatDefinition.TypeName getType();

}