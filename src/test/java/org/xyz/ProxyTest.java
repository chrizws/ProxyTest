package org.xyz;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xyz.service.ProxyService;

import java.io.File;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class ProxyTest {

    static ProxyService ps;
    static Map<String, Integer> proxy;
    String key;
    int value;

    @BeforeAll
    static void setUpForAll() {
        System.out.println("setupForAll");
        ps = new ProxyService("");
        //proxies = ps.getProxies();
        proxy = new HashMap<>();
        proxy.put("18.228.198.164", 80);
        proxy.put("13.37.89.201", 80);
        proxy.put("3.124.133.93", 80);
        proxy.put("45.230.50.3", 999);
        proxy.put("41.216.186.146", 8080);
    }

    @Test
    public void testFetchProxies() {
        assertTrue(proxy.size() > 0);
    }

    @Test
    public void testProxyExists() {
         key = proxy.keySet().stream().findFirst().get();
         value = proxy.get(key);

        assertTrue(key.contains("."));
        assertTrue(value > 0);
    }

    @Test
    public void isProxyAlive() {
        boolean result = ps.isProxyAlive("3.124.133.93", 80);
        assertNotNull(result, "return true or false, not null");
        assertTrue(result || !result, "return a boolean");

        assertThrows(IllegalArgumentException.class, () -> ps.isProxyAlive(null, 123));

    }

    @Test
    public void testProxiesAlive() {

        Map<String, Integer> proxies = ps.getAliveProxies(proxy);
        assertNotEquals(5, proxies.size());
    }

    @Test
    public void testSave() {

        ps.save(proxy);

        File file = new File("testedProxies.txt");
        assertTrue(file.length() > 0);
    }
}
