package com.lyihub.archiveassistant.ui.screens

internal object MemorialStackGeometry {
  fun offsetX(level: Int, dp: (Float) -> Float): Float {
    return when (level) {
      0 -> 0f
      1 -> dp(8f)
      2 -> -dp(7f)
      3 -> dp(13f)
      4 -> -dp(11f)
      else -> dp(5f)
    }
  }

  fun offsetY(level: Int, dp: (Float) -> Float): Float {
    if (level <= 0) return 0f
    return dp(7f + level * 6.5f)
  }

  fun scale(level: Int): Float {
    if (level <= 0) return 1f
    return 1f - level * 0.014f
  }

  fun rotation(level: Int): Float {
    return when (level) {
      0 -> 0f
      1 -> -1.1f
      2 -> 1.35f
      3 -> -0.8f
      4 -> 1.05f
      else -> -0.55f
    }
  }
}
