package com.dazou.iptvplayer.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.dazou.iptvplayer.App
import com.dazou.iptvplayer.R
import com.dazou.iptvplayer.adapter.AccountsAdapter
import com.dazou.iptvplayer.databinding.FragmentAccountsBinding
import com.dazou.iptvplayer.model.XtreamServer

class AccountsFragment : Fragment() {

    private var _binding: FragmentAccountsBinding? = null
    private val binding get() = _binding!!
    private lateinit var accountManager: com.dazou.iptvplayer.data.AccountManager
    private lateinit var adapter: AccountsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        accountManager = (requireActivity().application as App).container.accountManager
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAccountsBinding.inflate(inflater, container, false)

        binding.rvAccounts.layoutManager = LinearLayoutManager(requireContext())

        adapter = AccountsAdapter(
            onAccountClick = { account, position ->
                showAccountOptionsDialog(account, position)
            }
        )
        binding.rvAccounts.adapter = adapter

        binding.btnAddAccount.setOnClickListener {
            showAddAccountDialog()
        }

        loadAccounts()
        return binding.root
    }

    private fun loadAccounts() {
        val accounts = accountManager.getAccounts()
        adapter.submitList(accounts)

        if (accounts.isEmpty()) {
            binding.tvNoAccounts.visibility = View.VISIBLE
            binding.rvAccounts.visibility = View.GONE
        } else {
            binding.tvNoAccounts.visibility = View.GONE
            binding.rvAccounts.visibility = View.VISIBLE
        }
    }

    private fun showAddAccountDialog() {
        val t = com.dazou.iptvplayer.model.ThemeColors
        val dlgSize = 18f
        val inpSize = 14f
        val pad = 24

        val scrollView = android.widget.ScrollView(requireContext()).apply {
            setPadding(40, 40, 40, 40)
        }
        val d = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(android.graphics.Color.parseColor("#1A1A35"))
        }

        d.addView(TextView(requireContext()).apply {
            text = "⚙️ إضافة حساب Xtream"
            textSize = dlgSize
            setTextColor(android.graphics.Color.parseColor("#FF6B6B"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, 24)
        })

        val labels = arrayOf("رابط السيرفر:", "اسم المستخدم:", "كلمة المرور:")
        val hints = arrayOf("http://example.com:8080", "username", "password")
        val fields = mutableListOf<EditText>()

        for ((index, label) in labels.withIndex()) {
            d.addView(TextView(requireContext()).apply {
                text = label
                textSize = 12f
                setTextColor(android.graphics.Color.parseColor("#AAAAAA"))
                setPadding(0, 8, 0, 4)
            })
            val editText = EditText(requireContext()).apply {
                hint = hints[index]
                setHintTextColor(android.graphics.Color.parseColor("#AAAAAA"))
                setTextColor(android.graphics.Color.WHITE)
                setBackgroundColor(android.graphics.Color.parseColor("#0F0F1A"))
                setPadding(pad, pad, pad, pad)
                textSize = inpSize
                if (index == 0) setText("http://")
            }
            d.addView(editText)
            fields.add(editText)
            if (index < labels.size - 1) {
                d.addView(View(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 10)
                })
            }
        }

        scrollView.addView(d)

        AlertDialog.Builder(requireContext())
            .setView(scrollView)
            .setPositiveButton("حفظ") { _, _ ->
                val url = fields[0].text.toString().trim().trimEnd('/')
                val username = fields[1].text.toString().trim()
                val password = fields[2].text.toString().trim()

                if (url.isEmpty() || username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(requireContext(), "الرجاء ملء جميع الحقول", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val server = XtreamServer(url, username, password)
                accountManager.saveAccount(server)
                val accounts = accountManager.getAccounts()
                accountManager.setActiveAccount(accounts.size - 1)
                loadAccounts()
                Toast.makeText(requireContext(), "✅ تم حفظ الحساب", Toast.LENGTH_SHORT).show()

                // إعادة تحميل القنوات
                val liveFragment = parentFragmentManager.findFragmentByTag("live")
                if (liveFragment is LiveFragment) {
                    liveFragment.refreshData()
                }
            }
            .setNegativeButton("إلغاء", null)
            .setCancelable(false)
            .show()
    }

    private fun showAccountOptionsDialog(account: XtreamServer, position: Int) {
        val items = arrayOf("✅ تعيين كحساب نشط", "🗑️ حذف الحساب")
        AlertDialog.Builder(requireContext())
            .setTitle("${account.username} @ ${account.url}")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> {
                        accountManager.setActiveAccount(position)
                        Toast.makeText(requireContext(), "✅ تم تعيين الحساب النشط", Toast.LENGTH_SHORT).show()
                        // إعادة تحميل القنوات
                        val liveFragment = parentFragmentManager.findFragmentByTag("live")
                        if (liveFragment is LiveFragment) {
                            liveFragment.refreshData()
                        }
                    }
                    1 -> {
                        AlertDialog.Builder(requireContext())
                            .setTitle("تأكيد الحذف")
                            .setMessage("هل أنت متأكد من حذف هذا الحساب؟")
                            .setPositiveButton("نعم") { _, _ ->
                                val accounts = accountManager.getAccounts().toMutableList()
                                accounts.removeAt(position)
                                // إعادة حفظ القائمة بدون الحساب المحذوف
                                val prefs = requireContext().getSharedPreferences("iptv_accounts", 0)
                                val json = org.json.JSONArray()
                                accounts.forEach {
                                    val obj = org.json.JSONObject()
                                    obj.put("url", it.url)
                                    obj.put("username", it.username)
                                    obj.put("password", it.password)
                                    json.put(obj)
                                }
                                prefs.edit().putString("accounts", json.toString()).apply()
                                loadAccounts()
                                Toast.makeText(requireContext(), "🗑️ تم حذف الحساب", Toast.LENGTH_SHORT).show()
                            }
                            .setNegativeButton("إلغاء", null)
                            .show()
                    }
                }
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}