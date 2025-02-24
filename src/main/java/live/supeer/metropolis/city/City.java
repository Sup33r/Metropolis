package live.supeer.metropolis.city;

import co.aikar.idb.DB;
import co.aikar.idb.DbRow;
import live.supeer.apied.ApiedAPI;
import live.supeer.apied.MPlayer;
import live.supeer.metropolis.Database;
import live.supeer.metropolis.JailManager;
import live.supeer.metropolis.Metropolis;
import live.supeer.metropolis.homecity.HCDatabase;
import live.supeer.metropolis.octree.Octree;
import live.supeer.metropolis.utils.LocationUtil;
import live.supeer.metropolis.plot.Plot;
import live.supeer.metropolis.utils.Utilities;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.World;

import java.sql.SQLException;
import java.util.*;

@Getter
public class City {
    private final int cityId;
    private String cityName;
    private final String originalMayorUUID;
    private final List<Member> cityMembers = new ArrayList<>();
    private List<CityPerms> cityPerms = new ArrayList<>();

    private int cityClaims;
    private final Map<World, Octree<Claim>> claimOctree = new HashMap<>();

    private final List<Plot> cityPlots = new ArrayList<>();
    private final List<District> cityDistricts = new ArrayList<>();
    private List<City> twinCities;
//    private final List<Ban> cityBans = new ArrayList<>();
    private int minChunkDistance;
    private int minSpawnDistance;
    private int bonusClaims;
    private int cityBalance;
    private Location citySpawn;
    private final long cityCreationDate;
    private String enterMessage;
    private String exitMessage;
    private String motdMessage;
    private String taxLevel;
    private char[] memberPerms;
    private char[] outsiderPerms;
    private char[] cityFlags;
    private boolean isOpen;
    private boolean isPublic;
    private boolean isTaxExempt;
    private boolean isReserve;
    private double cityTax;
    private int maxPlotsPerMember;

    public City(DbRow data) {
        this.cityId = data.getInt("cityId");
        this.cityName = data.getString("cityName");
        this.originalMayorUUID = data.getString("originalMayorUUID");
        this.cityBalance = data.getInt("cityBalance");
        this.citySpawn = LocationUtil.stringToLocation(data.getString("citySpawn"));
        this.cityCreationDate = data.getLong("createDate");
        this.enterMessage = data.getString("enterMessage");
        this.exitMessage = data.getString("exitMessage");
        this.motdMessage = data.getString("motdMessage");
        this.isOpen = data.get("isOpen");
        this.isPublic = data.get("isPublic");
        this.isTaxExempt = data.get("isTaxExempt");
        this.isReserve = data.get("isReserve");
        this.bonusClaims = data.getInt("bonusClaims");
        this.minChunkDistance = data.getInt("minChunkDistance");
        this.minSpawnDistance = data.getInt("minSpawnDistance");
        this.cityTax = data.getDbl("cityTax");
        this.twinCities = new ArrayList<>(Utilities.stringToCityList(data.getString("twinCities")));
        this.cityFlags = data.getString("cityFlags") == null ? new char[0] : data.getString("cityFlags").toCharArray();
        this.maxPlotsPerMember = data.getInt("maxPlotsPerMember");
        this.taxLevel = data.getString("taxLevel");
        this.memberPerms = data.getString("memberPerms") == null ? new char[0] : data.getString("memberPerms").toCharArray();
        this.outsiderPerms = data.getString("outsiderPerms") == null ? new char[0] : data.getString("outsiderPerms").toCharArray();
        this.cityPerms = loadPlayerCityPerms();
    }

    public void setTaxLevel(Role role) {
        if (role == null) {
            this.taxLevel = "none";
        } else {
            this.taxLevel = role.toString();
        }
        DB.executeUpdateAsync("UPDATE `mp_cities` SET `taxLevel` = ? WHERE `cityId` = ?", taxLevel, cityId);
    }

    public void addTwinCity(City city) {
        twinCities.add(city);
        DB.executeUpdateAsync("UPDATE `mp_cities` SET `twinCities` = ? WHERE `cityId` = ?", Utilities.cityListToString(twinCities), cityId);
    }

    public void removeTwinCity(City city) {
        twinCities.remove(city);
        DB.executeUpdateAsync("UPDATE `mp_cities` SET `twinCities` = ? WHERE `cityId` = ?", Utilities.cityListToString(twinCities), cityId);
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
        DB.executeUpdateAsync("UPDATE `mp_cities` SET `cityName` = ? WHERE `cityId` = ?", cityName, cityId);
    }

