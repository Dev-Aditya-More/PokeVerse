package com.aditya1875.pokeverse.feature.facematch.presentation

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aditya1875.pokeverse.feature.facematch.domain.FaceAnalyzer
import com.aditya1875.pokeverse.feature.facematch.domain.FaceMatcher
import com.aditya1875.pokeverse.feature.facematch.domain.PokemonLookalike
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class FaceMatchState {
    data object Idle : FaceMatchState()
    data class Analyzing(val photo: Bitmap) : FaceMatchState()
    data class Result(val match: PokemonLookalike, val photo: Bitmap) : FaceMatchState()
    data class Error(val message: String) : FaceMatchState()
}

/** On-device face detection typically resolves in well under this, but a bare instant flash of
 * the "analyzing" state reads as broken rather than as the app doing real work — this floor
 * gives the scan animation room to land before the result appears. */
private const val MIN_ANALYZING_MS = 1400L

class FaceMatchViewModel : ViewModel() {

    private val _state = MutableStateFlow<FaceMatchState>(FaceMatchState.Idle)
    val state: StateFlow<FaceMatchState> = _state.asStateFlow()

    // Built lazily so the (small, bundled) model only loads once this screen is actually used,
    // and only once per ViewModel instance.
    private val detector by lazy {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build()
        )
    }

    /** [bitmap] is null when the user backed out of the camera without taking a photo — treated
     * as a silent no-op, not an error. */
    fun analyze(bitmap: Bitmap?) {
        if (bitmap == null) {
            _state.value = FaceMatchState.Idle
            return
        }

        _state.value = FaceMatchState.Analyzing(bitmap)
        val startedAt = System.currentTimeMillis()

        val genericErrorMessage = "Something went wrong analyzing that photo. Try again?"

        fun finish(result: FaceMatchState) {
            viewModelScope.launch {
                val elapsed = System.currentTimeMillis() - startedAt
                if (elapsed < MIN_ANALYZING_MS) delay(MIN_ANALYZING_MS - elapsed)
                _state.value = result
            }
        }

        runCatching {
            val image = InputImage.fromBitmap(bitmap, 0)
            detector.process(image)
                .addOnSuccessListener { faces ->
                    val face = faces.maxByOrNull { it.boundingBox.width().toLong() * it.boundingBox.height() }
                    if (face == null) {
                        finish(
                            FaceMatchState.Error(
                                "No face detected — try again with your whole face in frame and good lighting."
                            )
                        )
                        return@addOnSuccessListener
                    }
                    runCatching {
                        val skinColor = FaceAnalyzer.sampleSkinColor(bitmap, face.boundingBox)
                        val shape = FaceAnalyzer.classifyShape(face.boundingBox)
                        val smilingProb = face.smilingProbability ?: 0.5f
                        val eyeOpenProb = ((face.leftEyeOpenProbability ?: 0.8f) + (face.rightEyeOpenProbability ?: 0.8f)) / 2f
                        FaceMatcher.pickMatch(skinColor, shape, smilingProb, eyeOpenProb)
                    }.onSuccess { match ->
                        finish(FaceMatchState.Result(match, bitmap))
                    }.onFailure {
                        finish(FaceMatchState.Error(genericErrorMessage))
                    }
                }
                .addOnFailureListener {
                    finish(FaceMatchState.Error(genericErrorMessage))
                }
        }.onFailure {
            finish(FaceMatchState.Error(genericErrorMessage))
        }
    }

    fun reset() {
        _state.value = FaceMatchState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        detector.close()
    }
}
