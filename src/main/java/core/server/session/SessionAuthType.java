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
     */
    EXTERNAL_AUTHENTICATION,

    /**
     * Request authentication from the PIE.
     */
    INTERNAL_AUTHENTICATION,
}
