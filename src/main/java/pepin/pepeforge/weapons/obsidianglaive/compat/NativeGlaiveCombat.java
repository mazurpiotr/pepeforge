package pepin.pepeforge.weapons.obsidianglaive.compat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.plugin.java.JavaPlugin;
import pepin.pepeforge.util.scheduler.SchedulerCompat;

final class NativeGlaiveCombat {
    private final Method getHandle;
    private final Method getWeapon;
    private final Method copyWeapon;
    private final Method createSource;
    private final Method getItem;
    private final Method itemBonus;
    private final Method getStrength;
    private final Method canSweep;
    private final Method canCriticalAttack;
    private final Method getLevel;
    private final Method modifyDamage;
    private final Method knownCause;
    private final Constructor<?> bukkitSource;
    private final Method postAttackEffects;
    private final Method criticalSource;
    private final Method paperConfig;
    private final Field entitiesConfig;
    private final Field behaviorConfig;
    private final Field disablePlayerCrits;

    private NativeGlaiveCombat(JavaPlugin plugin) throws ReflectiveOperationException {
        Class<?> entity = Class.forName("net.minecraft.world.entity.Entity");
        Class<?> player = Class.forName("net.minecraft.world.entity.player.Player");
        Class<?> stack = Class.forName("net.minecraft.world.item.ItemStack");
        Class<?> source = Class.forName("net.minecraft.world.damagesource.DamageSource");
        Class<?> level = Class.forName("net.minecraft.server.level.ServerLevel");
        Class<?> enchantments = Class.forName("net.minecraft.world.item.enchantment.EnchantmentHelper");
        Class<?> craftEntity = Class.forName(org.bukkit.Bukkit.getServer().getClass().getPackageName()
                + ".entity.CraftEntity");
        getHandle = craftEntity.getMethod("getHandle");
        getWeapon = player.getMethod("getWeaponItem");
        copyWeapon = stack.getMethod("copy");
        createSource = player.getDeclaredMethod("createAttackSource", stack);
        createSource.setAccessible(true);
        getItem = stack.getMethod("getItem");
        itemBonus = getItem.getReturnType().getMethod("getAttackDamageBonus", entity, float.class, source);
        getStrength = player.getMethod("getAttackStrengthScale", float.class);
        canSweep = player.getDeclaredMethod("isSweepAttack", boolean.class, boolean.class, boolean.class);
        canSweep.setAccessible(true);
        canCriticalAttack = player.getDeclaredMethod("canCriticalAttack", entity);
        canCriticalAttack.setAccessible(true);
        getLevel = entity.getMethod("level");
        modifyDamage = enchantments.getMethod("modifyDamage", level, stack, entity, source, float.class);
        Method config = null;
        Field entities = null;
        Field behavior = null;
        Field disableCrits = null;
        try {
            config = getLevel.getReturnType().getMethod("paperConfig");
        } catch (NoSuchMethodException ignored) {
            // Bukkit-only implementations have no Paper critical-hit toggle.
        }
        if (config != null) {
            entities = config.getReturnType().getField("entities");
            behavior = entities.getType().getField("behavior");
            disableCrits = behavior.getType().getField("disablePlayerCrits");
        }
        paperConfig = config;
        entitiesConfig = entities;
        behaviorConfig = behavior;
        disablePlayerCrits = disableCrits;
        Method critical = null;
        try {
            critical = source.getMethod("critical");
        } catch (NoSuchMethodException exception) {
            plugin.getLogger().warning("Obsidian Glaive cannot expose critical-hit damage source metadata "
                    + "on this server; critical damage calculations remain enabled.");
        }
        criticalSource = critical;
        Method cause = null;
        Constructor<?> wrapper = null;
        try {
            cause = source.getMethod("knownCause", DamageCause.class);
            wrapper = Class.forName(org.bukkit.Bukkit.getServer().getClass().getPackageName()
                    + ".damage.CraftDamageSource").getConstructor(source);
        } catch (ClassNotFoundException | NoSuchMethodException exception) {
            plugin.getLogger().warning("Obsidian Glaive cannot mark secondary damage as ENTITY_SWEEP_ATTACK "
                    + "on this server; secondary hits retain normal player attack events.");
        }
        knownCause = wrapper == null ? null : cause;
        bukkitSource = wrapper;
        Method effects = null;
        try {
            effects = enchantments.getMethod("doPostAttackEffectsWithItemSource", level, entity, source, stack);
        } catch (NoSuchMethodException exception) {
            plugin.getLogger().warning("Obsidian Glaive cannot apply native post-attack enchantment effects "
                    + "with its captured weapon on this server.");
        }
        postAttackEffects = effects;
    }

    static NativeGlaiveCombat discover(JavaPlugin plugin) {
        try {
            return new NativeGlaiveCombat(plugin);
        } catch (ReflectiveOperationException | SecurityException exception) {
            plugin.getLogger().warning("Native Obsidian Glaive combat is unavailable; using approximate "
                    + "Bukkit movement, critical hits and standard damage enchantments: "
                    + exception.getClass().getSimpleName());
            return null;
        }
    }

