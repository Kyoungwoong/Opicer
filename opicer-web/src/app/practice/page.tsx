"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { ROUTES } from "@/lib/routes";
import { logout } from "@/lib/auth-client";
import { useAuthState } from "@/lib/use-auth-state";
import { LoadingScreen } from "@/components/common/LoadingScreen";
import { TopicPracticeView } from "@/features/practice/components/TopicPracticeView";

export default function PracticePage() {
  const auth = useAuthState();
  const router = useRouter();

  useEffect(() => {
    if (auth.status === "unauthenticated") {
      router.replace(ROUTES.login);
    }
  }, [auth.status, router]);

  if (auth.status === "unauthenticated") return <LoadingScreen />;
  if (auth.status === "loading") return <LoadingScreen />;

  const handleLogout = async () => {
    await logout();
    window.location.href = ROUTES.login;
  };

  return (
    <TopicPracticeView
      userLabel={auth.user.name ?? auth.user.email ?? "사용자"}
      onLogout={handleLogout}
    />
  );
}
