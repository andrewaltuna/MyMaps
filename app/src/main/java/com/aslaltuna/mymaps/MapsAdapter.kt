package com.aslaltuna.mymaps

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aslaltuna.mymaps.models.UserMap

private const val TAG = "MapsAdapter"
class MapsAdapter(val context: Context, val userMaps: List<UserMap>, val recyclerViewInterface: RecyclerViewInterface) : RecyclerView.Adapter<MapsAdapter.ViewHolder>() {

    interface RecyclerViewInterface {
        fun onItemClick(position: Int)
        fun onItemLongClick(position: Int)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_user_map, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val userMap = userMaps[position]
        holder.itemView.setOnClickListener {
            Log.i(TAG, "Tapped on position $position")
            recyclerViewInterface.onItemClick(position)
        }

        val textViewTitle = holder.itemView.findViewById<TextView>(R.id.tvMapTitle)
        textViewTitle.text = userMap.title

        val textViewSubTitle = holder.itemView.findViewById<TextView>(R.id.tvMapPlaces)

        val placeList = userMap.places.map { it.title }
        textViewSubTitle.text = placeList.joinToString(", ")

        holder.itemView.setOnLongClickListener {
            recyclerViewInterface.onItemLongClick(position)
            return@setOnLongClickListener true
        }
    }

    override fun getItemCount() = userMaps.size

}
