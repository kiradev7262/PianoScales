package com.example.pianoscales.ui.me

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pianoscales.domain.profile.ProfileRepository
import com.example.pianoscales.domain.profile.StreakInfo
import com.example.pianoscales.domain.progress.BeginnerProgressRepository
import com.example.pianoscales.domain.progress.LessonProgress
import com.example.pianoscales.domain.progress.ProgressRepository
import com.example.pianoscales.theory.ConceptCategory
import com.example.pianoscales.theory.ConceptType
import com.example.pianoscales.theory.Note
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MeUiState(
    val displayName: String = "Learner",
    val profileImagePath: String? = null,
    val streakInfo: StreakInfo = StreakInfo(0, 0, 0),
    val todayGoal: TodayGoal? = null,
    val todayActivities: List<String> = emptyList(),
    val progressMetrics: ProgressMetrics = ProgressMetrics()
)

data class TodayGoal(
    val title: String,
    val description: String,
    val ctaText: String,
    val route: String? = null,
    val deepLinkAction: (() -> Unit)? = null
)

data class ProgressMetrics(
    val beginnerJourneyProgress: Float = 0f,
    val conceptsExplored: Int = 0,
    val totalConcepts: Int = 0,
    val scalesPracticed: Int = 0,
    val earTrainingSessions: Int = 0, // Mocked for now if not tracked
    val voiceTrainingSessions: Int = 0, // Mocked for now if not tracked
    val freestyleSessions: Int = 0 // Mocked for now if not tracked
)

@HiltViewModel
class MeViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val beginnerRepository: BeginnerProgressRepository,
    private val progressRepository: ProgressRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MeUiState())
    val uiState: StateFlow<MeUiState> = _uiState.asStateFlow()

    init {
        observeProfile()
        observeGoalsAndActivities()
    }

    private fun observeProfile() {
        profileRepository.getDisplayName().onEach { name ->
            _uiState.update { it.copy(displayName = name) }
        }.launchIn(viewModelScope)

        profileRepository.getProfileImage().onEach { path ->
            _uiState.update { it.copy(profileImagePath = path) }
        }.launchIn(viewModelScope)

        profileRepository.getStreakInfo().onEach { info ->
            _uiState.update { it.copy(streakInfo = info) }
        }.launchIn(viewModelScope)
    }

    private fun observeGoalsAndActivities() {
        combine(
            beginnerRepository.getCompletedLessonsWithTimestamps(),
            progressRepository.getAllProgress()
        ) { beginnerProgress, allProgress ->
            val todayStart = getStartOfToday()
            
            // 1. Today's Activities
            val activities = mutableListOf<String>()
            beginnerProgress.filter { it.completedTimestamp != null && it.completedTimestamp >= todayStart }
                .forEach { activities.add("✓ Beginner Lesson ${it.lessonId}") }
            
            allProgress.filter { it.completed && it.completedAt >= todayStart }
                .forEach { activities.add("✓ ${it.rootNote.displayName} ${it.conceptType.displayName}") }
            
            // 2. Progress Metrics
            val totalConcepts = Note.entries.size * ConceptType.entries.size
            val completedConcepts = allProgress.count { it.completed }
            val scalesPracticed = allProgress.count { it.completed && it.conceptType.category == ConceptCategory.SCALE }
            
            val metrics = ProgressMetrics(
                beginnerJourneyProgress = beginnerProgress.size.toFloat() / 5f,
                conceptsExplored = completedConcepts,
                totalConcepts = totalConcepts,
                scalesPracticed = scalesPracticed
            )

            // 3. Today's Goal
            val goal = determineGoal(beginnerProgress.map { it.lessonId }.toSet(), allProgress)

            _uiState.update { it.copy(
                todayActivities = activities,
                progressMetrics = metrics,
                todayGoal = goal
            ) }
        }.launchIn(viewModelScope)
    }

    private fun determineGoal(completedBeginner: Set<Int>, allProgress: List<LessonProgress>): TodayGoal {
        if (completedBeginner.size < 5) {
            val next = (1..5).first { it !in completedBeginner }
            return TodayGoal(
                title = "Complete Beginner Lesson $next",
                description = "Build your musical foundation step by step.",
                ctaText = "Continue Beginner Journey",
                route = "lesson/$next"
            )
        }

        // Check for next concept
        for (note in Note.entries) {
            for (concept in ConceptType.entries) {
                val isCompleted = allProgress.any { it.rootNote == note && it.conceptType == concept && it.completed }
                if (!isCompleted) {
                    return TodayGoal(
                        title = "Learn ${note.displayName} ${concept.displayName}",
                        description = "Expand your knowledge of music theory and practice.",
                        ctaText = "Start Learning",
                        route = "practice/${note.name}/${concept.name}"
                    )
                }
            }
        }

        // All completed - rotate goals
        val goals = listOf(
            TodayGoal("Practice a Scale", "Spend 5 minutes practicing a scale of your choice.", "Go to Freestyle", "freestyle_root"),
            TodayGoal("Ear Training", "Sharpen your ears with a quick training session.", "Train Now", "audio_intelligence_root"),
            TodayGoal("Voice Training", "Improve your pitch by singing along with the notes.", "Sing Now", "audio_intelligence_root"),
            TodayGoal("Review Basics", "Revisit a beginner lesson to stay sharp.", "Relearn Basics", "journey_root")
        )
        return goals[(System.currentTimeMillis() / (1000 * 60 * 60 * 24) % goals.size).toInt()]
    }

    private fun getStartOfToday(): Long {
        val now = System.currentTimeMillis()
        return now - (now % (1000 * 60 * 60 * 24))
    }

    fun updateName(name: String) {
        viewModelScope.launch {
            profileRepository.updateDisplayName(if (name.isBlank()) "Learner" else name)
        }
    }

    fun updateProfileImage(path: String) {
        viewModelScope.launch {
            profileRepository.updateProfileImage(path)
        }
    }
}
