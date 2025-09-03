package niconicotagger.serde

import com.mariten.kanatools.KanaConverter
import com.mariten.kanatools.KanaConverter.OP_HAN_KATA_TO_ZEN_KATA
import com.mariten.kanatools.KanaConverter.OP_ZEN_ASCII_TO_HAN_ASCII
import com.mariten.kanatools.KanaConverter.OP_ZEN_KATA_TO_ZEN_HIRA
import java.time.Duration

object Utils {
    private const val NORMALIZATION_FLAGS = OP_HAN_KATA_TO_ZEN_KATA or OP_ZEN_ASCII_TO_HAN_ASCII

    internal fun normalizeToken(s: String): String {
        return if (s == "OR") s else KanaConverter.convertKana(s, NORMALIZATION_FLAGS)
    }

    internal fun kata2hiraAndLowercase(s: String): String {
        return KanaConverter.convertKana(s, OP_ZEN_KATA_TO_ZEN_HIRA).lowercase()
    }

    internal fun durationToTimeString(value: Duration) =
        if (value.toHours() > 0) "${value.toHoursPart()}:%02d:%02d".format(value.toMinutesPart(), value.toSecondsPart())
        else "${value.toMinutesPart()}:%02d".format(value.toSecondsPart())
}
