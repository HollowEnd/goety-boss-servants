import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Dev-only: copies a mod jar with one of its own methods renamed everywhere in its classes (where it
 * is declared and wherever it is called), for a mod whose own method name only clashes with a
 * Minecraft method once ForgeGradle renames Minecraft's methods for dev runs. Saints Dragons declares
 * its own final isFlying() in RideableDragonBase and Minecraft's m_29443_ (FlyingAnimal.isFlying) in
 * a subclass that calls it, which become the same method once renamed. The game uses the real jar.
 *
 * <pre>
 * java -cp asm.jar;asm-commons.jar tools/RenameMethods.java in.jar out.jar com/leon/saintsdragons/ isFlying ()Z saintsdragons$isFlying
 * </pre>
 */
public class RenameMethods {
    public static void main(String[] args) throws IOException {
        Path in = Path.of(args[0]);
        Path out = Path.of(args[1]);
        String classPrefix = args[2], name = args[3], descriptor = args[4], renamed = args[5];
        Remapper remapper = new Remapper() {
            @Override
            public String mapMethodName(String owner, String method, String desc) {
                return owner.startsWith(classPrefix) && method.equals(name) && desc.equals(descriptor) ? renamed : method;
            }
        };
        try (ZipFile zip = new ZipFile(in.toFile()); ZipOutputStream dest = new ZipOutputStream(Files.newOutputStream(out))) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                byte[] bytes = zip.getInputStream(entry).readAllBytes();
                if (entry.getName().endsWith(".class") && entry.getName().startsWith(classPrefix)) {
                    ClassReader reader = new ClassReader(bytes);
                    ClassWriter writer = new ClassWriter(0);
                    reader.accept(new ClassRemapper(writer, remapper), 0);
                    bytes = writer.toByteArray();
                }
                dest.putNextEntry(new ZipEntry(entry.getName()));
                dest.write(bytes);
                dest.closeEntry();
            }
        }
    }
}
