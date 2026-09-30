package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.PlaybackState
import com.example.data.model.Surah

@Composable
fun AudioControlBar(
    playbackState: PlaybackState,
    currentSurah: Surah,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onOpenRepeatSettings: () -> Unit,
    onStopClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentAyah = playbackState.currentAyah

    AnimatedVisibility(
        visible = currentAyah != null,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier
    ) {
        if (currentAyah != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .shadow(12.dp, shape = RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    // Repetition Status and Info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "سورة ${currentSurah.nameArabic} - الآية ${currentAyah.ayahNumber}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            val targetCountText = if (playbackState.targetRepeatCount == -1) "∞ غير محدود" else "${playbackState.targetRepeatCount} مرات"
                            val statusDesc = if (playbackState.isPauseBetweenRepeats) {
                                "⏳ استراحة الترديد: ${playbackState.pauseSecondsRemaining} ثانية (ردّد الآن)"
                            } else {
                                "🔁 التكرار: ${playbackState.currentRepeatIndex} من $targetCountText (السرعة: ${playbackState.playbackSpeed}x)"
                            }
                            Text(
                                text = statusDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Repeat settings icon button
                        IconButton(
                            onClick = onOpenRepeatSettings,
                            modifier = Modifier.testTag("open_repeat_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "إعدادات التكرار",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Close button
                        IconButton(onClick = onStopClick) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إيقاف",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }

                    // Progress bar for buffering or countdown
                    if (playbackState.isBuffering) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Media Controls
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onPreviousClick) {
                            Icon(
                                Icons.Default.FastForward, // RTL Forward is Next in text but previous in index
                                contentDescription = "الآية السابقة",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        FilledIconButton(
                            onClick = onPlayPauseClick,
                            modifier = Modifier.size(52.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(
                                imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (playbackState.isPlaying) "إيقاف مؤقت" else "تشغيل",
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        IconButton(onClick = onNextClick) {
                            Icon(
                                Icons.Default.FastRewind, // RTL
                                contentDescription = "الآية التالية",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}
