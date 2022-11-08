package fr.rayto.randomcrates.listener;

import fr.rayto.randomcrates.RandomCrates;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import java.util.Random;
import java.util.UUID;

public class EntityDamageByEntityListener implements Listener {
    private final RandomCrates main;
    public EntityDamageByEntityListener(RandomCrates randomCrates) {
        this.main = randomCrates;
    }

    /**
     * Detects if the attacked entity is a WonCore-MobCrate entity.
     * & If it is attacked by a player in order to prevent some natural minecraft
     * events such as knockback and generate loot when the Crate is killed.
     */
    @SuppressWarnings("deprecation")
    @EventHandler
    public void EntityKnockback(EntityDamageByEntityEvent event){
        if(event.getEntity() instanceof LivingEntity && (event.getDamager() instanceof Player || event.getDamager().getType().toString().contains("FLANS")) && !(event.getEntity() instanceof Player)){
            LivingEntity entity = (LivingEntity) event.getEntity();
            if(entity.getType().name().equals("WONCORE_MOBCRATE")) {
                entity.setVelocity(new Vector());
                Bukkit.getScheduler().scheduleSyncDelayedTask(main, () -> entity.setVelocity(new Vector()), 1);
                entity.setVelocity(new Vector());
                if (entity.getHealth() - event.getFinalDamage() <= 0) {
                    entity.damage(20.0);
                    Bukkit.getScheduler().scheduleSyncDelayedTask(main, () -> {
                        int i =0 ;
                        while(i < main.getConfig().getInt("crate.loot_number")){
                            i++;
                            int random = new Random().nextInt(100);
                            Material material = Material.getMaterial(main.getConfig().getInt("loot." + i + ".id"));
                            if(material.equals(Material.AIR)) continue;
                            if(main.getConfig().getInt("loot." + i +".drop_rate") >= random) {
                                int drop_rate = new Random().nextInt(main.getConfig().getInt("loot."+i+".amount_max")) +1; // 10 = 100% 10 < 10

                                ItemStack itemStack = new ItemStack(material, drop_rate);

                                if(!main.getConfig().getString("loot."+ i +".item_name").equals("default")){
                                    ItemMeta itemMeta = itemStack.getItemMeta();
                                    itemMeta.setDisplayName(main.getConfig().getString("loot."+ i +".item_name"));
                                    itemStack.setItemMeta(itemMeta);
                                }
                                entity.getWorld().dropItem(entity.getLocation(), itemStack);

                            }
                        }
                        if(event.getDamager() instanceof Player) {
                            Player player = (Player) event.getDamager();

                            Bukkit.broadcastMessage(" ");
                            Bukkit.broadcastMessage("§fLe largage a été récupéré par§6 " + player.getName() + "§f.");
                            Bukkit.broadcastMessage(" ");
                        } else {
                            String playername = "un joueur";

                            if(event.getDamager().getType().toString().equals("FLANSMOD_BULLET")) {
                                for(Entity e : entity.getNearbyEntities(128, 128, 128)) {
                                    if(e instanceof Player) {
                                        if(main.shooter.containsKey(e.getUniqueId())) {
                                            playername = ((Player)e).getName();
                                        }
                                    }
                                }
                            }

                            Bukkit.broadcastMessage(" ");
                            Bukkit.broadcastMessage("§fLe largage a été récupéré par §6" + playername + "§f.");
                            Bukkit.broadcastMessage(" ");
                        }
                    }, 20);

                }

            }
        }
    }


    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent event) {
        if(event.getAction().equals(Action.LEFT_CLICK_AIR) ||event.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
            if(event.getPlayer().getItemInHand().getType().toString().contains("FLANSMOD")) {
                UUID playerID = event.getPlayer().getUniqueId();
                if (main.shooter.containsKey(playerID)) {
                    Bukkit.getServer().getScheduler().cancelTask(main.shooter.get(playerID));
                    main.shooter.remove(playerID);
                }
                main.shooter.put(playerID,
                        Bukkit.getScheduler().scheduleSyncDelayedTask(main, () -> {
                            Bukkit.getServer().getScheduler().cancelTask(main.shooter.get(playerID));
                            main.shooter.remove(playerID);
                        }, 200L));

            }
        }
    }
}

