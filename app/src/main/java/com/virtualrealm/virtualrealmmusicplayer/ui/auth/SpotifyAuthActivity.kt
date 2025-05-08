// ui/auth/SpotifyAuthActivity.kt
package com.virtualrealm.virtualrealmmusicplayer.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.virtualrealm.virtualrealmmusicplayer.databinding.ActivitySpotifyAuthBinding
import com.virtualrealm.virtualrealmmusicplayer.domain.model.Resource
import com.virtualrealm.virtualrealmmusicplayer.ui.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SpotifyAuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySpotifyAuthBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySpotifyAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        handleIntent(intent)
        observeAuthResult()
    }

    private fun handleIntent(intent: Intent) {
        val uri = intent.data
        if (uri != null) {
            val code = uri.getQueryParameter("code")
            if (code != null) {
                viewModel.exchangeCodeForToken(code)
            } else {
                val error = uri.getQueryParameter("error")
                Toast.makeText(
                    this,
                    "Authentication failed: $error",
                    Toast.LENGTH_LONG
                ).show()
                navigateToMainActivity()
            }
        }
    }

    private fun observeAuthResult() {
        viewModel.authResult.observe(this) { result ->
            when (result) {
                is Resource.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    navigateToMainActivity()
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(
                        this,
                        "Authentication failed: ${result.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    navigateToMainActivity()
                }
            }
        }
    }

    private fun navigateToMainActivity() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}
