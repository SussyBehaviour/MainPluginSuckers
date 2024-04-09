package Thisiscool.Cancer;
import java.nio.channels.ClosedSelectorException;

import arc.net.Client;
import arc.net.DcReason;
import arc.net.NetListener;
import arc.util.Log;
import arc.util.Threads;
import arc.util.Timer;
import lombok.Getter;
import lombok.SneakyThrows;
import reactor.netty.Connection;

@Getter
public class ClientLegend extends Legend {
    private final Client client;
    private final int port;

    private boolean wasConnected;

    public ClientLegend(int port) {
        this.client = new Client(65536, 32768, serializer);
        this.port = port;

        this.client.addListener(new MainThreadListener(new ClientLegendListener()));

        Timer.schedule(() -> {
            if (!wasConnected || isConnected()) return;

            Log.info("[Legend Client] Trying to reconnect to Legend server...");

            try {
                connect();
            } catch (Throwable e) {
                Log.err(e);
            }
        }, 30f, 30f);
    }

    /**
     * Connects to the server on a specified port
     */
    @Override
    @SneakyThrows
    public void connect() {
        Threads.daemon("Legend Client", () -> {
            try {
                client.run();
            } catch (ClosedSelectorException e) {
                // ignore
            } catch (Throwable e) {
                Log.err(e);
            }
        });

        wasConnected = true;
        client.connect(5000, "localhost", port);
    }

    /**
     * Disconnects from the server
     */
    @Override
    @SneakyThrows
    public void disconnect() {
        wasConnected = false;
        client.close();
    }

    /**
     * Fires all listeners, then sends an object to the server
     */
    @Override
    public void send(Object value) {
        bus.fire(value);
        if (isConnected()) client.sendTCP(value);
    }

    @Override
    public boolean isConnected() {
        return client.isConnected();
    }

    public class ClientLegendListener implements NetListener {
        @Override
        public void connected(Connection connection) {
            client.sendTCP(new LegendName(name));
        }

        @Override
        public void disconnected(Connection connection, DcReason reason) {
            Log.info("[Legend Client] Disconnected from server @. (@)", connection, reason);
        }

        @Override
        public void received(Connection connection, Object object) {
            if (object instanceof LegendName name) {
                connection.setName(name.name());
                Log.info("[Legend Client] Connected to server @. (@)", connection, connection.getRemoteAddressTCP());
                return;
            }

            bus.fire(object);
        }
    }
}