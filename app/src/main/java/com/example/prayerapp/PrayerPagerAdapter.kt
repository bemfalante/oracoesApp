package com.example.prayerapp

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class PrayerPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    private var categories: List<Category> = emptyList()

    fun setCategories(newCategories: List<Category>) {
        categories = newCategories
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = categories.size

    override fun createFragment(position: Int): Fragment {
        return PrayerListFragment.newInstance(categories[position].id)
    }

    override fun getItemId(position: Int): Long {
        return categories[position].id.toLong()
    }

    override fun containsItem(itemId: Long): Boolean {
        return categories.any { it.id.toLong() == itemId }
    }
}
