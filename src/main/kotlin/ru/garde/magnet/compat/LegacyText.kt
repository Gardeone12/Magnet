// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Garde1 / Gardeone12

package ru.garde.magnet.compat

import org.bukkit.ChatColor
import org.bukkit.command.CommandSender

internal typealias NamedTextColor = ChatColor

internal class LegacyComponent private constructor(
    val text: String,
    val color: ChatColor
) {
    fun color(color: ChatColor): LegacyComponent = LegacyComponent(text, color)

    companion object {
        fun text(text: String): LegacyComponent = LegacyComponent(text, ChatColor.RESET)
    }
}

internal fun CommandSender.sendMessage(component: LegacyComponent) {
    sendMessage(component.color.toString() + component.text)
}
