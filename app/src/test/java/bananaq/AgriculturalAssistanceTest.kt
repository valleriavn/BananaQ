package bananaq

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import androidx.core.graphics.ColorUtils
import androidx.core.content.ContextCompat
import java.io.File
import android.view.View
import android.view.LayoutInflater
import android.content.res.Configuration
import androidx.appcompat.view.ContextThemeWrapper
import java.util.Locale
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import bananaq.data.DiseaseRepository
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = BananaQApplication::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AgriculturalAssistanceTest {
    @Test fun accountContactWorksOfflineAndOpensOnlyTheDialerInBothLanguages() {
        for (language in listOf("en", "tl")) for (fontScale in listOf(1f, 2f)) {
            val widthDp = if (fontScale == 1f) 411 else 320
            val heightDp = if (fontScale == 1f) 890 else 480
            RuntimeEnvironment.setQualifiers("$language-w${widthDp}dp-h${heightDp}dp-320dpi")
            RuntimeEnvironment.setFontScale(fontScale)
            LocaleHelper.saveLanguage(RuntimeEnvironment.getApplication(), language)
            val controller = Robolectric.buildActivity(AccountActivity::class.java).setup()
            try {
                val activity = controller.get()
                activity.findViewById<View>(R.id.agriculturalAssistanceRow).performClick()
                val dialog = ShadowDialog.getLatestDialog()
                assertTrue(dialog.isShowing)
                val assistance = DiseaseRepository(activity).getDefaultPrevention()
                val office = assistance.office
                val phone = dialog.findViewById<TextView>(R.id.agricultureOfficePhone)
                val email = dialog.findViewById<TextView>(R.id.agricultureOfficeEmail)
                assertEquals(office.phone, phone.text.toString())
                assertEquals(office.email, email.text.toString())
                assertEquals(activity.getString(R.string.agriculture_extension, "307"),
                    dialog.findViewById<TextView>(R.id.agricultureOfficeExtension).text.toString())
                assertEquals(office.hours, dialog.findViewById<TextView>(R.id.agricultureOfficeHours).text.toString())
                assertEquals(office.address, dialog.findViewById<TextView>(R.id.agricultureOfficeAddress).text.toString())
                assertEquals(activity.getString(R.string.call_agriculture_office),
                    dialog.findViewById<TextView>(R.id.callAgricultureOffice).text.toString())
                assertTrue("Phone number should be prominent", phone.textSize > email.textSize)
                assertTrue("Call button needs strong contrast", ColorUtils.calculateContrast(
                    android.graphics.Color.WHITE, ContextCompat.getColor(activity, R.color.banana_text_dark)) >= 4.5)

                val scroll = dialog.findViewById<ViewGroup>(android.R.id.content).getChildAt(0) as NestedScrollView
                val density = activity.resources.displayMetrics.density
                val width = (widthDp * 0.96f * density).toInt()
                val height = ((heightDp - 64) * density).toInt()
                scroll.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
                scroll.layout(0, 0, width, height)
                for (id in listOf(R.id.callAgricultureOffice, R.id.agricultureOfficeSource, R.id.closeAgriculturalAssistance)) {
                    assertTrue("Action is too small", dialog.findViewById<View>(id).height >= 48 * density)
                }
                val body = scroll.getChildAt(0) as ViewGroup
                val callBounds = Rect()
                val hoursBounds = Rect()
                val callAction = dialog.findViewById<View>(R.id.callAgricultureOffice)
                val hoursField = dialog.findViewById<View>(R.id.agricultureOfficeHours)
                callAction.getDrawingRect(callBounds)
                hoursField.getDrawingRect(hoursBounds)
                body.offsetDescendantRectToMyCoords(callAction, callBounds)
                body.offsetDescendantRectToMyCoords(hoursField, hoursBounds)
                assertTrue("Call action should come before secondary details", callBounds.bottom <= hoursBounds.top)
                if (fontScale == 1f) assertTrue("Call action is below the first screen", callBounds.bottom <= scroll.height)
                scroll.fullScroll(View.FOCUS_DOWN)
                assertFalse("Contact dialog cannot reach its actions", scroll.canScrollVertically(1))
                assertTrue(dialog.findViewById<View>(R.id.closeAgriculturalAssistance).bottom - scroll.scrollY <= scroll.height)
                for (id in listOf(R.id.agricultureOfficePhone, R.id.agricultureOfficeExtension,
                        R.id.agricultureOfficeHours, R.id.agricultureOfficeAddress, R.id.agricultureOfficeEmail,
                        R.id.callAgricultureOffice, R.id.closeAgriculturalAssistance)) {
                    val text = dialog.findViewById<TextView>(id)
                    assertTrue(text.layout.lineCount > 0)
                    for (line in 0 until text.layout.lineCount) assertEquals("Text clipped: $id", 0, text.layout.getEllipsisCount(line))
                }

                dialog.findViewById<View>(R.id.callAgricultureOffice).performClick()
                val dial = shadowOf(activity).nextStartedActivity
                assertEquals(Intent.ACTION_DIAL, dial.action)
                assertEquals("tel", dial.data?.scheme)
                assertEquals("+63437848022", dial.data?.schemeSpecificPart)
                assertTrue("Keep extension visible when returning from dialer", dialog.isShowing)

                dialog.findViewById<View>(R.id.agricultureOfficeSource).performClick()
                val source = shadowOf(activity).nextStartedActivity
                assertEquals(Intent.ACTION_VIEW, source.action)
                assertEquals(assistance.contactSourceUrl, source.dataString)
                dialog.findViewById<View>(R.id.closeAgriculturalAssistance).performClick()
                assertFalse(dialog.isShowing)
            } finally {
                controller.pause().stop().destroy()
            }
        }
    }

    private fun capture(view: View, name: String) {
        fun prepare(node: View) {
            if (node is TextView) node.onPreDraw()
            if (node is ViewGroup) for (index in 0 until node.childCount) prepare(node.getChildAt(index))
        }
        prepare(view)
        view.viewTreeObserver.dispatchOnPreDraw()
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(bitmap)
            if (view is NestedScrollView) {
                // A detached root has no parent to apply its scroll offset during drawing.
                val body = view.getChildAt(0)
                canvas.translate((body.left - view.scrollX).toFloat(), (body.top - view.scrollY).toFloat())
                body.draw(canvas)
            } else view.draw(canvas)
            val file = File("build/reports/scaling/assistance/$name.png")
            file.parentFile?.mkdirs()
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        } finally { bitmap.recycle() }
    }

    @Test fun contactLayoutRespectsActualEnlargedFontsAndScrollsToEveryAction() {
        val application = RuntimeEnvironment.getApplication()
        for (language in listOf("en", "tl")) for (font in listOf(1f, 2f)) {
            val widthDp = if (font == 1f) 411 else 320
            val heightDp = if (font == 1f) 890 else 480
            val config = Configuration(application.resources.configuration).apply {
                setLocale(Locale.forLanguageTag(language))
                fontScale = font
                densityDpi = 320
                screenWidthDp = widthDp
                screenHeightDp = heightDp
            }
            val context = ContextThemeWrapper(application.createConfigurationContext(config), R.style.Theme_BananaQ)
            assertEquals(font, context.resources.configuration.fontScale, 0f)
            val office = DiseaseRepository(context).getDefaultPrevention(language).office
            val body = LayoutInflater.from(context).inflate(R.layout.dialog_agricultural_assistance, null)
            for ((id, value) in mapOf(R.id.agricultureOfficeName to office.name,
                    R.id.agricultureOfficePhone to office.phone,
                    R.id.agricultureOfficeExtension to context.getString(R.string.agriculture_extension, office.extension),
                    R.id.agricultureOfficeHours to office.hours,
                    R.id.agricultureOfficeAddress to office.address,
                    R.id.agricultureOfficeEmail to office.email)) {
                body.findViewById<TextView>(id).text = value
            }
            val scroll = NestedScrollView(context).apply {
                isSmoothScrollingEnabled = false
                addView(body)
            }
            val density = context.resources.displayMetrics.density
            val width = (widthDp * 0.96f * density).toInt()
            val height = ((heightDp - 64) * density).toInt()
            scroll.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
            scroll.layout(0, 0, width, height)
            capture(scroll, "layout_${language}_${font}_top")
            scroll.scrollTo(0, (body.height - scroll.height).coerceAtLeast(0))
            assertTrue(body.findViewById<View>(R.id.closeAgriculturalAssistance).bottom - scroll.scrollY <= scroll.height)
            for (id in listOf(R.id.callAgricultureOffice, R.id.closeAgriculturalAssistance, R.id.agricultureOfficeSource)) {
                val action = body.findViewById<TextView>(id)
                assertTrue(action.height >= 48 * density)
                for (line in 0 until action.layout.lineCount) assertEquals(0, action.layout.getEllipsisCount(line))
            }
            capture(scroll, "layout_${language}_${font}_bottom")
        }
    }

    @Test fun repeatedTapsReuseTheDialogAndDestroyClosesIt() {
        val controller = Robolectric.buildActivity(AccountActivity::class.java).setup()
        val row = controller.get().findViewById<View>(R.id.agriculturalAssistanceRow)
        row.performClick()
        val first = ShadowDialog.getLatestDialog()
        row.performClick()
        assertSame(first, ShadowDialog.getLatestDialog())
        controller.pause().stop().destroy()
        assertFalse(first.isShowing)
    }
}
