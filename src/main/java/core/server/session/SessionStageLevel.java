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
     * User start the authentication process.
     */
    AUTHENTICATION_REQUESTED,

    /**
     * User is authenticated! It can send request to the server and send
     * messages to other authenticated users.
     */
    AUTHENTICATED,
}
