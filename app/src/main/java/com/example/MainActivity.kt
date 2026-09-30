package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.QuranDataProvider
import com.example.ui.QuranViewModel
import com.example.ui.components.AudioControlBar
import com.example.ui.components.RepeatSettingsDialog
import com.example.ui.screens.DifficultyLevelsScreen
import com.example.ui.screens.LearnRecitationScreen
import com.example.ui.screens.ProgressHistoryScreen
import com.example.ui.screens.RecitationCorrectionScreen
import com.example.ui.screens.RewardsAndCertificatesScreen
import com.example.ui.screens.ShortSurahsLibraryScreen
import com.example.ui.screens.TajweedGuideScreen
import com.example.ui.theme.MyApplicationTheme

enum class MainTab(val title: String, val icon: ImageVector) {
    LEARN("التلاوة", Icons.Default.MenuBook),
    LIBRARY("مكتبة السور", Icons.Default.LibraryBooks),
    CORRECTION("تصحيح التلاوة", Icons.Default.RecordVoiceOver),
    LEVELS("المستويات", Icons.Default.School),
    REWARDS("المكافآت والشهادات", Icons.Default.WorkspacePremium),
    HISTORY("سجلي", Icons.Default.History)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                QuranMainApp()
            }
        }
    }
}

@Composable
fun QuranMainApp(viewModel: QuranViewModel = viewModel()) {
    var selectedTab by remember { mutableStateOf(MainTab.LEARN) }
    var showRepeatSettingsSheet by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val speechState by viewModel.speechState.collectAsStateWithLifecycle()
    val attemptsHistory by viewModel.attemptsHistory.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val learnerStats by viewModel.learnerStats.collectAsStateWithLifecycle()
    val badges by viewModel.badges.collectAsStateWithLifecycle()
    val certificates by viewModel.certificates.collectAsStateWithLifecycle()
    val levelProgressList by viewModel.levelProgressList.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Floating Audio Control Bar above navigation
                AudioControlBar(
                    playbackState = playbackState,
                    currentSurah = uiState.selectedSurah,
                    onPlayPauseClick = {
                        if (playbackState.isPlaying) viewModel.pauseAudio() else viewModel.resumeAudio()
                    },
                    onPreviousClick = { viewModel.playPreviousAyah() },
                    onNextClick = { viewModel.advanceToNextAyah() },
                    onOpenRepeatSettings = { showRepeatSettingsSheet = true },
                    onStopClick = { viewModel.stopAudio() },
                    modifier = Modifier.fillMaxWidth()
                )

                // Main Navigation Bar with Tabs
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    MainTab.values().forEach { tab ->
                        val isSelected = selectedTab == tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { selectedTab = tab },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.title
                                )
                            },
                            label = {
                                Text(
                                    text = tab.title,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1
                                )
                            },
                            modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(targetState = selectedTab, label = "tab_fade") { currentTab ->
                when (currentTab) {
                    MainTab.LEARN -> {
                        LearnRecitationScreen(
                            uiState = uiState,
                            playbackState = playbackState,
                            bookmarks = bookmarks,
                            onSelectSurah = { viewModel.loadSurah(it) },
                            onPlayAyah = { viewModel.playAyah(it) },
                            onPauseAudio = { viewModel.pauseAudio() },
                            onCorrectionClick = { ayah ->
                                viewModel.selectAyahForCorrection(ayah)
                                selectedTab = MainTab.CORRECTION
                            },
                            onBookmarkClick = { viewModel.toggleBookmark(it) },
                            onToggleTajweedColors = { viewModel.toggleTajweedColors() },
                            onToggleTranslation = { viewModel.toggleTranslation() },
                            onFontSizeChange = { viewModel.setFontSize(it) },
                            onRepeatCountChange = { viewModel.setRepeatCount(it) },
                            onRepeatDelayChange = { viewModel.setRepeatDelaySeconds(it) },
                            onPlaybackSpeedChange = { viewModel.setPlaybackSpeed(it) },
                            onReciterChange = { viewModel.selectReciter(it) }
                        )
                    }

                    MainTab.LIBRARY -> {
                        ShortSurahsLibraryScreen(
                            uiState = uiState,
                            playbackState = playbackState,
                            onSelectSurah = { viewModel.loadSurah(it) },
                            onPlayAyah = { viewModel.playAyah(it) },
                            onPauseAudio = { viewModel.pauseAudio() },
                            onRepeatAyahSeparately = { ayah, count, delay ->
                                viewModel.repeatAyahIndividually(ayah, count, delay)
                            },
                            onCorrectionClick = { ayah ->
                                viewModel.selectAyahForCorrection(ayah)
                                selectedTab = MainTab.CORRECTION
                            }
                        )
                    }

                    MainTab.CORRECTION -> {
                        RecitationCorrectionScreen(
                            uiState = uiState,
                            speechState = speechState,
                            onSelectSurah = { viewModel.loadSurah(it) },
                            onSelectAyah = { viewModel.selectAyahForCorrection(it) },
                            onPlayReferenceAudio = { viewModel.playAyah(it) },
                            onStartRecording = { viewModel.startRecording() },
                            onStopRecording = { viewModel.stopRecording() },
                            onCancelRecording = { viewModel.cancelRecording() },
                            onSimulateRecitation = { sampleText ->
                                viewModel.simulateRecitation(sampleText)
                            }
                        )
                    }

                    MainTab.LEVELS -> {
                        DifficultyLevelsScreen(
                            levelProgressList = levelProgressList,
                            learnerStats = learnerStats,
                            onSelectSurahForPractice = { surah ->
                                viewModel.loadSurah(surah)
                                selectedTab = MainTab.LIBRARY
                            },
                            onSelectLessonForPractice = { lesson ->
                                viewModel.selectTajweedLesson(lesson)
                                selectedTab = MainTab.LEARN
                            },
                            onGenerateCertificateForLevel = { level ->
                                viewModel.generateCertificate(learnerStats?.userName ?: "طالب القرآن", level)
                                selectedTab = MainTab.REWARDS
                            }
                        )
                    }

                    MainTab.REWARDS -> {
                        RewardsAndCertificatesScreen(
                            learnerStats = learnerStats,
                            badges = badges,
                            certificates = certificates,
                            activeCertificate = uiState.activeCertificate,
                            onViewCertificate = { cert ->
                                // Trigger active certificate view
                                viewModel.generateCertificate(cert.recipientName, com.example.data.model.DifficultyLevel.BEGINNER)
                            },
                            onDismissCertificate = { viewModel.dismissCertificate() },
                            onGenerateCertificateForLevel = { level, name ->
                                viewModel.updateUserName(name)
                                viewModel.generateCertificate(name, level)
                            }
                        )
                    }

                    MainTab.HISTORY -> {
                        ProgressHistoryScreen(
                            learnerStats = learnerStats,
                            attemptsHistory = attemptsHistory,
                            bookmarks = bookmarks,
                            onSelectBookmark = { surahNum, ayahNum ->
                                val targetSurah = QuranDataProvider.surahs.find { it.number == surahNum }
                                if (targetSurah != null) {
                                    viewModel.loadSurah(targetSurah)
                                    selectedTab = MainTab.LEARN
                                }
                            },
                            onDeleteAttempt = { viewModel.deleteAttempt(it) }
                        )
                    }
                }
            }
        }
    }

    // Modal sheet for Repeat Settings
    if (showRepeatSettingsSheet) {
        RepeatSettingsDialog(
            targetRepeatCount = playbackState.targetRepeatCount,
            repeatDelaySeconds = playbackState.repeatDelaySeconds,
            playbackSpeed = playbackState.playbackSpeed,
            selectedReciter = uiState.selectedReciter,
            onRepeatCountChange = { viewModel.setRepeatCount(it) },
            onRepeatDelayChange = { viewModel.setRepeatDelaySeconds(it) },
            onPlaybackSpeedChange = { viewModel.setPlaybackSpeed(it) },
            onReciterChange = { viewModel.selectReciter(it) },
            onDismiss = { showRepeatSettingsSheet = false }
        )
    }
}
