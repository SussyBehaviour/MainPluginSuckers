package Thisiscool.StuffForUs.menus.menu;

import Thisiscool.MainHelper.Action;
import Thisiscool.MainHelper.Action2;
import Thisiscool.MainHelper.Bundle;
import Thisiscool.StuffForUs.menus.Interface;
import Thisiscool.StuffForUs.menus.State;
import Thisiscool.StuffForUs.menus.State.StateKey;
import Thisiscool.StuffForUs.menus.menu.Menu.MenuView;
import arc.func.Cons;
import arc.func.Cons2;
import arc.func.Cons3;
import arc.func.Func;
import arc.struct.Seq;
import mindustry.gen.Call;
import mindustry.gen.Player;
import mindustry.ui.Menus;

public class Menu extends Interface<MenuView> {

    public boolean followUp;

    @Override
    public int register() {
        return Menus.registerMenu((player, choice) -> {
            var view = views.remove(player);
            if (view == null) return;

            var option = view.option(choice);

            if (option == null) view.closed.get(view);
            else option.action().get(view);
        });
    }

    @Override
    public MenuView show(Player player, State state, View previous) {
        return views.get(player, () -> {
            var view = new MenuView(player, state, previous);
            transformers.each(transformer -> transformer.get(view));

            if (followUp) Call.followUpMenu(player.con, id, view.title, view.content, view.options.map(options -> options.map(MenuOption::button).toArray(String.class)).toArray(String[].class));
            else Call.menu(player.con, id, view.title, view.content, view.options.map(options -> options.map(MenuOption::button).toArray(String.class)).toArray(String[].class));

            return view;
        });
    }

    @Override
    public void hide(Player player) {
        if (followUp)
            Call.hideFollowUpMenu(player.con, id);
    }

    @Override
    public Menu transform(Cons<MenuView> transformer) {
        return (Menu) super.transform(transformer);
    }

    @Override
    public <T1> Menu transform(StateKey<T1> key, Cons2<MenuView, T1> transformer) {
        return (Menu) super.transform(view -> transformer.get(view, view.state.get(key)));
    }

    @Override
    public <T1, T2> Menu transform(StateKey<T1> key1, StateKey<T2> key2, Cons3<MenuView, T1, T2> transformer) {
        return (Menu) super.transform(view -> transformer.get(view, view.state.get(key1), view.state.get(key2)));
    }

    public Menu followUp(boolean followUp) {
        this.followUp = followUp;
        return this;
    }

    public class MenuView extends Interface<MenuView>.View {
        public String title = "";
        public String content = "";

        public Seq<Seq<MenuOption>> options = new Seq<>();
        public Action<MenuView> closed = Action.none();

        public MenuView(Player player, State state, View previous) {
            super(player, state, previous);
        }

        public MenuView title(String title, Object... values) {
            this.title = Bundle.format(title, player, values);
            return this;
        }

        public MenuView content(String content, Object... values) {
            this.content = Bundle.format(content, player, values);
            return this;
        }

        public MenuView row() {
            this.options.add(new Seq<MenuOption>());
            return this;
        }

        public MenuView option(String button, Object... values) {
            return option(MenuOption.of(Bundle.format(button, player, values)));
        }

        public MenuView option(String button, Action<MenuView> action, Object... values) {
            return option(MenuOption.of(Bundle.format(button, player, values), action));
        }

        public MenuView option(String button, Action<MenuView> action1, Action<MenuView> action2, Object... values) {
            return option(MenuOption.of(Bundle.format(button, player, values), Action.both(action1, action2)));
        }

        public MenuView option(String button, Action<MenuView> action1, Action<MenuView> action2, Action<MenuView> action3, Object... values) {
            return option(MenuOption.of(Bundle.format(button, player, values), Action.both(action1, action2, action3)));
        }

        public MenuView option(OptionData provider) {
            provider.option(this);
            return this;
        }

        public MenuView option(MenuOption option) {
            if (this.options.isEmpty())
                this.row();

            this.options.peek().add(option);
            return this;
        }

        public <T> MenuView options(Iterable<T> values, Func<T, String> button, Action2<MenuView, T> action) {
            return options(1, values, button, action);
        }

        public <T> MenuView options(int maxPerRow, Iterable<T> values, Func<T, String> button, Action2<MenuView, T> action) {
            if (this.options.isEmpty() || this.options.peek().any())
                this.row();

            for (var value : values) {
                if (this.options.peek().size >= maxPerRow)
                    this.row();

                option(MenuOption.of(button.get(value), view -> action.get(view, value)));
            }

            return this;
        }

        public MenuView options(int maxPerRow, OptionData... datas) {
            if (this.options.isEmpty() || this.options.peek().any())
                this.row();

            for (var data : datas) {
                if (this.options.peek().size >= maxPerRow)
                    this.row();

                option(data);
            }

            return this;
        }

        public MenuView options(int maxPerRow, MenuOption... options) {
            if (this.options.isEmpty() || this.options.peek().any())
                this.row();

            for (var option : options) {
                if (this.options.peek().size >= maxPerRow)
                    this.row();

                option(option);
            }

            return this;
        }

        public MenuOption option(int id) {
            if (id < 0) return null;

            int i = 0;
            for (var row : options) {
                i += row.size;
                if (i > id)
                    return row.get(id - i + row.size);
            }

            return null;
        }

        public MenuView closed(Runnable closed) {
            this.closed = Action.run(closed);
            return this;
        }

        public MenuView closed(Action<MenuView> closed) {
            this.closed = closed;
            return this;
        }

        public interface OptionData {
            // MUST add an option
            void option(MenuView menu);
        }
    }
}