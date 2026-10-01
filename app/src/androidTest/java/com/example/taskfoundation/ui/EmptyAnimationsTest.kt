package com.example.taskfoundation.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.test.core.app.ApplicationProvider
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieDrawable
import com.example.taskfoundation.R
import org.junit.Assert.*
import org.junit.Test

class EmptyAnimationsTest {
    @Test fun everyBundledIllustrationParsesAndDrawsVisiblePixels() {
        for (resource in listOf(R.raw.empty_tasks, R.raw.empty_done, R.raw.empty_projects, R.raw.empty_gantt, R.raw.empty_search)) {
            val result = LottieCompositionFactory.fromRawResSync(ApplicationProvider.getApplicationContext(), resource)
            assertNull(result.exception)
            val composition = checkNotNull(result.value)
            assertEquals(3000f, composition.duration, 1f)
            val drawable = LottieDrawable().apply {
                setComposition(composition)
                setBounds(0, 0, 200, 200)
                progress = .5f
            }
            val bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
            drawable.draw(Canvas(bitmap))
            val pixels = IntArray(40000)
            bitmap.getPixels(pixels, 0, 200, 0, 0, 200, 200)
            assertTrue("Illustration $resource should draw visible pixels", pixels.count { it ushr 24 > 0 } > 200)
            bitmap.recycle()
        }
    }
}
