package Thisiscool.Cancer;

import java.nio.channels.ClosedSelectorException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import Thisiscool.config.Config;
import arc.net.Client;
import arc.net.Connection;
import arc.net.DcReason;
import arc.net.NetListener;
import arc.util.Log;
import arc.util.Threads;
import arc.util.Timer;
import lombok.Getter;
import lombok.SneakyThrows;

@Getter
public class ClientLegend extends Legend {
    private final Client client;
    private final int port;
    private boolean Connected;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    public ClientLegend(int port) {
        this.client = new Client(65536, 32768, serializer);
        this.port = port;
        this.client.addListener(new MainThreadListener(new ClientLegendListener()));

        Timer.schedule(() -> {
            if (!Connected && !isConnected()) {
                try {
                    connect();
                } catch (Throwable e) {
                    Log.err(e);
                }
            }
        }, 30f, 30f);
    }

    @Override
    @SneakyThrows
    public void connect() {
        executorService.submit(() -> {
            try {
                Threads.daemon("Legend Client", () -> {
                    try {
                        client.run();
                    } catch (ClosedSelectorException e) {
                        // ignore
                    } catch (Throwable e) {
                        Log.err(e);
                    }
                });
                client.connect(5000, "n1-uk.serphost.xyz", port);
                Connected = true;
            } catch (Exception e) {
                Log.err("Failed to connect to Legend server: " + e.getMessage());
            }
        });
    }

    @Override
    @SneakyThrows
    public void disconnect() {
        Connected = false;
        client.close();
    }

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
            client.sendTCP(new LegendName(Config.getMode().displayName));
        }
    
        @Override
        public void disconnected(Connection connection, DcReason reason) {
        }
        @Override
        public void received(Connection connection, Object object) {
            if (object instanceof LegendName name) {
                connection.setName(name.name());
                return;
            }
            bus.fire(object);
        }
    }
}