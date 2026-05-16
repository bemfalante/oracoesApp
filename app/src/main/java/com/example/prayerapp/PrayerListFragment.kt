package com.example.prayerapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.prayerapp.databinding.FragmentPrayerListBinding
import kotlinx.coroutines.launch
import java.util.*

class PrayerListFragment : Fragment() {

    private var _binding: FragmentPrayerListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PrayerViewModel by activityViewModels {
        val db = PrayerDatabase.getDatabase(requireContext())
        PrayerViewModelFactory(db.prayerDao(), db.categoryDao())
    }
    private lateinit var adapter: PrayerAdapter
    private var categoryId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        categoryId = arguments?.getInt(ARG_CATEGORY_ID) ?: 0
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPrayerListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = PrayerAdapter { prayer ->
            (activity as? MainActivity)?.showPrayerBottomSheet(prayer)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = adapter

        viewModel.getPrayersByCategory(categoryId).observe(viewLifecycleOwner) { prayers ->
            adapter.submitList(prayers)
        }

        setupDragAndDrop()
    }

    private fun setupDragAndDrop() {
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPos = viewHolder.adapterPosition
                val toPos = target.adapterPosition

                val list = adapter.currentList.toMutableList()
                Collections.swap(list, fromPos, toPos)
                adapter.submitList(list)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val prayer = adapter.currentList[position]

                viewLifecycleOwner.lifecycleScope.launch {
                    val categories = viewModel.getAllCategoriesList()
                    if (categories.size > 1) {
                        val currentIdx = categories.indexOfFirst { it.id == prayer.categoryId }
                        val nextIdx = (currentIdx + 1) % categories.size
                        val nextCategory = categories[nextIdx]
                        viewModel.update(prayer.copy(categoryId = nextCategory.id, position = 0))
                    } else {
                        adapter.notifyItemChanged(position)
                    }
                }
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                val list = adapter.currentList.mapIndexed { index, prayer ->
                    prayer.copy(position = index)
                }
                viewModel.updateAll(list)
            }
        })
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_CATEGORY_ID = "categoryId"

        fun newInstance(categoryId: Int) = PrayerListFragment().apply {
            arguments = Bundle().apply {
                putInt(ARG_CATEGORY_ID, categoryId)
            }
        }
    }
}
