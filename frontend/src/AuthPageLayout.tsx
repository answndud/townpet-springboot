import { Link } from "react-router-dom";
import type { ReactNode } from "react";

type AuthPageLayoutProps = {
  eyebrow: string;
  title: string;
  description: string;
  children: ReactNode;
};

export default function AuthPageLayout({
  eyebrow,
  title,
  description,
  children,
}: AuthPageLayoutProps) {
  return (
    <main className="page auth-page">
      <section className="surface-card auth-card">
        <header className="auth-heading">
          <span className="eyebrow">{eyebrow}</span>
          <h1>{title}</h1>
          <p>{description}</p>
        </header>
        {children}
        <Link to="/" className="back-link">
          홈으로 돌아가기
        </Link>
      </section>
    </main>
  );
}
