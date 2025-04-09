package io.github.ostrails.dmpevaluatorservice.controller

import io.github.ostrails.dmpevaluatorservice.plugin.EvaluatorPlugin
import io.github.ostrails.dmpevaluatorservice.service.PluginManagerService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/plugins")
class PluginController(
    val pluginManagerService: PluginManagerService
) {

    @GetMapping()
    suspend fun getAllPlugins(): ResponseEntity<List<EvaluatorPlugin>> {
        val plugins = pluginManagerService.getEvaluators()
        return ResponseEntity.ok(plugins)
    }



}