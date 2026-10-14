package com.example.dripguardai.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dripguardai.data.ai.DryingRecommendation
import com.example.dripguardai.ui.DripGuardViewModel
import com.example.dripguardai.ui.PlannerUiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PlannerScreen(
    viewModel: DripGuardViewModel,
    onNavigateBack: () -> Unit
) {
    val plannerState by viewModel.plannerState.collectAsState()
    val savedLocations by viewModel.savedLocations.collectAsState()
    val defaultSetup by viewModel.defaultDryingSetup.collectAsState()

    var cityName by remember { mutableStateOf("New York") }
    val availableFabrics = remember {
        listOf("Cotton", "Denim/Jeans", "Synthetics/Polyester", "Wool/Delicates", "Linens/Bedding", "Towels")
    }
    var selectedFabrics by remember { mutableStateOf(setOf("Cotton", "Denim/Jeans")) }

    val hangingOptions = remember {
        listOf("Outdoor Direct Sun", "Outdoor Covered/Shade", "Indoor Well-Ventilated", "Indoor Enclosed")
    }
    var selectedHangingOption by remember { mutableStateOf(defaultSetup) }
    var sessionTitle by remember { mutableStateOf("Wash on Tuesday") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Drying Planner", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column {
                OutlinedTextField(
                    value = cityName,
                    onValueChange = { cityName = it },
                    label = { Text("Location / City Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (savedLocations.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Saved Locations:", style = MaterialTheme.typography.labelSmall)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        savedLocations.forEach { loc ->
                            FilterChip(
                                selected = (cityName == loc.cityName),
                                onClick = { cityName = loc.cityName },
                                label = { Text(loc.name) }
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = sessionTitle,
                onValueChange = { sessionTitle = it },
                label = { Text("Wash Session Name / Log Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Select Garment Fabrics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        availableFabrics.forEach { fabric ->
                            val isSelected = selectedFabrics.contains(fabric)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedFabrics = if (isSelected) {
                                        if (selectedFabrics.size > 1) selectedFabrics - fabric else selectedFabrics
                                    } else {
                                        selectedFabrics + fabric
                                    }
                                },
                                label = { Text(fabric) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Hanging & Drying Setup",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    hangingOptions.forEach { option ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            RadioButton(
                                selected = (selectedHangingOption == option),
                                onClick = { selectedHangingOption = option }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = option, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            Button(
                onClick = {
                    viewModel.calculatePlannerDrying(
                        cityName = cityName,
                        selectedFabrics = selectedFabrics.toList(),
                        hangingLocation = selectedHangingOption
                    )
                },
                enabled = plannerState !is PlannerUiState.Loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Analyze & Generate AI Drying Plan", fontSize = 16.sp)
            }

            AnimatedVisibility(
                visible = plannerState is PlannerUiState.Loading,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Consulting Gemini 1.5 Flash AI for physics drying kinetics...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (plannerState is PlannerUiState.Error) {
                val err = (plannerState as PlannerUiState.Error).message
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = err, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }

            if (plannerState is PlannerUiState.Success) {
                val success = plannerState as PlannerUiState.Success
                val rec = success.recommendation

                PlannerResultCard(
                    rec = rec,
                    cityName = success.cityName,
                    onStartTimer = {
                        viewModel.startDryingTimer(
                            durationHours = rec.dryingDurationHours,
                            label = "$sessionTitle ($cityName)"
                        )
                    },
                    onSaveLog = {
                        viewModel.saveWashLog(
                            title = sessionTitle,
                            cityName = success.cityName,
                            fabrics = selectedFabrics.toList(),
                            hangingLocation = selectedHangingOption,
                            rec = rec,
                            status = "Success"
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun PlannerResultCard(
    rec: DryingRecommendation,
    cityName: String,
    onStartTimer: () -> Unit,
    onSaveLog: () -> Unit
) {
    var savedSuccessMessage by remember { mutableStateOf(false) }

    val (badgeBg, badgeFg) = when (rec.rainRisk.uppercase()) {
        "HIGH" -> Color(0xFFFFEBEE) to Color(0xFFC62828)
        "MODERATE" -> Color(0xFFFFF3E0) to Color(0xFFE65100)
        else -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Recommendation for $cityName",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeBg
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Rain Risk Level: ",
                            style = MaterialTheme.typography.labelMedium,
                            color = badgeFg.copy(alpha = 0.85f)
                        )
                        Text(
                            text = rec.rainRisk.uppercase(),
                            color = badgeFg,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Est. Drying Duration: ${"%.1f".format(rec.dryingDurationHours)} hours",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.WbSunny, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Optimal Window: ${rec.optimalDryingWindow}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Text(
                text = rec.summaryText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (rec.actionableTips.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Actionable Tips:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    rec.actionableTips.forEach { tip ->
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = Color(0xFFF57F17),
                                modifier = Modifier.size(16.dp).padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = tip, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onStartTimer,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Start Timer")
                }

                OutlinedButton(
                    onClick = {
                        onSaveLog()
                        savedSuccessMessage = true
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Bookmark, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (savedSuccessMessage) "Log Saved! ✓" else "Save Log")
                }
            }
        }
    }
}
