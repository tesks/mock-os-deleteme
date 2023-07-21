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
package jpl.gds.shared.email;

import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.SendFailedException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.sun.mail.smtp.SMTPSendFailedException;

import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.string.StringUtil;
import jpl.gds.shared.sys.SystemUtilities;
import jpl.gds.shared.thread.SleepUtilities;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.types.UnsignedInteger;


/**
 * This is a utility class for sending emails via SMTP.
 *
 * The transport needs to be closed, so there is a set kept
 * of all EmailCenter instances. That set must be synchronized.
 * To prevent deadlocks, "this" and the set are never synchronized
 * at the same time.
 *
 * It isn't at all clear whether the "too many connections" can still
 * be detected, so I no longer rely on it. I retry connection failure
 * on any MessagingException.
 *
 *
 */
public class EmailCenter implements AutoCloseable
{
    /** The name of the Java system property for debugging the mail library */
    private static final String DEBUG_PROPERTY = "mail.debug";

    /** The name of the Java system property for setting the SMTP host */
    private static final String SMTP_HOST_PROPERTY = "mail.smtp.host";
    
    /** The name of the Java system property for setting the SMTP port */
	private static final String SMTP_PORT_PROPERTY = "mail.smtp.port";

    private final String TRANSPORT_TYPE = "smtps";

    private static final Set<EmailCenter> INSTANCES =
        Collections.synchronizedSet(new HashSet<EmailCenter>());

    private static final int TOO_MANY_CONNECTIONS = 451;

    /** True if we should turn on debug mode. */
    private static final boolean DEBUG = false;

    private static Tracer LOG;

    /** The SMTP host to use for sending email */
    private final String smtpHost;
    
    private final UnsignedInteger emailPort;
    
    private final String transportType;

    /** Email session, never null */
    private final Session session;

    /** Below here are synchronized on this */

    /** Transport, may be null, but only if closed */
    private final Transport transport;

    /** True if we are closed and disconnected */
    private boolean closed = false;

    private final int  emailMaxAttempts;
    private final long emailSendDelay;
    private final long emailFailureDelay;


    /**
     * Constructor
     *
     * @param host
     *            SMTP host
     *  @param port
     *            mail server port number
     *  @param emailTransportType
     *            email transport type
     * @param maxAttempts
     *            How many times to try
     * @param sendDelay
     *            Wait before trying
     * @param failureDelay
     *            Wait before trying again
     * @param trace
     *            The context Tracer
     */
    public EmailCenter(final String host,
    		           final UnsignedInteger port,
    		           final String emailTransportType,
                       final int    maxAttempts,
                       final long   sendDelay,
                       final long   failureDelay,
                       final Tracer trace)
    {
        super();

        // Cannot go below certain limits
        emailMaxAttempts  = Math.max(maxAttempts,      1);
        emailSendDelay    = Math.max(sendDelay,       1L);
        emailFailureDelay = Math.max(failureDelay, 1000L);

        smtpHost = StringUtil.safeTrim(host);
        this.emailPort = port;
        this.transportType = emailTransportType;
        
        LOG = trace;

        if (smtpHost.isEmpty())
        {
            throw new IllegalArgumentException("Null email host");
        }

        final Properties props = new Properties();

        props.put(SMTP_HOST_PROPERTY, smtpHost);
        props.put(DEBUG_PROPERTY, String.valueOf(DEBUG));
        props.put(SMTP_PORT_PROPERTY, emailPort.intValue());

        session = Session.getInstance(props, null);

        LOG.debug("EmailCenter Configured with host " + smtpHost +
        		  " port " + emailPort +
                  " max attempts " + emailMaxAttempts +
                  " send delay " + emailSendDelay +
                  " ms failure delay " + emailFailureDelay + " ms");

        session.setDebug(DEBUG);

        Transport localTransport = null;

        try
        {
            localTransport = session.getTransport(transportType);
        }
        catch (final NoSuchProviderException nspe)
        {
            LOG.error("EmailCenter No "        +
                      transportType          +
                      " transport available: " +
                      ExceptionTools.rollUpMessages(nspe));
        }
        catch (final ClassCastException ce)
        {
            LOG.error("EmailCenter Transport is not of " +
                      "expected class: "                 +
                      ExceptionTools.rollUpMessages(ce));
        }
        finally
        {
            transport = localTransport;

            if (transport != null)
            {
                INSTANCES.add(this);
            }
            else
            {
                closed = true;
            }
        }

        // Transport will connect the first time it is needed
    }


    /**
     * Accessor for the SMTP Host
     *
     * @return SMTP host being used to send email
     */
    public String getSMTPHost()
    {
        return smtpHost;
    }
    
