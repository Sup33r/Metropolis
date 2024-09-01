package live.supeer.metropolis.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation .*;
import co.aikar.commands.annotation.Optional;
import live.supeer.apied.ApiedAPI;
import live.supeer.apied.MPlayer;
import live.supeer.metropolis.AutoclaimManager;
import live.supeer.metropolis.Database;
import live.supeer.metropolis.Metropolis;
import live.supeer.metropolis.MetropolisListener;
import live.supeer.metropolis.event.*;
import live.supeer.metropolis.plot.Plot;
import live.supeer.metropolis.plot.PlotDatabase;
import live.supeer.metropolis.utils.LocationUtil;
import live.supeer.metropolis.utils.Utilities;
import live.supeer.metropolis.city.*;
import live.supeer.metropolis.homecity.HCDatabase;
import live.supeer.metropolis.utils.DateUtil;
import net.coreprotect.CoreProtect;
import net.coreprotect.CoreProtectAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@CommandAlias("city|c")
public class CommandCity extends BaseCommand {
    private static final GeometryFactory geometryFactory = new GeometryFactory();


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
        if (CoreProtect.APIVersion() < 10) {
            return null;
        }

        return CoreProtect;
    }

    @Subcommand("info")
    @Default
    public static void onInfo(Player player, @Optional String cityName) {
        City city;
        if (cityName == null) {
            if (HCDatabase.hasHomeCity(player.getUniqueId().toString())) {
                Metropolis.sendMessage(player, "messages.error.missing.homeCity");
                return;
            }
            city = HCDatabase.getHomeCityToCity(player.getUniqueId().toString());
            if (city == null) {
                Metropolis.sendMessage(player, "messages.error.missing.city");
                return;
            }

            // Get the list of cities the player is a member of
            List<City> memberCities = CityDatabase.memberCityList(player.getUniqueId().toString());

            // Create the formatted string
            StringBuilder cityList = new StringBuilder();
            assert memberCities != null;
            int size = memberCities.size();

            if (size == 1) {
                cityList.append(memberCities.getFirst().getCityName());
            } else if (size == 2) {
                cityList.append(memberCities.get(0).getCityName())
                        .append(" & ")
                        .append(memberCities.get(1).getCityName());
            } else if (size > 2) {
                for (int i = 0; i < size; i++) {
                    if (i == size - 1) {
                        cityList.append("& ").append(memberCities.get(i).getCityName());
                    } else if (i == size - 2) {
                        cityList.append(memberCities.get(i).getCityName()).append(" ");
                    } else {
                        cityList.append(memberCities.get(i).getCityName()).append(", ");
                    }
                }
            }

            Metropolis.sendMessage(player, "messages.city.cityMemberships", "%cities%", cityList.toString());
            Metropolis.sendMessage(player,"messages.city.homeCity", "%cityname%", city.getCityName());
            player.sendMessage("");
        } else {
            if (CityDatabase.getCity(cityName).isEmpty()) {
                Metropolis.sendMessage(player, "messages.error.missing.city");
                return;
            }
            city = CityDatabase.getCity(cityName).get();
        }
        Role role = CityDatabase.getCityRole(city, player.getUniqueId().toString());


        Metropolis.sendMessage(player, "messages.city.cityInfo.header", "%cityname%", city.getCityName(), "%claims%", String.valueOf(city.getCityClaims()), "%maxclaims%", String.valueOf(((city.getCityMembers().size() * 20) + city.getBonusClaims())), "%bonus%", String.valueOf(city.getBonusClaims()));
        if (role != null) {
            Metropolis.sendMessage(player, "messages.city.cityInfo.role", "%role%", Objects.requireNonNull(Metropolis.getMessage("messages.city.roles." + role.getRoleName())).toLowerCase());
        }
        if (city.getMotdMessage() != null)  {
            Metropolis.sendMessage(player, "messages.city.cityInfo.motd", "%motd%", city.getMotdMessage());
        }
        String bans = CityDatabase.getCityBans(city) == null ? "0" : String.valueOf(Objects.requireNonNull(CityDatabase.getCityBans(city)).size());
        Metropolis.sendMessage(player, "messages.city.cityInfo.membersPlots", "%members%", String.valueOf(city.getCityMembers().size()), "%plots%", String.valueOf(city.getCityPlots().size()));
        Metropolis.sendMessage(player, "messages.city.cityInfo.bansTwins", "%bans%", bans, "%twins%", String.valueOf(city.getTwinCities().size()));
        String cityOpen = city.isOpen() ? Metropolis.getMessage("messages.words.yes_word") : Metropolis.getMessage("messages.words.no_word");
        String cityPublic = city.isPublic() ? Metropolis.getMessage("messages.words.yes_word") : Metropolis.getMessage("messages.words.no_word");
        String founderName = ApiedAPI.getPlayer(UUID.fromString(city.getOriginalMayorUUID())).getName();
        Metropolis.sendMessage(player, "messages.city.cityInfo.openPublic", "%open%", cityOpen, "%public%", cityPublic);
        Metropolis.sendMessage(player, "messages.city.cityInfo.founded", "%founded%", DateUtil.niceDate(city.getCityCreationDate()), "%by%" , founderName);
        Metropolis.sendMessage(player, "messages.city.cityInfo.balance", "%balance%", Utilities.formattedMoney(CityDatabase.getCityBalance(city)));
        Metropolis.sendMessage(player, "messages.city.cityInfo.tax", "%tax%", String.valueOf(city.getCityTax()), "%payedBy%", Utilities.taxPayedBy(city.getTaxLevel()));
        Metropolis.sendMessage(player, "messages.city.cityInfo.stateTax", "%tax%", String.valueOf(city.getCityClaims()*Metropolis.configuration.getStateTax()));
        Metropolis.sendMessage(player, "messages.city.cityInfo.maxPlotsPerMember", "%maxplots%", String.valueOf(city.getMaxPlotsPerMember()));
        Metropolis.sendMessage(player, "messages.city.cityInfo.minChunkDistance", "%distance%", String.valueOf(city.getMinChunkDistance()));
        Metropolis.sendMessage(player, "messages.city.cityInfo.minSpawnDistance", "%distance%", String.valueOf(city.getMinSpawnDistance()));
        if (city.isPublic() || city.getCityMember(player.getUniqueId().toString()) != null) {
            Metropolis.sendMessage(player, "messages.city.cityInfo.spawnpoint", "%spawnpoint%", LocationUtil.formatLocation(city.getCitySpawn()));
        }
        List<String> mayors = new ArrayList<>();
        List<String> viceMayors = new ArrayList<>();
        List<String> assistants = new ArrayList<>();
        List<String> inviters = new ArrayList<>();
        for (Member member : city.getCityMembers()) {
            Role memberRole = CityDatabase.getCityRole(city, member.getPlayerUUID());
            MPlayer mPlayer = ApiedAPI.getPlayer(UUID.fromString(member.getPlayerUUID()));
            if (mPlayer == null) {
                continue;
            }
            if (memberRole == Role.MAYOR) {
                mayors.add(mPlayer.getName());
            } else if (memberRole == Role.VICE_MAYOR) {
                viceMayors.add(mPlayer.getName());
            } else if (memberRole == Role.ASSISTANT) {
                assistants.add(mPlayer.getName());
            } else if (memberRole == Role.INVITER) {
                inviters.add(mPlayer.getName());
            }
        }
        if (!mayors.isEmpty()) {
            Metropolis.sendMessage(player, "messages.city.cityInfo.mayors", "%count%", String.valueOf(mayors.size()), "%mayors%", Utilities.formatStringList(mayors));
        }
        if (!viceMayors.isEmpty()) {
            Metropolis.sendMessage(player, "messages.city.cityInfo.viceMayors", "%count%", String.valueOf(viceMayors.size()), "%vicemayors%", Utilities.formatStringList(viceMayors));
        }
        if (!assistants.isEmpty()) {
            Metropolis.sendMessage(player, "messages.city.cityInfo.assistants", "%count%", String.valueOf(assistants.size()), "%assistants%", Utilities.formatStringList(assistants));
        }
        if (!inviters.isEmpty()) {
            Metropolis.sendMessage(player, "messages.city.cityInfo.inviters", "%count%", String.valueOf(inviters.size()), "%inviters%", Utilities.formatStringList(inviters));
        }
        player.sendMessage("");
        if (city.isReserve()) {
            Metropolis.sendMessage(player, "messages.city.cityInfo.reserve", "%cost%", Utilities.formattedMoney(city.calculateCost()));
        }
    }

    @Subcommand("bank")
    public static void onBank(Player player, @Optional String[] args) {
        if (!player.hasPermission("metropolis.city.bank")) {
            Metropolis.sendMessage(player, "messages.error.permissionDenied");
            return;
        }
        MPlayer mPlayer = ApiedAPI.getPlayer(player);
        if (args.length == 0) {
            if (HCDatabase.getHomeCityToCity(player.getUniqueId().toString()) == null) {
                Metropolis.sendMessage(player, "messages.error.missing.homeCity");
                return;
            }
            City city = HCDatabase.getHomeCityToCity(player.getUniqueId().toString());
            if (city == null) {
                Metropolis.sendMessage(player, "messages.error.missing.city");
                return;
            }
            if (city.isReserve()) {
                Metropolis.sendMessage(player, "messages.error.city.reserve");
                return;
            }
            String cityBalance = Utilities.formattedMoney(CityDatabase.getCityBalance(city));
            Metropolis.sendMessage(
                    player, "messages.city.balance", "%balance%", cityBalance, "%cityname%", city.getCityName());
            return;
        }
        if (args[0].startsWith("+")) {
            if (args[0].substring(1).replaceAll("[0-9]", "").matches("[^0-9]")
                    || args.length < 2
                    || args[0].length() == 1) {
                Metropolis.sendMessage(player, "messages.syntax.city.bank.deposit");
                return;
            }

            int inputBalance = Integer.parseInt(args[0].replaceAll("[^0-9]", ""));
            String cityName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

            if (CityDatabase.getCity(cityName).isEmpty()) {
                Metropolis.sendMessage(player, "messages.error.missing.city");
                return;
            }
            City city = CityDatabase.getCity(cityName).get();
            if (city.isReserve()) {
                Metropolis.sendMessage(player, "messages.error.city.reserve");
                return;
            }
            if (mPlayer.getBalance() < inputBalance) {
                Metropolis.sendMessage(
                        player, "messages.error.missing.playerBalance", "%cityname%", city.getCityName());
                return;
            }

            mPlayer.removeBalance(inputBalance, "{ \"type\": \"city\", \"subtype\": \"deposit\", \"cityId\": " + city.getCityId() + "}");
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
        if (args[0].startsWith("-")) {
            if (args[0].substring(1).replaceAll("[0-9]", "").matches("[^0-9]")
                    || args.length < 2
                    || args[0].length() == 1) {
                Metropolis.sendMessage(player, "messages.syntax.city.bank.withdraw");
                return;
            }
            City city = Utilities.hasCityPermissions(player,"metropolis.city.bank.withdraw", Role.VICE_MAYOR);
            if (city == null) {
                return;
            }
            if (city.isReserve()) {
                Metropolis.sendMessage(player, "messages.error.city.reserve");
                return;
            }
            int inputBalance = Integer.parseInt(args[0].replaceAll("[^0-9]", ""));
            String inputBalanceFormatted = Utilities.formattedMoney(inputBalance);
            String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            int cityBalance = city.getCityBalance();
            if (!(reason.length() >= 8)) {
                Metropolis.sendMessage(
                        player, "messages.error.missing.reasonLength", "%cityname%", city.getCityName());
                return;
            }
            if (cityBalance <= 100000 || inputBalance > cityBalance - 100000) {
                Metropolis.sendMessage(
                        player, "messages.error.missing.balance", "%cityname%", city.getCityName());
                return;
            }
            CityDatabase.removeCityBalance(city, inputBalance);
            Database.addLogEntry(
                    city,
                    "{ \"type\": \"cityBank\", \"subtype\": \"withdraw\", \"balance\": "
                            + inputBalance
                            + ", \"player\": "
                            + player.getUniqueId()
                            + ", \"reason\": \""
                            + reason
                            + "\" }");
            mPlayer.addBalance(inputBalance, "{ \"type\": \"city\", \"subtype\": \"withdraw\", \"cityId\": " + city.getCityId() + "}");
            Metropolis.sendMessage(
                    player,
                    "messages.city.successful.withdraw",
                    "%amount%",
                    inputBalanceFormatted,
                    "%cityname%",
                    city.getCityName());
            return;
        }
        Metropolis.sendMessage(player, "messages.syntax.city.bank.bank");
        Metropolis.sendMessage(player, "messages.syntax.city.bank.deposit");
        Metropolis.sendMessage(player, "messages.syntax.city.bank.withdraw");
    }

    @Subcommand("new")
    public static void onNew(Player player, String cityName) {
        MPlayer mPlayer = ApiedAPI.getPlayer(player);
        if (!player.hasPermission("metropolis.city.new")) {
            Metropolis.sendMessage(player, "messages.error.permissionDenied");
            return;
        }
        if (CityDatabase.getPlayerCityCount(player.getUniqueId().toString()) >= 3) {
            Metropolis.sendMessage(player, "messages.error.city.maxCityCount");
            return;
        }
        if (mPlayer.getBalance() < Metropolis.configuration.getCityCreationCost()) {
            Metropolis.sendMessage(player, "messages.error.city.missing.balance.cityCost");
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

        City city = CityDatabase.newCity(cityName, player);
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
                        + player.getUniqueId()
                        + " }");
        Database.addLogEntry(
                city,
                "{ \"type\": \"join\", \"subtype\": \"city\", \"player\": "
                        + player.getUniqueId()
                        + " }");
        Database.addLogEntry(
                city,
                "{ \"type\": \"rank\", \"subtype\": \"change\", \"from\": "
                        + "member"
                        + ", \"to\": "
                        + "mayor"
                        + ", \"player\": "
                        + player.getUniqueId()
                        + " }");
        CityDatabase.setCityRole(city, player.getUniqueId().toString(), Role.MAYOR);
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
                        + player.getUniqueId()
                        + " }");
        mPlayer.removeBalance(Metropolis.configuration.getCityCreationCost(), "{ \"type\": \"city\", \"subtype\": \"new\", \"cityId\": " + city.getCityId() + "}");
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.hasPermission("metropolis.city.new")) {
                if (onlinePlayer == player) {
                    Metropolis.sendMessage(
                            onlinePlayer, "messages.city.successful.creation.self", "%cityname%", cityName);
                } else {
                    Metropolis.sendMessage(
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
            Metropolis.sendMessage(player, "messages.error.permissionDenied");
            return;
        }
        if (HCDatabase.hasHomeCity(player.getUniqueId().toString())) {
            Metropolis.sendMessage(player, "messages.error.missing.homeCity");
            return;
        }
        City city = HCDatabase.getHomeCityToCity(player.getUniqueId().toString());

        if (city == null) {
            Metropolis.sendMessage(player, "messages.error.missing.city");
            return;
        }
        if (city.isReserve()) {
            Metropolis.sendMessage(player, "messages.error.city.reserve");
            return;
        }

        if (arg != null) {
            if (arg.equals("-")) {
                // Stop autoclaiming
                AutoclaimManager.stopAutoclaim(player);
                Metropolis.sendMessage(player, "messages.city.autoclaim.stopped", "%cityname%", city.getCityName());
                return;
            }

            try {
                int autoclaimCount = Integer.parseInt(arg);
                if (autoclaimCount > 0) {
                    if (CityDatabase.getClaim(player.getLocation()) != null) {
                        Metropolis.sendMessage(player, "messages.city.autoclaim.notNature");
                        return;
                    }
                    if (CityDatabase.getCityBalance(city) < Metropolis.configuration.getCityClaimCost()) {
                        Metropolis.sendMessage(player, "messages.error.missing.claimCost", "%cityname%", city.getCityName(), "%cost%", ""+Metropolis.configuration.getCityClaimCost());
                        return;
                    }
                    if (Utilities.cannotClaimOrCreateCity(player.getLocation().toBlockLocation(), city)) {
                        Metropolis.sendMessage(player, "messages.error.city.tooCloseToOtherCity");
                        return;
                    }
                    if (CityDatabase.getCityRole(city, player.getUniqueId().toString()) == null || Objects.equals(CityDatabase.getCityRole(city, player.getUniqueId().toString()), Role.MEMBER)
                            || Objects.equals(
                            CityDatabase.getCityRole(city, player.getUniqueId().toString()), Role.INVITER)) {
                        Metropolis.sendMessage(player, "messages.error.city.permissionDenied", "%cityname%", city.getCityName());
                        return;
                    }

                    if (!Utilities.cityCanClaim(city)) {
                        Metropolis.sendMessage(player, "messages.error.city.maxClaims", "%cityname%", city.getCityName());
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
                        Claim claim = CityDatabase.createClaim(city, player.getLocation(), false, player.getUniqueId().toString());
                        assert claim != null;
                        Database.addLogEntry(
                                city,
                                "{ \"type\": \"buy\", \"subtype\": \"claim\", \"balance\": "
                                        + "500"
                                        + ", \"claimlocation\": "
                                        + LocationUtil.formatChunk(
                                        claim.getClaimWorld().getName(), claim.getXPosition(), claim.getZPosition())
                                        + ", \"player\": "
                                        + player.getUniqueId()
                                        + " }");
                        PlayerEnterCityEvent enterCityEvent = new PlayerEnterCityEvent(player, city);
                        Bukkit.getServer().getPluginManager().callEvent(enterCityEvent);
                        CityDatabase.removeCityBalance(city, Metropolis.configuration.getCityClaimCost());
                        Metropolis.sendMessage(player, "messages.city.successful.claim", "%cityname%", city.getCityName(), "%amount%", Utilities.formattedMoney(Metropolis.configuration.getCityClaimCost()));
                    } else {
                        Metropolis.sendMessage(player, "messages.error.city.claimTooFar", "%cityname%", city.getCityName());
                    }

                    // Start autoclaiming
                    AutoclaimManager.startAutoclaim(player, city, autoclaimCount-1);
                    Metropolis.sendMessage(player, "messages.city.autoclaim.started", "%remaining%", String.valueOf(autoclaimCount), "%cityname%", city.getCityName());
                    return;
                }
            } catch (NumberFormatException e) {
                Metropolis.sendMessage(player, "messages.syntax.city.claim");
                return;
            }
        }

        if (CityDatabase.getClaim(player.getLocation()) != null) {
            Metropolis.sendMessage(player, "messages.error.city.claimExists");
            return;
        }

        if (CityDatabase.getCityBalance(city) < Metropolis.configuration.getCityClaimCost()) {
            Metropolis.sendMessage(player, "messages.error.missing.claimCost", "%cityname%", city.getCityName(), "%cost%", ""+Metropolis.configuration.getCityClaimCost());
            return;
        }
        if (Utilities.cannotClaimOrCreateCity(player.getLocation().toBlockLocation(), city)) {
            Metropolis.sendMessage(player, "messages.error.city.tooCloseToOtherCity");
            return;
        }
        if (CityDatabase.getCityRole(city, player.getUniqueId().toString()) == null || Objects.equals(CityDatabase.getCityRole(city, player.getUniqueId().toString()), Role.MEMBER)
                || Objects.equals(
                CityDatabase.getCityRole(city, player.getUniqueId().toString()), Role.INVITER)) {
            Metropolis.sendMessage(player, "messages.error.city.permissionDenied", "%cityname%", city.getCityName());
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
            Claim claim = CityDatabase.createClaim(city, player.getLocation(), false, player.getUniqueId().toString());
            assert claim != null;
            Database.addLogEntry(
                    city,
                    "{ \"type\": \"buy\", \"subtype\": \"claim\", \"balance\": "
                            + "500"
                            + ", \"claimlocation\": "
                            + LocationUtil.formatChunk(
                            claim.getClaimWorld().getName(), claim.getXPosition(), claim.getZPosition())
                            + ", \"player\": "
                            + player.getUniqueId()
                            + " }");
            PlayerEnterCityEvent enterCityEvent = new PlayerEnterCityEvent(player, city);
            Bukkit.getServer().getPluginManager().callEvent(enterCityEvent);
            CityDatabase.removeCityBalance(city, Metropolis.configuration.getCityClaimCost());
            Metropolis.sendMessage(player, "messages.city.successful.claim", "%cityname%", city.getCityName(), "%amount%", Utilities.formattedMoney(Metropolis.configuration.getCityClaimCost()));
        } else {
            Metropolis.sendMessage(player, "messages.error.city.claimTooFar", "%cityname%", city.getCityName());
        }
    }

    @Subcommand("price")
    public static void onPrice(Player player) {
        if (player.hasPermission("metropolis.city.price")) {
            Metropolis.sendMessage(
                    player,
                    "messages.city.price",
                    "%city%",
                    Utilities.formattedMoney(Metropolis.getInstance().getConfig().getInt("settings.city.creationcost")),
                    "%chunk%",
                    Utilities.formattedMoney(Metropolis.getInstance().getConfig().getInt("settings.city.claimcost")),
                    "%bonus%",
                    Utilities.formattedMoney(Metropolis.getInstance().getConfig().getInt("settings.city.bonuscost")),
                    "%go%",
                    Utilities.formattedMoney(Metropolis.getInstance().getConfig().getInt("settings.city.citygocost")),
                    "%outpost%",
                    Utilities.formattedMoney(Metropolis.getInstance().getConfig().getInt("settings.city.outpostcost")));
        } else {
            Metropolis.sendMessage(player, "messages.error.permissionDenied");
        }
    }

    @Subcommand("join")
    public static void onJoin(Player player, String cityname) {
        if (!player.hasPermission("metropolis.city.join")) {
            Metropolis.sendMessage(player, "messages.error.permissionDenied");
            return;
        }
        if (CityDatabase.getCity(cityname).isEmpty() || cityname == null) {
            Metropolis.sendMessage(player, "messages.error.missing.city");
            return;
        }
        City city = CityDatabase.getCity(cityname).get();
        if (CityDatabase.memberExists(player.getUniqueId().toString(), city)) {
            Metropolis.sendMessage(player, "messages.error.city.alreadyInCity");
            return;
        }
        if(CityDatabase.getCityBan(city, player.getUniqueId().toString()) != null) {
            Metropolis.sendMessage(player, "messages.error.city.banned");
            return;
        }
        if (city.isReserve()) {
            Metropolis.sendMessage(player, "messages.error.city.reserve");
            return;
        }
        if (!city.isOpen() && (!player.hasPermission("metropolis.admin.city.join") || !invites.containsKey(player) || invites.get(player) == null || !invites.get(player).equals(city))) {
            Metropolis.sendMessage(player, "messages.error.city.closed", "%cityname%", city.getCityName());
            return;
        }
        if (Objects.requireNonNull(CityDatabase.memberCityList(player.getUniqueId().toString())).size()
                >= 3) {
            Metropolis.sendMessage(player, "messages.error.city.maxCityCount");
            return;
        }
        if (CityDatabase.getCityRole(city, player.getUniqueId().toString()) != null) {
            Metropolis.sendMessage(player, "messages.error.city.alreadyInACity");
            return;
        }
        CityDatabase.newMember(city, player);
        invites.remove(player, city);
        CityJoinEvent joinEvent = new CityJoinEvent(player, city);
        Metropolis.getInstance().getServer().getPluginManager().callEvent(joinEvent);
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (CityDatabase.memberExists(player.getUniqueId().toString(), city)) {
                if (onlinePlayer == player) {
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
            Metropolis.sendMessage(player, "messages.error.missing.player");
            return;
        }
        if (city.isReserve()) {
            Metropolis.sendMessage(player, "messages.error.city.reserve");
            return;
        }
        HashMap<UUID, City> uuidCityHashMap = new HashMap<>() {{put(inviteePlayer.getUniqueId(), city);}};
        if (CityDatabase.memberExists(inviteePlayer.getUniqueId().toString(), city)) {
            Metropolis.sendMessage(player, "messages.error.city.alreadyInACity");
            return;
        }
        if (CityDatabase.getCityBan(city, inviteePlayer.getUniqueId().toString()) != null) {
            Metropolis.sendMessage(player, "messages.error.city.playerBanned", "%cityname%", city.getCityName());
            return;
        }
        if (invites.containsKey(player) && invites.get(player).equals(city)) {
            Metropolis.sendMessage(
                    player, "messages.error.city.invite.alreadyInvited", "%cityname%", city.getCityName());
            return;
        }
        if (inviteePlayer == player) {
            Metropolis.sendMessage(player, "messages.error.city.invite.self", "%cityname%", city.getCityName());
            return;
        }
        MPlayer inviteeMPlayer = ApiedAPI.getPlayer(inviteePlayer);
        if (inviteeMPlayer.hasFlag('v')) {
            Metropolis.sendMessage(player, "messages.error.city.invite.off");
            return;
        }
        if (!inviteCooldownTime.containsKey(uuidCityHashMap)) {
            invites.put(inviteePlayer, city);
            Metropolis.sendMessage(
                    player,
                    "messages.city.invite.invited",
                    "%player%",
                    inviteePlayer.getName(),
                    "%cityname%",
                    city.getCityName(),
                    "%inviter%",
                    player.getName());
            Metropolis.sendMessage(
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
            inviteCooldownTask.get(uuidCityHashMap).runTaskTimer(Metropolis.getInstance(), 20, 20);
        } else {
            if (inviteCooldownTime.get(uuidCityHashMap) > 0) {
                Metropolis.sendMessage(
                        player,
                        "messages.error.city.invite.cooldown",
                        "%playername%",
                        inviteePlayer.getName(),
                        "%time%",
                        inviteCooldownTime.get(uuidCityHashMap).toString());
            } else {
                invites.put(inviteePlayer, city);
                Metropolis.sendMessage(
                        player,
                        "messages.city.invite.invited",
                        "%player%",
                        inviteePlayer.getName(),
                        "%cityname%",
                        city.getCityName(),
                        "%inviter%",
                        player.getName());
                Metropolis.sendMessage(
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
                inviteCooldownTask.get(uuidCityHashMap).runTaskTimer(Metropolis.getInstance(), 20, 20);
            }
        }
    }

    @Subcommand("go")
    @CommandCompletion("@cityGoes @cityGo1 @cityGo2 @nothing")
    public static void onGo(Player player, String[] args) {
        if (!player.hasPermission("metropolis.city.go")) {
            Metropolis.sendMessage(player, "messages.error.permissionDenied");
            return;
        }
        if (HCDatabase.hasHomeCity(player.getUniqueId().toString())
                || HCDatabase.getHomeCityToCity(player.getUniqueId().toString()) == null) {
            Metropolis.sendMessage(player, "messages.error.missing.homeCity");
            return;
        }
        City city = HCDatabase.getHomeCityToCity(player.getUniqueId().toString());
        assert city != null;
        Role role = CityDatabase.getCityRole(city, player.getUniqueId().toString());
        if (role == null) {
            Metropolis.sendMessage(
                    player, "messages.error.city.permissionDenied", "%cityname%", city.getCityName());
            return;
        }
        if (city.isReserve()) {
            Metropolis.sendMessage(player, "messages.error.city.reserve");
            return;
        }
        boolean isInviter = role.equals(Role.INVITER) || role.equals(Role.ASSISTANT) || role.equals(Role.VICE_MAYOR) || role.equals(Role.MAYOR);
        boolean isAssistant = role.equals(Role.ASSISTANT) || role.equals(Role.VICE_MAYOR) || role.equals(Role.MAYOR);
        boolean isViceMayor = role.equals(Role.VICE_MAYOR) || role.equals(Role.MAYOR);
        boolean isMayor = role.equals(Role.MAYOR);
        if (args.length == 0
                || args.length == 1 && !args[0].replaceAll("[0-9]", "").matches("[^0-9].*")) {
            if (!player.hasPermission("metropolis.city.go.list")) {
                Metropolis.sendMessage(player, "messages.error.permissionDenied");
                return;
            }
            if (CityDatabase.getCityGoCount(city, role) == 0) {
                Metropolis.sendMessage(player, "messages.error.missing.goes");
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
                Metropolis.sendMessage(player, "messages.error.missing.page");
                return;
            }
            if (start >= CityDatabase.getCityGoCount(city, role)) {
                Metropolis.sendMessage(player, "messages.error.missing.page");
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
            Metropolis.sendMessage(
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
                Metropolis.sendMessage(player, "messages.error.missing.go", "%cityname%", city.getCityName());
                return;
            }
            if (!args[1].equals("delete")) {
                Metropolis.sendMessage(player, "messages.syntax.city.go");
                return;
            }
            if (!player.hasPermission("metropolis.city.go.delete")) {
                Metropolis.sendMessage(player, "messages.error.permissionDenied");
                return;
            }
            if (!CityDatabase.cityGoExists(args[0], city)) {
                Metropolis.sendMessage(player, "messages.error.missing.go", "%cityname%", city.getCityName());
                return;
            }

            String goAccessLevel = CityDatabase.getCityGoAccessLevel(args[0], city);
            if (goAccessLevel == null
                    || goAccessLevel.equals("inviter")
                    || goAccessLevel.equals("assistant")
                    || goAccessLevel.equals("vicemayor")) {
                if (!isViceMayor) {
                    Metropolis.sendMessage(
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
                                + player.getUniqueId()
                                + " }");
                Metropolis.sendMessage(player, "messages.city.successful.delete.citygo", "%cityname%", city.getCityName(), "%name%", goName);
            }

        } else if (args.length == 4) {
            if (!CityDatabase.cityGoExists(args[0], city)) {
                Metropolis.sendMessage(player, "messages.error.missing.go", "%cityname%", city.getCityName());
                return;
            }
            if (!args[1].equals("set")) {
                Metropolis.sendMessage(player, "messages.syntax.city.go");
                return;
            }
            String goAccessLevel = CityDatabase.getCityGoAccessLevel(args[0], city);
            if (args[2].equals("displayname")) {
                if (!isViceMayor) {
                    Metropolis.sendMessage(
                            player, "messages.error.city.permissionDenied", "%cityname%", city.getCityName());
                    return;
                }
                if (args[3].length() > Metropolis.configuration.getCityGoDisplayNameLimit()) {
                    Metropolis.sendMessage(
                            player,
                            "messages.error.city.go.invalidDisplayname", "%cityname%", city.getCityName(), "%maxlength%", String.valueOf(Metropolis.configuration.getCityGoDisplayNameLimit()));
                    return;
                }
                if (Objects.equals(CityDatabase.getCityGoDisplayname(args[0], city), args[3])) {
                    Metropolis.sendMessage(
                            player,
                            "messages.error.city.go.sameDisplayname",
                            "%cityname%",
                            city.getCityName(),
                            "%name%",
                            args[3]);
                    return;
                }
                CityDatabase.setCityGoDisplayname(args[0], city, args[3]);
                Metropolis.sendMessage(
                        player,
                        "messages.city.go.changedDisplayname",
                        "%cityname%",
                        city.getCityName(),
                        "%name%",
                        args[0]);
            }
            if (args[2].equals("name")) {
                if (!isViceMayor) {
                    Metropolis.sendMessage(
                            player, "messages.error.city.permissionDenied", "%cityname%", city.getCityName());
                    return;
                }
                final String regex = "[^\\p{L}_0-9\\\\-]+";
                final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
                final Matcher matcher = pattern.matcher(args[3]);
                if (matcher.find() || args[3].matches("^[0-9].*") || args[3].length() > Metropolis.configuration.getCityGoNameLimit()) {
                    Metropolis.sendMessage(player, "messages.error.city.go.invalidName","%maxlength%", String.valueOf(Metropolis.configuration.getCityGoNameLimit()));
                    return;
                }
                if (CityDatabase.cityGoExists(args[3], city)) {
                    Metropolis.sendMessage(
                            player, "messages.error.city.go.alreadyExists", "%cityname%", city.getCityName());
                    return;
                }
                CityDatabase.setCityGoName(args[0], city, args[3]);
                Metropolis.sendMessage(
                        player,
                        "messages.city.go.changedName",
                        "%cityname%",
                        city.getCityName(),
                        "%name%",
                        args[0]);
                return;
            }
            if (args[2].equals("accesslevel")) {
                if (!args[3].equals("-")
                        && !args[3].equals("inviter")
                        && !args[3].equals("assistant")
                        && !args[3].equals("vicemayor")
                        && !args[3].equals("mayor")) {
                    Metropolis.sendMessage(
                            player,
                            "messages.error.city.go.invalidAccessLevel",
                            "%cityname%",
                            city.getCityName());
                    return;
                }
                switch (args[3]) {
                    case "-" -> {
                        if (!isAssistant) {
                            Metropolis.sendMessage(
                                    player, "messages.error.city.permissionDenied", "%cityname%", city.getCityName());
                            return;
                        }
                        if (goAccessLevel == null) {
                            Metropolis.sendMessage(
                                    player,
                                    "messages.error.city.go.alreadyAccessLevel",
                                    "%cityname%",
                                    city.getCityName());
                            return;
                        }
                        CityDatabase.setCityGoAccessLevel(args[0], city, null);
                        Metropolis.sendMessage(
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
                            Metropolis.sendMessage(
                                    player, "messages.error.city.permissionDenied", "%cityname%", city.getCityName());
                            return;
                        }
                        if (goAccessLevel != null && goAccessLevel.equals("inviter")) {
                            Metropolis.sendMessage(
                                    player,
                                    "messages.error.city.go.alreadyAccessLevel",
                                    "%cityname%",
                                    city.getCityName());
                            return;
                        }
                        CityDatabase.setCityGoAccessLevel(args[0], city, args[3]);
                        Metropolis.sendMessage(
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
                            Metropolis.sendMessage(
                                    player, "messages.error.city.permissionDenied", "%cityname%", city.getCityName());
                            return;
                        }
                        if (goAccessLevel != null && goAccessLevel.equals("assistant")) {
                            Metropolis.sendMessage(
                                    player,
                                    "messages.error.city.go.alreadyAccessLevel",
                                    "%cityname%",
                                    city.getCityName());
                            return;
                        }
                        CityDatabase.setCityGoAccessLevel(args[0], city, args[3]);
                        Metropolis.sendMessage(
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
                            Metropolis.sendMessage(
                                    player, "messages.error.city.permissionDenied", "%cityname%", city.getCityName());
                            return;
                        }
                        if (goAccessLevel != null && goAccessLevel.equals("vicemayor")) {
                            Metropolis.sendMessage(
                                    player,
                                    "messages.error.city.go.alreadyAccessLevel",
                                    "%cityname%",
                                    city.getCityName());
                            return;
                        }
                        CityDatabase.setCityGoAccessLevel(args[0], city, args[3]);
                        Metropolis.sendMessage(
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
                            Metropolis.sendMessage(
                                    player, "messages.error.city.permissionDenied", "%cityname%", city.getCityName());
                            return;
                        }
                        if (goAccessLevel != null && goAccessLevel.equals("mayor")) {
                            Metropolis.sendMessage(
                                    player,
                                    "messages.error.city.go.alreadyAccessLevel",
                                    "%cityname%",
                                    city.getCityName());
                            return;
                        }
                        CityDatabase.setCityGoAccessLevel(args[0], city, args[3]);
                        Metropolis.sendMessage(
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
            Metropolis.sendMessage(player, "messages.syntax.city.go");
        } else if (args.length == 1) {
            if (!player.hasPermission("metropolis.city.go.teleport")) {
                Metropolis.sendMessage(player, "messages.error.permissionDenied");
                return;
            }
            if (!CityDatabase.cityGoExists(args[0], city)) {
                Metropolis.sendMessage(player, "messages.error.missing.go", "%cityname%", city.getCityName());
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
                Metropolis.sendMessage(
                        player, "messages.error.city.permissionDenied", "%cityname%", city.getCityName());
                return;
            }
            Location location = CityDatabase.getCityGoLocation(args[0], city);
            assert location != null;
            player.teleport(location);
            String name = CityDatabase.getCityGoDisplayname(args[0], city);
            Metropolis.sendMessage(player, "messages.city.go.teleported", "%cityname%", city.getCityName(), "%name%", name);
            // Istället för player.teleport här så ska vi ha en call till Mandatory, som sköter VIP
            // teleportering.
        } else {
            Metropolis.sendMessage(player, "messages.syntax.city.go");
        }
    }

    @Subcommand("set")
    public static void onSet(Player player) {
        if (!player.hasPermission("metropolis.city.set")) {
            Metropolis.sendMessage(player, "messages.error.permissionDenied");
            return;
        }
        Metropolis.sendMessage(player, "messages.syntax.city.set.enter");
        Metropolis.sendMessage(player, "messages.syntax.city.set.exit");
        Metropolis.sendMessage(player, "messages.syntax.city.set.maxplotspermember");
        Metropolis.sendMessage(player, "messages.syntax.city.set.motd");
        Metropolis.sendMessage(player, "messages.syntax.city.set.name");
        Metropolis.sendMessage(player, "messages.syntax.city.set.spawn");
        Metropolis.sendMessage(player, "messages.syntax.city.set.tax");
    }

    @Subcommand("set")
    public class Set extends BaseCommand {

        @Subcommand("enter")
        public static void onEnter(Player player, String message) {
            City city = Utilities.hasCityPermissions(player, "metropolis.city.set.enter", Role.VICE_MAYOR);
            if (city == null) {
                return;
            }
            if (city.isReserve()) {
                Metropolis.sendMessage(player, "messages.error.city.reserve");
                return;
            }
            if (message.equals("-")) {
                city.setEnterMessage(null);
                Database.addLogEntry(
                        city,
                        "{ \"type\": \"set\", \"subtype\": \"enterMessage\", \"message\": "
                                + null
                                + ", \"player\": "
                                + player.getUniqueId()
                                + " }");
                Metropolis.sendMessage(
                        player, "messages.city.successful.set.enter.removed", "%cityname%", city.getCityName());
                return;
            }
            if (message.length() > Metropolis.configuration.getCityEnterMessageLimit()) {
                Metropolis.sendMessage(
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
                            + player.getUniqueId()
                            + " }");
            Metropolis.sendMessage(
                    player, "messages.city.successful.set.enter.set", "%cityname%", city.getCityName());
        }

        @Subcommand("exit")
        public static void onExit(Player player, String message) {
            City city = Utilities.hasCityPermissions(player, "metropolis.city.set.exit", Role.VICE_MAYOR);
            if (city == null) {
                return;
            }
            if (city.isReserve()) {
                Metropolis.sendMessage(player, "messages.error.city.reserve");
                return;
            }
            if (message.equals("-")) {
                city.setExitMessage(null);
                Database.addLogEntry(
                        city,
                        "{ \"type\": \"set\", \"subtype\": \"exitMessage\", \"message\": "
                                + null
                                + ", \"player\": "
                                + player.getUniqueId()
                                + " }");
                Metropolis.sendMessage(
                        player, "messages.city.successful.set.exit.removed", "%cityname%", city.getCityName());
                return;
            }
            if (message.length() > Metropolis.configuration.getCityExitMessageLimit()) {
                Metropolis.sendMessage(
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
                            + player.getUniqueId()
                            + " }");
            Metropolis.sendMessage(
                    player, "messages.city.successful.set.exit.set", "%cityname%", city.getCityName());
        }

        @Subcommand("motd")
        public static void onMotd(Player player, String message) {
            City city = Utilities.hasCityPermissions(player, "metropolis.city.set.motd", Role.VICE_MAYOR);
            if (city == null) {
                return;
            }
            if (city.isReserve()) {
                Metropolis.sendMessage(player, "messages.error.city.reserve");
                return;
            }
            if (message.equals("-")) {
                city.setMotdMessage(null);
                Database.addLogEntry(
                        city,
                        "{ \"type\": \"set\", \"subtype\": \"motdMessage\", \"message\": "
                                + null
                                + ", \"player\": "
                                + player.getUniqueId()
                                + " }");
                Metropolis.sendMessage(
                        player, "messages.city.successful.set.motd.removed", "%cityname%", city.getCityName());
                return;
            }
            if (message.length() > Metropolis.configuration.getCityMotdLimit()) {
                Metropolis.sendMessage(
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
                            + player.getUniqueId()
                            + " }");
            Metropolis.sendMessage(
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
                Metropolis.sendMessage(player, "messages.error.city.set.spawn.outsideCity", "%cityname%", city.getCityName());
                return;
            }
            Database.addLogEntry(
                    city,
                    "{ \"type\": \"set\", \"subtype\": \"spawn\", \"from\": "
                            + "([" + city.getCitySpawn().getWorld().getName() + "]" + city.getCitySpawn().getX() + ", " + city.getCitySpawn().getY() + ", " + city.getCitySpawn().getZ() + ")"
                            + ", \"to\": "
                            + "([" + player.getLocation().getWorld().getName() + "]" + player.getLocation().getX() + ", " + player.getLocation().getY() + ", " + player.getLocation().getZ() + ")"
                            + ", \"player\": "
                            + player.getUniqueId()
                            + " }");
            city.setCitySpawn(player.getLocation());
            Metropolis.sendMessage(player, "messages.city.successful.set.spawn", "%cityname%", city.getCityName());
        }

        @Subcommand("name")
        public static void onName(Player player, String name) {
            City city = Utilities.hasCityPermissions(player, "metropolis.city.set.name", Role.MAYOR);
            if (city == null) {
                return;
            }
            if (city.isReserve()) {
                Metropolis.sendMessage(player, "messages.error.city.reserve");
                return;
            }
            if (name.isEmpty() || name.length() > Metropolis.configuration.getCityNameLimit()) {
                Metropolis.sendMessage(player, "messages.error.city.nameLength","%maxlength%", String.valueOf(Metropolis.configuration.getCityNameLimit()));
                return;
            }
            if (!name.matches("[A-Za-zÀ-ÖØ-öø-ÿ0-9 ]+")) {
                Metropolis.sendMessage(player, "messages.error.city.invalidName");
                return;
            }
            if (CityDatabase.getCity(name).isPresent()) {
                Metropolis.sendMessage(player, "messages.error.city.cityExists");
                return;
            }

            int latestNameChange = CityDatabase.getLatestNameChange(city);
            int cooldownTime = Metropolis.configuration.getNameChangeCooldown();

            if (latestNameChange != 0) {
                int currentTime = (int) (System.currentTimeMillis() / 1000); // Convert to seconds
                int timeSinceLastChange = currentTime - latestNameChange;

                if (timeSinceLastChange < cooldownTime) {
                    Metropolis.sendMessage(player, "messages.error.city.namechange.cooldown","%cityname%", city.getCityName(), "%timeleft%", DateUtil.formatTimeFromSeconds(cooldownTime - timeSinceLastChange));
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
                            + player.getUniqueId()
                            + " }");
            city.setCityName(name);
            CityDatabase.setLatestNameChange(city, (int) (System.currentTimeMillis() / 1000));
            Metropolis.sendMessage(player, "messages.city.successful.set.name", "%cityname%", name);
        }

        @Subcommand("tax")
        public static void onTax(Player player, double tax) {
            City city = Utilities.hasCityPermissions(player, "metropolis.city.set.tax", Role.VICE_MAYOR);
            if (city == null) {
                return;
            }
            if (city.isReserve()) {
                Metropolis.sendMessage(player, "messages.error.city.reserve");
                return;
            }
            double maxTax = Metropolis.configuration.getCityMaxTax();
            if (tax < 0) {
                Metropolis.sendMessage(player, "messages.error.city.tax.invalidAmount", "%max%", String.valueOf(maxTax));
                return;
            }

            tax = Math.round(tax * 100.0) / 100.0;

            if (tax > maxTax) {
                Metropolis.sendMessage(player, "messages.error.city.tax.maxAmount", "%max%", String.valueOf(maxTax));
                return;
            }
            Database.addLogEntry(
                    city,
                    "{ \"type\": \"set\", \"subtype\": \"tax\", \"from\": "
                            + city.getCityTax()
                            + ", \"to\": "
                            + tax
                            + ", \"player\": "
                            + player.getUniqueId()
                            + " }");
            city.setCityTax(tax);
            Metropolis.sendMessage(player, "messages.city.successful.set.tax", "%cityname%", city.getCityName(), "%tax%", String.valueOf(tax));
        }

        @Subcommand("maxplotspermember")
        public static void onMaxPlotsPerMember(Player player, String argument) {
            City city = Utilities.hasCityPermissions(player, "metropolis.city.set.maxplotspermember", Role.VICE_MAYOR);
            if (city == null) {
                return;
            }
            if (city.isReserve()) {
                Metropolis.sendMessage(player, "messages.error.city.reserve");
                return;
            }
            if (argument.equals("-")) {
                city.setMaxPlotsPerMember(-1);
                Database.addLogEntry(
                        city,
                        "{ \"type\": \"set\", \"subtype\": \"maxPlotsPerMember\", \"from\": "
                                + city.getMaxPlotsPerMember()
                                + ", \"to\": "
                                + -1
                                + ", \"player\": "
                                + player.getUniqueId()
                                + " }");
                Metropolis.sendMessage(player, "messages.city.successful.maxPlotsPerMember.removed", "%cityname%", city.getCityName());
                return;
            }
            if (!argument.matches("[0-9]+")) {
                Metropolis.sendMessage(player, "messages.error.city.maxPlotsPerMember.invalidAmount");
                return;
            }
            int maxPlotsPerMember = Integer.parseInt(argument);
            if (maxPlotsPerMember > Metropolis.configuration.getMaxAmountOfPlots()) {
                Metropolis.sendMessage(player, "messages.error.city.maxPlotsPerMember.maxAmount", "%max%", String.valueOf(Metropolis.configuration.getMaxAmountOfPlots()));
            }
            Database.addLogEntry(
                    city,
                    "{ \"type\": \"set\", \"subtype\": \"maxPlotsPerMember\", \"from\": "
                            + city.getMaxPlotsPerMember()
                            + ", \"to\": "
                            + maxPlotsPerMember
                            + ", \"player\": "
                            + player.getUniqueId()
                            + " }");
            city.setMaxPlotsPerMember(maxPlotsPerMember);
            Metropolis.sendMessage(player, "messages.city.successful.maxPlotsPerMember.set", "%cityname%", city.getCityName(), "%amount%", String.valueOf(maxPlotsPerMember));
        }

        @Subcommand("taxlevel")
        @CommandCompletion("@taxLevel")
        public static void onTaxLevel(Player player, String argument) {
            City city = Utilities.hasCityPermissions(player, "metropolis.city.set.taxlevel", Role.VICE_MAYOR);
            if (city == null) {
                return;
            }
            if (city.isReserve()) {
                Metropolis.sendMessage(player, "messages.error.city.reserve");
                return;
            }
            if (argument.equals("-")) {
                city.setTaxLevel(null);
                Database.addLogEntry(
                        city,
                        "{ \"type\": \"set\", \"subtype\": \"taxLevel\", \"from\": "
                                + city.getTaxLevel()
                                + ", \"to\": "
                                + "none"
                                + ", \"player\": "
                                + player.getUniqueId()
                                + " }");
                Metropolis.sendMessage(player, "messages.city.successful.taxLevel.removed", "%cityname%", city.getCityName());
                return;
            }
            if (!argument.equals("member") && !argument.equals("inviter") && !argument.equals("assistant") && !argument.equals("vicemayor") && !argument.equals("mayor") && !argument.equals("all")) {
                Metropolis.sendMessage(player, "messages.error.city.taxlevel.invalidRole");
                return;
            }
            String role;
            if (argument.equals("all")) {
                argument = "mayor";
                role = Metropolis.getMessage("messages.city.roles.mayor");
            } else {
                role = Metropolis.getMessage("messages.city.roles." + argument);
            }
            argument = argument.toLowerCase();
            Database.addLogEntry(
                    city,
                    "{ \"type\": \"set\", \"subtype\": \"taxLevel\", \"from\": "
                            + city.getTaxLevel()
                            + ", \"to\": "
                            + argument
                            + ", \"player\": "
                            + player.getUniqueId()
                            + " }");
            city.setTaxLevel(Role.fromString(argument));
            Metropolis.sendMessage(player, "messages.city.successful.taxLevel.set", "%cityname%", city.getCityName(), "%role%", role);
        }
    }

    @Subcommand("buy")
    public static void onBuy(Player player) {
        if (player.hasPermission("metropolis.city.buy")) {
            Metropolis.sendMessage(player, "messages.error.permissionDenied");
            return;
        }
        Metropolis.sendMessage(player, "messages.syntax.city.buy.bonus");
        Metropolis.sendMessage(player, "messages.syntax.city.buy.district");
        Metropolis.sendMessage(player, "messages.syntax.city.buy.go");
        Metropolis.sendMessage(player, "messages.syntax.city.buy.outpost");
    }

    @Subcommand("buy")
    public class Buy extends BaseCommand {

        @Subcommand("bonus")
        public static void onBonus(Player player, int count) {
            City city = Utilities.hasCityPermissions(player, "metropolis.city.buy.bonus", Role.VICE_MAYOR);
            if (city == null) {
                return;
            }
            if (city.isReserve()) {
                Metropolis.sendMessage(player, "messages.error.city.reserve");
                return;
            }
            if (count < 1) {
                Metropolis.sendMessage(player, "messages.error.city.bonus.invalidAmount");
                return;
            }
            if (city.getCityBalance() < Metropolis.configuration.getCityBonusCost() * count) {
                Metropolis.sendMessage(
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
                            + player.getUniqueId()
                            + ", \"balance\": "
                            + Metropolis.configuration.getCityBonusCost() * count
                            + " }");
            Metropolis.sendMessage(
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
            if (city.isReserve()) {
                Metropolis.sendMessage(player, "messages.error.city.reserve");
                return;
            }
            final String regex = "[^\\p{L}_\\\\\\-]+";
            final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
            final Matcher matcher = pattern.matcher(name);
            if (matcher.find()
                    || name.matches("^[0-9].*")
                    || name.length() > Metropolis.configuration.getDistrictNameLimit()
                    || name.startsWith("-")) {
                Metropolis.sendMessage(
                        player, "messages.error.city.district.invalidName", "%cityname%", city.getCityName(), "%maxlength%", String.valueOf(Metropolis.configuration.getDistrictNameLimit()));
                return;
            }
            if (city.getCityBalance() < Metropolis.configuration.getDistrictCreationCost()) {
                Metropolis.sendMessage(
                        player, "messages.error.city.missing.balance.districtCost", "%cityname%", city.getCityName(), "%cost%", Utilities.formattedMoney(Metropolis.configuration.getDistrictCreationCost()));
                return;
            }
            if (CityDatabase.districtExists(name, city)) {
                Metropolis.sendMessage(
                        player, "messages.error.city.district.alreadyExists", "%cityname%", city.getCityName());
                return;
            }
            if (CityDatabase.getCityByClaim(player.getLocation()) != city) {
                Metropolis.sendMessage(player, "messages.error.city.district.outsideCity", "%cityname%", city.getCityName());
                return;
            }
            if (!MetropolisListener.playerPolygons.containsKey(player.getUniqueId())) {
                Metropolis.sendMessage(player, "messages.error.missing.plot");
                return;
            }

            Polygon regionPolygon = MetropolisListener.playerPolygons.get(player.getUniqueId());
            double minX = regionPolygon.getEnvelopeInternal().getMinX();
            double maxX = regionPolygon.getEnvelopeInternal().getMaxX();
            double minY = regionPolygon.getEnvelopeInternal().getMinY();
            double maxY = regionPolygon.getEnvelopeInternal().getMaxY();
            if (maxX - minX < 3 || maxY - minY < 3) {
                Metropolis.sendMessage(player, "messages.error.plot.tooSmall");
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
                        if (!Utilities.containsOnlyCompletePlots(regionPolygon, -64, 319, city, player.getWorld())) {
                            Metropolis.sendMessage(player, "messages.error.city.district.plotsNotCompletelyInside");
                            return;
                        }
                        CityDatabase.createDistrict(city, regionPolygon, name, player.getWorld());
                        city.removeCityBalance(Metropolis.configuration.getDistrictCreationCost());
                        Database.addLogEntry(
                                city,
                                "{ \"type\": \"buy\", \"subtype\": \"district\", \"name\": "
                                        + name
                                        + ", \"player\": "
                                        + player.getUniqueId()
                                        + ", \"balance\": "
                                        + Metropolis.configuration.getDistrictCreationCost()
                                        + ", \"districtBounds\": "
                                        + regionPolygon.toText()
                                        + " }");
                        Metropolis.sendMessage(
                                player, "messages.city.district.created", "%cityname%", city.getCityName(), "%districtname%", name);
                        return;
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
            if (city.isReserve()) {
                Metropolis.sendMessage(player, "messages.error.city.reserve");
                return;
            }
            final String regex = "[^\\p{L}_0-9\\\\-]+";
            final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
            final Matcher matcher = pattern.matcher(name);
            if (matcher.find()
                    || name.matches("^[0-9].*")
                    || name.length() > Metropolis.configuration.getCityGoNameLimit()
                    || name.startsWith("-")) {
                Metropolis.sendMessage(
                        player, "messages.error.city.go.invalidName", "%cityname%", city.getCityName(), "%maxlength%", String.valueOf(Metropolis.configuration.getCityGoNameLimit()));
                return;
            }
            if (city.getCityBalance() < Metropolis.configuration.getCityGoCost()) {
                Metropolis.sendMessage(
                        player, "messages.error.city.missing.balance.goCost", "%cityname%", city.getCityName());
                return;
            }
            if (CityDatabase.cityGoExists(name, city)) {
                Metropolis.sendMessage(
                        player, "messages.error.city.go.alreadyExists", "%cityname%", city.getCityName());
                return;
            }
            if (CityDatabase.getCityByClaim(player.getLocation()) != city) {
                Metropolis.sendMessage(player, "messages.error.city.go.outsideCity", "%cityname%", city.getCityName());
                return;
            }
            CityDatabase.newCityGo(player.getLocation(), name, city);
            city.removeCityBalance(Metropolis.configuration.getCityGoCost());
            Database.addLogEntry(
                    city,
                    "{ \"type\": \"buy\", \"subtype\": \"go\", \"name\": "
                            + name
                            + ", \"player\": "
                            + player.getUniqueId()
                            + ", \"balance\": "
                            + Metropolis.configuration.getCityGoCost()
                            + ", \"claimlocation\": "
                            + LocationUtil.formatLocation(player.getLocation())
                            + " }");
            Metropolis.sendMessage(
                    player, "messages.city.go.created", "%cityname%", city.getCityName(), "%name%", name);
        }

        @Subcommand("outpost")
        public static void onOutpost(Player player) {
            City city = Utilities.hasCityPermissions(player, "metropolis.city.buy.outpost", Role.VICE_MAYOR);
            if (city == null) {
                return;
            }
            if (city.isReserve()) {
                Metropolis.sendMessage(player, "messages.error.city.reserve");
                return;
            }
            if (city.getCityBalance() < Metropolis.configuration.getCityOutpostCost()) {
                Metropolis.sendMessage(
                        player, "messages.error.city.missing.balance.outpostCost", "%cityname%", city.getCityName());
                return;
            }
            Claim claim = CityDatabase.createClaim(city, player.getLocation(), true, player.getUniqueId().toString());
            assert claim != null;
            city.removeCityBalance(Metropolis.configuration.getCityOutpostCost());
            Database.addLogEntry(
                    city,
                    "{ \"type\": \"buy\", \"subtype\": \"outpost\", \"player\": "
                            + player.getUniqueId()
                            + ", \"balance\": "
                            + Metropolis.configuration.getCityOutpostCost()
                            + ", \"claimlocation\": "
                            + LocationUtil.formatLocation(player.getLocation())
                            + " }");
            PlayerEnterCityEvent enterCityEvent = new PlayerEnterCityEvent(player, city);
            Bukkit.getServer().getPluginManager().callEvent(enterCityEvent);
            Metropolis.sendMessage(player, "messages.city.successful.outpost", "%cityname%", city.getCityName(), "%amount%", Utilities.formattedMoney(Metropolis.configuration.getCityOutpostCost()));
        }
    }

    public static final List<Player> blockEnabled = new ArrayList<>();

    @Subcommand("block")
    public void onBlock(Player player, @Optional Integer page) {
        if (!player.hasPermission("metropolis.city.block")) {
            Metropolis.sendMessage(player, "messages.error.permissionDenied");
            return;
        }
        if (getCoreProtect() == null) {
            Metropolis.getInstance().logger.severe("[Metropolis] CoreProtect not found.");
            player.sendMessage("§cSomething went wrong. Please contact an administrator.");
            return;
        }
        if (page == null) {
            if (blockEnabled.contains(player)) {
                blockEnabled.remove(player);
                MetropolisListener.savedBlockHistory.remove(player.getUniqueId());
                Metropolis.sendMessage(player, "messages.block.disabled");
                return;
            } else {
                blockEnabled.add(player);
                Metropolis.sendMessage(player, "messages.block.enabled");
            }
            if (HCDatabase.hasHomeCity(player.getUniqueId().toString())
                    || HCDatabase.getHomeCityToCity(player.getUniqueId().toString()) == null) {
                Metropolis.sendMessage(player, "messages.error.missing.homeCity");
            }
        } else {
            if (!MetropolisListener.savedBlockHistory.containsKey(player.getUniqueId())) {
                Metropolis.sendMessage(player, "messages.error.missing.blockHistory");
                return;
            }
            if (page < 1) {
                Metropolis.sendMessage(player, "messages.error.missing.page");
                return;
            }
            int itemsPerPage = 8;
            int start = (page * itemsPerPage) - itemsPerPage;
            int stop = page * itemsPerPage;

            if (start >= MetropolisListener.savedBlockHistory.get(player.getUniqueId()).size()) {
                Metropolis.sendMessage(player, "messages.error.missing.page");
                return;
            }
            String[] firstLine = MetropolisListener.savedBlockHistory.get(player.getUniqueId()).getFirst();
            CoreProtectAPI.ParseResult firstResult = getCoreProtect().parseResult(firstLine);
            player.sendMessage("");
            Metropolis.sendMessage(
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
        if (city.isReserve()) {
            Metropolis.sendMessage(player, "messages.error.city.reserve");
            return;
        }
        if (message.length() < 5) {
            Metropolis.sendMessage(player, "messages.error.city.helpop.tooShort");
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
                Metropolis.sendMessage(online, "messages.city.helpop.receive", "%cityname%", city.getCityName(), "%player%", player.getName(), "%message%", message);
            }
        }
        if (cityStaffOnline == 0) {
            Metropolis.sendMessage(player, "messages.city.helpop.noStaffOnline", "%cityname%", city.getCityName());
            return;
        }
        Metropolis.sendMessage(player, "messages.city.helpop.sent", "%cityname%", city.getCityName());
    }

    @Subcommand("leave")
    public static void onLeave(Player player, String cityname) {
        City city = Utilities.hasCityPermissions(player, "metropolis.city.leave", Role.MEMBER);
        if (city == null) {
            return;
        }
//        if (!CityDatabase.memberExists(player.getUniqueId().toString(), CityDatabase.getCity(cityname).get())) {
//            plugin.sendMessage(player, "messages.error.city.notInCity");
//            return;
//        }
        if (Objects.equals(CityDatabase.getCityRole(city, player.getUniqueId().toString()), Role.MAYOR) && !city.isReserve()) {
            Metropolis.sendMessage(player, "messages.error.city.leave.mayor", "%cityname%", cityname);
            return;
        }
        city.removeCityMember(city.getCityMember(player.getUniqueId().toString()));
        CityLeaveEvent leaveEvent = new CityLeaveEvent(player, city);
        Metropolis.getInstance().getServer().getPluginManager().callEvent(leaveEvent);
        Database.addLogEntry(
                city,
                "{ \"type\": \"leave\", \"player\": "
                        + player.getUniqueId()
                        + " }");
        Utilities.sendScoreboard(player);
        Metropolis.sendMessage(player, "messages.city.leave.success", "%cityname%", cityname);
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (CityDatabase.memberExists(online.getUniqueId().toString(), city)) {
                if (!online.getUniqueId().equals(player.getUniqueId())) {
                    Metropolis.sendMessage(
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
            Metropolis.sendMessage(player, "messages.error.permissionDenied");
            return;
        }
        if (CityDatabase.getCity(cityname).isEmpty()) {
            Metropolis.sendMessage(player, "messages.error.missing.city");
            return;
        }
        City city = CityDatabase.getCity(cityname).get();
        Metropolis.sendMessage(player, "messages.city.members.header", "%cityname%", city.getCityName(), "%membercount%",String.valueOf(CityDatabase.getCityMemberCount(city)));
        player.sendMessage(Objects.requireNonNull(CityDatabase.getCityMembers(city)));
    }

    @Subcommand("online")
    public static void onOnline(Player player, String cityname) {
        if (!player.hasPermission("metropolis.city.online")) {
            Metropolis.sendMessage(player, "messages.error.permissionDenied");
            return;
        }
        if (CityDatabase.getCity(cityname).isEmpty()) {
            Metropolis.sendMessage(player, "messages.error.missing.city");
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
            Metropolis.sendMessage(player, "messages.city.online.none", "%cityname%", city.getCityName());
            return;
        }
        Metropolis.sendMessage(player, "messages.city.online.header", "%cityname%", city.getCityName(), "%online%",String.valueOf(cityStaffOnline + cityMembersOnline), "%membercount%",String.valueOf(CityDatabase.getCityMemberCount(city)));
        if (cityStaffOnline > 0) {
            Metropolis.sendMessage(player, "messages.city.online.admins");
            player.sendMessage(onlineStaff.delete(onlineStaff.length()-4,onlineStaff.length())+"");
        }
        if (cityStaffOnline > 0 && cityMembersOnline > 0) {
            player.sendMessage("§2");
        }
        if (cityMembersOnline > 0) {
            Metropolis.sendMessage(player, "messages.city.online.members");
            player.sendMessage(onlineMembers.delete(onlineMembers.length()-4,onlineMembers.length())+"");
        }
        if (totalOnline > 0) {
            player.sendMessage("§2");
            player.sendMessage("§2" + Metropolis.getMessage("messages.city.online.total", "%online%", String.valueOf(totalOnline)));
        }

    }

    @Subcommand("spawn")
    public static void onSpawn(Player player,@Optional String cityName) {
        if (!player.hasPermission("metropolis.city.spawn")) {
            Metropolis.sendMessage(player, "messages.error.permissionDenied");
            return;
        }
        if (cityName == null) {
            City city = Utilities.hasCityPermissions(player, "metropolis.city.spawn", Role.MEMBER);
            if (city == null) {
                return;
            }
            if (city.getCitySpawn() == null) {
                Metropolis.sendMessage(player, "messages.error.missing.spawn");
                return;
            }
            player.teleport(city.getCitySpawn());
            Metropolis.sendMessage(player, "messages.teleport", "%to%", "startpunkten i " + city.getCityName());
        } else {
            if (CityDatabase.getCity(cityName).isEmpty()) {
                Metropolis.sendMessage(player, "messages.error.missing.city");
                return;
            }
            City city = CityDatabase.getCity(cityName).get();
            if (city.getCitySpawn() == null) {
                Metropolis.sendMessage(player, "messages.error.missing.spawn");
                return;
            }
            if (player.hasPermission("metropolis.city.spawn.bypass")) {
                player.teleport(city.getCitySpawn());
                Metropolis.sendMessage(player, "messages.teleport", "%to%", "startpunkten i " + city.getCityName());
                return;
            }
            if (CityDatabase.getCityBan(city, player.getUniqueId().toString()) != null) {
                Metropolis.sendMessage(player, "messages.city.spawn.banned", "%cityname%", city.getCityName());
                return;
            }
            if (!city.isPublic() && !CityDatabase.memberExists(player.getUniqueId().toString(), city)) {
                Metropolis.sendMessage(player, "messages.city.spawn.closed", "%cityname%", city.getCityName());
                return;
            }
            player.teleport(city.getCitySpawn());
            Metropolis.sendMessage(player, "messages.teleport", "%to%", "startpunkten i " + city.getCityName());
        }
    }

    @Subcommand("ban")
    public static void onBan(Player player, @Optional String playerName, @Optional String args) {
        City city = Utilities.hasCityPermissions(player, "metropolis.city.ban", Role.ASSISTANT);
        if (city == null) {
            return;
        }
        if (city.isReserve()) {
            Metropolis.sendMessage(player, "messages.error.city.reserve");
            return;
        }
        List<Ban> bannedPlayers = CityDatabase.getCityBans(city);
        if (playerName == null && args == null) {
            if (bannedPlayers == null || bannedPlayers.isEmpty()) {
                Metropolis.sendMessage(player, "messages.city.ban.none", "%cityname%", city.getCityName());
                return;
            }
            StringBuilder bannedPlayersList = new StringBuilder().append("§4");
            for (Ban ban : bannedPlayers) {
                bannedPlayersList.append(ApiedAPI.getPlayer(UUID.fromString(ban.getPlayerUUID())).getName()).append("§c, §4");
            }
            Metropolis.sendMessage(player, "messages.city.ban.header", "%cityname%", city.getCityName());
            player.sendMessage(bannedPlayersList.delete(bannedPlayersList.length()-4,bannedPlayersList.length())+"");
            return;
        }
        MPlayer mPlayer = ApiedAPI.getPlayer(player);
        if (mPlayer == null) {
            Metropolis.sendMessage(player, "messages.error.player.notFound");
            return;
        }
        if (playerName != null && args == null) {
            if (CityDatabase.getCityBan(city, mPlayer.getUuid().toString()) == null) {
                Metropolis.sendMessage(player, "messages.city.ban.notBanned","%cityname%", city.getCityName());
            } else {
                Ban ban = CityDatabase.getCityBan(city, mPlayer.getUuid().toString());
                assert ban != null;
                Metropolis.sendMessage(player, "messages.city.ban.banned", "%player%", mPlayer.getName(), "%cityname%", city.getCityName(), "%reason%", ban.getReason(), "%length%", DateUtil.formatDateDiff(ban.getLength()));
            }
            return;
        }
        long length = DateUtil.parseDateDiff(args, true);
        boolean isMinus = args.startsWith("-");
        String reason = DateUtil.removeTimePattern(args);
        boolean noReason = reason.length() < 2;
        if (playerName != null && isMinus && noReason) {
            if (bannedPlayers == null) {
                Metropolis.sendMessage(player, "messages.city.ban.none", "%cityname%", city.getCityName());
                return;
            }
            for (Ban ban : bannedPlayers) {
                if (ban.getPlayerUUID().equals(mPlayer.getUuid().toString())) {
                    CityDatabase.removeCityBan(city, ban);
                    Database.addLogEntry(city, "{ \"type\": \"unban\", \"subtype\": \"city\", \"player\": " + ban.getPlayerUUID() + ", \"placer\": " + player.getUniqueId() + " }");
                    Metropolis.sendMessage(player, "messages.city.ban.unbanned", "%player%", mPlayer.getName(), "%cityname%", city.getCityName());
                    return;
                }
            }
            Metropolis.sendMessage(player, "messages.city.ban.notBanned", "%cityname%", city.getCityName());
            return;
        }
        if (playerName != null && noReason) {
            Metropolis.sendMessage(player, "messages.syntax.city.ban");
            return;
        }
        if (playerName != null && length != -1) {
            long maxBanTime = DateUtil.parseDateDiff(Metropolis.configuration.getMaxBanTime(), true);
            if (length > maxBanTime) {
                Metropolis.sendMessage(player, "messages.error.city.banTooLong", "%maxtime%", DateUtil.formatDateDiff(maxBanTime));
                return;
            }
            if (bannedPlayers != null) {
                for (Ban ban : bannedPlayers) {
                    if (ban.getPlayerUUID().equals(mPlayer.getUuid().toString())) {
                        Metropolis.sendMessage(player, "messages.city.ban.alreadyBanned", "%player%", mPlayer.getName(), "%cityname%", city.getCityName());
                        return;
                    }
                }
            }
            long placeDate = System.currentTimeMillis();
            String expiryDate = DateUtil.formatDateDiff(length);
            CityDatabase.addCityBan(city, mPlayer.getUuid().toString(), reason, player, placeDate, length);
            if (CityDatabase.memberExists(mPlayer.getUuid().toString(), city)) {
                city.removeCityMember(city.getCityMember(mPlayer.getUuid().toString()));
                CityLeaveEvent leaveEvent = new CityLeaveEvent(player, city);
                Metropolis.getInstance().getServer().getPluginManager().callEvent(leaveEvent);
            }
            if (Bukkit.getPlayer(mPlayer.getUuid()) != null) {
                Metropolis.sendMessage(Objects.requireNonNull(Bukkit.getPlayer(mPlayer.getUuid())), "messages.city.ban.playerBanned", "%player%", mPlayer.getName(), "%cityname%", city.getCityName(), "%reason%", reason, "%length%", expiryDate);
            }
            for (Member member : city.getCityMembers()) {
                Player memberPlayer = Bukkit.getPlayer(UUID.fromString(member.getPlayerUUID()));
                if (memberPlayer == null) {
                    continue;
                }
                if(member.getCityRole().hasPermission(Role.ASSISTANT)) {
                    Metropolis.sendMessage(memberPlayer, "messages.city.ban.success", "%player%", mPlayer.getName(), "%cityname%", city.getCityName(), "%reason%", reason, "%length%", expiryDate);
                    continue;
                }
                Metropolis.sendMessage(memberPlayer, "messages.city.ban.others", "%player%", mPlayer.getName(), "%cityname%", city.getCityName());
            }

            // Log the ban
            Database.addLogEntry(city, "{ \"type\": \"ban\", \"subtype\": \"city\", \"player\": " + mPlayer.getUuid() + ", \"placer\": " + player.getUniqueId() + ", \"reason\": \"" + reason + "\", \"length\": \"" + length + "\" }");
            return;
        }
        Metropolis.sendMessage(player, "messages.error.usage", "%command%", "/city ban <player> <length> <reason>");
    }

    @Subcommand("rank")
    @CommandCompletion("@players @cityRoles")
    public static void onRank(Player player , String playerName, String rank) {
        City city = Utilities.hasCityPermissions(player, "metropolis.city.rank", Role.VICE_MAYOR);
        if (city == null) {
            return;
        }
        if (city.isReserve()) {
            Metropolis.sendMessage(player, "messages.error.city.reserve");
            return;
        }
        Role role = CityDatabase.getCityRole(city, player.getUniqueId().toString());
        assert role != null;
        boolean isViceMayor = role.equals(Role.MAYOR) || role.equals(Role.VICE_MAYOR);
        boolean isMayor = role.equals(Role.MAYOR);

        MPlayer mPlayer = ApiedAPI.getPlayer(playerName);
        if (mPlayer == null) {
            Metropolis.sendMessage(player, "messages.error.player.notFound");
            return;
        }
        if (!CityDatabase.memberExists(mPlayer.getUuid().toString(), city)) {
            Metropolis.sendMessage(player, "messages.error.city.rank.notInCity", "%cityname%", city.getCityName(), "%playername%", mPlayer.getName());
            return;
        }

        if (mPlayer.getUuid() == player.getUniqueId()) {
            Metropolis.sendMessage(player, "messages.error.city.rank.cannotChangeOwnRole", "%cityname%", city.getCityName());
            return;
        }

        Role targetRole = CityDatabase.getCityRole(city, mPlayer.getUuid().toString());
        if (targetRole == null) {
            Metropolis.sendMessage(player, "messages.error.city.rank.notInCity", "%cityname%", city.getCityName(), "%playername%", mPlayer.getName());
            return;
        }

        if (!isViceMayor) {
            Metropolis.sendMessage(player, "messages.error.city.permissionDenied", "%cityname%", city.getCityName());
            return;
        }

        if (!isMayor && targetRole.equals(Role.MAYOR)) {
            Metropolis.sendMessage(player, "messages.error.city.rank.cannotChangeHigherRole", "%cityname%", city.getCityName());
            return;
        }

        Player targetPlayer = Bukkit.getPlayer(mPlayer.getUuid());

        switch (rank.toLowerCase()) {
            case "assistant":
                if (!isMayor && targetRole.equals(Role.VICE_MAYOR)) {
                    Metropolis.sendMessage(player, "messages.error.city.rank.cannotChangeHigherRole", "%playername%", mPlayer.getName());
                    return;
                }
                CityDatabase.setCityRole(city, mPlayer.getUuid().toString(), Role.ASSISTANT);
                Metropolis.sendMessage(player, "messages.city.successful.rank.changed","%cityname%", city.getCityName(), "%playername%", mPlayer.getName(), "%newrole%", Metropolis.getMessage("messages.city.roles.assistant"));
                if (targetPlayer != null) {
                    Metropolis.sendMessage(targetPlayer, "messages.city.successful.rank.promoted", "%cityname%", city.getCityName(), "%newrole%", Metropolis.getMessage("messages.city.roles.assistant"));
                }
                Database.addLogEntry(city, "{ \"type\": \"rank\", \"subtype\": \"change\", \"from\": \"" + targetRole + "\", \"to\": \"" + rank.toLowerCase() + "\", \"issuer\": \"" + player.getUniqueId() + "\", \"player\": \"" + mPlayer.getUuid() + "\" }");
                break;
            case "inviter":
                if (!isMayor && targetRole.equals(Role.VICE_MAYOR)) {
                    Metropolis.sendMessage(player, "messages.error.city.rank.cannotChangeHigherRole", "%playername%", mPlayer.getName());
                    return;
                }
                CityDatabase.setCityRole(city, mPlayer.getUuid().toString(), Role.INVITER);
                Metropolis.sendMessage(player, "messages.city.successful.rank.changed","%cityname%", city.getCityName(), "%playername%", mPlayer.getName(), "%newrole%", Metropolis.getMessage("messages.city.roles.inviter"));
                if (targetPlayer != null) {
                    Metropolis.sendMessage(targetPlayer, "messages.city.successful.rank.promoted", "%cityname%", city.getCityName(), "%newrole%", Metropolis.getMessage("messages.city.roles.inviter"));
                }
                Database.addLogEntry(city, "{ \"type\": \"rank\", \"subtype\": \"change\", \"from\": \"" + targetRole + "\", \"to\": \"" + rank.toLowerCase() + "\", \"issuer\": \"" + player.getUniqueId() + "\", \"player\": \"" + mPlayer.getUuid() + "\" }");
                break;
            case "vicemayor":
                if (!isMayor && targetRole.equals(Role.VICE_MAYOR)) {
                    Metropolis.sendMessage(player, "messages.error.city.rank.cannotChangeHigherRole", "%playername%", mPlayer.getName());
                    return;
                }
                CityDatabase.setCityRole(city, mPlayer.getUuid().toString(), Role.VICE_MAYOR);
                Metropolis.sendMessage(player, "messages.city.successful.rank.changed","%cityname%", city.getCityName(), "%playername%", mPlayer.getName(), "%newrole%", Metropolis.getMessage("messages.city.roles.vicemayor"));
                if (targetPlayer != null) {
                    Metropolis.sendMessage(targetPlayer, "messages.city.successful.rank.promoted", "%cityname%", city.getCityName(), "%newrole%", Metropolis.getMessage("messages.city.roles.vicemayor"));
                }
                Database.addLogEntry(city, "{ \"type\": \"rank\", \"subtype\": \"change\", \"from\": \"" + targetRole + "\", \"to\": \"" + rank.toLowerCase() + "\", \"issuer\": \"" + player.getUniqueId() + "\", \"player\": \"" + mPlayer.getUuid() + "\" }");
                break;
            case "member":
            case "-":
                if (!isMayor && targetRole.equals(Role.VICE_MAYOR)) {
                    Metropolis.sendMessage(player, "messages.error.city.rank.cannotChangeHigherRole", "%playername%", mPlayer.getName());
                    return;
                }
                CityDatabase.setCityRole(city, mPlayer.getUuid().toString(), Role.MEMBER);
                Metropolis.sendMessage(player, "messages.city.successful.rank.changed","%cityname%", city.getCityName(), "%playername%", mPlayer.getName(), "%newrole%", Metropolis.getMessage("messages.city.roles.member"));
                if (targetPlayer != null) {
                    Metropolis.sendMessage(targetPlayer, "messages.city.successful.rank.promoted", "%cityname%", city.getCityName(), "%newrole%", Metropolis.getMessage("messages.city.roles.member"));
                }
                Database.addLogEntry(city, "{ \"type\": \"rank\", \"subtype\": \"remove\", \"from\": \"" + targetRole + "\", \"issuer\": \"" + player.getUniqueId() + "\", \"player\": \"" + mPlayer.getUuid() + "\" }");
                break;
            case "swap":
                if (!isMayor) {
                    Metropolis.sendMessage(player, "messages.error.city.permissionDenied", "%cityname%", city.getCityName());
                    return;
                }
                CityDatabase.setCityRole(city, player.getUniqueId().toString(), targetRole);
                CityDatabase.setCityRole(city, mPlayer.getUuid().toString(), Role.MAYOR);
                Metropolis.sendMessage(player, "messages.city.successful.rank.swapped","%cityname%", city.getCityName(), "%playername%", mPlayer.getName(), "%newrole%", Metropolis.getMessage("messages.city.roles." + targetRole));
                if (targetPlayer != null) {
                    Metropolis.sendMessage(targetPlayer, "messages.city.successful.rank.promoted", "%cityname%", city.getCityName(), "%newrole%", Metropolis.getMessage("messages.city.roles.mayor"));
                }
                Database.addLogEntry(city, "{ \"type\": \"rank\", \"subtype\": \"swap\", \"from\": \"" + role + "\", \"to\": \"" + targetRole + "\", \"issuer\": \"" + player.getUniqueId() + "\", \"player\": \"" + mPlayer.getUuid() + "\" }");
                break;
            default:
                Metropolis.sendMessage(player, "messages.error.city.rank.invalid", "%cityname%", city.getCityName());
                break;
        }
    }

    @Subcommand("toggle")
    public static void onToggle(Player player) {
        if (!player.hasPermission("metropolis.city.toggle")) {
            Metropolis.sendMessage(player, "messages.error.permissionDenied");
            return;
        }
        Metropolis.sendMessage(player, "messages.syntax.city.toggle.open");
        Metropolis.sendMessage(player, "messages.syntax.city.toggle.public");
    }

    @Subcommand("toggle")
    public class Toggle extends BaseCommand {

        @Subcommand("open")
        public static void onOpen(Player player) {
            City city = Utilities.hasCityPermissions(player, "metropolis.city.toggle.open", Role.VICE_MAYOR);
            if (city == null) {
                return;
            }
            if (city.isReserve()) {
                Metropolis.sendMessage(player, "messages.error.city.reserve");
                return;
            }
            if (city.toggleOpen()) {
                Metropolis.sendMessage(player, "messages.city.toggle.open", "%cityname%", city.getCityName());
                Database.addLogEntry(city, "{ \"type\": \"city\", \"subtype\": \"toggleOpen\", \"state\": \"open\", \"player\": \"" + player.getUniqueId() + "\" }");
            } else {
                Metropolis.sendMessage(player, "messages.city.toggle.closed", "%cityname%", city.getCityName());
                Database.addLogEntry(city, "{ \"type\": \"city\", \"subtype\": \"toggleOpen\", \"state\": \"closed\", \"player\": \"" + player.getUniqueId() + "\" }");
            }
        }

        @Subcommand("public")
        public static void onPublic(Player player) {
            City city = Utilities.hasCityPermissions(player, "metropolis.city.toggle.public", Role.VICE_MAYOR);
            if (city == null) {
                return;
            }
            if (city.isReserve()) {
                Metropolis.sendMessage(player, "messages.error.city.reserve");
                return;
            }
            if (city.togglePublic()) {
                Metropolis.sendMessage(player, "messages.city.toggle.public", "%cityname%", city.getCityName());
                Database.addLogEntry(city, "{ \"type\": \"city\", \"subtype\": \"togglePublic\", \"state\": \"public\", \"player\": \"" + player.getUniqueId() + "\" }");
            } else {
                Metropolis.sendMessage(player, "messages.city.toggle.private", "%cityname%", city.getCityName());
                Database.addLogEntry(city, "{ \"type\": \"city\", \"subtype\": \"togglePublic\", \"state\": \"private\", \"player\": \"" + player.getUniqueId() + "\" }");
            }
        }
    }

    @Subcommand("search")
    public static void onSearch(Player player, @Optional String searchterm) {
        if (!player.hasPermission("metropolis.city.list")) {
            Metropolis.sendMessage(player, "messages.error.permissionDenied");
            return;
        }
        int maxCount = 20;
        StringBuilder stringBuilder = new StringBuilder();

        if (searchterm == null) {
            Metropolis.sendMessage(player, "messages.city.list.header");
            List<City> cityList = CityDatabase.getCityList(player, maxCount, null);
            for (City city : cityList) {
                stringBuilder.append("§2").append(city.getCityName()).append(" (").append(CityDatabase.getCityMemberCount(city)).append(")").append("§a, §2");
            }
            if (!cityList.isEmpty()) {
                player.sendMessage(stringBuilder.delete(stringBuilder.length()-4,stringBuilder.length())+"");
            } else {
                Metropolis.sendMessage(player, "messages.city.list.none");
            }
            return;
        }

        if (searchterm.length() < 3) {
            Metropolis.sendMessage(player, "messages.error.city.list.tooShort");
            return;
        }

        Metropolis.sendMessage(player, "messages.city.list.header");
        List<City> cityList = CityDatabase.getCityList(player, maxCount, searchterm);
        for (City city : cityList) {
            stringBuilder.append("§2").append(city.getCityName()).append(" (").append(CityDatabase.getCityMemberCount(city)).append(")").append("§a, §2");
        }
        if (!cityList.isEmpty()) {
            player.sendMessage(stringBuilder.delete(stringBuilder.length()-4,stringBuilder.length())+"");
        } else {
            Metropolis.sendMessage(player, "messages.city.list.none");
        }
    }

    @Subcommand("map")
    public static void onMap(Player player) {
        if (!player.hasPermission("metropolis.city.map")) {
            Metropolis.sendMessage(player, "messages.error.permissionDenied");
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
    @CommandCompletion("@nothing @range:100-10000")
    public static void onNear(Player player, @Optional Integer blocks) {
        if (!player.hasPermission("metropolis.city.near")) {
            Metropolis.sendMessage(player, "messages.error.permissionDenied");
            return;
        }

        int radius = 3000;
        if (blocks != null) {
            if (!player.hasPermission("metropolis.city.near.custom")) {
                Metropolis.sendMessage(player, "messages.error.permissionDenied");
                return;
            }
            radius = blocks;
        }

        Location playerLocation = player.getLocation();
        List<CityDistance> nearbyCities = CityDatabase.getCitiesWithinRadius(playerLocation, radius);

        if (nearbyCities.isEmpty()) {
            Metropolis.sendMessage(player, "messages.city.near.noCitiesFound", "%radius%", String.valueOf(radius));
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
                set = Metropolis.getMessage("messages.words.here");
            }
            String cityEntry = "§2" + city.getCityName() + " (" +
                    "§a" + set +
                    "§2" + ")";

            if (i == nearbyCities.size() - 2) {
                cityEntry += " & ";
            } else if (i < nearbyCities.size() - 2) {
                cityEntry += ", ";
            }

            cityList.append(cityEntry);
        }

        Metropolis.sendMessage(player, "messages.city.near.header", "%radius%", String.valueOf(radius));
        player.sendMessage(cityList.toString());
    }

    @Subcommand("district")
    public static void onDistrict(Player player, @Optional String argument) {
        if (argument == null) {
            City city = Utilities.hasCityPermissions(player, "metropolis.city.district", null);
            if (city == null) {
                return;
            }
            if (city.isReserve()) {
                Metropolis.sendMessage(player, "messages.error.city.reserve");
                return;
            }
            List<live.supeer.metropolis.city.District> districtList = CityDatabase.getDistricts(city);
            if (districtList.isEmpty()) {
                Metropolis.sendMessage(player, "messages.error.city.district.none");
                return;
            }
            int districtCount = districtList.size();
            StringBuilder districtListString = new StringBuilder();
            for (live.supeer.metropolis.city.District district : districtList) {
                districtListString.append("<green>").append(district.getDistrictName()).append("<dark_green>, <green>");
            }
            districtListString.delete(districtListString.length() - 9, districtListString.length());
            Metropolis.sendMessage(player, "messages.city.district.list", "%districts%", districtListString.toString(), "%count%", String.valueOf(districtCount), "%cityname%", city.getCityName());
            return;
        }
        if (argument.startsWith("-") || argument.startsWith("+")) {
            String playerName = argument.substring(1);
            MPlayer mPlayer = ApiedAPI.getPlayer(playerName);
            if (mPlayer == null) {
                Metropolis.sendMessage(player, "messages.error.player.notFound");
                return;
            }
            City city = Utilities.hasCityPermissions(player, "metropolis.city.district", Role.VICE_MAYOR);
            if (city == null) {
                return;
            }
            live.supeer.metropolis.city.District district = CityDatabase.getDistrict(player.getLocation().toBlockLocation());
            if (district == null) {
                Metropolis.sendMessage(player, "messages.error.city.district.notInDistrict");
                return;
            }
            if (mPlayer == null) {
                Metropolis.sendMessage(player, "messages.error.player.notFound", "%player%", mPlayer.getName());
                return;
            }
            if (!CityDatabase.memberExists(mPlayer.getUuid().toString(), city)) {
                Metropolis.sendMessage(player, "messages.error.city.district.contactNotInCity", "%player%", mPlayer.getName(), "%cityname%", city.getCityName());
                return;
            }
            if (argument.startsWith("-")) {
                if (district.getContactplayers().contains(mPlayer.getUuid())) {
                    district.removeContactPlayer(mPlayer);
                    Database.addLogEntry(city, "{ \"type\": \"district\", \"subtype\": \"contactRemove\", \"district\": \"" + district.getDistrictName() + "\", \"player\": \"" + mPlayer.getUuid() + "\" }");
                    Metropolis.sendMessage(player, "messages.city.district.contact.removed", "%player%", mPlayer.getName(), "%cityname%", city.getCityName());
                    return;
                }
                Metropolis.sendMessage(player, "messages.error.city.district.contactNotInDistrict", "%player%", mPlayer.getName(), "%cityname%", city.getCityName());
                return;
            }
            if (argument.startsWith("+")) {
                if (district.getContactplayers().contains(mPlayer.getUuid())) {
                    Metropolis.sendMessage(player, "messages.error.city.district.contactAlreadyInDistrict", "%player%", mPlayer.getName(), "%cityname%", city.getCityName());
                    return;
                }
                district.addContactPlayer(mPlayer);
                Database.addLogEntry(city, "{ \"type\": \"district\", \"subtype\": \"contactAdd\", \"district\": \"" + district.getDistrictName() + "\", \"player\": \"" + mPlayer.getUuid() + "\" }");
                Metropolis.sendMessage(player, "messages.city.district.contact.added", "%player%", mPlayer.getName(), "%cityname%", city.getCityName());
            }

        } else {
            Metropolis.sendMessage(player,"messages.syntax.city.district");
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
            if (city.isReserve()) {
                Metropolis.sendMessage(player, "messages.error.city.reserve");
                return;
            }
            live.supeer.metropolis.city.District district = CityDatabase.getDistrict(player.getLocation().toBlockLocation());
            if (district == null) {
                Metropolis.sendMessage(player, "messages.error.city.district.notInDistrict");
                return;
            }
            CityDatabase.deleteDistrict(district);
            city.removeCityDistrict(district);
            Metropolis.playerInDistrict.remove(player.getUniqueId());
            Database.addLogEntry(city, "{ \"type\": \"district\", \"subtype\": \"delete\", \"district\": \"" + district.getDistrictName() + "\", \"player\": \"" + player.getUniqueId() + "\" }");
            Metropolis.sendMessage(player, "messages.city.district.deleted", "%districtname%", district.getDistrictName(), "%cityname%", city.getCityName());
            Utilities.sendScoreboard(player);
        }

        @Subcommand("info")
        public static void onInfo(Player player, @Optional String name) {
            if (!player.hasPermission("metropolis.city.district.info")) {
                Metropolis.sendMessage(player, "messages.error.permissionDenied");
                return;
            }
            if (name == null) {
                live.supeer.metropolis.city.District district = CityDatabase.getDistrict(player.getLocation().toBlockLocation());
                if (district == null) {
                    Metropolis.sendMessage(player, "messages.error.city.district.notInDistrict");
                    return;
                }
                Metropolis.sendMessage(player, "messages.city.district.header", "%districtname%", district.getDistrictName());
                Metropolis.sendMessage(player, "messages.city.district.city", "%cityname%", district.getCity().getCityName());
                int contactCount = district.getContactplayers().size();
                if (contactCount > 0) {
                    StringBuilder contacts = new StringBuilder();
                    contacts.append("<green>");
                    for (UUID contact : district.getContactplayers()) {
                        contacts.append(ApiedAPI.getPlayer(contact).getName()).append("<dark_green>, <green>");
                    }
                    contacts.delete(contacts.length() - 9, contacts.length());
                    if (!contacts.isEmpty()) {
                        Metropolis.sendMessage(player, "messages.city.district.contacts", "%contacts%", contacts.toString(), "%count%", String.valueOf(contactCount));
                    }
                }
            }
            if (name != null) {
                City city = CityDatabase.getCityByClaim(player.getLocation().toBlockLocation());
                assert city != null;
                live.supeer.metropolis.city.District district = CityDatabase.getDistrict(name, city);
                if (district == null) {
                    Metropolis.sendMessage(player, "messages.error.city.district.notFound");
                    return;
                }
                Metropolis.sendMessage(player, "messages.city.district.header", "%districtname%", district.getDistrictName());
                Metropolis.sendMessage(player, "messages.city.district.city", "%cityname%", district.getCity().getCityName());
                int contactCount = district.getContactplayers().size();
                if (contactCount > 0) {
                    StringBuilder contacts = new StringBuilder();
                    contacts.append("<green>");
                    for (UUID contact : district.getContactplayers()) {
                        contacts.append(ApiedAPI.getPlayer(contact).getName()).append("<dark_green>, <green>");
                        contacts.delete(contacts.length() - 9, contacts.length());
                        if (!contacts.isEmpty()) {
                            Metropolis.sendMessage(player, "messages.city.district.contacts", "%contacts%", contacts.toString(), "%count%", String.valueOf(contactCount));
                        }
                    }
                }
            }
        }

        @Subcommand("set")
        @CommandCompletion("name")
        public static void onSet(Player player, String subcommand, String name) {
            City city = Utilities.hasCityPermissions(player, "metropolis.city.district.set", Role.VICE_MAYOR);
            if (city == null) {
                return;
            }
            if (city.isReserve()) {
                Metropolis.sendMessage(player, "messages.error.city.reserve");
                return;
            }
            if (subcommand.equalsIgnoreCase("name")) {
                live.supeer.metropolis.city.District district = CityDatabase.getDistrict(player.getLocation().toBlockLocation());
                if (district == null) {
                    Metropolis.sendMessage(player, "messages.error.city.district.notInDistrict");
                    return;
                }
                if (name.length() < 3) {
                    Metropolis.sendMessage(player, "messages.error.city.district.nameError", "%maxlength%", String.valueOf(Metropolis.configuration.getDistrictNameLimit()));
                    return;
                }
                if (name.length() > Metropolis.configuration.getDistrictNameLimit()) {
                    Metropolis.sendMessage(player, "messages.error.city.district.nameError", "%maxlength%", String.valueOf(Metropolis.configuration.getDistrictNameLimit()));
                    return;
                }
                district.setDistrictName(name);
                Database.addLogEntry(city, "{ \"type\": \"district\", \"subtype\": \"nameChange\", \"district\": \"" + name + "\", \"player\": \"" + player.getUniqueId() + "\" }");
                Metropolis.sendMessage(player, "messages.city.district.nameChanged", "%districtname%", name, "%cityname%", city.getCityName());
                Utilities.sendScoreboard(player);
            }
        }

        @Subcommand("update")
        public static void onUpdate(Player player) {
            City city = Utilities.hasCityPermissions(player, "metropolis.city.update.district", Role.VICE_MAYOR);
            if (city == null) {
                return;
            }
            if (city.isReserve()) {
                Metropolis.sendMessage(player, "messages.error.city.reserve");
                return;
            }
            live.supeer.metropolis.city.District district = CityDatabase.getDistrict(player.getLocation().toBlockLocation());

            if (district == null) {
                Metropolis.sendMessage(player, "messages.error.city.district.notInDistrict");
                return;
            }

            if (CityDatabase.getCityByClaim(player.getLocation()) != city) {
                Metropolis.sendMessage(player, "messages.error.city.district.outsideCity", "%cityname%", city.getCityName());
                return;
            }

            if (!MetropolisListener.playerPolygons.containsKey(player.getUniqueId())) {
                Metropolis.sendMessage(player, "messages.error.missing.plot");
                return;
            }


            Polygon regionPolygon = MetropolisListener.playerPolygons.get(player.getUniqueId());
            double minX = regionPolygon.getEnvelopeInternal().getMinX();
            double maxX = regionPolygon.getEnvelopeInternal().getMaxX();
            double minY = regionPolygon.getEnvelopeInternal().getMinY();
            double maxY = regionPolygon.getEnvelopeInternal().getMaxY();
            if (maxX - minX < 3 || maxY - minY < 3) {
                Metropolis.sendMessage(player, "messages.error.plot.tooSmall");
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
                        if (!Utilities.containsOnlyCompletePlots(regionPolygon, -64, 319, city, player.getWorld())) {
                            Metropolis.sendMessage(player, "messages.error.city.district.plotsNotCompletelyInside");
                            return;
                        }
                        district.update(player, regionPolygon);
                        Database.addLogEntry(
                                city,
                                "{ \"type\": \"update\", \"subtype\": \"district\", \"name\": "
                                        + district.getDistrictName()
                                        + ", \"player\": "
                                        + player.getUniqueId()
                                        + ", \"districtBounds\": "
                                        + regionPolygon.toText()
                                        + " }");
                        Metropolis.sendMessage(
                                player, "messages.city.district.updated", "%cityname%", city.getCityName(), "%districtname%", district.getDistrictName());
                        return;
                    }
                }
            }
        }
    }

    @Subcommand("unclaim")
    public static void onUnclaim(Player player) {
        City city = Utilities.hasCityPermissions(player, "metropolis.city.unclaim", Role.VICE_MAYOR);
        if (city == null) {
            return;
        }
        if (city.isReserve()) {
            Metropolis.sendMessage(player, "messages.error.city.reserve");
            return;
        }
        Claim claim = CityDatabase.getClaim(player.getLocation());
        if (claim == null || !claim.getCity().equals(city)) {
            Metropolis.sendMessage(player, "messages.error.city.notInClaim", "%cityname%", city.getCityName());
            return;
        }
        if (PlotDatabase.hasPlotInClaim(claim)) {
            Metropolis.sendMessage(player, "messages.error.city.unclaim.hasPlots", "%cityname%", city.getCityName());
            return;
        }
        if (CityDatabase.isDistrictInClaim(claim)) {
            Metropolis.sendMessage(player, "messages.error.city.unclaim.hasDistricts", "%cityname%", city.getCityName());
            return;
        }
        if (CityDatabase.hasCityGoInClaim(claim)) {
            Metropolis.sendMessage(player, "messages.error.city.unclaim.hasCityGo", "%cityname%", city.getCityName());
            return;
        }
        if (CityDatabase.isCitySpawnInClaim(claim)) {
            Metropolis.sendMessage(player, "messages.error.city.unclaim.hasCitySpawn", "%cityname%", city.getCityName());
            return;
        }
        PlayerExitCityEvent exitEvent = new PlayerExitCityEvent(player, city,true);
        Metropolis.getInstance().getServer().getPluginManager().callEvent(exitEvent);
        CityDatabase.deleteClaim(claim);
        Metropolis.sendMessage(player, "messages.city.successful.unclaim", "%cityname%", city.getCityName());
        Database.addLogEntry(
                city,
                "{ \"type\": \"unclaim\", \"claimlocation\": \"" +
                        LocationUtil.formatChunk(claim.getClaimWorld().getName(), claim.getXPosition(), claim.getZPosition()) +
                        "\", \"player\": \"" + player.getUniqueId() + "\" }"
        );
    }

    @Subcommand("delete")
    public static void onDelete(Player player) {
        City city = Utilities.hasCityPermissions(player, "metropolis.city.delete", Role.MAYOR);
        if (city == null) {
            return;
        }
        if (city.isReserve()) {
            Metropolis.sendMessage(player, "messages.error.city.reserve");
            return;
        }

        Metropolis.sendMessage(player, "messages.city.delete.confirmation", "%cityname%", city.getCityName());
    }

    @Subcommand("delete!")
    @Private
    public static void onDeleteConfirm(Player player) {
        City city = Utilities.hasCityPermissions(player, "metropolis.city.delete", Role.MAYOR);
        if (city == null) {
            return;
        }
        if (city.isReserve()) {
            Metropolis.sendMessage(player, "messages.error.city.reserve");
            return;
        }
        Metropolis.playerInCity.remove(player.getUniqueId(), city);
        if (Metropolis.playerInDistrict.containsKey(player.getUniqueId()) && Metropolis.playerInDistrict.get(player.getUniqueId()).getCity().equals(city)) {
            Metropolis.playerInDistrict.remove(player.getUniqueId());
        }
        if (Metropolis.playerInPlot.containsKey(player.getUniqueId()) && Metropolis.playerInPlot.get(player.getUniqueId()).getCity().equals(city)) {
            Metropolis.playerInPlot.remove(player.getUniqueId());
        }
        String cityName = city.getCityName();
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

    @Subcommand("chunk")
    public static void onChunk(Player player) {
        if (!player.hasPermission("metropolis.city.chunk")) {
            Metropolis.sendMessage(player, "messages.error.permissionDenied");
            return;
        }
        int chunkX = player.getChunk().getX();
        int chunkZ = player.getChunk().getZ();
        Claim claim = CityDatabase.getClaim(player.getLocation().toBlockLocation());
        if (claim == null) {
            Metropolis.sendMessage(player,"messages.city.chunk.header","%chunkx%", String.valueOf(chunkX), "%chunkz%", String.valueOf(chunkZ));
            Metropolis.sendMessage(player,"messages.city.chunk.status.nature");
            return;
        }
        chunkZ = claim.getZPosition();
        chunkX = claim.getXPosition();

        // Create a polygon representing the chunk
        GeometryFactory geometryFactory = new GeometryFactory();
        Coordinate[] coordinates = new Coordinate[5];
        coordinates[0] = new Coordinate(chunkX * 16, chunkZ * 16);
        coordinates[1] = new Coordinate((chunkX + 1) * 16, chunkZ * 16);
        coordinates[2] = new Coordinate((chunkX + 1) * 16, (chunkZ + 1) * 16);
        coordinates[3] = new Coordinate(chunkX * 16, (chunkZ + 1) * 16);
        coordinates[4] = coordinates[0]; // Close the polygon
        Polygon chunkPolygon = geometryFactory.createPolygon(coordinates);
        List<Plot> plots = List.of(Objects.requireNonNull(PlotDatabase.intersectingPlots(chunkPolygon, -64, 319, claim.getCity(), player.getWorld())));
        StringBuilder plotList = new StringBuilder();
        Metropolis.sendMessage(player,"messages.city.chunk.header","%chunkx%", String.valueOf(chunkX), "%chunkz%", String.valueOf(chunkZ));
        Metropolis.sendMessage(player,"messages.city.chunk.status.claimed");
        Metropolis.sendMessage(player,"messages.city.chunk.city", "%cityname%", claim.getCity().getCityName());
        if (!plots.isEmpty()) {
            for (Plot plot : plots) {
                plotList.append("<green>").append(plot.getPlotName()).append("<dark_green>, <green>");
            }
            plotList.delete(plotList.length() - 9, plotList.length());
            Metropolis.sendMessage(player, "messages.city.chunk.plots", "%count%", String.valueOf(plots.size()), "%plots%", plotList.toString());
        }
    }

    @Subcommand("twin")
    public static void onTwin(Player player,@Optional String argument) {
        if (argument == null) {
            City city = Utilities.hasCityPermissions(player, "metropolis.city.twin", null);
            if (city == null) {
                return;
            }
            if (city.isReserve()) {
                Metropolis.sendMessage(player, "messages.error.city.reserve");
                return;
            }
            List<City> twinCities = city.getTwinCities();
            if (twinCities.isEmpty()) {
                Metropolis.sendMessage(player, "messages.error.city.twin.none");
                return;
            }
            StringBuilder twinCityList = new StringBuilder();
            for (City twinCity : twinCities) {
                twinCityList.append("<green>").append(twinCity.getCityName()).append("<dark_green>, <green>");
            }
            twinCityList.delete(twinCityList.length() - 9, twinCityList.length());
            Metropolis.sendMessage(player, "messages.city.twin.list", "%cityname%", city.getCityName(), "%twins%", twinCityList.toString(), "%count%", String.valueOf(twinCities.size()));
        } else {
            City city = Utilities.hasCityPermissions(player, "metropolis.city.twin", Role.VICE_MAYOR);
            if (city == null) {
                return;
            }
            if (argument.startsWith("-") || argument.startsWith("+")) {
                String cityName = argument.substring(1);
                if (CityDatabase.getCity(cityName).isEmpty()) {
                    Metropolis.sendMessage(player, "messages.error.city.twin.notFound", "%cityname%", city.getCityName());
                    return;
                }
                City twinCity = CityDatabase.getCity(cityName).get();
                if (twinCity.equals(city)) {
                    Metropolis.sendMessage(player, "messages.error.city.twin.sameCity", "%cityname%", city.getCityName());
                    return;
                }
                if (argument.startsWith("-")) {
                    if (!player.hasPermission("metropolis.city.twin.remove")) {
                        Metropolis.sendMessage(player, "messages.error.permissionDenied");
                        return;
                    }
                    if (city.getTwinCities().contains(twinCity)) {
                        city.removeTwinCity(twinCity);
                        Database.addLogEntry(city, "{ \"type\": \"twin\", \"subtype\": \"remove\", \"twin\": \"" + twinCity.getCityName() + "\", \"player\": \"" + player.getUniqueId() + "\" }");
                        Metropolis.sendMessage(player, "messages.city.twin.removed", "%cityname%", city.getCityName(), "%twin%", twinCity.getCityName());
                        return;
                    }
                    Metropolis.sendMessage(player, "messages.error.city.twin.notTwin", "%cityname%", city.getCityName(), "%twin%", twinCity.getCityName());
                    return;
                }
                if (argument.startsWith("+")) {
                    if (city.getTwinCities().contains(twinCity)) {
                        Metropolis.sendMessage(player, "messages.error.city.twin.alreadyTwin", "%cityname%", city.getCityName(), "%twin%", twinCity.getCityName());
                        return;
                    }
                    city.addTwinCity(twinCity);
                    Database.addLogEntry(city, "{ \"type\": \"twin\", \"subtype\": \"add\", \"twin\": \"" + twinCity.getCityName() + "\", \"player\": \"" + player.getUniqueId() + "\" }");
                    Metropolis.sendMessage(player, "messages.city.twin.added", "%cityname%", city.getCityName(), "%twin%", twinCity.getCityName());
                }
            } else {
                Metropolis.sendMessage(player, "messages.syntax.city.twin");
            }
        }
    }

    @Subcommand("kick")
    public static void onKick(Player player, String playerName, String reason) {
        City city = Utilities.hasCityPermissions(player, "metropolis.city.kick", Role.ASSISTANT);
        if (city == null) {
            return;
        }
        if (city.isReserve()) {
            Metropolis.sendMessage(player, "messages.error.city.reserve");
            return;
        }
        Role role = CityDatabase.getCityRole(city, player.getUniqueId().toString());
        assert role != null;
        MPlayer mPlayer = ApiedAPI.getPlayer(playerName);
        if (mPlayer == null) {
            Metropolis.sendMessage(player, "messages.error.player.notFound", "%player%", playerName);
            return;
        }
        if (reason.length() < 4) {
            Metropolis.sendMessage(player, "messages.error.city.kick.tooShort");
            return;
        }
        if (!CityDatabase.memberExists(mPlayer.getUuid().toString(), city)) {
            Metropolis.sendMessage(player, "messages.error.city.kick.notInCity", "%cityname%", city.getCityName());
            return;
        }
        if (mPlayer.getUuid() == player.getUniqueId()) {
            Metropolis.sendMessage(player, "messages.error.city.kick.cannotKickSelf", "%cityname%", city.getCityName());
            return;
        }
        //if targetPlayer has higher role
        Role targetRole = CityDatabase.getCityRole(city, mPlayer.getUuid().toString());
        if (targetRole == null) {
            Metropolis.sendMessage(player, "messages.error.city.kick.notInCity", "%cityname%", city.getCityName());
            return;
        }
        if (targetRole.getPermissionLevel() >= role.getPermissionLevel()) {
            Metropolis.sendMessage(player, "messages.error.city.kick.cannotKickHigherRole", "%cityname%", city.getCityName());
            return;
        }
        if (targetRole.equals(Role.MAYOR)) {
            Metropolis.sendMessage(player, "messages.error.city.kick.cannotKickMayor", "%cityname%", city.getCityName());
            return;
        }
        if (Metropolis.getInstance().getServer().getPlayer(mPlayer.getUuid()) != null) {
            Metropolis.sendMessage(Objects.requireNonNull(Metropolis.getInstance().getServer().getPlayer(mPlayer.getUuid())), "messages.city.kick.kicked", "%cityname%", city.getCityName(), "%reason%", reason);
        }
        city.removeCityMember(mPlayer.getUuid().toString());
        CityLeaveEvent leaveEvent = new CityLeaveEvent(player, city);
        Metropolis.getInstance().getServer().getPluginManager().callEvent(leaveEvent);
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
        Database.addLogEntry(city, "{ \"type\": \"kick\", \"player\": \"" + mPlayer.getUuid() + "\", \"issuer\": \"" + player.getUniqueId() + "\", \"reason\": \"" + reason + "\" }");
    }

    @Subcommand("broadcast")
    public static void onBroadcast(Player player, String message) {
        City city = Utilities.hasCityPermissions(player, "metropolis.city.broadcast", Role.ASSISTANT);
        if (city == null) {
            return;
        }
        if (city.isReserve()) {
            Metropolis.sendMessage(player, "messages.error.city.reserve");
            return;
        }
        for (Member member : city.getCityMembers()) {
            Player targetPlayer = Bukkit.getPlayer(UUID.fromString(member.getPlayerUUID()));
            if (targetPlayer == null) {
                continue;
            }
            Metropolis.sendMessage(targetPlayer, "messages.city.broadcast", "%cityname%", city.getCityName(), "%message%", message);
        }
    }
}
