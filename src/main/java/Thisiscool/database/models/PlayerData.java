package Thisiscool.database.models;

import Thisiscool.StuffForUs.menus.MenuHandler.EffectsPack;
import Thisiscool.StuffForUs.menus.MenuHandler.Language;
import Thisiscool.database.Database;
import Thisiscool.database.Ranks.Rank;
import arc.util.Strings;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Field;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Index;
import dev.morphia.annotations.Indexes;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity(value = "players", useDiscriminator = false)
@Indexes({
        @Index(fields = @Field("uuid")),
        @Index(fields = @Field("_id"))
})
@Data
@NoArgsConstructor
public class PlayerData {
   
    public String uuid;
    public String name = "<unknown>";

    @Id
    public int id;

    public boolean alerts = true;
    public boolean history = false;
    public boolean welcomeMessage = true;
    public boolean discordLink = true;

    public Language language = Language.off;
    public EffectsPack effects = EffectsPack.none;

    public int playTime = 0;
    public int blocksPlaced = 0;
    public int blocksBroken = 0;
    public int gamesPlayed = 0;
    public int wavesSurvived = 0;

    public int attackWins = 0;
    public int TowerdefenseWins = 0;
    public int FootballWins = 0;
    public int HungerGamesWins = 0;
    public int pvpWins = 0;
    public long DiscordId = 0;
    public Rank rank = Rank.Civilian;

    public PlayerData(String uuid) {
        this.uuid = uuid;
    }

    public void generateID() {
        this.id = Database.generateNextID("players");
    }

    public String plainName() {
        return Strings.stripColors(name);
    }
}