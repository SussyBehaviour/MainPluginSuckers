package Thisiscool.database.models;

import java.util.Date;

import Thisiscool.database.Database;
import arc.util.Time;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Field;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Index;
import dev.morphia.annotations.IndexOptions;
import dev.morphia.annotations.Indexes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity(value = "bans", useDiscriminator = false)
@Indexes({
        @Index(fields = @Field("uuid")),
        @Index(fields = @Field("ip")),
        @Index(fields = @Field("_id")),
        @Index(fields = @Field("unbanDate"), options = @IndexOptions(expireAfterSeconds = 0))
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ban {
    public String uuid;
    public String ip;

    public String playerName, adminName;
    public int playerID;

    @Id
    public int id;

    public String reason;
    public Date unbanDate;

    public void generateID() {
        this.id = Database.generateNextID("bans");
    }

    public void generatePlayerID() {
        this.playerID = Database.getPlayerData(uuid).id;
    }

    public boolean expired() {
        return unbanDate.getTime() < Time.millis();
    }

    public long remaining() {
        return unbanDate.getTime() - Time.millis();
    }
}