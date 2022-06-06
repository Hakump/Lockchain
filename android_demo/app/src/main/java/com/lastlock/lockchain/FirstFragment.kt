package com.lastlock.lockchain

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.icu.util.Calendar
import android.icu.util.GregorianCalendar
import android.os.Bundle
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.lastlock.lockchain.databinding.FragmentFirstBinding
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.math.BigInteger
import java.net.ConnectException
import java.net.Socket
import java.security.*
import javax.security.auth.x500.X500Principal


private const val ANDROID_KEYSTORE = "AndroidKeyStore"
private const val KEY_ALIAS = "MalakappsKey"
private const val REQUEST_CODE_FOR_CREDENTIALS = 1

/**
 * A simple [Fragment] subclass as the default destination in the navigation.

 */
private const val TAG = "FirstFragment"
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null
    private lateinit var privateKey: String
    private lateinit var publicKey: String
    private lateinit var keyguardManager: KeyguardManager

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        keyguardManager = activity?.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

        if (!checkKeyExists()) {
            Log.d(TAG, "Generating keys")
            val success = generateKeys()
            if(!success) {
                throw Exception("Failed to generate keys")
            }
        }
        Log.d(TAG, "Reading keys")
        val sharedPref = activity?.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE) ?: throw Exception("Cannot get shared preferences")
        privateKey = sharedPref.getString("privateKey", "none").toString()
        publicKey = sharedPref.getString("publicKey", "none").toString()

        Log.d(TAG, "Public Key: $publicKey\nPrivate Key: $privateKey")

        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonUnlock.setOnClickListener { view ->
            Log.d(TAG, "Unlock Button Clicked")
            // Authenticate with biometrics before unlocking
            showAuthenticationScreen() // Will hit onActivityResult
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    // Hardcoded random RSA keys generated for testing purposes
    private fun generateKeys() : Boolean {
        val sharedPref = activity?.getSharedPreferences(
            getString(R.string.preference_file_key), Context.MODE_PRIVATE) ?: return false
        with(sharedPref.edit()) {
            putString("privateKey", "MIICWwIBAAKBgQDvuDIxBmdbSrGwH0qC4hrOcj8vox2aFt5LUu6mrb0bb8MPQAZm" +
                    "m0E4/YYJ7CL7xrgO44Mev43CgITeCaQ1gVM5uuaLmVhCOZh/apA2p184dfICgYgL" +
                    "oRWb7iyfwXeAI3FudW4nWdQ+MyF0Xtm/23d/ouvMNVsgQpBf0xyEPwjRrQIDAQAB" +
                    "AoGAImEhWaSYeCBLQX4LWqAAxjDeXP+gxDOZZ+YZ8e+i2md56xr8kASeIwVBDvU9" +
                    "wywpw4Dfki5Yr1dtNsbdNQs6GxteZqlXOzwf/n3jc5RYQu/cGFy+twx6WxvJd8Nc" +
                    "wVeHI0LGJHI25IxQyWXNQGcPZZU4pqxH8zBsqnCiKACE5SUCQQD9pQ+RwZZ1jt6Y" +
                    "24w9Yoj6iaaIXfI9WsfTxn3KOTZt29u9bh1R1lq0w6bv8P4hY7Ixe/MLu4GXadgw" +
                    "pJ8lxoIrAkEA8fIIlEwpiUgC4BafzBJUx52g8Qu1V9D0yJc51HmQRcN5HlF/tlp5" +
                    "NZuAgAYH6HCpt/ll0OVVZBfhSl4qeuEHhwJAU/kOk4HUzyyyk0a0QipaER/7S1BW" +
                    "/2sLlxgy0v1oAKz2LdvDxzFBLHFN1kghymoYN3vCtlKoj7lSAqCts7LpqwJAPHLS" +
                    "PesBfbEmNsKwThZOZEk3ysG4Hyav4eYWpsVwjF2YR0DpfbABFKmzY0kedK6hWLvQ" +
                    "+m4x9X4Z5bC9QFI0xQJAfRGI8mcydofwuBEm+1vr0TfXqiJeIkE1pATQQX0w4mVP" +
                    "BIYT1+8/XYmE/581GTTW/Bq/MCX9mgWMT46Q73Sb0A==")
            putString("publicKey", "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDvuDIxBmdbSrGwH0qC4hrOcj8v" +
                    "ox2aFt5LUu6mrb0bb8MPQAZmm0E4/YYJ7CL7xrgO44Mev43CgITeCaQ1gVM5uuaL" +
                    "mVhCOZh/apA2p184dfICgYgLoRWb7iyfwXeAI3FudW4nWdQ+MyF0Xtm/23d/ouvM" +
                    "NVsgQpBf0xyEPwjRrQIDAQAB")
            apply()
        }
        return true
    }

    private fun checkKeyExists(): Boolean {
        val sharedPref = activity?.getSharedPreferences(
            getString(R.string.preference_file_key), Context.MODE_PRIVATE) ?: return false
        if (sharedPref.getString("privateKey", null) == null) {
            return false
        }
        if (sharedPref.getString("publicKey", null) == null) {
            return false
        }
        return true
    }

    private fun showAuthenticationScreen(): Boolean {
        //This will open a screen to enter the user credentials (fingerprint, pin, pattern). We can display a custom title and description
        val intent: Intent? = keyguardManager.createConfirmDeviceCredentialIntent("Authenticate with lock",
            "LastLock needs to confirm your identity. Please enter your pin/pattern or scan your fingerprint")
        if (intent != null) {
            startActivityForResult(intent, REQUEST_CODE_FOR_CREDENTIALS)
            return true
        }
        return false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_FOR_CREDENTIALS) {
            if (resultCode == Activity.RESULT_OK) {
                Log.d(TAG, "Authentication successful")
                var thread = Thread().run {
                    Log.d(TAG, "Hitting the server")
                    GlobalScope.launch {
                        try {
                            sendKeyToServer("192.168.1.17")
                        } catch (e: ConnectException) {
                            Log.d(TAG, "Connection failure $e.toString()")
                        } catch (e: Exception) {
                            Log.d(TAG, "Unknown error $e.toString()")
                        }
                    }
                }
            } else {
                Log.d(TAG, "Authentication failed")
                this.view?.let {
                    Snackbar.make(it, "Failed to authenticate", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show()
                }
            }
        }
    }

    private suspend fun connectToServer(ip: String) {
        val client = Socket(ip, 6969)
        client.outputStream.write("What up boof".toByteArray())
        client.close()
    }

    private suspend fun sendKeyToServer(ip: String) {
        val client = Socket(ip, 6969)
        client.outputStream.write(privateKey.toByteArray())
        client.close()
    }
}