package com.shadowmonarchbooks.dayloop.ui.skin

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SubmergedChromeTest {
    @Test
    fun `legacy skins cannot enter replacement chrome through their motif or motion`() {
        for (motif in listOf(null, "masks", "moon", "crown")) {
            for (motion in listOf(null, "slash", "fade", "flip")) {
                assertFalse(SkinSpec.Engine.copy(hasSkin = true, motif = motif, motion = motion).hasSubmergedChrome())
            }
        }
        assertFalse(SkinSpec.Engine.copy(chrome = "submerged").hasSubmergedChrome())
        assertTrue(SkinSpec.Engine.copy(hasSkin = true, chrome = "submerged").hasSubmergedChrome())
    }
}
