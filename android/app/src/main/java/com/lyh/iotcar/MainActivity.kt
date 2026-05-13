package com.lyh.iotcar

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {

    /**
     *  0 - 7: 表示八个方向
     *  8: 原地左转
     *  9: 原地右转
     *  10: 停止
     *
     */

    lateinit var tcpConnect: TcpConnect;
    lateinit var logText: TextView;

    lateinit var scrollView: ScrollView;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        setContentView(R.layout.main_layout)

        logText = findViewById<TextView>(R.id.text_log)

        scrollView = findViewById<ScrollView>(R.id.scrollview)

        val clearText = findViewById<TextView>(R.id.text_clear)

        clearText.setOnClickListener {
            logText.text = ""
        }

        val turnLeftText = findViewById<Button>(R.id.btn_turn_left)
        turnLeftText.setOnClickListener {
            tcpConnect.sendCmd(8, 80, object : TcpConnect.OnTcpTxRxListener{

                override fun onMsgReceive(msg: String?) {
                    runOnUiThread(Runnable {
                        addLog("Info: 接受到消息 => $msg \n", Color.GREEN)
                    })

                }

                override fun onError(msg: String?) {
                    runOnUiThread(Runnable {
                        addLog("Error: 收发消息异常 => $msg \n", Color.RED)
                    })

                }
            })
        }

        val turnRightText = findViewById<Button>(R.id.btn_turn_right)
        turnRightText.setOnClickListener {
            tcpConnect.sendCmd(9, 80, object : TcpConnect.OnTcpTxRxListener{

                override fun onMsgReceive(msg: String?) {
                    runOnUiThread(Runnable {
                        addLog("Info: 接受到消息 => $msg \n", Color.GREEN)
                    })

                }

                override fun onError(msg: String?) {
                    runOnUiThread(Runnable {
                        addLog("Error: 收发消息异常 => $msg \n", Color.RED)
                    })

                }
            })
        }


        tcpConnect = TcpConnect();

        val btnConnect = findViewById<Button>(R.id.btn_connect)
        btnConnect.setOnClickListener {
            tcpConnect.connect { bool, string ->
                runOnUiThread(Runnable {
                    if(bool){
                        addLog("Info: 连接成功!!! \n", Color.GREEN)
                    } else {
                        addLog("Error: 连接失败， 失败原因：${string} \n", Color.RED)
                    }
                })
            }
        }

        val controlView = findViewById<ControlView>(R.id.controlView)
        controlView.setOnTriggerListener { i, f, bool ->

            if(bool){
                tcpConnect.sendCmd(10, 0, object : TcpConnect.OnTcpTxRxListener{

                    override fun onMsgReceive(msg: String?) {
                        runOnUiThread(Runnable {
                            addLog("Info: 接受到消息 => $msg \n", Color.GREEN)
                        })
                    }

                    override fun onError(msg: String?) {
                        runOnUiThread(Runnable {
                            addLog("Error: 收发消息异常 => $msg \n", Color.RED)
                        })

                    }
                })
                return@setOnTriggerListener
            }

            tcpConnect.sendCmd(i, (f * 100).toInt(), object : TcpConnect.OnTcpTxRxListener{

                override fun onMsgReceive(msg: String?) {
                    runOnUiThread(Runnable {
                        addLog("Info: 接受到消息 => $msg \n", Color.GREEN)
                    })

                }

                override fun onError(msg: String?) {
                    runOnUiThread(Runnable {
                        addLog("Error: 收发消息异常 => $msg \n", Color.RED)
                    })

                }
            })
        }
    }

    fun TextView.appendColoredText(text: String, color: Int) {
        val spannable = SpannableString(text)
        // 设置颜色 span
        spannable.setSpan(
            ForegroundColorSpan(color),
            0,
            text.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        // 追加到 TextView
        this.append(spannable)
    }

    fun addLog(text: String, color: Int) {
        // 1. 追加文本
        logText.appendColoredText("$text\n", color)

        // 2. 核心：在 post 中执行滚动
        scrollView.post {
            // fullScroll 会平滑移动到最底部
            scrollView.fullScroll(View.FOCUS_DOWN)
        }
    }

}
