package mbean;

/**
 * PsychicAboutMBean.
 *
 * @author Thibault Meyer
 * @version 1.3.0
 * @since 1.3.0
 */
public interface PsychicAboutMBean {

    /**
     * Get the internal name.
     *
     * @return The internal name
     * @since 1.3.0
     */
    String getName();

    /**
     * Get the version number (ie: "1.3.0").
     *
     * @return The version number
     * @since 1.3.0
     */
    String getVersion();

    /**
     * Get the build date (ie: "2015-12-10T08:22:10UTC").
     *
     * @return The build date
     * @since 1.3.0
     */
    String getBuildDate();
}
