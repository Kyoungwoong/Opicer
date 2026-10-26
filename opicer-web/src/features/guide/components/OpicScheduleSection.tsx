import { GuideSection } from "@/features/guide/components/GuideSection";
import { OpicCalendar } from "@/components/opic/OpicCalendar";

export function OpicScheduleSection() {
  return (
    <GuideSection id="schedule" title="오픽 일정" subtitle="날짜를 눌러 접수 기간과 성적 발표일을 확인하세요">
      <OpicCalendar size="comfortable" />
    </GuideSection>
  );
}
