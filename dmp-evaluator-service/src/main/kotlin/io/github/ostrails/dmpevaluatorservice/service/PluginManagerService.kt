package io.github.ostrails.dmpevaluatorservice.service

import io.github.ostrails.dmpevaluatorservice.model.PluginInfo
import io.github.ostrails.dmpevaluatorservice.plugin.EvaluatorPlugin
import io.github.ostrails.dmpevaluatorservice.plugin.ExternalBenchmarkPlugin
import org.springframework.plugin.core.PluginRegistry
import org.springframework.stereotype.Service
import java.util.*

@Service
class PluginManagerService(
    private val pluginRegistry: PluginRegistry<EvaluatorPlugin, String>
) {

    fun getEvaluators(): List<PluginInfo> {
        return pluginRegistry.plugins.map { plugin ->
            PluginInfo(
                pluginId = plugin.getPluginIdentifier(),
                description = plugin.getPluginInformation().description,
                functions = plugin.resolvedFunctionNames(),
            )
        }
    }

    fun getEvaluatorByPluginId(pluginId: String): PluginInfo {
        val plugin = pluginRegistry.getPluginFor(pluginId).orElseThrow {
            IllegalArgumentException("Plugin '$pluginId' not found")
        }
        return PluginInfo(
            pluginId = plugin.getPluginIdentifier(),
            description = plugin.getPluginInformation().description,
            functions = plugin.resolvedFunctionNames(),
        )
    }

    private fun EvaluatorPlugin.resolvedFunctionNames(): List<String> =
        if (this is ExternalBenchmarkPlugin) benchmarkFunctionMap.keys.toList()
        else functionMap.keys.toList()
}
