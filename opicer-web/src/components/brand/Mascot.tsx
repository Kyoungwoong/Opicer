import { cn } from "@/lib/utils";

type Props = {
  className?: string;
  /** Soft circle behind the character (off for small inline uses). */
  backdrop?: boolean;
  /** Accessible name; pass "" when the mascot is purely decorative. */
  label?: string;
  /** "talk" animates the mouth (reading a question), "listen" pulses sound waves at the ear (recording). */
  expression?: "smile" | "talk" | "listen";
};

/** SVG parts scale around their own box; animations stop under prefers-reduced-motion. */
const SVG_ANIMATED = "origin-center [transform-box:fill-box] motion-reduce:animate-none";

/**
 * "오피" — Opicer's original practice-buddy character (a round face with a headset).
 * Not modeled on any exam's official avatar. Colors come from the design tokens.
 */
export function Mascot({ className, backdrop = true, label = "연습 도우미 캐릭터 오피", expression = "smile" }: Props) {
  const decorative = label === "";
  return (
    <svg
      viewBox="0 0 260 240"
      className={cn("h-auto w-full", className)}
      role={decorative ? undefined : "img"}
      aria-label={decorative ? undefined : label}
      aria-hidden={decorative ? true : undefined}
    >
      {backdrop ? (
        <>
          <circle cx="130" cy="124" r="104" style={{ fill: "var(--accent)" }} />
          <ellipse cx="130" cy="220" rx="70" ry="8" style={{ fill: "color-mix(in oklab, var(--accent) 70%, var(--primary))" }} />
        </>
      ) : null}
      <path d="M66 122c0-45 29-80 64-80s64 35 64 80v36c0 28-22 50-50 50h-28c-28 0-50-22-50-50z" style={{ fill: "var(--brand)" }} />
      <path d="M88 120c0-28 19-48 42-48s42 20 42 48v24c0 20-16 36-36 36h-12c-20 0-36-16-36-36z" fill="#ffffff" />
      <circle cx="114" cy="124" r="6" style={{ fill: "var(--foreground)" }} />
      <circle cx="146" cy="124" r="6" style={{ fill: "var(--foreground)" }} />
      {expression === "talk" ? (
        <ellipse
          cx="130"
          cy="152"
          rx="9"
          ry="7"
          className={cn("animate-opi-talk", SVG_ANIMATED)}
          style={{ fill: "var(--foreground)" }}
        />
      ) : (
        <path d="M119 149q11 9 22 0" fill="none" strokeWidth="4" strokeLinecap="round" style={{ stroke: "var(--foreground)" }} />
      )}
      {expression === "listen" ? (
        <g fill="none" strokeWidth="4" strokeLinecap="round" style={{ stroke: "var(--primary)" }}>
          <path d="M34 116q-8 14 0 28" className={cn("animate-opi-listen", SVG_ANIMATED)} />
          <path d="M22 108q-12 22 0 44" className={cn("animate-opi-listen [animation-delay:0.3s]", SVG_ANIMATED)} />
        </g>
      ) : null}
      <circle cx="101" cy="142" r="6" fill="#f2c1ad" />
      <circle cx="159" cy="142" r="6" fill="#f2c1ad" />
      <path d="M60 122c0-44 31-76 70-76s70 32 70 76" fill="none" strokeWidth="6" strokeLinecap="round" style={{ stroke: "var(--foreground)" }} />
      <rect x="48" y="112" width="20" height="36" rx="9" strokeWidth="4" style={{ fill: "var(--highlight)", stroke: "var(--foreground)" }} />
      <rect x="192" y="112" width="20" height="36" rx="9" strokeWidth="4" style={{ fill: "var(--highlight)", stroke: "var(--foreground)" }} />
      <path d="M202 148c0 22-13 33-36 35" fill="none" strokeWidth="4" strokeLinecap="round" style={{ stroke: "var(--foreground)" }} />
      <circle cx="164" cy="183" r="6" style={{ fill: "var(--foreground)" }} />
    </svg>
  );
}
