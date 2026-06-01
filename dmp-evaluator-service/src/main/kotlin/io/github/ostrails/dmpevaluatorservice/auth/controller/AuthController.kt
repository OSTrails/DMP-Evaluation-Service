package io.github.ostrails.dmpevaluatorservice.auth.controller

import io.github.ostrails.dmpevaluatorservice.auth.model.ChangeSecretRequest
import io.github.ostrails.dmpevaluatorservice.auth.model.TokenRequest
import io.github.ostrails.dmpevaluatorservice.auth.model.TokenResponse
import io.github.ostrails.dmpevaluatorservice.auth.service.ClientService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@Tag(name = "Authentication", description = "Obtain and manage JWT tokens")
@RestController
@RequestMapping("/auth")
class AuthController(private val clientService: ClientService) {

    @Operation(summary = "Obtain a JWT token", description = "Authenticate with clientId and clientSecret to receive a Bearer token valid for 1 hour")
    @PostMapping("/token")
    suspend fun token(@RequestBody request: TokenRequest): ResponseEntity<TokenResponse> {
        val response = clientService.authenticate(request.clientId, request.clientSecret)
        return ResponseEntity.ok(response)
    }

    @Operation(
        summary = "Change your own secret",
        description = "Requires a valid Bearer token plus the current secret as proof of identity",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @PutMapping("/change-secret")
    suspend fun changeSecret(
        @RequestBody request: ChangeSecretRequest,
        authentication: Authentication
    ): ResponseEntity<Void> {
        clientService.changeSecret(authentication.name, request.currentSecret, request.newSecret)
        return ResponseEntity.noContent().build()
    }
}
