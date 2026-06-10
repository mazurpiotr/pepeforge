package pepin.pepeforge.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pepin.pepeforge.gui.CustomItemsMenu;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.lang.PluginLang;
import pepin.pepeforge.weapons.crimsonsword.CrimsonSwordDefinition;
import pepin.pepeforge.weapons.crimsonsword.CrimsonSwordManager;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class PepeForgeCommand implements CommandExecutor, TabCompleter {

    private final PluginLang lang;
    private final ItemFactory itemFactory;
    private final CrimsonSwordManager crimsonSwordManager;
    private final pepin.pepeforge.stats.StatisticsManager statsManager;
    private final pepin.pepeforge.item.ItemMigrator itemMigrator;

    public PepeForgeCommand(PluginLang lang, ItemFactory itemFactory, CrimsonSwordManager crimsonSwordManager, pepin.pepeforge.stats.StatisticsManager statsManager, pepin.pepeforge.item.ItemMigrator itemMigrator) {
        this.lang = lang;
        this.itemFactory = itemFactory;
        this.crimsonSwordManager = crimsonSwordManager;
        this.statsManager = statsManager;
        this.itemMigrator = itemMigrator;
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
                sender.sendMessage(lang.message("messages.command.setlevel_success", Map.of("level", String.valueOf(level))));
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
                sender.sendMessage(net.kyori.adventure.text.Component.text("Usage: /pepeforge migration <on|pause|disable>").color(net.kyori.adventure.text.format.NamedTextColor.RED));
                return true;
            }
            String action = args[1].toLowerCase(java.util.Locale.ROOT);
            org.bukkit.plugin.Plugin plugin = Bukkit.getPluginManager().getPlugin("PepeForge");
            if (plugin != null) {
                if ("on".equals(action)) {
                    itemMigrator.setActive(true);
                    plugin.getConfig().set("migration.enabled", true);
                    plugin.saveConfig();
                    sender.sendMessage(net.kyori.adventure.text.Component.text("Lazy Item Migration enabled completely.").color(net.kyori.adventure.text.format.NamedTextColor.GREEN));
                } else if ("pause".equals(action)) {
                    itemMigrator.setActive(false);
                    sender.sendMessage(net.kyori.adventure.text.Component.text("Lazy Item Migration paused until restart.").color(net.kyori.adventure.text.format.NamedTextColor.YELLOW));
                } else if ("disable".equals(action) || "off".equals(action)) {
                    itemMigrator.setActive(false);
                    plugin.getConfig().set("migration.enabled", false);
                    plugin.saveConfig();
                    sender.sendMessage(net.kyori.adventure.text.Component.text("Lazy Item Migration disabled completely.").color(net.kyori.adventure.text.format.NamedTextColor.RED));
                } else {
                    sender.sendMessage(net.kyori.adventure.text.Component.text("Invalid state. Use on, pause, or disable.").color(net.kyori.adventure.text.format.NamedTextColor.RED));
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

        target.getInventory().addItem(item);
        statsManager.incrementGiven(itemFactory.getItemId(item));
        String itemName = itemFactory.getBestName(item);
        sender.sendMessage(lang.message("messages.command.give_success_sender", Map.of(
                "item", itemName,
                "player", target.getName()
        )));
        target.sendMessage(lang.message("messages.command.give_success_target", Map.of("item", itemName)));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("give", "items", "setlevel", "migration").stream()
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
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
