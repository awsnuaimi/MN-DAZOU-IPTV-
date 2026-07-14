package com.dazou.iptvplayer.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import androidx.fragment.app.Fragment
import android.widget.Toast
import com.dazou.iptvplayer.R


class AccountsFragment : Fragment() {


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {


        val view = inflater.inflate(
            R.layout.fragment_accounts,
            container,
            false
        )


        val btnAddAccount = view.findViewById<Button>(
            R.id.btnAddAccount
        )


        btnAddAccount.setOnClickListener {

            showLoginDialog()

        }


        return view
    }



    private fun showLoginDialog() {


        val dialogView = layoutInflater.inflate(
            R.layout.fragment_login,
            null
        )


        val radioXtream = dialogView.findViewById<RadioButton>(
            R.id.radioXtream
        )


        val radioM3u = dialogView.findViewById<RadioButton>(
            R.id.radioM3u
        )


        val layoutXtream = dialogView.findViewById<View>(
            R.id.layoutXtream
        )


        val layoutM3u = dialogView.findViewById<View>(
            R.id.layoutM3u
        )


        val etUrl = dialogView.findViewById<EditText>(
            R.id.etUrl
        )


        val etUser = dialogView.findViewById<EditText>(
            R.id.etUser
        )


        val etPass = dialogView.findViewById<EditText>(
            R.id.etPass
        )


        val etM3uUrl = dialogView.findViewById<EditText>(
            R.id.etM3uUrl
        )



        radioXtream.setOnClickListener {

            layoutXtream.visibility = View.VISIBLE
            layoutM3u.visibility = View.GONE

        }



        radioM3u.setOnClickListener {

            layoutXtream.visibility = View.GONE
            layoutM3u.visibility = View.VISIBLE

        }



        AlertDialog.Builder(requireContext())

            .setTitle("إضافة حساب IPTV")

            .setView(dialogView)

            .setPositiveButton("حفظ") { _, _ ->


                if (radioXtream.isChecked) {


                    val server = etUrl.text.toString()
                    val username = etUser.text.toString()
                    val password = etPass.text.toString()


                    Toast.makeText(
                        requireContext(),
                        "Xtream تم حفظه: $username",
                        Toast.LENGTH_SHORT
                    ).show()


                    // هنا سنربط تسجيل الدخول Xtream لاحقاً


                } else {


                    val playlist = etM3uUrl.text.toString()


                    Toast.makeText(
                        requireContext(),
                        "M3U تم حفظه",
                        Toast.LENGTH_SHORT
                    ).show()


                    // هنا سنربط تحميل قائمة M3U لاحقاً

                }


            }


            .setNegativeButton(
                "إلغاء",
                null
            )


            .show()


    }

}