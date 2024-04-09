package Thisiscool.Cancer;

import java.nio.channels.ClosedSelectorException;

import arc.net.Connection;
import arc.net.DcReason;
import arc.net.NetListener;
import arc.util.Log;
import arc.util.Threads;
import lombok.Getter;
import lombok.SneakyThrows;
import mindustry.ui.dialogs.JoinDialog.Server;

@Getter
public class ServerLegend extends Legend {
    public final Server server;
    public final int port;

    public ServerLegend(int port) {
        this.server = new Server(65536, 32768, serializer);
        this.port = port;

        this.server.addListener(new MainThreadListener(new ServerLegendListener()));
    }

    /**
     * Binds the server to a specified port
     */
    @Override
    @SneakyThrows
    public void connect() {
        Threads.daemon("Legend Server", () -> {
            try {
                server.run();
            } catch (ClosedSelectorException e) {
                // ignore
            } catch (Throwable e) {
                Log.err(e);
            }
        });

        server.bind(port);
    }

    /**
     * Closes all connections, then stops the server
     */
    @Override
    @SneakyThrows
    public void disconnect() {
        server.close();
    }

    /**
     * Fires all listeners, then sends an object to all clients
     */
    @Override
    public void send(Object value) {
        bus.fire(value);
        if (isConnected()) server.sendToAllTCP(value);
    }

    /**
     * The ServerLegend is always connected, even if {@link Legend#connect()} wasn't called before
     */
    public boolean isConnected() {
        return super.isConnected();
    }

    public class ServerLegendListener implements NetListener {
        @Override
        public void connected(Connection connection) {
            if (connection == null) {
                Log.debug("[Legend Server] Null connection obtained");
                return;
            }

            try {
                if (!connection.getRemoteAddressTCP().getAddress().isLoopbackAddress()) {
                    connection.close(DcReason.closed);
                    return;
                }
            } catch (Exception e) {
                Log.debug("[Legend Server] Null address obtained");
                Log.debug(e);
                return;
            }

            server.sendToTCP(connection.getID(), new LegendName(name));
        }

        @Override
        public void disconnected(Connection connection, DcReason reason) {
            Log.info("[Legend Server] Client @ has disconnected. (@)", connection, reason);
        }

        @Override
        public void received(Connection connection, Object object) {
            if (object instanceof LegendName name) {
                connection.setName(name.name());
                Log.info("[Legend Server] Client @ has connected. (@)", connection, connection.getRemoteAddressTCP());
                return;
            }

            bus.fire(object);
            server.sendToAllExceptTCP(connection.getID(), object);
        }
    }
}