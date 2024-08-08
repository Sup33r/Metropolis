package live.supeer.metropolis;

import lombok.Getter;

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
    private final int inviteCooldown;
    private final int nameChangeCooldown;
    private final String maxBanTime;

    private final int cityNameLimit;
    private final int cityGoNameLimit;
    private final int cityGoDisplayNameLimit;
    private final int cityMotdLimit;
    private final int cityEnterMessageLimit;
    private final int cityExitMessageLimit;
    private final int plotNameLimit;

    private final double cityMaxTax;

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
        nameChangeCooldown = plugin.getConfig().getInt("settings.cooldownTime.namechange");
        maxBanTime = plugin.getConfig().getString("settings.limits.maxbantime");

        cityNameLimit = plugin.getConfig().getInt("settings.limits.cityname");
        cityGoDisplayNameLimit = plugin.getConfig().getInt("settings.limits.citygodisplayname");
        cityGoNameLimit = plugin.getConfig().getInt("settings.limits.citygoname");
        cityMotdLimit = plugin.getConfig().getInt("settings.limits.motd");
        cityEnterMessageLimit = plugin.getConfig().getInt("settings.limits.entermessage");
        cityExitMessageLimit = plugin.getConfig().getInt("settings.limits.exitmessage");
        plotNameLimit = plugin.getConfig().getInt("settings.limits.plotname");

        cityMaxTax = plugin.getConfig().getDouble("settings.limits.citymaxtax");
    }
}
