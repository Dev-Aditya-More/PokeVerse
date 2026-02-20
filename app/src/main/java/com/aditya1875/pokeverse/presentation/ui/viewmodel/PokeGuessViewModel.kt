package com.aditya1875.pokeverse.presentation.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aditya1875.pokeverse.domain.repository.PokemonRepo
import com.aditya1875.pokeverse.presentation.screens.game.pokeguess.components.GuessDifficulty
import com.aditya1875.pokeverse.presentation.screens.game.pokeguess.components.GuessGameState
import com.aditya1875.pokeverse.presentation.screens.game.pokeguess.components.PokeGuessQuestion
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PokeGuessViewModel(
    private val repository: PokemonRepo
) : ViewModel() {

    private val _gameState = MutableStateFlow<GuessGameState>(GuessGameState.Idle)
    val gameState: StateFlow<GuessGameState> = _gameState.asStateFlow()

    private var timerJob: Job? = null
    private var currentScore = 0
    private var correctAnswers = 0
    private val allQuestions = mutableListOf<PokeGuessQuestion>()

    fun startGame(difficulty: GuessDifficulty) {
        viewModelScope.launch {
            _gameState.value = GuessGameState.Loading

            try {
                // Generate questions based on difficulty
                val questions = generateQuestions(difficulty)
                allQuestions.clear()
                allQuestions.addAll(questions)

                currentScore = 0
                correctAnswers = 0

                // Show first question
                showQuestion(0, difficulty)
            } catch (e: Exception) {
                Log.e("PokeGuess", "Failed to generate questions", e)
                _gameState.value = GuessGameState.Idle
            }
        }
    }

    private suspend fun generateQuestions(difficulty: GuessDifficulty): List<PokeGuessQuestion> {
        val questions = mutableListOf<PokeGuessQuestion>()
        val usedIds = mutableSetOf<Int>()

        val maxPokemonId = when (difficulty) {
            GuessDifficulty.EASY -> 151      // Gen 1 only
            GuessDifficulty.MEDIUM -> 493    // Up to Gen 4
            GuessDifficulty.HARD -> 1010     // All Pokemon
        }

        repeat(difficulty.questionsPerGame) {
            var attempts = 0
            while (attempts < 50) {
                val randomId = (1..maxPokemonId).random()
                if (randomId !in usedIds) {
                    usedIds.add(randomId)

                    try {
                        val pokemon = repository.getPokemonByName(randomId.toString())
                        val spriteUrl = pokemon.sprites.other?.officialArtwork?.frontDefault
                            ?: pokemon.sprites.front_default

                        if (spriteUrl != null) {
                            // Generate wrong options
                            val wrongOptions = generateWrongOptions(
                                correctName = pokemon.name,
                                count = difficulty.optionCount - 1,
                                maxId = maxPokemonId,
                                usedIds = usedIds
                            )

                            val allOptions = (wrongOptions + pokemon.name).shuffled()
                            val correctIndex = allOptions.indexOf(pokemon.name)

                            questions.add(
                                PokeGuessQuestion(
                                    pokemonId = randomId,
                                    pokemonName = pokemon.name,
                                    spriteUrl = spriteUrl,
                                    options = allOptions,
                                    correctIndex = correctIndex,
                                    generation = getGeneration(randomId),
                                    types = pokemon.types.map { it.type.name }
                                )
                            )
                            break
                        }
                    } catch (e: Exception) {
                        Log.w("PokeGuess", "Skipping pokemon $randomId: ${e.message}")
                    }
                }
                attempts++
            }
        }

        return questions
    }

    private suspend fun generateWrongOptions(
        correctName: String,
        count: Int,
        maxId: Int,
        usedIds: Set<Int>
    ): List<String> {
        val wrongOptions = mutableListOf<String>()
        var attempts = 0

        while (wrongOptions.size < count && attempts < 100) {
            val randomId = (1..maxId).random()
            if (randomId !in usedIds) {
                try {
                    val pokemon = repository.getPokemonByName(randomId.toString())
                    if (pokemon.name != correctName && pokemon.name !in wrongOptions) {
                        wrongOptions.add(pokemon.name)
                    }
                } catch (e: Exception) {
                    // Skip
                }
            }
            attempts++
        }

        return wrongOptions
    }

    private fun getGeneration(pokemonId: Int): Int {
        return when (pokemonId) {
            in 1..151 -> 1
            in 152..251 -> 2
            in 252..386 -> 3
            in 387..493 -> 4
            in 494..649 -> 5
            in 650..721 -> 6
            in 722..809 -> 7
            in 810..905 -> 8
            else -> 9
        }
    }

    private fun showQuestion(index: Int, difficulty: GuessDifficulty) {
        if (index >= allQuestions.size) {
            finishGame(difficulty)
            return
        }

        val question = allQuestions[index]
        _gameState.value = GuessGameState.ShowingSilhouette(
            question = question,
            currentQuestionIndex = index,
            totalQuestions = allQuestions.size,
            score = currentScore,
            timeRemaining = difficulty.timePerQuestion
        )

        startTimer(difficulty.timePerQuestion, index, difficulty)
    }

    private fun startTimer(totalTime: Int, questionIndex: Int, difficulty: GuessDifficulty) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var timeLeft = totalTime

            while (timeLeft > 0) {
                delay(1000)
                timeLeft--

                val currentState = _gameState.value
                if (currentState is GuessGameState.ShowingSilhouette) {
                    _gameState.value = currentState.copy(timeRemaining = timeLeft)
                }
            }

            onTimeUp(questionIndex, difficulty)
        }
    }

    private fun onTimeUp(questionIndex: Int, difficulty: GuessDifficulty) {
        timerJob?.cancel()

        val question = allQuestions[questionIndex]

        _gameState.value = GuessGameState.Revealing(
            question = question,
            selectedAnswer = "",
            isCorrect = false,
            isTimeUp = true,
            currentQuestionIndex = questionIndex,
            totalQuestions = allQuestions.size,
            score = currentScore
        )
    }

    fun submitAnswer(selectedAnswer: String, questionIndex: Int, difficulty: GuessDifficulty) {
        timerJob?.cancel()

        val question = allQuestions[questionIndex]
        val isCorrect = selectedAnswer == question.pokemonName

        if (isCorrect) {
            correctAnswers++

            val currentState = _gameState.value
            val timeBonus = if (currentState is GuessGameState.ShowingSilhouette) {
                (currentState.timeRemaining * 10).coerceAtLeast(0)
            } else {
                0
            }
            currentScore += 100 + timeBonus
        }

        _gameState.value = GuessGameState.Revealing(
            question = question,
            selectedAnswer = selectedAnswer,
            isCorrect = isCorrect,
            isTimeUp = false,
            currentQuestionIndex = questionIndex,
            totalQuestions = allQuestions.size,
            score = currentScore
        )
    }

    fun nextQuestion(difficulty: GuessDifficulty) {
        val currentState = _gameState.value
        if (currentState is GuessGameState.Revealing) {
            val nextIndex = currentState.currentQuestionIndex + 1
            showQuestion(nextIndex, difficulty)
        }
    }

    private fun finishGame(difficulty: GuessDifficulty) {
        _gameState.value = GuessGameState.Finished(
            score = currentScore,
            correctAnswers = correctAnswers,
            totalQuestions = allQuestions.size,
            difficulty = difficulty
        )
    }

    fun resetGame() {
        timerJob?.cancel()
        _gameState.value = GuessGameState.Idle
        currentScore = 0
        correctAnswers = 0
        allQuestions.clear()
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}