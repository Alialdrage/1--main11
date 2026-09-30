package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.audio.PlaybackState
import com.example.data.local.BookmarkEntity
import com.example.data.model.Ayah
import com.example.data.model.QuranDataProvider
import com.example.data.model.Surah
import com.example.ui.QuranUiState
import com.example.ui.components.AyahItemCard
import com.example.ui.components.RepeatSettingsDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnRecitationScreen(
    uiState: QuranUiState,
    playbackState: PlaybackState,
    bookmarks: List<BookmarkEntity>,
    onSelectSurah: (Surah) -> Unit,
    onPlayAyah: (Ayah) -> Unit,
    onPauseAudio: () -> Unit,
    onCorrectionClick: (Ayah) -> Unit,
    onBookmarkClick: (Ayah) -> Unit,
    onToggleTajweedColors: () -> Unit,
    onToggleTranslation: () -> Unit,
    onFontSizeChange: (Int) -> Unit,
    onRepeatCountChange: (Int) -> Unit,
    onRepeatDelayChange: (Int) -> Unit,
    onPlaybackSpeedChange: (Float) -> Unit,
    onReciterChange: (com.example.data.model.Reciter) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showFontSizeDialog by remember { mutableStateOf(false) }

    val surahs = QuranDataProvider.surahs
    val selectedSurah = uiState.selectedSurah

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "رتّل - المصحف المعلم والتكرار",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "القارئ: ${uiState.selectedReciter.nameArabic}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // Repeat settings
                    IconButton(
                        onClick = { showSettingsSheet = true },
                        modifier = Modifier.testTag("topbar_repeat_settings")
                    ) {
                        Icon(
                            Icons.Default.Repeat,
                            contentDescription = "خيارات التكرار",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Font size dialog
                    IconButton(onClick = { showFontSizeDialog = true }) {
                        Icon(
                            Icons.Default.FormatSize,
                            contentDescription = "حجم الخط",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // Hero Banner with Islamic Art
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                        Image(
                            painter = painterResource(id = R.drawable.quran_header_art_1790279038574),
                            contentDescription = "لوحة زخرفية قرآنية",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    androidx.compose.ui.graphics.Brush.verticalGradient(
                                        colors = listOf(
                                            androidx.compose.ui.graphics.Color.Transparent,
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "﴿ وَرَتِّلِ الْقُرْآنَ تَرْتِيلًا ﴾",
                                color = androidx.compose.ui.graphics.Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "خاصية التكرار الصوتي وتصحيح التلاوة للمبتدئين",
                                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // Quick Surah Selector Chips (Juz Amma Short Surahs)
            item {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(
                        text = "اختر السورة للتعلم والتكرار:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(surahs) { surah ->
                            val isSelected = surah.number == selectedSurah.number
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSelectSurah(surah) },
                                label = { Text("سورة ${surah.nameArabic}") },
                                leadingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            Icons.Default.MenuBook,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null,
                                modifier = Modifier.testTag("surah_chip_${surah.number}")
                            )
                        }
                    }
                }
            }

            // Current Surah Info Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "سورة ${selectedSurah.nameArabic}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${selectedSurah.versesCount} آيات",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "•",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = selectedSurah.revelationType,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = selectedSurah.description,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Quick Tool Bar: Tajweed highlight toggle, Translation toggle, Repeat options button
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = uiState.showTajweedColors,
                            onClick = onToggleTajweedColors,
                            label = { Text("ألوان التجويد", fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.ColorLens, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        )
                        FilterChip(
                            selected = uiState.showTranslation,
                            onClick = onToggleTranslation,
                            label = { Text("معاني الكلمات", fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        )
                    }

                    FilledTonalButton(
                        onClick = { showSettingsSheet = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        val repText = if (playbackState.targetRepeatCount == -1) "∞" else "${playbackState.targetRepeatCount}x"
                        Text("التكرار ($repText)", fontSize = 12.sp)
                    }
                }
            }

            // Basmalah for Surahs other than At-Tawbah and Al-Fatihah (which has it as Ayah 1)
            if (selectedSurah.number != 1 && selectedSurah.number != 9) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = (uiState.fontSizeSp).sp
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Verses List
            items(uiState.ayahs, key = { "${it.surahNumber}_${it.ayahNumber}" }) { ayah ->
                val isBookmarked = bookmarks.any { it.surahNumber == ayah.surahNumber && it.ayahNumber == ayah.ayahNumber }
                AyahItemCard(
                    ayah = ayah,
                    fontSizeSp = uiState.fontSizeSp,
                    showTajweed = uiState.showTajweedColors,
                    showTranslation = uiState.showTranslation,
                    playbackState = playbackState,
                    isBookmarked = isBookmarked,
                    onPlayClick = { onPlayAyah(ayah) },
                    onPauseClick = onPauseAudio,
                    onCorrectionClick = { onCorrectionClick(ayah) },
                    onBookmarkClick = { onBookmarkClick(ayah) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }
    }

    // Modal Sheet for Repeat Settings
    if (showSettingsSheet) {
        RepeatSettingsDialog(
            targetRepeatCount = playbackState.targetRepeatCount,
            repeatDelaySeconds = playbackState.repeatDelaySeconds,
            playbackSpeed = playbackState.playbackSpeed,
            selectedReciter = uiState.selectedReciter,
            onRepeatCountChange = onRepeatCountChange,
            onRepeatDelayChange = onRepeatDelayChange,
            onPlaybackSpeedChange = onPlaybackSpeedChange,
            onReciterChange = onReciterChange,
            onDismiss = { showSettingsSheet = false }
        )
    }

    // Font Size Dialog
    if (showFontSizeDialog) {
        AlertDialog(
            onDismissRequest = { showFontSizeDialog = false },
            title = { Text("تعديل حجم خط الآيات") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                        fontSize = uiState.fontSizeSp.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { onFontSizeChange(uiState.fontSizeSp - 2) },
                            enabled = uiState.fontSizeSp > 18
                        ) {
                            Text("A- أصغر")
                        }
                        Text("${uiState.fontSizeSp} sp", fontWeight = FontWeight.Bold)
                        Button(
                            onClick = { onFontSizeChange(uiState.fontSizeSp + 2) },
                            enabled = uiState.fontSizeSp < 36
                        ) {
                            Text("A+ أكبر")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFontSizeDialog = false }) {
                    Text("تم")
                }
            }
        )
    }
}
