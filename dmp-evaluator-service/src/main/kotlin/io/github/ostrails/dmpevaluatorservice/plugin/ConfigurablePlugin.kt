package io.github.ostrails.dmpevaluatorservice.plugin

import io.github.ostrails.dmpevaluatorservice.model.PluginInfo
import org.springframework.plugin.core.Plugin

interface ConfigurablePlugin<T, Q>: Plugin<T> {
    fun getPluginIdentifier(): String
    fun getPluginInformation(): PluginInfo
}