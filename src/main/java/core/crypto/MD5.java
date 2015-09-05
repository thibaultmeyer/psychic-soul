package core.crypto;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * This class simplifies the generation of MD5 hash in Java.
 *
 * @author Thibaut Meyer
 * @version 1.0.0
 * @since 1.0.0
 */
public final class MD5 {

    /**
     * Compute the MD5 hash of the string passed as argument.
     *
     * @param str The string that will be used to generate an MD5 hash
     * @return the MD5 hash
     * @throws java.lang.Error This version of Java does not support MD5
     */
    public static String hash(final String str) {
        final byte[] uniqueKey = str.getBytes();
        byte[] hash;

        try {
            hash = MessageDigest.getInstance("MD5").digest(uniqueKey);
        } catch (NoSuchAlgorithmException e) {
            throw new Error("This version of Java does not support MD5");
        }

        final StringBuilder hashString = new StringBuilder();
        for (byte aHash : hash) {
            String hex = Integer.toHexString(aHash);
            if (hex.length() == 1) {
                hashString.append('0');
                hashString.append(hex.charAt(hex.length() - 1));
            } else
                hashString.append(hex.substring(hex.length() - 2));
        }
        return hashString.toString();
    }
}
