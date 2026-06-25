// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Garde1 / Gardeone12

package ru.garde.magnet.command

import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import ru.garde.magnet.core.MagnetCoreManager
import java.util.Locale

internal class MagnetTabCompleter(
    private val coreManager: MagnetCoreManager
) : TabCompleter {
    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): MutableList<String> {
        if (!command.name.equals("magnet", ignoreCase = true)) {
            return mutableListOf()
        }

        return magnetCommandCompletions(args).toMutableList()
    }

    private fun magnetCommandCompletions(args: Array<out String>): List<String> {
        val rootCommands = listOf("help", "give", "reload", "debug", "core", "profile")
        val coreCommands = listOf(
            "create",
            "createat",
            "remove",
            "list",
            "info",
            "rescan",
            "refresh",
            "override",
            "set",
            "reload"
        )
        val profileCommands = listOf("list", "info", "set", "reload")

        return when (args.size) {
            0 -> rootCommands
            1 -> filterCompletions(args[0], rootCommands)
            2 -> when (args[0].lowercase(Locale.ROOT)) {
                "debug" -> filterCompletions(args[1], listOf("item"))
                "core" -> filterCompletions(args[1], coreCommands)
                "profile" -> filterCompletions(args[1], profileCommands)
                else -> emptyList()
            }
            3 -> when (args[0].lowercase(Locale.ROOT)) {
                "core" -> when (args[1].lowercase(Locale.ROOT)) {
                    "remove", "delete", "info", "check", "rescan", "refresh", "override", "set" ->
                        filterCompletions(args[2], coreIds())
                    else -> emptyList()
                }
                "profile" -> when (args[1].lowercase(Locale.ROOT)) {
                    "info", "set" -> filterCompletions(args[2], coreMaterialNames())
                    else -> emptyList()
                }
                else -> emptyList()
            }
            4 -> when (args[0].lowercase(Locale.ROOT)) {
                "core" -> when (args[1].lowercase(Locale.ROOT)) {
                    "override" -> filterCompletions(args[3], listOf("true", "false"))
                    "set" -> filterCompletions(args[3], listOf("radius", "strength"))
                    else -> emptyList()
                }
                "profile" -> when (args[1].lowercase(Locale.ROOT)) {
                    "set" -> filterCompletions(args[3], listOf("radius", "strength", "priority"))
                    else -> emptyList()
                }
                else -> emptyList()
            }
            else -> emptyList()
        }
    }

    private fun filterCompletions(prefix: String, options: Iterable<String>): List<String> {
        val normalizedPrefix = prefix.lowercase(Locale.ROOT)
        return options
            .filter { it.lowercase(Locale.ROOT).startsWith(normalizedPrefix) }
            .sorted()
    }

    private fun coreIds(): List<String> {
        return coreManager.all().map { it.id }
    }

    private fun coreMaterialNames(): List<String> {
        val configuredProfiles = coreManager.coreMaterialProfiles().map { it.material.name }
        if (configuredProfiles.isNotEmpty()) return configuredProfiles

        return Material.values()
            .asSequence()
            .filter { it.isBlock && it != Material.AIR }
            .map { it.name }
            .toList()
    }
}
