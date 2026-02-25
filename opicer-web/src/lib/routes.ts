export const ROUTES = {
  home: "/",
  login: "/login",
  admin: "/admin",
  auth: {
    loginKakao: "/api/auth/login/kakao",
    me: "/api/auth/me",
    logout: "/api/auth/logout",
    error: "/auth/error",
    success: "/auth/success",
  },
} as const;
