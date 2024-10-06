package live.supeer.metropolis.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import live.supeer.apied.ApiedAPI;
import live.supeer.apied.MPlayer;
import live.supeer.metropolis.Database;
import live.supeer.metropolis.Metropolis;
import live.supeer.metropolis.city.*;
import live.supeer.metropolis.event.*;
import live.supeer.metropolis.plot.PlotDatabase;
import live.supeer.metropolis.utils.LocationUtil;
import live.supeer.metropolis.utils.Utilities;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@CommandAlias("metropolis | mp")
public class CommandMetropolis extends BaseCommand {
    private static final List<Player> mergingPlayers = new ArrayList<>();

    @Default
    public void onDefault(Player player) {
        if (player.hasPermission("metropolis.admin")) {
            Metropolis.sendMessage(player, "messages.syntax.admin.all");
        } else {
            Metropolis.sendAccessDenied(player);
        }
    }

    @Subcommand("taxcollect")
    public void onTaxCollect(Player player) {
        if (player.hasPermission("metropolis.admin.taxcollect")) {
            CityDatabase.collectTaxes();
        } else {
            Metropolis.sendAccessDenied(player);
        }
    }

    @Subcommand("rentcollect")
    public void onRentCollect(Player player) {
        if (player.hasPermission("metropolis.admin.rentcollect")) {
            PlotDatabase.collectPlotRents();
        } else {
            Metropolis.sendAccessDenied(player);
        }
    }

    @Subcommand("override|or")
    public void onOverride(Player player) {
        if (!player.hasPermission("metropolis.admin.override")) {
            Metropolis.sendAccessDenied(player);
            return;
        }
        if (Metropolis.overrides.contains(player)) {
            Metropolis.overrides.remove(player);
            Metropolis.sendMessage(player, "messages.override.disabled");
        } else {
            Metropolis.overrides.add(player);
            Metropolis.sendMessage(player, "messages.override.enabled");
        }
    }


    @Subcommand("city")
    public class City extends BaseCommand {


        @Subcommand("balance")
        @CommandCompletion("@nothing @cityNames")
        public void onCityBalance(Player player, String argument, String cityName) {
            if (player.hasPermission("metropolis.admin.city.balance")) {
                live.supeer.metropolis.city.City city = CityDatabase.getCity(cityName).get();
                if (CityDatabase.getCity(cityName).isEmpty()) {
                    Metropolis.sendMessage(player, "messages.error.city.notFound");
                    return;
                }
                if (argument == null) {
                    Metropolis.sendMessage(player,"messages.syntax.admin.balance");
                    return;
                }
                boolean numberSubstring = argument.substring(1).replaceAll("[0-9]", "").matches("[^0-9]")
                        || argument.length() == 1;
                if (argument.startsWith("+")) {
                    if (numberSubstring) {
                        Metropolis.sendMessage(player, "messages.syntax.admin.balance");
                        return;
                    }
                    int inputBalance = Integer.parseInt(argument.replaceAll("[^0-9]", ""));
                    CityDatabase.addCityBalance(city, inputBalance);
                    Database.addLogEntry(
                            city,
                            "{ \"type\": \"cityBank\", \"subtype\": \"deposit\", \"balance\": "
                                    + inputBalance
                                    + ", \"player\": "
                                    + player.getUniqueId()
                                    + " }");
                    Metropolis.sendMessage(
                            player,
                            "messages.city.successful.deposit",
                            "%amount%",
                            Utilities.formattedMoney(inputBalance),
                            "%cityname%",
                            cityName);
                    return;
                }
                if (argument.startsWith("-")) {
                    if (numberSubstring) {
                        Metropolis.sendMessage(player, "messages.syntax.admin.balance");
                        return;
                    }
                    int inputBalance = Integer.parseInt(argument.replaceAll("[^0-9]", ""));
                    String inputBalanceFormatted = Utilities.formattedMoney(inputBalance);
                    CityDatabase.removeCityBalance(city, inputBalance);
                    Database.addLogEntry(
                            city,
                            "{ \"type\": \"cityBank\", \"subtype\": \"withdraw\", \"balance\": "
                                    + inputBalance
                                    + ", \"player\": "
                                    + player.getUniqueId()
                                    + "\" }");
                    Metropolis.sendMessage(
                            player,
                            "messages.city.successful.withdraw",
                            "%amount%",
                            inputBalanceFormatted,
                            "%cityname%",
                            city.getCityName());
                    return;
                }
                Metropolis.sendMessage(player, "messages.syntax.admin.balance");
            } else {
                Metropolis.sendAccessDenied(player);
            }
        }

