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
     */
    NO_ACTIVITY,

    /**
     * The client close the connection.
     */
    CLIENT_GONE_AWAY,

    /**
     * The client have too many session on this server.
     */
    TOO_MANY_SESSIONS,

    /**
     * Client disconnect from the application.
     */
    APPLICATION_REQUESTED,

    /**
     * Too many open connection.
     */
    TOO_MANY_CLIENTS
}
