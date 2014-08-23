package uk.co.thinkofdeath.patchtools.patch;

public enum Instruction {
    ANY(null, null),
    LDC(InstructionCheckers::checkLdc, InstructionCreators::createLdc),
    ARETURN(InstructionCheckers::checkAReturn, InstructionCreators::createAReturn),;
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
