// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Garde1 / Gardeone12

package ru.garde.magnet.resourcepack

import ru.garde.magnet.MagnetPlugin
import ru.garde.magnet.config.MagnetConfig
import ru.garde.magnet.portable.MagnetItemFactory

internal class ResourcePackService(
    private val plugin: MagnetPlugin,
    private val config: MagnetConfig
) {
    fun validateConfiguration() {
        val resourcePack = config.resourcePack()

        if (resourcePack.invalidSection) {
            plugin.logger.warning("resource-pack must be a configuration section.")
            return
        }

        if (resourcePack.modelKey.isBlank()) {
            plugin.logger.warning(
                "resource-pack.model-key is empty; expected ${MagnetItemFactory.EXPECTED_PORTABLE_MAGNET_MODEL_KEY}."
            )
        } else if (resourcePack.modelKey != MagnetItemFactory.EXPECTED_PORTABLE_MAGNET_MODEL_KEY) {
            plugin.logger.warning(
                "resource-pack.model-key is '${resourcePack.modelKey}', but Portable Magnet uses " +
                    "${MagnetItemFactory.EXPECTED_PORTABLE_MAGNET_MODEL_KEY}. " +
                    "A namespace mismatch can cause missing texture."
            )
        }

        if (!resourcePack.enabled) return

        when (resourcePack.mode) {
            "external" -> {
                if (resourcePack.url.isBlank()) {
                    plugin.logger.warning("resource-pack is enabled in config, but resource-pack.url is empty.")
                }
            }
            "builtin", "built-in", "local" -> {
                if (resourcePack.file.isBlank()) {
                    plugin.logger.warning(
                        "resource-pack mode '${resourcePack.mode}' is enabled, but resource-pack.file is empty."
                    )
                    return
                }

                val file = plugin.dataFolder.resolve(resourcePack.file)
                if (!file.isFile) {
                    plugin.logger.warning(
                        "resource-pack mode '${resourcePack.mode}' is enabled, but file '${resourcePack.file}' was not found in " +
                            plugin.dataFolder.absolutePath + "."
                    )
                }
            }
            else -> {
                plugin.logger.warning("Unknown resource-pack.mode '${resourcePack.mode}'. Use external or builtin.")
            }
        }
    }
}
