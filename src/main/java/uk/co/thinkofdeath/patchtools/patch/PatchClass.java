package uk.co.thinkofdeath.patchtools.patch;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;
import uk.co.thinkofdeath.patchtools.PatchScope;
import uk.co.thinkofdeath.patchtools.PatchVerifyException;
import uk.co.thinkofdeath.patchtools.wrappers.ClassSet;
import uk.co.thinkofdeath.patchtools.wrappers.ClassWrapper;
import uk.co.thinkofdeath.patchtools.wrappers.MethodWrapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// TODO: Interfaces
public class PatchClass {

    private Ident ident;
    private Mode mode;
    private List<Command> superCommands = new ArrayList<>();
    private List<Command> interfaceCommands = new ArrayList<>();

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
                case "extends":
                    if (command.args.length != 1) throw new IllegalArgumentException("extends requires 1 parameter");
                    superCommands.add(command);
                    break;
                case "interface":
                    if (command.args.length != 1) throw new IllegalArgumentException("interface requires 1 parameter");
                    interfaceCommands.add(command);
                    break;
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

        for (Command superCommand : superCommands) {
            if (superCommand.mode == Mode.ADD) {
                Ident name = new Ident(superCommand.args[0]);
                String clName = name.getName();
                if (name.isWeak()) {
                    ClassWrapper cl = scope.getClass(clName);
                    if (cl == null) throw new IllegalStateException();
                    clName = cl.getNode().name;
                }
                classWrapper.getNode().superName = clName;
            } else if (superCommand.mode == Mode.REMOVE) {
                Ident name = new Ident(superCommand.args[0]);
                String clName = name.getName();
                if (name.isWeak()) {
                    ClassWrapper cl = scope.getClass(clName);
                    if (cl == null) throw new IllegalStateException();
                    clName = cl.getNode().name;
                }
                if (clName.equals("*") || clName.equals(classWrapper.getNode().superName)) {
                    classWrapper.getNode().superName = "java/lang/Object";
                }
            }
        }

        interLoop:
        for (Command interCommand : interfaceCommands) {
            if (interCommand.mode != Mode.ADD) {
                Ident name = new Ident(interCommand.args[0]);
                String clName = name.getName();
                if (clName.equals("*") && classWrapper.getNode().interfaces.size() > 0) {
                    continue;
                }
                for (String inter : classWrapper.getNode().interfaces) {
                    if (name.isWeak()) {
                        ClassWrapper cl = scope.getClass(clName);
                        if (cl == null) {
                            throw new RuntimeException();
                        }
                        clName = cl.getNode().name;
                    }
                    if (clName.equals(inter)) {
                        continue interLoop;
                    }
                }
                throw new RuntimeException();
            } else {
                Ident name = new Ident(interCommand.args[0]);
                String clName = name.getName();
                if (name.isWeak()) {
                    ClassWrapper cl = scope.getClass(clName);
                    if (cl == null) {
                        throw new RuntimeException();
                    }
                    clName = cl.getNode().name;
                }
                classWrapper.getNode().interfaces.add(clName);
            }
        }

