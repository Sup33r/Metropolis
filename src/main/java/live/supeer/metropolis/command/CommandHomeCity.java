package live.supeer.metropolis.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import live.supeer.metropolis.Metropolis;
import live.supeer.metropolis.city.City;
import live.supeer.metropolis.city.CityDatabase;
import live.supeer.metropolis.city.Role;
import live.supeer.metropolis.homecity.HCDatabase;
import live.supeer.metropolis.utils.BannerUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Objects;

@CommandAlias("homecity|hc")
public class CommandHomeCity extends BaseCommand implements Listener {
    @Default
    public static void onHomeCity(Player player, @Optional String cityname) {
        if (!player.hasPermission("metropolis.homecity")) {
            Metropolis.sendMessage(player,"messages.error.permissionDenied");
            return;
        }
        if (cityname == null) {
            playerGui(player);
        } else {
            if (CityDatabase.getCity(cityname).isPresent()) {
                City city = CityDatabase.getCity(cityname).get();
                if (CityDatabase.getCityRole(city,player.getUniqueId().toString()) == null) {
                    Metropolis.sendMessage(player,"messages.error.missing.membership");
                    return;
                }
                if (Objects.requireNonNull(CityDatabase.getCityRole(city, player.getUniqueId().toString())).hasPermission(Role.MEMBER)) {
                    if (CityDatabase.getPlayerCityCount(player.getUniqueId().toString()) < 1) {
                        Metropolis.sendMessage(player,"messages.error.missing.homeCity");
                        return;
                    }
                    HCDatabase.setHomeCity(player.getUniqueId().toString(),city);
                    Metropolis.sendMessage(player,"messages.save.membership","%cityname%",city.getCityName());
                } else {
                    Metropolis.sendMessage(player,"messages.error.missing.membership");
                }

            } else {
                Metropolis.sendMessage(player,"messages.error.missing.city");
            }
        }
    }
    @EventHandler
    public void OnInventory(final InventoryClickEvent e) {
        if (e.getView().getTitle().equals("§8Homecity")) {

            e.setCancelled(true);
            if (e.getCurrentItem() == null) {
                return;
            }
            if (e.getCurrentItem().getItemMeta() == null) {
                return;
            }
            if (!e.getCurrentItem().getType().equals(Material.WHITE_BANNER)) {
                return;
            }
            if (!e.getCurrentItem().getItemMeta().getDisplayName().startsWith("§")) {
                return;
            }

            String cityname = e.getCurrentItem().getItemMeta().getDisplayName().substring(4);
            if (CityDatabase.getCity(cityname).isEmpty()) {
                return;
            }
            City city = CityDatabase.getCity(cityname).get();
            Player player = (Player) e.getWhoClicked();

            HCDatabase.setHomeCity(player.getUniqueId().toString(),city);
            Metropolis.sendMessage(player,"messages.save.membership","%cityname%",cityname);
            player.closeInventory();
        }
    }

    private static void playerGui(Player player) {
        List<City> cityList = CityDatabase.memberCityList(player.getUniqueId().toString());
        if (cityList == null || cityList.isEmpty()) {
            Metropolis.sendMessage(player, "messages.error.missing.membership");
            return;
        }

        int inventorySize = Math.min(cityList.size(), 3) * 9;
        Inventory gui = Bukkit.createInventory(player, inventorySize, "§8Homecity");

        for (int i = 0; i < cityList.size() && i < 3; i++) {
            City city = cityList.get(i);
            String cityName = city.getCityName();
            for (int j = 0; j < cityName.length() && j < 9; j++) {
                ItemStack item = BannerUtil.letterBanner(String.valueOf(cityName.charAt(j)), cityName);
                gui.setItem(i * 9 + j, item);
            }
        }

        player.openInventory(gui);
    }
}
