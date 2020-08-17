package cfh.tcp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

/**
 * Server for TCP connections.
 * 
 * @author Carlos Heuberger
 * $Revision: 1.12 $
 */
public class Server {
    
    public static String getRevision() {
        return Server.class.getName() + " $Revision: 1.12 $";
    }
    
    private final List<ServerListener> listeners;
    private List<Connection> connections;
    
    private final Acceptor acceptor;
    
    /**
     * Creates a new Server.
     * 
     * <P>The <code>port</code> must be between 0 and 65535, inclusive.
     * <BR>If the <code>backlog</code> value is equal or less
     * than 0, then the default value will be assumed.
     * <BR>If <code>bindAddr</code> is null, it will default accepting
     * connections on any/all local addresses.
     * 
     * @param port the local TCP port
     * @param backlog the listen backlog
     * @param bindAddr the local InetAddress the server will bind to
     * @throws IOException 
     */
    public Server(int port, int backlog, InetAddress bindAddr) {
        if (port < 0 || port > 0xFFFF)
            throw new IllegalArgumentException("Port: " + port);
        
        listeners = new ArrayList<ServerListener>();
        connections = new ArrayList<Connection>();
        acceptor = new Acceptor(port, backlog, bindAddr);
    }
    
    /**
     * Creates a new Server.
     * Uses the default <i>backlog</i> and accept connections on all 
     * local addresses.
     * 
     * <P>The <code>port</code> must be between 0 and 65535, inclusive.
     * 
     * @param port the local TCP port
     * @throws IOException 
     */
    public Server(int port) {
        this(port, 0, null);
    }
    
    public void start() throws IOException {
        acceptor.start();
    }
    
    public void stop() throws IOException {
        acceptor.stop();
    }
    
    public void sendData(byte[] data) throws IOException {
        if (data == null)
            throw new IllegalArgumentException("data must not be null");
        if (acceptor == null)
            throw new IllegalStateException("not running");
        
        List<Connection> copy;
        synchronized (connections) {
            copy = new ArrayList<Connection>(connections);
        }
        for (Connection c : copy) {
            c.sendData(data);
        }
    }
    
    public int getPort() {
        return acceptor.getPort();
    }
    
    public List<Connection> getConnections() {
        synchronized (connections) {
            return Collections.unmodifiableList(connections);
        }
    }
    
    public void addListener(ServerListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }
    
    public void removeListener(ServerListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }
    
//  ############################################################################
    
    /**
     * Waits for connections.
     */
    private class Acceptor implements Runnable {
        
        private final int port;
        private final int backlog;
        private final InetAddress bindAddr;
        
        private ServerSocket socket = null;
        
        private Thread thread = null;
        private boolean shutdown = false;
        
        private final CloseConnListener closeConnListener;
        
        /**
         * Creates a new Acceptor to wait for connections.
         * 
         * <P>The <code>port</code> must be between 0 and 65535, inclusive.
         * <BR>If the <code>backlog</code> value is equal or less
         * than 0, then the default value will be assumed.
         * <BR>If <code>bindAddr</code> is null, it will default accepting
         * connections on any/all local addresses.
         * 
         * @param port the local TCP port
         * @param backlog the listen backlog
         * @param bindAddr the local InetAddress the server will bind to
         * 
         * @throws  SecurityException if a security manager exists and 
         * its <code>checkListen</code> method doesn't allow the operation.
         * 
         * @throws IOException if an I/O error occurs when opening the socket.
         */
        protected Acceptor(int port, int backlog, InetAddress bindAddr) {
            this.port = port;
            this.backlog = backlog;
            this.bindAddr = bindAddr;
            
            closeConnListener = new CloseConnListener();
        }
        
        protected void start() throws IOException {
            if (shutdown)
                new IllegalStateException("can only be started once");
            if (socket != null)
                new IllegalStateException("already started");
            
            socket = new ServerSocket(port, backlog, bindAddr);
            if (thread == null) {
                thread = new Thread(this);
                thread.setDaemon(true);
                thread.setName("Acceptor:" + port);
            }
            thread.start();
        }
        
        protected void stop() throws IOException {
            shutdown = true;
            if (socket != null) {
                socket.close();
            }
            synchronized (connections) {
                ListIterator<Connection> iter = connections.listIterator();
                while (iter.hasNext()) {
                    Connection c = iter.next();
                    iter.remove();
                    c.stop();
                }
            }
        }
        
        protected int getPort() {
            return (socket != null) ? socket.getLocalPort() : port;
        }
        
        public void run() {
            List<ServerListener> copy;  // to allow list changes in the listener 
            
            synchronized (listeners) {
                copy = new ArrayList<ServerListener>(listeners);
            }
            for (ServerListener listener : copy) {
                listener.started(Server.this);
            }
            try {
                while (!shutdown) {
                    Socket client = socket.accept();
                    Connection connection = new Connection(client);
                    synchronized (connections) {
                        connections.add(connection);
                    }
                    connection.addListener(closeConnListener);
                    
                    synchronized (listeners) {
                        copy = new ArrayList<ServerListener>(listeners);
                    }
                    for (ServerListener listener : copy) {
                        listener.connected(Server.this, connection);
                    }
                    
                    connection.start();
                }
            } catch (IOException ex) {
                if (shutdown && (ex instanceof SocketException)) {
                    // ignore, socket was closed
                } else {
                    synchronized (listeners) {
                        copy = new ArrayList<ServerListener>(listeners);
                    }
                    for (ServerListener listener : copy) {
                        listener.handleException(Server.this, ex);
                    }
                }
            } finally {
                shutdown = true;
                synchronized (listeners) {
                    copy = new ArrayList<ServerListener>(listeners);
                }
                for (ServerListener listener : copy) {
                    listener.shutdown(Server.this);
                }
            }
        }
    }
    
//  ############################################################################
    
    private class CloseConnListener extends ConnectionListener.Adapter {
        @Override
        public void shutdown(Connection connection) {
            synchronized (connections) {
                connections.remove(connection);
            }
        }
    }
}
