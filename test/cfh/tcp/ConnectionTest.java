package cfh.tcp;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ConnectionTest {

    private TestServer testServer;
    private Socket serverSocket;
    private Connection testConnection;
    private Socket clientSocket;
    private TestConnListener testConnListener;

    @Before
    public void setup() throws Exception {
        testServer = new TestServer();
        testConnListener = new TestConnListener();
        Future<Socket> connected = testServer.start();
        clientSocket = new Socket("127.0.0.1", testServer.getPort());
        testConnection = new Connection(clientSocket);
        testConnection.addListener(testConnListener);
        serverSocket = connected.get(100, MILLISECONDS);
    }
    
    @After
    public void tearDown() throws Exception {
        testConnection.stop();
        testServer.stop();
        testConnection.close();
        serverSocket.close();
    }
    

    @Test
    public void testConnection_HostPort() throws Exception {
        final String msg = "test 123";
        final Future<Socket> connected = testServer.start();
        final Connection connection = new Connection("localhost", testServer.getPort());
        connection.start();
        connected.get(100, MILLISECONDS);
        
        assertTrue(testServer.input.available() == 0);
        connection.sendData(msg.getBytes());
        sleep();
        assertTrue(testServer.input.available() > 0);
    }
    
    @Test
    public void testConnection_Socket() throws Exception {
        final String msg = "test 123";
        final Future<Socket> connected = testServer.start();
        final Socket socket = new Socket("localhost", testServer.getPort());
        final Connection connection = new Connection(socket);
        connection.start();
        connected.get(100, MILLISECONDS);
        
        assertTrue(testServer.input.available() == 0);
        connection.sendData(msg.getBytes());
        sleep();
        assertTrue(testServer.input.available() > 0);
    }
    
    @Test
    public void testStart() throws Exception {
        assertTrue(testServer.input.available() == 0);
        testServer.output.write("test1".getBytes());
        sleep();
        assertTrue(clientSocket.getInputStream().available() > 0);
        
        testConnListener.assertWasStarted(false);
        testConnection.start();
        testConnListener.assertWasStarted(true);
        testConnListener.assertReceived(1);
        assertTrue(testServer.input.available() == 0);
    }

    @Test(expected=IllegalThreadStateException.class)
    public void testStart_SecondTime() {
        testConnection.start();
        testConnListener.assertWasStarted(true);
        testConnection.start();
    }
    
    @Test(expected=IllegalThreadStateException.class)
    public void testStart_Restart() throws Exception {
        testConnection.start();
        testConnListener.assertWasStarted(true);
        testConnection.stop();
        testConnection.start();
    }
    
    @Test
    public void testStop() throws Exception {
        testConnection.start();
        testConnListener.assertStatus(true, false);
        
        testConnection.stop();
        sleep();
        testServer.output.write("test2".getBytes());
        testConnListener.assertReceived(0);
        
        testConnection.stop();
    }
    
    @Test
    public void testClose() throws IOException {
        testConnection.start();
        testConnListener.assertStatus(true, false);
        
        testConnection.close();
        sleep();
        testServer.output.write("test2".getBytes());
        testConnListener.assertReceived(0);
    }

    @Test
    public void testSendData() throws Exception {
        final byte[] data = "test2".getBytes();
        testConnection.start();
        testConnListener.assertSent(0);
        
        testConnection.sendData(data);
        testConnListener.assertSent(1);
        byte[] received = new byte[data.length];
        int len = testServer.input.read(received);
        assertEquals(5, len);
        assertArrayEquals(data, received);
    }

    @Test
    public void testGetLocalPort() {
        testConnection.start();
        assertEquals(clientSocket.getLocalPort(), testConnection.getLocalPort());
    }

    @Test
    public void testGetRemoteAddress() {
        testConnection.start();
        SocketAddress address = testConnection.getRemoteAddress();
        InetSocketAddress serverAddress = new InetSocketAddress("localhost", testServer.getPort());
        assertEquals(serverAddress, address);
    }

    @Test
    public void testAddListener() throws Exception {
        TestConnListener listener = new TestConnListener();
        listener.assertExceptions(0);
        listener.assertReceived(0);
        listener.assertSent(0);
        listener.assertStatus(false, false);
        
        testConnection.addListener(listener);
        listener.assertExceptions(0);
        listener.assertReceived(0);
        listener.assertSent(0);
        listener.assertStatus(false, false);
        
        testConnection.start();
        listener.assertWasStarted(true);
        testConnection.sendData("test4".getBytes());
        listener.assertSent(1);
        testServer.output.write("test5".getBytes());
        listener.assertReceived(1);
        testServer.socket.shutdownOutput();
        listener.assertWasShutdown(true);
    }

    @Test
    public void testRemoveListener() throws Exception {
        TestConnListener listener = new TestConnListener();
        listener.assertExceptions(0);
        listener.assertReceived(0);
        listener.assertSent(0);
        listener.assertStatus(false, false);
        
        testConnection.addListener(listener);
        listener.assertExceptions(0);
        listener.assertReceived(0);
        listener.assertSent(0);
        listener.assertStatus(false, false);
        
        testConnection.start();
        listener.assertWasStarted(true);
        testConnection.sendData("test4".getBytes());
        listener.assertSent(1);
        
        testConnection.removeListener(listener);
        testConnection.sendData("test4".getBytes());
        listener.assertSent(1);
        testServer.socket.shutdownOutput();
        listener.assertWasShutdown(false);
    }
    
    private static void sleep() {
        try {
            Thread.sleep(10);
        } catch (InterruptedException ignored) {
        }
    }
    
//  ############################################################################
    
    private static class TestServer {
        final ServerSocket server;
        Socket socket = null;
        InputStream input = null;
        OutputStream output = null;
        
        TestServer() throws IOException {
            server = new ServerSocket(0);
        }
        
        Future<Socket> start() {
            Callable<Socket> waiter = new Callable<Socket>() {
                @Override
                public Socket call() throws Exception {
                    return waitConnection();
                }
            };
            return Executors.newSingleThreadExecutor().submit(waiter);
        }
        
        void stop() throws IOException {
            server.close();
        }
        
        int getPort() {
            return server.getLocalPort();
        }
        
        private Socket waitConnection() throws IOException {
            socket = server.accept();
            input = socket.getInputStream();
            output = socket.getOutputStream();
            return socket;
        }
    }
    
//  ============================================================================
    
    private static class TestConnListener implements ConnectionListener {
        final List<byte[]> received = new ArrayList<byte[]>();
        final List<byte[]> sent = new ArrayList<byte[]>();
        final List<Exception> exceptions = new ArrayList<Exception>();
        boolean wasStarted = false;
        boolean wasShutdown = false;
        
        void assertReceived(int count) {
            sleep();
            assertEquals("Received", count, received.size());
        }
        void assertSent(int count) {
            sleep();
            assertEquals("Sent", count, sent.size());
        }
        void assertExceptions(int count) {
            sleep();
            assertEquals("Exceptions", count, exceptions.size());
        }
        void assertWasStarted(boolean started) {
            sleep();
            assertEquals("Started", started, wasStarted);
        }
        void assertWasShutdown(boolean shutdown) {
            sleep();
            assertEquals("Shutdown", shutdown, wasShutdown);
        }
        void assertStatus(boolean started, boolean shutdown) {
            sleep();
            assertEquals("Started", started, wasStarted);
            assertEquals("Shutdown", shutdown, wasShutdown);
        }
        
        @Override
        public void handleException(Connection connection, Exception ex) {
            exceptions.add(ex);
        }
        @Override
        public void receivedData(Connection connection, byte[] data) {
            received.add(data);
        }
        @Override
        public void sentData(Connection connection, byte[] data) {
            sent.add(data);
        }
        @Override
        public void started(Connection connection) {
            wasStarted = true;
        }
        @Override
        public void shutdown(Connection connection) {
            wasShutdown = true;
        }
    }
}