    /**
     * Getter for email port number
     * @return port number being used to send email
     */
    public UnsignedInteger getEmailPort() {
    	return emailPort;
    }


    /**
     * Send an email
     *
     * @param toList   CSV of email addresses for the TO field
     * @param ccList   CSV of email addresses for the CC field
     * @param bccList  CSV of email addresses for the BCC field
     * @param fromList CSV of email addresses for the FROM field
     * @param subject  Email subject
     * @param body     Email body
     *
     * @throws MessagingException On failure to send
     */
    public synchronized void sendEmail(final String toList,
                                       final String ccList,
                                       final String bccList,
                                       final String fromList,
                                       final String subject,
                                       final String body)
        throws MessagingException
    {
        if (closed)
        {
            return;
        }

        sendEmail(parseInternetAddresses(toList),
                  parseInternetAddresses(ccList),
                  parseInternetAddresses(bccList),
                  parseInternetAddresses(fromList),
                  subject,
                  body);
    }


    /**
     * Send an email
     *
     * @param to      An array of email addresses for the TO field
     * @param cc      An array of email addresses for the CC field
     * @param bcc     An array of email addresses for the BCC field
     * @param from    An array of email addresses for the FROM field
     * @param subject Email subject
     * @param body    Email body
     *
     * @throws MessagingException On failure to send
     */
    public synchronized void sendEmail(final String[] to,
                                       final String[] cc,
                                       final String[] bcc,
                                       final String[] from,
                                       final String   subject,
                                       final String   body)
        throws MessagingException
    {
        if (closed)
        {
            return;
        }

        sendEmail(parseInternetAddresses(to),
                  parseInternetAddresses(cc),
                  parseInternetAddresses(bcc),
                  parseInternetAddresses(from),
                  subject,
                  body);
    }


    /**
     * Send an email
     *
     * @param to      Array of email addresses for the TO field
     * @param cc      Array of email addresses for the CC field
     * @param bcc     Array of email addresses for the BCC field
     * @param from    Array of email addresses for the FROM field
     * @param subject Email subject
     * @param body    Email body
     *
     * @throws MessagingException On failure to send
     */
    public synchronized void sendEmail(final InternetAddress[] to,
                                       final InternetAddress[] cc,
                                       final InternetAddress[] bcc,
                                       final InternetAddress[] from,
                                       final String            subject,
                                       final String            body)
       throws MessagingException
    {
        if (closed)
        {
            return;
        }

        // Connect if necessary

        connect();

        // Create an empty email message

        final Message msg = new MimeMessage(session);

        if ((to != null) && (to.length > 0))
        {
            msg.addRecipients(RecipientType.TO, to);
        }

        if ((cc != null) && (cc.length > 0))
        {
            msg.addRecipients(RecipientType.CC, cc);
        }

        if ((bcc != null) && (bcc.length > 0))
        {
            msg.addRecipients(RecipientType.BCC, bcc);
        }

        if ((from != null) && (from.length > 0))
        {
            msg.addFrom(from);
        }

        msg.setSubject(StringUtil.safeTrim(subject));

        msg.setText(StringUtil.safeTrim(body));

        msg.setSentDate(new AccurateDateTime());

        msg.saveChanges();

        // Send the message

        final long mark = System.currentTimeMillis();

        try
        {
            transport.sendMessage(msg, msg.getAllRecipients());
        }
        catch (final SMTPSendFailedException ssfe)
        {
            LOG.error("EmailCenter SMTP error, code " +
                      ssfe.getReturnCode()            +
                      ": "                            +
                      ExceptionTools.rollUpMessages(ssfe));

            throw ssfe;
        }
        catch (final SendFailedException sfe)
        {
            final StringBuilder sb = new StringBuilder();

            sb.append("EmailCenter semd failure: [");
            appendBadAddresses(sb,
                               sfe.getInvalidAddresses(),
                               sfe.getValidUnsentAddresses());
            sb.append("]: ");
            sb.append(ExceptionTools.rollUpMessages(sfe));

            LOG.error(sb.toString());

            throw sfe;
        }
        catch (final MessagingException me)
        {
            LOG.error("EmailCenter Unable to send email:  " + ExceptionTools.rollUpMessages(me));

            throw me;
        }

        final String message = "EmailCenter Sent email: "          +
                               (System.currentTimeMillis() - mark) +
                               " ms";
        if (DEBUG)
        {
            LOG.debug(message);
        }
        else
        {
            LOG.trace(message);
        }
    }


