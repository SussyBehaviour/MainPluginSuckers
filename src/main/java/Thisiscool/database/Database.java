package Thisiscool.database;

import java.util.List;
import java.util.Optional;

import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClients;
import com.mongodb.client.model.ReturnDocument;

import Thisiscool.config.Config;
import Thisiscool.database.models.Ban;
import Thisiscool.database.models.Counter;
import Thisiscool.database.models.Petsdata;
import Thisiscool.database.models.PlayerData;
import Thisiscool.database.models.UnitTypeCodec;
import arc.util.Log;
import dev.morphia.Datastore;
import dev.morphia.ModifyOptions;
import dev.morphia.Morphia;
import dev.morphia.mapping.Mapper;
import dev.morphia.query.filters.Filters;
import dev.morphia.query.updates.UpdateOperators;
import mindustry.gen.Player;

public class Database {

    public static Datastore datastore;
    public static Mapper mapper;

    public static void connect() {
        try {
            datastore = Morphia.createDatastore(MongoClients.create(Config.geturl()), "Thisiscool");
            CodecRegistry codecRegistry = CodecRegistries.fromRegistries(
                    MongoClientSettings.getDefaultCodecRegistry(),
                    CodecRegistries.fromCodecs(new UnitTypeCodec()));

            MongoClientSettings.builder()
                    .codecRegistry(codecRegistry)
                    .build();
            mapper = datastore.getMapper();
            mapper.getEntityModel(Ban.class);
            mapper.getEntityModel(Counter.class);
            mapper.getEntityModel(PlayerData.class);
            mapper.getEntityModel(Petsdata.Pet.class);
            Log.info("Database connected.");
        } catch (Exception e) {
            Log.err("Failed to connect to the database", e);
        }
    }

    // region player data

    public static PlayerData getPlayerData(Player player) {
        return getPlayerData(player.uuid());
    }

    public static PlayerData getPlayerData(String uuid) {
        return Optional.ofNullable(Cache.get(uuid)).orElseGet(() -> datastore.find(PlayerData.class)
                .filter(Filters.eq("uuid", uuid))
                .first());
    }

    public static PlayerData getPlayerData(int id) {
        return Optional.ofNullable(Cache.get(id)).orElseGet(() -> datastore.find(PlayerData.class)
                .filter(Filters.eq("_id", id))
                .first());
    }

    public static PlayerData getPlayerDataByDiscordId(Long discordId) {
        return datastore.find(PlayerData.class)
                .filter(Filters.eq("DiscordId", discordId))
                .first();
    }

    public static PlayerData getPlayerDataOrCreate(String uuid) {
        return Optional.ofNullable(datastore.find(PlayerData.class).filter(Filters.eq("uuid", uuid)).first())
                .orElseGet(() -> {
                    var data = new PlayerData(uuid);
                    data.generateID();

                    return savePlayerData(data);
                });
    }
    public static int getPlayerDataByUuid(String uuid) {
        PlayerData playerData = getPlayerData(uuid);
        if (playerData != null) {
            return playerData.id;
        } else {
            Log.err("PlayerData not found for uuid: " + uuid);
            return 0;
        }
    }
    public static PlayerData savePlayerData(PlayerData data) {
        return datastore.save(data);
    }

    // endregion
    // region ban

    public static Ban addBan(Ban ban) {
        return datastore.save(ban);
    }

    public static Ban removeBan(String uuid, String ip) {
        return datastore.find(Ban.class)
                .filter(Filters.or(Filters.eq("uuid", uuid), Filters.eq("ip", ip)))
                .findAndDelete();
    }

    public static Ban getBan(String uuid, String ip) {
        return datastore.find(Ban.class)
                .filter(Filters.or(Filters.eq("uuid", uuid), Filters.eq("ip", ip)))
                .first();
    }

    public static List<Ban> getBans() {
        return datastore.find(Ban.class).stream().toList();
    }

    public static Ban getBanByUUID(String uuid) {
        return datastore.find(Ban.class).filter(Filters.eq("uuid", uuid)).first();
    }
    // endregion
    // region ID

    public static int generateNextID(String key) {
        return Optional.ofNullable(datastore.find(Counter.class)
                .filter(Filters.eq("_id", key))
                .modify(new ModifyOptions().returnDocument(ReturnDocument.AFTER), UpdateOperators.inc("value")))
                .orElseGet(() -> datastore.save(new Counter(key))).value;
    }

    // endregion
}