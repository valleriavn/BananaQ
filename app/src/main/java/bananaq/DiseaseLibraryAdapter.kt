package bananaq

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class LibraryDisease(
    val key: String,
    val name: Int,
    val image: Int,
    val scientificName: Int,
    val description: Int,
    val symptoms: Int,
    val spread: Int
)

val libraryDiseases = listOf(
    LibraryDisease(
        "Black Sigatoka", R.string.disease_black_sigatoka, R.drawable.black_sigatoka,
        R.string.scientific_black_sigatoka, R.string.description_black_sigatoka,
        R.string.symptoms_black_sigatoka, R.string.spread_black_sigatoka
    ),
    LibraryDisease(
        "Panama Disease", R.string.disease_panama, R.drawable.panama_disease,
        R.string.scientific_panama, R.string.description_panama,
        R.string.symptoms_panama, R.string.spread_panama
    ),
    LibraryDisease(
        "Cordana Leaf Spot", R.string.disease_cordana, R.drawable.cordana_leaf_spot,
        R.string.scientific_cordana, R.string.description_cordana,
        R.string.symptoms_cordana, R.string.spread_cordana
    ),
)

/** Repeat cached disease cards without an end stop or visible recentering during a swipe. */
class DiseaseLibraryAdapter(private val onClick: (String) -> Unit) :
    RecyclerView.Adapter<DiseaseLibraryAdapter.Holder>() {
    private val images = mutableMapOf<Int, Bitmap?>()
    val initialPosition = (Int.MAX_VALUE / 2 / libraryDiseases.size) * libraryDiseases.size

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.libraryImage)
        val name: TextView = view.findViewById(R.id.libraryName)
    }

    override fun getItemCount() = Int.MAX_VALUE

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_disease_library, parent, false)
        val metrics = parent.resources.displayMetrics
        // Resolve sp through Android (including nonlinear font scaling on API 34+).
        val textScale = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14f, metrics) /
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 14f, metrics)
        val available = maxOf(parent.width, parent.measuredWidth) - parent.paddingLeft - parent.paddingRight
        view.layoutParams = view.layoutParams.apply {
            val desired = (width * maxOf(1f, textScale)).toInt()
            val margins = (this as? ViewGroup.MarginLayoutParams)?.let {
                it.leftMargin + it.rightMargin
            } ?: 0
            width = if (available > margins) minOf(desired, available - margins) else desired
        }
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val disease = libraryDiseases[position % libraryDiseases.size]
        holder.name.setText(disease.name)
        holder.image.setImageBitmap(images.getOrPut(disease.image) {
            thumbnail(holder.itemView.resources, disease.image)
        })
        holder.itemView.contentDescription = holder.itemView.context.getString(disease.name)
        holder.itemView.setOnClickListener { onClick(disease.key) }
    }

    private fun thumbnail(resources: Resources, id: Int): Bitmap? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true; inScaled = false }
        BitmapFactory.decodeResource(resources, id, options)
        var sample = 1
        while (maxOf(options.outWidth, options.outHeight) / sample > 512) sample *= 2
        options.inJustDecodeBounds = false
        options.inSampleSize = sample
        return BitmapFactory.decodeResource(resources, id, options)
    }
}
