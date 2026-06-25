// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Garde1 / Gardeone12

package ru.garde.magnet.command

import ru.garde.magnet.compat.NamedTextColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

internal class GiveCommand(
    private val context: CommandContext
) {
    fun execute(sender: CommandSender): Boolean {
        if (sender !is Player) {
            context.messages.send(sender, "command.player-only", NamedTextColor.RED)
            return true
        }

        val language = context.messages.languageFor(sender)

        sender.inventory.addItem(context.itemFactory.createPortableMagnet(language))
        context.messages.send(sender, "command.received", NamedTextColor.GREEN)

        return true
    }
}
