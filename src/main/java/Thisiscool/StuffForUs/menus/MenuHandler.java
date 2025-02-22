package Thisiscool.StuffForUs.menus;

import static Thisiscool.PluginVars.*;
import static Thisiscool.database.Ranks.*;
import static Thisiscool.utils.Utils.*;
import static mindustry.net.Administration.Config.*;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import Thisiscool.MainHelper.Action;
import Thisiscool.MainHelper.Bundle;
import Thisiscool.MainHelper.Effects;
import Thisiscool.StuffForUs.menus.State.StateKey;
import Thisiscool.StuffForUs.menus.menu.Menu;
import Thisiscool.StuffForUs.menus.menu.Menu.MenuView;
import Thisiscool.StuffForUs.menus.menu.Menu.MenuView.OptionData;
import Thisiscool.StuffForUs.menus.menu.impl.ConfirmMenu;
import Thisiscool.StuffForUs.menus.menu.impl.ListMenu;
import Thisiscool.StuffForUs.menus.text.TextInput;
import Thisiscool.database.Cache;
import Thisiscool.database.models.PlayerData;
import Thisiscool.utils.Admins;
import arc.func.Cons;
import arc.func.Cons3;
import arc.func.Func;
import arc.func.Prov;
import arc.graphics.Color;
import arc.struct.Seq;
import arc.util.Tmp;
import mindustry.content.Fx;
import mindustry.entities.Effect;
import mindustry.gen.Player;
import mindustry.graphics.Pal;

public class MenuHandler {
    public static void showTrailMenu(Player player) {
        TrailMenu.show(player);
    }
    // region menu

    public static final ListMenu listMenu = new ListMenu(maxPerPage);
    public static final ConfirmMenu confirmMenu = new ConfirmMenu();

    public static final Menu statsMenu = new Menu(),
            promotionMenu = new Menu(),
            requirementsMenu = new Menu(),
            welcomeMenu = new Menu(),
            settingsMenu = new Menu(),
            languagesMenu = new Menu(),
            TrailMenu = new Menu(),
            invalidDurationMenu = new Menu();

    // endregion
    // region input

    public static final TextInput kickDurationInput = new TextInput(),
            banDurationInput = new TextInput(),
            kickReasonInput = new TextInput(),
            banReasonInput = new TextInput();

    // endregion
    // region keys

    public static final StateKey<Long> DURATION = new StateKey<>("duration");
    public static final StateKey<Player> TARGET = new StateKey<>("target");
    public static final StateKey<PlayerData> DATA = new StateKey<>("data");

    // endregion
    // region transforms

