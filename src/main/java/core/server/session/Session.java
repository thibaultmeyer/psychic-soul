package core.server.session;

import core.network.DisconnectReason;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * {@code Session} contain all information about an active session.
 *
 * @author Thibault Meyer
 * @version 1.2.0
 * @since 1.0.0
 */
public class Session {

    /**
     * Chunk size.
     *
     * @since 1.2.0
     */
    private static final int CHUNK_SIZE = 256;

    /**
     * Network information.
     *
     * @since 1.0.0
     */
    public final SessionNetwork network;

    /**
     * User information.
     *
     * @since 1.0.0
     */
    public final SessionUser user;

    /**
     * Input buffer.
     *
     * @since 1.0.0
     */
    public final List<String> inputBuffer;

    /**
     * Output buffer.
     *
     * @since 1.0.0
     */
    public final List<String> outputBuffer;

    /**
     * Compiled pattern used to create chunk.
     *
     * @since 1.2.0
     */
    private final Pattern splitPattern;

    /**
     * Random hash generated at connection.
     *
     * @since 1.0.0
     */
    public String hash;

    /**
     * The current stage level of this user session.
     *
     * @since 1.0.0
     */
    public SessionStageLevel stageLevel;

    /**
     * The authentication type of this session.
     *
     * @since 1.0.0
     */
    public SessionAuthType authType;

    /**
     * Disconnect user with given reason. This variable must stay at null.
     *
     * @since 1.0.0
     */
    public DisconnectReason disconnectReason;

    /**
     * When the last ping was sent to this session.
     *
     * @since 1.0.0
     */
    public Instant lastPingSent;

    /**
     * When the last ping was received from the connected client.
     *
     * @since 1.0.0
     */
    public Instant lastPingReceived;

    /**
     * Default constructor.
     *
     * @since 1.0.0
     */
    public Session() {
        this.network = new SessionNetwork();
        this.user = new SessionUser();
        this.stageLevel = SessionStageLevel.NOT_AUTHENTICATED;
        this.inputBuffer = new ArrayList<>();
        this.outputBuffer = new ArrayList<>();
        this.lastPingSent = Instant.now();
        this.lastPingReceived = Instant.now();
        this.splitPattern = Pattern.compile("(?<=\\G.{" + Session.CHUNK_SIZE + "})");
    }

    /**
     * Get the next complete command payload.
     *
     * @return The payload without the "\n" end of line
     * @since 1.0.0
     */
    public String[] getNextPayload() {
        String payload = "";
        boolean nextPayloadFound = false;

        while (!this.inputBuffer.isEmpty()) {
            final String data = this.inputBuffer.remove(0);
            if (data.contains("\n")) {
                nextPayloadFound = true;
                payload += data.substring(0, data.indexOf('\n'));
                if (data.length() != (data.indexOf('\n') + 1)) {
                    this.inputBuffer.add(0, data.substring(data.indexOf('\n') + 1));
                }
                break;
            } else {
                payload += data;
            }
        }
        if (!nextPayloadFound) {
            this.inputBuffer.add(0, payload);
            return null;
        }
        return payload.isEmpty() ? null : payload.trim().split("\\s+");
    }

    /**
     * Add data to the output buffer. Data will be split of n part of
     * size defined by {@code Session.CHUNK_SIZE}.
     *
     * @param data The data to append to the output buffer
     * @since 1.2.0
     */
    public void addOutputDataAsChunk(final String data) {
        if (this.disconnectReason == null) {
            Collections.addAll(this.outputBuffer, splitPattern.split(data));
        }
    }

    /**
     * Add data to the output buffer. Data will be split of n part of
     * size defined by {@code Session.CHUNK_SIZE}.
     *
     * @param data The collection of data to append to the output buffer
     * @since 1.2.0
     */
    public void addOutputDataAsChunk(final Collection<? extends String> data) {
        if (this.disconnectReason == null) {
            for (final String s : data) {
                Collections.addAll(this.outputBuffer, splitPattern.split(s));
            }
        }
    }
}
