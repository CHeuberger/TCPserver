package cfh.tcp;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.Socket;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class ServerTest {

    private static final int PORT = 1234;
    
    private Server server;
    private TestServListener testListener;
    
    @Before
    public void setUp() {
        server = new Server(PORT);
        testListener = new TestServListener();
        server.addListener(testListener);
    }

    @After
    public void tearDown() throws Exception {
        server.stop();
    }


    @Test
    public void testStart() throws Exception {
        testListener.assertStatus(false, false);
        try {
            new Socket("localhost", PORT);
            fail("not started but connected");
        } catch (IOException expected) { 
        }
        
        server.start();
        testListener.assertWasStarted(true);
        Socket socket = new Socket("localhost", PORT);
        assertEquals(true, socket.isConnected());
        socket.shutdownOutput();
    }

    @Test
    public void testStop() throws IOException, InterruptedException {
        server.start();
        testListener.assertStatus(true, false);
        Socket socket = new Socket("localhost", PORT);
        assertEquals(true, socket.isConnected());
        
        server.stop();
        testListener.assertWasShutdown(true);
        try {
            new Socket("localhost", PORT);
            fail("not stopped");
        } catch (IOException expected) {
        }
        
        server.stop();
    }

    @Test
    public void testSendData() throws IOException, InterruptedException {
        final byte[] data = "test1".getBytes();
        
        server.start();
        TestClient client = new TestClient(PORT);
        testListener.assertConnections(1);
        
        server.sendData(data);
        client.assertReceived(1);
        assertArrayEquals(data, client.getReceived().get(0));
    }

    @Test
    public void testGetPort() {
        assertEquals(PORT, server.getPort());
    }
    
    @Test
    public void testGetPort_Zero() throws IOException {
        final Server server0 = new Server(0);
        assertEquals(0, server0.getPort());
        
        server0.start();
        final int port0 = server0.getPort();
        assertTrue(port0 > 0);
        
        new Socket("localhost", port0);
    }

    @Test
    public void testGetConnections() throws IOException, InterruptedException {
        List<Connection> connections;
        
        server.start();
        connections = server.getConnections();
        assertEquals(true, connections.isEmpty());
        
        TestClient client1 = new TestClient(PORT);
        Thread.sleep(10);
        connections = server.getConnections();
        assertEquals(1, connections.size());
        
        new TestClient(PORT);
        connections = server.getConnections();
        Thread.sleep(10);
        assertEquals(2, connections.size());
        
        client1.stop();
        connections = server.getConnections();
        Thread.sleep(10);
        assertEquals(1, connections.size());
    }

    @Test
    public void testAddListener() throws IOException, InterruptedException {
        server.start();
        TestServListener listener = new TestServListener();
        
        listener.assertConnections(0);
        new TestClient(PORT);
        listener.assertConnections(0);
        
        server.addListener(listener);
        listener.assertConnections(0);
        new TestClient(PORT);
        listener.assertConnections(1);
    }

    @Test
    public void testRemoveListener() throws IOException, InterruptedException {
        server.start();
        TestServListener listener = new TestServListener();
        server.addListener(listener);
        
        listener.assertConnections(0);
        new TestClient(PORT);
        listener.assertConnections(1);
        
        server.removeListener(listener);
        listener.assertConnections(1);
        new TestClient(PORT);
        listener.assertConnections(1);
    }
}
