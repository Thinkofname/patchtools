/*
 * Copyright 2014 Matthew Collins
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.co.thinkofdeath.patchtools.instruction;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;
import uk.co.thinkofdeath.patchtools.instruction.instructions.*;

public enum Instruction {
    ANY(null), // Virtual instruction
    LABEL(new LabelInstruction()), // Virtual instruction
    NOP(new SingleInstruction(Opcodes.NOP)),
    PUSH_NULL(new SingleInstruction(Opcodes.ACONST_NULL)),
    PUSH_INT(new PushIntInstruction()), // Virtual instruction
    PUSH_LONG(new PushLongInstruction()), // Virtual instruction
    PUSH_FLOAT(new PushFloatInstruction()), // Virtual instruction
    PUSH_DOUBLE(new PushDoubleInstruction()), // Virtual instruction
    // iconst_m1 (push-int)
    // iconst_0 (push-int)
    // iconst_1 (push-int)
    // iconst_2 (push-int)
    // iconst_3 (push-int)
    // iconst_4 (push-int)
    // iconst_5 (push-int)
    // lconst_0 (push-long)
    // lconst_1 (push-long)
    // fconst_0 (push-float)
    // fconst_1 (push-float)
    // fconst_2 (push-float)
    // dconst_0 (push-double)
    // dconst_1 (push-double)
    // bipush (push-int)
    // sipush (push-int)
    PUSH_STRING(new PushStringInstruction()), // Virtual instruction (ldc)
    PUSH_CLASS(new PushClassInstruction()), // Virtual instruction (ldc)
    // ldc (push-*)
    // ldc_w (push-*)
    // ldc2_w (push-*)
    LOAD_INT(new VarInstruction(Opcodes.ILOAD)),
    LOAD_LONG(new VarInstruction(Opcodes.LLOAD)),
    LOAD_FLOAT(new VarInstruction(Opcodes.FLOAD)),
    LOAD_DOUBLE(new VarInstruction(Opcodes.DLOAD)),
    LOAD_OBJECT(new VarInstruction(Opcodes.ALOAD)),
    // iload_0 (load-int)
    // iload_1 (load-int)
    // iload_2 (load-int)
    // iload_3 (load-int)
    // lload_0 (load-long)
    // lload_1 (load-long)
    // lload_2 (load-long)
    // lload_3 (load-long)
    // fload_0 (load-float)
    // fload_1 (load-float)
    // fload_2 (load-float)
    // fload_3 (load-float)
    // dload_0 (load-double)
    // dload_1 (load-double)
    // dload_2 (load-double)
    // dload_3 (load-double)
    // aload_0 (load-object)
    // aload_1 (load-object)
    // aload_2 (load-object)
    // aload_3 (load-object)
    ARRAY_LOAD_INT(new SingleInstruction(Opcodes.IALOAD)),
    ARRAY_LOAD_LONG(new SingleInstruction(Opcodes.LALOAD)),
    ARRAY_LOAD_FLOAT(new SingleInstruction(Opcodes.FALOAD)),
    ARRAY_LOAD_DOUBLE(new SingleInstruction(Opcodes.DALOAD)),
    ARRAY_LOAD_OBJECT(new SingleInstruction(Opcodes.AALOAD)),
    ARRAY_LOAD_BYTE(new SingleInstruction(Opcodes.BALOAD)),
    ARRAY_LOAD_CHAR(new SingleInstruction(Opcodes.CALOAD)),
    ARRAY_LOAD_SHORT(new SingleInstruction(Opcodes.SALOAD)),
    STORE_INT(new VarInstruction(Opcodes.ISTORE)),
    STORE_LONG(new VarInstruction(Opcodes.LSTORE)),
    STORE_FLOAT(new VarInstruction(Opcodes.FSTORE)),
    STORE_DOUBLE(new VarInstruction(Opcodes.DSTORE)),
    STORE_OBJECT(new VarInstruction(Opcodes.ASTORE)),
    // istore_0 (store-int)
    // istore_1 (store-int)
    // istore_2 (store-int)
    // istore_3 (store-int)
    // lstore_0 (store-long)
    // lstore_1 (store-long)
    // lstore_2 (store-long)
    // lstore_3 (store-long)
    // fstore_0 (store-float)
    // fstore_1 (store-float)
    // fstore_2 (store-float)
    // fstore_3 (store-float)
    // dstore_0 (store-double)
    // dstore_1 (store-double)
    // dstore_2 (store-double)
    // dstore_3 (store-double)
    // astore_0 (store-object)
    // astore_1 (store-object)
    // astore_2 (store-object)
    // astore_3 (store-object)
    ARRAY_STORE_INT(new SingleInstruction(Opcodes.IASTORE)),
    ARRAY_STORE_LONG(new SingleInstruction(Opcodes.LASTORE)),
    ARRAY_STORE_FLOAT(new SingleInstruction(Opcodes.FASTORE)),
    ARRAY_STORE_DOUBLE(new SingleInstruction(Opcodes.DASTORE)),
    ARRAY_STORE_OBJECT(new SingleInstruction(Opcodes.AASTORE)),
    ARRAY_STORE_BYTE(new SingleInstruction(Opcodes.BASTORE)),
    ARRAY_STORE_CHAR(new SingleInstruction(Opcodes.CASTORE)),
    ARRAY_STORE_SHORT(new SingleInstruction(Opcodes.SASTORE)),
    POP(new SingleInstruction(Opcodes.POP)),
    POP2(new SingleInstruction(Opcodes.POP2)),
    DUP(new SingleInstruction(Opcodes.DUP)),
    DUP_X1(new SingleInstruction(Opcodes.DUP_X1)),
    DUP_X2(new SingleInstruction(Opcodes.DUP_X2)),
    DUP2(new SingleInstruction(Opcodes.DUP2)),
    DUP2_X1(new SingleInstruction(Opcodes.DUP2_X1)),
    DUP2_X2(new SingleInstruction(Opcodes.DUP2_X2)),
    SWAP(new SingleInstruction(Opcodes.SWAP)),
    ADD_INT(new SingleInstruction(Opcodes.IADD)),
    ADD_LONG(new SingleInstruction(Opcodes.LADD)),
    ADD_FLOAT(new SingleInstruction(Opcodes.FADD)),
    ADD_DOUBLE(new SingleInstruction(Opcodes.DADD)),
    SUB_INT(new SingleInstruction(Opcodes.ISUB)),
    SUB_LONG(new SingleInstruction(Opcodes.LSUB)),
    SUB_FLOAT(new SingleInstruction(Opcodes.FSUB)),
    SUB_DOUBLE(new SingleInstruction(Opcodes.DSUB)),
    MUL_INT(new SingleInstruction(Opcodes.IMUL)),
    MUL_LONG(new SingleInstruction(Opcodes.LMUL)),
    MUL_FLOAT(new SingleInstruction(Opcodes.FMUL)),
    MUL_DOUBLE(new SingleInstruction(Opcodes.DMUL)),
    DIV_INT(new SingleInstruction(Opcodes.IDIV)),
    DIV_LONG(new SingleInstruction(Opcodes.LDIV)),
    DIV_FLOAT(new SingleInstruction(Opcodes.FDIV)),
    DIV_DOUBLE(new SingleInstruction(Opcodes.DDIV)),
    REM_INT(new SingleInstruction(Opcodes.IREM)),
    REM_LONG(new SingleInstruction(Opcodes.LREM)),
    REM_FLOAT(new SingleInstruction(Opcodes.FREM)),
    REM_DOUBLE(new SingleInstruction(Opcodes.DREM)),
    NEG_INT(new SingleInstruction(Opcodes.INEG)),
    NEG_LONG(new SingleInstruction(Opcodes.LNEG)),
    NEG_FLOAT(new SingleInstruction(Opcodes.FNEG)),
    NEG_DOUBLE(new SingleInstruction(Opcodes.DNEG)),
    SHIFT_LEFT_INT(new SingleInstruction(Opcodes.ISHL)),
    SHIFT_LEFT_LONG(new SingleInstruction(Opcodes.LSHL)),
    SHIFT_RIGHT_INT(new SingleInstruction(Opcodes.ISHR)),
    SHIFT_RIGHT_LONG(new SingleInstruction(Opcodes.LSHR)),
    USHIFT_RIGHT_INT(new SingleInstruction(Opcodes.IUSHR)),
    USHIFT_RIGHT_LONG(new SingleInstruction(Opcodes.LUSHR)),
    AND_INT(new SingleInstruction(Opcodes.IAND)),
    AND_LONG(new SingleInstruction(Opcodes.LAND)),
    OR_INT(new SingleInstruction(Opcodes.IOR)),
    OR_LONG(new SingleInstruction(Opcodes.LOR)),
    XOR_INT(new SingleInstruction(Opcodes.IXOR)),
    XOR_LONG(new SingleInstruction(Opcodes.LXOR)),
    INC_INT(new IntIncInstruction()),
    CONVERT_INT_LONG(new SingleInstruction(Opcodes.I2L)),
    CONVERT_INT_FLOAT(new SingleInstruction(Opcodes.I2F)),
    CONVERT_INT_DOUBLE(new SingleInstruction(Opcodes.I2D)),
    CONVERT_LONG_INT(new SingleInstruction(Opcodes.L2I)),
    CONVERT_LONG_FLOAT(new SingleInstruction(Opcodes.L2F)),
    CONVERT_LONG_DOUBLE(new SingleInstruction(Opcodes.L2D)),
    CONVERT_FLOAT_INT(new SingleInstruction(Opcodes.F2I)),
    CONVERT_FLOAT_LONG(new SingleInstruction(Opcodes.F2L)),
    CONVERT_FLOAT_DOUBLE(new SingleInstruction(Opcodes.F2D)),
    CONVERT_DOUBLE_INT(new SingleInstruction(Opcodes.D2I)),
    CONVERT_DOUBLE_LONG(new SingleInstruction(Opcodes.D2L)),
    CONVERT_DOUBLE_FLOAT(new SingleInstruction(Opcodes.D2F)),
    CONVERT_INT_BYTE(new SingleInstruction(Opcodes.I2B)),
    CONVERT_INT_CHAR(new SingleInstruction(Opcodes.I2C)),
    CONVERT_INT_SHORT(new SingleInstruction(Opcodes.I2S)),
    COMPARE_LONG(new SingleInstruction(Opcodes.LCMP)),
    COMPARE_FLOAT(new SingleInstruction(Opcodes.FCMPL)),
    COMPARE_FLOAT_INV(new SingleInstruction(Opcodes.FCMPG)),
    COMPARE_DOUBLE(new SingleInstruction(Opcodes.DCMPL)),
    COMPARE_DOUBLE_INV(new SingleInstruction(Opcodes.DCMPG)),
    IF_ZERO(new JumpInstruction(Opcodes.IFEQ)),
    IF_NOT_ZERO(new JumpInstruction(Opcodes.IFNE)),
    IF_LESS_ZERO(new JumpInstruction(Opcodes.IFLT)),
    IF_GREATER_EQUAL_ZERO(new JumpInstruction(Opcodes.IFGE)),
    IF_GREATER_ZERO(new JumpInstruction(Opcodes.IFGT)),
    IF_LESS_EQUAL_ZERO(new JumpInstruction(Opcodes.IFLE)),
    IF_EQUAL_INT(new JumpInstruction(Opcodes.IF_ICMPEQ)),
    IF_NOT_EQUAL_INT(new JumpInstruction(Opcodes.IF_ICMPNE)),
    IF_LESS_INT(new JumpInstruction(Opcodes.IF_ICMPLT)),
    IF_GREATER_EQUAL_INT(new JumpInstruction(Opcodes.IF_ICMPGE)),
    IF_GREATER_INT(new JumpInstruction(Opcodes.IF_ICMPGT)),
    IF_LESS_EQUAL_INT(new JumpInstruction(Opcodes.IF_ICMPLE)),
    IF_EQUAL_OBJECT(new JumpInstruction(Opcodes.IF_ACMPEQ)),
    IF_NOT_EQUAL_OBJECT(new JumpInstruction(Opcodes.IF_ACMPNE)),
    GOTO(new JumpInstruction(Opcodes.GOTO)),
    JSR(new JumpInstruction(Opcodes.JSR)),
    RET(new VarInstruction(Opcodes.RET)),
    SWITCH_TABLE(new TableSwitchInstruction(), true),
    SWITCH_LOOKUP(new LookupSwitchInstruction(), true),
    // ireturn (return)
    // lreturn (return)
    // freturn (return)
    // dreturn (return)
    // areturn (return)
    RETURN(new ReturnInstruction()),
    GET_STATIC(new FieldInstruction(Opcodes.GETSTATIC)),
    PUT_STATIC(new FieldInstruction(Opcodes.PUTSTATIC)),
    GET_FIELD(new FieldInstruction(Opcodes.GETFIELD)),
    PUT_FIELD(new FieldInstruction(Opcodes.PUTFIELD)),
    INVOKE_VIRTUAL(new InvokeInstruction(Opcodes.INVOKEVIRTUAL)),
    INVOKE_SPECIAL(new InvokeInstruction(Opcodes.INVOKESPECIAL)),
    INVOKE_STATIC(new InvokeInstruction(Opcodes.INVOKESTATIC)),
    INVOKE_INTERFACE(new InvokeInstruction(Opcodes.INVOKEINTERFACE)),
    // TODO: invokedynamic (invoke-dynamic)
    NEW(new TypeInstruction(Opcodes.NEW)),
    NEW_ARRAY(new ArrayInstruction()),
    // anewarray (new-array)
    LENGTH_ARRAY(new SingleInstruction(Opcodes.ARRAYLENGTH)),
    THROW(new SingleInstruction(Opcodes.ATHROW)),
    CHECK_CAST(new TypeInstruction(Opcodes.CHECKCAST)),
    INSTANCE_OF(new TypeInstruction(Opcodes.INSTANCEOF)),
    MONITOR_ENTER(new SingleInstruction(Opcodes.MONITORENTER)),
    MONITOR_EXIT(new SingleInstruction(Opcodes.MONITOREXIT)),
    // wide
    NEW_ARRAY_MULTI(new MultiArrayInstruction()),
    IF_NULL(new JumpInstruction(Opcodes.IFNULL)),
    IF_NOT_NULL(new JumpInstruction(Opcodes.IFNONNULL)),
    // goto_w
    // jsr_w
    // breakpoint
    ;

    private final InstructionHandler handler;
    private final boolean requiresMeta;

    Instruction(InstructionHandler handler) {
        this.handler = handler;
        requiresMeta = false;
    }

    Instruction(InstructionHandler handler, boolean requiresMeta) {
        this.handler = handler;
        this.requiresMeta = requiresMeta;
    }

    public InstructionHandler getHandler() {
        return handler;
    }

    public boolean isMetaRequired() {
        return requiresMeta;
    }

    public static boolean print(StringBuilder patch, MethodNode method, AbstractInsnNode insn) {
        for (Instruction i : values()) {
            if (i.getHandler() != null
                && i.getHandler().print(i, patch, method, insn)) {
                return true;
            }
        }
        return false;
    }
}
