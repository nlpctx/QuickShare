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
import kotlinx.serialization.Serializable
import io.ktor.server.engine.EmbeddedServer
import dev.haas.quickshare.core.models.SharedPhotoInfo
import dev.haas.quickshare.core.security.RequestValidator
import android.content.Intent
import androidx.core.content.ContextCompat
import dev.haas.quickshare.core.AndroidContext
import dev.haas.quickshare.core.QuickShareService

actual class PlatformServerController actual constructor() {
    private var server: EmbeddedServer<*, *>? = null
    private var _isRunning: Boolean = false
    actual val isRunning: Boolean get() = _isRunning

    private val tokenFileMap = mutableMapOf<String, List<PlatformFile>>()

    actual suspend fun start(port: Int, files: List<PlatformFile>, token: String) {
        // Initial token share
        tokenFileMap[token] = files
        
        if (_isRunning) return 
        
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
                    val sessionFiles = tokenFileMap[clientToken]
                    if (sessionFiles == null) {
                        call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                        return@get
                    }
                    
                    val photos = sessionFiles.mapIndexed { index, file ->
                        SharedPhotoInfo(
                            id = index.toString(),
                            name = file.name,
                            size = file.size,
                            thumbnailUrl = "/thumbnail/$index?token=$clientToken",
                            downloadUrl = "/download/$index?token=$clientToken"
                        )
                    }
                    call.respond(photos)
                }

                get("/thumbnail/{id}") {
                    val clientToken = call.request.queryParameters["token"]
                    val sessionFiles = tokenFileMap[clientToken]
                    if (sessionFiles == null) {
                        call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                        return@get
                    }
                    
                    val id = call.parameters["id"]?.toIntOrNull()
                    if (id != null && id in sessionFiles.indices) {
                        val file = sessionFiles[id]
                        val contentType = if (file.name.endsWith(".mp4", ignoreCase = true)) ContentType.Video.MP4 else ContentType.Image.Any
                        call.respondBytes(file.readBytes(), contentType)
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }

                get("/download/{id}") {
                    val clientToken = call.request.queryParameters["token"]
                    val sessionFiles = tokenFileMap[clientToken]
                    if (sessionFiles == null) {
                        call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                        return@get
                    }
                    
                    val id = call.parameters["id"]?.toIntOrNull()
                    if (id != null && id in sessionFiles.indices) {
                        val file = sessionFiles[id]
                        call.response.headers.append("Content-Disposition", "attachment; filename=\"${file.name}\"")
                        val contentType = if (file.name.endsWith(".mp4", ignoreCase = true)) ContentType.Video.MP4 else ContentType.Application.OctetStream
                        call.respondBytes(file.readBytes(), contentType)
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }
                
                get("/") {
                    val clientToken = call.request.queryParameters["token"]
                    if (tokenFileMap[clientToken] == null) {
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
                                .grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 20px; }
                                .item { background: #222; border-radius: 12px; overflow: hidden; padding-bottom: 15px; box-shadow: 0 4px 12px rgba(0,0,0,0.5); }
                                img, video { width: 100%; aspect-ratio: 16/9; object-fit: contain; background: black; }
                                .name { padding: 10px; font-size: 14px; font-weight: 500; }
                                a { color: #fff; text-decoration: none; display: inline-block; padding: 8px 20px; background: #444; border-radius: 20px; font-size: 12px; }
                                a:hover { background: #555; }
                            </style>
                        </head>
                        <body>
                            <h1>QuickShare Gallery</h1>
                            <div class="grid" id="grid">Loading...</div>
                            <script>
                                async function load() {
                                    const res = await fetch('/photos?token=$clientToken');
                                    const photos = await res.json();
                                    const grid = document.getElementById('grid');
                                    grid.innerHTML = photos.map(p => {
                                        const isVideo = p.name.toLowerCase().endsWith('.mp4') || p.name.toLowerCase().endsWith('.mov');
                                        const media = isVideo 
                                            ? `<video controls src="${'$'}{p.downloadUrl}"></video>` 
                                            : `<img src="${'$'}{p.thumbnailUrl}" />`;
                                        
                                        return `
                                            <div class="item">
                                                ${'$'}{media}
                                                <div class="name">${'$'}{p.name}</div>
                                                <a href="${'$'}{p.downloadUrl}" download>Download</a>
                                            </div>
                                        `;
                                    }).join('');
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

        val context = AndroidContext.get()
        val serviceIntent = Intent(context, QuickShareService::class.java).apply {
            action = QuickShareService.ACTION_START
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }

    actual fun addShare(files: List<PlatformFile>, token: String) {
        tokenFileMap[token] = files
    }

    actual fun removeShare(token: String) {
        tokenFileMap.remove(token)
    }

    actual suspend fun stop() {
        server?.stop(500, 1000)
        server = null
        tokenFileMap.clear()
        _isRunning = false

        val context = AndroidContext.get()
        val serviceIntent = Intent(context, QuickShareService::class.java).apply {
            action = QuickShareService.ACTION_STOP
        }
        context.startService(serviceIntent)
    }
}
