package live.supeer.metropolis;

import live.supeer.apied.ApiedAPI;
import live.supeer.apied.ChestManager;
import live.supeer.apied.MPlayer;
import live.supeer.apied.MPlayerManager;
import live.supeer.metropolis.city.City;
import live.supeer.metropolis.city.CityDatabase;
import live.supeer.metropolis.city.District;
import live.supeer.metropolis.city.Role;
import live.supeer.metropolis.command.CommandCity;
import live.supeer.metropolis.event.*;
import live.supeer.metropolis.plot.Plot;
import live.supeer.metropolis.plot.PlotDatabase;
import live.supeer.metropolis.utils.DateUtil;
import live.supeer.metropolis.utils.LocationUtil;
import live.supeer.metropolis.utils.Utilities;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.event.player.*;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import net.coreprotect.CoreProtect;
import net.coreprotect.CoreProtectAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.List;

public class MetropolisListener implements Listener {

    private CoreProtectAPI getCoreProtect() {
        Plugin corePlugin = Metropolis.getInstance().getServer().getPluginManager().getPlugin("CoreProtect");

        // Check that CoreProtect is loaded
        if (!(corePlugin instanceof CoreProtect)) {
            return null;
        }

        // Check that the API is enabled
        CoreProtectAPI CoreProtect = ((CoreProtect) corePlugin).getAPI();
        if (!CoreProtect.isEnabled()) {
            return null;
        }

        // Check that a compatible version of the API is loaded
        if (CoreProtect.APIVersion() < 9) {
            return null;
        }

        return CoreProtect;
    }

    private static final GeometryFactory geometryFactory = new GeometryFactory();

    private static final List<Player> savedPlayers = new ArrayList<>();

    public static HashMap<UUID, List<Location>> savedLocs = new HashMap<>();

    public static HashMap<UUID, Cell> waitingForSignClick = new HashMap<>();

