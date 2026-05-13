package com.example.prayerapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.prayerapp.databinding.ActivityMainBinding
import com.example.prayerapp.databinding.BottomSheetPrayerBinding
import com.google.android.material.bottomsheet.BottomSheetDialog

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: PrayerViewModel by viewModels {
        PrayerViewModelFactory(PrayerDatabase.getDatabase(this).prayerDao())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter = PrayerAdapter { prayer ->
            showPrayerBottomSheet(prayer)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        viewModel.allPrayers.observe(this) { prayers ->
            adapter.submitList(prayers)
        }

        binding.fabAdd.setOnClickListener {
            showPrayerBottomSheet()
        }

        if (savedInstanceState == null) {
            handleIntent(intent)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (sharedText != null) {
                showPrayerBottomSheet(Prayer(title = "", content = sharedText))
                intent.action = null // Prevent re-processing
            }
        }
    }

    private fun showPrayerBottomSheet(prayer: Prayer? = null) {
        val dialog = BottomSheetDialog(this)
        val sheetBinding = BottomSheetPrayerBinding.inflate(layoutInflater)
        dialog.setContentView(sheetBinding.root)

        val isEditing = prayer != null && prayer.id != 0

        if (prayer != null) {
            sheetBinding.etTitle.setText(prayer.title)
            sheetBinding.etContent.setText(prayer.content)
        }

        if (isEditing) {
            sheetBinding.btnDelete.visibility = View.VISIBLE
            sheetBinding.btnShare.visibility = View.VISIBLE

            sheetBinding.btnDelete.setOnClickListener {
                AlertDialog.Builder(this)
                    .setTitle(R.string.delete)
                    .setMessage(R.string.confirm_delete)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        viewModel.delete(prayer!!)
                        dialog.dismiss()
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }

            sheetBinding.btnShare.setOnClickListener {
                sharePrayer(prayer!!)
            }
        }

        sheetBinding.btnSave.setOnClickListener {
            val title = sheetBinding.etTitle.text.toString()
            val content = sheetBinding.etContent.text.toString()

            if (content.isBlank()) {
                Toast.makeText(this, R.string.error_empty_content, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val finalTitle = if (title.isBlank()) getString(R.string.untitled) else title

            if (isEditing) {
                viewModel.update(prayer!!.copy(title = finalTitle, content = content))
            } else {
                viewModel.insert(Prayer(title = finalTitle, content = content))
            }
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun sharePrayer(prayer: Prayer) {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TITLE, prayer.title)
            putExtra(Intent.EXTRA_TEXT, "${prayer.title}\n\n${prayer.content}")
            type = "text/plain"
        }
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share)))
    }
}
