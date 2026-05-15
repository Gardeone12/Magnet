package ru.garde.magnet

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import java.util.Locale

object MagnetPlugin : JavaPlugin() {

    private const val DEFAULT_LANGUAGE = "en"

    private lateinit var magnetKey: NamespacedKey
    private val supportedLanguages = setOf(DEFAULT_LANGUAGE, "ru")
    private val messages = mutableMapOf<String, YamlConfiguration>()

    private val ferromagneticMaterials = setOf(
        Material.IRON_INGOT,
        Material.RAW_IRON,
        Material.IRON_NUGGET,
        Material.IRON_BLOCK,
        Material.RAW_IRON_BLOCK,
        Material.IRON_ORE,
        Material.DEEPSLATE_IRON_ORE,

        Material.IRON_SWORD,
        Material.IRON_PICKAXE,
        Material.IRON_AXE,
        Material.IRON_SHOVEL,
        Material.IRON_HOE,

        Material.IRON_HELMET,
        Material.IRON_CHESTPLATE,
        Material.IRON_LEGGINGS,
        Material.IRON_BOOTS,
        Material.IRON_HORSE_ARMOR,

        Material.CHAINMAIL_HELMET,
        Material.CHAINMAIL_CHESTPLATE,
        Material.CHAINMAIL_LEGGINGS,
        Material.CHAINMAIL_BOOTS,

        Material.ANVIL,
        Material.CHIPPED_ANVIL,
        Material.DAMAGED_ANVIL,

        Material.IRON_BARS,
        Material.IRON_DOOR,
        Material.IRON_TRAPDOOR,
        Material.HEAVY_WEIGHTED_PRESSURE_PLATE,

        Material.HOPPER,
        Material.CAULDRON,
        Material.LANTERN,
        Material.SOUL_LANTERN,

        Material.BUCKET,
        Material.WATER_BUCKET,
        Material.LAVA_BUCKET,
        Material.MILK_BUCKET,
        Material.POWDER_SNOW_BUCKET,

        Material.SHEARS,
        Material.FLINT_AND_STEEL,
        Material.COMPASS,
        Material.RECOVERY_COMPASS,

        Material.RAIL,
        Material.POWERED_RAIL,
        Material.DETECTOR_RAIL,
        Material.ACTIVATOR_RAIL,

        Material.MINECART,
        Material.CHEST_MINECART,
        Material.FURNACE_MINECART,
        Material.HOPPER_MINECART,
        Material.TNT_MINECART
    )

    private val weakMagneticMaterials = setOf(
        Material.NETHERITE_INGOT,
        Material.NETHERITE_SCRAP,
        Material.NETHERITE_BLOCK,

        Material.NETHERITE_SWORD,
        Material.NETHERITE_PICKAXE,
        Material.NETHERITE_AXE,
        Material.NETHERITE_SHOVEL,
        Material.NETHERITE_HOE,

        Material.NETHERITE_HELMET,
        Material.NETHERITE_CHESTPLATE,
        Material.NETHERITE_LEGGINGS,
        Material.NETHERITE_BOOTS
    )

    override fun onEnable() {
        magnetKey = NamespacedKey(this, "magnet")

        loadMessages()
        startMagnetTask()

        logger.info("MagnetPlugin enabled")
    }

    private fun loadMessages() {
        for (language in supportedLanguages) {
            saveResource("lang/$language.yml", false)
            messages[language] = YamlConfiguration.loadConfiguration(
                dataFolder.resolve("lang/$language.yml")
            )
        }
    }

    private fun startMagnetTask() {
        Bukkit.getScheduler().runTaskTimer(this, Runnable {
            for (player in Bukkit.getOnlinePlayers()) {
                if (!hasMagnet(player)) continue

                attractItems(player)
            }
        }, 20L, 2L)
    }

    private fun attractItems(player: Player) {
        val radius = 7.0
        val baseSpeed = 0.35

        val magnetLocation = player.location.clone().add(0.0, 1.0, 0.0)

        for (entity in player.getNearbyEntities(radius, radius, radius)) {
            if (entity !is Item) continue
            if (!entity.isValid || entity.isDead) continue

            val itemStack = entity.itemStack
            val magneticMultiplier = getMagneticMultiplier(itemStack.type) ?: continue

            val direction = magnetLocation.toVector()
                .subtract(entity.location.toVector())

            if (direction.length() <= 0.3) continue

            entity.velocity = direction
                .normalize()
                .multiply(baseSpeed * magneticMultiplier)
        }
    }

    private fun getMagneticMultiplier(material: Material): Double? {
        return when {
            material in ferromagneticMaterials -> 1.0
            material in weakMagneticMaterials -> 0.5
            else -> null
        }
    }

    private fun hasMagnet(player: Player): Boolean {
        return isMagnet(player.inventory.itemInMainHand) ||
                isMagnet(player.inventory.itemInOffHand)
    }

    private fun isMagnet(item: ItemStack?): Boolean {
        if (item == null || item.type == Material.AIR) return false

        val meta = item.itemMeta ?: return false

        return meta.persistentDataContainer.has(
            magnetKey,
            PersistentDataType.BYTE
        )
    }

    private fun createMagnet(language: String): ItemStack {
        val item = ItemStack(Material.AMETHYST_SHARD)
        val meta = item.itemMeta

        meta.displayName(
            Component.text(message(language, "item.name"))
                .color(NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false)
        )

        meta.lore(
            messageList(language, "item.lore").mapIndexed { index, line ->
                Component.text(line)
                    .color(if (index == 0) NamedTextColor.GRAY else NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false)
            }
        )

        meta.persistentDataContainer.set(
            magnetKey,
            PersistentDataType.BYTE,
            1
        )

        item.itemMeta = meta
        return item
    }

    private fun languageFor(player: Player): String {
        val language = player.locale().language.lowercase(Locale.ROOT)
        return if (language in supportedLanguages) language else DEFAULT_LANGUAGE
    }

    private fun message(language: String, path: String): String {
        return messages[language]?.getString(path)
            ?: messages[DEFAULT_LANGUAGE]?.getString(path)
            ?: path
    }

    private fun messageList(language: String, path: String): List<String> {
        val localized = messages[language]?.getStringList(path).orEmpty()
        if (localized.isNotEmpty()) return localized

        val fallback = messages[DEFAULT_LANGUAGE]?.getStringList(path).orEmpty()
        return fallback.ifEmpty { listOf(path) }
    }

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (!command.name.equals("magnet", ignoreCase = true)) {
            return false
        }

        if (sender !is Player) {
            sender.sendMessage(message(DEFAULT_LANGUAGE, "command.player-only"))
            return true
        }

        val language = languageFor(sender)

        sender.inventory.addItem(createMagnet(language))

        sender.sendMessage(
            Component.text(message(language, "command.received"))
                .color(NamedTextColor.GREEN)
        )

        return true
    }
}
