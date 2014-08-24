package uk.co.thinkofdeath.patchtools.patch;

import org.objectweb.asm.Opcodes;

import static uk.co.thinkofdeath.patchtools.patch.InstructionCheckers.checkInvoke;
import static uk.co.thinkofdeath.patchtools.patch.InstructionCreators.createInvoke;

public enum Instruction {
    ANY(null, null),
    LDC(InstructionCheckers::checkLdc, InstructionCreators::createLdc),
    ARETURN(InstructionCheckers::checkAReturn, InstructionCreators::createAReturn),
    INVOKE_STATIC(checkInvoke(Opcodes.INVOKESTATIC), createInvoke(Opcodes.INVOKESTATIC)),
    INVOKE_SPECIAL(checkInvoke(Opcodes.INVOKESPECIAL), createInvoke(Opcodes.INVOKESPECIAL)),
    INVOKE_VIRTUAL(checkInvoke(Opcodes.INVOKEVIRTUAL), createInvoke(Opcodes.INVOKEVIRTUAL)),
    INVOKE_INTERFACE(checkInvoke(Opcodes.INVOKEINTERFACE), createInvoke(Opcodes.INVOKEINTERFACE)),;
    private final InstructionChecker checker;
    private final InstructionCreator creator;

    Instruction(InstructionChecker checker, InstructionCreator creator) {
        this.checker = checker;
        this.creator = creator;
    }

    public InstructionChecker getChecker() {
        return checker;
    }

    public InstructionCreator getCreator() {
        return creator;
    }
}
