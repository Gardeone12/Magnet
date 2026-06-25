// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Garde1 / Gardeone12

package ru.garde.magnet.command

import ru.garde.magnet.compat.LegacyComponent as Component
import ru.garde.magnet.compat.sendMessage
import ru.garde.magnet.compat.NamedTextColor
import org.bukkit.command.CommandSender
import ru.garde.magnet.MagnetPlugin
import ru.garde.magnet.core.MagnetCoreManager
import ru.garde.magnet.message.MessageService
import ru.garde.magnet.portable.MagnetItemFactory
import ru.garde.magnet.resourcepack.ResourcePackService
import java.util.Locale

internal class CommandContext(
    val plugin: MagnetPlugin,
    val messages: MessageService,
    val itemFactory: MagnetItemFactory,
    val coreManager: MagnetCoreManager,
    val resourcePackService: ResourcePackService
) {
    val coreIdPattern = Regex("[a-z0-9_-]{1,32}")

    fun sendHelp(sender: CommandSender) {
        for (line in messages.messageList(messages.languageFor(sender), "command.help")) {
            sender.sendMessage(Component.text(line).color(NamedTextColor.GRAY))
        }
    }

    fun debugColor(ok: Boolean): NamedTextColor {
        return if (ok) NamedTextColor.GREEN else NamedTextColor.RED
    }

    fun parseRadius(sender: CommandSender, raw: String): Double? {
        val requested = raw.toDoubleOrNull()
        if (requested == null || !java.lang.Double.isFinite(requested) || requested <= 0.0) {
            messages.send(sender, "command.invalid-number", NamedTextColor.RED)
            return null
        }

        val limited = coreManager.limitRadius(requested)
        if (limited < requested) {
            messages.send(
                sender,
                "command.core-radius-clamped",
                NamedTextColor.YELLOW,
                "radius" to formatDouble(limited)
            )
        }

        return limited
    }

    fun parseStrength(sender: CommandSender, raw: String): Double? {
        val requested = raw.toDoubleOrNull()
        if (requested == null || !java.lang.Double.isFinite(requested) || requested <= 0.0) {
            messages.send(sender, "command.invalid-number", NamedTextColor.RED)
            return null
        }

        val limited = coreManager.limitStrength(requested)
        if (limited != requested) {
            messages.send(
                sender,
                "command.core-strength-clamped",
                NamedTextColor.YELLOW,
                "strength" to formatDouble(limited)
            )
        }

        return limited
    }

    fun formatDouble(value: Double): String {
        return String.format(Locale.US, "%.2f", value)
    }
}
