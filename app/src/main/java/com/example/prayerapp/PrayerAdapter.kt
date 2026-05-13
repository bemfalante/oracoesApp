package com.example.prayerapp

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.prayerapp.databinding.ItemPrayerBinding

class PrayerAdapter(private val onItemClicked: (Prayer) -> Unit) :
    ListAdapter<Prayer, PrayerAdapter.PrayerViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PrayerViewHolder {
        return PrayerViewHolder(
            ItemPrayerBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: PrayerViewHolder, position: Int) {
        val current = getItem(position)
        holder.bind(current)
        holder.itemView.setOnClickListener {
            onItemClicked(current)
        }
    }

    class PrayerViewHolder(private var binding: ItemPrayerBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(prayer: Prayer) {
            binding.tvTitle.text = prayer.title
            binding.tvContent.text = prayer.content
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<Prayer>() {
            override fun areItemsTheSame(oldItem: Prayer, newItem: Prayer): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Prayer, newItem: Prayer): Boolean {
                return oldItem == newItem
            }
        }
    }
}
