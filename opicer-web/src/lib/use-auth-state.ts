import { useEffect, useState } from "react";
import { fetchAuthMe } from "@/lib/auth-client";
import type { AuthState } from "@/types/auth";

export function useAuthState(): AuthState {
  const [state, setState] = useState<AuthState>({ status: "loading" });

  useEffect(() => {
    let alive = true;

    fetchAuthMe()
      .then((user) => {
        if (!alive) return;
        if (user) setState({ status: "authenticated", user });
        else setState({ status: "unauthenticated" });
      })
      .catch(() => {
        if (!alive) return;
        setState({ status: "unauthenticated" });
      });

    return () => {
      alive = false;
    };
  }, []);

  return state;
}
