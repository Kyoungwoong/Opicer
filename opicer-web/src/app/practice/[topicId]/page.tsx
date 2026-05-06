"use client";

import { useEffect } from "react";
import { useRouter, useParams, useSearchParams } from "next/navigation";
import { ROUTES } from "@/lib/routes";
import { logout } from "@/lib/auth-client";
import { useAuthState } from "@/lib/use-auth-state";
import { LoadingScreen } from "@/components/common/LoadingScreen";
import { PracticeSessionView } from "@/features/practice/components/PracticeSessionView";

export default function PracticeSessionPage() {
  const auth = useAuthState();
  const router = useRouter();
  const params = useParams();
  const searchParams = useSearchParams();
  const topicId = typeof params.topicId === "string" ? params.topicId : "";
  const topicSelectionId = searchParams.get("selectionId") ?? "";

  useEffect(() => {
    if (auth.status === "unauthenticated") {
      router.replace(ROUTES.login);
    }
  }, [auth.status, router]);

  useEffect(() => {
    if (!topicId || !topicSelectionId) {
      router.replace(ROUTES.practice);
    }
  }, [topicId, topicSelectionId, router]);

  if (auth.status === "loading" || auth.status === "unauthenticated") {
    return <LoadingScreen />;
  }

  if (!topicId || !topicSelectionId) return <LoadingScreen />;

  const handleLogout = async () => {
    await logout();
    window.location.href = ROUTES.login;
  };

  return (
    <PracticeSessionView
      topicId={topicId}
      topicSelectionId={topicSelectionId}
      userLabel={auth.user.name ?? auth.user.email ?? "User"}
      onLogout={handleLogout}
    />
  );
}
