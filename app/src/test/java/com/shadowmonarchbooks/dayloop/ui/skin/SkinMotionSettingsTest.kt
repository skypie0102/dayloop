package com.shadowmonarchbooks.dayloop.ui.skin

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SkinMotionSettingsTest {
    @Test fun `either system scale can remove motion`() {
        assertTrue(animationsDisabled(0f, 1f))
        assertTrue(animationsDisabled(1f, 0f))
        assertFalse(animationsDisabled(0.5f, 1.5f))
    }
    @Test fun `every navigation grammar snaps when motion is disabled`() {
        for (token in listOf(null, "none", "fade", "slash", "flip")) {
            assertEquals(SkinMotion.Snap, SkinSpec.Engine.copy(motion = token).navMotion(true))
        }
    }
}
