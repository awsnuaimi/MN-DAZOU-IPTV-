package com.dazou.iptvplayer.fragments

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.dazou.iptvplayer.App
import com.dazou.iptvplayer.R
import com.dazou.iptvplayer.adapter.AccountsAdapter
import com.dazou.iptvplayer.databinding.FragmentAccountsBinding
import com.dazou.iptvplayer.model.XtreamServer
import com.dazou.iptvplayer.viewmodel.LiveViewModel
import com.dazou.iptvplayer.viewmodel.ViewModelFactory

class AccountsFragment : Fragment() {

    private var _binding: FragmentAccountsBinding? = null
    private val binding get() = _binding!!
    private lateinit var accountManager: com.dazou.iptvplayer.data.AccountManager
    private lateinit var adapter: AccountsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        accountManager = (requireActivity().application as App).container.accountManager
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountsBinding.inflate(inflater, container, false)

        setupRecyclerView()
        setupListeners()
        loadAccounts()

        return binding.root
    }

    private fun setupRecyclerView() {
        binding.rvAccounts.layoutManager = LinearLayoutManager(requireContext())
        adapter = AccountsAdapter(
            onAccountClick = { account, position ->
                showAccountOptionsDialog(account, position)
            }
        )
        binding.rvAccounts.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnAddAccount.setOnClickListener {
            showAddAccountDialog()
        }
    }

    private fun loadAccounts() {
        val accounts = accountManager.getAccounts()
        adapter.submitList(accounts)

        if (accounts.isEmpty()) {
            binding.tvNoAccounts.visibility = View.VISIBLE
            binding.rvAccounts.visibility = View.GONE
            binding.btnAddAccount.requestFocus()
        } else {
            binding.tvNoAccounts.visibility = View.GONE
            binding.rvAccounts.visibility = View.VISIBLE
        }
    }

    private fun showAddAccountDialog() {
        val scrollView = ScrollView(requireContext()).apply {
            setPadding(40, 40, 40, 40)
        }
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#1A1A35"))
        }

        layout.addView(TextView(requireContext()).apply {
            text = getString(R.string.accounts_add_title)
            textSize = 18f
            setTextColor(Color.parseColor("#FF6B6B"))
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 24)
        })

        val etUrl = createEditText("http://example.com:8080", "http://")
        val etUsername = createEditText("username", "")
        val etPassword = createEditText("password", "")

        layout.addView(createLabel(getString(R.string.accounts_server_url_label)))
        layout.addView(etUrl)
        layout.addView(createSpacer(12))
        layout.addView(createLabel(getString(R.string.accounts_username_label)))
        layout.addView(etUsername)
        layout.addView(createSpacer(12))
        layout.addView(createLabel(getString(R.string.accounts_password_label)))
        layout.addView(etPassword)

        scrollView.addView(layout)

        AlertDialog.Builder(requireContext())
            .setView(scrollView)
            .setPositiveButton(getString(R.string.common_save)) { _, _ ->
                val url = etUrl.text.toString().trim().trimEnd('/')
                val username = etUsername.text.toString().trim()
                val password = etPassword.text.toString().trim()

                if (url.isEmpty() || username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(requireContext(), getString(R.string.common_fill_all_fields), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                accountManager.saveAccount(XtreamServer(url, username, password))
                val accounts = accountManager.getAccounts()
                accountManager.setActiveAccount(accounts.size - 1)
                loadAccounts()
                refreshLiveChannels()
                Toast.makeText(requireContext(), getString(R.string.accounts_saved), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(getString(R.string.common_cancel), null)
            .setCancelable(false)
            .show()
    }

    private fun showAccountOptionsDialog(account: XtreamServer, position: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle("${account.username} @ ${account.url}")
            .setItems(arrayOf(getString(R.string.accounts_set_active), getString(R.string.accounts_delete))) { _, which ->
                when (which) {
                    0 -> setActiveAccount(position)
                    1 -> deleteAccount(position)
                }
            }
            .show()
    }

    private fun setActiveAccount(position: Int) {
        accountManager.setActiveAccount(position)
        refreshLiveChannels()
        Toast.makeText(requireContext(), getString(R.string.accounts_set_active_success), Toast.LENGTH_SHORT).show()
    }

    private fun deleteAccount(position: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.accounts_delete_confirm_title))
            .setMessage(getString(R.string.accounts_delete_confirm_message))
            .setPositiveButton(getString(R.string.common_yes)) { _, _ ->
                accountManager.deleteAccount(position)
                loadAccounts()
                refreshLiveChannels()
                Toast.makeText(requireContext(), getString(R.string.accounts_deleted), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(getString(R.string.common_cancel), null)
            .show()
    }

    private fun refreshLiveChannels() {
        try {
            val app = requireActivity().application as App
            val repository = app.container.currentRepository
            val viewModel = ViewModelProvider(
                requireActivity(),
                ViewModelFactory(repository)
            ).get(LiveViewModel::class.java)
            viewModel.loadCategories()
        } catch (e: Exception) {
            // تجاهل إذا لم يكن LiveFragment نشطاً
        }
    }

    private fun createLabel(text: String) = TextView(requireContext()).apply {
        this.text = text
        textSize = 12f
        setTextColor(Color.parseColor("#AAAAAA"))
        setPadding(0, 8, 0, 4)
    }

    private fun createEditText(hint: String, defaultText: String) = EditText(requireContext()).apply {
        this.hint = hint
        setHintTextColor(Color.parseColor("#AAAAAA"))
        setTextColor(Color.WHITE)
        setBackgroundColor(Color.parseColor("#0F0F1A"))
        setPadding(24, 24, 24, 24)
        textSize = 14f
        setText(defaultText)
    }

    private fun createSpacer(height: Int) = View(requireContext()).apply {
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            height
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}