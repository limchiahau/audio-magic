/*
 * Developed by Lim Chia Hau on 11/2/18 2:35 PM.
 * Last modified 11/2/18 2:35 PM.
 * Copyright (c) 2018. All rights reserved.
 */


import com.chl.audiomagic.*
import org.junit.Test
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue


private val randomGenerator = Random(500)

class CacheTest : MyTest {
    @Test
    override fun hasMethods() {
        Assertor.assertMethods(Cache<Int>(1),
                "get",
                "cache")
    }

    @Test
    fun ableToGetSameItemBackFromCache() {
        val cache = Cache<Int>(10)
        val sample = createSample()

        cache.cache(sample.first, sample.second)
        assertEquals(sample.second, cache.get(sample.first))
    }

    @Test
    fun cacheIsFlushedAfterSizedLimitIsReached() {
        val cache = Cache<Int>(10)
        val sample = List(10) { createSample() }
        val additionalSample = createSample()

        sample.forEach {
            cache.cache(it.first, it.second)
        }

        cache.cache(additionalSample.first, additionalSample.second)

        assertTrue(sample.map {
            cache.get(it.first) == null
        }.all {
            it == true
        })
    }

    private fun createSample() : Pair<String, Int> =
      Pair(generateString(), randomGenerator.nextInt())

    private fun generateString(): String {
        val strLength = 100
        return List(strLength) {randomGenerator.nextInt(100)}
                .map(Int::toChar)
                .map(Char::toString)
                .reduce(String::plus)
    }
}

class EventLoopTest : MyTest {
    @Test
    override fun hasMethods() {
        Assertor.assertMethods(EventLoop({}),
                "start")
    }
}

class AudioManagerTest : MyTest {
    @Test
    override fun hasMethods() {
        Assertor.assertMethods(AudioManager,
                "autoSwitchOutput")
    }
}

class PacTLTest : MyTest {
    @Test
    override fun hasMethods() {
        Assertor.assertMethods(PacTL,
                "getAudioOutputs",
                "setAudioOutputDeviceTo"
                )
    }
}

class GridTest : MyTest {
    @Test
    override fun hasMethods() {

    }

    @Test
    fun toRowConvertsTheListProperly() {
        /**
         * [["1", "1", "1"],
         * ["2", "2", "2"],
         * ["3", "3", "3"]]
         */
        val grid: Grid = listOf("1", "2", "3").map {
            val data = it
            List(3) {data }
        }

        /**
         * [["1" ,"2" ,"3"],
         * ["1", "2", "3"],
         * ["1", "2", "3"]]
         */
        val expectedGrid = List(3) {
            listOf("1", "2", "3")
        }

        assertEquals(expectedGrid, grid.toRows())
    }
}

object Assertor {
    fun assertMethods(obj: Any, vararg methods: String) {
        assertTrue(assertMethodsHelper(obj, *methods))
    }

    private fun assertMethodsHelper(obj: Any, vararg methods: String): Boolean {
        return methods.all {
            obj.javaClass.kotlin.members.any {
                member -> member.name == it
            }
        }
    }
}

interface MyTest {
    fun hasMethods()
}