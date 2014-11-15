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

package uk.co.thinkofdeath.patchtools.patch

import org.objectweb.asm.Type
import uk.co.thinkofdeath.patchtools.lexer.Token

public class PatchField(public val owner: PatchClass,
                        it: Iterator<Token>,
                        type: Ident,
                        dimCount: Int,
                        public val ident: Ident,
                        modifiers: Set<String>,
                        public val patchAnnotations: List<String>,
                        public val value: Any? = null
) {
    public val descRaw: String
    public val desc: Type
        get() = Type.getMethodType(descRaw)
    public val mode: Mode
    val access: Int

    {
        var access = 0
        for (modifier in modifiers) {
            if (modifier in modifierAccess) {
                access = access or modifierAccess[modifier]!!
            }
        }
        this.access = access and fieldModifiers

        mode = if ("add" in modifiers) Mode.ADD
        else if ("remove" in modifiers) Mode.REMOVE
        else Mode.MATCH

        val descBuilder = StringBuilder()
        for (i in 1..dimCount) {
            descBuilder.append('[')
        }
        PatchClass.appendType(descBuilder, type.toString())
        descRaw = descBuilder.toString()

    }
}
