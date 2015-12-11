package core.network;

/**
 * Possible reasons for what a client socket has been closed and removed.
 *
 * @author Thibault Meyer
 * @version 1.0.0
 * @since 1.0.0
 */
public enum DisconnectReason {

    /**
     * No activity detected (RX or TX) during a too long period.
     *
     * @since 1.0.0
     */
    NO_ACTIVITY,

    /**
     * The client close the connection.
     *
     * @since 1.0.0
     */
    CLIENT_GONE_AWAY,

    /**
     * The client have too many session on this server.
     *
     * @since 1.0.0
     */
    TOO_MANY_SESSIONS,

    /**
     * Client disconnect from the application.
     *
     * @since 1.0.0
     */
    APPLICATION_REQUESTED,

    /**
     * Too many open connection.
     *
     * @since 1.0.0
     */
    TOO_MANY_CLIENTS
}
