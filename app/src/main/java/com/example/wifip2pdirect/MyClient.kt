package com.example.wifip2pdirect

import android.os.Handler
import android.os.Looper
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MyClient(var groupOwnerAddress: String, var activity: MainActivity) : Thread() {
    private lateinit var hostAdd: String
    private lateinit var inputStream: InputStream
    private lateinit var outputStream: OutputStream
    private lateinit var socket: Socket

    init {
        hostAdd = groupOwnerAddress
        socket = Socket()
    }

    fun write(bytes: ByteArray) {
        try {
            outputStream.write(bytes)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun run() {
        super.run()
        try {
            socket.connect(InetSocketAddress(hostAdd, 8888), 500)
            inputStream = socket.getInputStream()
            outputStream = socket.getOutputStream()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        var executor: ExecutorService = Executors.newSingleThreadExecutor()
        var handler: Handler = Handler(Looper.getMainLooper())

        executor.execute {
            val buffer = ByteArray(1024)
            var bytes: Int

            while (socket != null) {
                try {
                    bytes = inputStream.read(buffer)
                    if (bytes > 0) {
                        var finalBytes: Int = bytes
                        handler.post {
                            var tempMsg: String = String(buffer, 0, finalBytes)
                            activity.getBinding().messageTextView.text = tempMsg
                        }
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }
}