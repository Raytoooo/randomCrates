package fr.rayto.randomcrates;

import fr.rayto.randomcrates.commands.RandomCratesCommand;
import fr.rayto.randomcrates.listener.EntityDamageByEntityListener;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.Random;

public final class RandomCrates extends JavaPlugin {

    /* Config */
    FileConfiguration config = getConfig();

    /*  States & Obj */
    private State current;

    /*  Runnable */
    public int task1;
    public int task3;

    /*On Enable*/
    @Override
    public void onEnable()
    {
        config.addDefault("crate.event_ready", false);
        config.addDefault("crate.event_frequency", 60);
        config.addDefault("crate.preparation_time", 2);
        config.addDefault("crate.despawn_time", 10);
        config.addDefault("crate.world_spawn_name", "world");
        config.addDefault("crate.loot_number", 10);


        int i =0 ;
        while(i < config.getInt("crate.loot_number")  ){
            i++;
            config.addDefault("loot." + i +".id", 0);
            config.addDefault("loot."+ i +".amount_max", 1);
            config.addDefault("loot."+ i +".item_name", "default");
            config.addDefault("loot."+ i +".drop_rate", 0);
        }

        config.options().copyDefaults(true);
        saveConfig();
        setState(State.WAITING);

        getCommand("crate").setExecutor(new RandomCratesCommand(this));

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new EntityDamageByEntityListener(this), this);

        System.out.println("RandomCrates - Plugin by Rayto#1745");

