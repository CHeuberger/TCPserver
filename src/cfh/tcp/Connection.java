package cfh.tcp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * TODO
 * 
 * @author Carlos Heuberger
 * $Revision: 1.12 $
 */
public class Connection {

    public static String getRevision() {
        return Connection.class.getName() + " $Revision: 1.12 $";
    }
    
    private final Socket socket;
    private final List<ConnectionListener> listeners;

    private final Receiver receiver;

    public Connection(String host, int port) throws IOException {
        socket = new Socket(host, port);

        listeners = new ArrayList<ConnectionListener>();
        receiver = new Receiver(socket);
}
    
    Connection(Socket socket) throws IOException {
        if (socket == null)
            throw new IllegalArgumentException("socket must not be null");
        
        this.socket = socket;

        listeners = new ArrayList<ConnectionListener>();
        receiver = new Receiver(socket);
    }

    public void start() {
        receiver.start();
    }

    public void stop() throws IOException {
        receiver.stop();
    }
    
    public void close() throws IOException {
        stop();
        socket.close();
    }

    public void sendData(byte[] data) throws IOException {
        if (data == null)
            throw new IllegalArgumentException("data must not be null");
        
        socket.getOutputStream().write(data);
        
        synchronized (listeners) {
            for (ConnectionListener listener : listeners) {
                listener.sentData(Connection.this, data);
            }
        }
    }
    
    public int getLocalPort() {
        return socket.getLocalPort();
    }
    
    public int getRemotePort() {
        return socket.getPort();
    }
    
    public SocketAddress getRemoteAddress() {
        return socket.getRemoteSocketAddress();
    }
    
    public void addListener(ConnectionListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    public void removeListener(ConnectionListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }
    
    
    private class Receiver implements Runnable {

        private final InputStream input;

        private Thread thread = null;
        private boolean shutdown = false;
        
        private final ByteArrayOutputStream buffer;

        Receiver(Socket socket) throws IOException {
            assert socket != null : "null socket";
            
            input = socket.getInputStream();
            buffer = new ByteArrayOutputStream(128);
        }

        synchronized void start() {
            if (shutdown)
                new IllegalStateException("can only be started once");
            if (thread == null) {
                thread = new Thread(this);
                thread.setDaemon(true);
                thread.setName("Receiver: " + socket.getPort());
            }
            thread.start();
        }

        synchronized void stop() throws IOException {
            shutdown = true;
            if (!socket.isOutputShutdown()) {
                socket.shutdownOutput();
            }
            if (!socket.isInputShutdown()) {
                socket.shutdownInput();
            }
            thread.interrupt();
        }

        @Override
        public void run() {
            List<ConnectionListener> copy;  // to allow list changes in the listener
            
            synchronized (listeners) {
                copy = new ArrayList<ConnectionListener>(listeners);
            }
            for (ConnectionListener listener : copy) {
                listener.started(Connection.this);
            }
            try {
                while (!shutdown) {
                    byte[] data = readData();
                    if (data.length == 0) {
                        break;
                    }
                    synchronized (listeners) {
                        copy = new ArrayList<ConnectionListener>(listeners);
                    }
                    for (ConnectionListener listener : copy) {
                        listener.receivedData(Connection.this, data);
                    }
                }
            } catch (IOException ex) {
                synchronized (listeners) {
                    copy = new ArrayList<ConnectionListener>(listeners);
                }
                for (ConnectionListener listener : copy) {
                    listener.handleException(Connection.this, ex);
                }
            } finally {
                shutdown = true;
                synchronized (listeners) {
                    copy = new ArrayList<ConnectionListener>(listeners);
                }
                for (ConnectionListener listener : copy) {
                    listener.shutdown(Connection.this);
                }
                try {
                    socket.close();
                } catch (IOException ex) {
                    synchronized (listeners) {
                        copy = new ArrayList<ConnectionListener>(listeners);
                    }
                    for (ConnectionListener listener : copy) {
                        listener.handleException(Connection.this, ex);
                    }
                }
            }
        }

        private byte[] readData() throws IOException {
            buffer.reset();
            do {
                int b = input.read();  // block
                if (b == -1) {
                    break;
                }
                buffer.write(b);
            } while (input.available() > 0);  // don't block
            return buffer.toByteArray();
        }
    }
}
