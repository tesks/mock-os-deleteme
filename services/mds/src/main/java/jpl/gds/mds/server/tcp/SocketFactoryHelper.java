/*
 * Copyright 2006-2021. California Institute of Technology.
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

package jpl.gds.mds.server.tcp;

import jpl.gds.security.ssl.ISslConfiguration;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

/**
 * Socket factory helper helps set up socket factories for creating both secure and insecure TCP sockets.
 */
public class SocketFactoryHelper {
    private SocketFactoryHelper() {
    }

    /**
     * Create a server socket factory
     *
     * @param secureTcp        true for secure socket factory, false for default
     * @param sslConfiguration SSL/TLS configuration
     * @return
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws UnrecoverableKeyException
     * @throws CertificateException
     * @throws KeyManagementException
     * @throws IOException
     */
    public static ServerSocketFactory createServerSocketFactory(final boolean secureTcp,
                                                                final ISslConfiguration sslConfiguration) throws
                                                                                                          KeyStoreException,
                                                                                                          NoSuchAlgorithmException,
                                                                                                          UnrecoverableKeyException,
                                                                                                          CertificateException,
                                                                                                          KeyManagementException,
                                                                                                          IOException {
        if (secureTcp) {
            final SSLContext context = getSslContext(sslConfiguration);
            return context.getServerSocketFactory();
        } else {
            return ServerSocketFactory.getDefault();
        }
    }

    /**
     * Create a socket factory
     *
     * @param secureTcp        true for secure socket factory, false for default
     * @param sslConfiguration SSL/TLS configuration
     * @return
     * @throws CertificateException
     * @throws UnrecoverableKeyException
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws KeyManagementException
     * @throws IOException
     */
    public static SocketFactory createSocketFactory(final boolean secureTcp,
                                                    final ISslConfiguration sslConfiguration) throws
                                                                                              CertificateException,
                                                                                              UnrecoverableKeyException,
                                                                                              NoSuchAlgorithmException,
                                                                                              KeyStoreException,
                                                                                              KeyManagementException,
                                                                                              IOException {
        if (secureTcp) {
            final SSLContext context = getSslContext(sslConfiguration);
            return context.getSocketFactory();
        } else {
            return SocketFactory.getDefault();
        }
    }

    /**
     * Set up the SSL Context
     *
     * @param sslConfiguration
     * @return
     * @throws KeyStoreException
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     * @throws UnrecoverableKeyException
     * @throws KeyManagementException
     */
    private static SSLContext getSslContext(final ISslConfiguration sslConfiguration) throws
                                                                                      KeyStoreException,
                                                                                      IOException,
                                                                                      NoSuchAlgorithmException,
                                                                                      CertificateException,
                                                                                      UnrecoverableKeyException,
                                                                                      KeyManagementException {
        // set up keystore
        final KeyStore keyStore = KeyStore.getInstance(sslConfiguration.getKeystoreType());
        try (final InputStream inputStream = new FileInputStream(sslConfiguration.getKeystorePath())) {
            keyStore.load(inputStream, sslConfiguration.getKeystorePassword().toCharArray());
        }
        final KeyManagerFactory keyManagerFactory = KeyManagerFactory
                .getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, sslConfiguration.getKeystorePassword().toCharArray());

        // set up truststore
        final KeyStore trustStore = KeyStore.getInstance(sslConfiguration.getTruststoreType());
        try (final InputStream inputStream = new FileInputStream(sslConfiguration.getTruststorePath())) {
            trustStore.load(inputStream, sslConfiguration.getTruststorePassword().toCharArray());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        final TrustManagerFactory trustManagerFactory = TrustManagerFactory
                .getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);

        // set up SSL context
        final SSLContext context = SSLContext.getInstance(sslConfiguration.getHttpsProtocol());

        context.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), new SecureRandom());
        return context;
    }
}
