package uk.co.thinkofdeath.patchtools.patch;

import org.objectweb.asm.tree.AbstractInsnNode;
import uk.co.thinkofdeath.patchtools.ClassSet;
import uk.co.thinkofdeath.patchtools.PatchScope;

public interface InstructionCreator {

    AbstractInsnNode create(ClassSet classSet, PatchScope scope, PatchInstruction instruction);
}
