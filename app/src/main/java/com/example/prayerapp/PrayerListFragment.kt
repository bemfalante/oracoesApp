package com.example.prayerapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.prayerapp.databinding.FragmentPrayerListBinding
import java.util.*

class PrayerListFragment : Fragment() {

    private var _binding: FragmentPrayerListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PrayerViewModel by activityViewModels {
        PrayerViewModelFactory(PrayerDatabase.getDatabase(requireContext()).prayerDao())
    }
    private lateinit var adapter: PrayerAdapter
    private var category: String = "Catholic"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        category = arguments?.getString(ARG_CATEGORY) ?: "Catholic"
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

        viewModel.getPrayersByCategory(category).observe(viewLifecycleOwner) { prayers ->
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
                val newCategory = if (prayer.category == "Catholic") "Umbanda" else "Catholic"
                viewModel.update(prayer.copy(category = newCategory, position = 0))
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
        private const val ARG_CATEGORY = "category"

        fun newInstance(category: String) = PrayerListFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_CATEGORY, category)
            }
        }
    }
}
