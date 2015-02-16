package core.server.session;

/**
 * {@code SessionUser} contain all user information about
 * an active session.
 *
 * @author Thibault Meyer
 * @since 1.0.0
 */
public class SessionUser {

    /**
     * Name of the user attached to this session.
     */
    public String login;

    /**
     * Group of the user attached to this session.
     */
    public String group;

    /**
     * Current location of the user attached to this session.
     */
    public String location;

    /**
     * Current name of the client used by the attached user.
     */
    public String clientName;

    /**
     * Operating System name of the user attached to this session.
     */
    public String operatingSystem;

    /**
     * Timestamp when user get logged in the server.
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
     */
    public String state;

    /**
     * The timestamp when the status has been modified.
     */
    public long stateModifiedAt;

    /**
     * Client trust level.
     */
    public int trustLevelClient;

    /**
     * User trust level.
     */
    public int trustLevelUser;

    /**
     * Default constructor.
     */
    public SessionUser() {
        this.operatingSystem = "~";
        this.state = "connection";
        this.trustLevelClient = 1;
        this.trustLevelUser = 3;
        this.stateModifiedAt = System.currentTimeMillis() / 1000;
    }
}
