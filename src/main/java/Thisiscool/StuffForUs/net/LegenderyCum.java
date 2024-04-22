package Thisiscool.StuffForUs.net;

import static Thisiscool.config.Config.*;

import Thisiscool.Cancer.EventBus.Request;
import Thisiscool.Cancer.EventBus.Response;
import Thisiscool.Cancer.Legend;
import arc.func.Cons;
import arc.util.Log;

public class LegenderyCum {

    public static Legend LegenderyCum;

    public static void connect() {
        try {
            LegenderyCum = Legend.create(config.sockPort, config.mode.isMainServer);
            LegenderyCum.connect();
        } catch (Exception e) {
            Log.err("Failed to connect LegenderyCum", e);
        }
    }

    public static boolean isConnected() {
        return LegenderyCum.isConnected();
    }

    public static void send(Object value) {
        LegenderyCum.send(value);
    }

    public static <T> void on(Class<T> type, Cons<T> listener) {
        LegenderyCum.on(type, listener);
    }

    public static <T extends Response> void request(Request<T> request, Cons<T> listener) {
        LegenderyCum.request(request, listener).withTimeout(3f);
    }

    public static <T extends Response> void request(Request<T> request, Cons<T> listener, Runnable expired) {
        LegenderyCum.request(request, listener, expired).withTimeout(3f);
    }

    public static <T extends Response> void respond(Request<T> request, T response) {
        LegenderyCum.respond(request, response);
    }
}