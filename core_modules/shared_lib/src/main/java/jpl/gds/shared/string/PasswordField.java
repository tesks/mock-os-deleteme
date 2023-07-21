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
package jpl.gds.shared.string;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.Arrays;


/**
 * Class PasswordField.
 *
 * This class prompts the user for a password and attempts to mask input.
 *
 */
public class PasswordField
{
    private static final int INCREMENT = 128;


    /**
     * Prompt user for password and return.
     *
     * @param in     Stream to be used (e.g. System.in)
     * @param prompt The prompt to display to the user.
     *
     * @return The password as entered by the user.
     *
     * @throws IOException I/O error
     */
    public static final char[] getPassword(final InputStream in,
                                           final String      prompt)
        throws IOException
    {
        final MaskingThread       maskingthread = new MaskingThread(prompt);
        final Thread              thread        = new Thread(maskingthread);
        final PushbackInputStream is            =
            (! (in instanceof PushbackInputStream))
                ? new PushbackInputStream(in)
                : (PushbackInputStream) in;

        char[]  lineBuffer = new char[INCREMENT];
        char[]  buf        = new char[INCREMENT];
        int     room       = buf.length;
        int     offset     = 0;
        int     c          = 0;
        boolean keepGoing  = true;

        thread.setDaemon(true);
        thread.setName("PasswordField");

        thread.start();
	
        while (keepGoing)
        {
            switch (c = is.read())
            {
                case -1:
                case '\n':
                    keepGoing = false;
                    break;

                case '\r':
                    final int c2 = is.read();

                    if ((c2 != '\n') && (c2 != -1))
                    {
                       is.unread(c2);
                    }
                    else
                    {
                       keepGoing = false;
                    }

                    break;

                default:
                    if (--room < 0)
                    {
                        buf  = new char[offset + INCREMENT];
                        room = buf.length - offset - 1;

                        System.arraycopy(lineBuffer, 0, buf, 0, offset);

                        Arrays.fill(lineBuffer, ' ');

                        lineBuffer = buf;
                    }

                    buf[offset++] = (char) c;

                    break;
            }
        }

        maskingthread.stopMasking();

        if (offset == 0)
        {
            return null;
        }

        final char[] ret = new char[offset];

        System.arraycopy(buf, 0, ret, 0, offset);

        Arrays.fill(buf, ' ');

        return ret;
    }


    /**
     * Class MaskingThread.
     *
     * This class attempts to erase characters echoed to the console.
     */
    private static class MaskingThread extends Thread
    {
        private static final String ECHO = "\b*";

        private volatile boolean keepGoing = true;


        /**
         * Constructor.
         *
         * @param prompt The prompt displayed to the user
         */
        public MaskingThread(final String prompt)
        {
            System.out.print(prompt);

            // Add blank to avoid overwriting prompt

            System.out.print(' ');
        }


        /**
         * Begin masking until asked to stop.
         *
         * {@inheritDoc}
         */
        @Override
        public void run()
        {
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

            while (keepGoing)
            {
                System.out.print(ECHO);

                try
                {
                    // Attempt masking at this rate
                    Thread.sleep(1L);
                }
                catch (InterruptedException iex)
                {
                    break;
                }
            }
        }


        /**
         * Instruct the thread to stop masking.
         */
        public void stopMasking()
        {
            keepGoing = false;
        }
    }
}
