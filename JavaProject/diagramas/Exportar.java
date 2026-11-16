import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;

/** Conversión y verificación con herramientas incluidas en el JDK. */
public class Exportar {
    static BufferedImage read(String name) throws Exception {
        BufferedImage image=ImageIO.read(Path.of("diagramas",name).toFile());
        if(image==null) throw new IllegalStateException("Imagen inválida: "+name);
        return image;
    }
    static void bmp(BufferedImage source,String name) throws Exception {
        BufferedImage rgb=new BufferedImage(source.getWidth(),source.getHeight(),BufferedImage.TYPE_INT_RGB);
        Graphics2D g=rgb.createGraphics();g.setColor(Color.WHITE);g.fillRect(0,0,rgb.getWidth(),rgb.getHeight());g.drawImage(source,0,0,null);g.dispose();
        Path path=Path.of("diagramas",name+".bmp");
        if(!ImageIO.write(rgb,"bmp",path.toFile())) throw new IllegalStateException("Escritor BMP no disponible");
        byte[] bytes=Files.readAllBytes(path);
        BufferedImage verified=ImageIO.read(path.toFile());
        if(bytes[0]!='B'||bytes[1]!='M'||verified==null||verified.getWidth()!=source.getWidth()||verified.getHeight()!=source.getHeight()) throw new IllegalStateException("BMP inválido");
        System.out.println(name+".bmp: firma BM, RGB 24 bits, "+verified.getWidth()+" x "+verified.getHeight()+", "+bytes.length+" bytes; reabierto correctamente");
    }
    public static void main(String[] args) throws Exception {
        BufferedImage a=read("componentes_acceso.png"),b=read("componentes_negocio.png");
        int width=Math.max(a.getWidth(),b.getWidth());
        BufferedImage combined=new BufferedImage(width,a.getHeight()+b.getHeight()+100,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=combined.createGraphics();g.setColor(Color.WHITE);g.fillRect(0,0,width,combined.getHeight());
        g.drawImage(a,(width-a.getWidth())/2,0,null);g.drawImage(b,(width-b.getWidth())/2,a.getHeight()+100,null);g.dispose();
        ImageIO.write(combined,"png",Path.of("diagramas/componentes.png").toFile());
        bmp(read("clases.png"),"clases");bmp(combined,"componentes");
    }
}
