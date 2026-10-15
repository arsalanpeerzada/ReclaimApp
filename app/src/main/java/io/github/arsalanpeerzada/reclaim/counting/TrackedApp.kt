package io.github.arsalanpeerzada.reclaim.counting

/**
 * The apps whose reels we count, and the only apps whose scroll events the service reads at all
 * (see ReelCountingService).
 */
enum class TrackedApp(
    val id: String,
    val displayName: String,
    val packages: Set<String>,
    /**
     * View ids of the full-screen reel pager, when the app exposes readable ids. A scroll in a
     * container with a different id (the home feed, say) is ignored. Empty means "no id hint":
     * rely on the shape of the container alone.
     */
    val reelContainerIds: Set<String>,
) {
    INSTAGRAM(
        id = "instagram",
        displayName = "Instagram",
        packages = setOf("com.instagram.android"),
        // Unverified: the Reels viewer's pager id in recent Instagram builds. Check it with
        // Settings > Counting diagnostics, and update it if Instagram renames the view.
        reelContainerIds = setOf("com.instagram.android:id/clips_viewer_view_pager"),
    ),
    TIKTOK(
        id = "tiktok",
        displayName = "TikTok",
        // The global build and the regional build.
        packages = setOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill"),
        // TikTok's view ids are obfuscated and change between releases, so no hint.
        reelContainerIds = emptySet(),
    ),
    ;

    companion object {
        fun fromPackage(packageName: String): TrackedApp? = entries.firstOrNull { packageName in it.packages }

        fun fromId(id: String): TrackedApp? = entries.firstOrNull { it.id == id }
    }
}
