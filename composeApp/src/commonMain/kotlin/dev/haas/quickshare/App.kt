package dev.haas.quickshare

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.*

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.haas.quickshare.core.AppState
import dev.haas.quickshare.core.models.PlatformFile
import dev.haas.quickshare.ui.PlatformUI
import dev.haas.quickshare.ui.QRCodeView
import dev.haas.quickshare.core.models.PermissionInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


val CustomBlack = Color(0xFF16161D)

val LightColors = lightColorScheme(
    primary = CustomBlack,
    onPrimary = Color.White,
    primaryContainer = Color.White,
    onPrimaryContainer = CustomBlack,
    secondary = CustomBlack,
    onSecondary = Color.White,
    background = Color.White,
    onBackground = CustomBlack,
    surface = Color.White,
    onSurface = CustomBlack,
    surfaceVariant = Color.White,
    onSurfaceVariant = CustomBlack,
    error = Color(0xFFB00020),
    onError = Color.White,
    outline = CustomBlack
)

@Composable
fun App(
    permissionRefreshTick: Int = 0,
    permissions: List<PermissionInfo> = emptyList(),
    onRequestPermissions: () -> Unit = {},
    onOpenBatterySettings: () -> Unit = { PlatformUI.openBatterySettings() },
    onOpenAppSettings: () -> Unit = {}
) {
    val sessionManager = AppState.sessionManager
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val state by lifecycleOwner.lifecycle.currentStateFlow.collectAsStateWithLifecycle()

    LaunchedEffect(state) {
        if (state == androidx.lifecycle.Lifecycle.State.CREATED) {
            // Un-comment to kill session in background
            // sessionManager.stopSession("App entered background")
        }
    }

    LaunchedEffect(Unit) {
        AppState.albumManager.loadRecentShares()
    }

    MaterialTheme(colorScheme = LightColors) {
        val isSharing by sessionManager.isSharing.collectAsStateWithLifecycle()
        val tunnelUrl by sessionManager.tunnelUrl.collectAsStateWithLifecycle()
        val activeShares by sessionManager.activeShares.collectAsStateWithLifecycle()
        val logs by sessionManager.logs.collectAsStateWithLifecycle()
        val secondsRunning by sessionManager.secondsRunning.collectAsStateWithLifecycle()
        
        val albums by AppState.albumManager.albums.collectAsStateWithLifecycle()
        val selectedFiles by AppState.selectedFiles.collectAsStateWithLifecycle()

        val pickFiles = PlatformUI.rememberImagePicker { files ->
            AppState.setSelectedFiles(selectedFiles + files)
            sessionManager.appendLog("Added ${files.size} files.")
        }

        var currentTab by remember { mutableIntStateOf(0) }
        var bannerDismissed by remember { mutableStateOf(false) }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentTab == 0,
                        onClick = { currentTab = 0 },
                        icon = { Icon(Icons.Default.Share, "Share") },
                        label = { Text("Share") }
                    )
                    NavigationBarItem(
                        selected = currentTab == 1,
                        onClick = { currentTab = 1 },
                        icon = { Icon(Icons.Default.Album, "Albums") },
                        label = { Text("Albums") }
                    )
                    NavigationBarItem(
                        selected = currentTab == 2,
                        onClick = { currentTab = 2 },
                        icon = { Icon(Icons.Default.Settings, "Settings") },
                        label = { Text("Settings") }
                    )
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "QuickShare", 
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text("Secure peer-to-peer sharing", style = MaterialTheme.typography.bodySmall)
                
                if (!bannerDismissed) {
                    Spacer(modifier = Modifier.height(12.dp))
                    PermissionSection(
                        permissions = permissions,
                        onRequestPermissions = onRequestPermissions,
                        onOpenBatterySettings = onOpenBatterySettings,
                        onOpenAppSettings = onOpenAppSettings,
                        onDismiss = { bannerDismissed = true }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (currentTab) {
                    0 -> ShareScreen(
                        selectedFiles = selectedFiles,
                        activeShares = activeShares,
                        tunnelUrl = tunnelUrl,
                        secondsRunning = secondsRunning,
                        isSharing = isSharing,
                        onPickFiles = pickFiles,
                        onSaveToAlbum = { 
                            AppState.albumManager.createAlbumFromSelection("Album ${albums.size + 1}", selectedFiles) 
                            PlatformUI.showToast("Saved to Albums")
                        },
                        onClearFiles = { AppState.setSelectedFiles(emptyList()) },
                        onStartMainShare = { sessionManager.startSession(it) },
                        onCreateAdditionalShare = { sessionManager.createAdditionalShare(it) },
                        onStopShare = { sessionManager.stopShare(it) },
                        onStopSession = { sessionManager.stopSession() }
                    )
                    1 -> AlbumsScreen(
                        albums = albums,
                        onShareAlbum = { album ->
                            val files = AppState.albumManager.getFilesForAlbum(album)
                            AppState.setSelectedFiles(files)
                            currentTab = 0
                        }
                    )
                    2 -> SettingsScreen(logs)
                }
            }
        }
    }
}

