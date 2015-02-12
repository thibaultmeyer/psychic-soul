package core.server.session;

/**
 * Possible stage level.
 *
 * @author Thibault Meyer
 * @since 1.0.0
 */
public enum SessionStageLevel {

    /**
     * Newly created user.
     */
    NOT_AUTHENTICATED,

    /**
     * Request authentication outside of the PIE.
     */
    EXTERNAL_AUTHENTICATION,

    /**
     * Request authentication from the PIE.
     */
    INTERNAL_AUTHENTICATION,

    /**
     * User is authenticated! It can send request to the server and send
     * messages to other authenticated users.
     */
    AUTHENTICATED,

    /**
     * User is authenticated from external location! It can send request to
     * the server and send messages to other authenticated users.
     */
    AUTHENTICATED_EXTERNAL
}
