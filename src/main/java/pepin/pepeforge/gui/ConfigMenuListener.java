package pepin.pepeforge.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import pepin.pepeforge.PepeForgePlugin;
import pepin.pepeforge.gui.itemconfig.anchor.AnchorConfigListener;
import pepin.pepeforge.gui.itemconfig.rapier.RapierConfigListener;
import pepin.pepeforge.gui.itemconfig.rapier.IceRapierConfigListener;
import pepin.pepeforge.gui.itemconfig.crescentkhopesh.CrescentKhopeshConfigListener;
import pepin.pepeforge.gui.itemconfig.emberfang.EmberfangConfigListener;
import pepin.pepeforge.gui.itemconfig.obsidianglaive.ObsidianGlaiveConfigListener;
import pepin.pepeforge.gui.itemconfig.greatsword.GreatswordConfigListener;
import pepin.pepeforge.gui.itemconfig.windblade.WindBladeConfigListener;
import pepin.pepeforge.gui.itemconfig.stormcleaver.StormcleaverConfigListener;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.item.ItemIds;
import pepin.pepeforge.lang.PluginLang;
import pepin.pepeforge.util.scheduler.ScheduledTaskCompat;
import pepin.pepeforge.util.scheduler.SchedulerCompat;

import java.util.Map;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class ConfigMenuListener implements Listener {

    private final PepeForgePlugin plugin;
    private final ItemFactory itemFactory;
    private final PluginLang lang;

    // Listeners for specific item config menus
    private final AnchorConfigListener anchorConfigListener;
    private final RapierConfigListener rapierConfigListener;
    private final IceRapierConfigListener iceRapierConfigListener;
    private final WindBladeConfigListener windBladeConfigListener;
    private final StormcleaverConfigListener stormcleaverConfigListener;
    private final EmberfangConfigListener emberfangConfigListener;
    private final ObsidianGlaiveConfigListener obsidianGlaiveConfigListener;
    private final GreatswordConfigListener greatswordConfigListener;
    private final CrescentKhopeshConfigListener crescentKhopeshConfigListener;


    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();
    private final Object sessionLock = new Object();
    private volatile boolean running = true;

    public ConfigMenuListener(PepeForgePlugin plugin, ItemFactory itemFactory, PluginLang lang) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
        this.lang = lang;
        this.anchorConfigListener = new AnchorConfigListener(plugin);
        this.rapierConfigListener = new RapierConfigListener(plugin);
        this.iceRapierConfigListener = new IceRapierConfigListener(plugin);
        this.windBladeConfigListener = new WindBladeConfigListener(plugin);
        this.stormcleaverConfigListener = new StormcleaverConfigListener(plugin);
        this.emberfangConfigListener = new EmberfangConfigListener(plugin);
        this.obsidianGlaiveConfigListener = new ObsidianGlaiveConfigListener(plugin);
        this.greatswordConfigListener = new GreatswordConfigListener(plugin);
        this.crescentKhopeshConfigListener = new CrescentKhopeshConfigListener(plugin);
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        Inventory inventory = event.getView().getTopInventory();
        if (!ConfigMenu.isConfigMenu(inventory) && !ItemConfigMenu.isItemConfigMenu(inventory)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)
                || sessionFor(player, inventory) == null) {
            return;
        }
        if (!canConfigure(player)) {
            player.sendMessage(lang.message("messages.command.no_permission"));
            return;
        }
        if (ConfigMenu.isConfigMenu(event.getView().getTopInventory())) {
            handleConfigMenuClick(event);
        } else if (ItemConfigMenu.isItemConfigMenu(event.getView().getTopInventory())) {
            handleItemConfigMenuClick(event);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (ConfigMenu.isConfigMenu(event.getView().getTopInventory())
                || ItemConfigMenu.isItemConfigMenu(event.getView().getTopInventory())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        Session session = sessionFor(player, event.getInventory());
        if (session != null && !session.transitioning) {
            finish(session, true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Session session = sessions.get(event.getPlayer().getUniqueId());
        if (session != null && session.player == event.getPlayer()) {
            finish(session, false);
        }
    }

    public void open(Player player) {
        if (!running || !player.isOnline()) {
            return;
        }
        if (!canConfigure(player)) {
            player.sendMessage(lang.message("messages.command.no_permission"));
            return;
        }
        Session session;
        synchronized (sessionLock) {
            if (!running) {
                return;
            }
            UUID playerId = player.getUniqueId();
            session = sessionFor(player, player.getOpenInventory().getTopInventory());
            if (session == null) {
                session = new Session(player, playerId);
                Session previous = sessions.put(playerId, session);
                if (previous != null) {
                    previous.cancelPending();
                }
            } else {
                session.cancelPending();
            }
        }
        show(session, ConfigMenu.create(itemFactory, plugin, lang));
    }

    public void stop() {
        List<Session> closing;
        synchronized (sessionLock) {
            running = false;
            closing = List.copyOf(sessions.values());
            sessions.clear();
        }
        for (Session session : closing) {
            session.cancelPending();
            Inventory expectedInventory = session.inventory;
            Runnable close = () -> {
                if (session.player.isOnline()
                        && session.player.getOpenInventory().getTopInventory() == expectedInventory) {
                    session.player.closeInventory();
                }
            };
            if (SchedulerCompat.isOwnedByCurrentRegion(session.player)) {
                close.run();
            } else if (plugin.isEnabled()) {
                SchedulerCompat.runLaterForPlayer(session.player, plugin, close, 1L);
            }
        }
    }

    private boolean canConfigure(Player player) {
        return player.hasPermission("pepeforge.config") || player.isOp();
    }

    private Session sessionFor(Player player, Inventory inventory) {
        Session session = sessions.get(player.getUniqueId());
        return running && session != null && session.player == player && session.inventory == inventory
                ? session : null;
    }

    private void show(Session session, Inventory inventory) {
        if (!running || sessions.get(session.playerId) != session) {
            return;
        }
        session.transitioning = true;
        try {
            session.player.openInventory(inventory);
            if (!running || sessions.get(session.playerId) != session) {
                if (session.player.isOnline()
                        && session.player.getOpenInventory().getTopInventory() == inventory) {
                    session.player.closeInventory();
                }
                return;
            }
            if (!session.player.isOnline()) {
                finish(session, false);
            } else {
                Inventory actual = session.player.getOpenInventory().getTopInventory();
                if (actual == inventory) {
                    session.inventory = inventory;
                } else if (actual != session.inventory) {
                    finish(session, true);
                }
            }
        } finally {
            session.transitioning = false;
        }
    }

    private void finish(Session session, boolean notify) {
        if (sessions.remove(session.playerId, session)) {
            session.cancelPending();
            if (notify && session.dirty && session.player.isOnline()) {
                session.player.sendMessage(lang.message("messages.config.reload_reminder"));
            }
        }
    }

    private void markDirty(Player player) {
        Session session = sessions.get(player.getUniqueId());
        if (running && session != null && session.player == player) {
            session.dirty = true;
        }
    }

    private void defer(Player player, Inventory expectedInventory, Consumer<Session> action) {
        Session session = sessionFor(player, expectedInventory);
        if (session == null) {
            return;
        }
        Navigation navigation = new Navigation(session, expectedInventory, action);
        synchronized (sessionLock) {
            if (!running || sessions.get(session.playerId) != session) {
                return;
            }
            session.cancelPending();
            session.pending = navigation;
        }
        try {
            navigation.attach(SchedulerCompat.runLaterForPlayer(
                    player, plugin, navigation, navigation::retire, 1L));
        } catch (RuntimeException | Error failure) {
            navigation.cancel();
            if (session.pending == navigation) {
                session.pending = null;
            }
            throw failure;
        }
    }

    private void handleConfigMenuClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) {
            return;
        }

        if (event.getSlot() == ConfigMenu.METRICS_SLOT) {
            if (!player.hasPermission("pepeforge.config") && !player.isOp()) {
                player.sendMessage(lang.message("messages.command.no_permission"));
                return;
            }
            plugin.setMetricsEnabled(!plugin.getConfig().getBoolean("metrics.enabled", true));
            ConfigMenu.updateMetricsButton(event.getView().getTopInventory(), plugin, lang);
            return;
        }

        if (event.getSlot() == 53) {
            if (!player.hasPermission("pepeforge.reload") && !player.isOp()) {
                player.sendMessage(lang.message("messages.command.no_permission"));
            } else {
                defer(player, event.getView().getTopInventory(), session -> {
                    if (!player.hasPermission("pepeforge.reload") && !player.isOp()) {
                        player.sendMessage(lang.message("messages.command.no_permission"));
                        return;
                    }
                    if (plugin.reloadPlugin()) {
                        player.sendMessage(lang.message("messages.config.reloaded"));
                    } else {
                        player.sendMessage(lang.message("messages.config.reload_in_progress"));
                    }
                });
            }
            return;
        }

        String itemId = itemFactory.getItemId(clicked);
        if (itemId != null) {
            defer(player, event.getView().getTopInventory(), session ->
                    show(session, ItemConfigMenu.create(itemId, itemFactory, plugin)));
        }
    }

    private void handleItemConfigMenuClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }

        String itemId = ItemConfigMenu.getItemId(event.getView().getTopInventory());
        if (itemId == null) {
            return;
        }

        String configPath = "items." + itemId;
        boolean refresh = false;

        if (event.getSlot() == 11) {
            boolean current = plugin.getConfig().getBoolean(configPath + ".enabled", true);
            plugin.getConfig().set(configPath + ".enabled", !current);
            plugin.saveConfig();
            refresh = true;
        } else if (event.getSlot() == 15 && ItemConfigMenu.hasRecipeToggle(itemId)) {
            boolean current = plugin.getConfig().contains(configPath + ".recipe_enabled")
                    ? plugin.getConfig().getBoolean(configPath + ".recipe_enabled")
                    : true;
            plugin.getConfig().set(configPath + ".recipe_enabled", !current);
            plugin.saveConfig();
            refresh = true;
        } else if ("anchor".equals(itemId)) {
            if (anchorConfigListener.handleClick(event)) {
                refresh = true;
            }
        } else if (ItemConfigMenu.isWindBlade(itemId)) {
            if (windBladeConfigListener.handleClick(event)) {
                refresh = true;
            }
        } else if (ItemIds.ICE_RAPIER.equals(itemId)) {
            if (iceRapierConfigListener.handleClick(event)) {
                refresh = true;
            }
        } else if (ItemConfigMenu.isRapier(itemId)) {
            if (rapierConfigListener.handleClick(event)) {
                refresh = true;
            }
        } else if (ItemConfigMenu.isGreatsword(itemId)) {
            if (greatswordConfigListener.handleClick(event)) {
                refresh = true;
            }
        } else if ("stormcleaver".equals(itemId)) {
            if (stormcleaverConfigListener.handleClick(event)) {
                refresh = true;
            }
        } else if ("emberfang".equals(itemId)) {
            if (emberfangConfigListener.handleClick(event)) {
                refresh = true;
            }
        } else if (ItemIds.OBSIDIAN_GLAIVE.equals(itemId)) {
            if (obsidianGlaiveConfigListener.handleClick(event)) {
                refresh = true;
            }
        } else if (ItemIds.CRESCENT_KHOPESH.equals(itemId)) {
            if (crescentKhopeshConfigListener.handleClick(event)) {
                refresh = true;
            }
        }

        if (event.getSlot() == 26) {
            defer(player, event.getView().getTopInventory(), session ->
                    show(session, ConfigMenu.create(itemFactory, plugin, lang)));
        }

        if (refresh) {
            markDirty(player);
            ItemConfigMenu.refresh(event.getView().getTopInventory(), itemFactory, plugin);
        }
    }

    private final class Navigation implements Runnable {
        private final Session session;
        private final Inventory expectedInventory;
        private final Consumer<Session> action;
        private volatile boolean cancelled;
        private ScheduledTaskCompat task;

        private Navigation(Session session, Inventory expectedInventory, Consumer<Session> action) {
            this.session = session;
            this.expectedInventory = expectedInventory;
            this.action = action;
        }

        @Override
        public void run() {
            if (cancelled || !running || sessions.get(session.playerId) != session || session.pending != this) {
                return;
            }
            session.pending = null;
            if (!session.player.isOnline()) {
                finish(session, false);
                return;
            }
            if (session.player.getOpenInventory().getTopInventory() != expectedInventory) {
                finish(session, true);
                return;
            }
            if (!canConfigure(session.player)) {
                session.player.sendMessage(lang.message("messages.command.no_permission"));
                return;
            }
            if (running && sessions.get(session.playerId) == session) {
                action.accept(session);
            }
        }

        private void retire() {
            synchronized (sessionLock) {
                if (session.pending == this) {
                    sessions.remove(session.playerId, session);
                    session.pending = null;
                }
            }
            cancel();
        }

        private synchronized void attach(ScheduledTaskCompat scheduledTask) {
            if (cancelled) {
                scheduledTask.cancel();
            } else {
                task = scheduledTask;
            }
        }

        private synchronized void cancel() {
            cancelled = true;
            if (task != null) {
                task.cancel();
                task = null;
            }
        }
    }

    private final class Session {
        private final Player player;
        private final UUID playerId;
        private volatile Inventory inventory;
        private volatile Navigation pending;
        private boolean dirty;
        private boolean transitioning;

        private Session(Player player, UUID playerId) {
            this.player = player;
            this.playerId = playerId;
        }

        private void cancelPending() {
            Navigation navigation = pending;
            pending = null;
            if (navigation != null) {
                navigation.cancel();
            }
        }
    }

}