    GlaiveCombatAdapter.Attack capture(Player player, LivingEntity primary, boolean nativeCharge) {
        Object handle = invoke(getHandle, player);
        Object weapon = invoke(copyWeapon, invoke(getWeapon, handle));
        Object attackSource = invoke(createSource, handle, weapon);
        Object source = knownCause == null ? attackSource
                : invoke(knownCause, attackSource, DamageCause.ENTITY_SWEEP_ATTACK);
        DamageSource damageSource = bukkitSource == null ? null : wrapSource(source);
        float strength = nativeCharge ? ((Number) invoke(getStrength, handle, 0.5F)).floatValue()
                : player.getAttackCooldown();
        boolean fullStrength = Float.isFinite(strength) && strength > 0.9F;
        boolean critical = fullStrength && (boolean) invoke(canCriticalAttack, handle, invoke(getHandle, primary))
                && !criticalHitsDisabled(handle);
        boolean allowed = (boolean) invoke(canSweep, handle, fullStrength, critical,
                fullStrength && player.isSprinting());
        float attackDamage = (float) attribute(player, Attribute.ATTACK_DAMAGE, 1.0D);
        float scaledDamage = attackDamage * (0.2F + 0.8F * strength * strength);
        float baseDamage = scaledDamage;
        baseDamage += ((Number) invoke(itemBonus, invoke(getItem, weapon), invoke(getHandle, primary),
                baseDamage, attackSource)).floatValue();
        float sweepBase = 1.0F + (float) attribute(player, Attribute.SWEEPING_DAMAGE_RATIO, 0.0D) * baseDamage;
        Object world = invoke(getLevel, handle);
        UUID worldId = player.getWorld().getUID();
        boolean valid = allowed && Float.isFinite(sweepBase) && sweepBase > 0.0F;
        boolean validStab = Float.isFinite(strength) && strength >= 0.0F
                && Float.isFinite(attackDamage) && Float.isFinite(scaledDamage);
        Object stabSource = critical && criticalSource != null ? invoke(criticalSource, attackSource) : attackSource;
        DamageSource stabDamageSource = bukkitSource == null ? null : wrapSource(stabSource);
        return new GlaiveCombatAdapter.Attack() {
            @Override
            public boolean sweepAllowed() {
                return valid;
            }

            @Override
            public double sweepDamage(LivingEntity secondary) {
                if (!valid || !canAffect(player, secondary, worldId)) {
                    return 0.0D;
                }
                float damage = ((Number) invoke(modifyDamage, null, world, weapon,
                        invoke(getHandle, secondary), source, sweepBase)).floatValue() * strength;
                return Float.isFinite(damage) && damage > 0.0F ? damage : 0.0D;
            }

            @Override
            public DamageSource sweepDamageSource() {
                return damageSource;
            }

            @Override
            public void afterSweepHit(LivingEntity secondary) {
                if (valid && postAttackEffects != null && canAffect(player, secondary, worldId)) {
                    invoke(postAttackEffects, null, world, invoke(getHandle, secondary), source, weapon);
                }
            }

            @Override
            public double stabDamage(LivingEntity secondary) {
                if (!validStab || !canAffect(player, secondary, worldId)) {
                    return 0.0D;
                }
                Object target = invoke(getHandle, secondary);
                float bonus = ((Number) invoke(itemBonus, invoke(getItem, weapon), target,
                        scaledDamage, attackSource)).floatValue();
                float enchanted = ((Number) invoke(modifyDamage, null, world, weapon, target,
                        attackSource, attackDamage)).floatValue();
                float damage = NativeGlaiveCombat.stabDamage(
                        attackDamage, scaledDamage, bonus, enchanted, strength, critical);
                return Float.isFinite(damage) && damage > 0.0F ? damage : 0.0D;
            }

            @Override
            public DamageSource stabDamageSource() {
                return stabDamageSource;
            }

            @Override
            public void afterStabHit(LivingEntity secondary) {
                if (validStab && postAttackEffects != null && canAffect(player, secondary, worldId)) {
                    invoke(postAttackEffects, null, world, invoke(getHandle, secondary), stabSource, weapon);
                }
            }
        };
    }

    static float stabDamage(float attackDamage, float scaledDamage, float itemBonus,
            float enchantedDamage, float strength, boolean critical) {
        return (scaledDamage + itemBonus) * (critical ? 1.5F : 1.0F)
                + strength * (enchantedDamage - attackDamage);
    }

    private boolean criticalHitsDisabled(Object handle) {
        if (paperConfig == null) {
            return false;
        }
        Object config = invoke(paperConfig, invoke(getLevel, handle));
        try {
            return disablePlayerCrits.getBoolean(behaviorConfig.get(entitiesConfig.get(config)));
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Could not access the server critical-hit configuration", exception);
        }
    }

    private static boolean canAffect(Player player, LivingEntity secondary, UUID worldId) {
        return SchedulerCompat.isOwnedByCurrentRegion(player) && SchedulerCompat.isOwnedByCurrentRegion(secondary)
                && player.isOnline() && !player.isDead() && secondary.isValid() && !secondary.isDead()
                && player.getWorld().getUID().equals(worldId) && secondary.getWorld() == player.getWorld();
    }

    private DamageSource wrapSource(Object source) {
        try {
            return (DamageSource) bukkitSource.newInstance(source);
        } catch (InvocationTargetException exception) {
            throw new IllegalStateException("Could not create Obsidian Glaive sweep damage source", exception.getCause());
        } catch (InstantiationException | IllegalAccessException exception) {
            throw new IllegalStateException("Could not access Obsidian Glaive sweep damage source", exception);
        }
    }

    static double attribute(Player player, Attribute attribute, double fallback) {
        AttributeInstance instance = player.getAttribute(attribute);
        return instance == null ? fallback : instance.getValue();
    }

    private static Object invoke(Method method, Object instance, Object... arguments) {
        try {
            return method.invoke(instance, arguments);
        } catch (InvocationTargetException exception) {
            throw new IllegalStateException("Native Obsidian Glaive combat failed", exception.getCause());
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Native Obsidian Glaive combat is inaccessible", exception);
        }
    }
}
