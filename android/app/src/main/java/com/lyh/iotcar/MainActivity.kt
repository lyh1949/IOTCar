package com.lyh.iotcar

import android.app.Activity
import android.os.Bundle
import android.util.Log

class MainActivity : Activity() {

    val TAG = "ControlView"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_layout)

        val controlView = findViewById<ControlView>(R.id.controlView)
        controlView.setOnTriggerListener { i, f, bool ->
            if(bool){
                Log.i("lyh", "已停止")
            }
            Log.i("lyh", "direction: $i throttle: $f")
        }
    }
}
