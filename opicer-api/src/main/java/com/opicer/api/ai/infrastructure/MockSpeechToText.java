package com.opicer.api.ai.infrastructure;

import com.opicer.api.ai.application.SpeechToText;
import com.opicer.api.config.SttProperties;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.CRC32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.InputStreamSource;
import org.springframework.stereotype.Component;

/**
 * Offline stand-in for real STT: returns a canned learner answer instead of calling a provider, so the whole
 * practice → evaluation flow runs without API keys or cost. The audio is not read. The same hint (question
 * text) always yields the same transcript, and different questions spread across answers from IL to AL —
 * with typical learner mistakes left in — so downstream evaluation has realistic input.
 * Default provider; replace with {@code opicer.ai.stt.provider=openai}.
 */
@Component
@ConditionalOnProperty(name = SttProperties.PROVIDER_KEY, havingValue = "mock", matchIfMissing = true)
public class MockSpeechToText implements SpeechToText {

	private static final Logger log = LoggerFactory.getLogger(MockSpeechToText.class);

	/** Ordered roughly IL → AL. */
	static final List<String> TRANSCRIPTS = List.of(
		// IL: short, present tense, little linking
		"I live in apartment. It is small. I like my room because it is comfortable. I watch TV in my room every night.",
		// IM: linked sentences, article and agreement slips
		"Um, I usually go to the park near my house on weekends. The park is very big and there is many trees. "
			+ "I walk with my dog and sometimes I go jogging. I like the park because it is quiet and I can relax. "
			+ "After walking, I buy a coffee in the cafe and go home.",
		// IM: past experience with tense mistakes
		"Last summer I go to Jeju island with my family. We stayed in a hotel near the beach. The weather was really "
			+ "hot, so we swim in the sea every day. I eat a lot of seafood and it was so delicious. It was the best trip "
			+ "because I can spend a lot of time with my family, and I want to go there again next year.",
		// IH: comparison over time, mostly accurate
		"When I was a university student, I used to cook at home almost every day because eating out was too expensive. "
			+ "These days things are quite different. I work late, so I usually order food or grab something at a "
			+ "convenience store on my way home. Compared to the past, it is much more convenient, but honestly I think "
			+ "my eating habits got worse. So recently I started cooking simple meals on weekends, like pasta or kimchi "
			+ "fried rice, and I prepare some side dishes for the week. It takes time, but I feel healthier and I save money.",
		// IH: problem solving / role play
		"Hi, I'm calling because I bought a pair of headphones from your store last week, and there is a problem. "
			+ "The left side stopped working after two days. I checked the cable and tried it with my phone and my laptop, "
			+ "but it still doesn't work. I still have the receipt, so I'd like to exchange it for a new one. If that is "
			+ "not possible, could I get a refund instead? Also, could you tell me how long it would take? I need them for "
			+ "my commute, so it would be great if I could pick them up this weekend.",
		// AL: paragraph-level opinion with nuance
		"Honestly, I think technology has changed the way we spend our free time more than anything else. Ten years ago, "
			+ "if I had a free afternoon, I would call a friend and we would just walk around the city without a plan. "
			+ "Now, most of my free time happens on a screen, whether it's streaming a series or scrolling through social "
			+ "media. On the one hand, it's incredibly convenient, because I can watch a documentary or learn a new skill "
			+ "whenever I want. On the other hand, I've noticed that I rarely feel truly rested after a day like that. "
			+ "So lately I've been trying to set some boundaries, for example leaving my phone at home when I go hiking. "
			+ "Looking back, the moments I remember most clearly are the offline ones, and that's why I think balance "
			+ "matters more than ever."
	);

	public MockSpeechToText() {
		log.warn("[STT:mock] active — transcripts are canned samples, not real speech recognition."
			+ " Set opicer.ai.stt.provider=openai to use Whisper.");
	}

	@Override
	public String transcribe(InputStreamSource audio, String filename, String hint) {
		return TRANSCRIPTS.get(indexFor(hint == null || hint.isBlank() ? filename : hint));
	}

	@Override
	public String provider() {
		return "mock";
	}

	/** Stable across JVMs (unlike String.hashCode semantics being an implementation detail). */
	static int indexFor(String key) {
		CRC32 crc = new CRC32();
		crc.update((key == null ? "" : key).getBytes(StandardCharsets.UTF_8));
		return (int) (crc.getValue() % TRANSCRIPTS.size());
	}
}
