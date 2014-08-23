package uk.co.thinkofdeath.patchtools.patch;

import org.objectweb.asm.Type;
import uk.co.thinkofdeath.patchtools.ClassSet;
import uk.co.thinkofdeath.patchtools.PatchScope;
import uk.co.thinkofdeath.patchtools.PatchVerifyException;
import uk.co.thinkofdeath.patchtools.wrappers.ClassWrapper;
import uk.co.thinkofdeath.patchtools.wrappers.MethodWrapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PatchClass {

    private Ident ident;
    private Mode mode;

    private List<PatchMethod> methods = new ArrayList<>();

    public PatchClass(Command clCommand, BufferedReader reader) throws IOException {
        if (clCommand.args.length != 1) throw new IllegalArgumentException();
        ident = new Ident(clCommand.args[0]);
        mode = clCommand.mode;
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.startsWith("//") || line.length() == 0) continue;

            Command command = Command.from(line);
            if (mode == Mode.ADD && command.mode != Mode.ADD) {
                throw new IllegalArgumentException("In added classes everything must be +");
            } else if (mode == Mode.REMOVE && command.mode != Mode.REMOVE) {
                throw new IllegalArgumentException("In removed classes everything must be -");
            }
            switch (command.name) {
                case "method":
                    methods.add(new PatchMethod(this, command, reader));
                    break;
                case "end-class":
                    return;
                default:
                    throw new IllegalArgumentException(command.toString());
            }
        }
    }

    public Ident getIdent() {
        return ident;
    }

    public Mode getMode() {
        return mode;
    }

    public List<PatchMethod> getMethods() {
        return methods;
    }

    public void apply(PatchScope scope, ClassSet classSet) {
        if (mode == Mode.ADD) throw new UnsupportedOperationException("NYI");
        ClassWrapper classWrapper = scope.getClass(ident.getName());

        methods.forEach(m -> {
            if (m.getMode() == Mode.ADD) throw new UnsupportedOperationException("NYI");

            MethodWrapper methodWrapper = scope.getMethod(classWrapper, m.getIdent().getName());

            m.apply(classSet, scope, methodWrapper);
        });
    }

    public void check(PatchScope scope, ClassSet classSet) {
        if (mode == Mode.ADD) return;
        ClassWrapper classWrapper = scope.getClass(ident.getName());
        if (!ident.isWeak() && !classWrapper.getNode().name.equals(ident.getName())) {
            throw new PatchVerifyException();
        }

        methods.forEach(m -> {
            if (m.getMode() == Mode.ADD) return;

            MethodWrapper methodWrapper = scope.getMethod(classWrapper, m.getIdent().getName());
            if (!m.getIdent().isWeak()
                    && !methodWrapper.getNode().name.equals(m.getIdent().getName())) {
                throw new PatchVerifyException();
            }

            Type patchDesc = m.getDesc();
            Type desc = Type.getMethodType(methodWrapper.getNode().desc);

            if (patchDesc.getArgumentTypes().length != desc.getArgumentTypes().length) {
                throw new PatchVerifyException();
            }

            for (int i = 0; i < patchDesc.getArgumentTypes().length; i++) {
                Type pt = patchDesc.getArgumentTypes()[i];
                Type t = desc.getArgumentTypes()[i];

                checkTypes(classSet, scope, pt, t);
            }

            checkTypes(classSet, scope, patchDesc.getReturnType(), desc.getReturnType());

            m.checkInstructions(classSet, scope, methodWrapper);
        });
    }

    private void checkTypes(ClassSet classSet, PatchScope scope, Type pt, Type t) {
        if (pt.getSort() != t.getSort()) {
            throw new PatchVerifyException();
        }

        if (pt.getSort() == Type.OBJECT) {
            Ident id = new Ident(pt.getInternalName());
            String cls = id.getName();
            if (id.isWeak()) {
                ClassWrapper ptcls = scope.getClass(cls);
                if (ptcls == null) { // Assume true
                    cls = t.getInternalName();
                    scope.putClass(classSet.getClassWrapper(cls), cls);
                    return;
                }
                cls = ptcls.getNode().name;
            }
            if (!cls.equals(t.getInternalName())) {
                throw new PatchVerifyException(cls + " : " + t.getInternalName());
            }
        } else if (pt.getSort() == Type.ARRAY) {
            Ident id = new Ident(pt.getElementType().getInternalName());
            String cls = id.getName();
            if (id.isWeak()) {
                ClassWrapper ptcls = scope.getClass(cls);
                cls = ptcls.getNode().name;
            }
            if (!cls.equals(t.getElementType().getInternalName())) {
                throw new PatchVerifyException();
            }
        } else {
            if (!pt.equals(t)) {
                throw new PatchVerifyException();
            }
        }
    }
}
