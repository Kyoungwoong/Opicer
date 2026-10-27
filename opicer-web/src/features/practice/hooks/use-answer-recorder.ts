"use client";

import { useCallback, useRef, useState } from "react";

/** Longest recording length the server accepts for one answer. */
export const MAX_ANSWER_SECONDS = 600;

/** One finished recording. The audio stays in the browser as an object URL; it is never uploaded or stored. */
export type RecordedClip = {
  audioUrl: string;
  /** Rounded and clamped to 1–600 seconds, the range the server validates. */
  durationSeconds: number;
  recordedAt: string;
};

/**
 * Microphone recording for one answer at a time. `micStream` is exposed only for the input level meter.
 */
export function useAnswerRecorder() {
  const recorderRef = useRef<MediaRecorder | null>(null);
  const chunksRef = useRef<Blob[]>([]);
  const startedAtRef = useRef(0);
  const [isRecording, setIsRecording] = useState(false);
  const [micStream, setMicStream] = useState<MediaStream | null>(null);

  const isActive = useCallback(() => recorderRef.current != null && recorderRef.current.state !== "inactive", []);

  /**
   * Starts recording; `onStopped` receives the clip once {@link stop} (or the browser) ends it.
   * Resolves false when the microphone is unavailable or permission is denied.
   */
  const start = useCallback(
    async (onStopped: (clip: RecordedClip) => void): Promise<boolean> => {
      if (isActive()) return true;
      try {
        const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
        const recorder = new MediaRecorder(stream);
        chunksRef.current = [];
        recorder.ondataavailable = (e) => {
          if (e.data.size > 0) chunksRef.current.push(e.data);
        };
        recorder.onstop = () => {
          stream.getTracks().forEach((t) => t.stop());
          setMicStream(null);
          const blob = new Blob(chunksRef.current, { type: "audio/webm" });
          const elapsed = Math.round((Date.now() - startedAtRef.current) / 1000);
          onStopped({
            audioUrl: URL.createObjectURL(blob),
            durationSeconds: Math.min(Math.max(elapsed, 1), MAX_ANSWER_SECONDS),
            recordedAt: new Date().toISOString(),
          });
        };
        recorderRef.current = recorder;
        recorder.start();
        startedAtRef.current = Date.now();
        setIsRecording(true);
        setMicStream(stream);
        return true;
      } catch {
        return false;
      }
    },
    [isActive]
  );

  const stop = useCallback(() => {
    if (recorderRef.current && recorderRef.current.state !== "inactive") {
      recorderRef.current.stop();
    }
    recorderRef.current = null;
    setIsRecording(false);
  }, []);

  return { start, stop, isActive, isRecording, micStream };
}
