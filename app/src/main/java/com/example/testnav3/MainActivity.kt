package com.example.testnav3

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.autoSaver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.saved
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.ViewModelStoreNavContentWrapper
import androidx.navigation3.NavDisplay
import androidx.navigation3.NavWrapperManager
import androidx.navigation3.RecordProviderBuilder
import androidx.navigation3.record
import androidx.navigation3.recordProvider
import androidx.navigation3.rememberNavWrapperManager
import androidx.savedstate.SavedState
import kotlinx.serialization.KSerializer
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import kotlinx.serialization.serializer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TestAny()
        }
    }
}

@Serializable
object Home

@Serializable
object Details

@Composable
fun TestAny() {
    val module = SerializersModule {
        polymorphic(Any::class) {
            subclass(Home::class)
            subclass(Details::class)
            subclass(String::class)
        }
    }

    val backStack = rememberSaveable(saver = snapshotStateListSaver(
        listSaver = listSaver(
            save = { it.map { encodeToSavedState(PolymorphicSerializer(Any::class), module, it) } },
            restore = { it.map { decodeFromSavedState(PolymorphicSerializer(Any::class), module, it) } }
        )
    )) {
        mutableStateListOf<Any>(Home)
    }

    val manager = rememberNavWrapperManager(emptyList())

    NavDisplay(
        backstack = backStack,
        wrapperManager = manager,
        onBack = { backStack.removeAt(backStack.size - 1) },
        recordProvider = recordProvider {
            record(Home) { Home { backStack.add(it) } }
            record(Details) { Details("A") }
            record("foo") { Details("B") }
        }
    )
}

@Suppress("UNCHECKED_CAST")
fun <T> snapshotStateListSaver(
    listSaver: Saver<List<T>, out Any> = autoSaver()
): Saver<SnapshotStateList<T>, Any> =
    with(listSaver as Saver<List<T>, Any>) {
        Saver(
            save = { state ->
                // We use toMutableList() here to ensure that save() is
                // sent a list that is saveable by default (e.g., something
                // that autoSaver() can handle)
                save(state.toList().toMutableList())
            },
            restore = { state -> restore(state)?.toMutableStateList() }
        )
    }

@Composable
fun Home(goto: (Any) -> Unit) {
    Column {
        Text("Home Screen")
        Button(onClick = { goto(Details) }) { Text("Go to A") }
        Button(onClick = { goto("foo") }) { Text("Go to B") }
    }
}

@Composable
fun Details(text: String) {
    Column {
        Text(text)
    }
}
