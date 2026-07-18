package com.dazou.iptvplayer.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.dazou.iptvplayer.App
import com.dazou.iptvplayer.MainActivity
import com.dazou.iptvplayer.databinding.FragmentLoginBinding
import com.dazou.iptvplayer.model.XtreamServer

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

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
            Toast.makeText(requireContext(), "الرجاء ملء جميع الحقول", Toast.LENGTH_SHORT).show()
            return
        }

        val accountManager = (requireActivity().application as App).container.accountManager
        accountManager.saveAccount(XtreamServer(url, username, password))
        val accounts = accountManager.getAccounts()
        accountManager.setActiveAccount(accounts.size - 1)

        Toast.makeText(requireContext(), "✅ تم تسجيل الدخول", Toast.LENGTH_SHORT).show()

        (activity as? MainActivity)?.goToHome()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}