        methods.forEach(m -> {
            if (m.getMode() == Mode.ADD) {
                StringBuilder mappedDesc = new StringBuilder("(");
                Type desc = m.getDesc();
                for (Type type : desc.getArgumentTypes()) {
                    updatedTypeString(classSet, scope, mappedDesc, type);
                }
                mappedDesc.append(")");
                updatedTypeString(classSet, scope, mappedDesc, desc.getReturnType());

                int access = (m.isPrivate() ? Opcodes.ACC_PRIVATE : Opcodes.ACC_PUBLIC)
                        | (m.isStatic() ? Opcodes.ACC_STATIC : 0);
                MethodWrapper methodWrapper = null;

                if (((access & Opcodes.ACC_PUBLIC) != 0
                        || (access & Opcodes.ACC_PROTECTED) != 0)
                        && (access & Opcodes.ACC_STATIC) == 0) {
                    methodWrapper = searchParent(classSet, scope, classWrapper, m.getIdent(), mappedDesc.toString());
                }

                String name = methodWrapper == null ? m.getIdent().getName() : methodWrapper.getName();

                MethodNode node = new MethodNode(Opcodes.ASM5,
                        access,
                        name,
                        mappedDesc.toString(),
                        null, null);
                if (methodWrapper == null) {
                    methodWrapper = new MethodWrapper(classWrapper, node);
                } else {
                    methodWrapper.add(classWrapper);
                }
                scope.putMethod(methodWrapper, m.getIdent().getName(), m.getDesc().getDescriptor());
                classWrapper.getMethods().add(methodWrapper);
                classWrapper.getNode().methods.add(node);
            }

            MethodWrapper methodWrapper = scope.getMethod(classWrapper, m.getIdent().getName(), m.getDesc().getDescriptor());

            m.apply(classSet, scope, classWrapper.getMethodNode(methodWrapper));
        });
    }

    private MethodWrapper searchParent(ClassSet classSet, PatchScope scope,
                                       ClassWrapper classWrapper, Ident name, String desc) {
        if (classWrapper == null) {
            return null;
        }
        MethodWrapper mw = null;

        try {
            String pName = name.getName();
            if (name.isWeak()) {
                MethodWrapper wrapper = scope.getMethod(classWrapper, pName, desc);
                if (wrapper == null) throw new IllegalStateException();
                pName = wrapper.getName();
            }
            StringBuilder mappedDesc = new StringBuilder("(");
            Type pDesc = Type.getMethodType(desc);
            for (Type type : pDesc.getArgumentTypes()) {
                updatedTypeString(classSet, scope, mappedDesc, type);
            }
            mappedDesc.append(")");
            updatedTypeString(classSet, scope, mappedDesc, pDesc.getReturnType());

            final String finalPName = pName;
            mw = Arrays.stream(classWrapper.getMethods(true))
                    .filter(m -> m.getName().equals(finalPName) && m.getDesc().equals(mappedDesc.toString()))
                    .findFirst().orElse(null);
        } catch (IllegalStateException ignored) {

        }
        if (mw == null) {
            mw = searchParent(classSet, scope, classSet.getClassWrapper(classWrapper.getNode().superName), name, desc);
            if (mw == null) {
                for (String inter : classWrapper.getNode().interfaces) {
                    mw = searchParent(classSet, scope, classSet.getClassWrapper(inter), name, desc);
                    if (mw != null) {
                        break;
                    }
                }
            }
        }
        return mw;
    }

    static void updatedTypeString(ClassSet classSet, PatchScope scope, StringBuilder builder, Type type) {
        if (type.getSort() == Type.OBJECT) {
            builder.append("L");
            Ident id = new Ident(type.getInternalName());
            String cls = id.getName();
            if (id.isWeak()) {
                ClassWrapper ptcls = scope.getClass(cls);
                cls = ptcls.getNode().name;
            }
            builder.append(cls);
            builder.append(";");
        } else if (type.getSort() == Type.ARRAY) {
            builder.append("[");
            updatedTypeString(classSet, scope, builder, type.getElementType());
        } else {
            builder.append(type.getDescriptor());
        }
    }

    public void check(PatchScope scope, ClassSet classSet) {
        if (mode == Mode.ADD) return;
        ClassWrapper classWrapper = scope.getClass(ident.getName());
        if (!ident.isWeak() && !classWrapper.getNode().name.equals(ident.getName())) {
            throw new PatchVerifyException();
        }


        for (Command superCommand : superCommands) {
            if (superCommand.mode != Mode.ADD) {
                Ident name = new Ident(superCommand.args[0]);
                String clName = name.getName();
                if (name.isWeak()) {
                    ClassWrapper cl = scope.getClass(clName);
                    if (cl == null) {
                        cl = classSet.getClassWrapper(classWrapper.getNode().superName);
                        scope.putClass(cl, clName);
                    }
                    clName = cl.getNode().name;
                }
                if (!clName.equals("*") && !clName.equals(classWrapper.getNode().superName)) {
                    throw new PatchVerifyException();
                }
            }
        }

        interLoop:
        for (Command interCommand : interfaceCommands) {
            if (interCommand.mode != Mode.ADD) {
                Ident name = new Ident(interCommand.args[0]);
                String clName = name.getName();
                if (clName.equals("*") && classWrapper.getNode().interfaces.size() > 0) {
                    continue;
                }
                for (String inter : classWrapper.getNode().interfaces) {
                    if (name.isWeak()) {
                        ClassWrapper cl = scope.getClass(clName);
                        if (cl == null) {
                            cl = classSet.getClassWrapper(inter);
                            scope.putClass(cl, clName);
                        }
                        clName = cl.getNode().name;
                    }
                    if (clName.equals(inter)) {
                        continue interLoop;
                    }
                }
                throw new PatchVerifyException();
            }
        }

        methods.forEach(m -> {
            if (m.getMode() == Mode.ADD) return;

            MethodWrapper methodWrapper = scope.getMethod(classWrapper,
                    m.getIdent().getName(),
                    m.getDesc().getDescriptor());
            if (!m.getIdent().isWeak()
                    && !methodWrapper.getName().equals(m.getIdent().getName())) {
                throw new PatchVerifyException();
            }

            Type patchDesc = m.getDesc();
            Type desc = Type.getMethodType(methodWrapper.getDesc());

            if (patchDesc.getArgumentTypes().length != desc.getArgumentTypes().length) {
                throw new PatchVerifyException();
            }

            for (int i = 0; i < patchDesc.getArgumentTypes().length; i++) {
                Type pt = patchDesc.getArgumentTypes()[i];
                Type t = desc.getArgumentTypes()[i];

                checkTypes(classSet, scope, pt, t);
            }

            checkTypes(classSet, scope, patchDesc.getReturnType(), desc.getReturnType());

            m.checkInstructions(classSet, scope, classWrapper.getMethodNode(methodWrapper));
        });
    }

    static void checkTypes(ClassSet classSet, PatchScope scope, Type pt, Type t) {
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
                throw new PatchVerifyException("'" + cls + "' : '" + t.getInternalName() + "'");
            }
        } else if (pt.getSort() == Type.ARRAY) {
            checkTypes(classSet, scope, pt.getElementType(), t.getElementType());
        } else {
            if (!pt.equals(t)) {
                throw new PatchVerifyException();
            }
        }
    }
}
