package Thisiscool.StuffForUs;

import Thisiscool.database.models.Petsdata;
import Thisiscool.database.models.Petsdata.Pet;
import arc.Events;
import arc.graphics.Color;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Structs;
import arc.util.Timer;
import mindustry.Vars;
import mindustry.ai.types.MinerAI;
import mindustry.content.Blocks;
import mindustry.content.Items;
import mindustry.content.UnitTypes;
import mindustry.entities.units.UnitController;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.gen.Unit;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.type.UnitType;
import mindustry.world.Tile;
import mindustry.world.blocks.defense.BuildTurret;
import mindustry.world.blocks.defense.turrets.BaseTurret;
import mindustry.world.blocks.defense.turrets.PointDefenseTurret;
import mindustry.world.blocks.defense.turrets.Turret;
import mindustry.world.blocks.storage.CoreBlock;

public class Pets {
    /**
     * Pets that are currently spawned. Continuously read by controllers, which
     * despawn their unit if it is not in this list.
     */
    public static ObjectMap<String, Seq<String>> spawnedPets = new ObjectMap<>();

    public static int maxPets(String rank) {
        Log.info("[Pets] Calculating max pets for rank " + rank);
        int max = switch (rank) {
            case "Civilian", "DClass" -> {
                Log.info("[Pets] Rank is Civilian or DClass, returning 0");
                yield 0;
            }
            case "LEVEL0", "LEVEL1" -> {
                Log.info("[Pets] Rank is LEVEL0 or LEVEL1, returning 1");
                yield 1;
            }
            case "LEVEL2" -> {
                Log.info("[Pets] Rank is LEVEL2, returning 2");
                yield 2;
            }
            case "LEVEL3" -> {
                Log.info("[Pets] Rank is LEVEL3, returning 5");
                yield 5;
            }
            default -> {
                Log.info("[Pets] Rank is something else, returning 3");
                yield 3;
            }
        };
        Log.info("[Pets] Max pets for rank " + rank + " is " + max);
        return max;
    }

    public static int maxTier(String rank) {
        Log.info("[Pets] Calculating max tier for rank " + rank);
        int max = switch (rank) {
            case "Civilian", "DClass" -> {
                Log.info("[Pets] Rank is Civilian or DClass, returning 0");
                yield 0;
            }
            case "LEVEL0", "LEVEL1" -> {
                Log.info("[Pets] Rank is LEVEL0 or LEVEL1, returning 1");
                yield 1;
            }
            case "LEVEL2" -> {
                Log.info("[Pets] Rank is LEVEL2, returning 2");
                yield 2;
            }
            case "LEVEL3", "LEVEL4" -> {
                Log.info("[Pets] Rank is LEVEL3 or LEVEL4, returning 3");
                yield 3;
            }
            case "LEVEL5" -> {
                Log.info("[Pets] Rank is LEVEL5, returning 4");
                yield 4;
            }
            default -> {
                Log.info("[Pets] Rank is something else, returning 4");
                yield 4;
            }
        };
        Log.info("[Pets] Max tier for rank " + rank + " is " + max);
        return max;
    }

    public static int tierOf(UnitType type) {
        if (type == UnitTypes.quad || type == UnitTypes.scepter || type == UnitTypes.vela || type == UnitTypes.alpha
                || type == UnitTypes.beta || type == UnitTypes.gamma) {
            return 4;
        } else if (type == UnitTypes.fortress || type == UnitTypes.quasar || type == UnitTypes.spiroct
                || type == UnitTypes.zenith || type == UnitTypes.mega ||
                type == UnitTypes.precept || type == UnitTypes.anthicus || type == UnitTypes.obviate) {
            return 3;
        } else if (type == UnitTypes.mace || type == UnitTypes.pulsar || type == UnitTypes.atrax
                || type == UnitTypes.horizon || type == UnitTypes.poly ||
                type == UnitTypes.locus || type == UnitTypes.cleroi || type == UnitTypes.avert) {
            return 2;
        } else if (type == UnitTypes.dagger || type == UnitTypes.nova || type == UnitTypes.crawler
                || type == UnitTypes.flare || type == UnitTypes.mono ||
                type == UnitTypes.elude || type == UnitTypes.merui || type == UnitTypes.stell) {
            return 1;
        }
        return -1;
    }

