import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { TranslateService, type Language } from '@ngx-translate/core';
import { firstValueFrom } from 'rxjs';

export const SUPPORTED_LANGS = ['fr', 'en', 'de', 'ar'] as const;
export type SupportedLang = (typeof SUPPORTED_LANGS)[number];

const STORAGE_KEY = 'app-lang';

@Injectable({ providedIn: 'root' })
export class LanguageService {

  constructor(
    @Inject(PLATFORM_ID) private platformId: Object,
    private translate: TranslateService
  ) {}

  init(): Promise<unknown> {
    const lang = this.resolveInitialLang();
    this.translate.onLangChange.subscribe(({ lang: next }) => {
      this.applyDocument(next as SupportedLang);
      if (isPlatformBrowser(this.platformId)) {
        localStorage.setItem(STORAGE_KEY, next);
      }
    });
    return firstValueFrom(this.translate.use(lang));
  }

  use(lang: SupportedLang): void {
    this.translate.use(lang);
  }

  current(): SupportedLang {
    return (this.translate.getCurrentLang() ?? 'fr') as SupportedLang;
  }

  private resolveInitialLang(): SupportedLang {
    if (isPlatformBrowser(this.platformId)) {
      const saved = localStorage.getItem(STORAGE_KEY);
      if (saved && (SUPPORTED_LANGS as readonly string[]).includes(saved)) {
        return saved as SupportedLang;
      }
      const browser = (navigator.language || 'fr').slice(0, 2).toLowerCase();
      if ((SUPPORTED_LANGS as readonly string[]).includes(browser)) {
        return browser as SupportedLang;
      }
    }
    return 'fr';
  }

  private applyDocument(lang: SupportedLang): void {
    if (typeof document === 'undefined') {
      return;
    }
    document.documentElement.lang = lang;
    document.documentElement.dir = lang === 'ar' ? 'rtl' : 'ltr';
  }
}