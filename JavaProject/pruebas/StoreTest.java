import polimarket.*;
import polimarket.Domain.*;
import java.io.*;
import java.nio.file.*;
import java.util.Date;

public final class StoreTest {
    private static int count;
    private static void check(boolean condition, String name) { if (!condition) throw new AssertionError(name); System.out.println("OK " + name); count++; }
    public static void main(String[] args) throws Exception {
        Path root=Files.createTempDirectory("polimarket-store-");
        Path folder=Files.createDirectory(root.resolve("datos"));
        Path file=folder.resolve("estado.ser");
        try(Store s=new Store(file)) {
            // Renombrar el directorio provoca un fallo real de escritura, sin tocar datos de demostración.
            Path moved=root.resolve("movido"); Files.move(folder,moved);
            try {
                s.transaction(()->{s.state().existencias.put("p1",new Existencia("p1",20,0));return null;});
                throw new AssertionError("Se esperaba fallo de persistencia");
            } catch(BusinessException e) {check(e.status()==500,"Error de persistencia explícito");}
            check(s.read(()->s.state().existencias.get("p1").fisicas())==10,"Rollback en memoria tras fallo de escritura");
            Files.move(moved,folder);
        }
        try(Store s=new Store(file)) {check(s.read(()->s.state().existencias.get("p1").fisicas())==10,"Archivo anterior intacto tras fallo de escritura");}
        Path bad=root.resolve("ajeno.ser");
        try(ObjectOutputStream out=new ObjectOutputStream(Files.newOutputStream(bad))) {out.writeObject(new Date());}
        try(Store s=new Store(bad)) {throw new AssertionError("Filtro aceptó Date");}
        catch(InvalidClassException e) {check(true,"Filtro rechaza tipos no permitidos");}
        System.out.println(count+"/"+count+" aprobadas");
    }
}
