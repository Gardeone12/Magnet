// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Garde1 / Gardeone12

package ru.garde.magnet.command

import ru.garde.magnet.compat.LegacyComponent as Component
import ru.garde.magnet.compat.sendMessage
import ru.garde.magnet.compat.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.Locale

internal class CoreCommand(
    private val context: CommandContext
) {
    fun execute(sender: CommandSender, args: Array<out String>): Boolean {
        return when (args.getOrNull(1)?.lowercase(Locale.ROOT)) {
            "create" -> handleCoreCreate(sender, args)
            "createat" -> handleCoreCreateAt(sender, args)
            "remove", "delete" -> handleCoreRemove(sender, args)
            "list" -> handleCoreList(sender)
            "info", "check" -> handleCoreInfo(sender, args)
            "rescan" -> handleCoreRescan(sender, args)
            "refresh" -> handleCoreRefresh(sender, args)
            "override" -> handleCoreOverride(sender, args)
            "set" -> handleCoreSet(sender, args)
            "reload" -> handleCoreReload(sender)
            else -> {
                context.messages.send(sender, "command.core-usage", NamedTextColor.YELLOW)
                true
            }
        }
    }

    private fun handleCoreCreate(sender: CommandSender, args: Array<out String>): Boolean {
        if (sender !is Player) {
            context.messages.send(sender, "command.player-only", NamedTextColor.RED)
            return true
        }

        val id = args.getOrNull(2)?.lowercase(Locale.ROOT)
        if (id == null) {
            context.messages.send(sender, "command.core-create-usage", NamedTextColor.YELLOW)
            return true
        }

        if (!context.coreIdPattern.matches(id)) {
            context.messages.send(sender, "command.core-id-invalid", NamedTextColor.RED)
            return true
        }

        if (context.coreManager.exists(id)) {
            context.messages.send(sender, "command.core-already-exists", NamedTextColor.RED, "id" to id)
            return true
        }

        val targetBlock = sender.getTargetBlockExact(10)
        if (targetBlock == null || !context.coreManager.isCoreMaterial(targetBlock.type)) {
            context.messages.send(sender, "command.core-need-material", NamedTextColor.RED)
            return true
        }

        val radiusOverride = args.getOrNull(3)?.let { context.parseRadius(sender, it) ?: return true }
        val strengthOverride = args.getOrNull(4)?.let { context.parseStrength(sender, it) ?: return true }
        val result = context.coreManager.create(id, targetBlock, radiusOverride, strengthOverride)
        if (result == null) {
            context.messages.send(sender, "command.core-2x2x2-not-found", NamedTextColor.RED)
            return true
        }
        val core = result.core

        context.messages.send(
            sender,
            "command.core-created",
            NamedTextColor.GREEN,
            "id" to core.id,
            "world" to core.worldName,
            "x" to context.formatDouble(core.centerX),
            "y" to context.formatDouble(core.centerY),
            "z" to context.formatDouble(core.centerZ),
            "radius" to context.formatDouble(core.radius),
            "strength" to context.formatDouble(core.strength),
            "profile" to core.coreProfile,
            "calculatedRadius" to context.formatDouble(core.calculatedRadius),
            "calculatedStrength" to context.formatDouble(core.calculatedStrength),
            "override" to core.manualOverride.toString(),
            "critical" to core.criticalFrameBlocks.size.toString(),
            "optional" to core.optionalFrameBlocks.size.toString()
        )
        sendFrameWarning(sender, result.frameWarning)

        return true
    }

    private fun handleCoreCreateAt(sender: CommandSender, args: Array<out String>): Boolean {
        val id = args.getOrNull(2)?.lowercase(Locale.ROOT)
        if (id == null) {
            context.messages.send(sender, "command.core-createat-usage", NamedTextColor.YELLOW)
            return true
        }

        if (!context.coreIdPattern.matches(id)) {
            context.messages.send(sender, "command.core-id-invalid", NamedTextColor.RED)
            return true
        }

        if (context.coreManager.exists(id)) {
            context.messages.send(sender, "command.core-already-exists", NamedTextColor.RED, "id" to id)
            return true
        }

        val parsed = parseCreateAtArguments(sender, args) ?: return true
        val world = Bukkit.getWorld(parsed.worldName)
        if (world == null) {
            context.messages.send(sender, "command.world-not-found", NamedTextColor.RED, "world" to parsed.worldName)
            return true
        }

        val block = world.getBlockAt(parsed.x, parsed.y, parsed.z)
        if (!context.coreManager.isCoreMaterial(block.type)) {
            context.messages.send(
                sender,
                "command.core-createat-not-material",
                NamedTextColor.RED,
                "material" to block.type.name
            )
            return true
        }

        val result = context.coreManager.createAt(
            id = id,
            worldName = world.name,
            x = parsed.x,
            y = parsed.y,
            z = parsed.z,
            radiusOverride = parsed.radiusOverride,
            strengthOverride = parsed.strengthOverride
        )
        if (result == null) {
            context.messages.send(sender, "command.core-2x2x2-not-found", NamedTextColor.RED)
            return true
        }
        val core = result.core

        context.messages.send(
            sender,
            "command.core-created",
            NamedTextColor.GREEN,
            "id" to core.id,
            "world" to core.worldName,
            "x" to context.formatDouble(core.centerX),
            "y" to context.formatDouble(core.centerY),
            "z" to context.formatDouble(core.centerZ),
            "radius" to context.formatDouble(core.radius),
            "strength" to context.formatDouble(core.strength),
            "profile" to core.coreProfile,
            "calculatedRadius" to context.formatDouble(core.calculatedRadius),
            "calculatedStrength" to context.formatDouble(core.calculatedStrength),
            "override" to core.manualOverride.toString(),
            "critical" to core.criticalFrameBlocks.size.toString(),
            "optional" to core.optionalFrameBlocks.size.toString()
        )
        sendFrameWarning(sender, result.frameWarning)

        return true
    }

    private fun parseCreateAtArguments(
        sender: CommandSender,
        args: Array<out String>
    ): ParsedCreateAt? {
        val firstNumberIndex = args.indexOfFirstFrom(3) { it.toIntOrNull() != null }
        if (firstNumberIndex == -1) {
            context.messages.send(sender, "command.core-createat-usage", NamedTextColor.YELLOW)
            return null
        }

        val worldName = if (firstNumberIndex == 3) {
            val player = sender as? Player
            if (player == null) {
                context.messages.send(sender, "command.core-createat-console-usage", NamedTextColor.YELLOW)
                return null
            }

            player.world.name
        } else {
            args[3]
        }

        val x = args.getOrNull(firstNumberIndex)?.toIntOrNull()
        val y = args.getOrNull(firstNumberIndex + 1)?.toIntOrNull()
        val z = args.getOrNull(firstNumberIndex + 2)?.toIntOrNull()
        if (x == null || y == null || z == null) {
            context.messages.send(sender, "command.core-createat-usage", NamedTextColor.YELLOW)
            return null
        }

        val radiusOverride = args.getOrNull(firstNumberIndex + 3)?.let {
            context.parseRadius(sender, it) ?: return null
        }
        val strengthOverride = args.getOrNull(firstNumberIndex + 4)?.let {
            context.parseStrength(sender, it) ?: return null
        }

        return ParsedCreateAt(worldName, x, y, z, radiusOverride, strengthOverride)
    }

    private fun handleCoreRemove(sender: CommandSender, args: Array<out String>): Boolean {
        val id = args.getOrNull(2)?.lowercase(Locale.ROOT)
        if (id == null) {
            context.messages.send(sender, "command.core-remove-usage", NamedTextColor.YELLOW)
            return true
        }

        if (context.coreManager.remove(id) == null) {
            context.messages.send(sender, "command.core-not-found", NamedTextColor.RED, "id" to id)
            return true
        }

        context.messages.send(sender, "command.core-removed", NamedTextColor.GREEN, "id" to id)
        return true
    }

    private fun handleCoreRescan(sender: CommandSender, args: Array<out String>): Boolean {
        val id = args.getOrNull(2)?.lowercase(Locale.ROOT)
        if (id == null) {
            context.messages.send(sender, "command.core-rescan-usage", NamedTextColor.YELLOW)
            return true
        }

        val result = context.coreManager.rescanFrame(id)
        if (result == null) {
            context.messages.send(sender, "command.core-not-found", NamedTextColor.RED, "id" to id)
            return true
        }

        if (!result.success) {
            context.messages.send(
                sender,
                "command.core-frame-rescan-failed",
                NamedTextColor.RED,
                "id" to result.core.id,
                "reason" to (result.message ?: "unknown")
            )
            return true
        }

        context.messages.send(
            sender,
            "command.core-frame-rescanned",
            NamedTextColor.GREEN,
            "id" to result.core.id,
            "critical" to result.criticalCount.toString(),
            "optional" to result.optionalCount.toString()
        )
        sendFrameWarning(sender, result.message)
        return true
    }

    private fun handleCoreRefresh(sender: CommandSender, args: Array<out String>): Boolean {
        val id = args.getOrNull(2)?.lowercase(Locale.ROOT)
        if (id == null) {
            context.messages.send(sender, "command.core-refresh-usage", NamedTextColor.YELLOW)
            return true
        }

        val result = context.coreManager.refreshCore(id)
        if (result == null) {
            context.messages.send(sender, "command.core-not-found", NamedTextColor.RED, "id" to id)
            return true
        }

        if (!result.success) {
            context.messages.send(
                sender,
                "command.core-refresh-failed",
                NamedTextColor.RED,
                "id" to result.core.id,
                "reason" to (result.message ?: "unknown")
            )
            return true
        }

        context.messages.send(
            sender,
            "command.core-refreshed",
            NamedTextColor.GREEN,
            "id" to result.core.id,
            "profile" to result.core.coreProfile,
            "radius" to context.formatDouble(result.core.radius),
            "strength" to context.formatDouble(result.core.strength)
        )
        return true
    }

    private fun handleCoreOverride(sender: CommandSender, args: Array<out String>): Boolean {
        val id = args.getOrNull(2)?.lowercase(Locale.ROOT)
        val enabled = args.getOrNull(3)?.toBooleanStrictOrNull()
        if (id == null || enabled == null) {
            context.messages.send(sender, "command.core-override-usage", NamedTextColor.YELLOW)
            return true
        }

        val result = context.coreManager.setCoreOverride(id, enabled)
        if (result == null) {
            context.messages.send(sender, "command.core-not-found", NamedTextColor.RED, "id" to id)
            return true
        }
        if (!result.success) {
            context.messages.send(
                sender,
                "command.core-refresh-failed",
                NamedTextColor.RED,
                "id" to result.core.id,
                "reason" to (result.message ?: "unknown")
            )
            return true
        }

        context.messages.send(
            sender,
            "command.core-override-updated",
            NamedTextColor.GREEN,
            "id" to result.core.id,
            "override" to result.core.manualOverride.toString(),
            "radius" to context.formatDouble(result.core.radius),
            "strength" to context.formatDouble(result.core.strength)
        )
        return true
    }

    private fun handleCoreSet(sender: CommandSender, args: Array<out String>): Boolean {
        val id = args.getOrNull(2)?.lowercase(Locale.ROOT)
        val field = args.getOrNull(3)?.lowercase(Locale.ROOT)
        val value = args.getOrNull(4)
        if (id == null || field == null || value == null) {
            context.messages.send(sender, "command.core-set-usage", NamedTextColor.YELLOW)
            return true
        }

        val result = when (field) {
            "radius" -> {
                val radius = context.parseRadius(sender, value) ?: return true
                context.coreManager.setCoreRadius(id, radius)
            }
            "strength" -> {
                val strength = context.parseStrength(sender, value) ?: return true
                context.coreManager.setCoreStrength(id, strength)
            }
            else -> {
                context.messages.send(sender, "command.core-set-usage", NamedTextColor.YELLOW)
                return true
            }
        }

        if (result == null) {
            context.messages.send(sender, "command.core-not-found", NamedTextColor.RED, "id" to id)
            return true
        }

        context.messages.send(
            sender,
            "command.core-set-updated",
            NamedTextColor.GREEN,
            "id" to result.core.id,
            "radius" to context.formatDouble(result.core.radius),
            "strength" to context.formatDouble(result.core.strength)
        )
        return true
    }

    private fun handleCoreList(sender: CommandSender): Boolean {
        val cores = context.coreManager.all()
        if (cores.isEmpty()) {
            context.messages.send(sender, "command.core-list-empty", NamedTextColor.GRAY)
            return true
        }

        context.messages.send(sender, "command.core-list-header", NamedTextColor.AQUA)
        for (core in cores) {
            val activity = if (core.active && !core.damaged) "active" else "inactive"
            val frameState = if (core.damaged) {
                "damaged"
            } else if (core.criticalFrameBlocks.isNotEmpty()) {
                "ok"
            } else {
                "missing"
            }
            val line = if (core.damaged) {
                "${core.id} | $activity | damaged | reason: ${core.damageReason ?: "unknown"}"
            } else {
                "${core.id} | $activity | profile: ${core.coreProfile} | " +
                    "radius: ${context.formatDouble(core.radius)} | strength: ${context.formatDouble(core.strength)} | " +
                    "calculated: ${context.formatDouble(core.calculatedRadius)}/${context.formatDouble(core.calculatedStrength)} | " +
                    "override: ${core.manualOverride} | frame: $frameState | " +
                    "critical: ${core.criticalFrameBlocks.size} | optional: ${core.optionalFrameBlocks.size}"
            }
            sender.sendMessage(
                Component.text(line).color(if (core.damaged) NamedTextColor.RED else NamedTextColor.GRAY)
            )
        }

        return true
    }

    private fun handleCoreInfo(sender: CommandSender, args: Array<out String>): Boolean {
        val id = args.getOrNull(2)?.lowercase(Locale.ROOT)
        if (id == null) {
            context.messages.send(sender, "command.core-info-usage", NamedTextColor.YELLOW)
            return true
        }

        val inspection = context.coreManager.inspect(id)
        if (inspection == null) {
            context.messages.send(sender, "command.core-not-found", NamedTextColor.RED, "id" to id)
            return true
        }

        val core = inspection.core
        sender.sendMessage(
            Component.text(
                "Core ${core.id}:"
            ).color(NamedTextColor.AQUA)
        )
        sender.sendMessage(
            Component.text(
                "Status: ${if (core.active && !core.damaged) "active" else "inactive"}, damaged=${core.damaged}"
            ).color(if (core.active && !core.damaged) NamedTextColor.GREEN else NamedTextColor.RED)
        )
        sender.sendMessage(
            Component.text(
                "World: ${core.worldName}, center: " +
                    "${context.formatDouble(core.centerX)} ${context.formatDouble(core.centerY)} ${context.formatDouble(core.centerZ)}"
            ).color(NamedTextColor.GRAY)
        )
        sender.sendMessage(
            Component.text(
                "Profile: ${core.coreProfile}, radius: ${context.formatDouble(core.radius)}, " +
                    "strength: ${context.formatDouble(core.strength)}, calculated: " +
                    "${context.formatDouble(core.calculatedRadius)} / ${context.formatDouble(core.calculatedStrength)}, " +
                    "override: ${core.manualOverride}, core-size: ${core.coreSize}"
            ).color(NamedTextColor.GRAY)
        )
        sender.sendMessage(
            Component.text(
                "World loaded=${inspection.worldLoaded}, chunk loaded=${inspection.chunkLoaded}, " +
                    "core complete=${inspection.coreComplete}"
            ).color(if (inspection.coreComplete) NamedTextColor.GREEN else NamedTextColor.RED)
        )
        sender.sendMessage(
            Component.text(
                "Frame complete=${inspection.frameComplete}, critical=${core.criticalFrameBlocks.size}, " +
                    "optional=${core.optionalFrameBlocks.size}, reason=${inspection.frameReason ?: core.damageReason ?: "none"}"
            ).color(if (inspection.frameComplete && !core.damaged) NamedTextColor.GREEN else NamedTextColor.RED)
        )
        sender.sendMessage(Component.text("Core blocks:").color(NamedTextColor.AQUA))
        for ((index, position) in core.coreBlocks.withIndex()) {
            val material = inspection.blockTypes.getOrNull(index)?.name ?: "UNKNOWN"
            sender.sendMessage(
                Component.text("- ${core.worldName} ${position.x} ${position.y} ${position.z} $material")
                    .color(if (material == "UNKNOWN") NamedTextColor.RED else NamedTextColor.GRAY)
            )
        }
        sender.sendMessage(
            Component.text(
                "Nearby dropped items=${inspection.nearbyItems}, magnetic items=${inspection.magneticItems}"
            ).color(if (inspection.magneticItems > 0) NamedTextColor.GREEN else NamedTextColor.YELLOW)
        )

        return true
    }

    private fun sendFrameWarning(sender: CommandSender, warning: String?) {
        if (warning == null) return

        context.messages.send(
            sender,
            "command.core-frame-warning",
            NamedTextColor.YELLOW,
            "warning" to warning
        )
    }

    private fun handleCoreReload(sender: CommandSender): Boolean {
        context.coreManager.reload()
        context.messages.send(sender, "command.core-reloaded", NamedTextColor.GREEN)
        return true
    }

    private inline fun Array<out String>.indexOfFirstFrom(
        startIndex: Int,
        predicate: (String) -> Boolean
    ): Int {
        for (index in startIndex until size) {
            if (predicate(this[index])) return index
        }

        return -1
    }

    private data class ParsedCreateAt(
        val worldName: String,
        val x: Int,
        val y: Int,
        val z: Int,
        val radiusOverride: Double?,
        val strengthOverride: Double?
    )
}
