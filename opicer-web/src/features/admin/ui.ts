import { buttonVariants } from "@/components/ui/button";

/**
 * Shared class names for the admin tabs. The five tabs repeat the same
 * form/table markup, so styling lives here instead of in 100+ copies.
 */
export const adminUi = {
  section: "flex flex-col gap-6",
  card: "flex flex-col gap-4 rounded-3xl border bg-card/90 p-5 shadow-card sm:p-6",
  cardTitle: "text-lg font-semibold text-foreground",
  subCard: "flex flex-col gap-2 rounded-2xl border bg-secondary p-4",
  empty: "rounded-2xl border border-dashed p-6 text-center text-sm text-muted-foreground",

  grid2: "grid gap-4 sm:grid-cols-2",
  grid3: "grid gap-4 sm:grid-cols-3",
  /** Wraps a caption + one control, so the <label> labels the control. */
  field: "flex flex-col gap-1.5",
  fieldGroup: "flex flex-col gap-2",
  labelText: "text-sm font-medium text-foreground",
  control:
    "w-full rounded-lg border border-input bg-card px-3 py-2 text-sm outline-none transition-colors placeholder:text-muted-foreground focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/30 disabled:opacity-50",
  controlMono:
    "w-full rounded-lg border border-input bg-card px-3 py-2 font-mono text-xs outline-none transition-colors focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/30",
  checkRow: "flex cursor-pointer items-center gap-2 text-sm",
  checkInline: "flex cursor-pointer items-center gap-1.5 text-sm",
  checkbox: "size-4 accent-primary",
  actions: "flex flex-wrap gap-2",

  error: "rounded-lg bg-destructive/10 px-3 py-2 text-sm text-destructive",

  primaryButton: buttonVariants({ className: "h-10 rounded-full px-5" }),
  secondaryButton: buttonVariants({ variant: "outline", className: "h-10 rounded-full px-5" }),
  smallButton: buttonVariants({ variant: "outline", size: "sm", className: "rounded-full px-3" }),
  smallDangerButton: buttonVariants({ variant: "destructive", size: "sm", className: "rounded-full px-3" }),
  linkButton: "font-medium text-primary hover:underline",
  dangerLink: "font-medium text-destructive hover:underline",

  tableWrap: "overflow-x-auto rounded-2xl border bg-card/90 shadow-card",
  table: "w-full min-w-[640px] text-sm",
  thead: "bg-muted/60 text-muted-foreground",
  th: "px-4 py-3 text-left text-xs font-semibold whitespace-nowrap",
  tbody: "divide-y divide-border",
  tr: "transition-colors hover:bg-muted/40",
  td: "px-4 py-3",
  tdStrong: "px-4 py-3 font-medium",
  tdMuted: "px-4 py-3 text-muted-foreground",
  tdSmall: "px-4 py-3 text-xs text-muted-foreground",
  tdEmpty: "px-4 py-8 text-center text-muted-foreground",

  pillOn: "rounded-full bg-success-subtle px-2 py-0.5 text-xs font-semibold text-success",
  pillOff: "rounded-full bg-muted px-2 py-0.5 text-xs font-semibold text-muted-foreground",
  pillPrimary: "rounded-full bg-accent px-2 py-0.5 text-xs font-medium text-accent-foreground",
} as const;
