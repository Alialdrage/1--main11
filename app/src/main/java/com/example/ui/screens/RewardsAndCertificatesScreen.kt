package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.CertificateEntity
import com.example.data.local.LearnerStatEntity
import com.example.data.model.Badge
import com.example.data.model.DifficultyLevel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewardsAndCertificatesScreen(
    learnerStats: LearnerStatEntity?,
    badges: List<Badge>,
    certificates: List<CertificateEntity>,
    activeCertificate: CertificateEntity?,
    onViewCertificate: (CertificateEntity) -> Unit,
    onDismissCertificate: () -> Unit,
    onGenerateCertificateForLevel: (DifficultyLevel, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalPoints = learnerStats?.totalPoints ?: 0
    var showCertDialogForLevel by remember { mutableStateOf<DifficultyLevel?>(null) }
    var userNameInput by remember { mutableStateOf(learnerStats?.userName ?: "طالب القرآن الكريم") }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = GoldMedium.copy(alpha = 0.18f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    tint = GoldDark,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "المكافآت والشارات والشهادات",
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
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Points Hero Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "رصيدك من نقاط الإتقان",
                            color = Color.White.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "$totalPoints",
                                fontSize = 42.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = GoldLight
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "نقطة",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(12.dp))

                        // How to earn points info
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            PointsBenefitItem("🎙️ تلاوة صحيحة", "+100 ن")
                            PointsBenefitItem("🎓 إكمال درس", "+50 ن")
                            PointsBenefitItem("🔁 جلسة تكرار", "+15 ن")
                        }
                    }
                }
            }

            // Digital Badges Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.MilitaryTech,
                            contentDescription = null,
                            tint = GoldDark,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "الشارات الرقمية للأداء القرآني",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    val unlockedCount = badges.count { it.isUnlocked }
                    Text(
                        text = "$unlockedCount من ${badges.size} مكتملة",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Badges Grid
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    badges.chunked(2).forEach { rowBadges ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowBadges.forEach { badge ->
                                BadgeCardItem(
                                    badge = badge,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowBadges.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // Certificates Section Header
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            tint = GoldDark,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "الشهادات التقديرية الرسمية",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Button to request a certificate
                    FilledTonalButton(
                        onClick = { showCertDialogForLevel = DifficultyLevel.BEGINNER },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("request_new_certificate_button")
                    ) {
                        Text("إصدار شهادة جديدة", fontSize = 12.sp)
                    }
                }
            }

            // Certificates List
            if (certificates.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "📜",
                                fontSize = 40.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "لم تقم بإصدار أي شهادة تقديرية بعد",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "أكمل دروس وسور أي مستوى، ثم اضغط على 'إصدار شهادة جديدة' لتحصل على شهادتك المعتمدة باسمك!",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(certificates) { cert ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onViewCertificate(cert) }
                            .testTag("certificate_item_${cert.id}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.5.dp, GoldLight),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(GoldSoft),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.WorkspacePremium,
                                        contentDescription = null,
                                        tint = GoldDark,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = cert.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "الممنوحة إلى: ${cert.recipientName} • بتاريخ ${cert.dateIssued}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Button(
                                onClick = { onViewCertificate(cert) },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("عرض الشهادة", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Dialog: Request/Issue Certificate
    if (showCertDialogForLevel != null) {
        val selectedLevel = showCertDialogForLevel!!
        AlertDialog(
            onDismissRequest = { showCertDialogForLevel = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = GoldDark)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إصدار شهادة تقدير جديدة")
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "اختر المستوى الذي أكملت متطلباته:",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DifficultyLevel.values().forEach { level ->
                            FilterChip(
                                selected = selectedLevel == level,
                                onClick = { showCertDialogForLevel = level },
                                label = { Text(level.titleArabic.replace("مستوى ", "")) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = userNameInput,
                        onValueChange = { userNameInput = it },
                        label = { Text("اسم الطالب كما تحب أن يظهر") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onGenerateCertificateForLevel(selectedLevel, userNameInput)
                        showCertDialogForLevel = null
                    }
                ) {
                    Text("إصدار الشهادة الآن")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCertDialogForLevel = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Interactive Golden Certificate Modal
    if (activeCertificate != null) {
        Dialog(
            onDismissRequest = onDismissCertificate,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                // The Certificate Sheet
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(24.dp, shape = RoundedCornerShape(24.dp))
                        .testTag("golden_certificate_display_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = CreamBackground),
                    border = BorderStroke(4.dp, GoldLight)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(onClick = onDismissCertificate) {
                                Icon(Icons.Default.Close, contentDescription = "إغلاق")
                            }
                        }

                        // Certificate Inner Border
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(BorderStroke(1.5.dp, GoldDark.copy(alpha = 0.6f)), RoundedCornerShape(12.dp))
                                .padding(18.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldDark
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                Icon(
                                    Icons.Default.WorkspacePremium,
                                    contentDescription = null,
                                    tint = GoldDark,
                                    modifier = Modifier.size(54.dp)
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "شَهَادَةُ تَقْدِيرٍ وَإِتْقَانِ تِلَاوَةٍ",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = EmeraldDark
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "تشهد إدارة تطبيق «رتّل» لتعليم تلاوة القرآن الكريم بأن القارئ / القارئة الموفق:",
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    color = TextPrimaryLight
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = GoldSoft,
                                    border = BorderStroke(1.dp, GoldMedium)
                                ) {
                                    Text(
                                        text = activeCertificate.recipientName,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldDark,
                                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "قد أتم بنجاح واقتدار متطلبات ${activeCertificate.levelTitle}\nمع إتقان مخارج الحروف وأحكام التجويد والتكرار الصوتي.",
                                    fontSize = 14.sp,
                                    lineHeight = 22.sp,
                                    textAlign = TextAlign.Center,
                                    color = TextPrimaryLight
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(text = "درجة التميز", fontSize = 11.sp, color = TextSecondaryLight)
                                        Text(
                                            text = "${activeCertificate.score}% (ممتاز)",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldMedium
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Default.Verified,
                                            contentDescription = null,
                                            tint = GoldDark,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Text(
                                            text = activeCertificate.verificationCode,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GoldDark
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(text = "تاريخ الإصدار", fontSize = 11.sp, color = TextSecondaryLight)
                                        Text(
                                            text = activeCertificate.dateIssued,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimaryLight
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = onDismissCertificate,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("تم ومبارك الإنجاز 🎉")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PointsBenefitItem(title: String, points: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = title, fontSize = 12.sp, color = Color.White.copy(alpha = 0.9f))
        Text(text = points, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GoldLight)
    }
}

@Composable
private fun BadgeCardItem(badge: Badge, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .testTag("badge_card_${badge.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (badge.isUnlocked) GoldSoft else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = BorderStroke(
            if (badge.isUnlocked) 1.5.dp else 1.dp,
            if (badge.isUnlocked) GoldMedium else Color.Gray.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(if (badge.isUnlocked) Color.White else Color.Gray.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                if (badge.isUnlocked) {
                    Text(text = badge.iconEmoji, fontSize = 24.sp)
                } else {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "مقفل",
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = badge.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (badge.isUnlocked) GoldDark else Color.Gray,
                textAlign = TextAlign.Center
            )

            Text(
                text = badge.description,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                color = if (badge.isUnlocked) TextPrimaryLight else Color.Gray,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (badge.isUnlocked) EmeraldSoft else Color.Gray.copy(alpha = 0.15f)
            ) {
                Text(
                    text = if (badge.isUnlocked) "✓ مُكتسبة" else "${badge.pointsRequired} نقطة",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (badge.isUnlocked) EmeraldMedium else Color.Gray,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}
