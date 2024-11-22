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
            // TestAny()
            //TestMyConverter()
            TopNav()
            //TestMySerializer()
        }
    }
}

@Composable
fun TestMySerializer() {
    val backStack = rememberSaveable(
        saver = Saver(
            save = { state ->
                encodeToSavedState(snapshotStateListSerializer(), EmptySerializersModule(), state)
            },
            restore = { value ->
                decodeFromSavedState(snapshotStateListSerializer(), EmptySerializersModule(), value)
            }
        )
    ) {
        mutableStateListOf("A")
    }

    val manager = rememberNavWrapperManager(emptyList())

    MyNavDisplay(
        backstack = backStack,
        wrapperManager = manager,
        onBack = {
            backStack.removeAt(backStack.size - 1)
        }
    ) {
        record("A") {
            Column {
                Text("A")
                Button(onClick = { backStack.add("B") }) {
                    Text("Goto B")
                }
            }
        }
        record("B") { Text("B") }
    }
}

//interface Destination
//@Composable
//fun TestMyConverter() {
//
//    @Serializable
//    class Home: Destination
//
//    @Serializable
//    class Details: Destination
//
//    val module = SerializersModule {
//        polymorphic(Destination::class) {
//            subclass(Home::class)
//            subclass(Details::class)
//        }
//    }
//    val backStack = rememberSaveable(
//        saver = serializableSaver<SnapshotStateList<Any>, List<Any>>(
//            module,
//            toSerializable = { toList() },
//            fromSerializable = { toMutableStateList() }
//        )
//    ) {
//        mutableStateListOf<Any>(Home)
//    }
//
//    val manager = rememberNavWrapperManager(listOf(ViewModelStoreNavContentWrapper))
//
//    MyNavDisplay(
//        backstack = backStack,
//        wrapperManager = manager,
//        onBack = {
//            Log.d("gyz", "onback called")
//            backStack.removeAt(backStack.size - 1)
//        }
//    ) {
//        record(Home) { Home { backStack.add(it) } }
//        record(Details) { Details("a") }
//        record("foo") { Details("b") }
//    }
//}

@Serializable
object Home

@Serializable
object Details

@Composable
fun TestAnyWithSnapshotStateListSaver() {
    val module = SerializersModule {
        polymorphic(Any::class) {
            subclass(Home::class)
            subclass(Details::class)
            subclass(String::class)
        }
    }

    val serializer = serializer(
        kClass = MutableList::class,
        typeArgumentsSerializers = listOf(PolymorphicSerializer(Any::class)),
        isNullable = false
    )

    val backStack = rememberSaveable(saver = snapshotStateListSaver(
        listSaver = listSaver(
            save = { list -> list.map { encodeToSavedState(it) } },
            restore = { list -> list.map { decodeFromSavedState(it) } }
        )
    )) {
        mutableStateListOf<Any>(Home)
    }

    val manager = rememberNavWrapperManager(listOf(ViewModelStoreNavContentWrapper))

    MyNavDisplay(
        backstack = backStack,
        wrapperManager = manager,
        onBack = { backStack.removeAt(backStack.size - 1) }
    ) {
        record(Home) { Home { backStack.add(it) } }
        record(Details) { Details("a") }
        record("foo") { Details("b") }
    }
}

inline fun <reified T> snapshotStateListSerializer(): KSerializer<SnapshotStateList<T>> {
    return object: KSerializer<SnapshotStateList<T>> {
        private val delegateSerializer = serializer<MutableList<T>>()

        override val descriptor: SerialDescriptor = SerialDescriptor("SnapshotStateListSerializer", delegateSerializer.descriptor)

        override fun serialize(encoder: Encoder, value: SnapshotStateList<T>) {
            encoder.encodeSerializableValue(delegateSerializer, value)
        }

        override fun deserialize(decoder: Decoder): SnapshotStateList<T> {
            val restored = decoder.decodeSerializableValue(delegateSerializer)
            return restored.toMutableStateList()
        }
    }
}

class MyViewModel(handle: SavedStateHandle): ViewModel() {
    init {
        Log.d("gyz", "vm init")
    }
    val backStack by handle.saved(serializer = snapshotStateListSerializer<String>()) {
        Log.d("gyz", "init")
        mutableStateListOf("A")
    }

    val backStackA = mutableListOf("A")

    val backStackB = mutableListOf("B")

    val manager = NavWrapperManager(listOf(ViewModelStoreNavContentWrapper))

    fun goto(destination: String) {
        Log.d("gyz", "backstack: ${backStack::class}")
        when (destination) {
            "A" -> {
                backStack.clear()
                backStack.addAll(backStackA)
            }
            "B" -> {
                backStack.clear()
                backStack.addAll(backStackB)
            }
        }
    }

    fun goBack() {
        backStack.removeAt(backStack.size - 1)
    }
}

class ViewModelA(handle: SavedStateHandle): ViewModel()

class ViewModelB(handle: SavedStateHandle): ViewModel()

@Composable
fun TopNav() {
    val vm = viewModel<MyViewModel>()
    Column {
        Row {
            Button(onClick = { vm.goto("A") }) {
                Text("A")
            }
            Button(onClick = { vm.goto("B") }) {
                Text("B")
            }
        }
        Box(
            Modifier
                .padding(10.dp)
                .border(BorderStroke(10.dp, Color.Blue))) {
            NavDisplay(
                backstack = vm.backStack,
                wrapperManager = vm.manager,
                onBack = { vm.goBack() },
                recordProvider = recordProvider {
                    record("A") {

                    }
                    record("B") {
                        Column {
                            Text("B")
                            Button(onClick = { vm.goto("B.A") }) {
                                Text("B.A")
                            }
                            Button(onClick = { vm.goto("B.B") }) {
                                Text("B.B")
                            }
                        }
                    }
                    record("A.A") {
                        Column {
                            Text("A.A")
                        }
                    }
                    record("A.B") {
                        Column {
                            Text("A.B")
                        }
                    }
                }
            )
        }
    }
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


inline fun <reified NonSerializable: Any, reified Serializable: Any> serializableSaver(
    module: SerializersModule,
    crossinline toSerializable: NonSerializable.() -> Serializable,
    crossinline fromSerializable: Serializable.() -> NonSerializable
): Saver<NonSerializable, SavedState> {
    return Saver(
        save = { encodeToSavedState(serializer(), module, it.toSerializable()).also { Log.d("gyz", it.toString()) } },
        restore = { decodeFromSavedState<Serializable>(serializer(), module, it).fromSerializable() }
    )
}

//inline fun <reified T> serializableSaver(): Saver<SnapshotStateList<T>, SavedState> =
//    Saver(
//        save = { encodeToSavedState(it.toList()) },
//        restore = { decodeFromSavedState<List<T>>(it).toMutableStateList() }
//    )

@Composable
fun <T : Any> MyNavDisplay(
    backstack: List<T>,
    wrapperManager: NavWrapperManager,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    builder: RecordProviderBuilder.() -> Unit
) {
    return NavDisplay(
        backstack = backstack,
        wrapperManager = wrapperManager,
        modifier = modifier,
        onBack = onBack,
        recordProvider = recordProvider(builder = builder)
    )
}

@Composable
fun Home(goto: (Any) -> Unit) {
    Column {
        Text("Home Screen")
        Button(onClick = { goto(Details) }) { Text("A") }
        Button(onClick = { goto("foo") }) { Text("B") }
    }
}

@Composable
fun Details(text: String) {
    Column {
        Text(text)
    }
}
