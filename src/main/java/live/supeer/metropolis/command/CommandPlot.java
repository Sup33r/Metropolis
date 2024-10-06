package live.supeer.metropolis.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import co.aikar.commands.annotation.Optional;
import live.supeer.apied.ApiedAPI;
import live.supeer.apied.MPlayer;
import live.supeer.metropolis.*;
import live.supeer.metropolis.city.Role;
import live.supeer.metropolis.event.PlayerEnterPlotEvent;
import live.supeer.metropolis.utils.DateUtil;
import live.supeer.metropolis.utils.LocationUtil;
import live.supeer.metropolis.utils.Utilities;
import live.supeer.metropolis.city.City;
import live.supeer.metropolis.city.CityDatabase;
import live.supeer.metropolis.homecity.HCDatabase;
import live.supeer.metropolis.plot.Plot;
import live.supeer.metropolis.plot.PlotDatabase;
import live.supeer.metropolis.plot.PlotPerms;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;

@CommandAlias("plot")
public class CommandPlot extends BaseCommand {
    private static final GeometryFactory geometryFactory = new GeometryFactory();

    @Default
    @CatchUnknown
    public static void onPlot(Player player) {
        if (!player.hasPermission("metropolis.plot")) {
            Metropolis.sendAccessDenied(player);
            return;
        }
        Metropolis.sendMessage(player, "messages.plot.help.header");
        Metropolis.sendMessage(player, "messages.plot.help.buy");
        Metropolis.sendMessage(player, "messages.plot.help.leave");
        Metropolis.sendMessage(player, "messages.plot.help.market");
        Metropolis.sendMessage(player, "messages.plot.help.perm");
        Metropolis.sendMessage(player, "messages.plot.help.set.rent");
        Metropolis.sendMessage(player, "messages.plot.help.set.name");
        Metropolis.sendMessage(player, "messages.plot.help.set.type");
        Metropolis.sendMessage(player, "messages.plot.help.set.owner");
        Metropolis.sendMessage(player, "messages.plot.help.share");
        Metropolis.sendMessage(player, "messages.plot.help.toggle.animals");
        Metropolis.sendMessage(player, "messages.plot.help.toggle.mobs");
        Metropolis.sendMessage(player, "messages.plot.help.toggle.meeting");
        Metropolis.sendMessage(player, "messages.plot.help.toggle.k");
        Metropolis.sendMessage(player, "messages.plot.help.type.church");
        Metropolis.sendMessage(player, "messages.plot.help.type.farm");
        Metropolis.sendMessage(player, "messages.plot.help.type.shop");
        Metropolis.sendMessage(player, "messages.plot.help.type.vacation");
        Metropolis.sendMessage(player, "messages.plot.help.type.none");
        Metropolis.sendMessage(player, "messages.plot.help.update");
    }

    @Subcommand("new")
    public static void onNew(Player player, @Optional String plotname) {
        City city = Utilities.hasCityPermissions(player, "metropolis.plot.new", Role.ASSISTANT);
        if (city == null) {
            return;
        }
        if (city.isReserve()) {
            Metropolis.sendMessage(player, "messages.error.city.reserve");
            return;
        }
        if (!MetropolisListener.playerPolygons.containsKey(player.getUniqueId())) {
            Metropolis.sendMessage(player, "messages.error.missing.plot");
            return;
        }
        Polygon regionPolygon = MetropolisListener.playerPolygons.get(player.getUniqueId());
        Location[] locations = MetropolisListener.savedLocs.get(player.getUniqueId()).toArray(new Location[0]);
        double minX = regionPolygon.getEnvelopeInternal().getMinX();
        double maxX = regionPolygon.getEnvelopeInternal().getMaxX();
        double minY = regionPolygon.getEnvelopeInternal().getMinY();
        double maxY = regionPolygon.getEnvelopeInternal().getMaxY();
        player.sendMessage("minX: " + minX + " | maxX: " + maxX + " | minY: " + minY + " | maxY: " + maxY);

        if (maxX - minX < 3 || maxY - minY < 3) {
            Metropolis.sendMessage(player, "messages.error.plot.tooSmall");
            return;
        }
        if (!MetropolisListener.playerYMin.containsKey(player.getUniqueId()) || !MetropolisListener.playerYMax.containsKey(player.getUniqueId())) {
            Metropolis.sendMessage(player, "messages.error.plot.tooLowY");
            return;
        }
        int yMin = MetropolisListener.playerYMin.get(player.getUniqueId());
        int yMax = MetropolisListener.playerYMax.get(player.getUniqueId());
        player.sendMessage(String.valueOf(yMin));
        player.sendMessage(String.valueOf(yMax));
        if ((yMax - yMin) < 3) {
            Metropolis.sendMessage(player, "messages.error.plot.tooLowY");
            return;
        }

        int chunkSize = 16;
        int startX = (int) Math.floor(minX / chunkSize) * chunkSize;
        int endX = (int) Math.floor(maxX / chunkSize) * chunkSize + chunkSize;
        int startY = (int) Math.floor(minY / chunkSize) * chunkSize;
        int endY = (int) Math.floor(maxY / chunkSize) * chunkSize + chunkSize;
        for (int x = startX; x < endX; x += chunkSize) {
            for (int z = startY; z < endY; z += chunkSize) {
                Polygon chunkPolygon = geometryFactory.createPolygon(new Coordinate[]{
                        new Coordinate(x, z),
                        new Coordinate(x + chunkSize, z),
                        new Coordinate(x + chunkSize, z + chunkSize),
                        new Coordinate(x, z + chunkSize),
                        new Coordinate(x, z)
                });
                if (regionPolygon.intersects(chunkPolygon)) {
                    if (CityDatabase.getClaim(new Location(player.getWorld(), x, 0, z)) == null || !Objects.equals(Objects.requireNonNull(CityDatabase.getClaim(new Location(player.getWorld(), x, 0, z))).getCity(), HCDatabase.getHomeCityToCity(player.getUniqueId().toString()))) {
                        Metropolis.sendMessage(player, "messages.error.plot.intersectsExistingClaim");
                        return;
                    }
                    if (PlotDatabase.intersectsExistingPlot(
                            regionPolygon,
                            yMin,
                            yMax,
                            city, player.getWorld())) {
                        Metropolis.sendMessage(player, "messages.error.plot.intersectsExistingPlot");
                        return;
                    }
                    Plot plot =
                            PlotDatabase.createPlot(
                                    player,
                                    regionPolygon,
                                    plotname,
                                    city,
                                    yMin,
                                    yMax,
                                    player.getWorld());
                    assert plot != null;
                    if (regionPolygon.contains(geometryFactory.createPoint(new Coordinate(player.getLocation().getX(), player.getLocation().getZ())))) {
                        Metropolis.playerInPlot.put(player.getUniqueId(), plot);
                    }
                    Utilities.sendScoreboard(player);
                    Database.addLogEntry(
                            city,
                            "{ \"type\": \"create\", \"subtype\": \"plot\", \"id\": "
                                    + plot.getPlotId()
                                    + ", \"name\": "
                                    + plotname
                                    + ", \"points\": "
                                    + LocationUtil.parsePoints(locations)
                                    + ", \"ymin\": "
                                    + yMin
                                    + ", \"ymax\": "
                                    + yMax
                                    + ", \"player\": "
                                    + player.getUniqueId()
                                    + " }");
                    MetropolisListener.savedLocs.remove(player.getUniqueId());
                    MetropolisListener.playerPolygons.remove(player.getUniqueId());
                    MetropolisListener.playerYMin.remove(player.getUniqueId());
                    MetropolisListener.playerYMax.remove(player.getUniqueId());
                    Metropolis.sendMessage(
                            player,
                            "messages.city.successful.set.plot.new",
                            "%cityname%",
                            city.getCityName(),
                            "%plotname%",
                            plot.getPlotName());
                }
            }
        }
    }

