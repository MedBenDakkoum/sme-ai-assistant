import { Routes } from '@angular/router';
import { Chat } from './features/chat/chat';
import { AdminDocuments } from './features/admin/admin';

export const routes: Routes = [
  { path: '', component: Chat },
  { path: 'admin', component: AdminDocuments },
  { path: '**', redirectTo: '' }
];
