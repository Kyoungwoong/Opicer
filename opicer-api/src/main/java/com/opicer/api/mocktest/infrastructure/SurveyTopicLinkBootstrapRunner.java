package com.opicer.api.mocktest.infrastructure;

import com.opicer.api.mocktest.domain.SurveyCatalog;
import com.opicer.api.mocktest.domain.SurveyTopicLink;
import com.opicer.api.topic.domain.Topic;
import com.opicer.api.topic.infrastructure.TopicRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fills an empty survey link table from the default topics in {@link SurveyCatalog}, so a fresh or upgraded database
 * picks survey combos as before. Once any link exists the table belongs to the admin screen and is left alone.
 * Default titles with no topic yet are skipped; the table is tried again on the next start while it stays empty.
 */
@Component
public class SurveyTopicLinkBootstrapRunner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(SurveyTopicLinkBootstrapRunner.class);

	private final SurveyTopicLinkRepository links;
	private final TopicRepository topics;

	public SurveyTopicLinkBootstrapRunner(SurveyTopicLinkRepository links, TopicRepository topics) {
		this.links = links;
		this.topics = topics;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (links.count() > 0) {
			return;
		}
		List<SurveyTopicLink> seeded = new ArrayList<>();
		Set<String> missing = new TreeSet<>();
		SurveyCatalog.PARTS.forEach(part -> part.items().forEach(item -> item.defaultTopics().forEach(title -> {
			Optional<Topic> topic = topics.findByTitle(title);
			if (topic.isPresent()) {
				seeded.add(new SurveyTopicLink(item.id(), topic.get().getId()));
			} else {
				missing.add(title);
			}
		})));
		links.saveAll(seeded);
		if (!seeded.isEmpty()) {
			log.info("[MockTest] seeded {} survey topic links", seeded.size());
		}
		if (!missing.isEmpty()) {
			log.info("[MockTest] survey default topics not found, left unlinked: {}", missing);
		}
	}
}
