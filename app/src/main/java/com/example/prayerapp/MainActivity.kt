package com.example.prayerapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.prayerapp.databinding.ActivityMainBinding
import com.example.prayerapp.databinding.BottomSheetPrayerBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: PrayerViewModel by viewModels {
        PrayerViewModelFactory(PrayerDatabase.getDatabase(this).prayerDao())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val pagerAdapter = PrayerPagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = if (position == 0) getString(R.string.tab_catholic) else getString(R.string.tab_umbanda)
        }.attach()

        binding.fabAdd.setOnClickListener {
            val category = if (binding.viewPager.currentItem == 0) Constants.CATEGORY_CATHOLIC else Constants.CATEGORY_UMBANDA
            showPrayerBottomSheet(category = category)
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
                val activeCategory = if (binding.viewPager.currentItem == 0) Constants.CATEGORY_CATHOLIC else Constants.CATEGORY_UMBANDA
                showPrayerBottomSheet(Prayer(title = "", content = sharedText), category = activeCategory)
                intent.action = null // Prevent re-processing
            }
        }
    }

    fun showPrayerBottomSheet(prayer: Prayer? = null, category: String = Constants.CATEGORY_CATHOLIC) {
        val dialog = BottomSheetDialog(this)
        val sheetBinding = BottomSheetPrayerBinding.inflate(layoutInflater)
        dialog.setContentView(sheetBinding.root)

        var isEditMode = prayer == null || prayer.id == 0

        fun updateUI() {
            val content = prayer?.content ?: ""

            val instagramRegex = "https?://(?:www\\.)?instagram\\.com/(?:p|reels|reel)/([^/?#&]+)".toRegex()
            val youtubeRegex = "https?://(?:www\\.)?(?:youtube\\.com/watch\\?v=|youtu\\.be/|youtube\\.com/shorts/|youtube\\.com/embed/)([^/?#&]+)".toRegex()

            val instagramMatch = instagramRegex.find(content)
            val youtubeMatch = youtubeRegex.find(content)

            val isInstagram = instagramMatch != null
            val isYoutube = youtubeMatch != null

            if (isEditMode) {
                sheetBinding.tvTitle.visibility = View.GONE
                sheetBinding.tvContent.visibility = View.GONE
                sheetBinding.webViewInstagram.visibility = View.GONE
                sheetBinding.tilTitle.visibility = View.VISIBLE
                sheetBinding.tilContent.visibility = View.VISIBLE
                sheetBinding.btnSave.visibility = View.VISIBLE
                sheetBinding.btnEdit.visibility = View.GONE
            } else {
                sheetBinding.tvTitle.visibility = View.VISIBLE
                sheetBinding.tvContent.visibility = View.VISIBLE
                sheetBinding.tilTitle.visibility = View.GONE
                sheetBinding.tilContent.visibility = View.GONE
                sheetBinding.btnSave.visibility = View.GONE
                sheetBinding.btnEdit.visibility = View.VISIBLE

                sheetBinding.tvTitle.text = prayer?.title
                sheetBinding.tvContent.text = prayer?.content

                if (isInstagram || isYoutube) {
                    sheetBinding.webViewInstagram.visibility = View.VISIBLE
                    sheetBinding.webViewInstagram.settings.javaScriptEnabled = true
                    sheetBinding.webViewInstagram.settings.domStorageEnabled = true
                    sheetBinding.webViewInstagram.settings.mediaPlaybackRequiresUserGesture = false
                    sheetBinding.webViewInstagram.settings.loadWithOverviewMode = true
                    sheetBinding.webViewInstagram.settings.useWideViewPort = true
                    // Use a desktop user agent to avoid some embed restrictions
                    sheetBinding.webViewInstagram.settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"
                    sheetBinding.webViewInstagram.webViewClient = WebViewClient()

                    val embedUrl = when {
                        isInstagram -> {
                            val id = instagramMatch?.groupValues?.get(1)
                            "https://www.instagram.com/p/$id/embed"
                        }
                        isYoutube -> {
                            val id = youtubeMatch?.groupValues?.get(1)
                            "https://www.youtube.com/embed/$id?rel=0&autoplay=0&showinfo=0"
                        }
                        else -> ""
                    }
                    sheetBinding.webViewInstagram.loadUrl(embedUrl)
                } else {
                    sheetBinding.webViewInstagram.visibility = View.GONE
                }
            }

            if (prayer != null && prayer.id != 0) {
                sheetBinding.btnDelete.visibility = View.VISIBLE
                sheetBinding.btnShare.visibility = View.VISIBLE
            } else {
                sheetBinding.btnDelete.visibility = View.GONE
                sheetBinding.btnShare.visibility = View.GONE
            }
        }

        if (prayer != null) {
            sheetBinding.etTitle.setText(prayer.title)
            sheetBinding.etContent.setText(prayer.content)
        }

        updateUI()

        sheetBinding.btnEdit.setOnClickListener {
            isEditMode = true
            updateUI()
        }

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

        sheetBinding.btnSave.setOnClickListener {
            val title = sheetBinding.etTitle.text.toString()
            val content = sheetBinding.etContent.text.toString()

            if (content.isBlank()) {
                Toast.makeText(this, R.string.error_empty_content, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val finalTitle = if (title.isBlank()) getString(R.string.untitled) else title

            if (prayer != null && prayer.id != 0) {
                viewModel.update(prayer.copy(title = finalTitle, content = content))
            } else {
                viewModel.insert(Prayer(title = finalTitle, content = content, category = category))
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
