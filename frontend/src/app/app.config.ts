import { ApplicationConfig, provideBrowserGlobalErrorListeners, APP_INITIALIZER } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideTranslateService } from '@ngx-translate/core';
import { provideTranslateHttpLoader } from '@ngx-translate/http-loader';

import { routes } from './app.routes';
import { LanguageService } from './core/services/language.service';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(),
    provideTranslateService({
      fallbackLang: 'fr',
      loader: provideTranslateHttpLoader({ prefix: './assets/i18n/', suffix: '.json' })
    }),
    {
      provide: APP_INITIALIZER,
      useFactory: (languageService: LanguageService) => () => languageService.init(),
      deps: [LanguageService],
      multi: true
    }
  ]
};