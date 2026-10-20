package de.foodplanner.architecture.fixtures.applicationnetwork.application;

import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.http.HttpClient;
import java.nio.channels.SocketChannel;
import java.sql.Connection;

public class NetworkHandler {
    HttpClient httpClient;
    HttpURLConnection httpUrlConnection;
    Socket socket;
    ServerSocket serverSocket;
    DatagramSocket datagramSocket;
    SocketChannel socketChannel;
    Connection connection;

    InputStream openStream(URL url) throws IOException {
        return url.openStream();
    }

    Object openConnection(URL url) throws IOException {
        return url.openConnection();
    }

    Object getContent(URL url) throws IOException {
        return url.getContent();
    }
}