    public static Item[] possibleFoods(UnitType type) {
        if (type == UnitTypes.crawler) {
            return new Item[] { Items.coal };
        } else if (type == UnitTypes.quasar || type == UnitTypes.pulsar) {
            return new Item[] { Items.beryllium, Items.titanium, Items.thorium };
        } else if (type.flying && type != UnitTypes.quad) {
            return new Item[] { Items.copper, Items.lead, Items.titanium, Items.thorium };
        } else {
            return new Item[] { Items.copper, Items.lead, Items.titanium };
        }
    }

    protected static int rank(Pet pet) {
        UnitType unitType = Vars.content.units().find(u -> u.name.equals(pet.speciesName));
        var items = possibleFoods(unitType);
        long min = Long.MAX_VALUE;
        for (var item : items) {
            long value = switch (item.name) {
                case "coal" -> pet.eatenCoal;
                case "copper" -> pet.eatenCopper;
                case "lead" -> pet.eatenLead;
                case "titanium" -> pet.eatenTitanium;
                case "thorium" -> pet.eatenThorium;
                case "beryllium" -> pet.eatenBeryllium;
                default -> 0;
            };
            if (value < min) {
                min = value;
            }
        }

        if (min > 1000000) {
            return 5;
        } else if (min > 100000) {
            return 4;
        } else if (min > 10000) {
            return 3;
        } else if (min > 1000) {
            return 2;
        } else if (min > 500) {
            return 1;
        } else {
            return 0;
        }
    }

    /**
     * Creates a team if one does not already exist
     */
    private static Team getTeam(Color color) {
        double minErr = Double.POSITIVE_INFINITY;
        Team bestTeam = null;

        // hsv2 = hsv of the desired color
        float[] hsv2 = color.toHsv(new float[3]);
        if (hsv2[2] <= 0.3 && hsv2[1] <= 0.15) {
            return Team.derelict;
        }

        for (Team team : Team.all) {
            if (team.id <= 5 && team != Team.derelict)
                continue; // don't want player to control pets

            float[] hsv1 = team.color.toHsv(new float[3]);
            double err = 1.0 * (hsv1[0] - hsv2[0]) * (hsv1[0] - hsv2[0]) +
                    200.0 * 200.0 * (hsv1[1] - hsv2[1]) * (hsv1[1] - hsv2[1]) +
                    200.0 * 200.0 * (hsv1[2] - hsv2[2]) * (hsv1[2] - hsv2[2]);

            if (err < minErr) {
                minErr = err;
                bestTeam = team;
            }
        }

        return bestTeam;
    }

    public void registerEvents() {
        for (UnitType unit : Vars.content.units()) {
            if (unit.itemCapacity != 10) {
                unit.itemCapacity = 10;
            }
        }
    }

    public static boolean spawnPet(Pet pet, Player player) {
        Log.info("Spawning pet '" + pet.name + "' for player " + player.name());
        UnitType unitType = Vars.content.units().find(u -> u.name.equals(pet.speciesName));
        Unit unit = unitType.spawn(player.team(), player.x, player.y);
        if (unit == null) {
            Log.err("Failed to spawn pet '" + pet.name + "' for player " + player.name());
            return false;
        }

        // initialize controller
        Team team = getTeam(pet.color);
        Log.info("Giving pet '" + pet.name + "' team " + team.id);
        UnitController controller = new PetController(player, pet.name, pet.color, team, rank(pet));
        unit.controller(controller);
        controller.unit(unit);

        Call.spawnEffect(unit.x, unit.y, unit.rotation, unit.type);
        Log.info("Spawned pet '" + pet.name + "' for player " + player.name());
        Events.fire(new EventType.UnitSpawnEvent(unit));
        return true;
    }

