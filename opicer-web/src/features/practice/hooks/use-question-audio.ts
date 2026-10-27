"use client";

import { useCallback, useEffect, useRef } from "react";

export type SpokenQuestion = { promptText: string; promptAudioUrl?: string | null };

const FEMALE_VOICE_NAMES = [
  "Samantha", "Karen", "Ava", "Victoria", "Susan", "Allison",
  "Google US English", "Google UK English Female",
];

/**
 * Plays a question like the exam's interviewer: the uploaded audio when there is one, otherwise an English
 * speech-synthesis voice (a female one when the browser offers it).
 */
export function useQuestionAudio(onError?: (message: string) => void) {
  const audioRef = useRef<HTMLAudioElement | null>(null);
  const voicesRef = useRef<SpeechSynthesisVoice[]>([]);
  const onErrorRef = useRef(onError);

  useEffect(() => {
    onErrorRef.current = onError;
  });

  // Voices load asynchronously in some browsers.
  useEffect(() => {
    if (!("speechSynthesis" in window)) return;
    const load = () => {
      voicesRef.current = window.speechSynthesis.getVoices();
    };
    load();
    window.speechSynthesis.addEventListener("voiceschanged", load);
    return () => window.speechSynthesis.removeEventListener("voiceschanged", load);
  }, []);

  const englishVoice = useCallback((): SpeechSynthesisVoice | null => {
    const voices = voicesRef.current.length ? voicesRef.current : window.speechSynthesis.getVoices();
    const english = voices.filter((v) => v.lang.startsWith("en"));
    return english.find((v) => FEMALE_VOICE_NAMES.some((n) => v.name.includes(n))) ?? english[0] ?? null;
  }, []);

  /** Plays the question once and calls `onEnded` when it finishes. */
  const play = useCallback(
    (question: SpokenQuestion, onEnded: () => void) => {
      if (question.promptAudioUrl) {
        if (!audioRef.current) audioRef.current = new Audio(question.promptAudioUrl);
        else audioRef.current.src = question.promptAudioUrl;
        audioRef.current.onended = onEnded;
        audioRef.current.play().catch(() => onErrorRef.current?.("오디오 재생에 실패했습니다."));
      } else if ("speechSynthesis" in window) {
        const utterance = new SpeechSynthesisUtterance(question.promptText);
        utterance.lang = "en-US";
        utterance.rate = 0.9;
        const voice = englishVoice();
        if (voice) utterance.voice = voice;
        utterance.onend = onEnded;
        window.speechSynthesis.cancel();
        window.speechSynthesis.speak(utterance);
      }
    },
    [englishVoice]
  );

  /** Stops playback without calling the pending `onEnded`. */
  const stop = useCallback(() => {
    if ("speechSynthesis" in window) window.speechSynthesis.cancel();
    if (audioRef.current) {
      audioRef.current.pause();
      audioRef.current.onended = null;
    }
  }, []);

  return { play, stop };
}
