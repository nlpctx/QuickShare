package dev.haas.quickshare.core

import dev.haas.quickshare.core.models.Album
import dev.haas.quickshare.core.models.PlatformFile
import dev.haas.quickshare.ui.PlatformUI
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

object AlbumManager {
    private val _albums = MutableStateFlow<List<Album>>(emptyList())
    val albums = _albums.asStateFlow()

    private fun persistAlbums() {
        val jsonString = Json.encodeToString(_albums.value)
        PlatformUI.savePersistentData("albums_json", jsonString)
    }

    fun createAlbumFromSelection(name: String, files: List<PlatformFile>) {
        val album = Album(
            id = System.currentTimeMillis().toString(),
            name = name,
            fileUris = files.map { it.uriString },
            fileNames = files.map { it.name }
        )
        _albums.value = _albums.value + album
        persistAlbums()
    }

    fun getFilesForAlbum(album: Album): List<PlatformFile> {
        return album.fileUris.mapNotNull { PlatformUI.createPlatformFile(it) }
    }

    fun renameAlbum(id: String, newName: String) {
        _albums.value = _albums.value.map { if (it.id == id) it.copy(name = newName) else it }
        persistAlbums()
    }

    fun deleteAlbum(id: String) {
        _albums.value = _albums.value.filter { it.id != id }
        persistAlbums()
    }

    /**
     * DATABASE MIGRATION STRATEGY:
     * 1. Current state: No persistent database (in-memory only).
     * 2. Phase 1 (Next): Introduce SharedPreferences/DataStore to persist [Album] objects as JSON.
     * 3. Phase 2 (Future): If complex relations are needed, migrate to SQLDelight.
     * 4. Migration Handling: For users moving from no-storage to Phase 1, the list will start empty.
     *    If migrating from Phase 1 to Phase 2, a bootstrap task will read JSON and insert into SQLDelight.
     */
    fun loadRecentShares() {
        val jsonString = PlatformUI.loadPersistentData("albums_json")
        if (jsonString != null) {
            try {
                val loadedAlbums = Json.decodeFromString<List<Album>>(jsonString)
                _albums.value = loadedAlbums
            } catch (e: Exception) {
                // Formatting error, ignore old data
            }
        }
    }
}
