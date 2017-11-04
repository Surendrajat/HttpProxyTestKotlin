package xpc.sonu.httpproxytestkotlin

import android.app.ProgressDialog
import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.InputMethodManager
import kotlinx.android.synthetic.main.activity_main.*
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URL

class MainActivity : AppCompatActivity() {

    internal var inProgress: Boolean = false
    var proxyTest: ProxyTest? = null
    var response: StringBuilder? = null
    var responseData: StringBuilder? = null

    private val proxy: Proxy
        get() {
            val proxyURI = editPROXY.text.toString().trim { it <= ' ' }
            if (proxyURI.contains(":")) {
                val parts = proxyURI.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (parts.size == 3) {
                    parts[0] = parts[1].replace("/", "")
                    parts[1] = parts[2].replace("/", "")
                    return Proxy(Proxy.Type.HTTP, InetSocketAddress(parts[1].replace("/", ""), Integer.parseInt(parts[2].replace("/", ""))))
                }
                return Proxy(Proxy.Type.HTTP, InetSocketAddress(parts[0], Integer.parseInt(parts[1])))
            } else if (proxyURI != "")
                return Proxy(Proxy.Type.HTTP, InetSocketAddress(proxyURI, 80))
            return Proxy.NO_PROXY
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.on_option_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        proxyTest = ProxyTest()
        when {
            item.itemId == R.id.btn_test -> if (!inProgress) {
                proxyTest!!.execute()
                val inputManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputManager.hideSoftInputFromWindow(currentFocus!!.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun tesConnection() {
        try {
            response = StringBuilder()
            responseData = StringBuilder()

            val connection = URL(editURL.text.toString()).openConnection(proxy) as HttpURLConnection
            Log.e(">>PROXY", proxy.toString())
            Log.e(">>CONNECTION", connection.toString())
            connection.connectTimeout = Integer.parseInt(timeOut.text.toString()) * 1000
            connection.readTimeout = connection.connectTimeout
            connection.connect()
            for (header in connection.headerFields.keys) {
                when {
                    header != null -> connection.headerFields[header]?.forEach { value -> response?.append("\n" + header + ":" + value) }
                    else -> response?.append(connection.headerFields[header])
                }
            }
            val inputStream = connection.inputStream
            val bytes = ByteArray(1)
            while (inputStream.read(bytes) > 0) {
                responseData?.append(String(bytes))
            }
            connection.disconnect()
        } catch (e: Exception) {
            response?.append(e.toString())
        }

    }

    inner class ProxyTest : AsyncTask<Void, Void, Void?>() {
        private var progress: ProgressDialog? = null

        override fun onPreExecute() {
            inProgress = true
            progress = ProgressDialog(this@MainActivity, R.style.MyTheme)
            progress!!.setCancelable(false)
            progress!!.show()
        }

        override fun doInBackground(vararg params: Void?): Void? {
            tesConnection()
            return null
        }

        override fun onPostExecute(result: Void?) {
            responce_header.text = response.toString()
            responce_data.text = responseData.toString()
            progress!!.dismiss()
            webview.loadData(responseData.toString(), "text/html", null)
            inProgress = false
        }

        override fun onCancelled() {
            progress!!.dismiss()
            inProgress = false
            proxyTest = null
            super.onCancelled()
        }
    }

}