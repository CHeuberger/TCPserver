package cfh.tcp;

/**
 * Listener for Server events.
 * 
 * @author Carlos Heuberger
 * $Revision: 1.3 $
 */
public interface ServerListener {
    
    /**
     * Server started to listen.
     * 
     * @param server the Server instance
     */
    void started(Server server);
    
    /**
     * A new client connected to the server.
     * A new Connection is created and will be started after all
     * listeners have been called.
     * 
     * @param server the Server instance accepting the connection.
     * @param connection the new Connection.
     */
    void connected(Server server, Connection connection);
    
    /**
     * An exception was catched in the server.
     * The {@link #shutdown(Server)} method will also be called.
     * 
     * @param server the Server instance.
     * @param ex The Exception.
     */
    void handleException(Server server, Exception ex);
    
    /**
     * The server shut down, stopping to listen for new connections.
     * 
     * @param server the Server instance. 
     */
    void shutdown(Server server);
    
//  ############################################################################
    
    public class Adapter implements ServerListener {
        
        public void started(Server server) {
        }

        public void connected(Server server, Connection connection) {
        }

        public void handleException(Server server, Exception ex) {
        }

        public void shutdown(Server server) {
        }
    }
}
