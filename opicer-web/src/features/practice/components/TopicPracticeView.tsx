import { useEffect, useMemo, useRef, useState } from "react";
import { TopNav } from "@/components/common/TopNav";
import {
  fetchPracticeQuestions,
  fetchTopics,
  submitTopicSelection,
} from "@/features/practice/api";
import type {
  PracticeAnswer,
  PracticeQuestion,
  TopicCategory,
  TopicItem,
} from "@/features/practice/types";

type Props = {
  userLabel?: string;
  onLogout?: () => void;
};

const CATEGORY_META: Record<string, string> = {
  "프로필/배경": "서베이 기본 정보",
  "여가/일상": "취미와 일상 활동",
  "취미/관심사": "개인 취미 중심",
  "운동/건강": "운동 관련 경험",
  "여행/출장": "이동과 휴가 경험",
  "돌발/상황 대처": "돌발 주제 빈출",
};

const PRACTICE_DURATION_SECONDS = 120;
const REPLAY_WINDOW_SECONDS = 5;

export function TopicPracticeView({ userLabel, onLogout }: Props) {
  const [topics, setTopics] = useState<TopicItem[]>([]);
  const [activeCategoryId, setActiveCategoryId] = useState<string | null>(null);
  const [query, setQuery] = useState("");
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [mode, setMode] = useState<"select" | "ready" | "practice" | "summary">("select");
  const [questions, setQuestions] = useState<PracticeQuestion[]>([]);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [answers, setAnswers] = useState<PracticeAnswer[]>([]);

  const [isRecording, setIsRecording] = useState(false);
  const [remainingSeconds, setRemainingSeconds] = useState(
    PRACTICE_DURATION_SECONDS
  );
  const timerRef = useRef<number | null>(null);
  const recorderRef = useRef<MediaRecorder | null>(null);
  const chunksRef = useRef<Blob[]>([]);
  const audioRef = useRef<HTMLAudioElement | null>(null);
  const voicesRef = useRef<SpeechSynthesisVoice[]>([]);
  const replayTimerRef = useRef<number | null>(null);
  const [replayWindow, setReplayWindow] = useState(0);
  const [hasReplayed, setHasReplayed] = useState(false);
  const [hasPlayed, setHasPlayed] = useState(false);

  useEffect(() => {
    if (!("speechSynthesis" in window)) return;
    const load = () => { voicesRef.current = window.speechSynthesis.getVoices(); };
    load();
    window.speechSynthesis.addEventListener("voiceschanged", load);
    return () => { window.speechSynthesis.removeEventListener("voiceschanged", load); };
  }, []);

  useEffect(() => {
    let mounted = true;
    setIsLoading(true);
    fetchTopics()
      .then((data) => {
        if (!mounted) return;
        setTopics(data);
        const firstCategory = data
          .sort((a, b) => a.categoryOrder - b.categoryOrder)
          .map((topic) => topic.category)[0];
        setActiveCategoryId(firstCategory ?? null);
      })
      .catch((err) => {
        if (!mounted) return;
        setError(err.message ?? "주제를 불러오는 중 문제가 발생했습니다.");
      })
      .finally(() => {
        if (mounted) setIsLoading(false);
      });
    return () => {
      mounted = false;
    };
  }, []);

  const categories = useMemo<TopicCategory[]>(() => {
    const grouped = new Map<string, TopicItem[]>();
    const categoryOrderMap = new Map<string, number>();
    topics.forEach((topic) => {
      if (!grouped.has(topic.category)) {
        grouped.set(topic.category, []);
        categoryOrderMap.set(topic.category, topic.categoryOrder);
      }
      grouped.get(topic.category)?.push(topic);
    });
    return Array.from(grouped.entries())
      .map(([category, items]) => ({
        id: category,
        label: category,
        description: CATEGORY_META[category] ?? "주제별 연습",
        topics: items.sort((a, b) => a.topicOrder - b.topicOrder),
        order: categoryOrderMap.get(category) ?? 0,
      }))
      .sort((a, b) => a.order - b.order)
      .map(({ order, ...rest }) => rest);
  }, [topics]);

  const activeCategory = useMemo<TopicCategory | undefined>(
    () => categories.find((c) => c.id === activeCategoryId),
    [categories, activeCategoryId]
  );

  const filteredTopics = useMemo(() => {
    const base = activeCategory?.topics ?? [];
    const trimmed = query.trim().toLowerCase();
    if (!trimmed) return base;
    return base.filter(
      (topic) =>
        topic.title.toLowerCase().includes(trimmed) ||
        topic.englishTitle.toLowerCase().includes(trimmed)
    );
  }, [activeCategory, query]);

  const selectedTopic = useMemo<TopicItem | undefined>(() => {
    return topics.find((topic) => topic.id === selectedId);
  }, [topics, selectedId]);

  const currentQuestion = questions[currentIndex];
  const isLastQuestion = currentIndex === questions.length - 1;

  const handleSelect = (topic: TopicItem) => {
    setSuccessMessage(null);
    setSelectedId((prev) => (prev === topic.id ? null : topic.id));
  };

  const resetPracticeState = () => {
    stopRecording();
    stopTimers();
    setHasPlayed(false);
    setHasReplayed(false);
    setReplayWindow(0);
    setRemainingSeconds(PRACTICE_DURATION_SECONDS);
  };

  const handleSubmit = async () => {
    if (!selectedId) return;
    setIsSubmitting(true);
    setError(null);
    setSuccessMessage(null);
    try {
      await submitTopicSelection(selectedId);
      setMode("ready");
    } catch (err: any) {
      setError(err?.message ?? "선택 저장 중 문제가 발생했습니다.");
    } finally {
      setIsSubmitting(false);
    }
  };

  const startPractice = async () => {
    if (!selectedId) {
      setError("주제를 선택한 뒤 연습을 시작해주세요.");
      return;
    }
    setIsLoading(true);
    setError(null);
    try {
      const data = await fetchPracticeQuestions(selectedId);
      if (data.length === 0) {
        setError("선택한 주제에 등록된 질문이 없습니다.");
        setIsLoading(false);
        return;
      }
      setQuestions(data);
      setCurrentIndex(0);
      setAnswers([]);
      setMode("practice");
      resetPracticeState();
    } catch (err: any) {
      setError(err?.message ?? "질문을 불러오는 중 문제가 발생했습니다.");
    } finally {
      setIsLoading(false);
    }
  };

  const startRecording = async () => {
    if (isRecording) return;
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      const recorder = new MediaRecorder(stream);
      chunksRef.current = [];
      recorder.ondataavailable = (event) => {
        if (event.data.size > 0) {
          chunksRef.current.push(event.data);
        }
      };
      recorder.onstop = () => {
        stream.getTracks().forEach((track) => track.stop());
        const blob = new Blob(chunksRef.current, { type: "audio/webm" });
        const url = URL.createObjectURL(blob);
        setAnswers((prev) => {
          const question = currentQuestion;
          if (!question) return prev;
          const next = prev.filter((a) => a.questionId !== question.id);
          return [
            ...next,
            {
              questionId: question.id,
              questionText: question.promptText,
              audioUrl: url,
              recordedAt: new Date().toISOString(),
            },
          ];
        });
      };
      recorderRef.current = recorder;
      recorder.start();
      setIsRecording(true);
      startTimer();
    } catch (err) {
      setError("마이크 접근이 필요합니다. 권한을 확인해주세요.");
    }
  };

  const stopRecording = () => {
    if (recorderRef.current && recorderRef.current.state !== "inactive") {
      recorderRef.current.stop();
    }
    recorderRef.current = null;
    setIsRecording(false);
  };

  const startTimer = () => {
    stopTimers();
    setRemainingSeconds(PRACTICE_DURATION_SECONDS);
    timerRef.current = window.setInterval(() => {
      setRemainingSeconds((prev) => {
        if (prev <= 1) {
          stopRecording();
          stopTimers();
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
  };

  const startReplayWindow = () => {
    setReplayWindow(REPLAY_WINDOW_SECONDS);
    if (replayTimerRef.current) {
      window.clearInterval(replayTimerRef.current);
    }
    replayTimerRef.current = window.setInterval(() => {
      setReplayWindow((prev) => {
        if (prev <= 1) {
          if (replayTimerRef.current) {
            window.clearInterval(replayTimerRef.current);
          }
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
  };

  const stopTimers = () => {
    if (timerRef.current) {
      window.clearInterval(timerRef.current);
      timerRef.current = null;
    }
    if (replayTimerRef.current) {
      window.clearInterval(replayTimerRef.current);
      replayTimerRef.current = null;
    }
  };

  const getEnglishFemaleVoice = (): SpeechSynthesisVoice | null => {
    const voices = voicesRef.current.length
      ? voicesRef.current
      : window.speechSynthesis.getVoices();
    const enVoices = voices.filter((v) => v.lang.startsWith("en"));
    const femaleNames = [
      "Samantha", "Karen", "Ava", "Victoria", "Susan", "Allison",
      "Google US English", "Google UK English Female",
    ];
    return (
      enVoices.find((v) => femaleNames.some((name) => v.name.includes(name))) ??
      enVoices[0] ??
      null
    );
  };

  const playQuestion = () => {
    if (!currentQuestion) return;
    if (currentQuestion.promptAudioUrl) {
      if (!audioRef.current) {
        audioRef.current = new Audio(currentQuestion.promptAudioUrl);
      } else {
        audioRef.current.src = currentQuestion.promptAudioUrl;
      }
      audioRef.current.play().catch(() => {
        setError("오디오 재생에 실패했습니다.");
      });
    } else if ("speechSynthesis" in window) {
      const utterance = new SpeechSynthesisUtterance(currentQuestion.promptText);
      utterance.lang = "en-US";
      utterance.rate = 0.9;
      const femaleVoice = getEnglishFemaleVoice();
      if (femaleVoice) utterance.voice = femaleVoice;
      window.speechSynthesis.cancel();
      window.speechSynthesis.speak(utterance);
    }
    if (!hasPlayed) {
      setHasPlayed(true);
      startReplayWindow();
      startRecording();
    }
  };

  const handlePlayButton = () => {
    if (!hasPlayed) {
      playQuestion();
    } else if (!hasReplayed && replayWindow > 0) {
      setHasReplayed(true);
      playQuestion();
    }
  };

  const goToNext = () => {
    stopRecording();
    stopTimers();
    if ("speechSynthesis" in window) {
      window.speechSynthesis.cancel();
    }
    setHasPlayed(false);
    setHasReplayed(false);
    setReplayWindow(0);
    setRemainingSeconds(PRACTICE_DURATION_SECONDS);
    if (isLastQuestion) {
      setMode("summary");
      return;
    }
    setCurrentIndex((prev) => Math.min(prev + 1, questions.length - 1));
  };

  const goToSummary = () => {
    stopRecording();
    stopTimers();
    if ("speechSynthesis" in window) {
      window.speechSynthesis.cancel();
    }
    setMode("summary");
  };

  useEffect(() => {
    return () => {
      stopTimers();
      stopRecording();
      if (audioRef.current) {
        audioRef.current.pause();
        audioRef.current = null;
      }
    };
  }, []);

  if (mode === "ready") {
    return (
      <div className="min-h-screen px-6 py-10 text-[var(--ink)]">
        <div className="mx-auto flex max-w-4xl flex-col gap-8">
          <TopNav userLabel={userLabel} onLogout={onLogout} />

          <div className="flex flex-col items-center justify-center gap-10 py-16">
            <div className="text-center space-y-3">
              <p className="text-xs uppercase tracking-[0.32em] text-[var(--muted)]">
                Topic Practice
              </p>
              <h1 className="text-4xl font-semibold tracking-tight">
                {selectedTopic?.title ?? ""}
              </h1>
              {selectedTopic?.englishTitle ? (
                <p className="text-base text-[var(--muted)]">
                  {selectedTopic.englishTitle}
                </p>
              ) : null}
              {selectedTopic?.badges && selectedTopic.badges.length > 0 ? (
                <div className="flex justify-center gap-2 pt-1">
                  {selectedTopic.badges.map((badge) => (
                    <span
                      key={badge.label}
                      className="rounded-full border border-[var(--accent)]/30 px-3 py-1 text-xs font-semibold text-[var(--accent-strong)]"
                    >
                      {badge.label}
                      {badge.count != null ? ` ${badge.count}` : ""}
                    </span>
                  ))}
                </div>
              ) : null}
            </div>

            <div className="flex flex-col items-center gap-4">
              {error ? (
                <p className="text-sm text-red-600">{error}</p>
              ) : null}
              <button
                type="button"
                disabled={isLoading}
                onClick={startPractice}
                className="rounded-full bg-[var(--accent)] px-10 py-4 text-base font-semibold text-white shadow-lg shadow-[var(--accent)]/20 transition hover:bg-[var(--accent-strong)] disabled:opacity-50"
              >
                {isLoading
                  ? "질문 불러오는 중..."
                  : `${selectedTopic?.title ?? ""}에 맞는 연습 시작하기`}
              </button>
              <button
                type="button"
                onClick={() => {
                  setMode("select");
                  setError(null);
                }}
                className="text-sm text-[var(--muted)] hover:underline"
              >
                ← 주제 다시 선택하기
              </button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  if (mode === "summary") {
    return (
      <div className="min-h-screen px-6 py-10 text-[var(--ink)]">
        <div className="mx-auto flex max-w-4xl flex-col gap-8">
          <TopNav userLabel={userLabel} onLogout={onLogout} />
          <section className="rounded-[32px] border border-black/5 bg-white/70 p-8 shadow-sm">
            <p className="text-xs uppercase tracking-[0.32em] text-[var(--muted)]">
              Practice Summary
            </p>
            <h1 className="mt-2 text-3xl font-semibold tracking-tight">
              연습 기록
            </h1>
            <p className="mt-2 text-sm text-[var(--muted)]">
              질문과 답변이 대화 형태로 저장되었습니다.
            </p>
          </section>

          <div className="space-y-6">
            {answers.map((answer) => (
              <div
                key={answer.questionId}
                className="rounded-[24px] border border-black/10 bg-white/70 p-5 shadow-sm"
              >
                <p className="text-xs uppercase tracking-[0.2em] text-[var(--muted)]">
                  Question
                </p>
                <p className="mt-2 text-base font-semibold">
                  {answer.questionText}
                </p>
                <div className="mt-4 rounded-2xl border border-[var(--accent)]/20 bg-[var(--accent)]/10 p-4">
                  <p className="text-xs font-semibold text-[var(--accent-strong)]">
                    Your Answer
                  </p>
                  {answer.audioUrl ? (
                    <audio
                      controls
                      src={answer.audioUrl}
                      className="mt-2 w-full"
                    />
                  ) : (
                    <p className="mt-2 text-sm text-[var(--muted)]">
                      녹음된 답변이 없습니다.
                    </p>
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    );
  }

  if (mode === "practice") {
    const warning = remainingSeconds <= 10;
    return (
      <div className="min-h-screen px-6 py-10 text-[var(--ink)]">
        <div className="mx-auto flex max-w-4xl flex-col gap-8">
          <TopNav userLabel={userLabel} onLogout={onLogout} />
          <section className="rounded-[32px] border border-black/5 bg-white/70 p-8 shadow-sm">
            <div className="flex flex-wrap items-center justify-between gap-4">
              <div>
                <p className="text-xs uppercase tracking-[0.32em] text-[var(--muted)]">
                  Topic Practice
                </p>
                <h1 className="mt-2 text-3xl font-semibold tracking-tight">
                  {selectedTopic?.title ?? "연습 진행 중"}
                </h1>
                <p className="mt-2 text-sm text-[var(--muted)]">
                  질문 {currentIndex + 1} / {questions.length}
                </p>
              </div>
              <div
                className={`rounded-2xl border px-4 py-3 text-center ${
                  warning
                    ? "border-red-400 bg-red-50 text-red-700 animate-pulse"
                    : "border-black/10 bg-white text-[var(--muted)]"
                }`}
              >
                <p className="text-xs uppercase tracking-[0.2em]">Time</p>
                <p className="mt-1 text-xl font-semibold">
                  {Math.floor(remainingSeconds / 60)
                    .toString()
                    .padStart(2, "0")}
                  :
                  {(remainingSeconds % 60).toString().padStart(2, "0")}
                </p>
                {warning ? (
                  <p className="text-xs font-semibold">마감 임박</p>
                ) : null}
              </div>
            </div>
          </section>

          <section className="rounded-[28px] border border-black/5 bg-white/70 p-6 shadow-sm">
            <p className="text-xs uppercase tracking-[0.2em] text-[var(--muted)]">
              Question
            </p>
            <h2 className="mt-3 text-2xl font-semibold leading-relaxed">
              {currentQuestion?.promptText ?? "질문을 불러오는 중입니다."}
            </h2>
            {currentQuestion?.structuralHint ? (
              <p className="mt-3 text-sm text-[var(--muted)]">
                {currentQuestion.structuralHint}
              </p>
            ) : null}

            <div className="mt-6 flex flex-wrap items-center gap-3">
              <button
                type="button"
                onClick={handlePlayButton}
                disabled={hasPlayed && (hasReplayed || replayWindow <= 0)}
                className={`rounded-full px-4 py-2 text-sm font-semibold ${
                  hasPlayed && (hasReplayed || replayWindow <= 0)
                    ? "cursor-not-allowed bg-black/10 text-[var(--muted)]"
                    : "bg-[var(--accent)] text-white hover:bg-[var(--accent-strong)]"
                }`}
              >
                재생하기{hasPlayed && !hasReplayed && replayWindow > 0 ? ` (${replayWindow}s)` : ""}
              </button>
              {hasPlayed && !hasReplayed && replayWindow > 0 && (
                <span className="text-xs text-[var(--muted)]">
                  5초 안에 한 번 더 들을 수 있어요
                </span>
              )}
            </div>

            <div className="mt-6 flex flex-wrap items-center justify-between gap-4">
              <div className="text-sm text-[var(--muted)]">
                {isRecording ? "녹음 중..." : "녹음 대기"}
              </div>
              <div className="flex gap-2">
                {!isLastQuestion ? (
                  <button
                    type="button"
                    onClick={goToNext}
                    className="rounded-full bg-black px-4 py-2 text-sm font-semibold text-white hover:bg-black/80"
                  >
                    다음 질문
                  </button>
                ) : (
                  <button
                    type="button"
                    onClick={goToSummary}
                    className="rounded-full bg-black px-4 py-2 text-sm font-semibold text-white hover:bg-black/80"
                  >
                    제출하기
                  </button>
                )}
              </div>
            </div>
          </section>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen px-6 py-10 text-[var(--ink)]">
      <div className="mx-auto flex max-w-6xl flex-col gap-8">
        <TopNav
          maxWidthClassName="max-w-6xl"
          userLabel={userLabel}
          onLogout={onLogout}
        />

        <section className="rounded-[32px] border border-black/5 bg-white/70 p-8 shadow-sm">
          <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
            <div>
              <p className="text-xs uppercase tracking-[0.32em] text-[var(--muted)]">
                Topic Practice
              </p>
              <h1 className="mt-2 text-3xl font-semibold tracking-tight">
                주제별 연습을 시작해볼까요?
              </h1>
              <p className="mt-2 text-sm text-[var(--muted)]">
                하나의 주제를 선택하면 그 주제에 맞는 연습 흐름을 준비합니다.
              </p>
            </div>
            <div className="flex w-full max-w-sm items-center gap-2 rounded-full border border-black/10 bg-white px-4 py-2 shadow-sm">
              <span className="text-sm text-[var(--muted)]">🔎</span>
              <input
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                placeholder="주제 검색 (한국어/English)"
                className="w-full bg-transparent text-sm outline-none placeholder:text-[var(--muted)]"
              />
            </div>
          </div>
        </section>

        <div className="grid gap-6 lg:grid-cols-[0.65fr_1.35fr]">
          <aside className="flex flex-col gap-4">
            <div className="rounded-[28px] border border-black/5 bg-white/70 p-6 shadow-sm">
              <p className="text-xs font-semibold uppercase tracking-[0.2em] text-[var(--muted)]">
                카테고리
              </p>
              <div className="mt-4 flex flex-col gap-2">
                {categories.map((category) => {
                  const isActive = category.id === activeCategoryId;
                  return (
                    <button
                      key={category.id}
                      type="button"
                      onClick={() => setActiveCategoryId(category.id)}
                      className={`rounded-2xl border px-4 py-3 text-left transition ${
                        isActive
                          ? "border-transparent bg-[var(--accent)] text-white shadow-sm"
                          : "border-black/10 bg-white text-[var(--ink)] hover:border-[var(--accent)]/40"
                      }`}
                    >
                      <div className="flex items-center justify-between">
                        <span className="font-semibold">{category.label}</span>
                        <span
                          className={`text-xs ${
                            isActive ? "text-white/80" : "text-[var(--muted)]"
                          }`}
                        >
                          {category.topics.length} topics
                        </span>
                      </div>
                      <p
                        className={`mt-1 text-xs ${
                          isActive ? "text-white/80" : "text-[var(--muted)]"
                        }`}
                      >
                        {category.description}
                      </p>
                    </button>
                  );
                })}
              </div>
            </div>

            <div className="rounded-[28px] border border-black/5 bg-white/70 p-6 shadow-sm">
              <p className="text-xs font-semibold uppercase tracking-[0.2em] text-[var(--muted)]">
                선택된 주제
              </p>
              {selectedTopic ? (
                <div className="mt-4 rounded-2xl border border-[var(--accent)]/30 bg-[var(--accent)]/10 p-4">
                  <p className="text-sm font-semibold">{selectedTopic.title}</p>
                  <p className="text-xs text-[var(--muted)]">
                    {selectedTopic.englishTitle}
                  </p>
                  <div className="mt-3 flex flex-wrap gap-2">
                    {selectedTopic.badges?.map((badge) => (
                      <span
                        key={`${selectedTopic.id}-${badge.label}`}
                        className="rounded-full border border-[var(--accent)]/30 px-2.5 py-1 text-[10px] font-semibold text-[var(--accent-strong)]"
                      >
                        {badge.label}
                        {badge.count !== null && badge.count !== undefined
                          ? ` ${badge.count}`
                          : ""}
                      </span>
                    ))}
                  </div>
                </div>
              ) : (
                <p className="mt-3 text-sm text-[var(--muted)]">
                  아직 선택된 주제가 없습니다.
                </p>
              )}
              <button
                type="button"
                disabled={!selectedId || isSubmitting}
                onClick={handleSubmit}
                className={`mt-4 w-full rounded-full px-4 py-2 text-sm font-semibold transition ${
                  selectedTopic && !isSubmitting
                    ? "bg-[var(--accent)] text-white hover:bg-[var(--accent-strong)]"
                    : "cursor-not-allowed bg-black/10 text-[var(--muted)]"
                }`}
              >
                {isSubmitting ? "저장 중..." : "선택 완료"}
              </button>
              <button
                type="button"
                disabled={!selectedId || isSubmitting}
                onClick={startPractice}
                className={`mt-3 w-full rounded-full px-4 py-2 text-sm font-semibold transition ${
                  selectedTopic && !isSubmitting
                    ? "border border-[var(--accent)]/40 text-[var(--accent-strong)] hover:bg-[var(--accent)]/10"
                    : "cursor-not-allowed border border-black/10 text-[var(--muted)]"
                }`}
              >
                연습 시작하기
              </button>
              {successMessage ? (
                <p className="mt-3 text-xs text-[var(--accent-strong)]">
                  {successMessage}
                </p>
              ) : null}
              {error ? (
                <p className="mt-3 text-xs text-red-600">{error}</p>
              ) : null}
            </div>
          </aside>

          <section className="rounded-[28px] border border-black/5 bg-white/70 p-6 shadow-sm">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-xs uppercase tracking-[0.2em] text-[var(--muted)]">
                  Topics
                </p>
                <h2 className="mt-1 text-2xl font-semibold">
                  {activeCategory?.label ?? "주제 목록"}
                </h2>
              </div>
              <span className="text-xs text-[var(--muted)]">
                {filteredTopics.length} topics
              </span>
            </div>

            {isLoading ? (
              <div className="mt-6 grid gap-4 sm:grid-cols-2">
                {Array.from({ length: 6 }).map((_, index) => (
                  <div
                    key={`skeleton-${index}`}
                    className="h-24 rounded-3xl border border-black/10 bg-white/60 animate-pulse"
                  />
                ))}
              </div>
            ) : (
              <div className="mt-6 grid gap-4 sm:grid-cols-2">
                {filteredTopics.map((topic) => {
                  const isSelected = selectedId === topic.id;
                  return (
                    <button
                      key={topic.id}
                      type="button"
                      onClick={() => handleSelect(topic)}
                      className={`rounded-3xl border p-4 text-left transition ${
                        isSelected
                          ? "border-transparent bg-[var(--accent)] text-white shadow-lg shadow-[var(--accent)]/20"
                          : "border-black/10 bg-white hover:border-[var(--accent)]/40"
                      }`}
                    >
                      <div className="flex items-start justify-between gap-3">
                        <div>
                          <p className="text-base font-semibold">
                            {topic.title}
                          </p>
                          <p
                            className={`mt-1 text-xs ${
                              isSelected
                                ? "text-white/80"
                                : "text-[var(--muted)]"
                            }`}
                          >
                            {topic.englishTitle}
                          </p>
                        </div>
                        <span
                          className={`rounded-full px-2.5 py-1 text-[10px] font-semibold ${
                            isSelected
                              ? "bg-white/20 text-white"
                              : "bg-[var(--accent)]/10 text-[var(--accent-strong)]"
                          }`}
                        >
                          {isSelected ? "선택됨" : "선택"}
                        </span>
                      </div>
                      {topic.badges && topic.badges.length > 0 ? (
                        <div className="mt-3 flex flex-wrap gap-2">
                          {topic.badges.map((badge) => (
                            <span
                              key={`${topic.id}-${badge.label}`}
                              className={`rounded-full border px-2.5 py-1 text-[10px] font-semibold ${
                                isSelected
                                  ? "border-white/30 text-white/90"
                                  : "border-[var(--accent)]/20 text-[var(--accent-strong)]"
                              }`}
                            >
                              {badge.label}
                              {badge.count !== null &&
                              badge.count !== undefined
                                ? ` ${badge.count}`
                                : ""}
                            </span>
                          ))}
                        </div>
                      ) : null}
                    </button>
                  );
                })}
              </div>
            )}
          </section>
        </div>
      </div>
    </div>
  );
}
