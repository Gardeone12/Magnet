package ru.garde.magnet

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.plugin.java.JavaPlugin
import java.util.Locale

class MagnetStructureBreakListener(
    private val plugin: JavaPlugin,
    private val coreManager: MagnetCoreManager
) : Listener {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        val ref = coreManager.lookupStructureBlock(event.block) ?: return
        if (ref.blockType == MagnetStructureBlockType.FRAME_OPTIONAL) return

        val damages = coreManager.handleBrokenBlocks(listOf(event.block))
        for (damage in damages) {
            event.player.sendMessage(
                Component.text("Magnet core ${damage.core.id} disabled: ${damage.reason}")
                    .color(NamedTextColor.RED)
            )
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockPlace(event: BlockPlaceEvent) {
        val result = coreManager.handlePlacedBlock(event.blockPlaced) ?: return
        if (result.success) {
            event.player.sendMessage(
                Component.text(
                    "Magnet core ${result.core.id} restored. Profile: ${result.core.coreProfile}, " +
                        "radius: ${formatDouble(result.core.radius)}, strength: ${formatDouble(result.core.strength)}."
                ).color(NamedTextColor.GREEN)
            )
            return
        }

        val message = if (result.placedMaterialAllowed) {
            "Magnet core ${result.core.id} is still invalid: ${result.message ?: "unknown"}"
        } else {
            "Block does not fit magnet core ${result.core.id}. Magnet disabled."
        }
        event.player.sendMessage(Component.text(message).color(NamedTextColor.RED))
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEntityExplode(event: EntityExplodeEvent) {
        handleExplosion(event.blockList())
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockExplode(event: BlockExplodeEvent) {
        handleExplosion(event.blockList())
    }

    private fun handleExplosion(blocks: List<org.bukkit.block.Block>) {
        val damages = coreManager.handleBrokenBlocks(blocks)
        for (damage in damages) {
            plugin.logger.info("Magnet core ${damage.core.id} disabled: ${damage.reason}")
        }
    }

    private fun formatDouble(value: Double): String {
        return String.format(Locale.US, "%.2f", value)
    }
}
