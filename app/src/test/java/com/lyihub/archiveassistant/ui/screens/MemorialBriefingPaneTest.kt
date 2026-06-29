package com.lyihub.archiveassistant.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MemorialBriefingPaneTest {
  @Test
  fun departmentLabelAlpha_hidesNearActiveNeighbors() {
    assertEquals(0f, departmentLabelAlpha(0f), 0.001f)
    assertEquals(0f, departmentLabelAlpha(0.8f), 0.001f)
    assertTrue(departmentLabelAlpha(0.9f) in 0.55f..0.75f)
  }

  @Test
  fun departmentLabelAlpha_showsOnlyAtActivePeak() {
    assertTrue(departmentLabelAlpha(0.95f) > 0.8f)
    assertEquals(1f, departmentLabelAlpha(1f), 0.001f)
  }
}