    public void setMaxPlotsPerMember(int maxPlotsPerMember) {
        this.maxPlotsPerMember = maxPlotsPerMember;
        DB.executeUpdateAsync("UPDATE `mp_cities` SET `maxPlotsPerMember` = ? WHERE `cityId` = ?", maxPlotsPerMember, cityId);
    }

    public void addCityBalance(int cityBalance) {
        this.cityBalance += cityBalance;
        DB.executeUpdateAsync("UPDATE `mp_cities` SET `cityBalance` = ? WHERE `cityId` = ?", this.cityBalance, cityId);
    }

    public void removeCityBalance(int cityBalance) {
        this.cityBalance -= cityBalance;
        DB.executeUpdateAsync("UPDATE `mp_cities` SET `cityBalance` = ? WHERE `cityId` = ?", this.cityBalance, cityId);
    }

    public void setMinChunkDistance(int minChunkDistance) {
        this.minChunkDistance = minChunkDistance;
        DB.executeUpdateAsync("UPDATE `mp_cities` SET `minChunkDistance` = ? WHERE `cityId` = ?", minChunkDistance, cityId);
    }

    public void setMinSpawnDistance(int minSpawnDistance) {
        this.minSpawnDistance = minSpawnDistance;
        DB.executeUpdateAsync("UPDATE `mp_cities` SET `minSpawnDistance` = ? WHERE `cityId` = ?", minSpawnDistance, cityId);
    }

    public boolean hasFlag(char needle) {
        if (this.cityFlags == null) {
            return false;
        }

        for (char option : this.cityFlags) {
            if (option == needle) {
                return true;
            }
        }
        return false;
    }

    public void setCityFlags(String flags) {
        this.cityFlags = flags.toCharArray();
        DB.executeUpdateAsync("UPDATE `mp_cities` SET `cityFlags` = ? WHERE `cityId` = ?", flags, cityId);
    }

    public boolean hasMember(String uuid) {
        for (Member member : cityMembers) {
            if (member.getPlayerUUID().equals(uuid)) {
                return true;
            }
        }
        return false;
    }

