package com.dazou.iptvplayer.fragments

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.dazou.iptvplayer.R
import com.dazou.iptvplayer.model.XtreamServer


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


        val btnAddAccount =
            view.findViewById<Button>(R.id.btnAddAccount)


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


        val etUrl =
            dialogView.findViewById<EditText>(R.id.etUrl)

        val etUser =
            dialogView.findViewById<EditText>(R.id.etUser)

        val etPass =
            dialogView.findViewById<EditText>(R.id.etPass)



        AlertDialog.Builder(requireContext())

            .setTitle("إضافة حساب Xtream")

            .setView(dialogView)

            .setPositiveButton("حفظ") { _, _ ->


                val url =
                    etUrl.text.toString().trim()

                val user =
                    etUser.text.toString().trim()

                val pass =
                    etPass.text.toString().trim()



                if(url.isEmpty() || user.isEmpty() || pass.isEmpty()){


                    Toast.makeText(
                        requireContext(),
                        "املأ جميع الحقول",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setPositiveButton
                }



                saveAccount(
                    XtreamServer(
                        url,
                        user,
                        pass
                    )
                )



                Toast.makeText(
                    requireContext(),
                    "تم حفظ الحساب",
                    Toast.LENGTH_SHORT
                ).show()


            }


            .setNegativeButton(
                "إلغاء",
                null
            )

            .show()

    }




    private fun saveAccount(server: XtreamServer){


        val pref =
            requireContext()
                .getSharedPreferences(
                    "iptv_account",
                    Context.MODE_PRIVATE
                )


        pref.edit()
            .putString("url",server.url)
            .putString("username",server.username)
            .putString("password",server.password)
            .apply()

    }


}