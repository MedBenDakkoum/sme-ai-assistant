import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ChatRequest {
  question: string;
  sessionId?: string;
}

export interface ChatResponse {
  answer: string;
}

export interface HistoryMessage {
  role: string;        // "user" | "assistant"
  content: string;
  timestamp: string;
}

@Injectable({
  providedIn: 'root'
})
export class ChatService {

  private apiUrl = 'http://localhost:8080/api/chat';
  private readonly sessionKey = 'chat_session_id';

  constructor(private http: HttpClient) {}

  getOrCreateSessionId(): string {
    let sessionId = localStorage.getItem(this.sessionKey);
    if (!sessionId) {
      sessionId = this.generateSessionId();
      localStorage.setItem(this.sessionKey, sessionId);
    }
    return sessionId;
  }

  resetSessionId(): string {
    localStorage.removeItem(this.sessionKey);
    return this.getOrCreateSessionId();
  }

  private generateSessionId(): string {
    if (typeof crypto !== 'undefined' && crypto.randomUUID) {
      return crypto.randomUUID();   // available on localhost (secure context)
    }
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, c => {
      const r = Math.random() * 16 | 0;
      const v = c === 'x' ? r : (r & 0x3 | 0x8);
      return v.toString(16);
    });
  }

  getHistory(): Observable<HistoryMessage[]> {
    const sessionId = this.getOrCreateSessionId();
    return this.http.get<HistoryMessage[]>(`${this.apiUrl}/${sessionId}/history`);
  }

  ask(question: string): Observable<ChatResponse> {
    const sessionId = this.getOrCreateSessionId();
    return this.http.post<ChatResponse>(this.apiUrl, { question, sessionId });
  }
}
