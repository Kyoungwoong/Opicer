"use client";

import { useEffect } from "react";
import { useRouter, useParams } from "next/navigation";
import { ROUTES } from "@/lib/routes";
import { logout } from "@/lib/auth-client";
import { useAuthState } from "@/lib/use-auth-state";
import { LoadingScreen } from "@/components/common/LoadingScreen";
import { PracticeSessionView } from "@/features/practice/components/PracticeSessionView";

export default function PracticeSessionPage() {
  const auth = useAuthState();
  const router = useRouter();
  const params = useParams();
  const topicId = typeof params.topicId === "string" ? params.topicId : "";

  useEffect(() => {
    if (auth.status === "unauthenticated") {
      router.replace(ROUTES.login);
    }
  }, [auth.status, router]);

  useEffect(() => {
    if (!topicId) {
      router.replace(ROUTES.practice);
    }
  }, [topicId, router]);

  if (auth.status === "loading" || auth.status === "unauthenticated") {
    return <LoadingScreen />;
  }

  if (!topicId) return <LoadingScreen />;

  const handleLogout = async () => {
    await logout();
    window.location.href = ROUTES.login;
  };

  return (
    <PracticeSessionView
      topicId={topicId}
      userLabel={auth.user.name ?? auth.user.email ?? "사용자"}
      onLogout={handleLogout}
    />
  );
}
