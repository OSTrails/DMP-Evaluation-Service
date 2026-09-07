package io.github.ostrails.dmpevaluatorservice.auth.service

import io.github.ostrails.dmpevaluatorservice.auth.model.*
import io.github.ostrails.dmpevaluatorservice.auth.repository.ClientRepository
import io.github.ostrails.dmpevaluatorservice.exceptionHandler.ClientDisabledException
import io.github.ostrails.dmpevaluatorservice.exceptionHandler.InvalidCredentialsException
import io.github.ostrails.dmpevaluatorservice.exceptionHandler.ResourceNotFoundException
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.dao.DuplicateKeyException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class ClientService(
    private val clientRepository: ClientRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService
) {

    suspend fun authenticate(clientId: String, clientSecret: String): TokenResponse {
        val client = clientRepository.findByClientId(clientId).awaitFirstOrNull()
            ?: throw InvalidCredentialsException("Invalid credentials")
        if (!client.enabled)
            throw ClientDisabledException("Client is disabled")
        if (!passwordEncoder.matches(clientSecret, client.passwordHash))
            throw InvalidCredentialsException("Invalid credentials")
        return TokenResponse(
            accessToken = jwtService.generateToken(client),
            expiresIn = jwtService.expirationSeconds
        )
    }

    suspend fun changeSecret(clientId: String, currentSecret: String, newSecret: String) {
        val client = clientRepository.findByClientId(clientId).awaitFirstOrNull()
            ?: throw ResourceNotFoundException("Client $clientId not found")
        if (!passwordEncoder.matches(currentSecret, client.passwordHash))
            throw IllegalArgumentException("Current secret is incorrect")
        clientRepository.save(client.copy(passwordHash = passwordEncoder.encode(newSecret))).awaitSingle()
    }

    suspend fun registerClient(request: RegisterClientRequest): ClientResponse {
        if (clientRepository.findByClientId(request.clientId).awaitFirstOrNull() != null)
            throw IllegalArgumentException("Client ID '${request.clientId}' already exists")
        val client = Client(
            clientId = request.clientId,
            passwordHash = passwordEncoder.encode(request.clientSecret),
            displayName = request.displayName,
            roles = request.roles
        )
        try {
            return clientRepository.save(client).awaitSingle().toResponse()
        } catch (e: DuplicateKeyException) {
            throw IllegalArgumentException("Client ID '${request.clientId}' already exists")
        }
    }

    suspend fun listClients(): List<ClientResponse> =
        clientRepository.findAll().collectList().awaitSingle().map { it.toResponse() }

    suspend fun deleteClient(clientId: String) {
        val client = clientRepository.findByClientId(clientId).awaitFirstOrNull()
            ?: throw ResourceNotFoundException("Client $clientId not found")
        clientRepository.delete(client).awaitFirstOrNull()
    }

    suspend fun resetSecret(clientId: String, newSecret: String): ClientResponse {
        val client = clientRepository.findByClientId(clientId).awaitFirstOrNull()
            ?: throw ResourceNotFoundException("Client $clientId not found")
        return clientRepository.save(client.copy(passwordHash = passwordEncoder.encode(newSecret)))
            .awaitSingle().toResponse()
    }

    suspend fun createClientIfNotExists(
        clientId: String,
        clientSecret: String,
        displayName: String,
        roles: Set<Role>
    ): Boolean {
        if (clientRepository.findByClientId(clientId).awaitFirstOrNull() != null) return false
        val client = Client(
            clientId = clientId,
            passwordHash = passwordEncoder.encode(clientSecret),
            displayName = displayName,
            roles = roles
        )
        return try {
            clientRepository.save(client).awaitSingle()
            true
        } catch (e: DuplicateKeyException) {
            false
        }
    }
}
