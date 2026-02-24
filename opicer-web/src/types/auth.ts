export type User = {
  id: string;
  email: string | null;
  name: string | null;
  role: string;
  provider: string;
};

export type AuthState =
  | { status: "loading" }
  | { status: "authenticated"; user: User }
  | { status: "unauthenticated" };
