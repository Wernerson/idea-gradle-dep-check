package net.sebyte.plugins

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import net.sebyte.checker.isVulnerable

fun Application.configureRouting() {
    routing {
        get("/check/{signature}") {
            val signature = call.parameters["signature"]
            if (signature != null && isVulnerable(signature)) {
                call.respondText("Vulnerable!")
            } else {
                call.respondText("OK!")
            }
        }
    }
}
