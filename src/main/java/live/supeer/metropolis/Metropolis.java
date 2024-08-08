package live.supeer.metropolis;

import co.aikar.commands.PaperCommandManager;
import co.aikar.idb.DB;
import com.google.common.collect.ImmutableList;
import live.supeer.metropolis.city.*;
import live.supeer.metropolis.command.CommandCity;
import live.supeer.metropolis.command.CommandHere;
import live.supeer.metropolis.command.CommandHomeCity;
import live.supeer.metropolis.command.CommandPlot;
import live.supeer.metropolis.homecity.HCDatabase;
import live.supeer.metropolis.plot.Plot;
import live.supeer.metropolis.plot.PlotDatabase;
import live.supeer.metropolis.utils.DateUtil;
import live.supeer.metropolis.utils.LocationUtil;
import live.supeer.metropolis.utils.Utilities;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Logger;

public final class Metropolis extends JavaPlugin {
    public static HashMap<UUID, City> playerInCity = new HashMap<>();
    public static HashMap<UUID, Plot> playerInPlot = new HashMap<>();
    public Logger logger = null;
    public static MetropolisConfiguration configuration;
    @Getter
    private static Metropolis plugin;
    private LanguageManager languageManager;
    private static Economy econ = null;

    @Override
    public void onEnable() {
        plugin = this;
        this.logger = getLogger();
        configuration = new MetropolisConfiguration(this);
        Utilities.plugin = this;
        DateUtil.plugin = this;
        CommandCity.plugin = this;
        CommandPlot.plugin = this;
        CommandHomeCity.plugin = this;
        CommandHere.plugin = this;
        Database.plugin = this;
        HCDatabase.plugin = this;
        CityDatabase.plugin = this;
        CityListener.plugin = this;
        Claim.plugin = this;
        City.plugin = this;
        Member.plugin = this;
        LocationUtil.plugin = this;
        MetropolisListener.plugin = this;
        PlotDatabase.plugin = this;
        this.languageManager = new LanguageManager(this, "sv_se");
        if (!setupEconomy()) {
            this.getLogger().severe("[Metropolis] Vault not found, disabling plugin");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        if (getServer().getPluginManager().getPlugin("CoreProtect") == null) {
            this.getLogger().severe("[Metropolis] CoreProtect not found, disabling plugin");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        PaperCommandManager manager = new PaperCommandManager(this);
        manager.enableUnstableAPI("brigadier");

        manager.registerCommand(new CommandHomeCity());
        manager.registerCommand(new CommandCity());
        manager.registerCommand(new CommandPlot());
        manager.registerCommand(new CommandHere());
        this.getServer().getPluginManager().registerEvents(new CommandHomeCity(), this);
        this.getServer().getPluginManager().registerEvents(new MetropolisListener(), this);
        this.getServer().getPluginManager().registerEvents(new CityListener(), this);
        Database.initialize();
        Database.synchronize();
        registerCompletions(manager);
    }

    @Override
    public void onDisable() {
        DB.close();
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp =
                getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    public static Economy getEconomy() {
        return econ;
    }

    public void sendMessage(
            @NotNull CommandSender sender, @NotNull String key, String... replacements) {
        String message = this.languageManager.getValue(key, getLocale(sender), replacements);

        if (message != null && !message.isEmpty()) {
            Component component = languageManager.getMiniMessage().deserialize(message);
            sender.sendMessage(component);
        }
    }

    public String getMessage(@NotNull String key, String... replacements) {
        String message = this.languageManager.getValue(key, "sv_se", replacements);

        if (message != null && !message.isEmpty()) {
            // Deserialize MiniMessage to a Component
            Component component = languageManager.getMiniMessage().deserialize(message);
            // Convert the Component to a legacy formatted string
            return LegacyComponentSerializer.legacySection().serialize(component);
        }
        return null;
    }

    public String getRawMessage(@NotNull String key, String... replacements) {
        return this.languageManager.getValue(key, "sv_se", replacements);
    }

    private @NotNull String getLocale(@NotNull CommandSender sender) {
        if (sender instanceof Player) {
            return ((Player) sender).getLocale();
        } else {
            return this.getConfig().getString("settings.locale", "sv_se");
        }
    }

    public static void registerCompletions(PaperCommandManager manager) {
        manager.getCommandCompletions().registerCompletion("plotType", c -> ImmutableList.of("church", "farm", "shop", "vacation"));
        manager.getCommandCompletions().registerCompletion("cityRoles", c -> ImmutableList.of("vicemayor", "assistant", "inviter", "member", "swap", "-", "member"));
        manager.getCommandCompletions().registerCompletion("cityGo1", c -> ImmutableList.of("delete", "set"));
        manager.getCommandCompletions().registerCompletion("cityGo2", c -> ImmutableList.of("set"));
        manager.getCommandCompletions().registerCompletion("cityGo3", c -> ImmutableList.of("displayname", "accesslevel"));
        manager.getCommandCompletions().registerCompletion("cityGoes", c -> {
            Player player = c.getPlayer();
            if (player == null) {
                return Collections.emptyList();
            }

            City city = HCDatabase.getHomeCityToCity(player.getUniqueId().toString());
            if (city == null) {
                return Collections.emptyList();
            }

            Role role = CityDatabase.getCityRole(city, player.getUniqueId().toString());
            if (role == null) {
                return Collections.emptyList();
            }

            return CityDatabase.getCityGoNames(city, role);
        });
    }
}
