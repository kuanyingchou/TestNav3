package com.example.testnav3

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
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
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.serializer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TestViewModel()
        }
    }
}

@Composable
fun TestViewModel() {
    val vm = viewModel<MyViewModel>()

    MyNavDisplay(
        backstack = vm.backStack,
        wrapperManager = vm.manager,
        onBack = {
            vm.goBack()
        }
    ) {
        record("Home") {
            Column {
                Text("Home")
                Button(onClick = { vm.goto("Details") }) {
                    Text("Go to Details")
                }
            }
        }
        record("Details") { Text("Details") }
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

    val backStack by handle.saved(serializer = snapshotStateListSerializer<String>()) {
        mutableStateListOf("Home")
    }

    val manager = NavWrapperManager(listOf(ViewModelStoreNavContentWrapper))

    fun goto(destination: String) {
        when (destination) {
            "Home" -> {
                backStack.add("Home")
            }
            "Details" -> {
                backStack.add("Details")
            }
        }
    }

    fun goBack() {
        backStack.removeAt(backStack.size - 1)
    }
}

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
