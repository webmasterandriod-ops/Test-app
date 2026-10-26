# SkipFrame Progress Log

## Build Status
- **Current Milestone**: All Milestones (M1 to M6) Fully Built & Integrated
- **Architecture**: Kotlin + Jetpack Compose (M3 dark cinematic theme) + Media3 ExoPlayer + FFmpegKit Full GPL (v8.1.7) + Room Database.
- **NDK ABIs**: Restricted to `arm64-v8a` and `x86_64`
- **ProGuard**: Rules applied for `com.arthenica.ffmpegkit.**`
- **Tuning Config**: Centralized in `SkipFrameConfig` data class

---

## Milestone Status

### M1: Project setup and engine — COMPLETED
- [x] FFmpeg binding: `dev.ffmpegkit-maintained:ffmpeg-kit-full-gpl:8.1.7` added with `com.arthenica:smart-exception-java:0.2.1`.
- [x] Application ID set to `com.aistudio.skipframe.krpzx`.
- [x] NDK ABI filters restricted to `arm64-v8a` and `x86_64`.
- [x] Custom adaptive launcher icon with razor/film skip geometry created (foreground layer, linear gradient background, and raster mipmaps).
- [x] FileProvider declared in `AndroidManifest.xml` with `res/xml/file_paths.xml` for sharing videos and logs.
- [x] Media3 ExoPlayer (`1.5.1`) integrated for playback.
- [x] Every FFmpeg and FFprobe call wrapped in `try/catch(Throwable)`.
- [x] FFmpeg failure handler: displays return code, last 40 lines of `session.allLogsAsString`, filter script path, saves full log to file (`ffmpeg_failure_<timestamp>.txt`), and provides "Share Log" button.
- [x] Tested: Gradle build compilation, dependency resolution, Room database instantiation test.

### M2: Shrink mode end to end — COMPLETED
- [x] Unified config object: `SkipFrameConfig` containing silence thresholds, quiet-parts sensitivity, edge cushion, N, crossfade, denoise, encoding speeds, and resolutions.
- [x] Silence detection via FFmpeg `silencedetect` relative to file's own loudness and quiet sensitivity.
- [x] 120ms cushion preserved on each side of every cut.
- [x] Silence slices removed from video and audio together.
- [x] Every N-th frame skipped in sequence with matching audio slice ($\Delta t = 1/\text{fps}$) to close gaps and maintain synchronization.
- [x] 3ms audio crossfade filter applied at cuts.
- [x] Light 3D denoise filter (`hqdn3d`) before encoding.
- [x] Filter scripts written to disk files (`.filter`), passed via `-filter_complex_script` with interleaved `[v$i][a$i]` pads per segment for FFmpeg `concat=v=1:a=1`.
- [x] Multi-speed encoding: Fast (`libx264 veryfast, CRF 26`), Balanced (`libx264 medium, CRF 23`), Clean (`libx264 slow, CRF 20`), Hardware (`h264_mediacodec`), with automatic hardware fallback if x264 fails.
- [x] Honest preview estimate labeled as estimate and split into silence and frame skipping.
- [x] Processing screen with real stages, progress bar, video specs, and cancel.
- [x] Results screen with actual measured sizes, durations, storage saved %, processing time, in-app player, Save to Gallery, and Share.
- [x] Tested: Cut slice math tests, preview analysis logic, sample generator tool for on-device testing, filter link verification.

### M3: Surprise Lottery analysis, draw and debug view — COMPLETED
- [x] Decodes audio stream only to mono 8 kHz raw PCM.
- [x] Measures loudness $e(t)$ in dB per 100ms block (800 samples).
- [x] Computes $s(t) = e(t) - \text{median of surrounding 30s}$.
- [x] Computes rate of rise $r(t) = e(t) - e(t - 0.5s)$.
- [x] Computes composite energy $m(t) = \max(0, s(t)) + 0.5 \cdot r(t)$ converted to percentile rank $0..1$ over file.
- [x] Finds local peaks of $r(t)$, establishes window $[t - 0.4s, t - 0.4s + L]$.
- [x] Computes clip score $W = \text{mean of top 20\% of } m$ inside window.
- [x] Weighted draw without replacement: $P(i) = W_i^g / \sum W_j^g$ where $g = 4(1 - c)$.
- [x] Seed random by default, displayed, copyable, with Re-roll and Previous result buttons.
- [x] Raw mode ON vs OFF (snapping to quiet minima, skipping first 2% / last 8%, 120s minimum gap with automatic shrink, hook bonus).
- [x] Debug view screen printing loudness statistics and candidate picks as plain text.
- [x] Tested: Weighted distribution math, seed consistency, debug view text generation.

