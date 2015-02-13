package core.server.session;

import core.Settings;
import core.network.DisconnectReason;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * {@code Session} contain all information about an active session.
 *
 * @author Thibault Meyer
 * @since 1.0.0
 */
public class Session {

    /**
     * Network information.
     */
    public SessionNetwork network;

    /**
     * Random hash generated at connection.
     */
    public String hash;

    /**
     * The current stage level of this user session.
     */
    public SessionStageLevel stageLevel;

    /**
     * User information.
     */
    public SessionUser user;

    /**
     * Input buffer
     */
    public List<String> inputBuffer;

    /**
     * Output buffer
     */
    public List<String> outputBuffer;

    /**
     * Disconnect user with given reason. This variable must stay at null.
     */
    public DisconnectReason disconnectReason;

    /**
     * When the last ping was sent to this session
     */
    public Instant lastPingSent;

    /**
     * Default constructor.
     */
    public Session() {
        this.network = new SessionNetwork();
        this.user = new SessionUser();
        this.stageLevel = SessionStageLevel.NOT_AUTHENTICATED;
        this.inputBuffer = new ArrayList<String>();
        this.outputBuffer = new ArrayList<String>();
        this.lastPingSent = Instant.now();
    }

    /**
     * Get the next complete command payload.
     *
     * @return The payload without the "\n" end of line
     */
    public String[] getNextPayload() {
        String payload = "";

        while (!this.inputBuffer.isEmpty()) {
            final String data = this.inputBuffer.remove(0);
            if (data.contains("\n")) {
                payload += data.substring(0, data.indexOf('\n'));
                if (data.length() != (data.indexOf('\n') + 1)) {
                    this.inputBuffer.add(0, data.substring(data.indexOf('\n') + 1));
                }
                break;
            } else {
                payload += data;
            }
        }
        return payload.isEmpty() ? null : payload.trim().split("\\s+");
    }
}
