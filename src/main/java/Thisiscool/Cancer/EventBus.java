package Thisiscool.Cancer;

import java.util.UUID;

import arc.func.Cons;
import arc.net.FrameworkMessage;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Nullable;
import arc.util.Timer;
import arc.util.Timer.Task;
import lombok.Getter;
import lombok.Setter;

@Getter
@SuppressWarnings("unchecked")
public class EventBus {
    protected final Legend Legend;

    protected final ObjectMap<String, RequestSubscription<?>> requests = new ObjectMap<>();
    protected final ObjectMap<Class<?>, Seq<EventSubscription<?>>> events = new ObjectMap<>();

    public EventBus(Legend Legend) {
        this.Legend = Legend;
    }

    public <T> EventSubscription<T> run(T value, Runnable listener) {
        return on((Class<T>) value.getClass(), event -> {
            if (event.equals(value))
                listener.run();
        });
    }

    public <T> EventSubscription<T> on(Class<T> type, Cons<T> listener) {
        var listeners = events.get(type, Seq::new);

        var subscription = new EventSubscription<>(type, listener);
        listeners.add(subscription);

        return subscription;
    }

    public <T extends Response> RequestSubscription<T> request(Request<T> request, Cons<T> listener) {
        var subscription = new RequestSubscription<>(request.uuid, listener);
        requests.put(request.uuid, subscription);

        return subscription;
    }

    public <T> void fire(T value) {
        if (value instanceof FrameworkMessage) return;

        if (value instanceof Response response) {
            var subscription = requests.get(response.uuid);
            if (subscription == null) return;

            subscription.call(value);
            return;
        }

        var listeners = events.get(value.getClass());
        if (listeners == null) return;

        for (var subscription : listeners)
            subscription.call(value);
    }

    public boolean contains(Class<?> type) {
        return events.containsKey(type);
    }

    public void clear() {
        events.clear();
    }

    @Getter
    public abstract static class Subscription<T> {
        protected @Nullable Task task;

        public abstract void call(Object value);

        public Subscription<T> withTimeout(float seconds) {
            this.task = Timer.schedule(this::unsubscribe, seconds);
            return this;
        }

        public Subscription<T> withTimeout(Runnable expired) {
            return withTimeout(3f, expired);
        }

        public Subscription<T> withTimeout(float seconds, Runnable expired) {
            this.task = Timer.schedule(() -> {
                if (unsubscribe())
                    expired.run();
            }, seconds);

            return this;
        }

        public abstract boolean unsubscribe();
    }

    @Getter
    public abstract static class Request<T extends Response> {
        public String uuid;

        public Request() {
            this.uuid = UUID.randomUUID().toString();
        }
    }

    @Getter
    @Setter
    public abstract static class Response {
        private String uuid;
    }

    @Getter
    public class EventSubscription<T> extends Subscription<T> {
        protected final Class<T> type;
        protected final Cons<T> listener;

        EventSubscription(Class<T> type, Cons<T> listener) {
            this.type = type;
            this.listener = listener;
        }

        @Override
        public void call(Object value) {
            listener.get((T) value);
        }

        @Override
        public EventSubscription<T> withTimeout(float seconds) {
            super.withTimeout(seconds);
            return this;
        }

        @Override
        public EventSubscription<T> withTimeout(Runnable expired) {
            return withTimeout(3f, expired);
        }

        @Override
        public EventSubscription<T> withTimeout(float seconds, Runnable expired) {
            super.withTimeout(seconds, expired);
            return this;
        }

        @Override
        public boolean unsubscribe() {

            if (task != null && task.isScheduled())
                task.cancel(); // Cancel the expiration task just in case

            return events.containsKey(type) && events.get(type).remove(this, true);
        }
    }

    @Getter
    public class RequestSubscription<T extends Response> extends Subscription<T> {
        private final String uuid;
        private final Cons<T> listener;

        public int responses;

        RequestSubscription(String uuid, Cons<T> listener) {
            this.uuid = uuid;
            this.listener = listener;
        }

        @Override
        public void call(Object value) {
            responses++;
            listener.get((T) value);
        }

        @Override
        public RequestSubscription<T> withTimeout(float seconds) {
            super.withTimeout(seconds);
            return this;
        }

        @Override
        public RequestSubscription<T> withTimeout(Runnable expired) {
            return withTimeout(3f, expired);
        }

        @Override
        public RequestSubscription<T> withTimeout(float seconds, Runnable expired) {
            super.withTimeout(seconds, expired);
            return this;
        }

        @Override
        public boolean unsubscribe() {
            if (task != null && task.isScheduled())
                task.cancel(); // Cancel the expiration task just in case

            return requests.remove(uuid) != null && responses == 0;
        }
    }
}