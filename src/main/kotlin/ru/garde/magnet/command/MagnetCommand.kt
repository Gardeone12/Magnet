// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Garde1 / Gardeone12

package ru.garde.magnet.command

import ru.garde.magnet.compat.NamedTextColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import java.util.Locale

internal class MagnetCommand(
    private val context: CommandContext,
    private val giveCommand: GiveCommand,
    private val reloadCommand: ReloadCommand,
    private val debugCommand: DebugCommand,
    private val coreCommand: CoreCommand,
    private val profileCommand: ProfileCommand
) : CommandExecutor {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (!command.name.equals("magnet", ignoreCase = true)) {
            return false
        }

        if (args.isEmpty() || args[0].equals("help", ignoreCase = true)) {
            context.sendHelp(sender)
            return true
        }

        return when (args[0].lowercase(Locale.ROOT)) {
            "give" -> giveCommand.execute(sender)
            "reload" -> reloadCommand.execute(sender)
            "debug" -> debugCommand.execute(sender, args)
            "core" -> coreCommand.execute(sender, args)
            "profile" -> profileCommand.execute(sender, args)
            else -> {
                context.messages.send(sender, "command.unknown", NamedTextColor.RED)
                context.sendHelp(sender)
                true
            }
        }
    }
}
