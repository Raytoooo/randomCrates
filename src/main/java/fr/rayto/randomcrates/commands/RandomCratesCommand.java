package fr.rayto.randomcrates.commands;

import fr.rayto.randomcrates.RandomCrates;
import net.minecraft.server.v1_7_R4.EntityPlayer;
import net.minecraft.server.v1_7_R4.MinecraftServer;
import net.minecraft.server.v1_7_R4.PlayerInteractManager;
import net.minecraft.server.v1_7_R4.WorldServer;
import net.minecraft.util.com.mojang.authlib.GameProfile;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.WeatherType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandException;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_7_R4.CraftServer;
import org.bukkit.craftbukkit.v1_7_R4.CraftWorld;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.UUID;

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
            Player player = (Player) sender;
            if(player.hasPermission("crate.init")){
                if(main.getConfig().getBoolean("crate.event_ready")) {
                    Location crateLoc = main.generate(player.getWorld(), 19200, -1530, 14200, 900, 0, 0);
                    crateLoc.getChunk().load(true);
                    Bukkit.getServer().getWorld(main.getConfig().getString("crate.world_spawn_name")).getChunkAt(crateLoc).load();

                    final LivingEntity[] crate = {null};


                    MinecraftServer nmsServer = ((CraftServer) Bukkit.getServer()).getServer();

                    WorldServer nmsWorld = ((CraftWorld) Bukkit.getWorlds().get(0)).getHandle();

                    EntityPlayer placeholder = new EntityPlayer(nmsServer, nmsWorld, new GameProfile(UUID.randomUUID(), "Largage"), new PlayerInteractManager(nmsWorld));

                    placeholder.setLocation(crateLoc.getX(), crateLoc.getY(), crateLoc.getZ(), 0, 0);
                    placeholder.teleportTo(crateLoc, false);
                    placeholder.weather=WeatherType.CLEAR;

                    CraftPlayer delegate = new CraftPlayer((CraftServer) main.getServer(), placeholder);
                    delegate.setOp(true);


                    Bukkit.getScheduler().scheduleSyncDelayedTask(main, () -> {
                        try {
                            Bukkit.getServer().dispatchCommand(delegate.getPlayer(), "summon MobCrate " + Math.round(crateLoc.getX()) + " " + Math.round(crateLoc.getY()) + " " + Math.round(crateLoc.getZ()));
                        } catch(CommandException ignored) {}
                        delegate.setOp(false);
                        delegate.disconnect("No reason");

                        for (Entity nearby : getNearbyEntities(crateLoc, 10)) {
                            if(nearby.getType().name().equals("WONCORE_MOBCRATE")) {
                                crate[0] = (LivingEntity) nearby;
                                crate[0].setRemoveWhenFarAway(false);
                            }
                        }

                        main.StartParachute(crate[0]);

                    }, 40L);


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

                    // TIMER END //
                    Bukkit.getScheduler().scheduleSyncDelayedTask(main, () -> {
                        if(crate[0] != null) {
                            if(!(crate[0].isDead())) {
                                crate[0].setHealth(0.0);

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

            } else {
                player.sendMessage("§cErreur: Vous n'avez pas la permission d'exécuter cette commande.");
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
