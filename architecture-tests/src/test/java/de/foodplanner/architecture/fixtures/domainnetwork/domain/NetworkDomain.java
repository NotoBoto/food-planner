package de.foodplanner.architecture.fixtures.domainnetwork.domain;

import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Proxy;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.http.HttpClient;
import java.nio.channels.SocketChannel;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.sql.Connection;

public class NetworkDomain {
    HttpClient httpClient;
    HttpURLConnection httpUrlConnection;
    Socket socket;
    ServerSocket serverSocket;
    DatagramSocket datagramSocket;
    SocketChannel socketChannel;
    Connection connection;
    InetSocketAddress socketAddress;
    NetworkInterface networkInterface;

    InputStream openStream(URL url) throws IOException {
        return url.openStream();
    }

    Object openConnection(URL url) throws IOException {
        return url.openConnection();
    }

    Object openConnectionViaProxy(URL url, Proxy proxy) throws IOException {
        return url.openConnection(proxy);
    }

    Object getContent(URL url) throws IOException {
        return url.getContent();
    }

    Object getContentAs(URL url) throws IOException {
        return url.getContent(new Class<?>[] {String.class});
    }

    boolean isReachable(String host) throws IOException {
        return InetAddress.getByName(host).isReachable(1000);
    }

    Object lookup(String name) throws IOException, NotBoundException {
        return Naming.lookup(name);
    }
}
