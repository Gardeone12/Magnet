package ru.garde.magnit

import io.papermc.paper.plugin.bootstrap.BootstrapContext
import io.papermc.paper.plugin.bootstrap.PluginBootstrap
import io.papermc.paper.plugin.bootstrap.PluginProviderContext
import org.bukkit.plugin.java.JavaPlugin
import ru.garde.magnet.MagnetPlugin

internal class MagnetPluginBootsTrap : PluginBootstrap {

    override fun bootstrap(context: BootstrapContext) {
        //plugin BootsTrap logic
    }

    override fun createPlugin(context: PluginProviderContext): JavaPlugin {
        return MagnetPlugin
    }
}