        @Subcommand("toggle taxexempt")
        @CommandCompletion("@cityNames")
        public void onToggleTaxExempt(Player player, String cityName) {
            if (player.hasPermission("metropolis.admin.city.toggle.taxexempt")) {
                live.supeer.metropolis.city.City city = CityDatabase.getCity(cityName).get();
                if (CityDatabase.getCity(cityName).isEmpty()) {
                    Metropolis.sendMessage(player, "messages.error.city.notFound");
                    return;
                }
                if (city.isTaxExempt()) {
                    city.setTaxExempt(false);
                    Metropolis.sendMessage(player, "messages.city.successful.offTaxExempt", "%cityname%", city.getCityName());
                    Database.addLogEntry(city, "{ \"type\": \"city\", \"subtype\": \"toggleTaxExempt\", \"state\": \"false\", \"player\": \"" + player.getUniqueId() + "\" }");
                } else {
                    city.setTaxExempt(true);
                    Metropolis.sendMessage(player, "messages.city.successful.onTaxExempt", "%cityname%", city.getCityName());
                    Database.addLogEntry(city, "{ \"type\": \"city\", \"subtype\": \"toggleTaxExempt\", \"state\": \"true\", \"player\": \"" + player.getUniqueId() + "\" }");
                }
            } else {
                Metropolis.sendAccessDenied(player);
            }
        }

        @Subcommand("new")
        @CommandCompletion("@players @nothing")
        public void onNew(Player player, OnlinePlayer target, String cityName) {
            MPlayer mPlayer = ApiedAPI.getPlayer(target.player);
            if (!player.hasPermission("metropolis.admin.city.new")) {
                Metropolis.sendAccessDenied(player);
                return;
            }
            if (mPlayer == null) {
                Metropolis.sendMessage(player, "messages.error.player.notFound");
                return;
            }
            if (cityName.isEmpty() || cityName.length() > Metropolis.configuration.getCityNameLimit()) {
                Metropolis.sendMessage(player, "messages.error.city.nameLength", "%maxlength%", String.valueOf(Metropolis.configuration.getCityNameLimit()));
                return;
            }
            if (!cityName.matches("[A-Za-zÀ-ÖØ-öø-ÿ0-9 ]+")) {
                Metropolis.sendMessage(player, "messages.error.city.invalidName");
                return;
            }
            if (CityDatabase.getCity(cityName).isPresent()) {
                Metropolis.sendMessage(player, "messages.error.city.cityExists");
                return;
            }
            if (CityDatabase.getClaim(player.getLocation()) != null) {
                Metropolis.sendMessage(player, "messages.error.city.claimExists");
                return;
            }
            if (Utilities.cannotClaimOrCreateCity(player.getLocation().toBlockLocation(), null)) {
                Metropolis.sendMessage(player, "messages.error.city.tooCloseToOtherCity");
                return;
            }

            live.supeer.metropolis.city.City city = CityDatabase.newCity(cityName, target.getPlayer());
            assert city != null;
            CityCreationEvent creationEvent = new CityCreationEvent( city);
            Metropolis.getInstance().getServer().getPluginManager().callEvent(creationEvent);
            Database.addLogEntry(
                    city,
                    "{ \"type\": \"create\", \"subtype\": \"city\", \"name\": "
                            + city.getCityName()
                            + ", \"tax\": "
                            + Metropolis.configuration.getCityStartingTax()
                            + ", \"spawn\": "
                            + LocationUtil.formatLocation(city.getCitySpawn())
                            + ", \"balance\": "
                            + Metropolis.configuration.getCityStartingBalance()
                            + ", \"player\": "
                            + mPlayer.getUuid().toString()
                            + " }");
            Database.addLogEntry(
                    city,
                    "{ \"type\": \"join\", \"subtype\": \"city\", \"player\": "
                            + mPlayer.getUuid().toString()
                            + " }");
            Database.addLogEntry(
                    city,
                    "{ \"type\": \"rank\", \"subtype\": \"change\", \"from\": "
                            + "member"
                            + ", \"to\": "
                            + "mayor"
                            + ", \"player\": "
                            + mPlayer.getUuid().toString()
                            + " }");
            CityDatabase.setCityRole(city, mPlayer.getUuid().toString(), Role.MAYOR);
            Claim claim = CityDatabase.createClaim(city, player.getLocation(), false, player.getUniqueId().toString());
            PlayerEnterCityEvent enterCityEvent = new PlayerEnterCityEvent(player, city);
            Bukkit.getServer().getPluginManager().callEvent(enterCityEvent);
            CityJoinEvent joinEvent = new CityJoinEvent(player, city);
            Metropolis.getInstance().getServer().getPluginManager().callEvent(joinEvent);
            assert claim != null;
            Database.addLogEntry(
                    city,
                    "{ \"type\": \"buy\", \"subtype\": \"claim\", \"balance\": "
                            + "0"
                            + ", \"claimlocation\": "
                            + LocationUtil.formatChunk(
                            claim.getClaimWorld().getName(), claim.getXPosition(), claim.getZPosition())
                            + ", \"player\": "
                            + mPlayer.getUuid().toString()
                            + " }");
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (onlinePlayer.hasPermission("metropolis.city.new")) {
                    if (onlinePlayer.getUniqueId() == mPlayer.getUuid()) {
                        Metropolis.sendMessage(
                                onlinePlayer, "messages.city.successful.creation.self", "%cityname%", cityName);
                    } else {
                        Metropolis.sendMessage(
                                onlinePlayer,
                                "messages.city.successful.creation.others",
                                "%playername%",
                                mPlayer.getName(),
                                "%cityname%",
                                cityName);
                    }
                }
            }
        }

