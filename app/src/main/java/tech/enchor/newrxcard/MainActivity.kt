package tech.enchor.newrxcard

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.content.Context
import kotlinx.android.synthetic.main.activity_main.*
import org.liquidplayer.javascript.JSContext
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONObject
import org.liquidplayer.service.MicroService
import java.net.URI


class MainActivity : AppCompatActivity() {

    var updateUIReciver : BroadcastReceiver? = null


    override fun onCreate(savedInstanceState: Bundle?) {

        val filter = IntentFilter()

        filter.addAction("tech.enchor.newrxcard")

        updateUIReciver = object : BroadcastReceiver() {

            override fun onReceive(context: Context, intent: Intent) {
                //UI update here
                printToScreen(intent.getStringExtra("tag"), intent.getStringExtra("console"))
            }
        }
        registerReceiver(updateUIReciver, filter)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        val service = MicroService(
//            applicationContext,
//            URI("android.resource://tech.enchor.newrxcard/raw/test"),
//            MicroService.ServiceStartListener { service, synchronizer ->
//                Log.d("js", "running script")
//
//
//                service.addEventListener("console", MicroService.EventListener{
//                    service, event, payload ->
//                    printToScreen("mam", payload.getString("content"))
//                })
//
//                service.addEventListener("signed_content", MicroService.EventListener{
//                    service, event, payload ->
//                    Log.d("mam", payload.getString("content"))
//                    printToScreen("mam", payload.getString("content").substring(0,100) + "...")
//                })
//
//            }
//        )
//
//        test.setOnClickListener {
//            val payload = JSONObject()
//            payload.put("msg", "OSADAJDD")
//            service.emit("sign_content", payload)
//
//            Log.d("emit_to_service", payload.toString())
//        }
//
//        service.start()





        // val returned = jsContext.evaluateScript("", "android.resource://com.example.myapp/raw/test", 0)

        val intent = Intent(this, HostCardEmulatorService::class.java)
        this.startService(intent)

        printToScreen("build", "280501")
    }

    override fun onDestroy() {
        unregisterReceiver(updateUIReciver);
        super.onDestroy()
    }

    fun printToScreen(tag: String, msg: String){
        runOnUiThread {
            textView.append("\n" + tag + " :" + msg)
            textViewScroll.fullScroll(View.FOCUS_DOWN)
        }
    }
}
