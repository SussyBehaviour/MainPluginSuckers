package Thisiscool;

import static java.time.temporal.ChronoUnit.*;

import java.time.temporal.ChronoUnit;
import java.util.regex.Pattern;

import Thisiscool.StuffForUs.votes.Report;
import Thisiscool.StuffForUs.votes.VoteKick;
import Thisiscool.StuffForUs.votes.VoteSession;
import arc.struct.Seq;
import arc.util.CommandHandler;
import mindustry.core.Version;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
public class PluginVars {
    public static final float voteRatio = 0.55f;
    public static final int voteDuration = 50;
    public static final int kickDuration = 30 * 60 * 1000;
    public static final int maxWavesAmount = 5;
    public static final int maxGiveAmount = 100000;
    public static final int maxSpawnAmount = 25;
    public static final int maxEffectDuration = 60 * 60;
    public static final int maxFillArea = 512;
    public static final int maxPerPage = 6;
    public static final int maxHistorySize = 8;
    public static final int maxIdenticalIPs = 3;
    public static final int mindustryVersion = Version.build;
    public static final String configFile = "config.json";
    public static final String discordConfigFile = "discord-config.json";
    public static final String discordServerUrl = "https://discord.gg/vaGBFkvCwg";
    public static final String translationApiUrl = "https://clients5.google.com/translate_a/t?client=dict-chrome-ex&dt=t";
    public static final Pattern durationPattern = Pattern.compile("(\\d+)\\s*?([a-zA-Zа-яА-Я]+)");
    @SuppressWarnings("unchecked")
    public static final Seq<Tuple2<Pattern, ChronoUnit>> durationPatterns = Seq.with();
    static {
        durationPatterns.add(Tuples.of(Pattern.compile("(mon|month|months)"), MONTHS));
        durationPatterns.add(Tuples.of(Pattern.compile("(w|week|weeks)"), WEEKS));
        durationPatterns.add(Tuples.of(Pattern.compile("(d|day|days)"), DAYS));
        durationPatterns.add(Tuples.of(Pattern.compile("(h|hour|hours)"), HOURS));
        durationPatterns.add(Tuples.of(Pattern.compile("(m|min|mins|minute|minutes)"), MINUTES));
        durationPatterns.add(Tuples.of(Pattern.compile("(s|sec|secs|second|seconds)"), SECONDS));
    }
    public static VoteSession vote;
    public static VoteKick voteKick;
    public static Report report;
    public static CommandHandler serverHandler, discordHandler;
}