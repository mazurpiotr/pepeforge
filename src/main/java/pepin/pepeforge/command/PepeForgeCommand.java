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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class PepeForgeCommand implements CommandExecutor, TabCompleter {

    private final PluginLang lang;
    private final ItemFactory itemFactory;

    public PepeForgeCommand(PluginLang lang, ItemFactory itemFactory) {
        this.lang = lang;
        this.itemFactory = itemFactory;
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
            return List.of("give", "items").stream()
                    .filter(option -> option.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && "give".equalsIgnoreCase(args[0])) {
            return itemFactory.knownGiveNames().stream()
                    .filter(name -> name.startsWith(args[1].toLowerCase()))
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
