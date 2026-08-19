package io.github.ostrails.dmpevaluatorservice.service

import io.github.ostrails.dmpevaluatorservice.model.PluginInfo
import io.github.ostrails.dmpevaluatorservice.plugin.EvaluatorPlugin
import io.github.ostrails.dmpevaluatorservice.plugin.ExternalBenchmarkPlugin
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.plugin.core.PluginRegistry
import org.springframework.stereotype.Service

@Service
class PluginManagerService(
    private val pluginRegistry: PluginRegistry<EvaluatorPlugin, String>
) {

    private val log: Logger = LoggerFactory.getLogger(PluginManagerService::class.java)

    fun getEvaluators(): List<PluginInfo> {
        val evaluators = pluginRegistry.plugins.map { plugin ->
            PluginInfo(
                pluginId = plugin.getPluginIdentifier(),
                description = plugin.getPluginInformation().description,
                functions = plugin.resolvedFunctionNames(),
            )
        }
        log.debug("Listing ${evaluators.size} registered evaluator plugin(s)")
        return evaluators
    }

    fun getEvaluatorByPluginId(pluginId: String): PluginInfo {
        val plugin = pluginRegistry.getPluginFor(pluginId).orElseThrow {
            log.warn("Plugin '$pluginId' not found")
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
