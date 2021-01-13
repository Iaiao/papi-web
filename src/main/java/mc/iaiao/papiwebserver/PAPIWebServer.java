package mc.iaiao.papiwebserver;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.plugin.java.JavaPlugin;

import javax.net.ssl.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.concurrent.Executors;

public class PAPIWebServer extends JavaPlugin {
    private HttpServer server;
    private HttpsServer https;
    
    @Override
    public void onEnable() {
        saveDefaultConfig();
        String name = getConfig().getString("name");
        assert name != null;
        
        try {
            server = HttpServer.create(new InetSocketAddress(getConfig().getString("http.ip"), getConfig().getInt("http.port")), 0);
            server.createContext("/", httpExchange -> {
                String query = httpExchange.getRequestURI().getQuery();
                if(query.startsWith("q=")) {
                    String response = PlaceholderAPI.setPlaceholders(getServer().getOfflinePlayer(name), "%" + query.substring(2) + "%");
                    httpExchange.sendResponseHeaders(200, response.getBytes().length);
                    httpExchange.getResponseBody().write(response.getBytes());
                    httpExchange.getResponseBody().close();
                }
            });
            server.setExecutor(Executors.newFixedThreadPool(10));
            server.start();
            getLogger().info("HTTP server started on " + getConfig().getString("http.ip") + ":" + getConfig().getInt("http.port"));
            
            if(getConfig().getBoolean("https.enabled")) {
                https = HttpsServer.create(new InetSocketAddress(getConfig().getString("https.ip"), getConfig().getInt("https.port")), 0);
                SSLContext ssl = SSLContext.getInstance("TLS");
                char[] password = getConfig().getString("https.password").toCharArray();
                KeyStore ks = KeyStore.getInstance(getConfig().getString("https.keystore"));
                FileInputStream fis = new FileInputStream(new File(getDataFolder(), getConfig().getString("https.file")));
                ks.load(fis, password);
                KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
                kmf.init(ks, password);
                TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
                tmf.init(ks);
                ssl.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
                https.setHttpsConfigurator(new HttpsConfigurator(ssl) {
                    public void configure(HttpsParameters params) {
                        try {
                            SSLContext context = getSSLContext();
                            SSLEngine engine = context.createSSLEngine();
                            params.setNeedClientAuth(false);
                            params.setCipherSuites(engine.getEnabledCipherSuites());
                            params.setProtocols(engine.getEnabledProtocols());

                            SSLParameters sslParameters = context.getSupportedSSLParameters();
                            params.setSSLParameters(sslParameters);

                        } catch (Exception ex) {
                            System.out.println("Failed to configure HTTPS:");
                            ex.printStackTrace();
                        }
                    }
                });
                https.createContext("/", httpExchange -> {
                    String query = httpExchange.getRequestURI().getQuery();
                    if(query.startsWith("q=")) {
                        String response = PlaceholderAPI.setPlaceholders(getServer().getOfflinePlayer(name), "%" + query.substring(2) + "%");
                        httpExchange.sendResponseHeaders(200, response.getBytes().length);
                        httpExchange.getResponseBody().write(response.getBytes());
                        httpExchange.getResponseBody().close();
                    }
                });
                https.setExecutor(Executors.newFixedThreadPool(10));
                https.start();
                getLogger().info("HTTPS server started on " + getConfig().getString("https.ip") + ":" + getConfig().getInt("https.port"));
            }
        } catch (IOException | NoSuchAlgorithmException | KeyStoreException | CertificateException | UnrecoverableKeyException | KeyManagementException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        server.stop(0);
        https.stop(0);
    }
}
