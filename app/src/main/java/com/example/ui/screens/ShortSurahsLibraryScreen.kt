package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.data.model.DifficultyLevel
import com.example.data.model.QuranDataProvider
import com.example.data.model.Surah
import com.example.ui.QuranUiState
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortSurahsLibraryScreen(
    uiState: QuranUiState,
    playbackState: PlaybackState,
    onSelectSurah: (Surah) -> Unit,
    onPlayAyah: (Ayah) -> Unit,
    onPauseAudio: () -> Unit,
    onRepeatAyahSeparately: (Ayah, Int, Int) -> Unit,
    onCorrectionClick: (Ayah) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedLevelFilter by remember { mutableStateOf<DifficultyLevel?>(null) }
    var selectedAyahForTafseerDialog by remember { mutableStateOf<Ayah?>(null) }
    var showSeparateRepeatDialogForAyah by remember { mutableStateOf<Ayah?>(null) }

    val filteredSurahs = remember(searchQuery, selectedLevelFilter) {
        QuranDataProvider.surahs.filter { surah ->
            val matchesLevel = selectedLevelFilter == null || surah.difficultyLevel == selectedLevelFilter
            val matchesSearch = searchQuery.isBlank() ||
                    surah.nameArabic.contains(searchQuery) ||
                    surah.nameEnglish.contains(searchQuery, ignoreCase = true) ||
                    surah.description.contains(searchQuery)
            matchesLevel && matchesSearch
        }
    }

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
                                    Icons.Default.LibraryBooks,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "مكتبة السور القصيرة وتفسيرها",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
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
            // Header Description
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "تلاوة صوتية مجزأة لكل آية مع التفسير الميسر وإمكانية تكرار أي آية منفردة للحفظ والترديد.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("ابحث عن سورة (مثال: الفاتحة، الإخلاص، الفلق)...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "بحث") },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "مسح")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            // Difficulty Level Filters
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedLevelFilter == null,
                            onClick = { selectedLevelFilter = null },
                            label = { Text("جميع السور (${filteredSurahs.size})") }
                        )
                    }
                    items(DifficultyLevel.values()) { level ->
                        val isSelected = selectedLevelFilter == level
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedLevelFilter = if (isSelected) null else level },
                            label = { Text(level.titleArabic) }
                        )
                    }
                }
            }

            // Horizontal Surahs Shelf
            item {
                Text(
                    text = "اختر السورة لعرض نصها الكامل وتفسيرها:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 6.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    items(filteredSurahs) { surah ->
                        val isSelected = surah.number == selectedSurah.number
                        Card(
                            modifier = Modifier
                                .width(135.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onSelectSurah(surah) }
                                .testTag("library_surah_card_${surah.number}"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(
                                if (isSelected) 2.dp else 1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "سورة ${surah.nameArabic}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${surah.versesCount} آيات",
                                    fontSize = 11.sp,
                                    color = if (isSelected) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSelected) Color.White.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = surah.difficultyLevel.titleArabic.replace("مستوى ", ""),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Current Surah Header & Description
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "النص القرآني الكامل لسورة ${selectedSurah.nameArabic}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${selectedSurah.revelationType} • ${selectedSurah.versesCount} آيات • ${selectedSurah.difficultyLevel.titleArabic}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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

            // Basmalah for Surahs
            if (selectedSurah.number != 1 && selectedSurah.number != 9) {
                item {
                    Text(
                        text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    )
                }
            }

            // Verses List with Partitioned Audio & Brief Tafseer
            items(uiState.ayahs, key = { "lib_${it.surahNumber}_${it.ayahNumber}" }) { ayah ->
                val isCurrentAyah = playbackState.currentAyah?.ayahNumber == ayah.ayahNumber &&
                        playbackState.currentAyah?.surahNumber == ayah.surahNumber
                val isPlaying = isCurrentAyah && playbackState.isPlaying

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("library_ayah_card_${ayah.ayahNumber}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCurrentAyah) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        if (isCurrentAyah) 2.dp else 1.dp,
                        if (isCurrentAyah) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Header with Ayah Number & Repeat Badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${ayah.ayahNumber}",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            if (isCurrentAyah) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primary
                                ) {
                                    Text(
                                        text = if (playbackState.isPauseBetweenRepeats) "ردّد الآن (${playbackState.pauseSecondsRemaining}ث)" else "تكرار الآية: ${playbackState.currentRepeatIndex}/${if (playbackState.targetRepeatCount == -1) "∞" else playbackState.targetRepeatCount}",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            // Quick Tafseer Icon
                            IconButton(onClick = { selectedAyahForTafseerDialog = ayah }) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = "عرض التفسير",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Ayah Text
                        Text(
                            text = ayah.textWithTashkeel,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontSize = 23.sp,
                                lineHeight = 40.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Brief Tafseer Box (تفسير مبسط ومختصر)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = GoldSoft,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = ayah.tafseerBrief,
                                    style = MaterialTheme.typography.bodySmall,
                                    lineHeight = 20.sp,
                                    color = GoldDark,
                                    textAlign = TextAlign.Right
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        Spacer(modifier = Modifier.height(8.dp))

                        // Action Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. Play / Pause partitioned recitation
                            FilledTonalButton(
                                onClick = {
                                    if (isPlaying) onPauseAudio() else onPlayAyah(ayah)
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("play_partition_${ayah.ayahNumber}")
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isPlaying) "إيقاف" else "تلاوة المقطع", fontSize = 12.sp)
                            }

                            // 2. Repeat this Ayah separately (إمكانية تكرار أي آية بشكل منفصل)
                            OutlinedButton(
                                onClick = { showSeparateRepeatDialogForAyah = ayah },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("repeat_individually_button_${ayah.ayahNumber}")
                            ) {
                                Icon(
                                    Icons.Default.Repeat,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تكرار منفصل", fontSize = 12.sp)
                            }

                            // 3. Test recitation
                            IconButton(onClick = { onCorrectionClick(ayah) }) {
                                Icon(
                                    Icons.Default.RecordVoiceOver,
                                    contentDescription = "تصحيح التلاوة",
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Dialog: Repeat single Ayah separately (تكرار الآية بشكل منفصل)
    if (showSeparateRepeatDialogForAyah != null) {
        val ayahToRepeat = showSeparateRepeatDialogForAyah!!
        var repCount by remember { mutableStateOf(3) }
        var delaySec by remember { mutableStateOf(2) }

        AlertDialog(
            onDismissRequest = { showSeparateRepeatDialogForAyah = null },
            title = {
                Text(
                    text = "تكرار الآية ${ayahToRepeat.ayahNumber} بشكل منفصل",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "« ${ayahToRepeat.textWithTashkeel} »",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    )

                    Text(
                        text = "اختر عدد مرات تكرار هذه الآية وحدها:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(1, 3, 5, 10, -1).forEach { count ->
                            val label = if (count == -1) "∞" else "$count"
                            FilterChip(
                                selected = repCount == count,
                                onClick = { repCount = count },
                                label = { Text(label) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "فاصل الترديد بين كل تكرار:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0, 1, 2, 3, 5).forEach { sec ->
                            FilterChip(
                                selected = delaySec == sec,
                                onClick = { delaySec = sec },
                                label = { Text("${sec}ث") }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRepeatAyahSeparately(ayahToRepeat, repCount, delaySec)
                        showSeparateRepeatDialogForAyah = null
                    }
                ) {
                    Icon(Icons.Default.Repeat, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ابدأ التكرار المنفصل")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSeparateRepeatDialogForAyah = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Modal Dialog: Full Tafseer details
    if (selectedAyahForTafseerDialog != null) {
        val ayah = selectedAyahForTafseerDialog!!
        AlertDialog(
            onDismissRequest = { selectedAyahForTafseerDialog = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("التفسير الميسر للآية ${ayah.ayahNumber}")
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "« ${ayah.textWithTashkeel} »",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = ayah.tafseerBrief,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedAyahForTafseerDialog = null }) {
                    Text("إغلاق")
                }
            }
        )
    }
}
