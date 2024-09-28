package live.supeer.metropolis;

import co.aikar.commands.PaperCommandManager;
import co.aikar.idb.DB;
import com.google.common.collect.ImmutableList;
import live.supeer.apied.ApiedAPI;
import live.supeer.apied.ExpiringBan;
import live.supeer.apied.MPlayer;
import live.supeer.metropolis.city.*;
import live.supeer.metropolis.command.*;
import live.supeer.metropolis.homecity.HCDatabase;
import live.supeer.metropolis.plot.Plot;
import live.supeer.metropolis.plot.PlotDatabase;
import live.supeer.metropolis.utils.DateUtil;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public final class Metropolis extends JavaPlugin {
    public static HashMap<UUID, City> playerInCity = new HashMap<>();
    public static HashMap<UUID, Plot> playerInPlot = new HashMap<>();
    public static HashMap<UUID, District> playerInDistrict = new HashMap<>();
    public static HashMap<Plot,Leaderboard> plotLeaderboards = new HashMap<>();
    public static HashMap<Plot, List<Standing>> plotStandings = new HashMap<>();
    public static HashMap<UUID, List<Component>> afkPlayers = new HashMap<>();
    public static HashMap<Player, Location> backLocations = new HashMap<>();
    public static List<Player> overrides = new ArrayList<>();
    public static List<UUID> scheduledForUnban = new ArrayList<>();
    public Logger logger = null;
    public static MetropolisConfiguration configuration;
    @Getter
    private static Metropolis plugin;
    private static LanguageManager languageManager;

    public static Metropolis getInstance() {
        return plugin;
    }

    public void onEnable() {
        plugin = this;
        this.logger = getLogger();
        configuration = new MetropolisConfiguration(this);
        languageManager = new LanguageManager(this, "sv_se");
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
        manager.registerCommand(new CommandShutdown());
        manager.registerCommand(new CommandMetropolis());
        this.getServer().getPluginManager().registerEvents(new MetropolisListener(), this);
        this.getServer().getPluginManager().registerEvents(new CityListener(), this);
        this.getServer().getPluginManager().registerEvents(new ProtectionListener(), this);
        Database.initialize();
        Database.synchronize();
        registerCompletions(manager);
        scheduleDailyTaxCollection();
        scheduleWeeklyPlotRentCollection();

        MetropolisAPI.setPlugin(this);

        if (getServer().getPluginManager().getPlugin("Apied") != null) {
            getLogger().info("Apied found!");
        } else {
            getLogger().warning("Apied not found. disabling plugin");
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        DB.close();
    }

    public static void sendMessage(@NotNull CommandSender sender, @NotNull String key, String... replacements) {
        String message = languageManager.getValue(key, getLocale(sender), replacements);
        if (message == null || message.isEmpty()) {
            sender.sendMessage("§cMessage not found: " + key);
        }
        if (message != null && !message.isEmpty()) {
            Component component = languageManager.getMiniMessage().deserialize(message);
            sender.sendMessage(component);
        }
    }

    public static String getMessage(@NotNull String key, String... replacements) {
        String message = languageManager.getValue(key, "sv_se", replacements);

        if (message != null && !message.isEmpty()) {
            // Deserialize MiniMessage to a Component
            Component component = languageManager.getMiniMessage().deserialize(message);
            // Convert the Component to a legacy formatted string
            return LegacyComponentSerializer.legacySection().serialize(component);
        }
        return null;
    }

    public static Component getMessageComponent(@NotNull String key, String... replacements) {
        String message = languageManager.getValue(key, "sv_se", replacements);

        if (message != null && !message.isEmpty()) {
            return languageManager.getMiniMessage().deserialize(message);
        }
        return null;
    }

    public static String getRawMessage(@NotNull String key, String... replacements) {
        return languageManager.getValue(key, "sv_se", replacements);
    }

    private static @NotNull String getLocale(@NotNull CommandSender sender) {
        if (sender instanceof Player) {
            return ((Player) sender).getLocale();
        } else {
            return Metropolis.getInstance().getConfig().getString("settings.locale", "sv_se");
        }
    }

    public static void registerCompletions(PaperCommandManager manager) {
        manager.getCommandCompletions().registerAsyncCompletion("plotType", c -> ImmutableList.of("church", "farm", "shop", "vacation"));
        manager.getCommandCompletions().registerAsyncCompletion("cityRoles", c -> ImmutableList.of("vicemayor", "assistant", "inviter", "swap", "-", "member"));
        manager.getCommandCompletions().registerAsyncCompletion("allCityRoles", c -> ImmutableList.of("vicemayor", "assistant", "inviter", "mayor", "member"));
        manager.getCommandCompletions().registerAsyncCompletion("cityGo1", c -> ImmutableList.of("delete", "set"));
        manager.getCommandCompletions().registerAsyncCompletion("cityGo2", c -> ImmutableList.of("displayname", "accesslevel","name"));
        manager.getCommandCompletions().registerAsyncCompletion("taxLevel", c -> ImmutableList.of("member", "inviter", "assistant", "vicemayor", "mayor", "all", "-"));
        manager.getCommandCompletions().registerAsyncCompletion("leaderboardTypes", c -> ImmutableList.of("place", "break", "mobs"));
        manager.getCommandCompletions().registerAsyncCompletion("leaderboardCConditions", c -> {
            Player player = c.getPlayer();
            if (player == null) {
                return Collections.emptyList();
            }
            Plot plot = playerInPlot.get(player.getUniqueId());
            if (plot == null) {
                return Collections.emptyList();
            }
            Leaderboard leaderboard = plotLeaderboards.get(plot);
            if (leaderboard == null) {
                return Collections.emptyList();
            }
            return Arrays.asList(leaderboard.getConditions());
        });
        manager.getCommandCompletions().registerAsyncCompletion("leaderboardAConditions", c -> {
            Player player = c.getPlayer();
            if (player == null) {
                return Collections.emptyList();
            }
            Plot plot = playerInPlot.get(player.getUniqueId());
            if (plot == null) {
                return Collections.emptyList();
            }
            Leaderboard leaderboard = plotLeaderboards.get(plot);
            if (leaderboard == null) {
                return Collections.emptyList();
            }
            if (leaderboard.getType().equals("place") || leaderboard.getType().equals("break")) {
                List<String> allBlocks = Arrays.stream(Material.values())
                        .filter(Material::isBlock)
                        .map(Material::name)
                        .collect(Collectors.toList());
                List<String> blockFilter = Metropolis.configuration.getLeaderboardBlockFilter();
                allBlocks.removeAll(blockFilter);
                return allBlocks;
            } else if (leaderboard.getType().equals("kill")) {
                List<String> allMobs = Arrays.stream(EntityType.values())
                        .filter(EntityType::isAlive)
                        .map(EntityType::name)
                        .collect(Collectors.toList());
                List<String> mobFilter = Metropolis.configuration.getLeaderboardMobFilter();
                allMobs.removeAll(mobFilter);
                return allMobs;
            }
            return Collections.emptyList();
        });
        manager.getCommandCompletions().registerAsyncCompletion("cityGoes", c -> {
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
        manager.getCommandCompletions().registerAsyncCompletion("cityNames", c -> {
            Player player = c.getPlayer();
            if (player == null) {
                return Collections.emptyList();
            }

            return CityDatabase.getCitynames(player);
        });
        manager.getCommandCompletions().registerAsyncCompletion("playerCities", c -> {
            Player player = c.getPlayer();
            if (player == null) {
                return Collections.emptyList();
            }

            return HCDatabase.getPlayerHomeCities(player.getUniqueId());
        });
        manager.getCommandCompletions().registerAsyncCompletion("playerNames", c -> {
            Player player = c.getPlayer();
            if (player == null) {
                return Collections.emptyList();
            }

            City homeCity = HCDatabase.getHomeCityToCity(player.getUniqueId().toString());
            if (homeCity == null) {
                return Collections.emptyList();
            }

            List<String> memberNames = new ArrayList<>();
            for (Member member : homeCity.getCityMembers()) {
                MPlayer mPlayer = ApiedAPI.getPlayer(UUID.fromString(member.getPlayerUUID()));
                if (mPlayer != null) {
                    memberNames.add(mPlayer.getName());
                }
            }

            return memberNames;
        });
    }

    public void scheduleWeeklyPlotRentCollection() {
        Calendar now = Calendar.getInstance();

        Calendar nextRun = Calendar.getInstance();
        nextRun.set(Calendar.DAY_OF_WEEK, configuration.getRentTimeDay());
        nextRun.set(Calendar.HOUR_OF_DAY, configuration.getRentTimeHour());
        nextRun.set(Calendar.MINUTE, configuration.getRentTimeMinute());
        nextRun.set(Calendar.SECOND, configuration.getRentTimeSecond());

        if (now.after(nextRun)) {
            nextRun.add(Calendar.WEEK_OF_YEAR, 1);
        }

        long initialDelay = nextRun.getTimeInMillis() - now.getTimeInMillis();

        getServer().getScheduler().runTaskTimer(this, () -> {
            getServer().getScheduler().runTask(this, PlotDatabase::collectPlotRents);
        }, initialDelay / 50, 7 * 24 * 60 * 60 * 20); // Convert to ticks (20 ticks = 1 second)
    }

    public void scheduleDailyTaxCollection() {
        Calendar now = Calendar.getInstance();

        Calendar nextRun = Calendar.getInstance();
        nextRun.set(Calendar.HOUR_OF_DAY, configuration.getTaxTimeHour());
        nextRun.set(Calendar.MINUTE, configuration.getTaxTimeMinute());
        nextRun.set(Calendar.SECOND, configuration.getTaxTimeSecond());

        if (now.after(nextRun)) {
            nextRun.add(Calendar.DAY_OF_MONTH, 1);
        }

        long initialDelay = nextRun.getTimeInMillis() - now.getTimeInMillis();

        getServer().getScheduler().runTaskTimer(this, () -> {
            getServer().getScheduler().runTask(this, CityDatabase::collectTaxes);
        }, initialDelay / 50, 24 * 60 * 60 * 20); // Convert to ticks (20 ticks = 1 second)
    }

    public void scheduleUnban(ExpiringBan expiringBan) {
        long banTime = expiringBan.getUnbanTime();
        UUID uuid = expiringBan.getPlayerUUID();
        if (scheduledForUnban.contains(uuid)) {
            return;
        }
        scheduledForUnban.add(uuid);
        getServer().getScheduler().runTaskLater(this, () -> {
            Player player = getServer().getPlayer(uuid);
            if (player != null) {
                MPlayer mPlayer = ApiedAPI.getPlayer(player);
                Cell cell = JailManager.getCellForPrisoner(player.getUniqueId().toString());
                cell.setPrisonerUUID(null);
                JailManager.displaySignEmptyCell(cell);
                player.teleport(mPlayer.getLastLocation());
                Metropolis.sendMessage(player, "messages.cell.expired");
            }
        }, (banTime - DateUtil.getTimestamp()) * 20);
    }
}
