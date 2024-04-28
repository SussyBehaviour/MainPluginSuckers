package Thisiscool.database.models;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import mindustry.Vars;
import mindustry.type.UnitType;

public class UnitTypeCodec implements Codec<UnitType> {

    @Override
    public void encode(BsonWriter writer, UnitType value, EncoderContext encoderContext) {
        writer.writeString(value.localizedName);
    }

    @Override
    public UnitType decode(BsonReader reader, DecoderContext decoderContext) {
        String unitTypeName = reader.readString();
        return Vars.content.units().find(u -> u.name.equals(unitTypeName));
    }

    @Override
    public Class<UnitType> getEncoderClass() {
        return UnitType.class;
    }
}