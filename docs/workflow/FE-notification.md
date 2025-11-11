# React 프론트엔드 - 투표 알림 WebSocket 연동 가이드

## 📋 목차
1. [개요](#개요)
2. [사전 요구사항](#사전-요구사항)
3. [의존성 설치](#의존성-설치)
4. [WebSocket 연결 설정](#websocket-연결-설정)
5. [React Hook 구현](#react-hook-구현)
6. [컴포넌트 통합](#컴포넌트-통합)
7. [UI 알림 구현](#ui-알림-구현)
8. [에러 처리 및 재연결](#에러-처리-및-재연결)
9. [테스트 가이드](#테스트-가이드)
10. [베스트 프랙티스](#베스트-프랙티스)

---

## 개요

### 목적
백엔드 서버의 WebSocket/STOMP 엔드포인트에 연결하여 투표 종료 및 종료 10분 전 알림을 실시간으로 수신합니다.

### 알림 종류
- **투표 종료 10분 전 알림**: `/topic/vote/soon` 구독
- **투표 종료 알림**: `/topic/vote/end` 구독

### 메시지 포맷
```typescript
interface VoteNotificationMessage {
  voteId: number;
  title: string;
  type: 'COMPLETED' | 'ENDING_SOON';
  timestamp: string; // ISO 8601 형식
}
```

---

## 사전 요구사항

- React 18+ (함수형 컴포넌트 및 Hooks 사용)
- TypeScript (권장)
- Node.js 16+
- 백엔드 서버 WebSocket 엔드포인트: `http://localhost:8080/ws` (개발 환경 기준)

---

## 의존성 설치

### NPM
```bash
npm install sockjs-client @stomp/stompjs
npm install --save-dev @types/sockjs-client
```

### Yarn
```bash
yarn add sockjs-client @stomp/stompjs
yarn add -D @types/sockjs-client
```

### 패키지 설명
- **sockjs-client**: WebSocket fallback 지원 (구형 브라우저 호환)
- **@stomp/stompjs**: STOMP 프로토콜 클라이언트 라이브러리

---

## WebSocket 연결 설정

### 1. WebSocket 클라이언트 유틸리티 생성

**파일 위치**: `src/utils/websocket.ts`

```typescript
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

export class WebSocketClient {
  private client: Client | null = null;
  private subscriptions: Map<string, StompSubscription> = new Map();

  constructor(private url: string) {}

  /**
   * WebSocket 연결
   */
  connect(
    onConnect: () => void,
    onError: (error: any) => void
  ): Promise<void> {
    return new Promise((resolve, reject) => {
      this.client = new Client({
        webSocketFactory: () => new SockJS(this.url),
        reconnectDelay: 5000, // 5초 후 재연결
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
        debug: (str) => {
          console.log('[STOMP Debug]', str);
        },
        onConnect: () => {
          console.log('[WebSocket] Connected');
          onConnect();
          resolve();
        },
        onStompError: (frame) => {
          console.error('[WebSocket] STOMP Error', frame);
          onError(frame);
          reject(frame);
        },
        onWebSocketError: (event) => {
          console.error('[WebSocket] Error', event);
          onError(event);
        },
      });

      this.client.activate();
    });
  }

  /**
   * 채널 구독
   */
  subscribe(
    destination: string,
    callback: (message: IMessage) => void
  ): string {
    if (!this.client?.connected) {
      throw new Error('WebSocket is not connected');
    }

    const subscription = this.client.subscribe(destination, callback);
    this.subscriptions.set(destination, subscription);

    console.log(`[WebSocket] Subscribed to ${destination}`);
    return subscription.id;
  }

  /**
   * 구독 해제
   */
  unsubscribe(destination: string): void {
    const subscription = this.subscriptions.get(destination);
    if (subscription) {
      subscription.unsubscribe();
      this.subscriptions.delete(destination);
      console.log(`[WebSocket] Unsubscribed from ${destination}`);
    }
  }

  /**
   * 연결 종료
   */
  disconnect(): void {
    if (this.client) {
      this.subscriptions.forEach((sub) => sub.unsubscribe());
      this.subscriptions.clear();
      this.client.deactivate();
      console.log('[WebSocket] Disconnected');
    }
  }

  /**
   * 연결 상태 확인
   */
  isConnected(): boolean {
    return this.client?.connected ?? false;
  }
}

// 싱글톤 인스턴스 (선택사항)
const WEBSOCKET_URL = process.env.REACT_APP_WEBSOCKET_URL || 'http://localhost:8080/ws';
export const websocketClient = new WebSocketClient(WEBSOCKET_URL);
```

---

## React Hook 구현

### 2. useVoteNotification Hook 생성

**파일 위치**: `src/hooks/useVoteNotification.ts`

```typescript
import { useEffect, useState, useCallback, useRef } from 'react';
import { IMessage } from '@stomp/stompjs';
import { websocketClient } from '../utils/websocket';

export interface VoteNotificationMessage {
  voteId: number;
  title: string;
  type: 'COMPLETED' | 'ENDING_SOON';
  timestamp: string;
}

export interface UseVoteNotificationOptions {
  onVoteCompleted?: (notification: VoteNotificationMessage) => void;
  onVoteEndingSoon?: (notification: VoteNotificationMessage) => void;
  onError?: (error: any) => void;
  autoConnect?: boolean; // 자동 연결 여부 (기본: true)
}

export const useVoteNotification = (options: UseVoteNotificationOptions = {}) => {
  const {
    onVoteCompleted,
    onVoteEndingSoon,
    onError,
    autoConnect = true,
  } = options;

  const [isConnected, setIsConnected] = useState(false);
  const [error, setError] = useState<Error | null>(null);
  const [notifications, setNotifications] = useState<VoteNotificationMessage[]>([]);

  // Ref로 최신 콜백 보관 (useEffect 의존성 문제 방지)
  const callbacksRef = useRef({ onVoteCompleted, onVoteEndingSoon, onError });

  useEffect(() => {
    callbacksRef.current = { onVoteCompleted, onVoteEndingSoon, onError };
  }, [onVoteCompleted, onVoteEndingSoon, onError]);

  /**
   * 메시지 핸들러
   */
  const handleMessage = useCallback((message: IMessage, type: 'COMPLETED' | 'ENDING_SOON') => {
    try {
      const notification: VoteNotificationMessage = JSON.parse(message.body);

      console.log(`[Vote Notification] ${type}:`, notification);

      // 상태 업데이트
      setNotifications((prev) => [notification, ...prev].slice(0, 50)); // 최근 50개만 유지

      // 콜백 실행
      if (type === 'COMPLETED' && callbacksRef.current.onVoteCompleted) {
        callbacksRef.current.onVoteCompleted(notification);
      } else if (type === 'ENDING_SOON' && callbacksRef.current.onVoteEndingSoon) {
        callbacksRef.current.onVoteEndingSoon(notification);
      }
    } catch (err) {
      console.error('[Vote Notification] Failed to parse message:', err);
      const error = err instanceof Error ? err : new Error('Failed to parse message');
      setError(error);
      callbacksRef.current.onError?.(error);
    }
  }, []);

  /**
   * WebSocket 연결
   */
  const connect = useCallback(async () => {
    if (websocketClient.isConnected()) {
      console.log('[Vote Notification] Already connected');
      return;
    }

    try {
      setError(null);

      await websocketClient.connect(
        () => {
          setIsConnected(true);

          // 투표 종료 알림 구독
          websocketClient.subscribe('/topic/vote/end', (msg) =>
            handleMessage(msg, 'COMPLETED')
          );

          // 투표 종료 10분 전 알림 구독
          websocketClient.subscribe('/topic/vote/soon', (msg) =>
            handleMessage(msg, 'ENDING_SOON')
          );
        },
        (err) => {
          const error = err instanceof Error ? err : new Error('Connection failed');
          setError(error);
          setIsConnected(false);
          callbacksRef.current.onError?.(error);
        }
      );
    } catch (err) {
      const error = err instanceof Error ? err : new Error('Connection failed');
      setError(error);
      callbacksRef.current.onError?.(error);
    }
  }, [handleMessage]);

  /**
   * WebSocket 연결 해제
   */
  const disconnect = useCallback(() => {
    websocketClient.disconnect();
    setIsConnected(false);
  }, []);

  /**
   * 알림 목록 초기화
   */
  const clearNotifications = useCallback(() => {
    setNotifications([]);
  }, []);

  /**
   * 자동 연결 및 정리
   */
  useEffect(() => {
    if (autoConnect) {
      connect();
    }

    return () => {
      if (autoConnect) {
        disconnect();
      }
    };
  }, [autoConnect, connect, disconnect]);

  return {
    isConnected,
    error,
    notifications,
    connect,
    disconnect,
    clearNotifications,
  };
};
```

---

## 컴포넌트 통합

### 3. 앱 레벨에서 WebSocket 연결

**파일 위치**: `src/App.tsx`

```typescript
import React, { useCallback } from 'react';
import { useVoteNotification, VoteNotificationMessage } from './hooks/useVoteNotification';
import VoteNotificationToast from './components/VoteNotificationToast';

function App() {
  const handleVoteCompleted = useCallback((notification: VoteNotificationMessage) => {
    console.log('투표 종료:', notification);
    // 토스트 알림 표시, 상태 업데이트 등
  }, []);

  const handleVoteEndingSoon = useCallback((notification: VoteNotificationMessage) => {
    console.log('투표 종료 임박:', notification);
    // 토스트 알림 표시, 상태 업데이트 등
  }, []);

  const handleError = useCallback((error: any) => {
    console.error('WebSocket 에러:', error);
    // 에러 토스트 표시
  }, []);

  const { isConnected, error, notifications } = useVoteNotification({
    onVoteCompleted: handleVoteCompleted,
    onVoteEndingSoon: handleVoteEndingSoon,
    onError: handleError,
    autoConnect: true,
  });

  return (
    <div className="App">
      {/* 연결 상태 표시 (개발 환경) */}
      {process.env.NODE_ENV === 'development' && (
        <div style={{ position: 'fixed', top: 10, right: 10, padding: 10, background: '#f0f0f0' }}>
          WebSocket: {isConnected ? '🟢 Connected' : '🔴 Disconnected'}
          {error && <div style={{ color: 'red' }}>Error: {error.message}</div>}
        </div>
      )}

      {/* 알림 토스트 */}
      <VoteNotificationToast notifications={notifications} />

      {/* 나머지 앱 컨텐츠 */}
    </div>
  );
}

export default App;
```

### 4. 특정 페이지에서만 연결하기

투표 페이지에서만 WebSocket 연결이 필요한 경우:

**파일 위치**: `src/pages/VotePage.tsx`

```typescript
import React, { useEffect } from 'react';
import { useVoteNotification } from '../hooks/useVoteNotification';
import { toast } from 'react-toastify'; // 또는 다른 토스트 라이브러리

function VotePage() {
  const { isConnected, connect, disconnect } = useVoteNotification({
    onVoteCompleted: (notification) => {
      toast.info(`"${notification.title}" 투표가 종료되었습니다!`);
    },
    onVoteEndingSoon: (notification) => {
      toast.warning(`"${notification.title}" 투표가 10분 후 종료됩니다!`);
    },
    autoConnect: false, // 수동 연결
  });

  useEffect(() => {
    connect();
    return () => disconnect();
  }, [connect, disconnect]);

  return (
    <div>
      <h1>투표 페이지</h1>
      {/* 투표 목록 등 */}
    </div>
  );
}

export default VotePage;
```

---

## UI 알림 구현

### 5. Toast 알림 컴포넌트

**파일 위치**: `src/components/VoteNotificationToast.tsx`

```typescript
import React, { useEffect } from 'react';
import { VoteNotificationMessage } from '../hooks/useVoteNotification';
import { toast, ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';

interface Props {
  notifications: VoteNotificationMessage[];
}

const VoteNotificationToast: React.FC<Props> = ({ notifications }) => {
  useEffect(() => {
    if (notifications.length === 0) return;

    const latest = notifications[0];

    if (latest.type === 'COMPLETED') {
      toast.info(
        <div>
          <strong>투표 종료</strong>
          <p>{latest.title}</p>
        </div>,
        {
          position: 'top-right',
          autoClose: 5000,
          hideProgressBar: false,
          closeOnClick: true,
          pauseOnHover: true,
        }
      );
    } else if (latest.type === 'ENDING_SOON') {
      toast.warning(
        <div>
          <strong>⏰ 투표 종료 10분 전</strong>
          <p>{latest.title}</p>
        </div>,
        {
          position: 'top-right',
          autoClose: 8000,
        }
      );
    }
  }, [notifications]);

  return <ToastContainer />;
};

export default VoteNotificationToast;
```

### 6. Custom 알림 컴포넌트 (Toast 라이브러리 없이)

**파일 위치**: `src/components/CustomNotification.tsx`

```typescript
import React, { useState, useEffect } from 'react';
import { VoteNotificationMessage } from '../hooks/useVoteNotification';
import './CustomNotification.css';

interface Props {
  notifications: VoteNotificationMessage[];
}

const CustomNotification: React.FC<Props> = ({ notifications }) => {
  const [visible, setVisible] = useState<VoteNotificationMessage | null>(null);

  useEffect(() => {
    if (notifications.length === 0) return;

    const latest = notifications[0];
    setVisible(latest);

    const timer = setTimeout(() => {
      setVisible(null);
    }, 5000);

    return () => clearTimeout(timer);
  }, [notifications]);

  if (!visible) return null;

  const isEndingSoon = visible.type === 'ENDING_SOON';

  return (
    <div className={`notification ${isEndingSoon ? 'warning' : 'info'}`}>
      <div className="notification-header">
        {isEndingSoon ? '⏰ 투표 종료 10분 전' : '✅ 투표 종료'}
      </div>
      <div className="notification-body">{visible.title}</div>
      <button className="notification-close" onClick={() => setVisible(null)}>
        ✕
      </button>
    </div>
  );
};

export default CustomNotification;
```

**CSS 파일**: `src/components/CustomNotification.css`

```css
.notification {
  position: fixed;
  top: 20px;
  right: 20px;
  max-width: 400px;
  padding: 16px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  z-index: 9999;
  animation: slideIn 0.3s ease-out;
}

@keyframes slideIn {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

.notification.info {
  border-left: 4px solid #3b82f6;
}

.notification.warning {
  border-left: 4px solid #f59e0b;
}

.notification-header {
  font-weight: bold;
  font-size: 14px;
  margin-bottom: 8px;
}

.notification-body {
  font-size: 13px;
  color: #666;
}

.notification-close {
  position: absolute;
  top: 8px;
  right: 8px;
  background: none;
  border: none;
  font-size: 18px;
  cursor: pointer;
  color: #999;
}

.notification-close:hover {
  color: #333;
}
```

---

## 에러 처리 및 재연결

### 7. 재연결 로직 강화

이미 `WebSocketClient`에 `reconnectDelay: 5000` 설정이 되어 있지만, 수동 재연결 UI를 추가할 수 있습니다.

**파일 위치**: `src/components/WebSocketStatus.tsx`

```typescript
import React from 'react';
import { useVoteNotification } from '../hooks/useVoteNotification';

const WebSocketStatus: React.FC = () => {
  const { isConnected, error, connect } = useVoteNotification({
    autoConnect: true,
  });

  if (isConnected) {
    return null; // 연결되어 있으면 표시하지 않음
  }

  return (
    <div style={{
      position: 'fixed',
      bottom: 20,
      left: 20,
      padding: '12px 16px',
      background: '#ef4444',
      color: 'white',
      borderRadius: 8,
      boxShadow: '0 4px 12px rgba(0,0,0,0.15)',
    }}>
      <div style={{ marginBottom: 8 }}>
        🔴 실시간 알림 연결 끊김
        {error && <div style={{ fontSize: 12 }}>{error.message}</div>}
      </div>
      <button
        onClick={connect}
        style={{
          padding: '6px 12px',
          background: 'white',
          color: '#ef4444',
          border: 'none',
          borderRadius: 4,
          cursor: 'pointer',
          fontWeight: 'bold',
        }}
      >
        재연결
      </button>
    </div>
  );
};

export default WebSocketStatus;
```

---

## 테스트 가이드

### 8. 개발 환경 테스트

#### 1) 백엔드 서버 실행
```bash
cd mechuragi_main_server
./gradlew bootRun
```

#### 2) 프론트엔드 실행
```bash
cd frontend
npm start
```

#### 3) 테스트 시나리오

**시나리오 1: 투표 종료 10분 전 알림**
1. 투표 생성 (마감 시간을 현재 + 11분으로 설정)
2. 1분 대기
3. 10분 전 알림이 표시되는지 확인

**시나리오 2: 투표 종료 알림**
1. 투표 생성 (마감 시간을 현재 + 1분으로 설정)
2. 1~2분 대기
3. 종료 알림이 표시되는지 확인

**시나리오 3: 재연결 테스트**
1. WebSocket 연결 확인
2. 백엔드 서버 재시작
3. 5초 후 자동 재연결 확인
4. 알림 정상 수신 확인

### 9. 브라우저 개발자 도구 확인

#### Console 로그
```
[STOMP Debug] ...
[WebSocket] Connected
[WebSocket] Subscribed to /topic/vote/end
[WebSocket] Subscribed to /topic/vote/soon
[Vote Notification] ENDING_SOON: { voteId: 123, ... }
```

#### Network 탭
- `ws://localhost:8080/ws` 연결 확인
- WebSocket 프레임 확인

---

## 베스트 프랙티스

### 1. 환경 변수 설정

**파일 위치**: `.env.development`

```env
REACT_APP_WEBSOCKET_URL=http://localhost:8080/ws
```

**파일 위치**: `.env.production`

```env
REACT_APP_WEBSOCKET_URL=https://api.mechuragi.com/ws
```

### 2. TypeScript 타입 안정성

모든 메시지와 이벤트에 타입을 명시하여 런타임 에러를 방지합니다.

```typescript
// types/vote.ts
export interface VoteNotificationMessage {
  voteId: number;
  title: string;
  type: 'COMPLETED' | 'ENDING_SOON';
  timestamp: string;
}
```

### 3. 메모리 누수 방지

- 컴포넌트 언마운트 시 반드시 `disconnect()` 호출
- `useEffect` cleanup 함수 활용

```typescript
useEffect(() => {
  connect();
  return () => disconnect(); // cleanup
}, []);
```

### 4. 중복 알림 방지

같은 투표에 대해 중복 알림이 표시되지 않도록 처리:

```typescript
const [shownNotifications, setShownNotifications] = useState<Set<number>>(new Set());

const handleVoteCompleted = useCallback((notification: VoteNotificationMessage) => {
  if (shownNotifications.has(notification.voteId)) {
    return; // 이미 표시한 알림
  }

  setShownNotifications((prev) => new Set(prev).add(notification.voteId));
  toast.info(`"${notification.title}" 투표가 종료되었습니다!`);
}, [shownNotifications]);
```

### 5. 네트워크 상태 모니터링

```typescript
useEffect(() => {
  const handleOnline = () => {
    console.log('네트워크 온라인');
    connect();
  };

  const handleOffline = () => {
    console.log('네트워크 오프라인');
    disconnect();
  };

  window.addEventListener('online', handleOnline);
  window.addEventListener('offline', handleOffline);

  return () => {
    window.removeEventListener('online', handleOnline);
    window.removeEventListener('offline', handleOffline);
  };
}, [connect, disconnect]);
```

### 6. 사용자 권한에 따른 구독

특정 사용자(예: 투표 참여자)만 알림을 받도록:

```typescript
const { isConnected } = useVoteNotification({
  onVoteCompleted: (notification) => {
    // 사용자가 참여한 투표인지 확인
    if (userVoteIds.includes(notification.voteId)) {
      toast.info(`참여한 투표 "${notification.title}"가 종료되었습니다!`);
    }
  },
});
```

### 7. 성능 최적화

- `useCallback`과 `useMemo`를 활용한 불필요한 리렌더링 방지
- 알림 목록 크기 제한 (최근 50개)
- Debounce/Throttle 적용

```typescript
import { debounce } from 'lodash';

const debouncedNotify = useMemo(
  () =>
    debounce((notification: VoteNotificationMessage) => {
      toast.info(notification.title);
    }, 500),
  []
);
```

---

## 트러블슈팅

### 문제 1: CORS 에러

**증상**: `Access-Control-Allow-Origin` 에러

**해결**: 백엔드 `WebSocketConfig.java`에서 CORS 설정 확인
```java
registry.addEndpoint("/ws")
        .setAllowedOriginPatterns("http://localhost:3000") // 프론트엔드 주소
        .withSockJS();
```

### 문제 2: 연결은 되지만 메시지 수신 안 됨

**원인**: 채널 이름 불일치

**확인 사항**:
- 프론트: `/topic/vote/end`, `/topic/vote/soon`
- 백엔드: `messagingTemplate.convertAndSend("/topic/vote/end", message)`

### 문제 3: 페이지 새로고침 시 중복 구독

**해결**: 싱글톤 패턴 또는 Context API 활용

```typescript
// WebSocketContext.tsx
import React, { createContext, useContext } from 'react';

const WebSocketContext = createContext<ReturnType<typeof useVoteNotification> | null>(null);

export const WebSocketProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const ws = useVoteNotification({ autoConnect: true });
  return <WebSocketContext.Provider value={ws}>{children}</WebSocketContext.Provider>;
};

export const useWebSocket = () => {
  const context = useContext(WebSocketContext);
  if (!context) throw new Error('useWebSocket must be used within WebSocketProvider');
  return context;
};
```

---

## 참고 자료

### 공식 문서
- [STOMP.js Documentation](https://stomp-js.github.io/stomp-websocket/)
- [SockJS Client](https://github.com/sockjs/sockjs-client)
- [React Hooks Best Practices](https://react.dev/reference/react)

### 프로젝트 파일
- 백엔드: `docs/workflow/notification-redis.md`
- 백엔드: `WebSocketConfig.java`

### 추가 라이브러리
- **react-toastify**: 토스트 알림 UI
  ```bash
  npm install react-toastify
  ```
- **zustand**: 전역 상태 관리 (WebSocket 상태 공유)
  ```bash
  npm install zustand
  ```

---

## 체크리스트

- [ ] 의존성 설치 (`sockjs-client`, `@stomp/stompjs`)
- [ ] `websocket.ts` 유틸리티 생성
- [ ] `useVoteNotification` Hook 구현
- [ ] 환경 변수 설정 (`.env.development`)
- [ ] 알림 UI 컴포넌트 구현
- [ ] 에러 처리 및 재연결 로직 추가
- [ ] 개발 환경에서 테스트
- [ ] 네트워크 단절 시나리오 테스트
- [ ] 프로덕션 빌드 및 배포

---

## 예제 코드 전체 구조

```
src/
├── utils/
│   └── websocket.ts          # WebSocket 클라이언트
├── hooks/
│   └── useVoteNotification.ts # React Hook
├── components/
│   ├── VoteNotificationToast.tsx  # Toast 알림
│   ├── CustomNotification.tsx     # Custom 알림
│   └── WebSocketStatus.tsx        # 연결 상태 표시
├── pages/
│   └── VotePage.tsx               # 투표 페이지
├── types/
│   └── vote.ts                    # 타입 정의
└── App.tsx                        # 앱 진입점
```
