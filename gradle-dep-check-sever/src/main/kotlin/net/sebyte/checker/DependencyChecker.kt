package net.sebyte.checker

private val VULNERABLE_DEPENDENCIES = setOf(
    "com.tngtech.archunit:archunit-junit5:1.0.0",
    "com.auth0:java-jwt:4.2.1",
    "com.google.gms:google-services:4.3.13",
)

fun isVulnerable(signature: String): Boolean = signature in VULNERABLE_DEPENDENCIES
