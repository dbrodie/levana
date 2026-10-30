package ninja.doskey.app.levana.screenshots

/**
 * Window sizes of the Galaxy Z Fold8 and Fold8 Ultra (TASK-52.1), for [ScreenshotTest]'s
 * device. Samsung's published pixels at an assumed 420 dpi (Samsung publishes no Android
 * density), as the full display without system bars; the split-screen size is half the
 * landscape inner display. At mdpi, like [ScreenshotTest.DEVICE], so an image is the window
 * in dp. Sources and arithmetic are in TASK-52.1 and specs/ARCHITECTURE_SPEC.md.
 */
enum class FoldableSize(widthDp: Int, heightDp: Int) {
    FOLD8_COVER(475, 751),
    FOLD8_INNER_PORTRAIT(704, 933),
    FOLD8_INNER_LANDSCAPE(933, 704),
    FOLD8_SPLIT_HALF(466, 704),
    FOLD8_ULTRA_COVER(411, 960),
    FOLD8_ULTRA_INNER_PORTRAIT(859, 954),
    FOLD8_ULTRA_INNER_LANDSCAPE(954, 859),
    FOLD8_ULTRA_SPLIT_HALF(477, 859);

    private val orientation = if (widthDp > heightDp) "land" else "port"

    /** Robolectric qualifiers; the screen size class and aspect follow from the dimensions. */
    val qualifiers = "w${widthDp}dp-h${heightDp}dp-$orientation-mdpi-keyshidden-nonav"

    val tag = name.lowercase()
}
