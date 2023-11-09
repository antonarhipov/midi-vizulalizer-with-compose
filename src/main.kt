import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.flow.MutableStateFlow
import javax.sound.midi.MidiMessage
import javax.sound.midi.MidiSystem
import javax.sound.midi.Receiver

const val midiDeviceName = "MPKmini2"

fun main() = application {
    val flow = MutableStateFlow<MidiMessage>(DummyMidiMessage)
    receiveMidiMessages(flow)

    Window(
        onCloseRequest = ::exitApplication,
        title = "MIDI Visualizer",
        state = rememberWindowState(width = 800.dp, height = 800.dp)
    ) {
        val message by flow.collectAsState(DummyMidiMessage)

        //TODO: animate fade out

        Canvas(modifier = Modifier.fillMaxSize().background(Color.White).clipToBounds()) {
            displayNoteOn(message)
        }
    }
}

fun receiveMidiMessages(flow: MutableStateFlow<MidiMessage>) {
    System.setProperty("javax.sound.midi.Transmitter", "javax.sound.midi.Transmitter#$midiDeviceName")
    MidiSystem.getTransmitter().receiver = object : Receiver {
        override fun send(message: MidiMessage, timeStamp: Long) {
            val status = message.getStatus()

            if (status == 0xf8) return
            if (status == 0xfe) return

            // Strip channel number out of status
            val leftNibble = status and 0xf0
            when (leftNibble) {
                // only emit "note on" messages
                0x90 -> flow.tryEmit(message)
            }
        }
        override fun close() {}
    }
}

@OptIn(ExperimentalStdlibApi::class)
private fun DrawScope.displayNoteOn(message: MidiMessage) {
    if (message.length < 3 || message.length % 2 == 0) return // Bad MIDI message

    val bytes = message.message
    println("Message: ${bytes.toHexString()}")
    val noteNumber = byteToInt(bytes[1])

    val color = Color(
        (0..255).random(),
        (0..255).random(),
        (0..255).random(),
    )

    drawCircle(
        color = color,
        radius = (noteNumber * 10).toFloat(),
        center = center,
        style = Stroke(width = 20f),
    )
}

private fun byteToInt(b: Byte) = b.toInt() and 0xff

// not supposed to be used for anything
object DummyMidiMessage : MidiMessage(ByteArray(0)) {
    override fun clone(): Any {
        TODO("This is a dummy message")
    }
}