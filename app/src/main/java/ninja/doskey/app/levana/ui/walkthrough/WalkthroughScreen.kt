package ninja.doskey.app.levana.ui.walkthrough

import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import ninja.doskey.app.levana.R
import ninja.doskey.app.levana.ui.location.CityPickerScreen
import ninja.doskey.app.levana.ui.contentWidth
import ninja.doskey.app.levana.ui.CappedFontScale
import kotlin.math.roundToInt

/**
 * The walkthrough with its side effects: the permission dialogs, the permission recheck on
 * resume (a grant in system settings), the city picker and system back.
 */
@Composable
fun WalkthroughScreen(state: WalkthroughState, onIntent: (WalkthroughIntent) -> Unit) {
    var pendingPermission by rememberSaveable { mutableStateOf<WalkthroughPermission?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        pendingPermission?.let { onIntent(WalkthroughIntent.PermissionResult(it, it.isGranted(results))) }
        pendingPermission = null
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { onIntent(WalkthroughIntent.RefreshPermissions) }

    // Back steps through the pages; on the first page a replay closes and a first run leaves
    // the app as usual (the pages shown so far stay seen)
    BackHandler(enabled = state.page > 0 || state.replay) {
        onIntent(if (state.page > 0) WalkthroughIntent.Back else WalkthroughIntent.Finish)
    }

    if (state.addingCity) {
        // Over the walkthrough, in the activity's own window; the pager returns to its page
        CityPickerLayer(onClose = { onIntent(WalkthroughIntent.CloseCityPicker) })
        return
    }
    WalkthroughContent(
        state = state,
        onIntent = onIntent,
        onRequestPermission = { permission ->
            pendingPermission = permission
            permissionLauncher.launch(permission.permissions)
        },
        reducedMotion = rememberReducedMotion(),
    )
}

/**
 * The walkthrough's pages and controls for [state]: Skip at the top end, the pager, and the
 * bottom bar (page indicator, Back, Next / Get started, or a lone Got it). While the page can
 * still scroll down, Next / Get started / Got it scroll it instead of moving on (TASK-65).
 */
@Composable
fun WalkthroughContent(
    state: WalkthroughState,
    onIntent: (WalkthroughIntent) -> Unit,
    onRequestPermission: (WalkthroughPermission) -> Unit,
    modifier: Modifier = Modifier,
    reducedMotion: Boolean = false,
) {
    val count = state.screens.size
    val pagerState = rememberPagerState(initialPage = state.page) { count }
    PagerSync(pagerState, state.page, reducedMotion) { onIntent(WalkthroughIntent.PageShown(it)) }
    val actions = remember(onIntent) { WalkthroughActions(onAddCity = { onIntent(WalkthroughIntent.AddCity) }) }
    // Each composed page's scroll, by screen id, so the bottom bar can see the current one's
    val pageScrolls = remember { mutableStateMapOf<String, ScrollState>() }
    val currentScroll = state.screens.getOrNull(state.page)?.let { pageScrolls[it.id] }
    val scope = rememberCoroutineScope()
    val forward: (WalkthroughIntent) -> Unit = { intent ->
        if (currentScroll?.canScrollForward == true) {
            scope.launch { currentScroll.showMore(reducedMotion) }
        } else {
            onIntent(intent)
        }
    }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            Box(
                Modifier.contentWidth().fillMaxWidth().height(56.dp).padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                // A single screen has nothing to skip
                if (count > 1) {
                    TextButton(onClick = { onIntent(WalkthroughIntent.Skip) }) {
                        Text(stringResource(R.string.walkthrough_skip))
                    }
                }
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.Top,
                key = { state.screens[it].id },
            ) { index ->
                val screen = state.screens[index]
                // Saved with the pager's item, as before; registered for the bottom bar
                val scroll = rememberScrollState()
                DisposableEffect(screen.id, scroll) {
                    pageScrolls[screen.id] = scroll
                    onDispose { if (pageScrolls[screen.id] === scroll) pageScrolls.remove(screen.id) }
                }
                WalkthroughPage(
                    screen = screen,
                    scroll = scroll,
                    pageLabel = stringResource(R.string.walkthrough_page_of, index + 1, count),
                    permissionStatus = screen.permission?.let(state::status),
                    actions = actions,
                    onAllow = onRequestPermission,
                    onNotNow = { onIntent(WalkthroughIntent.NotNow) },
                )
            }
            WalkthroughBottomBar(
                count = count,
                page = state.page,
                indicator = state.indicator,
                isLastPage = state.isLastPage,
                showsMore = currentScroll?.canScrollForward == true,
                reducedMotion = reducedMotion,
                onIntent = onIntent,
                onForward = forward,
            )
        }
    }
}

/**
 * Scrolls down by about a viewport, keeping a little of what was shown for context, or to the
 * end if that is nearer. Instant under reduced motion.
 */
