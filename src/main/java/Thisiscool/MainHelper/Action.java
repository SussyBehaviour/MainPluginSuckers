package Thisiscool.MainHelper;

import Thisiscool.features.menus.Interface;
import Thisiscool.features.menus.Interface.View;
import Thisiscool.features.menus.State.StateKey;
import Thisiscool.features.menus.text.TextInput.TextInputView;
import arc.func.Cons;
import mindustry.gen.Call;

@FunctionalInterface
@SuppressWarnings("unchecked")
public interface Action<V extends View> extends Cons<V> {

    static <V extends View> Action<TextInputView> none() {
        return hide();
    }

    static <V extends View> Action<V> run(Runnable runnable) {
        return view -> runnable.run();
    }

    static <V extends View> Action<View> open(Interface<?> next) {
        return both(hide(), next::open);
    }

    static <V extends View, T> Action<View> openWith(Interface<?> next, StateKey<T> key, T value) {
        return both(hide(), view -> next.show(view.player, view.state.put(key, value), view));
    }

    static <V extends View, T> Action<View> openWithout(Interface<?> next, StateKey<T> key) {
        return both(hide(), view -> next.show(view.player, view.state.remove(key), view));
    }

    static <V extends View> Action<View> back() {
        return both(hide(), view -> {
            if (view.parent == null) return;
            view.parent.getInterface().show(view.player, view.state, view.parent.parent);
        });
    }

    static <V extends View> Action<V> show() {
        return view -> view.getInterface().show(view);
    }

    static <V extends View, T> Action<V> showWith(StateKey<T> key, T value) {
        return view -> view.getInterface().show(view.player, view.state.put(key, value), view.parent);
    }

    static <V extends View, T> Action<V> showWithout(StateKey<T> key) {
        return view -> view.getInterface().show(view.player, view.state.remove(key), view.parent);
    }

    static <V extends View> Action<V> hide() {
        return view -> view.getInterface().hide(view.player);
    }

    static <V extends View> Action<V> uri(String uri) {
        return view -> Call.openURI(view.player.con, uri);
    }

    static <V extends View> Action<V> connect(String ip, int port) {
        return view -> Call.connect(view.player.con, ip, port);
    }

    static <V extends View> Action<V> both(Action<V> first, Action<V> second) {
        return view -> {
            first.get(view);
            second.get(view);
        };
    }

    static <V extends View> Action<V> both(Action<V> first, Action<V> second, Action<V> third) {
        return view -> {
            first.get(view);
            second.get(view);
            third.get(view);
        };
    }

    default Action<V> then(Action<V> second) {
        return view -> {
            get(view);
            second.get(view);
        };
    }

    default Action<V> then(Action<V> second, Action<V> third) {
        return view -> {
            get(view);
            second.get(view);
            third.get(view);
        };
    }
}