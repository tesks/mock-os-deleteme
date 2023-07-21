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
package jpl.gds.dictionary.api.frame;

import java.util.Map;

import jpl.gds.shared.reflect.ReflectionException;
import jpl.gds.shared.time.CustomSclkExtractor;
import jpl.gds.shared.time.ISclkExtractor;
import jpl.gds.shared.time.ProjectSclkExtractor;

/**
 * An interface to be implemented by frame time field definition objects. Defines 
 * a time code type enumeration and allows access to class name or instance
 * of the actual time field extractor. 
 * <p>
 * <b>THIS IS A MULTI-MISSION CORE ADAPTATION INTERFACE</b><p>
 * <p>
 * <b>This is a controlled interface. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b><p>
 * <p>
 * 
 * 
 *
 */
public interface IFrameTimeFieldDefinition {

    /**
     * An enumeration of valid frame time code formats.
     * 
     *
     */
    public enum TimecodeType {
        
        /** Time code matches the project standard SCLK */
        PROJECT_SCLK(ProjectSclkExtractor.class.getName()),
        
        /** Time code is a SCLK with different parameters than the project SCLK */
        CUSTOM_SCLK(CustomSclkExtractor.class.getName()),
        
        /** Time code must be handled by a custom class */
        CUSTOM_CLASS(null);
        
        private String defaultExtractionClass;
        
        /**
         * Constructor.
         * 
         * @param className name of the Java class that extracts timecode from binary.
         */
        private TimecodeType(String className) {
            this.defaultExtractionClass = className;
        }
    
        /**
         * Gets the name of the java class that extracts timecode from binary.
         * 
         * @return java class name including package
         */
        public String getDefaultExtractionClass() {
            return defaultExtractionClass;
        }
    }

    /**
     * Gets the timecode extractor instance. If it has not already been created
     * or set, it is instantiated by this call.
     * 
     * @return ISclkExtractor object
     * 
     * @throws ReflectionException if the extractor cannot be created
     */
    public ISclkExtractor getExtractor() throws ReflectionException;
    
    /**
     * Sets the timecode extractor class name.
     * 
     * @param className java class name including package
     */
    public void setExtractorClass(String className);
    
    /**
     * Gets the timecode extractor class name.
     * 
     * @return class name including package
     */
    public String getExtractorClass();

    /**
     * Sets the bit offset of the timecode field within the frame.
     * 
     * @param offset bit offset from start of frame primary header
     */
    public void setBitOffset(int offset);

    /**
     * Gets the bit offset of the timecode field within the frame.
     * 
     * @return bit offset from start of frame primary header
     */
    public int getBitOffset();
    
    /**
     * Sets the bit size of the timecode field.
     * 
     * @return bit size
     */
    public int getBitSize();
    
    /**
     * Gets the bit size of time timecode field.
     * 
     * @param size bit size to set
     */
    public void setBitSize(int size);

    /**
     * Gets the TimecodeType enumeration value.
     * 
     * @return TimecodeType
     */
    public IFrameTimeFieldDefinition.TimecodeType getType();
    
    /**
     * Sets the map of parameters to the time extraction.
     * 
     * @param params Map of String parameter name to Object value
     */
    public void setParameterMap(Map<String, Object> params);
    
    /**
     * Gets the map of parameters to the time extraction.
     * 
     * @return Map of String parameter name to Object value
     */
    public Map<String, Object> getParameterMap();

}