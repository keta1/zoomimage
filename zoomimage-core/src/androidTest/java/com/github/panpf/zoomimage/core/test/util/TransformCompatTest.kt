package com.github.panpf.zoomimage.core.test.util

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransformCompatTest {
    // todo Implementation tests

//    @Test
//    fun testConcatAndSplit() {
//        // todo 修复这个测试
//        val containerSize = IntSizeCompat(1080, 1656)
//        val contentSize = IntSizeCompat(7500, 232)
//        val contentScale = ScaleType.FIT_CENTER
//        val readMode = ReadMode.Default
//
//        val baseTransform = contentScale.computeTransform(
//            dstSize = containerSize,
//            srcSize = contentSize,
//        ).also {
//            val expected = TransformCompat(ScaleFactorCompat(0.144f), OffsetCompat(0f, 812f))
//            Assert.assertEquals(/* expected = */ expected,/* actual = */ it)
//        }
//
//        val readModeTransform = readMode.computeTransform(
//            containerSize = containerSize,
//            contentSize = contentSize,
//            baseTransform = baseTransform
//        ).also {
//            val expected = TransformCompat(ScaleFactorCompat(7.137931f), OffsetCompat(0f, 0f))
//            Assert.assertEquals(/* expected = */ expected,/* actual = */ it)
//        }
//
//        val targetUserTransform = TransformCompat(
//            scale = ScaleFactorCompat(readModeTransform.scaleX / baseTransform.scaleX),
//            offset = OffsetCompat(0f, -40250f)
//        )
//        val userTransform = (readModeTransform - baseTransform).also {
//            Assert.assertEquals(/* expected = */ targetUserTransform,/* actual = */ it)
//        }
//        (baseTransform + userTransform).also {
//            Assert.assertEquals(/* expected = */ readModeTransform,/* actual = */ it)
//        }
//    }

//    @Test
//    fun testConcatAndSplit2() {
//        // todo 修复这个测试
//        val containerSize = IntSizeCompat(1080, 1656)
//        val contentSize = IntSizeCompat(7500, 232)
//        val contentScale = ScaleType.CENTER
//        val readMode = ReadMode.Default
//
//        val baseTransform = contentScale.computeTransform(
//            dstSize = containerSize,
//            srcSize = contentSize,
//        ).also {
//            val expected = TransformCompat(ScaleFactorCompat(1f), OffsetCompat(-3210f, 712f))
//            Assert.assertEquals(/* expected = */ expected,/* actual = */ it)
//        }
//
//        val readModeTransform = readMode.computeTransform(
//            containerSize = containerSize,
//            contentSize = contentSize,
//            baseTransform = baseTransform
//        ).also {
//            val expected =
//                TransformCompat(ScaleFactorCompat(7.137931f), OffsetCompat(-22912.758f, 0f))
//            Assert.assertEquals(/* expected = */ expected,/* actual = */ it)
//        }
//
//        val targetUserTransform = TransformCompat(
//            scale = ScaleFactorCompat(readModeTransform.scaleX / baseTransform.scaleX),
//            offset = OffsetCompat(0f, -5082.2065f)
//        )
//        val userTransform = (readModeTransform - baseTransform).also {
//            Assert.assertEquals(/* expected = */ targetUserTransform,/* actual = */ it)
//        }
//        (baseTransform + userTransform).also {
//            Assert.assertEquals(/* expected = */ readModeTransform,/* actual = */ it)
//        }
//    }
}