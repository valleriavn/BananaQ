package bananaq

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.SystemClock
import android.view.View
import android.widget.EditText
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WindowBehaviorTest {
    @Test fun libraryStartsWithFirstCardAndStillOpensDiseaseDetails() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val list = activity.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.diseaseCarousel)
                val library = list.adapter as DiseaseLibraryAdapter
                val first = requireNotNull(list.findViewHolderForAdapterPosition(library.initialPosition))
                val content = activity.findViewById<View>(R.id.homeContent)
                assertTrue("Large empty space above navigation",
                    content.height - list.bottom <= 25 * activity.resources.displayMetrics.density)
                assertTrue("First library card starts outside its content inset",
                    first.itemView.left >= list.paddingLeft)
            }
            onView(androidx.test.espresso.matcher.ViewMatchers.withText(
                InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.disease_black_sigatoka)))
                .perform(click())
            onView(withId(R.id.detailDiseaseName)).check(matches(isDisplayed()))
            androidx.test.espresso.Espresso.pressBack()
        }
    }

    @Test fun libraryCanCrossCycleBoundariesInBothDirections() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            onView(withId(R.id.diseaseCarousel)).check(matches(isDisplayed()))
            var start = 0
            scenario.onActivity { activity ->
                val list = activity.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.diseaseCarousel)
                start = (list.adapter as DiseaseLibraryAdapter).initialPosition
                list.smoothScrollToPosition(start + 5)
            }
            waitForLibraryScroll(scenario, start + 5)
            scenario.onActivity { activity ->
                val list = activity.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.diseaseCarousel)
                val manager = list.layoutManager as androidx.recyclerview.widget.LinearLayoutManager
                assertTrue("Carousel stopped at the old final card", manager.findLastVisibleItemPosition() >= start + 5)
                list.smoothScrollToPosition(start - 3)
            }
            waitForLibraryScroll(scenario, start - 3)
            scenario.onActivity { activity ->
                val list = activity.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.diseaseCarousel)
                val manager = list.layoutManager as androidx.recyclerview.widget.LinearLayoutManager
                assertTrue("Carousel cannot loop backward", manager.findFirstVisibleItemPosition() <= start - 3)
            }
        }
    }

    private fun waitForLibraryScroll(scenario: ActivityScenario<MainActivity>, target: Int) {
        val deadline = SystemClock.uptimeMillis() + 5000
        var settled = false
        while (!settled && SystemClock.uptimeMillis() < deadline) {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity { activity ->
                val list = activity.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.diseaseCarousel)
                val manager = list.layoutManager as androidx.recyclerview.widget.LinearLayoutManager
                settled = list.scrollState == androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE &&
                    target in manager.findFirstVisibleItemPosition()..manager.findLastVisibleItemPosition()
            }
            if (!settled) SystemClock.sleep(50)
        }
        assertTrue("Carousel did not settle", settled)
    }

    @Test fun repeatedBarCutoutAndKeyboardInsetsDoNotAccumulate() {
        ActivityScenario.launch<FeedbackDetailActivity>(feedbackIntent()).use { scenario ->
            scenario.onActivity { activity ->
                val root = activity.findViewById<android.view.ViewGroup>(android.R.id.content).getChildAt(0)
                val nav = activity.findViewById<View>(R.id.bottomNavigation)
                val barHeight = activity.resources.getDimensionPixelSize(R.dimen.bottom_navigation_height)
                val insets = WindowInsetsCompat.Builder()
                    .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.of(12, 24, 12, 32))
                    .setInsets(WindowInsetsCompat.Type.displayCutout(), Insets.of(20, 0, 0, 0))
                    .setInsets(WindowInsetsCompat.Type.ime(), Insets.of(0, 0, 0, 300)).build()
                repeat(3) {
                    ViewCompat.dispatchApplyWindowInsets(root, insets)
                    assertEquals(20, root.paddingLeft)
                    assertEquals(24, root.paddingTop)
                    assertEquals(12, root.paddingRight)
                    assertEquals(268, root.paddingBottom)
                    assertEquals(barHeight + 32, nav.layoutParams.height)
                }
                ViewCompat.getRootWindowInsets(root)?.let { ViewCompat.dispatchApplyWindowInsets(root, it) }
            }
        }
    }

    @Test fun shortKeyboardWindowsCollapseHeadersAndRestoreThem() {
        ActivityScenario.launch<FeedbackDetailActivity>(feedbackIntent()).use { scenario ->
            scenario.onActivity { activity ->
                val root = activity.findViewById<android.view.ViewGroup>(android.R.id.content).getChildAt(0)
                val actual = requireNotNull(ViewCompat.getRootWindowInsets(root))
                val density = activity.resources.displayMetrics.density
                val keyboardHeight = (root.height - 200 * density).toInt().coerceAtLeast(1)
                val shortWindow = WindowInsetsCompat.Builder(actual)
                    .setInsets(WindowInsetsCompat.Type.ime(), Insets.of(0, 0, 0, keyboardHeight)).build()
                ViewCompat.dispatchApplyWindowInsets(root, shortWindow)
                assertEquals(View.GONE, activity.findViewById<View>(R.id.header).visibility)
                assertEquals(View.GONE, activity.findViewById<View>(R.id.titleRow).visibility)
                ViewCompat.dispatchApplyWindowInsets(root, actual)
                assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.header).visibility)
                assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.titleRow).visibility)
            }
        }
    }

    @Test fun keyboardKeepsCommentsVisibleAndSubmitReachable() {
        ActivityScenario.launch<FeedbackDetailActivity>(feedbackIntent()).use { scenario ->
            onView(withId(R.id.etComments)).perform(scrollTo(), click())
            scenario.onActivity { activity ->
                WindowInsetsControllerCompat(activity.window, activity.window.decorView)
                    .show(WindowInsetsCompat.Type.ime())
            }
            awaitIme(scenario, true)
            scenario.onActivity { activity ->
                val input = activity.findViewById<EditText>(R.id.etComments)
                input.requestRectangleOnScreen(Rect(0, 0, input.width, input.height), true)
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            onView(withId(R.id.etComments)).check(matches(isDisplayed()))
            scenario.onActivity { activity ->
                val input = activity.findViewById<View>(R.id.etComments)
                val visible = Rect()
                assertTrue("Comments hidden by keyboard", input.getGlobalVisibleRect(visible))
                assertEquals("Comments clipped above keyboard", input.height, visible.height())
                WindowInsetsControllerCompat(activity.window, activity.window.decorView)
                    .hide(WindowInsetsCompat.Type.ime())
            }
            awaitIme(scenario, false)
            onView(withId(R.id.btnSubmitFeedback)).perform(scrollTo()).check(matches(isDisplayed()))
        }
    }

    @Test fun avatarPickerActionsRemainReachableWithoutChangingSelection() {
        ActivityScenario.launch(AccountActivity::class.java).use {
            onView(withId(R.id.editAvatarButton)).perform(click())
            onView(withId(R.id.saveAvatarSelection)).perform(scrollTo()).check(matches(isDisplayed()))
            onView(withId(R.id.cancelAvatarSelection)).perform(scrollTo(), click())
        }
    }

    @Test fun captureActualOnboardingAndHomeLayouts() {
        for (activity in listOf(LanguageActivity::class.java, GetStartedActivity::class.java, MainActivity::class.java)) {
            ActivityScenario.launch(activity).use { scenario ->
                // Wait for the existing welcome/reveal animations, without changing device settings.
                SystemClock.sleep(1000)
                scenario.onActivity { screen ->
                    val root = screen.findViewById<android.view.ViewGroup>(android.R.id.content).getChildAt(0)
                    assertTrue(root.width > 0 && root.height > 0)
                    val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
                    try {
                        root.draw(android.graphics.Canvas(bitmap))
                        val file = File(screen.getExternalFilesDir(null), "ui-audit/${activity.simpleName}.png")
                        file.parentFile?.mkdirs()
                        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    } finally { bitmap.recycle() }
                }
            }
        }
    }

    private fun feedbackIntent() = Intent(
        InstrumentationRegistry.getInstrumentation().targetContext, FeedbackDetailActivity::class.java).apply {
        putExtra("DISEASE_NAME", "Cordana Leaf Spot")
        putExtra("DATE_TIME", "Wednesday, October 21, 2026 at 04:15 PM")
        putExtra("ACCURACY", "99.50%")
    }

    private fun awaitIme(scenario: ActivityScenario<FeedbackDetailActivity>, visible: Boolean) {
        val deadline = SystemClock.uptimeMillis() + 5000
        var matched = false
        while (!matched && SystemClock.uptimeMillis() < deadline) {
            scenario.onActivity { activity ->
                matched = ViewCompat.getRootWindowInsets(activity.window.decorView)
                    ?.isVisible(WindowInsetsCompat.Type.ime()) == visible
            }
            if (!matched) SystemClock.sleep(50)
        }
        assertTrue("Keyboard did not become visible=$visible", matched)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }
}
