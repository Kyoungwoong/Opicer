"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { AlertCircle, ArrowLeft, LoaderCircle, Mic, Play, Square } from "lucide-react";
import { SectionHeader } from "@/components/common/SectionHeader";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { ROUTES } from "@/lib/routes";
import { MicLevelMeter } from "@/features/practice/components/MicLevelMeter";
import { useAnswerRecorder, type RecordedClip } from "@/features/practice/hooks/use-answer-recorder";
import { useQuestionAudio } from "@/features/practice/hooks/use-question-audio";

const SAMPLE_QUESTION = "Hi, how are you today? Please tell me a little about the weather where you are right now.";

type Props = {
  starting: boolean;
  startError: string | null;
  /** True when the start failed for lack of credits, so the screen can link to the credit page. */
  needsCredits: boolean;
  onBack: () => void;
  onStart: () => void;
};

/** Sample question to check the speaker and microphone. The sample answer is neither evaluated nor sent anywhere. */
export function MicCheckStep({ starting, startError, needsCredits, onBack, onStart }: Props) {
  const { start, stop, isRecording, micStream } = useAnswerRecorder();
  const questionAudio = useQuestionAudio();
  const [clip, setClip] = useState<RecordedClip | null>(null);
  const [micError, setMicError] = useState(false);

  // Drop the sample recording when it is replaced or the step is left.
  useEffect(() => {
    return () => {
      if (clip) URL.revokeObjectURL(clip.audioUrl);
    };
  }, [clip]);

  useEffect(() => {
    return () => {
      stop();
      questionAudio.stop();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const record = async () => {
    setMicError(false);
    const started = await start(setClip);
    if (!started) setMicError(true);
  };

  return (
    <section className="flex flex-col gap-6">
      <div className="flex flex-col gap-3 rounded-3xl border bg-card p-5 shadow-card sm:p-6">
        <SectionHeader
          as="h1"
          eyebrow="3단계 · 소리와 마이크 확인"
          title="시작 전에 한 번 들어 보고 말해 보세요"
          description="샘플 질문을 듣고 짧게 답해 보세요. 이 녹음은 평가하지 않고 어디에도 보내지 않아요."
        />
      </div>

      <div className="flex flex-col gap-5 rounded-3xl border bg-card p-5 shadow-card sm:p-6">
        <div className="flex flex-wrap items-center gap-3">
          <Button type="button" variant="outline" onClick={() => questionAudio.play({ promptText: SAMPLE_QUESTION }, () => {})}>
            <Play />
            샘플 질문 듣기
          </Button>
          {isRecording ? (
            <Button type="button" variant="destructive" onClick={stop}>
              <Square />
              녹음 멈추기
            </Button>
          ) : (
            <Button type="button" variant="secondary" onClick={() => void record()}>
              <Mic />
              {clip ? "다시 녹음하기" : "녹음해 보기"}
            </Button>
          )}
        </div>
        <MicLevelMeter stream={micStream} />
        {clip ? (
          <div className="flex flex-col gap-2">
            <p className="text-sm text-muted-foreground">내 목소리가 잘 들리는지 확인해 보세요.</p>
            <audio controls src={clip.audioUrl} className="w-full" />
          </div>
        ) : null}
        {micError ? (
          <Alert variant="destructive">
            <AlertCircle />
            <AlertDescription>마이크를 쓸 수 없어요. 브라우저 주소창의 권한 설정에서 마이크를 허용해 주세요.</AlertDescription>
          </Alert>
        ) : null}
      </div>

      {startError ? (
        <Alert variant="destructive">
          <AlertCircle />
          <AlertTitle>시작하지 못했어요</AlertTitle>
          <AlertDescription className="flex flex-col gap-2">
            <p>{startError}</p>
            {needsCredits ? (
              <Link href={ROUTES.credit} className="font-semibold underline underline-offset-4">
                크레딧 충전하러 가기
              </Link>
            ) : null}
          </AlertDescription>
        </Alert>
      ) : null}

      <div className="flex flex-col-reverse items-stretch gap-3 sm:flex-row sm:items-center sm:justify-between">
        <Button type="button" variant="ghost" onClick={onBack} disabled={starting} className="text-muted-foreground">
          <ArrowLeft />
          자가평가로
        </Button>
        <div className="flex flex-col items-center gap-1 sm:items-end">
          <Button
            type="button"
            onClick={() => {
              stop();
              onStart();
            }}
            disabled={!clip || isRecording || starting}
            className="h-11 rounded-full px-8 text-sm font-semibold"
          >
            {starting ? <LoaderCircle className="animate-spin" /> : null}
            시험 시작하기
          </Button>
          {!clip ? <p className="text-xs text-muted-foreground">한 번 녹음해 봐야 시작할 수 있어요.</p> : null}
        </div>
      </div>
    </section>
  );
}
