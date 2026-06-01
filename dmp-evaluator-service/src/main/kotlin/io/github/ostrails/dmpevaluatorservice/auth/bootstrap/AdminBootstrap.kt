package io.github.ostrails.dmpevaluatorservice.auth.bootstrap

import io.github.ostrails.dmpevaluatorservice.auth.model.Role
import io.github.ostrails.dmpevaluatorservice.auth.service.ClientService
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

@Component
class AdminBootstrap(
    private val clientService: ClientService,
    @Value("\${admin.client-id:admin}") private val adminClientId: String,
    @Value("\${admin.client-secret}") private val adminClientSecret: String,
    @Value("\${admin.display-name:System Administrator}") private val adminDisplayName: String
) : ApplicationRunner {

    private val logger = LoggerFactory.getLogger(AdminBootstrap::class.java)

    override fun run(args: ApplicationArguments) = runBlocking {
        val created = clientService.createClientIfNotExists(
            clientId = adminClientId,
            clientSecret = adminClientSecret,
            displayName = adminDisplayName,
            roles = setOf(Role.ADMIN)
        )
        if (created) {
            logger.info("Bootstrap: admin client '{}' created successfully", adminClientId)
        } else {
            logger.info("Bootstrap: admin client '{}' already exists, skipping", adminClientId)
        }
    }
}
