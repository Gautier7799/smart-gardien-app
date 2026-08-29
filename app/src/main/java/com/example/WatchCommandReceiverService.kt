package com.example

import android.content.Intent
import android.os.Build
import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

class WatchCommandReceiverService : WearableListenerService() {

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        
        Log.d("WatchReceiver", "Received message from watch: ${messageEvent.path}")
        
        when (messageEvent.path) {
            "/start_guard" -> {
                val intent = Intent(this, GuardService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
            }
            "/stop_guard" -> {
                val intent = Intent(this, GuardService::class.java)
                intent.action = "STOP_GUARD"
                startService(intent)
            }
        }
    }
}
