package com.example.taskfoundation.focus

import org.junit.Assert.assertEquals
import org.junit.Test

class FocusSessionTest {
    @Test fun countdownRecoversAfterSuspensionAndClampsAtDeadline() {
        assertEquals(1500, remainingSeconds(1_500_000, 0, 1500))
        assertEquals(900, remainingSeconds(1_500_000, 600_000, 1500))
        assertEquals(1, remainingSeconds(1_500_000, 1_499_999, 1500))
        assertEquals(0, remainingSeconds(1_500_000, 1_600_000, 1500))
        assertEquals(1500, remainingSeconds(1_500_000, -5000, 1500))
    }
}
