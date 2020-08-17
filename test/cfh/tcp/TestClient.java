/**
 * 
 */
package cfh.tcp;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TestClient {
        
        private final Socket socket;
        private final Connection connection;
        
        private final List<byte[]> received = new ArrayList<byte[]>();

        public TestClient(int port) throws IOException {
            socket = new Socket("localhost", port);
            connection = new Connection(socket);
            connection.addListener(new ConnListener());
            connection.start();
        }
        
        public Socket getSocket() {
            return socket;
        }
        
        public Connection getConnection() {
            return connection;
        }
        
        public void stop() throws IOException {
            connection.stop();
        }
        
        public void close() throws IOException {
            socket.close();
        }
        
        public void assertReceived(int count) throws InterruptedException {
            Thread.sleep(10);
            assertEquals(count, received.size());
        }
        
        public List<byte[]> getReceived() {
            return Collections.unmodifiableList(received);
        }
        
        public byte[] getLastReceived() {
            if (received.isEmpty())
                return null;
            else
                return received.get(received.size()-1);
        }
        
//  ----------------------------------------------------------------------------        
        private class ConnListener extends ConnectionListener.Adapter {
            @Override
            public void receivedData(Connection _, byte[] data) {
                received.add(data);
            }
        }
    }