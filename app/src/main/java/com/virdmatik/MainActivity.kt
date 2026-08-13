package com.virdmatik

import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.virdmatik.data.local.ZikirEntity
import com.virdmatik.data.local.ZikirProgressRow
import com.virdmatik.ui.*
import com.virdmatik.ui.theme.VirdmatikTheme
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { VirdmatikTheme { VirdmatikApp() } }
    }
}

@Composable
fun VirdmatikApp() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "main") {
        composable("main") { MainTabs(onCounter = { id, date -> nav.navigate("counter/$id/$date") }) }
        composable(
            "counter/{zikirId}/{date}",
            arguments = listOf(navArgument("zikirId") { type = NavType.LongType }, navArgument("date") { type = NavType.StringType })
        ) { CounterScreen(onBack = { nav.popBackStack() }) }
    }
}

@Composable
private fun MainTabs(onCounter: (Long, String) -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Bugün") })
                NavigationBarItem(selected = tab == 1, onClick = { tab = 1 }, icon = { Icon(Icons.Default.DateRange, null) }, label = { Text("Geçmiş") })
                NavigationBarItem(selected = tab == 2, onClick = { tab = 2 }, icon = { Icon(Icons.Default.Settings, null) }, label = { Text("Zikirler") })
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (tab) {
                0 -> HomeScreen(onCounter)
                1 -> HistoryScreen(onCounter)
                else -> ManageScreen()
            }
        }
    }
}

@Composable
private fun HomeScreen(onCounter: (Long, String) -> Unit, vm: HomeViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.refresh() }
    DayContent(
        title = "Bugünün Virdi",
        date = state.date,
        items = state.items,
        progress = state.progress,
        onCounter = onCounter
    )
}

@Composable
private fun DayContent(title: String, date: String, items: List<ZikirProgressRow>, progress: Float, onCounter: (Long, String) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 18.dp, bottom = 28.dp)
    ) {
        item {
            Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(formatDate(date), style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(18.dp))
            Text("%${(progress * 100).toInt()} Tamamlandı", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
        }
        items(items, key = { it.id }) { item ->
            Card(Modifier.fillMaxWidth().clickable { onCounter(item.id, date) }) {
                Column(Modifier.padding(18.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(item.name, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text("${item.count} / ${item.target}", fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { if (item.target == 0) 0f else (item.count.toFloat() / item.target).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (item.completed) { Spacer(Modifier.height(8.dp)); Text("✓ Tamamlandı", color = MaterialTheme.colorScheme.primary) }
                }
            }
        }
        if (items.isEmpty()) item { Text("Günlük kayıt hazırlanıyor…") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CounterScreen(onBack: () -> Unit, vm: CounterViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        else @Suppress("DEPRECATION") (context.getSystemService(Vibrator::class.java))
    }
    var completionBuzzed by remember(state.date, state.name) { mutableStateOf(false) }
    LaunchedEffect(state.completed) {
        if (state.completed && !completionBuzzed) {
            vibrator?.vibrate(VibrationEffect.createOneShot(320, VibrationEffect.DEFAULT_AMPLITUDE))
            completionBuzzed = true
        }
        if (!state.completed) completionBuzzed = false
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text(state.name.ifBlank { "Zikirmatik" }) }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Geri") }
        })
    }) { padding ->
        if (state.loading) Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        else Column(
            Modifier.fillMaxSize().padding(padding).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(formatDate(state.date), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(22.dp))
            Text("${state.count} / ${state.target}", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.weight(1f))
            Button(
                onClick = {
                    if (!state.completed) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        vm.increment()
                    }
                },
                modifier = Modifier.size(260.dp),
                shape = CircleShape,
                enabled = !state.completed
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (state.completed) "✓" else state.count.toString(), fontSize = 64.sp, fontWeight = FontWeight.Bold)
                    Text(if (state.completed) "Hedef Tamamlandı" else "DOKUN", fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.weight(1f))
            OutlinedButton(onClick = { vm.undo() }, enabled = state.count > 0, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Undo, null); Spacer(Modifier.width(8.dp)); Text("Geri Al")
            }
        }
    }
}

