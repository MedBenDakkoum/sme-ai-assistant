import { Routes } from '@angular/router';
import { Chat } from './features/chat/chat';

export const routes: Routes = [
  { path: '', component: Chat },
  { path: '**', redirectTo: '' }
];
