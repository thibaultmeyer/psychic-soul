package mbean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * PsychicAbout.
 *
 * @author Thibault Meyer
 * @version 1.3.0
 * @since 1.3.0
 */
public class PsychicAbout implements PsychicAboutMBean {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(PsychicAbout.class.getName());

    /**
     * The name of the project.
     */
    private String name;

    /**
     * The version number.
     */
    private String version;

    /**
     * The build date.
     */
    private String buildDate;

    /**
     * Build a basic instance.
     *
     * @since 1.3.0
     */
    public PsychicAbout() {
        final InputStream fis = PsychicAbout.class.getResourceAsStream("/version.properties");
        try {
            final Properties properties = new Properties();
            properties.load(fis);
            this.name = properties.getOrDefault("MAVEN_PROJECT_NAME", "psychic-soul").toString();
            this.version = properties.getOrDefault("MAVEN_PROJECT_VERSION", "0.0.0").toString();
            this.buildDate = properties.getOrDefault("MAVEN_PROJECT_BUILD", "1970-01-01T00:00:00UTC").toString();
        } catch (IOException ignore) {
            this.name = "psychic-soul";
            this.version = "0.0.0";
            this.buildDate = "1970-01-01T00:00:00UTC";
        }
        try {
            fis.close();
        } catch (IOException ignore) {
        }
    }

    @Override
    public String getName() {
        LOG.trace("getName");
        return this.name;
    }

    @Override
    public String getVersion() {
        LOG.trace("getVersion");
        return this.version;
    }

    @Override
    public String getBuildDate() {
        LOG.trace("getBuildDate");
        return this.buildDate;
    }
}
