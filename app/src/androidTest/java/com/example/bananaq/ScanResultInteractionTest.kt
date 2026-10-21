package com.example.bananaq

import android.content.Intent
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.CoordinatesProvider
import androidx.test.espresso.action.GeneralLocation
import androidx.test.espresso.action.GeneralSwipeAction
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Swipe
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.material.bottomsheet.BottomSheetBehavior
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScanResultInteractionTest {
    @Test fun tapOpensAndDraggingDownReturnsToTheSummary() {
        launchResult().use { scenario ->
            onView(withId(R.id.predictionSummary)).perform(click())
            waitForSheet(scenario, BottomSheetBehavior.STATE_EXPANDED)
            onView(withId(R.id.fullResultCard)).check(matches(isDisplayed()))
            val bottomOfWindow = CoordinatesProvider { view ->
                val origin = IntArray(2)
                view.rootView.getLocationOnScreen(origin)
                val handle = IntArray(2)
                view.getLocationOnScreen(handle)
                floatArrayOf(handle[0] + view.width / 2f,
                    origin[1] + view.rootView.height - 32 * view.resources.displayMetrics.density)
            }
            onView(withId(R.id.resultDragHandle)).perform(
                GeneralSwipeAction(Swipe.SLOW, GeneralLocation.CENTER, bottomOfWindow, Press.FINGER))
            waitForSheet(scenario, BottomSheetBehavior.STATE_HIDDEN)
            onView(withId(R.id.compactResultPanel)).check(matches(isDisplayed()))
            scenario.onActivity { activity ->
                assertEquals(View.GONE, activity.findViewById<View>(R.id.fullResultCard).visibility)
            }
            // A dismissed result remains usable without rescanning.
            onView(withId(R.id.predictionSummary)).perform(click())
            waitForSheet(scenario, BottomSheetBehavior.STATE_EXPANDED)
            onView(withId(R.id.fullResultCard)).check(matches(isDisplayed()))
        }
    }

    @Test fun handlesSupportAccessibleTapActions() {
        launchResult().use { scenario ->
            onView(withId(R.id.compactResultDragHandle)).perform(click())
            waitForSheet(scenario, BottomSheetBehavior.STATE_EXPANDED)
            onView(withId(R.id.resultDragHandle)).perform(click())
            waitForSheet(scenario, BottomSheetBehavior.STATE_HIDDEN)
            onView(withId(R.id.compactResultPanel)).check(matches(isDisplayed()))
        }
    }

    private fun launchResult(): ActivityScenario<ScannerActivity> {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return ActivityScenario.launch(Intent(context, ScannerActivity::class.java).apply {
            putExtra("DISEASE_NAME", "Black Sigatoka")
            putExtra("LIBRARY", true)
            putExtra("CONFIDENCE_SCORE", 0.9f)
        })
    }

    private fun waitForSheet(scenario: ActivityScenario<ScannerActivity>, expected: Int) {
        val ready = CountDownLatch(1)
        var callback: BottomSheetBehavior.BottomSheetCallback? = null
        scenario.onActivity { activity ->
            val behavior = BottomSheetBehavior.from(activity.findViewById<View>(R.id.fullResultCard))
            if (behavior.state == expected) ready.countDown()
            else {
                callback = object : BottomSheetBehavior.BottomSheetCallback() {
                    override fun onStateChanged(bottomSheet: View, newState: Int) {
                        if (newState == expected) ready.countDown()
                    }
                    override fun onSlide(bottomSheet: View, slideOffset: Float) = Unit
                }
                behavior.addBottomSheetCallback(callback!!)
            }
        }
        try {
            assertTrue("Sheet did not reach state $expected", ready.await(5, TimeUnit.SECONDS))
        } finally {
            scenario.onActivity { activity ->
                callback?.let {
                    BottomSheetBehavior.from(activity.findViewById<View>(R.id.fullResultCard))
                        .removeBottomSheetCallback(it)
                }
            }
        }
    }
}
