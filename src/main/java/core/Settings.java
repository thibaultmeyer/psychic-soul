package core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

/**
 * Settings of the Netsoul dedicated server.
 *
 * @author Thibault Meyer
 * @version 1.2.0
 * @since 1.0.0
 */
public final class Settings {

    /**
     * Logger.
     *
     * @since 1.0.0
     */
    private static final Logger LOG = LoggerFactory.getLogger(Settings.class.getName());

    /**
     * The server will listen on this port.
     *
     * @since 1.0.0
     */
    public static Integer socketPort;

    /**
     * The time in seconds before inactive socket was closed.
     *
     * @since 1.0.0
     */
    public static Integer socketTTL;

    /**
     * The maximal number of connected socket.
     *
     * @since 1.0.0
     */
    public static Integer socketMaxConn;

    /**
     * The driver to use with the database.
     *
     * @since 1.0.0
     */
    public static String databaseDriver;

    /**
     * The URL used to connect to the database.
     *
     * @since 1.0.0
     */
    public static String databaseUrl;

    /**
     * Username used to database authentication.
     *
     * @since 1.0.0
     */
    public static String databaseUsername;

    /**
     * Password used to database authentication.
     *
     * @since 1.0.0
     */
    public static String databasePassword;

    /**
     * Is this database server have built-in functions to check password ?
     *
     * @since 1.0.0
     */
    public static Boolean databaseBuiltIntFunction;

    /**
     * Maximal number of simultaneous session allowed with the same login.
     *
     * @since 1.0.0
     */
    public static Integer cfgMaxSessionPerLogin;

    /**
     * Kerberos 5 Debug mode
     *
     * @since 1.2.0
     */
    public static Boolean krb5Debug;

    /**
     * Kerberos 5 OID.
     *
     * @since 1.2.0
     */
    public static String krb5Oid;

    /**
     * Kerberos 5 Realm.
     *
     * @since 1.2.0
     */
    public static String krb5Realm;

    /**
     * Kerberos 5 KDC.
     *
     * @since 1.2.0
     */
    public static String krb5Kdc;

    /**
     * Kerberos 5 Password.
     *
     * @since 1.2.0
     */
    public static String krb5Password;

    /**
     * Kerberos 5 JAAS configuration file.
     *
     * @since 1.2.0
     */
    public static String krb5JaasFile;

    /**
     * Static constructor.
     *
     * @since 1.0.0
     */
    static {
        InputStream fis = null;
        try {
            if (System.getProperty("core.configuration", null) != null) {
                URL u = new URL(System.getProperty("core.configuration"));
                fis = u.openStream();
            } else {
                fis = Settings.class.getResourceAsStream("/configuration.properties");
            }
            Properties properties = new Properties();
            properties.load(fis);

            Settings.socketPort = Integer.valueOf(properties.getProperty("server.socket.port"));
            Settings.socketTTL = Integer.valueOf(properties.getProperty("server.socket.ttl"));
            Settings.socketMaxConn = Integer.valueOf(properties.getProperty("server.socket.maxconn"));
            Settings.databaseDriver = properties.getProperty("server.database.driver");
            Settings.databaseUrl = properties.getProperty("server.database.url");
            if (Settings.databaseUrl.contains("~")) {
                Settings.databaseUrl = Settings.databaseUrl.replace("~", System.getProperty("user.home"));
            }
            Settings.databaseUsername = properties.getProperty("server.database.username", null);
            Settings.databasePassword = properties.getProperty("server.database.password", null);
            Settings.databaseBuiltIntFunction = Boolean.valueOf(properties.getProperty("server.database.usebuiltin", "false"));
            Settings.cfgMaxSessionPerLogin = Integer.valueOf(properties.getProperty("server.config.max_sess_per_login"));
            Settings.krb5Debug = Boolean.valueOf(properties.getProperty("server.auth.krb5.debug", "false"));
            Settings.krb5Oid = properties.getProperty("server.auth.krb5.oid", null);
            Settings.krb5Realm = properties.getProperty("server.auth.krb5.realm", null);
            Settings.krb5Kdc = properties.getProperty("server.auth.krb5.kdc", null);
            Settings.krb5Password = properties.getProperty("server.auth.krb5.password", null);
            Settings.krb5JaasFile = properties.getProperty("server.auth.krb5.jaasfile", null);
        } catch (NumberFormatException e) {
            LOG.error("Can't parse settings file", e);
        } catch (Throwable e) {
            LOG.error("Can't open settings file", e);
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException ignore) {
            }
        }
    }
}
