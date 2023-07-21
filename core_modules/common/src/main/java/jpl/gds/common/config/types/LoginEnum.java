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
package jpl.gds.common.config.types;

import java.util.ArrayList;
import java.util.List;


/**
 * An enumeration of possible user authentication methods.
 *
 */
public enum LoginEnum
{
    /** Authenticate via a GUI window */
    GUI_WINDOW(true, false),

    /** Authenticate via a keytab file */
    KEYTAB_FILE(true, true),

    /** Authenticate via a commandline prompt */
    TEXT_PROMPT(false, true),
    
    /** Authenticate via Kerberos */
    KERBEROS(true, true),
    
    /** Authenticate via command line with RSA SecurID */
    SECURID_CLI(false, true),
    
    /** Authenticate via GUI with RSA SecurId */
    SECURID_GUI(true, false);

    private final boolean allowedForGui;
    private final boolean allowedForNonGui;


    /**
     * Private constructor.
     *
     * @param gui    True if choice is allowed in GUIs.
     * @param nonGui True if choice is allowed without a GUI.
     */
    private LoginEnum(final boolean gui,
                      final boolean nonGui)
    {
        allowedForGui    = gui;
        allowedForNonGui = nonGui;
    }


    /**
     * Getter for GUI state.
     *
     * @return True if choice is allowed in GUIs.
     */
    public boolean allowedForGui()
    {
        return allowedForGui;
    }


    /**
     * Getter for non-GUI state.
     *
     * @return True if choice is allowed without a GUI.
     */
    public boolean allowedForNonGui()
    {
        return allowedForNonGui;
    }


    /**
     * Return string of choices valid in GUI mode.
     *
     * @return List as string
     */
    public static String guiChoices()
    {
        final StringBuilder sb    = new StringBuilder();
        boolean             first = true;

        for (final LoginEnum le : values())
        {
            if (! le.allowedForGui())
            {
                continue;
            }

            if (first)
            {
                first = false;
            }
            else
            {
                sb.append(',');
            }

            sb.append(le);
        }

        return sb.toString();
    }


    /**
     * Return string of choices valid in non-GUI mode.
     *
     * @return List as string
     */
    public static String nonGuiChoices()
    {
        final StringBuilder sb    = new StringBuilder();
        boolean             first = true;

        for (final LoginEnum le : values())
        {
            if (! le.allowedForNonGui())
            {
                continue;
            }

            if (first)
            {
                first = false;
            }
            else
            {
                sb.append(',');
            }

            sb.append(le);
        }

        return sb.toString();
    }


    /**
     * Return list of choices valid in GUI mode.
     *
     * @return List
     */
    public static List<LoginEnum> guiChoicesList()
    {
        final List<LoginEnum> list = new ArrayList<LoginEnum>();

        for (final LoginEnum le : values())
        {
            if (le.allowedForGui())
            {
                list.add(le);
            }
        }

        return list;
    }


    /**
     * Return list of choices valid in non-GUI mode.
     *
     * @return List
     */
    public static List<LoginEnum> nonGuiChoicesList()
    {
        final List<LoginEnum> list = new ArrayList<LoginEnum>();

        for (final LoginEnum le : values())
        {
            if (le.allowedForNonGui())
            {
                list.add(le);
            }
        }

        return list;
    }
}
