"use client";

import { useState } from "react";
import { OpicCenterManager } from "@/features/admin/OpicCenterManager";
import { OpicCenterReviewTab } from "@/features/admin/OpicCenterReviewTab";
import { adminUi } from "@/features/admin/ui";

/** Test centers first, then their reviews; the review section reloads its center list after a change. */
export function OpicCenterTab() {
  const [centersVersion, setCentersVersion] = useState(0);

  return (
    <div className={adminUi.section}>
      <OpicCenterManager onChanged={() => setCentersVersion((v) => v + 1)} />
      <OpicCenterReviewTab key={centersVersion} />
    </div>
  );
}
