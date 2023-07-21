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
package jpl.gds.shared.template;

import org.apache.velocity.context.Context;

/**
 * Tool for working with <code>null</code> in Velocity templates.
 * It provides a method to set a VTL reference back to <code>null</code>.
 * Also provides methods to check if a VTL reference is <code>null</code> or not.
 * <p>
 *   NOTE: These examples assume you have placed an
 *   instance of the current context within itself as 'ctx'.
 *   And, of course, the NullTool is assumed to be available as 'null'.
 * </p>
 * <p><pre>
 * Example uses:
 *  $foo                              -> bar
 *  $null.isNull($foo)                -> false
 *  $null.isNotNull($foo)             -> true
 *
 *  $null.setNull($ctx, "foo")
 *  $foo                              -> $foo (null)
 *  $null.isNull($foo)                -> true
 *  $null.isNotNull($foo)             -> false
 *
 *  $null.set($ctx, $foo, "hoge")
 *  $foo                              -> hoge
 *  $null.set($ctx, $foo, $null.null)
 *  $foo                              -> $foo (null)
 * </pre></p>
 *
 * <p>This tool is entirely threadsafe, and has no instance members.
 * It may be used in any scope (request, session, or application).
 * </p>
 *
 */
public class NullTool
{
	private static NullTool instance;


    /**
     * Get single instance.
     *
     * @return Instance
     */	
	public static synchronized NullTool getInstance() {
		
		if (instance == null) {
			instance = new NullTool(); 
		}
		
		return instance;
	}

    /**
     * Default constructor.
     */
    public NullTool()
    {
    }

    /**
     * Sets the given VTL reference back to <code>null</code>.
     * @param context the current Context
     * @param key the VTL reference to set back to <code>null</code>.
     */
    public void setNull(Context context, String key)
    {
        if (this.isNull(context))
        {
            return;
        }
        context.remove(key);
    }

    /**
     * Sets the given VTL reference to the given value.
     * If the value is <code>null</code>,
     * the VTL reference is set to <code>null</code>.
     * @param context the current Context
     * @param key the VTL reference to set.
     * @param value the value to set the VTL reference to.
     */
    public void set(Context context, String key, Object value)
    {
        if (this.isNull(context))
        {
            return;
        }
        if (this.isNull(value))
        {
            this.setNull(context, key);
            return;
        }
        context.put(key, value);
    }

    /**
     * Checks if a VTL reference is <code>null</code>.
     * @param object the VTL reference to check.
     * @return <code>true</code> if the VTL reference is <code>null</code>,
     *          <code>false</code> if otherwise.
     */
    public boolean isNull(Object object)
    {
        return object == null;
    }

    /**
     * Checks if a VTL reference is not <code>null</code>.
     * @param object the VTL reference to check.
     * @return <code>true</code> if the VTL reference is not <code>null</code>,
     *          <code>false</code> if otherwise.
     */
    public boolean isNotNull(Object object)
    {
        return !this.isNull(object);
    }

    /**
     * A convenient method which returns <code>null</code>.
     * Actually, this tool will work the same without this method,
     * because Velocity treats non-existing methods as null. :)
     * @return <code>null</code>
     */
    public Object getNull()
    {
        return null;
    }

}
