import { SectionShell } from "@/features/home/components/SectionShell";
import { OpicCalendar } from "@/components/opic/OpicCalendar";
import { ko } from "@/locales/ko";

export function OpicScheduleSection() {
  return (
    <SectionShell title={ko.home.opicSchedule.description}>
      <OpicCalendar size="compact" />
    </SectionShell>
  );
}
