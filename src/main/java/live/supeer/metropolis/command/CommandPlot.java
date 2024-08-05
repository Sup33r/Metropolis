package live.supeer.metropolis.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import live.supeer.metropolis.Database;
import live.supeer.metropolis.Metropolis;
import live.supeer.metropolis.MetropolisListener;
import live.supeer.metropolis.city.Role;
import live.supeer.metropolis.utils.Utilities;
import live.supeer.metropolis.city.City;
import live.supeer.metropolis.city.CityDatabase;
import live.supeer.metropolis.homecity.HCDatabase;
import live.supeer.metropolis.plot.Plot;
import live.supeer.metropolis.plot.PlotDatabase;
import live.supeer.metropolis.plot.PlotPerms;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@CommandAlias("plot")
public class CommandPlot extends BaseCommand {
    public static Metropolis plugin;
    private static final GeometryFactory geometryFactory = new GeometryFactory();


    @Default
    @CatchUnknown
    public static void onPlot(Player player) {
        if (!player.hasPermission("metropolis.plot")) {
            plugin.sendMessage(player, "messages.error.permissionDenied");
            return;
        }
        plugin.sendMessage(player, "messages.plot.help.header");
        plugin.sendMessage(player, "messages.plot.help.buy");
        plugin.sendMessage(player, "messages.plot.help.leave");
        plugin.sendMessage(player, "messages.plot.help.market");
        plugin.sendMessage(player, "messages.plot.help.perm");
        plugin.sendMessage(player, "messages.plot.help.set.rent");
        plugin.sendMessage(player, "messages.plot.help.set.name");
        plugin.sendMessage(player, "messages.plot.help.set.type");
        plugin.sendMessage(player, "messages.plot.help.set.owner");
        plugin.sendMessage(player, "messages.plot.help.share");
        plugin.sendMessage(player, "messages.plot.help.toggle.animals");
        plugin.sendMessage(player, "messages.plot.help.toggle.mobs");
        plugin.sendMessage(player, "messages.plot.help.toggle.meeting");
        plugin.sendMessage(player, "messages.plot.help.toggle.k");
        plugin.sendMessage(player, "messages.plot.help.type.church");
        plugin.sendMessage(player, "messages.plot.help.type.farm");
        plugin.sendMessage(player, "messages.plot.help.type.shop");
        plugin.sendMessage(player, "messages.plot.help.type.vacation");
        plugin.sendMessage(player, "messages.plot.help.type.none");
        plugin.sendMessage(player, "messages.plot.help.update");
    }