    public static void load() {
        // region menu

        statsMenu.transform(TARGET, Player.class, DATA, PlayerData.class, (menu, target, data) -> {
            menu.title("stats.title");
            menu.content("stats.content", target.coloredName(), data.id, data.rank.name(menu.player),
                    data.rank.description(menu.player), data.blocksPlaced, data.blocksBroken, data.gamesPlayed,
                    data.wavesSurvived, data.attackWins, data.TowerdefenseWins, data.FootballWins,
                    data.HungerGamesWins, data.pvpWins,
                    Bundle.formatDuration(menu.player, Duration.ofMinutes(data.playTime)));

            menu.option("stats.requirements.show", Action.open(requirementsMenu)).row();
            menu.option("ui.button.close");
        });

        promotionMenu.transform(DATA, PlayerData.class, (menu, data) -> {
            menu.title("stats.promotion.title");
            menu.content("stats.promotion.content", data.rank.name(menu.player), data.rank.description(menu.player));

            menu.option("stats.requirements.show", Action.open(requirementsMenu)).row();
            menu.option("ui.button.close");
        });

        requirementsMenu.transform(menu -> {
            var builder = new StringBuilder();
            ranks.each(rank -> rank.requirements != null,
                    rank -> builder.append(rank.requirements(menu.player)).append("\n"));

            menu.title("stats.requirements.title");
            menu.content(builder.toString());

            menu.option("ui.button.back", Action.back());
            menu.option("ui.button.close");
        });

        welcomeMenu.transform(menu -> {
            var builder = new StringBuilder();
            availableCommands(menu.player).each(command -> command.welcomeMessage,
                    command -> builder.append("[cyan]/").append(command.name).append("[gray] - [lightgray]")
                            .append(command.description(menu.player)).append("\n"));

            menu.title("welcome.title");
            menu.content("welcome.content", serverName.string(), builder.toString());

            menu.option("ui.button.close").row();
            menu.option("welcome.discord", Action.uri(discordServerUrl)).row();
            menu.option("welcome.disable", view -> {
                Cache.get(view.player).welcomeMessage = false;
                Bundle.send(view.player, "welcome.disabled");
            });
        });

        settingsMenu.transform(menu -> {
            var data = Cache.get(menu.player);

            menu.title("settings.title");
            menu.content("settings.content");

            menu.options(1, Setting.values()).row();
            menu.option("setting.translator", Action.open(languagesMenu), data.language.name(menu)).row();
            menu.option("setting.effects", Action.open(TrailMenu), data.trail.name(menu)).row();

            menu.option("ui.button.close");
        }).followUp(true);

        languagesMenu.transform(menu -> {
            var data = Cache.get(menu.player);

            menu.title("language.title");
            menu.content("language.content", data.language.name(menu));

            menu.options(3, Language.values()).row();

            menu.option("ui.button.back", Action.back());
            menu.option("ui.button.close");
        }).followUp(true);

        TrailMenu.transform(menu -> {
            var data = Cache.get(menu.player);

            menu.title("trail.title");
            menu.content("trail.content", data.trail.name(menu));

            menu.options(2, TrailsPack.values()).row();

            menu.option("ui.button.back", Action.back());
            menu.option("ui.button.close");
        }).followUp(true);

        invalidDurationMenu.transform(menu -> {
            menu.title("duration.invalid.title");
            menu.content("duration.invalid.content");

            menu.option("ui.button.back", Action.back());
        });

        // endregion
        // region input

        kickDurationInput.transform(TARGET, Player.class, (input, target) -> {
            input.title("kick.duration.title");
            input.content("kick.duration.content", target.coloredName());

            input.defaultText("kick.duration.default");
            input.textLength(32);

            input.result(text -> {
                var duration = parseDuration(text);
                kickReasonInput.open(input, DURATION, duration.toMillis());
            });
        });

        banDurationInput.transform(TARGET, Player.class, (input, target) -> {
            input.title("ban.duration.title");
            input.content("ban.duration.content", target.coloredName());

            input.defaultText("ban.duration.default");
            input.textLength(32);

            input.result(text -> {
                var duration = parseDuration(text);
                banReasonInput.open(input, DURATION, duration.toMillis());
            });
        });

        kickReasonInput.transform(TARGET, Player.class, (input, target) -> {
            input.title("kick.reason.title");
            input.content("kick.reason.content", target.coloredName(),
                    Bundle.formatDuration(input.player, input.state.get(DURATION, Long.class)));

            input.defaultText("kick.reason.default");
            input.textLength(64);

            input.closed((Runnable) Action.back());
            input.result(text -> Admins.kick(target, input.player, input.state.get(DURATION, Long.class), text));
        });

        banReasonInput.transform(TARGET, Player.class, (input, target) -> {
            input.title("ban.reason.title");
            input.content("ban.reason.content", target.coloredName(),
                    Bundle.formatDuration(input.player, input.state.get(DURATION, Long.class)));

            input.defaultText("ban.reason.default");
            input.textLength(64);

            input.closed((Runnable) Action.back());
            input.result(text -> Admins.ban(target, input.player, input.state.get(DURATION, Long.class), text));
        });

        // endregion
    }

    // endregion
    // region show

    public static <T> void showListMenu(Player player, int page, String title, Prov<Seq<T>> content,
            Cons3<StringBuilder, Integer, T> formatter) {
        listMenu.show(player, page, title, content, formatter);
    }

    public static void showConfirmMenu(Player player, String content, Runnable confirmed, Object... values) {
        confirmMenu.show(player, "ui.title.confirm", content, confirmed, values);
    }

