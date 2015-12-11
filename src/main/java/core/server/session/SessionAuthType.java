package core.server.session;

/**
 * Possible session authentication type.
 *
 * @author Thibault Meyer
 * @version 1.0.1
 * @since 1.0.1
 */
public enum SessionAuthType {

    /**
     * Request authentication outside of the PIE.
     *
     * @since 1.0.1
     */
    EXTERNAL_AUTHENTICATION,

    /**
     * Request authentication from the PIE.
     *
     * @since 1.0.1
     */
    INTERNAL_AUTHENTICATION
}