    static class PetController implements UnitController {
        final String uuid;
        final Player player;
        final String name;
        final Team unitTeam;
        final Color color;
        /**
         * 1/s
         */
        int maxVel = 250;
        int rank = 0;
        Unit unit;
        long prevTime = System.currentTimeMillis();
        boolean hasLabel = false;
        boolean isEating = false;
        // mining subset of eating
        Tile mining = null;
        // for friends
        float friendRotDir = 0;
        private int itemsEaten = 0;
        private long lastAction = 0;

        public PetController(Player player, String name, Color color, Team unitTeam, int rank) {
            this.player = player;
            this.uuid = player.uuid();
            this.name = name;
            this.color = color;
            this.unitTeam = unitTeam;
            this.rank = rank;
        }

        @Override
        public void unit(Unit unit) {
            this.unit = unit;
        }

        @Override
        public Unit unit() {
            return unit;
        }

        @Override
        public void removed(Unit ignore) {
            var pets = spawnedPets.get(uuid);
            pets.remove(name);

            if (player.con != null && player.con.isConnected()) {
                Call.sendMessage("Pet" + "yellow" + "Your pet [#" + color.toString().substring(0, 6) + "]" + name
                        + "[yellow] died! " +
                        "Make sure you are spawning any ground pets on empty tiles.");
            }
        }

        private Unit closePet() {
            for (Unit unit : Groups.unit) {
                if (isPet(unit)
                        && unit.dst(this.unit) <= 15 * Vars.tilesize
                        && unit.type == this.unit.type
                        && unit != this.unit
                        && petOwner(unit) != uuid) {
                    return unit;
                }
            }
            return null;
        }

        /**
         * Returns whether the pet is near a turret or friendly unit
         */
        private boolean isNearDanger() {
            for (var data : Vars.state.teams.active) {
                if (data.turretTree == null) {
                    continue;
                }
                final boolean[] shouldHide = new boolean[1];
                data.turretTree.intersect(unit.x - 500f, unit.y - 500f, 1000f, 1000f, turret -> {
                    if (!(turret.block() instanceof BaseTurret)) {
                        Log.warn("a turret that isn't a turret: " + turret.getClass().getCanonicalName());
                    }
                    BaseTurret bt = (BaseTurret) turret.block();
                    boolean targetAir = true;
                    if (bt instanceof Turret) {
                        targetAir = ((Turret) bt).targetAir;
                    }
                    if (bt instanceof PointDefenseTurret || bt instanceof BuildTurret) {
                        targetAir = false;
                    }

                    if (targetAir && unit.dst(turret) <= bt.range + Vars.tilesize) {
                        shouldHide[0] = true;
                    }
                });
                if (shouldHide[0]) {
                    return true;
                }
            }

            for (Unit enemyUnit : Groups.unit) {
                if (enemyUnit.team() == player.team() && !enemyUnit.isPlayer() &&
                        !(enemyUnit.controller() instanceof PetController) &&
                        !(enemyUnit.controller() instanceof MinerAI) && // exclude mono
                        enemyUnit.type.targetAir &&
                        enemyUnit.type != UnitTypes.mega && enemyUnit.type != UnitTypes.poly) { // exclude mega & poly
                    if (unit.dst(enemyUnit) <= enemyUnit.range() + Vars.tilesize) {
                        return true;
                    }
                }
            }

            return false;
        }

        private boolean isPet(Unit unit) {
            return unit.controller() instanceof PetController;
        }

        private String petOwner(Unit unit) {
            PetController controller = (PetController) unit.controller();
            return controller.uuid;
        }

