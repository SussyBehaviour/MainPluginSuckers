package Thisiscool.StuffForUs.menus;

import arc.struct.ObjectMap;

public record State(ObjectMap<String, Object> map) {

    public static State create() {
        return new State(new ObjectMap<>());
    }

    public static <T> State create(StateKey<T> key, T value) {
        return new State(new ObjectMap<>()).put(key, value);
    }

    public <T> State put(StateKey<T> key, Object value) {
        map.put(key.name, value);
        return this;
    }

    public State remove(StateKey<?> key) {
        map.remove(key.name);
        return this;
    }
    public <T> T get(StateKey<T> key, Class<T> type) {
        Object value = map.get(key.name);
        if (value == null) {
            return null;
        }
        return type.cast(value);
    }
    
    public <T> T get(StateKey<T> key, T def, Class<T> type) {
        Object value = map.get(key.name, def);
        return type.cast(value);
    }

    public boolean contains(StateKey<?> key) {
        return map.containsKey(key.name);
    }

    public record StateKey<T>(String name) {}
}