    @EventHandler
    public static void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        MPlayer mPlayer = ApiedAPI.getPlayer(player);
        if (mPlayer.isBanned()) {
            if (!JailManager.hasCell(player.getUniqueId().toString())) {
                Cell cell = JailManager.getRandomEmptyCell();
                cell.setPrisonerUUID(player.getUniqueId().toString());
                player.teleport(cell.getLocation());
                JailManager.displaySign(cell);
            } else {
                Cell cell = JailManager.getCellForPrisoner(player.getUniqueId().toString());
                player.teleport(cell.getLocation());
            }
            JailManager.sendJailMessage(player);
            if (JailManager.banExpiresSoon(player.getUniqueId())) {
                Metropolis.getInstance().scheduleUnban(MPlayerManager.getExpiringBan(player.getUniqueId()));
            }
        } else {
            if (JailManager.hasCell(player.getUniqueId().toString())) {
                Cell cell = JailManager.getCellForPrisoner(player.getUniqueId().toString());
                cell.setPrisonerUUID(null);
                JailManager.displaySignEmptyCell(cell);
                player.teleport(mPlayer.getLastLocation());
            }
        }
        if (CityDatabase.getClaim(player.getLocation()) != null) {
            City city = Objects.requireNonNull(CityDatabase.getClaim(player.getLocation().toBlockLocation())).getCity();
            PlayerEnterCityEvent enterCityEvent = new PlayerEnterCityEvent(event.getPlayer(), city);
            Bukkit.getServer().getPluginManager().callEvent(enterCityEvent);
            District district = CityDatabase.getDistrict(player.getLocation());
            Plot plot = PlotDatabase.getPlotAtLocation(player.getLocation());
            if (district != null) {
                PlayerEnterDistrictEvent enterDistrictEvent = new PlayerEnterDistrictEvent(event.getPlayer(), district);
                Bukkit.getServer().getPluginManager().callEvent(enterDistrictEvent);
            }
            if (plot != null) {
                PlayerEnterPlotEvent enterPlotEvent = new PlayerEnterPlotEvent(event.getPlayer(), plot);
                Bukkit.getServer().getPluginManager().callEvent(enterPlotEvent);
            }
        } else {
            Utilities.sendNatureScoreboard(player);
            Metropolis.playerInCity.remove(player.getUniqueId());
        }
        if (!mPlayer.isBanned()) {
            List<City> memberCities = CityDatabase.memberCityList(player.getUniqueId().toString());
            if (memberCities != null) {
                for (City city : memberCities) {
                    String cityMotd = city.getMotdMessage();
                    if (cityMotd != null) {
                        if (city.isReserve()) {
                            Metropolis.sendMessage(player, "messages.city.motd.reserve", "%cityname%", city.getCityName(), "%motd%", cityMotd);
                        } else {
                            Metropolis.sendMessage(player, "messages.city.motd.normal", "%cityname%", city.getCityName(), "%motd%", cityMotd);
                        }
                    }
                    if (!city.isReserve() && city.cityCouldGoUnder(1)) {
                        Metropolis.sendMessage(player, "messages.city.warning.lowBalance", "%cityname%", city.getCityName());
                    }
                }
            }
        }
    }

    public static HashMap<UUID, Polygon> playerPolygons = new HashMap<>();
    public static HashMap<UUID, Integer> playerYMin = new HashMap<>();
    public static HashMap<UUID, Integer> playerYMax = new HashMap<>();
    public static HashMap<UUID, List<String[]>> savedBlockHistory = new HashMap<>();

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && Objects.requireNonNull(event.getClickedBlock()).getState() instanceof Sign) {
            if (!waitingForSignClick.containsKey(player.getUniqueId())) {
                return;
            }
            event.setCancelled(true);
            if (JailManager.signAlreadyExists(event.getClickedBlock().getLocation())) {
                Metropolis.sendMessage(player, "messages.cell.sign.alreadyExists");
                return;
            }
            Cell cell = waitingForSignClick.get(player.getUniqueId());
            JailManager.UpdateCellSign(cell, event.getClickedBlock().getLocation(), ((Sign) event.getClickedBlock().getState()).getInteractableSideFor(player));
            Metropolis.sendMessage(player, "messages.cell.sign.set");
            JailManager.displaySignEmptyCell(cell);
            waitingForSignClick.remove(player.getUniqueId());
        }

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            if (event.getMaterial() == Material.STICK) {
                event.setCancelled(true);
                savedPlayers.remove(player);
                savedLocs.remove(player.getUniqueId());
                savedLocs.put(player.getUniqueId(), new ArrayList<>());
                savedLocs.get(player.getUniqueId()).add(event.getClickedBlock().getLocation());
                playerYMax.remove(player.getUniqueId());
                playerYMin.remove(player.getUniqueId());
                playerPolygons.remove(player.getUniqueId());
                Metropolis.sendMessage(
                        player,
                        "messages.city.markings.new",
                        "%world%",
                        event.getClickedBlock().getWorld().getName(),
                        "%x%",
                        String.valueOf(event.getClickedBlock().getX()),
                        "%y%",
                        String.valueOf(event.getClickedBlock().getY()),
                        "%z%",
                        String.valueOf(event.getClickedBlock().getZ()));
            }
        }
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (event.getMaterial() == Material.STICK) {
                Block block = event.getClickedBlock();
                assert block != null;
                if (block.getType().equals(Material.CHEST) || block.getType().equals(Material.TRAPPED_CHEST) || block.getType().equals(Material.BARREL) || block.getType().equals(Material.SHULKER_BOX)) {
                    if (ChestManager.chestInventories.containsKey(player.getUniqueId())) {
                        event.setCancelled(true);
                        if (block.getState() instanceof Chest chest1) {
                            Inventory inventory = chest1.getInventory();
                            if (inventory instanceof DoubleChestInventory doubleChestInventory) {
                                live.supeer.apied.Chest chest = ChestManager.getClaimedChest(event.getClickedBlock().getLocation());
                                if (chest == null) {
                                    Metropolis.sendMessage(player, "messages.chest.error.notLocked");
                                    return;
                                }
                                if (!chest.getOwnerUUID().equals(player.getUniqueId())) {
                                    Metropolis.sendMessage(player, "messages.chest.error.notOwner");
                                    return;
                                }
                                //TODO: ADD A CHECK FOR IF THE CITYCLAIM IS OCCUPIED
                                if (Arrays.stream(doubleChestInventory.getContents()).allMatch(Objects::isNull)) {
                                    int id = ChestManager.chestInventories.get(player.getUniqueId());
                                    Inventory archiveInventory = ChestManager.getInventoryFromArchive(id);
                                    if (archiveInventory != null) {
                                        chest1.getInventory().setContents(archiveInventory.getContents());
                                        ChestManager.removeArchive(id);
                                        ChestManager.chestInventories.remove(player.getUniqueId());
                                        Metropolis.sendMessage(player, "messages.chest.claimedArchive");
                                        return;
                                    } else {
                                        Metropolis.sendMessage(player, "messages.chest.error.noItems");
                                        return;
                                    }
                                } else {
                                    Metropolis.sendMessage(player, "messages.chest.error.notEmpty");
                                    return;
                                }
                            } else {
                                Metropolis.sendMessage(player, "messages.chest.error.notDouble");
                                return;
                            }
                        }
                        return;
                    }
                    if (ChestManager.isChestEventCounting(player.getUniqueId())) {
                        event.setCancelled(true);
                        String action = ChestManager.getChestEventCount(player.getUniqueId()).getType();
                        switch (action) {
                            case "info" -> {
                                ChestManager.decrementChestEventCount(player.getUniqueId());
                                live.supeer.apied.Chest chest = ChestManager.getClaimedChest(event.getClickedBlock().getLocation());
                                ChestManager.ChestEventCount chestEventCount = ChestManager.getChestEventCount(player.getUniqueId());
                                int count;
                                if (chestEventCount == null) {
                                    count = 0;
                                } else {
                                    count = chestEventCount.getCount();
                                }
                                if (chest == null) {
                                    Metropolis.sendMessage(player, "messages.chest.info.unlocked", "%location%", LocationUtil.formatLocation(event.getClickedBlock().getLocation()));
                                    if (!(count <= 0)) {
                                        Metropolis.sendMessage(player, "messages.chest.operationsleft", "%count%", String.valueOf(count));
                                    }
                                    return;
                                } else {
                                    MPlayer owner = ApiedAPI.getPlayer(chest.getOwnerUUID());
                                    Metropolis.sendMessage(player, "messages.chest.info.locked", "%location%", LocationUtil.formatLocation(event.getClickedBlock().getLocation()), "%owner%", owner.getName(), "%id%", String.valueOf(chest.getId()));
                                    if (!(count <= 0)) {
                                        Metropolis.sendMessage(player, "messages.chest.operationsleft", "%count%", String.valueOf(count));
                                    }
                                    return;
                                }
                            }
                            case "share" -> {
                                ChestManager.decrementChestEventCount(player.getUniqueId());
                                ChestManager.ChestEventCount chestEventCount = ChestManager.getChestEventCount(player.getUniqueId());
                                int count;
                                if (chestEventCount == null) {
                                    count = 0;
                                } else {
                                    count = chestEventCount.getCount();
                                }
                                ChestManager.handleChestShare(event.getClickedBlock().getLocation(), player, ChestManager.getChestEventCount(player.getUniqueId()).getTarget());
                                if (!(count <= 0)) {
                                    Metropolis.sendMessage(player, "messages.chest.operationsleft", "%count%", String.valueOf(count));
                                }
                                return;
                            }
                            case "give" -> {
                                ChestManager.decrementChestEventCount(player.getUniqueId());
                                ChestManager.ChestEventCount chestEventCount = ChestManager.getChestEventCount(player.getUniqueId());
                                int count;
                                if (chestEventCount == null) {
                                    count = 0;
                                } else {
                                    count = chestEventCount.getCount();
                                }
                                ChestManager.handleChestGive(event.getClickedBlock().getLocation(), player, ChestManager.getChestEventCount(player.getUniqueId()).getTarget());
                                if (!(count <= 0)) {
                                    Metropolis.sendMessage(player, "messages.chest.operationsleft", "%count%", String.valueOf(count));
                                }
                                return;
                            }
                            case "archive" -> {
                                ChestManager.decrementChestEventCount(player.getUniqueId());
                                ChestManager.ChestEventCount chestEventCount = ChestManager.getChestEventCount(player.getUniqueId());
                                int count;
                                if (chestEventCount == null) {
                                    count = 0;
                                } else {
                                    count = chestEventCount.getCount();
                                }
                                boolean city = MetropolisAPI.playerHasCityPermission(event.getClickedBlock().getLocation(), player, Role.ASSISTANT);
                                boolean self = player.hasPermission("mandatory.command.chest.archive");
                                ChestManager.handleArchiveItems(player, event.getClickedBlock().getLocation(), city, self);
                                if (!(count <= 0)) {
                                    Metropolis.sendMessage(player, "messages.chest.operationsleft", "%count%", String.valueOf(count));
                                }
                                return;
                            }
                        }
                        return;
                    }
                    ChestManager.handleChestClick(block.getLocation(), player);
                    event.setCancelled(true);
                    return;
                }
                if (CommandCity.blockEnabled.contains(player)) {
                    event.setCancelled(true);
                    if (CityDatabase.getClaim(event.getClickedBlock().getLocation()) == null) {
                        Metropolis.sendMessage(player, "messages.error.permissionDenied");
                        return;
                    }
                    City city = CityDatabase.getCityByClaim(event.getClickedBlock().getLocation());
                    assert city != null;
                    Role role = CityDatabase.getCityRole(city, player.getUniqueId().toString());
                    assert role != null;
                    Plot plot = PlotDatabase.getPlotAtLocation(event.getClickedBlock().getLocation());
                    if (plot != null) {
                        if (plot.getPlotOwnerUUID().equals(player.getUniqueId().toString()) || role.getPermissionLevel() > Role.ASSISTANT.getPermissionLevel()) {
                            coreProtectInteractCheck(player, event);
                            return;
                        } else {
                            Metropolis.sendMessage(
                                    player,
                                    "messages.error.city.permissionDenied",
                                    "%cityname%",
                                    city.getCityName());
                            return;
                        }
                    }
                    boolean isAssistant = Objects.equals(role, Role.ASSISTANT)
                            || Objects.equals(role, Role.VICE_MAYOR)
                            || Objects.equals(role, Role.MAYOR);
                    if (!isAssistant) {
                        Metropolis.sendMessage(
                                player, "messages.error.city.permissionDenied", "%cityname%", city.getCityName());
                        return;
                    }
                    coreProtectInteractCheck(player, event);
                    return;
                }
                event.setCancelled(true);
                if (savedPlayers.contains(player)) {
                    Metropolis.sendMessage(player, "messages.city.markings.finished");
                    return;
                }
                if (savedLocs.get(player.getUniqueId()) == null
                        || savedLocs.get(player.getUniqueId()).isEmpty()) {
                    Metropolis.sendMessage(player, "messages.city.markings.none");
                    return;
                }
                if (!savedLocs.get(player.getUniqueId()).isEmpty()
                        && savedLocs
                        .get(player.getUniqueId())
                        .get(savedLocs.get(player.getUniqueId()).size() - 1)
                        .equals(event.getClickedBlock().getLocation())) {
                    Metropolis.sendMessage(player, "messages.city.markings.sameBlock");
                    return;
                }
                if (!savedLocs.get(player.getUniqueId()).isEmpty()
                        && !savedLocs
                        .get(player.getUniqueId())
                        .get(0)
                        .getWorld()
                        .equals(event.getClickedBlock().getWorld())) {
                    Metropolis.sendMessage(player, "messages.city.markings.differentWorlds");
                    return;
                }

                Metropolis.sendMessage(
                        player,
                        "messages.city.markings.add",
                        "%world%",
                        event.getClickedBlock().getWorld().getName(),
                        "%x%",
                        String.valueOf(event.getClickedBlock().getX()),
                        "%y%",
                        String.valueOf(event.getClickedBlock().getY()),
                        "%z%",
                        String.valueOf(event.getClickedBlock().getZ()),
                        "%number%",
                        String.valueOf(savedLocs.get(player.getUniqueId()).size()));
                if (savedLocs.get(player.getUniqueId()).size() > 2
                        && savedLocs
                        .get(player.getUniqueId())
                        .get(0)
                        .equals(event.getClickedBlock().getLocation())) {
                    Polygon regionPolygon = LocationUtil.createPolygonFromLocations(savedLocs.get(player.getUniqueId()).toArray(new Location[0]), geometryFactory);
                    for (Location location : savedLocs.get(player.getUniqueId())) {
                        if (playerYMax.get(player.getUniqueId()) == null
                                || location.getBlockY() > playerYMax.get(player.getUniqueId())) {
                            playerYMax.put(player.getUniqueId(), location.getBlockY());
                        }
                        if (playerYMin.get(player.getUniqueId()) == null
                                || location.getBlockY() < playerYMin.get(player.getUniqueId())) {
                            playerYMin.put(player.getUniqueId(), location.getBlockY());
                        }
                    }
                    playerPolygons.put(player.getUniqueId(), regionPolygon);
                    Metropolis.sendMessage(player, "messages.city.markings.finish");
                    savedPlayers.add(player);
                }
                savedLocs.get(player.getUniqueId()).add(event.getClickedBlock().getLocation());
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (event.getBlock().getType().equals(Material.DIRT)) {
            if (CommandCity.blockEnabled.contains(player)) {
                event.setCancelled(true);
                if (CityDatabase.getClaim(event.getBlockPlaced().getLocation()) == null) {
                    Metropolis.sendMessage(player, "messages.error.permissionDenied");
                    return;
                }
                City city = CityDatabase.getCityByClaim(event.getBlockPlaced().getLocation());
                Role role = CityDatabase.getCityRole(city, player.getUniqueId().toString());
                assert role != null;
                Plot plot = PlotDatabase.getPlotAtLocation(event.getBlockPlaced().getLocation());
                if (plot != null) {
                    if (plot.getPlotOwnerUUID().equals(player.getUniqueId().toString()) || role.getPermissionLevel() > Role.ASSISTANT.getPermissionLevel()) {
                        coreProtectPlaceCheck(player, event);
                        return;
                    } else {
                        Metropolis.sendMessage(
                                player,
                                "messages.error.city.permissionDenied",
                                "%cityname%",
                                city.getCityName());
                        return;
                    }
                }
                boolean isAssistant = Objects.equals(role, Role.ASSISTANT)
                        || Objects.equals(role, Role.VICE_MAYOR)
                        || Objects.equals(role, Role.MAYOR);
                if (!isAssistant) {
                    Metropolis.sendMessage(
                            player, "messages.error.city.permissionDenied", "%cityname%", city.getCityName());
                    return;
                }
                coreProtectPlaceCheck(player, event);
            }
        }
    }
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom().toBlockLocation();
        Location to = event.getTo().toBlockLocation();
        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ()) {
            return;
        }
        // Existing city enter/exit logic
        if ((from.getBlockX() >> 4) != (to.getBlockX() >> 4) || (from.getBlockZ() >> 4) != (to.getBlockZ() >> 4)) {
            // Autoclaiming check
            if (AutoclaimManager.isAutoclaiming(player) && !from.getChunk().equals(to.getChunk())) {
                AutoclaimManager.AutoclaimInfo info = AutoclaimManager.getAutoclaimInfo(player);
                City city = info.getCity();
                AutoclaimAttemptEvent autoclaimEvent = new AutoclaimAttemptEvent(player, city, to.toBlockLocation());
                Bukkit.getPluginManager().callEvent(autoclaimEvent);
            }
            if (Metropolis.playerInCity.containsKey(player.getUniqueId()) && CityDatabase.getClaim(to) == null) {
                City fromCity = Metropolis.playerInCity.get(player.getUniqueId());
                PlayerExitCityEvent exitCityEvent = new PlayerExitCityEvent(player, fromCity, true);
                Bukkit.getServer().getPluginManager().callEvent(exitCityEvent);
            } else if (Metropolis.playerInCity.containsKey(player.getUniqueId()) && CityDatabase.getClaim(to) != null && Metropolis.playerInCity.get(player.getUniqueId()) != CityDatabase.getCityByClaim(to)) {
                City fromCity = Metropolis.playerInCity.get(player.getUniqueId());
                City toCity = CityDatabase.getCityByClaim(to);
                PlayerExitCityEvent exitCityEvent = new PlayerExitCityEvent(player, fromCity, false);
                Bukkit.getServer().getPluginManager().callEvent(exitCityEvent);
                PlayerEnterCityEvent enterCityEvent = new PlayerEnterCityEvent(player, toCity);
                Bukkit.getServer().getPluginManager().callEvent(enterCityEvent);
            } else if (!Metropolis.playerInCity.containsKey(player.getUniqueId()) && CityDatabase.getClaim(to) != null) {
                City toCity = CityDatabase.getCityByClaim(to);
                PlayerEnterCityEvent enterCityEvent = new PlayerEnterCityEvent(player, toCity);
                Bukkit.getServer().getPluginManager().callEvent(enterCityEvent);
            }
            return;
        }

        // Existing plot enter/exit logic
        if (from.getX() != to.getX() || from.getZ() != to.getZ() || from.getY() != to.getY()) {
            if (Metropolis.playerInCity.containsKey(player.getUniqueId())) {
                Plot plot = PlotDatabase.getPlotAtLocation(to);
                District district = CityDatabase.getDistrict(to);
                if (district == null) {
                    if (Metropolis.playerInDistrict.containsKey(player.getUniqueId())) {
                        PlayerExitDistrictEvent exitDistrictEvent = new PlayerExitDistrictEvent(player, Metropolis.playerInDistrict.get(player.getUniqueId()));
                        Metropolis.getInstance().getServer().getPluginManager().callEvent(exitDistrictEvent);
                    }
                } else if (!Metropolis.playerInDistrict.containsKey(player.getUniqueId())) {
                    PlayerEnterDistrictEvent enterDistrictEvent = new PlayerEnterDistrictEvent(player, district);
                    Metropolis.getInstance().getServer().getPluginManager().callEvent(enterDistrictEvent);
                }
                if (plot == null) {
                    if (Metropolis.playerInPlot.containsKey(player.getUniqueId())) {
                        PlayerExitPlotEvent exitEvent = new PlayerExitPlotEvent(player, Metropolis.playerInPlot.get(player.getUniqueId()));
                        Metropolis.getInstance().getServer().getPluginManager().callEvent(exitEvent);
                    }
                    return;
                }
                if (!Metropolis.playerInPlot.containsKey(player.getUniqueId())) {
                    PlayerEnterPlotEvent enterEvent = new PlayerEnterPlotEvent(player, plot);
                    Metropolis.getInstance().getServer().getPluginManager().callEvent(enterEvent);
                }
            }
        }
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        Location to = player.getLocation();
        UUID playerId = player.getUniqueId();

        if(AutoclaimManager.isAutoclaiming(player)) {
            final String cityname = AutoclaimManager.getAutoclaimInfo(player).getCity().getCityName();
            AutoclaimManager.stopAutoclaim(player);
            Metropolis.sendMessage(player, "messages.city.autoclaim.stopped", "%cityname%", cityname);
        }

        // Check for city changes
        if (Metropolis.playerInCity.containsKey(playerId)) {
            City fromCity = Metropolis.playerInCity.get(playerId);
            if (CityDatabase.getClaim(to) == null) {
                PlayerExitCityEvent exitCityEvent = new PlayerExitCityEvent(player, fromCity, true);
                Bukkit.getServer().getPluginManager().callEvent(exitCityEvent);
            } else {
                City toCity = CityDatabase.getCityByClaim(to);
                if (!fromCity.equals(toCity)) {
                    PlayerExitCityEvent exitCityEvent = new PlayerExitCityEvent(player, fromCity, false);
                    Bukkit.getServer().getPluginManager().callEvent(exitCityEvent);
                    PlayerEnterCityEvent enterCityEvent = new PlayerEnterCityEvent(player, toCity);
                    Bukkit.getServer().getPluginManager().callEvent(enterCityEvent);
                }
            }
        } else if (CityDatabase.getClaim(to) != null) {
            City toCity = CityDatabase.getCityByClaim(to);
            PlayerEnterCityEvent enterCityEvent = new PlayerEnterCityEvent(player, toCity);
            Bukkit.getServer().getPluginManager().callEvent(enterCityEvent);
        }

        // Check for plot changes
        if (Metropolis.playerInCity.containsKey(playerId)) {
            Plot plot = PlotDatabase.getPlotAtLocation(to);
            District district = CityDatabase.getDistrict(to);
            if (district == null) {
                if (Metropolis.playerInDistrict.containsKey(playerId)) {
                    PlayerExitDistrictEvent exitDistrictEvent = new PlayerExitDistrictEvent(player, Metropolis.playerInDistrict.get(playerId));
                    Metropolis.getInstance().getServer().getPluginManager().callEvent(exitDistrictEvent);
                }
            } else if (!Metropolis.playerInDistrict.containsKey(playerId)) {
                PlayerEnterDistrictEvent enterDistrictEvent = new PlayerEnterDistrictEvent(player, district);
                Metropolis.getInstance().getServer().getPluginManager().callEvent(enterDistrictEvent);
            }
            if (plot == null) {
                if (Metropolis.playerInPlot.containsKey(playerId)) {
                    PlayerExitPlotEvent exitEvent = new PlayerExitPlotEvent(player, Metropolis.playerInPlot.get(playerId));
                    Metropolis.getInstance().getServer().getPluginManager().callEvent(exitEvent);
                }
            } else if (!Metropolis.playerInPlot.containsKey(playerId)) {
                PlayerEnterPlotEvent enterEvent = new PlayerEnterPlotEvent(player, plot);
                Metropolis.getInstance().getServer().getPluginManager().callEvent(enterEvent);
            }
        }
    }

    @EventHandler
    public void onDimension(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        Location to = player.getLocation();
        UUID playerId = player.getUniqueId();

        if(AutoclaimManager.isAutoclaiming(player)) {
            final String cityname = AutoclaimManager.getAutoclaimInfo(player).getCity().getCityName();
            AutoclaimManager.stopAutoclaim(player);
            Metropolis.sendMessage(player, "messages.city.autoclaim.stopped", "%cityname%", cityname);
        }

        // Check for city changes
        if (Metropolis.playerInCity.containsKey(playerId)) {
            City fromCity = Metropolis.playerInCity.get(playerId);
            if (CityDatabase.getClaim(to) == null) {
                PlayerExitCityEvent exitCityEvent = new PlayerExitCityEvent(player, fromCity, true);
                Bukkit.getServer().getPluginManager().callEvent(exitCityEvent);
            } else {
                City toCity = CityDatabase.getCityByClaim(to);
                if (!fromCity.equals(toCity)) {
                    PlayerExitCityEvent exitCityEvent = new PlayerExitCityEvent(player, fromCity, false);
                    Bukkit.getServer().getPluginManager().callEvent(exitCityEvent);
                    PlayerEnterCityEvent enterCityEvent = new PlayerEnterCityEvent(player, toCity);
                    Bukkit.getServer().getPluginManager().callEvent(enterCityEvent);
                }
            }
        } else if (CityDatabase.getClaim(to) != null) {
            City toCity = CityDatabase.getCityByClaim(to);
            PlayerEnterCityEvent enterCityEvent = new PlayerEnterCityEvent(player, toCity);
            Bukkit.getServer().getPluginManager().callEvent(enterCityEvent);
        }

        // Check for plot changes
        if (Metropolis.playerInCity.containsKey(playerId)) {
            Plot plot = PlotDatabase.getPlotAtLocation(to);
            if (plot == null) {
                if (Metropolis.playerInPlot.containsKey(playerId)) {
                    PlayerExitPlotEvent exitEvent = new PlayerExitPlotEvent(player, Metropolis.playerInPlot.get(playerId));
                    Metropolis.getInstance().getServer().getPluginManager().callEvent(exitEvent);
                }
            } else if (!Metropolis.playerInPlot.containsKey(playerId)) {
                PlayerEnterPlotEvent enterEvent = new PlayerEnterPlotEvent(player, plot);
                Metropolis.getInstance().getServer().getPluginManager().callEvent(enterEvent);
            }
        }
    }

    public void coreProtectInteractCheck(Player player, PlayerInteractEvent event) {
        if (getCoreProtect() == null) {
            Bukkit.getLogger().severe("[Metropolis] CoreProtect not found.");
            player.sendMessage("§cSomething went wrong. Please contact an administrator.");
            return;
        }
        if (getCoreProtect().blockLookup(event.getClickedBlock(), 0).isEmpty()) {
            Metropolis.sendMessage(player, "messages.city.blockhistory.noData");
            return;
        }
        int itemsPerPage = 8;
        int start = 0;
        player.sendMessage("");
        Metropolis.sendMessage(
                player,
                "messages.city.blockhistory.header",
                "%location%",
                LocationUtil.formatLocation(event.getClickedBlock().getLocation()),
                "%page%",
                String.valueOf(start + 1),
                "%totalpages%",
                String.valueOf(
                        (int)
                                Math.ceil(
                                        ((double) getCoreProtect().blockLookup(event.getClickedBlock(), 0).size())
                                                / ((double) itemsPerPage))));
        for (int i = start; i < itemsPerPage; i++) {
            if (i >= getCoreProtect().blockLookup(event.getClickedBlock(), 0).size()) {
                break;
            }
            CoreProtectAPI.ParseResult result =
                    getCoreProtect()
                            .parseResult(getCoreProtect().blockLookup(event.getClickedBlock(), 0).get(i));
            String row = "";
            int show = i + 1;
            if (result.getActionId() == 0) {
                row =
                        "§2#"
                                + show
                                + " "
                                + result.getPlayer()
                                + " -- §c"
                                + result.getType().toString().toLowerCase().replace("_", " ")
                                + "§2 -- "
                                + DateUtil.niceDate(result.getTimestamp() / 1000L);
            }
            if (result.getActionId() == 1) {
                row =
                        "§2#"
                                + show
                                + " "
                                + result.getPlayer()
                                + " -- §a"
                                + result.getType().toString().toLowerCase().replace("_", " ")
                                + "§2 -- "
                                + DateUtil.niceDate(result.getTimestamp() / 1000L);
            }
            if (result.getActionId() == 2) {
                row =
                        "§2#"
                                + show
                                + " "
                                + result.getPlayer()
                                + " -- §e"
                                + result.getType().toString().toLowerCase().replace("_", " ")
                                + "§2 -- "
                                + DateUtil.niceDate(result.getTimestamp() / 1000L);
            }
            if (!row.isEmpty()) {
                player.sendMessage(row);
            }
        }
        savedBlockHistory.remove(player.getUniqueId());
        savedBlockHistory.put(
                player.getUniqueId(), getCoreProtect().blockLookup(event.getClickedBlock(), 0));
    }

    public void coreProtectPlaceCheck(Player player, BlockPlaceEvent event) {
        if (getCoreProtect() == null) {
            Bukkit.getLogger().severe("[Metropolis] CoreProtect not found.");
            player.sendMessage("§cSomething went wrong. Please contact an administrator.");
            return;
        }
        if (getCoreProtect().blockLookup(event.getBlockPlaced(), 0).isEmpty()) {
            Metropolis.sendMessage(player, "messages.city.blockhistory.noData");
            return;
        }
        int itemsPerPage = 8;
        int start = 0;
        player.sendMessage("");
        Metropolis.sendMessage(
                player,
                "messages.city.blockhistory.header",
                "%location%",
                LocationUtil.formatLocation(event.getBlockPlaced().getLocation()),
                "%page%",
                String.valueOf(start + 1),
                "%totalpages%",
                String.valueOf(
                        (int)
                                Math.ceil(
                                        ((double) getCoreProtect().blockLookup(event.getBlockPlaced(), 0).size())
                                                / ((double) itemsPerPage))));
        for (int i = start; i < itemsPerPage; i++) {
            if (i >= getCoreProtect().blockLookup(event.getBlockPlaced(), 0).size()) {
                break;
            }
            CoreProtectAPI.ParseResult result =
                    getCoreProtect()
                            .parseResult(getCoreProtect().blockLookup(event.getBlockPlaced(), 0).get(i));
            String row = "";
            int show = i + 1;
            if (result.getActionId() == 0) {
                row =
                        "§2#"
                                + show
                                + " "
                                + result.getPlayer()
                                + " -- §c"
                                + result.getType().toString().toLowerCase().replace("_", " ")
                                + "§2 -- "
                                + DateUtil.niceDate(result.getTimestamp() / 1000L);
            }
            if (result.getActionId() == 1) {
                row =
                        "§2#"
                                + show
                                + " "
                                + result.getPlayer()
                                + " -- §a"
                                + result.getType().toString().toLowerCase().replace("_", " ")
                                + "§2 -- "
                                + DateUtil.niceDate(result.getTimestamp() / 1000L);
            }
            if (result.getActionId() == 2) {
                row =
                        "§2#"
                                + show
                                + " "
                                + result.getPlayer()
                                + " -- §e"
                                + result.getType().toString().toLowerCase().replace("_", " ")
                                + "§2 -- "
                                + DateUtil.niceDate(result.getTimestamp() / 1000L);
            }
            if (!row.isEmpty()) {
                player.sendMessage(row);
            }
        }
        savedBlockHistory.remove(player.getUniqueId());
        savedBlockHistory.put(
                player.getUniqueId(), getCoreProtect().blockLookup(event.getBlockPlaced(), 0));
    }
}
