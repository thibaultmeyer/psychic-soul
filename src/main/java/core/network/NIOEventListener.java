package core.network;

import java.io.IOException;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

/**
 * Provide abstraction to the event callbacks of NIO Server class.
 *
 * @author Thibault Meyer
 * @see core.network.NIOServer
 * @since 1.0.0
 */
public interface NIOEventListener {

    /**
     * Called each time a new channel is accepted.
     *
     * @param selector The event selector
     * @param socket   The channel socket
     * @throws java.io.IOException If IO operation fail (like read/write on socket)
     */
    public abstract void onAcceptableEvent(Selector selector, SocketChannel socket) throws IOException;

    /**
     * Called each time a channel is ready to read.
     *
     * @param selector The event selector
     * @param socket   The channel socket
     * @return The number of read bytes
     * @throws java.io.IOException If IO operation fail (like read/write on socket)
     */
    public abstract int onReadableEvent(Selector selector, SocketChannel socket) throws IOException;

    /**
     * Called each time a channel is ready to write.
     *
     * @param selector The event selector
     * @param socket   The channel socket
     * @return The number of wrote bytes
     * @throws java.io.IOException If IO operation fail (like read/write on socket)
     */
    public abstract int onWritableEvent(Selector selector, SocketChannel socket) throws IOException;

    /**
     * Called each time the select() method timeout.
     *
     * @param selector The event selector
     * @throws java.io.IOException If IO operation fail (like read/write on socket)
     */
    public abstract void onTimeoutEvent(Selector selector) throws IOException;

    /**
     * Called each time after all selectors are processed (only when select()
     * returned value is superior to 0).
     *
     * @param selector The event selector
     * @throws java.io.IOException If IO operation fail (like read/write on socket)
     */
    public abstract void onFinalize(Selector selector) throws IOException;

    /**
     * Called when socket channel will be closed.
     *
     * @param socket       The socket channel
     * @param discoReason The disconnection reason
     * @throws java.io.IOException If IO operation fail (like read/write on socket)
     */
    public abstract void onDisconnected(SocketChannel socket, DisconnectReason discoReason) throws IOException;
}
