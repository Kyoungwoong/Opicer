import { cn } from "@/lib/utils";

const WIDTHS = {
  md: "max-w-4xl",
  lg: "max-w-5xl",
  xl: "max-w-6xl",
} as const;

type Props = {
  /** Usually <TopNav />. Rendered above the page content at the same width. */
  nav?: React.ReactNode;
  width?: keyof typeof WIDTHS;
  className?: string;
  children: React.ReactNode;
};

/** Standard page frame: responsive gutters, max width and vertical rhythm. */
export function PageShell({ nav, width = "xl", className, children }: Props) {
  return (
    <div className="min-h-dvh px-4 py-6 sm:px-6 sm:py-10">
      <div className={cn("mx-auto flex w-full flex-col gap-8", WIDTHS[width])}>
        {nav}
        <main className={cn("flex flex-col gap-8", className)}>{children}</main>
      </div>
    </div>
  );
}
