package com.example.ptmanageremployee

import com.example.ptmanageremployee.data.metaLine
import com.example.ptmanageremployee.data.shiftMinutes
import com.example.ptmanageremployee.data.weekBucketIndex
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/** 화면 여러 곳에서 함께 쓰는 Format 헬퍼들의 동작 고정. */
class FormatTest {

    @Test
    fun shiftMinutes_counts_overnight_shift_across_midnight() {
        assertEquals(300, shiftMinutes("18:00:00", "23:00:00"))
        assertEquals(480, shiftMinutes("22:00:00", "06:00:00"))
        assertEquals(0, shiftMinutes(null, "06:00:00"))
        assertEquals(0, shiftMinutes("22:00", "이상한값"))
    }

    @Test
    fun weekBucketIndex_matches_backend_buckets() {
        val bucket = { day: Int -> weekBucketIndex(LocalDate.of(2026, 7, day)) }
        assertEquals(0, bucket(1))
        assertEquals(0, bucket(7))
        assertEquals(1, bucket(8))
        assertEquals(2, bucket(21))
        assertEquals(3, bucket(22))
        assertEquals(3, bucket(31))
    }

    @Test
    fun metaLine_drops_blank_parts() {
        assertEquals("김알바 · 2026-07-01", metaLine("김알바", "2026-07-01T10:00:00", "작성자"))
        assertEquals("작성자", metaLine(null, null, "작성자"))
        assertEquals("작성자 · 2026-07-01", metaLine(null, "2026-07-01T10:00:00", "작성자"))
    }
}
