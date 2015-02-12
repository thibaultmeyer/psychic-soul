import core.server.NSServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Program entry point.
 *
 * @author Thibault Meyer
 * @since 1.0.0
 */
public class MainEntry {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(MainEntry.class.getName());

    /**
     * Static Constructor. Hack to programmically configure Log4J and set the
     * configuration file to use.
     */
    static {
        if (MainEntry.class.getResource("/log4j.xml") == null) {
            if (System.getProperty("log4j.configuration", null) == null) {
                try {
                    final Path path = Paths.get(MainEntry.class.getProtectionDomain().getCodeSource().getLocation().toURI());
                    System.setProperty("log4j.configuration", String.format("file:%s/log4j.xml", path.getParent()));
                } catch (URISyntaxException ignore) {
                }
            }
        }
        if (MainEntry.class.getResource("/configuration.properties") == null) {
            if (System.getProperty("core.configuration", null) == null) {
                try {
                    final Path path = Paths.get(MainEntry.class.getProtectionDomain().getCodeSource().getLocation().toURI());
                    System.setProperty("core.configuration", String.format("file:%s/configuration.properties", path.getParent()));
                } catch (URISyntaxException ignore) {
                }
            }
        }
        if (MainEntry.class.getResource("/commands.properties") == null) {
            if (System.getProperty("core.commands", null) == null) {
                try {
                    final Path path = Paths.get(MainEntry.class.getProtectionDomain().getCodeSource().getLocation().toURI());
                    System.setProperty("core.commands", String.format("file:%s/commands.properties", path.getParent()));
                } catch (URISyntaxException ignore) {
                }
            }
        }
    }

    /**
     * Entry point.
     *
     * @param args Arguments passed to the application
     */
    public static void main(final String[] args) {
        LOG.info("**************************");
        LOG.info("       Psychic Soul       ");
        LOG.info("**************************");
        final NSServer nsSrv = new NSServer();
        System.exit(nsSrv.run());
    }
}
