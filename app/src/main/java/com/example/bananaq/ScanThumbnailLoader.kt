package com.example.bananaq

import android.net.Uri
import android.widget.ImageView
import com.example.bananaq.ml.ScanImageLoader
import java.lang.ref.WeakReference
import java.util.concurrent.Future
import java.util.concurrent.FutureTask
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/** Decode bounded previews off the UI thread and ignore results for recycled rows. */
object ScanThumbnailLoader {
    private val executor = ThreadPoolExecutor(2, 2, 30L, TimeUnit.SECONDS,
        LinkedBlockingQueue<Runnable>()).apply { allowCoreThreadTimeOut(true) }

    fun load(view: ImageView, uri: String?) {
        (view.getTag(R.id.scan_image_request) as? Future<*>)?.cancel(true)
        executor.purge()
        view.setTag(R.id.scan_image_request, null)
        view.setImageResource(R.drawable.ic_logo)
        if (uri.isNullOrBlank()) return
        val target = WeakReference(view)
        val context = view.context.applicationContext
        lateinit var request: FutureTask<Unit>
        request = FutureTask<Unit> {
            val bitmap = try { ScanImageLoader.load(context, Uri.parse(uri), 256) }
                catch (_: Exception) { null }
            if (bitmap != null) {
                val image = target.get()
                if (image == null || !image.post {
                    if (image.getTag(R.id.scan_image_request) === request) {
                        image.setImageBitmap(bitmap)
                    } else bitmap.recycle()
                }) bitmap.recycle()
            }
            Unit
        }
        view.setTag(R.id.scan_image_request, request)
        executor.execute(request)
    }
}
