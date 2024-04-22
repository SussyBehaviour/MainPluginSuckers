package Thisiscool.StuffForUs.menus.menu;
import Thisiscool.MainHelper.Action;
import Thisiscool.StuffForUs.menus.menu.Menu.MenuView;

public record MenuOption(String button, Action<MenuView> action) {
    public static MenuOption empty() {
        return new MenuOption("", Action.none());
    }

    public static MenuOption of(String button) {
        return new MenuOption(button, Action.none());
    }

    public static MenuOption of(String button, Action<MenuView> action) {
        return new MenuOption(button, action);
    }
}