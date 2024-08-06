package live.supeer.metropolis;

import live.supeer.metropolis.city.City;
import live.supeer.metropolis.city.CityDatabase;
import live.supeer.metropolis.city.Role;
import live.supeer.metropolis.command.CommandCity;
import live.supeer.metropolis.event.PlayerEnterCityEvent;
import live.supeer.metropolis.event.PlayerEnterPlotEvent;
import live.supeer.metropolis.event.PlayerExitCityEvent;
import live.supeer.metropolis.event.PlayerExitPlotEvent;
import live.supeer.metropolis.plot.Plot;
import live.supeer.metropolis.plot.PlotDatabase;
import live.supeer.metropolis.utils.DateUtil;
import live.supeer.metropolis.utils.LocationUtil;
import live.supeer.metropolis.utils.Utilities;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.Point;
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
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.Plugin;

import java.awt.*;
import java.util.*;
import java.util.List;

public class MetropolisListener implements Listener {
    static Metropolis plugin;

    private CoreProtectAPI getCoreProtect() {
        Plugin corePlugin = plugin.getServer().getPluginManager().getPlugin("CoreProtect");

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

    @EventHandler
    public static void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (CityDatabase.getClaim(player.getLocation()) != null) {
            if (CityDatabase.getCity(Objects.requireNonNull(CityDatabase.getClaim(player.getLocation())).getCityName()).isEmpty()) {
                Utilities.sendNatureScoreboard(player);
                playerInCity.remove(player.getUniqueId());
                return;
            }
            City city = CityDatabase.getCity(Objects.requireNonNull(CityDatabase.getClaim(player.getLocation())).getCityName()).get();
            PlayerEnterCityEvent enterCityEvent = new PlayerEnterCityEvent(event.getPlayer(), city);
            Bukkit.getServer().getPluginManager().callEvent(enterCityEvent);
            Plot plot = PlotDatabase.getPlotAtLocation(player.getLocation());
            if (plot != null) {
                PlayerEnterPlotEvent enterPlotEvent = new PlayerEnterPlotEvent(event.getPlayer(), plot);
                Bukkit.getServer().getPluginManager().callEvent(enterPlotEvent);
            }
        } else {
            Utilities.sendNatureScoreboard(player);
            playerInCity.remove(player.getUniqueId());
        }
        String[] list = CityDatabase.memberCityList(player.getUniqueId().toString());
        for (int i = 0; i < Objects.requireNonNull(list).length; i++) {
            if (CityDatabase.getCity(list[i]).isPresent()) {
                City city = CityDatabase.getCity(list[i]).get();
                String cityMotd = city.getMotdMessage();
                if (cityMotd != null) {
                    plugin.sendMessage(
                            player, "messages.city.motd", "%cityname%", city.getCityName(), "%motd%", cityMotd);
                }
            }
        }
    }

