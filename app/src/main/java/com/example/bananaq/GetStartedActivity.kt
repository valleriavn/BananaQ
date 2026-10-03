package com.example.bananaq

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.view.View
import androidx.activity.enableEdgeToEdge

class GetStartedActivity : LocaleAwareActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_get_started)
        applySystemInsets(androidx.core.content.ContextCompat.getColor(this, R.color.onboarding_cream))

        val pageBackground = findViewById<View>(R.id.pageBackground)
        val content = listOf<View>(
            findViewById(R.id.logoOnboarding),
            findViewById(R.id.descText),
            findViewById(R.id.btnGetStarted),
            findViewById(R.id.bananaPlant)
        )
        content.forEach { it.alpha = 0f }
        pageBackground.post {
            pageBackground.translationY = pageBackground.height.toFloat()
            pageBackground.animate()
                .translationY(0f)
                .setDuration(550L)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
            content.forEachIndexed { index, view ->
                view.animate().alpha(1f).setStartDelay(180L + index * 45L)
                    .setDuration(320L).start()
            }
        }

        animatePlantIllustration(findViewById(R.id.bananaPlant))

        val btnGetStarted = findViewById<Button>(R.id.btnGetStarted)
        btnGetStarted.setOnClickListener {
            startActivity(Intent(this, UserAgreementActivity::class.java))
        }
    }
}
