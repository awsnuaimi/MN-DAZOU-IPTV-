package com.dazou.iptvplayer.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.dazou.iptvplayer.databinding.FragmentSettingsBinding
import java.io.File

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)

        binding.btnCheckErrors.setOnClickListener {
            checkForErrors()
        }

        return binding.root
    }

    private fun checkForErrors() {
        // ===== تم التعديل هنا =====
        val dir = File(requireContext().filesDir, "crash_logs")  // ← تغيير المسار
        val files = dir.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()

        binding.tvDiagnosticsResult.visibility = View.VISIBLE

        if (files.isEmpty()) {
            binding.tvDiagnosticsResult.text = getString(com.dazou.iptvplayer.R.string.no_crash_logs)
        } else {
            val latest = files.first()
            val content = latest.readText()
            binding.tvDiagnosticsResult.text =
                "📄 ${latest.name}\n\n$content"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}