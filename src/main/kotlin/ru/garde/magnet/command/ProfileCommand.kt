// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Garde1 / Gardeone12

package ru.garde.magnet.command

import ru.garde.magnet.compat.LegacyComponent as Component
import ru.garde.magnet.compat.sendMessage
import ru.garde.magnet.compat.NamedTextColor
import org.bukkit.command.CommandSender
import java.util.Locale

internal class ProfileCommand(
    private val context: CommandContext
) {
    fun execute(sender: CommandSender, args: Array<out String>): Boolean {
        return when (args.getOrNull(1)?.lowercase(Locale.ROOT)) {
            "list" -> handleProfileList(sender)
            "info" -> handleProfileInfo(sender, args)
            "set" -> handleProfileSet(sender, args)
            "reload" -> handleProfileReload(sender)
            else -> {
                context.messages.send(sender, "command.profile-usage", NamedTextColor.YELLOW)
                true
            }
        }
    }

    private fun handleProfileList(sender: CommandSender): Boolean {
        val profiles = context.coreManager.coreMaterialProfiles()
        if (profiles.isEmpty()) {
            context.messages.send(sender, "command.profile-list-empty", NamedTextColor.GRAY)
            return true
        }

        context.messages.send(sender, "command.profile-list-header", NamedTextColor.AQUA)
        for (profile in profiles) {
            sender.sendMessage(
                Component.text(
                    "${profile.material.name} | profile: ${profile.profile} | " +
                        "radius: ${context.formatDouble(profile.baseRadius)} | " +
                        "strength: ${context.formatDouble(profile.baseStrength)} | priority: ${profile.priority}"
                ).color(NamedTextColor.GRAY)
            )
        }
        return true
    }

    private fun handleProfileInfo(sender: CommandSender, args: Array<out String>): Boolean {
        val materialName = args.getOrNull(2)
        if (materialName == null) {
            context.messages.send(sender, "command.profile-info-usage", NamedTextColor.YELLOW)
            return true
        }

        val profile = context.coreManager.coreMaterialProfile(materialName)
        if (profile == null) {
            context.messages.send(
                sender,
                "command.profile-not-found",
                NamedTextColor.RED,
                "material" to materialName.uppercase(Locale.ROOT)
            )
            return true
        }

        sender.sendMessage(
            Component.text(
                "${profile.material.name}: profile=${profile.profile}, " +
                    "radius=${context.formatDouble(profile.baseRadius)}, " +
                    "strength=${context.formatDouble(profile.baseStrength)}, priority=${profile.priority}"
            ).color(NamedTextColor.AQUA)
        )
        return true
    }

    private fun handleProfileSet(sender: CommandSender, args: Array<out String>): Boolean {
        val materialName = args.getOrNull(2)
        val field = args.getOrNull(3)?.lowercase(Locale.ROOT)
        val value = args.getOrNull(4)
        if (materialName == null || field == null || value == null) {
            context.messages.send(sender, "command.profile-set-usage", NamedTextColor.YELLOW)
            return true
        }

        val profile = when (field) {
            "radius" -> {
                val radius = context.parseRadius(sender, value) ?: return true
                context.coreManager.setMaterialProfileRadius(materialName, radius)
            }
            "strength" -> {
                val strength = context.parseStrength(sender, value) ?: return true
                context.coreManager.setMaterialProfileStrength(materialName, strength)
            }
            "priority" -> {
                val priority = value.toIntOrNull()
                if (priority == null) {
                    context.messages.send(sender, "command.invalid-number", NamedTextColor.RED)
                    return true
                }
                context.coreManager.setMaterialProfilePriority(materialName, priority)
            }
            else -> {
                context.messages.send(sender, "command.profile-set-usage", NamedTextColor.YELLOW)
                return true
            }
        }

        if (profile == null) {
            context.messages.send(
                sender,
                "command.profile-not-found",
                NamedTextColor.RED,
                "material" to materialName.uppercase(Locale.ROOT)
            )
            return true
        }

        context.messages.send(
            sender,
            "command.profile-updated",
            NamedTextColor.GREEN,
            "material" to profile.material.name,
            "profile" to profile.profile,
            "radius" to context.formatDouble(profile.baseRadius),
            "strength" to context.formatDouble(profile.baseStrength),
            "priority" to profile.priority.toString()
        )
        return true
    }

    private fun handleProfileReload(sender: CommandSender): Boolean {
        val refreshed = context.coreManager.reloadProfiles()
        context.messages.send(
            sender,
            "command.profile-reloaded",
            NamedTextColor.GREEN,
            "count" to refreshed.toString()
        )
        return true
    }
}
