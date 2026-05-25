package ru.garde.magnet

import io.papermc.paper.plugin.bootstrap.BootstrapContext
import io.papermc.paper.plugin.bootstrap.PluginBootstrap
import io.papermc.paper.plugin.bootstrap.PluginProviderContext
import org.bukkit.plugin.java.JavaPlugin

internal class MagnetPluginBootsTrap : PluginBootstrap {
    override fun bootstrap(context: BootstrapContext) {
        // Bootstrap logic is not needed yet.
    }

    override fun createPlugin(context: PluginProviderContext): JavaPlugin {
        return MagnetPlugin
    }
}
