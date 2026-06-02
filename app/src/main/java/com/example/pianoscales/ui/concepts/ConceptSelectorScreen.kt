package com.example.pianoscales.ui.concepts

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.pianoscales.theory.ConceptCategory
import com.example.pianoscales.theory.ConceptType
import com.example.pianoscales.theory.Note
import com.example.pianoscales.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConceptSelectorScreen(
    rootNote: Note,
    onConceptSelected: (ConceptType) -> Unit,
    onBack: () -> Unit,
    viewModel: ConceptSelectorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(rootNote) {
        viewModel.loadProgress(rootNote)
    }

    Scaffold(
        containerColor = PrimaryBackground,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Lessons in ${rootNote.displayName}",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryBackground,
                    titleContentColor = TextPrimary,
                    navigationIconContentColor = TextPrimary
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Hero Section
            item {
                HeroSection()
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Category Groups
            val categories = listOf(
                ConceptCategory.SCALE,
                ConceptCategory.CHORD,
                ConceptCategory.ARPEGGIO
            )

            categories.forEach { category ->
                val categoryConcepts = ConceptType.entries.filter { it.category == category }
                if (categoryConcepts.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        CategoryHeader(category = category)
                    }
                    items(categoryConcepts) { concept ->
                        val isCompleted = uiState.completedConcepts.contains(concept)
                        PremiumLessonCard(
                            concept = concept,
                            isCompleted = isCompleted,
                            onClick = { onConceptSelected(concept) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroSection() {
    Column {
        Text(
            text = "Scales, Chords & Arpeggios",
            style = MaterialTheme.typography.headlineLarge,
            color = PrimaryAccent
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Master the building blocks of music theory through guided practice.",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary
        )
    }
}

@Composable
private fun CategoryHeader(category: ConceptCategory) {
    val title: String
    val icon: ImageVector
    val tint: Color

    when (category) {
        ConceptCategory.SCALE -> {
            title = "Scales"
            icon = Icons.Default.Menu
            tint = ScaleIndicator
        }
        ConceptCategory.CHORD -> {
            title = "Chords"
            icon = Icons.Default.PlayArrow
            tint = ChordIndicator
        }
        ConceptCategory.ARPEGGIO -> {
            title = "Arpeggios"
            icon = Icons.Default.Star
            tint = ArpeggioIndicator
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(tint.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary
        )
    }
}

@Composable
private fun PremiumLessonCard(
    concept: ConceptType,
    isCompleted: Boolean,
    onClick: () -> Unit
) {
    val indicatorColor = when (concept.category) {
        ConceptCategory.SCALE -> ScaleIndicator
        ConceptCategory.CHORD -> ChordIndicator
        ConceptCategory.ARPEGGIO -> ArpeggioIndicator
    }

    val description = when (concept) {
        ConceptType.MAJOR_SCALE -> "The foundation of western music theory"
        ConceptType.NATURAL_MINOR_SCALE -> "Exploring emotional and melancholic tones"
        ConceptType.MAJOR_CHORD -> "Bright and resonant triad structures"
        ConceptType.MINOR_CHORD -> "Dark and expressive triads"
        ConceptType.MAJOR_ARPEGGIO -> "Broken chord played sequentially"
        ConceptType.MINOR_ARPEGGIO -> "Minor triad tones played individually"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Indicator
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(indicatorColor)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Center Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = concept.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 1
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    maxLines = 1
                )
            }
            
            // Right Badge
            AnimatedVisibility(
                visible = isCompleted,
                enter = fadeIn() + scaleIn(initialScale = 0.8f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)),
                exit = fadeOut() + scaleOut()
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(SuccessAccent.copy(alpha = 0.2f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = SuccessAccent,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "COMPLETED",
                            style = MaterialTheme.typography.labelSmall,
                            color = SuccessAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