        private CoreBlock.CoreBuild closeCore(float targetx, float targety) {
            var cores = Vars.state.teams.cores(player.team());
            if (cores == null || cores.size == 0)
                return null;
            CoreBlock.CoreBuild closestCore = null;
            float closestDst = 12 * Vars.tilesize;
            for (var core : cores) {
                if (core.dst(targetx, targety) <= closestDst) {
                    closestDst = unit.dst(core.x, core.y);
                    closestCore = core;
                }
            }
            return closestCore;
        }

        @SuppressWarnings("null")
        @Override
        public void updateUnit() {
            if (unit == null)
                return;
            if (!Groups.player.contains(p -> p == player)) {
                Log.warn("pet owner disconnected :(");
                Call.unitDespawn(unit);
            }

            // despawn pet if not in spawnedPets list
            var pets = spawnedPets.get(player.uuid());
            if (pets == null) {
                Call.unitDespawn(unit);
            }
            if (!pets.contains(name)) {
                Call.unitDespawn(unit);
            }

            long dt = System.currentTimeMillis() - prevTime;
            prevTime += dt;

            // set team
            if (isNearDanger()) {
                // stealth mode
                unit.team = Team.derelict;
            } else {
                unit.team = unitTeam;
            }

            // keep pet alive
            unit.health(unit.maxHealth);
            unit.shield(0);
            unit.shieldAlpha = 0;
            unit.armor(1000f);

            // determine angle behind which to set
            double theta = player.unit().rotation;
            var allPets = spawnedPets.get(uuid);
            if (allPets.size == 2) {
                int idx = allPets.indexOf(name);
                theta = theta - 25 + 50 * idx;
            } else if (allPets.size == 3) {
                int idx = allPets.indexOf(name);
                theta = theta - 45 + 45 * idx;
            }
            theta *= (Math.PI / 180);

            // movement
            float targetx = (player.x - (float) (40 * Math.cos(theta)));
            float targety = (player.y - (float) (40 * Math.sin(theta)));
            var core = closeCore(targetx, targety);
            if (core != null) {
                double thetaCore = core.angleTo(targetx, targety) * Math.PI / 180;
                targetx = core.x + 12 * (float) Vars.tilesize * (float) Math.cos(thetaCore);
                targety = core.y + 12 * (float) Vars.tilesize * (float) Math.sin(thetaCore);
            }

            float vx = 10f * (targetx - unit.x);
            float vy = 10f * (targety - unit.y);
            if (vx * vx + vy * vy > maxVel * maxVel) {
                double mul = Math.sqrt(maxVel * maxVel / ((double) vx * vx + (double) vy * vy));
                vx *= mul;
                vy *= mul;
            }

            unit.x += vx * (dt) / 1000f;
            unit.y += vy * (dt) / 1000f;

            // rotation
            if (!isEating) {
                Unit closePet = closePet();
                if (closePet != null) {
                    float targetAngle = unit.angleTo(closePet);
                    if (Math.abs(unit.rotation - targetAngle) >= 30) {
                        unit.rotation += (targetAngle - unit.rotation) * 0.25f;
                    } else {
                        if (unit.rotation - targetAngle >= 25) {
                            friendRotDir = -1;
                        } else if (unit.rotation - targetAngle <= -25) {
                            friendRotDir = 1;
                        }

                        if (friendRotDir > 0) {
                            unit.rotation += 2 * 360 * dt / 1000;
                        } else {
                            unit.rotation -= 2 * 360 * dt / 1000;
                        }
                    }
                } else {
                    unit.rotation = unit.angleTo(player);
                }
            }

            // boost
            unit.elevation(1f);

            // labels
            boolean isStill = Math.abs(vx) < 2 && Math.abs(vy) < 2;
            if (!hasLabel && isStill) {
                // Call.label(Utils.formatName(Rank.all[rank], "[#" +
                // color.toString().substring(0, 6) + "]" + name), 1f, unit.x, unit.y +
                // unit.hitSize() / 2 + Vars.tilesize);
                hasLabel = true;
                Timer.schedule(() -> {
                    hasLabel = false;
                }, 1f);
            }

            handleFood(isStill, dt);
        }

