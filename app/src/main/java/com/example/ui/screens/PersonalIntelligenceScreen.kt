package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.capabilities.Capability
import com.example.data.capabilities.CapabilityRegistry
import com.example.data.capabilities.ExecutionMode
import com.example.data.capabilities.ResourceCost
import com.example.data.personalization.ExperimentHistoryEntity
import com.example.data.recipe.AspectRatioMode
import com.example.data.recipe.ExperienceRecipe
import com.example.data.recipe.MoodPreset
import com.example.engine.DirectorGoal
import com.example.engine.DirectorPlanResponse
import com.example.engine.ExperimentVariation
import com.example.ui.navigation.Screen
import com.example.ui.theme.BorderStroke
import com.example.ui.theme.CoralFlame
import com.example.ui.theme.CyberTeal
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceContainer
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricAmber
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.SkipFrameViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalIntelligenceScreen(
    viewModel: SkipFrameViewModel,
    initialTab: Int = 0
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(initialTab) }
    val tabTitles = listOf("Experiences", "Experiments", "AI Director", "Capabilities", "Memory")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "PERSONAL INTELLIGENCE",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = CyberTeal,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = "Extensible viewing environment • 100% On-Device",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.Home) },
                        modifier = Modifier.testTag("intelligence_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = DarkSurface,
                contentColor = CyberTeal
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) CyberTeal else TextSecondary
                            )
                        },
                        modifier = Modifier.testTag("intel_tab_$index")
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                when (selectedTab) {
                    0 -> MyExperiencesTab(viewModel = viewModel)
                    1 -> ExperimentLabTab(viewModel = viewModel)
                    2 -> AiDirectorTab(viewModel = viewModel)
                    3 -> CapabilitiesTab()
                    4 -> PersonalizationMemoryTab(viewModel = viewModel)
                }
            }
        }
    }
}

// =========================================================================
// TAB 0: MY EXPERIENCES
// =========================================================================

