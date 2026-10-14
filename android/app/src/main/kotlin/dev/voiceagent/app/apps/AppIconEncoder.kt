package dev.voiceagent.app.apps

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import java.io.ByteArrayOutputStream

/** Renders an app's launcher icon (any Drawable, incl. adaptive) to small PNG bytes. */
object AppIconEncoder {
    private const val SIZE_PX = 96

    fun toPng(drawable: Drawable): ByteArray {
        val bitmap = Bitmap.createBitmap(SIZE_PX, SIZE_PX, Bitmap.Config.ARGB_8888)
        // mutate() so changing bounds does not affect the shared cached drawable.
        drawable.mutate().apply {
            setBounds(0, 0, SIZE_PX, SIZE_PX)
            draw(Canvas(bitmap))
        }
        return ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            bitmap.recycle()
            out.toByteArray()
        }
    }
}
