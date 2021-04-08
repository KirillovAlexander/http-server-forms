package ru.netology.server;

import org.apache.http.NameValuePair;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class ServerRunnable implements Runnable {

    public static final String GET = "GET";
    public static final String POST = "POST";

    private static final String FORM_URLENCODED = "application/x-www-form-urlencoded";

    private final Socket socket;
    private final List<String> validPaths;
    private final Server server;

    public ServerRunnable(Socket socket, List<String> validPaths, Server server) {
        this.socket = socket;
        this.validPaths = validPaths;
        this.server = server;
    }

    @Override
    public void run() {
        final var allowedMethods = List.of(GET, POST);
        try (final BufferedInputStream in = new BufferedInputStream((socket.getInputStream()));
             final BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream())) {

            final int limit = 4096;
            in.mark(limit);

            in.mark(limit);
            final byte[] buffer = new byte[limit];
            final int read = in.read(buffer);

            // ищем request line
            final byte[] requestLineDelimiter = new byte[]{'\r', '\n'};
            final int requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);
            if (requestLineEnd == -1) {
                badRequest(out);
                socket.close();
                return;
            }

            // читаем request line
            final String[] requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");
            if (requestLine.length != 3) {
                badRequest(out);
                socket.close();
                return;
            }

            final String method = requestLine[0];
            if (!allowedMethods.contains(method)) {
                methodNotImplemented(out);
                socket.close();
                return;
            }

            final String path = requestLine[1];
            if (!path.startsWith("/")) {
                badRequest(out);
                socket.close();
                return;
            }

            Request request = new Request();
            request.setMethod(method);
            request.setPath(path);
            request.setVersion(requestLine[2]);
            request.setQueryParams();

            // ищем заголовки
            final byte[] headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
            final int headersStart = requestLineEnd + requestLineDelimiter.length;
            final int headersEnd = indexOf(buffer, headersDelimiter, headersStart, read);
            if (headersEnd == -1) {
                badRequest(out);
                socket.close();
                return;
            }

            // отматываем на начало буфера
            in.reset();
            // пропускаем requestLine
            in.skip(headersStart);

            final byte[] headersBytes = in.readNBytes(headersEnd - headersStart);
            final List<String> headers = Arrays.asList(new String(headersBytes).split("\r\n"));
            for (String s : headers) {
                request.addHeader(s);
            }

            // для GET тела нет
            if (!method.equals(GET)) {
                in.skip(headersDelimiter.length);
                // вычитываем Content-Length, чтобы прочитать body
                final Optional<String> contentLength = request.extractHeader("Content-Length");
                if (contentLength.isPresent()) {
                    final int length = Integer.parseInt(contentLength.get());
                    final byte[] bodyBytes = in.readNBytes(length);
                    final var body = new String(bodyBytes);
                    request.setBody(body);
                    //Считаем параметры для Content-Type = application/x-www-form-urlencoded
                    Optional<String> contentType = request.getHeaderValue("Content-Type");
                    if (contentType.isPresent() && contentType.get().equals(FORM_URLENCODED)) {
                        System.out.println("Параметры из тела: ");
                        List<NameValuePair> bodyParams = request.getBodyParams();
                        for (NameValuePair pair:
                             bodyParams) {
                            System.out.println(pair.getName() + ": " + pair.getValue());
                        }
                    }
                }
            }
            System.out.println("Пришедший запрос: " + request);
            Handler handler = server.getHandler(request.getMethod(), request.getPathWithoutQueryParams());

            if (handler == null) {
                methodNotImplemented(out);
                socket.close();
                return;
            }
            handler.handle(request, out);
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void badRequest(BufferedOutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 400 Bad Request\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }

    private static void methodNotImplemented(BufferedOutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 501 Not Implemented\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }

    private static int indexOf(byte[] array, byte[] target, int start, int max) {
        outer:
        for (int i = start; i < max - target.length + 1; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }
}
