package com.lyh.iotcar;


import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;

import java.net.InetSocketAddress;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TcpConnect {

    private Socket socket;
    private PrintWriter writer;
    private InputStream input;

    // 使用线程池管理后台任务，避免阻塞主线程
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public interface OnTcpConnectListener {
        void onConnect(boolean success, String msg);
    }

    public interface OnTcpTxRxListener {
        void onMsgReceive(String msg);
        void onError(String msg);
    }

    public void connect(final OnTcpConnectListener mConnectListener) {
        executorService.execute(() -> {
            try {
                // 如果已有连接，先关闭
                closeInternal();

                int SERVER_PORT = 8080;
                String SERVER_IP = "192.168.4.1";

                socket = new Socket();
                // 设置超时时间，防止无限等待
                socket.connect(new InetSocketAddress(SERVER_IP, SERVER_PORT), 5000);

                // 连接成功后初始化输入输出流
                writer = new PrintWriter(socket.getOutputStream(), true);
                input = socket.getInputStream();

                if (mConnectListener != null) {
                    mConnectListener.onConnect(true, "Connected successfully");
                }

            } catch (Exception e) {
                if (mConnectListener != null) {
                    mConnectListener.onConnect(false, e.getMessage());
                }
            }
        });
    }

    public void sendCmd(final int direction, final int throttle, final OnTcpTxRxListener listener) {
        // 异步发送
        executorService.execute(() -> {
            if (socket == null || !socket.isConnected() || socket.isClosed()) {
                if (listener != null) listener.onError("Socket is not connected");
                return;
            }

            try {
                String command = String.format(Locale.CHINA, "$%d,%d#", direction, throttle);

                // 发送数据
                writer.println(command);

                try {
                    byte[] buffer = new byte[1024];
                    int len = input.read(buffer);

                    if (len != -1) {
                        String response = new String(buffer, 0, len, "UTF-8");
                        if (listener != null) {
                            listener.onMsgReceive(response);
                        }
                    }
                } catch (IOException e) {
                    // 处理异常
                }

            } catch (Exception e) {
                if (listener != null) {
                    listener.onError("Communication error: " + e.getMessage());
                }
            }
        });
    }

    public void close() {
        executorService.execute(this::closeInternal);
    }

    private void closeInternal() {
        try {
            if (writer != null) writer.close();
            if (input != null) input.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}