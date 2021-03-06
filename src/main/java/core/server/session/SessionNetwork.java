package core.server.session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

/**
 * {@code SessionNetwork} contain all network information about
 * an active session.
 *
 * @author Thibault Meyer
 * @version 1.0.0
 * @since 1.0.0
 */
public class SessionNetwork {

    /**
     * Logger.
     *
     * @since 1.0.0
     */
    private static final Logger LOG = LoggerFactory.getLogger(SessionNetwork.class.getName());

    /**
     * Socket used by this session.
     *
     * @since 1.0.0
     */
    public SocketChannel socket;

    /**
     * Current selector.
     *
     * @since 1.0.0
     */
    public Selector selector;

    /**
     * Remote IP Address.
     *
     * @since 1.0.0
     */
    public String ip;

    /**
     * Remote address (ie: /127.0.0.1:40866).
     *
     * @since 1.0.0
     */
    public String address;

    /**
     * Remote port number.
     *
     * @since 1.0.0
     */
    public int port;

    /**
     * File descriptor used by the socket.
     *
     * @since 1.0.0
     */
    public long fd;

    /**
     * Determine is the current socket is registered for write event.
     *
     * @since 1.0.0
     */
    private boolean isWriteRegistered;

    /**
     * Default constructor.
     *
     * @since 1.0.0
     */
    public SessionNetwork() {
        this.isWriteRegistered = false;
    }

    /**
     * Register the "Write" event for the next NIO events.
     *
     * @since 1.0.0
     */
    public void registerWriteEvent() {
        if (!this.isWriteRegistered) {
            try {
                this.socket.register(this.selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                this.isWriteRegistered = true;
            } catch (ClosedChannelException e) {
                LOG.warn("can't register write event", e);
            }
        }
    }

    /**
     * Unregister the "Write" event from the next NIO events.
     *
     * @since 1.0.0
     */
    public void unregisterWriteEvent() {
        if (this.isWriteRegistered) {
            try {
                this.socket.register(this.selector, SelectionKey.OP_READ);
                this.isWriteRegistered = false;
            } catch (ClosedChannelException e) {
                LOG.warn("can't unregister write event", e);
            }
        }
    }
}
