package live.supeer.metropolis;

import com.destroystokyo.paper.event.entity.PreCreatureSpawnEvent;
import io.papermc.paper.event.player.PlayerTradeEvent;
import live.supeer.apied.ApiedAPI;
import live.supeer.apied.MPlayer;
import live.supeer.metropolis.city.City;
import live.supeer.metropolis.city.CityDatabase;
import live.supeer.metropolis.plot.Plot;
import live.supeer.metropolis.plot.PlotDatabase;
import live.supeer.metropolis.utils.Utilities;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

public class ProtectionListener implements Listener {

    //(MOSTLY) EVENTS WITH THE FLAG 'b' (BUILD)

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        MPlayer mPlayer = ApiedAPI.getPlayer(player);
        if (mPlayer.isBanned()) {
            event.setCancelled(true);
            Metropolis.sendAccessDenied(player);
        }
        if (!Utilities.hasLocationPermissionFlags(player.getUniqueId(), event.getBlock().getLocation(), 'b') && !Metropolis.overrides.contains(player)) {
            event.setCancelled(true);
            Metropolis.sendAccessDenied(player);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        MPlayer mPlayer = ApiedAPI.getPlayer(player);
        if (mPlayer.isBanned()) {
            event.setCancelled(true);
            Metropolis.sendAccessDenied(player);
        }
        if (!Utilities.hasLocationPermissionFlags(player.getUniqueId(), event.getBlock().getLocation(), 'b') && !Metropolis.overrides.contains(player)) {
            event.setCancelled(true);
            Metropolis.sendAccessDenied(player);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onHangingBreakByEntity(HangingBreakByEntityEvent event) {
        if (!(event.getRemover() instanceof Player player)) return;
        if (!Utilities.hasLocationPermissionFlags(player.getUniqueId(), event.getEntity().getLocation(), 'b') && !Metropolis.overrides.contains(player)) {
            event.setCancelled(true);
            Metropolis.sendAccessDenied(player);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
        Player player = event.getPlayer();
        MPlayer mPlayer = ApiedAPI.getPlayer(player);
        if (mPlayer.isBanned()) {
            event.setCancelled(true);
            Metropolis.sendAccessDenied(player);
        }
        if (!Utilities.hasLocationPermissionFlags(player.getUniqueId(), event.getBlockClicked().getRelative(event.getBlockFace()).getLocation(), 'b') && !Metropolis.overrides.contains(player)) {
            event.setCancelled(true);
            Metropolis.sendAccessDenied(player);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerBucketFill(PlayerBucketFillEvent event) {
        Player player = event.getPlayer();
        if (!Utilities.hasLocationPermissionFlags(player.getUniqueId(), event.getBlockClicked().getLocation(), 'b') && !Metropolis.overrides.contains(player)) {
            event.setCancelled(true);
            Metropolis.sendAccessDenied(player);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onVehicleDestroy(VehicleDestroyEvent event) {
        if (!(event.getAttacker() instanceof Player player)) return;
        if (!Utilities.hasLocationPermissionFlags(player.getUniqueId(), event.getVehicle().getLocation(), 'b') && !Metropolis.overrides.contains(player)) {
            event.setCancelled(true);
            Metropolis.sendAccessDenied(player);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        MPlayer mPlayer = ApiedAPI.getPlayer(player);
        if (mPlayer.isBanned()) {
            event.setCancelled(true);
            Metropolis.sendAccessDenied(player);
        }
        if (event.getAction() == Action.RIGHT_CLICK_AIR) {
            ItemStack item = event.getItem();
            if (item != null) {
                Material itemType = item.getType();
                if (itemType == Material.OAK_BOAT || itemType == Material.SPRUCE_BOAT ||
                        itemType == Material.BIRCH_BOAT || itemType == Material.JUNGLE_BOAT || itemType == Material.ACACIA_BOAT ||
                        itemType == Material.DARK_OAK_BOAT || itemType == Material.CHERRY_BOAT || itemType == Material.MANGROVE_BOAT ||
                        itemType == Material.OAK_CHEST_BOAT || itemType == Material.SPRUCE_CHEST_BOAT ||
                        itemType == Material.BIRCH_CHEST_BOAT || itemType == Material.JUNGLE_CHEST_BOAT || itemType == Material.ACACIA_CHEST_BOAT ||
                        itemType == Material.DARK_OAK_CHEST_BOAT || itemType == Material.CHERRY_CHEST_BOAT || itemType == Material.MANGROVE_CHEST_BOAT) {
                    if (!Utilities.hasLocationPermissionFlags(player.getUniqueId(), player.getLocation().toBlockLocation(), 'b') && !Metropolis.overrides.contains(player)) {
                        event.setCancelled(true);
                        Metropolis.sendAccessDenied(player);
                    }
                } else if (itemType == Material.MAP) {
                    if (!Utilities.hasLocationPermissionFlags(player.getUniqueId(), player.getLocation().toBlockLocation(), 'b') && !Metropolis.overrides.contains(player)) {
                        event.setCancelled(true);
                        Metropolis.sendAccessDenied(player);
                    }
                } else if (itemType == Material.ENDER_PEARL || itemType == Material.TRIDENT) {
                    if (!Utilities.hasLocationPermissionFlags(player.getUniqueId(), player.getLocation().toBlockLocation(), 'e') && !Metropolis.overrides.contains(player)) {
                        event.setCancelled(true);
                        Metropolis.sendAccessDenied(player);
                    }
                }
            }
        }
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = event.getItem();

            Location location = Objects.requireNonNull(event.getClickedBlock()).getLocation();
            Material blockType = event.getClickedBlock().getType();
            if (item != null) {
                Material itemType = item.getType();
                if (itemType == Material.OAK_BOAT || itemType == Material.SPRUCE_BOAT ||
                        itemType == Material.BIRCH_BOAT || itemType == Material.JUNGLE_BOAT || itemType == Material.ACACIA_BOAT ||
                        itemType == Material.DARK_OAK_BOAT || itemType == Material.CHERRY_BOAT || itemType == Material.MANGROVE_BOAT ||
                        itemType == Material.OAK_CHEST_BOAT || itemType == Material.SPRUCE_CHEST_BOAT ||
                        itemType == Material.BIRCH_CHEST_BOAT || itemType == Material.JUNGLE_CHEST_BOAT || itemType == Material.ACACIA_CHEST_BOAT ||
                        itemType == Material.DARK_OAK_CHEST_BOAT || itemType == Material.CHERRY_CHEST_BOAT || itemType == Material.MANGROVE_CHEST_BOAT) {
                    if (!Utilities.hasLocationPermissionFlags(player.getUniqueId(), location, 'b') && !Metropolis.overrides.contains(player)) {
                        event.setCancelled(true);
                        Metropolis.sendAccessDenied(player);
                    }
                } else if (itemType == Material.MAP) {
                    if (!Utilities.hasLocationPermissionFlags(player.getUniqueId(), location, 'b') && !Metropolis.overrides.contains(player)) {
                        event.setCancelled(true);
                        Metropolis.sendAccessDenied(player);
                    }
                } else if (itemType == Material.ENDER_PEARL || itemType == Material.TRIDENT) {
                    if (!Utilities.hasLocationPermissionFlags(player.getUniqueId(), location, 'e') && !Metropolis.overrides.contains(player)) {
                        event.setCancelled(true);
                        Metropolis.sendAccessDenied(player);
                    }
                }
            }
            if (blockType == Material.ARMOR_STAND) {
                if (!Utilities.hasLocationPermissionFlags(player.getUniqueId(), location, 'b') && !Metropolis.overrides.contains(player)) {
                    event.setCancelled(true);
                    Metropolis.sendAccessDenied(player);
                }
            } else if (blockType == Material.REPEATER || blockType == Material.COMPARATOR) {
                if (!Utilities.hasLocationPermissionFlags(player.getUniqueId(), location, 'b') && !Metropolis.overrides.contains(player)) {
                    event.setCancelled(true);
                    Metropolis.sendAccessDenied(player);
                }
            } else if (blockType == Material.FLOWER_POT) {
                if (!Utilities.hasLocationPermissionFlags(player.getUniqueId(), location, 'b') && !Metropolis.overrides.contains(player)) {
                    event.setCancelled(true);
                    Metropolis.sendAccessDenied(player);
                }
            } else if (blockType == Material.CAKE) {
                if (!Utilities.hasLocationPermissionFlags(player.getUniqueId(), location, 'k') && !Metropolis.overrides.contains(player)) {
                    event.setCancelled(true);
                    Metropolis.sendAccessDenied(player);
                }
            } else if (blockType == Material.BEEHIVE || blockType == Material.BEE_NEST) {
                if (!Utilities.hasLocationPermissionFlags(player.getUniqueId(), location, 'a') && !Metropolis.overrides.contains(player)) {
                    event.setCancelled(true);
                    Metropolis.sendAccessDenied(player);
                }

            } else if (blockType == Material.NOTE_BLOCK) {
                if (!Utilities.hasLocationPermissionFlags(player.getUniqueId(), location, 'b') && !Metropolis.overrides.contains(player)) {
                    event.setCancelled(true);
                    Metropolis.sendAccessDenied(player);
                }
            } else if (event.getClickedBlock().getState() instanceof Sign sign) {
                if (!sign.isWaxed()) {
                    if (!Utilities.hasLocationPermissionFlags(player.getUniqueId(), location, 'b') && !Metropolis.overrides.contains(player)) {
                        event.setCancelled(true);
                        Metropolis.sendAccessDenied(player);
                    }
                }
            } else if (Utilities.isBlockContainer(blockType)) {
                if (!Utilities.hasLocationPermissionFlags(player.getUniqueId(), location, 'c') && !Metropolis.overrides.contains(player)) {
                    event.setCancelled(true);
                    Metropolis.sendAccessDenied(player);
                }
            } else if (blockType == Material.FURNACE || blockType == Material.BLAST_FURNACE || blockType == Material.SMOKER || blockType == Material.CAMPFIRE || blockType == Material.COMPOSTER || blockType == Material.BEACON) {
                if (!Utilities.hasLocationPermissionFlags(player.getUniqueId(), location, 'c') && !Metropolis.overrides.contains(player)) {
                    event.setCancelled(true);
                    Metropolis.sendAccessDenied(player);
                }
            } else if (blockType == Material.HOPPER || blockType == Material.DROPPER || blockType == Material.DISPENSER) {
                if (!Utilities.hasLocationPermissionFlags(player.getUniqueId(), location, 'c') && !Metropolis.overrides.contains(player)) {
                    event.setCancelled(true);
                    Metropolis.sendAccessDenied(player);
                }
            } else if (blockType == Material.JUKEBOX) {
                if (!Utilities.hasLocationPermissionFlags(player.getUniqueId(), location, 'j') && !Metropolis.overrides.contains(player)) {
                    event.setCancelled(true);
                    Metropolis.sendAccessDenied(player);
                }
            } else if (blockType == Material.BREWING_STAND) {
                if (!Utilities.hasLocationPermissionFlags(player.getUniqueId(), location, 'c') && !Metropolis.overrides.contains(player)) {
                    event.setCancelled(true);
                    Metropolis.sendAccessDenied(player);
                }
            } else if (blockType == Material.CAULDRON) {
                if (!Utilities.hasLocationPermissionFlags(player.getUniqueId(), location, 'c') && !Metropolis.overrides.contains(player)) {
                    event.setCancelled(true);
                    Metropolis.sendAccessDenied(player);
                }
            } else if (blockType == Material.BELL) {
                if (!Utilities.hasLocationPermissionFlags(player.getUniqueId(), location, 'b') && !Metropolis.overrides.contains(player)) {
                    event.setCancelled(true);
                    Metropolis.sendAccessDenied(player);
                }
            } else if (blockType == Material.STONE_BUTTON || blockType == Material.POLISHED_BLACKSTONE_BUTTON || blockType == Material.LEVER) {
                if (!Utilities.hasLocationPermissionFlags(player.getUniqueId(), location, 's') && !Metropolis.overrides.contains(player)) {
                    event.setCancelled(true);
                    Metropolis.sendAccessDenied(player);
                }
            } else if (blockType == Material.ANVIL || blockType == Material.CHIPPED_ANVIL || blockType == Material.DAMAGED_ANVIL) {
                if (!Utilities.hasLocationPermissionFlags(player.getUniqueId(), location, 'r') && !Metropolis.overrides.contains(player)) {
                    event.setCancelled(true);
                    Metropolis.sendAccessDenied(player);
                }
            } else if (blockType == Material.ACACIA_DOOR || blockType == Material.BIRCH_DOOR || blockType == Material.CRIMSON_DOOR || blockType == Material.DARK_OAK_DOOR || blockType == Material.JUNGLE_DOOR || blockType == Material.OAK_DOOR || blockType == Material.SPRUCE_DOOR || blockType == Material.WARPED_DOOR || blockType == Material.BAMBOO_DOOR || blockType == Material.MANGROVE_DOOR || blockType == Material.CHERRY_DOOR || blockType == Material.COPPER_DOOR || blockType == Material.EXPOSED_COPPER_DOOR || blockType == Material.OXIDIZED_COPPER_DOOR || blockType == Material.WAXED_COPPER_DOOR || blockType == Material.WAXED_EXPOSED_COPPER_DOOR || blockType == Material.WAXED_OXIDIZED_COPPER_DOOR || blockType == Material.WAXED_WEATHERED_COPPER_DOOR || blockType == Material.WEATHERED_COPPER_DOOR) {
                if (!Utilities.hasLocationPermissionFlags(player.getUniqueId(), location, 'd') && !Metropolis.overrides.contains(player)) {
                    event.setCancelled(true);
                    Metropolis.sendAccessDenied(player);
                }
            } else if (blockType == Material.ACACIA_FENCE_GATE || blockType == Material.BIRCH_FENCE_GATE || blockType == Material.CRIMSON_FENCE_GATE || blockType == Material.DARK_OAK_FENCE_GATE || blockType == Material.JUNGLE_FENCE_GATE || blockType == Material.OAK_FENCE_GATE || blockType == Material.SPRUCE_FENCE_GATE || blockType == Material.WARPED_FENCE_GATE || blockType == Material.BAMBOO_FENCE_GATE || blockType == Material.MANGROVE_FENCE_GATE || blockType == Material.CHERRY_FENCE_GATE) {
                if (!Utilities.hasLocationPermissionFlags(player.getUniqueId(), location, 'b') && !Metropolis.overrides.contains(player)) {
                    event.setCancelled(true);
                    Metropolis.sendAccessDenied(player);
                }
            } else if (blockType == Material.ACACIA_TRAPDOOR || blockType == Material.BIRCH_TRAPDOOR || blockType == Material.CRIMSON_TRAPDOOR || blockType == Material.DARK_OAK_TRAPDOOR || blockType == Material.JUNGLE_TRAPDOOR || blockType == Material.OAK_TRAPDOOR || blockType == Material.SPRUCE_TRAPDOOR || blockType == Material.WARPED_TRAPDOOR || blockType == Material.BAMBOO_TRAPDOOR || blockType == Material.MANGROVE_TRAPDOOR || blockType == Material.CHERRY_TRAPDOOR || blockType == Material.COPPER_TRAPDOOR || blockType == Material.EXPOSED_COPPER_TRAPDOOR || blockType == Material.OXIDIZED_COPPER_TRAPDOOR || blockType == Material.WAXED_COPPER_TRAPDOOR || blockType == Material.WAXED_EXPOSED_COPPER_TRAPDOOR || blockType == Material.WAXED_OXIDIZED_COPPER_TRAPDOOR || blockType == Material.WAXED_WEATHERED_COPPER_TRAPDOOR || blockType == Material.WEATHERED_COPPER_TRAPDOOR) {
                if (!Utilities.hasLocationPermissionFlags(player.getUniqueId(), location, 'd') && !Metropolis.overrides.contains(player)) {
                    event.setCancelled(true);
                    Metropolis.sendAccessDenied(player);
                }
            }
        }
        if (event.getAction() == Action.PHYSICAL) {
            Location location = Objects.requireNonNull(event.getClickedBlock()).getLocation();
            Material blockType = event.getClickedBlock().getType();
            if (blockType == Material.FARMLAND) {
                if (!Utilities.hasLocationPermissionFlags(player.getUniqueId(), location, 'b') && !Metropolis.overrides.contains(player)) {
                    event.setCancelled(true);
                    Metropolis.sendAccessDenied(player);
                }
            } else if (blockType == Material.TURTLE_EGG) {
                if (!Utilities.hasLocationPermissionFlags(player.getUniqueId(), location, 'b') && !Metropolis.overrides.contains(player)) {
                    event.setCancelled(true);
                    Metropolis.sendAccessDenied(player);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockFertilizeEvent(BlockFertilizeEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        Location location = event.getBlock().getLocation();
        if (!Utilities.hasLocationPermissionFlags(player.getUniqueId(), location, 'b') && !Metropolis.overrides.contains(player)) {
            event.setCancelled(true);
            Metropolis.sendAccessDenied(player);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerHarvestBlock(PlayerHarvestBlockEvent event) {
        Player player = event.getPlayer();
        Location location = event.getHarvestedBlock().getLocation();
        if (!Utilities.hasLocationPermissionFlags(player.getUniqueId(), location, 'b') && !Metropolis.overrides.contains(player)) {
            event.setCancelled(true);
            Metropolis.sendAccessDenied(player);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerTakeLecternBook(PlayerTakeLecternBookEvent event) {
        Player player = event.getPlayer();
        Location location = event.getLectern().getLocation();
        if (!Utilities.hasLocationPermissionFlags(player.getUniqueId(), location, 'b') && !Metropolis.overrides.contains(player)) {
            event.setCancelled(true);
            Metropolis.sendAccessDenied(player);
        }
    }

    //EVENTS WITH THE FLAG 'a' (ANIMAL)

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof Animals) {
            Player player = event.getPlayer();
            if (!Utilities.hasLocationPermissionFlags(player.getUniqueId(), event.getRightClicked().getLocation(), 'a') && !Metropolis.overrides.contains(player)) {
                event.setCancelled(true);
                Metropolis.sendAccessDenied(player);
            }
        } else if (event.getRightClicked() instanceof ItemFrame) {
            Player player = event.getPlayer();
            if (!Utilities.hasLocationPermissionFlags(player.getUniqueId(), event.getRightClicked().getLocation(), 'f') && !Metropolis.overrides.contains(player)) {
                event.setCancelled(true);
                Metropolis.sendAccessDenied(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamamgeByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Animals)) return;
        if (!(event.getDamager() instanceof Player player)) return;
        if (!Utilities.hasLocationPermissionFlags(player.getUniqueId(), event.getEntity().getLocation(), 'a') && !Metropolis.overrides.contains(player)) {
            event.setCancelled(true);
            Metropolis.sendAccessDenied(player);
        }
    }

    //EVENTS WITH THE FLAG 't' (TELEPORT)

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityPortal(EntityPortalEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!Utilities.hasLocationPermissionFlags(player.getUniqueId(), event.getTo(), 't') && !Metropolis.overrides.contains(player)) {
            event.setCancelled(true);
            Metropolis.sendAccessDenied(player);
        }
    }

    //EVENTS WITH THE FLAG 'e' (ENDERPEARL, AMONG OTHER THINGS)

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT && event.getCause() != PlayerTeleportEvent.TeleportCause.ENDER_PEARL) return;
        Player player = event.getPlayer();
        if (!Utilities.hasLocationPermissionFlags(player.getUniqueId(), event.getTo(), 'e') && !Metropolis.overrides.contains(player)) {
            event.setCancelled(true);
            Metropolis.sendAccessDenied(player);
        }
    }

    //EVENTS WITH THE FLAG 'v' (VILLAGER

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerTrade(PlayerTradeEvent event) {
        Player player = event.getPlayer();
        if (!Utilities.hasLocationPermissionFlags(player.getUniqueId(), event.getVillager().getLocation(), 'v') && !Metropolis.overrides.contains(player)) {
            event.setCancelled(true);
            Metropolis.sendAccessDenied(player);
        }
    }

    //EVENTS FOR PLOT TYPES AND FLAGS

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPreCreatureSpawn(PreCreatureSpawnEvent event) {
        if (event.getType().getEntityClass() == null) {
            return;
        }
        if (Animals.class.isAssignableFrom(event.getType().getEntityClass())) {
            City city = CityDatabase.getCityByClaim(event.getSpawnLocation().toBlockLocation());
            if (city != null) {
                Plot plot = PlotDatabase.getCityPlot(city, event.getSpawnLocation().toBlockLocation());
                if (plot != null) {
                    if (!plot.hasFlag('a')) {
                        event.setCancelled(true);
                        event.setShouldAbortSpawn(true);
                        return;
                    }
                }
                if (!city.hasFlag('a')) {
                    event.setCancelled(true);
                    event.setShouldAbortSpawn(true);
                }
            }
        }
        if (Monster.class.isAssignableFrom(event.getType().getEntityClass())) {
            City city = CityDatabase.getCityByClaim(event.getSpawnLocation().toBlockLocation());
            if (city != null) {
                Plot plot = PlotDatabase.getCityPlot(city, event.getSpawnLocation().toBlockLocation());
                if (plot != null) {
                    if (!plot.hasFlag('m')) {
                        event.setCancelled(true);
                        event.setShouldAbortSpawn(true);
                        return;
                    }
                }
                if (city.hasFlag('m')) {
                    event.setCancelled(true);
                    event.setShouldAbortSpawn(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        City city = CityDatabase.getCityByClaim(player.getLocation().toBlockLocation());
        if (city != null) {
            Plot plot = PlotDatabase.getCityPlot(city, player.getLocation().toBlockLocation());
            if (plot != null) {
                if (plot.hasFlag('i')) {
                    event.setKeepInventory(true);
                }
                if (plot.hasFlag('x')) {
                    event.setKeepLevel(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        if (!(event.getEntity() instanceof Player player)) {
            if (!(event.getEntity() instanceof Monster)) {
                City city = CityDatabase.getCityByClaim(event.getEntity().getLocation());
                if (city != null) {
                    Plot plot = PlotDatabase.getCityPlot(city, event.getEntity().getLocation());
                    if (plot != null) {
                        if (!plot.hasFlag('a')) {
                            event.setCancelled(true);
                        }
                    } else {
                        if (!city.hasFlag('a')) {
                            event.setCancelled(true);
                        }
                    }
                }
            }
        } else {
            City city = CityDatabase.getCityByClaim(player.getLocation().toBlockLocation());
            if (city != null) {
                Plot plot = PlotDatabase.getCityPlot(city, player.getLocation().toBlockLocation());
                if (plot != null) {
                    if (!plot.hasFlag('p')) {
                        event.setCancelled(true);
                    }
                } else {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (event.getEntity() instanceof Player) {
            return;
        }
        if (event.getEntity().getType() == EntityType.FALLING_BLOCK) {
            return;
        }
        City city = CityDatabase.getCityByClaim(event.getBlock().getLocation());
        if (city != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        MPlayer mPlayer = ApiedAPI.getPlayer(event.getPlayer());
        if (mPlayer.isBanned()) {
            event.setCancelled(true);
            Metropolis.sendAccessDenied(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerPickupItem(PlayerAttemptPickupItemEvent event) {
        MPlayer mPlayer = ApiedAPI.getPlayer(event.getPlayer());
        if (mPlayer.isBanned()) {
            event.setCancelled(true);
            Metropolis.sendAccessDenied(event.getPlayer());
        }
    }
}
