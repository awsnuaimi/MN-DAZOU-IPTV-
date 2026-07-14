package com.dazou.iptvplayer.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.dazou.iptvplayer.R
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

class AccountsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.fragment_accounts, container, false)

        val btnAdd = view.findViewById<Button>(R.id.btnAddAccount)

        btnAdd.setOnClickListener {
            showLoginDialog()
        }

        return view
    }


    private fun showLoginDialog() {

        val dialogView = layoutInflater.inflate(
            R.layout.dialog_login,
            null
        )

        val etUrl = dialogView.findViewById<EditText>(R.id.etUrl)
        val etUser = dialogView.findViewById<EditText>(R.id.etUser)
        val etPass = dialogView.findViewById<EditText>(R.id.etPass)


        AlertDialog.Builder(requireContext())
            .setTitle("إضافة حساب Xtream")
            .setView(dialogView)
            .setPositiveButton("دخول") { _, _ ->

                val url = etUrl.text.toString()
                val user = etUser.text.toString()
                val pass = etPass.text.toString()


                if(url.isEmpty() || user.isEmpty() || pass.isEmpty()) {

                    Toast.makeText(
                        requireContext(),
                        "يرجى ملء جميع الحقول",
                        Toast.LENGTH_SHORT
                    ).show()

                } else {

                    Toast.makeText(
                        requireContext(),
                        "جاري تسجيل الدخول...",
                        Toast.LENGTH_SHORT
                    ).show()


                    // هنا سنربط Xtream API لاحقاً
                    // حالياً فقط اختبار الواجهة

                }

            }
            .setNegativeButton("إلغاء", null)
            .show()
    }
}