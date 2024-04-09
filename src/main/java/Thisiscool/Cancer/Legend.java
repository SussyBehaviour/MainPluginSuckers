package Thisiscool.Cancer;
import Thisiscool.Cancer.EventBus.EventSubscription;
import Thisiscool.Cancer.EventBus.Request;
import Thisiscool.Cancer.EventBus.RequestSubscription;
import Thisiscool.Cancer.EventBus.Response;
import arc.func.Cons;
import lombok.Getter;
import lombok.Setter;

@Getter
public abstract class Legend {

    protected final EventBus bus = new EventBus(this);
    protected final PacketSerializer serializer = new PacketSerializer(this);

    /**
     *  The name of this Legend instance
     */
    @Setter
    protected String name;

    /**
     * Creates a new instance of Legend
     *
     * @param port   the port to which this Legend will connect
     * @param server if true, creates ServerLegend, otherwise ClientLegend
     * @return a new instance of Legend
     */
    public static Legend create(int port, boolean server) {
        return server ? server(port) : client(port);
    }

    /**
     * Creates a new instance of ClientLegend
     *
     * @param port the port to which this ClientLegend will connect
     * @return a new instance of ClientLegend
     */
    public static ClientLegend client(int port) {
        return new ClientLegend(port);
    }

    /**
     * Creates a new instance of ServerLegend
     *
     * @param port the port on which this ServerLegend will bind
     * @return a new instance of ServerLegend
     */
    public static ServerLegend server(int port) {
        return new ServerLegend(port);
    }

    /**
     * Connects the current Legend
     */
    public abstract void connect();

    /**
     * Disconnects the current Legend
     */
    public abstract void disconnect();

    /**
     * Sends an object across the Legend network
     *
     * @param value the object to be sent
     */
    public abstract void send(Object value);

    /**
     * Subscribes this Legend to a value listener
     *
     * @param value    a value to subscribe
     * @param listener a listener to be called when this value is received
     * @return a subscription which can be unsubscribed or time limited
     */
    public <T> EventSubscription<T> run(T value, Runnable listener) {
        return bus.run(value, listener);
    }

    /**
     * Subscribes this Legend to a class listener
     *
     * @param type     a class to subscribe
     * @param listener a listener to be called when an object of this class is received
     * @return a subscription which can be unsubscribed or time limited
     */
    public <T> EventSubscription<T> on(Class<T> type, Cons<T> listener) {
        return bus.on(type, listener);
    }

    /**
     * Creates a response subscription and sends a request across the Legend network
     *
     * @param request  a request to be sent
     * @param listener a listener to be called when any response to this request is received
     * @return a subscription which can be unsubscribed or time limited
     */
    public <T extends Response> RequestSubscription<T> request(Request<T> request, Cons<T> listener) {
        var subscription = bus.request(request, listener);
        send(request);

        return subscription;
    }

    /**
     * Creates a response subscription and sends a request across the Legend network
     *
     * @param request  a request to be sent
     * @param listener a listener to be called when any response to this request is received
     * @param seconds  the number of seconds after which this request will expire
     * @return a subscription which can be unsubscribed or time limited
     */
    public <T extends Response> RequestSubscription<T> request(Request<T> request, Cons<T> listener, float seconds) {
        var subscription = bus.request(request, listener).withTimeout(seconds);
        send(request);

        return subscription;
    }

    /**
     * Creates a response subscription and sends a request across the Legend network
     *
     * @param request  a request to be sent
     * @param listener a listener to be called when any response to this request is received
     * @param expired  a listener to be called if no response is received for a certain time
     * @return a subscription which can be unsubscribed or time limited
     */
    public <T extends Response> RequestSubscription<T> request(Request<T> request, Cons<T> listener, Runnable expired) {
        var subscription = bus.request(request, listener).withTimeout(expired);
        send(request);

        return subscription;
    }

    /**
     * Creates a response subscription and sends a request across the Legend network
     *
     * @param request  a request to be sent
     * @param listener a listener to be called when any response to this request is received
     * @param seconds  the number of seconds after which this request will expire
     * @param expired  a listener to be called if no response is received for a certain time
     * @return a subscription which can be unsubscribed or time limited
     */
    public <T extends Response> RequestSubscription<T> request(Request<T> request, Cons<T> listener, Runnable expired, float seconds) {
        var subscription = bus.request(request, listener).withTimeout(seconds, expired);
        send(request);

        return subscription;
    }

    /**
     * Responds to a request across the Legend network
     *
     * @param request  a request to be responded
     * @param response a response to be sent
     */
    public <T extends Response> void respond(Request<T> request, T response) {
        response.setUuid(request.getUuid());
        send(response);
    }

    /**
     * @return whether this Legend instance is connected
     */
    public boolean isConnected() {
        return true;
    }

    /**
     * @return whether this Legend instance is a ServerLegend
     */
    public boolean isServer() {
        return this instanceof ServerLegend;
    }

    /**
     * @return whether this Legend instance is a ClientLegend
     */
    public boolean isClient() {
        return this instanceof ClientLegend;
    }
}