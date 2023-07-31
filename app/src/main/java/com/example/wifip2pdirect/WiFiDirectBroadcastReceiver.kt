package com.example.wifip2pdirect

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pManager

class WiFiDirectBroadcastReceiver(
    private val manager: WifiP2pManager,
    private val channel: WifiP2pManager.Channel,
    private val activity: MainActivity
) : BroadcastReceiver() {


    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                activity.setIsWifiP2pEnabled(state == WifiP2pManager.WIFI_P2P_STATE_ENABLED)
            }

            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                if (activity.checkPermission(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        MainActivity.ACCESS_FINE_LOCATION_PERMISSION_CODE
                    ) && activity.checkPermission(
                        Manifest.permission.NEARBY_WIFI_DEVICES,
                        MainActivity.NEARBY_WIFI_DEVICES_PERMISSION_CODE
                    )
                ) {
                    manager?.requestPeers(channel, activity.getPeerListListener())
                }
            }

            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                manager?.let { manager ->
                    val networkInfo: NetworkInfo? = intent
                        .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO) as NetworkInfo?
                    if (networkInfo?.isConnected == true) {
                        manager.requestConnectionInfo(channel, activity.getConnectionListener())
                    }
                }
            }

            WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {

            }
        }
    }
}
