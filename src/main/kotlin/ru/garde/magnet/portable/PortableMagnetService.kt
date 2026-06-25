// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Garde1 / Gardeone12

package ru.garde.magnet.portable

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitTask
import ru.garde.magnet.MagnetPlugin

internal class PortableMagnetService(
    private val plugin: MagnetPlugin,
    private val itemFactory: MagnetItemFactory
) {
    private var task: BukkitTask? = null

    private val ferromagneticMaterials = resolveMaterials(
        "IRON_INGOT", "RAW_IRON", "IRON_NUGGET", "IRON_BLOCK", "RAW_IRON_BLOCK",
        "IRON_ORE", "DEEPSLATE_IRON_ORE", "IRON_SWORD", "IRON_PICKAXE", "IRON_AXE",
        "IRON_SHOVEL", "IRON_HOE", "IRON_HELMET", "IRON_CHESTPLATE", "IRON_LEGGINGS",
        "IRON_BOOTS", "IRON_HORSE_ARMOR", "CHAINMAIL_HELMET", "CHAINMAIL_CHESTPLATE",
        "CHAINMAIL_LEGGINGS", "CHAINMAIL_BOOTS", "ANVIL", "CHIPPED_ANVIL", "DAMAGED_ANVIL",
        "IRON_BARS", "IRON_DOOR", "IRON_TRAPDOOR", "HEAVY_WEIGHTED_PRESSURE_PLATE",
        "HOPPER", "CAULDRON", "LANTERN", "SOUL_LANTERN", "BUCKET", "WATER_BUCKET",
        "LAVA_BUCKET", "MILK_BUCKET", "POWDER_SNOW_BUCKET", "SHEARS", "FLINT_AND_STEEL",
        "COMPASS", "RECOVERY_COMPASS", "RAIL", "POWERED_RAIL", "DETECTOR_RAIL",
        "ACTIVATOR_RAIL", "MINECART", "CHEST_MINECART", "FURNACE_MINECART",
        "HOPPER_MINECART", "TNT_MINECART"
    )

    private val weakMagneticMaterials = resolveMaterials(
        "NETHERITE_INGOT", "NETHERITE_SCRAP", "NETHERITE_BLOCK", "NETHERITE_SWORD",
        "NETHERITE_PICKAXE", "NETHERITE_AXE", "NETHERITE_SHOVEL", "NETHERITE_HOE",
        "NETHERITE_HELMET", "NETHERITE_CHESTPLATE", "NETHERITE_LEGGINGS", "NETHERITE_BOOTS"
    )

    fun start() {
        stop()
        task = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            applyPortableMagnetTick()
        }, 20L, 2L)
    }

    fun stop() {
        task?.cancel()
        task = null
    }

    fun applyPortableMagnetTick() {
        for (player in Bukkit.getOnlinePlayers()) {
            if (!hasMagnet(player)) continue
            attractItemsToPlayer(player)
        }
    }

    fun isPortableMagnet(item: ItemStack?): Boolean = itemFactory.isPortableMagnet(item)

    fun getMagneticMultiplier(material: Material): Double? {
        return when {
            material in ferromagneticMaterials -> 1.0
            material in weakMagneticMaterials -> 0.5
            else -> null
        }
    }

    private fun hasMagnet(player: Player): Boolean {
        return isPortableMagnet(player.inventory.itemInMainHand) ||
            isPortableMagnet(player.inventory.itemInOffHand)
    }

    private fun attractItemsToPlayer(player: Player) {
        val radius = 7.0
        val baseSpeed = 0.35
        val magnetLocation = player.location.clone().add(0.0, 1.0, 0.0)

        for (entity in player.getNearbyEntities(radius, radius, radius)) {
            if (entity !is Item || !entity.isValid || entity.isDead) continue

            val magneticMultiplier = getMagneticMultiplier(entity.itemStack.type) ?: continue
            val direction = magnetLocation.toVector().subtract(entity.location.toVector())
            if (direction.lengthSquared() <= 0.09) continue

            entity.velocity = direction.normalize().multiply(baseSpeed * magneticMultiplier)
        }
    }

    private fun resolveMaterials(vararg names: String): Set<Material> {
        return names.mapNotNull(Material::matchMaterial).toSet()
    }
}
