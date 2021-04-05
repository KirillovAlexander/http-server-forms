package ru.netology.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private final Map<String, Map<String, Handler>> handlersMap = new ConcurrentHashMap<>();
    private final int port;
    final ExecutorService threadPool = Executors.newFixedThreadPool(64);

    public Server(int port) {
        this.port = port;
    }

    public void start() {
        final List<String> validPaths = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html", "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");

        try (final ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                try {
                    final Socket socket = serverSocket.accept();
                    Runnable serverRunnable = new ServerRunnable(socket, validPaths, this);
                    threadPool.submit(serverRunnable);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean addHandler(String method, String path, Handler handler) {
        Map<String, Handler> pathMap = handlersMap.get(method);
        if (null == pathMap) {
            pathMap = new ConcurrentHashMap<>();
            pathMap.put(path, handler);
            handlersMap.put(method, pathMap);
            return true;
        }
        Handler currentHandler = pathMap.get(path);
        if (null == currentHandler) {
            pathMap.put(path, handler);
            return true;
        }
        return false;
    }

    public Handler getHandler(String method, String path) {
        Map<String, Handler> pathMap = handlersMap.get(method);
        if (null == pathMap) return null;
        return pathMap.get(path);
    }

    public void printHandlers() {
        System.out.println("Список добавленных хэндлеров: ");
        for (Map.Entry<String, Map<String, Handler>> entry : handlersMap.entrySet()) {
            System.out.print(entry.getKey() + ": ");
            for (Map.Entry<String, Handler> innerEntry : entry.getValue().entrySet()) {
                System.out.print(innerEntry.getKey() + "; ");
            }
            System.out.println();
        }
    }
}


