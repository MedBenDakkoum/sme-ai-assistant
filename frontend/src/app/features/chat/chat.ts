import { Component, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ChatService } from '../../core/services/chat.service';

interface Message {
  role: 'user' | 'assistant';
  content: string;
}

@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './chat.html',
  styleUrl: './chat.css'
})
export class Chat {

  messages: Message[] = [];
  currentQuestion = '';
  isLoading = false;

  constructor(
    private chatService: ChatService,
    private cdr: ChangeDetectorRef
  ) {}

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
        const content = response?.answer ?? 'Réponse vide du serveur.';
        this.messages = [...this.messages, { role: 'assistant', content }];
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (err: any) => {
        console.error('[Chat] Erreur:', err);
        this.messages = [...this.messages, {
          role: 'assistant',
          content: 'Désolé, une erreur est survenue. Veuillez réessayer.'
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
