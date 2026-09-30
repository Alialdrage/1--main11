package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.PlaybackState
import com.example.data.model.Ayah
import com.example.data.model.RuleType
import com.example.ui.theme.*

@Composable
fun AyahItemCard(
    ayah: Ayah,
    fontSizeSp: Int,
    showTajweed: Boolean,
    showTranslation: Boolean,
    playbackState: PlaybackState,
    isBookmarked: Boolean,
    onPlayClick: () -> Unit,
    onPauseClick: () -> Unit,
    onCorrectionClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCurrentAyah = playbackState.currentAyah?.ayahNumber == ayah.ayahNumber &&
            playbackState.currentAyah?.surahNumber == ayah.surahNumber
    val isPlaying = isCurrentAyah && playbackState.isPlaying

    val cardBorderColor by animateColorAsState(
        targetValue = if (isCurrentAyah) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
        label = "cardBorderColor"
    )

    val cardContainerColor by animateColorAsState(
        targetValue = if (isCurrentAyah) {
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        label = "cardContainerColor"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ayah_card_${ayah.ayahNumber}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor),
        border = BorderStroke(if (isCurrentAyah) 2.dp else 1.dp, cardBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrentAyah) 4.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Ayah Number & Repetition Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Ayah badge
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (isCurrentAyah) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${ayah.ayahNumber}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isCurrentAyah) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.primary
                    )
                }

                // Active Repetition pill indicator
                if (isCurrentAyah) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Repeat,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            val targetCountText = if (playbackState.targetRepeatCount == -1) "∞" else "${playbackState.targetRepeatCount}"
                            if (playbackState.isPauseBetweenRepeats) {
                                Text(
                                    text = "ردّد الآن... (${playbackState.pauseSecondsRemaining}ث)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Text(
                                    text = "تكرار ${playbackState.currentRepeatIndex} / $targetCountText",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Bookmark Icon
                IconButton(onClick = onBookmarkClick) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "حفظ الآية",
                        tint = if (isBookmarked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Ayah Arabic Text
            Text(
                text = ayah.textWithTashkeel,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = fontSizeSp.sp,
                    lineHeight = (fontSizeSp * 1.75).sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Right,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            )

            // Meaning / Translation if enabled
            if (showTranslation && ayah.translationArabicBrief.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "المعنى: ${ayah.translationArabicBrief}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(10.dp),
                        textAlign = TextAlign.Right
                    )
                }
            }

            // Tajweed Annotations Chips
            if (showTajweed && ayah.tajweedNotes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "التجويد:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    ayah.tajweedNotes.forEach { note ->
                        val (chipBg, chipText) = when (note.ruleType) {
                            RuleType.MADD -> TajweedMadd.copy(alpha = 0.15f) to TajweedMadd
                            RuleType.GHUNNAH -> TajweedGhunnah.copy(alpha = 0.15f) to TajweedGhunnah
                            RuleType.QALQALAH -> TajweedQalqalah.copy(alpha = 0.15f) to TajweedQalqalah
                            RuleType.IDGHAM -> TajweedIdgham.copy(alpha = 0.15f) to TajweedIdgham
                            RuleType.IQLAB -> TajweedIqlab.copy(alpha = 0.15f) to TajweedIqlab
                            RuleType.MAKHRAJ -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) to MaterialTheme.colorScheme.primary
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = chipBg
                        ) {
                            Text(
                                text = "${note.ruleName} (${note.part})",
                                color = chipText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Listen & Repeat button
                FilledTonalButton(
                    onClick = {
                        if (isPlaying) onPauseClick() else onPlayClick()
                    },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                        contentColor = if (isPlaying) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.testTag("play_button_${ayah.ayahNumber}")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "إيقاف مؤقت" else "استماع وتكرار",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isPlaying) "إيقاف التكرار" else "استمع وكرر",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Correct Recitation Button
                Button(
                    onClick = onCorrectionClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ),
                    modifier = Modifier.testTag("correct_recitation_button_${ayah.ayahNumber}")
                ) {
                    Icon(
                        imageVector = Icons.Default.RecordVoiceOver,
                        contentDescription = "صحّح تلاوتك",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "صحّح تلاوتك",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
