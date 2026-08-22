import { Component, ErrorInfo, ReactNode } from "react";
import { Link } from "react-router-dom";

type Props = { children: ReactNode };
type State = { error: unknown };

export default class ErrorBoundary extends Component<Props, State> {
  state: State = { error: null };

  static getDerivedStateFromError(error: unknown): State {
    return { error };
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error("Unhandled render error", { message: error.message, componentStack: info.componentStack });
  }

  render() {
    if (!this.state.error) return this.props.children;
    return (
      <main className="page placeholder-page">
        <section className="surface-card" role="alert">
          <h1>화면을 표시하는 중 문제가 발생했습니다</h1>
          <p>잠시 후 다시 시도하거나 홈으로 이동해 주세요.</p>
          <div className="profile-actions">
            <button className="button button-soft" type="button" onClick={() => window.location.reload()}>새로고침</button>
            <Link className="button button-primary" to="/">홈으로</Link>
          </div>
        </section>
      </main>
    );
  }
}