    public static void showStatsMenu(Player player, Player target, PlayerData data) {
        statsMenu.show(player, TARGET, target, DATA, data);
    }

    public static void showPromotionMenu(Player player, PlayerData data) {
        promotionMenu.show(player, DATA, data);
    }

    public static void showWelcomeMenu(Player player) {
        welcomeMenu.show(player);
    }

    public static void showSettingsMenu(Player player) {
        settingsMenu.show(player);
    }

    public static void showKickInput(Player player, Player target) {
        kickDurationInput.show(player, TARGET, target);
    }

    public static void showBanInput(Player player, Player target) {
        banDurationInput.show(player, TARGET, target);
    }

    // endregion
    // region enums

    public enum Setting implements OptionData {
        history(data -> data.history = !data.history, data -> data.history),
        welcomeMessage(data -> data.welcomeMessage = !data.welcomeMessage, data -> data.welcomeMessage),
        discordLink(data -> data.discordLink = !data.discordLink, data -> data.discordLink);

        public final Cons<PlayerData> setter;
        public final Func<PlayerData, Boolean> getter;

        Setting(Cons<PlayerData> setter, Func<PlayerData, Boolean> getter) {
            this.setter = setter;
            this.getter = getter;
        }

        @Override
        public void option(MenuView menu) {
            menu.option("setting." + name(), view -> {
                var data = Cache.get(view.player);
                setter.get(data);

                view.getInterface().show(view);
            }, Bundle.get(getter.get(Cache.get(menu.player)) ? "setting.on" : "setting.off", menu.player));
        }
    }

    public enum Language implements OptionData {
        english("en", "English"),
        french("fr", "Français"),
        german("de", "Deutsch"),

        italian("it", "Italiano"),
        spanish("es", "Español"),
        portuguese("pt", "Portuga"),

        russian("ru", "Русский"),
        polish("pl", "Polski"),
        turkish("tr", "Türkçe"),

        chinese("zh", "简体中文"),
        korean("ko", "한국어"),
        japanese("ja", "日本語"),

        off("off", "language.disabled", "language.disable");

        public final String code, name, button;

        Language(String code, String name) {
            this(code, name, name);
        }

        Language(String code, String name, String button) {
            this.code = code;
            this.name = name;
            this.button = button;
        }

        public String name(MenuView menu) {
            return Bundle.get(name, menu.player);
        }

        @Override
        public void option(MenuView menu) {
            menu.option(button, view -> {
                var data = Cache.get(view.player);
                data.language = this;

                view.getInterface().show(view);
            });
        }
    }

    public enum TrailsPack implements OptionData {
        trail1("Trail1",
                player -> Effects.stack(player, getRandomEffect(), getRandomEffect()),
                player -> Effects.stack(player, getRandomEffect(), getRandomEffect()),
                player -> Effects.at(Fx.artilleryTrailSmoke, player)),

        trail2("Trail2",
                player -> Effects.stack(player, getRandomEffect(), getRandomEffect()),
                player -> Effects.stack(player, getRandomEffect(), getRandomEffect()),
                player -> Effects.at(Fx.incendTrail, player, 3f)),

        trail3("Trail3",
                player -> Effects.stack(player, getRandomEffect(), getRandomEffect(), getRandomEffect()),
                player -> Effects.stack(player, getRandomEffect(), getRandomEffect(), getRandomEffect()),
                player -> Effects.at(Fx.lightningCharge, player)),

        trail4("Trail4",
                player -> Effects.stack(player, getRandomEffect(), getRandomEffect(), getRandomEffect()),
                player -> Effects.stack(player, getRandomEffect(), getRandomEffect(), getRandomEffect()),
                player -> Effects.at(Fx.mineWallSmall, player, Color.yellow)),

        trail5("Trail5",
                player -> Effects.stack(player, Pal.neoplasm1, getRandomEffect(), getRandomEffect()),
                player -> Effects.stack(player, Pal.neoplasm1, getRandomEffect(), getRandomEffect()),
                player -> Effects.at(Fx.neoplasmHeal, player)),

