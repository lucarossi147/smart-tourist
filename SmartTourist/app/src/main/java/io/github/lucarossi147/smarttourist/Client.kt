package io.github.lucarossi147.smarttourist

import io.ktor.client.*
import io.ktor.client.engine.android.*

object Client {
    val client = HttpClient(Android)
}