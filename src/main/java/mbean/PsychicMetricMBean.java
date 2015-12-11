package mbean;

/**
 * PsychicMetricMBean.
 *
 * @author Thibault Meyer
 * @version 1.3.0
 * @since 1.3.0
 */
public interface PsychicMetricMBean {

    /**
     * Get the number of connected sessions.
     *
     * @return The number of connected sessions
     * @since 1.3.0
     */
    int getConnectedSessionsCount();
}
