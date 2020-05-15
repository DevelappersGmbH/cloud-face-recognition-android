package de.develappers.facerecognition.adapter

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import de.develappers.facerecognition.R
import de.develappers.facerecognition.database.model.RecognisedCandidate
import de.develappers.facerecognition.listeners.OnVisitorItemClickedListener
import de.develappers.facerecognition.database.model.Visitor
import de.develappers.facerecognition.serviceAI.ImageHelper
import kotlinx.android.synthetic.main.item_visitor.view.*

class VisitorListAdapter internal constructor(
    val context: Context,
    val listener: OnVisitorItemClickedListener
): RecyclerView.Adapter<VisitorListAdapter.VisitorViewHolder>() {

    private var items = emptyList<RecognisedCandidate>() // Cached copy of words

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VisitorViewHolder {
        return VisitorViewHolder(LayoutInflater.from(context).inflate(R.layout.item_visitor, parent, false))
    }

    override fun onBindViewHolder(holder: VisitorViewHolder, position: Int) {
        val candidate: RecognisedCandidate = items[position]
        holder.fullNameView?.text = context.resources.getString(R.string.full_name, candidate.visitor?.firstName, candidate.visitor?.lastName);
        holder.probabilityView.text = candidate.microsoft_conf.toString()
        val imgBitmap = ImageHelper.loadSizeLimitedBitmapFromUri(Uri.parse(candidate.visitor.imgPaths[0]), context)
        holder.photoView.setImageBitmap(imgBitmap)
        holder.mView.setOnClickListener { listener.onVisitorItemClicked(candidate.visitor) }

    }

    inner class VisitorViewHolder (view: View) : RecyclerView.ViewHolder(view) {
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

