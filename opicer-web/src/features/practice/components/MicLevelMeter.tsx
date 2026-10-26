"use client";

import { useEffect, useState } from "react";
import { cn } from "@/lib/utils";

const BAR_COUNT = 12;
/** Speech RMS rarely exceeds ~0.25; scale it so normal talking fills most of the meter. */
const GAIN = 4;
const DECAY = 0.94;

/**
 * Live input level (0..1) of a recording stream, read with an AnalyserNode on each animation frame.
 * Lives in its own component so the 60fps updates re-render only the meter, not the practice screen.
 */
function useMicLevel(stream: MediaStream | null): number {
  const [level, setLevel] = useState(0);

  useEffect(() => {
    if (!stream || typeof AudioContext === "undefined") return;
    const context = new AudioContext();
    // Recording starts from a TTS callback, not a click; resume in case autoplay policy suspended the context.
    if (context.state === "suspended") void context.resume();
    const source = context.createMediaStreamSource(stream);
    const analyser = context.createAnalyser();
    analyser.fftSize = 512;
    source.connect(analyser);
    const samples = new Uint8Array(analyser.fftSize);

    let frame = 0;
    const tick = () => {
      analyser.getByteTimeDomainData(samples);
      let sumSquares = 0;
      for (const sample of samples) {
        const centered = (sample - 128) / 128;
        sumSquares += centered * centered;
      }
      const raw = Math.min(1, Math.sqrt(sumSquares / samples.length) * GAIN);
      // Peak-hold with decay so the meter and its label don't flicker between syllables.
      setLevel((prev) => Math.max(raw, prev * DECAY));
      frame = requestAnimationFrame(tick);
    };
    frame = requestAnimationFrame(tick);

    return () => {
      cancelAnimationFrame(frame);
      source.disconnect();
      void context.close();
    };
  }, [stream]);

  return stream ? level : 0;
}

type Props = {
  stream: MediaStream | null;
  className?: string;
};

export function MicLevelMeter({ stream, className }: Props) {
  const level = useMicLevel(stream);
  const lit = Math.round(level * BAR_COUNT);

  return (
    <div className={cn("flex flex-col gap-1.5", className)}>
      <div className="flex items-center justify-between text-xs text-muted-foreground">
        <span>마이크 입력</span>
        <span>{!stream ? "꺼짐" : level < 0.05 ? "조용함 — 조금 더 크게 말해 보세요" : "잘 들려요"}</span>
      </div>
      {/* Visual only; the text above carries the state for screen readers. */}
      <div aria-hidden className="flex h-6 items-end gap-1">
        {Array.from({ length: BAR_COUNT }, (_, i) => (
          <span
            key={i}
            className={cn(
              "flex-1 rounded-sm transition-[height,background-color] duration-75 motion-reduce:transition-none",
              i < lit ? (i >= BAR_COUNT - 2 ? "bg-highlight" : "bg-primary") : "bg-muted"
            )}
            style={{ height: `${30 + (i / (BAR_COUNT - 1)) * 70}%` }}
          />
        ))}
      </div>
    </div>
  );
}