        @Subcommand("delete")
        @CommandCompletion("@cityNames")
        public static void onDelete(Player player, String cityName) {
            if (!player.hasPermission("metropolis.admin.city.delete")) {
                Metropolis.sendAccessDenied(player);
                return;
            }
            if (CityDatabase.getCity(cityName).isEmpty()) {
                Metropolis.sendMessage(player, "messages.error.city.notFound");
                return;
            }
            live.supeer.metropolis.city.City city = CityDatabase.getCity(cityName).get();
            Metropolis.sendMessage(player, "messages.city.admin.delete.confirmation", "%cityname%", city.getCityName());
        }

        @Subcommand("delete!")
        @Private
        public static void onDeleteConfirm(Player player, String cityName) {
            if (!player.hasPermission("metropolis.admin.city.delete")) {
                Metropolis.sendAccessDenied(player);
                return;
            }
            live.supeer.metropolis.city.City city = CityDatabase.getCity(cityName).get();
            if (CityDatabase.getCity(cityName).isEmpty()) {
                Metropolis.sendMessage(player, "messages.error.city.notFound");
                return;
            }
            Metropolis.playerInCity.remove(player.getUniqueId(), city);
            if (Metropolis.playerInDistrict.containsKey(player.getUniqueId()) && Metropolis.playerInDistrict.get(player.getUniqueId()).getCity().equals(city)) {
                Metropolis.playerInDistrict.remove(player.getUniqueId());
            }
            if (Metropolis.playerInPlot.containsKey(player.getUniqueId()) && Metropolis.playerInPlot.get(player.getUniqueId()).getCity().equals(city)) {
                Metropolis.playerInPlot.remove(player.getUniqueId());
            }
            cityName = city.getCityName();
            CityDatabase.deleteCity(city);
            CityDeletionEvent deletionEvent = new CityDeletionEvent(city);
            Metropolis.getInstance().getServer().getPluginManager().callEvent(deletionEvent);
            Database.addLogEntry(city, "{ \"type\": \"city\", \"subtype\": \"delete\", \"city\": \"" + cityName + "\", \"player\": \"" + player.getUniqueId() + "\" }");

            for (Player p : Bukkit.getOnlinePlayers()) {
                if (player == p) {
                    Metropolis.sendMessage(player, "messages.city.delete.success", "%cityname%", cityName);
                }
                Metropolis.sendMessage(player, "messages.city.delete.others", "%cityname%", cityName);

            }
            Utilities.sendScoreboard(player);
        }

