package com.example.prayerapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebViewClient
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.prayerapp.databinding.ActivityMainBinding
import com.example.prayerapp.databinding.BottomSheetPrayerBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: PrayerViewModel by viewModels {
        val db = PrayerDatabase.getDatabase(this)
        PrayerViewModelFactory(db.prayerDao(), db.categoryDao())
    }
    private lateinit var pagerAdapter: PrayerPagerAdapter
    private var categories: List<Category> = emptyList()

    private var pendingSharedText: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        pagerAdapter = PrayerPagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter

        var tabLayoutMediator: TabLayoutMediator? = null

        viewModel.allCategories.observe(this) { newCategories ->
            val wasEmpty = categories.isEmpty()
            categories = newCategories
            pagerAdapter.setCategories(newCategories)

            if (newCategories.isEmpty()) {
                binding.tvEmptyInstructions.visibility = View.VISIBLE
                binding.tabLayout.visibility = View.GONE
                binding.viewPager.visibility = View.GONE
                tabLayoutMediator?.detach()
                tabLayoutMediator = null
            } else {
                binding.tvEmptyInstructions.visibility = View.GONE
                binding.tabLayout.visibility = View.VISIBLE
                binding.viewPager.visibility = View.VISIBLE

                tabLayoutMediator?.detach()
                tabLayoutMediator = TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
                    if (position in newCategories.indices) {
                        tab.text = newCategories[position].name
                    }
                }
                tabLayoutMediator?.attach()
                setupTabLongClick()
            }

            // Handle pending shared text after categories are loaded
            pendingSharedText?.let {
                showChooseCategoryDialog(it)
                pendingSharedText = null
            }
        }

        binding.fabAdd.setOnClickListener {
            val currentPos = binding.viewPager.currentItem
            if (currentPos in categories.indices) {
                showPrayerBottomSheet(categoryId = categories[currentPos].id)
            } else {
                Toast.makeText(this, R.string.add_category, Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnAddCategory.setOnClickListener {
            showAddCategoryDialog()
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
                if (categories.isEmpty() && viewModel.allCategories.value == null) {
                    pendingSharedText = sharedText
                } else {
                    showChooseCategoryDialog(sharedText)
                }
                intent.action = null // Prevent re-processing
            }
        }
    }

    private fun showChooseCategoryDialog(sharedText: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_choose_category, null)
        val spinner = dialogView.findViewById<Spinner>(R.id.spinnerCategories)
        val etNewCategory = dialogView.findViewById<EditText>(R.id.etNewCategory)

        etNewCategory.visibility = View.VISIBLE
        val categoryNames = categories.map { it.name }

        if (categoryNames.isEmpty()) {
            spinner.visibility = View.GONE
        } else {
            spinner.visibility = View.VISIBLE
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoryNames)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.choose_category)
            .setView(dialogView)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val newCategoryName = etNewCategory.text.toString().trim()

                if (newCategoryName.isNotBlank()) {
                    lifecycleScope.launch {
                        val newId = viewModel.insertCategoryWithId(Category(name = newCategoryName, position = categories.size))
                        showPrayerBottomSheet(Prayer(title = "", content = sharedText), categoryId = newId.toInt())
                    }
                } else if (categories.isNotEmpty()) {
                    val selectedIdx = spinner.selectedItemPosition
                    val category = categories[selectedIdx]
                    showPrayerBottomSheet(Prayer(title = "", content = sharedText), categoryId = category.id)
                } else {
                    Toast.makeText(this, R.string.category_name_hint, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showAddCategoryDialog() {
        val editText = EditText(this)
        editText.setPadding(48, 16, 48, 16)
        AlertDialog.Builder(this)
            .setTitle(R.string.add_category)
            .setView(editText)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val name = editText.text.toString()
                if (name.isNotBlank()) {
                    viewModel.insertCategory(Category(name = name, position = categories.size))
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun setupTabLongClick() {
        val tabLayout = binding.tabLayout
        for (i in 0 until tabLayout.tabCount) {
            val tab = tabLayout.getTabAt(i)
            val categoryIndex = i
            if (categoryIndex in categories.indices) {
                tab?.view?.setOnLongClickListener {
                    showCategoryOptionsDialog(categories[categoryIndex])
                    true
                }
            }
        }
    }

    private fun showCategoryOptionsDialog(category: Category) {
        val options = arrayOf(getString(R.string.rename_category), getString(R.string.delete))
        AlertDialog.Builder(this)
            .setTitle(category.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showRenameCategoryDialog(category)
                    1 -> showDeleteCategoryConfirmDialog(category)
                }
            }
            .show()
    }

    private fun showRenameCategoryDialog(category: Category) {
        val editText = EditText(this)
        editText.setText(category.name)
        editText.setPadding(48, 16, 48, 16)
        AlertDialog.Builder(this)
            .setTitle(R.string.rename_category)
            .setView(editText)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val newName = editText.text.toString()
                if (newName.isNotBlank()) {
                    viewModel.updateCategory(category.copy(name = newName))
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showDeleteCategoryConfirmDialog(category: Category) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete)
            .setMessage(R.string.delete_category_confirm)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                viewModel.deleteCategory(category)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    fun showPrayerBottomSheet(prayer: Prayer? = null, categoryId: Int = 0) {
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

                    if (isYoutube) {
                        val videoId = youtubeMatch?.groupValues?.get(1) ?: ""
                        val html = """
                            <!DOCTYPE html>
                            <html>
                            <body style="margin:0;padding:0;">
                                <iframe
                                    width="100%"
                                    height="100%"
                                    src="https://www.youtube.com/embed/$videoId?rel=0&autoplay=1&showinfo=0"
                                    frameborder="0"
                                    allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                                    allowfullscreen
                                    referrerpolicy="strict-origin-when-cross-origin">
                                </iframe>
                            </body>
                            </html>
                        """.trimIndent()
                        sheetBinding.webViewInstagram.loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "UTF-8", null)
                    } else if (isInstagram) {
                        val id = instagramMatch?.groupValues?.get(1)
                        val embedUrl = "https://www.instagram.com/p/$id/embed"
                        sheetBinding.webViewInstagram.loadUrl(embedUrl)
                    }
                } else {
                    sheetBinding.webViewInstagram.visibility = View.GONE
                }
            }

            if (prayer != null && prayer.id != 0) {
                sheetBinding.btnDelete.visibility = View.VISIBLE
                sheetBinding.btnShare.visibility = View.VISIBLE
            sheetBinding.btnMove.visibility = View.VISIBLE
            } else {
                sheetBinding.btnDelete.visibility = View.GONE
                sheetBinding.btnShare.visibility = View.GONE
            sheetBinding.btnMove.visibility = View.GONE
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

        sheetBinding.btnMove.setOnClickListener {
            showMovePrayerDialog(prayer!!, dialog)
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
                viewModel.insert(Prayer(title = finalTitle, content = content, categoryId = categoryId, position = 0))
            }
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showMovePrayerDialog(prayer: Prayer, bottomSheet: BottomSheetDialog) {
        val otherCategories = categories.filter { it.id != prayer.categoryId }
        if (otherCategories.isEmpty()) {
            Toast.makeText(this, R.string.add_category, Toast.LENGTH_SHORT).show()
            return
        }

        val categoryNames = otherCategories.map { it.name }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(R.string.move_to)
            .setItems(categoryNames) { _, which ->
                val newCategory = otherCategories[which]
                viewModel.update(prayer.copy(categoryId = newCategory.id, position = 0))
                bottomSheet.dismiss()
            }
            .show()
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
