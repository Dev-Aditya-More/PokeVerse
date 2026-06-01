package com.aditya1875.pokeverse.data.review

import android.app.Activity
import android.content.Context
import com.aditya1875.pokeverse.utils.IReviewManager
import com.google.android.play.core.review.ReviewManagerFactory

class ReviewManager(private val context: Context) : IReviewManager {
    override fun requestReview(activity: Activity) {
        val manager = ReviewManagerFactory.create(context)
        manager.requestReviewFlow().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                manager.launchReviewFlow(activity, task.result)
            }
        }
    }
}
