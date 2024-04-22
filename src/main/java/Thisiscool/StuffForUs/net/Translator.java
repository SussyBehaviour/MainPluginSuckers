package Thisiscool.StuffForUs.net;

import static Thisiscool.PluginVars.*;

import Thisiscool.MainHelper.Bundle;
import Thisiscool.StuffForUs.menus.MenuHandler.Language;
import Thisiscool.database.Cache;
import arc.func.Boolf;
import arc.func.Cons;
import arc.func.Cons2;
import arc.util.Http;
import arc.util.Strings;
import arc.util.serialization.JsonReader;
import mindustry.gen.Groups;
import mindustry.gen.Player;


public class Translator {

    public static void translate(String text, String from, String to, Cons<String> result) {
        Http.post(translationApiUrl, "tl=" + to + "&sl=" + from + "&q=" + Strings.encode(text))
                .error(throwable -> result.get(""))
                .submit(response -> result
                        .get(new JsonReader().parse(response.getResultAsString()).child().child().asString()));
    }

    public static void translate(Player from, String text) {
        translate(player -> true, from, text, (player, result) -> player.sendUnformatted(from, result));
    }

    public static void translate(Boolf<Player> filter, Player from, String text, String key, Object... values) {
        translate(filter, from, text, (player, result) -> Bundle.sendFrom(player, from, result, key, values));
    }

    public static void translate(Boolf<Player> filter, Player from, String text, Cons2<Player, String> result) {
        Groups.player.each(filter, player -> {
            var data = Cache.get(player);

            if (player == from || data.language == Language.off) {
                result.get(player, text);
                return;
            }

            translate(text, "auto", data.language.code, translated -> result.get(player,
                    translated.isEmpty() ? text : text + " [white]([lightgray]" + translated + "[])"));
        });
    }
}