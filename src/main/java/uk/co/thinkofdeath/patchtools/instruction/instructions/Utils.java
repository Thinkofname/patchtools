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

package uk.co.thinkofdeath.patchtools.instruction.instructions;

import org.objectweb.asm.Label;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;
import uk.co.thinkofdeath.patchtools.PatchScope;
import uk.co.thinkofdeath.patchtools.patch.Ident;
import uk.co.thinkofdeath.patchtools.patch.ValidateException;

import java.util.Map;
import java.util.WeakHashMap;

public class Utils {
    public static Object parseConstant(String cst) {
        if (cst.startsWith("\"") && cst.endsWith("\"")) {
            return cst.substring(1, cst.length() - 1);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public static void printConstant(StringBuilder patch, Object value) {
        if (value instanceof String) {
            patch.append('"').append(value).append('"');
        } else {
            //throw new UnsupportedOperationException("Unsupported " + value.getClass());
            patch.append("unsupported: ").append(value);
        }
    }

    public static boolean checkOrSetLabel(PatchScope scope, MethodNode method, String labelName, LabelNode label) {
        Ident ident = new Ident(labelName);
        if (!ident.isWeak()) {
            if (!ident.getName().equals("*")) {
                return false;
            }
        } else if (scope != null) {
            LabelNode l = scope.getLabel(method, ident.getName());
            if (l == null) {
                scope.putLabel(method, label, ident.getName());
            } else {
                return l == label;
            }
        }
        return true;
    }


    public static LabelNode getLabel(PatchScope scope, MethodNode method, String labelName) {
        Ident ident = new Ident(labelName);
        if (!ident.isWeak()) {
            throw new UnsupportedOperationException("Non-weak label " + labelName);
        }

        LabelNode label = scope.getLabel(method, ident.getName());
        if (label == null) {
            label = new LabelNode();
            scope.putLabel(method, label, ident.getName());
        }
        return label;
    }

    public static boolean equalOrWild(String val, int other) {
        return val.equals("*") || Integer.parseInt(val) == other;
    }

    private static Map<MethodNode, Map<Label, String>> labels = new WeakHashMap<>();

    public static String printLabel(MethodNode methodNode, LabelNode labelNode) {
        if (!labels.containsKey(methodNode)) {
            labels.put(methodNode, new WeakHashMap<>());
        }
        Map<Label, String> lbls = labels.get(methodNode);
        if (!lbls.containsKey(labelNode.getLabel())) {
            StringBuilder id = new StringBuilder("label-");
            int i = lbls.size();
            do {
                char c = (char) ('A' + (i % 26));
                i /= 26;
                id.append(c);
            } while (i > 0);
            lbls.put(labelNode.getLabel(), id.toString());
        }
        return lbls.get(labelNode.getLabel());
    }

    public static int validateType(String type) {
        int offset = validateType(type, 0);
        if (offset != type.length()) {
            throw new ValidateException("Extra characters found '" + type.substring(offset) + "'");
        }
        return offset;
    }

    private static int validateType(String type, int offset) {
        int i = offset;
        boolean inObject = false;
        while (i < type.length()) {
            int c = type.codePointAt(i);
            i += Character.charCount(c);
            if (inObject) {
                if (c == ';') {
                    return i;
                }
            } else {
                switch (c) {
                    case 'L':
                        inObject = true;
                        break;
                    case '[':
                        break; // Array
                    case 'B': // Byte
                    case 'C': // Char
                    case 'D': // Double
                    case 'F': // Float
                    case 'I': // Int
                    case 'J': // Long
                    case 'S': // Short
                    case 'Z': // Boolean
                    case 'V': // Void
                        return i;
                    default:
                        throw new ValidateException("Unexpected " + Character.toString((char) c));
                }
            }
        }
        if (inObject) {
            throw new ValidateException("Unexpected end of type, expected ';' (object end)");
        }
        throw new ValidateException("Unexpected end of type");
    }

    public static void validateMethodType(String type) {
        int i = 0;
        int state = 0;
        while (i < type.length()) {
            int c = type.codePointAt(i);
            i += Character.charCount(c);
            if (state == 0) {
                if (c != '(') {
                    throw new ValidateException("Expected '('");
                }
                state = 1;
            } else if (state == 1) {
                if (c == ')') {
                    state = 2;
                    continue;
                }
                i -= Character.charCount(c);
                i = validateType(type, i);
            } else {
                i -= Character.charCount(c);
                i = validateType(type, i);
                state = 3;
                break;
            }
        }
        switch (state) {
            case 0:
                throw new ValidateException("Unexpected end of type, expected '('");
            case 1:
                throw new ValidateException("Unexpected end of type, expected ')'");
            case 2:
                throw new ValidateException("Unexpected end of type, expected return type");
            case 3:
                if (i == type.length()) {
                    return;
                }
                throw new ValidateException("Extra characters found '" + type.substring(i) + "'");
        }
        throw new ValidateException("Unexpected end of type");
    }

    public static void validateObjectType(String param) {
        validateType(param.startsWith("[") ? param : "L" + param + ";");
    }
}
