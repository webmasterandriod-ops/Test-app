package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.database.ViewingRecipeEntity
import com.example.data.recipe.AspectRatioMode
import com.example.data.recipe.MoodPreset
import com.example.data.recipe.PlaybackSegment
import com.example.data.recipe.ViewingExperience
import com.example.data.recipe.ViewingRecipe
import com.example.ui.components.RevisionPlayerSurface
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
fun RevisionScreen(
    viewModel: SkipFrameViewModel
) {
    val context = LocalContext.current
    val selectedFile by viewModel.selectedFile.collectAsStateWithLifecycle()
    val metadata by viewModel.selectedMetadata.collectAsStateWithLifecycle()
    val recipe by viewModel.currentRecipe.collectAsStateWithLifecycle()
    val savedRecipesList by viewModel.savedRecipes.collectAsStateWithLifecycle()

    var playerInstance by remember { mutableStateOf<ExoPlayer?>(null) }
    var currentPlaybackPositionMs by remember { mutableLongStateOf(0L) }
    var totalPlaybackDurationMs by remember { mutableLongStateOf(1L) }
    var isPlaying by remember { mutableStateOf(true) }

    var showSaveRecipeDialog by remember { mutableStateOf(false) }
    var showSavedRecipesSheet by remember { mutableStateOf(false) }
    var recipeNameInput by remember { mutableStateOf(recipe.name) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "RE:VISION",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = CyberTeal,
                                letterSpacing = 2.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = DarkSurfaceContainer
                            ) {
                                Text(
                                    text = recipe.name,
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    maxLines = 1
                                )
                            }
                        }
                        Text(
                            text = recipe.activeTransformationSummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (recipe.isTransformed) ElectricAmber else TextMuted,
                            fontSize = 10.sp,
                            maxLines = 1
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.Home) },
                        modifier = Modifier.testTag("revision_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    // Restore Original action
                    IconButton(
                        onClick = {
                            viewModel.restoreOriginalRecipe()
                            Toast.makeText(context, "All transformations reset", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("restore_original_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Restore Original", tint = CoralFlame)
                    }

                    // Open Saved Recipes Sheet
                    IconButton(
                        onClick = { showSavedRecipesSheet = true },
                        modifier = Modifier.testTag("saved_recipes_button")
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = "Saved Recipes", tint = TextSecondary)
                    }

                    // Save Recipe action
                    IconButton(
                        onClick = {
                            recipeNameInput = recipe.name
                            showSaveRecipeDialog = true
                        },
                        modifier = Modifier.testTag("save_recipe_button")
                    ) {
                        Icon(Icons.Default.Bookmark, contentDescription = "Save Recipe", tint = CyberTeal)
                    }

                    // Export Edited Video action
                    IconButton(
                        onClick = { viewModel.exportRevisionVideo() },
                        modifier = Modifier.testTag("export_revision_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "Export Edited Video", tint = ElectricAmber)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        if (selectedFile == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No video selected", color = TextMuted)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.navigateTo(Screen.Home) },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberTeal, contentColor = Color.Black)
                    ) {
                        Text("Return Home")
                    }
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Large Live Video Preview Surface
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, BorderStroke, RoundedCornerShape(16.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    RevisionPlayerSurface(
                        videoPath = selectedFile!!.absolutePath,
                        recipe = recipe,
                        modifier = Modifier.fillMaxSize(),
                        onPlayerCreated = { playerInstance = it },
                        onPositionUpdate = { pos, dur ->
                            currentPlaybackPositionMs = pos
                            totalPlaybackDurationMs = dur
                            isPlaying = playerInstance?.isPlaying ?: false
                        }
                    )

                    // Active experience watermark chip
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Black.copy(alpha = 0.65f),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = when (recipe.activeExperience) {
                                    ViewingExperience.DIRECTORS_CUT -> Icons.Default.Movie
                                    ViewingExperience.FOCUS_LENS -> Icons.Default.Crop
                                    ViewingExperience.DREAMCUT -> Icons.Default.Speed
                                    ViewingExperience.MOOD_ENGINE -> Icons.Default.Palette
                                    ViewingExperience.CHAOS_DIRECTOR -> Icons.Default.Casino
                                },
                                contentDescription = null,
                                tint = CyberTeal,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = recipe.activeExperience.title,
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 2. Playback Controls Bar (Seek slider, Play/Pause, Time labels)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderStroke, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatMs(currentPlaybackPositionMs),
                                style = MaterialTheme.typography.bodySmall,
                                color = CyberTeal,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                            Text(
                                text = formatMs(totalPlaybackDurationMs),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        }

                        // Seek bar
                        Slider(
                            value = currentPlaybackPositionMs.toFloat().coerceIn(0f, totalPlaybackDurationMs.toFloat()),
                            onValueChange = { targetMs ->
                                playerInstance?.seekTo(targetMs.toLong())
                                currentPlaybackPositionMs = targetMs.toLong()
                            },
                            valueRange = 0f..totalPlaybackDurationMs.toFloat().coerceAtLeast(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = CyberTeal,
                                activeTrackColor = CyberTeal,
                                inactiveTrackColor = BorderStroke
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(28.dp)
                                .testTag("revision_seek_slider")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // -5s Seek
                            IconButton(
                                onClick = {
                                    val newPos = (currentPlaybackPositionMs - 5000L).coerceAtLeast(0L)
                                    playerInstance?.seekTo(newPos)
                                },
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(Icons.Default.FastRewind, contentDescription = "Rewind 5s", tint = TextSecondary)
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // Play / Pause
                            IconButton(
                                onClick = {
                                    playerInstance?.let { p ->
                                        if (p.isPlaying) p.pause() else p.play()
                                        isPlaying = p.isPlaying
                                    }
                                },
                                modifier = Modifier
                                    .size(52.dp)
                                    .background(CyberTeal, CircleShape)
                                    .testTag("revision_play_pause_button")
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = Color.Black,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // +5s Seek
                            IconButton(
                                onClick = {
                                    val newPos = (currentPlaybackPositionMs + 5000L).coerceAtMost(totalPlaybackDurationMs)
                                    playerInstance?.seekTo(newPos)
                                },
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(Icons.Default.FastForward, contentDescription = "Forward 5s", tint = TextSecondary)
                            }
                        }
                    }
                }
            }

            // 3. Horizontal Experiences Selector Tabs
            item {
                Text(
                    text = "VIEWING EXPERIENCES",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(ViewingExperience.values()) { exp ->
                        val isSelected = recipe.activeExperience == exp
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                viewModel.updateRecipe { it.copy(activeExperience = exp) }
                            },
                            label = { Text(exp.title, fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = when (exp) {
                                        ViewingExperience.DIRECTORS_CUT -> Icons.Default.Movie
                                        ViewingExperience.FOCUS_LENS -> Icons.Default.Crop
                                        ViewingExperience.DREAMCUT -> Icons.Default.Speed
                                        ViewingExperience.MOOD_ENGINE -> Icons.Default.Palette
                                        ViewingExperience.CHAOS_DIRECTOR -> Icons.Default.Casino
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isSelected) Color.Black else CyberTeal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyberTeal,
                                selectedLabelColor = Color.Black,
                                containerColor = DarkSurface,
                                labelColor = TextPrimary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = BorderStroke,
                                selectedBorderColor = CyberTeal
                            ),
                            modifier = Modifier.testTag("experience_tab_${exp.name.lowercase()}")
                        )
                    }
                }
            }

            // 4. Contextual Experience Control Panel
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderStroke, RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = recipe.activeExperience.title.uppercase(),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = ElectricAmber
                            )
                            Text(
                                text = recipe.activeExperience.summary,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Render contextual controls based on activeExperience
                        when (recipe.activeExperience) {
                            ViewingExperience.DIRECTORS_CUT -> {
                                DirectorsCutPanel(viewModel = viewModel, recipe = recipe, totalDurationMs = totalPlaybackDurationMs)
                            }
                            ViewingExperience.FOCUS_LENS -> {
                                FocusLensPanel(viewModel = viewModel, recipe = recipe)
                            }
                            ViewingExperience.DREAMCUT -> {
                                DreamcutPanel(viewModel = viewModel, recipe = recipe, currentPosMs = currentPlaybackPositionMs, totalDurationMs = totalPlaybackDurationMs)
                            }
                            ViewingExperience.MOOD_ENGINE -> {
                                MoodEnginePanel(viewModel = viewModel, recipe = recipe)
                            }
                            ViewingExperience.CHAOS_DIRECTOR -> {
                                ChaosDirectorPanel(viewModel = viewModel, recipe = recipe)
                            }
                        }
                    }
                }
            }

            // 5. Bottom Actions: Save Recipe & Export Edited Video
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            recipeNameInput = recipe.name
                            showSaveRecipeDialog = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("save_recipe_action_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberTeal),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberTeal)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Recipe", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { viewModel.exportRevisionVideo() },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("export_recipe_action_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricAmber, contentColor = Color.Black)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export Video", fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // Save Recipe Dialog
        if (showSaveRecipeDialog) {
            androidx.compose.ui.window.Dialog(onDismissRequest = { showSaveRecipeDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = DarkSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Save Viewing Recipe",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Stores all framing, cuts, speeds, and color grades locally in Room for instant recall.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedTextField(
                            value = recipeNameInput,
                            onValueChange = { recipeNameInput = it },
                            label = { Text("Recipe Name") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("recipe_name_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberTeal,
                                unfocusedBorderColor = BorderStroke
                            )
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OutlinedButton(onClick = { showSaveRecipeDialog = false }) {
                                Text("Cancel", color = TextSecondary)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    viewModel.saveCurrentRecipe(recipeNameInput)
                                    showSaveRecipeDialog = false
                                    Toast.makeText(context, "Recipe saved: $recipeNameInput", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberTeal, contentColor = Color.Black),
                                modifier = Modifier.testTag("confirm_save_recipe_button")
                            ) {
                                Text("Save", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Saved Recipes Bottom Sheet
        if (showSavedRecipesSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSavedRecipesSheet = false },
                containerColor = DarkSurface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "SAVED RECIPES (${savedRecipesList.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CyberTeal
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    if (savedRecipesList.isEmpty()) {
                        Text("No saved recipes yet.", color = TextMuted, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(30.dp))
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.height(320.dp)
                        ) {
                            items(savedRecipesList) { entity ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            try {
                                                val loaded = ViewingRecipe.fromJsonString(entity.recipeJson)
                                                viewModel.loadRecipe(loaded)
                                                showSavedRecipesSheet = false
                                                Toast.makeText(context, "Loaded recipe: ${loaded.name}", Toast.LENGTH_SHORT).show()
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Failed to load recipe", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = entity.name,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = "Source: ${entity.sourceIdentifier} • ${SimpleDateFormat("MMM d, HH:mm", Locale.US).format(Date(entity.modifiedAt))}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextMuted,
                                                fontSize = 11.sp
                                            )
                                        }
                                        IconButton(
                                            onClick = { viewModel.deleteSavedRecipe(entity.id) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete Recipe", tint = CoralFlame)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// Sub-Panels for each Viewing Experience
// -------------------------------------------------------------------------

@Composable
private fun DirectorsCutPanel(
    viewModel: SkipFrameViewModel,
    recipe: ViewingRecipe,
    totalDurationMs: Long
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Segment Cuts (${recipe.segments.count { it.isEnabled }} of ${recipe.segments.size} active)",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 14.sp
                )
                Text(
                    text = "Reorder, toggle or generate rhythmic alternate cut sequences.",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
            Button(
                onClick = { viewModel.generateAlternateCut(rhythmTargetSec = 4) },
                colors = ButtonDefaults.buttonColors(containerColor = CyberTeal, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("alternate_cut_button")
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Alternate Cut", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (recipe.segments.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = DarkSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Playing entire source continuously. Tap 'Alternate Cut' to generate an automatic rhythmic edit, or add segments.",
                    color = TextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
        } else {
            // Display segment list
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                recipe.segments.take(6).forEachIndexed { index, seg ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (seg.isEnabled) DarkSurfaceVariant else DarkSurfaceContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("#${index + 1}", fontWeight = FontWeight.Bold, color = CyberTeal, fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${formatMs(seg.startMs)} → ${formatMs(seg.endMs)} (${String.format(Locale.US, "%.1fs", seg.durationMs / 1000.0)})",
                                    color = if (seg.isEnabled) TextPrimary else TextMuted,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Switch(
                                checked = seg.isEnabled,
                                onCheckedChange = { enabled ->
                                    val updated = recipe.segments.toMutableList()
                                    updated[index] = seg.copy(isEnabled = enabled)
                                    viewModel.updateRecipe { it.copy(segments = updated) }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = CyberTeal,
                                    checkedTrackColor = CyberTeal.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
            }

            // Clear cuts / reset to continuous button
            OutlinedButton(
                onClick = { viewModel.updateRecipe { it.copy(segments = emptyList()) } },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralFlame)
            ) {
                Text("Clear All Cuts (Play Full Source)", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun FocusLensPanel(
    viewModel: SkipFrameViewModel,
    recipe: ViewingRecipe
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Horizontal Mirror & Rotation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { viewModel.updateRecipe { it.copy(isMirrored = !it.isMirrored) } },
                modifier = Modifier.weight(1f).testTag("mirror_toggle_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (recipe.isMirrored) CyberTeal else DarkSurfaceVariant,
                    contentColor = if (recipe.isMirrored) Color.Black else TextPrimary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Flip, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (recipe.isMirrored) "Mirrored ON" else "Mirror", fontSize = 13.sp)
            }

            Button(
                onClick = {
                    val nextRot = (recipe.rotationDegrees + 90) % 360
                    viewModel.updateRecipe { it.copy(rotationDegrees = nextRot) }
                },
                modifier = Modifier.weight(1f).testTag("rotate_toggle_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (recipe.rotationDegrees != 0) ElectricAmber else DarkSurfaceVariant,
                    contentColor = if (recipe.rotationDegrees != 0) Color.Black else TextPrimary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.RotateRight, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("${recipe.rotationDegrees}°", fontSize = 13.sp)
            }
        }

        // Zoom Scale Slider (1.0x to 3.0x)
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Zoom Scale", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Text(String.format(Locale.US, "%.1fx", recipe.zoomScale), style = MaterialTheme.typography.bodySmall, color = CyberTeal, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = recipe.zoomScale,
                onValueChange = { z -> viewModel.updateRecipe { it.copy(zoomScale = z) } },
                valueRange = 1.0f..3.0f,
                colors = SliderDefaults.colors(thumbColor = CyberTeal, activeTrackColor = CyberTeal),
                modifier = Modifier.testTag("zoom_slider")
            )
        }

        // Aspect Ratio Modes
        Column {
            Text("Cinematic Aspect Ratio", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(AspectRatioMode.values()) { mode ->
                    val isSel = recipe.aspectRatio == mode
                    FilterChip(
                        selected = isSel,
                        onClick = { viewModel.updateRecipe { it.copy(aspectRatio = mode) } },
                        label = { Text(mode.displayName, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricAmber,
                            selectedLabelColor = Color.Black
                        )
                    )
                }
            }
        }

        // On-device honest disclaimer about subject tracking
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = DarkSurfaceContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Reframing respects native boundary limits. Automated AI subject-tracking is omitted to uphold zero-simulation honesty until an on-device neural model is verified.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                fontSize = 10.sp,
                modifier = Modifier.padding(10.dp)
            )
        }
    }
}

@Composable
private fun DreamcutPanel(
    viewModel: SkipFrameViewModel,
    recipe: ViewingRecipe,
    currentPosMs: Long,
    totalDurationMs: Long
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Speed modulation chips
        Column {
            Text("Playback Speed", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Spacer(modifier = Modifier.height(6.dp))
            val speedOptions = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(speedOptions) { spd ->
                    val isSel = kotlin.math.abs(recipe.playbackSpeed - spd) < 0.05f
                    FilterChip(
                        selected = isSel,
                        onClick = { viewModel.updateRecipe { it.copy(playbackSpeed = spd) } },
                        label = { Text("${spd}x", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyberTeal,
                            selectedLabelColor = Color.Black
                        )
                    )
                }
            }
        }

        // Mute on Speed Change Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Mute Audio on Speed Shift", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                Text("Silences extreme high/low pitch artifacts", color = TextMuted, fontSize = 11.sp)
            }
            Switch(
                checked = recipe.muteAudioOnSpeedChange,
                onCheckedChange = { m -> viewModel.updateRecipe { it.copy(muteAudioOnSpeedChange = m) } },
                colors = SwitchDefaults.colors(checkedThumbColor = CyberTeal)
            )
        }

        // Loop selected range toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Loop Section", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                Text(
                    text = if (recipe.isLoopEnabled) "${formatMs(recipe.loopStartMs)} → ${formatMs(recipe.loopEndMs)}" else "Disabled",
                    color = if (recipe.isLoopEnabled) ElectricAmber else TextMuted,
                    fontSize = 11.sp
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (recipe.isLoopEnabled) {
                    OutlinedButton(
                        onClick = {
                            // Set loop around current playback position (±5s)
                            val start = (currentPosMs - 3000L).coerceAtLeast(0L)
                            val end = (currentPosMs + 3000L).coerceAtMost(totalDurationMs)
                            viewModel.updateRecipe { it.copy(loopStartMs = start, loopEndMs = end) }
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Set Here", fontSize = 11.sp, color = CyberTeal)
                    }
                }
                Switch(
                    checked = recipe.isLoopEnabled,
                    onCheckedChange = { enabled ->
                        val end = if (recipe.loopEndMs > 0) recipe.loopEndMs else totalDurationMs
                        viewModel.updateRecipe { it.copy(isLoopEnabled = enabled, loopEndMs = end) }
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = ElectricAmber)
                )
            }
        }

        // Frame Hold at selected moment
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Moment Frame Hold", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                Text(
                    text = if (recipe.holdFrameAtMs != null) "Freeze at ${formatMs(recipe.holdFrameAtMs)} for ${recipe.holdDurationMs / 1000.0}s" else "Disabled",
                    color = if (recipe.holdFrameAtMs != null) ElectricAmber else TextMuted,
                    fontSize = 11.sp
                )
            }
            Button(
                onClick = {
                    if (recipe.holdFrameAtMs == null) {
                        viewModel.updateRecipe { it.copy(holdFrameAtMs = currentPosMs) }
                    } else {
                        viewModel.updateRecipe { it.copy(holdFrameAtMs = null) }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (recipe.holdFrameAtMs != null) CoralFlame else DarkSurfaceVariant,
                    contentColor = if (recipe.holdFrameAtMs != null) Color.Black else TextPrimary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (recipe.holdFrameAtMs != null) "Clear Hold" else "Hold Here", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun MoodEnginePanel(
    viewModel: SkipFrameViewModel,
    recipe: ViewingRecipe
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Atmospheric Presets (Live GPU Rendering)", style = MaterialTheme.typography.bodySmall, color = TextSecondary)

        // Presets Chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(MoodPreset.values()) { preset ->
                val isSel = recipe.moodPreset == preset
                FilterChip(
                    selected = isSel,
                    onClick = { viewModel.updateRecipe { it.copy(moodPreset = preset) } },
                    label = { Text(preset.displayName, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = when (preset) {
                            MoodPreset.NONE -> DarkSurfaceVariant
                            MoodPreset.MONOCHROME -> Color.White
                            MoodPreset.WARM -> ElectricAmber
                            MoodPreset.COOL -> CyberTeal
                            MoodPreset.HIGH_CONTRAST -> Color.LightGray
                            MoodPreset.VIGNETTE -> CoralFlame
                        },
                        selectedLabelColor = Color.Black
                    )
                )
            }
        }

        Text(
            text = recipe.moodPreset.description,
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            fontSize = 11.sp
        )

        // Effect Intensity Slider (when preset != NONE)
        if (recipe.moodPreset != MoodPreset.NONE) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Grade Intensity", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text("${(recipe.moodIntensity * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = CyberTeal, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = recipe.moodIntensity,
                    onValueChange = { i -> viewModel.updateRecipe { it.copy(moodIntensity = i) } },
                    valueRange = 0.1f..1.0f,
                    colors = SliderDefaults.colors(thumbColor = CyberTeal, activeTrackColor = CyberTeal),
                    modifier = Modifier.testTag("mood_intensity_slider")
                )
            }
        }
    }
}

@Composable
private fun ChaosDirectorPanel(
    viewModel: SkipFrameViewModel,
    recipe: ViewingRecipe
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Seed #${(recipe.randomSeed % 10000).let { if (it < 0) -it else it }}",
                    fontWeight = FontWeight.Bold,
                    color = CyberTeal,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 15.sp
                )
                Text(
                    text = "Stochastic algorithmic synthesis of cuts, speed, framing & grade.",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }

            Button(
                onClick = { viewModel.generateChaosRecipe() },
                colors = ButtonDefaults.buttonColors(containerColor = CyberTeal, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("regenerate_chaos_button")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Regenerate", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        // Active Recipe Spec Sheet
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = DarkSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("CHAOS SPECIFICATION:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = TextSecondary)
                Text("• Cuts: ${if (recipe.segments.isNotEmpty()) "${recipe.segments.size} rhythmic cuts" else "Continuous"}", fontSize = 12.sp, color = TextPrimary)
                Text("• Speed: ${recipe.playbackSpeed}x", fontSize = 12.sp, color = TextPrimary)
                Text("• Framing: ${if (recipe.isMirrored) "Mirrored" else "Standard"}, ${String.format(Locale.US, "%.1fx", recipe.zoomScale)} Zoom, ${recipe.aspectRatio.displayName}", fontSize = 12.sp, color = TextPrimary)
                Text("• Grade: ${recipe.moodPreset.displayName} (${(recipe.moodIntensity * 100).toInt()}%)", fontSize = 12.sp, color = TextPrimary)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    viewModel.updateRecipe { it.copy(isFavorite = !it.isFavorite) }
                    viewModel.saveCurrentRecipe(recipe.name)
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Default.Bookmark,
                    contentDescription = null,
                    tint = if (recipe.isFavorite) ElectricAmber else TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (recipe.isFavorite) "Favorited" else "Lock Favorite", fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = { viewModel.restoreOriginalRecipe() },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralFlame)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Restore Original", fontSize = 12.sp)
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0L)
    val mins = totalSec / 60
    val secs = totalSec % 60
    return String.format(Locale.US, "%02d:%02d", mins, secs)
}
