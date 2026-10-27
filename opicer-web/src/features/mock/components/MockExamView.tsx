"use client";

import { useEffect, useState } from "react";
import { AlertCircle, LoaderCircle, RotateCcw } from "lucide-react";
import { TopNav } from "@/components/common/TopNav";
import { PageShell } from "@/components/common/PageShell";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { DEFAULT_TARGET_LEVEL } from "@/features/practice/evaluation-labels";
import type { TargetLevel } from "@/features/practice/types";
import { MockApiError, fetchSurveyCatalog, startMockTest } from "@/features/mock/api";
import { readSavedSurvey, saveSurvey } from "@/features/mock/survey";
import type { SelfAssessment, StartedMockTest, SurveyCatalog, SurveySelections } from "@/features/mock/types";
import { MockIntro } from "@/features/mock/components/MockIntro";
import { SurveyStep } from "@/features/mock/components/SurveyStep";
import { SelfAssessmentStep } from "@/features/mock/components/SelfAssessmentStep";
import { MicCheckStep } from "@/features/mock/components/MicCheckStep";
import { ExamScreen } from "@/features/mock/components/ExamScreen";

type Step = "intro" | "survey" | "self" | "mic" | "exam";

type Props = { userLabel?: string; onLogout?: () => void };

/**
 * The whole mock test on one page: nothing is stored until the test starts, and an unfinished test leaves no
 * history (the server lists submitted tests only), so a reload simply starts over.
 */
export function MockExamView({ userLabel, onLogout }: Props) {
  const [step, setStep] = useState<Step>("intro");
  const [catalog, setCatalog] = useState<SurveyCatalog | null>(null);
  const [catalogError, setCatalogError] = useState<string | null>(null);
  const [selections, setSelections] = useState<SurveySelections>({});
  const [selfAssessment, setSelfAssessment] = useState<SelfAssessment | null>(null);
  const [targetLevel, setTargetLevel] = useState<TargetLevel>(DEFAULT_TARGET_LEVEL);
  const [starting, setStarting] = useState(false);
  const [startError, setStartError] = useState<MockApiError | null>(null);
  const [test, setTest] = useState<StartedMockTest | null>(null);

  const loadCatalog = () => {
    setCatalogError(null);
    fetchSurveyCatalog()
      .then((loaded) => {
        setCatalog(loaded);
        setSelections(readSavedSurvey(loaded));
      })
      .catch((err: unknown) => setCatalogError(err instanceof Error ? err.message : "설문 항목을 불러오지 못했어요."));
  };

  useEffect(loadCatalog, []);

  // Each step starts at the top of the page.
  useEffect(() => {
    window.scrollTo({ top: 0 });
  }, [step]);

  const start = async () => {
    if (selfAssessment == null || starting) return;
    setStarting(true);
    setStartError(null);
    try {
      const started = await startMockTest({ survey: selections, selfAssessment, targetLevel });
      saveSurvey(selections);
      setTest(started);
      setStep("exam");
    } catch (err) {
      setStartError(err instanceof MockApiError ? err : new MockApiError("실전연습을 시작하지 못했어요.", null, 0));
    } finally {
      setStarting(false);
    }
  };

  // The top navigation would let the learner leave mid-exam, so the exam runs without it.
  const nav = step === "exam" ? undefined : <TopNav userLabel={userLabel} onLogout={onLogout} />;

  let content: React.ReactNode;
  if (step === "exam" && test) {
    content = <ExamScreen testId={test.id} firstPart={test.items} />;
  } else if (step === "intro") {
    content = <MockIntro cost={catalog?.cost ?? null} targetLevel={targetLevel} onTargetLevelChange={setTargetLevel} onNext={() => setStep("survey")} />;
  } else if (!catalog) {
    content = (
      <div className="flex flex-col items-center gap-4 py-24">
        {catalogError ? (
          <Alert variant="destructive" className="max-w-md">
            <AlertCircle />
            <AlertDescription className="flex flex-col gap-3">
              <p>{catalogError}</p>
              <Button type="button" variant="outline" size="sm" onClick={loadCatalog} className="self-start rounded-full">
                <RotateCcw />
                다시 불러오기
              </Button>
            </AlertDescription>
          </Alert>
        ) : (
          <LoaderCircle role="status" aria-label="불러오는 중" className="size-8 animate-spin text-primary-strong" />
        )}
      </div>
    );
  } else if (step === "survey") {
    content = (
      <SurveyStep
        catalog={catalog}
        selections={selections}
        onChange={setSelections}
        onBack={() => setStep("intro")}
        onNext={() => setStep("self")}
      />
    );
  } else if (step === "self") {
    content = (
      <SelfAssessmentStep
        value={selfAssessment}
        onChange={setSelfAssessment}
        onBack={() => setStep("survey")}
        onNext={() => setStep("mic")}
      />
    );
  } else {
    content = (
      <MicCheckStep
        starting={starting}
        startError={startError?.message ?? null}
        needsCredits={startError?.code === "CREDIT_INSUFFICIENT_BALANCE"}
        onBack={() => setStep("self")}
        onStart={() => void start()}
      />
    );
  }

  return (
    <PageShell width="lg" nav={nav}>
      {content}
    </PageShell>
  );
}
