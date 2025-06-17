package com.donut.sellgui;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class DonutSellGUI extends JavaPlugin implements Listener {

    private Economy econ;
    private FileConfiguration config;

    @Override
    public void onEnable() {
        if (!setupEconomy()) {
            getLogger().severe("Vault with an economy plugin is required!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();
        config = getConfig();
        getCommand("sell").setExecutor((sender, command, label, args) -> {
            if (!(sender instanceof Player)) return true;
            openSellGUI((Player) sender);
            return true;
        });

        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("DonutSellGUI enabled!");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        econ = rsp.getProvider();
        return econ != null;
    }

    private void openSellGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, "Sell Menu");

        // Add emerald sell button
        ItemStack sellButton = new ItemStack(Material.EMERALD);
        ItemMeta meta = sellButton.getItemMeta();
        meta.setDisplayName("§aClick to Sell All");
        sellButton.setItemMeta(meta);
        inv.setItem(53, sellButton);

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        if (!event.getView().getTitle().equals("Sell Menu")) return;

        if (event.getRawSlot() == 53) {
            // Clicked the emerald sell button
            Inventory inv = event.getInventory();
            double total = 0;

            for (int i = 0; i < 53; i++) {
                ItemStack item = inv.getItem(i);
                if (item == null || item.getType() == Material.AIR) continue;
                Material mat = item.getType();
                int amount = item.getAmount();
                if (config.contains("prices." + mat.name())) {
                    double price = config.getDouble("prices." + mat.name());
                    total += price * amount;
                }
                inv.setItem(i, null);
            }

            if (total > 0) {
                econ.depositPlayer(player, total);
                player.sendMessage("§aYou sold items for $" + total + "!");
            } else {
                player.sendMessage("§cNo sellable items found.");
            }
            event.setCancelled(true);
        }
    }
}
