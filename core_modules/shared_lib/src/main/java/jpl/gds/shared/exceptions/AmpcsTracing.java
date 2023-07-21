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
package jpl.gds.shared.exceptions;

import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicReference;

import jpl.gds.shared.string.StringUtil;
import jpl.gds.shared.types.Triplet;


/**
 * Static methods to support AMPCS exception/error tracing. We accept calls
 * only from specific exception hierarchies.
 *
 * The calls to the perform methods are presumed to be the last thing in the
 * constructors for the class creating the hierarchy (AmpcsException and
 * AmpcsRuntimeException as of now).
 *
 * It is somewhat odd to make such a call in a constructor before the instance
 * is fully created (because the subclasses may perform actions). But that
 * works because the traceback logic is independent of anything AMPCS might
 * reasonably do. If the subclasses are messing with the traceback logic then
 * they should not be in the tracing hierarchies.
 *
 * We deliberately do not handle Throwable and Error.
 *
 */
public final class AmpcsTracing extends Object
{
    /** How to trace */
    public static enum MarkerEnum
    {
        /** Never trace */
        NEVER,

        /** Trace if configured */
        CONTROL,

        /** Trace if configured plus always log */
        CONTROL_LOG,

        /** Always trace */
        TRACE,

        /** Always log */
        LOG,

        /** Always trace plus always log */
        TRACE_LOG;
    }

    /** public statics are needed by JUnit tests */

    /** Master flag to control tracing */
    public static final boolean TRACING_ENABLED = true;

    /** Master flag to control logging */
    public static final boolean LOGGING_ENABLED = true;

    /** General traceback string */
    public static final String PREFIX = "Traceback_";

    /** Global enable name */
    public static final String ON = PREFIX + "ON";

    /** Enable value */
    public static final String OK = "TRUE";

    /** Default marker if one is not provided */
    private static final Marker DEFAULT_MARKER = new Marker();

    /** System-level print writer used for logging if none in marker */
    private static final AtomicReference<PrintWriter> SYSTEM_PW =
        new AtomicReference<PrintWriter>(null);


    /**
     * Constructor, never called.
     */
    private AmpcsTracing()
    {
        super();
    }


    /**
     * Set the print writer to be used for logging if none is specifically
     * provided.
     *
     * @param pw Print writer (may be null)
     */
    public static void setPrintWriter(final PrintWriter pw)
    {
        SYSTEM_PW.set(pw);
    }


    /**
     * Determine if a traceback is needed and then do it.
     *
     * The master mode must allow tracing, and the exception must not be null.
     * The global mode must be enabled, and the specific exception class name
     * must be enabled or the marker must exist and be enabled.
     *
     * @param e         Exception whose class is to be checked
     * @param rawMarker Marker or null
     */
    private static void internalPerformTracebackIfDesired(
                            final Exception e,
                            final Marker    rawMarker)
    {
        if ((e == null) || (! TRACING_ENABLED && ! LOGGING_ENABLED))
        {
            // Nothing to do
            return;
        }

        final Marker marker = ((rawMarker != null)
                                   ? rawMarker
                                   : DEFAULT_MARKER);

        boolean ok  = false;
        boolean log = false;

        switch (marker.getEnum())
        {
            case CONTROL:
                ok = TRACING_ENABLED && checkEnv(e, marker);
                break;

            case CONTROL_LOG:
                ok  = TRACING_ENABLED && checkEnv(e, marker);
                log = LOGGING_ENABLED;
                break;

            case TRACE:
                ok = TRACING_ENABLED;
                break;

            case LOG:
                log = LOGGING_ENABLED;
                break;

            case TRACE_LOG:
                ok  = TRACING_ENABLED;
                log = LOGGING_ENABLED;
                break;

            case NEVER:
            default:
                break;
        }

        if (! log && ! ok)
        {
            return;
        }

        final String messages = ExceptionTools.rollUpMessages(e);

        if (log)
        {
            PrintWriter pw = marker.getPrintWriter();

            if (pw == null)
            {
                pw = SYSTEM_PW.get();
            }

            if (pw != null)
            {
                pw.println(messages);

                e.printStackTrace(pw);
            }
        }

        if (ok)
        {
            System.err.println(messages);

            e.printStackTrace();
        }
    }


    /**
     * Check environment variables.
     *
     * @param e      Exception
     * @param marker Marker
     *
     * @return True if enabled
     */
    private static boolean checkEnv(final Exception e,
                                    final Marker    marker)
    {
        if (! OK.equals(StringUtil.safeTrim(System.getenv(ON))))
        {
            return false;
        }

        final StringBuilder sb = new StringBuilder();

        sb.append(PREFIX).append(e.getClass().getSimpleName());

        if (OK.equals(StringUtil.safeTrim(System.getenv(sb.toString()))))
        {
            return true;
        }

        sb.setLength(0);
        sb.append(PREFIX).append(marker.getFlag());

        return OK.equals(StringUtil.safeTrim(System.getenv(sb.toString())));
    }


    /**
     * Perform traceback on AMPCSException if desired.
     *
     * @param ae     AmpcsException to trace back
     * @param marker Marker
     */
    public static void performTracebackIfDesired(final AmpcsException ae,
                                                 final Marker         marker)
    {
        internalPerformTracebackIfDesired(ae, marker);
    }


    /**
     * Perform traceback on AmpcsRuntimeException if desired.
     *
     * @param are    AmpcsRuntimeException to trace back
     * @param marker Marker
     */
    public static void performTracebackIfDesired(
                           final AmpcsRuntimeException are,
                           final Marker                marker)
    {
        internalPerformTracebackIfDesired(are, marker);
    }


    /** Class that represents how an exception wants to be handled */
    public static final class Marker
        extends Triplet<MarkerEnum, String, PrintWriter>
    {
		private static final long serialVersionUID = 1L;


		/**
         * Constructor.
         *
         * @param type Type of marker
         * @param flag Name to look for
         * @param pw   Print writer to log to
         */
        public Marker(final MarkerEnum  type,
                      final String      flag,
                      final PrintWriter pw)
        {
            super((type != null) ? type : MarkerEnum.NEVER,
                  StringUtil.safeTrim(flag),
                  pw);
        }


        /**
         * Constructor.
         *
         * @param type Type of marker
         * @param flag Name to look for
         */
        public Marker(final MarkerEnum type,
                      final String     flag)
        {
            this(type, flag, null);
        }


        /**
         * Constructor.
         *
         * @param type Type of marker
         */
        public Marker(final MarkerEnum type)
        {
            this(type, null, null);
        }


        /**
         * Constructor.
         */
        public Marker()
        {
            this(null, null, null);
        }


        /**
         * Getter for enum type.
         *
         * @return Type
         */
        public MarkerEnum getEnum()
        {
            return getOne();
        }


        /**
         * Getter for search name.
         *
         * @return Name
         */
        public String getFlag()
        {
            return getTwo();
        }


        /**
         * Getter for print writer.
         *
         * @return Print writer
         */
        public PrintWriter getPrintWriter()
        {
            return getThree();
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public String toString()
        {
            final StringBuilder sb = new StringBuilder(getEnum().toString());

            final String flag = getFlag();

            if (! flag.isEmpty())
            {
                sb.append('/').append(flag);
            }

            final PrintWriter pw = getPrintWriter();

            if (pw != null)
            {
                // There's no good name for the print writer.
                // This at least allows you to tell one from another

                sb.append('/').append(pw);
            }

            return sb.toString();
        }
    }
}
