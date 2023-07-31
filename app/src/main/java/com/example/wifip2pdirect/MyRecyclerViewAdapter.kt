package com.example.wifip2pdirect

import android.net.wifi.p2p.WifiP2pDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MyRecyclerViewAdapter(
    private val dataSet: MutableList<WifiP2pDevice>,
    private val listener: (WifiP2pDevice) -> Unit
) :
    RecyclerView.Adapter<MyRecyclerViewAdapter.MyViewHolder>() {
    class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val rcDeviceNameTextView: TextView
        val rcDeviceAddressTextView: TextView

        init {
            rcDeviceNameTextView = view.findViewById(R.id.rc_device_name_text_view)
            rcDeviceAddressTextView = view.findViewById(R.id.rc_device_address_text_view)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_device, parent, false)
        return MyViewHolder(view)
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.rcDeviceNameTextView.text = "Device name:  ${dataSet[position].deviceName}"
        holder.rcDeviceAddressTextView.text = "Device address: ${dataSet[position].deviceAddress}"
        holder.itemView.setOnClickListener {
            listener(dataSet[position])
        }
    }
}