    @Subcommand("new")
    public static void onNew(Player player, @Optional String plotname) {
        City city = Utilities.hasCityPermissions(player, "metropolis.plot.new", Role.ASSISTANT);
        if (city == null) {
            return;
        }
        if (!MetropolisListener.playerPolygons.containsKey(player.getUniqueId())) {
            plugin.sendMessage(player, "messages.error.missing.plot");
            return;
        }
        Polygon regionPolygon = MetropolisListener.playerPolygons.get(player.getUniqueId());
        Location[] locations =
                MetropolisListener.savedLocs.get(player.getUniqueId()).toArray(new Location[0]);
        double minX = regionPolygon.getEnvelopeInternal().getMinX();
        double maxX = regionPolygon.getEnvelopeInternal().getMaxX();
        double minY = regionPolygon.getEnvelopeInternal().getMinY();
        double maxY = regionPolygon.getEnvelopeInternal().getMaxY();

        if (maxX - minX < 3 || maxY - minY < 3) {
            plugin.sendMessage(player, "messages.error.plot.tooSmall");
            return;
        }
        if (MetropolisListener.playerYMax.get(player.getUniqueId()) - MetropolisListener.playerYMin.get(player.getUniqueId()) < 3 || !MetropolisListener.playerYMin.containsKey(player.getUniqueId()) || !MetropolisListener.playerYMax.containsKey(player.getUniqueId())) {
            plugin.sendMessage(player, "messages.error.plot.tooLowY");
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
                    if (CityDatabase.getClaim(new Location(player.getWorld(), x, 0, z)) == null || !Objects.equals(Objects.requireNonNull(CityDatabase.getClaim(new Location(player.getWorld(), x, 0, z))).getCityName(), HCDatabase.getHomeCityToCityname(player.getUniqueId().toString()))) {
                        plugin.sendMessage(player, "messages.error.plot.intersectsExistingClaim");
                        return;
                    }
                    if (PlotDatabase.intersectsExistingPlot(
                            regionPolygon,
                            MetropolisListener.playerYMin.get(player.getUniqueId()),
                            MetropolisListener.playerYMax.get(player.getUniqueId()),
                            city)) {
                        plugin.sendMessage(player, "messages.error.plot.intersectsExistingPlot");
                        return;
                    }
                    Plot plot =
                            PlotDatabase.createPlot(
                                    player,
                                    locations,
                                    plotname,
                                    city,
                                    MetropolisListener.playerYMin.get(player.getUniqueId()),
                                    MetropolisListener.playerYMax.get(player.getUniqueId()));
                    assert plot != null;
                    Database.addLogEntry(
                            city,
                            "{ \"type\": \"create\", \"subtype\": \"plot\", \"id\": "
                                    + plot.getPlotID()
                                    + ", \"name\": "
                                    + plotname
                                    + ", \"points\": "
                                    + Utilities.parsePoints(locations)
                                    + ", \"ymin\": "
                                    + MetropolisListener.playerYMin.get(player.getUniqueId())
                                    + ", \"ymax\": "
                                    + MetropolisListener.playerYMax.get(player.getUniqueId())
                                    + ", \"player\": "
                                    + player.getUniqueId().toString()
                                    + " }");
                    MetropolisListener.savedLocs.remove(player.getUniqueId());
                    MetropolisListener.playerPolygons.remove(player.getUniqueId());
                    MetropolisListener.playerYMin.remove(player.getUniqueId());
                    MetropolisListener.playerYMax.remove(player.getUniqueId());
                    plugin.sendMessage(
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
            plugin.sendMessage(player, "messages.error.permissionDenied");
            return;
        }
        if (!MetropolisListener.playerPolygons.containsKey(player.getUniqueId())) {
            plugin.sendMessage(player, "messages.error.missing.plot");
            return;
        }

        if (args.length == 0) {
            MetropolisListener.playerYMin.put(player.getUniqueId(), -64);
            MetropolisListener.playerYMax.put(player.getUniqueId(), 319);
            plugin.sendMessage(player, "messages.plot.set.plot.expand.max");
            return;
        }
        if (args.length > 2) {
            plugin.sendMessage(player, "messages.syntax.plot.expand");
            return;
        }
        if (args.length == 1) {
            if (args[0].equals("up")) {
                MetropolisListener.playerYMax.put(player.getUniqueId(), 319);
                plugin.sendMessage(player, "messages.plot.set.plot.expand.up.max");
                return;
            } else if (args[0].equals("down")) {
                MetropolisListener.playerYMin.put(player.getUniqueId(), -64);
                plugin.sendMessage(player, "messages.plot.set.plot.expand.down.max");
                return;
            } else {
                plugin.sendMessage(player, "messages.syntax.plot.expand");
                return;
            }
        }
        if (args[0].matches("[0-9]+")) {
            if (Integer.parseInt(args[0]) == 0) {
                plugin.sendMessage(player, "messages.error.plot.expand.invalidHeight");
                return;
            }
            if (args[1].equals("up")) {
                if (MetropolisListener.playerYMax.get(player.getUniqueId()) + Integer.parseInt(args[0])
                        > 319) {
                    plugin.sendMessage(player, "messages.error.plot.tooHighExpand");
                    return;
                }
                MetropolisListener.playerYMax.put(
                        player.getUniqueId(),
                        MetropolisListener.playerYMax.get(player.getUniqueId()) + Integer.parseInt(args[0]));
                plugin.sendMessage(player, "messages.plot.set.plot.expand.up.amount", "%amount%", args[0]);
            } else if (args[1].equals("down")) {
                if (MetropolisListener.playerYMin.get(player.getUniqueId()) - Integer.parseInt(args[0])
                        < -64) {
                    plugin.sendMessage(player, "messages.error.plot.tooLowExpand");
                    return;
                }
                MetropolisListener.playerYMin.put(
                        player.getUniqueId(),
                        MetropolisListener.playerYMin.get(player.getUniqueId()) - Integer.parseInt(args[0]));
                plugin.sendMessage(
                        player, "messages.plot.set.plot.expand.down.amount", "%amount%", args[0]);
            } else {
                plugin.sendMessage(player, "messages.syntax.plot.expand");
            }
        } else {
            plugin.sendMessage(player, "messages.syntax.plot.expand");
        }
    }

    @Subcommand("delete")
    public static void onDelete(Player player) {
        Plot plot = Utilities.hasPlotPermissions(player, "metropolis.plot.delete", Role.ASSISTANT, false);
        if (plot == null) {
            return;
        }
        Database.addLogEntry(plot.getCity(),
                "{ \"type\": \"delete\", \"subtype\": \"plot\", \"id\": "
                        + plot.getPlotID()
                        + ", \"name\": "
                        + plot.getPlotName()
                        + ", \"player\": "
                        + player.getUniqueId().toString()
                        + " }");
        PlotDatabase.deletePlot(plot);
        plugin.sendMessage(
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
        plot.removePlotOwner();
        Database.addLogEntry(
                plot.getCity(),
                "{ \"type\": \"leave\", \"subtype\": \"plot\", \"id\": "
                        + plot.getPlotID()
                        + ", \"name\": "
                        + plot.getPlotName()
                        + ", \"player\": "
                        + player.getUniqueId().toString()
                        + " }");
        plugin.sendMessage(
                player,
                "messages.city.successful.set.plot.leave",
                "%cityname%",
                plot.getCity().getCityName(),
                "%plotname%",
                plot.getPlotName());
    }

    @Subcommand("market")
    public static void onMarket(Player player, String arg) {
        Plot plot = Utilities.hasPlotPermissions(player, "metropolis.plot.market", Role.INVITER, true);
        if (plot == null) {
            return;
        }
        if (arg.equals("-")) {
            plot.setForSale(false);
            plugin.sendMessage(
                    player,
                    "messages.city.successful.set.plot.market.remove",
                    "%cityname%",
                    plot.getCity().getCityName(),
                    "%plotname%",
                    plot.getPlotName());
            Database.addLogEntry(
                    plot.getCity(),
                    "{ \"type\": \"plotMarket\", \"subtype\": \"remove\", \"id\": "
                            + plot.getPlotID()
                            + ", \"name\": "
                            + plot.getPlotName()
                            + ", \"player\": "
                            + player.getUniqueId().toString()
                            + " }");
            return;
        }
        if (arg.matches("[0-9]+")) {
            if (plot.isForSale()) {
                if (plot.getPlotPrice() == Integer.parseInt(arg)) {
                    plugin.sendMessage(
                            player,
                            "messages.error.plot.market.noChange",
                            "%cityname%",
                            plot.getCity().getCityName());
                    return;
                }
                plugin.sendMessage(
                        player,
                        "messages.city.successful.set.plot.market.change",
                        "%cityname%",
                        plot.getCity().getCityName(),
                        "%plotname%",
                        plot.getPlotName(),
                        "%from%",
                        Utilities.formattedMoney(plot.getPlotPrice()),
                        "%to%",
                        Utilities.formattedMoney(Integer.valueOf(arg)));
                Database.addLogEntry(
                        plot.getCity(),
                        "{ \"type\": \"plotMarket\", \"subtype\": \"change\", \"id\": "
                                + plot.getPlotID()
                                + ", \"name\": "
                                + plot.getPlotName()
                                + ", \"from\": "
                                + plot.getPlotPrice()
                                + ", \"to\": "
                                + arg
                                + ", \"player\": "
                                + player.getUniqueId().toString()
                                + " }");
                plot.setPlotPrice(Integer.parseInt(arg));
                return;
            }
            plot.setForSale(true);
            if (plot.getPlotPrice() == Integer.parseInt(arg)) {
                plugin.sendMessage(
                        player,
                        "messages.error.plot.market.noChange",
                        "%cityname%",
                        plot.getCity().getCityName());
                return;
            }
            plot.setPlotPrice(Integer.parseInt(arg));
            Database.addLogEntry(
                    plot.getCity(),
                    "{ \"type\": \"plotMarket\", \"subtype\": \"add\", \"id\": "
                            + plot.getPlotID()
                            + ", \"name\": "
                            + plot.getPlotName()
                            + ", \"to\": "
                            + arg
                            + ", \"player\": "
                            + player.getUniqueId().toString()
                            + " }");
            plugin.sendMessage(
                    player,
                    "messages.city.successful.set.plot.market.set",
                    "%cityname%",
                    plot.getCity().getCityName(),
                    "%plotname%",
                    plot.getPlotName(),
                    "%amount%",
                    Utilities.formattedMoney(Integer.valueOf(arg)));
            return;
        }
        plugin.sendMessage(player, "messages.syntax.plot.market");
    }

    @Subcommand("info")
    public static void onInfo(Player player) {
        if (!player.hasPermission("metropolis.plot.info")) {
            plugin.sendMessage(player, "messages.error.permissionDenied");
            return;
        }
        Plot plot = PlotDatabase.getPlotAtLocation(player.getLocation());
        if (plot == null) {
            return;
        }
        Role role = CityDatabase.getCityRole(plot.getCity(), player.getUniqueId().toString());
        List<Player> players = new ArrayList<>();
        plugin.sendMessage(player, "messages.plot.list.header", "%plot%", plot.getPlotName());
        plugin.sendMessage(
                player, "messages.plot.list.id", "%id%", String.valueOf(plot.getPlotID()));
        plugin.sendMessage(player, "messages.plot.list.city", "%cityname%", plot.getCity().getCityName());
        plugin.sendMessage(player, "messages.plot.list.owner", "%owner%", plot.getPlotOwner());
        if (Arrays.toString(plot.getPlotFlags()).contains("p")) {
            plugin.sendMessage(player, "messages.plot.list.pvp", "%status%", "§cPå");
        } else {
            plugin.sendMessage(player, "messages.plot.list.pvp", "%status%", "§aAv");
        }
        if (Arrays.toString(plot.getPlotFlags()).contains("a")) {
            plugin.sendMessage(player, "messages.plot.list.animals", "%status%", "§aPå");
        } else {
            plugin.sendMessage(player, "messages.plot.list.animals", "%status%", "§aAv");
        }
        if (Arrays.toString(plot.getPlotFlags()).contains("m")) {
            plugin.sendMessage(player, "messages.plot.list.monsters", "%status%", "§cPå");
        } else {
            plugin.sendMessage(player, "messages.plot.list.monsters", "%status%", "§aAv");
        }
        if (Arrays.toString(plot.getPlotFlags()).contains("l")) {
            plugin.sendMessage(player, "messages.plot.list.monsters", "%status%", "§aJa");
        } else {
            plugin.sendMessage(player, "messages.plot.list.monsters", "%status%", "§aNej");
        }
        if (plot.isKMarked()) {
            plugin.sendMessage(player, "messages.plot.list.k-marked", "%status%", "§aJa");
        } else {
            plugin.sendMessage(player, "messages.plot.list.k-marked", "%status%", "§aNej");
        }
        if (Arrays.toString(plot.getPlotFlags()).contains("i")) {
            plugin.sendMessage(player, "messages.plot.list.lose.items", "%status%", "§cJa");
        } else {
            plugin.sendMessage(player, "messages.plot.list.lose.items", "%status%", "§aNej");
        }
        if (Arrays.toString(plot.getPlotFlags()).contains("x")) {
            plugin.sendMessage(player, "messages.plot.list.lose.xp", "%status%", "§cJa");
        } else {
            plugin.sendMessage(player, "messages.plot.list.lose.xp", "%status%", "§aNej");
        }
        if (player.hasPermission("metropolis.plot.info.coordinates")) {
            plugin.sendMessage(
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
            plugin.sendMessage(
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
        for (Player p : Bukkit.getOnlinePlayers()) {
            Polygon polygon = Utilities.createPolygonFromLocations(plot.getPlotPoints(), geometryFactory);
            Point point = geometryFactory.createPoint(new Coordinate(p.getLocation().getBlockX(), p.getLocation().getBlockZ()));
            if (polygon.contains(point)) {
                if (plot.getPlotID() == plot.getPlotID()) {
                    if (!players.contains(p)) {
                        players.add(p);
                    }
                }
            }
        }
        if (!players.isEmpty()) {
            StringBuilder stringBuilder = new StringBuilder();
            for (Player p : players) {
                // dont append last player
                if (players.indexOf(p) == players.size() - 1)
                    stringBuilder.append(p.getName()).append("§2");
                else stringBuilder.append(p.getName()).append("§2,§a ");
            }
            plugin.sendMessage(
                    player,
                    "messages.plot.list.players",
                    "%players%",
                    stringBuilder.substring(0, stringBuilder.toString().length() - 2));
        }
    }

    @Subcommand("player")
    public static void onPlayer(Player player, String playerName) {
        if (!player.hasPermission("metropolis.plot.player")) {
            plugin.sendMessage(player, "messages.error.permissionDenied");
            return;
        }
        @Deprecated Player p = Bukkit.getOfflinePlayer(playerName).getPlayer();
        if (p == null) {
            plugin.sendMessage(player, "messages.error.player.notFound");
            return;
        }
        if (CityDatabase.memberCityList(p.getUniqueId().toString()) == null
                || Objects.requireNonNull(CityDatabase.memberCityList(p.getUniqueId().toString())).length
                == 0) {
            plugin.sendMessage(player, "messages.error.city.notInCity");
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (String c :
                Objects.requireNonNull(CityDatabase.memberCityList(p.getUniqueId().toString()))) {
            if (CityDatabase.getCity(c).isEmpty()) {
                return;
            }
            City city = CityDatabase.getCity(c).get();
            for (Plot plot : city.getCityPlots()) {
                if (plot.getPlotOwner().equals(p.getName())) {
                    stringBuilder.append("§a").append(plot.getPlotName()).append("§2,§a ");
                }
            }
            if (stringBuilder.toString().isEmpty()) {
                return;
            }
            player.sendMessage(
                    "§a§l"
                            + city.getCityName()
                            + "§2: §a"
                            + stringBuilder.substring(0, stringBuilder.toString().length()));
        }
    }

    @Subcommand("tp")
    public static void onTp(Player player, String plotID) {
        if (!player.hasPermission("metropolis.plot.tp")) {
            plugin.sendMessage(player, "messages.error.permissionDenied");
            return;
        }
        // For the eventual time when i will add the syntax stuff
        // plugin.sendMessage(player, "messages.syntax.plot.tp");
        if (!plotID.matches("[0-9]")) {
            plugin.sendMessage(player, "messages.syntax.plot.tp");
            return;
        }
        if (!PlotDatabase.plotExists(Integer.parseInt(plotID))) {
            plugin.sendMessage(player, "messages.error.plot.notFound");
            return;
        }
        Plot plot = PlotDatabase.getPlot(Integer.parseInt(plotID));
        assert plot != null;
        if (CityDatabase.getCity(plot.getCityID()).isEmpty()) {
            plugin.sendMessage(player, "messages.error.city.missing.city");
            return;
        }
        City city = CityDatabase.getCity(plot.getCityID()).get();
        plugin.sendMessage(
                player,
                "messages.city.successful.set.plot.tp",
                "%plotname%",
                plot.getPlotName(),
                "%cityname%",
                city.getCityName());
        player.teleport(plot.getPlotCenter());
    }

    @Subcommand("perm")
    public static void onPerm(Player player, String[] args) throws SQLException {
        City city = Utilities.hasCityPermissions(player, "metropolis.plot.perm", null);
        if (city == null) {
            return;
        }

        Plot plot = PlotDatabase.getPlotAtLocation(player.getLocation());
        if (plot == null) {
            return;
        }
        Role role = CityDatabase.getCityRole(city, player.getUniqueId().toString());
        if (args == null || args.length == 0) {
            StringBuilder stringBuilderOutsiders = new StringBuilder();
            for (char s : plot.getPermsOutsiders()) {
                stringBuilderOutsiders.append(s);
            }
            String permsOutsiders =
                    "+"
                            + stringBuilderOutsiders.substring(
                            0, stringBuilderOutsiders.toString().length());
            if (stringBuilderOutsiders
                    .substring(0, stringBuilderOutsiders.toString().length())
                    .equals(" ")
                    || stringBuilderOutsiders
                    .substring(0, stringBuilderOutsiders.toString().length())
                    .isEmpty()
                    || stringBuilderOutsiders.substring(0, stringBuilderOutsiders.toString().length())
                    == null) {
                permsOutsiders = "§onada";
            }
            StringBuilder stringBuilderMembers = new StringBuilder();
            for (char s : plot.getPermsMembers()) {
                stringBuilderMembers.append(s);
            }
            String permsMembers =
                    "+" + stringBuilderMembers.substring(0, stringBuilderMembers.toString().length());
            if (stringBuilderOutsiders
                    .substring(0, stringBuilderOutsiders.toString().length())
                    .equals(" ")
                    || stringBuilderOutsiders
                    .substring(0, stringBuilderOutsiders.toString().length())
                    .isEmpty()
                    || stringBuilderOutsiders.substring(0, stringBuilderOutsiders.toString().length())
                    == null) {
                permsMembers = "§onada";
            }
            plugin.sendMessage(
                    player, "messages.plot.list.perm.header", "%plot%", plot.getPlotName());
            plugin.sendMessage(
                    player, "messages.plot.list.perm.outsiders", "%perms%", permsOutsiders);
            plugin.sendMessage(player, "messages.plot.list.perm.members", "%perms%", permsMembers);

            for (PlotPerms plotPerms : plot.getPlayerPlotPerms()) {
                StringBuilder stringBuilder = new StringBuilder();
                for (char s : plotPerms.getPerms()) {
                    stringBuilder.append(s);
                }
                String perms = "+" + stringBuilder.substring(0, stringBuilder.toString().length());
                if (plotPerms.getPerms() == null || plotPerms.getPerms().length == 0) {
                    return;
                }
                plugin.sendMessage(
                        player,
                        "messages.plot.list.perm.players",
                        "%player%",
                        plotPerms.getPlayerName(),
                        "%perms%",
                        perms);
            }
            plugin.sendMessage(player, "messages.plot.list.perm.permsrow.1");
            plugin.sendMessage(player, "messages.plot.list.perm.permsrow.2");
            plugin.sendMessage(player, "messages.plot.list.perm.permsrow.3");
            return;
        }
        if (args.length > 2) {
            plugin.sendMessage(player, "messages.syntax.plot.perm");
            return;
        }
        if (args.length == 1) {
            if (args[0].equals("-")) {
                if (plot.getPlotOwnerUUID().equals(player.getUniqueId().toString()) || !Objects.equals(role, Role.MAYOR) || !Objects.equals(role, Role.ASSISTANT) || !Objects.equals(role, Role.VICE_MAYOR)) {
                    if (plot.isKMarked() && !Objects.equals(role, Role.MAYOR)) {
                        plugin.sendMessage(
                                player,
                                "messages.error.city.permissionDenied",
                                "%cityname%",
                                city.getCityName());
                        return;
                    }
                    Database.addLogEntry(
                            city,
                            "{ \"type\": \"plot\", \"subtype\": \"perm\", \"id\": "
                                    + plot.getPlotID()
                                    + ", \"name\": "
                                    + plot.getPlotName()
                                    + ", \"type\": "
                                    + "all"
                                    + ", \"from\": "
                                    + ""
                                    + ", \"to\": "
                                    + ""
                                    + ", \"player\": "
                                    + player.getUniqueId().toString()
                                    + " }");
                    plot.removePlotPerms();
                    plugin.sendMessage(
                            player,
                            "messages.city.successful.set.plot.perm.remove.all",
                            "%cityname%",
                            city.getCityName());
                    return;
                } else {
                    plugin.sendMessage(
                            player, "messages.error.city.permissionDenied", "%cityname%", city.getCityName());
                    return;
                }
            }
            plugin.sendMessage(player, "messages.syntax.plot.perm");
            return;
        }
        if (!args[0].equals("members") && !args[0].equals("outsiders") && !args[0].equals("-")) {
            @Deprecated OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(args[0]);
            if (offlinePlayer == null) {
                plugin.sendMessage(player, "messages.error.player.notFound");
                return;
            }
        }
        if (CityDatabase.getClaim(player.getLocation()) == null) {
            plugin.sendMessage(player, "messages.error.plot.notFound");
            return;
        }
        if (role == null) {
            plugin.sendMessage(
                    player, "messages.error.city.permissionDenied", "%cityname%", city.getCityName());
            return;
        }
        if (plot.getPlotOwnerUUID().equals(player.getUniqueId().toString()) || !Objects.equals(role, Role.MAYOR) || !Objects.equals(role, Role.ASSISTANT) || !Objects.equals(role, Role.VICE_MAYOR)) {
            if (plot.isKMarked() && !Objects.equals(role, Role.MAYOR)) {
                plugin.sendMessage(
                        player,
                        "messages.error.city.permissionDenied",
                        "%cityname%",
                        city.getCityName());
                return;
            }
            if (args[0].equals("members")) {
                if (Utilities.parsePermChange(plot.getPermsMembers(), args[1], player, "plot") == null) {
                    return;
                }
                Database.addLogEntry(
                        city,
                        "{ \"type\": \"plot\", \"subtype\": \"perm\", \"id\": "
                                + plot.getPlotID()
                                + ", \"name\": "
                                + plot.getPlotName()
                                + ", \"type\": "
                                + "members"
                                + ", \"from\": "
                                + Arrays.toString(plot.getPermsMembers())
                                + ", \"to\": "
                                + Utilities.parsePermChange(plot.getPermsMembers(), args[1], player, "plot")
                                + ", \"issuer\": "
                                + player.getUniqueId().toString()
                                + " }");
                plot.setPlotPerms(
                        "members",
                        Utilities.parsePermChange(plot.getPermsMembers(), args[1], player, "plot"),
                        null);
                plugin.sendMessage(
                        player,
                        "messages.city.successful.set.plot.perm.change.members",
                        "%perms%",
                        args[1],
                        "%cityname%",
                        city.getCityName());
                return;
            }

            if (args[0].equals("outsiders")) {
                if (Utilities.parsePermChange(plot.getPermsOutsiders(), args[1], player, "plot") == null) {
                    return;
                }
                Database.addLogEntry(
                        city,
                        "{ \"type\": \"plot\", \"subtype\": \"perm\", \"id\": "
                                + plot.getPlotID()
                                + ", \"name\": "
                                + plot.getPlotName()
                                + ", \"type\": "
                                + "outsiders"
                                + ", \"from\": "
                                + Arrays.toString(plot.getPermsOutsiders())
                                + ", \"to\": "
                                + Utilities.parsePermChange(plot.getPermsOutsiders(), args[1], player, "plot")
                                + ", \"issuer\": "
                                + player.getUniqueId().toString()
                                + " }");
                plot.setPlotPerms(
                        "outsiders",
                        Utilities.parsePermChange(plot.getPermsOutsiders(), args[1], player, "plot"),
                        null);
                plugin.sendMessage(
                        player,
                        "messages.city.successful.set.plot.perm.change.outsiders",
                        "%perms%",
                        args[1],
                        "%cityname%",
                        city.getCityName());
                return;
            }

            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(args[0]);
            if (!offlinePlayer.hasPlayedBefore()) {
                plugin.sendMessage(player, "messages.error.player.notFound");
                return;
            }

            if (plot.getPlayerPlotPerm(offlinePlayer.getUniqueId().toString()) == null
                    || plot.getPlayerPlotPerm(offlinePlayer.getUniqueId().toString()).getPerms()
                    == null) {
                plot.setPlotPerms(
                        "players",
                        Utilities.parsePerms(args[1], "plot", player),
                        offlinePlayer.getUniqueId().toString());
            } else {
                Database.addLogEntry(
                        city,
                        "{ \"type\": \"plot\", \"subtype\": \"perm\", \"id\": "
                                + plot.getPlotID()
                                + ", \"name\": "
                                + plot.getPlotName()
                                + ", \"type\": "
                                + "player"
                                + ", \"from\": "
                                + Arrays.toString(
                                plot.getPlayerPlotPerm(offlinePlayer.getUniqueId().toString()).getPerms())
                                + ", \"to\": "
                                + Utilities.parsePermChange(
                                plot.getPlayerPlotPerm(offlinePlayer.getUniqueId().toString()).getPerms(),
                                args[1],
                                player,
                                "plot")
                                + ", \"issuer\": "
                                + player.getUniqueId().toString()
                                + ", \"player\": "
                                + offlinePlayer.getUniqueId().toString()
                                + " }");
                plot.setPlotPerms(
                        "players",
                        Utilities.parsePermChange(
                                plot.getPlayerPlotPerm(offlinePlayer.getUniqueId().toString()).getPerms(),
                                args[1],
                                player,
                                "plot"),
                        offlinePlayer.getUniqueId().toString());
            }
            plugin.sendMessage(
                    player,
                    "messages.successful.set.plot.perm.change.player",
                    "%player%",
                    offlinePlayer.getName(),
                    "%perms%",
                    args[1],
                    "%cityname%",
                    city.getCityName());
        } else {
            plugin.sendMessage(
                    player, "messages.error.city.permissionDenied", "%cityname%", city.getCityName());
        }
    }

    @Subcommand("set")
    public static void onSet(Player player) {
        plugin.sendMessage(player, "messages.syntax.plot.set.typeAdmin");
        plugin.sendMessage(player, "messages.syntax.plot.set.set");
    }

    @Subcommand("set")
    public class Set extends BaseCommand {

        @Subcommand("owner")
        @CommandCompletion("@players")
        public static void onOwner(Player player, String playerName) {
            Plot plot = Utilities.hasPlotPermissions(player, "metropolis.plot.set.owner", Role.INVITER, true);
            if (plot == null) {
                return;
            }
            boolean isMayor = Objects.equals(CityDatabase.getCityRole(plot.getCity(), player.getUniqueId().toString()), Role.MAYOR);
            if (plot.isKMarked() && isMayor) {
                plugin.sendMessage(
                        player, "messages.error.city.permissionDenied", "%cityname%", plot.getCity().getCityName());
                return;
            }
            if (playerName.equals("-")) {
                if (plot.getPlotOwner() == null) {
                    plugin.sendMessage(
                            player,
                            "messages.error.plot.set.owner.alreadyNoOwner",
                            "%cityname%",
                            plot.getCity().getCityName());
                    return;
                }
                plot.setPlotOwner(null);
                Utilities.sendCityScoreboard(player, plot.getCity(), plot);
                plugin.sendMessage(
                        player, "messages.plot.set.owner.removed", "%cityname%", plot.getCity().getCityName());
                return;
            }
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
            if (!offlinePlayer.hasPlayedBefore()) {
                plugin.sendMessage(player, "messages.error.player.notFound");
                return;
            }
            if (!Arrays.stream(
                            Objects.requireNonNull(
                                    CityDatabase.memberCityList(offlinePlayer.getUniqueId().toString())))
                    .toList()
                    .contains(plot.getCity().getCityName())) {
                if (!plot.getPlotType().equals("vacation")) {
                    plugin.sendMessage(
                            player,
                            "messages.error.plot.set.owner.notInCity",
                            "%cityname%",
                            plot.getCity().getCityName());
                    return;
                }
            }
            if (plot.getPlotOwner() != null) {
                if (plot.getPlotOwner().equals(offlinePlayer.getName())) {
                    plugin.sendMessage(
                            player,
                            "messages.error.plot.set.owner.alreadyOwner",
                            "%cityname%",
                            plot.getCity().getCityName());
                    return;
                }
            }
            plot.setPlotOwner(offlinePlayer.getName());
            plot.setPlotOwnerUUID(offlinePlayer.getUniqueId().toString());
            Utilities.sendCityScoreboard(player, plot.getCity(), plot);
            plugin.sendMessage(
                    player,
                    "messages.plot.set.owner.success",
                    "%player%",
                    offlinePlayer.getName(),
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
            Role role = CityDatabase.getCityRole(plot.getCity(), player.getUniqueId().toString());
            assert role != null;
            if (plot.isKMarked() && !role.equals(Role.MAYOR)) {
                plugin.sendMessage(
                        player, "messages.error.city.permissionDenied", "%cityname%", plot.getCity().getCityName());
                return;
            }
            switch (type) {
                case "-" -> {
                    if (!player.hasPermission("metropolis.plot.set.type.remove")) {
                        plugin.sendMessage(player, "messages.error.permissionDenied");
                        return;
                    }
                    if (plot.getPlotType() == null) {
                        plugin.sendMessage(
                                player,
                                "messages.error.plot.set.type.alreadyNoType",
                                "%cityname%",
                                plot.getCity().getCityName());
                        return;
                    }
                    Database.addLogEntry(
                            plot.getCity(),
                            "{ \"type\": \"plot\", \"subtype\": \"type\", \"id\": "
                                    + plot.getPlotID()
                                    + ", \"name\": "
                                    + plot.getPlotName()
                                    + ", \"from\": "
                                    + plot.getPlotType()
                                    + ", \"to\": "
                                    + null
                                    + ", \"issuer\": "
                                    + player.getUniqueId().toString()
                                    + " }");
                    plot.setPlotType(null);
                    Utilities.sendCityScoreboard(player, plot.getCity(), plot);
                    plugin.sendMessage(
                            player, "messages.plot.set.type.removed", "%cityname%", plot.getCity().getCityName());
                    return;
                }
                case "church" -> {
                    if (!player.hasPermission("metropolis.plot.set.type.church")) {
                        plugin.sendMessage(player, "messages.error.permissionDenied");
                        return;
                    }
                    if (plot.getPlotType() == null) {
                        Database.addLogEntry(
                                plot.getCity(),
                                "{ \"type\": \"plot\", \"subtype\": \"type\", \"id\": "
                                        + plot.getPlotID()
                                        + ", \"name\": "
                                        + plot.getPlotName()
                                        + ", \"from\": "
                                        + null
                                        + ", \"to\": "
                                        + "church"
                                        + ", \"issuer\": "
                                        + player.getUniqueId().toString()
                                        + " }");
                        plot.setPlotType("church");
                        Utilities.sendCityScoreboard(player, plot.getCity(), plot);
                        plugin.sendMessage(
                                player,
                                "messages.plot.set.type.success",
                                "%cityname%",
                                plot.getCity().getCityName(),
                                "%type%",
                                "Kyrka");
                        return;
                    }
                    if (plot.getPlotType().equals("church")) {
                        plugin.sendMessage(
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
                                    + plot.getPlotID()
                                    + ", \"name\": "
                                    + plot.getPlotName()
                                    + ", \"from\": "
                                    + plot.getPlotType()
                                    + ", \"to\": "
                                    + "church"
                                    + ", \"issuer\": "
                                    + player.getUniqueId().toString()
                                    + " }");
                    plot.setPlotType("church");
                    Utilities.sendCityScoreboard(player, plot.getCity(), plot);
                    plugin.sendMessage(
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
                        plugin.sendMessage(player, "messages.error.permissionDenied");
                        return;
                    }
                    if (plot.getPlotType() == null) {
                        Database.addLogEntry(
                                plot.getCity(),
                                "{ \"type\": \"plot\", \"subtype\": \"type\", \"id\": "
                                        + plot.getPlotID()
                                        + ", \"name\": "
                                        + plot.getPlotName()
                                        + ", \"from\": "
                                        + null
                                        + ", \"to\": "
                                        + "farm"
                                        + ", \"issuer\": "
                                        + player.getUniqueId().toString()
                                        + " }");
                        plot.setPlotType("farm");
                        Utilities.sendCityScoreboard(player, plot.getCity(), plot);
                        plugin.sendMessage(
                                player,
                                "messages.plot.set.type.success",
                                "%cityname%",
                                plot.getCity().getCityName(),
                                "%type%",
                                "Farm");
                        return;
                    }
                    if (plot.getPlotType().equals("farm")) {
                        plugin.sendMessage(
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
                                    + plot.getPlotID()
                                    + ", \"name\": "
                                    + plot.getPlotName()
                                    + ", \"from\": "
                                    + plot.getPlotType()
                                    + ", \"to\": "
                                    + "farm"
                                    + ", \"issuer\": "
                                    + player.getUniqueId().toString()
                                    + " }");
                    plot.setPlotType("farm");
                    Utilities.sendCityScoreboard(player, plot.getCity(), plot);
                    plugin.sendMessage(
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
                        plugin.sendMessage(player, "messages.error.permissionDenied");
                        return;
                    }
                    if (plot.getPlotType() == null) {
                        Database.addLogEntry(
                                plot.getCity(),
                                "{ \"type\": \"plot\", \"subtype\": \"type\", \"id\": "
                                        + plot.getPlotID()
                                        + ", \"name\": "
                                        + plot.getPlotName()
                                        + ", \"from\": "
                                        + null
                                        + ", \"to\": "
                                        + "shop"
                                        + ", \"issuer\": "
                                        + player.getUniqueId().toString()
                                        + " }");
                        plot.setPlotType("shop");
                        Utilities.sendCityScoreboard(player, plot.getCity(), plot);
                        plugin.sendMessage(
                                player,
                                "messages.plot.set.type.success",
                                "%cityname%",
                                plot.getCity().getCityName(),
                                "%type%",
                                "Affär");
                        return;
                    }
                    if (plot.getPlotType().equals("shop")) {
                        plugin.sendMessage(
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
                                    + plot.getPlotID()
                                    + ", \"name\": "
                                    + plot.getPlotName()
                                    + ", \"from\": "
                                    + plot.getPlotType()
                                    + ", \"to\": "
                                    + "shop"
                                    + ", \"issuer\": "
                                    + player.getUniqueId().toString()
                                    + " }");
                    plot.setPlotType("shop");
                    Utilities.sendCityScoreboard(player, plot.getCity(), plot);
                    plugin.sendMessage(
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
                        plugin.sendMessage(player, "messages.error.permissionDenied");
                        return;
                    }
                    if (plot.getPlotType() == null) {
                        Database.addLogEntry(
                                plot.getCity(),
                                "{ \"type\": \"plot\", \"subtype\": \"type\", \"id\": "
                                        + plot.getPlotID()
                                        + ", \"name\": "
                                        + plot.getPlotName()
                                        + ", \"from\": "
                                        + null
                                        + ", \"to\": "
                                        + "vacation"
                                        + ", \"issuer\": "
                                        + player.getUniqueId().toString()
                                        + " }");
                        plot.setPlotType("vacation");
                        Utilities.sendCityScoreboard(player, plot.getCity(), plot);
                        plugin.sendMessage(
                                player,
                                "messages.plot.set.type.success",
                                "%cityname%",
                                plot.getCity().getCityName(),
                                "%type%",
                                "Ferietomt");
                        return;
                    }
                    if (plot.getPlotType().equals("vacation")) {
                        plugin.sendMessage(
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
                                    + plot.getPlotID()
                                    + ", \"name\": "
                                    + plot.getPlotName()
                                    + ", \"from\": "
                                    + plot.getPlotType()
                                    + ", \"to\": "
                                    + "vacation"
                                    + ", \"issuer\": "
                                    + player.getUniqueId().toString()
                                    + " }");
                    plot.setPlotType("vacation");
                    Utilities.sendCityScoreboard(player, plot.getCity(), plot);
                    plugin.sendMessage(
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
                                            + plot.getPlotID()
                                            + ", \"name\": "
                                            + plot.getPlotName()
                                            + ", \"from\": "
                                            + null
                                            + ", \"to\": "
                                            + "jail"
                                            + ", \"issuer\": "
                                            + player.getUniqueId().toString()
                                            + " }");
                            plot.setPlotType("jail");
                            Utilities.sendCityScoreboard(player, plot.getCity(), plot);
                            plugin.sendMessage(
                                    player,
                                    "messages.plot.set.type.success",
                                    "%cityname%",
                                    plot.getCity().getCityName(),
                                    "%type%",
                                    "Fängelse");
                            return;
                        }
                        if (plot.getPlotType().equals("jail")) {
                            plugin.sendMessage(
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
                                        + plot.getPlotID()
                                        + ", \"name\": "
                                        + plot.getPlotName()
                                        + ", \"from\": "
                                        + plot.getPlotType()
                                        + ", \"to\": "
                                        + "jail"
                                        + ", \"issuer\": "
                                        + player.getUniqueId().toString()
                                        + " }");
                        plot.setPlotType("jail");
                        Utilities.sendCityScoreboard(player, plot.getCity(), plot);
                        plugin.sendMessage(
                                player,
                                "messages.plot.set.type.success",
                                "%cityname%",
                                plot.getCity().getCityName(),
                                "%type%",
                                "Fängelse");
                    } else {
                        plugin.sendMessage(
                                player,
                                "messages.error.city.permissionDenied",
                                "%cityname%",
                                plot.getCity().getCityName());
                    }
                    return;
                }
            }
            plugin.sendMessage(
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
            Role role = CityDatabase.getCityRole(plot.getCity(), player.getUniqueId().toString());
            assert role != null;
            boolean isMayor = role.equals(Role.MAYOR);
            if (plot.isKMarked() && !isMayor) {
                plugin.sendMessage(
                        player, "messages.error.city.permissionDenied", "%cityname%", plot.getCity().getCityName());
                return;
            }
            if (Objects.equals(plot.getPlotName(), name)) {
                plugin.sendMessage(
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
                                + plot.getPlotID()
                                + ", \"from\": "
                                + plot.getPlotName()
                                + ", \"to\": "
                                + "Tomt #"
                                + plot.getPlotID()
                                + ", \"issuer\": "
                                + player.getUniqueId().toString()
                                + " }");
                plot.setPlotName("Tomt #" + plot.getPlotID());
                Utilities.sendCityScoreboard(player, plot.getCity(), plot);
                plugin.sendMessage(
                        player,
                        "messages.plot.set.name.success",
                        "%cityname%",
                        plot.getCity().getCityName(),
                        "%plotname%",
                        name);
                return;
            }
            if (name.length() > Metropolis.configuration.getPlotNameLimit()) {
                plugin.sendMessage(
                        player, "messages.error.plot.set.name.tooLong", "%cityname%", plot.getCity().getCityName(), "%maxlength%", String.valueOf(Metropolis.configuration.getPlotNameLimit()));
                return;
            }
            Database.addLogEntry(
                    plot.getCity(),
                    "{ \"type\": \"plot\", \"subtype\": \"name\", \"id\": "
                            + plot.getPlotID()
                            + ", \"from\": "
                            + plot.getPlotName()
                            + ", \"to\": "
                            + name
                            + ", \"issuer\": "
                            + player.getUniqueId().toString()
                            + " }");
            plot.setPlotName(name);
            Utilities.sendCityScoreboard(player, plot.getCity(), plot);
            plugin.sendMessage(
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
            City city = plot.getCity();
            Role role = CityDatabase.getCityRole(city, player.getUniqueId().toString());
            assert role != null;
            boolean isMayor = role.equals(Role.MAYOR);
            if (plot.isKMarked() && !isMayor) {
                plugin.sendMessage(
                        player, "messages.error.city.permissionDenied", "%cityname%", city.getCityName());
                return;
            }
            if (rent.equals("-") || rent.equals("0")) {
                Database.addLogEntry(
                        city,
                        "{ \"type\": \"plot\", \"subtype\": \"rent\", \"id\": "
                                + plot.getPlotID()
                                + ", \"name\": "
                                + plot.getPlotName()
                                + ", \"from\": "
                                + plot.getPlotRent()
                                + ", \"to\": "
                                + 0
                                + ", \"issuer\": "
                                + player.getUniqueId().toString()
                                + " }");
                plot.setPlotRent(0);
                Utilities.sendCityScoreboard(player, city, plot);
                plugin.sendMessage(
                        player, "messages.plot.set.rent.removed", "%cityname%", city.getCityName());
                return;
            }
            if (!rent.matches("[0-9]+")) {
                plugin.sendMessage(player, "messages.syntax.plot.set.rent");
                return;
            }
            int rentInt = Integer.parseInt(rent);
            if (rentInt > 100000) {
                plugin.sendMessage(
                        player, "messages.error.plot.set.rent.tooHigh", "%cityname%", city.getCityName());
                return;
            }
            if (!plot.isForSale()) {
                plot.setPlotPrice(0);
                Utilities.sendCityScoreboard(player, city, plot);
                Database.addLogEntry(
                        city,
                        "{ \"type\": \"plotMarket\", \"subtype\": \"add\", \"id\": "
                                + plot.getPlotID()
                                + ", \"name\": "
                                + plot.getPlotName()
                                + ", \"to\": "
                                + 0
                                + ", \"player\": "
                                + player.getUniqueId().toString()
                                + " }");
                plot.setForSale(true);
            }
            Database.addLogEntry(
                    city,
                    "{ \"type\": \"plot\", \"subtype\": \"rent\", \"id\": "
                            + plot.getPlotID()
                            + ", \"name\": "
                            + plot.getPlotName()
                            + ", \"from\": "
                            + plot.getPlotRent()
                            + ", \"to\": "
                            + rentInt
                            + ", \"issuer\": "
                            + player.getUniqueId().toString()
                            + " }");
            plot.setPlotRent(rentInt);
            Utilities.sendCityScoreboard(player, city, plot);
            plugin.sendMessage(
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
            player.sendMessage("§7Syntax: /plot toggle §nkeepinv");
        }
        if (player.hasPermission("metropolis.admin.plot.toggle.keepexp")) {
            player.sendMessage("§7Syntax: /plot toggle §nkeepexp");
        }
        if (player.hasPermission("metropolis.admin.plot.toggle.pvp")) {
            player.sendMessage("§7Syntax: /plot toggle §npvp");
        }
        if (player.hasPermission("metropolis.admin.plot.toggle.lock")) {
            player.sendMessage("§7Syntax: /plot toggle §nlock");
        }
        plugin.sendMessage(player, "messages.syntax.plot.toggle");
    }

    @Subcommand("toggle")
    public class Toggle extends BaseCommand {

        @Subcommand("pvp")
        public static void onPvp(Player player) {
            if (!player.hasPermission("metropolis.admin.plot.toggle.pvp")) {
                plugin.sendMessage(player, "messages.error.permissionDenied");
                return;
            }
            Plot plot = PlotDatabase.getPlotAtLocation(player.getLocation());
            if (plot == null) {
                return;
            }
            City city = plot.getCity();
            if (plot.hasFlag('p')) {
                Database.addLogEntry(
                        city,
                        "{ \"type\": \"plot\", \"subtype\": \"toggle\", \"id\": "
                                + plot.getPlotID()
                                + ", \"name\": "
                                + plot.getPlotName()
                                + ", \"item\": "
                                + "pvp"
                                + ", \"state\": "
                                + false
                                + ", \"issuer\": "
                                + player.getUniqueId().toString()
                                + " }");
                plot.setPlotFlags(
                        Objects.requireNonNull(Utilities.parseFlagChange(plot.getPlotFlags(), "-p")));
                plugin.sendMessage(
                        player,
                        "messages.plot.toggle.pvp.success",
                        "%cityname%",
                        city.getCityName(),
                        "%status%",
                        "inaktiverat");
                return;
            }
            if (plot.getPlotFlags().length == 0) {
                Database.addLogEntry(
                        city,
                        "{ \"type\": \"plot\", \"subtype\": \"toggle\", \"id\": "
                                + plot.getPlotID()
                                + ", \"name\": "
                                + plot.getPlotName()
                                + ", \"item\": "
                                + "pvp"
                                + ", \"state\": "
                                + true
                                + ", \"issuer\": "
                                + player.getUniqueId().toString()
                                + " }");
                plot.setPlotFlags("p");
                plugin.sendMessage(
                        player,
                        "messages.plot.toggle.pvp.success",
                        "%cityname%",
                        city.getCityName(),
                        "%status%",
                        "aktiverat");
                return;
            }
            plot.setPlotFlags(
                    Objects.requireNonNull(Utilities.parseFlagChange(plot.getPlotFlags(), "+p")));
            plugin.sendMessage(
                    player,
                    "messages.plot.toggle.pvp.success",
                    "%cityname%",
                    city.getCityName(),
                    "%status%",
                    "aktiverat");
        }

        @Subcommand("animals")
        public static void onAnimals(Player player) {
            Plot plot = Utilities.hasPlotPermissions(player, "metropolis.plot.toggle.animals", Role.ASSISTANT, false);
            if (plot == null) {
                return;
            }
            City city = plot.getCity();
            Role role = CityDatabase.getCityRole(city, player.getUniqueId().toString());
            assert role != null;
            boolean isMayor = role.equals(Role.MAYOR);
            if (plot.isKMarked() && !isMayor) {
                plugin.sendMessage(
                        player, "messages.error.city.permissionDenied", "%cityname%", city.getCityName());
                return;
            }
            if (plot.hasFlag('a')) {
                Database.addLogEntry(
                        city,
                        "{ \"type\": \"plot\", \"subtype\": \"toggle\", \"id\": "
                                + plot.getPlotID()
                                + ", \"name\": "
                                + plot.getPlotName()
                                + ", \"item\": "
                                + "animals"
                                + ", \"state\": "
                                + false
                                + ", \"issuer\": "
                                + player.getUniqueId().toString()
                                + " }");
                plot.setPlotFlags(
                        Objects.requireNonNull(Utilities.parseFlagChange(plot.getPlotFlags(), "-a")));
                plugin.sendMessage(
                        player,
                        "messages.plot.toggle.animals.success",
                        "%cityname%",
                        city.getCityName(),
                        "%status%",
                        "inaktiverat");
                return;
            }
            if (plot.getPlotFlags().length == 0) {
                Database.addLogEntry(
                        city,
                        "{ \"type\": \"plot\", \"subtype\": \"toggle\", \"id\": "
                                + plot.getPlotID()
                                + ", \"name\": "
                                + plot.getPlotName()
                                + ", \"item\": "
                                + "animals"
                                + ", \"state\": "
                                + true
                                + ", \"issuer\": "
                                + player.getUniqueId().toString()
                                + " }");
                plot.setPlotFlags("a");
                plugin.sendMessage(
                        player,
                        "messages.plot.toggle.animals.success",
                        "%cityname%",
                        city.getCityName(),
                        "%status%",
                        "aktiverat");
                return;
            }
            plot.setPlotFlags(
                    Objects.requireNonNull(Utilities.parseFlagChange(plot.getPlotFlags(), "+a")));
            plugin.sendMessage(
                    player,
                    "messages.plot.toggle.animals.success",
                    "%cityname%",
                    city.getCityName(),
                    "%status%",
                    "aktiverat");
        }

        @Subcommand("mobs")
        public static void onMobs(Player player) {
            Plot plot = Utilities.hasPlotPermissions(player, "metropolis.plot.toggle.mobs", Role.ASSISTANT, false);
            if (plot == null) {
                return;
            }
            City city = plot.getCity();
            Role role = CityDatabase.getCityRole(city, player.getUniqueId().toString());
            assert role != null;
            boolean isMayor = role.equals(Role.MAYOR);
            if (plot.isKMarked() && !isMayor) {
                plugin.sendMessage(
                        player, "messages.error.city.permissionDenied", "%cityname%", city.getCityName());
                return;
            }
            if (plot.hasFlag('m')) {
                Database.addLogEntry(
                        city,
                        "{ \"type\": \"plot\", \"subtype\": \"toggle\", \"id\": "
                                + plot.getPlotID()
                                + ", \"name\": "
                                + plot.getPlotName()
                                + ", \"item\": "
                                + "monsters"
                                + ", \"state\": "
                                + false
                                + ", \"issuer\": "
                                + player.getUniqueId().toString()
                                + " }");
                plot.setPlotFlags(
                        Objects.requireNonNull(Utilities.parseFlagChange(plot.getPlotFlags(), "-m")));
                plugin.sendMessage(
                        player,
                        "messages.plot.toggle.monsters.success",
                        "%cityname%",
                        city.getCityName(),
                        "%status%",
                        "inaktiverat");
                return;
            }
            if (plot.getPlotFlags().length == 0) {
                Database.addLogEntry(
                        city,
                        "{ \"type\": \"plot\", \"subtype\": \"toggle\", \"id\": "
                                + plot.getPlotID()
                                + ", \"name\": "
                                + plot.getPlotName()
                                + ", \"item\": "
                                + "monsters"
                                + ", \"state\": "
                                + true
                                + ", \"issuer\": "
                                + player.getUniqueId().toString()
                                + " }");
                plot.setPlotFlags("m");
                plugin.sendMessage(
                        player,
                        "messages.plot.toggle.monsters.success",
                        "%cityname%",
                        city.getCityName(),
                        "%status%",
                        "aktiverat");
                return;
            }
            plot.setPlotFlags(
                    Objects.requireNonNull(Utilities.parseFlagChange(plot.getPlotFlags(), "+m")));
            plugin.sendMessage(
                    player,
                    "messages.plot.toggle.monsters.success",
                    "%cityname%",
                    city.getCityName(),
                    "%status%",
                    "aktiverat");
        }

        @Subcommand("meeting")
        public static void onMeeting(Player player) {
            Plot plot = Utilities.hasPlotPermissions(player, "metropolis.plot.toggle.meeting", Role.ASSISTANT, false);
            if (plot == null) {
                return;
            }
            City city = plot.getCity();
            Role role = CityDatabase.getCityRole(city, player.getUniqueId().toString());
            assert role != null;
            boolean isMayor = role.equals(Role.MAYOR);
            if (plot.isKMarked() && !isMayor) {
                plugin.sendMessage(
                        player, "messages.error.city.permissionDenied", "%cityname%", city.getCityName());
                return;
            }
            if (plot.hasFlag('c')) {
                Database.addLogEntry(
                        city,
                        "{ \"type\": \"plot\", \"subtype\": \"toggle\", \"id\": "
                                + plot.getPlotID()
                                + ", \"name\": "
                                + plot.getPlotName()
                                + ", \"item\": "
                                + "meeting"
                                + ", \"state\": "
                                + false
                                + ", \"issuer\": "
                                + player.getUniqueId().toString()
                                + " }");
                plot.setPlotFlags(
                        Objects.requireNonNull(Utilities.parseFlagChange(plot.getPlotFlags(), "-c")));
                plugin.sendMessage(
                        player,
                        "messages.plot.toggle.meeting.success",
                        "%cityname%",
                        city.getCityName(),
                        "%status%",
                        "inaktiverat");
                return;
            }
            if (plot.getPlotFlags().length == 0) {
                plot.setPlotFlags("c");
                plugin.sendMessage(
                        player,
                        "messages.plot.toggle.meeting.success",
                        "%cityname%",
                        city.getCityName(),
                        "%status%",
                        "aktiverat");
                return;
            }
            Database.addLogEntry(
                    city,
                    "{ \"type\": \"plot\", \"subtype\": \"toggle\", \"id\": "
                            + plot.getPlotID()
                            + ", \"name\": "
                            + plot.getPlotName()
                            + ", \"item\": "
                            + "meeting"
                            + ", \"state\": "
                            + true
                            + ", \"issuer\": "
                            + player.getUniqueId().toString()
                            + " }");
            plot.setPlotFlags(
                    Objects.requireNonNull(Utilities.parseFlagChange(plot.getPlotFlags(), "+c")));
            plugin.sendMessage(
                    player,
                    "messages.plot.toggle.meeting.success",
                    "%cityname%",
                    city.getCityName(),
                    "%status%",
                    "aktiverat");
        }

        @Subcommand("keepexp")
        public static void onXp(Player player) {
            if (!player.hasPermission("metropolis.admin.plot.toggle.keepexp")) {
                plugin.sendMessage(player, "messages.error.permissionDenied");
                return;
            }
            Plot plot = PlotDatabase.getPlotAtLocation(player.getLocation());
            if (plot == null) {
                return;
            }
            City city = plot.getCity();
            if (plot.hasFlag('x')) {
                Database.addLogEntry(
                        city,
                        "{ \"type\": \"plot\", \"subtype\": \"toggle\", \"id\": "
                                + plot.getPlotID()
                                + ", \"name\": "
                                + plot.getPlotName()
                                + ", \"item\": "
                                + "keepexp"
                                + ", \"state\": "
                                + false
                                + ", \"issuer\": "
                                + player.getUniqueId().toString()
                                + " }");
                plot.setPlotFlags(
                        Objects.requireNonNull(Utilities.parseFlagChange(plot.getPlotFlags(), "-x")));
                plugin.sendMessage(
                        player,
                        "messages.plot.toggle.keepexp.success",
                        "%cityname%",
                        city.getCityName(),
                        "%status%",
                        "tappas");
                return;
            }
            if (plot.getPlotFlags().length == 0) {
                plot.setPlotFlags("x");
                plugin.sendMessage(
                        player,
                        "messages.plot.toggle.keepexp.success",
                        "%cityname%",
                        city.getCityName(),
                        "%status%",
                        "behålls");
                return;
            }
            Database.addLogEntry(
                    city,
                    "{ \"type\": \"plot\", \"subtype\": \"toggle\", \"id\": "
                            + plot.getPlotID()
                            + ", \"name\": "
                            + plot.getPlotName()
                            + ", \"item\": "
                            + "keepexp"
                            + ", \"state\": "
                            + true
                            + ", \"issuer\": "
                            + player.getUniqueId().toString()
                            + " }");
            plot.setPlotFlags(
                    Objects.requireNonNull(Utilities.parseFlagChange(plot.getPlotFlags(), "+x")));
            plugin.sendMessage(
                    player,
                    "messages.plot.toggle.keepexp.success",
                    "%cityname%",
                    city.getCityName(),
                    "%status%",
                    "behålls");
        }

        @Subcommand("keepinv")
        public static void onKeepInv(Player player) {
            if (!player.hasPermission("metropolis.admin.plot.toggle.keepinv")) {
                plugin.sendMessage(player, "messages.error.permissionDenied");
                return;
            }
            Plot plot = PlotDatabase.getPlotAtLocation(player.getLocation());
            if (plot == null) {
                return;
            }
            City city = plot.getCity();
            if (plot.hasFlag('i')) {
                Database.addLogEntry(
                        city,
                        "{ \"type\": \"plot\", \"subtype\": \"toggle\", \"id\": "
                                + plot.getPlotID()
                                + ", \"name\": "
                                + plot.getPlotName()
                                + ", \"item\": "
                                + "keepinv"
                                + ", \"state\": "
                                + false
                                + ", \"issuer\": "
                                + player.getUniqueId().toString()
                                + " }");
                plot.setPlotFlags(
                        Objects.requireNonNull(Utilities.parseFlagChange(plot.getPlotFlags(), "-i")));
                plugin.sendMessage(
                        player,
                        "messages.plot.toggle.keepinv.success",
                        "%cityname%",
                        city.getCityName(),
                        "%status%",
                        "tappas");
                return;
            }
            if (plot.getPlotFlags().length == 0) {
                plot.setPlotFlags("i");
                plugin.sendMessage(
                        player,
                        "messages.plot.toggle.keepinv.success",
                        "%cityname%",
                        city.getCityName(),
                        "%status%",
                        "behålls");
                return;
            }
            Database.addLogEntry(
                    city,
                    "{ \"type\": \"plot\", \"subtype\": \"toggle\", \"id\": "
                            + plot.getPlotID()
                            + ", \"name\": "
                            + plot.getPlotName()
                            + ", \"item\": "
                            + "keepinv"
                            + ", \"state\": "
                            + true
                            + ", \"issuer\": "
                            + player.getUniqueId().toString()
                            + " }");
            plot.setPlotFlags(
                    Objects.requireNonNull(Utilities.parseFlagChange(plot.getPlotFlags(), "+i")));
            plugin.sendMessage(
                    player,
                    "messages.plot.toggle.keepinv.success",
                    "%cityname%",
                    city.getCityName(),
                    "%status%",
                    "behålls");
        }

        @Subcommand("lock")
        public static void onLock(Player player) {
            if (!player.hasPermission("metropolis.admin.plot.toggle.lock")) {
                plugin.sendMessage(player, "messages.error.permissionDenied");
                return;
            }
            Plot plot = PlotDatabase.getPlotAtLocation(player.getLocation());
            if (plot == null) {
                return;
            }
            City city = plot.getCity();
            if (plot.hasFlag('l')) {
                Database.addLogEntry(
                        city,
                        "{ \"type\": \"plot\", \"subtype\": \"toggle\", \"id\": "
                                + plot.getPlotID()
                                + ", \"name\": "
                                + plot.getPlotName()
                                + ", \"item\": "
                                + "lock"
                                + ", \"state\": "
                                + false
                                + ", \"issuer\": "
                                + player.getUniqueId().toString()
                                + " }");
                plot.setPlotFlags(
                        Objects.requireNonNull(Utilities.parseFlagChange(plot.getPlotFlags(), "-l")));
                plugin.sendMessage(
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
                plugin.sendMessage(
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
                            + plot.getPlotID()
                            + ", \"name\": "
                            + plot.getPlotName()
                            + ", \"item\": "
                            + "lock"
                            + ", \"state\": "
                            + true
                            + ", \"issuer\": "
                            + player.getUniqueId().toString()
                            + " }");
            plot.setPlotFlags(
                    Objects.requireNonNull(Utilities.parseFlagChange(plot.getPlotFlags(), "+l")));
            plugin.sendMessage(
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
            City city = plot.getCity();
            if (plot.isKMarked()) {
                Database.addLogEntry(
                        city,
                        "{ \"type\": \"plot\", \"subtype\": \"toggle\", \"id\": "
                                + plot.getPlotID()
                                + ", \"name\": "
                                + plot.getPlotName()
                                + ", \"item\": "
                                + "k"
                                + ", \"state\": "
                                + false
                                + ", \"issuer\": "
                                + player.getUniqueId().toString()
                                + " }");
                plot.setKMarked(false);
                Utilities.sendCityScoreboard(player, city, plot);
                plugin.sendMessage(
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
                                + plot.getPlotID()
                                + ", \"name\": "
                                + plot.getPlotName()
                                + ", \"item\": "
                                + "k"
                                + ", \"state\": "
                                + true
                                + ", \"issuer\": "
                                + player.getUniqueId().toString()
                                + " }");
                plot.setKMarked(true);
                Utilities.sendCityScoreboard(player, city, plot);
                plugin.sendMessage(
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
        City city = plot.getCity();
        Role role = CityDatabase.getCityRole(city, player.getUniqueId().toString());
        assert role != null;
        boolean isMayor = role.equals(Role.MAYOR);

        if (plot.isKMarked() && !isMayor) {
            plugin.sendMessage(
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
            plugin.sendMessage(player, "messages.error.plot.tooSmall");
            return;
        }
        if (MetropolisListener.playerYMax.get(player.getUniqueId())
                - MetropolisListener.playerYMin.get(player.getUniqueId())
                < 3) {
            plugin.sendMessage(player, "messages.error.plot.tooLowY");
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
                                    .getCityName(),
                            HCDatabase.getHomeCityToCityname(player.getUniqueId().toString()))) {
                        plugin.sendMessage(player, "messages.error.plot.intersectsExistingClaim");
                        return;
                    }
                    Plot[] intersectingPlots = PlotDatabase.intersectingPlots(regionPolygon, yMin, yMax, city);
                    if (!(intersectingPlots == null) && intersectingPlots.length > 1) {
                        plugin.sendMessage(player, "messages.error.plot.intersectsExistingPlot");
                        return;
                    }
                    Database.addLogEntry(
                            city,
                            "{ \"type\": \"update\", \"subtype\": \"plot\", \"id\": "
                                    + plot.getPlotID()
                                    + ", \"name\": "
                                    + plot.getPlotName()
                                    + ", \"points\": "
                                    + Utilities.parsePoints(locations)
                                    + ", \"ymin\": "
                                    + MetropolisListener.playerYMin.get(player.getUniqueId())
                                    + ", \"ymax\": "
                                    + MetropolisListener.playerYMax.get(player.getUniqueId())
                                    + ", \"player\": "
                                    + player.getUniqueId().toString()
                                    + " }");
                    plot.updatePlot(player, locations, MetropolisListener.playerYMin.get(player.getUniqueId()), MetropolisListener.playerYMax.get(player.getUniqueId()));
                    MetropolisListener.savedLocs.remove(player.getUniqueId());
                    MetropolisListener.playerPolygons.remove(player.getUniqueId());
                    MetropolisListener.playerYMin.remove(player.getUniqueId());
                    MetropolisListener.playerYMax.remove(player.getUniqueId());
                    Utilities.sendCityScoreboard(player, city, plot);
                    plugin.sendMessage(
                            player,
                            "messages.city.successful.set.plot.new",
                            "%cityname%",
                            city.getCityName(),
                            "%plotname%",
                            plot.getPlotName());
                    plugin.sendMessage(
                            player, "messages.plot.update.success", "%cityname%", city.getCityName());
                    return;
                }
            }
        }
    }

    @Subcommand("buy")
    public static void onBuy(Player player) {
        if (!player.hasPermission("metropolis.plot.buy")) {
            plugin.sendMessage(player, "messages.error.permissionDenied");
            return;
        }
        Plot plot = PlotDatabase.getPlotAtLocation(player.getLocation());
        if (plot == null) {
            return;
        }
        City city = plot.getCity();
        if (CityDatabase.getCityBan(city, player.getUniqueId().toString()) != null) {
            plugin.sendMessage(player, "messages.error.city.banned");
            return;
        }
        Role role = CityDatabase.getCityRole(city, player.getUniqueId().toString());
        if (role == null && !plot.getPlotType().equals("vacation")) {
            plugin.sendMessage(
                    player, "messages.error.city.permissionDenied", "%cityname%", city.getCityName());
            return;
        }
        Economy economy = Metropolis.getEconomy();
        if (plot.getPlotOwner() != null) {
            plugin.sendMessage(
                    player,
                    "messages.error.plot.set.owner.alreadyOwner",
                    "%cityname%",
                    city.getCityName());
            return;
        }
        if (!plot.isForSale()) {
            plugin.sendMessage(
                    player,
                    "messages.error.plot.set.owner.notForSale",
                    "%cityname%",
                    city.getCityName());
            return;
        }
        if (economy.getBalance(player) < plot.getPlotPrice()) {
            plugin.sendMessage(
                    player, "messages.error.missing.playerBalance", "%cityname%", city.getCityName());
            return;
        }
        economy.withdrawPlayer(player, plot.getPlotPrice());
        city.addCityBalance(plot.getPlotPrice());
        Database.addLogEntry(
                city,
                "{ \"type\": \"buy\", \"subtype\": \"plot\", \"id\": "
                        + plot.getPlotID()
                        + ", \"name\": "
                        + plot.getPlotName()
                        + ", \"player\": "
                        + player.getUniqueId().toString()
                        + " }");
        plot.setPlotOwner(player.getName());
        plot.setPlotOwnerUUID(player.getUniqueId().toString());
        plugin.sendMessage(
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
    }
}
