package pepin.pepeforge.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import pepin.pepeforge.gui.CustomItemsMenu;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.lang.PluginLang;
import pepin.pepeforge.util.ColorUtil;
import pepin.pepeforge.util.scheduler.SchedulerCompat;
import pepin.pepeforge.weapons.crimsonsword.CrimsonSwordDefinition;
import pepin.pepeforge.weapons.crimsonsword.CrimsonSwordManager;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class PepeForgeCommand implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final PluginLang lang;
    private final ItemFactory itemFactory;
    private final CrimsonSwordManager crimsonSwordManager;
    private final pepin.pepeforge.stats.StatisticsManager statsManager;
    private final pepin.pepeforge.item.ItemMigrator itemMigrator;
    private final OnlinePlayerNames onlinePlayerNames;

    public PepeForgeCommand(JavaPlugin plugin, PluginLang lang, ItemFactory itemFactory,
            CrimsonSwordManager crimsonSwordManager,
            pepin.pepeforge.stats.StatisticsManager statsManager, pepin.pepeforge.item.ItemMigrator itemMigrator) {
        this.plugin = plugin;
        this.lang = lang;
        this.itemFactory = itemFactory;
        this.crimsonSwordManager = crimsonSwordManager;
        this.statsManager = statsManager;
        this.itemMigrator = itemMigrator;
        this.onlinePlayerNames = new OnlinePlayerNames();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(lang.message("messages.command.usage"));
            return true;
        }

        if ("items".equalsIgnoreCase(args[0])) {
            if (!sender.hasPermission("pepeforge.items")) {
                sender.sendMessage(lang.message("messages.command.no_permission"));
                return true;
            }
            if (!(sender instanceof Player player)) {
                sender.sendMessage(lang.message("messages.command.players_only"));
                return true;
            }
            player.openInventory(CustomItemsMenu.create(lang, itemFactory));
            return true;
        }

        if ("config".equalsIgnoreCase(args[0])) {
            if (!sender.hasPermission("pepeforge.config") && !sender.isOp()) {
                sender.sendMessage(lang.message("messages.command.no_permission"));
                return true;
            }
            if (!(sender instanceof Player player)) {
                sender.sendMessage(lang.message("messages.command.players_only"));
                return true;
            }
            player.openInventory(pepin.pepeforge.gui.ConfigMenu.create(itemFactory));
            return true;
        }

        if ("reload".equalsIgnoreCase(args[0])) {
            if (!sender.hasPermission("pepeforge.reload") && !sender.isOp()) {
                sender.sendMessage(lang.message("messages.command.no_permission"));
                return true;
            }
            org.bukkit.plugin.Plugin plugin = Bukkit.getPluginManager().getPlugin("PepeForge");
            if (plugin instanceof pepin.pepeforge.PepeForgePlugin pepePlugin) {
                pepePlugin.reloadPlugin();
                sender.sendMessage(lang.message("messages.config.reloaded"));
            }
            return true;
        }

        if ("setlevel".equalsIgnoreCase(args[0])) {
            if (!sender.hasPermission("pepeforge.setlevel") && !sender.isOp()) {
                sender.sendMessage(lang.message("messages.command.no_permission"));
                return true;
            }
            if (!(sender instanceof Player player)) {
                sender.sendMessage(lang.message("messages.command.players_only"));
                return true;
            }
            if (args.length != 2) {
                sender.sendMessage(lang.message("messages.command.usage"));
                return true;
            }
            ItemStack item = player.getInventory().getItemInMainHand();
            if (!CrimsonSwordDefinition.ITEM_ID.equals(itemFactory.getItemId(item))) {
                sender.sendMessage(lang.message("messages.command.crimson_sword_only"));
                return true;
            }
            try {
                int level = Integer.parseInt(args[1]);
                crimsonSwordManager.setLevel(item, level);
                sender.sendMessage(
                        lang.message("messages.command.setlevel_success", Map.of("level", String.valueOf(level))));
            } catch (NumberFormatException e) {
                sender.sendMessage(lang.message("messages.command.invalid_level"));
            }
            return true;
        }

        if ("migration".equalsIgnoreCase(args[0])) {
            if (!sender.hasPermission("pepeforge.migration") && !sender.isOp()) {
                sender.sendMessage(lang.message("messages.command.no_permission"));
                return true;
            }
            if (args.length != 2) {
                sender.sendMessage(ColorUtil.RED + "Usage: /pepeforge migration <on|pause|disable>");
                return true;
            }
            String action = args[1].toLowerCase(java.util.Locale.ROOT);
            org.bukkit.plugin.Plugin plugin = Bukkit.getPluginManager().getPlugin("PepeForge");
            if (plugin != null) {
                if ("on".equals(action)) {
                    itemMigrator.setActive(true);
                    plugin.getConfig().set("migration.enabled", true);
                    plugin.saveConfig();
                    sender.sendMessage(ColorUtil.GREEN + "Lazy Item Migration enabled completely.");
                } else if ("pause".equals(action)) {
                    itemMigrator.setActive(false);
                    sender.sendMessage(ColorUtil.YELLOW + "Lazy Item Migration paused until restart.");
                } else if ("disable".equals(action) || "off".equals(action)) {
                    itemMigrator.setActive(false);
                    plugin.getConfig().set("migration.enabled", false);
                    plugin.saveConfig();
                    sender.sendMessage(ColorUtil.RED + "Lazy Item Migration disabled completely.");
                } else {
                    sender.sendMessage(ColorUtil.RED + "Invalid state. Use on, pause, or disable.");
                }
            }
            return true;
        }

        if (!"give".equalsIgnoreCase(args[0]) || args.length != 3) {
            sender.sendMessage(lang.message("messages.command.unknown_subcommand"));
            sender.sendMessage(lang.message("messages.command.usage"));
            return true;
        }

        if (!sender.hasPermission("pepeforge.give")) {
            sender.sendMessage(lang.message("messages.command.no_permission"));
            return true;
        }

        if (!itemFactory.isKnownItemName(args[1])) {
            sender.sendMessage(lang.message("messages.command.unknown_item", Map.of("item", args[1])));
            return true;
        }
        if (!itemFactory.isItemEnabledByName(args[1])) {
            sender.sendMessage(lang.message("messages.command.item_disabled", Map.of("item", args[1])));
            return true;
        }

        ItemStack item = itemFactory.createByName(args[1]);

        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            sender.sendMessage(lang.message("messages.command.player_not_found", Map.of("player", args[2])));
            return true;
        }

        String itemName = itemFactory.getBestName(item);
        String itemId = itemFactory.getItemId(item);
        String targetName = args[2];
        SchedulerCompat.runForPlayer(target, plugin, () -> {
            if (!target.isOnline()) {
                sendGivePlayerUnavailable(sender, targetName);
                return;
            }

            if (!hasRoomFor(target, item)) {
                sendGiveFailure(sender, itemName, targetName);
                return;
            }

            Map<Integer, ItemStack> remaining = target.getInventory().addItem(item.clone());
            if (!remaining.isEmpty()) {
                sendGiveFailure(sender, itemName, targetName);
                return;
            }

            sendGiveSuccess(sender, target, itemName, targetName);
            statsManager.incrementGiven(itemId);
        }, () -> sendGivePlayerUnavailable(sender, targetName));
        return true;
    }

    private boolean hasRoomFor(Player player, ItemStack item) {
        int remaining = item.getAmount();
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (stack == null || stack.getType().isAir()) {
                remaining -= item.getMaxStackSize();
            } else if (stack.isSimilar(item)) {
                remaining -= Math.max(0, stack.getMaxStackSize() - stack.getAmount());
            }

            if (remaining <= 0) {
                return true;
            }
        }
        return false;
    }

    private void sendGiveSuccess(CommandSender sender, Player target, String itemName, String targetName) {
        Runnable notifySender = () -> sender.sendMessage(lang.message(
                "messages.command.give_success_sender",
                Map.of("item", itemName, "player", targetName)));
        if (sender instanceof Player senderPlayer && senderPlayer != target) {
            SchedulerCompat.runForPlayer(senderPlayer, plugin, notifySender);
        } else {
            notifySender.run();
        }
        target.sendMessage(lang.message("messages.command.give_success_target", Map.of("item", itemName)));
    }

    private void sendGiveFailure(CommandSender sender, String itemName, String targetName) {
        Runnable notify = () -> sender.sendMessage(lang.message(
                "messages.command.give_inventory_full",
                Map.of("item", itemName, "player", targetName)));
        if (sender instanceof Player senderPlayer) {
            SchedulerCompat.runForPlayer(senderPlayer, plugin, notify);
        } else {
            notify.run();
        }
    }

    private void sendGivePlayerUnavailable(CommandSender sender, String targetName) {
        Runnable notify = () -> sender.sendMessage(lang.message(
                "messages.command.player_not_found",
                Map.of("player", targetName)));
        if (sender instanceof Player senderPlayer) {
            SchedulerCompat.runForPlayer(senderPlayer, plugin, notify);
        } else {
            notify.run();
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("give", "items", "setlevel", "migration", "config", "reload").stream()
                    .filter(option -> option.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && "migration".equalsIgnoreCase(args[0])) {
            return List.of("on", "pause", "disable").stream()
                    .filter(option -> option.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && "give".equalsIgnoreCase(args[0])) {
            return itemFactory.knownGiveNames().stream()
                    .filter(name -> name.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && "setlevel".equalsIgnoreCase(args[0])) {
            return List.of("1", "10", "20", "30").stream()
                    .filter(option -> option.startsWith(args[1]))
                    .collect(Collectors.toList());
        }
        if (args.length == 3 && "give".equalsIgnoreCase(args[0])) {
            return onlinePlayerNames.matching(args[2]);
        }
        return Collections.emptyList();
    }
}