    public static HashMap<UUID, Polygon> playerPolygons = new HashMap<>();
    public static HashMap<UUID, Integer> playerYMin = new HashMap<>();
    public static HashMap<UUID, Integer> playerYMax = new HashMap<>();
    public static HashMap<UUID, City> playerInCity = new HashMap<>();
    public static HashMap<UUID, Plot> playerInPlot = new HashMap<>();
    public static HashMap<UUID, List<String[]>> savedBlockHistory = new HashMap<>();

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
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
                plugin.sendMessage(
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
                if (CommandCity.blockEnabled.contains(player)) {
                    event.setCancelled(true);
                    if (CityDatabase.getClaim(event.getClickedBlock().getLocation()) == null || CityDatabase.getCity(Objects.requireNonNull(CityDatabase.getClaim(event.getClickedBlock().getLocation())).getCityName()).isEmpty()) {
                        plugin.sendMessage(player, "messages.error.permissionDenied");
                        return;
                    }
                    City city = CityDatabase.getCity(Objects.requireNonNull(CityDatabase.getClaim(event.getClickedBlock().getLocation())).getCityName()).get();
                    Role role = CityDatabase.getCityRole(city, player.getUniqueId().toString());
                    assert role != null;
                    Plot plot = PlotDatabase.getPlotAtLocation(event.getClickedBlock().getLocation());
                    if (plot != null) {
                        if (plot.getPlotOwnerUUID().equals(player.getUniqueId().toString()) || role.getPermissionLevel() > Role.ASSISTANT.getPermissionLevel()) {
                            coreProtectInteractCheck(player, event);
                            return;
                        } else {
                            plugin.sendMessage(
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
                        plugin.sendMessage(
                                player, "messages.error.city.permissionDenied", "%cityname%", city.getCityName());
                        return;
                    }
                    coreProtectInteractCheck(player, event);
                    return;
                }
                event.setCancelled(true);
                if (savedPlayers.contains(player)) {
                    plugin.sendMessage(player, "messages.city.markings.finished");
                    return;
                }
                if (savedLocs.get(player.getUniqueId()) == null
                        || savedLocs.get(player.getUniqueId()).isEmpty()) {
                    plugin.sendMessage(player, "messages.city.markings.none");
                    return;
                }
                if (!savedLocs.get(player.getUniqueId()).isEmpty()
                        && savedLocs
                        .get(player.getUniqueId())
                        .get(savedLocs.get(player.getUniqueId()).size() - 1)
                        .equals(event.getClickedBlock().getLocation())) {
                    plugin.sendMessage(player, "messages.city.markings.sameBlock");
                    return;
                }
                if (!savedLocs.get(player.getUniqueId()).isEmpty()
                        && !savedLocs
                        .get(player.getUniqueId())
                        .get(0)
                        .getWorld()
                        .equals(event.getClickedBlock().getWorld())) {
                    plugin.sendMessage(player, "messages.city.markings.differentWorlds");
                    return;
                }

                savedLocs.get(player.getUniqueId()).add(event.getClickedBlock().getLocation());
                plugin.sendMessage(
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
                    plugin.sendMessage(player, "messages.city.markings.finish");
                    savedPlayers.add(player);
                }
            }
            }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (event.getBlock().getType().equals(Material.DIRT)) {
            if (CommandCity.blockEnabled.contains(player)) {
                event.setCancelled(true);
                if (CityDatabase.getClaim(event.getBlockPlaced().getLocation()) == null || CityDatabase.getCity(Objects.requireNonNull(CityDatabase.getClaim(event.getBlockPlaced().getLocation())).getCityName()).isEmpty()) {
                    plugin.sendMessage(player, "messages.error.permissionDenied");
                    return;
                }
                City city = CityDatabase.getCity(Objects.requireNonNull(CityDatabase.getClaim(event.getBlockPlaced().getLocation())).getCityName()).get();
                Role role = CityDatabase.getCityRole(city, player.getUniqueId().toString());
                assert role != null;
                Plot plot = PlotDatabase.getPlotAtLocation(event.getBlockPlaced().getLocation());
                if (plot != null) {
                    if (plot.getPlotOwnerUUID().equals(player.getUniqueId().toString()) || role.getPermissionLevel() > Role.ASSISTANT.getPermissionLevel()) {
                        coreProtectPlaceCheck(player, event);
                        return;
                    } else {
                        plugin.sendMessage(
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
                    plugin.sendMessage(
                            player, "messages.error.city.permissionDenied", "%cityname%", city.getCityName());
                    return;
                }
                coreProtectPlaceCheck(player, event);
            }
        }
    }
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        if ((from.getBlockX() >> 4) != (to.getBlockX() >> 4) || (from.getBlockZ() >> 4) != (to.getBlockZ() >> 4)) {
            if (playerInCity.containsKey(event.getPlayer().getUniqueId()) && CityDatabase.getClaim(to) == null) {
                City fromCity = playerInCity.get(event.getPlayer().getUniqueId());
                PlayerExitCityEvent exitCityEvent = new PlayerExitCityEvent(event.getPlayer(), fromCity, true);
                Bukkit.getServer().getPluginManager().callEvent(exitCityEvent);
            } else if (playerInCity.containsKey(event.getPlayer().getUniqueId()) && CityDatabase.getClaim(to) != null && playerInCity.get(event.getPlayer().getUniqueId()) != CityDatabase.getCity(Objects.requireNonNull(CityDatabase.getClaim(to)).getCityName()).get()) {
                City fromCity = playerInCity.get(event.getPlayer().getUniqueId());
                City toCity = CityDatabase.getCity(Objects.requireNonNull(CityDatabase.getClaim(to)).getCityName()).get();
                PlayerExitCityEvent exitCityEvent = new PlayerExitCityEvent(event.getPlayer(), fromCity, false);
                Bukkit.getServer().getPluginManager().callEvent(exitCityEvent);
                PlayerEnterCityEvent enterCityEvent = new PlayerEnterCityEvent(event.getPlayer(), toCity);
                Bukkit.getServer().getPluginManager().callEvent(enterCityEvent);
            } else if (!playerInCity.containsKey(event.getPlayer().getUniqueId()) && CityDatabase.getClaim(to) != null) {
                City toCity = CityDatabase.getCity(Objects.requireNonNull(CityDatabase.getClaim(to)).getCityName()).get();
                PlayerEnterCityEvent enterCityEvent = new PlayerEnterCityEvent(event.getPlayer(), toCity);
                Bukkit.getServer().getPluginManager().callEvent(enterCityEvent);
            }
            return;
        }

        if (event.getFrom().getX() != event.getTo().getX() || event.getFrom().getZ() != event.getTo().getZ() || event.getFrom().getY() != event.getTo().getY()) {
            if (playerInCity.containsKey(event.getPlayer().getUniqueId())) {
                Plot plot = PlotDatabase.getPlotAtLocation(event.getTo());
                if (plot == null) {
                    if (playerInPlot.containsKey(event.getPlayer().getUniqueId())) {
                        PlayerExitPlotEvent exitEvent = new PlayerExitPlotEvent(event.getPlayer(), playerInPlot.get(event.getPlayer().getUniqueId()));
                        plugin.getServer().getPluginManager().callEvent(exitEvent);
                    }
                    return;
                }
                if (!playerInPlot.containsKey(event.getPlayer().getUniqueId())) {
                    PlayerEnterPlotEvent enterEvent = new PlayerEnterPlotEvent(event.getPlayer(), plot);
                    plugin.getServer().getPluginManager().callEvent(enterEvent);
                }
            }
        }
    }

    @EventHandler
    public void onDimension(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        Location to = player.getLocation();
        UUID playerId = player.getUniqueId();

        // Check for city changes
        if (playerInCity.containsKey(playerId)) {
            City fromCity = playerInCity.get(playerId);
            if (CityDatabase.getClaim(to) == null) {
                PlayerExitCityEvent exitCityEvent = new PlayerExitCityEvent(player, fromCity, true);
                Bukkit.getServer().getPluginManager().callEvent(exitCityEvent);
            } else {
                City toCity = CityDatabase.getCity(Objects.requireNonNull(CityDatabase.getClaim(to)).getCityName()).get();
                if (!fromCity.equals(toCity)) {
                    PlayerExitCityEvent exitCityEvent = new PlayerExitCityEvent(player, fromCity, false);
                    Bukkit.getServer().getPluginManager().callEvent(exitCityEvent);
                    PlayerEnterCityEvent enterCityEvent = new PlayerEnterCityEvent(player, toCity);
                    Bukkit.getServer().getPluginManager().callEvent(enterCityEvent);
                }
            }
        } else if (CityDatabase.getClaim(to) != null) {
            City toCity = CityDatabase.getCity(Objects.requireNonNull(CityDatabase.getClaim(to)).getCityName()).get();
            PlayerEnterCityEvent enterCityEvent = new PlayerEnterCityEvent(player, toCity);
            Bukkit.getServer().getPluginManager().callEvent(enterCityEvent);
        }

        // Check for plot changes
        if (playerInCity.containsKey(playerId)) {
            Plot plot = PlotDatabase.getPlotAtLocation(to);
            if (plot == null) {
                if (playerInPlot.containsKey(playerId)) {
                    PlayerExitPlotEvent exitEvent = new PlayerExitPlotEvent(player, playerInPlot.get(playerId));
                    plugin.getServer().getPluginManager().callEvent(exitEvent);
                }
            } else if (!playerInPlot.containsKey(playerId)) {
                PlayerEnterPlotEvent enterEvent = new PlayerEnterPlotEvent(player, plot);
                plugin.getServer().getPluginManager().callEvent(enterEvent);
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
            plugin.sendMessage(player, "messages.city.blockhistory.noData");
            return;
        }
        int itemsPerPage = 8;
        int start = 0;
        player.sendMessage("");
        plugin.sendMessage(
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
            plugin.sendMessage(player, "messages.city.blockhistory.noData");
            return;
        }
        int itemsPerPage = 8;
        int start = 0;
        player.sendMessage("");
        plugin.sendMessage(
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
