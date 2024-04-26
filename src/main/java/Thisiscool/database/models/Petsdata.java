package Thisiscool.database.models;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import Thisiscool.database.Database;
import arc.graphics.Color;
import arc.struct.Seq;
import arc.util.Log;
import mindustry.Vars;
import mindustry.type.UnitType;

public class Petsdata {

    public static Pet[] getPets(String owner) {
        String sql = "SELECT * FROM pets WHERE owner = ?";
        try {
            PreparedStatement ps = Database.conn.prepareStatement(sql);
            ps.setString(1, owner);

            ResultSet rs = ps.executeQuery();
            Seq<Pet> pets = new Seq<>();
            while (rs.next()) {
                Pet pet = Pet.fromSQL(rs);
                pets.add(pet);
            }
            return pets.toArray(Pet.class);
        } catch (SQLException e) {
            Log.err("pet error: " + e);
            return null;
        }
    }

    /**
     * Inserts a new pet
     */
    public static void addPet(Pet pet) {
        String sql = "INSERT INTO pets VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement pstmt = Database.conn.prepareStatement(sql);
            pstmt.setString(1, pet.owner);
            pstmt.setString(2, pet.name);
            pstmt.setString(3, pet.species.name);
            pstmt.setString(4, pet.color.toString());
            pstmt.setLong(5, pet.eatenCoal);
            pstmt.setLong(6, pet.eatenCopper);
            pstmt.setLong(7, pet.eatenLead);
            pstmt.setLong(8, pet.eatenTitanium);
            pstmt.setLong(9, pet.eatenThorium);
            pstmt.setLong(10, pet.eatenBeryllium);

            pstmt.executeUpdate();
        } catch (SQLException e) {
            Log.err(e);
        }
    }

    /**
     * Updates a given pet, using name and owner as identifiers
     */
    public static void updatePet(Pet pet) {
        String sql = "UPDATE pets SET color = ?, species = ?, eatenCoal = ?, eatenCopper = ?, eatenLead = ?, eatenTitanium = ?, eatenThorium = ?, eatenBeryllium = ? WHERE owner = ? AND name = ?";
        try {
            PreparedStatement pstmt = Database.conn.prepareStatement(sql);
            pstmt.setString(1, pet.color.toString());
            pstmt.setString(2, pet.species.name);
            pstmt.setLong(3, pet.eatenCoal);
            pstmt.setLong(4, pet.eatenCopper);
            pstmt.setLong(5, pet.eatenLead);
            pstmt.setLong(6, pet.eatenTitanium);
            pstmt.setLong(7, pet.eatenThorium);
            pstmt.setLong(8, pet.eatenBeryllium);
            pstmt.setString(9, pet.owner);
            pstmt.setString(10, pet.name);

            pstmt.executeUpdate();
        } catch (SQLException e) {
            Log.err(e);
        }
    }

    /**
     * Removes a pet
     */
    public static void removePet(String owner, String name) {
        String sql = "DELETE FROM pets WHERE owner = ? AND name = ?";
        try {
            PreparedStatement pstmt = Database.conn.prepareStatement(sql);
            pstmt.setString(1, owner);
            pstmt.setString(2, name);

            pstmt.executeUpdate();
        } catch (SQLException e) {
            Log.err(e);
        }
    }

    public static class Pet {
        /**
         * UUID of user who owns the pet
         */
        public String owner;
        public String name;
        public UnitType species;
        public Color color;

        public long eatenCoal;
        public long eatenCopper;
        public long eatenLead;
        public long eatenTitanium;
        public long eatenThorium;
        public long eatenBeryllium;

        public Pet(String owner, String name) {
            this.owner = owner;
            this.name = name;
        }

        public static Pet fromSQL(ResultSet rs) throws SQLException {
            String name = rs.getString("name");
            String owner = rs.getString("owner");
            String colorStr = rs.getString("color");
            String speciesStr = rs.getString("species");
            UnitType species = Vars.content.units().find(u -> u.name.equals(speciesStr));
            if (species == null) {
                return null;
            }
            Color color = Color.valueOf(colorStr);

            Pet pet = new Pet(owner, name);
            pet.color = color;
            pet.species = species;

            pet.eatenCopper = rs.getLong("eatenCopper");
            pet.eatenLead = rs.getLong("eatenLead");
            pet.eatenTitanium = rs.getLong("eatenTitanium");
            pet.eatenThorium = rs.getLong("eatenThorium");
            pet.eatenCoal = rs.getLong("eatenCoal");
            pet.eatenBeryllium = rs.getLong("eatenBeryllium");

            return pet;
        }
    }
    
}
