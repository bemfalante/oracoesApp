package com.example.prayerapp

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.example.prayerapp.databinding.ActivityStoicBinding
import java.util.*

class StoicActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStoicBinding
    private val viewModel: PrayerViewModel by viewModels {
        val db = PrayerDatabase.getDatabase(this)
        PrayerViewModelFactory(db.prayerDao(), db.categoryDao(), db.stoicDao())
    }

    private var currentCalendar = Calendar.getInstance()
    private var selectedDay: Int = currentCalendar.get(Calendar.DAY_OF_MONTH)
    private var currentMeditations: List<StoicMeditation> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStoicBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        setupCalendar()
        observeMeditations()

        binding.btnPrevMonth.setOnClickListener {
            currentCalendar.add(Calendar.MONTH, -1)
            updateMonth()
        }

        binding.btnNextMonth.setOnClickListener {
            currentCalendar.add(Calendar.MONTH, 1)
            updateMonth()
        }
    }

    private fun setupCalendar() {
        binding.rvCalendar.layoutManager = GridLayoutManager(this, 7)
        updateMonth()
    }

    private fun updateMonth(isInitial: Boolean = false) {
        val month = currentCalendar.get(Calendar.MONTH) + 1
        binding.tvMonthName.text = currentCalendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())

        viewModel.getStoicMeditationsByMonth(month).observe(this) { meditations ->
            currentMeditations = meditations
            renderCalendar()

            if (isInitial) {
                selectDay(currentCalendar.get(Calendar.DAY_OF_MONTH))
            } else {
                // If we changed month, select 1st day of that month
                selectDay(1)
            }
        }
    }

    private fun observeMeditations() {
        // Handled in updateMonth for now
    }

    private fun renderCalendar() {
        val tempCal = currentCalendar.clone() as Calendar
        tempCal.set(Calendar.DAY_OF_MONTH, 1)
        val firstDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK) - 1 // 0-indexed (Sunday=0)
        val daysInMonth = currentCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        val totalItems = firstDayOfWeek + daysInMonth

        binding.rvCalendar.adapter = object : androidx.recyclerview.widget.RecyclerView.Adapter<CalendarViewHolder>() {
            override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): CalendarViewHolder {
                val view = layoutInflater.inflate(R.layout.item_calendar_day, parent, false)
                return CalendarViewHolder(view)
            }

            override fun onBindViewHolder(holder: CalendarViewHolder, position: Int) {
                val tv = holder.itemView.findViewById<android.widget.TextView>(R.id.tvDay)

                if (position < firstDayOfWeek) {
                    tv.text = ""
                    tv.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    tv.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                    holder.itemView.setOnClickListener(null)
                } else {
                    val day = position - firstDayOfWeek + 1
                    tv.text = day.toString()

                    val meditation = currentMeditations.find { it.day == day }

                    // Read status indicator
                    if (meditation?.isRead == true) {
                        tv.setTextColor(ContextCompat.getColor(this@StoicActivity, R.color.white))
                        tv.setBackgroundResource(R.drawable.bg_calendar_read)
                    } else {
                        tv.setTextColor(ContextCompat.getColor(this@StoicActivity, R.color.black))
                        tv.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    }

                    // Selection indicator
                    if (day == selectedDay) {
                        tv.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, android.R.drawable.button_onoff_indicator_on)
                    } else {
                        tv.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                    }

                    holder.itemView.setOnClickListener {
                        selectDay(day)
                    }
                }
            }

            override fun getItemCount(): Int = totalItems
        }
    }

    private fun selectDay(day: Int) {
        selectedDay = day
        renderCalendar()

        val meditation = currentMeditations.find { it.day == day }
        if (meditation != null) {
            showMeditation(meditation)
        } else {
            binding.meditationContent.visibility = View.GONE
        }
    }

    private fun showMeditation(meditation: StoicMeditation) {
        binding.meditationContent.visibility = View.VISIBLE
        binding.tvMeditationTitle.text = meditation.title
        binding.tvQuote.text = "\"${meditation.quote}\""
        binding.tvSource.text = meditation.source
        binding.tvCommentary.text = meditation.commentary

        updateReadButton(meditation.isRead)

        binding.btnMarkRead.setOnClickListener {
            viewModel.updateStoicMeditation(meditation.copy(isRead = !meditation.isRead))
        }
    }

    private fun updateReadButton(isRead: Boolean) {
        if (isRead) {
            binding.btnMarkRead.text = getString(R.string.already_read)
            binding.btnMarkRead.alpha = 0.5f
        } else {
            binding.btnMarkRead.text = getString(R.string.mark_as_read)
            binding.btnMarkRead.alpha = 1.0f
        }
    }

    class CalendarViewHolder(view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view)
}
