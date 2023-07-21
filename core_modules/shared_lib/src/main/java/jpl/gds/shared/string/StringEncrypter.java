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

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.spec.KeySpec;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.IvParameterSpec;

import org.apache.commons.codec.binary.Base64;


/**
 * Class StringEncrypter.
 *
 * Support for encrypting and decrypting strings. The encrypted string is in
 * string form as a base-64 integer. The encryption scheme is DES.
 *
 */
public class StringEncrypter extends Object
{
    private static final String UNICODE_FORMAT = "UTF8";

    private static final String ENCRYPTION_SCHEME     = "DESede";
    private static final String TRANSFORMATION_SCHEME =
                                    "DESede/CBC/PKCS5Padding";
    private static final byte[] INITIAL_VALUE =
        { (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0,
          (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0};

    // Must be at least 24 characters long.
    private static final byte[] ENCRYPTION_KEY =
        getBytes(StringEncrypter.class.getCanonicalName() + ".ENCRYPTION_KEY");

    private final SecretKey _secretKey;
    private final Cipher    _cipher;

    
    /**
     * Construct a string encrypter/decrypter.
     *
     * @throws GeneralSecurityException Security error
     */
    public StringEncrypter() throws GeneralSecurityException
    {
        super();

        final KeySpec          keySpec    =
            new DESedeKeySpec(ENCRYPTION_KEY);
        final SecretKeyFactory keyFactory =
            SecretKeyFactory.getInstance(ENCRYPTION_SCHEME);

        _secretKey = keyFactory.generateSecret(keySpec);
        _cipher    = Cipher.getInstance(TRANSFORMATION_SCHEME);
    }


    /**
     * Encrypt string.
     *
     * @param unencryptedString String to encrypt
     *
     * @return Encrypted string
     *
     * @throws GeneralSecurityException Security error
     */
    public String encrypt(final String unencryptedString)
        throws GeneralSecurityException
    {
        if ((unencryptedString == null) ||
            (unencryptedString.length() == 0))
        {
            throw new IllegalArgumentException(
                          "unencryptedString was null or empty");
        }

        _cipher.init(Cipher.ENCRYPT_MODE, _secretKey,
                     new IvParameterSpec(INITIAL_VALUE));

        final byte[] cleartext  = getBytes(unencryptedString);
        final byte[] ciphertext = _cipher.doFinal(cleartext);

        return bytesAsString(ciphertext);
    }


    /**
     * Decrypt string.
     *
     * @param encryptedString String to decrypt.
     *
     * @return Decrypted string
     *
     * @throws GeneralSecurityException Security error
     */
    public String decrypt(final String encryptedString)
        throws GeneralSecurityException
    {
        if ((encryptedString == null) ||
            (encryptedString.length() == 0))
        {
            throw new IllegalArgumentException(
                          "encryptedString was null or empty");
        }

        _cipher.init(Cipher.DECRYPT_MODE, _secretKey,
                     new IvParameterSpec(INITIAL_VALUE));

        final byte[] ciphertext = bytesFromString(encryptedString);
        final byte[] cleartext  = _cipher.doFinal(ciphertext);

        return new String(cleartext);
    }

    /**
     * Convert byte array, considered as an integer, into a base-64
     * integer represented as a string.
     *
     * @param bytes byte array to convert
     *
     * @return String representing the integer
     */
    private static String bytesAsString(final byte[] bytes)
    {
    	return new String(Base64.encodeBase64(bytes));
    }

    /**
     * Convert base-64 integer represented as a string to an integer in
     * the form of a byte array.
     *
     * @param s string representing an integer
     *
     * @return Byte array representing the integer
     */
    private static byte[] bytesFromString(final String s)
    {
    	return Base64.decodeBase64(getBytes(s));
    }

    /**
     * Turn a string to UTF-8 bytes. We do not expect the exception
     * to be thrown.
     *
     * @param s the string to convert
     *
     * @return UTF-8 byte array created from the string
     */
    private static byte[] getBytes(final String s)
    {
        try
        {
            return s.getBytes(UNICODE_FORMAT);
        }
        catch (final UnsupportedEncodingException uee)
        {
            throw new RuntimeException(uee);
        }
    }


    /**
     * Main application.
     * 
     * @param args array of argument strings.
     *
     * @throws Exception On error
     */
    public static void main(final String[] args) throws Exception
    {
        final StringEncrypter se = new StringEncrypter();

        while (true)
        {
            final char[] pw = PasswordField.getPassword(System.in, "Password:");

            if ((pw == null) || (pw.length == 0))
            {
                break;
            }

            System.out.println("\n\nEncrypted password:");
            System.out.println(se.encrypt(new String(pw)));

            Arrays.fill(pw, ' ');
        }

        System.exit(0);
    }
}
