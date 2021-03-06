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

package uk.co.thinkofdeath.patchtools.instruction

import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.Opcodes
import uk.co.thinkofdeath.patchtools.instruction.instructions.*
import kotlin.platform.platformStatic
import uk.co.thinkofdeath.patchtools.instruction.Instruction.values

private val _intellijSucks = javaClass<LabelInstruction>();
private val _intellijOptimizesImportsBadlyForKotlin = Opcodes.AALOAD

public enum class Instruction(
    public val handler: InstructionHandler?,
    private val requiresMeta: Boolean = false
) {
    ANY : Instruction(null)
    // Virtual instruction
    LABEL : Instruction(LabelInstruction())
    // Virtual instruction
    NOP : Instruction(SingleInstruction(Opcodes.NOP))
    PUSH_NULL : Instruction(SingleInstruction(Opcodes.ACONST_NULL))
    PUSH_INT : Instruction(PushIntInstruction())
    // Virtual instruction
    PUSH_LONG : Instruction(PushLongInstruction())
    // Virtual instruction
    PUSH_FLOAT : Instruction(PushFloatInstruction())
    // Virtual instruction
    PUSH_DOUBLE : Instruction(PushDoubleInstruction())
    // Virtual instruction
    TRY_CATCH : Instruction(TryCatchInstruction())
    // Virtual instruction
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
    PUSH_STRING : Instruction(PushStringInstruction())
    // Virtual instruction (ldc)
    PUSH_CLASS : Instruction(PushClassInstruction())
    // Virtual instruction (ldc)
    // ldc (push-*)
    // ldc_w (push-*)
    // ldc2_w (push-*)
    LOAD_INT : Instruction(VarInstruction(Opcodes.ILOAD))
    LOAD_LONG : Instruction(VarInstruction(Opcodes.LLOAD))
    LOAD_FLOAT : Instruction(VarInstruction(Opcodes.FLOAD))
    LOAD_DOUBLE : Instruction(VarInstruction(Opcodes.DLOAD))
    LOAD_OBJECT : Instruction(VarInstruction(Opcodes.ALOAD))
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
    ARRAY_LOAD_INT : Instruction(SingleInstruction(Opcodes.IALOAD))
    ARRAY_LOAD_LONG : Instruction(SingleInstruction(Opcodes.LALOAD))
    ARRAY_LOAD_FLOAT : Instruction(SingleInstruction(Opcodes.FALOAD))
    ARRAY_LOAD_DOUBLE : Instruction(SingleInstruction(Opcodes.DALOAD))
    ARRAY_LOAD_OBJECT : Instruction(SingleInstruction(Opcodes.AALOAD))
    ARRAY_LOAD_BYTE : Instruction(SingleInstruction(Opcodes.BALOAD))
    ARRAY_LOAD_CHAR : Instruction(SingleInstruction(Opcodes.CALOAD))
    ARRAY_LOAD_SHORT : Instruction(SingleInstruction(Opcodes.SALOAD))
    STORE_INT : Instruction(VarInstruction(Opcodes.ISTORE))
    STORE_LONG : Instruction(VarInstruction(Opcodes.LSTORE))
    STORE_FLOAT : Instruction(VarInstruction(Opcodes.FSTORE))
    STORE_DOUBLE : Instruction(VarInstruction(Opcodes.DSTORE))
    STORE_OBJECT : Instruction(VarInstruction(Opcodes.ASTORE))
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
    ARRAY_STORE_INT : Instruction(SingleInstruction(Opcodes.IASTORE))
    ARRAY_STORE_LONG : Instruction(SingleInstruction(Opcodes.LASTORE))
    ARRAY_STORE_FLOAT : Instruction(SingleInstruction(Opcodes.FASTORE))
    ARRAY_STORE_DOUBLE : Instruction(SingleInstruction(Opcodes.DASTORE))
    ARRAY_STORE_OBJECT : Instruction(SingleInstruction(Opcodes.AASTORE))
    ARRAY_STORE_BYTE : Instruction(SingleInstruction(Opcodes.BASTORE))
    ARRAY_STORE_CHAR : Instruction(SingleInstruction(Opcodes.CASTORE))
    ARRAY_STORE_SHORT : Instruction(SingleInstruction(Opcodes.SASTORE))
    POP : Instruction(SingleInstruction(Opcodes.POP))
    POP2 : Instruction(SingleInstruction(Opcodes.POP2))
    DUP : Instruction(SingleInstruction(Opcodes.DUP))
    DUP_X1 : Instruction(SingleInstruction(Opcodes.DUP_X1))
    DUP_X2 : Instruction(SingleInstruction(Opcodes.DUP_X2))
    DUP2 : Instruction(SingleInstruction(Opcodes.DUP2))
    DUP2_X1 : Instruction(SingleInstruction(Opcodes.DUP2_X1))
    DUP2_X2 : Instruction(SingleInstruction(Opcodes.DUP2_X2))
    SWAP : Instruction(SingleInstruction(Opcodes.SWAP))
    ADD_INT : Instruction(SingleInstruction(Opcodes.IADD))
    ADD_LONG : Instruction(SingleInstruction(Opcodes.LADD))
    ADD_FLOAT : Instruction(SingleInstruction(Opcodes.FADD))
    ADD_DOUBLE : Instruction(SingleInstruction(Opcodes.DADD))
    SUB_INT : Instruction(SingleInstruction(Opcodes.ISUB))
    SUB_LONG : Instruction(SingleInstruction(Opcodes.LSUB))
    SUB_FLOAT : Instruction(SingleInstruction(Opcodes.FSUB))
    SUB_DOUBLE : Instruction(SingleInstruction(Opcodes.DSUB))
    MUL_INT : Instruction(SingleInstruction(Opcodes.IMUL))
    MUL_LONG : Instruction(SingleInstruction(Opcodes.LMUL))
    MUL_FLOAT : Instruction(SingleInstruction(Opcodes.FMUL))
    MUL_DOUBLE : Instruction(SingleInstruction(Opcodes.DMUL))
    DIV_INT : Instruction(SingleInstruction(Opcodes.IDIV))
    DIV_LONG : Instruction(SingleInstruction(Opcodes.LDIV))
    DIV_FLOAT : Instruction(SingleInstruction(Opcodes.FDIV))
    DIV_DOUBLE : Instruction(SingleInstruction(Opcodes.DDIV))
    REM_INT : Instruction(SingleInstruction(Opcodes.IREM))
    REM_LONG : Instruction(SingleInstruction(Opcodes.LREM))
    REM_FLOAT : Instruction(SingleInstruction(Opcodes.FREM))
    REM_DOUBLE : Instruction(SingleInstruction(Opcodes.DREM))
    NEG_INT : Instruction(SingleInstruction(Opcodes.INEG))
    NEG_LONG : Instruction(SingleInstruction(Opcodes.LNEG))
    NEG_FLOAT : Instruction(SingleInstruction(Opcodes.FNEG))
    NEG_DOUBLE : Instruction(SingleInstruction(Opcodes.DNEG))
    SHIFT_LEFT_INT : Instruction(SingleInstruction(Opcodes.ISHL))
    SHIFT_LEFT_LONG : Instruction(SingleInstruction(Opcodes.LSHL))
    SHIFT_RIGHT_INT : Instruction(SingleInstruction(Opcodes.ISHR))
    SHIFT_RIGHT_LONG : Instruction(SingleInstruction(Opcodes.LSHR))
    USHIFT_RIGHT_INT : Instruction(SingleInstruction(Opcodes.IUSHR))
    USHIFT_RIGHT_LONG : Instruction(SingleInstruction(Opcodes.LUSHR))
    AND_INT : Instruction(SingleInstruction(Opcodes.IAND))
    AND_LONG : Instruction(SingleInstruction(Opcodes.LAND))
    OR_INT : Instruction(SingleInstruction(Opcodes.IOR))
    OR_LONG : Instruction(SingleInstruction(Opcodes.LOR))
    XOR_INT : Instruction(SingleInstruction(Opcodes.IXOR))
    XOR_LONG : Instruction(SingleInstruction(Opcodes.LXOR))
    INC_INT : Instruction(IntIncInstruction())
    CONVERT_INT_LONG : Instruction(SingleInstruction(Opcodes.I2L))
    CONVERT_INT_FLOAT : Instruction(SingleInstruction(Opcodes.I2F))
    CONVERT_INT_DOUBLE : Instruction(SingleInstruction(Opcodes.I2D))
    CONVERT_LONG_INT : Instruction(SingleInstruction(Opcodes.L2I))
    CONVERT_LONG_FLOAT : Instruction(SingleInstruction(Opcodes.L2F))
    CONVERT_LONG_DOUBLE : Instruction(SingleInstruction(Opcodes.L2D))
    CONVERT_FLOAT_INT : Instruction(SingleInstruction(Opcodes.F2I))
    CONVERT_FLOAT_LONG : Instruction(SingleInstruction(Opcodes.F2L))
    CONVERT_FLOAT_DOUBLE : Instruction(SingleInstruction(Opcodes.F2D))
    CONVERT_DOUBLE_INT : Instruction(SingleInstruction(Opcodes.D2I))
    CONVERT_DOUBLE_LONG : Instruction(SingleInstruction(Opcodes.D2L))
    CONVERT_DOUBLE_FLOAT : Instruction(SingleInstruction(Opcodes.D2F))
    CONVERT_INT_BYTE : Instruction(SingleInstruction(Opcodes.I2B))
    CONVERT_INT_CHAR : Instruction(SingleInstruction(Opcodes.I2C))
    CONVERT_INT_SHORT : Instruction(SingleInstruction(Opcodes.I2S))
    COMPARE_LONG : Instruction(SingleInstruction(Opcodes.LCMP))
    COMPARE_FLOAT : Instruction(SingleInstruction(Opcodes.FCMPL))
    COMPARE_FLOAT_INV : Instruction(SingleInstruction(Opcodes.FCMPG))
    COMPARE_DOUBLE : Instruction(SingleInstruction(Opcodes.DCMPL))
    COMPARE_DOUBLE_INV : Instruction(SingleInstruction(Opcodes.DCMPG))
    IF_ZERO : Instruction(JumpInstruction(Opcodes.IFEQ))
    IF_NOT_ZERO : Instruction(JumpInstruction(Opcodes.IFNE))
    IF_LESS_ZERO : Instruction(JumpInstruction(Opcodes.IFLT))
    IF_GREATER_EQUAL_ZERO : Instruction(JumpInstruction(Opcodes.IFGE))
    IF_GREATER_ZERO : Instruction(JumpInstruction(Opcodes.IFGT))
    IF_LESS_EQUAL_ZERO : Instruction(JumpInstruction(Opcodes.IFLE))
    IF_EQUAL_INT : Instruction(JumpInstruction(Opcodes.IF_ICMPEQ))
    IF_NOT_EQUAL_INT : Instruction(JumpInstruction(Opcodes.IF_ICMPNE))
    IF_LESS_INT : Instruction(JumpInstruction(Opcodes.IF_ICMPLT))
    IF_GREATER_EQUAL_INT : Instruction(JumpInstruction(Opcodes.IF_ICMPGE))
    IF_GREATER_INT : Instruction(JumpInstruction(Opcodes.IF_ICMPGT))
    IF_LESS_EQUAL_INT : Instruction(JumpInstruction(Opcodes.IF_ICMPLE))
    IF_EQUAL_OBJECT : Instruction(JumpInstruction(Opcodes.IF_ACMPEQ))
    IF_NOT_EQUAL_OBJECT : Instruction(JumpInstruction(Opcodes.IF_ACMPNE))
    GOTO : Instruction(JumpInstruction(Opcodes.GOTO))
    JSR : Instruction(JumpInstruction(Opcodes.JSR))
    RET : Instruction(VarInstruction(Opcodes.RET))
    SWITCH_TABLE : Instruction(TableSwitchInstruction(), true)
    SWITCH_LOOKUP : Instruction(LookupSwitchInstruction(), true)
    // ireturn (return)
    // lreturn (return)
    // freturn (return)
    // dreturn (return)
    // areturn (return)
    RETURN : Instruction(ReturnInstruction())
    GET_STATIC : Instruction(FieldInstruction(Opcodes.GETSTATIC))
    PUT_STATIC : Instruction(FieldInstruction(Opcodes.PUTSTATIC))
    GET_FIELD : Instruction(FieldInstruction(Opcodes.GETFIELD))
    PUT_FIELD : Instruction(FieldInstruction(Opcodes.PUTFIELD))
    INVOKE_VIRTUAL : Instruction(InvokeInstruction(Opcodes.INVOKEVIRTUAL))
    INVOKE_SPECIAL : Instruction(InvokeInstruction(Opcodes.INVOKESPECIAL))
    INVOKE_STATIC : Instruction(InvokeInstruction(Opcodes.INVOKESTATIC))
    INVOKE_INTERFACE : Instruction(InvokeInstruction(Opcodes.INVOKEINTERFACE))
    // TODO: invokedynamic (invoke-dynamic)
    NEW : Instruction(TypeInstruction(Opcodes.NEW))
    NEW_ARRAY : Instruction(ArrayInstruction())
    // anewarray (new-array)
    LENGTH_ARRAY : Instruction(SingleInstruction(Opcodes.ARRAYLENGTH))
    THROW : Instruction(SingleInstruction(Opcodes.ATHROW))
    CHECK_CAST : Instruction(TypeInstruction(Opcodes.CHECKCAST))
    INSTANCE_OF : Instruction(TypeInstruction(Opcodes.INSTANCEOF))
    MONITOR_ENTER : Instruction(SingleInstruction(Opcodes.MONITORENTER))
    MONITOR_EXIT : Instruction(SingleInstruction(Opcodes.MONITOREXIT))
    // wide
    NEW_ARRAY_MULTI : Instruction(MultiArrayInstruction())
    IF_NULL : Instruction(JumpInstruction(Opcodes.IFNULL))
    IF_NOT_NULL : Instruction(JumpInstruction(Opcodes.IFNONNULL))
    // goto_w
    // jsr_w
    // breakpoint

    public fun isMetaRequired(): Boolean {
        return requiresMeta
    }
}

object Instructions {
    platformStatic public fun print(patch: StringBuilder, method: MethodNode, insn: AbstractInsnNode): Boolean {
        for (i in values()) {
            if (i.handler != null && i.handler!!.print(i, patch, method, insn)) {
                return true
            }
        }
        return false
    }
}
