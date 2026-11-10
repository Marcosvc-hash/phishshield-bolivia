import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { TitleStrategy, provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { routes } from './app.routes';
import { authInterceptor } from './core/auth/auth.interceptor';
import { TituloPagina } from './core/titulo-pagina';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    // El titulo de la pestana sale del "title" de cada ruta
    { provide: TitleStrategy, useClass: TituloPagina },
    // Todas las peticiones pasan por authInterceptor, que agrega el token
    provideHttpClient(withInterceptors([authInterceptor]))
  ]
};
