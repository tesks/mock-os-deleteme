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
package jpl.gds.shared.cli.app.mc;

import java.security.cert.CertificateException;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.squareup.okhttp.OkHttpClient;

/**
 * Class UnsafeOkHttpClient
 *
 * This class overrides all SSL/TLS checking so that the client can ignore SSL/TLS certificate errors.
 * This is to support the --restInsecure option on the client side.
 */
public class UnsafeOkHttpClient extends OkHttpClient {

    /**
     * Connection timeout in seconds
     * This class is used when the --restInsecure option gets passed on the MC client side
     * See related timeout setting in class jpl.gds.security.cli.rest.AbstractRestfulClientCommandLineApp
     * for default OkHttpClient object.
     */
    private static final int CONNECTION_TIMEOUT_MINUTES = 10;
    /**
     * Read timeout in seconds
     */
    private static final int READ_TIMEOUT_MINUTES = CONNECTION_TIMEOUT_MINUTES;
    /**
     * Write timeout in seconds
     */
    private static final int WRITE_TIMEOUT_MINUTES = CONNECTION_TIMEOUT_MINUTES;

    public UnsafeOkHttpClient() {
        super();
        try {
            // Create a trust manager that does not validate certificate chains
            final TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
                @Override
                public void checkClientTrusted(final java.security.cert.X509Certificate[] chain, final String authType)
                        throws CertificateException {
                    // do nothing -- not checking
                }

                @Override
                public void checkServerTrusted(final java.security.cert.X509Certificate[] chain, final String authType)
                        throws CertificateException {
                    // do nothing -- not checking
                }

                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return new java.security.cert.X509Certificate[0];
                }
            } };

            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            // Create an ssl socket factory with our all-trusting manager
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            this.setSslSocketFactory(sslSocketFactory);
            this.setRetryOnConnectionFailure(false);
            this.setHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(final String hostname, final SSLSession session) {
                    return true;
                }
            });

            // Change TI/TP worker start procedures
            // Had been encountering the following socket timeout error relatively frequently
            // when using the MC command line tools with --restInsecure option
            // WARNING: Server Error: ApiException: java.net.SocketTimeoutException: timeout
            // Need to increase the timeout to allow enough time for product assembly
            this.setConnectTimeout(CONNECTION_TIMEOUT_MINUTES, TimeUnit.MINUTES);
            this.setReadTimeout(READ_TIMEOUT_MINUTES, TimeUnit.MINUTES);
            this.setWriteTimeout(WRITE_TIMEOUT_MINUTES, TimeUnit.MINUTES);
        }
        catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }
}
