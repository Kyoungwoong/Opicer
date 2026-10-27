package com.opicer.api.mocktest.application;

import com.opicer.api.mocktest.domain.SurveyCatalog;
import com.opicer.api.mocktest.domain.SurveyCatalog.SurveyItem;
import com.opicer.api.mocktest.domain.SurveyCatalog.SurveyPart;
import com.opicer.api.mocktest.domain.SurveyTopicLink;
import com.opicer.api.mocktest.infrastructure.SurveyTopicLinkRepository;
import com.opicer.api.shared.error.ApiException;
import com.opicer.api.shared.error.ErrorCode;
import com.opicer.api.topic.domain.Topic;
import com.opicer.api.topic.infrastructure.TopicRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Which practice topics each Background Survey item points to, as edited on the admin screen. */
@Service
public class SurveyTopicLinkService {

	/** Within one item, linked topics come in the practice list's order. */
	private static final Comparator<Topic> TOPIC_ORDER = Comparator.comparingInt(Topic::getCategoryOrder)
		.thenComparingInt(Topic::getTopicOrder)
		.thenComparing(Topic::getTitle);

	private final SurveyTopicLinkRepository links;
	private final TopicRepository topics;

	public SurveyTopicLinkService(SurveyTopicLinkRepository links, TopicRepository topics) {
		this.links = links;
		this.topics = topics;
	}

	/** Titles of the active topics the chosen items link to, in survey order and without repeats. */
	@Transactional(readOnly = true)
	public List<String> topicsOf(Map<String, List<String>> selections) {
		List<String> chosen = new ArrayList<>();
		for (SurveyPart part : SurveyCatalog.PARTS) {
			chosen.addAll(chosen(selections, part.code()));
		}
		return activeTitles(chosen);
	}

	/** Titles of the active topics the chosen residence links to; part 1 falls back to these combos. */
	@Transactional(readOnly = true)
	public List<String> residenceTopics(Map<String, List<String>> selections) {
		return activeTitles(chosen(selections, SurveyCatalog.RESIDENCE));
	}

	/** Every survey item with the topics it links to. */
	@Transactional(readOnly = true)
	public List<PartLinks> overview() {
		Map<String, List<Topic>> linked = linkedTopics(SurveyCatalog.PARTS.stream()
			.flatMap(part -> part.items().stream())
			.map(SurveyItem::id)
			.toList());
		return SurveyCatalog.PARTS.stream()
			.map(part -> new PartLinks(part.code(), part.title(), part.items().stream()
				.map(item -> ItemLinks.of(item, linked.getOrDefault(item.id(), List.of())))
				.toList()))
			.toList();
	}

	/** Replaces an item's links; only topics of the 설문조사 category can be linked. */
	@Transactional
	public ItemLinks replace(String itemId, List<UUID> topicIds) {
		SurveyItem item = SurveyCatalog.findItem(itemId)
			.orElseThrow(() -> new ApiException(ErrorCode.SURVEY_ITEM_NOT_FOUND, "Survey item not found: " + itemId));
		Set<UUID> wanted = new LinkedHashSet<>(topicIds == null ? List.of() : topicIds);
		List<Topic> found = topics.findAllById(wanted);
		if (found.size() != wanted.size()) {
			throw new ApiException(ErrorCode.VALIDATION_ERROR, "없는 주제가 들어 있어요. 화면을 새로고침해 주세요.");
		}
		if (found.stream().anyMatch(topic -> !Topic.CATEGORY_SURVEY.equals(topic.getCategory()))) {
			throw new ApiException(ErrorCode.VALIDATION_ERROR, "설문 항목에는 설문조사 분류의 주제만 연결할 수 있어요.");
		}
		links.deleteByItemId(itemId);
		links.saveAll(wanted.stream().map(topicId -> new SurveyTopicLink(itemId, topicId)).toList());
		return ItemLinks.of(item, found.stream().sorted(TOPIC_ORDER).toList());
	}

	/** Topic ids some survey item links to, for the admin topic list. */
	@Transactional(readOnly = true)
	public Set<UUID> linkedTopicIds() {
		return links.findLinkedTopicIds();
	}

	private List<String> activeTitles(List<String> itemIds) {
		Map<String, List<Topic>> linked = linkedTopics(itemIds);
		Set<String> titles = new LinkedHashSet<>();
		for (String itemId : itemIds) {
			linked.getOrDefault(itemId, List.of()).stream()
				.filter(Topic::isActive)
				.forEach(topic -> titles.add(topic.getTitle()));
		}
		return List.copyOf(titles);
	}

	/** Linked topics per item id, each list in {@link #TOPIC_ORDER}. */
	private Map<String, List<Topic>> linkedTopics(List<String> itemIds) {
		if (itemIds.isEmpty()) {
			return Map.of();
		}
		List<SurveyTopicLink> found = links.findByItemIdIn(itemIds);
		Map<UUID, Topic> byId = topics.findAllById(found.stream().map(SurveyTopicLink::getTopicId).collect(Collectors.toSet()))
			.stream()
			.collect(Collectors.toMap(Topic::getId, Function.identity()));
		return found.stream()
			.filter(link -> byId.containsKey(link.getTopicId()))
			.collect(Collectors.groupingBy(SurveyTopicLink::getItemId, Collectors.collectingAndThen(
				Collectors.mapping(link -> byId.get(link.getTopicId()), Collectors.toList()),
				list -> list.stream().sorted(TOPIC_ORDER).toList())));
	}

	/** A part sent as {@code null} counts as nothing chosen. */
	private static List<String> chosen(Map<String, List<String>> selections, String code) {
		List<String> chosen = selections == null ? null : selections.get(code);
		return chosen == null ? List.of() : chosen;
	}

	public record PartLinks(String code, String title, List<ItemLinks> items) {}

	public record ItemLinks(String id, String label, List<LinkedTopic> topics) {

		static ItemLinks of(SurveyItem item, List<Topic> topics) {
			return new ItemLinks(item.id(), item.label(), topics.stream()
				.map(topic -> new LinkedTopic(topic.getId(), topic.getTitle(), topic.isActive()))
				.toList());
		}
	}

	public record LinkedTopic(UUID id, String title, boolean active) {}
}
