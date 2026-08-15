import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { NavBar } from './shared/components/navbar/navbar';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, NavBar],
  template: `
    <div class="flex flex-col h-screen">
      <app-navbar />
      <main class="flex-1 overflow-y-auto">
        <router-outlet />
      </main>
    </div>
  `
})
export class App {}