    /**
     * Close transport.
     */
    @Override
    public void close()
    {
        synchronized (this)
        {
            if (closed)
            {
                return;
            }

            closed = true;

            if (transport.isConnected())
            {
                try
                {
                    transport.close();
                }
                catch (final MessagingException me)
                {
                    SystemUtilities.doNothing();
                }
            }
        }

        INSTANCES.remove(this);
    }


    /**
     * Close all transports.
     */
    public static void closeAll()
    {
        // Copy set so we do not need to iterate synchronized

        final Set<EmailCenter> temp = new HashSet<EmailCenter>();

        synchronized (INSTANCES)
        {
            if (INSTANCES.isEmpty())
            {
                return;
            }

            temp.addAll(INSTANCES);

            INSTANCES.clear();
        }

        for (final EmailCenter ec : temp)
        {
            ec.close();
        }

        LOG.debug("EmailCenter Closed");
    }


    /**
     * Connect transport to server. Always delay before sending email, but on
     * failure delay longer to allow the congestion to clear.
     *
     * @throws MessagingException On failure
     */
    private synchronized void connect() throws MessagingException
    {
        if (closed || transport.isConnected())
        {
            return;
        }

        MessagingException lastException = null;

        int attempt = 0;


        for (; attempt < emailMaxAttempts; ++attempt)
        {
            SleepUtilities.checkedSleep(
                (attempt == 0) ? emailSendDelay : emailFailureDelay);

            lastException = null;

            try
            {
                // Standard port, no user or password

                transport.connect(getSMTPHost(), -1, null, null);

                break;
            }
            catch (final MessagingException me)
            {
            	LOG.debug("EmailCenter Unable to connect for email.. Retrying: " + 
                           ExceptionTools.rollUpMessages(me));

                lastException = me;
            }
        }

        // Bump this because we started with attempt zero
        ++attempt;

        if (lastException != null)
        {
            LOG.error("EmailCenter Unable to connect for email after " +
                      emailMaxAttempts                                 +
                      " attempts: "                                    +
                      ExceptionTools.rollUpMessages(lastException));

            throw lastException;
        }

        if (! transport.isConnected())
        {
            // This shouldn't happen

            LOG.error("EmailCenter Transport failed to connect");

            throw new MessagingException("EmailCenter Transport failed to " +
                                         "connect");
        }

        LOG.debug("EmailCenter Able to connect for email after " +
                  attempt                                        +
                  " attempts");
    }


    /**
     * Parse a string as IP addresses.
     *
     * @param s String list of IP addresses
     *
     * @return Parsed array
     *
     * @throws AddressException On bad address
     */
    private static InternetAddress[] parseInternetAddresses(final String s)
        throws AddressException
    {
        InternetAddress[] result = null;

        final String ia = StringUtil.safeTrim(s);

        if (! ia.isEmpty())
        {
            result = InternetAddress.parse(ia);

            if ((result != null) && (result.length == 0))
            {
                result = null;
            }
        }

        return result;
    }


    /**
     * Parse a string array as IP addresses.
     *
     * @param sa String array of IP addresses
     *
     * @return Parsed array
     *
     * @throws AddressException On bad address
     */
    private static InternetAddress[] parseInternetAddresses(final String[] sa)
        throws AddressException
    {
        if ((sa == null) || (sa.length == 0))
        {
            return null;
        }

        final Set<InternetAddress> set = new HashSet<InternetAddress>(sa.length);

        for (final String s : sa)
        {
            final String ia = StringUtil.safeTrim(s);

            if (! ia.isEmpty())
            {
                set.add(new InternetAddress(ia));
            }
        }

        if (set.isEmpty())
        {
            return null;
        }

        return set.toArray(new InternetAddress[set.size()]);
    }


    /**
     * Append addresses to error message.
     *
     * @param sb                   String builder
     * @param invalidAddresses     Array of invalid addresses
     * @param validUnsentAddresses Array of valid unsent addresses
     */
    private static void appendBadAddresses(final StringBuilder sb,
                                           final Address[]     invalidAddresses,
                                           final Address[]     validUnsentAddresses)
    {
        boolean first = true;

        if ((invalidAddresses != null) && (invalidAddresses.length > 0))
        {
            for (final Address address : invalidAddresses)
            {
                if (first)
                {
                    first = false;
                }
                else
                {
                    sb.append(',');
                }

                sb.append(address);
            }
        }

        if ((validUnsentAddresses != null) && (validUnsentAddresses.length > 0))
        {
            for (final Address address : validUnsentAddresses)
            {
                if (first)
                {
                    first = false;
                }
                else
                {
                    sb.append(',');
                }

                sb.append(address);
            }
        }
    }
}
