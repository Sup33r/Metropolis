package live.supeer.metropolis.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation .*;
import co.aikar.commands.annotation.Optional;
import live.supeer.metropolis.AutoclaimManager;
import live.supeer.metropolis.Database;
import live.supeer.metropolis.Metropolis;
import live.supeer.metropolis.MetropolisListener;
import live.supeer.metropolis.city.District;
import live.supeer.metropolis.event.PlayerEnterCityEvent;
import live.supeer.metropolis.utils.LocationUtil;
import live.supeer.metropolis.utils.Utilities;
import live.supeer.metropolis.city.*;
import live.supeer.metropolis.homecity.HCDatabase;
import live.supeer.metropolis.utils.DateUtil;
import net.coreprotect.CoreProtect;
import net.coreprotect.CoreProtectAPI;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;

import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@CommandAlias("city|c")
public class CommandCity extends BaseCommand {
    public static Metropolis plugin;

    private static final GeometryFactory geometryFactory = new GeometryFactory();


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

    @Subcommand("info")
    @Default
    public static void onInfo(Player player, @Optional String cityName) {}

    @Subcommand("bank")
    public static void onBank(Player player, @Optional String[] args) {
        if (!player.hasPermission("metropolis.city.bank")) {
            plugin.sendMessage(player, "messages.error.permissionDenied");
            return;
        }
        Economy economy = Metropolis.getEconomy();
        if (args.length == 0) {
            if (HCDatabase.getHomeCityToCityname(player.getUniqueId().toString()) == null) {
                plugin.sendMessage(player, "messages.error.missing.homeCity");
                return;
            }
            String homeCity = HCDatabase.getHomeCityToCityname(player.getUniqueId().toString());
            if (CityDatabase.getCity(homeCity).isEmpty()) {
                plugin.sendMessage(player, "messages.error.missing.city");
                return;
            }
            City city = CityDatabase.getCity(homeCity).get();
            String cityBalance = Utilities.formattedMoney(CityDatabase.getCityBalance(city));
            plugin.sendMessage(
                    player, "messages.city.balance", "%balance%", cityBalance, "%cityname%", homeCity);
            return;
        }
        if (args[0].startsWith("+")) {
            if (args[0].substring(1).replaceAll("[0-9]", "").matches("[^0-9]")
                    || args.length < 2
                    || args[0].length() == 1) {
                plugin.sendMessage(player, "messages.syntax.city.bank.deposit");
                return;
            }

            int inputBalance = Integer.parseInt(args[0].replaceAll("[^0-9]", ""));
            String cityName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            String playerCity = HCDatabase.getHomeCityToCityname(player.getUniqueId().toString());

            if (CityDatabase.getCity(cityName).isEmpty()) {
                plugin.sendMessage(player, "messages.error.missing.city");
                return;
            }
            City city = CityDatabase.getCity(cityName).get();

            if (economy.getBalance(player) < inputBalance) {
                plugin.sendMessage(
                        player, "messages.error.missing.playerBalance", "%cityname%", playerCity);
                return;
            }

            economy.withdrawPlayer(player, inputBalance);
            CityDatabase.addCityBalance(city, inputBalance);
            Database.addLogEntry(
                    city,
                    "{ \"type\": \"cityBank\", \"subtype\": \"deposit\", \"balance\": "
                            + inputBalance
                            + ", \"player\": "
                            + player.getUniqueId().toString()
                            + " }");
            plugin.sendMessage(
                    player,
                    "messages.city.successful.deposit",
                    "%amount%",
                    Utilities.formattedMoney(inputBalance),
                    "%cityname%",
                    cityName);
            return;
        }
        if (args[0].startsWith("-")) {
            if (args[0].substring(1).replaceAll("[0-9]", "").matches("[^0-9]")
                    || args.length < 2
                    || args[0].length() == 1) {
                plugin.sendMessage(player, "messages.syntax.city.bank.withdraw");
                return;
            }
            City city = Utilities.hasCityPermissions(player,"metropolis.city.bank.withdraw", Role.VICE_MAYOR);
            if (city == null) {
                return;
            }
            int inputBalance = Integer.parseInt(args[0].replaceAll("[^0-9]", ""));
            String inputBalanceFormatted = Utilities.formattedMoney(inputBalance);
            String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            int cityBalance = city.getCityBalance();
            if (!(reason.length() >= 8)) {
                plugin.sendMessage(
                        player, "messages.error.missing.reasonLength", "%cityname%", city.getCityName());
                return;
            }
            if (cityBalance <= 100000 || inputBalance > cityBalance - 100000) {
                plugin.sendMessage(
                        player, "messages.error.missing.balance", "%cityname%", city.getCityName());
                return;
            }
            CityDatabase.removeCityBalance(city, inputBalance);
            Database.addLogEntry(
                    city,
                    "{ \"type\": \"cityBank\", \"subtype\": \"withdraw\", \"balance\": "
                            + inputBalance
                            + ", \"player\": "
                            + player.getUniqueId().toString()
                            + ", \"reason\": \""
                            + reason
                            + "\" }");
            economy.depositPlayer(player, inputBalance);
            plugin.sendMessage(
                    player,
                    "messages.city.successful.withdraw",
                    "%amount%",
                    inputBalanceFormatted,
                    "%cityname%",
                    city.getCityName());
            return;
        }
        plugin.sendMessage(player, "messages.syntax.city.bank.bank");
        plugin.sendMessage(player, "messages.syntax.city.bank.deposit");
        plugin.sendMessage(player, "messages.syntax.city.bank.withdraw");
    }

