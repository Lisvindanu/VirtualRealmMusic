// ui/auth/LoginFragment.kt
package com.virtualrealm.virtualrealmmusicplayer.ui.auth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.virtualrealm.virtualrealmmusicplayer.R
import com.virtualrealm.virtualrealmmusicplayer.databinding.FragmentLoginBinding
import com.virtualrealm.virtualrealmmusicplayer.domain.model.Resource
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()

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

        setupListeners()
        observeAuthState()
    }

    private fun setupListeners() {
        binding.btnLoginSpotify.setOnClickListener {
            val authUrl = viewModel.getSpotifyAuthUrl()
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(authUrl))
            startActivity(intent)
        }

        binding.btnSkipLogin.setOnClickListener {
            // User can continue without Spotify login
            findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
        }
    }

    private fun observeAuthState() {
        viewModel.authState.observe(viewLifecycleOwner) { state ->
            if (state.isAuthenticated) {
                findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
            }
        }

        viewModel.authResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Resource.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnLoginSpotify.isEnabled = false
                }
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnLoginSpotify.isEnabled = true
                    // Navigation will happen through authState observer
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnLoginSpotify.isEnabled = true
                    Toast.makeText(
                        requireContext(),
                        "Authentication failed: ${result.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
