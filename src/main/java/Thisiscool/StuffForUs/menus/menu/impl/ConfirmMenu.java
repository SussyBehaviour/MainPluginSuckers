package Thisiscool.StuffForUs.menus.menu.impl;

import Thisiscool.MainHelper.Action;
import Thisiscool.StuffForUs.menus.State;
import Thisiscool.StuffForUs.menus.State.StateKey;
import Thisiscool.StuffForUs.menus.menu.Menu;
import mindustry.gen.Player;

public class ConfirmMenu extends Menu {
    public static final StateKey<String>
            TITLE = new StateKey<>("title"),
            CONTENT = new StateKey<>("content");

    public static final StateKey<Object[]>
            VALUES = new StateKey<>("values");

    public static final StateKey<Runnable>
            CONFIRMED = new StateKey<>("confirmed"),
            DENIED = new StateKey<>("denied");

    public ConfirmMenu() {
        this("ui.button.yes", "ui.button.no");
    }

    public ConfirmMenu(String confirmButton, String denyButton) {
        this.transform(menu -> {
            menu.title(menu.state.get(TITLE, String.class));
            menu.content(menu.state.get(CONTENT, String.class), menu.state.get(VALUES, Object[].class));
    
            menu.option(confirmButton, Action.run(menu.state.get(CONFIRMED, Runnable.class)));
            menu.option(denyButton, Action.run(menu.state.get(DENIED, Runnable.class)));
        });
    }

    // region show

    public MenuView show(Player player, String title, String content, Runnable confirmed, Object... values) {
        return show(player, title, content, confirmed, () -> {}, values);
    }

    public MenuView show(Player player, String title, String content, Runnable confirmed, Runnable denied, Object... values) {
        return show(player, State.create().put(TITLE, title).put(CONTENT, content).put(VALUES, values).put(CONFIRMED, confirmed).put(DENIED, denied));
    }


    public MenuView show(Player player, MenuView parent, String title, String content, Runnable confirmed, Object... values) {
        return show(player, parent, title, content, confirmed, () -> {}, values);
    }

    public MenuView show(Player player, MenuView parent, String title, String content, Runnable confirmed, Runnable denied, Object... values) {
        return show(player, State.create().put(TITLE, title).put(CONTENT, content).put(VALUES, values).put(CONFIRMED, confirmed).put(DENIED, denied), parent);
    }

    // endregion
}