@Composable
private fun HistoryScreen(onCounter: (Long, String) -> Unit, vm: HistoryViewModel = hiltViewModel(), homeVm: HomeViewModel = hiltViewModel()) {
    val history by vm.history.collectAsStateWithLifecycle()
    var selectedDate by remember { mutableStateOf<String?>(null) }
    if (selectedDate != null) {
        HistoricalDayScreen(selectedDate!!, onBack = { selectedDate = null }, onCounter = onCounter)
        return
    }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp), contentPadding = PaddingValues(top = 18.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("Geçmiş Günler", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text("Son 30 gün içindeki kayıtların") }
        items(history, key = { it.date }) { day ->
            val pct = if (day.totalTarget <= 0) 0 else (day.totalCount * 100 / day.totalTarget).coerceIn(0, 100)
            val done = day.itemCount > 0 && day.completedCount == day.itemCount
            Card(Modifier.fillMaxWidth().clickable { selectedDate = day.date }) {
                Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (done) Icons.Default.CheckCircle else Icons.Default.Schedule, null, tint = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) { Text(formatDate(day.date), fontWeight = FontWeight.SemiBold); Text(if (done) "Tamamlandı" else "Eksik vird var") }
                    Text("%$pct", fontWeight = FontWeight.Bold)
                }
            }
        }
        if (history.isEmpty()) item { Text("Henüz geçmiş kayıt yok. Uygulamayı kullandıkça günler burada birikecek.") }
    }
}

@Composable
private fun HistoricalDayScreen(date: String, onBack: () -> Unit, onCounter: (Long, String) -> Unit) {
    val vm: HistoricalDayViewModel = hiltViewModel(key = "history-$date")
    LaunchedEffect(date) { vm.load(date) }
    val state by vm.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Geri") }
            Text("Kaza Virdi", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Box(Modifier.weight(1f)) { DayContent("Eksik Günü Tamamla", date, state.items, state.progress, onCounter) }
    }
}

@Composable
private fun ManageScreen(vm: ManageViewModel = hiltViewModel()) {
    val zikirs by vm.zikirs.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<ZikirEntity?>(null) }
    var adding by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column { Text("Zikirler", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text("Günlük listenizi düzenleyin") }
            FilledIconButton(onClick = { adding = true }) { Icon(Icons.Default.Add, "Ekle") }
        }
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(zikirs, key = { it.id }) { z ->
                Card(Modifier.fillMaxWidth().clickable { editing = z }) {
                    Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text(z.name, fontWeight = FontWeight.SemiBold); Text("Hedef: ${z.target}") }
                        IconButton(onClick = { editing = z }) { Icon(Icons.Default.Edit, "Düzenle") }
                        IconButton(onClick = { vm.archive(z.id) }) { Icon(Icons.Default.DeleteOutline, "Sil") }
                    }
                }
            }
        }
    }
    if (adding) ZikirDialog(title = "Yeni Zikir", initialName = "", initialTarget = 100, onDismiss = { adding = false }) { n, t -> vm.add(n, t); adding = false }
    editing?.let { z -> ZikirDialog(title = "Zikri Düzenle", initialName = z.name, initialTarget = z.target, onDismiss = { editing = null }) { n, t -> vm.update(z.id, n, t); editing = null } }
}

@Composable
private fun ZikirDialog(title: String, initialName: String, initialTarget: Int, onDismiss: () -> Unit, onSave: (String, Int) -> Unit) {
    var name by remember { mutableStateOf(initialName) }
    var target by remember { mutableStateOf(initialTarget.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("Zikir / Esma") }, singleLine = true)
            OutlinedTextField(target, { target = it.filter(Char::isDigit) }, label = { Text("Günlük hedef") }, singleLine = true)
        } },
        confirmButton = { TextButton(onClick = { val t = target.toIntOrNull() ?: 0; if (name.isNotBlank() && t > 0) onSave(name, t) }) { Text("Kaydet") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("İptal") } }
    )
}

private fun formatDate(date: String): String = runCatching {
    LocalDate.parse(date).format(DateTimeFormatter.ofPattern("d MMMM yyyy, EEEE", Locale("tr", "TR")))
}.getOrDefault(date)