    public CityPerms getPlayerCityPerm(UUID uuid) {
        for (CityPerms perm : cityPerms) {
            if (perm.getPlayerUUID().equals(uuid.toString())) {
                return perm;
            }
        }
        DbRow row = null;
        try {
            row = DB.getFirstRow(
                    "SELECT * FROM `mp_cityperms` WHERE `cityId` = ? AND `playerUUID` = ?", cityId, uuid.toString()
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (row == null) {
            return null; // Return null if no data is found
        }
        return new CityPerms(row);
    }

    public List<CityPerms> loadPlayerCityPerms() {
        List<CityPerms> cityPermsList = new ArrayList<>();
        try {
            List<DbRow> rows =
                    DB.getResults("SELECT * FROM `mp_cityperms` WHERE `cityId` = ?", cityId);
            for (DbRow row : rows) {
                cityPermsList.add(new CityPerms(row));
            }
            return cityPermsList;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Claim> getCityClaimList() {
        List<Claim> claims = new ArrayList<>();
        for (Octree<Claim> octree : claimOctree.values()) {
            octree.forEach(claims::add);
        }
        return claims;
    }

    public void setCityPerms(String type, String perms, UUID uuid) {
        try {
            if (type.equalsIgnoreCase("members")) {
                DB.executeUpdate("UPDATE `mp_cities` SET `memberPerms` = ? WHERE `cityId` = ?", perms, cityId);
                this.memberPerms = perms.toCharArray();
                return;
            } else if (type.equalsIgnoreCase("outsiders")) {
                DB.executeUpdate("UPDATE `mp_cities` SET `outsiderPerms` = ? WHERE `cityId` = ?", perms, cityId);
                this.outsiderPerms = perms.toCharArray();
                return;
            }
            DB.executeUpdate("INSERT INTO `mp_cityperms` (`cityId`, `cityPerms`, `playerUUID`) VALUES (?,?,?) ON DUPLICATE KEY UPDATE cityPerms = ?", cityId, perms, uuid.toString(), perms);
            CityPerms cityPerm = new CityPerms(cityId, perms, uuid.toString());
            cityPerms.removeIf(p -> p.getPlayerUUID().equals(uuid.toString()));
            cityPerms.add(cityPerm);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removeCityPerms() {
        try {
            DB.executeUpdate("UPDATE `mp_cities` SET `memberPerms` = ?, `outsiderPerms` = ? WHERE `plotId` = ?","","", cityId);
            DB.executeUpdate("DELETE FROM `mp_cityperms` WHERE `plotId` = ?", cityId);
            this.memberPerms = new char[] {' '};
            this.outsiderPerms = new char[] {' '};
            this.cityPerms.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void removeCityMember(Member member) {
        this.cityMembers.remove(member);
        HCDatabase.removeHomeCity(member.getPlayerUUID(), this);
        DB.executeUpdateAsync("DELETE FROM `mp_members` WHERE `cityId` = ? AND `playerUUID` = ?", cityId, member.getPlayerUUID());
    }

    public void removeCityMember(String playerUUID) {
        Member member = getCityMember(playerUUID);
        if (member != null) {
            this.cityMembers.remove(member);
            HCDatabase.removeHomeCity(playerUUID, this);
            DB.executeUpdateAsync("DELETE FROM `mp_members` WHERE `cityId` = ? AND `playerUUID` = ?", cityId, playerUUID);
        }
    }

    public void setAsReserve() {
        this.isReserve = true;
        DB.executeUpdateAsync("UPDATE `mp_cities` SET `isReserve` = ? WHERE `cityId` = ?", 1, cityId);
    }

    public void setAsNotReserve() {
        this.isReserve = false;
        DB.executeUpdateAsync("UPDATE `mp_cities` SET `isReserve` = ? WHERE `cityId` = ?", 0, cityId);
    }

    public void setCitySpawn(Location citySpawn) {
        this.citySpawn = citySpawn;
        DB.executeUpdateAsync("UPDATE `mp_cities` SET `citySpawn` = ? WHERE `cityId` = ?", LocationUtil.locationToString(citySpawn), cityId);
    }

    public void setCityTax(double cityTax) {
        DB.executeUpdateAsync("UPDATE `mp_cities` SET `cityTax` = ? WHERE `cityId` = ?", cityTax, cityId);
        this.cityTax = cityTax;
    }

    public void addBonusClaims(int bonusClaims) {
        this.bonusClaims += bonusClaims;
        DB.executeUpdateAsync("UPDATE `mp_cities` SET `bonusClaims` = ? WHERE `cityId` = ?", this.bonusClaims, cityId);
    }

    public void setEnterMessage(String enterMessage) {
        this.enterMessage = enterMessage;
        DB.executeUpdateAsync("UPDATE `mp_cities` SET `enterMessage` = ? WHERE `cityId` = ?", enterMessage, cityId);
    }

    public int calculateCost() {
        int baseCost = 100000;
        int claimCost = 500 * this.cityClaims;
        int balanceCost = Math.max(0, -this.cityBalance);

        return baseCost + claimCost + balanceCost;
    }

    public void setExitMessage(String exitMessage) {
        this.exitMessage = exitMessage;
        DB.executeUpdateAsync("UPDATE `mp_cities` SET `exitMessage` = ? WHERE `cityId` = ?", exitMessage, cityId);
    }

    public void setMotdMessage(String motdMessage) {
        this.motdMessage = motdMessage;
        DB.executeUpdateAsync("UPDATE `mp_cities` SET `motdMessage` = ? WHERE `cityId` = ?", motdMessage, cityId);
    }

    public void addCityMember(Member member) {
        cityMembers.add(member);
    }

    public Member getCityMember(String playerUUID) {
        for (Member member : cityMembers) {
            if (member.getPlayerUUID().equals(playerUUID)) {
                return member;
            }
        }
        return null;
    }

    public boolean toggleOpen() {
        DB.executeUpdateAsync("UPDATE `mp_cities` SET `isOpen` = ? WHERE `cityId` = ?", isOpen ? 0 : 1, cityId);
        return isOpen = !isOpen;
    }

    public boolean togglePublic() {
        DB.executeUpdateAsync("UPDATE `mp_cities` SET `isPublic` = ? WHERE `cityId` = ?", isPublic ? 0 : 1, cityId);
        return isPublic = !isPublic;
    }

    public void setTaxExempt(boolean isTaxExempt) {
        DB.executeUpdateAsync("UPDATE `mp_cities` SET `isTaxExempt` = ? WHERE `cityId` = ?", isTaxExempt ? 1 : 0, cityId);
        this.isTaxExempt = isTaxExempt;
    }

    public boolean cityCouldGoUnder(int days) {
        if (isTaxExempt) {
            return false;
        }
        return cityClaims * Metropolis.configuration.getStateTax() * days > cityBalance;
    }

    public boolean hasNegativeBalance() {
        return cityBalance < 0;
    }

    public void drawStateTaxes() {
        try {
            if (isTaxExempt) {
                return;
            }
            int tax = cityClaims * Metropolis.configuration.getStateTax();
            cityBalance -= tax;
            DB.executeUpdate("UPDATE `mp_cities` SET `cityBalance` = ? WHERE `cityId` = ?", cityBalance, cityId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void compensateForInmates() {
        int compensation = 0;
        for (Plot plot : cityPlots) {
            if (plot.getPlotType() != null && plot.getPlotType().equalsIgnoreCase("jail")) {
                compensation += JailManager.getOccupiedCellsCount(plot) * Metropolis.configuration.getDailyPayback();
            }
        }
        cityBalance += compensation;
        Database.addLogEntry(this, "{ \"type\": \"cityBank\", \"subtype\": \"dailyJailCompensation\", \"balance\": " + compensation + "}");
    }

    public boolean canBecomeReserve() {
        if (cityMembers.size() >= 20) {
            return true;
        }
        return cityClaims >= 25;
    }

    public void drawCityTaxes() {
        if (isTaxExempt) {
            return;
        }
        Role cityRole = Role.fromString(taxLevel);
        if (Objects.equals(taxLevel, "none") || cityTax == 0 || cityRole == null) {
            return;
        }

        for (Member member : cityMembers) {
            try {
                Role memberRole = member.getCityRole();
                if (memberRole == null) {
                    continue;
                }

                // Check if the member should pay tax based on their role
                if (memberRole.permissionLevel() >= cityRole.permissionLevel()) {
                    double taxRate = cityTax / 100.0; // Convert percentage to decimal

                    // Reduce tax rate for members in multiple cities
                    if (Objects.requireNonNull(CityDatabase.memberCityList(member.getPlayerUUID())).size() > 1) {
                        taxRate /= 2;
                    }

                    MPlayer mPlayer = ApiedAPI.getPlayer(UUID.fromString(member.getPlayerUUID()));
                    int playerBalance = mPlayer.getBalance();

                    if (playerBalance > 0) {
                        double taxAmount = playerBalance * taxRate;
                        int roundedTaxAmount = (int) Math.round(taxAmount); // Round to nearest integer

                        if (roundedTaxAmount > 0) {
                            mPlayer.removeBalance(roundedTaxAmount, "{ \"type\": \"city\", \"subtype\": \"tax\", \"cityId\": " + cityId +"}");
                            Database.addLogEntry(this, "{ \"type\": \"cityBank\", \"subtype\": \"tax\", \"balance\": " + roundedTaxAmount + ", \"player\": " + member.getPlayerUUID() + " }");
                            cityBalance += roundedTaxAmount;

                            DB.executeUpdateAsync("UPDATE `mp_cities` SET `cityBalance` = ? WHERE `cityId` = ?", cityBalance, cityId);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

//    public void addCityBan(Ban ban) {
//        cityBans.add(ban);
//    }
//
//    public void removeCityBan(Ban ban) {
//        cityBans.remove(ban);
//    }

    public void addCityClaim(Claim claim) {
        cityClaims++;
        var octree = claimOctree.computeIfAbsent(claim.getClaimWorld(), world -> new Octree<>());
        octree.put(claim.getXPosition(), 0, claim.getZPosition(), claim);
    }

    public void removeCityClaim(Claim claim) {
        cityClaims--;
        var octree = claimOctree.get(claim.getClaimWorld());
        if (octree != null)
            octree.remove(claim.getXPosition(), 0, claim.getZPosition());
    }

    public Claim getCityClaim(Location location) {
        var octree = claimOctree.get(location.getWorld());
        if (octree == null)
            return null;

        return octree.get(location.getChunk().getX(), 0, location.getChunk().getZ());
    }

    public void addCityPlot(Plot plot) {
        cityPlots.add(plot);
    }

    public void removeCityPlot(Plot plot) {
        cityPlots.remove(plot);
    }

    public void addCityDistrict(District district) {
        cityDistricts.add(district);
    }

    public void removeCityDistrict(District district) {
        cityDistricts.remove(district);
    }

    public boolean isMember(UUID uuid) {
        for (Member member : cityMembers) {
            if (member.getPlayerUUID().equals(uuid.toString())) {
                return true;
            }
        }
        return false;
    }
}
