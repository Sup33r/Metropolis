package live.supeer.metropolis.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import live.supeer.metropolis.Metropolis;
import live.supeer.metropolis.city.City;
import live.supeer.metropolis.city.CityDatabase;
import live.supeer.metropolis.city.Role;
import live.supeer.metropolis.homecity.HCDatabase;
import live.supeer.metropolis.utils.BannerUtil;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Objects;

@CommandAlias("homecity|hc")
public class CommandHomeCity extends BaseCommand {

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
                if (CityDatabase.getCityRole(city, player.getUniqueId().toString()) == null) {
                    Metropolis.sendMessage(player,"messages.error.missing.membership");
                    return;
                }
                if (Objects.requireNonNull(CityDatabase.getCityRole(city, player.getUniqueId().toString())).hasPermission(Role.MEMBER)) {
                    if (CityDatabase.getPlayerCityCount(player.getUniqueId().toString()) < 1) {
                        Metropolis.sendMessage(player,"messages.error.missing.homeCity");
                        return;
                    }
                    HCDatabase.setHomeCity(player.getUniqueId().toString(), city);
                    Metropolis.sendMessage(player,"messages.save.membership", "%cityname%", city.getCityName());
                } else {
                    Metropolis.sendMessage(player,"messages.error.missing.membership");
                }
            } else {
                Metropolis.sendMessage(player,"messages.error.missing.city");
            }
        }
    }

    private static void playerGui(Player player) {
        List<City> cityList = CityDatabase.memberCityList(player.getUniqueId().toString());
        if (cityList == null || cityList.isEmpty()) {
            Metropolis.sendMessage(player, "messages.error.missing.membership");
            return;
        }

        int rows = Math.min((cityList.size() + 8) / 9, 6);
        ChestGui gui = new ChestGui(rows, "§8Hemstäder");

        StaticPane pane = new StaticPane(0, 0, 9, rows);

        for (int i = 0; i < cityList.size() && i < 3; i++) {
            City city = cityList.get(i);
            String cityName = city.getCityName();
            for (int j = 0; j < cityName.length() && j < 9; j++) {
                ItemStack banner = BannerUtil.letterBanner(String.valueOf(cityName.charAt(j)), cityName);
                ItemMeta itemMeta = banner.getItemMeta();
                itemMeta.setHideTooltip(true);
                banner.setItemMeta(itemMeta);

                GuiItem guiItem = new GuiItem(banner, event -> {
                    Player clicker = (Player) event.getWhoClicked();
                    HCDatabase.setHomeCity(clicker.getUniqueId().toString(), city);
                    Metropolis.sendMessage(clicker, "messages.save.membership", "%cityname%", cityName);
                    clicker.closeInventory();
                    event.setCancelled(true);
                });

                pane.addItem(guiItem, j, i);
            }
        }

        gui.addPane(pane);
        gui.setOnGlobalClick(event -> event.setCancelled(true));
        gui.show(player);
    }
}