### M4: Lottery screens, export, Mismatch, Fresh only, Terrain view — COMPLETED
- [x] Lottery screen with sliders and direct numeric inputs (L up to 60 min, K up to 200 with min 5s per clip, chaos), switches (Raw mode, Mismatch, Fresh only, Spread across movie).
- [x] Target length display with "Target length: X min" and average clip length ($L/K$).
- [x] Picks guaranteed strictly non-overlapping (adjacent slices allowed where $s_2 = e_1$).
- [x] Output shape option: "Original (no crop)" keeping native aspect ratio and resolution, 9:16 Crop, 9:16 Blur. Default dynamically set to Original when $L > 3$ min, 9:16 Crop when $L \le 3$ min.
- [x] "Spread across movie" feature: divides video into $K$ equal partitions and draws 1 candidate per section via loudness lottery.
- [x] "Order" selector with 4 modes: Random (default), Calm to intense (by score $W$), Intense to calm, and Reverse (source chronological reverse). Applied to both exports and Lab Drift mode.
- [x] Interactive Terrain view: smoothed $m(t)$ waveform across full width, pick span markers, shaded prior export regions.
- [x] Tap anywhere on Terrain view to preview 5 seconds in-app without encoding.
- [x] Long-press on Terrain view forces moment into next pick.
- [x] Fast input seeking (`-ss` before `-i`) slice extraction and vertical 9:16 or original aspect ratio export.
- [x] Mismatch mode: extra audio moment drawn and layered under pick's video.
- [x] Fresh only: Room database `LotteryExportEntity` tracking and overlap exclusion.
- [x] Tested: Canvas drawing logic, Room export entity persistence.

### M5: Lab (live experiments) — COMPLETED
- [x] No progress bar, no time display, no pause button.
- [x] Gestures: tap = next moment, hold = stay longer, swipe down = leave.
- [x] A. Drift: endless chain of 5-20s moments with audio crossfades and corner seed copy.
- [x] B. Split: independent muted video player and audio player on separate draws and 8-25s timers. Tap = new picture draw, double tap = new sound draw.
- [x] C. Mosaic: two stacked panels playing different moments muted; tapping a panel routes sound to it.
- [x] D. Erode: records per 10s bucket hold time and $<3$s skips in Room. Erode button removes 12.5% of most-skipped buckets; Bake button exports real file via Shrink engine; Restore button clears erosion.
- [x] Tested: ExoPlayer lifecycle in compose, telemetry recording in Room.

### M6: Optimization and UI refinements — COMPLETED
- [x] Sleek cinematic dark theme (Obsidian, Electric Amber, Cyber Teal, Coral Flame).
- [x] Touch targets compliant with $\ge 48$dp Material 3 guidelines.
- [x] Test tags added across all interactive elements (`select_video_button`, `lab_button`, `commit_shrink_button`, `reroll_button`, etc.).
- [x] MediaStore gallery export helper for zero-permission photo picker/gallery compliance.
- [x] Proactive local synthetic video generator for immediate on-device testing.

---