@Composable
private fun MyExperiencesTab(viewModel: SkipFrameViewModel) {
    val context = LocalContext.current
    val experiences by viewModel.userExperiences.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var recipeToEdit by remember { mutableStateOf<ExperienceRecipe?>(null) }
    var recipeToExportJson by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Actions Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("create_experience_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberTeal, contentColor = Color.Black)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Create", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { showImportDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("import_recipe_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricAmber),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ElectricAmber)
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Import Recipe", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Experiences List
        items(experiences) { exp ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, if (exp.isFavorite) ElectricAmber else BorderStroke, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = exp.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                if (exp.isFavorite) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(Icons.Default.Bookmark, contentDescription = "Favorite", tint = ElectricAmber, modifier = Modifier.size(16.dp))
                                }
                            }
                            Text(
                                text = exp.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }

                        IconButton(
                            onClick = {
                                viewModel.saveExperienceRecipe(exp.copy(isFavorite = !exp.isFavorite))
                            }
                        ) {
                            Icon(
                                imageVector = if (exp.isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Toggle Favorite",
                                tint = if (exp.isFavorite) ElectricAmber else TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Key specs chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(shape = RoundedCornerShape(4.dp), color = DarkSurfaceVariant) {
                            Text("${exp.playbackSpeed}x Speed", fontSize = 10.sp, color = CyberTeal, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                        if (exp.aspectRatio != AspectRatioMode.ORIGINAL) {
                            Surface(shape = RoundedCornerShape(4.dp), color = DarkSurfaceVariant) {
                                Text(exp.aspectRatio.displayName, fontSize = 10.sp, color = ElectricAmber, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                        if (exp.moodPreset != MoodPreset.NONE) {
                            Surface(shape = RoundedCornerShape(4.dp), color = DarkSurfaceVariant) {
                                Text(exp.moodPreset.displayName, fontSize = 10.sp, color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                        if (exp.isMirrored) {
                            Surface(shape = RoundedCornerShape(4.dp), color = DarkSurfaceVariant) {
                                Text("Mirrored", fontSize = 10.sp, color = CoralFlame, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Experience Actions Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { viewModel.applyExperienceToActiveVideo(exp) },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberTeal, contentColor = Color.Black),
                            modifier = Modifier.weight(1.3f).height(40.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Apply to Video", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        IconButton(
                            onClick = { recipeToEdit = exp },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Experience", tint = TextSecondary)
                        }

                        IconButton(
                            onClick = { viewModel.duplicateExperienceRecipe(exp) },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate Experience", tint = TextSecondary)
                        }

                        IconButton(
                            onClick = { recipeToExportJson = exp.toJsonString() },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = "Export JSON Recipe", tint = ElectricAmber)
                        }

                        IconButton(
                            onClick = { viewModel.deleteExperienceRecipe(exp.id) },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = CoralFlame)
                        }
                    }
                }
            }
        }
    }

    // Recipe Creator / Editor Dialog
    if (showCreateDialog || recipeToEdit != null) {
        val editing = recipeToEdit
        var name by remember { mutableStateOf(editing?.name ?: "My Custom Vision") }
        var description by remember { mutableStateOf(editing?.description ?: "Handcrafted viewing recipe") }
        var speed by remember { mutableFloatStateOf(editing?.playbackSpeed ?: 1.0f) }
        var mirror by remember { mutableStateOf(editing?.isMirrored ?: false) }
        var zoom by remember { mutableFloatStateOf(editing?.zoomScale ?: 1.0f) }
        var aspect by remember { mutableStateOf(editing?.aspectRatio ?: AspectRatioMode.ORIGINAL) }
        var mood by remember { mutableStateOf(editing?.moodPreset ?: MoodPreset.NONE) }
        var intensity by remember { mutableFloatStateOf(editing?.moodIntensity ?: 0.8f) }
        var muteOnSpeed by remember { mutableStateOf(editing?.muteAudioOnSpeedChange ?: false) }

        Dialog(onDismissRequest = {
            showCreateDialog = false
            recipeToEdit = null
        }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DarkSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = if (editing != null) "Edit Experience" else "Create Experience",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = CyberTeal
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberTeal, unfocusedBorderColor = BorderStroke)
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Description") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberTeal, unfocusedBorderColor = BorderStroke)
                        )
                    }

                    // Speed
                    item {
                        Text("Playback Speed: ${speed}x", fontSize = 12.sp, color = TextSecondary)
                        Slider(
                            value = speed,
                            onValueChange = { speed = it },
                            valueRange = 0.25f..2.0f,
                            colors = SliderDefaults.colors(thumbColor = CyberTeal, activeTrackColor = CyberTeal)
                        )
                    }

                    // Mirror & Mute Toggles
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Horizontal Mirror", color = TextPrimary, fontSize = 13.sp)
                            Switch(checked = mirror, onCheckedChange = { mirror = it }, colors = SwitchDefaults.colors(checkedThumbColor = CyberTeal))
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Mute on Speed Shift", color = TextPrimary, fontSize = 13.sp)
                            Switch(checked = muteOnSpeed, onCheckedChange = { muteOnSpeed = it }, colors = SwitchDefaults.colors(checkedThumbColor = CyberTeal))
                        }
                    }

                    // Zoom
                    item {
                        Text("Zoom Scale: ${String.format(Locale.US, "%.1fx", zoom)}", fontSize = 12.sp, color = TextSecondary)
                        Slider(
                            value = zoom,
                            onValueChange = { zoom = it },
                            valueRange = 1.0f..3.0f,
                            colors = SliderDefaults.colors(thumbColor = CyberTeal, activeTrackColor = CyberTeal)
                        )
                    }

                    // Aspect Ratio
                    item {
                        Text("Aspect Ratio", fontSize = 12.sp, color = TextSecondary)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(AspectRatioMode.values()) { m ->
                                FilterChip(
                                    selected = aspect == m,
                                    onClick = { aspect = m },
                                    label = { Text(m.displayName, fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    // Mood Preset
                    item {
                        Text("Color Grade", fontSize = 12.sp, color = TextSecondary)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(MoodPreset.values()) { p ->
                                FilterChip(
                                    selected = mood == p,
                                    onClick = { mood = p },
                                    label = { Text(p.displayName, fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    // Buttons
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OutlinedButton(onClick = {
                                showCreateDialog = false
                                recipeToEdit = null
                            }) {
                                Text("Cancel", color = TextSecondary)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val newRecipe = ExperienceRecipe(
                                        id = editing?.id ?: UUID.randomUUID().toString(),
                                        name = name.ifBlank { "Custom Vision" },
                                        description = description,
                                        playbackSpeed = speed,
                                        isMirrored = mirror,
                                        zoomScale = zoom,
                                        aspectRatio = aspect,
                                        moodPreset = mood,
                                        moodIntensity = intensity,
                                        muteAudioOnSpeedChange = muteOnSpeed,
                                        createdAt = editing?.createdAt ?: System.currentTimeMillis(),
                                        modifiedAt = System.currentTimeMillis()
                                    )
                                    viewModel.saveExperienceRecipe(newRecipe)
                                    showCreateDialog = false
                                    recipeToEdit = null
                                    Toast.makeText(context, "Experience saved", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberTeal, contentColor = Color.Black)
                            ) {
                                Text("Save", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Export JSON Dialog
    if (recipeToExportJson != null) {
        val json = recipeToExportJson!!
        Dialog(onDismissRequest = { recipeToExportJson = null }) {
            Surface(shape = RoundedCornerShape(14.dp), color = DarkSurface, modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Portable Experience Recipe", fontWeight = FontWeight.Bold, color = CyberTeal)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Source-independent JSON format ready for exchange:", fontSize = 11.sp, color = TextMuted)
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(shape = RoundedCornerShape(8.dp), color = Color.Black, modifier = Modifier.fillMaxWidth().height(160.dp)) {
                        Text(json, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = TextPrimary, modifier = Modifier.padding(8.dp))
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Button(
                            onClick = {
                                val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cb.setPrimaryClip(ClipData.newPlainText("Recipe JSON", json))
                                Toast.makeText(context, "Recipe copied to clipboard", Toast.LENGTH_SHORT).show()
                                recipeToExportJson = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberTeal, contentColor = Color.Black)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy JSON")
                        }
                    }
                }
            }
        }
    }

    // Import Recipe Dialog
    if (showImportDialog) {
        var importInput by remember { mutableStateOf("") }
        var importError by remember { mutableStateOf<String?>(null) }

        Dialog(onDismissRequest = { showImportDialog = false }) {
            Surface(shape = RoundedCornerShape(14.dp), color = DarkSurface, modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Import Experience Recipe", fontWeight = FontWeight.Bold, color = ElectricAmber)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Paste JSON recipe. Strictly validated against local Capability Registry before activation.", fontSize = 11.sp, color = TextMuted)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = importInput,
                        onValueChange = { importInput = it; importError = null },
                        label = { Text("Paste JSON") },
                        modifier = Modifier.fillMaxWidth().height(140.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricAmber, unfocusedBorderColor = BorderStroke)
                    )
                    if (importError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(importError!!, color = CoralFlame, fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        OutlinedButton(onClick = { showImportDialog = false }) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                try {
                                    val parsed = ExperienceRecipe.fromJsonString(importInput)
                                    viewModel.saveExperienceRecipe(parsed)
                                    showImportDialog = false
                                    Toast.makeText(context, "Imported: ${parsed.name}", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    importError = e.message ?: "Invalid recipe JSON"
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricAmber, contentColor = Color.Black)
                        ) {
                            Text("Import & Validate", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// TAB 1: EXPERIMENT LAB
// =========================================================================

@Composable
private fun ExperimentLabTab(viewModel: SkipFrameViewModel) {
    val context = LocalContext.current
    val variations by viewModel.experimentVariations.collectAsStateWithLifecycle()
    var currentSeed by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderStroke, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Deterministic Experimentation", fontWeight = FontWeight.Bold, color = CyberTeal, fontSize = 15.sp)
                            Text("Explore multiple interpretations of the same source footage.", color = TextMuted, fontSize = 11.sp)
                        }
                        Text("Seed #${(currentSeed % 10000).let { if (it < 0) -it else it }}", fontFamily = FontFamily.Monospace, color = ElectricAmber, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                currentSeed = System.currentTimeMillis()
                                viewModel.generateExperimentVariations(seed = currentSeed)
                            },
                            modifier = Modifier.weight(1f).height(46.dp).testTag("generate_variations_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberTeal, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Suite (4x)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { viewModel.generateExperimentVariations(seed = currentSeed) },
                            modifier = Modifier.weight(1f).height(46.dp).testTag("regenerate_seed_button"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricAmber),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ElectricAmber)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Same Seed", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        if (variations.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Science, contentDescription = null, tint = CyberTeal, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("No active experiment variations", fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Tap 'New Suite (4x)' to generate reproducible interpretations.", color = TextMuted, fontSize = 12.sp)
                    }
                }
            }
        } else {
            items(variations) { v ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderStroke, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(v.title, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
                            Surface(shape = RoundedCornerShape(4.dp), color = DarkSurfaceContainer) {
                                Text("Seed #${(v.seed % 1000).let { if (it < 0) -it else it }}", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = TextSecondary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(v.rationale, style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontSize = 12.sp)

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { viewModel.applyExperienceToActiveVideo(v.recipe) },
                                modifier = Modifier.weight(1.2f).height(38.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CyberTeal, contentColor = Color.Black),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Preview", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.saveExperienceRecipe(v.recipe.copy(isFavorite = true))
                                    viewModel.recordExperimentRating(v, "SAVED")
                                    Toast.makeText(context, "Saved to My Experiences", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1.1f).height(38.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricAmber),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ElectricAmber)
                            ) {
                                Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Keep", fontSize = 12.sp)
                            }

                            IconButton(
                                onClick = {
                                    viewModel.recordExperimentRating(v, "DISLIKED")
                                    Toast.makeText(context, "Recorded feedback", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.ThumbDown, contentDescription = "Dislike", tint = TextMuted)
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// TAB 2: AI DIRECTOR INTERFACE
// =========================================================================

@Composable
private fun AiDirectorTab(viewModel: SkipFrameViewModel) {
    val context = LocalContext.current
    val lastPlan by viewModel.lastDirectorPlan.collectAsStateWithLifecycle()
    var selectedGoal by remember { mutableStateOf(DirectorGoal.TIGHT_PACED) }
    var customPrompt by remember { mutableStateOf("") }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderStroke, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Model-Independent Director Interface", fontWeight = FontWeight.Bold, color = CyberTeal, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Formulates structured operation requests validated against device capabilities. Backed by deterministic rule planner today; ready for on-device local AI.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Goal selector chips
        item {
            Text("Select Viewing Goal:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DirectorGoal.values().forEach { goal ->
                    val isSel = selectedGoal == goal
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSel) DarkSurfaceVariant else DarkSurfaceContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedGoal = goal }
                            .border(1.dp, if (isSel) CyberTeal else BorderStroke, RoundedCornerShape(10.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(if (isSel) CyberTeal else TextMuted, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(goal.label, fontWeight = FontWeight.Bold, color = if (isSel) TextPrimary else TextSecondary, fontSize = 13.sp)
                                Text(goal.promptTemplate, color = TextMuted, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = { viewModel.requestAiDirectorPlan(selectedGoal, customPrompt) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("ai_director_plan_button"),
                colors = ButtonDefaults.buttonColors(containerColor = CyberTeal, contentColor = Color.Black),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Plan & Validate Recipe", fontWeight = FontWeight.Bold)
            }
        }

        // Plan Result Card
        if (lastPlan != null) {
            val plan = lastPlan!!
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ElectricAmber, RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(plan.explanationTitle, fontWeight = FontWeight.Bold, color = ElectricAmber, fontSize = 16.sp)
                            Surface(shape = RoundedCornerShape(20.dp), color = CyberTeal.copy(alpha = 0.2f)) {
                                Text("Verified Safe", color = CyberTeal, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text("WHAT CHANGED & WHY:", fontWeight = FontWeight.Bold, color = TextSecondary, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        plan.explanationDetails.forEach { bullet ->
                            Text("• $bullet", color = TextPrimary, fontSize = 12.sp, modifier = Modifier.padding(vertical = 2.dp))
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text("CAPABILITIES USED:", fontWeight = FontWeight.Bold, color = TextSecondary, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(plan.capabilitiesUsed) { capId ->
                                Surface(shape = RoundedCornerShape(4.dp), color = DarkSurfaceVariant) {
                                    Text(capId, fontSize = 10.sp, color = CyberTeal, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                viewModel.applyDirectorPlan(plan)
                                Toast.makeText(context, "Loaded into Live Preview", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberTeal, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Apply Plan to Live Video", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// TAB 3: CAPABILITY REGISTRY
// =========================================================================

@Composable
private fun CapabilitiesTab() {
    val categories = remember { CapabilityRegistry.getByCategory() }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderStroke, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Verified Device Capability Registry", fontWeight = FontWeight.Bold, color = CyberTeal, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Every operation below is genuinely supported on this device. No aspirational or simulated features are permitted.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }

        categories.forEach { (category, caps) ->
            item {
                Text(
                    text = category.uppercase(),
                    fontWeight = FontWeight.Bold,
                    color = ElectricAmber,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                )
            }

            items(caps) { cap ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderStroke, RoundedCornerShape(10.dp)),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(cap.displayName, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = when (cap.executionMode) {
                                    ExecutionMode.LIVE_ONLY -> CyberTeal.copy(alpha = 0.2f)
                                    ExecutionMode.OFFLINE_ONLY -> CoralFlame.copy(alpha = 0.2f)
                                    ExecutionMode.BOTH -> ElectricAmber.copy(alpha = 0.2f)
                                }
                            ) {
                                Text(
                                    text = cap.executionMode.displayName,
                                    color = when (cap.executionMode) {
                                        ExecutionMode.LIVE_ONLY -> CyberTeal
                                        ExecutionMode.OFFLINE_ONLY -> CoralFlame
                                        ExecutionMode.BOTH -> ElectricAmber
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(cap.description, color = TextSecondary, fontSize = 11.sp)

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Cost: ${cap.resourceCost.label} • ${cap.resourceCost.description}", fontSize = 10.sp, color = TextMuted)
                        }

                        if (cap.failureBehavior.isNotEmpty()) {
                            Text("Failure Strategy: ${cap.failureBehavior}", fontSize = 10.sp, color = TextMuted)
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// TAB 4: PERSONALIZATION & MEMORY
// =========================================================================

@Composable
private fun PersonalizationMemoryTab(viewModel: SkipFrameViewModel) {
    val context = LocalContext.current
    val history by viewModel.experimentHistory.collectAsStateWithLifecycle()
    val preferences by viewModel.personalPreferences.collectAsStateWithLifecycle()

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderStroke, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Local Personalization Memory", fontWeight = FontWeight.Bold, color = CyberTeal, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Stores only explainable, user-approved viewing preferences and experiment ratings. Zero cloud sync, zero hidden profiling.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                viewModel.clearAllPreferences()
                                Toast.makeText(context, "Preferences cleared", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralFlame)
                        ) {
                            Text("Clear Preferences", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.clearExperimentHistory()
                                Toast.makeText(context, "Experiment history cleared", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralFlame)
                        ) {
                            Text("Clear History", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        item {
            Text("EXPERIMENT RATING HISTORY (${history.size})", fontWeight = FontWeight.Bold, color = TextSecondary, fontSize = 12.sp)
        }

        if (history.isEmpty()) {
            item {
                Text("No experiments recorded yet.", color = TextMuted, fontSize = 12.sp)
            }
        } else {
            items(history) { h ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderStroke, RoundedCornerShape(10.dp)),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(h.recipeName, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                            Text(h.summary, color = TextMuted, fontSize = 11.sp)
                            Text(SimpleDateFormat("MMM d, HH:mm", Locale.US).format(Date(h.timestamp)), color = TextMuted, fontSize = 10.sp)
                        }

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = when (h.rating) {
                                "SAVED" -> ElectricAmber.copy(alpha = 0.2f)
                                "LIKED" -> CyberTeal.copy(alpha = 0.2f)
                                "DISLIKED" -> CoralFlame.copy(alpha = 0.2f)
                                else -> DarkSurfaceContainer
                            }
                        ) {
                            Text(
                                text = h.rating,
                                color = when (h.rating) {
                                    "SAVED" -> ElectricAmber
                                    "LIKED" -> CyberTeal
                                    "DISLIKED" -> CoralFlame
                                    else -> TextSecondary
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
