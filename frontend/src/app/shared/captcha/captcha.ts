import {
  Component, ElementRef, NgZone, OnDestroy, afterNextRender, inject, input, output, viewChild
} from '@angular/core';

const SCRIPT_URL = 'https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit';

/** Widget de Cloudflare Turnstile. Emite el token al resolverse y null si expira o falla. */
@Component({
  selector: 'app-captcha',
  standalone: true,
  template: `<div #container></div>`
})
export class Captcha implements OnDestroy {
  private zone = inject(NgZone);

  siteKey = input.required<string>();
  tokenChange = output<string | null>();

  private container = viewChild.required<ElementRef<HTMLDivElement>>('container');
  private widgetId: string | null = null;

  constructor() {
    afterNextRender(() => {
      this.loadScript()
        .then(() => this.renderWidget())
        .catch(() => this.tokenChange.emit(null));
    });
  }

  /** El token es de un solo uso: despues de un intento fallido hay que pedir uno nuevo. */
  reset(): void {
    const turnstile = (window as any).turnstile;
    if (turnstile && this.widgetId !== null) {
      turnstile.reset(this.widgetId);
    }
    this.tokenChange.emit(null);
  }

  ngOnDestroy(): void {
    const turnstile = (window as any).turnstile;
    if (turnstile && this.widgetId !== null) {
      turnstile.remove(this.widgetId);
    }
  }

  private renderWidget(): void {
    const turnstile = (window as any).turnstile;
    if (!turnstile) {
      return;
    }
    this.widgetId = turnstile.render(this.container().nativeElement, {
      sitekey: this.siteKey(),
      callback: (token: string) => this.zone.run(() => this.tokenChange.emit(token)),
      'expired-callback': () => this.zone.run(() => this.tokenChange.emit(null)),
      'error-callback': () => this.zone.run(() => this.tokenChange.emit(null))
    });
  }

  private loadScript(): Promise<void> {
    if ((window as any).turnstile) {
      return Promise.resolve();
    }
    return new Promise<void>((resolve, reject) => {
      const existing = document.querySelector<HTMLScriptElement>(`script[src="${SCRIPT_URL}"]`);
      const script = existing ?? document.createElement('script');
      script.addEventListener('load', () => resolve());
      script.addEventListener('error', () => reject());
      if (!existing) {
        script.src = SCRIPT_URL;
        script.async = true;
        document.head.appendChild(script);
      }
    });
  }
}