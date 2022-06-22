package io.github.lucarossi147

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import java.util.*

class JWTConfig(
    private val secret: String,
    private val issuer: String,
    private val audience: String,
    val realm: String
){
    fun generateToken(username: String): String? {
        return JWT.create()
            //.withAudience(audience)
            //.withIssuer(issuer)
            .withClaim("username", username)
            .withExpiresAt(Date(System.currentTimeMillis() + 60000 * 60 * 24)) //A day
            .sign(Algorithm.HMAC256(secret))
    }

    fun generateVerifier(): JWTVerifier {
        return JWT.require(Algorithm.HMAC256(secret))
            //.withAudience(audience)
            //.withIssuer(issuer)
            .build()
    }
}