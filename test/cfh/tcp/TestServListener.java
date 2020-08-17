/**
 * 
 */
package cfh.tcp;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

public class TestServListener implements ServerListener {
    
    private final List<Connection> connections = new ArrayList<Connection>();
    private final List<Exception> exceptions = new ArrayList<Exception>();
    private boolean wasShutdown = false;
    private boolean wasStarted = false;

    public void assertConnections(int count) throws InterruptedException {
        sleep();
        assertEquals("Connections", count, connections.size());
    }
    public void assertExceptions(int count) throws InterruptedException {
        sleep();
        assertEquals("Exceptions", count, exceptions.size());
    }
    public void assertWasStarted(boolean started) throws InterruptedException {
        sleep();
        assertEquals("Started", started, wasStarted);
    }
    public void assertWasShutdown(boolean shutdown) throws InterruptedException {
        sleep();
        assertEquals("Shutdown", shutdown, wasShutdown);
    }
    public void assertStatus(boolean started, boolean shutdown) throws InterruptedException {
        sleep();
        assertEquals("Started", started, wasStarted);
        assertEquals("Shutdown", shutdown, wasShutdown);
    }
    
    private void sleep() throws InterruptedException {
        Thread.sleep(10);
    }

    public void connected(Server server, Connection connection) {
        connections.add(connection);
    }
    public void handleException(Server server, Exception ex) {
        exceptions.add(ex);
    }
    public void shutdown(Server server) {
        wasShutdown = true;
    }
    public void started(Server server) {
        wasStarted = true;
    }
}