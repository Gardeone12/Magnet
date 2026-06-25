// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Garde1 / Gardeone12

package ru.garde.magnet

import org.bukkit.plugin.java.JavaPlugin
import ru.garde.magnet.command.CommandContext
import ru.garde.magnet.command.CoreCommand
import ru.garde.magnet.command.DebugCommand
import ru.garde.magnet.command.GiveCommand
import ru.garde.magnet.command.MagnetCommand
import ru.garde.magnet.command.MagnetTabCompleter
import ru.garde.magnet.command.ProfileCommand
import ru.garde.magnet.command.ReloadCommand
import ru.garde.magnet.compat.ItemModelCompatibility
import ru.garde.magnet.compat.MagnetCompatibility
import ru.garde.magnet.compat.ServerVersion
import ru.garde.magnet.config.MagnetConfig
import ru.garde.magnet.core.MagnetCoreManager
import ru.garde.magnet.core.MagnetStructureBreakListener
import ru.garde.magnet.message.MessageService
import ru.garde.magnet.portable.MagnetItemFactory
import ru.garde.magnet.portable.PortableMagnetService
import ru.garde.magnet.resourcepack.ResourcePackService

class MagnetPlugin : JavaPlugin() {
    private lateinit var magnetConfig: MagnetConfig
    private lateinit var messages: MessageService
    private lateinit var itemModelCompatibility: ItemModelCompatibility
    private lateinit var compatibility: MagnetCompatibility
    private lateinit var itemFactory: MagnetItemFactory
    private lateinit var portableMagnetService: PortableMagnetService
    private lateinit var resourcePackService: ResourcePackService
    private lateinit var coreManager: MagnetCoreManager

    override fun onEnable() {
        val serverVersion = ServerVersion.current()
        if (serverVersion != null && serverVersion < ServerVersion.minimumSupported) {
            logger.severe(
                "Unsupported Minecraft version $serverVersion. Magnet requires ${ServerVersion.minimumSupported} or newer."
            )
            server.pluginManager.disablePlugin(this)
            return
        }
        if (serverVersion == null) {
            logger.warning("Could not parse Minecraft version from '${server.bukkitVersion}'. Starting with compatibility fallbacks.")
        }

        saveDefaultConfig()
        reloadConfig()
        config.options().copyDefaults(true)
        saveConfig()

        magnetConfig = MagnetConfig(this)
        messages = MessageService(this)
        messages.reload()

        itemModelCompatibility = ItemModelCompatibility(
            MagnetItemFactory.portableMagnetItemModel,
            MagnetItemFactory.PORTABLE_MAGNET_CUSTOM_MODEL_DATA
        )
        compatibility = MagnetCompatibility.detect(itemModelCompatibility)

        itemFactory = MagnetItemFactory(this, itemModelCompatibility, messages)
        portableMagnetService = PortableMagnetService(this, itemFactory)
        resourcePackService = ResourcePackService(this, magnetConfig)
        resourcePackService.validateConfiguration()

        coreManager = MagnetCoreManager(this, portableMagnetService::getMagneticMultiplier)
        coreManager.load()

        if (!registerCommands()) {
            server.pluginManager.disablePlugin(this)
            return
        }

        server.pluginManager.registerEvents(MagnetStructureBreakListener(this, coreManager), this)
        portableMagnetService.start()
        coreManager.start()

        logger.info("MagnetPlugin enabled with portable material ${itemFactory.portableMagnetMaterial.name}")
        logger.info(compatibility.summary())
    }

    override fun onDisable() {
        if (::portableMagnetService.isInitialized) portableMagnetService.stop()
        if (::coreManager.isInitialized) coreManager.shutdown()
    }

    private fun registerCommands(): Boolean {
        val pluginCommand = getCommand("magnet")
        if (pluginCommand == null) {
            logger.severe("Command 'magnet' is missing from plugin.yml; disabling Magnet.")
            return false
        }

        val context = CommandContext(
            plugin = this,
            messages = messages,
            itemFactory = itemFactory,
            coreManager = coreManager,
            resourcePackService = resourcePackService
        )
        pluginCommand.setExecutor(
            MagnetCommand(
                context = context,
                giveCommand = GiveCommand(context),
                reloadCommand = ReloadCommand(context),
                debugCommand = DebugCommand(context),
                coreCommand = CoreCommand(context),
                profileCommand = ProfileCommand(context)
            )
        )
        pluginCommand.tabCompleter = MagnetTabCompleter(coreManager)
        return true
    }
}
