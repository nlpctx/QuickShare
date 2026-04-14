package dev.haas.quickshare.core.controllers

import dev.haas.quickshare.core.models.PlatformFile
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.response.respondText
import io.ktor.server.engine.EmbeddedServer
import dev.haas.quickshare.core.models.SharedPhotoInfo
import dev.haas.quickshare.core.security.RequestValidator

actual class PlatformServerController actual constructor() {
    private var server: EmbeddedServer<*, *>? = null
    private var _isRunning: Boolean = false
    actual val isRunning: Boolean get() = _isRunning

    actual suspend fun start(port: Int, files: List<PlatformFile>, token: String) {
        stop()
        
        server = embeddedServer(CIO, port = port) {
            install(CORS) {
                anyHost()
            }
            install(ContentNegotiation) {
                json()
            }
            
            routing {
                get("/photos") {
                    val clientToken = call.request.queryParameters["token"]
                    if (!RequestValidator.validate(clientToken, token)) {
                        call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                        return@get
                    }
                    
                    val photos = files.mapIndexed { index, file ->
                        SharedPhotoInfo(
                            id = index.toString(),
                            name = file.name,
                            size = file.size,
                            thumbnailUrl = "/thumbnail/$index?token=$token",
                            downloadUrl = "/download/$index?token=$token"
                        )
                    }
                    call.respond(photos)
                }

                get("/thumbnail/{id}") {
                    val clientToken = call.request.queryParameters["token"]
                    if (!RequestValidator.validate(clientToken, token)) {
                        call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                        return@get
                    }
                    
                    val id = call.parameters["id"]?.toIntOrNull()
                    if (id != null && id in files.indices) {
                        val bytes = files[id].readBytes()
                        call.respondBytes(bytes, ContentType.Image.Any)
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }

                get("/download/{id}") {
                    val clientToken = call.request.queryParameters["token"]
                    if (!RequestValidator.validate(clientToken, token)) {
                        call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                        return@get
                    }
                    
                    val id = call.parameters["id"]?.toIntOrNull()
                    if (id != null && id in files.indices) {
                        val file = files[id]
                        call.response.headers.append("Content-Disposition", "attachment; filename=\"${file.name}\"")
                        call.respondBytes(file.readBytes(), ContentType.Application.OctetStream)
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }
                
                get("/") {
                    val clientToken = call.request.queryParameters["token"]
                    if (!RequestValidator.validate(clientToken, token)) {
                        call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                        return@get
                    }
                    
                    val html = """
                        <!DOCTYPE html>
                        <html lang="en">
                        <head>
                            <meta charset="UTF-8">
                            <meta name="viewport" content="width=device-width, initial-scale=1.0">
                            <title>QuickShare Gallery</title>
                            <style>
                                body { font-family: -apple-system, sans-serif; background: #16161D; color: white; margin: 0; padding: 20px; text-align: center; }
                                .grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(150px, 1fr)); gap: 15px; }
                                .item { background: #222; border-radius: 8px; overflow: hidden; }
                                img { width: 100%; aspect-ratio: 1; object-fit: cover; }
                                .name { padding: 5px; font-size: 12px; }
                                a { color: #fff; text-decoration: none; display: block; padding: 5px; background: #333; }
                            </style>
                        </head>
                        <body>
                            <h1>QuickShare Gallery (iOS)</h1>
                            <div class="grid" id="grid">Loading...</div>
                            <script>
                                async function load() {
                                    const res = await fetch('/photos?token=$token');
                                    const photos = await res.json();
                                    const grid = document.getElementById('grid');
                                    grid.innerHTML = photos.map(p => `
                                        <div class="item">
                                            <img src="${'$'}{p.thumbnailUrl}" />
                                            <div class="name">${'$'}{p.name}</div>
                                            <a href="${'$'}{p.downloadUrl}" download>Download</a>
                                        </div>
                                    `).join('');
                                }
                                load();
                            </script>
                        </body>
                        </html>
                    """.trimIndent()
                    call.respondText(html, ContentType.Text.Html)
                }
            }
        }.start(wait = false)
        _isRunning = true
    }

    actual fun addShare(files: List<PlatformFile>, token: String) {
        // iOS: no-op for now – server is single-session
    }

    actual suspend fun stop() {
        server?.stop(500, 1000)
        server = null
        _isRunning = false
    }
}
