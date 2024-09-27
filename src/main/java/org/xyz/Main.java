package org.xyz;

import org.xyz.service.ProxyService;

import java.io.*;
import java.util.*;

public class Main {

    private static ProxyService ps;
    private static final String FILE_WITH_LINKS = "links.txt";

    public static void main(String[] args) {

        List<String> links = Main.setUrl(args);
        Main.start(links);

    }

    private static List<String> setUrl(String[] args) {

        List<String> links = new ArrayList<>();

        if (!(args.length > 0)) {
            //read from file
            File file = new File(FILE_WITH_LINKS);

            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String link;
                while ((link = reader.readLine()) != null) {
                    links.add(link);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            //read from args
            links.add(args[0]);
        }

        return links;
    }

    private static void start(List<String> links) {

        links.forEach(e -> {
            ps = new ProxyService(e);
            Map<String, Integer> proxyList = ps.getProxies();
            Map<String, Integer> passedList = ps.getAliveProxies(proxyList);
            ps.save(passedList);
        });

    }
}
