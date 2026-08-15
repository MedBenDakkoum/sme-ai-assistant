import { Component, ElementRef, HostListener, ViewChild } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { UpperCasePipe } from '@angular/common';
import { TranslatePipe } from '@ngx-translate/core';

import { LanguageService, SUPPORTED_LANGS, SupportedLang } from '../../../core/services/language.service';

const FLAGS: Record<SupportedLang, string> = {
  fr: '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 16"><rect width="8" height="16" fill="#0055A4"/><rect x="8" width="8" height="16" fill="#FFFFFF"/><rect x="16" width="8" height="16" fill="#EF4135"/></svg>',
  en: '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 60 30"><path d="M0,0 v30 h60 v-30 z" fill="#012169"/><path d="M0,30 L60,0" stroke="#FFFFFF" stroke-width="6"/><path d="M0,0 L60,30" stroke="#FFFFFF" stroke-width="6"/><path d="M0,30 L60,0" stroke="#C8102E" stroke-width="4"/><path d="M0,0 L60,30" stroke="#C8102E" stroke-width="4"/><path d="M30,0 v30" stroke="#FFFFFF" stroke-width="10"/><path d="M0,15 h60" stroke="#FFFFFF" stroke-width="10"/><path d="M30,0 v30" stroke="#C8102E" stroke-width="6"/><path d="M0,15 h60" stroke="#C8102E" stroke-width="6"/></svg>',
  de: '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 16"><rect width="24" height="5.33" fill="#000000"/><rect y="5.33" width="24" height="5.33" fill="#DD0000"/><rect y="10.67" width="24" height="5.33" fill="#FFCE00"/></svg>',
  ar: '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 16"><rect width="24" height="16" fill="#006C35"/><rect y="7" width="24" height="3.5" fill="#FFFFFF"/></svg>'
};

const LANG_NAMES: Record<SupportedLang, string> = {
  fr: 'Français',
  en: 'English',
  de: 'Deutsch',
  ar: 'العربية'
};

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, UpperCasePipe, TranslatePipe],
  templateUrl: './navbar.html',
  styleUrl: './navbar.css'
})
export class NavBar {

  langs: SupportedLang[] = [...SUPPORTED_LANGS];
  langNames: Record<SupportedLang, string> = LANG_NAMES;
  flags: Record<SupportedLang, SafeHtml>;
  dropdownOpen = false;

  @ViewChild('dropdown') dropdown!: ElementRef<HTMLDivElement>;

  constructor(
    private sanitizer: DomSanitizer,
    private languageService: LanguageService
  ) {
    this.flags = {
      fr: this.sanitize(FLAGS.fr),
      en: this.sanitize(FLAGS.en),
      de: this.sanitize(FLAGS.de),
      ar: this.sanitize(FLAGS.ar)
    };
  }

  get currentLang(): SupportedLang {
    return this.languageService.current();
  }

  toggleDropdown(): void {
    this.dropdownOpen = !this.dropdownOpen;
  }

  selectLang(lang: SupportedLang): void {
    this.dropdownOpen = false;
    this.languageService.use(lang);
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: Event): void {
    const target = event.target instanceof Node ? event.target : null;
    if (this.dropdownOpen && this.dropdown && !this.dropdown.nativeElement.contains(target)) {
      this.dropdownOpen = false;
    }
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    this.dropdownOpen = false;
  }

  private sanitize(svg: string): SafeHtml {
    return this.sanitizer.bypassSecurityTrustHtml(svg);
  }
}