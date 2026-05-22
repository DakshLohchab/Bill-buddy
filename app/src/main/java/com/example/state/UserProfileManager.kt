package com.example.state

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object UserProfileManager {
    private const val PREFS_NAME = "BillBuddyUserPrefs"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_LOGGED_IN_EMAIL = "logged_in_email"
    private const val KEY_IS_NEW_USER = "is_new_user"

    // Profile Details
    private const val KEY_PROFILE_NAME = "profile_name"
    private const val KEY_PROFILE_PHONE = "profile_phone"
    private const val KEY_PROFILE_BIO = "profile_bio"
    private const val KEY_PROFILE_UPI = "profile_upi"

    // Local Registration Key Prefixes (Pre-fixed with "user_pwd_")
    private const val PREFIX_USER_PASSWORD = "user_pwd_"

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _currentUserEmail = MutableStateFlow<String?>(null)
    val currentUserEmail: StateFlow<String?> = _currentUserEmail.asStateFlow()

    private val _isNewUser = MutableStateFlow(false)
    val isNewUser: StateFlow<Boolean> = _isNewUser.asStateFlow()

    // Loaded profile fields
    private val _profileName = MutableStateFlow("")
    val profileName: StateFlow<String> = _profileName.asStateFlow()

    private val _profilePhone = MutableStateFlow("")
    val profilePhone: StateFlow<String> = _profilePhone.asStateFlow()

    private val _profileBio = MutableStateFlow("")
    val profileBio: StateFlow<String> = _profileBio.asStateFlow()

    private val _profileUpi = MutableStateFlow("")
    val profileUpi: StateFlow<String> = _profileUpi.asStateFlow()

    private var initialized = false

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun initialize(context: Context) {
        if (initialized) return
        val prefs = getPrefs(context)
        val loggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        val email = prefs.getString(KEY_LOGGED_IN_EMAIL, null)

        _isLoggedIn.value = loggedIn
        _currentUserEmail.value = email

        if (loggedIn && email != null) {
            _isNewUser.value = prefs.getBoolean(makeUserKey(email, KEY_IS_NEW_USER), false)
            _profileName.value = prefs.getString(makeUserKey(email, KEY_PROFILE_NAME), "") ?: ""
            _profilePhone.value = prefs.getString(makeUserKey(email, KEY_PROFILE_PHONE), "") ?: ""
            _profileBio.value = prefs.getString(makeUserKey(email, KEY_PROFILE_BIO), "") ?: ""
            _profileUpi.value = prefs.getString(makeUserKey(email, KEY_PROFILE_UPI), "") ?: ""
        }
        initialized = true
    }

    private fun makeUserKey(email: String, originalKey: String): String {
        return "${email.lowercase().replace(".", "_")}_$originalKey"
    }

    /**
     * Local login authentication
     */
    fun login(context: Context, email: String, password: String): Boolean {
        initialize(context)
        val trimmedEmail = email.trim().lowercase()
        val trimmedPassword = password.trim()

        if (trimmedEmail.isEmpty() || trimmedPassword.isEmpty()) return false

        val prefs = getPrefs(context)
        val registeredPwd = prefs.getString(PREFIX_USER_PASSWORD + trimmedEmail, null)

        if (registeredPwd != null && registeredPwd == trimmedPassword) {
            prefs.edit().apply {
                putBoolean(KEY_IS_LOGGED_IN, true)
                putString(KEY_LOGGED_IN_EMAIL, trimmedEmail)
                apply()
            }

            _isLoggedIn.value = true
            _currentUserEmail.value = trimmedEmail
            
            // Reload user specific data
            _isNewUser.value = prefs.getBoolean(makeUserKey(trimmedEmail, KEY_IS_NEW_USER), false)
            _profileName.value = prefs.getString(makeUserKey(trimmedEmail, KEY_PROFILE_NAME), "") ?: ""
            _profilePhone.value = prefs.getString(makeUserKey(trimmedEmail, KEY_PROFILE_PHONE), "") ?: ""
            _profileBio.value = prefs.getString(makeUserKey(trimmedEmail, KEY_PROFILE_BIO), "") ?: ""
            _profileUpi.value = prefs.getString(makeUserKey(trimmedEmail, KEY_PROFILE_UPI), "") ?: ""

            return true
        }
        return false
    }

    /**
     * Local sign up / registration
     */
    fun register(context: Context, email: String, name: String, password: String): Boolean {
        initialize(context)
        val trimmedEmail = email.trim().lowercase()
        val trimmedName = name.trim()
        val trimmedPassword = password.trim()

        if (trimmedEmail.isEmpty() || trimmedPassword.isEmpty() || trimmedName.isEmpty()) return false

        val prefs = getPrefs(context)
        if (prefs.contains(PREFIX_USER_PASSWORD + trimmedEmail)) {
            // Already registered
            return false
        }

        prefs.edit().apply {
            // Store password local representation
            putString(PREFIX_USER_PASSWORD + trimmedEmail, trimmedPassword)
            // Mark as new user for onboarding
            putBoolean(makeUserKey(trimmedEmail, KEY_IS_NEW_USER), true)
            // Set initial name
            putString(makeUserKey(trimmedEmail, KEY_PROFILE_NAME), trimmedName)
            apply()
        }

        // Auto-login after registration
        return login(context, trimmedEmail, trimmedPassword)
    }

    /**
     * Update active profile details
     */
    fun updateProfile(context: Context, name: String, phone: String, bio: String, upiId: String) {
        val email = _currentUserEmail.value ?: return
        val prefs = getPrefs(context)

        prefs.edit().apply {
            putString(makeUserKey(email, KEY_PROFILE_NAME), name.trim())
            putString(makeUserKey(email, KEY_PROFILE_PHONE), phone.trim())
            putString(makeUserKey(email, KEY_PROFILE_BIO), bio.trim())
            putString(makeUserKey(email, KEY_PROFILE_UPI), upiId.trim())
            apply()
        }

        _profileName.value = name.trim()
        _profilePhone.value = phone.trim()
        _profileBio.value = bio.trim()
        _profileUpi.value = upiId.trim()
    }

    /**
     * Complete or skip the onboarding information flow
     */
    fun completeOnboarding(context: Context) {
        val email = _currentUserEmail.value ?: return
        val prefs = getPrefs(context)

        prefs.edit().apply {
            putBoolean(makeUserKey(email, KEY_IS_NEW_USER), false)
            apply()
        }
        _isNewUser.value = false
    }

    /**
     * Log out active user session
     */
    fun logout(context: Context) {
        val prefs = getPrefs(context)
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, false)
            putString(KEY_LOGGED_IN_EMAIL, null)
            apply()
        }

        _isLoggedIn.value = false
        _currentUserEmail.value = null
        _isNewUser.value = false
        _profileName.value = ""
        _profilePhone.value = ""
        _profileBio.value = ""
        _profileUpi.value = ""
    }
}
