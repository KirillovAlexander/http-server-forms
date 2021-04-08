package ru.netology.server;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

public class Request {
    final char QUERY_DELIMITER = '?';
    private String method;
    private String path;
    private String version;
    private Map<String, String> headers;
    private String body;
    private List<NameValuePair> queryParams;

    public Request() {
        this.headers = new ConcurrentHashMap<>();
        this.method = "";
        this.path = "";
        this.version = "";
        this.body = "";
    }

    public boolean addHeader(String header) {
        String[] headerParts = header.split(":");
        if (headerParts.length == 2) {
            this.headers.put(headerParts[0], headerParts[1].replace(" ",""));
            return true;
        } else {
            return false;
        }
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getMethod() {
        return method;
    }

    @Override
    public String toString() {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("\n" + method + " " + path + " " + version + "\n");
            for (Map.Entry<String, String> entry : headers.entrySet())
                sb.append(entry.getKey() + ":" + entry.getValue() + "\n");
            sb.append("\n");
            sb.append(body);
            return sb.toString();
        } catch (Exception e) {
            System.out.print("Ошибка: ");
            e.printStackTrace();
        }
        return "";
    }

    public Optional<String> extractHeader(String header) {
        return headers.entrySet()
                .stream()
                .filter(o -> o.getKey().equals(header))
                .map(o -> o.getValue())
                .findFirst();
    }

    public void setQueryParams() {
        int delimiter = path.indexOf(QUERY_DELIMITER);
        if (delimiter == -1) return;
        queryParams = URLEncodedUtils.parse(path.substring(delimiter + 1), Charset.forName("UTF-8"));
    }

    public List<NameValuePair> getBodyParams() {
        return URLEncodedUtils.parse(body, Charset.forName("UTF-8"));
    }

    public String getBody() {
        return body;
    }

    public String getPathWithoutQueryParams() {
        int queryDelimiter = path.indexOf(QUERY_DELIMITER);
        if (queryDelimiter != -1) {
            path = path.substring(0, queryDelimiter);
            System.out.println(path);
        }
        return path;
    }

    public Optional<String> getQueryParamValue(String queryParam) {
        return queryParams.stream()
                .filter(o -> o.getName().equals(queryParam))
                .map(o -> o.getValue())
                .findFirst();
    }

    public Optional<String> getHeaderValue(String header) {
        return headers.entrySet()
        .stream()
        .filter(o -> o.getKey().equals(header))
        .map(o -> o.getValue())
        .findFirst();
    }
}
