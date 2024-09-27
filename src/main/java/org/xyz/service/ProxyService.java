package org.xyz.service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class ProxyService {

    private Map<String, Integer> proxies;
    private String url;
    private final int FIXED_THREADS = 200;
    private final String TEST_URL = "http://www.ifconfig.me/ip";
    private final String OUTPUT_FILENAME = "testedProxies.txt";

    public ProxyService(String url) {
        this.url = url;
    }

    public Map<String, Integer> getProxies() {

        proxies = Collections.synchronizedMap(new HashMap<>());

        HttpClient client = HttpClient.newBuilder()
                .build();
        HttpRequest request = null;
        try {
            request = HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .build();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        HttpResponse<String> response = null;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        String[] proxy = response.body().split("\n");

        for (int i = 0; i < proxy.length; i++) {

            if (!proxy[i].isEmpty() && proxy[i].charAt(0) >= '1' && proxy[i].charAt(0) <= '9') {
                String[] line = proxy[i].split(" ");
                String[] p = line[0].split(":");
                proxies.put(p[0], Integer.parseInt(p[1]));
            }
        }

        return proxies;
    }

    public void save(Map<String, Integer> proxies) {

        File file = new File(OUTPUT_FILENAME);

        try (BufferedWriter out = new BufferedWriter(new FileWriter(file))) {

            proxies.forEach((k, v) -> {
                try {
                    out.write(k + ":" + v + "\n");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, Integer> getAliveProxies(Map<String, Integer> proxies) {

        ExecutorService exec = Executors.newFixedThreadPool(FIXED_THREADS);

        List<CompletableFuture<AbstractMap.SimpleEntry<String, Integer>>> futures = proxies.entrySet().stream()
                .map(e -> CompletableFuture.supplyAsync(() -> {
                    try {
                        boolean alive = isProxyAlive(e.getKey(), e.getValue());

                        if (alive) {
                            return new AbstractMap.SimpleEntry<>(e.getKey(), e.getValue());
                        }
                    } catch (Exception ex) {

                    }

                    return null;

                }, exec))
                .toList();


        Map<String, Integer> passedProx = futures.stream()
                //.map(CompletableFuture::join)
                .map(e -> {
                    try {
                        return e.get();
                    } catch (Exception ex) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        System.out.println("Found " + passedProx.size() + " proxies under 1000ms");

        exec.shutdown();

        return passedProx;
    }

    public boolean isProxyAlive(String hostname, int port) {

        HttpClient c = HttpClient.newBuilder()
                .proxy(ProxySelector.of(new InetSocketAddress(hostname, port)))
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(1))
                .build();

        HttpRequest r = null;
        try {
            r = HttpRequest.newBuilder()
                    .uri(new URI(TEST_URL))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36 Edg/123.0.2420.97")
                    .timeout(Duration.ofSeconds(1))
                    .build();

        } catch (URISyntaxException e) {
            //System.out.println("Request " + e.getMessage());
        }


        HttpResponse<String> res = null;

        try {
            long start = System.currentTimeMillis();
            res = c.send(r, HttpResponse.BodyHandlers.ofString());

            if (res == null || res.statusCode() != 200 || res.body().isEmpty())
                return false;

            long end = System.currentTimeMillis();
            long time = (end - start);
            System.out.println(hostname + ":" + port + " - " + time + "ms");

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);

        } finally {
            c.close();
        }

        return true;

    }
}
