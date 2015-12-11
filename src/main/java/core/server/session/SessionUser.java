package core.server.session;

/**
 * {@code SessionUser} contain all user information about
 * an active session.
 *
 * @author Thibault Meyer
 * @version 1.0.0
 * @since 1.0.0
 */
public class SessionUser {

    /**
     * Operating System name of the user attached to this session.
     *
     * @since 1.0.0
     */
    public final String operatingSystem;
    /**
     * Name of the user attached to this session.
     *
     * @since 1.0.0
     */
    public String login;
    /**
     * Group of the user attached to this session.
     *
     * @since 1.0.0
     */
    public String group;
    /**
     * Current location of the user attached to this session.
     *
     * @since 1.0.0
     */
    public String location;
    /**
     * Current name of the client used by the attached user.
     *
     * @since 1.0.0
     */
    public String clientName;
    /**
     * Timestamp when user get logged in the server.
     *
     * @since 1.0.0
     */
    public long loginTime;

    /**
     * Current status.
     * <pre>
     *     connection
     *     actif
     *     away
     *     idle
     *     lock
     *     server
     *     none
     * </pre>
     *
     * @since 1.0.0
     */
    public String state;

    /**
     * The timestamp when the status has been modified.
     *
     * @since 1.0.0
     */
    public long stateModifiedAt;

    /**
     * Client trust level.
     *
     * @since 1.0.0
     */
    public int trustLevelClient;

    /**
     * User trust level.
     *
     * @since 1.0.0
     */
    public int trustLevelUser;

    /**
     * Default constructor.
     *
     * @since 1.0.0
     */
    public SessionUser() {
        this.operatingSystem = "~";
        this.state = "connection";
        this.trustLevelClient = 1;
        this.trustLevelUser = 3;
        this.stateModifiedAt = System.currentTimeMillis() / 1000;
    }
}
