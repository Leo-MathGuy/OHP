package omega.utils;

import arc.Core;
import arc.util.Strings;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import mindustry.Vars;
import mindustry.core.Version;
import mindustry.gen.Groups;
import mindustry.net.Administration;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.net.InetSocketAddress;

import static java.lang.System.currentTimeMillis;
import static omega.utils.Logger.discLogErr;

public class API {
    public static void main() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(6800), 0);
        server.createContext("/", new defaulthandle());
        server.createContext("/maplive", new mapLive());
        server.createContext("/status", new getStatus());
        server.setExecutor(null); // creates a default executor
        server.start();
    }

    static class defaulthandle implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String response = "well, you tried";
            try {
                t.sendResponseHeaders(200, response.length());
                t.getResponseBody().write(response.getBytes());
            } catch (IOException e) {
                discLogErr(e);
            }
        }
    }
    static class mapLive implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException, IOException {
            byte[] response = mapParse.renderMinimapLive();
            try {
                t.getResponseHeaders().set("Content-Type", "image/png");
                t.sendResponseHeaders(200, response.length);
                t.getResponseBody().write(response);
            } catch (IOException e) {
                discLogErr(e);
            }
        }
    }
    static class getStatus implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            JSONObject jo = new JSONObject();
            jo.put("status", "200");
            jo.put("players", Groups.player.size());
            jo.put("map", Vars.state.map.name());
            jo.put("author", Strings.stripColors(Vars.state.map.author()));
            jo.put("wave", Vars.state.wave);
            jo.put("tps", String.valueOf(Core.graphics.getFramesPerSecond()));
            jo.put("enemies", String.valueOf(Vars.state.enemies));
            jo.put("servername", Strings.stripColors(Administration.Config.serverName.get().toString()));
            jo.put("version", Version.build);
            jo.put("time", String.valueOf(currentTimeMillis()));
            String response = jo.toJSONString();
            try {
                t.sendResponseHeaders(200, response.length());
                t.getResponseHeaders().set("Content-Type", "application/json");
                t.getResponseBody().write(response.getBytes());
            } catch (IOException e) {
                t.sendResponseHeaders(500, response.length());
                t.getResponseHeaders().set("Content-Type", "application/json");
                t.getResponseBody().write(500);
                discLogErr(e);
            }
        }
    }
}
