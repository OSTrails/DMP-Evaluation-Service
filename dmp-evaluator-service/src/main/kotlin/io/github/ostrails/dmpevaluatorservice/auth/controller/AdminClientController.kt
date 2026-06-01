package io.github.ostrails.dmpevaluatorservice.auth.controller

import io.github.ostrails.dmpevaluatorservice.auth.model.ClientResponse
import io.github.ostrails.dmpevaluatorservice.auth.model.RegisterClientRequest
import io.github.ostrails.dmpevaluatorservice.auth.model.ResetSecretRequest
import io.github.ostrails.dmpevaluatorservice.auth.service.ClientService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Tag(name = "Admin — Client Management", description = "Register and manage API clients. Requires ADMIN role.")
@RestController
@RequestMapping("/admin/clients")
class AdminClientController(private val clientService: ClientService) {

    @Operation(
        summary = "Register a new client",
        description = "Creates a new API client with the specified role (ADMIN or WRITER)",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @PostMapping
    suspend fun register(@RequestBody request: RegisterClientRequest): ResponseEntity<ClientResponse> {
        val result = clientService.registerClient(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(result)
    }

    @Operation(
        summary = "List all registered clients",
        description = "Returns all clients — passwords are never included",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @GetMapping
    suspend fun list(): ResponseEntity<List<ClientResponse>> =
        ResponseEntity.ok(clientService.listClients())

    @Operation(
        summary = "Revoke a client",
        description = "Permanently removes the client. The client will no longer be able to authenticate.",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @DeleteMapping("/{clientId}")
    suspend fun revoke(@PathVariable clientId: String): ResponseEntity<Void> {
        clientService.deleteClient(clientId)
        return ResponseEntity.noContent().build()
    }

    @Operation(
        summary = "Reset a client's secret",
        description = "Admin sets a new secret for any client without needing the old one",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @PutMapping("/{clientId}/reset-secret")
    suspend fun resetSecret(
        @PathVariable clientId: String,
        @RequestBody request: ResetSecretRequest
    ): ResponseEntity<ClientResponse> {
        val result = clientService.resetSecret(clientId, request.newSecret)
        return ResponseEntity.ok(result)
    }
}
