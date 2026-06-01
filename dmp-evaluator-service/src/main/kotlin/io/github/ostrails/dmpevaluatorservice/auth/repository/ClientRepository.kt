package io.github.ostrails.dmpevaluatorservice.auth.repository

import io.github.ostrails.dmpevaluatorservice.auth.model.Client
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Mono

interface ClientRepository : ReactiveMongoRepository<Client, String> {
    fun findByClientId(clientId: String): Mono<Client>
}
