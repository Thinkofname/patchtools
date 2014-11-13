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

import com.google.common.base.Joiner
import org.objectweb.asm.Type
import uk.co.thinkofdeath.patchtools.instruction.instructions.Utils

public class PatchField(public val owner: PatchClass, mCommand: Command) {
    public val ident: Ident
    public val descRaw: String
    public val desc: Type
        get() = Type.getMethodType(descRaw)
    public val mode: Mode
    public val value: Any?
    public val isStatic: Boolean
    public val isPrivate: Boolean

    {
        if (mCommand.args.size < 2) {
            throw ValidateException("Incorrect number of arguments for field")
        }
        ident = Ident(mCommand.args[0])
        mode = mCommand.mode
        descRaw = mCommand.args[1]
        Utils.validateType(descRaw)
        var isS = false
        var isP = false
        var v: Any? = null
        if (mCommand.args.size >= 3) {
            var i = 2
            @accessModi
            while (i < mCommand.args.size) {
                when (mCommand.args[i]) {
                    "static" -> isS = true
                    "private" -> isP = true
                    else -> break@accessModi
                }
                i++
            }
            val parts = arrayOfNulls<String>(mCommand.args.size - i)
            if (parts.size != 0) {
                System.arraycopy(mCommand.args, i, parts, 0, parts.size)
                v = Utils.parseConstant(Joiner.on(' ').join(parts))
            }
        }
        isStatic = isS
        isPrivate = isP
        value = v
    }
}
