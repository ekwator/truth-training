package com.truth.training.client.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.truth.training.client.R
import android.view.View
import android.widget.TextView
import android.widget.ProgressBar

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        val email = findViewById<TextInputEditText>(R.id.email)
        val password = findViewById<TextInputEditText>(R.id.password)
        val signIn = findViewById<MaterialButton>(R.id.signIn)

        val vm by viewModels<LoginViewModel>()

        signIn.setOnClickListener {
            vm.login(email.text?.toString().orEmpty(), password.text?.toString().orEmpty())
        }

        lifecycleScope.launch {
            vm.success.collectLatest { ok ->
                if (ok) {
                    startActivity(Intent(this@LoginActivity, DashboardActivity::class.java))
                    finish()
                }
            }
        }

        val progress = findViewById<ProgressBar>(R.id.progress)
        val errorView = findViewById<TextView>(R.id.errorText)
        lifecycleScope.launch {
            vm.loading.collectLatest { loading ->
                progress.visibility = if (loading) View.VISIBLE else View.GONE
            }
        }
        lifecycleScope.launch {
            vm.error.collectLatest { err ->
                errorView.visibility = if (err.isNullOrEmpty()) View.GONE else View.VISIBLE
                errorView.text = err.orEmpty()
            }
        }
    }
}


