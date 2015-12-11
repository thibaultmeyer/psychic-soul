package core.server.session;

/**
 * Possible stage level.
 *
 * @author Thibault Meyer
 * @version 1.0.0
 * @since 1.0.0
 */
public enum SessionStageLevel {

    /**
     * Newly created user.
     *
     * @since 1.0.0
     */
    NOT_AUTHENTICATED,

    /**
     * User start the authentication process.
     *
     * @since 1.0.0
     */
    AUTHENTICATION_REQUESTED,

    /**
     * User is authenticated! It can send request to the server and send
     * messages to other authenticated users.
     *
     * @since 1.0.0
     */
    AUTHENTICATED
}
