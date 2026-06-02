package podcastService.transcript.service;

import org.junit.jupiter.api.Test;
import podcastService.transcript.service.VttSpeakerBlockParser.SpeakerBlock;

import static org.assertj.core.api.Assertions.assertThat;

class VttSpeakerBlockParserTest {

    private final VttSpeakerBlockParser parser = new VttSpeakerBlockParser();

    @Test
    void consecutiveSpeakerCuesAreMergedIntoBlocks() {
        assertThat(parser.parse("""
                WEBVTT

                00:00:07.560 --> 00:00:10.840
                SPEAKER_00: океан и включающие Центральный Арктический бассейн, это

                00:00:10.840 --> 00:00:14.360
                SPEAKER_00: глубокая такая чашка, и вот эти сравнительно мелководные

                00:00:14.360 --> 00:00:18.480
                SPEAKER_01: моря, Арктические моря России, других стран ещё есть моря.

                00:00:18.480 --> 00:00:21.000
                SPEAKER_01: Это так называемый шельф, вот он покрыт морями, и

                00:00:21.000 --> 00:00:22.000
                SPEAKER_00: снова первый спикер
                """))
                .containsExactly(
                        new SpeakerBlock(
                                "океан и включающие Центральный Арктический бассейн, это "
                                        + "глубокая такая чашка, и вот эти сравнительно мелководные",
                                "speaker_00"
                        ),
                        new SpeakerBlock(
                                "моря, Арктические моря России, других стран ещё есть моря. "
                                        + "Это так называемый шельф, вот он покрыт морями, и",
                                "speaker_01"
                        ),
                        new SpeakerBlock("снова первый спикер", "speaker_00")
                );
    }

    @Test
    void multilineCueIsAddedToItsSpeakerBlockAndMarkupIsRemoved() {
        assertThat(parser.parse("""
                WEBVTT

                00:00:00.000 --> 00:00:02.000
                SPEAKER_00: <b>Первая строка</b>
                и продолжение
                """))
                .containsExactly(new SpeakerBlock("Первая строка и продолжение", "speaker_00"));
    }
}
