package Thisiscool.StuffForUs.menus;

import Thisiscool.StuffForUs.menus.Interface.View;
import Thisiscool.StuffForUs.menus.State.StateKey;
import arc.Events;
import arc.func.Cons;
import arc.func.Cons2;
import arc.func.Cons3;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import mindustry.game.EventType.PlayerLeave;
import mindustry.gen.Player;


public abstract class Interface<V extends View> {
    public final ObjectMap<Player, V> views = new ObjectMap<>();
    public final Seq<Cons<V>> transformers = new Seq<>();

    public final int id = register();

    {
        Events.on(PlayerLeave.class, event -> views.remove(event.player));
    }

    // region abstract

    public abstract int register();

    public abstract V show(Player player, State state, View previous);
    public abstract void hide(Player player);

    // endregion
    // region show

    public V show(Player player) {
        return show(player, State.create());
    }

    public V show(Player player, State state) {
        return show(player, state, null);
    }

    public V show(View view) {
        return show(view.player, view.state, view.parent);
    }

    public <T> V show(Player player, StateKey<T> key, T value) {
        return show(player, State.create(key, value));
    }

    public <T1, T2> V show(Player player, StateKey<T1> key1, T1 value1, StateKey<T2> key2, T2 value2) {
        return show(player, State.create(key1, value1).put(key2, value2));
    }

    public <T1, T2, T3> V show(Player player, StateKey<T1> key1, T1 value1, StateKey<T2> key2, T2 value2, StateKey<T3> key3, T3 value3) {
        return show(player, State.create(key1, value1).put(key2, value2).put(key3, value3));
    }

    public V open(View parent) {
        return show(parent.player, parent.state, parent);
    }

    public <T> V open(View parent, StateKey<T> key, T value) {
        return show(parent.player, parent.state.put(key, value), parent);
    }

    public <T1, T2> V open(View parent, StateKey<T1> key1, T1 value1, StateKey<T2> key2, T2 value2) {
        return show(parent.player, parent.state.put(key1, value1).put(key2, value2), parent);
    }

    public <T1, T2, T3> V open(View parent, StateKey<T1> key1, T1 value1, StateKey<T2> key2, T2 value2, StateKey<T3> key3, T3 value3) {
        return show(parent.player, parent.state.put(key1, value1).put(key2, value2).put(key3, value3), parent);
    }

    // region transform

    public Interface<V> transform(Cons<V> transformer) {
        this.transformers.add(transformer);
        return this;
    }

    public <T1> Interface<V> transform(StateKey<T1> key, Class<T1> type, Cons2<V, T1> transformer) {
        return transform(view -> transformer.get(view, view.state.get(key, type)));
    }
    public <T1, T2> Interface<V> transform(StateKey<T1> key1, Class<T1> type1, StateKey<T2> key2, Class<T2> type2, Cons3<V, T1, T2> transformer) {
        return transform(view -> transformer.get(view, view.state.get(key1, type1), view.state.get(key2, type2)));
    }
    // endregion

    public abstract class View {
        public final Player player;
        public final State state;
        public final View parent;

        public View(Player player, State state, View parent) {
            this.player = player;
            this.state = state;
            this.parent = parent;
        }

        public Interface<V> getInterface() {
            return Interface.this;
        }
    }
}