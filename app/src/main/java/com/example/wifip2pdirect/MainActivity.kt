package com.example.wifip2pdirect

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.wifip2pdirect.databinding.ActivityMainBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    companion object {
        const val ACCESS_FINE_LOCATION_PERMISSION_CODE = 100
        const val NEARBY_WIFI_DEVICES_PERMISSION_CODE = 101
    }

    private val intentFilter = IntentFilter()

    private lateinit var binding: ActivityMainBinding
    fun getBinding(): ActivityMainBinding {
        return binding
    }

    lateinit var myServer: MyServer
    lateinit var myClient: MyClient
    var isHost: Boolean = false

    private lateinit var channel: WifiP2pManager.Channel
    private lateinit var manager: WifiP2pManager
    private lateinit var receiver: BroadcastReceiver
    private val peers = mutableListOf<WifiP2pDevice>()

    private var isWifiP2pEnabled: Boolean = false
    fun setIsWifiP2pEnabled(isWifiP2pEnabled: Boolean) {
        this.isWifiP2pEnabled = isWifiP2pEnabled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initP2P()
        handlerListener()
    }

    private fun initP2P() {
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)

        manager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        channel = manager.initialize(this, mainLooper, null)
    }


    private fun handlerListener() {
        binding.openWifiButton.setOnClickListener {
            if (isWifiP2pEnabled) {
                Toast.makeText(this, "Wifi is opened", Toast.LENGTH_LONG).show()
            } else {
                startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
            }
        }

        binding.discoverButton.setOnClickListener {
            if (checkPermission(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    ACCESS_FINE_LOCATION_PERMISSION_CODE
                ) && checkPermission(
                    Manifest.permission.NEARBY_WIFI_DEVICES,
                    NEARBY_WIFI_DEVICES_PERMISSION_CODE
                )
            ) {
                manager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        binding.connectStatusTextView.text = "Connect Status: Discover Started"
                    }

                    override fun onFailure(reasonCode: Int) {
                        binding.connectStatusTextView.text = "Connect Status: Discover Not Started"
                    }
                })
            }
        }

        binding.sendImageButton.setOnClickListener {
            var executor: ExecutorService = Executors.newSingleThreadExecutor()
            var msg: String = binding.inputMessageEditText.text.toString()
            executor.execute {
                if (msg == "") {
                    Toast.makeText(this, "Please input messsage!", Toast.LENGTH_LONG).show()
                } else if (isHost) {
                    binding.inputMessageEditText.setText("")
                    myServer.write(msg.toByteArray())
                } else if (!isHost) {
                    myClient.write(msg.toByteArray())
                    binding.inputMessageEditText.setText("")
                }
            }
        }
    }

    fun getPeerListListener(): WifiP2pManager.PeerListListener {
        return peerListListener
    }

    private val peerListListener = WifiP2pManager.PeerListListener { peerList ->
        val refreshedPeers = peerList.deviceList
        if (refreshedPeers != peers) {
            peers.clear()
            peers.addAll(refreshedPeers)

            binding.devicesRecyclerView.layoutManager = LinearLayoutManager(this)
            binding.devicesRecyclerView.adapter = MyRecyclerViewAdapter(peers, listener = {
                connect(it)
            })
        }

        if (peers.isEmpty()) {
            return@PeerListListener
        }
    }

    private fun connect(it: WifiP2pDevice) {
        val device = it
        val config = WifiP2pConfig().apply {
            deviceAddress = device.deviceAddress
            wps.setup = WpsInfo.PBC
        }
        if (checkPermission(
                Manifest.permission.ACCESS_FINE_LOCATION,
                ACCESS_FINE_LOCATION_PERMISSION_CODE
            ) && checkPermission(
                Manifest.permission.NEARBY_WIFI_DEVICES,
                NEARBY_WIFI_DEVICES_PERMISSION_CODE
            )
        ) {
            manager.connect(channel, config, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    binding.connectStatusTextView.text =
                        "Connect Status: Connect to ${device.deviceAddress}"
                }

                override fun onFailure(reason: Int) {
                    binding.connectStatusTextView.text = "Connect Status: Connect Failed"
                }
            })
        }
    }


    fun getConnectionListener(): WifiP2pManager.ConnectionInfoListener {
        return connectionListener
    }

    private val connectionListener = WifiP2pManager.ConnectionInfoListener { info ->
        val groupOwnerAddress: String = info.groupOwnerAddress.hostAddress
        if (info.groupFormed && info.isGroupOwner) {
            binding.connectStatusTextView.text = "Connect Status: Host"
            isHost = true
            myServer = MyServer(this)
            myServer.start()
        } else if (info.groupFormed) {
            binding.connectStatusTextView.text = "Connect Status: Client"
            isHost = false
            myClient = MyClient(groupOwnerAddress, this)
            myClient.start()
        }
    }

    //permission
    fun checkPermission(permission: String, requestCode: Int): Boolean {
        if (ActivityCompat.checkSelfPermission(
                this@MainActivity,
                permission,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this@MainActivity, arrayOf(permission), requestCode)
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            ACCESS_FINE_LOCATION_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(
                        this@MainActivity,
                        "ACCESS_FINE_LOCATION_PERMISSION_CODE Granted",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "ACCESS_FINE_LOCATION_PERMISSION_CODE Denied",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            NEARBY_WIFI_DEVICES_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(
                        this@MainActivity,
                        "NEARBY_WIFI_DEVICES_PERMISSION_CODE Granted",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "NEARBY_WIFI_DEVICES_PERMISSION_CODE Denied",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

        }
    }

    /** register the BroadcastReceiver with the intent values to be matched  */
    public override fun onResume() {
        super.onResume()
        receiver = WiFiDirectBroadcastReceiver(manager, channel, this)
        registerReceiver(receiver, intentFilter)
    }

    public override fun onPause() {
        super.onPause()
        unregisterReceiver(receiver)
    }
}