package dev.infinitetokens.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AngryVoiceTest {
    @Test
    fun prefixesRussianRepliesWithoutChangingTheRestOfTheAnswer() {
        assertEquals("Блядь, Привет.", AngryVoice.prefix("Привет.", "Как дела?"))
    }

    @Test
    fun prefixesEnglishRepliesAndDoesNotDoubleAnExistingCurse() {
        assertEquals("Fuck, Hello.", AngryVoice.prefix("Hello.", "How are you?"))
        assertEquals("fuck off", AngryVoice.prefix("fuck off", "How are you?"))
    }
}
