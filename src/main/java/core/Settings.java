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
 * @since 1.0.0
 */
public final class Settings {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(Settings.class.getName());

    /**
     * The server will listen on this port.
     */
    public static Integer socketPort;

    /**
     * The time in seconds before inactive socket was closed.
     */
    public static Integer socketTTL;

    /**
     * The maximal number of connected socket.
     */
    public static Integer socketMaxConn;

    /**
     * The driver to use with the database.
     */
    public static String databaseDriver;

    /**
     * The URL used to connect to the database.
     */
    public static String databaseUrl;

    /**
     * Username used to database authentication.
     */
    public static String databaseUsername;

    /**
     * Password used to database authentication
     */
    public static String databasePassword;

    /**
     * Is this database server have built-in functions to check password ?
     */
    public static Boolean databaseBuiltIntFunction;

    /**
     * Maximal number of simultaneous session allowed with the same login.
     */
    public static Integer cfgMaxSessionPerLogin;

    /**
     * Static constructor.
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
