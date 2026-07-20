package com.dazou.iptvplayer.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import com.dazou.iptvplayer.R
import com.dazou.iptvplayer.databinding.FragmentSettingsBinding
import java.io.File

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val supportedLanguages = listOf(
        "ar" to "العربية",
        "en" to "English",
        "de" to "Deutsch",
        "pl" to "Polski"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)

        binding.btnLanguage.setOnClickListener {
            showLanguagePicker()
        }

        binding.btnCheckErrors.setOnClickListener {
            checkForErrors()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // ✅ توجيه الفوكس تلقائيًا لأول زر بالشاشة، لأنه القائمة الجانبية تختفي هون
        // وبدون هذا ما في شي يستلم الفوكس أول ما تفتح الشاشة
        requestFocusWhenReady(binding.btnLanguage)
    }

    private fun requestFocusWhenReady(view: View) {
        if (view.isLaidOut && view.width > 0 && view.height > 0) {
            view.requestFocus()
        } else {
            view.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    view.requestFocus()
                }
            })
        }
    }

    private fun showLanguagePicker() {
        val labels = supportedLanguages.map { it.second }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.settings_language))
            .setItems(labels) { _, which ->
                val languageTag = supportedLanguages[which].first
                AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.forLanguageTags(languageTag)
                )
            }
            .show()
    }

    private fun checkForErrors() {
        val dir = File(requireContext().filesDir, "crash_logs")
        val files = dir.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()

        binding.tvDiagnosticsResult.visibility = View.VISIBLE

        if (files.isEmpty()) {
            binding.tvDiagnosticsResult.text = getString(R.string.no_crash_logs)
        } else {
            val latest = files.first()
            val content = latest.readText()
            binding.tvDiagnosticsResult.text = "📄 ${latest.name}\n\n$content"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}