    @Subcommand("expand")
    public static void onExpand(Player player, String[] args) {
        if (!player.hasPermission("metropolis.plot.expand")) {
            Metropolis.sendAccessDenied(player);
            return;
        }
        if (!MetropolisListener.playerPolygons.containsKey(player.getUniqueId())) {
            Metropolis.sendMessage(player, "messages.error.missing.plot");
            return;
        }

        if (args.length == 0) {
            MetropolisListener.playerYMin.put(player.getUniqueId(), -64);
            MetropolisListener.playerYMax.put(player.getUniqueId(), 319);
            Metropolis.sendMessage(player, "messages.plot.set.plot.expand.max");
            return;
        }
        if (args.length > 2) {
            Metropolis.sendMessage(player, "messages.syntax.plot.expand");
            return;
        }
        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("up")) {
                MetropolisListener.playerYMax.put(player.getUniqueId(), 319);
                Metropolis.sendMessage(player, "messages.plot.set.plot.expand.up.max");
                return;
            } else if (args[0].equalsIgnoreCase("down")) {
                MetropolisListener.playerYMin.put(player.getUniqueId(), -64);
                Metropolis.sendMessage(player, "messages.plot.set.plot.expand.down.max");
                return;
            } else {
                Metropolis.sendMessage(player, "messages.syntax.plot.expand");
                return;
            }
        }
        if (args[0].matches("[0-9]+")) {
            if (Integer.parseInt(args[0]) == 0) {
                Metropolis.sendMessage(player, "messages.error.plot.expand.invalidHeight");
                return;
            }
            if (args[1].equalsIgnoreCase("up")) {
                if (MetropolisListener.playerYMax.get(player.getUniqueId()) + Integer.parseInt(args[0])
                        > 319) {
                    Metropolis.sendMessage(player, "messages.error.plot.tooHighExpand");
                    return;
                }
                MetropolisListener.playerYMax.put(
                        player.getUniqueId(),
                        MetropolisListener.playerYMax.get(player.getUniqueId()) + Integer.parseInt(args[0]));
                Metropolis.sendMessage(player, "messages.plot.set.plot.expand.up.amount", "%amount%", args[0]);
            } else if (args[1].equalsIgnoreCase("down")) {
                if (MetropolisListener.playerYMin.get(player.getUniqueId()) - Integer.parseInt(args[0])
                        < -64) {
                    Metropolis.sendMessage(player, "messages.error.plot.tooLowExpand");
                    return;
                }
                MetropolisListener.playerYMin.put(
                        player.getUniqueId(),
                        MetropolisListener.playerYMin.get(player.getUniqueId()) - Integer.parseInt(args[0]));
                Metropolis.sendMessage(
                        player, "messages.plot.set.plot.expand.down.amount", "%amount%", args[0]);
            } else {
                Metropolis.sendMessage(player, "messages.syntax.plot.expand");
            }
        } else {
            Metropolis.sendMessage(player, "messages.syntax.plot.expand");
        }
    }

    @Subcommand("delete")
    public static void onDelete(Player player) {
        Plot plot = Utilities.hasPlotPermissions(player, "metropolis.plot.delete", Role.ASSISTANT, false);
        if (plot == null) {
            return;
        }
        if (plot.getCity().isReserve()) {
            Metropolis.sendMessage(player, "messages.error.city.reserve");
            return;
        }
        Database.addLogEntry(plot.getCity(),
                "{ \"type\": \"delete\", \"subtype\": \"plot\", \"id\": "
                        + plot.getPlotId()
                        + ", \"name\": "
                        + plot.getPlotName()
                        + ", \"player\": "
                        + player.getUniqueId()
                        + " }");
        PlotDatabase.deletePlot(plot);
        plot.getCity().removeCityPlot(plot);
        for (Player plotPlayer : plot.playersInPlot()) {
            Utilities.sendCityScoreboard(plotPlayer, plot.getCity(), plot);
        }
        Metropolis.sendMessage(
                player,
                "messages.city.successful.set.delete.plot",
                "%cityname%",
                plot.getCity().getCityName(),
                "%plotname%",
                plot.getPlotName());
    }

    @Subcommand("leave")
    public static void onLeave(Player player) {
        Plot plot = Utilities.hasPlotPermissions(player, "metropolis.plot.leave", null, true);
        if (plot == null) {
            return;
        }
        if (plot.getCity().isReserve()) {
            Metropolis.sendMessage(player, "messages.error.city.reserve");
            return;
        }
        plot.removePlotOwner();
        plot.setPlotRent(0);
        plot.setForSale(false);
        plot.setPlotPrice(0);
        Database.addLogEntry(
                plot.getCity(),
                "{ \"type\": \"leave\", \"subtype\": \"plot\", \"id\": "
                        + plot.getPlotId()
                        + ", \"name\": "
                        + plot.getPlotName()
                        + ", \"player\": "
                        + player.getUniqueId()
                        + " }");
        Metropolis.sendMessage(
                player,
                "messages.city.successful.set.plot.leave",
                "%cityname%",
                plot.getCity().getCityName(),
                "%plotname%",
                plot.getPlotName());
        for (Player plotPlayer : plot.playersInPlot()) {
            Utilities.sendCityScoreboard(plotPlayer, plot.getCity(), plot);
        }
    }

    @Subcommand("market")
    public static void onMarket(Player player, String arg) {
        Plot plot = Utilities.hasPlotPermissions(player, "metropolis.plot.market", Role.INVITER, true);
        if (plot == null) {
            return;
        }
        if (plot.getCity().isReserve()) {
            Metropolis.sendMessage(player, "messages.error.city.reserve");
            return;
        }
        if (arg.equals("-")) {
            plot.setForSale(false);
            for (Player plotPlayer : plot.playersInPlot()) {
                Utilities.sendCityScoreboard(plotPlayer, plot.getCity(), plot);
            }
            Metropolis.sendMessage(player, "messages.city.successful.set.plot.market.remove", "%cityname%", plot.getCity().getCityName(), "%plotname%", plot.getPlotName());
            Database.addLogEntry(plot.getCity(),
                    "{ \"type\": \"plotMarket\", \"subtype\": \"remove\", \"id\": "
                            + plot.getPlotId()
                            + ", \"name\": "
                            + plot.getPlotName()
                            + ", \"player\": "
                            + player.getUniqueId()
                            + " }");
            return;
        }
        if (arg.matches("[0-9]+")) {
            if (plot.isForSale()) {
                if (plot.getPlotPrice() == Integer.parseInt(arg)) {
                    Metropolis.sendMessage(player, "messages.error.plot.market.noChange", "%cityname%", plot.getCity().getCityName());
                    return;
                }
                Metropolis.sendMessage(player, "messages.city.successful.set.plot.market.change", "%cityname%", plot.getCity().getCityName(), "%plotname%", plot.getPlotName(), "%from%", Utilities.formattedMoney(plot.getPlotPrice()), "%to%", Utilities.formattedMoney(Integer.valueOf(arg)));
                Database.addLogEntry(
                        plot.getCity(),
                        "{ \"type\": \"plotMarket\", \"subtype\": \"change\", \"id\": "
                                + plot.getPlotId()
                                + ", \"name\": "
                                + plot.getPlotName()
                                + ", \"from\": "
                                + plot.getPlotPrice()
                                + ", \"to\": "
                                + arg
                                + ", \"player\": "
                                + player.getUniqueId()
                                + " }");
                plot.setPlotPrice(Integer.parseInt(arg));
                for (Player plotPlayer : plot.playersInPlot()) {
                    Utilities.sendCityScoreboard(plotPlayer, plot.getCity(), plot);
                }
                return;
            }
            plot.setForSale(true);
            if (plot.getPlotPrice() == Integer.parseInt(arg)) {
                Metropolis.sendMessage(player, "messages.error.plot.market.noChange", "%cityname%", plot.getCity().getCityName());
                return;
            }
            plot.setPlotPrice(Integer.parseInt(arg));
            for (Player plotPlayer : plot.playersInPlot()) {
                Utilities.sendCityScoreboard(plotPlayer, plot.getCity(), plot);
            }
            Database.addLogEntry(
                    plot.getCity(),
                    "{ \"type\": \"plotMarket\", \"subtype\": \"add\", \"id\": "
                            + plot.getPlotId()
                            + ", \"name\": "
                            + plot.getPlotName()
                            + ", \"to\": "
                            + arg
                            + ", \"player\": "
                            + player.getUniqueId()
                            + " }");
            Metropolis.sendMessage(player, "messages.city.successful.set.plot.market.set", "%cityname%", plot.getCity().getCityName(), "%plotname%", plot.getPlotName(), "%amount%", Utilities.formattedMoney(Integer.valueOf(arg)));
            return;
        }
        Metropolis.sendMessage(player, "messages.syntax.plot.market");
    }

    @Subcommand("info")
    public static void onInfo(Player player) {
        if (!player.hasPermission("metropolis.plot.info")) {
            Metropolis.sendAccessDenied(player);
            return;
        }
        sendInfo(player);
    }

    public static void sendInfo(Player player) {
        Plot plot = Metropolis.playerInPlot.get(player.getUniqueId());
        if (plot == null) {
            Metropolis.sendMessage(player, "messages.error.plot.notInPlot");
            return;
        }
        Role role = CityDatabase.getCityRole(plot.getCity(), player.getUniqueId().toString());
        Metropolis.sendMessage(player, "messages.plot.list.header", "%plot%", plot.getPlotName());
        Metropolis.sendMessage(
                player, "messages.plot.list.id", "%id%", String.valueOf(plot.getPlotId()));
        Metropolis.sendMessage(player, "messages.plot.list.city", "%cityname%", plot.getCity().getCityName());
        if (plot.getPlotOwnerUUID() != null) {
            Metropolis.sendMessage(player, "messages.plot.list.owner", "%owner%", ApiedAPI.getPlayer(UUID.fromString(plot.getPlotOwnerUUID())).getName());
        }
        if (plot.getPlotRent() > 0) {
            Metropolis.sendMessage(player, "messages.plot.list.rent", "%rent%", Utilities.formattedMoney(plot.getPlotRent()));
        }
        if (plot.hasFlag('p')) {
            Metropolis.sendMessage(player, "messages.plot.list.pvp", "%status%", "<red>" + Metropolis.getRawMessage("messages.words.on_state"));
        } else {
            Metropolis.sendMessage(player, "messages.plot.list.pvp", "%status%", "<green>" + Metropolis.getRawMessage("messages.words.off_state"));
        }
        if (plot.hasFlag('a')) {
            Metropolis.sendMessage(player, "messages.plot.list.animals", "%status%", "<green>" + Metropolis.getRawMessage("messages.words.on_state"));
        } else {
            Metropolis.sendMessage(player, "messages.plot.list.animals", "%status%", "<green>" + Metropolis.getRawMessage("messages.words.off_state"));
        }
        if (plot.hasFlag('m')) {
            Metropolis.sendMessage(player, "messages.plot.list.monsters", "%status%", "<red>" + Metropolis.getRawMessage("messages.words.on_state"));
        } else {
            Metropolis.sendMessage(player, "messages.plot.list.monsters", "%status%", "<green>" + Metropolis.getRawMessage("messages.words.off_state"));
        }
        if (plot.hasFlag('l')) {
            Metropolis.sendMessage(player, "messages.plot.list.locked", "%status%", "<green>" + Metropolis.getRawMessage("messages.words.yes_word"));
        } else {
            Metropolis.sendMessage(player, "messages.plot.list.locked", "%status%", "<green>" + Metropolis.getRawMessage("messages.words.no_word"));
        }
        if (plot.isKMarked()) {
            Metropolis.sendMessage(player, "messages.plot.list.k-marked", "%status%","<green>" + Metropolis.getRawMessage("messages.words.yes_word"));
        } else {
            Metropolis.sendMessage(player, "messages.plot.list.k-marked", "%status%","<green>" + Metropolis.getRawMessage("messages.words.no_word"));
        }
        if (!plot.hasFlag('i')) {
            Metropolis.sendMessage(player, "messages.plot.list.lose.items", "%status%","<red>" + Metropolis.getRawMessage("messages.words.yes_word"));
        } else {
            Metropolis.sendMessage(player, "messages.plot.list.lose.items", "%status%","<green>" + Metropolis.getRawMessage("messages.words.no_word"));
        }
        if (!plot.hasFlag('x')) {
            Metropolis.sendMessage(player, "messages.plot.list.lose.xp", "%status%","<red>" + Metropolis.getRawMessage("messages.words.yes_word"));
        } else {
            Metropolis.sendMessage(player, "messages.plot.list.lose.xp", "%status%", "<green>" + Metropolis.getRawMessage("messages.words.no_word"));
        }
        if (player.hasPermission("metropolis.plot.info.coordinates")) {
            Metropolis.sendMessage(
                    player,
                    "messages.plot.list.middle",
                    "%world%",
                    plot.getPlotCenter().getWorld().getName(),
                    "%x%",
                    String.valueOf(plot.getPlotCenter().getBlockX()),
                    "%y%",
                    String.valueOf(plot.getPlotCenter().getBlockY()),
                    "%z%",
                    String.valueOf(plot.getPlotCenter().getBlockZ()));
        }
        if (!player.hasPermission("metropolis.plot.info.coordinates") && role != null && role.getPermissionLevel() >= Role.ASSISTANT.getPermissionLevel()) {
            Metropolis.sendMessage(
                    player,
                    "messages.plot.list.middle",
                    "%world%",
                    plot.getPlotCenter().getWorld().getName(),
                    "%x%",
                    String.valueOf(plot.getPlotCenter().getBlockX()),
                    "%y%",
                    String.valueOf(plot.getPlotCenter().getBlockY()),
                    "%z%",
                    String.valueOf(plot.getPlotCenter().getBlockZ()));
        }
        List<Player> players = plot.playersInPlot();
        if (!players.isEmpty()) {
            StringBuilder stringBuilder = new StringBuilder();
            for (Player p : players) {
                // dont append last player
                if (players.indexOf(p) == players.size() - 1)
                    stringBuilder.append(p.getName()).append("<dark_green>  ");
                else stringBuilder.append(p.getName()).append("<dark_green>,<green> ");
            }
            Metropolis.sendMessage(
                    player,
                    "messages.plot.list.players",
                    "%players%",
                    stringBuilder.substring(0, stringBuilder.toString().length() - 2));
        }
    }

    @Subcommand("player")
    @CommandCompletion("@players")
    public static void onPlayer(Player player, String playerName) {
        if (!player.hasPermission("metropolis.plot.player")) {
            Metropolis.sendAccessDenied(player);
            return;
        }

        MPlayer mPlayer = ApiedAPI.getPlayer(playerName);
        if (mPlayer == null) {
            Metropolis.sendMessage(player, "messages.error.player.notFound");
            return;
        }

        List<City> memberCities = CityDatabase.memberCityList(mPlayer.getUuid().toString());
        if (memberCities == null || memberCities.isEmpty()) {
            Metropolis.sendMessage(player, "messages.error.city.notInCity");
            return;
        }

        for (City city : memberCities) {
            StringBuilder stringBuilder = new StringBuilder();
            for (Plot plot : city.getCityPlots()) {
                if (plot.getPlotOwnerUUID().equals(mPlayer.getUuid().toString())) {
                    stringBuilder.append("§a").append(plot.getPlotName()).append("§2,§a ");
                }
            }

            if (!stringBuilder.toString().isEmpty()) {
                player.sendMessage(
                        "§a§l" + city.getCityName() + "§2: §a" +
                                stringBuilder.substring(0, stringBuilder.length() - 4) // Remove trailing ", "
                );
            }
        }
    }

    @Subcommand("tp")
    public static void onTp(Player player, String plotID) {
        if (!player.hasPermission("metropolis.plot.tp")) {
            Metropolis.sendAccessDenied(player);
            return;
        }
        // For the eventual time when i will add the syntax stuff
        // plugin.sendMessage(player, "messages.syntax.plot.tp");
        if (!plotID.matches("[0-9]+")) {
            Metropolis.sendMessage(player, "messages.syntax.plot.tp");
            return;
        }
        if (!PlotDatabase.plotExists(Integer.parseInt(plotID))) {
            Metropolis.sendMessage(player, "messages.error.plot.notFound");
            return;
        }
        Plot plot = PlotDatabase.getPlot(Integer.parseInt(plotID));
        assert plot != null;
        if (CityDatabase.getCity(plot.getCityId()).isEmpty()) {
            Metropolis.sendMessage(player, "messages.error.city.missing.city");
            return;
        }
        City city = CityDatabase.getCity(plot.getCityId()).get();
        Metropolis.sendMessage(
                player,
                "messages.city.successful.set.plot.tp",
                "%plotname%",
                plot.getPlotName(),
                "%cityname%",
                city.getCityName());
        player.teleport(plot.getPlotCenter());
        PlayerEnterPlotEvent enterPlotEvent = new PlayerEnterPlotEvent(player, plot);
        Bukkit.getServer().getPluginManager().callEvent(enterPlotEvent);
    }

    @Subcommand("share")
    @CommandCompletion("@players")
    public static void onShare(Player player, String sharePlayer) {
        Plot plot = Utilities.hasPlotPermissions(player, "metropolis.plot.share", Role.ASSISTANT, true);
        if (plot == null) {
            return;
        }
        if (plot.getCity().isReserve()) {
            Metropolis.sendMessage(player, "messages.error.city.reserve");
            return;
        }
        MPlayer mPlayer = ApiedAPI.getPlayer(sharePlayer);
        if (mPlayer == null) {
            Metropolis.sendMessage(player, "messages.error.player.notFound");
            return;
        }

        PlotPerms perms = plot.getPlayerPlotPerm(mPlayer.getUuid().toString());
        if (perms == null || perms.getPerms().length == 0) {
            String newPerms = Utilities.parsePermChange(null, "+*", player, "plot");
            if (newPerms == null) {
                return;
            }
            Database.addLogEntry(
                    plot.getCity(),
                    "{ \"type\": \"plot\", \"subtype\": \"perm\", \"id\": "
                            + plot.getPlotId()
                            + ", \"name\": "
                            + plot.getPlotName()
                            + ", \"type\": "
                            + "player"
                            + ", \"from\": "
                            + "null"
                            + ", \"to\": "
                            + newPerms
                            + ", \"issuer\": "
                            + player.getUniqueId()
                            + ", \"player\": "
                            + mPlayer.getUuid()
                            + " }");
            plot.setPlotPerms("players", newPerms, mPlayer.getUuid().toString());
            Metropolis.sendMessage(player,"messages.plot.share.success", "%cityname%", plot.getCity().getCityName(), "%player%", mPlayer.getName());
        } else {
            String newPerms = Utilities.parsePermChange(perms.getPerms(), "-*", player, "plot");
            if (newPerms == null) {
                return;
            }
            Database.addLogEntry(
                    plot.getCity(),
                    "{ \"type\": \"plot\", \"subtype\": \"perm\", \"id\": "
                            + plot.getPlotId()
                            + ", \"name\": "
                            + plot.getPlotName()
                            + ", \"type\": "
                            + "player"
                            + ", \"from\": "
                            + Arrays.toString(perms.getPerms())
                            + ", \"to\": "
                            + newPerms
                            + ", \"issuer\": "
                            + player.getUniqueId()
                            + ", \"player\": "
                            + mPlayer.getUuid()
                            + " }");
            plot.setPlotPerms("players", newPerms, mPlayer.getUuid().toString());
            Metropolis.sendMessage(player,"messages.plot.share.remove", "%cityname%", plot.getCity().getCityName(), "%player%", mPlayer.getName());
        }
    }

    @Subcommand("perm")
    public static void onPerm(Player player, @Optional String subject, @Optional String perm) {
        City city = Utilities.hasCityPermissions(player, "metropolis.plot.perm", null);
        if (city == null) {
            return;
        }
        Plot plot = Metropolis.playerInPlot.get(player.getUniqueId());
        if (plot == null) {
            Metropolis.sendMessage(player, "messages.error.plot.notInPlot");
            return;
        }
        if (city.isReserve()) {
            Metropolis.sendMessage(player, "messages.error.city.reserve");
            return;
        }
        Role role = CityDatabase.getCityRole(city, player.getUniqueId().toString());
        if (subject == null) {
            String permsOutsiders = Utilities.formatPerms(plot.getPermsOutsiders());
            String permsMembers = Utilities.formatPerms(plot.getPermsMembers());
            Metropolis.sendMessage(
                    player, "messages.plot.list.perm.header", "%plot%", plot.getPlotName());
            Metropolis.sendMessage(
                    player, "messages.plot.list.perm.outsiders", "%perms%", permsOutsiders);
            Metropolis.sendMessage(player, "messages.plot.list.perm.members", "%perms%", permsMembers);

            for (PlotPerms plotPerms : plot.getPlotPerms()) {
                StringBuilder stringBuilder = new StringBuilder();
                for (char s : plotPerms.getPerms()) {
                    stringBuilder.append(s);
                }
                String perms = "+" + stringBuilder.substring(0, stringBuilder.toString().length());
                if (plotPerms.getPerms() == null || plotPerms.getPerms().length == 0) {
                    return;
                }
                Metropolis.sendMessage(
                        player,
                        "messages.plot.list.perm.players",
                        "%player%",
                        ApiedAPI.getPlayer(UUID.fromString(plotPerms.getPlayerUUID())).getName(),
                        "%perms%",
                        perms);
            }
            Metropolis.sendMessage(player, "messages.plot.list.perm.permsrow.1");
            Metropolis.sendMessage(player, "messages.plot.list.perm.permsrow.2");
            Metropolis.sendMessage(player, "messages.plot.list.perm.permsrow.3");
            return;
        }

        if (perm != null && perm.contains(" ")) {
            Metropolis.sendMessage(player, "messages.syntax.plot.perm");
            return;
        }

        if (subject.equals("-") && perm == null) {
            if (plot.getPlotOwnerUUID().equals(player.getUniqueId().toString()) || role != null && role.hasPermission(Role.ASSISTANT)) {
                if (plot.isKMarked() && !Objects.equals(role, Role.MAYOR)) {
                    Metropolis.sendMessage(
                            player,
                            "messages.error.city.permissionDenied",
                            "%cityname%",
                            city.getCityName());
                    return;
                }
                Database.addLogEntry(
                        city,
                        "{ \"type\": \"plot\", \"subtype\": \"perm\", \"id\": "
                                + plot.getPlotId()
                                + ", \"name\": "
                                + plot.getPlotName()
                                + ", \"type\": "
                                + "all"
                                + ", \"from\": "
                                + Arrays.toString(plot.getPermsMembers())
                                + ", \"to\": "
                                + "null"
                                + ", \"player\": "
                                + player.getUniqueId()
                                + " }");
                plot.removePlotPerms();
                Metropolis.sendMessage(
                        player,
                        "messages.city.successful.set.plot.perm.remove.all",
                        "%cityname%",
                        city.getCityName());
            } else {
                Metropolis.sendMessage(
                        player, "messages.error.city.permissionDenied", "%cityname%", city.getCityName());
            }
            return;
        }

        if (perm == null) {
            Metropolis.sendMessage(player, "messages.syntax.plot.perm");
            return;
        }

        if (!perm.startsWith("+") && !perm.startsWith("-")) {
            Metropolis.sendMessage(player, "messages.syntax.plot.perm");
            return;
        }

        if (plot.getPlotOwnerUUID().equals(player.getUniqueId().toString()) || role != null && role.hasPermission(Role.ASSISTANT)) {
            if (plot.isKMarked() && !Objects.equals(role, Role.MAYOR)) {
                Metropolis.sendMessage(
                        player,
                        "messages.error.city.permissionDenied",
                        "%cityname%",
                        city.getCityName());
                return;
            }
            if (subject.equalsIgnoreCase("members")) {
                if (Utilities.parsePermChange(plot.getPermsMembers(), perm, player, "plot") == null) {
                    return;
                }
                String perms = Utilities.parsePermChange(plot.getPermsMembers(), perm, player, "plot");
                if (perms == null) {
                    return;
                }
                Database.addLogEntry(
                        city,
                        "{ \"type\": \"plot\", \"subtype\": \"perm\", \"id\": "
                                + plot.getPlotId()
                                + ", \"name\": "
                                + plot.getPlotName()
                                + ", \"type\": "
                                + "members"
                                + ", \"from\": "
                                + Arrays.toString(plot.getPermsMembers())
                                + ", \"to\": "
                                + Utilities.parsePermChange(plot.getPermsMembers(), perm, player, "plot")
                                + ", \"issuer\": "
                                + player.getUniqueId()
                                + " }");
                plot.setPlotPerms("members", perms, null);
                Metropolis.sendMessage(
                        player,
                        "messages.city.successful.set.plot.perm.change.members",
                        "%perms%",
                        new String(plot.getPermsMembers()),
                        "%cityname%",
                        city.getCityName());
                return;
            }

            if (subject.equalsIgnoreCase("outsiders")) {
                if (Utilities.parsePermChange(plot.getPermsOutsiders(), perm, player, "plot") == null) {
                    return;
                }

                String perms = Utilities.parsePermChange(plot.getPermsOutsiders(), perm, player, "plot");
                if (perms == null) {
                    return;
                }
                Database.addLogEntry(
                        city,
                        "{ \"type\": \"plot\", \"subtype\": \"perm\", \"id\": "
                                + plot.getPlotId()
                                + ", \"name\": "
                                + plot.getPlotName()
                                + ", \"type\": "
                                + "outsiders"
                                + ", \"from\": "
                                + Arrays.toString(plot.getPermsOutsiders())
                                + ", \"to\": "
                                + Utilities.parsePermChange(plot.getPermsOutsiders(), perm, player, "plot")
                                + ", \"issuer\": "
                                + player.getUniqueId()
                                + " }");
                plot.setPlotPerms("outsiders", perms, null);
                Metropolis.sendMessage(
                        player,
                        "messages.city.successful.set.plot.perm.change.outsiders",
                        "%perms%",
                        perms,
                        "%cityname%",
                        city.getCityName());
                return;
            }

            MPlayer mPlayer = ApiedAPI.getPlayer(subject);
            if (mPlayer == null) {
                Metropolis.sendMessage(player, "messages.error.player.notFound");
                return;
            }
            if (plot.getPlayerPlotPerm(mPlayer.getUuid().toString()) == null) {
                String perms = Utilities.parsePermChange(null, perm, player,"plot");
                if (perms == null) {
                    return;
                }
                Database.addLogEntry(
                        city,
                        "{ \"type\": \"plot\", \"subtype\": \"perm\", \"id\": "
                                + plot.getPlotId()
                                + ", \"name\": "
                                + plot.getPlotName()
                                + ", \"type\": "
                                + "player"
                                + ", \"from\": "
                                + "null"
                                + ", \"to\": "
                                + perms
                                + ", \"issuer\": "
                                + player.getUniqueId()
                                + ", \"player\": "
                                + mPlayer.getUuid()
                                + " }");
                plot.setPlotPerms("players", perms, mPlayer.getUuid().toString());
            } else {
                String perms = Utilities.parsePermChange(plot.getPlayerPlotPerm(mPlayer.getUuid().toString()).getPerms(), perm, player, "plot");
                String from = Arrays.toString(plot.getPlayerPlotPerm(mPlayer.getUuid().toString()).getPerms());
                if (perms == null) {
                    return;
                }
                Database.addLogEntry(
                        city,
                        "{ \"type\": \"plot\", \"subtype\": \"perm\", \"id\": "
                                + plot.getPlotId()
                                + ", \"name\": "
                                + plot.getPlotName()
                                + ", \"type\": "
                                + "player"
                                + ", \"from\": "
                                + from
                                + ", \"to\": "
                                + perms
                                + ", \"issuer\": "
                                + player.getUniqueId()
                                + ", \"player\": "
                                + mPlayer.getUuid()
                                + " }");
                plot.setPlotPerms("players", perms, mPlayer.getUuid().toString());
            }
            Metropolis.sendMessage(
                    player,
                    "messages.city.successful.set.plot.perm.change.player",
                    "%player%",
                    mPlayer.getName(),
                    "%perms%",
                    new String(plot.getPlayerPlotPerm(mPlayer.getUuid().toString()).getPerms()),
                    "%cityname%",
                    city.getCityName());
        } else {
            Metropolis.sendMessage(
                    player, "messages.error.city.permissionDenied", "%cityname%", city.getCityName());
        }
    }

    @Subcommand("set")
    public static void onSet(Player player) {
        Metropolis.sendMessage(player, "messages.syntax.plot.set.typeAdmin");
        Metropolis.sendMessage(player, "messages.syntax.plot.set.set");
    }

    @Subcommand("set")
    public class Set extends BaseCommand {

        @Subcommand("owner")
        @CommandCompletion("@playerNames")
        public static void onOwner(Player player, String playerName) {
            Plot plot = Utilities.hasPlotPermissions(player, "metropolis.plot.set.owner", Role.INVITER, true);
            if (plot == null) {
                return;
            }
            if (plot.getCity().isReserve()) {
                Metropolis.sendMessage(player, "messages.error.city.reserve");
                return;
            }
            boolean isMayor = Objects.equals(CityDatabase.getCityRole(plot.getCity(), player.getUniqueId().toString()), Role.MAYOR);
            if (plot.isKMarked() && isMayor) {
                Metropolis.sendMessage(
                        player, "messages.error.city.permissionDenied", "%cityname%", plot.getCity().getCityName());
                return;
            }
            if (playerName.equals("-")) {
                if (plot.getPlotOwnerUUID() == null) {
                    Metropolis.sendMessage(
                            player,
                            "messages.error.plot.set.owner.alreadyNoOwner",
                            "%cityname%",
                            plot.getCity().getCityName());
                    return;
                }
                plot.setPlotOwnerUUID(null);
                for (Player plotPlayer : plot.playersInPlot()) {
                    Utilities.sendCityScoreboard(plotPlayer, plot.getCity(), plot);
                }
                Metropolis.sendMessage(
                        player, "messages.plot.set.owner.removed", "%cityname%", plot.getCity().getCityName());
                return;
            }
            MPlayer mPlayer = ApiedAPI.getPlayer(playerName);
            if (mPlayer == null) {
                Metropolis.sendMessage(player, "messages.error.player.notFound");
                return;
            }
            if (!Objects.requireNonNull(CityDatabase.memberCityList(mPlayer.getUuid().toString())).contains(plot.getCity())) {
                if (plot.getPlotType() == null || !plot.getPlotType().equals("vacation")) {
                    Metropolis.sendMessage(
                            player,
                            "messages.error.plot.set.owner.notInCity",
                            "%cityname%",
                            plot.getCity().getCityName());
                    return;
                }
            }
            if (plot.getPlotRent() > 0) {
                Metropolis.sendMessage(player, "messages.error.plot.set.owner.rented");
                return;
            }
            if (plot.getPlotOwnerUUID() != null) {
                if (plot.getPlotOwnerUUID().equals(mPlayer.getUuid().toString())) {
                    Metropolis.sendMessage(
                            player,
                            "messages.error.plot.set.owner.alreadyOwner",
                            "%cityname%",
                            plot.getCity().getCityName());
                    return;
                }
            }
            plot.setPlotOwnerUUID(mPlayer.getUuid().toString());
            for (Player plotPlayer : plot.playersInPlot()) {
                Utilities.sendCityScoreboard(plotPlayer, plot.getCity(), plot);
            }
            Metropolis.sendMessage(
                    player,
                    "messages.plot.set.owner.success",
                    "%player%",
                    mPlayer.getName(),
                    "%cityname%",
                    plot.getCity().getCityName());
        }

        @Subcommand("type")
        @CommandCompletion("@plotType")
        public static void onType(Player player, String type) {
            Plot plot = Utilities.hasPlotPermissions(player, "metropolis.plot.set.type", Role.ASSISTANT, false);
            if (plot == null) {
                return;
            }
            if (plot.getCity().isReserve()) {
                Metropolis.sendMessage(player, "messages.error.city.reserve");
                return;
            }
            Role role = CityDatabase.getCityRole(plot.getCity(), player.getUniqueId().toString());
            assert role != null;
            if (plot.isKMarked() && !role.equals(Role.MAYOR)) {
                Metropolis.sendMessage(
                        player, "messages.error.city.permissionDenied", "%cityname%", plot.getCity().getCityName());
                return;
            }
            switch (type) {
                case "-" -> {
                    if (!player.hasPermission("metropolis.plot.set.type.remove")) {
                        Metropolis.sendAccessDenied(player);
                        return;
                    }
                    if (plot.getPlotType() == null) {
                        Metropolis.sendMessage(
                                player,
                                "messages.error.plot.set.type.alreadyNoType",
                                "%cityname%",
                                plot.getCity().getCityName());
                        return;
                    }
                    Database.addLogEntry(
                            plot.getCity(),
                            "{ \"type\": \"plot\", \"subtype\": \"type\", \"id\": "
                                    + plot.getPlotId()
                                    + ", \"name\": "
                                    + plot.getPlotName()
                                    + ", \"from\": "
                                    + plot.getPlotType()
                                    + ", \"to\": "
                                    + null
                                    + ", \"issuer\": "
                                    + player.getUniqueId()
                                    + " }");
                    plot.setPlotType(null);
                    for (Player plotPlayer : plot.playersInPlot()) {
                        Utilities.sendCityScoreboard(plotPlayer, plot.getCity(), plot);
                    }
                    Metropolis.sendMessage(
                            player, "messages.plot.set.type.removed", "%cityname%", plot.getCity().getCityName());
                    return;
                }
                case "church" -> {
                    if (!player.hasPermission("metropolis.plot.set.type.church")) {
                        Metropolis.sendAccessDenied(player);
                        return;
                    }
                    if (plot.getPlotType() == null) {
                        Database.addLogEntry(
                                plot.getCity(),
                                "{ \"type\": \"plot\", \"subtype\": \"type\", \"id\": "
                                        + plot.getPlotId()
                                        + ", \"name\": "
                                        + plot.getPlotName()
                                        + ", \"from\": "
                                        + null
                                        + ", \"to\": "
                                        + "church"
                                        + ", \"issuer\": "
                                        + player.getUniqueId()
                                        + " }");
                        plot.setPlotType("church");
                        for (Player plotPlayer : plot.playersInPlot()) {
                            Utilities.sendCityScoreboard(plotPlayer, plot.getCity(), plot);
                        }
                        Metropolis.sendMessage(
                                player,
                                "messages.plot.set.type.success",
                                "%cityname%",
                                plot.getCity().getCityName(),
                                "%type%",
                                "Kyrka");
                        return;
                    }
                    if (plot.getPlotType().equalsIgnoreCase("church")) {
                        Metropolis.sendMessage(
                                player,
                                "messages.error.plot.set.type.alreadyType",
                                "%cityname%",
                                plot.getCity().getCityName(),
                                "%type%",
                                "Kyrka");
                        return;
                    }
                    Database.addLogEntry(
                            plot.getCity(),
                            "{ \"type\": \"plot\", \"subtype\": \"type\", \"id\": "
                                    + plot.getPlotId()
                                    + ", \"name\": "
                                    + plot.getPlotName()
                                    + ", \"from\": "
                                    + plot.getPlotType()
                                    + ", \"to\": "
                                    + "church"
                                    + ", \"issuer\": "
                                    + player.getUniqueId()
                                    + " }");
                    plot.setPlotType("church");
                    for (Player plotPlayer : plot.playersInPlot()) {
                        Utilities.sendCityScoreboard(plotPlayer, plot.getCity(), plot);
                    }
                    Metropolis.sendMessage(
                            player,
                            "messages.plot.set.type.success",
                            "%cityname%",
                            plot.getCity().getCityName(),
                            "%type%",
                            "Kyrka");
                    return;
                }
                case "farm" -> {
                    if (!player.hasPermission("metropolis.plot.set.type.farm")) {
                        Metropolis.sendAccessDenied(player);
                        return;
                    }
                    if (plot.getPlotType() == null) {
                        Database.addLogEntry(
                                plot.getCity(),
                                "{ \"type\": \"plot\", \"subtype\": \"type\", \"id\": "
                                        + plot.getPlotId()
                                        + ", \"name\": "
                                        + plot.getPlotName()
                                        + ", \"from\": "
                                        + null
                                        + ", \"to\": "
                                        + "farm"
                                        + ", \"issuer\": "
                                        + player.getUniqueId()
                                        + " }");
                        plot.setPlotType("farm");
                        for (Player plotPlayer : plot.playersInPlot()) {
                            Utilities.sendCityScoreboard(plotPlayer, plot.getCity(), plot);
                        }
                        Metropolis.sendMessage(
                                player,
                                "messages.plot.set.type.success",
                                "%cityname%",
                                plot.getCity().getCityName(),
                                "%type%",
                                "Farm");
                        return;
                    }
                    if (plot.getPlotType().equalsIgnoreCase("farm")) {
                        Metropolis.sendMessage(
                                player,
                                "messages.error.plot.set.type.alreadyType",
                                "%cityname%",
                                plot.getCity().getCityName(),
                                "%type%",
                                "Farm");
                        return;
                    }
                    Database.addLogEntry(
                            plot.getCity(),
                            "{ \"type\": \"plot\", \"subtype\": \"type\", \"id\": "
                                    + plot.getPlotId()
                                    + ", \"name\": "
                                    + plot.getPlotName()
                                    + ", \"from\": "
                                    + plot.getPlotType()
                                    + ", \"to\": "
                                    + "farm"
                                    + ", \"issuer\": "
                                    + player.getUniqueId()
                                    + " }");
                    plot.setPlotType("farm");
                    for (Player plotPlayer : plot.playersInPlot()) {
                        Utilities.sendCityScoreboard(plotPlayer, plot.getCity(), plot);
                    }
                    Metropolis.sendMessage(
                            player,
                            "messages.plot.set.type.success",
                            "%cityname%",
                            plot.getCity().getCityName(),
                            "%type%",
                            "Farm");
                    return;
                }
                case "shop" -> {
                    if (!player.hasPermission("metropolis.plot.set.type.shop")) {
                        Metropolis.sendAccessDenied(player);
                        return;
                    }
                    if (plot.getPlotType() == null) {
                        Database.addLogEntry(
                                plot.getCity(),
                                "{ \"type\": \"plot\", \"subtype\": \"type\", \"id\": "
                                        + plot.getPlotId()
                                        + ", \"name\": "
                                        + plot.getPlotName()
                                        + ", \"from\": "
                                        + null
                                        + ", \"to\": "
                                        + "shop"
                                        + ", \"issuer\": "
                                        + player.getUniqueId()
                                        + " }");
                        plot.setPlotType("shop");
                        for (Player plotPlayer : plot.playersInPlot()) {
                            Utilities.sendCityScoreboard(plotPlayer, plot.getCity(), plot);
                        }
                        Metropolis.sendMessage(
                                player,
                                "messages.plot.set.type.success",
                                "%cityname%",
                                plot.getCity().getCityName(),
                                "%type%",
                                "Affär");
                        return;
                    }
                    if (plot.getPlotType().equalsIgnoreCase("shop")) {
                        Metropolis.sendMessage(
                                player,
                                "messages.error.plot.set.type.alreadyType",
                                "%cityname%",
                                plot.getCity().getCityName(),
                                "%type%",
                                "Affär");
                        return;
                    }
                    Database.addLogEntry(
                            plot.getCity(),
                            "{ \"type\": \"plot\", \"subtype\": \"type\", \"id\": "
                                    + plot.getPlotId()
                                    + ", \"name\": "
                                    + plot.getPlotName()
                                    + ", \"from\": "
                                    + plot.getPlotType()
                                    + ", \"to\": "
                                    + "shop"
                                    + ", \"issuer\": "
                                    + player.getUniqueId()
                                    + " }");
                    plot.setPlotType("shop");
                    for (Player plotPlayer : plot.playersInPlot()) {
                        Utilities.sendCityScoreboard(plotPlayer, plot.getCity(), plot);
                    }
                    Metropolis.sendMessage(
                            player,
                            "messages.plot.set.type.success",
                            "%cityname%",
                            plot.getCity().getCityName(),
                            "%type%",
                            "Affär");
                    return;
                }
                case "vacation" -> {
                    if (!player.hasPermission("metropolis.plot.set.type.vacation")) {
                        Metropolis.sendAccessDenied(player);
                        return;
                    }
                    if (plot.getPlotType() == null) {
                        Database.addLogEntry(
                                plot.getCity(),
                                "{ \"type\": \"plot\", \"subtype\": \"type\", \"id\": "
                                        + plot.getPlotId()
                                        + ", \"name\": "
                                        + plot.getPlotName()
                                        + ", \"from\": "
                                        + null
                                        + ", \"to\": "
                                        + "vacation"
                                        + ", \"issuer\": "
                                        + player.getUniqueId()
                                        + " }");
                        plot.setPlotType("vacation");
                        for (Player plotPlayer : plot.playersInPlot()) {
                            Utilities.sendCityScoreboard(plotPlayer, plot.getCity(), plot);
                        }
                        Metropolis.sendMessage(
                                player,
                                "messages.plot.set.type.success",
                                "%cityname%",
                                plot.getCity().getCityName(),
                                "%type%",
                                "Ferietomt");
                        return;
                    }
                    if (plot.getPlotType().equalsIgnoreCase("vacation")) {
                        Metropolis.sendMessage(
                                player,
                                "messages.error.plot.set.type.alreadyType",
                                "%cityname%",
                                plot.getCity().getCityName(),
                                "%type%",
                                "Ferietomt");
                        return;
                    }
                    Database.addLogEntry(
                            plot.getCity(),
                            "{ \"type\": \"plot\", \"subtype\": \"type\", \"id\": "
                                    + plot.getPlotId()
                                    + ", \"name\": "
                                    + plot.getPlotName()
                                    + ", \"from\": "
                                    + plot.getPlotType()
                                    + ", \"to\": "
                                    + "vacation"
                                    + ", \"issuer\": "
                                    + player.getUniqueId()
                                    + " }");
                    plot.setPlotType("vacation");
                    for (Player plotPlayer : plot.playersInPlot()) {
                        Utilities.sendCityScoreboard(plotPlayer, plot.getCity(), plot);
                    }
                    Metropolis.sendMessage(
                            player,
                            "messages.plot.set.type.success",
                            "%cityname%",
                            plot.getCity().getCityName(),
                            "%type%",
                            "Ferietomt");
                    return;
                }
                case "jail" -> {
                    if (player.hasPermission("metropolis.admin.plot.set.type.jail")) {
                        if (plot.getPlotType() == null) {
                            Database.addLogEntry(
                                    plot.getCity(),
                                    "{ \"type\": \"plot\", \"subtype\": \"type\", \"id\": "
                                            + plot.getPlotId()
                                            + ", \"name\": "
                                            + plot.getPlotName()
                                            + ", \"from\": "
                                            + null
                                            + ", \"to\": "
                                            + "jail"
                                            + ", \"issuer\": "
                                            + player.getUniqueId()
                                            + " }");
                            plot.setPlotType("jail");
                            for (Player plotPlayer : plot.playersInPlot()) {
                                Utilities.sendCityScoreboard(plotPlayer, plot.getCity(), plot);
                            }
                            Metropolis.sendMessage(
                                    player,
                                    "messages.plot.set.type.success",
                                    "%cityname%",
                                    plot.getCity().getCityName(),
                                    "%type%",
                                    "Fängelse");
                            return;
                        }
                        if (plot.getPlotType().equalsIgnoreCase("jail")) {
                            Metropolis.sendMessage(
                                    player,
                                    "messages.error.plot.set.type.alreadyType",
                                    "%cityname%",
                                    plot.getCity().getCityName(),
                                    "%type%",
                                    "Fängelse");
                            return;
                        }
                        Database.addLogEntry(
                                plot.getCity(),
                                "{ \"type\": \"plot\", \"subtype\": \"type\", \"id\": "
                                        + plot.getPlotId()
                                        + ", \"name\": "
                                        + plot.getPlotName()
                                        + ", \"from\": "
                                        + plot.getPlotType()
                                        + ", \"to\": "
                                        + "jail"
                                        + ", \"issuer\": "
                                        + player.getUniqueId()
                                        + " }");
                        plot.setPlotType("jail");
                        for (Player plotPlayer : plot.playersInPlot()) {
                            Utilities.sendCityScoreboard(plotPlayer, plot.getCity(), plot);
                        }
                        Metropolis.sendMessage(
                                player,
                                "messages.plot.set.type.success",
                                "%cityname%",
                                plot.getCity().getCityName(),
                                "%type%",
                                "Fängelse");
                    } else {
                        Metropolis.sendMessage(
                                player,
                                "messages.error.city.permissionDenied",
                                "%cityname%",
                                plot.getCity().getCityName());
                    }
                    return;
                }
            }
            Metropolis.sendMessage(
                    player,
                    "messages.error.plot.set.type.invalidType",
                    "%cityname%",
                    plot.getCity().getCityName());
        }

        @Subcommand("name")
        public static void onName(Player player, String name) {
            Plot plot = Utilities.hasPlotPermissions(player, "metropolis.plot.set.name", Role.INVITER, true);
            if (plot == null) {
                return;
            }
            if (plot.getCity().isReserve()) {
                Metropolis.sendMessage(player, "messages.error.city.reserve");
                return;
            }
            Role role = CityDatabase.getCityRole(plot.getCity(), player.getUniqueId().toString());
            assert role != null;
            boolean isMayor = role.equals(Role.MAYOR);
            if (plot.isKMarked() && !isMayor) {
                Metropolis.sendMessage(
                        player, "messages.error.city.permissionDenied", "%cityname%", plot.getCity().getCityName());
                return;
            }
            if (Objects.equals(plot.getPlotName(), name)) {
                Metropolis.sendMessage(
                        player,
                        "messages.error.plot.set.name.alreadyName",
                        "%cityname%",
                        plot.getCity().getCityName(),
                        "%plotname%",
                        name);
                return;
            }
            if (name.equals("-")) {
                Database.addLogEntry(
                        plot.getCity(),
                        "{ \"type\": \"plot\", \"subtype\": \"name\", \"id\": "
                                + plot.getPlotId()
                                + ", \"from\": "
                                + plot.getPlotName()
                                + ", \"to\": "
                                + "Tomt #"
                                + plot.getPlotId()
                                + ", \"issuer\": "
                                + player.getUniqueId()
                                + " }");
                plot.setPlotName("Tomt #" + plot.getPlotId());
                for (Player plotPlayer : plot.playersInPlot()) {
                    Utilities.sendCityScoreboard(plotPlayer, plot.getCity(), plot);
                }
                Metropolis.sendMessage(
                        player,
                        "messages.plot.set.name.success",
                        "%cityname%",
                        plot.getCity().getCityName(),
                        "%plotname%",
                        name);
                return;
            }
            if (name.length() > Metropolis.configuration.getPlotNameLimit()) {
                Metropolis.sendMessage(
                        player, "messages.error.plot.set.name.tooLong", "%cityname%", plot.getCity().getCityName(), "%maxlength%", String.valueOf(Metropolis.configuration.getPlotNameLimit()));
                return;
            }
            Database.addLogEntry(
                    plot.getCity(),
                    "{ \"type\": \"plot\", \"subtype\": \"name\", \"id\": "
                            + plot.getPlotId()
                            + ", \"from\": "
                            + plot.getPlotName()
                            + ", \"to\": "
                            + name
                            + ", \"issuer\": "
                            + player.getUniqueId()
                            + " }");
            plot.setPlotName(name);
            for (Player plotPlayer : plot.playersInPlot()) {
                Utilities.sendCityScoreboard(plotPlayer, plot.getCity(), plot);
            }
            Metropolis.sendMessage(
                    player,
                    "messages.plot.set.name.success",
                    "%cityname%",
                    plot.getCity().getCityName(),
                    "%plotname%",
                    name);
        }

        @Subcommand("rent")
        public static void onRent(Player player, String rent) {
            Plot plot = Utilities.hasPlotPermissions(player, "metropolis.plot.set.rent", Role.ASSISTANT, false);
            if (plot == null) {
                return;
            }
            if (plot.getCity().isReserve()) {
                Metropolis.sendMessage(player, "messages.error.city.reserve");
                return;
            }
            City city = plot.getCity();
            Role role = CityDatabase.getCityRole(city, player.getUniqueId().toString());
            assert role != null;
            boolean isMayor = role.equals(Role.MAYOR);
            if (plot.isKMarked() && !isMayor) {
                Metropolis.sendMessage(
                        player, "messages.error.city.permissionDenied", "%cityname%", city.getCityName());
                return;
            }
            if (rent.equals("-") || rent.equals("0")) {
                Database.addLogEntry(
                        city,
                        "{ \"type\": \"plot\", \"subtype\": \"rent\", \"id\": "
                                + plot.getPlotId()
                                + ", \"name\": "
                                + plot.getPlotName()
                                + ", \"from\": "
                                + plot.getPlotRent()
                                + ", \"to\": "
                                + 0
                                + ", \"issuer\": "
                                + player.getUniqueId()
                                + " }");
                plot.setPlotRent(0);
                for (Player plotPlayer : plot.playersInPlot()) {
                    Utilities.sendCityScoreboard(plotPlayer, plot.getCity(), plot);
                }
                Metropolis.sendMessage(
                        player, "messages.plot.set.rent.removed", "%cityname%", city.getCityName());
                return;
            }
            if (!rent.matches("[0-9]+")) {
                Metropolis.sendMessage(player, "messages.syntax.plot.set.rent");
                return;
            }
            int rentInt = Integer.parseInt(rent);
            if (rentInt > 100000) {
                Metropolis.sendMessage(
                        player, "messages.error.plot.set.rent.tooHigh", "%cityname%", city.getCityName());
                return;
            }
            if (plot.getPlotOwnerUUID() != null) {
                Metropolis.sendMessage(player, "messages.error.plot.set.rent.owner");
                return;
            }
            if (!plot.isForSale()) {
                plot.setPlotPrice(0);
                for (Player plotPlayer : plot.playersInPlot()) {
                    Utilities.sendCityScoreboard(plotPlayer, plot.getCity(), plot);
                }
                Database.addLogEntry(
                        city,
                        "{ \"type\": \"plotMarket\", \"subtype\": \"add\", \"id\": "
                                + plot.getPlotId()
                                + ", \"name\": "
                                + plot.getPlotName()
                                + ", \"to\": "
                                + 0
                                + ", \"player\": "
                                + player.getUniqueId()
                                + " }");
                plot.setForSale(true);
            }
            Database.addLogEntry(
                    city,
                    "{ \"type\": \"plot\", \"subtype\": \"rent\", \"id\": "
                            + plot.getPlotId()
                            + ", \"name\": "
                            + plot.getPlotName()
                            + ", \"from\": "
                            + plot.getPlotRent()
                            + ", \"to\": "
                            + rentInt
                            + ", \"issuer\": "
                            + player.getUniqueId()
                            + " }");
            plot.setPlotRent(rentInt);
            for (Player plotPlayer : plot.playersInPlot()) {
                Utilities.sendCityScoreboard(plotPlayer, plot.getCity(), plot);
            }
            Metropolis.sendMessage(
                    player,
                    "messages.plot.set.rent.success",
                    "%cityname%",
                    city.getCityName(),
                    "%rent%",
                    Utilities.formattedMoney(rentInt));
        }
    }

    @Subcommand("toggle")
    public static void onToggle(Player player) {
        if (player.hasPermission("metropolis.admin.plot.toggle.keepinv")) {
            player.sendMessage("§7Syntax: /plot toggle keepinv");
        }
        if (player.hasPermission("metropolis.admin.plot.toggle.keepexp")) {
            player.sendMessage("§7Syntax: /plot toggle keepexp");
        }
        if (player.hasPermission("metropolis.admin.plot.toggle.pvp")) {
            player.sendMessage("§7Syntax: /plot toggle pvp");
        }
        if (player.hasPermission("metropolis.admin.plot.toggle.lock")) {
            player.sendMessage("§7Syntax: /plot toggle lock");
        }
        Metropolis.sendMessage(player, "messages.syntax.plot.toggle");
    }

    @Subcommand("toggle")
    public class Toggle extends BaseCommand {

        @Subcommand("pvp")
        public static void onPvp(Player player) {
            if (!player.hasPermission("metropolis.admin.plot.toggle.pvp")) {
                Metropolis.sendAccessDenied(player);
                return;
            }
            Plot plot = Metropolis.playerInPlot.get(player.getUniqueId());
            if (plot == null) {
                Metropolis.sendMessage(player, "messages.error.plot.notInPlot");
                return;
            }
            if (plot.getCity().isReserve()) {
                Metropolis.sendMessage(player, "messages.error.city.reserve");
                return;
            }
            City city = plot.getCity();
            if (plot.hasFlag('p')) {
                Database.addLogEntry(
                        city,
                        "{ \"type\": \"plot\", \"subtype\": \"toggle\", \"id\": "
                                + plot.getPlotId()
                                + ", \"name\": "
                                + plot.getPlotName()
                                + ", \"item\": "
                                + "pvp"
                                + ", \"state\": "
                                + false
                                + ", \"issuer\": "
                                + player.getUniqueId()
                                + " }");
                plot.setPlotFlags(
                        Objects.requireNonNull(Utilities.parseFlagChange(plot.getPlotFlags(), "-p")));
                Metropolis.sendMessage(
                        player,
                        "messages.plot.toggle.pvp.success",
                        "%cityname%",
                        city.getCityName(),
                        "%status%",
                        "inaktiverat");
                for (Player plotPlayer : plot.playersInPlot()) {
                    Utilities.sendCityScoreboard(plotPlayer, plot.getCity(), plot);
                }
                return;
            }
            if (plot.getPlotFlags().length == 0) {
                Database.addLogEntry(
                        city,
                        "{ \"type\": \"plot\", \"subtype\": \"toggle\", \"id\": "
                                + plot.getPlotId()
                                + ", \"name\": "
                                + plot.getPlotName()
                                + ", \"item\": "
                                + "pvp"
                                + ", \"state\": "
                                + true
                                + ", \"issuer\": "
                                + player.getUniqueId()
                                + " }");
                plot.setPlotFlags("p");
                Metropolis.sendMessage(
                        player,
                        "messages.plot.toggle.pvp.success",
                        "%cityname%",
                        city.getCityName(),
                        "%status%",
                        "aktiverat");
                for (Player plotPlayer : plot.playersInPlot()) {
                    Utilities.sendCityScoreboard(plotPlayer, plot.getCity(), plot);
                }
                return;
            }
            plot.setPlotFlags(
                    Objects.requireNonNull(Utilities.parseFlagChange(plot.getPlotFlags(), "+p")));
            Metropolis.sendMessage(
                    player,
                    "messages.plot.toggle.pvp.success",
                    "%cityname%",
                    city.getCityName(),
                    "%status%",
                    "aktiverat");
            for (Player plotPlayer : plot.playersInPlot()) {
                Utilities.sendCityScoreboard(plotPlayer, plot.getCity(), plot);
            }
        }

        @Subcommand("animals")
        public static void onAnimals(Player player) {
            Plot plot = Utilities.hasPlotPermissions(player, "metropolis.plot.toggle.animals", Role.ASSISTANT, false);
            if (plot == null) {
                return;
            }
            if (plot.getCity().isReserve()) {
                Metropolis.sendMessage(player, "messages.error.city.reserve");
                return;
            }
            City city = plot.getCity();
            Role role = CityDatabase.getCityRole(city, player.getUniqueId().toString());
            assert role != null;
            boolean isMayor = role.equals(Role.MAYOR);
            if (plot.isKMarked() && !isMayor) {
                Metropolis.sendMessage(
                        player, "messages.error.city.permissionDenied", "%cityname%", city.getCityName());
                return;
            }
            if (plot.hasFlag('a')) {
                Database.addLogEntry(
                        city,
                        "{ \"type\": \"plot\", \"subtype\": \"toggle\", \"id\": "
                                + plot.getPlotId()
                                + ", \"name\": "
                                + plot.getPlotName()
                                + ", \"item\": "
                                + "animals"
                                + ", \"state\": "
                                + false
                                + ", \"issuer\": "
                                + player.getUniqueId()
                                + " }");
                plot.setPlotFlags(
                        Objects.requireNonNull(Utilities.parseFlagChange(plot.getPlotFlags(), "-a")));
                Metropolis.sendMessage(
                        player,
                        "messages.plot.toggle.animals.success",
                        "%cityname%",
                        city.getCityName(),
                        "%status%",
                        "inaktiverat");
                for (Player plotPlayer : plot.playersInPlot()) {
                    Utilities.sendCityScoreboard(plotPlayer, plot.getCity(), plot);
                }
                return;
            }
            if (plot.getPlotFlags().length == 0) {
                Database.addLogEntry(
                        city,
                        "{ \"type\": \"plot\", \"subtype\": \"toggle\", \"id\": "
                                + plot.getPlotId()
                                + ", \"name\": "
                                + plot.getPlotName()
                                + ", \"item\": "
                                + "animals"
                                + ", \"state\": "
                                + true
                                + ", \"issuer\": "
                                + player.getUniqueId()
                                + " }");
                plot.setPlotFlags("a");
                Metropolis.sendMessage(
                        player,
                        "messages.plot.toggle.animals.success",
                        "%cityname%",
                        city.getCityName(),
                        "%status%",
                        "aktiverat");
                for (Player plotPlayer : plot.playersInPlot()) {
                    Utilities.sendCityScoreboard(plotPlayer, plot.getCity(), plot);
                }
                return;
            }
            plot.setPlotFlags(
                    Objects.requireNonNull(Utilities.parseFlagChange(plot.getPlotFlags(), "+a")));
            Metropolis.sendMessage(
                    player,
                    "messages.plot.toggle.animals.success",
                    "%cityname%",
                    city.getCityName(),
                    "%status%",
                    "aktiverat");
            for (Player plotPlayer : plot.playersInPlot()) {
                Utilities.sendCityScoreboard(plotPlayer, plot.getCity(), plot);
            }
        }

        @Subcommand("mobs")
        public static void onMobs(Player player) {
            Plot plot = Utilities.hasPlotPermissions(player, "metropolis.plot.toggle.mobs", Role.ASSISTANT, false);
            if (plot == null) {
                return;
            }
            if (plot.getCity().isReserve()) {
                Metropolis.sendMessage(player, "messages.error.city.reserve");
                return;
            }
            City city = plot.getCity();
            Role role = CityDatabase.getCityRole(city, player.getUniqueId().toString());
            assert role != null;
            boolean isMayor = role.equals(Role.MAYOR);
            if (plot.isKMarked() && !isMayor) {
                Metropolis.sendMessage(
                        player, "messages.error.city.permissionDenied", "%cityname%", city.getCityName());
                return;
            }
            if (plot.hasFlag('m')) {
                Database.addLogEntry(
                        city,
                        "{ \"type\": \"plot\", \"subtype\": \"toggle\", \"id\": "
                                + plot.getPlotId()
                                + ", \"name\": "
                                + plot.getPlotName()
                                + ", \"item\": "
                                + "monsters"
                                + ", \"state\": "
                                + false
                                + ", \"issuer\": "
                                + player.getUniqueId()
                                + " }");
                plot.setPlotFlags(
                        Objects.requireNonNull(Utilities.parseFlagChange(plot.getPlotFlags(), "-m")));
                Metropolis.sendMessage(
                        player,
                        "messages.plot.toggle.monsters.success",
                        "%cityname%",
                        city.getCityName(),
                        "%status%",
                        "inaktiverat");
                for (Player plotPlayer : plot.playersInPlot()) {
                    Utilities.sendCityScoreboard(plotPlayer, plot.getCity(), plot);
                }
                return;
            }
            if (plot.getPlotFlags().length == 0) {
                Database.addLogEntry(
                        city,
                        "{ \"type\": \"plot\", \"subtype\": \"toggle\", \"id\": "
                                + plot.getPlotId()
                                + ", \"name\": "
                                + plot.getPlotName()
                                + ", \"item\": "
                                + "monsters"
                                + ", \"state\": "
                                + true
                                + ", \"issuer\": "
                                + player.getUniqueId()
                                + " }");
                plot.setPlotFlags("m");
                Metropolis.sendMessage(
                        player,
                        "messages.plot.toggle.monsters.success",
                        "%cityname%",
                        city.getCityName(),
                        "%status%",
                        "aktiverat");
                for (Player plotPlayer : plot.playersInPlot()) {
                    Utilities.sendCityScoreboard(plotPlayer, plot.getCity(), plot);
                }
                return;
            }
            plot.setPlotFlags(
                    Objects.requireNonNull(Utilities.parseFlagChange(plot.getPlotFlags(), "+m")));
            Metropolis.sendMessage(
                    player,
                    "messages.plot.toggle.monsters.success",
                    "%cityname%",
                    city.getCityName(),
                    "%status%",
                    "aktiverat");
            for (Player plotPlayer : plot.playersInPlot()) {
                Utilities.sendCityScoreboard(plotPlayer, plot.getCity(), plot);
            }
        }

        @Subcommand("meeting")
        public static void onMeeting(Player player) {
            Plot plot = Utilities.hasPlotPermissions(player, "metropolis.plot.toggle.meeting", Role.ASSISTANT, false);
            if (plot == null) {
                return;
            }
            if (plot.getCity().isReserve()) {
                Metropolis.sendMessage(player, "messages.error.city.reserve");
                return;
            }
            City city = plot.getCity();
            Role role = CityDatabase.getCityRole(city, player.getUniqueId().toString());
            assert role != null;
            boolean isMayor = role.equals(Role.MAYOR);
            if (plot.isKMarked() && !isMayor) {
                Metropolis.sendMessage(
                        player, "messages.error.city.permissionDenied", "%cityname%", city.getCityName());
                return;
            }
            if (plot.hasFlag('c')) {
                Database.addLogEntry(
                        city,
                        "{ \"type\": \"plot\", \"subtype\": \"toggle\", \"id\": "
                                + plot.getPlotId()
                                + ", \"name\": "
                                + plot.getPlotName()
                                + ", \"item\": "
                                + "meeting"
                                + ", \"state\": "
                                + false
                                + ", \"issuer\": "
                                + player.getUniqueId()
                                + " }");
                plot.setPlotFlags(
                        Objects.requireNonNull(Utilities.parseFlagChange(plot.getPlotFlags(), "-c")));
                Metropolis.sendMessage(
                        player,
                        "messages.plot.toggle.meeting.success",
                        "%cityname%",
                        city.getCityName(),
                        "%status%",
                        "inaktiverat");
                for (Player plotPlayer : plot.playersInPlot()) {
                    Utilities.sendCityScoreboard(plotPlayer, plot.getCity(), plot);
                }
                return;
            }
            if (plot.getPlotFlags().length == 0) {
                plot.setPlotFlags("c");
                Metropolis.sendMessage(
                        player,
                        "messages.plot.toggle.meeting.success",
                        "%cityname%",
                        city.getCityName(),
                        "%status%",
                        "aktiverat");
                for (Player plotPlayer : plot.playersInPlot()) {
                    Utilities.sendCityScoreboard(plotPlayer, plot.getCity(), plot);
                }
                return;
            }
            Database.addLogEntry(
                    city,
                    "{ \"type\": \"plot\", \"subtype\": \"toggle\", \"id\": "
                            + plot.getPlotId()
                            + ", \"name\": "
                            + plot.getPlotName()
                            + ", \"item\": "
                            + "meeting"
                            + ", \"state\": "
                            + true
                            + ", \"issuer\": "
                            + player.getUniqueId()
                            + " }");
            plot.setPlotFlags(
                    Objects.requireNonNull(Utilities.parseFlagChange(plot.getPlotFlags(), "+c")));
            Metropolis.sendMessage(
                    player,
                    "messages.plot.toggle.meeting.success",
                    "%cityname%",
                    city.getCityName(),
                    "%status%",
                    "aktiverat");
            for (Player plotPlayer : plot.playersInPlot()) {
                Utilities.sendCityScoreboard(plotPlayer, plot.getCity(), plot);
            }
        }

        @Subcommand("keepexp")
        public static void onXp(Player player) {
            if (!player.hasPermission("metropolis.admin.plot.toggle.keepexp")) {
                Metropolis.sendAccessDenied(player);
                return;
            }
            Plot plot = Metropolis.playerInPlot.get(player.getUniqueId());
            if (plot == null) {
                Metropolis.sendMessage(player, "messages.error.plot.notInPlot");
                return;
            }
            if (plot.getCity().isReserve()) {
                Metropolis.sendMessage(player, "messages.error.city.reserve");
                return;
            }
            City city = plot.getCity();
            if (plot.hasFlag('x')) {
                Database.addLogEntry(
                        city,
                        "{ \"type\": \"plot\", \"subtype\": \"toggle\", \"id\": "
                                + plot.getPlotId()
                                + ", \"name\": "
                                + plot.getPlotName()
                                + ", \"item\": "
                                + "keepexp"
                                + ", \"state\": "
                                + false
                                + ", \"issuer\": "
                                + player.getUniqueId()
                                + " }");
                plot.setPlotFlags(
                        Objects.requireNonNull(Utilities.parseFlagChange(plot.getPlotFlags(), "-x")));
                Metropolis.sendMessage(
                        player,
                        "messages.plot.toggle.keepexp.success",
                        "%cityname%",
                        city.getCityName(),
                        "%status%",
                        "tappas");
                for (Player plotPlayer : plot.playersInPlot()) {
                    Utilities.sendCityScoreboard(plotPlayer, plot.getCity(), plot);
                }
                return;
            }
            if (plot.getPlotFlags().length == 0) {
                plot.setPlotFlags("x");
                Metropolis.sendMessage(
                        player,
                        "messages.plot.toggle.keepexp.success",
                        "%cityname%",
                        city.getCityName(),
                        "%status%",
                        "behålls");
                for (Player plotPlayer : plot.playersInPlot()) {
                    Utilities.sendCityScoreboard(plotPlayer, plot.getCity(), plot);
                }
                return;
            }
            Database.addLogEntry(
                    city,
                    "{ \"type\": \"plot\", \"subtype\": \"toggle\", \"id\": "
                            + plot.getPlotId()
                            + ", \"name\": "
                            + plot.getPlotName()
                            + ", \"item\": "
                            + "keepexp"
                            + ", \"state\": "
                            + true
                            + ", \"issuer\": "
                            + player.getUniqueId()
                            + " }");
            plot.setPlotFlags(
                    Objects.requireNonNull(Utilities.parseFlagChange(plot.getPlotFlags(), "+x")));
            Metropolis.sendMessage(
                    player,
                    "messages.plot.toggle.keepexp.success",
                    "%cityname%",
                    city.getCityName(),
                    "%status%",
                    "behålls");
            for (Player plotPlayer : plot.playersInPlot()) {
                Utilities.sendCityScoreboard(plotPlayer, plot.getCity(), plot);
            }
        }

        @Subcommand("keepinv")
        public static void onKeepInv(Player player) {
            if (!player.hasPermission("metropolis.admin.plot.toggle.keepinv")) {
                Metropolis.sendAccessDenied(player);
                return;
            }
            Plot plot = Metropolis.playerInPlot.get(player.getUniqueId());
            if (plot == null) {
                Metropolis.sendMessage(player, "messages.error.plot.notInPlot");
                return;
            }
            if (plot.getCity().isReserve()) {
                Metropolis.sendMessage(player, "messages.error.city.reserve");
                return;
            }
            City city = plot.getCity();
            if (plot.hasFlag('i')) {
                Database.addLogEntry(
                        city,
                        "{ \"type\": \"plot\", \"subtype\": \"toggle\", \"id\": "
                                + plot.getPlotId()
                                + ", \"name\": "
                                + plot.getPlotName()
                                + ", \"item\": "
                                + "keepinv"
                                + ", \"state\": "
                                + false
                                + ", \"issuer\": "
                                + player.getUniqueId()
                                + " }");
                plot.setPlotFlags(
                        Objects.requireNonNull(Utilities.parseFlagChange(plot.getPlotFlags(), "-i")));
                Metropolis.sendMessage(
                        player,
                        "messages.plot.toggle.keepinv.success",
                        "%cityname%",
                        city.getCityName(),
                        "%status%",
                        "tappas");
                for (Player plotPlayer : plot.playersInPlot()) {
                    Utilities.sendCityScoreboard(plotPlayer, plot.getCity(), plot);
                }
                return;
            }
            if (plot.getPlotFlags().length == 0) {
                plot.setPlotFlags("i");
                Metropolis.sendMessage(
                        player,
                        "messages.plot.toggle.keepinv.success",
                        "%cityname%",
                        city.getCityName(),
                        "%status%",
                        "behålls");
                for (Player plotPlayer : plot.playersInPlot()) {
                    Utilities.sendCityScoreboard(plotPlayer, plot.getCity(), plot);
                }
                return;
            }
            Database.addLogEntry(
                    city,
                    "{ \"type\": \"plot\", \"subtype\": \"toggle\", \"id\": "
                            + plot.getPlotId()
                            + ", \"name\": "
                            + plot.getPlotName()
                            + ", \"item\": "
                            + "keepinv"
                            + ", \"state\": "
                            + true
                            + ", \"issuer\": "
                            + player.getUniqueId()
                            + " }");
            plot.setPlotFlags(
                    Objects.requireNonNull(Utilities.parseFlagChange(plot.getPlotFlags(), "+i")));
            Metropolis.sendMessage(
                    player,
                    "messages.plot.toggle.keepinv.success",
                    "%cityname%",
                    city.getCityName(),
                    "%status%",
                    "behålls");
            for (Player plotPlayer : plot.playersInPlot()) {
                Utilities.sendCityScoreboard(plotPlayer, plot.getCity(), plot);
            }
        }

        @Subcommand("lock")
        public static void onLock(Player player) {
            if (!player.hasPermission("metropolis.admin.plot.toggle.lock")) {
                Metropolis.sendAccessDenied(player);
                return;
            }
            Plot plot = Metropolis.playerInPlot.get(player.getUniqueId());
            if (plot == null) {
                Metropolis.sendMessage(player, "messages.error.plot.notInPlot");
                return;
            }
            if (plot.getCity().isReserve()) {
                Metropolis.sendMessage(player, "messages.error.city.reserve");
                return;
            }
            City city = plot.getCity();
            if (plot.hasFlag('l')) {
                Database.addLogEntry(
                        city,
                        "{ \"type\": \"plot\", \"subtype\": \"toggle\", \"id\": "
                                + plot.getPlotId()
                                + ", \"name\": "
                                + plot.getPlotName()
                                + ", \"item\": "
                                + "lock"
                                + ", \"state\": "
                                + false
                                + ", \"issuer\": "
                                + player.getUniqueId()
                                + " }");
                plot.setPlotFlags(
                        Objects.requireNonNull(Utilities.parseFlagChange(plot.getPlotFlags(), "-l")));
                Metropolis.sendMessage(
                        player,
                        "messages.plot.toggle.locked.success",
                        "%cityname%",
                        city.getCityName(),
                        "%status%",
                        "olåst");
                return;
            }
            if (plot.getPlotFlags().length == 0) {
                plot.setPlotFlags("l");
                Metropolis.sendMessage(
                        player,
                        "messages.plot.toggle.locked.success",
                        "%cityname%",
                        city.getCityName(),
                        "%status%",
                        "låst");
                return;
            }
            Database.addLogEntry(
                    city,
                    "{ \"type\": \"plot\", \"subtype\": \"toggle\", \"id\": "
                            + plot.getPlotId()
                            + ", \"name\": "
                            + plot.getPlotName()
                            + ", \"item\": "
                            + "lock"
                            + ", \"state\": "
                            + true
                            + ", \"issuer\": "
                            + player.getUniqueId()
                            + " }");
            plot.setPlotFlags(
                    Objects.requireNonNull(Utilities.parseFlagChange(plot.getPlotFlags(), "+l")));
            Metropolis.sendMessage(
                    player,
                    "messages.plot.toggle.locked.success",
                    "%cityname%",
                    city.getCityName(),
                    "%status%",
                    "låst");
        }

        @Subcommand("k")
        public static void onKMark(Player player) {
            Plot plot = Utilities.hasPlotPermissions(player, "metropolis.plot.toggle.kmark", Role.MAYOR, false);
            if (plot == null) {
                return;
            }
            if (plot.getCity().isReserve()) {
                Metropolis.sendMessage(player, "messages.error.city.reserve");
                return;
            }
            City city = plot.getCity();
            if (plot.isKMarked()) {
                Database.addLogEntry(
                        city,
                        "{ \"type\": \"plot\", \"subtype\": \"toggle\", \"id\": "
                                + plot.getPlotId()
                                + ", \"name\": "
                                + plot.getPlotName()
                                + ", \"item\": "
                                + "k"
                                + ", \"state\": "
                                + false
                                + ", \"issuer\": "
                                + player.getUniqueId()
                                + " }");
                plot.setKMarked(false);
                for (Player plotPlayer : plot.playersInPlot()) {
                    Utilities.sendCityScoreboard(plotPlayer, plot.getCity(), plot);
                }
                Metropolis.sendMessage(
                        player,
                        "messages.plot.toggle.k-marked.success",
                        "%cityname%",
                        city.getCityName(),
                        "%status%",
                        "stängdes av");
            } else {
                Database.addLogEntry(
                        city,
                        "{ \"type\": \"plot\", \"subtype\": \"toggle\", \"id\": "
                                + plot.getPlotId()
                                + ", \"name\": "
                                + plot.getPlotName()
                                + ", \"item\": "
                                + "k"
                                + ", \"state\": "
                                + true
                                + ", \"issuer\": "
                                + player.getUniqueId()
                                + " }");
                plot.setKMarked(true);
                for (Player plotPlayer : plot.playersInPlot()) {
                    Utilities.sendCityScoreboard(plotPlayer, plot.getCity(), plot);
                }
                Metropolis.sendMessage(
                        player,
                        "messages.plot.toggle.k-marked.success",
                        "%cityname%",
                        city.getCityName(),
                        "%status%",
                        "sattes på");
            }
        }
    }

    @Subcommand("update")
    public static void onUpdate(Player player) {
        Plot plot = Utilities.hasPlotPermissions(player, "metropolis.plot.update", Role.VICE_MAYOR, false);
        if (plot == null) {
            return;
        }
        if (plot.getCity().isReserve()) {
            Metropolis.sendMessage(player, "messages.error.city.reserve");
            return;
        }
        City city = plot.getCity();
        Role role = CityDatabase.getCityRole(city, player.getUniqueId().toString());
        assert role != null;
        boolean isMayor = role.equals(Role.MAYOR);

        if (plot.isKMarked() && !isMayor) {
            Metropolis.sendMessage(
                    player, "messages.error.city.permissionDenied", "%cityname%", city.getCityName());
            return;
        }

        Polygon regionPolygon = MetropolisListener.playerPolygons.get(player.getUniqueId());
        Location[] locations =
                MetropolisListener.savedLocs.get(player.getUniqueId()).toArray(new Location[0]);
        double minX = regionPolygon.getEnvelopeInternal().getMinX();
        double maxX = regionPolygon.getEnvelopeInternal().getMaxX();
        double minY = regionPolygon.getEnvelopeInternal().getMinY();
        double maxY = regionPolygon.getEnvelopeInternal().getMaxY();

        int yMin = plot.getPlotYMin();
        int yMax = plot.getPlotYMax();

        if (maxX - minX < 3 || maxY - minY < 3) {
            Metropolis.sendMessage(player, "messages.error.plot.tooSmall");
            return;
        }
        if (MetropolisListener.playerYMax.get(player.getUniqueId())
                - MetropolisListener.playerYMin.get(player.getUniqueId())
                < 3) {
            Metropolis.sendMessage(player, "messages.error.plot.tooLowY");
            return;
        }

        int chunkSize = 16;
        int startX = (int) Math.floor(minX / chunkSize) * chunkSize;
        int endX = (int) Math.floor(maxX / chunkSize) * chunkSize + chunkSize;
        int startY = (int) Math.floor(minY / chunkSize) * chunkSize;
        int endY = (int) Math.floor(maxY / chunkSize) * chunkSize + chunkSize;

        for (int x = startX; x < endX; x += chunkSize) {
            for (int z = startY; z < endY; z += chunkSize) {
                Polygon chunkPolygon = geometryFactory.createPolygon(new Coordinate[]{
                        new Coordinate(x, z),
                        new Coordinate(x + chunkSize, z),
                        new Coordinate(x + chunkSize, z + chunkSize),
                        new Coordinate(x, z + chunkSize),
                        new Coordinate(x, z)
                });
                if (regionPolygon.intersects(chunkPolygon)) {
                    if (CityDatabase.getClaim(new Location(player.getWorld(), x, 0, z)) == null
                            || !Objects.equals(
                            Objects.requireNonNull(
                                            CityDatabase.getClaim(new Location(player.getWorld(), x, 0, z)))
                                    .getCity(),
                            HCDatabase.getHomeCityToCity(player.getUniqueId().toString()))) {
                        Metropolis.sendMessage(player, "messages.error.plot.intersectsExistingClaim");
                        return;
                    }
                    Plot[] intersectingPlots = PlotDatabase.intersectingPlots(regionPolygon, yMin, yMax, city, player.getWorld());
                    if (!(intersectingPlots == null) && intersectingPlots.length > 1) {
                        Metropolis.sendMessage(player, "messages.error.plot.intersectsExistingPlot");
                        return;
                    }
                    Database.addLogEntry(
                            city,
                            "{ \"type\": \"update\", \"subtype\": \"plot\", \"id\": "
                                    + plot.getPlotId()
                                    + ", \"name\": "
                                    + plot.getPlotName()
                                    + ", \"points\": "
                                    + LocationUtil.parsePoints(locations)
                                    + ", \"ymin\": "
                                    + MetropolisListener.playerYMin.get(player.getUniqueId())
                                    + ", \"ymax\": "
                                    + MetropolisListener.playerYMax.get(player.getUniqueId())
                                    + ", \"player\": "
                                    + player.getUniqueId()
                                    + " }");
                    plot.updatePlot(player, locations, MetropolisListener.playerYMin.get(player.getUniqueId()), MetropolisListener.playerYMax.get(player.getUniqueId()));
                    MetropolisListener.savedLocs.remove(player.getUniqueId());
                    MetropolisListener.playerPolygons.remove(player.getUniqueId());
                    MetropolisListener.playerYMin.remove(player.getUniqueId());
                    MetropolisListener.playerYMax.remove(player.getUniqueId());
                    for (Player plotPlayer : plot.playersInPlot()) {
                        Utilities.sendCityScoreboard(plotPlayer, plot.getCity(), plot);
                    }
                    Metropolis.sendMessage(
                            player,
                            "messages.city.successful.set.plot.new",
                            "%cityname%",
                            city.getCityName(),
                            "%plotname%",
                            plot.getPlotName());
                    Metropolis.sendMessage(
                            player, "messages.plot.update.success", "%cityname%", city.getCityName());
                    return;
                }
            }
        }
    }

    @Subcommand("buy")
    public static void onBuy(Player player) {
        if (!player.hasPermission("metropolis.plot.buy")) {
            Metropolis.sendAccessDenied(player);
            return;
        }
        Plot plot = Metropolis.playerInPlot.get(player.getUniqueId());
        if (plot == null) {
            Metropolis.sendMessage(player, "messages.error.plot.notInPlot");
            return;
        }
        if (plot.getCity().isReserve()) {
            Metropolis.sendMessage(player, "messages.error.city.reserve");
            return;
        }
        City city = plot.getCity();
        if (CityDatabase.getCityBan(city, player.getUniqueId().toString()) != null) {
            Metropolis.sendMessage(player, "messages.error.city.banned");
            return;
        }
        Role role = CityDatabase.getCityRole(city, player.getUniqueId().toString());
        if (role == null && !plot.getPlotType().equalsIgnoreCase("vacation")) {
            Metropolis.sendMessage(
                    player, "messages.error.city.permissionDenied", "%cityname%", city.getCityName());
            return;
        }
        MPlayer mPlayer = ApiedAPI.getPlayer(player);
        if (plot.getPlotOwnerUUID() != null) {
            Metropolis.sendMessage(
                    player,
                    "messages.error.plot.set.owner.alreadyOwner",
                    "%cityname%",
                    city.getCityName());
            return;
        }
        if (!plot.isForSale()) {
            Metropolis.sendMessage(
                    player,
                    "messages.error.plot.set.owner.notForSale",
                    "%cityname%",
                    city.getCityName());
            return;
        }
        if (city.getMaxPlotsPerMember() != -1 && PlotDatabase.getPlayerPlotCount(player) >= city.getMaxPlotsPerMember()) {
            Metropolis.sendMessage(
                    player,
                    "messages.error.plot.set.owner.tooManyPlots",
                    "%cityname%",
                    city.getCityName());
            return;
        }
        if (mPlayer.getBalance() < plot.getPlotPrice()) {
            Metropolis.sendMessage(
                    player, "messages.error.missing.playerBalance", "%cityname%", city.getCityName());
            return;
        }
        mPlayer.removeBalance(plot.getPlotPrice(), "{ \"type\": \"plot\", \"subtype\": \"buy\", \"plotId\": " + plot.getPlotId() + "}");
        city.addCityBalance(plot.getPlotPrice());
        Database.addLogEntry(
                city,
                "{ \"type\": \"buy\", \"subtype\": \"plot\", \"id\": "
                        + plot.getPlotId()
                        + ", \"name\": "
                        + plot.getPlotName()
                        + ", \"player\": "
                        + player.getUniqueId()
                        + " }");
        plot.setPlotOwnerUUID(player.getUniqueId().toString());
        Metropolis.sendMessage(
                player,
                "messages.plot.buy.success",
                "%cityname%",
                city.getCityName(),
                "%plotname%",
                plot.getPlotName(),
                "%price%",
                String.valueOf(plot.getPlotPrice()));
        plot.setForSale(false);
        plot.setPlotPrice(0);
        for (Player plotPlayer : plot.playersInPlot()) {
            Utilities.sendCityScoreboard(plotPlayer, plot.getCity(), plot);
        }
    }

    @Subcommand("cell")
    public class CellCommands extends BaseCommand {

        @Subcommand("new")
        public void onNewCell(Player player) {
            if (!player.hasPermission("metropolis.admin.cell.new")) {
                Metropolis.sendAccessDenied(player);
                return;
            }
            Plot plot = Metropolis.playerInPlot.get(player.getUniqueId());
            if (plot == null) {
                Metropolis.sendMessage(player, "messages.error.plot.notInPlot");
                return;
            }
            if (!plot.isJail()) {
                Metropolis.sendMessage(player, "messages.error.jail.notJail");
                return;
            }

            Location cellLocation = player.getLocation().toBlockLocation();
            Cell cell = JailManager.createCell(plot,cellLocation);
            Metropolis.sendMessage(player,"messages.cell.created", "%id%", String.valueOf(cell.getCellId()));
            Metropolis.sendMessage(player, "messages.cell.clickSign");

            MetropolisListener.waitingForSignClick.put(player.getUniqueId(), cell);
        }

        @Subcommand("cancel")
        public void onCancel(Player player) {
            if (!player.hasPermission("metropolis.admin.cell.cancel")) {
                Metropolis.sendAccessDenied(player);
                return;
            }
            Cell cell = MetropolisListener.waitingForSignClick.get(player.getUniqueId());
            if (cell == null) {
                Metropolis.sendMessage(player, "messages.error.cell.noCell");
                return;
            }
            JailManager.deleteCell(cell);
            MetropolisListener.waitingForSignClick.remove(player.getUniqueId());
            Metropolis.sendMessage(player, "messages.cell.cancelled");
        }

        @Subcommand("list")
        public void onList(Player player) {
            if (!player.hasPermission("metropolis.admin.cell.list")) {
                Metropolis.sendAccessDenied(player);
                return;
            }
            Plot plot = Metropolis.playerInPlot.get(player.getUniqueId());
            if (plot == null) {
                Metropolis.sendMessage(player, "messages.error.plot.notInPlot");
                return;
            }
            if (!plot.isJail()) {
                Metropolis.sendMessage(player, "messages.error.jail.notJail");
                return;
            }

            List<Cell> cells = JailManager.getCellsForPlot(plot);
            if (cells.isEmpty()) {
                Metropolis.sendMessage(player, "messages.error.cell.noCells");
                return;
            }

            cells.sort((c1, c2) -> {
                if (c1.getSignLocation() == null && c2.getSignLocation() != null) return -1;
                if (c1.getSignLocation() != null && c2.getSignLocation() == null) return 1;
                return Integer.compare(c1.getCellId(), c2.getCellId());
            });

            Metropolis.sendMessage(player, "messages.cell.list.header");

            for (Cell cell : cells) {
                Component message;
                if (cell.getSignLocation() == null) {
                    message = Component.text("Cell #" + cell.getCellId() + " (Invalid - No sign)")
                            .color(NamedTextColor.RED);
                } else {
                    message = Component.text("Cell #" + cell.getCellId())
                            .color(NamedTextColor.GREEN);
                }

                message = message.clickEvent(ClickEvent.runCommand("/plot cell tp " + cell.getCellId()))
                        .hoverEvent(HoverEvent.showText(Component.text("Click to teleport")));

                player.sendMessage(message);
            }
        }

        @Subcommand("tp")
        public void onTp(Player player, int cellId) {
            if (!player.hasPermission("metropolis.admin.cell.tp")) {
                Metropolis.sendAccessDenied(player);
                return;
            }
            Plot plot = Metropolis.playerInPlot.get(player.getUniqueId());
            if (plot == null) {
                Metropolis.sendMessage(player, "messages.error.plot.notInPlot");
                return;
            }
            if (!plot.isJail()) {
                Metropolis.sendMessage(player, "messages.error.jail.notJail");
                return;
            }

            List<Cell> cells = JailManager.getCellsForPlot(plot);
            Cell targetCell = cells.stream()
                    .filter(cell -> cell.getCellId() == cellId)
                    .findFirst()
                    .orElse(null);

            if (targetCell == null) {
                Metropolis.sendMessage(player, "messages.error.cell.notFound");
                return;
            }

            player.teleport(targetCell.getLocation());
            Metropolis.sendMessage(player, "messages.cell.teleported", "%id%", String.valueOf(cellId));
        }

        @Subcommand("update")
        public void onUpdate(Player player, int cellId) {
            if (!player.hasPermission("metropolis.admin.cell.update")) {
                Metropolis.sendAccessDenied(player);
                return;
            }
            Plot plot = Metropolis.playerInPlot.get(player.getUniqueId());
            if (plot == null) {
                Metropolis.sendMessage(player, "messages.error.plot.notInPlot");
                return;
            }
            if (!plot.isJail()) {
                Metropolis.sendMessage(player, "messages.error.jail.notJail");
                return;
            }

            if (!JailManager.cellExists(cellId)) {
                Metropolis.sendMessage(player, "messages.error.cell.notFound");
                return;
            }
            Cell cell = JailManager.getCell(cellId);
            cell.setLocation(player.getLocation().toBlockLocation());
            Metropolis.sendMessage(player, "messages.cell.updated", "%id%", String.valueOf(cellId));
        }
    }


    @Subcommand("leaderboard|lb")
    public static void onLeaderboard(Player player) {
        Metropolis.sendMessage(player, "messages.syntax.plot.leaderboard");
    }

    @Subcommand("leaderboard|lb")
    public class LeaderboardCommands extends BaseCommand {

        @Subcommand("start")
        @CommandCompletion("@leaderboardTypes")
        public void onStart(Player player, String type) {
            Plot plot = Utilities.hasPlotPermissions(player, "metropolis.plot.leaderboard.start", Role.ASSISTANT, true);
            if (plot == null) {
                return;
            }
            if (plot.getCity().isReserve()) {
                Metropolis.sendMessage(player, "messages.error.city.reserve");
                return;
            }
            if (PlotDatabase.leaderboardExists(plot.getPlotId())) {
                Metropolis.sendMessage(player, "messages.error.plot.leaderboard.alreadyExists");
                return;
            }
            if (!type.equalsIgnoreCase("place") && !type.equalsIgnoreCase("break") && !type.equalsIgnoreCase("mobs")) {
                Metropolis.sendMessage(player, "messages.error.plot.leaderboard.invalidType");
                return;
            }
            Metropolis.plotLeaderboards.put(plot, PlotDatabase.startLeaderboard(plot, type.toUpperCase(), player));
            Metropolis.plotStandings.put(plot, new ArrayList<>());
            plot.setLeaderboard(true);
            plot.setLeaderboardShown(true);
            Metropolis.sendMessage(player, "messages.plot.leaderboard.started", "%type%", type.toUpperCase());
        }

        @Subcommand("erase")
        public void onErase(Player player) {
            Plot plot = Utilities.hasPlotPermissions(player, "metropolis.plot.leaderboard.erase", Role.ASSISTANT, true);
            if (plot == null) {
                return;
            }
            if (plot.getCity().isReserve()) {
                Metropolis.sendMessage(player, "messages.error.city.reserve");
                return;
            }
            if (!plot.isHasLeaderboard()) {
                Metropolis.sendMessage(player, "messages.error.plot.leaderboard.notStarted");
                return;
            }
            PlotDatabase.eraseLeaderboard(plot);
            Metropolis.sendMessage(player, "messages.plot.leaderboard.erased");
        }

        @Subcommand("toggle")
        public void onToggle(Player player) {
            Plot plot = Utilities.hasPlotPermissions(player, "metropolis.plot.leaderboard.toggle", Role.ASSISTANT, true);
            if (plot == null) {
                return;
            }
            if (plot.getCity().isReserve()) {
                Metropolis.sendMessage(player, "messages.error.city.reserve");
                return;
            }
            if (!plot.isHasLeaderboard()) {
                Metropolis.sendMessage(player, "messages.error.plot.leaderboard.notStarted");
                return;
            }
            if (plot.isLeaderboardShown()) {
                plot.setLeaderboardShown(false);
                Metropolis.sendMessage(player, "messages.plot.leaderboard.hidden");
            } else {
                plot.setLeaderboardShown(true);
                Metropolis.sendMessage(player, "messages.plot.leaderboard.shown");
            }
        }

        @Subcommand("list")
        public void onList(Player player, @Default("1") int page) {
            Plot plot = Metropolis.playerInPlot.get(player.getUniqueId());
            if (plot == null) {
                Metropolis.sendMessage(player, "messages.error.plot.notInPlot");
                return;
            }
            List<Standing> standings = Metropolis.plotStandings.get(plot);
            if (standings == null || standings.isEmpty()) {
                Metropolis.sendMessage(player, "messages.error.plot.leaderboard.notStarted");
                return;
            }
            standings.sort((s1, s2) -> Integer.compare(s2.getCount(), s1.getCount()));

            int itemsPerPage = 8;
            int start = (page - 1) * itemsPerPage;
            int end = Math.min(page * itemsPerPage, standings.size());

            if (start >= standings.size()) {
                Metropolis.sendMessage(player, "messages.error.plot.leaderboard.notEnoughPages");
                return;
            }
            Leaderboard leaderboard = Metropolis.plotLeaderboards.get(plot);
            String type;
            if (leaderboard.getType().equalsIgnoreCase("place") || leaderboard.getType().equalsIgnoreCase("break")) {
                type = Metropolis.getMessage("messages.plot.scoreboard.leaderboardType.blocks");
            } else {
                type = Metropolis.getMessage("messages.plot.scoreboard.leaderboardType.mobs");
            }

            Metropolis.sendMessage(player, "messages.plot.leaderboard.list.header", "%start%", String.valueOf(start + 1), "%end%", String.valueOf(((int) Math.ceil(((double) standings.size()) / ((double) itemsPerPage)))), "%next%", String.valueOf(page + 1), "%previous%", String.valueOf(page - 1));
            for (int i = start; i < end; i++) {
                Standing standing = standings.get(i);
                if (standing == null) {
                    continue;
                }
                MPlayer mPlayer = ApiedAPI.getPlayer(standing.getPlayerUUID());
                if (mPlayer == null) {
                    continue;
                }
                Metropolis.sendMessage(player, "messages.plot.leaderboard.list.entry", "%position%", String.valueOf(i+1), "%player%", mPlayer.getName(), "%count%", Utilities.formattedMoney(standing.getCount()), "%type%", type);
            }
        }

        @Subcommand("info")
        public void onInfo(Player player) {
            if (!player.hasPermission("metropolis.plot.leaderboard.info")) {
                Metropolis.sendAccessDenied(player);
                return;
            }
            Plot plot = Metropolis.playerInPlot.get(player.getUniqueId());
            if (plot == null) {
                Metropolis.sendMessage(player, "messages.error.plot.notInPlot");
                return;
            }
            if (!plot.isHasLeaderboard()) {
                Metropolis.sendMessage(player, "messages.error.plot.leaderboard.notStarted");
                return;
            }
            Leaderboard leaderboard = Metropolis.plotLeaderboards.get(plot);
            MPlayer mPlayer = ApiedAPI.getPlayer(leaderboard.getCreatorUUID());
            if (mPlayer == null) {
                return;
            }
            Metropolis.sendMessage(player, "messages.plot.leaderboard.info", "%type%", leaderboard.getType(),"%creation%", DateUtil.niceDate(leaderboard.getCreateDate()), "%player%", mPlayer.getName(), "%count%", Utilities.formattedMoney(PlotDatabase.getTotalStanding(plot)), "%entries%", String.valueOf(PlotDatabase.getStandingEntries(plot)));
        }

        @Subcommand("stop")
        public void onStop(Player player) {
            Plot plot = Utilities.hasPlotPermissions(player, "metropolis.plot.leaderboard.stop", Role.ASSISTANT, true);
            if (plot == null) {
                return;
            }
            if (plot.getCity().isReserve()) {
                Metropolis.sendMessage(player, "messages.error.city.reserve");
                return;
            }
            if (!plot.isHasLeaderboard()) {
                Metropolis.sendMessage(player, "messages.error.plot.leaderboard.notStarted");
                return;
            }
            PlotDatabase.removeLeaderboard(plot.getPlotId());
            plot.setLeaderboard(false);
            plot.setLeaderboardShown(false);
            Metropolis.plotStandings.remove(plot);
            Metropolis.plotLeaderboards.remove(plot);
            for (Player plotPlayer : plot.playersInPlot()) {
                Utilities.sendCityScoreboard(plotPlayer, plot.getCity(), plot);
            }
            Metropolis.sendMessage(player, "messages.plot.leaderboard.stopped");
        }

        @Subcommand("condition list")
        public void onCondition(Player player) {
            if (!player.hasPermission("metropolis.plot.leaderboard.condition.list")) {
                Metropolis.sendAccessDenied(player);
                return;
            }
            Plot plot = Metropolis.playerInPlot.get(player.getUniqueId());
            if (plot == null) {
                Metropolis.sendMessage(player, "messages.error.plot.notInPlot");
                return;
            }
            if (plot.getCity().isReserve()) {
                Metropolis.sendMessage(player, "messages.error.city.reserve");
                return;
            }
            if (!plot.isHasLeaderboard()) {
                Metropolis.sendMessage(player, "messages.error.plot.leaderboard.notStarted");
                return;
            }
            Leaderboard leaderboard = Metropolis.plotLeaderboards.get(plot);
            if (leaderboard.getConditions() == null) {
                Metropolis.sendMessage(player, "messages.error.plot.leaderboard.noConditions");
                return;
            }
            boolean isBlock = leaderboard.getType().equalsIgnoreCase("place") || leaderboard.getType().equalsIgnoreCase("break");
            String[] conditions = leaderboard.getConditions();
            List<String> convertedConditions = new ArrayList<>();

            if (isBlock) {
                for (String condition : conditions) {
                    try {
                        convertedConditions.add(Material.valueOf(condition).getKey().getKey());
                    } catch (IllegalArgumentException ignored) {

                    }
                }
            } else if (leaderboard.getType().equalsIgnoreCase("mobs")) {
                for (String condition : conditions) {
                    try {
                        convertedConditions.add(EntityType.valueOf(condition).getKey().getKey());
                    } catch (IllegalArgumentException ignored) {

                    }
                }
            }

            Metropolis.sendMessage(player, "messages.plot.leaderboard.condition.list", "%conditions%", Utilities.listConditions(convertedConditions, isBlock));
        }

        @Subcommand("condition add")
        @CommandCompletion("@leaderboardAConditions")
        public void onConditionAdd(Player player, String condition) {
            Plot plot = Utilities.hasPlotPermissions(player, "metropolis.plot.leaderboard.condition.add", Role.ASSISTANT, true);
            if (plot == null) {
                return;
            }
            if (plot.getCity().isReserve()) {
                Metropolis.sendMessage(player, "messages.error.city.reserve");
                return;
            }
            if (!plot.isHasLeaderboard()) {
                Metropolis.sendMessage(player, "messages.error.plot.leaderboard.notStarted");
                return;
            }

            Leaderboard leaderboard = Metropolis.plotLeaderboards.get(plot);
            List<String> conditions = new ArrayList<>(Arrays.asList(leaderboard.getConditions()));
            boolean isBlock = leaderboard.getType().equalsIgnoreCase("place") || leaderboard.getType().equalsIgnoreCase("break");
            if (conditions.contains(condition)) {
                Metropolis.sendMessage(player, "messages.error.plot.leaderboard.condition.alreadyExists");
                return;
            }

            List<String> blockFilter = Metropolis.configuration.getLeaderboardBlockFilter();
            List<String> mobFilter = Metropolis.configuration.getLeaderboardMobFilter();

            if (blockFilter.contains(condition) &&  isBlock) {
                Metropolis.sendMessage(player, "messages.error.plot.leaderboard.condition.blacklisted");
                return;
            } else if (mobFilter.contains(condition) && !isBlock) {
                Metropolis.sendMessage(player, "messages.error.plot.leaderboard.condition.blacklisted");
                return;

            }

            boolean isValidBlock = Arrays.stream(Material.values()).filter(Material::isBlock).map(Material::name).anyMatch(condition::equalsIgnoreCase);
            boolean isValidMob = Arrays.stream(EntityType.values()).filter(EntityType::isAlive).map(EntityType::name).anyMatch(condition::equalsIgnoreCase);

            if (!isValidBlock && isBlock) {
                Metropolis.sendMessage(player, "messages.error.plot.leaderboard.condition.invalid");
                return;
            } else if (!isValidMob && !isBlock) {
                Metropolis.sendMessage(player, "messages.error.plot.leaderboard.condition.invalid");
                return;
            }

            leaderboard.addCondition(condition);
            PlotDatabase.updateLeaderboard(leaderboard, plot.getPlotId());
            if (isBlock) {
                Material material = Material.valueOf(condition);
                Metropolis.sendMessage(player, "messages.plot.leaderboard.condition.addedBlock", "%block%", material.getKey().getKey());
            } else {
                EntityType entityType = EntityType.valueOf(condition);
                Metropolis.sendMessage(player, "messages.plot.leaderboard.condition.addedMob", "%mob%", entityType.getKey().getKey());
            }
        }

        @Subcommand("condition remove")
        @CommandCompletion("@leaderboardCConditions")
        public void onConditionRemove(Player player, String condition) {
            Plot plot = Utilities.hasPlotPermissions(player, "metropolis.plot.leaderboard.condition.remove", Role.ASSISTANT, true);
            if (plot == null) {
                return;
            }
            if (plot.getCity().isReserve()) {
                Metropolis.sendMessage(player, "messages.error.city.reserve");
                return;
            }
            if (!plot.isHasLeaderboard()) {
                Metropolis.sendMessage(player, "messages.error.plot.leaderboard.notStarted");
                return;
            }

            Leaderboard leaderboard = Metropolis.plotLeaderboards.get(plot);
            List<String> conditions = new ArrayList<>(Arrays.asList(leaderboard.getConditions()));

            if (!conditions.contains(condition)) {
                Metropolis.sendMessage(player, "messages.error.plot.leaderboard.condition.notExists");
                return;
            }

            leaderboard.removeCondition(condition);
            PlotDatabase.updateLeaderboard(leaderboard, plot.getPlotId());
            if (leaderboard.getType().equalsIgnoreCase("place") || leaderboard.getType().equalsIgnoreCase("break")) {
                Material material = Material.valueOf(condition);
                Metropolis.sendMessage(player, "messages.plot.leaderboard.condition.removedBlock", "%block%", material.getKey().getKey());
            } else {
                EntityType entityType = EntityType.valueOf(condition);
                Metropolis.sendMessage(player, "messages.plot.leaderboard.condition.removedMob", "%mob%", entityType.getKey().getKey());
            }
        }
    }
}
