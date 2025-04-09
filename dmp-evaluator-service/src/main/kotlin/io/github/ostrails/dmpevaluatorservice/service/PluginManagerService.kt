package io.github.ostrails.dmpevaluatorservice.service

import io.github.ostrails.dmpevaluatorservice.plugin.EvaluatorPlugin
import org.springframework.plugin.core.PluginRegistry
import org.springframework.stereotype.Service

@Service
class PluginManagerService(
    private val pluginRegistry: PluginRegistry<EvaluatorPlugin, String>
) {
    fun getEvaluators(): List<EvaluatorPlugin> {
        return pluginRegistry.plugins
    }
}