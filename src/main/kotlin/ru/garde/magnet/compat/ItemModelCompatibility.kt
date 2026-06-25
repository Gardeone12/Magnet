// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Garde1 / Gardeone12

package ru.garde.magnet.compat

import org.bukkit.NamespacedKey
import org.bukkit.inventory.meta.ItemMeta
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

class ItemModelCompatibility(
    private val itemModelKey: NamespacedKey,
    private val customModelData: Int
) {
    val expectedItemModelKey: String = itemModelKey.toString()
    val hasItemModelApi: Boolean = itemModelMethods.available
    val hasCustomModelDataComponentApi: Boolean = customModelDataComponentMethods.available
    val hasLegacyCustomModelDataApi: Boolean = legacyCustomModelDataMethods.available

    fun hasPortableMagnetVisuals(meta: ItemMeta): Boolean {
        return if (hasItemModelApi) {
            itemModelKey(meta) == expectedItemModelKey
        } else {
            customModelDataComponentMatches(meta) || legacyCustomModelData(meta) == customModelData
        }
    }

    fun applyPortableMagnetVisuals(meta: ItemMeta) {
        setItemModel(meta)

        if (!setCustomModelDataComponent(meta)) {
            setLegacyCustomModelData(meta)
        }
    }

    fun debug(meta: ItemMeta): PortableMagnetVisualDebug {
        val itemModel = itemModelKey(meta) ?: "none"
        val legacy = legacyCustomModelData(meta)
        val component = customModelDataComponentSummary(meta) ?: "none"
        val modernAvailable = hasItemModelApi && itemModel == expectedItemModelKey
        val customDataAvailable = customModelDataComponentMatches(meta) || legacy == customModelData
        val bestPath = when {
            modernAvailable -> "item_model"
            customDataAvailable -> "custom_model_data"
            else -> "none"
        }

        return PortableMagnetVisualDebug(
            itemModelKey = itemModel,
            itemModelMatches = itemModel == expectedItemModelKey,
            customModelDataComponent = component,
            legacyCustomModelData = legacy?.toString() ?: "none",
            bestAvailableVisualPath = bestPath
        )
    }

    private fun itemModelKey(meta: ItemMeta): String? {
        if (!hasItemModelApi) return null
        if (invokeBoolean(itemModelMethods.hasItemModel, meta) != true) return null

        val key = invoke(itemModelMethods.getItemModel, meta) as? NamespacedKey ?: return null
        return key.toString()
    }

    private fun setItemModel(meta: ItemMeta): Boolean {
        if (!hasItemModelApi) return false
        return invoke(itemModelMethods.setItemModel, meta, itemModelKey) != InvocationFailed
    }

    private fun setCustomModelDataComponent(meta: ItemMeta): Boolean {
        if (!hasCustomModelDataComponentApi) return false

        val component = invoke(customModelDataComponentMethods.getCustomModelDataComponent, meta)
            ?: return false
        if (component == InvocationFailed) return false

        val floatsSet = invokeComponent(component, "setFloats", listOf(customModelData.toFloat())) != InvocationFailed
        val stringsSet = invokeComponent(component, "setStrings", listOf(expectedItemModelKey)) != InvocationFailed
        if (!floatsSet && !stringsSet) return false

        return invoke(customModelDataComponentMethods.setCustomModelDataComponent, meta, component) != InvocationFailed
    }

    private fun customModelDataComponentMatches(meta: ItemMeta): Boolean {
        if (!hasCustomModelDataComponentApi) return false

        val hasComponent = customModelDataComponentMethods.hasCustomModelDataComponent
            ?.let { invokeBoolean(it, meta) }
        if (hasComponent == false) return false

        val component = invoke(customModelDataComponentMethods.getCustomModelDataComponent, meta)
            ?: return false
        if (component == InvocationFailed) return false

        val floats = invokeComponent(component, "getFloats") as? List<*>
        val strings = invokeComponent(component, "getStrings") as? List<*>

        return floats?.any { (it as? Number)?.toInt() == customModelData } == true ||
            strings?.any { it == expectedItemModelKey } == true
    }

    private fun customModelDataComponentSummary(meta: ItemMeta): String? {
        if (!hasCustomModelDataComponentApi) return null

        val hasComponent = customModelDataComponentMethods.hasCustomModelDataComponent
            ?.let { invokeBoolean(it, meta) }
        if (hasComponent == false) return null

        val component = invoke(customModelDataComponentMethods.getCustomModelDataComponent, meta)
            ?: return null
        if (component == InvocationFailed) return null

        val parts = mutableListOf<String>()
        appendComponentList(parts, component, "getFloats", "floats")
        appendComponentList(parts, component, "getStrings", "strings")
        appendComponentList(parts, component, "getFlags", "flags")
        appendComponentList(parts, component, "getColors", "colors")

        return parts.joinToString("; ").ifBlank { "empty" }
    }

    private fun appendComponentList(
        parts: MutableList<String>,
        component: Any,
        methodName: String,
        label: String
    ) {
        val values = invokeComponent(component, methodName) as? List<*> ?: return
        if (values.isNotEmpty()) {
            parts += "$label=${values.joinToString()}"
        }
    }

    private fun setLegacyCustomModelData(meta: ItemMeta): Boolean {
        if (!hasLegacyCustomModelDataApi) return false
        return invoke(legacyCustomModelDataMethods.setCustomModelData, meta, customModelData) != InvocationFailed
    }

    private fun legacyCustomModelData(meta: ItemMeta): Int? {
        if (!hasLegacyCustomModelDataApi) return null
        if (invokeBoolean(legacyCustomModelDataMethods.hasCustomModelData, meta) != true) return null

        return (invoke(legacyCustomModelDataMethods.getCustomModelData, meta) as? Number)?.toInt()
    }

    private fun invoke(method: Method?, target: Any, vararg args: Any?): Any? {
        if (method == null) return InvocationFailed

        return try {
            method.invoke(target, *args)
        } catch (_: IllegalAccessException) {
            InvocationFailed
        } catch (_: InvocationTargetException) {
            InvocationFailed
        } catch (_: LinkageError) {
            InvocationFailed
        } catch (_: RuntimeException) {
            InvocationFailed
        }
    }

    private fun invokeBoolean(method: Method?, target: Any): Boolean? {
        return invoke(method, target) as? Boolean
    }

    private fun invokeComponent(component: Any, methodName: String, argument: Any? = NoArgument): Any? {
        val method = component.javaClass.methods.firstOrNull {
            it.name == methodName &&
                if (argument == NoArgument) it.parameterCount == 0 else it.parameterCount == 1
        } ?: return InvocationFailed

        return if (argument == NoArgument) {
            invoke(method, component)
        } else {
            invoke(method, component, argument)
        }
    }

    private data class ItemModelMethods(
        val hasItemModel: Method?,
        val getItemModel: Method?,
        val setItemModel: Method?
    ) {
        val available: Boolean = hasItemModel != null && getItemModel != null && setItemModel != null
    }

    private data class CustomModelDataComponentMethods(
        val hasCustomModelDataComponent: Method?,
        val getCustomModelDataComponent: Method?,
        val setCustomModelDataComponent: Method?
    ) {
        val available: Boolean = getCustomModelDataComponent != null && setCustomModelDataComponent != null
    }

    private data class LegacyCustomModelDataMethods(
        val hasCustomModelData: Method?,
        val getCustomModelData: Method?,
        val setCustomModelData: Method?
    ) {
        val available: Boolean = hasCustomModelData != null && getCustomModelData != null && setCustomModelData != null
    }

    private companion object {
        private val itemMetaClass = ItemMeta::class.java
        private val itemModelMethods = ItemModelMethods(
            hasItemModel = method("hasItemModel"),
            getItemModel = method("getItemModel"),
            setItemModel = method("setItemModel", NamespacedKey::class.java)
        )
        private val customModelDataComponentMethods = CustomModelDataComponentMethods(
            hasCustomModelDataComponent = method("hasCustomModelDataComponent"),
            getCustomModelDataComponent = method("getCustomModelDataComponent"),
            setCustomModelDataComponent = oneParameterMethod("setCustomModelDataComponent")
        )
        private val legacyCustomModelDataMethods = LegacyCustomModelDataMethods(
            hasCustomModelData = method("hasCustomModelData"),
            getCustomModelData = method("getCustomModelData"),
            setCustomModelData = method("setCustomModelData", Int::class.javaObjectType)
        )

        private object InvocationFailed
        private object NoArgument

        private fun method(name: String, vararg parameterTypes: Class<*>): Method? {
            return try {
                itemMetaClass.getMethod(name, *parameterTypes)
            } catch (_: NoSuchMethodException) {
                null
            } catch (_: LinkageError) {
                null
            }
        }

        private fun oneParameterMethod(name: String): Method? {
            return try {
                itemMetaClass.methods.firstOrNull { it.name == name && it.parameterCount == 1 }
            } catch (_: LinkageError) {
                null
            }
        }
    }
}

data class PortableMagnetVisualDebug(
    val itemModelKey: String,
    val itemModelMatches: Boolean,
    val customModelDataComponent: String,
    val legacyCustomModelData: String,
    val bestAvailableVisualPath: String
) {
    companion object {
        fun none(expectedItemModelKey: String): PortableMagnetVisualDebug {
            return PortableMagnetVisualDebug(
                itemModelKey = "none",
                itemModelMatches = false,
                customModelDataComponent = "none",
                legacyCustomModelData = "none",
                bestAvailableVisualPath = "none (expected $expectedItemModelKey)"
            )
        }
    }
}
