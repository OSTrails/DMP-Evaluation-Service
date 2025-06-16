package io.github.ostrails.dmpevaluatorservice.controller

import io.github.ostrails.dmpevaluatorservice.model.PluginInfo
import io.github.ostrails.dmpevaluatorservice.service.PluginManagerService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@Tag(name = "Plugin APIs", description = "Manage plugin APIs")
@RestController
@RequestMapping("/api/plugins")
class PluginController(
    val pluginManagerService: PluginManagerService
) {

    @GetMapping()
    suspend fun getAllPlugins(): ResponseEntity<List<PluginInfo>> {
        val plugins = pluginManagerService.getEvaluators()
        return ResponseEntity.ok(plugins)
    }

    @GetMapping("/{evaluatorId}")
    suspend fun getPlugin(@PathVariable evaluatorId: String): ResponseEntity<PluginInfo> {
        val plugin = pluginManagerService.getEvaluatorByPluginId(evaluatorId)
        return ResponseEntity.ok(plugin)
    }



}