        @Subcommand("join")
        @CommandCompletion("@players @cityNames")
        public void onJoin(Player player, OnlinePlayer target, String cityName) {
            if (!player.hasPermission("metropolis.admin.city.join")) {
                Metropolis.sendAccessDenied(player);
                return;
            }
            if (CityDatabase.getCity(cityName).isEmpty()) {
                Metropolis.sendMessage(player, "messages.error.city.notFound");
                return;
            }
            live.supeer.metropolis.city.City city = CityDatabase.getCity(cityName).get();
            MPlayer mPlayer = ApiedAPI.getPlayer(target.player);
            if (mPlayer == null) {
                Metropolis.sendMessage(player, "messages.error.player.notFound");
                return;
            }
            if (CityDatabase.getCityRole(city, mPlayer.getUuid().toString()) != null) {
                Metropolis.sendMessage(player, "messages.error.city.alreadyMember");
                return;
            }
            Database.addLogEntry(
                    city,
                    "{ \"type\": \"join\", \"subtype\": \"city\", \"player\": "
                            + mPlayer.getUuid().toString()
                            + " }");
            CityDatabase.newMember(city, target.getPlayer());
            CityJoinEvent joinEvent = new CityJoinEvent(target.player, city);
            Metropolis.getInstance().getServer().getPluginManager().callEvent(joinEvent);
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (CityDatabase.memberExists(player.getUniqueId().toString(), city)) {
                    if (onlinePlayer == player) {
                        Metropolis.sendMessage(player,"messages.city.admin.join", "%player%", target.getPlayer().getName(), "%cityname%", city.getCityName());
                    }
                    if (onlinePlayer == target.player) {
                        Metropolis.sendMessage(
                                onlinePlayer, "messages.city.successful.join.self", "%cityname%", city.getCityName());
                    } else {
                        Metropolis.sendMessage(
                                onlinePlayer,
                                "messages.city.successful.join.others",
                                "%playername%",
                                player.getName(),
                                "%cityname%",
                                city.getCityName());
                    }
                }
            }
        }

        @Subcommand("kick")
        @CommandCompletion("@players @cityNames")
        public void onKick(Player player, OnlinePlayer target, String cityName) {
            if (!player.hasPermission("metropolis.admin.city.kick")) {
                Metropolis.sendAccessDenied(player);
                return;
            }
            if (CityDatabase.getCity(cityName).isEmpty()) {
                Metropolis.sendMessage(player, "messages.error.city.notFound");
                return;
            }
            live.supeer.metropolis.city.City city = CityDatabase.getCity(cityName).get();
            MPlayer mPlayer = ApiedAPI.getPlayer(target.player);
            if (mPlayer == null) {
                Metropolis.sendMessage(player, "messages.error.player.notFound");
                return;
            }
            if (!CityDatabase.memberExists(mPlayer.getUuid().toString(), city)) {
                Metropolis.sendMessage(player, "messages.error.city.notMember");
                return;
            }
            city.removeCityMember(mPlayer.getUuid().toString());
            CityLeaveEvent leaveEvent = new CityLeaveEvent(mPlayer, city);
            Metropolis.getInstance().getServer().getPluginManager().callEvent(leaveEvent);
            Metropolis.sendMessage(player, "messages.city.kick.success", "%cityname%", city.getCityName(), "%playername%", mPlayer.getName());
            for (Member member : city.getCityMembers()) {
                Player memberPlayer = Bukkit.getPlayer(UUID.fromString(member.getPlayerUUID()));
                if (memberPlayer == null) {
                    continue;
                }
                if(member.getCityRole().hasPermission(Role.ASSISTANT)) {
                    Metropolis.sendMessage(memberPlayer, "messages.city.kick.success", "%cityname%", city.getCityName(), "%playername%", mPlayer.getName());
                    continue;
                }
                Metropolis.sendMessage(memberPlayer, "messages.city.kick.kickedOthers", "%cityname%", city.getCityName(), "%playername%", mPlayer.getName());
            }
            Database.addLogEntry(city, "{ \"type\": \"kick\", \"player\": \"" + mPlayer.getUuid() + "\", \"issuer\": \"" + player.getUniqueId() + "\" }");
        }

        @Subcommand("rank")
        @CommandCompletion("@players @allCityRoles @cityNames")
        public void onRank(Player player, String target, String role, String cityName) {
            if (!player.hasPermission("metropolis.admin.city.rank")) {
                Metropolis.sendAccessDenied(player);
                return;
            }
            if (CityDatabase.getCity(cityName).isEmpty()) {
                Metropolis.sendMessage(player, "messages.error.city.notFound");
                return;
            }
            live.supeer.metropolis.city.City city = CityDatabase.getCity(cityName).get();
            MPlayer mPlayer = ApiedAPI.getPlayer(target);
            if (mPlayer == null) {
                Metropolis.sendMessage(player, "messages.error.player.notFound");
                return;
            }
            if (!CityDatabase.memberExists(mPlayer.getUuid().toString(), city)) {
                Metropolis.sendMessage(player, "messages.error.city.notMember");
                return;
            }
            Role newRole = Role.fromString(role);
            if (newRole == null) {
                Metropolis.sendMessage(player, "messages.error.city.taxLevel.invalidRole");
                return;
            }
            Role oldRole = CityDatabase.getCityRole(city, mPlayer.getUuid().toString());
            if (oldRole == null) {
                Metropolis.sendMessage(player, "messages.error.city.notMember");
                return;
            }
            CityDatabase.setCityRole(city, mPlayer.getUuid().toString(), newRole);
            Database.addLogEntry(city, "{ \"type\": \"rank\", \"subtype\": \"change\", \"from\": \"" + oldRole + "\", \"to\": \"" + newRole + "\", \"issuer\": \"" + player.getUniqueId() + "\", \"player\": \"" + mPlayer.getUuid() + "\" }");
            Metropolis.sendMessage(player, "messages.city.admin.rank.success", "%cityname%", city.getCityName(), "%player%", mPlayer.getName(), "%role%", newRole.getRoleName());

        }

        @Subcommand("mergeinto")
        @CommandCompletion("@cityNames")
        public void onMergeInto(Player player, String cityName) {
            if (!player.hasPermission("metropolis.admin.city.mergeinto")) {
                Metropolis.sendAccessDenied(player);
                return;
            }
            live.supeer.metropolis.city.City currentCity = Metropolis.playerInCity.get(player.getUniqueId());
            if (CityDatabase.getCity(cityName).isEmpty()) {
                Metropolis.sendMessage(player, "messages.error.city.notFound");
                return;
            }
            live.supeer.metropolis.city.City targetCity = CityDatabase.getCity(cityName).get();
            if (currentCity == null) {
                Metropolis.sendMessage(player, "messages.error.city.outsideCity");
                return;
            }
            if (currentCity.getCityName().equals(cityName)) {
                Metropolis.sendMessage(player, "messages.error.city.sameCity");
                return;
            }
            if (!mergingPlayers.contains(player)) {
                mergingPlayers.add(player);
                Metropolis.sendMessage(player, "messages.city.merge.confirm", "%merging%", currentCity.getCityName(), "%merged%", targetCity.getCityName());
                return;
            }
            CityDatabase.mergeCities(currentCity, targetCity);
            Metropolis.sendMessage(player, "messages.city.merge.successful", "%merging%", currentCity.getCityName(), "%merged%", targetCity.getCityName());
            mergingPlayers.remove(player);
        }

        @Subcommand("restore")
        @CommandCompletion("@players @nothing @cityNames")
        public void onRestore(Player player, String playerName, String price, String cityName) {
            if (!player.hasPermission("metropolis.admin.city.restore")) {
                Metropolis.sendAccessDenied(player);
                return;
            }
            if (playerName == null || price == null || cityName == null) {
                Metropolis.sendMessage(player, "messages.syntax.admin.city.restore");
                return;
            }
            if (price.matches("[0-9]+") || price.equals("-")) {
                int priceInt;
                if (price.equals("-")) {
                    priceInt = 0;
                } else {
                    priceInt = Integer.parseInt(price);
                }
                if (CityDatabase.getCity(cityName).isEmpty()) {
                    Metropolis.sendMessage(player, "messages.error.city.notFound");
                    return;
                }
                live.supeer.metropolis.city.City city = CityDatabase.getCity(cityName).get();
                MPlayer targetPlayer = ApiedAPI.getPlayer(playerName);
                if (targetPlayer == null) {
                    Metropolis.sendMessage(player, "messages.error.player.notFound");
                    return;
                }
                if (targetPlayer.getBalance() < priceInt) {
                    Metropolis.sendMessage(player, "messages.error.player.insufficientFunds");
                    return;
                }
                city.setAsNotReserve();
                city.addCityBalance(Metropolis.configuration.getCityStartingBalance());
                Database.addLogEntry(
                        city,
                        "{ \"type\": \"restore\", \"subtype\": \"city\", \"player\": "
                                + targetPlayer.getUuid().toString()
                                + ", \"proxy\": "
                                + player.getUniqueId()
                                + ", \"balance\": "
                                + Metropolis.configuration.getCityStartingBalance()
                                + " }");
                Member member = city.getCityMember(targetPlayer.getUuid().toString());
                if (member != null) {
                    Role role = member.getCityRole();
                    if (member.getCityRole() != Role.MAYOR) {
                        member.setRole(Role.MAYOR);
                        Database.addLogEntry(
                                city,
                                "{ \"type\": \"rank\", \"subtype\": \"change\", \"from\": "
                                        + role.getRoleName()
                                        + ", \"to\": "
                                        + "mayor"
                                        + ", \"player\": "
                                        + targetPlayer.getUuid().toString()
                                        + " }");
                    }
                }
                if (priceInt > 0) {
                    targetPlayer.removeBalance(priceInt, "{ \"type\": \"city\", \"subtype\": \"restore\", \"player\": " + player.getUniqueId() + ",  \"cityId\": " + city.getCityId() + "}");
                }
                Metropolis.sendMessage(player, "messages.city.successful.restore", "%player%", playerName, "%price%", Utilities.formattedMoney(priceInt), "%cityname%", city.getCityName());
            } else {
                Metropolis.sendMessage(player, "messages.error.invalidNumber");
            }
        }


        @Subcommand("set")
        public class Set extends BaseCommand {

            @Subcommand("minchunkdistance")
            @CommandCompletion("@nothing @cityNames")
            public void onSetMinChunkDistance(Player player, String distance, String cityName) {
                if (player.hasPermission("metropolis.admin.city.set.minchunkdistance")) {
                    live.supeer.metropolis.city.City city = CityDatabase.getCity(cityName).get();
                    if (CityDatabase.getCity(cityName).isEmpty()) {
                        Metropolis.sendMessage(player, "messages.error.city.notFound");
                        return;
                    }
                    if (distance.equals("-")) {
                        city.setMinChunkDistance(Metropolis.configuration.getMinChunkDistance());
                        Metropolis.sendMessage(player, "messages.city.successful.resetMinChunkDistance","%cityname%", city.getCityName());
                        return;
                    }
                    if (distance.matches("[0-9]+")) {
                        city.setMinChunkDistance(Integer.parseInt(distance));
                        Metropolis.sendMessage(player, "messages.city.successful.setMinChunkDistance", "%distance%", distance, "%cityname%", city.getCityName());
                        return;
                    }
                    Metropolis.sendMessage(player, "messages.error.invalidNumber");
                } else {
                    Metropolis.sendAccessDenied(player);
                }
            }

            @Subcommand("minspawndistance")
            @CommandCompletion("@nothing @cityNames")
            public void onSetMinSpawnDistance(Player player, String distance, String cityName) {
                if (player.hasPermission("metropolis.admin.city.set.minspawndistance")) {
                    live.supeer.metropolis.city.City city = CityDatabase.getCity(cityName).get();
                    if (CityDatabase.getCity(cityName).isEmpty()) {
                        Metropolis.sendMessage(player, "messages.error.city.notFound");
                        return;
                    }
                    if (distance.equals("-")) {
                        city.setMinSpawnDistance(Metropolis.configuration.getMinSpawnDistance());
                        Metropolis.sendMessage(player, "messages.city.successful.resetMinSpawnDistance","%cityname%", city.getCityName());
                        return;
                    }
                    if (distance.matches("[0-9]+")) {
                        city.setMinSpawnDistance(Integer.parseInt(distance));
                        Metropolis.sendMessage(player, "messages.city.successful.setMinSpawnDistance", "%distance%", distance, "%cityname%", city.getCityName());
                        return;
                    }
                    Metropolis.sendMessage(player, "messages.error.invalidNumber");
                } else {
                    Metropolis.sendAccessDenied(player);
                }
            }
        }
    }
}
