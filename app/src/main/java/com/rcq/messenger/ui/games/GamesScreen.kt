package com.rcq.messenger.ui.games

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rcq.messenger.data.repository.GameRepository
import com.rcq.messenger.domain.model.GameState
import com.rcq.messenger.domain.model.GameStatus
import com.rcq.messenger.domain.model.GameType
import com.rcq.messenger.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class GamesViewModel @Inject constructor(
    private val gameRepository: GameRepository
) : ViewModel() {

    private val _crashGameState = MutableStateFlow(CrashGameState())
    val crashGameState: StateFlow<CrashGameState> = _crashGameState.asStateFlow()

    private val _hiLoGameState = MutableStateFlow(HiLoGameState())
    val hiLoGameState: StateFlow<HiLoGameState> = _hiLoGameState.asStateFlow()

    private val _userBalance = MutableStateFlow(1000L)
    val userBalance: StateFlow<Long> = _userBalance.asStateFlow()

    init {
        startCrashGame()
    }

    private fun startCrashGame() {
        viewModelScope.launch {
            while (true) {
                _crashGameState.value = CrashGameState(status = GameStatus.WAITING)
                delay(3000)
                _crashGameState.value = _crashGameState.value.copy(status = GameStatus.RUNNING)

                var multiplier = 1.0
                while (multiplier < _crashGameState.value.crashPoint) {
                    multiplier += Random.nextDouble(0.01, 0.05)
                    _crashGameState.value = _crashGameState.value.copy(currentMultiplier = multiplier)
                    delay(100)
                }

                _crashGameState.value = _crashGameState.value.copy(
                    status = GameStatus.ENDED,
                    crashPoint = multiplier
                )
                delay(2000)
            }
        }
    }

    fun placeBet(amount: Long) {
        if (amount <= _userBalance.value && _crashGameState.value.status == GameStatus.WAITING) {
            _userBalance.value -= amount
            _crashGameState.value = _crashGameState.value.copy(
                currentBet = amount,
                hasBet = true
            )
        }
    }

    fun cashout() {
        val state = _crashGameState.value
        if (state.hasBet && !state.hasCashedOut && state.status == GameStatus.RUNNING) {
            val winnings = (state.currentBet * state.currentMultiplier).toLong()
            _userBalance.value += winnings
            _crashGameState.value = state.copy(
                hasCashedOut = true,
                cashedOutAmount = winnings
            )
        }
    }
}

data class CrashGameState(
    val status: GameStatus = GameStatus.WAITING,
    val currentMultiplier: Double = 1.0,
    val crashPoint: Double = Random.nextDouble(1.5, 5.0),
    val currentBet: Long = 0,
    val hasBet: Boolean = false,
    val hasCashedOut: Boolean = false,
    val cashedOutAmount: Long = 0
)

data class HiLoGameState(
    val currentCard: Int = 1,
    val nextCard: Int = 0,
    val hiSelected: Boolean? = null,
    val multiplier: Double = 2.0,
    val streak: Int = 0,
    val balance: Long = 1000
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamesScreen(
    viewModel: GamesViewModel = hiltViewModel(),
    onGameClick: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Games",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Background,
                    titleContentColor = TextPrimary
                )
            )
        },
        containerColor = Background
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                GameCard(
                    title = "Crash",
                    icon = Icons.Default.TrendingUp,
                    color = CrashRed,
                    description = "Cash out before it crashes",
                    onClick = { onGameClick("crash") }
                )
            }
            item {
                GameCard(
                    title = "Hi-Lo",
                    icon = Icons.Default.ExpandLess,
                    color = HiLoHigh,
                    description = "Predict the next card",
                    onClick = { onGameClick("hilo") }
                )
            }
            item {
                GameCard(
                    title = "Pet Hunt",
                    icon = Icons.Default.Pets,
                    color = Success,
                    description = "Collect rare pets",
                    onClick = { onGameClick("pet_hunt") }
                )
            }
            item {
                GameCard(
                    title = "Roulette",
                    icon = Icons.Default.Casino,
                    color = Secondary,
                    description = "Spin and win",
                    onClick = { onGameClick("roulette") }
                )
            }
        }
    }
}

@Composable
fun GameCard(
    title: String,
    icon: ImageVector,
    color: androidx.compose.ui.graphics.Color,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(28.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun CrashGameScreen(
    onBack: () -> Unit,
    viewModel: GamesViewModel = hiltViewModel()
) {
    val gameState by viewModel.crashGameState.collectAsState()
    val balance by viewModel.userBalance.collectAsState()
    var betAmount by remember { mutableStateOf("100") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back", tint = TextPrimary)
            }
            Text(
                text = "Crash",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
            Text(
                text = "$balance",
                style = MaterialTheme.typography.titleMedium,
                color = Primary
            )
        }

        Spacer(modifier = Modifier.weight(0.5f))

        // Multiplier display
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(
                    when {
                        gameState.status == GameStatus.ENDED -> CrashRed
                        gameState.hasCashedOut -> Success
                        else -> SurfaceVariant
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${String.format("%.2f", gameState.currentMultiplier)}x",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                if (gameState.status == GameStatus.ENDED) {
                    Text(
                        text = "CRASHED",
                        style = MaterialTheme.typography.titleMedium,
                        color = OnPrimary
                    )
                }
                if (gameState.hasCashedOut) {
                    Text(
                        text = "WON: ${gameState.cashedOutAmount}",
                        style = MaterialTheme.typography.titleMedium,
                        color = OnPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(0.5f))

        // Bet controls
        if (gameState.status == GameStatus.WAITING && !gameState.hasBet) {
            OutlinedTextField(
                value = betAmount,
                onValueChange = { betAmount = it },
                label = { Text("Bet Amount") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = SurfaceVariant
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    betAmount.toLongOrNull()?.let { viewModel.placeBet(it) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Place Bet")
            }
        }

        if (gameState.status == GameStatus.RUNNING && gameState.hasBet && !gameState.hasCashedOut) {
            Button(
                onClick = { viewModel.cashout() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Success),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Cash Out ${String.format("%.0f", gameState.currentBet * gameState.currentMultiplier)}",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Game status
        Text(
            text = when (gameState.status) {
                GameStatus.WAITING -> "Waiting for next round..."
                GameStatus.RUNNING -> "Game in progress"
                GameStatus.ENDED -> "Round ended"
                else -> ""
            },
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}