    @Subcommand("new")
    public static void onNew(Player player, String cityName) {
        Economy economy = Metropolis.getEconomy();
        if (!player.hasPermission("metropolis.city.new")) {
            plugin.sendMessage(player, "messages.error.permissionDenied");
            return;
        }
        if (CityDatabase.getPlayerCityCount(player.getUniqueId().toString()) >= 3) {
            plugin.sendMessage(player, "messages.error.city.maxCityCount");
            return;
        }
        if (economy.getBalance(player) < Metropolis.configuration.getCityCreationCost()) {
            plugin.sendMessage(player, "messages.error.city.missing.balance.cityCost");
            return;
        }
        if (cityName.isEmpty() || cityName.length() > Metropolis.configuration.getCityNameLimit()) {
            plugin.sendMessage(player, "messages.error.city.nameLength", "%maxlength%", String.valueOf(Metropolis.configuration.getCityNameLimit()));
            return;
        }
        if (!cityName.matches("[A-Za-zÀ-ÖØ-öø-ÿ0-9 ]+")) {
            plugin.sendMessage(player, "messages.error.city.invalidName");
            return;
        }
        if (CityDatabase.getCity(cityName).isPresent()) {
            plugin.sendMessage(player, "messages.error.city.cityExists");
            return;
        }
        if (CityDatabase.getClaim(player.getLocation()) != null) {
            plugin.sendMessage(player, "messages.error.city.claimExists");
            return;
        }
        if (Utilities.isCloseToOtherCity(player, player.getLocation(), "newcity")) {
            plugin.sendMessage(player, "messages.error.city.tooCloseToOtherCity");
            return;
        }

        City city = CityDatabase.newCity(cityName, player);
        assert city != null;
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
                        + player.getUniqueId().toString()
                        + " }");
        Database.addLogEntry(
                city,
                "{ \"type\": \"join\", \"subtype\": \"city\", \"player\": "
                        + player.getUniqueId().toString()
                        + " }");
        Database.addLogEntry(
                city,
                "{ \"type\": \"rank\", \"subtype\": \"change\", \"from\": "
                        + "member"
                        + ", \"to\": "
                        + "mayor"
                        + ", \"player\": "
                        + player.getUniqueId().toString()
                        + " }");
        CityDatabase.setCityRole(city, player.getUniqueId().toString(), Role.MAYOR);
        Claim claim = CityDatabase.createClaim(city, player.getLocation(), false, player.getName(), player.getUniqueId().toString());
        PlayerEnterCityEvent enterCityEvent = new PlayerEnterCityEvent(player, city);
        Bukkit.getServer().getPluginManager().callEvent(enterCityEvent);
        assert claim != null;
        Database.addLogEntry(
                city,
                "{ \"type\": \"buy\", \"subtype\": \"claim\", \"balance\": "
                        + "0"
                        + ", \"claimlocation\": "
                        + LocationUtil.formatChunk(
                        claim.getClaimWorld().getName(), claim.getXPosition(), claim.getZPosition())
                        + ", \"player\": "
                        + player.getUniqueId().toString()
                        + " }");
        economy.withdrawPlayer(player, Metropolis.configuration.getCityCreationCost());
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.hasPermission("metropolis.city.new")) {
                if (onlinePlayer == player) {
                    plugin.sendMessage(
                            onlinePlayer, "messages.city.successful.creation.self", "%cityname%", cityName);
                } else {
                    plugin.sendMessage(
                            onlinePlayer,
                            "messages.city.successful.creation.others",
                            "%playername%",
                            player.getName(),
                            "%cityname%",
                            cityName);
                }
            }
        }
    }

    @Subcommand("claim")
    public static void onClaim(Player player, @Optional String arg) {
        if (!player.hasPermission("metropolis.city.claim")) {
            plugin.sendMessage(player, "messages.error.permissionDenied");
            return;
        }
        if (HCDatabase.hasHomeCity(player.getUniqueId().toString())) {
            plugin.sendMessage(player, "messages.error.missing.homeCity");
            return;
        }
        String cityName = HCDatabase.getHomeCityToCityname(player.getUniqueId().toString());

        if (CityDatabase.getCity(cityName).isEmpty()) {
            plugin.sendMessage(player, "messages.error.missing.city");
            return;
        }
        City city = CityDatabase.getCity(cityName).get();

        if (arg != null) {
            if (arg.equals("-")) {
                // Stop autoclaiming
                AutoclaimManager.stopAutoclaim(player);
                plugin.sendMessage(player, "messages.city.autoclaim.stopped", "%cityname%", cityName);
                return;
            }

            try {
                int autoclaimCount = Integer.parseInt(arg);
                if (autoclaimCount > 0) {
                    if (CityDatabase.getClaim(player.getLocation()) != null) {
                        plugin.sendMessage(player, "messages.city.autoclaim.notNature");
                        return;
                    }
                    if (CityDatabase.getCityBalance(city) < Metropolis.configuration.getCityClaimCost()) {
                        plugin.sendMessage(player, "messages.error.missing.claimCost", "%cityname%", city.getCityName(), "%cost%", ""+Metropolis.configuration.getCityClaimCost());
                        return;
                    }
                    if (Utilities.isCloseToOtherCity(player, player.getLocation(), "city")) {
                        plugin.sendMessage(player, "messages.error.city.tooCloseToOtherCity");
                        return;
                    }
                    if (CityDatabase.getCityRole(city, player.getUniqueId().toString()) == null || Objects.equals(CityDatabase.getCityRole(city, player.getUniqueId().toString()), Role.MEMBER)
                            || Objects.equals(
                            CityDatabase.getCityRole(city, player.getUniqueId().toString()), Role.INVITER)) {
                        plugin.sendMessage(player, "messages.error.city.permissionDenied", "%cityname%", cityName);
                        return;
                    }

                    if (!Utilities.cityCanClaim(city)) {
                        plugin.sendMessage(player, "messages.error.city.maxClaims", "%cityname%", city.getCityName());
                        return;
                    }

                    Claim claim1 = CityDatabase.getClaim(player.getLocation().toBlockLocation().add(16, 0, 0));
                    Claim claim2 = CityDatabase.getClaim(player.getLocation().toBlockLocation().add(-16, 0, 0));
                    Claim claim3 = CityDatabase.getClaim(player.getLocation().toBlockLocation().add(0, 0, 16));
                    Claim claim4 = CityDatabase.getClaim(player.getLocation().toBlockLocation().add(0, 0, -16));

                    if ((claim1 != null && claim1.getCity() == city) ||
                            (claim2 != null && claim2.getCity() == city) ||
                            (claim3 != null && claim3.getCity() == city) ||
                            (claim4 != null && claim4.getCity() == city)) {
                        Claim claim = CityDatabase.createClaim(city, player.getLocation(), false, player.getName(), player.getUniqueId().toString());
                        assert claim != null;
                        Database.addLogEntry(
                                city,
                                "{ \"type\": \"buy\", \"subtype\": \"claim\", \"balance\": "
                                        + "500"
                                        + ", \"claimlocation\": "
                                        + LocationUtil.formatChunk(
                                        claim.getClaimWorld().getName(), claim.getXPosition(), claim.getZPosition())
                                        + ", \"player\": "
                                        + player.getUniqueId().toString()
                                        + " }");
                        PlayerEnterCityEvent enterCityEvent = new PlayerEnterCityEvent(player, city);
                        Bukkit.getServer().getPluginManager().callEvent(enterCityEvent);
                        CityDatabase.removeCityBalance(city, Metropolis.configuration.getCityClaimCost());
                        plugin.sendMessage(player, "messages.city.successful.claim", "%cityname%", city.getCityName(), "%amount%", Utilities.formattedMoney(Metropolis.configuration.getCityClaimCost()));
                    } else {
                        plugin.sendMessage(player, "messages.error.city.claimTooFar", "%cityname%", city.getCityName());
                    }

                    // Start autoclaiming
                    AutoclaimManager.startAutoclaim(player, city, autoclaimCount-1);
                    plugin.sendMessage(player, "messages.city.autoclaim.started", "%remaining%", String.valueOf(autoclaimCount), "%cityname%", cityName);
                    return;
                }
            } catch (NumberFormatException e) {
                plugin.sendMessage(player, "messages.syntax.city.claim");
                return;
            }
        }

        if (CityDatabase.getClaim(player.getLocation()) != null) {
            plugin.sendMessage(player, "messages.error.city.claimExists");
            return;
        }

        if (CityDatabase.getCityBalance(city) < Metropolis.configuration.getCityClaimCost()) {
            plugin.sendMessage(player, "messages.error.missing.claimCost", "%cityname%", city.getCityName(), "%cost%", ""+Metropolis.configuration.getCityClaimCost());
            return;
        }
        if (Utilities.isCloseToOtherCity(player, player.getLocation(), "city")) {
            plugin.sendMessage(player, "messages.error.city.tooCloseToOtherCity");
            return;
        }
        if (CityDatabase.getCityRole(city, player.getUniqueId().toString()) == null || Objects.equals(CityDatabase.getCityRole(city, player.getUniqueId().toString()), Role.MEMBER)
                || Objects.equals(
                CityDatabase.getCityRole(city, player.getUniqueId().toString()), Role.INVITER)) {
            plugin.sendMessage(player, "messages.error.city.permissionDenied", "%cityname%", cityName);
            return;
        }

        Claim claim1 = CityDatabase.getClaim(player.getLocation().add(16, 0, 0));
        Claim claim2 = CityDatabase.getClaim(player.getLocation().add(-16, 0, 0));
        Claim claim3 = CityDatabase.getClaim(player.getLocation().add(0, 0, 16));
        Claim claim4 = CityDatabase.getClaim(player.getLocation().add(0, 0, -16));

        if ((claim1 != null && claim1.getCity() == city) ||
                (claim2 != null && claim2.getCity() == city) ||
                (claim3 != null && claim3.getCity() == city) ||
                (claim4 != null && claim4.getCity() == city)) {
            Claim claim = CityDatabase.createClaim(city, player.getLocation(), false, player.getName(), player.getUniqueId().toString());
            assert claim != null;
            Database.addLogEntry(
                    city,
                    "{ \"type\": \"buy\", \"subtype\": \"claim\", \"balance\": "
                            + "500"
                            + ", \"claimlocation\": "
                            + LocationUtil.formatChunk(
                            claim.getClaimWorld().getName(), claim.getXPosition(), claim.getZPosition())
                            + ", \"player\": "
                            + player.getUniqueId().toString()
                            + " }");
            PlayerEnterCityEvent enterCityEvent = new PlayerEnterCityEvent(player, city);
            Bukkit.getServer().getPluginManager().callEvent(enterCityEvent);
            CityDatabase.removeCityBalance(city, Metropolis.configuration.getCityClaimCost());
            plugin.sendMessage(player, "messages.city.successful.claim", "%cityname%", city.getCityName(), "%amount%", Utilities.formattedMoney(Metropolis.configuration.getCityClaimCost()));
        } else {
            plugin.sendMessage(player, "messages.error.city.claimTooFar", "%cityname%", city.getCityName());
        }
    }

    @Subcommand("price")
    public static void onPrice(Player player) {
        if (player.hasPermission("metropolis.city.price")) {
            plugin.sendMessage(
                    player,
                    "messages.city.price",
                    "%city%",
                    Utilities.formattedMoney(plugin.getConfig().getInt("settings.city.creationcost")),
                    "%chunk%",
                    Utilities.formattedMoney(plugin.getConfig().getInt("settings.city.claimcost")),
                    "%bonus%",
                    Utilities.formattedMoney(plugin.getConfig().getInt("settings.city.bonuscost")),
                    "%go%",
                    Utilities.formattedMoney(plugin.getConfig().getInt("settings.city.citygocost")),
                    "%outpost%",
                    Utilities.formattedMoney(plugin.getConfig().getInt("settings.city.outpostcost")));
        } else {
            plugin.sendMessage(player, "messages.error.permissionDenied");
        }
    }

    @Subcommand("join")
    public static void onJoin(Player player, String cityname) {
        if (!player.hasPermission("metropolis.city.join")) {
            plugin.sendMessage(player, "messages.error.permissionDenied");
            return;
        }
        if (CityDatabase.getCity(cityname).isEmpty() || cityname == null) {
            plugin.sendMessage(player, "messages.error.missing.city");
            return;
        }
        City city = CityDatabase.getCity(cityname).get();
        if (CityDatabase.memberExists(player.getUniqueId().toString(), city)) {
            plugin.sendMessage(player, "messages.error.city.alreadyInCity");
            return;
        }
        if(CityDatabase.getCityBan(city, player.getUniqueId().toString()) != null) {
            plugin.sendMessage(player, "messages.error.city.banned");
            return;
        }
        if (!city.isOpen() && (!player.hasPermission("metropolis.admin.city.join") || !invites.containsKey(player) || invites.get(player) == null || !invites.get(player).equals(city))) {
            plugin.sendMessage(player, "messages.error.city.closed", "%cityname%", city.getCityName());
            return;
        }
        if (Objects.requireNonNull(CityDatabase.memberCityList(player.getUniqueId().toString())).length
                >= 3) {
            plugin.sendMessage(player, "messages.error.city.maxCityCount");
            return;
        }
        if (CityDatabase.getCityRole(city, player.getUniqueId().toString()) != null) {
            plugin.sendMessage(player, "messages.error.city.alreadyInACity");
            return;
        }
        CityDatabase.newMember(city, player);
        invites.remove(player, city);
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (CityDatabase.memberExists(player.getUniqueId().toString(), city)) {
                if (onlinePlayer == player) {
                    plugin.sendMessage(
                            onlinePlayer, "messages.city.successful.join.self", "%cityname%", city.getCityName());
                } else {
                    plugin.sendMessage(
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

    private static final HashMap<Player, City> invites = new HashMap<>();
    private static final HashMap<HashMap<UUID, City>, Integer> inviteCooldownTime = new HashMap<>();
    private static final HashMap<HashMap<UUID, City>, BukkitRunnable> inviteCooldownTask =
            new HashMap<>();

    @Subcommand("invite")
    @CommandCompletion("@players")
    public static void onInvite(Player player, String invitee) {
        City city = Utilities.hasCityPermissions(player, "metropolis.city.invite", Role.INVITER);
        if (city == null) {
            return;
        }
        Player inviteePlayer = Bukkit.getPlayer(invitee);
        if (inviteePlayer == null) {
            plugin.sendMessage(player, "messages.error.missing.player");
            return;
        }

        HashMap<UUID, City> uuidCityHashMap = new HashMap<>() {{put(inviteePlayer.getUniqueId(), city);}};
        if (CityDatabase.memberExists(inviteePlayer.getUniqueId().toString(), city)) {
            plugin.sendMessage(player, "messages.error.city.alreadyInACity");
            return;
        }
        if (CityDatabase.getCityBan(city, inviteePlayer.getUniqueId().toString()) != null) {
            plugin.sendMessage(player, "messages.error.city.playerBanned", "%cityname%", city.getCityName());
            return;
        }
        if (invites.containsKey(player) && invites.get(player).equals(city)) {
            plugin.sendMessage(
                    player, "messages.error.city.invite.alreadyInvited", "%cityname%", city.getCityName());
            return;
        }
        if (inviteePlayer == player) {
            plugin.sendMessage(player, "messages.error.city.invite.self", "%cityname%", city.getCityName());
            return;
        }
        if (!inviteCooldownTime.containsKey(uuidCityHashMap)) {
            invites.put(inviteePlayer, city);
            plugin.sendMessage(
                    player,
                    "messages.city.invite.invited",
                    "%player%",
                    inviteePlayer.getName(),
                    "%cityname%",
                    city.getCityName(),
                    "%inviter%",
                    player.getName());
            plugin.sendMessage(
                    inviteePlayer,
                    "messages.city.invite.inviteMessage",
                    "%cityname%",
                    city.getCityName(),
                    "%inviter%",
                    player.getName());
            inviteCooldownTime.put(uuidCityHashMap, Metropolis.configuration.getInviteCooldown());
            inviteCooldownTask.put(
                    uuidCityHashMap,
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            inviteCooldownTime.put(uuidCityHashMap, inviteCooldownTime.get(uuidCityHashMap) - 1);
                            if (inviteCooldownTime.get(uuidCityHashMap) == 0) {
                                inviteCooldownTime.remove(uuidCityHashMap);
                                inviteCooldownTask.remove(uuidCityHashMap);
                                invites.remove(inviteePlayer, city);
                                cancel();
                            }
                        }
                    });
            inviteCooldownTask.get(uuidCityHashMap).runTaskTimer(plugin, 20, 20);
        } else {
            if (inviteCooldownTime.get(uuidCityHashMap) > 0) {
                plugin.sendMessage(
                        player,
                        "messages.error.city.invite.cooldown",
                        "%playername%",
                        inviteePlayer.getName(),
                        "%time%",
                        inviteCooldownTime.get(uuidCityHashMap).toString());
            } else {
                invites.put(inviteePlayer, city);
                plugin.sendMessage(
                        player,
                        "messages.city.invite.invited",
                        "%player%",
                        inviteePlayer.getName(),
                        "%cityname%",
                        city.getCityName(),
                        "%inviter%",
                        player.getName());
                plugin.sendMessage(
                        inviteePlayer,
                        "messages.city.invite.inviteMessage",
                        "%cityname%",
                        city.getCityName(),
                        "%inviter%",
                        player.getName());
                inviteCooldownTime.put(uuidCityHashMap, Metropolis.configuration.getInviteCooldown());
                inviteCooldownTask.put(
                        uuidCityHashMap,
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                inviteCooldownTime.put(uuidCityHashMap, inviteCooldownTime.get(uuidCityHashMap) - 1);
                                if (inviteCooldownTime.get(uuidCityHashMap) == 0) {
                                    inviteCooldownTime.remove(uuidCityHashMap);
                                    inviteCooldownTask.remove(uuidCityHashMap);
                                    invites.remove(inviteePlayer, city);
                                    cancel();
                                }
                            }
                        });
                inviteCooldownTask.get(uuidCityHashMap).runTaskTimer(plugin, 20, 20);
            }
        }
    }

    @Subcommand("go")
    @CommandCompletion("@cityGoes @cityGo1 @cityGo2 @cityGo3")
    public static void onGo(Player player, String[] args) {
        if (!player.hasPermission("metropolis.city.go")) {
            plugin.sendMessage(player, "messages.error.permissionDenied");
            return;
        }
        if (HCDatabase.hasHomeCity(player.getUniqueId().toString())
                || HCDatabase.getHomeCityToCityname(player.getUniqueId().toString()) == null) {
            plugin.sendMessage(player, "messages.error.missing.homeCity");
            return;
        }
        City city = HCDatabase.getHomeCityToCity(player.getUniqueId().toString());
        assert city != null;
        Role role = CityDatabase.getCityRole(city, player.getUniqueId().toString());
        if (role == null) {
            plugin.sendMessage(
                    player, "messages.error.city.permissionDenied", "%cityname%", city.getCityName());
            return;
        }
        boolean isInviter = role.equals(Role.INVITER) || role.equals(Role.ASSISTANT) || role.equals(Role.VICE_MAYOR) || role.equals(Role.MAYOR);
        boolean isAssistant = role.equals(Role.ASSISTANT) || role.equals(Role.VICE_MAYOR) || role.equals(Role.MAYOR);
        boolean isViceMayor = role.equals(Role.VICE_MAYOR) || role.equals(Role.MAYOR);
        boolean isMayor = role.equals(Role.MAYOR);
        if (args.length == 0
                || args.length == 1 && !args[0].replaceAll("[0-9]", "").matches("[^0-9].*")) {
            if (!player.hasPermission("metropolis.city.go.list")) {
                plugin.sendMessage(player, "messages.error.permissionDenied");
                return;
            }
            if (CityDatabase.getCityGoCount(city, role) == 0) {
                plugin.sendMessage(player, "messages.error.missing.goes");
                return;
            }
            if (args.length == 0) {
                args = new String[] {"1"};
            }
            if (args[0] == null) {
                args[0] = "1";
            }
            int goInt = Integer.parseInt(args[0]);
            StringBuilder tmpMessage = new StringBuilder();

            int itemsPerPage = 25;
            int start = (goInt * itemsPerPage) - itemsPerPage;
            int stop = goInt * itemsPerPage;
            if (Integer.parseInt(args[0]) < 1
                    || Integer.parseInt(args[0])
                    > (int)
                    Math.ceil(
                            ((double) CityDatabase.getCityGoCount(city, role))
                                    / ((double) itemsPerPage))) {
                plugin.sendMessage(player, "messages.error.missing.page");
                return;
            }
            if (start >= CityDatabase.getCityGoCount(city, role)) {
                plugin.sendMessage(player, "messages.error.missing.page");
                return;
            }

            for (int i = start; i < stop; i++) {
                if (i == CityDatabase.getCityGoCount(city, role)) {
                    break;
                }
                String name = Objects.requireNonNull(CityDatabase.getCityGoNames(city, role)).get(i);
                if (name == null) {
                    break;
                }
                tmpMessage.append(name).append("§2,§a ");
            }
            plugin.sendMessage(
                    player,
                    "messages.list.goes",
                    "%startPage%",
                    String.valueOf(goInt),
                    "%totalPages%",
                    String.valueOf(
                            (int)
                                    Math.ceil(
                                            ((double) CityDatabase.getCityGoCount(city, role))
                                                    / ((double) itemsPerPage))));
            player.sendMessage("§a" + tmpMessage.substring(0, tmpMessage.length() - 4));

        } else if (args.length == 2) {
            if (!CityDatabase.cityGoExists(args[0], city)) {
                plugin.sendMessage(player, "messages.error.missing.go", "%cityname%", city.getCityName());
                return;
            }
            if (!args[1].equals("delete")) {
                plugin.sendMessage(player, "messages.syntax.city.go");
                return;
            }
            if (!player.hasPermission("metropolis.city.go.delete")) {
                plugin.sendMessage(player, "messages.error.permissionDenied");
                return;
            }
            if (!CityDatabase.cityGoExists(args[0], city)) {
                plugin.sendMessage(player, "messages.error.missing.go", "%cityname%", city.getCityName());
                return;
            }

            String goAccessLevel = CityDatabase.getCityGoAccessLevel(args[0], city);
            if (goAccessLevel == null
                    || goAccessLevel.equals("inviter")
                    || goAccessLevel.equals("assistant")
                    || goAccessLevel.equals("vicemayor")) {
                if (!isViceMayor) {
                    plugin.sendMessage(
                            player, "messages.error.city.permissionDenied", "%cityname%", city.getCityName());
                    return;
                }
                String goName = CityDatabase.getCityGoDisplayname(args[0], city);
                CityDatabase.deleteGo(args[0], city);
                Database.addLogEntry(
                        city,
                        "{ \"type\": \"delete\", \"subtype\": \"go\", \"name\": "
                                + args[0]
                                + ", \"player\": "
                                + player.getUniqueId().toString()
                                + " }");
                plugin.sendMessage(player, "messages.city.successful.delete.citygo", "%cityname%", city.getCityName(), "%name%", goName);
            }

        } else if (args.length == 4) {
            if (!CityDatabase.cityGoExists(args[0], city)) {
                plugin.sendMessage(player, "messages.error.missing.go", "%cityname%", city.getCityName());
                return;
            }
            if (!args[1].equals("set")) {
                plugin.sendMessage(player, "messages.syntax.city.go");
                return;
            }
            String goAccessLevel = CityDatabase.getCityGoAccessLevel(args[0], city);
            if (args[2].equals("displayname")) {
                if (!isViceMayor) {
                    plugin.sendMessage(
                            player, "messages.error.city.permissionDenied", "%cityname%", city.getCityName());
                    return;
                }
                if (args[3].length() > Metropolis.configuration.getCityGoDisplayNameLimit()) {
                    plugin.sendMessage(
                            player,
                            "messages.error.city.go.invalidDisplayname", "%cityname%", city.getCityName(), "%maxlength%", String.valueOf(Metropolis.configuration.getCityGoDisplayNameLimit()));
                    return;
                }
                if (Objects.equals(CityDatabase.getCityGoDisplayname(args[0], city), args[3])) {
                    plugin.sendMessage(
                            player,
                            "messages.error.city.go.sameDisplayname",
                            "%cityname%",
                            city.getCityName(),
                            "%name%",
                            args[3]);
                    return;
                }
                CityDatabase.setCityGoDisplayname(args[0], city, args[3]);
                plugin.sendMessage(
                        player,
                        "messages.city.go.changedDisplayname",
                        "%cityname%",
                        city.getCityName(),
                        "%name%",
                        args[0]);
            }
            if (args[2].equals("name")) {
                if (!isViceMayor) {
                    plugin.sendMessage(
                            player, "messages.error.city.permissionDenied", "%cityname%", city.getCityName());
                    return;
                }
                final String regex = "[^\\p{L}_0-9\\\\-]+";
                final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
                final Matcher matcher = pattern.matcher(args[3]);
                if (matcher.find() || args[3].matches("^[0-9].*") || args[3].length() > Metropolis.configuration.getCityGoNameLimit()) {
                    plugin.sendMessage(player, "messages.error.city.go.invalidName","%maxlength%", String.valueOf(Metropolis.configuration.getCityGoNameLimit()));
                    return;
                }
                if (CityDatabase.cityGoExists(args[3], city)) {
                    plugin.sendMessage(
                            player, "messages.error.city.go.alreadyExists", "%cityname%", city.getCityName());
                    return;
                }
                CityDatabase.setCityGoName(args[0], city, args[3]);
                plugin.sendMessage(
                        player,
                        "messages.city.go.changedName",
                        "%cityname%",
                        city.getCityName(),
                        "%name%",
                        args[0]);
            }
            if (args[2].equals("accesslevel")) {
                if (!args[3].equals("-")
                        && !args[3].equals("inviter")
                        && !args[3].equals("assistant")
                        && !args[3].equals("vicemayor")
                        && !args[3].equals("mayor")) {
                    plugin.sendMessage(
                            player,
                            "messages.error.city.go.invalidAccessLevel",
                            "%cityname%",
                            city.getCityName());
                    return;
                }
                switch (args[3]) {
                    case "-" -> {
                        if (!isAssistant) {
                            plugin.sendMessage(
                                    player, "messages.error.city.permissionDenied", "%cityname%", city.getCityName());
                            return;
                        }
                        if (goAccessLevel == null) {
                            plugin.sendMessage(
                                    player,
                                    "messages.error.city.go.alreadyAccessLevel",
                                    "%cityname%",
                                    city.getCityName());
                            return;
                        }
                        CityDatabase.setCityGoAccessLevel(args[0], city, null);
                        plugin.sendMessage(
                                player,
                                "messages.city.go.changedAccessLevel",
                                "%cityname%",
                                city.getCityName(),
                                "%name%",
                                args[0],
                                "%accesslevel%",
                                "Medlemmar");
                        return;
                    }
                    case "inviter" -> {
                        if (!isAssistant) {
                            plugin.sendMessage(
                                    player, "messages.error.city.permissionDenied", "%cityname%", city.getCityName());
                            return;
                        }
                        if (goAccessLevel != null && goAccessLevel.equals("inviter")) {
                            plugin.sendMessage(
                                    player,
                                    "messages.error.city.go.alreadyAccessLevel",
                                    "%cityname%",
                                    city.getCityName());
                            return;
                        }
                        CityDatabase.setCityGoAccessLevel(args[0], city, args[3]);
                        plugin.sendMessage(
                                player,
                                "messages.city.go.changedAccessLevel",
                                "%cityname%",
                                city.getCityName(),
                                "%name%",
                                args[0],
                                "%accesslevel%",
                                "Inbjudare");
                        return;
                    }
                    case "assistant" -> {
                        if (!isAssistant) {
                            plugin.sendMessage(
                                    player, "messages.error.city.permissionDenied", "%cityname%", city.getCityName());
                            return;
                        }
                        if (goAccessLevel != null && goAccessLevel.equals("assistant")) {
                            plugin.sendMessage(
                                    player,
                                    "messages.error.city.go.alreadyAccessLevel",
                                    "%cityname%",
                                    city.getCityName());
                            return;
                        }
                        CityDatabase.setCityGoAccessLevel(args[0], city, args[3]);
                        plugin.sendMessage(
                                player,
                                "messages.city.go.changedAccessLevel",
                                "%cityname%",
                                city.getCityName(),
                                "%name%",
                                args[0],
                                "%accesslevel%",
                                "Assistenter");
                        return;
                    }
                    case "vicemayor" -> {
                        if (!isViceMayor) {
                            plugin.sendMessage(
                                    player, "messages.error.city.permissionDenied", "%cityname%", city.getCityName());
                            return;
                        }
                        if (goAccessLevel != null && goAccessLevel.equals("vicemayor")) {
                            plugin.sendMessage(
                                    player,
                                    "messages.error.city.go.alreadyAccessLevel",
                                    "%cityname%",
                                    city.getCityName());
                            return;
                        }
                        CityDatabase.setCityGoAccessLevel(args[0], city, args[3]);
                        plugin.sendMessage(
                                player,
                                "messages.city.go.changedAccessLevel",
                                "%cityname%",
                                city.getCityName(),
                                "%name%",
                                args[0],
                                "%accesslevel%",
                                "Vice Borgmästare");
                        return;
                    }
                    case "mayor" -> {
                        if (!isMayor) {
                            plugin.sendMessage(
                                    player, "messages.error.city.permissionDenied", "%cityname%", city.getCityName());
                            return;
                        }
                        if (goAccessLevel != null && goAccessLevel.equals("mayor")) {
                            plugin.sendMessage(
                                    player,
                                    "messages.error.city.go.alreadyAccessLevel",
                                    "%cityname%",
                                    city.getCityName());
                            return;
                        }
                        CityDatabase.setCityGoAccessLevel(args[0], city, args[3]);
                        plugin.sendMessage(
                                player,
                                "messages.city.go.changedAccessLevel",
                                "%cityname%",
                                city.getCityName(),
                                "%name%",
                                args[0],
                                "%accesslevel%",
                                "Borgmästare");
                        return;
                    }
                }
            }
            plugin.sendMessage(player, "messages.syntax.city.go");
        } else if (args.length == 1) {
            if (!player.hasPermission("metropolis.city.go.teleport")) {
                plugin.sendMessage(player, "messages.error.permissionDenied");
                return;
            }
            if (!CityDatabase.cityGoExists(args[0], city)) {
                plugin.sendMessage(player, "messages.error.missing.go", "%cityname%", city.getCityName());
                return;
            }
            String goAccessLevel = CityDatabase.getCityGoAccessLevel(args[0], city);
            if (goAccessLevel == null) {
                Location location = CityDatabase.getCityGoLocation(args[0], city);
                assert location != null;
                player.teleport(location);
                // Istället för player.teleport här så ska vi ha en call till Mandatory, som sköter VIP
                // teleportering.
                return;
            }
            boolean hasAccess = false;
            switch (goAccessLevel) {
                case "mayor" -> {
                    if (isMayor) {
                        hasAccess = true;
                    }
                }
                case "vicemayor" -> {
                    if (isViceMayor) {
                        hasAccess = true;
                    }
                }
                case "assistant" -> {
                    if (isAssistant) {
                        hasAccess = true;
                    }
                }
                case "inviter" -> {
                    if (isInviter) {
                        hasAccess = true;
                    }
                }
            }
            if (!hasAccess) {
                plugin.sendMessage(
                        player, "messages.error.city.permissionDenied", "%cityname%", city.getCityName());
                return;
            }
            Location location = CityDatabase.getCityGoLocation(args[0], city);
            assert location != null;
            player.teleport(location);
            // Istället för player.teleport här så ska vi ha en call till Mandatory, som sköter VIP
            // teleportering.
        } else {
            plugin.sendMessage(player, "messages.syntax.city.go");
        }
    }

    @Subcommand("set")
    public static void onSet(Player player) {
        if (!player.hasPermission("metropolis.city.set")) {
            plugin.sendMessage(player, "messages.error.permissionDenied");
            return;
        }
        plugin.sendMessage(player, "messages.syntax.city.set.enter");
        plugin.sendMessage(player, "messages.syntax.city.set.exit");
        plugin.sendMessage(player, "messages.syntax.city.set.maxplotspermember");
        plugin.sendMessage(player, "messages.syntax.city.set.motd");
        plugin.sendMessage(player, "messages.syntax.city.set.name");
        plugin.sendMessage(player, "messages.syntax.city.set.spawn");
        plugin.sendMessage(player, "messages.syntax.city.set.tax");
    }

    @Subcommand("set")
    public class Set extends BaseCommand {

        @Subcommand("enter")
        public static void onEnter(Player player, String message) {
            City city = Utilities.hasCityPermissions(player, "metropolis.city.set.enter", Role.VICE_MAYOR);
            if (city == null) {
                return;
            }
            if (message.equals("-")) {
                city.setEnterMessage(null);
                Database.addLogEntry(
                        city,
                        "{ \"type\": \"set\", \"subtype\": \"enterMessage\", \"message\": "
                                + null
                                + ", \"player\": "
                                + player.getUniqueId().toString()
                                + " }");
                plugin.sendMessage(
                        player, "messages.city.successful.set.enter.removed", "%cityname%", city.getCityName());
                return;
            }
            if (message.length() > Metropolis.configuration.getCityEnterMessageLimit()) {
                plugin.sendMessage(
                        player,
                        "messages.error.city.messageTooLong",
                        "%cityname%",
                        city.getCityName(),
                        "%count%",
                        String.valueOf(Metropolis.configuration.getCityEnterMessageLimit()));
                return;
            }
            city.setEnterMessage(message);
            Database.addLogEntry(
                    city,
                    "{ \"type\": \"set\", \"subtype\": \"enterMessage\", \"message\": "
                            + message
                            + ", \"player\": "
                            + player.getUniqueId().toString()
                            + " }");
            plugin.sendMessage(
                    player, "messages.city.successful.set.enter.set", "%cityname%", city.getCityName());
        }

        @Subcommand("exit")
        public static void onExit(Player player, String message) {
            City city = Utilities.hasCityPermissions(player, "metropolis.city.set.exit", Role.VICE_MAYOR);
            if (city == null) {
                return;
            }
            if (message.equals("-")) {
                city.setExitMessage(null);
                Database.addLogEntry(
                        city,
                        "{ \"type\": \"set\", \"subtype\": \"exitMessage\", \"message\": "
                                + null
                                + ", \"player\": "
                                + player.getUniqueId().toString()
                                + " }");
                plugin.sendMessage(
                        player, "messages.city.successful.set.exit.removed", "%cityname%", city.getCityName());
                return;
            }
            if (message.length() > Metropolis.configuration.getCityExitMessageLimit()) {
                plugin.sendMessage(
                        player,
                        "messages.error.city.messageTooLong",
                        "%cityname%",
                        city.getCityName(),
                        "%count%",
                        String.valueOf(Metropolis.configuration.getCityExitMessageLimit()));
                return;
            }
            city.setExitMessage(message);
            Database.addLogEntry(
                    city,
                    "{ \"type\": \"set\", \"subtype\": \"exitMessage\", \"message\": "
                            + message
                            + ", \"player\": "
                            + player.getUniqueId().toString()
                            + " }");
            plugin.sendMessage(
                    player, "messages.city.successful.set.exit.set", "%cityname%", city.getCityName());
        }

        @Subcommand("motd")
        public static void onMotd(Player player, String message) {
            City city = Utilities.hasCityPermissions(player, "metropolis.city.set.motd", Role.VICE_MAYOR);
            if (city == null) {
                return;
            }
            if (message.equals("-")) {
                city.setMotdMessage(null);
                Database.addLogEntry(
                        city,
                        "{ \"type\": \"set\", \"subtype\": \"motdMessage\", \"message\": "
                                + null
                                + ", \"player\": "
                                + player.getUniqueId().toString()
                                + " }");
                plugin.sendMessage(
                        player, "messages.city.successful.set.motd.removed", "%cityname%", city.getCityName());
                return;
            }
            if (message.length() > Metropolis.configuration.getCityMotdLimit()) {
                plugin.sendMessage(
                        player,
                        "messages.error.city.messageTooLong",
                        "%cityname%",
                        city.getCityName(),
                        "%count%",
                        String.valueOf(Metropolis.configuration.getCityMotdLimit()));
                return;
            }
            city.setMotdMessage(message);
            Database.addLogEntry(
                    city,
                    "{ \"type\": \"set\", \"subtype\": \"motdMessage\", \"message\": "
                            + message
                            + ", \"player\": "
                            + player.getUniqueId().toString()
                            + " }");
            plugin.sendMessage(
                    player, "messages.city.successful.set.motd.set", "%cityname%", city.getCityName());
        }
        @Subcommand("spawn")
        public static void onSpawn(Player player) {
            City city = Utilities.hasCityPermissions(player, "metropolis.city.set.spawn", Role.VICE_MAYOR);
            if (city == null) {
                return;
            }
            Claim claim = CityDatabase.getClaim(player.getLocation());
            if (claim == null || !claim.getCity().equals(city)) {
                plugin.sendMessage(player, "messages.error.city.set.spawn.outsideCity", "%cityname%", city.getCityName());
                return;
            }
            Database.addLogEntry(
                    city,
                    "{ \"type\": \"set\", \"subtype\": \"spawn\", \"from\": "
                            + "([" + city.getCitySpawn().getWorld().getName() + "]" + city.getCitySpawn().getX() + ", " + city.getCitySpawn().getY() + ", " + city.getCitySpawn().getZ() + ")"
                            + ", \"to\": "
                            + "([" + player.getLocation().getWorld().getName() + "]" + player.getLocation().getX() + ", " + player.getLocation().getY() + ", " + player.getLocation().getZ() + ")"
                            + ", \"player\": "
                            + player.getUniqueId().toString()
                            + " }");
            city.setCitySpawn(player.getLocation());
            plugin.sendMessage(player, "messages.city.successful.set.spawn", "%cityname%", city.getCityName());
        }

        @Subcommand("name")
        public static void onName(Player player, String name) {
            City city = Utilities.hasCityPermissions(player, "metropolis.city.set.name", Role.MAYOR);
            if (city == null) {
                return;
            }
            if (name.isEmpty() || name.length() > Metropolis.configuration.getCityNameLimit()) {
                plugin.sendMessage(player, "messages.error.city.nameLength","%maxlength%", String.valueOf(Metropolis.configuration.getCityNameLimit()));
                return;
            }
            if (!name.matches("[A-Za-zÀ-ÖØ-öø-ÿ0-9 ]+")) {
                plugin.sendMessage(player, "messages.error.city.invalidName");
                return;
            }
            if (CityDatabase.getCity(name).isPresent()) {
                plugin.sendMessage(player, "messages.error.city.cityExists");
                return;
            }

            int latestNameChange = CityDatabase.getLatestNameChange(city);
            int cooldownTime = plugin.getConfig().getInt("settings.cooldownTime.namechange"); // Time in seconds

            if (latestNameChange != 0) {
                int currentTime = (int) (System.currentTimeMillis() / 1000); // Convert to seconds
                int timeSinceLastChange = currentTime - latestNameChange;

                if (timeSinceLastChange < cooldownTime) {
                    plugin.sendMessage(player, "messages.error.namechange.cooldown", "%timeleft%", DateUtil.formatTimeFromSeconds(cooldownTime - timeSinceLastChange));
                    return;
                }
            }

            Database.addLogEntry(
                    city,
                    "{ \"type\": \"set\", \"subtype\": \"name\", \"from\": "
                            + city.getCityName()
                            + ", \"to\": "
                            + name
                            + ", \"player\": "
                            + player.getUniqueId().toString()
                            + " }");
            city.setCityName(name);
            CityDatabase.setLatestNameChange(city, (int) (System.currentTimeMillis() / 1000));
            plugin.sendMessage(player, "messages.city.successful.set.name", "%cityname%", name);
        }

        @Subcommand("tax")
        public static void onTax(Player player, double tax) {
            City city = Utilities.hasCityPermissions(player, "metropolis.city.set.tax", Role.VICE_MAYOR);
            if (city == null) {
                return;
            }
            double maxTax = Metropolis.configuration.getCityMaxTax();
            if (tax < 0) {
                plugin.sendMessage(player, "messages.error.city.tax.invalidAmount", "%max%", String.valueOf(maxTax));
                return;
            }

            tax = Math.round(tax * 100.0) / 100.0;

            if (tax > maxTax) {
                plugin.sendMessage(player, "messages.error.city.tax.maxAmount", "%max%", String.valueOf(maxTax));
                return;
            }
            Database.addLogEntry(
                    city,
                    "{ \"type\": \"set\", \"subtype\": \"tax\", \"from\": "
                            + city.getCityTax()
                            + ", \"to\": "
                            + tax
                            + ", \"player\": "
                            + player.getUniqueId().toString()
                            + " }");
            city.setCityTax(tax);
            plugin.sendMessage(player, "messages.city.successful.set.tax", "%cityname%", city.getCityName(), "%tax%", String.valueOf(tax));
        }
    }

    @Subcommand("buy")
    public static void onBuy(Player player) {
        if (player.hasPermission("metropolis.city.buy")) {
            plugin.sendMessage(player, "messages.error.permissionDenied");
            return;
        }
        plugin.sendMessage(player, "messages.syntax.city.buy.bonus");
        plugin.sendMessage(player, "messages.syntax.city.buy.district");
        plugin.sendMessage(player, "messages.syntax.city.buy.go");
        plugin.sendMessage(player, "messages.syntax.city.buy.outpost");
    }

    @Subcommand("buy")
    public class Buy extends BaseCommand {

        @Subcommand("bonus")
        public static void onBonus(Player player, int count) {
            City city = Utilities.hasCityPermissions(player, "metropolis.city.buy.bonus", Role.VICE_MAYOR);
            if (city == null) {
                return;
            }
            if (count < 1) {
                plugin.sendMessage(player, "messages.error.city.bonus.invalidAmount");
                return;
            }
            if (city.getCityBalance() < Metropolis.configuration.getCityBonusCost() * count) {
                plugin.sendMessage(
                        player, "messages.error.city.missing.balance.bonusCost", "%cityname%", city.getCityName(), "%amount%", Utilities.formattedMoney(Metropolis.configuration.getCityBonusCost() * count), "%count%", String.valueOf(count));
                return;
            }
            city.addBonusClaims(count);
            city.removeCityBalance(Metropolis.configuration.getCityBonusCost() * count);
            Database.addLogEntry(
                    city,
                    "{ \"type\": \"buy\", \"subtype\": \"bonus\", \"count\": "
                            + count
                            + ", \"player\": "
                            + player.getUniqueId().toString()
                            + ", \"balance\": "
                            + Metropolis.configuration.getCityBonusCost() * count
                            + " }");
            plugin.sendMessage(
                    player,
                    "messages.city.successful.bonus",
                    "%cityname%",
                    city.getCityName(),
                    "%amount%",
                    Utilities.formattedMoney(Metropolis.configuration.getCityBonusCost() * count), "%count%", String.valueOf(count));
        }

        @Subcommand("district")
        public static void onDistrict(Player player, String name) {
            City city = Utilities.hasCityPermissions(player, "metropolis.city.buy.district", Role.VICE_MAYOR);
            if (city == null) {
                return;
            }
            final String regex = "[^\\p{L}_\\\\\\-]+";
            final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
            final Matcher matcher = pattern.matcher(name);
            if (matcher.find()
                    || name.matches("^[0-9].*")
                    || name.length() > Metropolis.configuration.getDistrictNameLimit()
                    || name.startsWith("-")) {
                plugin.sendMessage(
                        player, "messages.error.city.district.invalidName", "%cityname%", city.getCityName(), "%maxlength%", String.valueOf(Metropolis.configuration.getDistrictNameLimit()));
                return;
            }
            if (city.getCityBalance() < Metropolis.configuration.getDistrictCreationCost()) {
                plugin.sendMessage(
                        player, "messages.error.city.missing.balance.districtCost", "%cityname%", city.getCityName());
                return;
            }
            if (CityDatabase.districtExists(name, city)) {
                plugin.sendMessage(
                        player, "messages.error.city.district.alreadyExists", "%cityname%", city.getCityName());
                return;
            }
            if (CityDatabase.getCityByClaim(player.getLocation()) != city) {
                plugin.sendMessage(player, "messages.error.city.district.outsideCity", "%cityname%", city.getCityName());
                return;
            }
            if (!MetropolisListener.playerPolygons.containsKey(player.getUniqueId())) {
                plugin.sendMessage(player, "messages.error.missing.plot");
                return;
            }

            Polygon regionPolygon = MetropolisListener.playerPolygons.get(player.getUniqueId());
            Location[] locations = MetropolisListener.savedLocs.get(player.getUniqueId()).toArray(new Location[0]);
            double minX = regionPolygon.getEnvelopeInternal().getMinX();
            double maxX = regionPolygon.getEnvelopeInternal().getMaxX();
            double minY = regionPolygon.getEnvelopeInternal().getMinY();
            double maxY = regionPolygon.getEnvelopeInternal().getMaxY();
            if (maxX - minX < 3 || maxY - minY < 3) {
                plugin.sendMessage(player, "messages.error.plot.tooSmall");
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
                        if (!Utilities.containsOnlyCompletePlots(regionPolygon, -64, 319, city, player.getWorld())) {
                            plugin.sendMessage(player, "messages.error.city.district.plotsNotCompletelyInside");
                            return;
                        }
                        CityDatabase.createDistrict(city, regionPolygon, name, player.getWorld());
                        city.removeCityBalance(Metropolis.configuration.getDistrictCreationCost());
                        Database.addLogEntry(
                                city,
                                "{ \"type\": \"buy\", \"subtype\": \"district\", \"name\": "
                                        + name
                                        + ", \"player\": "
                                        + player.getUniqueId().toString()
                                        + ", \"balance\": "
                                        + Metropolis.configuration.getDistrictCreationCost()
                                        + ", \"districtBounds\": "
                                        + regionPolygon.toText()
                                        + " }");
                        plugin.sendMessage(
                                player, "messages.city.district.created", "%cityname%", city.getCityName(), "%districtname%", name);
                        break;
                    }
                    }
            }
        }

        @Subcommand("go")
        public static void onGo(Player player, String name) {
            City city = Utilities.hasCityPermissions(player, "metropolis.city.buy.go", Role.VICE_MAYOR);
            if (city == null) {
                return;
            }
            final String regex = "[^\\p{L}_0-9\\\\-]+";
            final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
            final Matcher matcher = pattern.matcher(name);
            if (matcher.find()
                    || name.matches("^[0-9].*")
                    || name.length() > Metropolis.configuration.getCityGoNameLimit()
                    || name.startsWith("-")) {
                plugin.sendMessage(
                        player, "messages.error.city.go.invalidName", "%cityname%", city.getCityName(), "%maxlength%", String.valueOf(Metropolis.configuration.getCityGoNameLimit()));
                return;
            }
            if (city.getCityBalance() < Metropolis.configuration.getCityGoCost()) {
                plugin.sendMessage(
                        player, "messages.error.city.missing.balance.goCost", "%cityname%", city.getCityName());
                return;
            }
            if (CityDatabase.cityGoExists(name, city)) {
                plugin.sendMessage(
                        player, "messages.error.city.go.alreadyExists", "%cityname%", city.getCityName());
                return;
            }
            if (CityDatabase.getCityByClaim(player.getLocation()) != city) {
                plugin.sendMessage(player, "messages.error.city.go.outsideCity", "%cityname%", city.getCityName());
                return;
            }
            CityDatabase.newCityGo(player.getLocation(), name, city);
            city.removeCityBalance(Metropolis.configuration.getCityGoCost());
            Database.addLogEntry(
                    city,
                    "{ \"type\": \"buy\", \"subtype\": \"go\", \"name\": "
                            + name
                            + ", \"player\": "
                            + player.getUniqueId().toString()
                            + ", \"balance\": "
                            + Metropolis.configuration.getCityGoCost()
                            + ", \"claimlocation\": "
                            + LocationUtil.formatLocation(player.getLocation())
                            + " }");
            plugin.sendMessage(
                    player, "messages.city.go.created", "%cityname%", city.getCityName(), "%name%", name);
        }

        @Subcommand("outpost")
        public static void onOutpost(Player player) {
            City city = Utilities.hasCityPermissions(player, "metropolis.city.buy.outpost", Role.VICE_MAYOR);
            if (city == null) {
                return;
            }
            if (city.getCityBalance() < Metropolis.configuration.getCityOutpostCost()) {
                plugin.sendMessage(
                        player, "messages.error.city.missing.balance.outpostCost", "%cityname%", city.getCityName());
                return;
            }
            Claim claim = CityDatabase.createClaim(city, player.getLocation(), true, player.getName(), player.getUniqueId().toString());
            assert claim != null;
            city.removeCityBalance(Metropolis.configuration.getCityOutpostCost());
            Database.addLogEntry(
                    city,
                    "{ \"type\": \"buy\", \"subtype\": \"outpost\", \"player\": "
                            + player.getUniqueId().toString()
                            + ", \"balance\": "
                            + Metropolis.configuration.getCityOutpostCost()
                            + ", \"claimlocation\": "
                            + LocationUtil.formatLocation(player.getLocation())
                            + " }");
            PlayerEnterCityEvent enterCityEvent = new PlayerEnterCityEvent(player, city);
            Bukkit.getServer().getPluginManager().callEvent(enterCityEvent);
            plugin.sendMessage(player, "messages.city.successful.outpost", "%cityname%", city.getCityName(), "%amount%", Utilities.formattedMoney(Metropolis.configuration.getCityOutpostCost()));
        }
    }

    public static final List<Player> blockEnabled = new ArrayList<>();

    @Subcommand("block")
    public void onBlock(Player player, @Optional Integer page) {
        if (!player.hasPermission("metropolis.city.block")) {
            plugin.sendMessage(player, "messages.error.permissionDenied");
            return;
        }
        if (getCoreProtect() == null) {
            Bukkit.getLogger().severe("[Metropolis] CoreProtect not found.");
            player.sendMessage("§cSomething went wrong. Please contact an administrator.");
            return;
        }
        if (page == null) {
            if (blockEnabled.contains(player)) {
                blockEnabled.remove(player);
                MetropolisListener.savedBlockHistory.remove(player.getUniqueId());
                plugin.sendMessage(player, "messages.block.disabled");
                return;
            } else {
                blockEnabled.add(player);
                plugin.sendMessage(player, "messages.block.enabled");
            }
            if (HCDatabase.hasHomeCity(player.getUniqueId().toString())
                    || HCDatabase.getHomeCityToCityname(player.getUniqueId().toString()) == null) {
                plugin.sendMessage(player, "messages.error.missing.homeCity");
            }
        } else {
            if (!MetropolisListener.savedBlockHistory.containsKey(player.getUniqueId())) {
                plugin.sendMessage(player, "messages.error.missing.blockHistory");
                return;
            }
            if (page < 1) {
                plugin.sendMessage(player, "messages.error.missing.page");
                return;
            }
            int itemsPerPage = 8;
            int start = (page * itemsPerPage) - itemsPerPage;
            int stop = page * itemsPerPage;

            if (start >= MetropolisListener.savedBlockHistory.get(player.getUniqueId()).size()) {
                plugin.sendMessage(player, "messages.error.missing.page");
                return;
            }
            String[] firstLine = MetropolisListener.savedBlockHistory.get(player.getUniqueId()).get(0);
            CoreProtectAPI.ParseResult firstResult = getCoreProtect().parseResult(firstLine);
            player.sendMessage("");
            plugin.sendMessage(
                    player,
                    "messages.city.blockhistory.header",
                    "%location%",
                    "(["
                            + firstResult.worldName()
                            + "]"
                            + firstResult.getX()
                            + ","
                            + firstResult.getY()
                            + ","
                            + firstResult.getZ()
                            + ")",
                    "%page%",
                    page.toString(),
                    "%totalpages%",
                    String.valueOf(
                            (int)
                                    Math.ceil(
                                            ((double)
                                                    MetropolisListener.savedBlockHistory.get(player.getUniqueId()).size())
                                                    / ((double) itemsPerPage))));
            for (int i = start; i < stop; i++) {
                if (i >= MetropolisListener.savedBlockHistory.get(player.getUniqueId()).size()) {
                    break;
                }
                CoreProtectAPI.ParseResult result =
                        getCoreProtect()
                                .parseResult(MetropolisListener.savedBlockHistory.get(player.getUniqueId()).get(i));
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
        }
    }

    @Subcommand("helpop")
    public static void onHelpop(Player player, String message) {
        City city = Utilities.hasCityPermissions(player, "metropolis.city.helpop", Role.MEMBER);
        if (city == null) {
            return;
        }
        if (message.length() < 5) {
            plugin.sendMessage(player, "messages.error.city.helpop.tooShort");
            return;
        }
        int cityStaffOnline = 0;
        boolean isCityStaff =
                Objects.equals(CityDatabase.getCityRole(city, String.valueOf(player.getUniqueId())), Role.MAYOR) || Objects.equals(
                        CityDatabase.getCityRole(city, String.valueOf(player.getUniqueId())), Role.VICE_MAYOR)
                        || Objects.equals(
                        CityDatabase.getCityRole(city, String.valueOf(player.getUniqueId())), Role.ASSISTANT);
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (CityDatabase.memberExists(online.getUniqueId().toString(), city) && isCityStaff) {
                cityStaffOnline++;
                plugin.sendMessage(online, "messages.city.helpop.receive", "%cityname%", city.getCityName(), "%player%", player.getName(), "%message%", message);
            }
        }
        if (cityStaffOnline == 0) {
            plugin.sendMessage(player, "messages.city.helpop.noStaffOnline", "%cityname%", city.getCityName());
            return;
        }
        plugin.sendMessage(player, "messages.city.helpop.sent", "%cityname%", city.getCityName());
    }

    @Subcommand("leave")
    public static void onLeave(Player player, String cityname) throws SQLException {
        City city = Utilities.hasCityPermissions(player, "metropolis.city.leave", Role.MEMBER);
        if (city == null) {
            return;
        }
//        if (!CityDatabase.memberExists(player.getUniqueId().toString(), CityDatabase.getCity(cityname).get())) {
//            plugin.sendMessage(player, "messages.error.city.notInCity");
//            return;
//        }
        if (Objects.equals(CityDatabase.getCityRole(city, player.getUniqueId().toString()), Role.MAYOR)) {
            plugin.sendMessage(player, "messages.error.city.leave.mayor", "%cityname%", cityname);
            return;
        }
        city.removeCityMember(city.getCityMember(player.getUniqueId().toString()));
        Database.addLogEntry(
                city,
                "{ \"type\": \"leave\", \"player\": "
                        + player.getUniqueId().toString()
                        + " }");
        Utilities.sendScoreboard(player);
        plugin.sendMessage(player, "messages.city.leave.success", "%cityname%", cityname);
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (CityDatabase.memberExists(online.getUniqueId().toString(), city)) {
                if (!online.getUniqueId().equals(player.getUniqueId())) {
                    plugin.sendMessage(
                            online,
                            "messages.city.leave.message",
                            "%player%",
                            player.getName(),
                            "%cityname%",
                            cityname);
                }
            }
        }
    }

    @Subcommand("members")
    public static void onMember(Player player, String cityname) {
        if (!player.hasPermission("metropolis.city.members")) {
            plugin.sendMessage(player, "messages.error.permissionDenied");
            return;
        }
        if (CityDatabase.getCity(cityname).isEmpty()) {
            plugin.sendMessage(player, "messages.error.missing.city");
            return;
        }
        City city = CityDatabase.getCity(cityname).get();
        plugin.sendMessage(player, "messages.city.members.header", "%cityname%", city.getCityName(), "%membercount%",String.valueOf(CityDatabase.getCityMemberCount(city)));
        player.sendMessage(Objects.requireNonNull(CityDatabase.getCityMembers(city)));
    }

    @Subcommand("online")
    public static void onOnline(Player player, String cityname) {
        if (!player.hasPermission("metropolis.city.online")) {
            plugin.sendMessage(player, "messages.error.permissionDenied");
            return;
        }
        if (CityDatabase.getCity(cityname).isEmpty()) {
            plugin.sendMessage(player, "messages.error.missing.city");
            return;
        }
        City city = CityDatabase.getCity(cityname).get();
        //Check if any players with the role of mayor, assistant, inviter or vice mayor is online
        int cityStaffOnline = 0;
        int cityMembersOnline = 0;
        int totalOnline = 0;
        HashMap<String,Role> onlinePlayers = new HashMap<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (CityDatabase.memberExists(online.getUniqueId().toString(), city)) {
                onlinePlayers.put(online.getName(),CityDatabase.getCityRole(city,online.getUniqueId().toString()));
            }
        }
        StringBuilder onlineStaff = new StringBuilder().append("§2");
        StringBuilder onlineMembers = new StringBuilder().append("§2");
        for (Role role : onlinePlayers.values()) {
            if (role.equals(Role.MAYOR) || role.equals(Role.ASSISTANT) || role.equals(Role.INVITER) || role.equals(Role.VICE_MAYOR)) {
                cityStaffOnline++;
            } else {
                cityMembersOnline++;
            }
            totalOnline++;
        }

        for (String name : onlinePlayers.keySet()) {
            if (onlinePlayers.get(name).equals(Role.MAYOR) || onlinePlayers.get(name).equals(Role.ASSISTANT) || onlinePlayers.get(name).equals(Role.INVITER) || onlinePlayers.get(name).equals(Role.VICE_MAYOR)) {
                onlineStaff.append(name).append("§a, §2");
            } else {
                onlineMembers.append(name).append("§a, §2");
            }
        }
        if (totalOnline == 0) {
            plugin.sendMessage(player, "messages.city.online.none", "%cityname%", city.getCityName());
            return;
        }
        plugin.sendMessage(player, "messages.city.online.header", "%cityname%", city.getCityName(), "%online%",String.valueOf(cityStaffOnline + cityMembersOnline), "%membercount%",String.valueOf(CityDatabase.getCityMemberCount(city)));
        if (cityStaffOnline > 0) {
            plugin.sendMessage(player, "messages.city.online.admins");
            player.sendMessage(onlineStaff.delete(onlineStaff.length()-4,onlineStaff.length())+"");
        }
        if (cityStaffOnline > 0 && cityMembersOnline > 0) {
            player.sendMessage("§2");
        }
        if (cityMembersOnline > 0) {
            plugin.sendMessage(player, "messages.city.online.members");
            player.sendMessage(onlineMembers.delete(onlineMembers.length()-4,onlineMembers.length())+"");
        }
        if (totalOnline > 0) {
            player.sendMessage("§2");
            player.sendMessage("§2" + plugin.getMessage("messages.city.online.total", "%online%", String.valueOf(totalOnline)));
        }

    }

    @Subcommand("spawn")
    public static void onSpawn(Player player,@Optional String cityName) {
        if (!player.hasPermission("metropolis.city.spawn")) {
            plugin.sendMessage(player, "messages.error.permissionDenied");
            return;
        }
        if (cityName == null) {
            City city = Utilities.hasCityPermissions(player, "metropolis.city.spawn", Role.MEMBER);
            if (city == null) {
                return;
            }
            if (city.getCitySpawn() == null) {
                plugin.sendMessage(player, "messages.error.missing.spawn");
                return;
            }
            player.teleport(city.getCitySpawn());
            plugin.sendMessage(player, "messages.teleport", "%to%", "startpunkten i " + city.getCityName());
        } else {
            if (CityDatabase.getCity(cityName).isEmpty()) {
                plugin.sendMessage(player, "messages.error.missing.city");
                return;
            }
            City city = CityDatabase.getCity(cityName).get();
            if (city.getCitySpawn() == null) {
                plugin.sendMessage(player, "messages.error.missing.spawn");
                return;
            }
            if (player.hasPermission("metropolis.city.spawn.bypass")) {
                player.teleport(city.getCitySpawn());
                plugin.sendMessage(player, "messages.teleport", "%to%", "startpunkten i " + city.getCityName());
                return;
            }
            if (CityDatabase.getCityBan(city, player.getUniqueId().toString()) != null) {
                plugin.sendMessage(player, "messages.city.spawn.banned", "%cityname%", city.getCityName());
                return;
            }
            if (!city.isPublic() && !CityDatabase.memberExists(player.getUniqueId().toString(), city)) {
                plugin.sendMessage(player, "messages.city.spawn.closed", "%cityname%", city.getCityName());
                return;
            }
            player.teleport(city.getCitySpawn());
            plugin.sendMessage(player, "messages.teleport", "%to%", "startpunkten i " + city.getCityName());
        }
    }

    @Subcommand("ban")
    public static void onBan(Player player, @Optional String playerName, @Optional String args) {
        City city = Utilities.hasCityPermissions(player, "metropolis.city.ban", Role.ASSISTANT);
        if (city == null) {
            return;
        }
        List<Ban> bannedPlayers = CityDatabase.getCityBans(city);
        if (playerName == null && args == null) {
            if (bannedPlayers == null || bannedPlayers.isEmpty()) {
                plugin.sendMessage(player, "messages.city.ban.none", "%cityname%", city.getCityName());
                return;
            }
            StringBuilder bannedPlayersList = new StringBuilder().append("§4");
            for (Ban ban : bannedPlayers) {
                bannedPlayersList.append(plugin.getServer().getOfflinePlayer(UUID.fromString(ban.getPlayerUUID())).getName()).append("§c, §4");
            }
            plugin.sendMessage(player, "messages.city.ban.header", "%cityname%", city.getCityName());
            player.sendMessage(bannedPlayersList.delete(bannedPlayersList.length()-4,bannedPlayersList.length())+"");
            return;
        }
        if (playerName != null && args == null) {
            String playerUUID = Bukkit.getOfflinePlayer(playerName).getUniqueId().toString();
            if (CityDatabase.getCityBan(city, playerUUID) == null) {
                plugin.sendMessage(player, "messages.city.ban.notBanned","%cityname%", city.getCityName());
                return;
            } else {
                Ban ban = CityDatabase.getCityBan(city, playerUUID);
                assert ban != null;
                plugin.sendMessage(player, "messages.city.ban.banned", "%player%", playerName, "%cityname%", city.getCityName(), "%reason%", ban.getReason(), "%length%", DateUtil.formatDateDiff(ban.getLength()));
                return;
            }
        }
        long length = DateUtil.parseDateDiff(args, true);
        boolean isMinus = args.startsWith("-");
        String reason = DateUtil.removeTimePattern(args);
        boolean noReason = reason.length() < 2;
        if (playerName != null && isMinus && noReason) {
            if (bannedPlayers == null) {
                plugin.sendMessage(player, "messages.city.ban.none", "%cityname%", city.getCityName());
                return;
            }
            for (Ban ban : bannedPlayers) {
                if (ban.getPlayerUUID().equals(Bukkit.getOfflinePlayer(playerName).getUniqueId().toString())) {
                    CityDatabase.removeCityBan(city, ban);
                    Database.addLogEntry(city, "{ \"type\": \"unban\", \"subtype\": \"city\", \"player\": " + ban.getPlayerUUID() + ", \"placer\": " + player.getUniqueId().toString() + " }");
                    plugin.sendMessage(player, "messages.city.ban.unbanned", "%player%", playerName, "%cityname%", city.getCityName());
                    return;
                }
            }
            plugin.sendMessage(player, "messages.city.ban.notBanned", "%cityname%", city.getCityName());
            return;
        }
        if (playerName != null && noReason) {
            plugin.sendMessage(player, "messages.syntax.city.ban");
            return;
        }
        if (playerName != null && length != -1) {
            long maxBanTime = DateUtil.parseDateDiff(Metropolis.configuration.getMaxBanTime(), true);
            if (length > maxBanTime) {
                plugin.sendMessage(player, "messages.error.city.banTooLong", "%maxtime%", DateUtil.formatDateDiff(maxBanTime));
                return;
            }
            if (bannedPlayers != null) {
                for (Ban ban : bannedPlayers) {
                    if (ban.getPlayerUUID().equals(Bukkit.getOfflinePlayer(playerName).getUniqueId().toString())) {
                        plugin.sendMessage(player, "messages.city.ban.alreadyBanned", "%player%", playerName, "%cityname%", city.getCityName());
                        return;
                    }
                }
            }
            long placeDate = System.currentTimeMillis();
            String playerUUID = Bukkit.getOfflinePlayer(playerName).getUniqueId().toString();
            String expiryDate = DateUtil.formatDateDiff(length);
            CityDatabase.addCityBan(city, playerUUID, reason, player, placeDate, length);
            plugin.sendMessage(player, "messages.city.ban.success", "%player%", playerName, "%cityname%", city.getCityName(), "%reason%", reason, "%length%", expiryDate);

            // Log the ban
            Database.addLogEntry(city, "{ \"type\": \"ban\", \"subtype\": \"city\", \"player\": " + playerUUID + ", \"placer\": " + player.getUniqueId().toString() + ", \"reason\": \"" + reason + "\", \"length\": \"" + length + "\" }");
            return;
        }
        plugin.sendMessage(player, "messages.error.usage", "%command%", "/city ban <player> <length> <reason>");
    }

    @Subcommand("rank")
    @CommandCompletion("@players @cityRoles")
    public static void onRank(Player player , String playerName, String rank) {
        City city = Utilities.hasCityPermissions(player, "metropolis.city.rank", Role.VICE_MAYOR);
        if (city == null) {
            return;
        }
        Role role = CityDatabase.getCityRole(city, player.getUniqueId().toString());
        assert role != null;
        boolean isViceMayor = role.equals(Role.MAYOR) || role.equals(Role.VICE_MAYOR);
        boolean isMayor = role.equals(Role.MAYOR);

        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(playerName);
        if (!CityDatabase.memberExists(targetPlayer.getUniqueId().toString(), city)) {
            plugin.sendMessage(player, "messages.error.city.rank.notInCity", "%cityname%", city.getCityName(), "%playername%", playerName);
            return;
        }

        if (targetPlayer.getUniqueId() == player.getUniqueId()) {
            plugin.sendMessage(player, "messages.error.city.rank.cannotChangeOwnRole", "%cityname%", city.getCityName());
            return;
        }

        Role targetRole = CityDatabase.getCityRole(city, Bukkit.getOfflinePlayer(playerName).getUniqueId().toString());
        if (targetRole == null) {
            plugin.sendMessage(player, "messages.error.city.rank.notInCity", "%cityname%", city.getCityName(), "%playername%", playerName);
            return;
        }

        if (!isViceMayor) {
            plugin.sendMessage(player, "messages.error.city.permissionDenied", "%cityname%", city.getCityName());
            return;
        }

        if (!isMayor && targetRole.equals(Role.MAYOR)) {
            plugin.sendMessage(player, "messages.error.city.rank.cannotChangeHigherRole", "%cityname%", city.getCityName());
            return;
        }

        switch (rank.toLowerCase()) {
            case "assistant":
                if (!isMayor && targetRole.equals(Role.VICE_MAYOR)) {
                    plugin.sendMessage(player, "messages.error.city.rank.cannotChangeHigherRole", "%playername%", playerName);
                    return;
                }
                CityDatabase.setCityRole(city, targetPlayer.getUniqueId().toString(), Role.ASSISTANT);
                plugin.sendMessage(player, "messages.city.successful.rank.changed","%cityname%", city.getCityName(), "%playername%", playerName, "%newrole%", plugin.getMessage("messages.city.roles.assistant"));
                if (targetPlayer.isOnline()) {
                    plugin.sendMessage((Player) targetPlayer, "messages.city.successful.rank.promoted", "%cityname%", city.getCityName(), "%newrole%", plugin.getMessage("messages.city.roles.assistant"));
                }
                Database.addLogEntry(city, "{ \"type\": \"rank\", \"subtype\": \"change\", \"from\": \"" + targetRole + "\", \"to\": \"" + rank.toLowerCase() + "\", \"issuer\": \"" + player.getUniqueId().toString() + "\", \"player\": \"" + targetPlayer.getUniqueId().toString() + "\" }");
                break;
            case "inviter":
                if (!isMayor && targetRole.equals(Role.VICE_MAYOR)) {
                    plugin.sendMessage(player, "messages.error.city.rank.cannotChangeHigherRole", "%playername%", playerName);
                    return;
                }
                CityDatabase.setCityRole(city, targetPlayer.getUniqueId().toString(), Role.INVITER);
                plugin.sendMessage(player, "messages.city.successful.rank.changed","%cityname%", city.getCityName(), "%playername%", playerName, "%newrole%", plugin.getMessage("messages.city.roles.inviter"));
                if (targetPlayer.isOnline()) {
                    plugin.sendMessage((Player) targetPlayer, "messages.city.successful.rank.promoted", "%cityname%", city.getCityName(), "%newrole%", plugin.getMessage("messages.city.roles.inviter"));
                }
                Database.addLogEntry(city, "{ \"type\": \"rank\", \"subtype\": \"change\", \"from\": \"" + targetRole + "\", \"to\": \"" + rank.toLowerCase() + "\", \"issuer\": \"" + player.getUniqueId().toString() + "\", \"player\": \"" + targetPlayer.getUniqueId().toString() + "\" }");
                break;
            case "vicemayor":
                if (!isMayor && targetRole.equals(Role.VICE_MAYOR)) {
                    plugin.sendMessage(player, "messages.error.city.rank.cannotChangeHigherRole", "%playername%", playerName);
                    return;
                }
                CityDatabase.setCityRole(city, targetPlayer.getUniqueId().toString(), Role.VICE_MAYOR);
                plugin.sendMessage(player, "messages.city.successful.rank.changed","%cityname%", city.getCityName(), "%playername%", playerName, "%newrole%", plugin.getMessage("messages.city.roles.vicemayor"));
                if (targetPlayer.isOnline()) {
                    plugin.sendMessage((Player) targetPlayer, "messages.city.successful.rank.promoted", "%cityname%", city.getCityName(), "%newrole%", plugin.getMessage("messages.city.roles.vicemayor"));
                }
                Database.addLogEntry(city, "{ \"type\": \"rank\", \"subtype\": \"change\", \"from\": \"" + targetRole + "\", \"to\": \"" + rank.toLowerCase() + "\", \"issuer\": \"" + player.getUniqueId().toString() + "\", \"player\": \"" + targetPlayer.getUniqueId().toString() + "\" }");
                break;
            case "member":
            case "-":
                if (!isMayor && targetRole.equals(Role.VICE_MAYOR)) {
                    plugin.sendMessage(player, "messages.error.city.rank.cannotChangeHigherRole", "%playername%", playerName);
                    return;
                }
                CityDatabase.setCityRole(city, targetPlayer.getUniqueId().toString(), Role.MEMBER);
                plugin.sendMessage(player, "messages.city.successful.rank.changed","%cityname%", city.getCityName(), "%playername%", playerName, "%newrole%", plugin.getMessage("messages.city.roles.member"));
                if (targetPlayer.isOnline()) {
                    plugin.sendMessage((Player) targetPlayer, "messages.city.successful.rank.promoted", "%cityname%", city.getCityName(), "%newrole%", plugin.getMessage("messages.city.roles.member"));
                }
                Database.addLogEntry(city, "{ \"type\": \"rank\", \"subtype\": \"remove\", \"from\": \"" + targetRole + "\", \"issuer\": \"" + player.getUniqueId().toString() + "\", \"player\": \"" + targetPlayer.getUniqueId().toString() + "\" }");
                break;
            case "swap":
                if (!isMayor) {
                    plugin.sendMessage(player, "messages.error.city.permissionDenied", "%cityname%", city.getCityName());
                    return;
                }
                CityDatabase.setCityRole(city, player.getUniqueId().toString(), targetRole);
                CityDatabase.setCityRole(city, targetPlayer.getUniqueId().toString(), Role.MAYOR);
                plugin.sendMessage(player, "messages.city.successful.rank.swapped","%cityname%", city.getCityName(), "%playername%", playerName, "%newrole%", plugin.getMessage("messages.city.roles." + targetRole));
                if (targetPlayer.isOnline()) {
                    plugin.sendMessage((Player) targetPlayer, "messages.city.successful.rank.promoted", "%cityname%", city.getCityName(), "%newrole%", plugin.getMessage("messages.city.roles.mayor"));
                }
                Database.addLogEntry(city, "{ \"type\": \"rank\", \"subtype\": \"swap\", \"from\": \"" + role + "\", \"to\": \"" + targetRole + "\", \"issuer\": \"" + player.getUniqueId().toString() + "\", \"player\": \"" + targetPlayer.getUniqueId().toString() + "\" }");
                break;
            default:
                plugin.sendMessage(player, "messages.error.city.rank.invalid", "%cityname%", city.getCityName());
                break;
        }
    }

    @Subcommand("toggle")
    public static void onToggle(Player player) {
        if (!player.hasPermission("metropolis.city.toggle")) {
            plugin.sendMessage(player, "messages.error.permissionDenied");
            return;
        }
        plugin.sendMessage(player, "messages.syntax.city.toggle.open");
        plugin.sendMessage(player, "messages.syntax.city.toggle.public");
    }

    @Subcommand("toggle")
    public class Toggle extends BaseCommand {

        @Subcommand("open")
        public static void onOpen(Player player) {
            City city = Utilities.hasCityPermissions(player, "metropolis.city.toggle.open", Role.VICE_MAYOR);
            if (city == null) {
                return;
            }
            if (city.toggleOpen()) {
                plugin.sendMessage(player, "messages.city.toggle.open", "%cityname%", city.getCityName());
                Database.addLogEntry(city, "{ \"type\": \"city\", \"subtype\": \"toggleOpen\", \"state\": \"open\", \"player\": \"" + player.getUniqueId().toString() + "\" }");
            } else {
                plugin.sendMessage(player, "messages.city.toggle.closed", "%cityname%", city.getCityName());
                Database.addLogEntry(city, "{ \"type\": \"city\", \"subtype\": \"toggleOpen\", \"state\": \"closed\", \"player\": \"" + player.getUniqueId().toString() + "\" }");
            }
        }

        @Subcommand("public")
        public static void onPublic(Player player) {
            City city = Utilities.hasCityPermissions(player, "metropolis.city.toggle.public", Role.VICE_MAYOR);
            if (city == null) {
                return;
            }
            if (city.togglePublic()) {
                plugin.sendMessage(player, "messages.city.toggle.public", "%cityname%", city.getCityName());
                Database.addLogEntry(city, "{ \"type\": \"city\", \"subtype\": \"togglePublic\", \"state\": \"public\", \"player\": \"" + player.getUniqueId().toString() + "\" }");
            } else {
                plugin.sendMessage(player, "messages.city.toggle.private", "%cityname%", city.getCityName());
                Database.addLogEntry(city, "{ \"type\": \"city\", \"subtype\": \"togglePublic\", \"state\": \"private\", \"player\": \"" + player.getUniqueId().toString() + "\" }");
            }
        }
    }

    @Subcommand("search")
    public static void onSearch(Player player, @Optional String searchterm) {
        if (!player.hasPermission("metropolis.city.list")) {
            plugin.sendMessage(player, "messages.error.permissionDenied");
            return;
        }
        int maxCount = 20;
        StringBuilder stringBuilder = new StringBuilder();

        if (searchterm == null) {
            plugin.sendMessage(player, "messages.city.list.header");
            List<City> cityList = CityDatabase.getCityList(player, maxCount, null);
            for (City city : cityList) {
                stringBuilder.append("§2").append(city.getCityName()).append(" (").append(CityDatabase.getCityMemberCount(city)).append(")").append("§a, §2");
            }
            if (!cityList.isEmpty()) {
                player.sendMessage(stringBuilder.delete(stringBuilder.length()-4,stringBuilder.length())+"");
            } else {
                plugin.sendMessage(player, "messages.city.list.none");
            }
            return;
        }

        if (searchterm.length() < 3) {
            plugin.sendMessage(player, "messages.error.city.list.tooShort");
            return;
        }

        plugin.sendMessage(player, "messages.city.list.header");
        List<City> cityList = CityDatabase.getCityList(player, maxCount, searchterm);
        for (City city : cityList) {
            stringBuilder.append("§2").append(city.getCityName()).append(" (").append(CityDatabase.getCityMemberCount(city)).append(")").append("§a, §2");
        }
        if (!cityList.isEmpty()) {
            player.sendMessage(stringBuilder.delete(stringBuilder.length()-4,stringBuilder.length())+"");
        } else {
            plugin.sendMessage(player, "messages.city.list.none");
        }
    }

    @Subcommand("map")
    public static void onMap(Player player) {
        if (!player.hasPermission("metropolis.city.map")) {
            plugin.sendMessage(player, "messages.error.permissionDenied");
            return;
        }

        // Get the player's location and facing direction
        Location playerLoc = player.getLocation();
        int playerX = playerLoc.getBlockX() >> 4; // Convert to chunk coordinates
        int playerZ = playerLoc.getBlockZ() >> 4;

        City city = CityDatabase.getCityByClaim(playerLoc.toBlockLocation());

        // Generate the ASCII map
        String[][] asciiMap = Utilities.generateAsciiMap(player, playerX, playerZ, city);

        // Send the map to the player
        Utilities.sendMapToPlayer(player, asciiMap, city);
    }

    @Subcommand("near")
    @CommandCompletion("@nothing|@range:100-10000")
    public static void onNear(Player player, @Optional Integer blocks) {
        if (!player.hasPermission("metropolis.city.near")) {
            plugin.sendMessage(player, "messages.error.permissionDenied");
            return;
        }

        int radius = 3000;
        if (blocks != null) {
            if (!player.hasPermission("metropolis.city.near.custom")) {
                plugin.sendMessage(player, "messages.error.permissionDenied");
                return;
            }
            radius = blocks;
        }

        Location playerLocation = player.getLocation();
        List<CityDistance> nearbyCities = CityDatabase.getCitiesWithinRadius(playerLocation, radius);

        if (nearbyCities.isEmpty()) {
            plugin.sendMessage(player, "messages.city.near.noCitiesFound", "%radius%", String.valueOf(radius));
            return;
        }
        City inCity = CityDatabase.getCityByClaim(playerLocation.toBlockLocation());

        StringBuilder cityList = new StringBuilder();
        for (int i = 0; i < nearbyCities.size(); i++) {
            CityDistance cityDistance = nearbyCities.get(i);
            City city = cityDistance.getCity();
            int distance = cityDistance.getDistance();
            String set = distance + "m";
            if (inCity != null && inCity.equals(city)) {
                set = plugin.getMessage("messages.words.here");
            }
            String cityEntry = "§2" + city.getCityName() + " (" +
                    "§a" + distance + "m" +
                    "§2" + ")";

            if (i == nearbyCities.size() - 2) {
                cityEntry += " & ";
            } else if (i < nearbyCities.size() - 2) {
                cityEntry += ", ";
            }

            cityList.append(cityEntry);
        }

        plugin.sendMessage(player, "messages.city.near.header", "%radius%", String.valueOf(radius));
        player.sendMessage(cityList.toString());
    }

    @Subcommand("district")
    public static void onDistrict(Player player, @Optional String argument) {
        if (argument == null) {
            City city = Utilities.hasCityPermissions(player, "metropolis.city.district", null);
            if (city == null) {
                return;
            }
            List<live.supeer.metropolis.city.District> districtList = CityDatabase.getDistricts(city);
            if (districtList.isEmpty()) {
                plugin.sendMessage(player, "messages.error.city.district.none");
                return;
            }
            int districtCount = districtList.size();
            StringBuilder districtListString = new StringBuilder();
            for (live.supeer.metropolis.city.District district : districtList) {
                districtListString.append("<green>").append(district.getDistrictName()).append("<dark_green>, <green>");
            }
            districtListString.delete(districtListString.length() - 9, districtListString.length());
            plugin.sendMessage(player, "messages.city.district.list", "%districts%", districtListString.toString(), "%count%", String.valueOf(districtCount), "%cityname%", city.getCityName());

        }
        assert argument != null;
        if (argument.startsWith("-") || argument.startsWith("+")) {
            String playerName = argument.substring(1);
            OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(playerName);
            City city = Utilities.hasCityPermissions(player, "metropolis.city.district", Role.VICE_MAYOR);
            if (city == null) {
                return;
            }
            live.supeer.metropolis.city.District district = CityDatabase.getDistrict(player.getLocation().toBlockLocation());
            if (district == null) {
                plugin.sendMessage(player, "messages.error.city.district.notInDistrict");
                return;
            }
            if (!targetPlayer.hasPlayedBefore()) {
                plugin.sendMessage(player, "messages.error.player.notFound", "%player%", playerName);
                return;
            }
            if (!CityDatabase.memberExists(targetPlayer.getUniqueId().toString(), city)) {
                plugin.sendMessage(player, "messages.error.city.district.contactNotInCity", "%player%", playerName, "%cityname%", city.getCityName());
                return;
            }
            if (argument.startsWith("-")) {
                if (district.getContactplayers().contains(targetPlayer)) {
                    district.removeContactPlayer(targetPlayer);
                    Database.addLogEntry(city, "{ \"type\": \"district\", \"subtype\": \"contactRemove\", \"district\": \"" + district.getDistrictName() + "\", \"player\": \"" + targetPlayer.getUniqueId().toString() + "\" }");
                    plugin.sendMessage(player, "messages.city.district.contact.removed", "%player%", playerName, "%cityname%", city.getCityName());
                    return;
                }
                plugin.sendMessage(player, "messages.error.city.district.contactNotInDistrict", "%player%", playerName, "%cityname%", city.getCityName());
                return;
            }
            if (argument.startsWith("+")) {
                if (district.getContactplayers().contains(targetPlayer)) {
                    plugin.sendMessage(player, "messages.error.city.district.contactAlreadyInDistrict", "%player%", playerName, "%cityname%", city.getCityName());
                    return;
                }
                district.addContactPlayer(targetPlayer);
                Database.addLogEntry(city, "{ \"type\": \"district\", \"subtype\": \"contactAdd\", \"district\": \"" + district.getDistrictName() + "\", \"player\": \"" + targetPlayer.getUniqueId().toString() + "\" }");
                plugin.sendMessage(player, "messages.city.district.contact.added", "%player%", playerName, "%cityname%", city.getCityName());
            }

        } else {
            plugin.sendMessage(player,"messages.syntax.city.district");
        }
    }

    @Subcommand("district")
    public class District extends BaseCommand {
        @Subcommand("delete")
        public static void onDelete(Player player) {
            City city = Utilities.hasCityPermissions(player, "metropolis.city.district.delete", Role.VICE_MAYOR);
            if (city == null) {
                return;
            }
            live.supeer.metropolis.city.District district = CityDatabase.getDistrict(player.getLocation().toBlockLocation());
            if (district == null) {
                plugin.sendMessage(player, "messages.error.city.district.notInDistrict");
                return;
            }
            CityDatabase.deleteDistrict(district);
            Metropolis.playerInDistrict.remove(player.getUniqueId());
            Database.addLogEntry(city, "{ \"type\": \"district\", \"subtype\": \"delete\", \"district\": \"" + district.getDistrictName() + "\", \"player\": \"" + player.getUniqueId().toString() + "\" }");
            plugin.sendMessage(player, "messages.city.district.deleted", "%districtname%", district.getDistrictName(), "%cityname%", city.getCityName());
            Utilities.sendScoreboard(player);
        }

        @Subcommand("info")
        public static void onInfo(Player player, @Optional String name) {
            if (!player.hasPermission("metropolis.city.district.info")) {
                plugin.sendMessage(player, "messages.error.permissionDenied");
                return;
            }
            if (name == null) {
                live.supeer.metropolis.city.District district = CityDatabase.getDistrict(player.getLocation().toBlockLocation());
                if (district == null) {
                    plugin.sendMessage(player, "messages.error.city.district.notInDistrict");
                    return;
                }
                plugin.sendMessage(player, "messages.city.district.header", "%districtname%", district.getDistrictName());
                plugin.sendMessage(player, "messages.city.district.city", "%cityname%", district.getCity().getCityName());
                int contactCount = district.getContactplayers().size();
                if (contactCount > 0) {
                    StringBuilder contacts = new StringBuilder();
                    contacts.append("<green>");
                    for (OfflinePlayer contact : district.getContactplayers()) {
                        contacts.append(contact).append("<dark_green>, <green>");
                    }
                    contacts.delete(contacts.length() - 9, contacts.length());
                    if (!contacts.isEmpty()) {
                        plugin.sendMessage(player, "messages.city.district.contacts", "%contacts%", contacts.toString(), "%count%", String.valueOf(contactCount));
                    }
                }
            }
            if (name != null) {
                City city = CityDatabase.getCityByClaim(player.getLocation().toBlockLocation());
                live.supeer.metropolis.city.District district = CityDatabase.getDistrict(name, city);
                if (district == null) {
                    plugin.sendMessage(player, "messages.error.city.district.notFound");
                    return;
                }
                plugin.sendMessage(player, "messages.city.district.header", "%districtname%", district.getDistrictName());
                plugin.sendMessage(player, "messages.city.district.city", "%cityname%", district.getCity().getCityName());
                StringBuilder contacts = new StringBuilder();
                contacts.append("<green>");
                int contactCount = district.getContactplayers().size();
                for (OfflinePlayer contact : district.getContactplayers()) {
                    contacts.append(contact).append("<dark_green>, <green>");
                }
                contacts.delete(contacts.length() - 9, contacts.length());
                if (!contacts.isEmpty()) {
                    plugin.sendMessage(player, "messages.city.district.contacts", "%contacts%", contacts.toString(), "%count%", String.valueOf(contactCount));
                }
            }
        }

        @Subcommand("set")
        public static void onSet(Player player, String subcommand, String name) {
            City city = Utilities.hasCityPermissions(player, "metropolis.city.district.set", Role.VICE_MAYOR);
            if (city == null) {
                return;
            }
            if (subcommand.equalsIgnoreCase("name")) {
                live.supeer.metropolis.city.District district = CityDatabase.getDistrict(player.getLocation().toBlockLocation());
                if (district == null) {
                    plugin.sendMessage(player, "messages.error.city.district.notInDistrict");
                    return;
                }
                if (name.length() < 3) {
                    plugin.sendMessage(player, "messages.error.city.district.nameError", "%maxlength%", String.valueOf(Metropolis.configuration.getDistrictNameLimit()));
                    return;
                }
                if (name.length() > Metropolis.configuration.getDistrictNameLimit()) {
                    plugin.sendMessage(player, "messages.error.city.district.nameError", "%maxlength%", String.valueOf(Metropolis.configuration.getDistrictNameLimit()));
                    return;
                }
                district.setDistrictName(name);
                Database.addLogEntry(city, "{ \"type\": \"district\", \"subtype\": \"nameChange\", \"district\": \"" + name + "\", \"player\": \"" + player.getUniqueId().toString() + "\" }");
                plugin.sendMessage(player, "messages.city.district.nameChanged", "%districtname%", name, "%cityname%", city.getCityName());
                Utilities.sendScoreboard(player);
            }
        }

        @Subcommand("update")
        public static void onUpdate(Player player) {

        }
    }
}
