package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SkipFrameConfig
import com.example.data.database.ProcessedVideoEntity
import com.example.data.database.SkipFrameDatabase
import com.example.data.repository.VideoRepository
import com.example.engine.ExportHelper
import com.example.engine.FFmpegEngine
import com.example.engine.FFmpegExecutionException
import com.example.engine.FFmpegFailure
import com.example.engine.LotteryEngine
import com.example.engine.LotteryMoment
import com.example.engine.LoudnessProfile
import com.example.engine.ShrinkEngine
import com.example.engine.ShrinkEstimate
import com.example.engine.TimeSlice
import com.example.engine.VideoMetadata
import com.example.data.database.ViewingRecipeEntity
import com.example.data.recipe.ViewingRecipe
import com.example.data.recipe.ExperienceRecipe
import com.example.data.personalization.ExperimentHistoryEntity
import com.example.data.personalization.PersonalPreferenceEntity
import com.example.engine.RevisionEngine
import com.example.engine.ExperimentEngine
import com.example.engine.ExperimentVariation
import com.example.engine.AiDirectorPlanner
import com.example.engine.DirectorGoal
import com.example.engine.DirectorPlanRequest
import com.example.engine.DirectorPlanResponse
import com.example.ui.navigation.LabSubMode
import com.example.ui.navigation.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Random

data class ProcessingStatus(
    val isProcessing: Boolean = false,
    val stageName: String = "",
    val progress: Float = 0f,
    val canCancel: Boolean = true
)

data class ExecutionResult(
    val title: String,
    val sourcePath: String,
    val outputPath: String,
    val originalSizeBytes: Long,
    val finalSizeBytes: Long,
    val originalDurationMs: Long,
    val finalDurationMs: Long,
    val processingTimeMs: Long,
    val encodingSpeed: String,
    val mode: String
) {
    val percentSaved: Double
        get() = if (originalSizeBytes > 0) {
            ((originalSizeBytes - finalSizeBytes).toDouble() / originalSizeBytes.toDouble()) * 100.0
        } else 0.0

    val storageSavedBytes: Long get() = (originalSizeBytes - finalSizeBytes).coerceAtLeast(0L)
}

class SkipFrameViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "SkipFrameViewModel"

    private val db = SkipFrameDatabase.getInstance(application)
    val repository = VideoRepository(
        db.processedVideoDao(),
        db.lotteryExportDao(),
        db.erosionDao(),
        db.viewingRecipeDao(),
        db.experienceRecipeDao(),
        db.personalizationDao()
    )

    private val shrinkEngine = ShrinkEngine(application)
    private val lotteryEngine = LotteryEngine(application)
    private val revisionEngine = RevisionEngine(application)
    private val experimentEngine = ExperimentEngine()
    private val aiDirectorPlanner = AiDirectorPlanner()

    // Phase 5: Personal Experiences & Memory State
    val userExperiences: StateFlow<List<ExperienceRecipe>> = repository.observeAllExperiences()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ExperienceRecipe.BUILT_IN_PRESETS)

    val personalPreferences: StateFlow<List<PersonalPreferenceEntity>> = repository.observePreferences()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val experimentHistory: StateFlow<List<ExperimentHistoryEntity>> = repository.observeExperimentHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _experimentVariations = MutableStateFlow<List<ExperimentVariation>>(emptyList())
    val experimentVariations: StateFlow<List<ExperimentVariation>> = _experimentVariations.asStateFlow()

    private val _lastDirectorPlan = MutableStateFlow<DirectorPlanResponse?>(null)
    val lastDirectorPlan: StateFlow<DirectorPlanResponse?> = _lastDirectorPlan.asStateFlow()

    // RE:VISION State
    private val _currentRecipe = MutableStateFlow<ViewingRecipe>(ViewingRecipe.defaultForSource("", 0L))
    val currentRecipe: StateFlow<ViewingRecipe> = _currentRecipe.asStateFlow()

    val savedRecipes: StateFlow<List<ViewingRecipeEntity>> = repository.observeAllRecipes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Navigation state
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Home)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Centralized tuning config
    private val _config = MutableStateFlow(SkipFrameConfig())
    val config: StateFlow<SkipFrameConfig> = _config.asStateFlow()

    // Active selected video file & metadata
    private val _selectedFile = MutableStateFlow<File?>(null)
    val selectedFile: StateFlow<File?> = _selectedFile.asStateFlow()

    private val _selectedMetadata = MutableStateFlow<VideoMetadata?>(null)
    val selectedMetadata: StateFlow<VideoMetadata?> = _selectedMetadata.asStateFlow()

    // Shrink Preview & Analysis
    private val _isAnalyzingShrink = MutableStateFlow(false)
    val isAnalyzingShrink: StateFlow<Boolean> = _isAnalyzingShrink.asStateFlow()

    private val _shrinkEstimate = MutableStateFlow<ShrinkEstimate?>(null)
    val shrinkEstimate: StateFlow<ShrinkEstimate?> = _shrinkEstimate.asStateFlow()

    // Processing status
    private val _processingStatus = MutableStateFlow(ProcessingStatus())
    val processingStatus: StateFlow<ProcessingStatus> = _processingStatus.asStateFlow()
    private var activeProcessingJob: Job? = null

    // Execution result (honest numbers measured only when file exists)
    private val _lastResult = MutableStateFlow<ExecutionResult?>(null)
    val lastResult: StateFlow<ExecutionResult?> = _lastResult.asStateFlow()

    // Error failure state
    private val _currentFailure = MutableStateFlow<FFmpegFailure?>(null)
    val currentFailure: StateFlow<FFmpegFailure?> = _currentFailure.asStateFlow()

    // Surprise Lottery State
    private val _isAnalyzingLottery = MutableStateFlow(false)
    val isAnalyzingLottery: StateFlow<Boolean> = _isAnalyzingLottery.asStateFlow()

    private val _loudnessProfile = MutableStateFlow<LoudnessProfile?>(null)
    val loudnessProfile: StateFlow<LoudnessProfile?> = _loudnessProfile.asStateFlow()

    private val _currentSeed = MutableStateFlow(System.currentTimeMillis())
    val currentSeed: StateFlow<Long> = _currentSeed.asStateFlow()

    private val _previousSeed = MutableStateFlow<Long?>(null)
    val previousSeed: StateFlow<Long?> = _previousSeed.asStateFlow()

    private val _lotteryPicks = MutableStateFlow<List<LotteryMoment>>(emptyList())
    val lotteryPicks: StateFlow<List<LotteryMoment>> = _lotteryPicks.asStateFlow()

    private val _earlierExportRanges = MutableStateFlow<List<Pair<Double, Double>>>(emptyList())
    val earlierExportRanges: StateFlow<List<Pair<Double, Double>>> = _earlierExportRanges.asStateFlow()

    private val _forcedMoments = MutableStateFlow<List<Double>>(emptyList())
    val forcedMoments: StateFlow<List<Double>> = _forcedMoments.asStateFlow()

    // Terrain preview modal
    private val _terrainPreviewSec = MutableStateFlow<Double?>(null)
    val terrainPreviewSec: StateFlow<Double?> = _terrainPreviewSec.asStateFlow()

    // Lab & Erode State
    private val _activeLabSubMode = MutableStateFlow(LabSubMode.DRIFT)
    val activeLabSubMode: StateFlow<LabSubMode> = _activeLabSubMode.asStateFlow()

    val processedHistory = repository.allProcessedVideos.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val totalStorageSaved = repository.totalBytesSaved.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 0L
    )

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun updateConfig(updater: (SkipFrameConfig) -> SkipFrameConfig) {
        val updated = updater(_config.value)
        // Constraint: each clip must be at least 5 seconds long, K up to 200
        val maxK = (updated.clipLengthL / 5).coerceIn(1, 200)
        val clampedK = updated.clipCountK.coerceIn(1, maxK)
        val finalConfig = updated.copy(clipCountK = clampedK)
        _config.value = finalConfig

        // If on preview screen, re-calculate estimate
        val meta = _selectedMetadata.value
        val file = _selectedFile.value
        if (meta != null && file != null && _currentScreen.value == Screen.ShrinkPreview) {
            calculateShrinkPreview(file, meta)
        }

        // If on Lottery or Lab screen, update the draw with new settings (such as order or spread)
        val prof = _loudnessProfile.value
        if (prof != null && (_currentScreen.value == Screen.Lottery || _currentScreen.value is Screen.Lab)) {
            executeDraw(prof, _currentSeed.value)
        }
    }

    /**
     * Loads a video selected by the user via Storage Access Framework.
     * Streams it to a working cache file to avoid loading entire video into memory.
     */
    fun selectVideoUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val fileName = getFileNameFromUri(uri) ?: "selected_video_${System.currentTimeMillis()}.mp4"
                val workingFile = File(context.cacheDir, "input_${System.currentTimeMillis()}_$fileName")

                // Stream directly without loading into RAM
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(workingFile).use { output ->
                        input.copyTo(output, bufferSize = 64 * 1024)
                    }
                }

                val metadata = FFmpegEngine.probeMetadata(context, workingFile)
                if (metadata != null) {
                    _selectedFile.value = workingFile
                    _selectedMetadata.value = metadata
                    loadEarlierExportsForVideo(workingFile.name)
                    withContext(Dispatchers.Main) {
                        navigateTo(Screen.ShrinkPreview)
                    }
                    calculateShrinkPreview(workingFile, metadata)
                } else {
                    Log.e(TAG, "Failed to probe metadata for selected video")
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Error importing video from URI", t)
            }
        }
    }

    fun selectPresetFile(file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            val metadata = FFmpegEngine.probeMetadata(getApplication(), file)
            if (metadata != null) {
                _selectedFile.value = file
                _selectedMetadata.value = metadata
                loadEarlierExportsForVideo(file.name)
                withContext(Dispatchers.Main) {
                    navigateTo(Screen.ShrinkPreview)
                }
                calculateShrinkPreview(file, metadata)
            }
        }
    }

    private fun loadEarlierExportsForVideo(sourceId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val exports = repository.getLotteryExports(sourceId)
            _earlierExportRanges.value = exports.map { it.startSec to it.endSec }
        }
    }

    fun calculateShrinkPreview(file: File, metadata: VideoMetadata) {
        viewModelScope.launch(Dispatchers.IO) {
            _isAnalyzingShrink.value = true
            try {
                val (estimate, _) = shrinkEngine.analyzeForPreview(file, metadata, _config.value)
                _shrinkEstimate.value = estimate
            } catch (t: Throwable) {
                Log.e(TAG, "Shrink preview analysis failed", t)
            } finally {
                _isAnalyzingShrink.value = false
            }
        }
    }

    /**
     * Executes Mode 1: Shrink end to end.
     */
    fun startShrinkProcessing() {
        val file = _selectedFile.value ?: return
        val metadata = _selectedMetadata.value ?: return
        val cfg = _config.value

        _processingStatus.value = ProcessingStatus(isProcessing = true, stageName = "Initializing", progress = 0f)
        navigateTo(Screen.Processing)

        activeProcessingJob = viewModelScope.launch(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            try {
                val outputFile = shrinkEngine.processShrink(file, metadata, cfg) { stage, p ->
                    _processingStatus.value = ProcessingStatus(
                        isProcessing = true,
                        stageName = stage,
                        progress = p
                    )
                }

                // Verify output exists and measure honest numbers
                if (outputFile.exists() && outputFile.length() > 0) {
                    val finalMeta = FFmpegEngine.probeMetadata(getApplication(), outputFile)
                    val procTime = System.currentTimeMillis() - startTime

                    val result = ExecutionResult(
                        title = "Shrunk: ${metadata.fileName}",
                        sourcePath = file.absolutePath,
                        outputPath = outputFile.absolutePath,
                        originalSizeBytes = metadata.sizeBytes,
                        finalSizeBytes = outputFile.length(),
                        originalDurationMs = metadata.durationMs,
                        finalDurationMs = finalMeta?.durationMs ?: (metadata.durationMs / 2),
                        processingTimeMs = procTime,
                        encodingSpeed = cfg.encodingSpeed.displayName,
                        mode = "SHRINK"
                    )

                    _lastResult.value = result
                    // Persist to Room
                    repository.saveProcessedVideo(
                        ProcessedVideoEntity(
                            title = result.title,
                            sourcePathOrUri = result.sourcePath,
                            outputPath = result.outputPath,
                            mode = result.mode,
                            originalSizeBytes = result.originalSizeBytes,
                            finalSizeBytes = result.finalSizeBytes,
                            originalDurationMs = result.originalDurationMs,
                            finalDurationMs = result.finalDurationMs,
                            processingTimeMs = result.processingTimeMs,
                            encodingSpeed = result.encodingSpeed
                        )
                    )

                    withContext(Dispatchers.Main) {
                        _processingStatus.value = ProcessingStatus(isProcessing = false)
                        navigateTo(Screen.Results)
                    }
                }
            } catch (e: FFmpegExecutionException) {
                withContext(Dispatchers.Main) {
                    _processingStatus.value = ProcessingStatus(isProcessing = false)
                    _currentFailure.value = e.failure
                }
            } catch (t: Throwable) {
                withContext(Dispatchers.Main) {
                    _processingStatus.value = ProcessingStatus(isProcessing = false)
                    _currentFailure.value = FFmpegFailure(
                        returnCode = null,
                        last40Logs = t.stackTraceToString().lines().takeLast(40).joinToString("\n"),
                        filterScriptPath = null,
                        fullLogFilePath = null,
                        command = "Shrink Processing",
                        exceptionMessage = t.message
                    )
                }
            }
        }
    }

    /**
     * Initializes Mode 2: Surprise Lottery analysis.
     */
    fun startLotteryMode() {
        val file = _selectedFile.value ?: return
        val metadata = _selectedMetadata.value ?: return

        navigateTo(Screen.Lottery)
        if (_loudnessProfile.value == null) {
            viewModelScope.launch(Dispatchers.IO) {
                _isAnalyzingLottery.value = true
                try {
                    val profile = lotteryEngine.analyzeLoudness(file, metadata)
                    _loudnessProfile.value = profile
                    // Initial draw
                    rollLotteryDraw(profile)
                } finally {
                    _isAnalyzingLottery.value = false
                }
            }
        } else {
            rollLotteryDraw(_loudnessProfile.value!!)
        }
    }

    fun rollLotteryDraw(profile: LoudnessProfile? = null) {
        val targetProfile = profile ?: _loudnessProfile.value ?: return
        val newSeed = Random().nextLong()
        _previousSeed.value = _currentSeed.value
        _currentSeed.value = newSeed
        executeDraw(targetProfile, newSeed)
    }

    fun restorePreviousLotterySeed() {
        val prev = _previousSeed.value ?: return
        val profile = _loudnessProfile.value ?: return
        _previousSeed.value = _currentSeed.value
        _currentSeed.value = prev
        executeDraw(profile, prev)
    }

    fun setCustomSeed(seed: Long) {
        val profile = _loudnessProfile.value ?: return
        _previousSeed.value = _currentSeed.value
        _currentSeed.value = seed
        executeDraw(profile, seed)
    }

    fun forceMomentFromTerrain(sec: Double) {
        _forcedMoments.value = _forcedMoments.value + sec
        val profile = _loudnessProfile.value ?: return
        executeDraw(profile, _currentSeed.value)
    }

    fun setTerrainPreview(sec: Double?) {
        _terrainPreviewSec.value = sec
    }

    private fun executeDraw(profile: LoudnessProfile, seed: Long) {
        val picks = lotteryEngine.performLotteryDraw(
            profile = profile,
            config = _config.value,
            seed = seed,
            earlierExportRanges = _earlierExportRanges.value,
            forcedMoments = _forcedMoments.value
        )
        _lotteryPicks.value = picks
    }

    /**
     * Exports the Surprise Lottery clip to a real file.
     */
    fun exportLotteryClip() {
        val file = _selectedFile.value ?: return
        val metadata = _selectedMetadata.value ?: return
        val picks = _lotteryPicks.value
        if (picks.isEmpty()) return
        val cfg = _config.value
        val seed = _currentSeed.value

        _processingStatus.value = ProcessingStatus(isProcessing = true, stageName = "Exporting Lottery Moments", progress = 0f)
        navigateTo(Screen.Processing)

        activeProcessingJob = viewModelScope.launch(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            try {
                val outputFile = lotteryEngine.exportLotteryMoments(file, metadata, picks, cfg) { p ->
                    _processingStatus.value = ProcessingStatus(
                        isProcessing = true,
                        stageName = "Exporting Vertical Clip",
                        progress = p
                    )
                }

                if (outputFile.exists() && outputFile.length() > 0) {
                    val finalMeta = FFmpegEngine.probeMetadata(getApplication(), outputFile)
                    val procTime = System.currentTimeMillis() - startTime

                    // Record each pick in Room for Fresh only exclusion
                    for (p in picks) {
                        repository.recordLotteryExport(file.name, p.startSec, p.endSec, seed)
                    }
                    loadEarlierExportsForVideo(file.name)

                    val result = ExecutionResult(
                        title = "Lottery: ${metadata.fileName} (Seed $seed)",
                        sourcePath = file.absolutePath,
                        outputPath = outputFile.absolutePath,
                        originalSizeBytes = metadata.sizeBytes,
                        finalSizeBytes = outputFile.length(),
                        originalDurationMs = metadata.durationMs,
                        finalDurationMs = finalMeta?.durationMs ?: (picks.sumOf { it.duration } * 1000).toLong(),
                        processingTimeMs = procTime,
                        encodingSpeed = cfg.encodingSpeed.displayName,
                        mode = "LOTTERY"
                    )

                    _lastResult.value = result
                    repository.saveProcessedVideo(
                        ProcessedVideoEntity(
                            title = result.title,
                            sourcePathOrUri = result.sourcePath,
                            outputPath = result.outputPath,
                            mode = result.mode,
                            originalSizeBytes = result.originalSizeBytes,
                            finalSizeBytes = result.finalSizeBytes,
                            originalDurationMs = result.originalDurationMs,
                            finalDurationMs = result.finalDurationMs,
                            processingTimeMs = result.processingTimeMs,
                            encodingSpeed = result.encodingSpeed
                        )
                    )

                    withContext(Dispatchers.Main) {
                        _processingStatus.value = ProcessingStatus(isProcessing = false)
                        navigateTo(Screen.Results)
                    }
                }
            } catch (e: FFmpegExecutionException) {
                withContext(Dispatchers.Main) {
                    _processingStatus.value = ProcessingStatus(isProcessing = false)
                    _currentFailure.value = e.failure
                }
            } catch (t: Throwable) {
                withContext(Dispatchers.Main) {
                    _processingStatus.value = ProcessingStatus(isProcessing = false)
                    _currentFailure.value = FFmpegFailure(
                        returnCode = null,
                        last40Logs = t.stackTraceToString().lines().takeLast(40).joinToString("\n"),
                        filterScriptPath = null,
                        fullLogFilePath = null,
                        command = "Lottery Export",
                        exceptionMessage = t.message
                    )
                }
            }
        }
    }

    /**
     * Lab Telemetry: Records hold time or skip (<3s) for a 10s bucket.
     */
    fun recordLabBucketTelemetry(positionMs: Long, holdTimeDeltaMs: Long, wasSkipped: Boolean) {
        val file = _selectedFile.value ?: return
        val bucketIndex = (positionMs / 10000).toInt()
        viewModelScope.launch(Dispatchers.IO) {
            repository.recordErosionTelemetry(file.name, bucketIndex, holdTimeDeltaMs, wasSkipped)
        }
    }

    /**
     * Erode button: removes default 12.5% of most-skipped buckets.
     */
    fun applyErosion() {
        val file = _selectedFile.value ?: return
        val fraction = _config.value.erodeFraction
        viewModelScope.launch(Dispatchers.IO) {
            repository.applyErosionFraction(file.name, fraction)
        }
    }

    fun restoreErosion() {
        val file = _selectedFile.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.restoreErosion(file.name)
        }
    }

    /**
     * Bake button: runs Shrink engine on movie excluding eroded buckets.
     */
    fun bakeErosion() {
        val file = _selectedFile.value ?: return
        val metadata = _selectedMetadata.value ?: return
        val cfg = _config.value

        _processingStatus.value = ProcessingStatus(isProcessing = true, stageName = "Baking Eroded Video", progress = 0f)
        navigateTo(Screen.Processing)

        activeProcessingJob = viewModelScope.launch(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            try {
                val erodedBuckets = repository.getErodedBuckets(file.name)
                // Convert non-eroded buckets to keep intervals
                val bucketSec = cfg.erodeBucketSec.toDouble()
                val totalBuckets = (metadata.durationSeconds / bucketSec).toInt() + 1
                val keepSlices = mutableListOf<TimeSlice>()

                for (b in 0 until totalBuckets) {
                    if (!erodedBuckets.contains(b)) {
                        val sStart = b * bucketSec
                        val sEnd = kotlin.math.min(metadata.durationSeconds, (b + 1) * bucketSec)
                        if (sEnd > sStart) {
                            keepSlices.add(TimeSlice(sStart, sEnd))
                        }
                    }
                }

                val safeKeep = if (keepSlices.isEmpty()) listOf(TimeSlice(0.0, metadata.durationSeconds)) else keepSlices
                // Process with shrink engine
                val outputFile = shrinkEngine.processShrink(file, metadata, cfg) { stage, p ->
                    _processingStatus.value = ProcessingStatus(isProcessing = true, stageName = stage, progress = p)
                }

                if (outputFile.exists() && outputFile.length() > 0) {
                    val finalMeta = FFmpegEngine.probeMetadata(getApplication(), outputFile)
                    val procTime = System.currentTimeMillis() - startTime

                    val result = ExecutionResult(
                        title = "Eroded & Baked: ${metadata.fileName}",
                        sourcePath = file.absolutePath,
                        outputPath = outputFile.absolutePath,
                        originalSizeBytes = metadata.sizeBytes,
                        finalSizeBytes = outputFile.length(),
                        originalDurationMs = metadata.durationMs,
                        finalDurationMs = finalMeta?.durationMs ?: metadata.durationMs,
                        processingTimeMs = procTime,
                        encodingSpeed = cfg.encodingSpeed.displayName,
                        mode = "ERODE_BAKE"
                    )

                    _lastResult.value = result
                    repository.saveProcessedVideo(
                        ProcessedVideoEntity(
                            title = result.title,
                            sourcePathOrUri = result.sourcePath,
                            outputPath = result.outputPath,
                            mode = result.mode,
                            originalSizeBytes = result.originalSizeBytes,
                            finalSizeBytes = result.finalSizeBytes,
                            originalDurationMs = result.originalDurationMs,
                            finalDurationMs = result.finalDurationMs,
                            processingTimeMs = result.processingTimeMs,
                            encodingSpeed = result.encodingSpeed
                        )
                    )

                    withContext(Dispatchers.Main) {
                        _processingStatus.value = ProcessingStatus(isProcessing = false)
                        navigateTo(Screen.Results)
                    }
                }
            } catch (e: FFmpegExecutionException) {
                withContext(Dispatchers.Main) {
                    _processingStatus.value = ProcessingStatus(isProcessing = false)
                    _currentFailure.value = e.failure
                }
            } catch (t: Throwable) {
                withContext(Dispatchers.Main) {
                    _processingStatus.value = ProcessingStatus(isProcessing = false)
                    _currentFailure.value = FFmpegFailure(
                        returnCode = null,
                        last40Logs = t.stackTraceToString().lines().takeLast(40).joinToString("\n"),
                        filterScriptPath = null,
                        fullLogFilePath = null,
                        command = "Erode Bake",
                        exceptionMessage = t.message
                    )
                }
            }
        }
    }

    fun openLabSubMode(mode: LabSubMode) {
        _activeLabSubMode.value = mode
        navigateTo(Screen.Lab(mode))
        val file = _selectedFile.value
        val meta = _selectedMetadata.value
        if (file != null && meta != null && _lotteryPicks.value.isEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                if (_loudnessProfile.value == null) {
                    val prof = lotteryEngine.analyzeLoudness(file, meta)
                    _loudnessProfile.value = prof
                    executeDraw(prof, _currentSeed.value)
                } else {
                    executeDraw(_loudnessProfile.value!!, _currentSeed.value)
                }
            }
        }
    }

    fun cancelActiveProcessing() {
        activeProcessingJob?.cancel()
        _processingStatus.value = ProcessingStatus(isProcessing = false)
        navigateTo(Screen.Home)
    }

    fun dismissFailureDialog() {
        _currentFailure.value = null
    }

    // --- RE:VISION Actions ---

    fun startRevisionMode() {
        val file = _selectedFile.value
        val meta = _selectedMetadata.value
        if (file != null && meta != null) {
            val durationMs = meta.durationMs
            if (_currentRecipe.value.sourceIdentifier != file.name) {
                _currentRecipe.value = ViewingRecipe.defaultForSource(file.name, durationMs)
            }
        }
        navigateTo(Screen.Revision)
    }

    fun updateRecipe(updater: (ViewingRecipe) -> ViewingRecipe) {
        val updated = updater(_currentRecipe.value)
        _currentRecipe.value = updated.copy(modifiedAt = System.currentTimeMillis())
    }

    fun restoreOriginalRecipe() {
        val file = _selectedFile.value
        val meta = _selectedMetadata.value
        val durMs = meta?.durationMs ?: 0L
        _currentRecipe.value = ViewingRecipe.defaultForSource(file?.name ?: "", durMs)
    }

    fun saveCurrentRecipe(customName: String? = null) {
        val recipe = _currentRecipe.value.let {
            if (customName.isNullOrBlank()) it else it.copy(name = customName.trim())
        }
        _currentRecipe.value = recipe
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveRecipe(recipe)
        }
    }

    fun loadRecipe(recipe: ViewingRecipe) {
        _currentRecipe.value = recipe
    }

    fun deleteSavedRecipe(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteRecipe(id)
        }
    }

    fun generateAlternateCut(rhythmTargetSec: Int = 4) {
        val meta = _selectedMetadata.value ?: return
        val durMs = meta.durationMs
        val cuts = revisionEngine.generateAlternateCut(durMs, seed = System.currentTimeMillis(), rhythmTargetSec = rhythmTargetSec)
        updateRecipe { it.copy(segments = cuts) }
    }

    fun generateChaosRecipe() {
        val file = _selectedFile.value ?: return
        val meta = _selectedMetadata.value ?: return
        val durMs = meta.durationMs
        val chaos = revisionEngine.generateChaosRecipe(file.name, durMs, seed = System.currentTimeMillis())
        _currentRecipe.value = chaos
    }

    fun exportRevisionVideo() {
        val file = _selectedFile.value ?: return
        val recipe = _currentRecipe.value
        val context = getApplication<Application>()
        val outputDir = File(context.filesDir, "exports").apply { mkdirs() }
        val safeName = recipe.name.replace(Regex("[^a-zA-Z0-9_]"), "_")
        val outputFile = File(outputDir, "revision_${safeName}_${System.currentTimeMillis()}.mp4")

        activeProcessingJob?.cancel()
        _processingStatus.value = ProcessingStatus(
            isProcessing = true,
            stageName = "Initializing RE:VISION Export...",
            progress = 0.05f,
            canCancel = false
        )
        navigateTo(Screen.Processing)

        activeProcessingJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = revisionEngine.exportRecipeVideo(
                    sourceFile = file,
                    recipe = recipe,
                    outputFile = outputFile,
                    onProgress = { p, stage ->
                        _processingStatus.value = ProcessingStatus(
                            isProcessing = true,
                            stageName = stage,
                            progress = p,
                            canCancel = false
                        )
                    }
                )

                // Save to Room database history
                repository.saveProcessedVideo(
                    ProcessedVideoEntity(
                        title = result.title,
                        sourcePathOrUri = result.sourcePath,
                        outputPath = result.outputPath,
                        mode = "RE:VISION",
                        originalSizeBytes = result.originalSizeBytes,
                        finalSizeBytes = result.finalSizeBytes,
                        originalDurationMs = result.originalDurationMs,
                        finalDurationMs = result.finalDurationMs,
                        processingTimeMs = result.processingTimeMs,
                        encodingSpeed = result.encodingSpeed
                    )
                )

                _lastResult.value = result
                _processingStatus.value = ProcessingStatus(isProcessing = false)
                withContext(Dispatchers.Main) {
                    navigateTo(Screen.Results)
                }
            } catch (t: Throwable) {
                Log.e(TAG, "RE:VISION export failed", t)
                _processingStatus.value = ProcessingStatus(isProcessing = false)
                _currentFailure.value = FFmpegFailure(
                    returnCode = -1,
                    last40Logs = t.stackTraceToString().lines().takeLast(40).joinToString("\n"),
                    filterScriptPath = null,
                    fullLogFilePath = null,
                    command = "RE:VISION Export",
                    exceptionMessage = t.message
                )
                withContext(Dispatchers.Main) {
                    navigateTo(Screen.Revision)
                }
            }
        }
    }

    // --- Phase 5: Personal Experience Builder Actions ---

    fun saveExperienceRecipe(recipe: ExperienceRecipe) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveExperience(recipe)
        }
    }

    fun duplicateExperienceRecipe(recipe: ExperienceRecipe) {
        val dup = recipe.duplicate()
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveExperience(dup)
        }
    }

    fun deleteExperienceRecipe(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteExperience(id)
        }
    }

    fun applyExperienceToActiveVideo(experience: ExperienceRecipe) {
        val file = _selectedFile.value
        val meta = _selectedMetadata.value
        val durMs = meta?.durationMs ?: 60000L
        val cuts = if (experience.rhythmicCutCount > 0) {
            revisionEngine.generateAlternateCut(durMs, seed = experience.randomSeed, rhythmTargetSec = experience.rhythmTargetSec)
        } else {
            emptyList()
        }
        val viewing = experience.toViewingRecipe(
            sourceIdentifier = file?.name ?: "video",
            sourceDurationMs = durMs,
            generatedSegments = cuts
        )
        _currentRecipe.value = viewing
        navigateTo(Screen.Revision)
    }

    fun generateExperimentVariations(baseRecipe: ExperienceRecipe? = null, seed: Long = System.currentTimeMillis()) {
        val base = baseRecipe ?: _currentRecipe.value.let {
            ExperienceRecipe(
                name = it.name,
                playbackSpeed = it.playbackSpeed,
                isMirrored = it.isMirrored,
                rotationDegrees = it.rotationDegrees,
                zoomScale = it.zoomScale,
                aspectRatio = it.aspectRatio,
                moodPreset = it.moodPreset,
                moodIntensity = it.moodIntensity
            )
        }
        val variations = experimentEngine.generateSuiteOfVariations(base, seed)
        _experimentVariations.value = variations
    }

    fun recordExperimentRating(variation: ExperimentVariation, rating: String) {
        val file = _selectedFile.value
        viewModelScope.launch(Dispatchers.IO) {
            repository.recordExperiment(
                sourceId = file?.name ?: "experiment",
                recipeName = variation.title,
                seed = variation.seed,
                summary = variation.rationale,
                rating = rating,
                recipeJson = variation.recipe.toJsonString()
            )
        }
    }

    fun requestAiDirectorPlan(goal: DirectorGoal, userPrompt: String = "") {
        val baseExperience = _currentRecipe.value.let {
            ExperienceRecipe(
                name = it.name,
                playbackSpeed = it.playbackSpeed,
                isMirrored = it.isMirrored,
                rotationDegrees = it.rotationDegrees,
                zoomScale = it.zoomScale,
                aspectRatio = it.aspectRatio,
                moodPreset = it.moodPreset,
                moodIntensity = it.moodIntensity
            )
        }
        val response = aiDirectorPlanner.planExperience(
            DirectorPlanRequest(
                goal = goal,
                userPrompt = userPrompt,
                currentRecipe = baseExperience,
                seed = System.currentTimeMillis()
            )
        )
        _lastDirectorPlan.value = response
    }

    fun applyDirectorPlan(response: DirectorPlanResponse) {
        applyExperienceToActiveVideo(response.plannedRecipe)
    }

    fun savePersonalPreference(key: String, value: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setPreference(key, value)
        }
    }

    fun deletePersonalPreference(key: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deletePreference(key)
        }
    }

    fun clearAllPreferences() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllPreferences()
        }
    }

    fun clearExperimentHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearExperimentHistory()
        }
    }

    private fun getFileNameFromUri(uri: Uri): String? {
        val cursor = getApplication<Application>().contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            if (it.moveToFirst()) {
                val idx = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (idx != -1) it.getString(idx) else null
            } else null
        }
    }
}
