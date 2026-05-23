package com.winlator.fusion.core;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class WineRequestHandler {
    public abstract static class RequestCodes {
        public static final int OPEN_URL = 1;
        public static final int GET_WINE_CLIPBOARD = 2;
        public static final int SET_WINE_CLIPBOARD = 3;
    }

    private final Context context;
    private ServerSocket serverSocket;
    private volatile boolean running;

    public WineRequestHandler(Context context) {
        this.context = context;
    }

    public void start() {
        running = true;
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(20000, 50, InetAddress.getLoopbackAddress());
                while (running) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        handleClient(clientSocket);
                    } catch (IOException e) {
                        if (!running) break;
                    }
                }
            } catch (IOException e) {}
        }).start();
    }

    public void stop() {
        running = false;
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {}
            serverSocket = null;
        }
    }

    private void handleClient(Socket clientSocket) {
        try (clientSocket) {
            DataInputStream inputStream = new DataInputStream(clientSocket.getInputStream());
            DataOutputStream outputStream = new DataOutputStream(clientSocket.getOutputStream());
            int requestCode = inputStream.readInt();
            handleRequest(requestCode, inputStream, outputStream);
        } catch (IOException e) {}
    }

    private void handleRequest(int requestCode, DataInputStream inputStream, DataOutputStream outputStream) throws IOException {
        switch (requestCode) {
            case RequestCodes.OPEN_URL:
                openURL(inputStream);
                break;
            case RequestCodes.GET_WINE_CLIPBOARD:
                getWineClipboard(inputStream);
                break;
            case RequestCodes.SET_WINE_CLIPBOARD:
                setWineClipboard(outputStream);
                break;
        }
    }

    private static final int MAX_URL_LENGTH = 8192;

    private void openURL(DataInputStream inputStream) throws IOException {
        int urlLength = inputStream.readInt();
        if (urlLength <= 0 || urlLength > MAX_URL_LENGTH) return;
        byte[] urlBytes = new byte[urlLength];
        inputStream.readFully(urlBytes);
        String url = new String(urlBytes);
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    private void getWineClipboard(DataInputStream inputStream) throws IOException {
        int format = inputStream.readInt();
        int dataLength = inputStream.readInt();
        byte[] data = new byte[dataLength];
        inputStream.readFully(data);
        if (format == 13) {
            String text = new String(data, StandardCharsets.UTF_16LE);
            ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            clipboardManager.setPrimaryClip(ClipData.newPlainText("text", text));
        }
    }

    private void setWineClipboard(DataOutputStream outputStream) throws IOException {
        ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clipData = clipboardManager.getPrimaryClip();
        if (clipData != null && clipData.getItemCount() > 0) {
            CharSequence text = clipData.getItemAt(0).getText();
            if (text != null) {
                byte[] utf16leBytes = text.toString().getBytes(StandardCharsets.UTF_16LE);
                outputStream.writeInt(utf16leBytes.length);
                outputStream.write(utf16leBytes);
                outputStream.flush();
                return;
            }
        }
        outputStream.writeInt(0);
        outputStream.flush();
    }
}
