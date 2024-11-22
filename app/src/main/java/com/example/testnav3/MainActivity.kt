package com.example.testnav3

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.navigation3.NavDisplay
import androidx.navigation3.record
import androidx.navigation3.recordProvider
import androidx.navigation3.rememberNavWrapperManager
import androidx.savedstate.serialization.decodeFromSavedState
import androidx.savedstate.serialization.encodeToSavedState
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.serializer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TestMySerializer()
        }
    }
}

@Composable
fun TestMySerializer() {
    val backStack = rememberSaveable(
        saver = Saver(
            save = { encodeToSavedState(snapshotStateListSerializer(), it) },
            restore = { decodeFromSavedState(snapshotStateListSerializer(), it) }
        )
    ) {
        mutableStateListOf("A")
    }

    val manager = rememberNavWrapperManager(emptyList())

    NavDisplay(
        backstack = backStack,
        wrapperManager = manager,
        onBack = {
            backStack.removeAt(backStack.size - 1)
        },
        recordProvider = recordProvider {
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
    )
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

