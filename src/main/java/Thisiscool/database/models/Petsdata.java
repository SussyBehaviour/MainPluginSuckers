package Thisiscool.database.models;

import java.util.List;

import org.bson.Document;

import Thisiscool.database.Database;
import arc.util.Log;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.query.Query;
import dev.morphia.query.updates.UpdateOperators;

public class Petsdata {

    @SuppressWarnings("removal")
    public static Pet[] getPets(String owner) {
        try {
            Log.info("Attempting to find pets for owner: " + owner);
            Query<Pet> query = Database.datastore.find(Pet.class).filter("owner", owner);
            List<Pet> petsList = query.find().toList();
            Log.info("Found " + petsList.size() + " pets for owner: " + owner);
            return petsList.toArray(new Pet[0]);
        } catch (Exception e) {
            Log.err("pet error: " + e);
            return null;
        }
    }

    public static void addPet(Pet pet) {
        try {
            Database.datastore.save(pet);
        } catch (Exception e) {
            Log.err(e);
        }
    }

    @SuppressWarnings("removal")
    public static void updatePet(Pet pet) {
        try {
            Database.datastore.find(Pet.class)
                    .filter("owner", pet.owner)
                    .filter("name", pet.name)
                    .update(UpdateOperators.set("color", pet.color.toString()),
                            UpdateOperators.set("species", pet.speciesName),
                            UpdateOperators.set("eatenCoal", pet.eatenCoal),
                            UpdateOperators.set("eatenCopper", pet.eatenCopper),
                            UpdateOperators.set("eatenLead", pet.eatenLead),
                            UpdateOperators.set("eatenTitanium", pet.eatenTitanium),
                            UpdateOperators.set("eatenThorium", pet.eatenThorium),
                            UpdateOperators.set("eatenBeryllium", pet.eatenBeryllium));
        } catch (Exception e) {
            Log.err(e);
        }
    }

    @SuppressWarnings("removal")
    public static void removePet(String owner, String name) {
        try {
            Database.datastore.find(Pet.class)
                    .filter("owner", owner)
                    .filter("name", name)
                    .delete();
        } catch (Exception e) {
            Log.err(e);
        }
    }

    @Entity
    public static class Pet {
        @Id
        private int id;
        public String owner;
        public String name;
        public String speciesName;
        public String color;

        public long eatenCoal;
        public long eatenCopper;
        public long eatenLead;
        public long eatenTitanium;
        public long eatenThorium;
        public long eatenBeryllium;

        public Pet(String owner, String name) {
            this.owner = owner;
            this.name = name;
            this.id = Database.getPlayerDataByUuid(owner);
        }

        public void setSpeciesByName(String speciesName) {
            this.speciesName = speciesName;

        }

        public Document toDocument() {
            Document document = new Document();
            document.append("owner", owner);
            document.append("name", name);
            document.append("species", speciesName);
            document.append("color", color.toString());
            document.append("eatenCoal", eatenCoal);
            document.append("eatenCopper", eatenCopper);
            document.append("eatenLead", eatenLead);
            document.append("eatenTitanium", eatenTitanium);
            document.append("eatenThorium", eatenThorium);
            document.append("eatenBeryllium", eatenBeryllium);
            return document;
        }

        public static Pet fromDocument(Document document) {
            Pet pet = new Pet(document.getString("owner"), document.getString("name"));
            pet.speciesName = document.getString("species");
            pet.color = document.getString("color");
            pet.eatenCoal = document.getLong("eatenCoal");
            pet.eatenCopper = document.getLong("eatenCopper");
            pet.eatenLead = document.getLong("eatenLead");
            pet.eatenTitanium = document.getLong("eatenTitanium");
            pet.eatenThorium = document.getLong("eatenThorium");
            pet.eatenBeryllium = document.getLong("eatenBeryllium");
            return pet;
        }
    }
}
