package polimarket;

import polimarket.Domain.*;
import java.io.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.Supplier;

/** Una transacción abarca al coordinador y todas sus colaboraciones. */
public final class Store implements AutoCloseable {
    public static final class State implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
        public final Map<String, Usuario> usuarios = new LinkedHashMap<>();
        public final Map<String, Cliente> clientes = new LinkedHashMap<>();
        public final Map<String, Producto> productos = new LinkedHashMap<>();
        public final Map<String, Existencia> existencias = new LinkedHashMap<>();
        public final Map<String, Proveedor> proveedores = new LinkedHashMap<>();
        public final Map<String, Compra> compras = new LinkedHashMap<>();
        public final Map<String, Venta> ventas = new LinkedHashMap<>();
        public final Map<String, Entrega> entregas = new LinkedHashMap<>();
        public final Map<String, Movimiento> movimientos = new LinkedHashMap<>();
        State copy() {
            State s = new State();
            s.usuarios.putAll(usuarios); s.clientes.putAll(clientes); s.productos.putAll(productos);
            s.existencias.putAll(existencias); s.proveedores.putAll(proveedores); s.compras.putAll(compras);
            s.ventas.putAll(ventas); s.entregas.putAll(entregas); s.movimientos.putAll(movimientos);
            return s;
        }
    }
    private final Path file;
    private final FileChannel lockChannel;
    private final FileLock lock;
    private State state;
    private boolean writing;
    public Store(Path file) throws IOException {
        this.file = file.toAbsolutePath();
        Files.createDirectories(this.file.getParent());
        lockChannel = FileChannel.open(Path.of(this.file + ".lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        FileLock acquired;
        try { acquired = lockChannel.tryLock(); }
        catch (OverlappingFileLockException e) { acquired = null; }
        if (acquired == null) { lockChannel.close(); throw new IOException("Archivo de estado bloqueado por otro servidor"); }
        lock = acquired;
        try {
            state = Files.exists(this.file) ? read() : seed();
            if (!Files.exists(this.file)) save();
        } catch (IOException | RuntimeException e) { close(); throw e; }
    }
    private State read() throws IOException {
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(file))) {
            Set<String> allowed = Set.of(State.class.getName(), "java.util.LinkedHashMap", "java.util.HashMap",
                "java.util.CollSer", "java.util.ImmutableCollections$List12", "java.util.ImmutableCollections$ListN",
                "java.lang.String", "java.lang.Enum", "java.lang.Object", "java.util.Map$Entry");
            in.setObjectInputFilter(info -> {
                if (info.depth() > 30 || info.references() > 500_000 || info.streamBytes() > 50_000_000 || info.arrayLength() > 500_000)
                    return ObjectInputFilter.Status.REJECTED;
                Class<?> c = info.serialClass();
                if (c == null) return ObjectInputFilter.Status.UNDECIDED;
                while (c.isArray()) c = c.getComponentType();
                boolean domain = Arrays.asList(Domain.class.getDeclaredClasses()).contains(c);
                return c.isPrimitive() || domain || allowed.contains(c.getName())
                    ? ObjectInputFilter.Status.ALLOWED : ObjectInputFilter.Status.REJECTED;
            });
            return (State) in.readObject();
        } catch (ClassNotFoundException | ClassCastException e) { throw new IOException("Estado incompatible", e); }
    }
    private void save() throws IOException {
        Path temp = Files.createTempFile(file.getParent(), ".polimarket-", ".tmp");
        try {
            try (FileOutputStream fos = new FileOutputStream(temp.toFile()); ObjectOutputStream out = new ObjectOutputStream(fos)) {
                out.writeObject(state); out.flush(); fos.getFD().sync();
            }
            try { Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException e) { throw new IOException("El sistema de archivos no admite reemplazo atómico; operación rechazada", e); }
        } finally { Files.deleteIfExists(temp); }
    }
    public synchronized <T> T read(Supplier<T> operation) { return operation.get(); }
    public synchronized <T> T transaction(Supplier<T> operation) {
        if (writing) return operation.get();
        State previous = state.copy();
        writing = true;
        try { T result = operation.get(); save(); return result; }
        catch (IOException e) { state = previous; throw new BusinessException(500, "No se pudo persistir; operación revertida: " + e.getMessage()); }
        catch (RuntimeException | Error e) { state = previous; throw e; }
        finally { writing = false; }
    }
    public State state() {
        if (!Thread.holdsLock(this)) throw new IllegalStateException("Se requiere contexto de Store");
        return state;
    }
    public void requireTransaction() {
        if (!Thread.holdsLock(this) || !writing) throw new IllegalStateException("Se requiere transacción de escritura");
    }
    public static String id() { return UUID.randomUUID().toString(); }
    public static State seed() {
        State s = new State();
        s.usuarios.put("rh1", new Usuario("rh1", "Responsable RRHH demo", Rol.RRHH, true));
        s.usuarios.put("v1", new Usuario("v1", "Vendedor Uno demo", Rol.VENDEDOR, false));
        s.usuarios.put("v2", new Usuario("v2", "Vendedor Dos demo", Rol.VENDEDOR, true));
        s.usuarios.put("b1", new Usuario("b1", "Responsable Bodega demo", Rol.BODEGA, true));
        s.usuarios.put("e1", new Usuario("e1", "Responsable Entregas demo", Rol.ENTREGAS, true));
        s.clientes.put("c1", new Cliente("c1", "Cliente Uno ficticio", "Calle Demo 1, Ciudad Ficticia"));
        s.clientes.put("c2", new Cliente("c2", "Cliente Dos ficticio", "Calle Demo 2, Ciudad Ficticia"));
        s.productos.put("p1", new Producto("p1", "Cuaderno demo", List.of("pr1", "pr2")));
        s.productos.put("p2", new Producto("p2", "Lápiz demo", List.of("pr2")));
        s.existencias.put("p1", new Existencia("p1", 10, 0)); s.existencias.put("p2", new Existencia("p2", 5, 0));
        s.proveedores.put("pr1", new Proveedor("pr1", "Suministros Uno ficticios"));
        s.proveedores.put("pr2", new Proveedor("pr2", "Suministros Dos ficticios"));
        return s;
    }
    @Override public void close() throws IOException { lock.release(); lockChannel.close(); }
}
