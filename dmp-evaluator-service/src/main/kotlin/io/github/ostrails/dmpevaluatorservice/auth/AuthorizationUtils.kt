package io.github.ostrails.dmpevaluatorservice.auth

import io.github.ostrails.dmpevaluatorservice.auth.model.Role
import io.github.ostrails.dmpevaluatorservice.exceptionHandler.ForbiddenException
import org.springframework.security.core.Authentication

fun Authentication.isAdmin(): Boolean =
    authorities.any { it.authority == "ROLE_${Role.ADMIN.name}" }

fun checkOwnership(createdBy: String?, callerClientId: String, isAdmin: Boolean) {
    if (isAdmin) return
    if (createdBy == null)
        throw ForbiddenException("Only ADMIN can modify records without an owner")
    if (createdBy != callerClientId)
        throw ForbiddenException("You can only modify records you created")
}
