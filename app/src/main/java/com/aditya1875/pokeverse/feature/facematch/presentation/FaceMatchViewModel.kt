package com.aditya1875.pokeverse.feature.facematch.presentation

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import com.aditya1875.pokeverse.feature.facematch.domain.FaceAnalyzer
import com.aditya1875.pokeverse.feature.facematch.domain.FaceMatcher
import com.aditya1875.pokeverse.feature.facematch.domain.PokemonLookalike
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class FaceMatchState {
    data object Idle : FaceMatchState()
    data object Analyzing : FaceMatchState()
    data class Result(val match: PokemonLookalike, val photo: Bitmap) : FaceMatchState()
    data class Error(val message: String) : FaceMatchState()
}

class FaceMatchViewModel : ViewModel() {

    private val _state = MutableStateFlow<FaceMatchState>(FaceMatchState.Idle)
    val state: StateFlow<FaceMatchState> = _state.asStateFlow()

    // Built lazily so the (small, bundled) model only loads once this screen is actually used,
    // and only once per ViewModel instance.
    private val detector by lazy {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
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

        _state.value = FaceMatchState.Analyzing

        val genericErrorMessage = "Something went wrong analyzing that photo. Try again?"
        runCatching {
            val image = InputImage.fromBitmap(bitmap, 0)
            detector.process(image)
                .addOnSuccessListener { faces ->
                    val face = faces.maxByOrNull { it.boundingBox.width().toLong() * it.boundingBox.height() }
                    if (face == null) {
                        _state.value = FaceMatchState.Error(
                            "No face detected — try again with your whole face in frame and good lighting."
                        )
                        return@addOnSuccessListener
                    }
                    runCatching {
                        val skinColor = FaceAnalyzer.sampleSkinColor(bitmap, face.boundingBox)
                        val shape = FaceAnalyzer.classifyShape(face.boundingBox)
                        FaceMatcher.pickMatch(skinColor, shape)
                    }.onSuccess { match ->
                        _state.value = FaceMatchState.Result(match, bitmap)
                    }.onFailure {
                        _state.value = FaceMatchState.Error(genericErrorMessage)
                    }
                }
                .addOnFailureListener {
                    _state.value = FaceMatchState.Error(genericErrorMessage)
                }
        }.onFailure {
            _state.value = FaceMatchState.Error(genericErrorMessage)
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
