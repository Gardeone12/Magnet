// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Garde1 / Gardeone12

package ru.garde.magnet.command

import ru.garde.magnet.compat.LegacyComponent as Component
import ru.garde.magnet.compat.sendMessage
import ru.garde.magnet.compat.NamedTextColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import ru.garde.magnet.portable.MagnetItemFactory
import java.util.Locale

internal class DebugCommand(
    private val context: CommandContext
) {
    fun execute(sender: CommandSender, args: Array<out String>): Boolean {
        return when (args.getOrNull(1)?.lowercase(Locale.ROOT)) {
            "item" -> handleDebugItem(sender)
            else -> {
                context.messages.send(sender, "command.debug-usage", NamedTextColor.YELLOW)
                true
            }
        }
    }

    private fun handleDebugItem(sender: CommandSender): Boolean {
        if (sender !is Player) {
            context.messages.send(sender, "command.player-only", NamedTextColor.RED)
            return true
        }

        val item = sender.inventory.itemInMainHand
        val debug = context.itemFactory.debug(item)

        sender.sendMessage(Component.text("Portable Magnet item debug").color(NamedTextColor.AQUA))
        sender.sendMessage(
            Component.text("Is portable magnet: ${debug.isPortableMagnet}")
                .color(context.debugColor(debug.isPortableMagnet))
        )
        sender.sendMessage(Component.text("Base material: ${item.type.name} (expected ${debug.expectedBaseMaterial})").color(NamedTextColor.GRAY))
        sender.sendMessage(
            Component.text("PersistentDataContainer marker: ${debug.hasMarker}")
                .color(context.debugColor(debug.hasMarker))
        )
        sender.sendMessage(
            Component.text("Item model key: ${debug.visuals.itemModelKey}")
                .color(context.debugColor(debug.visuals.itemModelMatches))
        )
        sender.sendMessage(
            Component.text("Expected model key: ${MagnetItemFactory.EXPECTED_PORTABLE_MAGNET_MODEL_KEY}")
                .color(NamedTextColor.GRAY)
        )
        sender.sendMessage(
            Component.text("Custom model data component: ${debug.visuals.customModelDataComponent}")
                .color(NamedTextColor.GRAY)
        )
        sender.sendMessage(
            Component.text("Legacy custom model data: ${debug.visuals.legacyCustomModelData}")
                .color(NamedTextColor.GRAY)
        )
        sender.sendMessage(
            Component.text("Best available visual path: ${debug.visuals.bestAvailableVisualPath}")
                .color(NamedTextColor.GRAY)
        )
        sender.sendMessage(Component.text("Plugin version: ${context.plugin.description.version}").color(NamedTextColor.GRAY))

        return true
    }
}
