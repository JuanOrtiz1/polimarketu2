package polimarket;

import com.sun.net.httpserver.*;
import polimarket.Domain.LineaVenta;
import polimarket.contracts.Contracts.*;
import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import static polimarket.BusinessException.require;

public final class AdaptadorHTTP implements HttpHandler {
    private final IRRHH rrhh; private final IBodega bodega; private final IVentas ventas;
    private final IProveedores proveedores; private final IEntregas entregas; private final Path web;
    public AdaptadorHTTP(IRRHH rrhh, IBodega bodega, IVentas ventas, IProveedores proveedores, IEntregas entregas, Path web) {
        this.rrhh=rrhh; this.bodega=bodega; this.ventas=ventas; this.proveedores=proveedores; this.entregas=entregas; this.web=web;
    }
    public void handle(HttpExchange x) throws IOException {
        try {
            String path = x.getRequestURI().getPath();
            if (!path.startsWith("/api/")) { resource(x, path); return; }
            String auth = x.getRequestHeaders().getFirst("Authorization");
            String token = auth != null && auth.startsWith("Bearer ") ? auth.substring(7) : "";
            String method = x.getRequestMethod();
            Map<String,String> p = method.equals("POST") ? form(x) : Map.of();
            Object result = switch (method + " " + path) {
                case "GET /api/vendedores" -> rrhh.vendedores(token);
                case "POST /api/autorizacion" -> { fields(p, "vendedor", "activo"); String active = p.get("activo"); require(active.equals("true") || active.equals("false"), 400, "activo debe ser true o false"); yield rrhh.autorizar(token, p.get("vendedor"), Boolean.parseBoolean(active)); }
                case "GET /api/catalogo" -> ventas.catalogo(token);
                case "GET /api/existencias" -> bodega.existencias(token);
                case "GET /api/proveedores" -> proveedores.reposicion(token);
                case "POST /api/compras" -> { fields(p, "proveedor", "producto", "cantidad", "referencia"); yield proveedores.comprar(token, p.get("proveedor"), p.get("producto"), integer(p.get("cantidad")), p.get("referencia")); }
                case "POST /api/ventas" -> { fields(p, "cliente", "lineas"); yield ventas.vender(token, p.get("cliente"), lines(p.get("lineas"))); }
                case "GET /api/entregas/pendientes" -> entregas.pendientes(token);
                case "POST /api/entregas" -> { fields(p, "venta"); yield entregas.despachar(token, p.get("venta")); }
                default -> throw new BusinessException(404, "Ruta o método inexistente");
            };
            reply(x, 200, result);
        } catch (BusinessException e) { reply(x, e.status(), Map.of("error", e.getMessage(), "status", e.status())); }
        catch (Exception e) { e.printStackTrace(System.err); reply(x, 500, Map.of("error", "Error interno del servidor", "status", 500)); }
        finally { x.close(); }
    }
    private static void fields(Map<String,String> p, String... names) {
        require(p.keySet().equals(Set.of(names)), 400, "Parámetros requeridos: " + String.join(", ", names));
        for (String name : names) require(!p.get(name).isBlank(), 400, "Campo vacío: " + name);
    }
    private static int integer(String value) {
        require(value != null && value.matches("[0-9]{1,7}"), 400, "Cantidad entera positiva requerida");
        int n = Integer.parseInt(value); polimarket.business.Bodega.cantidad(n); return n;
    }
    private static List<LineaVenta> lines(String value) {
        String[] parts = value.split(",", -1);
        require(parts.length <= 100, 400, "Máximo 100 líneas");
        List<LineaVenta> result = new ArrayList<>();
        for (String part : parts) { String[] pair = part.split(":", -1); require(pair.length == 2 && !pair[0].isBlank(), 400, "Formato requerido: p1:2,p2:1"); result.add(new LineaVenta(pair[0], integer(pair[1]))); }
        return result;
    }
    private static Map<String,String> form(HttpExchange x) throws IOException {
        String type = x.getRequestHeaders().getFirst("Content-Type");
        require(type != null && type.split(";", 2)[0].trim().equalsIgnoreCase("application/x-www-form-urlencoded"), 400, "Use application/x-www-form-urlencoded");
        byte[] bytes = x.getRequestBody().readNBytes(65_537);
        require(bytes.length <= 65_536, 400, "Formulario demasiado grande");
        Map<String,String> result = new LinkedHashMap<>();
        try {
            for (String part : new String(bytes, StandardCharsets.UTF_8).split("&")) {
                String[] pair = part.split("=", 2); require(pair.length == 2, 400, "Formulario inválido");
                String key = URLDecoder.decode(pair[0], StandardCharsets.UTF_8);
                String value = URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
                require(result.putIfAbsent(key, value) == null, 400, "Parámetro duplicado");
            }
        } catch (IllegalArgumentException e) { throw new BusinessException(400, "Codificación de formulario inválida"); }
        return result;
    }
    private static void reply(HttpExchange x, int status, Object result) throws IOException {
        send(x, status, "application/json; charset=utf-8", Json.encode(result).getBytes(StandardCharsets.UTF_8));
    }
    private static void send(HttpExchange x, int status, String type, byte[] body) throws IOException {
        x.getResponseHeaders().set("Content-Type", type);
        x.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        x.getResponseHeaders().set("Cache-Control", "no-store");
        x.sendResponseHeaders(status, body.length); x.getResponseBody().write(body);
    }
    private void resource(HttpExchange x, String path) throws IOException {
        Map<String,String> types = Map.of("/index.html", "text/html", "/style.css", "text/css", "/api.mjs", "text/javascript", "/app.mjs", "text/javascript");
        if (path.equals("/")) path = "/index.html";
        require(x.getRequestMethod().equals("GET") && types.containsKey(path), 404, "Recurso inexistente");
        Path file = web.resolve(path.substring(1)); require(Files.isRegularFile(file), 404, "Recurso web no encontrado; ejecute desde la raíz del proyecto");
        x.getResponseHeaders().set("Content-Security-Policy", "default-src 'self'; connect-src 'self'; style-src 'self'; script-src 'self'; frame-ancestors 'none'; base-uri 'none'");
        send(x, 200, types.get(path) + "; charset=utf-8", Files.readAllBytes(file));
    }
}
