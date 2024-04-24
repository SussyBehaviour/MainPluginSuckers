package Thisiscool.StuffForUs.menus.menu.impl;


import Thisiscool.MainHelper.Action;
import Thisiscool.StuffForUs.menus.State;
import Thisiscool.StuffForUs.menus.State.StateKey;
import Thisiscool.StuffForUs.menus.menu.Menu;
import arc.func.Func;
import mindustry.gen.Player;

public class PageMenu extends Menu {
    public static final StateKey<Integer>
            PAGE = new StateKey<>("page"),
            PAGES = new StateKey<>("pages");

    public static final StateKey<Func>
            TITLE = new StateKey<>("title"),
            CONTENT = new StateKey<>("content");

    public PageMenu() {
        this("ui.button.left", "ui.button.right", "ui.button.page", "ui.button.close");
    }

    public PageMenu(String leftButton, String rightButton, String pageButton, String closeButton) {
        this.transform(menu -> {
            menu.title((String) menu.state.get(TITLE, Func.class).get(menu.state.get(PAGE, Integer.class)));
            menu.content((String) menu.state.get(CONTENT, Func.class).get(menu.state.get(PAGE, Integer.class)));
    
            menu.option(leftButton, Action.showWith(PAGE, Math.max(1, menu.state.get(PAGE, Integer.class) - 1)));
            menu.option(pageButton, Action.show(), menu.state.get(PAGE, Integer.class), menu.state.get(PAGES, Integer.class));
            menu.option(rightButton, Action.showWith(PAGE, Math.min(menu.state.get(PAGE, Integer.class) + 1, menu.state.get(PAGES, Integer.class))));
    
            menu.row();
    
            menu.option(closeButton);
        }).followUp(true);
    }

    // region content function

    public MenuView show(Player player, int page, int pages, String title, Func<Integer, String> content) {
        return show(player, page, pages, newPage -> title, content);
    }

    public MenuView show(Player player, int page, int pages, Func<Integer, String> title, Func<Integer, String> content) {
        return show(player, State.create().put(PAGE, page).put(PAGES, pages).put(TITLE, title).put(CONTENT, content));
    }


    public MenuView show(Player player, MenuView parent, int page, int pages, String title, Func<Integer, String> content) {
        return show(player, parent, page, pages, newPage -> title, content);
    }

    public MenuView show(Player player, MenuView parent, int page, int pages, Func<Integer, String> title, Func<Integer, String> content) {
        return show(player, State.create().put(PAGE, page).put(PAGES, pages).put(TITLE, title).put(CONTENT, content), parent);
    }

    // endregion
}