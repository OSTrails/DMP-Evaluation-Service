package io.github.ostrails.dmpevaluatorservice.service

import io.github.ostrails.dmpevaluatorservice.model.PluginInfo
import io.github.ostrails.dmpevaluatorservice.plugin.EvaluatorPlugin
import org.springframework.plugin.core.PluginRegistry
import org.springframework.stereotype.Service
import java.util.*

@Service
class PluginManagerService(
    private val pluginRegistry: PluginRegistry<EvaluatorPlugin, String>
) {

    fun getEvaluators(): List<PluginInfo> {
        return pluginRegistry.plugins.map { plugin ->
            println("identifier , ${plugin.getPluginIdentifier()}")
            plugin.getPluginInformation()
        }
    }

    fun getEvaluatorByPluginId(pluginId: String): PluginInfo {
        val plugin =  pluginRegistry.getPluginFor(pluginId).orElseThrow {
            IllegalArgumentException("Plugin '$pluginId' not found")
        }

        return plugin.getPluginInformation()
    }

}