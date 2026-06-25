// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Garde1 / Gardeone12

package ru.garde.magnet.message

import org.bukkit.ChatColor
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import ru.garde.magnet.MagnetPlugin
import java.util.Locale

internal class MessageService(
    private val plugin: MagnetPlugin
) {
    private val supportedLanguages = setOf(DEFAULT_LANGUAGE, "ru")
    private val messages = mutableMapOf<String, YamlConfiguration>()

    fun reload() {
        for (language in supportedLanguages) {
            val languageFile = plugin.dataFolder.resolve("lang/$language.yml")
            if (!languageFile.isFile) {
                plugin.saveResource("lang/$language.yml", false)
            }
            messages[language] = YamlConfiguration.loadConfiguration(languageFile)
        }
    }

    fun languageFor(sender: CommandSender): String {
        val player = sender as? Player ?: return DEFAULT_LANGUAGE
        val language = player.locale
            .substringBefore('_')
            .substringBefore('-')
            .lowercase(Locale.ROOT)
        return if (language in supportedLanguages) language else DEFAULT_LANGUAGE
    }

    fun message(language: String, path: String): String {
        return messages[language]?.getString(path)
            ?: messages[DEFAULT_LANGUAGE]?.getString(path)
            ?: path
    }

    fun messageList(language: String, path: String): List<String> {
        val localized = messages[language]?.getStringList(path).orEmpty()
        if (localized.isNotEmpty()) return localized

        val fallback = messages[DEFAULT_LANGUAGE]?.getStringList(path).orEmpty()
        return fallback.ifEmpty { listOf(path) }
    }

    fun send(
        sender: CommandSender,
        path: String,
        color: ChatColor,
        vararg replacements: Pair<String, String>
    ) {
        var text = message(languageFor(sender), path)
        for ((key, value) in replacements) {
            text = text.replace("{$key}", value)
        }

        sender.sendMessage(color.toString() + text)
    }

    companion object {
        const val DEFAULT_LANGUAGE = "en"
    }
}
