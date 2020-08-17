package cfh.tcp;

/**
 * TODO
 * 
 * @author Carlos Heuberger
 * $Revision: 1.3 $
 */
public interface ConnectionListener {
    
    void started(Connection connection);
    
    void sentData(Connection connection, byte[] data);
    
    void receivedData(Connection connection, byte[] data);
    
    void shutdown(Connection connection);
    
    /**
     * An exception was catched in the connection.
     * The {@link #shutdown(Connection)} method will also be called.
     * 
     * @param connection the Connection instance.
     * @param ex The Exception.
     */
    void handleException(Connection connection, Exception ex);
    
//  ############################################################################
    
    public class Adapter implements ConnectionListener {
        
        public void started(Connection connection) {
        }

        public void receivedData(Connection connection, byte[] data) {
        }

        public void sentData(Connection connection, byte[] data) {
        }
        
        public void handleException(Connection connection, Exception ex) {
        }

        public void shutdown(Connection connection) {
        }
    }
}
