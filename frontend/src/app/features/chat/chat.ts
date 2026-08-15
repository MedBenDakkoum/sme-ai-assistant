import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ChatService } from '../../core/services/chat.service';

interface Message {
  role: 'user' | 'assistant';
  content: string;
  sources?: string[];
}

@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslatePipe],
  templateUrl: './chat.html',
  styleUrl: './chat.css'
})
export class Chat implements OnInit {

  messages: Message[] = [];
  currentQuestion = '';
  isLoading = false;

  constructor(
    private chatService: ChatService,
    private translate: TranslateService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.chatService.getHistory().subscribe({
      next: (history) => {
        this.messages = history
          .filter(m => m.role === 'user' || m.role === 'assistant')
          .map(m => ({ role: m.role as 'user' | 'assistant', content: m.content }));
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('[Chat] Échec chargement historique:', err);
        this.messages = [];   // écran vide inchangé
      }
    });
  }

  newConversation(): void {
    this.chatService.resetSessionId();
    this.messages = [];
    this.currentQuestion = '';
    this.cdr.detectChanges();
  }

  sendMessage(): void {
    const question = this.currentQuestion.trim();
    if (!question || this.isLoading) {
      return;
    }

    // Message utilisateur
    this.messages = [...this.messages, { role: 'user', content: question }];
    this.currentQuestion = '';
    this.isLoading = true;
    this.cdr.detectChanges();

    this.chatService.ask(question).subscribe({
      next: (response) => {
        console.log('[Chat] Réponse reçue:', response);
        const content = response?.answer ?? this.translate.instant('chat.emptyResponse');
        this.messages = [...this.messages, { role: 'assistant', content }];
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (err: any) => {
        console.error('[Chat] Erreur:', err);
        this.messages = [...this.messages, {
          role: 'assistant',
          content: this.translate.instant('chat.error')
        }];
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      complete: () => {
        // Filet de sécurité : garantit la disparition de "Réflexion en cours..."
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
  }
}