@Composable
fun ShareScreen(
    selectedFiles: List<PlatformFile>,
    activeShares: List<dev.haas.quickshare.core.models.ActiveShare>,
    tunnelUrl: String?,
    secondsRunning: Int,
    isSharing: Boolean,
    onPickFiles: () -> Unit,
    onSaveToAlbum: () -> Unit,
    onClearFiles: () -> Unit,
    onStartMainShare: (List<PlatformFile>) -> Unit,
    onCreateAdditionalShare: (List<PlatformFile>) -> Unit,
    onStopShare: (String) -> Unit,
    onStopSession: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Current Selection (${selectedFiles.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row {
                        if (selectedFiles.isNotEmpty()) {
                            IconButton(onClick = onSaveToAlbum) {
                                Icon(Icons.Default.Save, contentDescription = "Save to Album")
                            }
                            IconButton(onClick = onClearFiles) {
                                Icon(Icons.Default.Close, contentDescription = "Clear Selection")
                            }
                        }
                        IconButton(onClick = onPickFiles) {
                            Icon(Icons.Default.Add, contentDescription = "Add Files")
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))

                if (selectedFiles.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .background(Color.F8F8F8.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .clickable { onPickFiles() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Tap + to select images/videos (Photos)", color = Color.Gray)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 80.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)
                    ) {
                        items(selectedFiles) { file ->
                            PhotoGridItem(file = file)
                        }
                    }
                    
                    Button(
                        onClick = { 
                            if (!isSharing) onStartMainShare(selectedFiles) 
                            else onCreateAdditionalShare(selectedFiles)
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        Text(if (!isSharing) "START FIRST SHARE" else "CREATE NEW LINK", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (isSharing) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.F8F8F8),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Active Links", fontWeight = FontWeight.Bold)
                        Text(formatTime(secondsRunning), color = MaterialTheme.colorScheme.primary)
                    }
                    
                    if (tunnelUrl == null) {
                        Text("Preparing tunnel...", modifier = Modifier.padding(top = 16.dp))
                    } else {
                        Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            activeShares.forEach { share ->
                                val fullUrl = "$tunnelUrl/?token=${share.token}"
                                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("${share.fileCount} files shared", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                                        
                                        Spacer(modifier = Modifier.height(8.dp))
                                        QRCodeView(fullUrl, modifier = Modifier.size(150.dp).clickable { PlatformUI.shareLink(fullUrl) })
                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(fullUrl, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), maxLines = 1)
                                            IconButton(onClick = { PlatformUI.copyToClipboard(fullUrl) }) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                                            }
                                            IconButton(onClick = { PlatformUI.shareLink(fullUrl) }) {
                                                Icon(Icons.Default.Share, contentDescription = "Share")
                                            }
                                            IconButton(onClick = { onStopShare(share.token) }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Stop this share", tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onStopSession, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error), modifier = Modifier.fillMaxWidth()) {
                        Text("STOP ALL SHARING")
                    }
                }
            }
        }
    }
}

@Composable
fun AlbumsScreen(
    albums: List<dev.haas.quickshare.core.models.Album>,
    onShareAlbum: (dev.haas.quickshare.core.models.Album) -> Unit
) {
    var viewAlbum by remember { mutableStateOf<dev.haas.quickshare.core.models.Album?>(null) }
    var renameAlbum by remember { mutableStateOf<dev.haas.quickshare.core.models.Album?>(null) }

    if (renameAlbum != null) {
        var newName by remember { mutableStateOf(renameAlbum!!.name) }
        AlertDialog(
            onDismissRequest = { renameAlbum = null },
            title = { Text("Rename Album") },
            text = { 
                OutlinedTextField(value = newName, onValueChange = { newName = it }) 
            },
            confirmButton = {
                Button(onClick = { 
                    AppState.albumManager.renameAlbum(renameAlbum!!.id, newName)
                    renameAlbum = null 
                }) { Text("Save") }
            }
        )
    }

    if (viewAlbum != null) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewAlbum = null }) { Icon(Icons.Default.Close, "Back") }
                Text(viewAlbum!!.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            val files = remember(viewAlbum) { AppState.albumManager.getFilesForAlbum(viewAlbum!!) }
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 100.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(files) { file -> PhotoGridItem(file) }
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            Text("Your Albums", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            if (albums.isEmpty()) {
                Text("No albums saved yet.", color = Color.Gray)
            } else {
                LazyColumn {
                    lazyItems(albums) { album ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { viewAlbum = album },
                            colors = CardDefaults.cardColors(containerColor = Color.F8F8F8)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Album, contentDescription = null)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(album.name, fontWeight = FontWeight.Bold)
                                    Text("${album.fileNames.size} files", style = MaterialTheme.typography.bodySmall)
                                }
                                IconButton(onClick = { onShareAlbum(album) }) { Icon(Icons.Default.Share, "Share Album") }
                                IconButton(onClick = { renameAlbum = album }) { Icon(Icons.Default.Edit, "Rename") }
                                IconButton(onClick = { AppState.albumManager.deleteAlbum(album.id) }) { Icon(Icons.Default.Delete, "Delete") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(logs: List<String>) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text("Logs & Diagnostics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        val listState = rememberLazyListState()
        LaunchedEffect(logs.size) { listState.animateScrollToItem(logs.size.coerceAtLeast(1) - 1) }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.Black.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            lazyItems(logs) { log ->
                Text(log, style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp))
            }
        }
        AboutSection()
    }
}

@Composable
fun PermissionSection(
    permissions: List<PermissionInfo>,
    onRequestPermissions: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    val allGranted = permissions.all { it.isGranted }
    val bgColor = if (allGranted) Color(0xFFF1FDF3) else Color(0xFFFFFBEE)
    val borderColor = if (allGranted) Color(0xFF4CAF50).copy(alpha = 0.5f) else Color(0xFFFBC02D).copy(alpha = 0.5f)

    Card(
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (allGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (allGranted) Color(0xFF4CAF50) else Color(0xFFFBC02D),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    if (allGranted) "All permissions granted" else "Permissions needed",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f)
                )
                if (allGranted) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, "Dismiss")
                    }
                }
            }

            if (permissions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                permissions.forEach { perm ->
                    Row(
                        modifier = Modifier.padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (perm.isGranted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (perm.isGranted) Color(0xFF4CAF50) else Color(0xFFE57373),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(perm.label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text(perm.description, fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row {
                if (!allGranted) {
                    Button(
                        onClick = onRequestPermissions,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFBC02D), contentColor = Color.Black),
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    ) {
                        Text("Grant Permissions", fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = onOpenAppSettings,
                        colors = ButtonDefaults.outlinedButtonColors(),
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    ) {
                        Text("Open Settings", fontSize = 11.sp)
                    }
                } else {
                    Button(
                        onClick = onOpenBatterySettings,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50).copy(alpha = 0.15f),
                            contentColor = Color(0xFF2E7D32)
                        ),
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    ) {
                        Text("Disable Battery Saver", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun AboutSection() {
    Column(modifier = Modifier.padding(top = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Developer Notes", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
        Text(
            "Database Migration: Currently using in-memory storage. Future updates will migrate to persistent DataStore. Previous users will start with empty albums upon local storage initialization.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.LightGray,
            textAlign = TextAlign.Center,
            fontSize = 9.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
fun PhotoGridItem(file: PlatformFile) {
    var thumbnail by remember(file.uriString) { mutableStateOf<ImageBitmap?>(null) }
    
    LaunchedEffect(file.uriString) {
        if (thumbnail == null) {
            thumbnail = PlatformUI.loadThumbnail(file)
        }
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.LightGray)
    ) {
        if (thumbnail != null) {
            Image(
                bitmap = thumbnail!!,
                contentDescription = file.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(file.name.take(1).uppercase(), style = MaterialTheme.typography.headlineMedium)
            }
        }
    }
}

fun formatTime(totalSeconds: Int): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    val mStr = if (m < 10) "0$m" else "$m"
    val sStr = if (s < 10) "0$s" else "$s"
    return "$mStr:$sStr"
}

val Color.Companion.F8F8F8: Color get() = Color(0xFFF8F8F8)