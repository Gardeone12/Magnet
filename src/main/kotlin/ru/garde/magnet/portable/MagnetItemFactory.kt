// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Garde1 / Gardeone12

package ru.garde.magnet.portable

import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import ru.garde.magnet.MagnetPlugin
import ru.garde.magnet.compat.ItemModelCompatibility
import ru.garde.magnet.compat.PortableMagnetVisualDebug
import ru.garde.magnet.message.MessageService

internal class MagnetItemFactory(
    private val plugin: MagnetPlugin,
    private val itemModelCompatibility: ItemModelCompatibility,
    private val messages: MessageService
) {
    private val magnetKey = NamespacedKey(plugin, "magnet")
    val portableMagnetMaterial: Material = Material.matchMaterial("AMETHYST_SHARD") ?: Material.COMPASS

    fun createPortableMagnet(language: String): ItemStack {
        val item = ItemStack(portableMagnetMaterial)
        val meta = item.itemMeta ?: return item

        meta.setDisplayName(ChatColor.AQUA.toString() + messages.message(language, "item.name"))
        meta.lore = messages.messageList(language, "item.lore").mapIndexed { index, line ->
            (if (index == 0) ChatColor.GRAY else ChatColor.DARK_GRAY).toString() + line
        }
        meta.persistentDataContainer.set(magnetKey, PersistentDataType.BYTE, 1)
        itemModelCompatibility.applyPortableMagnetVisuals(meta)

        item.itemMeta = meta
        return item
    }

    fun isPortableMagnet(item: ItemStack?): Boolean {
        if (item == null || item.type == Material.AIR) return false

        val meta = item.itemMeta ?: return false
        val isMagnet = meta.persistentDataContainer.has(magnetKey, PersistentDataType.BYTE)

        if (isMagnet && !itemModelCompatibility.hasPortableMagnetVisuals(meta)) {
            itemModelCompatibility.applyPortableMagnetVisuals(meta)
            item.itemMeta = meta
        }

        return isMagnet
    }

    fun debug(item: ItemStack): PortableMagnetItemDebug {
        val meta = item.itemMeta
        val hasMarker = meta?.persistentDataContainer?.has(magnetKey, PersistentDataType.BYTE) == true
        val visuals = meta?.let(itemModelCompatibility::debug)
            ?: PortableMagnetVisualDebug.none(EXPECTED_PORTABLE_MAGNET_MODEL_KEY)

        return PortableMagnetItemDebug(
            isPortableMagnet = item.type == portableMagnetMaterial && hasMarker,
            hasMarker = hasMarker,
            expectedBaseMaterial = portableMagnetMaterial.name,
            visuals = visuals
        )
    }

    companion object {
        const val PORTABLE_MAGNET_CUSTOM_MODEL_DATA = 9001001
        const val PORTABLE_MAGNET_MODEL_NAMESPACE = "magnit"
        const val PORTABLE_MAGNET_MODEL_PATH = "portable_magnet"
        const val EXPECTED_PORTABLE_MAGNET_MODEL_KEY = "magnit:portable_magnet"

        @Suppress("DEPRECATION")
        val portableMagnetItemModel = NamespacedKey(
            PORTABLE_MAGNET_MODEL_NAMESPACE,
            PORTABLE_MAGNET_MODEL_PATH
        )
    }
}

internal data class PortableMagnetItemDebug(
    val isPortableMagnet: Boolean,
    val hasMarker: Boolean,
    val expectedBaseMaterial: String,
    val visuals: PortableMagnetVisualDebug
)
