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
    // ldc (push-*)
    // ldc_w (push-*)
    // ldc2_w (push-*)
    // TODO: iload (load-int)
    // TODO: lload (load-long)
    // TODO: ffoad (load-float)
    // TODO: dfoad (load-double)
    // TODO: afoad (load-object)
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
    // TODO: iaload (array-load-int)
    // TODO: laload (array-load-long)
    // TODO: faload (array-load-float)
    // TODO: daload (array-load-double)
    // TODO: aaload (array-load-object)
    // TODO: baload (array-load-byte)
    // TODO: caload (array-load-char)
    // TODO: saload (array-load-short)
    // TODO: istore (store-int)
    // TODO: lstore (store-long)
    // TODO: fstore (store-float)
    // TODO: dstore (store-double)
    // TODO: astore (store-object)
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
    // TODO: iastore (array-store-int)
    // TODO: lastore (array-store-long)
    // TODO: fastore (array-store-float)
    // TODO: dastore (array-store-double)
    // TODO: aastore (array-store-object)
    // TODO: bastore (array-store-byte)
    // TODO: castore (array-store-char)
    // TODO: sastore (array-store-short)
    // TODO: pop (pop)
    // pop2
    // TODO: dup (dup)
    // TODO: dup_x1 (dup_x)
    // dup_x2 (dup_x)
    // dup2 (dup)
    // TODO: dup2_x1 (dup2_x)
    // dup2_x2 (dup2_x)
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
    REM_DOULBE(new SingleInstruction(Opcodes.DREM)),
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
    // TODO: iinc (inc)
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
    // TODO: fcmpl (compare-float)?
    // TODO: fcmpg (compare-float)?
    // TODO: dcmpl (compare-long)?
    // TODO: dcmpg (compare-long)?
    // TODO: lfeq (if-zero)
    // TODO: lfne (if-not-zero)
    // TODO: iflt (if-less-zero)
    // TODO: ifge (if-greater-equal-zero)
    // TODO: ifgt (if-greater-zero)
    // TODO: ifle (if-less-equal-zero)
    // TODO: if_icmpeq (if-equal-int)
    // TODO: if_icmpne (if-not-equal-int)
    // TODO: if_icmplt (if-less-int)
    // TODO: if_icmpge (if-greater-equal-int)
    // TODO: if_icmpgt (if-greater-int)
    // TODO: if_icmple (if-less-equal-int)
    // TODO: if_acmpeq (if-equal-object)
    // TODO: if_acmpne (if-not-equal-object)
    // TODO: jsr (goto)
    // TODO: ret (ret)
    // TODO: tableswitch (switch-table)
    // TODO: lookupswitch (switch-lookup)
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
    // TODO: new (new)
    // TODO: newarray (new-array)
    // anewarray (new-array)
    // TODO: arraylength (length-array)
    // TODO: athrow (throw)
    // TODO: checkcast (check-cast)
    // TODO: instanceof (instance-of)
    // TODO: monitorenter (monitor-enter)
    // TODO: monitorexit (monitor-exit)
    // wide
    // TODO: multianewarray (new-array-mutli)
    // TODO: ifnull (if-null)
    // TODO: ifnonnull (if-non-null)
    // goto_w
    // jsr_w
    // breakpoint
    ;

    private final InstructionHandler handler;

    Instruction(InstructionHandler handler) {
        this.handler = handler;
    }

    public InstructionHandler getHandler() {
        return handler;
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
