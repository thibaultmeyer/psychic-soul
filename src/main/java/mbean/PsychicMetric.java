package mbean;

import core.server.NSServer;

/**
 * PsychicMetric.
 *
 * @author Thibault Meyer
 * @version 1.3.0
 * @since 1.3.0
 */
public class PsychicMetric implements PsychicMetricMBean {

    /**
     * Handle to the current Netsoul server.
     */
    private final NSServer nsServer;

    /**
     * Build a basic instance.
     *
     * @param nsServer The {@code NSServer} instance to use
     * @since 1.3.0
     */
    public PsychicMetric(final NSServer nsServer) {
        this.nsServer = nsServer;
    }

    /**
     * Get the number of connected sessions.
     *
     * @return The number of connected sessions
     * @since 1.3.0
     */
    @Override
    public int getConnectedSessionsCount() {
        return nsServer.getConnectedSessionsCount();
    }
}
