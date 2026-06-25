// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Garde1 / Gardeone12

package ru.garde.magnet.command

import ru.garde.magnet.compat.NamedTextColor
import org.bukkit.command.CommandSender

internal class ReloadCommand(
    private val context: CommandContext
) {
    fun execute(sender: CommandSender): Boolean {
        context.messages.reload()
        context.coreManager.reload()
        context.resourcePackService.validateConfiguration()
        context.messages.send(sender, "command.reloaded", NamedTextColor.GREEN)
        return true
    }
}
