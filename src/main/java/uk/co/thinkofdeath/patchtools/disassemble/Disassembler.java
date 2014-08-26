package uk.co.thinkofdeath.patchtools.disassemble;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LineNumberNode;
import uk.co.thinkofdeath.patchtools.instruction.Instruction;
import uk.co.thinkofdeath.patchtools.instruction.instructions.Utils;
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet;
import uk.co.thinkofdeath.patchtools.wrappers.ClassWrapper;

import java.util.Arrays;

public class Disassembler {

    private final ClassSet classSet;

    public Disassembler(ClassSet classSet) {
        this.classSet = classSet;
    }

    public String disasemble(String cls) {
        ClassWrapper classWrapper = classSet.getClassWrapper(cls);

        StringBuilder patch = new StringBuilder("\n");

        ClassNode node = classWrapper.getNode();

        patch.append(".class ")
                .append(node.name)
                .append('\n');

        node.fields.forEach(f -> {
            patch.append("    ")
                    .append(".field ")
                    .append(f.name)
                    .append(' ')
                    .append(f.desc);
            if ((f.access & Opcodes.ACC_STATIC) != 0) {
                patch.append(" static");
            }
            if ((f.access & Opcodes.ACC_PRIVATE) != 0) {
                patch.append(" private");
            }
            if (f.value != null) {
                patch.append(" ");
                Utils.printConstant(patch, f.value);
            }
            patch.append('\n');
        });

        patch.append('\n');

        node.methods.forEach(m -> {
            patch.append("    ")
                    .append(".method ")
                    .append(m.name)
                    .append(' ')
                    .append(m.desc);
            if ((m.access & Opcodes.ACC_STATIC) != 0) {
                patch.append(" static");
            }
            if ((m.access & Opcodes.ACC_PRIVATE) != 0) {
                patch.append(" private");
            }
            patch.append('\n');

            Arrays.stream(m.instructions.toArray())
                    .filter(i -> !(i instanceof LineNumberNode))
                    .forEach(i -> {
                        patch.append("    ")
                                .append("    ")
                                .append('.');
                        if (!Instruction.print(patch, m, i)) {
                            // TODO: throw new UnsupportedOperationException(i.toString());
                            patch.append("unsupported ").append(i);
                        }
                        patch.append('\n');
                    });

            patch.append("    ")
                    .append(".end-method\n");
            patch.append('\n');
        });

        patch.append(".end-class\n");

        return patch.toString();
    }
}
