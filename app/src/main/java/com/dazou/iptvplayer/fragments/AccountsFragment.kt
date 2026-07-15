package com.dazou.iptvplayer.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.dazou.iptvplayer.R
import com.dazou.iptvplayer.data.AccountManager
import com.dazou.iptvplayer.model.XtreamServer


class AccountsFragment : Fragment() {


    private lateinit var accountManager: AccountManager

    private lateinit var listAccounts: LinearLayout



    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        accountManager =
            AccountManager(requireContext())

    }





    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        try {

            val view =
                inflater.inflate(
                    R.layout.fragment_accounts,
                    container,
                    false
                )



            listAccounts =
                view.findViewById(
                    R.id.listAccounts
                )



            val btnAdd =
                view.findViewById<Button>(
                    R.id.btnAddAccount
                )



            btnAdd.setOnClickListener {


                showAddDialog()


            }



            loadAccounts()



            return view

        } catch (e: Exception) {

            Toast.makeText(
                requireContext(),
                "خطأ: ${e.message}",
                Toast.LENGTH_LONG
            ).show()

            return View(requireContext())

        }

    }







    private fun loadAccounts(){


        listAccounts.removeAllViews()



        val accounts =
            accountManager.getAccounts()



        accounts.forEachIndexed { index, account ->



            val button =
                Button(requireContext())



            button.text =
                "${account.username}\n${account.url}"



            button.setOnClickListener {



                accountManager.setActiveAccount(
                    index
                )



                Toast.makeText(
                    requireContext(),
                    "تم اختيار الحساب",
                    Toast.LENGTH_SHORT
                ).show()


            }



            listAccounts.addView(
                button
            )


        }


    }







    private fun showAddDialog(){


        val dialogView =
            layoutInflater.inflate(
                R.layout.fragment_login,
                null
            )



        val etUrl =
            dialogView.findViewById<EditText>(
                R.id.etUrl
            )


        val etUser =
            dialogView.findViewById<EditText>(
                R.id.etUser
            )


        val etPass =
            dialogView.findViewById<EditText>(
                R.id.etPass
            )





        AlertDialog.Builder(
            requireContext()
        )

            .setTitle(
                "إضافة حساب Xtream"
            )


            .setView(
                dialogView
            )


            .setPositiveButton(
                "حفظ"
            ){ _, _ ->



                val url =
                    etUrl.text.toString()
                        .trim()



                val user =
                    etUser.text.toString()
                        .trim()



                val pass =
                    etPass.text.toString()
                        .trim()




                if(
                    url.isNotEmpty() &&
                    user.isNotEmpty() &&
                    pass.isNotEmpty()
                ){



                    accountManager.saveAccount(

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



                    loadAccounts()


                }else{


                    Toast.makeText(
                        requireContext(),
                        "املأ جميع الحقول",
                        Toast.LENGTH_SHORT
                    ).show()


                }



            }



            .setNegativeButton(
                "إلغاء",
                null
            )

            .show()



    }



}