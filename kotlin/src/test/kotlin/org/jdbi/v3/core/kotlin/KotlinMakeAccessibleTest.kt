/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jdbi.v3.core.kotlin

import org.assertj.core.api.Assertions.assertThat
import org.jdbi.v3.core.junit5.H2DatabaseExtension
import org.jdbi.v3.core.junit5.H2DatabaseExtension.SOMETHING_INITIALIZER
import org.jdbi.v3.core.mapper.reflect.ReflectionMappers
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.RegisterExtension
import kotlin.reflect.full.IllegalCallableAccessException

class KotlinMakeAccessibleTest {

    @RegisterExtension
    @JvmField
    val h2Extension: H2DatabaseExtension = H2DatabaseExtension.instance().withPlugin(KotlinPlugin()).withInitializer(SOMETHING_INITIALIZER)

    @Test
    fun testMakeFieldsAccessible() {
        val handle = h2Extension.sharedHandle
        handle.getConfig(ReflectionMappers::class.java).isMakeAttributesAccessible = true

        val testBean = handle.select("select 1 as id, 2 as inaccessible").mapTo<TestBean>().one()

        assertThat(testBean).isNotNull
        assertThat(testBean.id).isOne
        assertThat(testBean.getInaccessible()).isEqualTo(2)
    }

    @Test
    fun testMakeFieldsInaccessible() {
        val handle = h2Extension.sharedHandle
        handle.getConfig(ReflectionMappers::class.java).isMakeAttributesAccessible = false

        val e = assertThrows<IllegalCallableAccessException> {
            handle.select("select 1 as id, 2 as inaccessible").mapTo<TestBean>().one()
        }

        assertThat(e.message).contains("cannot access a member")
        assertThat(e.message).contains("with modifiers \"private\"")
    }

    data class TestBean(val id: Int = 0) {
        private var inaccessible: Int = 0

        fun getInaccessible() = inaccessible
    }
}
