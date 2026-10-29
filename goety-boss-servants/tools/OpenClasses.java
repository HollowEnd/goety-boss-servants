import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Dev-only: copies a mod jar with the "final" flag removed from the given classes and their methods,
 * so servants can be compiled as subclasses of them. Bosses of Mass Destruction (Fabric) is Kotlin,
 * where classes and members are final unless marked open. At runtime the same flags are removed by
 * GoetyBossServantsMixinPlugin, so the real jar is used in the game.
 *
 * <pre>
 * java -cp asm.jar tools/OpenClasses.java in.jar out.jar net/barribob/boss/mob/utils/BaseEntity ...
 * </pre>
 */
public class OpenClasses {
    public static void main(String[] args) throws IOException {
        Path in = Path.of(args[0]);
        Path out = Path.of(args[1]);
        Set<String> open = Set.of(java.util.Arrays.copyOfRange(args, 2, args.length));
        try (ZipFile zip = new ZipFile(in.toFile()); ZipOutputStream dest = new ZipOutputStream(Files.newOutputStream(out))) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                byte[] bytes = zip.getInputStream(entry).readAllBytes();
                String name = entry.getName();
                if (name.endsWith(".class") && open.contains(name.substring(0, name.length() - 6))) {
                    bytes = openClass(bytes);
                }
                dest.putNextEntry(new ZipEntry(name));
                dest.write(bytes);
                dest.closeEntry();
            }
        }
    }

    private static byte[] openClass(byte[] bytes) {
        ClassReader reader = new ClassReader(bytes);
        ClassWriter writer = new ClassWriter(0);
        reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                super.visit(version, access & ~Opcodes.ACC_FINAL, name, signature, superName, interfaces);
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                return super.visitMethod(access & ~Opcodes.ACC_FINAL, name, descriptor, signature, exceptions);
            }
        }, 0);
        return writer.toByteArray();
    }
}
