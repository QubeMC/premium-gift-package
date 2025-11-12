package com.qubemc.premium_gift;

import de.themoep.inventorygui.StaticGuiElement;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;

import de.themoep.inventorygui.InventoryGui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class Main extends JavaPlugin implements Listener {

    // Create an array to store all the Inventory instance (gift packages)
    ArrayList<ItemStack[]> giftPacks = new ArrayList<ItemStack[]>();
    // Arraylist to store exp date and perm group
    // [exp, permGroup]
    ArrayList<String[]> giftPacksData = new ArrayList<String[]>();
    Character[] characters = {'a','b','c','d','e','f','g','h','i','j','k','l','m','n','o'};
    // Set up the buttons on the gui
    // Max package amount allowed: 15
    String[] guiSetup = {
            "  abcde  ",
            "  fghij  ",
            "  klmno  "
    };

    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().info("Premium Gift Enabled");
        this.getConfig().options().copyDefaults(true);
        this.saveDefaultConfig();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("Premium Gift Disabled");
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof Player) {
            switch(cmd.getName()) {
                case "packgift":
                    // make sure all args of command are provided
                    if (args.length < 2) {
                        return false;
                    }

                    // Add a maximum package check to prevent error
                    if (this.giftPacks.size() > 14) {
                        sender.sendMessage(ChatColor.RED + this.getConfig().getString("exceedMaxPackCount"));
                        return false;
                    }

                    String[] permissions = {"level1","level2","level3","level4","level5"};
                    List<String> lst = Arrays.asList(permissions);

                    // Make sure only numbers are provided
                    if (!StringUtils.isNumeric(args[0])) {
                        return false;
                    }

                    // Make sure only the five permission levels are provided
                    if (!lst.contains(args[1])) {
                        return false;
                    }

                    sender.sendMessage(ChatColor.GREEN + this.getConfig().getString("inventoryUploaded"));
                    Inventory inv = ((Player) sender).getInventory();
                    ItemStack[] invContent = inv.getContents();
                    for (ItemStack a: invContent) {
                        if (a != null) {
                            sender.sendMessage(ChatColor.GREEN + a.getI18NDisplayName() + ":" + a.getAmount());
                        }
                    }

                    // Create a copy of the array to prevent the inventoryClear issue
                    ItemStack[] newStack = invContent.clone();
                    this.giftPacks.add(newStack);

                    // process expiration date and permission group
                    // get current unix time in hours
                    Long unixTime = System.currentTimeMillis() / 1000L / 3600L;
                    String newTime = Long.toString(unixTime + Integer.parseInt(args[0]));
                    String[] data = new String[2];
                    data[0] = newTime;
                    data[1] = args[1];
                    this.giftPacksData.add(data);
                    return true;
                case "gifts":
                    sender.sendMessage(ChatColor.GREEN + this.getConfig().getString("giftMenuOpen"));
                    InventoryGui gui = new InventoryGui(this, (InventoryHolder) sender, "GIFTS", guiSetup);
                    gui.setFiller(new ItemStack(Material.GRAY_STAINED_GLASS, 1)); // fill the empty slots with this
                    for (int i = 0; i < this.giftPacks.size(); i++) {
                        int finalI = i;
                        Long currentTimeInH = System.currentTimeMillis() / 1000L / 3600L;
                        // Check expiration
                        if (Integer.parseInt(this.giftPacksData.get(i)[0]) < currentTimeInH) {
                            continue;
                        }

                        int expirationTime = Integer.parseInt(this.giftPacksData.get(i)[0]);

                        gui.addElement(new StaticGuiElement(this.characters[i],
                                new ItemStack(Material.DIAMOND_BLOCK),
                                1, // Display a number as the item count
                                click -> {
                                    Player player = (Player) click.getEvent().getWhoClicked();
                                    if (!player.hasPermission("premiumgift." + this.giftPacksData.get(finalI)[1])) {
                                        player.sendMessage(ChatColor.RED + this.getConfig().getString("missingPermission"));
                                        return true;
                                    }

                                    if (player.equals(sender)) {
                                        player.sendMessage(ChatColor.GREEN + this.getConfig().getString("packageGiven"));
                                        ItemStack[] tempContents = this.giftPacks.get(finalI);
                                        for (ItemStack a: tempContents) {
                                            if (a != null) {
                                                player.sendMessage(ChatColor.GREEN + "[DEBUG]" + a.getI18NDisplayName() + ":" + a.getAmount());
                                                player.getInventory().addItem(a);
                                            }
                                        }
                                        return true;
                                    }
                                    return true;
                                },
                                this.getConfig().getString("packageName") + " " + i,
                                this.getConfig().getString("expiration") + " " + (expirationTime - currentTimeInH) +
                                        this.getConfig().getString("hours")
                        ));
                    }
                    gui.show((Player) sender);
                    return true;
            }
            return false;
        }
        return false;
    }
}
