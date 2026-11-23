package com.calendarioazulpixel

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging

object NotificationHelper {

    fun askForNotificationPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            } else {
                updateFCMToken()
            }
        } else {
            // Android 12 y menor no requiere solicitar este permiso en runtime
            updateFCMToken()
        }
    }

    private fun updateFCMToken() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    val db = FirebaseFirestore.getInstance()
                    val tokenData = hashMapOf(
                        "fcmToken" to token,
                        "lastUpdated" to System.currentTimeMillis()
                    )
                    db.collection("users").document(currentUser.uid)
                        .set(tokenData, SetOptions.merge())
                }
            }
        }
    }
}