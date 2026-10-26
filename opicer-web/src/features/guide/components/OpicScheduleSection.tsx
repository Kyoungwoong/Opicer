import { GuideSection } from "@/features/guide/components/GuideSection";
import { OpicOfficialSchedule } from "@/components/opic/OpicOfficialSchedule";

export function OpicScheduleSection() {
  return (
    <GuideSection id="schedule" title="오픽 일정" subtitle="일정은 회차마다 달라요. 공식 사이트에서 확인하세요">
      <OpicOfficialSchedule size="comfortable" />
    </GuideSection>
  );
}
