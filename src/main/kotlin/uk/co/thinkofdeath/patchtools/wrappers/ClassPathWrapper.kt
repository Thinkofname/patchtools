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

package uk.co.thinkofdeath.patchtools.wrappers

import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode

import java.io.*
import java.util.zip.ZipFile

public class ClassPathWrapper(vararg libs: File) : Closeable {

    private val searchFiles: Array<out ZipFile>

    {
        try {
            val tmp = arrayOfNulls<ZipFile>(libs.size())
            for (i in 0..libs.size() - 1) {
                tmp[i] = ZipFile(libs[i])
            }
            searchFiles = tmp.requireNoNulls()
        } catch (e: IOException) {
            throw UncheckedIOException(e)
        }

    }

    public fun find(classSet: ClassSet, clazz: String): ClassWrapper? {
        var inStr: InputStream? = null
        for (zip in searchFiles) {
            val entry = zip.getEntry(clazz + ".class")
            if (entry == null) continue
            inStr = zip.getInputStream(entry)
            break
        }
        if (inStr == null) {
            inStr = javaClass.getResourceAsStream("/" + clazz + ".class")
            if (inStr == null) {
                return null
            }
        }
        val stream = inStr!!;
        stream.use { ignored ->
            val data = stream.readBytes()
            val node = ClassNode(Opcodes.ASM5)
            val reader = ClassReader(data)
            reader.accept(node, 0)
            return ClassWrapper(classSet, node, true)
        }

    }

    override fun close() {
        for (zip in searchFiles) {
            zip.close()
        }
    }
}
