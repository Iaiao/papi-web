package mc.iaiao.papiwebserver;

import com.sun.net.httpserver.HttpServer;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

public class PAPIWebServer extends JavaPlugin {
    private HttpServer server;
    
    @Override
    public void onEnable() {
        saveDefaultConfig();
        String ip = getConfig().getString("ip");
        short port = (short) getConfig().getInt("port");
        String name = getConfig().getString("name");
        
        assert ip != null;
        assert name != null;
        
        try {
            server = HttpServer.create(new InetSocketAddress(ip, port), 0);
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
            getLogger().info("Server started on port " + port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        server.stop(0);
    }
}