private suspend fun ScrollState.showMore(reducedMotion: Boolean) {
    val step = (viewportSize * SHOW_MORE_STEP).roundToInt().coerceAtLeast(1)
    val target = (value + step).coerceAtMost(maxValue)
    if (reducedMotion) scrollTo(target) else animateScrollTo(target)
}

/** The part of the viewport one Next scrolls; the rest stays on screen for context. */
private const val SHOW_MORE_STEP = 0.85f

/**
 * Keeps the pager on [page] (Next and Back) and reports where it settles after a swipe.
 * Positions mid-scroll are ignored, so a quick double tap cannot pull the page back.
 */
@Composable
private fun PagerSync(pagerState: PagerState, page: Int, reducedMotion: Boolean, onShown: (Int) -> Unit) {
    LaunchedEffect(page) {
        if (pagerState.currentPage != page) {
            if (reducedMotion) pagerState.scrollToPage(page) else pagerState.animateScrollToPage(page)
        }
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { if (pagerState.isScrollInProgress) null else pagerState.currentPage }
            .filterNotNull()
            .distinctUntilChanged()
            .collect(onShown)
    }
}

@Composable
private fun WalkthroughPage(
    screen: FeatureScreen,
    scroll: ScrollState,
    pageLabel: String,
    permissionStatus: PermissionStatus?,
    actions: WalkthroughActions,
    onAllow: (WalkthroughPermission) -> Unit,
    onNotNow: () -> Unit,
) {
    val title = stringResource(screen.title)
    Column(
        Modifier
            .fillMaxSize()
            .semantics { paneTitle = "$title, $pageLabel" }
            .verticalScroll(scroll)
            .contentWidth()
            .padding(horizontal = 24.dp),
    ) {
        // Mock-ups of fixed-size app surfaces: their text overlaps beyond a modest scale
        IllustrationCard { CappedFontScale { screen.illustration(actions) } }
        Spacer(Modifier.height(32.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 4.dp).semantics {
                heading()
                stateDescription = pageLabel
            },
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(screen.body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        if (screen.permission != null && permissionStatus != null) {
            Spacer(Modifier.height(20.dp))
            PermissionPanel(
                permission = screen.permission,
                status = permissionStatus,
                onAllow = { onAllow(screen.permission) },
                onNotNow = onNotNow,
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun WalkthroughBottomBar(
    count: Int,
    page: Int,
    indicator: PageIndicator,
    isLastPage: Boolean,
    showsMore: Boolean,
    reducedMotion: Boolean,
    onIntent: (WalkthroughIntent) -> Unit,
    onForward: (WalkthroughIntent) -> Unit,
) {
    // TalkBack says what the forward button will do: "double tap to show more" while it scrolls
    val showMoreLabel = stringResource(R.string.walkthrough_show_more)
    val forwardSemantics = { intent: WalkthroughIntent ->
        if (showsMore) {
            Modifier.semantics { onClick(label = showMoreLabel) { onForward(intent); true } }
        } else {
            Modifier
        }
    }
    Column(
        Modifier.contentWidth().fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (indicator) {
            PageIndicator.NONE -> Unit
            PageIndicator.DOTS -> PageDots(count = count, page = page, reducedMotion = reducedMotion)
            PageIndicator.COUNTER -> PageCounter(count = count, page = page)
        }
        if (indicator != PageIndicator.NONE) Spacer(Modifier.height(20.dp))
        if (count == 1) {
            Button(
                onClick = { onForward(WalkthroughIntent.Finish) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).then(forwardSemantics(WalkthroughIntent.Finish)),
            ) { Text(stringResource(R.string.walkthrough_got_it)) }
        } else {
            val forwardIntent = if (isLastPage) WalkthroughIntent.Finish else WalkthroughIntent.Next
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                if (page > 0) {
                    TextButton(onClick = { onIntent(WalkthroughIntent.Back) }, modifier = Modifier.heightIn(min = 52.dp)) {
                        Text(stringResource(R.string.walkthrough_back))
                    }
                } else {
                    Spacer(Modifier)
                }
                Button(
                    onClick = { onForward(forwardIntent) },
                    modifier = Modifier.heightIn(min = 52.dp).then(forwardSemantics(forwardIntent)),
                ) {
                    Text(
                        stringResource(if (isLastPage) R.string.walkthrough_get_started else R.string.walkthrough_next),
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                }
            }
        }
    }
}

/** The city picker for the zmanim screen's Add city; a chosen city is saved and it closes. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CityPickerLayer(onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
            TopAppBar(
                title = { Text(stringResource(R.string.walkthrough_add_city)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
            CityPickerScreen(onLocationSaved = onClose)
        }
    }
}

/** Whether the user turned animations off (Remove animations / animator scale 0). */
@Composable
private fun rememberReducedMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}