        protected void handleFood(boolean isStill, long dt) {
            // food
            if (!isEating && !unit.hasItem() && isStill) {
                int startX = unit.tileX() - 5;
                int startY = unit.tileY() - 5;
                Seq<Tile> tiles = new Seq<>();
                for (int x = startX; x < startX + 10; x++) {
                    for (int y = startY; y < startY + 10; y++) {
                        Tile tile = Vars.world.tiles.get(x, y);
                        if (tile == null)
                            continue;
                        if ((tile.drop() != null && Structs.contains(possibleFoods(unit.type), tile.drop())
                                && tile.block() == Blocks.air) ||
                                (tile.wallDrop() != null
                                        && Structs.contains(possibleFoods(unit.type), tile.wallDrop()))) {
                            tiles.add(tile);
                        }
                    }
                }

                // Log.info("possible ores: " + tiles.size);
                if (tiles.size != 0) {
                    this.mining = tiles.random();
                    // Log.info("mining: " + this.mining.drop());
                    isEating = true;
                }
            }

            if (isEating) {
                // if `mining` is non-null, mine that tile
                // additionally increase stack 4/s (1/250ms)
                if (mining != null) {
                    unit.mineTile(mining);
                    if (System.currentTimeMillis() - lastAction > 250) {
                        if (unit.stack == null || unit.stack.item != mining.drop()) {
                            unit.stack = new ItemStack(mining.drop(), 1);
                        }
                        if (unit.stack.amount < unit.itemCapacity())
                            unit.stack.amount += 1;
                        lastAction = System.currentTimeMillis();
                    }
                }

                // if mine is too far away, skip to eating
                if (mining != null && (mining.x - unit.tileX()) * (mining.x - unit.tileX())
                        + (mining.y - unit.tileY()) * (mining.y - unit.tileY()) >= 10 * 10) {
                    mining = null;
                    if (unit.stack != null) {
                        itemsEaten = unit.stack.amount;
                    } else {
                        itemsEaten = 0;
                    }
                }
                if (unit.stack != null && unit.stack.amount >= unit.itemCapacity()) {
                    mining = null;
                    itemsEaten = unit.stack.amount;
                }

                // eat
                if (unit.stack != null && mining == null) {
                    // eat one item every 500ms
                    if (System.currentTimeMillis() - lastAction > 500) {
                        unit.stack.amount -= 1;
                        unit.heal();
                        lastAction = System.currentTimeMillis();
                    }

                    if (unit.stack.amount == 0) {
                        var item = unit.stack.item;
                        unit.clearItem();
                        isEating = false;

                        // update database
                        int amount = itemsEaten;
                        var pet = Structs.find(Petsdata.getPets(uuid), p -> p.name.equals(this.name));
                        if (pet == null) { // pet was deleted
                            Call.unitDespawn(unit);
                            return;
                        }
                        if (item == Items.coal) {
                            pet.eatenCoal += amount;
                        } else if (item == Items.copper) {
                            pet.eatenCopper += amount;
                        } else if (item == Items.lead) {
                            pet.eatenLead += amount;
                        } else if (item == Items.titanium) {
                            pet.eatenTitanium += amount;
                        } else if (item == Items.thorium) {
                            pet.eatenThorium += amount;
                        } else if (item == Items.beryllium) {
                            pet.eatenBeryllium += amount;
                        }
                        rank = rank(pet);
                        Petsdata.updatePet(pet);
                    }
                }

                // rotation
                if (mining != null)
                    unit.rotation = unit.angleTo(mining);
                else
                    // spin 360 degrees/sec
                    unit.rotation += dt * 360f / 1000f;

            }
        }
    }
}