## Phase 3: RE:VISION Viewing Engine — COMPLETED
- [x] Dedicated RE:VISION viewing mode with large live hardware-accelerated preview surface.
- [x] Full playback controls: play/pause, seek slider with monospace timestamps, ±5s jump buttons.
- [x] Horizontal viewing experience tabs:
  - **Director's Cut**: Sequence of source cuts with live timeline orchestration, automatic rhythmic alternate cut generator (`revisionEngine.generateAlternateCut`).
  - **Focus Lens**: Live GPU horizontal mirroring (`GraphicsLayer.scaleX = -zoom`), rotation (0, 90, 180, 270), zoom (1.0x to 3.0x), and cinematic aspect ratios (16:9, 9:16, 1:1, 4:3, 2.39:1). Disclaims automated tracking until verified neural model.
  - **Dreamcut**: Live speed modulation (0.25x to 2.0x), loop range handler, frame hold handler with countdown badge, and audio mute protection on speed shift. Reverse playback handled via accurate FFmpeg pre-rendered cache without seek stutter.
  - **Mood Engine**: Live GPU color grading via Skia `ColorMatrix` (Noir Monochrome, Amber Warmth, Cyan Dusk, High Contrast) and Canvas radial gradient vignette. Zero CPU rendering overhead.
  - **Chaos Director**: Deterministic algorithmic synthesis combining cuts, speed, framing, and mood with reproducible seeds.
- [x] Recipe persistence in Room (`viewing_recipes` table with `MIGRATION_1_2`).
- [x] Full export pipeline via FFmpeg filter scripts with software (`libx264`) and hardware (`h264_mediacodec`) encoding fallback.
- [x] Tested: Live transformation matrix rendering, Room recipe persistence test, export filter graph builder.

---

## Phase 5: Personal Viewing Intelligence — COMPLETED
- [x] **My Experiences**:
  - Fully decoupled, portable `ExperienceRecipe` format with `schemaVersion = 1` and strict schema validation.
  - Actions: Create, Duplicate, Rename, Edit, Preview/Apply to video, Save, Reset, Delete, Export JSON, Import JSON.
  - Built-in presets: 2.39:1 Anamorphic Noir, Warm Sunset Dream, 9:16 Vertical Highlight, Brisk Rhythmic Montage.
  - JSON recipe import validates every operation against the local Capability Registry before activation.
- [x] **Capability Registry** (`CapabilityRegistry`):
  - Shared registry declaring real on-device operations (`cap_framing_mirror`, `cap_framing_rotation`, `cap_framing_zoom`, `cap_framing_aspect`, `cap_timing_speed`, `cap_timing_loop`, `cap_timing_hold`, `cap_timing_reverse`, `cap_visual_grade`, `cap_audio_mute`, `cap_cuts_sequence`, `cap_chaos_generator`).
  - Each capability declares parameters, valid ranges, execution mode (Live Only, Offline Only, Both), resource cost, and failure recovery.
  - Truth in advertising: Reverse playback explicitly declared as offline/pre-render only.
- [x] **Experiment Engine** (`ExperimentEngine`):
  - Deterministic generation of single alternatives and 4-variation comparison suites (Rhythmic Pulse, Atmospheric Drift, 2.39:1 Cinema Noir, Mirrored Vertical).
  - Reproducible with fixed seeds. Actions: Preview in RE:VISION, Keep Favorite, Dislike/Rate, Regenerate.
- [x] **Local Personalization Memory**:
  - Room entities: `personal_preferences` and `experiment_history` with `MIGRATION_2_3`.
  - Zero cloud, 100% device-local storage.
  - User controls: inspect ratings, clear preferences, clear experiment history.
- [x] **AI Director Interface** (`AiDirectorPlanner`):
  - Model-independent contract: `DirectorGoal`, `DirectorPlanRequest`, `DirectorPlanResponse`.
  - Formulates structured requests (Tightly Paced, Atmospheric Dream, Vertical Social, Cinematic Noir, Stochastic Edit) backed by a transparent rule-based planner.
  - Generates natural-language explanation bullets ("What changed & why") and declares capabilities used.
  - Ready for future local on-device Gemini Nano / LLM integration without breaking architecture.
- [x] **Testing & Verification**:
  - `ExampleRobolectricTest`: Room migrations (v1 → v2 → v3), JSON serialization round-trips, schema validation, Capability Registry integrity, AiDirectorPlanner determinism, ExperimentEngine variation suite, and Personalization memory clear.
  - Build & compile verified: `BUILD SUCCESSFUL` across all 33 tasks in JVM test suite.
