import { SectionShell } from "@/features/home/components/SectionShell";
import { OpicOfficialSchedule } from "@/components/opic/OpicOfficialSchedule";
import { ko } from "@/locales/ko";

export function OpicScheduleSection() {
  return (
    <SectionShell title={ko.home.opicSchedule.description}>
      <OpicOfficialSchedule size="compact" />
    </SectionShell>
  );
}
