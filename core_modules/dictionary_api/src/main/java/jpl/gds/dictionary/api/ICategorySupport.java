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
package jpl.gds.dictionary.api;

/**
 * The ICategorySupport interface is to be implemented by all dictionary
 * definition classes that support an attached Categories object. It defines the
 * AMPCS pre-defined category names and includes the setters and getters for the
 * Categories object and individual category name/value pairs. <p>
 * <p>
 * <p>
 * <b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <p>
 * <p>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <p>
 * <p>
 * 
 *
 * @see Categories
 */
public interface ICategorySupport {
    
    /**
     * Category key for subsystem
     */
    public static final String SUBSYSTEM = "subsystem";
    /**
     * Category key for operational category
     */
    public static final String OPS_CAT = "ops category"; 
    /**
     * Category key for module
     */
    public static final String MODULE = "module";
    
    /**
     * Sets the categories map.
     * 
     * @param map  the map of all the categories
     */
    public void setCategories(Categories map);
    
    /**
     * Gets the map of all the defined categories.  
     * 
     * @return the map of all the defined categories.  
     */
    public Categories getCategories();

    /**
     * Sets a category name and value into the existing
     * categories map.
     * 
     * @param catName  the category name
     * @param catValue  the category value 
     */
    public void setCategory(String catName, String catValue); 
    
    /**
     * Retrieves a category value given the category name.
     * 
     * @param catName  the category name
     *
     * @return the category value to which the specified category name is associated with.
     */
    public String getCategory(String catName);
    

}
