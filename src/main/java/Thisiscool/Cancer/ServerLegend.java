package Thisiscool.Cancer;

import java.nio.channels.ClosedSelectorException;

import Thisiscool.config.Config;
import arc.net.Connection;
import arc.net.DcReason;
import arc.net.NetListener;
import arc.net.Server;
import arc.util.Log;
import arc.util.Threads;
import lombok.Getter;
import lombok.SneakyThrows;

@Getter
public class ServerLegend extends Legend {
    public final Server server;
    public final int port;
    public ServerLegend(int port) {
        this.server = new Server(65536, 32768, serializer);
        this.port = port;
        this.server.addListener(new MainThreadListener(new ServerLegendListener()));
    }
    @Override
    @SneakyThrows
    public void connect() {
        Threads.daemon("Legend Server", () -> {
            try {
                server.run();
            } catch (ClosedSelectorException e) {
                Log.err("ClosedSelectorException caught: " + e.getMessage());
            } catch (Throwable e) {
                Log.err("Unexpected error: " + e.getMessage());
            }
        });
        server.bind(port);
    }
    @Override
    @SneakyThrows
    public void disconnect() {
        server.close();
    }

    @Override
    public void send(Object value) {
        bus.fire(value);
        if (isConnected()) server.sendToAllTCP(value);
    }

    public boolean isConnected() {
        return super.isConnected();
    }

    public class ServerLegendListener implements NetListener {
        @Override
        public void connected(Connection connection) {
            server.sendToTCP(connection.getID(), new LegendName(Config.getMode().displayName));
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
            server.sendToAllExceptTCP(connection.getID(), object);
        }
    }
}