        trail6("Trail6",
                player -> Effects.stack(player, getRandomEffect(), getRandomEffect(), getRandomEffect()),
                player -> Effects.stack(player, getRandomEffect(), getRandomEffect(), getRandomEffect()),
                player -> Effects.at(Fx.smeltsmoke, player, Color.red)),

        trail7("Trail7",
                player -> Effects.rotatedPoly(getRandomEffect(), player, 6, 12f, -180f, 90f),
                player -> Effects.rotatedPoly(getRandomEffect(), player, 6, 4f, -90f, 30f),
                player -> Effects.at(Fx.bubble, player, 30f)),

        trail8("Trail8",
                player -> Effects.repeat(6, 60f,
                        time -> Effects.at(getRandomEffect(), Tmp.v1.set(60f, 0f).rotate(time * 60f).add(player))),
                player -> Effects.repeat(6, 60f,
                        time -> Effects.at(getRandomEffect(), Tmp.v1.set(60f, 0f).rotate(time * 300f).add(player))),
                player -> Effects.at(Fx.airBubble, player)),

        trail9("Trail9",
                player -> Effects.stack(player, 120f, getRandomEffect(), getRandomEffect(), getRandomEffect(),
                        Fx.mineImpact),
                player -> Effects.stack(player, 120f, getRandomEffect(), getRandomEffect(), getRandomEffect(),
                        Fx.mineImpact),
                player -> Effects.at(Fx.mine, player, Color.cyan)),

        trail10("Trail10",
                player -> Effects.at(getRandomEffect(), player),
                player -> Effects.at(getRandomEffect(), player),
                player -> Effects.at(getRandomEffect(), player)),

        trail11("Trail11",
                player -> Effects.at(getRandomEffect(), player),
                player -> Effects.at(getRandomEffect(), player),
                player -> Effects.at(getRandomEffect(), player, player.unit().rotation - 180f, Pal.reactorPurple)),

        trail12("Trail12",
                player -> Effects.at(getRandomEffect(), player),
                player -> Effects.at(getRandomEffect(), player),
                player -> Effects.at(getRandomEffect(), player, player.unit().rotation - 180f, Pal.lighterOrange)),

        trail13("Trail13",
                player -> Effects.at(getRandomEffect(), player, 60f, Pal.sapBullet),
                player -> Effects.at(getRandomEffect(), player, 60f, Pal.sapBullet),
                player -> Effects.at(Fx.regenSuppressSeek, player, player.unit())),

        trail14("Trail14",
                player -> Effects.rotatedPoly(getRandomEffect(), player, 6, 6f, -180f, 50f),
                player -> Effects.rotatedPoly(getRandomEffect(), player, 6, 6f, 0f, 100f),
                player -> Effects.at(Fx.vapor, player)),

        none("effects.disabled", "effects.disable");

        public final String name, button;
        public final Cons<Player> join, leave, move;

        TrailsPack(String name, Cons<Player> join, Cons<Player> leave, Cons<Player> move) {
            this(name, name, join, leave, move);
        }

        TrailsPack(String name, String button) {
            this(name, button, player -> {
            }, player -> {
            }, player -> {
            });
        }

        TrailsPack(String name, String button, Cons<Player> join, Cons<Player> leave, Cons<Player> move) {
            this.name = name;
            this.button = button;

            this.join = join;
            this.leave = leave;
            this.move = move;
        }

        public String name(MenuView menu) {
            return Bundle.get(name, menu.player);
        }

        @Override
        public void option(MenuView menu) {
            menu.option(button, view -> {
                var data = Cache.get(view.player);
                data.trail = this;

                view.getInterface().show(view);
            });
        }
    }

    // endregion
    public static Effect getRandomEffect() {
        List<Effect> effects = new ArrayList<>();
        Random random = new Random();

        for (Field field : Fx.class.getFields()) {
            try {
                Object obj = field.get(null);
                if (obj instanceof Effect) {
                    effects.add((Effect) obj);
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        if (effects.isEmpty()) {
            return null; // or handle the case where no Effects are found
        }

        return effects.get(random.nextInt(effects.size()));
    }
}