package com.dazou.iptvplayer.fragments

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.dazou.iptvplayer.App
import com.dazou.iptvplayer.MainActivity
import com.dazou.iptvplayer.R
import com.dazou.iptvplayer.databinding.FragmentLoginBinding
import com.dazou.iptvplayer.model.XtreamServer

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private var ringAnimator: ObjectAnimator? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ✅ دوران مستمر للدائرة المتقطعة بس (المثلث بالنص يضل ثابت) — نفس شعار
        // التطبيق الحقيقي، بحركة سلسة بدون توقف طول ما شاشة تسجيل الدخول ظاهرة
        ringAnimator = ObjectAnimator.ofFloat(binding.ivLogoRing, "rotation", 0f, 360f).apply {
            duration = 3000L
            repeatCount = ObjectAnimator.INFINITE
            interpolator = LinearInterpolator()
            start()
        }

        binding.etUrl.requestFocus()

        binding.btnLogin.setOnClickListener {
            attemptLogin()
        }
    }

    private fun attemptLogin() {
        val url = binding.etUrl.text.toString().trim().trimEnd('/')
        val username = binding.etUser.text.toString().trim()
        val password = binding.etPass.text.toString().trim()

        if (url.isEmpty() || username.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.common_fill_all_fields), Toast.LENGTH_SHORT).show()
            return
        }

        val accountManager = (requireActivity().application as App).container.accountManager
        accountManager.saveAccount(XtreamServer(url, username, password))
        val accounts = accountManager.getAccounts()
        accountManager.setActiveAccount(accounts.size - 1)

        Toast.makeText(requireContext(), getString(R.string.login_success), Toast.LENGTH_SHORT).show()

        (activity as? MainActivity)?.goToHome()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // ✅ إيقاف الحركة صراحة قبل تدمير الشاشة — يمنع أي تسريب ذاكرة
        // من استمرار الأنيميشن على View ما عاد موجود
        ringAnimator?.cancel()
        ringAnimator = null
        _binding = null
    }
}