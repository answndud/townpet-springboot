import { createContext, ReactNode, useCallback, useContext, useEffect, useMemo, useRef, useState } from "react";
import { useLocation } from "react-router-dom";
import { ApiError, memberApi, type Member } from "../api/client";

export type AuthStatus = "loading" | "authenticated" | "anonymous" | "error";

type AuthContextValue = {
  status: AuthStatus;
  member: Member | null;
  refresh: () => void;
};

const AuthContext = createContext<AuthContextValue | null>(null);

function isCredentialsRoute(pathname: string) {
  return ["/login", "/password", "/verify-email"].some(
    (path) => pathname === path || pathname.startsWith(`${path}/`),
  );
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const { pathname } = useLocation();
  const [state, setState] = useState<{ status: AuthStatus; member: Member | null }>({
    status: "loading",
    member: null,
  });
  const controllerRef = useRef<AbortController | null>(null);

  const loadMember = useCallback(() => {
    controllerRef.current?.abort();
    const controller = new AbortController();
    controllerRef.current = controller;
    setState({ status: "loading", member: null });
    memberApi.current(controller.signal)
      .then((member) => {
        if (controllerRef.current === controller) setState({ status: "authenticated", member });
      })
      .catch((requestError: unknown) => {
        if (requestError instanceof DOMException && requestError.name === "AbortError") return;
        if (controllerRef.current !== controller) return;
        if (requestError instanceof ApiError && requestError.status === 401) {
          setState({ status: "anonymous", member: null });
          return;
        }
        setState({ status: "error", member: null });
      });
  }, []);

  const credentialsRoute = isCredentialsRoute(pathname);
  useEffect(() => {
    if (credentialsRoute) {
      controllerRef.current?.abort();
      // Keep the transition unresolved until the next protected route can
      // load the session. Setting `anonymous` here races with login's
      // navigate() and can immediately redirect a successful login back to
      // the credentials page.
      setState({ status: "loading", member: null });
      return;
    }
    loadMember();
  }, [credentialsRoute, loadMember]);

  useEffect(() => {
    const handleAuthChange = () => {
      if (!credentialsRoute) loadMember();
    };
    window.addEventListener("townpet:auth-change", handleAuthChange);
    return () => window.removeEventListener("townpet:auth-change", handleAuthChange);
  }, [credentialsRoute, loadMember]);

  useEffect(() => () => controllerRef.current?.abort(), []);

  const value = useMemo(() => ({ ...state, refresh: loadMember }), [loadMember, state]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const value = useContext(AuthContext);
  if (!value) throw new Error("useAuth must be used inside AuthProvider");
  return value;
}