        if(config.getBoolean("crate.event_ready")) {
            EventRepetitionScheduler();
            getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "RandomCrates - Le plugin est opérationel et configuré !");
        } else {
            getServer().getConsoleSender().sendMessage(ChatColor.RED + "RandomCrates - Veuillez configurer le plugin afin qu'il soit opérationnel.");
        }
    }

    /*On Disable*/
    @Override
    public void onDisable() {
        System.out.println("[RandomCrates] Désactivé ! ");
    }


    /**
     * Allows slowing down the velocity of the fall of the crate entity
     *
     * @param crate crate that requires a parachute
     */
    public void StartParachute(LivingEntity crate) {
        this.task1 = Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> {
            if(!crate.getLocation().getChunk().isLoaded()) {
                crate.getLocation().getChunk().load();
            }
            if(crate.isDead()) {
                stopScheduler(task1);
            }
            if( crate.getVelocity().getY() != 0.0 ) {
                if (-0.5 > crate.getVelocity().getY() && -0.1 > crate.getVelocity().getY()) {
                    crate.setVelocity(new Vector(0, crate.getVelocity().getY() / 2, 0));
                }
            }
            if(getDistance(crate) < 10){
                if(getDistance(crate) <2) {
                    stopScheduler(task1);
                }
                crate.setFallDistance(0.0F);
                crate.setVelocity(new Vector(0,crate.getVelocity().getY()/2.5,0));
            }
        }, 0, 5);
    }

    /**
     * Cancel BukkitScheduler task
     *
     * @param task The task to be stopped
     */
    public void stopScheduler(int task) {
        if(task == task1) {
            Bukkit.getServer().getScheduler().cancelTask(this.task1);
        }
    }

    /**
     * Recover the distance between an entity and the ground.
     *
     * @param entity The entity whose distance from the ground is required
     */
    public static int getDistance(Entity entity){
        Location loc = entity.getLocation().clone();
        double y = loc.getBlockY();
        int distance = 0;
        for (double i = y; i >= 0; i--){
            loc.setY(i);
            if(loc.getBlock().getType().isSolid())break;
            distance++;
        }
        return distance-1;
    }

    /**
     * Allows to randomly generate a location in a predefined area.
     * This location will be a safe location, the ground must not be water or lava.
     * The location will be in Y=250 because the crate falls from the sky, parachuted.
     *
     * @param world The world in which we want to generate a random location
     * @param Xmax The highest point X in the area
     * @param Xmin The lowest point X in the area
     * @param Zmax The highest Z-point in the area
     * @param Zmin The lowest Z-point in the area
     * @param yaw The horizontal orientation of the location
     * @param pitch The vertical orientation of the location
     * @return Location random
     */
    public Location generate(World world, double Xmax, double Xmin, double Zmax, double Zmin , float yaw, float pitch) {
        Random random = new Random();
        Material below = null;
        Location location = null;
        while (below == null || !(below.isSolid())) {
            int x = (int) (random.nextInt((int) (Xmax-Xmin)) + Xmin);
            int z = (int) (random.nextInt((int) (Zmax-Zmin)) + Zmin);
            int y = world.getHighestBlockYAt(x,z);
            location = new Location(world, x, y+ (250-y), z, yaw, pitch);
            below = world.getBlockAt(x, y-1, z).getType();
        }
        return location;
    }

    /**
     * Only takes care of starting the event and the required schedulers, spawn the crate, generate the random location.
     */
    public void EventRepetitionScheduler() {
        this.task3 = Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> {
            Location crateLoc = this.generate(Bukkit.getWorld(config.getString("crate.world_spawn_name")), 19200, -1530, 14200, 900, 0, 0);

            Bukkit.broadcastMessage("§8§l§m+                                    +");
            Bukkit.broadcastMessage(" ");
            Bukkit.broadcastMessage("§fUn largage va être lancé§6 "+ config.getInt("crate.despawn_time") +  " §fminutes.");
            Bukkit.broadcastMessage(" ");
            Bukkit.broadcastMessage("§6x:§f " + Math.round(crateLoc.getX()) + " §6y:§f " + Math.round(crateLoc.getWorld().getHighestBlockYAt((int)crateLoc.getX(),(int)crateLoc.getY())) + " §6z:§f " + Math.round(crateLoc.getZ()));
            Bukkit.broadcastMessage(" ");
            Bukkit.broadcastMessage("§8§l§m+                                    +");

            if (!crateLoc.getChunk().isLoaded()) {
                crateLoc.getChunk().load();
            }

            Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> {


                Bukkit.broadcastMessage("§8§l§m+                                    +");
                Bukkit.broadcastMessage(" ");
                Bukkit.broadcastMessage("§fLe §6largage a été lancé !");
                Bukkit.broadcastMessage(" ");
                Bukkit.broadcastMessage("§6x:§f " + Math.round(crateLoc.getX()) + " §6y:§f " + Math.round(crateLoc.getWorld().getHighestBlockYAt((int)crateLoc.getX(),(int)crateLoc.getY())) + " §6z:§f " + Math.round(crateLoc.getZ()));
                Bukkit.broadcastMessage(" ");
                Bukkit.broadcastMessage("§8§l§m+                                    +");

                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "summon MobCrate " + crateLoc.getX() + " " + crateLoc.getY() + " " + crateLoc.getZ());

                final LivingEntity[] crate = {null};

                for (Entity nearby : RandomCratesCommand.getNearbyEntities(crateLoc, 5)) {
                    if (nearby.getType().name().equals("WONCORE_MOBCRATE")) {
                        crate[0] = (LivingEntity) nearby;
                        crate[0].setRemoveWhenFarAway(false);
                    }
                }

                StartParachute(crate[0]);

                Bukkit.getScheduler().scheduleSyncDelayedTask(RandomCrates.this, () -> {
                    for (Entity nearby : RandomCratesCommand.getNearbyEntities(crateLoc, 5)) {
                        if (nearby.getType().name().equals("WONCORE_MOBCRATE")) {
                            crate[0] = (LivingEntity) nearby;
                            crate[0].setRemoveWhenFarAway(false);
                        }
                    }
                    if(crate[0] != null){
                        if(!(crate[0].isDead())) {
                            crate[0].setHealth(0.0);

                            Bukkit.broadcastMessage(" ");
                            Bukkit.broadcastMessage("§7Personne n'a récupéré le largage, il a subitement disparue !");
                            Bukkit.broadcastMessage(" ");
                        }

                    }
                }, (long) config.getInt("crate.despawn_time") *60*20);
            }, config.getLong("crate.preparation_time")*60*20);
        }, config.getLong("crate.event_frequency")*60*20, config.getLong("crate.event_frequency")*60*20);
    }


    /**
     * Allows you to change the status of the "supply box" event
     *
     * @param newCurrentState Set the new state of the crate event
     */
    public void setState(State newCurrentState) {
        this.current = newCurrentState;
    }

    /**
     * Returns a state equality, which will allow to perform some actions depending on the state of the event
     *
     * @param comparedState State to be compared to the current state
     * @return boolean Equality status
     */
    @SuppressWarnings("unused")
    public boolean isState(State comparedState) {
        return this.current == comparedState;
    }

    /**
     * Returns a state equality, which will allow to perform some actions depending on the state of the event
     *
     * @return State current state
     */
    @SuppressWarnings("unused")
    public State getState() {
        return this.current;
    }
}
