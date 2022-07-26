package fr.rayto.randomcrates.commands;

import fr.rayto.randomcrates.RandomCrates;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.HashSet;

public class RandomCratesCommand implements CommandExecutor {

    /**
     * Get Main class
     */
    private final RandomCrates main;

    public RandomCratesCommand(RandomCrates randomCrates) {
        this.main = randomCrates;
    }

    /**
     * Artificially generate the crate event
     *
     * @param sender Source object which is executing this command
     * @param commandLabel The alias of the command used
     * @param command The command sent
     * @param args All arguments passed to the command, split via ' '
     * @return true if the command was successful, otherwise false
     */
    public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {


        if (sender instanceof Player) {
            if(main.getConfig().getBoolean("crate.event_ready")) {
                Player player = (Player) sender;
                Location crateLoc = main.generate(player.getWorld(), 19200, -1530, 14200, 900, 0, 0);

                if (!crateLoc.getChunk().isLoaded()) {
                    crateLoc.getChunk().load();
                }

                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "summon MobCrate " + crateLoc.getX() + " " + crateLoc.getY() + " " + crateLoc.getZ());

                LivingEntity crate = null;

                for (Entity nearby : getNearbyEntities(crateLoc, 5)) {
                    if (nearby.getType().name().equals("WONCORE_MOBCRATE")) {
                        crate = (LivingEntity) nearby;
                        crate.setRemoveWhenFarAway(false);
                    }
                }

                main.StartParachute(crate);


                Bukkit.broadcastMessage("§8§l§m+                                    +");
                Bukkit.broadcastMessage(" ");
                Bukkit.broadcastMessage("§fLe §6largage §fa été lancé !");
                Bukkit.broadcastMessage(" ");
                Bukkit.broadcastMessage("§6x:§f " + Math.round(crateLoc.getX()) + " §6y:§f " + Math.round(crateLoc.getY()) + " §6z:§f " + Math.round(crateLoc.getZ()));
                Bukkit.broadcastMessage(" ");
                Bukkit.broadcastMessage("§fDepêchez-vous ! Le largage va disparaitre dans§6 " + main.getConfig().getInt("crate.despawn_time") + " §fminutes.");
                Bukkit.broadcastMessage("§8§l§m+                                    +");

                int loc = crateLoc.getWorld().getHighestBlockYAt((int) crateLoc.getX(), (int) crateLoc.getZ());
                player.teleport(new Location(crateLoc.getWorld(), crateLoc.getX(), loc, (int) crateLoc.getZ()));

                LivingEntity finalCrate = crate;
                Bukkit.getScheduler().scheduleSyncDelayedTask(main, () -> {

                    if(finalCrate != null) {

                        if(!(finalCrate.isDead())) {
                            finalCrate.setHealth(0.0);

                            Bukkit.broadcastMessage(" ");
                            Bukkit.broadcastMessage("§7Personne n'a récupéré le largage, il a subitement disparue !");
                            Bukkit.broadcastMessage(" ");
                        }

                    }


                }, (long) main.getConfig().getInt("crate.despawn_time") *60*20);


                return true;
            } else {
                sender.sendMessage("§cErreur : Vous n'avez pas configuré le plugin, la crate ne peut pas être envoyée.");
            }

        }
        return false;
    }

    /**
     * Artificially generate the crate event
     *
     * @param location The location from which we want to retrieve the entities
     * @param radius The detection radius of the entities around the location
     * @return Entities
     */
    public static Entity[] getNearbyEntities(Location location, int radius) {
        int chunkRadius = radius < 16 ? 1 : (radius - (radius % 16)) / 16;
        HashSet<Entity> radiusEntities = new HashSet<>();
        for (int chX = -chunkRadius; chX <= chunkRadius; chX++) {
            for (int chZ = -chunkRadius; chZ <= chunkRadius; chZ++) {
                int x = (int) location.getX(), y = (int) location.getY(), z = (int) location.getZ();
                for (Entity entity : new Location(location.getWorld(), x + (chX * 16), y, z + (chZ * 16)).getChunk().getEntities()) {
                    if (entity.getLocation().distance(location) <= radius && entity.getLocation().getBlock() != location.getBlock())
                        radiusEntities.add(entity);
                }
            }
        }
        return radiusEntities.toArray(new Entity[0]);
    }
}