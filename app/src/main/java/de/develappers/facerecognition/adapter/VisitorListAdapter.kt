package de.develappers.facerecognition.adapter

import android.content.Context
import android.net.Uri
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import de.develappers.facerecognition.FaceApp
import de.develappers.facerecognition.R
import de.develappers.facerecognition.database.model.RecognisedCandidate
import de.develappers.facerecognition.listeners.OnVisitorItemClickedListener
import de.develappers.facerecognition.utils.ImageHelper
import kotlinx.android.synthetic.main.item_visitor.view.*


class VisitorListAdapter internal constructor(
    val context: Context,
    val listener: OnVisitorItemClickedListener
) : RecyclerView.Adapter<VisitorListAdapter.VisitorViewHolder>() {

    private var items = emptyList<RecognisedCandidate>() // Cached copy of candidates

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VisitorViewHolder {
        return VisitorViewHolder(LayoutInflater.from(context).inflate(R.layout.item_visitor, parent, false))
    }

    override fun onBindViewHolder(holder: VisitorViewHolder, position: Int) {
        val candidate: RecognisedCandidate = items[position]
        holder.fullNameView?.text =
            context.resources.getString(R.string.full_name, candidate.visitor.firstName, candidate.visitor.lastName);
        //set the confidence results in the columns according to predefined order in FaceApp
        FaceApp.values.keys.map { context.getString(it) }.forEach { title ->
            val textView = TextView(context)
            candidate.serviceResults.find { it.provider == title }?.confidence?.times(100.0)?.let{
                textView.text = context.getString(R.string.confidence, it)
            }

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1.0f
            )
            params.gravity = Gravity.CENTER;
            textView.layoutParams = params
            textView.gravity = Gravity.CENTER;

            holder.probabilityView.addView(textView)
        }

        val imgBitmap = ImageHelper.loadSizeLimitedBitmapFromUri(Uri.parse(candidate.visitor.imgPaths[0]), context)
        holder.photoView.setImageBitmap(imgBitmap)
        holder.mView.setOnClickListener { listener.onVisitorItemClicked(candidate) }

    }

    inner class VisitorViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val mView = view
        val photoView = view.photoView
        val fullNameView = view.fullNameView
        val probabilityView = view.probabilityView
    }

    internal fun setVisitors(candidates: List<RecognisedCandidate>) {
        this.items = candidates
        notifyDataSetChanged()
    }

}

