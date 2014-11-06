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

package uk.co.thinkofdeath.patchtools.instruction.instructions

import org.objectweb.asm.Label
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.MethodNode
import uk.co.thinkofdeath.patchtools.PatchScope
import uk.co.thinkofdeath.patchtools.patch.Ident
import uk.co.thinkofdeath.patchtools.patch.ValidateException
import java.util.WeakHashMap

public class Utils {
    class object {
        public fun parseConstant(cst: String): Any {
            if (cst.startsWith("\"") && cst.endsWith("\"")) {
                return cst.substring(1, cst.length() - 1)
            } else {
                throw UnsupportedOperationException()
            }
        }

        public fun printConstant(patch: StringBuilder, value: Any) {
            if (value is String) {
                patch.append('"').append(value).append('"')
            } else {
                //throw new UnsupportedOperationException("Unsupported " + value.getClass());
                patch.append("unsupported: ").append(value)
            }
        }

        public fun checkOrSetLabel(scope: PatchScope?, method: MethodNode, labelName: String, label: LabelNode): Boolean {
            val ident = Ident(labelName)
            if (!ident.isWeak()) {
                if (ident.name != "*") {
                    return false
                }
            } else if (scope != null) {
                val l = scope.getLabel(method, ident.name)
                if (l == null) {
                    scope.putLabel(method, label, ident.name)
                } else {
                    return l == label
                }
            }
            return true
        }


        public fun getLabel(scope: PatchScope, method: MethodNode, labelName: String): LabelNode {
            val ident = Ident(labelName)
            if (!ident.isWeak()) {
                throw UnsupportedOperationException("Non-weak label " + labelName)
            }

            val label = scope.getLabel(method, ident.name)
            if (label == null) {
                val label = LabelNode()
                scope.putLabel(method, label, ident.name)
                return label
            }
            return label
        }

        public fun equalOrWild(`val`: String, other: Int): Boolean {
            return `val` == "*" || Integer.parseInt(`val`) == other
        }

        private val labels = WeakHashMap<MethodNode, MutableMap<Label, String>>()

        public fun printLabel(methodNode: MethodNode, labelNode: LabelNode): String {
            if (!labels.containsKey(methodNode)) {
                labels.put(methodNode, WeakHashMap<Label, String>())
            }
            val lbls = labels.get(methodNode)
            if (!lbls.containsKey(labelNode.getLabel())) {
                val id = StringBuilder("label-")
                var i = lbls.size()
                do {
                    val c = ('A' + (i % 26)).toChar()
                    i /= 26
                    id.append(c)
                } while (i > 0)
                lbls.put(labelNode.getLabel(), id.toString())
            }
            return lbls.get(labelNode.getLabel())!!
        }

        public fun validateType(`type`: String): Int {
            val offset = validateType(`type`, 0)
            if (offset != `type`.length()) {
                throw ValidateException("Extra characters found '" + `type`.substring(offset) + "'")
            }
            return offset
        }

        private fun validateType(type: String, offset: Int): Int {
            var i = offset
            var inObject = false
            while (i < type.length()) {
                val point = type.codePointAt(i)
                val c = point.toChar()
                i += Character.charCount(point)
                if (inObject) {
                    if (c == ';') {
                        return i
                    }
                } else {
                    when (c) {
                        'L' -> inObject = true
                        '[' -> {
                        }
                        'B' // Byte
                            , 'C' // Char
                            , 'D' // Double
                            , 'F' // Float
                            , 'I' // Int
                            , 'J' // Long
                            , 'S' // Short
                            , 'Z' // Boolean
                            , 'V' // Void
                        -> return i
                        else -> throw ValidateException("Unexpected " + Character.toString(c.toChar()))
                    }// Array
                }
            }
            if (inObject) {
                throw ValidateException("Unexpected end of type, expected ';' (object end)")
            }
            throw ValidateException("Unexpected end of type")
        }

        public fun validateMethodType(`type`: String) {
            var i = 0
            var state = 0
            while (i < `type`.length()) {
                val point = `type`.codePointAt(i)
                val c = point.toChar()
                i += Character.charCount(point)
                if (state == 0) {
                    if (c != '(') {
                        throw ValidateException("Expected '('")
                    }
                    state = 1
                } else if (state == 1) {
                    if (c == ')') {
                        state = 2
                        continue
                    }
                    i -= Character.charCount(point)
                    i = validateType(`type`, i)
                } else {
                    i -= Character.charCount(point)
                    i = validateType(`type`, i)
                    state = 3
                    break
                }
            }
            when (state) {
                0 -> throw ValidateException("Unexpected end of type, expected '('")
                1 -> throw ValidateException("Unexpected end of type, expected ')'")
                2 -> throw ValidateException("Unexpected end of type, expected return type")
                3 -> {
                    if (i == `type`.length()) {
                        return
                    }
                    throw ValidateException("Extra characters found '" + `type`.substring(i) + "'")
                }
            }
            throw ValidateException("Unexpected end of type")
        }

        public fun validateObjectType(param: String) {
            validateType(if (param.startsWith("[")) param else "L" + param + ";")
        }
    }
}
