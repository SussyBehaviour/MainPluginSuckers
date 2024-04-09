package Thisiscool.features.net;

import static Thisiscool.config.Config.*;

import com.ospx.sock.EventBus.Request;
import com.ospx.sock.EventBus.Response;
import com.ospx.sock.Sock;

import arc.func.Cons;
import arc.util.Log;

public class Socket {

    public static Sock socket;

    public static void connect() {
        try {
            socket = Sock.create(config.sockPort, config.mode.isMainServer);
            socket.connect();
        } catch (Exception e) {
            Log.err("Failed to connect socket", e);
        }
    }

    public static boolean isConnected() {
        return socket.isConnected();
    }

    public static void send(Object value) {
        socket.send(value);
    }

    public static <T> void on(Class<T> type, Cons<T> listener) {
        socket.on(type, listener);
    }

    public static <T extends Response> void request(Request<T> request, Cons<T> listener) {
        socket.request(request, listener).withTimeout(3f);
    }

    public static <T extends Response> void request(Request<T> request, Cons<T> listener, Runnable expired) {
        socket.request(request, listener, expired).withTimeout(3f);
    }

    public static <T extends Response> void respond(Request<T> request, T response) {
        socket.respond(request, response);
    }
}