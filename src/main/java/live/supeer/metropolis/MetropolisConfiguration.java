package live.supeer.metropolis;

import lombok.Getter;

import java.util.List;

@Getter
public class MetropolisConfiguration {

    private final String sqlHost;
    private final int sqlPort;
    private final String sqlDatabase;
    private final String sqlUsername;
    private final String sqlPassword;
    private final int cityCreationCost;
    private final int cityStartingBalance;
    private final int cityStartingTax;
    private final int cityBonusCost;
    private final int cityGoCost;
    private final int cityClaimCost;
    private final int cityOutpostCost;
    private final int districtCreationCost;
    private final int inviteCooldown;
    private final int nameChangeCooldown;
    private final String maxBanTime;
    private final String startingTaxLevel;

    private final int cityNameLimit;
    private final int cityGoNameLimit;
    private final int cityGoDisplayNameLimit;
    private final int cityMotdLimit;
    private final int cityEnterMessageLimit;
    private final int cityExitMessageLimit;
    private final int plotNameLimit;
    private final int districtNameLimit;
    private final int maxAmountOfPlots;
    private final int stateTax;

    private final int taxTimeHour;
    private final int taxTimeMinute;
    private final int taxTimeSecond;

    private final int minChunkDistance;
    private final int minSpawnDistance;

    private final int prisonerPayback;
    private final int dailyPayback;

    private final double cityMaxTax;

    private final List<String> leaderboardMobFilter;
    private final List<String> leaderboardBlockFilter;

    MetropolisConfiguration(Metropolis plugin) {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        sqlHost = plugin.getConfig().getString("sql.host");
        sqlPort = plugin.getConfig().getInt("sql.port");
        sqlDatabase = plugin.getConfig().getString("sql.database");
        sqlUsername = plugin.getConfig().getString("sql.username");
        sqlPassword = plugin.getConfig().getString("sql.password");
        inviteCooldown = plugin.getConfig().getInt("settings.cooldownTime.invite");
        cityOutpostCost = plugin.getConfig().getInt("settings.city.outpostcost");
        cityCreationCost = plugin.getConfig().getInt("settings.city.creationcost");
        cityStartingBalance = plugin.getConfig().getInt("settings.city.startingbalance");
        cityStartingTax = plugin.getConfig().getInt("settings.city.startingtax");
        cityGoCost = plugin.getConfig().getInt("settings.city.gocost");
        cityBonusCost = plugin.getConfig().getInt("settings.city.bonuscost");
        cityClaimCost = plugin.getConfig().getInt("settings.city.claimcost");
        districtCreationCost = plugin.getConfig().getInt("settings.city.districtcost");
        nameChangeCooldown = plugin.getConfig().getInt("settings.cooldownTime.namechange");
        maxBanTime = plugin.getConfig().getString("settings.limits.maxbantime");
        startingTaxLevel = plugin.getConfig().getString("settings.city.startingtaxlevel");
        stateTax = plugin.getConfig().getInt("settings.city.statetax");

        cityNameLimit = plugin.getConfig().getInt("settings.limits.cityname");
        cityGoDisplayNameLimit = plugin.getConfig().getInt("settings.limits.citygodisplayname");
        cityGoNameLimit = plugin.getConfig().getInt("settings.limits.citygoname");
        cityMotdLimit = plugin.getConfig().getInt("settings.limits.motd");
        cityEnterMessageLimit = plugin.getConfig().getInt("settings.limits.entermessage");
        cityExitMessageLimit = plugin.getConfig().getInt("settings.limits.exitmessage");
        plotNameLimit = plugin.getConfig().getInt("settings.limits.plotname");
        districtNameLimit = plugin.getConfig().getInt("settings.limits.districtname");
        maxAmountOfPlots = plugin.getConfig().getInt("settings.limits.maxamountofplots");

        cityMaxTax = plugin.getConfig().getDouble("settings.limits.citymaxtax");
        minChunkDistance = plugin.getConfig().getInt("settings.limits.minchunkdistance");
        minSpawnDistance = plugin.getConfig().getInt("settings.limits.minspawndistance");

        taxTimeHour = plugin.getConfig().getInt("settings.taxtime.hour");
        taxTimeMinute = plugin.getConfig().getInt("settings.taxtime.minute");
        taxTimeSecond = plugin.getConfig().getInt("settings.taxtime.second");

        prisonerPayback = plugin.getConfig().getInt("settings.jail.prisonerpayback");
        dailyPayback = plugin.getConfig().getInt("settings.jail.dailypayback");

        leaderboardMobFilter = plugin.getConfig().getStringList("leaderboard.mobfilter");
        leaderboardBlockFilter = plugin.getConfig().getStringList("leaderboard.blockfilter");
    }
}
