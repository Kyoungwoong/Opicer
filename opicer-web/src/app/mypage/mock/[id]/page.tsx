"use client";

import { useEffect } from "react";
import { useParams, useRouter } from "next/navigation";
import { ROUTES } from "@/lib/routes";
import { logout } from "@/lib/auth-client";
import { useAuthState } from "@/lib/use-auth-state";
import { LoadingScreen } from "@/components/common/LoadingScreen";
import { MockResultView } from "@/features/mock/components/MockResultView";

export default function MockResultPage() {
  const auth = useAuthState();
  const router = useRouter();
  const { id } = useParams<{ id: string }>();

  useEffect(() => {
    if (auth.status === "unauthenticated") {
      router.replace(ROUTES.login);
    }
  }, [auth.status, router]);

  if (auth.status !== "authenticated") return <LoadingScreen />;

  const handleLogout = async () => {
    await logout();
    window.location.href = ROUTES.login;
  };

  return (
    <MockResultView
      mockTestId={id}
      userLabel={auth.user.name ?? auth.user.email ?? "사용자"}
      onLogout={handleLogout}
    />
  );
}
