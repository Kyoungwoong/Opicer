import Markdown, { type Components } from "react-markdown";
import { cn } from "@/lib/utils";

/** Element styles for the preview; the app has no typography plugin. */
const COMPONENTS: Components = {
  h1: ({ children }) => <h1 className="mt-4 mb-2 text-xl font-bold first:mt-0">{children}</h1>,
  h2: ({ children }) => <h2 className="mt-4 mb-2 text-lg font-bold first:mt-0">{children}</h2>,
  h3: ({ children }) => <h3 className="mt-3 mb-1.5 text-base font-semibold first:mt-0">{children}</h3>,
  p: ({ children }) => <p className="my-2 leading-relaxed">{children}</p>,
  ul: ({ children }) => <ul className="my-2 list-disc pl-5">{children}</ul>,
  ol: ({ children }) => <ol className="my-2 list-decimal pl-5">{children}</ol>,
  li: ({ children }) => <li className="my-0.5">{children}</li>,
  strong: ({ children }) => <strong className="font-semibold">{children}</strong>,
  blockquote: ({ children }) => (
    <blockquote className="my-2 border-l-2 border-primary/40 pl-3 text-muted-foreground">{children}</blockquote>
  ),
  code: ({ children }) => <code className="rounded bg-muted px-1 py-0.5 font-mono text-xs">{children}</code>,
  pre: ({ children }) => <pre className="my-2 overflow-x-auto rounded-lg bg-muted p-3 text-xs">{children}</pre>,
  a: ({ children, href }) => (
    <a href={href} target="_blank" rel="noreferrer noopener" className="text-primary-strong underline">
      {children}
    </a>
  ),
  hr: () => <hr className="my-3 border-border" />,
};

/** Renders admin-written markdown. Raw HTML in the source is dropped, never rendered. */
export function MarkdownPreview({ markdown, className }: { markdown: string; className?: string }) {
  if (!markdown.trim()) {
    return <p className={cn("text-sm text-muted-foreground", className)}>미리볼 내용이 없어요.</p>;
  }
  return (
    <div className={cn("text-sm text-foreground", className)}>
      <Markdown skipHtml components={COMPONENTS}>
        {markdown}
      </Markdown>
    </div>
  );
}
