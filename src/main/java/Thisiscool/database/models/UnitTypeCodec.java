package Thisiscool.database.models;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import arc.util.Log; // Ensure you have the correct import for Log
import mindustry.Vars;
import mindustry.type.UnitType;

public class UnitTypeCodec implements Codec<UnitType> {

    @Override
    public void encode(BsonWriter writer, UnitType value, EncoderContext encoderContext) {
        Log.info("Encoding UnitType: " + value.name); // Log the name of the UnitType being encoded
        writer.writeString("name", value.name);
    }

    @Override
    public UnitType decode(BsonReader reader, DecoderContext decoderContext) {
        String name = reader.readString("name");
        Log.info("Decoding UnitType: " + name); // Log the name of the UnitType being decoded
        return Vars.content.units().find(u -> u.name.equals(name));
    }

    @Override
    public Class<UnitType> getEncoderClass() {
        return UnitType.class;
    }
}